package com.sunny.flink.io;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ProtocolException;
import java.nio.Buffer;
import java.nio.ByteBuffer;

import static com.sunny.util.Preconditions.checkArgument;
import static com.sunny.util.Preconditions.checkNotNull;

public abstract class NettyMessage {

    // ------------------------------------------------------------------------
    // Note: Every NettyMessage subtype needs to have a public 0-argument
    // constructor in order to work with the generic deserializer.
    // ------------------------------------------------------------------------

    static final int FRAME_HEADER_LENGTH = 4 + 4 + 1; // frame length (4), magic number (4), msg ID (1)

    static final int MAGIC_NUMBER = 0xBADC0FFE;

    abstract ByteBuf write(ByteBufAllocator allocator) throws Exception;

    // ------------------------------------------------------------------------

    /**
     * Allocates a new (header and contents) buffer and adds some header information for the frame
     * decoder.
     *
     * <p>Before sending the buffer, you must write the actual length after adding the contents as
     * an integer to position <tt>0</tt>!
     *
     * @param allocator
     * 		byte buffer allocator to use
     * @param id
     * 		{@link NettyMessage} subclass ID
     *
     * @return a newly allocated direct buffer with header data written for {@link
     * NettyMessageDecoder}
     */
    public static ByteBuf allocateBuffer(ByteBufAllocator allocator, byte id) {
        return allocateBuffer(allocator, id, -1);
    }

    /**
     * Allocates a new (header and contents) buffer and adds some header information for the frame
     * decoder.
     *
     * <p>If the <tt>contentLength</tt> is unknown, you must write the actual length after adding
     * the contents as an integer to position <tt>0</tt>!
     *
     * @param allocator
     * 		byte buffer allocator to use
     * @param id
     * 		{@link NettyMessage} subclass ID
     * @param contentLength
     * 		content length (or <tt>-1</tt> if unknown)
     *
     * @return a newly allocated direct buffer with header data written for {@link
     * NettyMessageDecoder}
     */
    public static ByteBuf allocateBuffer(ByteBufAllocator allocator, byte id, int contentLength) {
        return allocateBuffer(allocator, id, 0, contentLength, true);
    }

    /**
     * Allocates a new buffer and adds some header information for the frame decoder.
     *
     * <p>If the <tt>contentLength</tt> is unknown, you must write the actual length after adding
     * the contents as an integer to position <tt>0</tt>!
     *
     * @param allocator
     * 		byte buffer allocator to use
     * @param id
     * 		{@link NettyMessage} subclass ID
     * @param messageHeaderLength
     * 		additional header length that should be part of the allocated buffer and is written
     * 		outside of this method
     * @param contentLength
     * 		content length (or <tt>-1</tt> if unknown)
     * @param allocateForContent
     * 		whether to make room for the actual content in the buffer (<tt>true</tt>) or whether to
     * 		only return a buffer with the header information (<tt>false</tt>)
     *
     * @return a newly allocated direct buffer with header data written for {@link
     * NettyMessageDecoder}
     */
    public static ByteBuf allocateBuffer(
            ByteBufAllocator allocator,
            byte id,
            int messageHeaderLength,
            int contentLength,
            boolean allocateForContent) {
        checkArgument(contentLength <= Integer.MAX_VALUE - FRAME_HEADER_LENGTH);

        final ByteBuf buffer;
        if (!allocateForContent) {
            buffer = allocator.directBuffer(FRAME_HEADER_LENGTH + messageHeaderLength);
        } else if (contentLength != -1) {
            buffer = allocator.directBuffer(FRAME_HEADER_LENGTH + messageHeaderLength + contentLength);
        } else {
            // content length unknown -> start with the default initial size (rather than FRAME_HEADER_LENGTH only):
            buffer = allocator.directBuffer();
        }
        buffer.writeInt(FRAME_HEADER_LENGTH + messageHeaderLength + contentLength); // may be updated later, e.g. if contentLength == -1
        buffer.writeInt(MAGIC_NUMBER);
        buffer.writeByte(id);

        return buffer;
    }

    // ------------------------------------------------------------------------
    // Generic NettyMessage encoder and decoder
    // ------------------------------------------------------------------------

    @ChannelHandler.Sharable
    static class NettyMessageEncoder extends ChannelOutboundHandlerAdapter {

