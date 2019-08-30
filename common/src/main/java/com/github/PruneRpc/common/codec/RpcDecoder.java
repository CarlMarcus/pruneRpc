package com.github.PruneRpc.common.codec;

import com.github.PruneRpc.common.util.SerializationUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

/**
 * RPC request 解码器
 */
public class RpcDecoder extends ByteToMessageDecoder {

    private Class<?> genericClass;

    public RpcDecoder(Class<?> genericClass) {
        this.genericClass = genericClass;
    }

    @Override
    public void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        /*
         * ChannelPipeline并不是直接管理ChannelHandler，而是通过ChannelHandlerContext来间接管理，这一点通过
         * ChannelPipeline 的默认实现 DefaultChannelPipeline 可以看出来。
         * ChannelHandlerContext是连接ChannelPipeline和ChannelHander的枢纽， 它可以拦截ChannelPipeline中
         * 传递的事件交给ChannelHander处理， 同时也可以把事件传给相邻的其他ChannelHandlerContext。
         */
        // 发送数据包之前, 先用四个字节占位, 表示数据包的长度.
        if (in.readableBytes() < 4) {
            return;
        }
        /*
         +-------------------+------------------+------------------+
         | discardable bytes |  readable bytes  |  writable bytes  |
         |  (already read)   |     (CONTENT)    |  (haven't read)  |
         +-------------------+------------------+------------------+
         |                   |                  |                  |
         0      <=      readerIndex   <=   writerIndex    <=    capacity
         */
        in.markReaderIndex(); // Marks the current readerIndex in this buffer.
        int dataLength = in.readInt(); // Gets a 32-bit integer at the current readerIndex 代表此消息包内容的长度
        if (in.readableBytes() < dataLength) {
            in.resetReaderIndex(); // 将当前的readerindex重置为此ByteBuf之前标记的readerindex，因为这个是被拆包后的部分包
            return;
        }
        byte[] data = new byte[dataLength];
        in.readBytes(data);
        out.add(SerializationUtil.deserialize(data, genericClass));
    }
}
