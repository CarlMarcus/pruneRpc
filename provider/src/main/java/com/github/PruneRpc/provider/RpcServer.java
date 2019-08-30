package com.github.PruneRpc.provider;

import com.github.PruneRpc.common.protocol.RpcRequest;
import com.github.PruneRpc.common.protocol.RpcResponse;
import com.github.PruneRpc.common.codec.RpcDecoder;
import com.github.PruneRpc.common.codec.RpcEncoder;
import com.github.PruneRpc.common.util.StringUtil;
import com.github.PruneRpc.registry.ServiceRegistry;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.apache.commons.collections4.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.util.HashMap;
import java.util.Map;

/**
 * RPC 服务器（用于发布 RPC 服务）
 */
public class RpcServer implements ApplicationContextAware, InitializingBean {
    /* 当一个类实现了接口ApplicationContextAware之后，这个类就可以方便获得ApplicationContext中的所有bean，
     *  就是这个类可以直接获取spring配置文件中，所有有引用到的bean对象。
     *  场景：适用于当前运行的代码和已启动的Spring代码处于同一个Spring上下文，否则获取到的ApplicationContext是空的。
     *
     *  Why not
     *  ApplicationContext appContext = new ClassPathXmlApplicationContext("spring.xml")?
     *  因为它会重新装载applicationContext-common.xml并实例化上下文bean，如果有些线程配置类也是在这个配置文件中，
     *  那么会造成做相同工作的的线程会被启两次。一次是web容器初始化时启动，另一次是上述代码显示的实例化了一次。
     *  当于重新初始化一遍！这样就产生了冗余。
     */

    private static final Logger LOGGER = LoggerFactory.getLogger(RpcServer.class);

    private String serviceAddress;

    private ServiceRegistry serviceRegistry;

    /*
     * 存放 服务名 与 服务对象 之间的映射关系
     */
    private Map<String, Object> handlerMap = new HashMap<>();

    public RpcServer(String serviceAddress) {
        this.serviceAddress = serviceAddress;
    }

    public RpcServer(String serviceAddress, ServiceRegistry serviceRegistry) {
        this.serviceAddress = serviceAddress;
        this.serviceRegistry = serviceRegistry;
    }

    /*
     * 重写ApplicationContextAware接口的方法
     * 扫描带有 RpcService 注解的类并初始化 handlerMap 对象
     */
    @Override
    public void setApplicationContext(ApplicationContext ctx) throws BeansException {
        Map<String, Object> serviceBeanMap = ctx.getBeansWithAnnotation(RpcService.class);
        if (MapUtils.isNotEmpty(serviceBeanMap)) {
            for (Object serviceBean : serviceBeanMap.values()) {
                RpcService rpcService = serviceBean.getClass().getAnnotation(RpcService.class);
                String serviceName = rpcService.value().getName();
                String serviceVersion = rpcService.version();
                if (StringUtil.isNotEmpty(serviceVersion)) {
                    serviceName += "-" + serviceVersion;
                }
                handlerMap.put(serviceName, serviceBean);
            }
        }
    }

    /*
     * 我们的Bean中有某个属性需要注入，但不支持spring注入，只能通过Build或者new的方式创建。
     * 但又想在Spring配置Bean的时候一起将该属性注入进来，则实现InitializingBean接口，实现其afterPropertiesSet方法
     * 通过在这个方法中创建来实现 与其他bean同一环节 注入
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        EventLoopGroup bossGroup = new NioEventLoopGroup(); // BOSS 线程池
        EventLoopGroup workerGroup = new NioEventLoopGroup(); // WORK 线程池
        try {
            // 创建并初始化 服务端的 Bootstrap 对象，这个就是服务启动器
            ServerBootstrap bootstrap = new ServerBootstrap();
            // 指定Netty服务端进程 的Boss线程和work线程
            bootstrap.group(bossGroup, workerGroup);
            // 如果是以下的申明方式，说明BOSS线程和WORK线程共享一个线程池（实际上一般的情况环境下，这种共享线程池的方式已经够了）
            // serverBootstrap.group(workerGroup);

            // 设置服务端的通道类型，只能是实现了ServerChannel接口的“服务器”通道类
            bootstrap.channel(NioServerSocketChannel.class);
            // 用Netty编写网络程序的时候，你很少直接操纵Channel，而是通过ChannelHandler来间接操纵Channel,用于处理Channel对应的事件。
            bootstrap.childHandler(new ChannelInitializer<NioSocketChannel>() {
                @Override
                public void initChannel(NioSocketChannel channel) throws Exception {
                    /*
                    ChannelPipeline实际上应该叫做ChannelHandlerPipeline，可以把ChannelPipeline看成是一个ChandlerHandler的链表，
                    当需要对Channel进行某种处理的时候，Pipeline负责依次调用每一个Handler进行处理。每个Channel都有一个属于自己的
                    Pipeline，调用Channel#pipeline()方法可以获得Channel的Pipeline，调用Pipeline#channel()方法可以获得Pipeline
                    的Channel。
                     */
                    ChannelPipeline pipeline = channel.pipeline();
                    pipeline.addLast(new RpcDecoder(RpcRequest.class)); // 解码 RPC 请求
                    pipeline.addLast(new RpcEncoder(RpcResponse.class)); // 编码 RPC 响应
                    pipeline.addLast(new RpcServerHandler(handlerMap)); // 处理 RPC 请求
                }
            });
            bootstrap.option(ChannelOption.SO_BACKLOG, 1024);
            bootstrap.childOption(ChannelOption.SO_KEEPALIVE, true);
            // 获取 RPC 服务器的 IP 地址与端口号
            String[] addressArray = StringUtil.split(serviceAddress, ":");
            String ip = addressArray[0];
            int port = Integer.parseInt(addressArray[1]);
            //  RPC 服务器 绑定ip和端口 并启动（异步）
            ChannelFuture future = bootstrap.bind(ip, port).sync();
            // 注册 RPC 服务地址
            if (serviceRegistry != null) {
                for (String interfaceName : handlerMap.keySet()) {
                    serviceRegistry.register(interfaceName, serviceAddress);
                    LOGGER.debug("register service: {} => {}", interfaceName, serviceAddress);
                }
            }
            LOGGER.debug("server started on port {}", port);
            // 关闭 RPC 服务器
            future.channel().closeFuture().sync();
        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }
}