        @Override
        public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
            if (msg instanceof NettyMessage) {

                ByteBuf serialized = null;

                try {
                    serialized = ((NettyMessage) msg).write(ctx.alloc());
                }
                catch (Throwable t) {
                    throw new IOException("Error while serializing message: " + msg, t);
                }
                finally {
                    if (serialized != null) {
                        ctx.write(serialized, promise);
                    }
                }
            }
            else {
                ctx.write(msg, promise);
            }
        }
    }

    /**
     * Message decoder based on netty's {@link LengthFieldBasedFrameDecoder} but avoiding the
     * additional memory copy inside {@link #extractFrame(ChannelHandlerContext, ByteBuf, int, int)}
     * since we completely decode the {@link ByteBuf} inside {@link #decode(ChannelHandlerContext,
     * ByteBuf)} and will not re-use it afterwards.
     *
     * <p>The frame-length encoder will be based on this transmission scheme created by {@link NettyMessage#allocateBuffer(ByteBufAllocator, byte, int)}:
     * <pre>
     * +------------------+------------------+--------++----------------+
     * | FRAME LENGTH (4) | MAGIC NUMBER (4) | ID (1) || CUSTOM MESSAGE |
     * +------------------+------------------+--------++----------------+
     * </pre>
     */
    static class NettyMessageDecoder extends LengthFieldBasedFrameDecoder {
        private final boolean restoreOldNettyBehaviour;

        /**
         * Creates a new message decoded with the required frame properties.
         *
         * @param restoreOldNettyBehaviour
         * 		restore Netty 4.0.27 code in {@link LengthFieldBasedFrameDecoder#extractFrame} to
         * 		copy instead of slicing the buffer
         */
        NettyMessageDecoder(boolean restoreOldNettyBehaviour) {
            super(Integer.MAX_VALUE, 0, 4, -4, 4);
            this.restoreOldNettyBehaviour = restoreOldNettyBehaviour;
        }

        @Override
        protected Object decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
            ByteBuf msg = (ByteBuf) super.decode(ctx, in);
            if (msg == null) {
                return null;
            }

            try {
                int magicNumber = msg.readInt();

                if (magicNumber != MAGIC_NUMBER) {
                    throw new IllegalStateException(
                            "Network stream corrupted: received incorrect magic number.");
                }

                byte msgId = msg.readByte();

                final NettyMessage decodedMsg;
                switch (msgId) {
                    case BufferResponse.ID:
                        decodedMsg = BufferResponse.readFrom(msg);
                        break;
                    case PartitionRequest.ID:
                        decodedMsg = PartitionRequest.readFrom(msg);
                        break;
                    case TaskEventRequest.ID:
                        decodedMsg = TaskEventRequest.readFrom(msg);
                        break;
                    case ErrorResponse.ID:
                        decodedMsg = ErrorResponse.readFrom(msg);
                        break;
                    case CancelPartitionRequest.ID:
                        decodedMsg = CancelPartitionRequest.readFrom(msg);
                        break;
                    case CloseRequest.ID:
                        decodedMsg = CloseRequest.readFrom(msg);
                        break;
                    case AddCredit.ID:
                        decodedMsg = AddCredit.readFrom(msg);
                        break;
                    case TestRequest.ID:
                        decodedMsg = TestRequest.readFrom(msg);
                        break;
                    default:
                        throw new ProtocolException(
                                "Received unknown message from producer: " + msg);
                }

                return decodedMsg;
            } finally {
                // ByteToMessageDecoder cleanup (only the BufferResponse holds on to the decoded
                // msg but already retain()s the buffer once)
                msg.release();
            }
        }

        @Override
        protected ByteBuf extractFrame(ChannelHandlerContext ctx, ByteBuf buffer, int index, int length) {
            if (restoreOldNettyBehaviour) {
				/*
				 * For non-credit based code paths with Netty >= 4.0.28.Final:
				 * These versions contain an improvement by Netty, which slices a Netty buffer
				 * instead of doing a memory copy [1] in the
				 * LengthFieldBasedFrameDecoder. In some situations, this
				 * interacts badly with our Netty pipeline leading to OutOfMemory
				 * errors.
				 *
				 * [1] https://github.com/netty/netty/issues/3704
				 *
				 * TODO: remove along with the non-credit based fallback protocol
				 */
                ByteBuf frame = ctx.alloc().buffer(length);
                frame.writeBytes(buffer, index, length);
                return frame;
            } else {
                return super.extractFrame(ctx, buffer, index, length);
            }
        }
    }

    // ------------------------------------------------------------------------
    // Server responses
    // ------------------------------------------------------------------------

    static class BufferResponse extends NettyMessage {

        private static final byte ID = 0;

        final ByteBuf buffer;


        final int sequenceNumber;

        final int backlog;

        final boolean isBuffer;

        private BufferResponse(
                ByteBuf buffer,
                boolean isBuffer,
                int sequenceNumber,
                int backlog) {
            this.buffer = checkNotNull(buffer);
            this.isBuffer = isBuffer;
            this.sequenceNumber = sequenceNumber;
            this.backlog = backlog;
        }


        boolean isBuffer() {
            return isBuffer;
        }

        ByteBuf getNettyBuffer() {
            return buffer;
        }

        void releaseBuffer() {
            buffer.release();
        }

        // --------------------------------------------------------------------
        // Serialization
        // --------------------------------------------------------------------

        @Override
        ByteBuf write(ByteBufAllocator allocator) throws IOException {
           return null;

        }
        static BufferResponse readFrom(ByteBuf buffer) {
            return null;
        }

    }

    static class ErrorResponse extends NettyMessage {

        private static final byte ID = 1;

        final Throwable cause;


        ErrorResponse(Throwable cause) {
            this.cause = checkNotNull(cause);
        }




        @Override
        ByteBuf write(ByteBufAllocator allocator) throws IOException {
            return null;
        }
        static ErrorResponse readFrom(ByteBuf buffer) {
            return null;
        }

    }

    // ------------------------------------------------------------------------
    // Client requests
    // ------------------------------------------------------------------------

    // 测试
    public static class TestRequest extends NettyMessage {

        private static final byte ID = 7;
        private long lower;
        private long upper;

        public TestRequest(long lower, long upper) {
            this.lower = lower;
            this.upper = upper;
        }

        @Override
        ByteBuf write(ByteBufAllocator allocator) throws IOException {
            ByteBuf result = null;

            try {
                result = allocateBuffer(allocator, ID, 8 + 8);
                result.writeLong(lower);
                result.writeLong(upper);

                return result;
            }
            catch (Throwable t) {
                if (result != null) {
                    result.release();
                }

                throw new IOException(t);
            }
        }
        static TestRequest readFrom(ByteBuf buffer) {
            long lower = buffer.readLong();
            long upper = buffer.readLong();
            return new TestRequest(lower,upper);
        }

    }



    // flink
    static class PartitionRequest extends NettyMessage {

        private static final byte ID = 2;

        @Override
        ByteBuf write(ByteBufAllocator allocator) throws IOException {
            return null;
        }
        static PartitionRequest readFrom(ByteBuf buffer) {
            return null;
        }

    }

    static class TaskEventRequest extends NettyMessage {
        private static final byte ID = 3;

        @Override
        ByteBuf write(ByteBufAllocator allocator) throws IOException {
           return null;
        }
        static TaskEventRequest readFrom(ByteBuf buffer) {
            return null;
        }
    }

    /**
     * Cancels the partition request of the {@link InputChannel} identified by
     * {@link InputChannelID}.
     *
     * <p>There is a 1:1 mapping between the input channel and partition per physical channel.
     * Therefore, the {@link InputChannelID} instance is enough to identify which request to cancel.
     */
    static class CancelPartitionRequest extends NettyMessage {
        private static final byte ID = 4;
        @Override
        ByteBuf write(ByteBufAllocator allocator) throws Exception {
            return null;
        }
        static CancelPartitionRequest readFrom(ByteBuf buffer) {
            return null;
        }
    }

    static class CloseRequest extends NettyMessage {
        private static final byte ID = 5;
        @Override
        ByteBuf write(ByteBufAllocator allocator) throws Exception {
            return null;
        }
        static CloseRequest readFrom(ByteBuf buffer) {
            return null;
        }
    }

    /**
     * Incremental credit announcement from the client to the server.
     */
    static class AddCredit extends NettyMessage {

        private static final byte ID = 6;
        final int credit;

        public AddCredit(int credit) {
            this.credit = credit;
        }

        @Override
        ByteBuf write(ByteBufAllocator allocator) throws IOException {
            return null;
        }

        static AddCredit readFrom(ByteBuf buffer) {
            return null;
        }
    }
}
