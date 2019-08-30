**客户端行为：**

1. 客户端启动client包下的HelloClient，通过ClassPathXmlApplicationContext加载xml中的配置；
2. ClassPathXmlApplicationContext启动调用父类AbstractXmlApplicationContext方法refresh，进行初始化

3. 获取RpcProxy.class实例bean，并且已经注入了ServiceDiscovery对象ZooKeeperServiceDiscovery
4. rpcProxy调用create方法，以传入的接口类型为蓝本创建动态代理对象
5. 动态代理对象里创建RpcRequest，分配UUID、传入接口名、版本号（同接口的不同实现通过版本号进行区分，其具体实现在对应实现类的RpcService注解上加入了版本号标识）、方法名、参数类型和参数
6. 如果服务发现对象ServiceDiscovery不为空，说明Zk中注册了该服务，然后ServiceDiscovery调用discover()方法返回这个这个接口名+版本号对应的实现类在zk中的路径，然后转换成所在的主机和端口。discover的流程如下：

> - 创建zkClient客户端，zk端口号为2181，从客户端xml配置文件注入的
> - 从/registry/${serviceName}下获取所有子节点，随机获取一个作为服务路径，返回，没有就抛异常

7. 客户端RpcClient根据服务发现获得的rpc服务所在的主机和端口(8000)，创建连接该ip+port的RpcClient，发送RpcRequest，获得RpcResponse
8. RpcClient是一个NIO客户端(*)，处理过程中包含着Encoding RpcRequest和Decoding RpcResponse
9. 客户端对结果进行方法调用，完成远程过程请求

> 客户端行为里，与服务端耦合出在于RpcClient的行为 以及 与ZK服务发现的交互，ZK服务发现于ZK服务注册紧密相关，ZK服务注册与服务端直接关联。
>

**服务端行为：**

1. 启动RpcBootStrap创建并运行客户端，由ClassPathXmlApplicationContext从spring.xml配置文件注入实例bean对象并初始化
2. 其中就初始化了RpcServer对象，并注入了从属性文件中读到的rpc服务ip+port(8000)，以及ZK服务注册对象serviceRegistry，其实现类是ZooKeeperServiceRegistry
3. 跳去看RpcServer，实现了ApplicationContextAware和InitializingBean
4. 其中创建了存放 服务名 与 服务对象 之间的映射关系的handlerMap
5. spring.xml配置文件中的context:component-scan base-package=语句声明了将被spring扫描的包，rpcserver的setApplicationContext方法中，扫描了此包下所有被@RpcService注解的类，其便是api包中接口的实现类，也即我们所提供的服务，存在serviceBeanMap中，并转换成带版本号的服务名和服务类实例，存在handlerMap中。
6. 在spring依赖注入实例初始化之后跟着立马创建了一个NIO服务端(*)
7. 如果zk服务注册地址serviceRegistry不为空，赶紧把handlerMap中扫描到的服务名依次注册到zk中。register(interfaceName, serviceAddress)行为：

> - 根据zkAddress ip+port(2181)，创建ZooKeeper客户端
> - 创建/registry 持久化节点，再创建/registry/${serviceName}持久化节点，然后创建一个临时顺序节点，类似于 /registry/com.github.PruneRpc.api.HelloService/address-0000000001，完成注册
> - 有这么个服务名，说明handlerMap中有这么一个服务类实例，说明已经实现了api包中的接口并实例化了，客户端向ip+port(8000)发送请求过来，只要正确解码请求就能找到这个服务类实例

8. 把解码RpcRequest、编码RpcResponse和用RpcServerHandler类处理handlerMap的过程都加入其中，绑定ip+port(8000)，并监听等待连接过来
9. RpcServerHandler中，创建RpcResponse，id设置成和request一样的，调用handle(request)获取值，存入response，写入RPC响应对象并自动关闭连接
10. handle(request)细节：

> - 通过request对象获取服务接口名与版本号拼接在一起，从handlerMap中获取对应的服务bean实例
> - 获取服务bean实例的类型、方法名、参数类型和参数，对其进行反射调用