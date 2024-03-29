package com.sunny.flink.server;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.sunny.flink.io.NettyBufferPool;
import com.sunny.flink.io.NettyConfig;
import com.sunny.flink.io.NettyProtocol;
import com.sunny.flink.ssl.SSLHandlerFactory;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.ThreadFactory;

import static com.sunny.util.Preconditions.checkNotNull;
import static com.sunny.util.Preconditions.checkState;

public class NettyServer {private static final ThreadFactoryBuilder THREAD_FACTORY_BUILDER =
        new ThreadFactoryBuilder()
                .setDaemon(true)
                .setUncaughtExceptionHandler(FatalExitExceptionHandler.INSTANCE);

    private static final Logger LOG = LoggerFactory.getLogger(NettyServer.class);

    private final NettyConfig config;

    private ServerBootstrap bootstrap;

    private ChannelFuture bindFuture;

    private InetSocketAddress localAddress;

    NettyServer(NettyConfig config) {
        this.config = checkNotNull(config);
        localAddress = null;
    }

    void init(final NettyProtocol protocol, NettyBufferPool nettyBufferPool) throws IOException, InterruptedException {
        checkState(bootstrap == null, "Netty server has already been initialized.");

        final long start = System.nanoTime();

        bootstrap = new ServerBootstrap();

        // --------------------------------------------------------------------
        // Transport-specific configuration
        // --------------------------------------------------------------------

        switch (config.getTransportType()) {
            case NIO:
                initNioBootstrap();
                break;

            case EPOLL:
                initEpollBootstrap();
                break;

            case AUTO:
                if (Epoll.isAvailable()) {
                    initEpollBootstrap();
                    LOG.info("Transport type 'auto': using EPOLL.");
                }
                else {
                    initNioBootstrap();
                    LOG.info("Transport type 'auto': using NIO.");
                }
        }

        // --------------------------------------------------------------------
        // Configuration
        // --------------------------------------------------------------------

        // Server bind address
        bootstrap.localAddress(config.getServerAddress(), config.getServerPort());

        // Pooled allocators for Netty's ByteBuf instances
        bootstrap.option(ChannelOption.ALLOCATOR, nettyBufferPool);
        bootstrap.childOption(ChannelOption.ALLOCATOR, nettyBufferPool);

        if (config.getServerConnectBacklog() > 0) {
            bootstrap.option(ChannelOption.SO_BACKLOG, config.getServerConnectBacklog());
        }

        // Receive and send buffer size
        int receiveAndSendBufferSize = config.getSendAndReceiveBufferSize();
        if (receiveAndSendBufferSize > 0) {
            bootstrap.childOption(ChannelOption.SO_SNDBUF, receiveAndSendBufferSize);
            bootstrap.childOption(ChannelOption.SO_RCVBUF, receiveAndSendBufferSize);
        }

        // Low and high water marks for flow control
        // hack around the impossibility (in the current netty version) to set both watermarks at
        // the same time:
        final int defaultHighWaterMark = 64 * 1024; // from DefaultChannelConfig (not exposed)
        final int newLowWaterMark = config.getMemorySegmentSize() + 1;
        final int newHighWaterMark = 2 * config.getMemorySegmentSize();
        if (newLowWaterMark > defaultHighWaterMark) {
            bootstrap.childOption(ChannelOption.WRITE_BUFFER_HIGH_WATER_MARK, newHighWaterMark);
            bootstrap.childOption(ChannelOption.WRITE_BUFFER_LOW_WATER_MARK, newLowWaterMark);
        } else { // including (newHighWaterMark < defaultLowWaterMark)
            bootstrap.childOption(ChannelOption.WRITE_BUFFER_LOW_WATER_MARK, newLowWaterMark);
            bootstrap.childOption(ChannelOption.WRITE_BUFFER_HIGH_WATER_MARK, newHighWaterMark);
        }

        // SSL related configuration
        final SSLHandlerFactory sslHandlerFactory;
        try {
            sslHandlerFactory = config.createServerSSLEngineFactory();
        } catch (Exception e) {
            throw new IOException("Failed to initialize SSL Context for the Netty Server", e);
        }

        // --------------------------------------------------------------------
        // Child channel pipeline for accepted connections
        // --------------------------------------------------------------------

        bootstrap.childHandler(new ChannelInitializer<SocketChannel>() {
            @Override
            public void initChannel(SocketChannel channel) throws Exception {
                if (sslHandlerFactory != null) {
                    channel.pipeline().addLast("ssl", sslHandlerFactory.createNettySSLHandler());
                }

                channel.pipeline().addLast(protocol.getServerChannelHandlers());
            }
        });

        // --------------------------------------------------------------------
        // Start Server
        // --------------------------------------------------------------------

//        bindFuture = bootstrap.bind().syncUninterruptibly();
        // 绑定端口，同步等待成功
        bindFuture = bootstrap.bind().sync();
        // 等待服务端监听端口关闭
        bindFuture.channel().closeFuture().sync();

        localAddress = (InetSocketAddress) bindFuture.channel().localAddress();

        final long duration = (System.nanoTime() - start) / 1_000_000;
        LOG.info("Successful initialization (took {} ms). Listening on SocketAddress {}.", duration, localAddress);
    }

    NettyConfig getConfig() {
        return config;
    }

    ServerBootstrap getBootstrap() {
        return bootstrap;
    }

    public InetSocketAddress getLocalAddress() {
        return localAddress;
    }

    void shutdown() {
        final long start = System.nanoTime();
        if (bindFuture != null) {
            bindFuture.channel().close().awaitUninterruptibly();
            bindFuture = null;
        }

        if (bootstrap != null) {
            if (bootstrap.group() != null) {
                bootstrap.group().shutdownGracefully();
            }
            bootstrap = null;
        }
        final long duration = (System.nanoTime() - start) / 1_000_000;
        LOG.info("Successful shutdown (took {} ms).", duration);
    }

    private void initNioBootstrap() {
        // Add the server port number to the name in order to distinguish
        // multiple servers running on the same host.
        String name = NettyConfig.SERVER_THREAD_GROUP_NAME + " (" + config.getServerPort() + ")";

        NioEventLoopGroup nioGroup = new NioEventLoopGroup(config.getServerNumThreads(), getNamedThreadFactory(name));
        bootstrap.group(nioGroup).channel(NioServerSocketChannel.class);
    }

    private void initEpollBootstrap() {
        // Add the server port number to the name in order to distinguish
        // multiple servers running on the same host.
        String name = NettyConfig.SERVER_THREAD_GROUP_NAME + " (" + config.getServerPort() + ")";

        EpollEventLoopGroup epollGroup = new EpollEventLoopGroup(config.getServerNumThreads(), getNamedThreadFactory(name));
        bootstrap.group(epollGroup).channel(EpollServerSocketChannel.class);
    }

    public static ThreadFactory getNamedThreadFactory(String name) {
        return THREAD_FACTORY_BUILDER.setNameFormat(name + " Thread %d").build();
    }
}
