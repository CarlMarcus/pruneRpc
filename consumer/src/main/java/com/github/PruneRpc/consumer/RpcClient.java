package com.github.PruneRpc.consumer;

import com.github.PruneRpc.common.protocol.RpcRequest;
import com.github.PruneRpc.common.protocol.RpcResponse;
import com.github.PruneRpc.common.codec.RpcDecoder;
import com.github.PruneRpc.common.codec.RpcEncoder;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * RPC 客户端（用于发送 RPC 请求）
 */
public class RpcClient extends SimpleChannelInboundHandler<RpcResponse> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RpcClient.class);

    private final String host;
    private final int port;

    private RpcResponse response;

    public RpcClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, RpcResponse response) throws Exception {
        this.response = response;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        LOGGER.error("API caught exception", cause);
        ctx.close();
    }

    public RpcResponse send(RpcRequest request) throws Exception {
        EventLoopGroup group = new NioEventLoopGroup(); // 一个死循环，不停地检测IO事件，处理IO事件，执行任务
        try {
            // 创建并初始化 客户端 Bootstrap 对象，这就是客户端启动器
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(group);
            bootstrap.channel(NioSocketChannel.class); // 表示客户端启动的是nio相关的channel
            bootstrap.handler(new ChannelInitializer<SocketChannel>() { // 表示客户端启动过程中，需要经过哪些流程
                @Override
                public void initChannel(SocketChannel channel) throws Exception {
                    ChannelPipeline pipeline = channel.pipeline();
                    pipeline.addLast(new RpcEncoder(RpcRequest.class)); // 编码 RPC 请求
                    pipeline.addLast(new RpcDecoder(RpcResponse.class)); // 解码 RPC 响应
                    pipeline.addLast(RpcClient.this); // 处理 RPC 响应
                }
            });
            bootstrap.option(ChannelOption.TCP_NODELAY, true);
            // 连接 RPC 服务器
            // 这里就是真正的启动过程了，绑定ip+端口，同步等待连接指服务器完毕，才会进入下行代码
            ChannelFuture future = bootstrap.connect(host, port).sync();
            // 写入 RPC 请求数据并关闭连接
            Channel channel = future.channel();
            channel.writeAndFlush(request).sync();
            channel.closeFuture().sync();  // 等待关闭socket
            // 返回 RPC 响应对象
            return response;
        } finally {
            group.shutdownGracefully();
        }
    }
}
