package com.sunny.flink.client;

import com.sunny.flink.io.NettyBufferPool;
import com.sunny.flink.io.NettyConfig;
import com.sunny.flink.io.NettyProtocol;
import com.sunny.flink.server.NettyServer;
import com.sunny.flink.ssl.SSLHandlerFactory;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelException;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.ssl.SslHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;

import static com.sunny.util.Preconditions.checkState;

public class NettyClient {
    private static final Logger LOG = LoggerFactory.getLogger(NettyClient.class);

    private final NettyConfig config;

    private NettyProtocol protocol;

    private Bootstrap bootstrap;
    private SSLHandlerFactory clientSSLFactory;



    NettyClient(NettyConfig config) {
        this.config = config;
    }

    void init(final NettyProtocol protocol, NettyBufferPool nettyBufferPool) throws IOException {
        checkState(bootstrap == null, "Netty client has already been initialized.");

        this.protocol = protocol;

        final long start = System.nanoTime();

        bootstrap = new Bootstrap();

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

        bootstrap.option(ChannelOption.TCP_NODELAY, true);
        bootstrap.option(ChannelOption.SO_KEEPALIVE, true);

        // Timeout for new connections
        bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, config.getClientConnectTimeoutSeconds() * 1000);

        // Pooled allocator for Netty's ByteBuf instances
        bootstrap.option(ChannelOption.ALLOCATOR, nettyBufferPool);

        // Receive and send buffer size
        int receiveAndSendBufferSize = config.getSendAndReceiveBufferSize();
        if (receiveAndSendBufferSize > 0) {
            bootstrap.option(ChannelOption.SO_SNDBUF, receiveAndSendBufferSize);
            bootstrap.option(ChannelOption.SO_RCVBUF, receiveAndSendBufferSize);
        }

        try {
            clientSSLFactory = config.createClientSSLEngineFactory();
        } catch (Exception e) {
            throw new IOException("Failed to initialize SSL Context for the Netty client", e);
        }

        final long duration = (System.nanoTime() - start) / 1_000_000;
        LOG.info("Successful initialization (took {} ms).", duration);
    }

    NettyConfig getConfig() {
        return config;
    }

    Bootstrap getBootstrap() {
        return bootstrap;
    }

    void shutdown() {
        final long start = System.nanoTime();

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
        // multiple clients running on the same host.
        String name = NettyConfig.CLIENT_THREAD_GROUP_NAME + " (" + config.getServerPort() + ")";

        NioEventLoopGroup nioGroup = new NioEventLoopGroup(config.getClientNumThreads(), NettyServer.getNamedThreadFactory(name));
        bootstrap.group(nioGroup).channel(NioSocketChannel.class);
    }

    private void initEpollBootstrap() {
        // Add the server port number to the name in order to distinguish
        // multiple clients running on the same host.
        String name = NettyConfig.CLIENT_THREAD_GROUP_NAME + " (" + config.getServerPort() + ")";

        EpollEventLoopGroup epollGroup = new EpollEventLoopGroup(config.getClientNumThreads(), NettyServer.getNamedThreadFactory(name));
        bootstrap.group(epollGroup).channel(EpollSocketChannel.class);
    }

    // ------------------------------------------------------------------------
    // Client connections
    // ------------------------------------------------------------------------

    ChannelFuture connect(final InetSocketAddress serverSocketAddress) {
        checkState(bootstrap != null, "Client has not been initialized yet.");

        // --------------------------------------------------------------------
        // Child channel pipeline for accepted connections
        // --------------------------------------------------------------------

        bootstrap.handler(new ChannelInitializer<SocketChannel>() {
            @Override
            public void initChannel(SocketChannel channel) throws Exception {

                // SSL handler should be added first in the pipeline
                if (clientSSLFactory != null) {
                    SslHandler sslHandler = clientSSLFactory.createNettySSLHandler(
                            serverSocketAddress.getAddress().getCanonicalHostName(),
                            serverSocketAddress.getPort());
                    channel.pipeline().addLast("ssl", sslHandler);
                }
                channel.pipeline().addLast(protocol.getClientChannelHandlers());
            }
        });

        try {
            return bootstrap.connect(serverSocketAddress);
        }
        catch (ChannelException e) {
            if ((e.getCause() instanceof java.net.SocketException &&
                    e.getCause().getMessage().equals("Too many open files")) ||
                    (e.getCause() instanceof ChannelException &&
                            e.getCause().getCause() instanceof java.net.SocketException &&
                            e.getCause().getCause().getMessage().equals("Too many open files")))
            {
                throw new ChannelException(
                        "The operating system does not offer enough file handles to open the network connection. " +
                                "Please increase the number of available file handles.", e.getCause());
            }
            else {
                throw e;
            }
        }
    }
}
