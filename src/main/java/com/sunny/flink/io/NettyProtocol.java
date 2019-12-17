package com.sunny.flink.io;

import io.netty.channel.ChannelHandler;

public class NettyProtocol {
    private final NettyMessage.NettyMessageEncoder
            messageEncoder = new NettyMessage.NettyMessageEncoder();

    /**
     * Returns the server channel handlers.
     *
     * <pre>
     * +-------------------------------------------------------------------+
     * |                        SERVER CHANNEL PIPELINE                    |
     * |                                                                   |
     * |    +----------+----------+ (3) write  +----------------------+    |
     * |    | Queue of queues     +----------->| Message encoder      |    |
     * |    +----------+----------+            +-----------+----------+    |
     * |              /|\                                 \|/              |
     * |               | (2) enqueue                       |               |
     * |    +----------+----------+                        |               |
     * |    | Request handler     |                        |               |
     * |    +----------+----------+                        |               |
     * |              /|\                                  |               |
     * |               |                                   |               |
     * |   +-----------+-----------+                       |               |
     * |   | Message+Frame decoder |                       |               |
     * |   +-----------+-----------+                       |               |
     * |              /|\                                  |               |
     * +---------------+-----------------------------------+---------------+
     * |               | (1) client request               \|/
     * +---------------+-----------------------------------+---------------+
     * |               |                                   |               |
     * |       [ Socket.read() ]                    [ Socket.write() ]     |
     * |                                                                   |
     * |  Netty Internal I/O Threads (Transport Implementation)            |
     * +-------------------------------------------------------------------+
     * </pre>
     *
     * @return channel handlers
     */
    public ChannelHandler[] getServerChannelHandlers() {
        return new ChannelHandler[] {
                messageEncoder,
                new NettyMessage.NettyMessageDecoder(true)
        };
    }

    /**
     * Returns the client channel handlers.
     *
     * <pre>
     *     +-----------+----------+            +----------------------+
     *     | Remote input channel |            | request client       |
     *     +-----------+----------+            +-----------+----------+
     *                 |                                   | (1) write
     * +---------------+-----------------------------------+---------------+
     * |               |     CLIENT CHANNEL PIPELINE       |               |
     * |               |                                  \|/              |
     * |    +----------+----------+            +----------------------+    |
     * |    | Request handler     +            | Message encoder      |    |
     * |    +----------+----------+            +-----------+----------+    |
     * |              /|\                                 \|/              |
     * |               |                                   |               |
     * |    +----------+------------+                      |               |
     * |    | Message+Frame decoder |                      |               |
     * |    +----------+------------+                      |               |
     * |              /|\                                  |               |
     * +---------------+-----------------------------------+---------------+
     * |               | (3) server response              \|/ (2) client request
     * +---------------+-----------------------------------+---------------+
     * |               |                                   |               |
     * |       [ Socket.read() ]                    [ Socket.write() ]     |
     * |                                                                   |
     * |  Netty Internal I/O Threads (Transport Implementation)            |
     * +-------------------------------------------------------------------+
     * </pre>
     *
     * @return channel handlers
     */
    public ChannelHandler[] getClientChannelHandlers() {
        return new ChannelHandler[] {
                messageEncoder,
                new NettyMessage.NettyMessageDecoder(true)};
    }
}
