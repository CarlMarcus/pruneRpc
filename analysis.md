### 设一个rpc需要的元素：

1. 网络通信：一般用netty
2. 序列化和反序列化：https://github.com/eishay/jvm-serializers/wiki
3. 客户端要能得知服务端是否提供它想要的服务
4. Call调用和服务实例对应

### **客户端行为：**

1. 客户端启动client包下的HelloClient，通过ClassPathXmlApplicationContext加载xml中的配置；
2. ClassPathXmlApplicationContext启动调用父类AbstractXmlApplicationContext方法refresh，其中过程具体为：

> - 准备刷新的上下文环境，例如对系统属性或者环境变量进行准备及验证
> - 初始化BeanFactory，并进行XML文件读取，这一步之后，ClassPathXmlApplicationContext实际上就已经包含了BeanFactory所提供的功能，也就是可以进行Bean的提取等基础操作了
> - 对BeanFactory进行各种功能填充
> - 子类覆盖方法做额外的处理,提供了一个空的函数实现postProcessBeanFactory来方便程序员在业务上做进一步扩展
> - 激活各种BeanFactory处理器
> - 注册拦截Bean创建的Bean处理器,这里只是注册，真正的调用是在getBean时候
> - 为上下文初始化Message源，即不同语言的消息体进行国际化处理
> - 初始化应用消息广播器，并放入“applicationEventMulticaster”bean中
> - 留给子类来初始化其它的Bean
> - 在所有注册的bean中查找Listener bean，注册到消息广播器中
> - 初始化剩下的单实例（非惰性的）
> - 完成刷新过程，通知生命周期处理器lifecycleProcessor刷新过程，同时发出ContextRefreshEvent通知别人

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

### **服务端行为：**

1. 启动RpcBootStrap创建并运行客户端，由ClassPathXmlApplicationContext从spring.xml配置文件注入实例bean对象并初始化
2. 其中就初始化了RpcServer对象，并注入了从属性文件中读到的rpc服务ip+port(8000)，以及ZK服务注册对象serviceRegistry，其实现类是ZooKeeperServiceRegistry
3. 跳去看RpcServer，实现了ApplicationContextAware和InitializingBean
4. 其中创建了存放 服务名 与 服务对象 之间的映射关系的handlerMap
5. spring.xml配置文件中的context:component-scan base-package=语句声明了将被spring扫描的包，rpcserver的setApplicationContext方法中，扫描了此包下所有被@RpcService注解的类，其便是api包中接口的实现类，也即我们所提供的服务，存在serviceBeanMap中，并转换成带版本号的服务名和服务类实例，存在handlerMap中。
6. 在spring依赖注入实例初始化之后跟着立马创建了一个NIO服务端(*)
7. 如果zk服务注册地址serviceRegistry不为空，赶紧把handlerMap中扫描到的服务名依次注册到zk中。register(interfaceName, serviceAddress)行为：

> - 根据zkAddress ip+port(2181)，创建ZooKeeper客户端
> - 创建/registry 持久化节点，再创建/registry/${接口+版本号} 持久化节点，然后创建一个临时顺序节点，类似于 /registry/com.github.PruneRpc.api.HelloService/address-0000000001，完成注册
> - 有这么个服务名，说明handlerMap中有这么一个服务类实例，说明已经实现了api包中的接口并实例化了，客户端向ip+port(8000)发送请求过来，只要正确解码请求就能找到这个服务类实例

8. 把解码RpcRequest、编码RpcResponse和用RpcServerHandler类处理handlerMap的过程都加入其中，绑定ip+port(8000)，并监听等待连接过来
9. RpcServerHandler中，创建RpcResponse，id设置成和request一样的，调用handle(request)获取值，存入response，写入RPC响应对象并自动关闭连接
10. handle(request)细节：

> - 通过request对象获取服务接口名与版本号拼接在一起，从handlerMap中获取对应的服务bean实例
> - 获取服务bean实例的类型、方法名、参数类型和参数，对其进行反射调用

### 细节问题：

使用了什么协议？

> 自己定义的request和response对象，封装id、接口名、版本号、方法名、参数类型、参数对象

为什么用Protostuff实现序列化？

> java中实现的序列化效率是极低的，在小项目中使用还尚可，对序列化速度要求比较高的项目是将会成为瓶颈问题。
>
> protobuf序列化后的大小是json的10分之一，xml格式的20分之一，是二进制序列化的10分之一，速度够快，相对来说出现TCP拆包和丢失的可能性就小多了，数据更安全。但是需要自己写一个.proto文件用来描述序列化的格式，然后用protobuf提供的protoc工具将.proto文件编译成一个Java文件。
>
> protostuff则是基于 Protobuf 序列化框架，面向 POJO，无需编写 .proto 文件，而且api也简单。

为什么要用Netty？

> 基于TCP协议建立的连接，相对来说有TCP本身的数据安全保障机制，且非阻塞的IO、灵活的IO线程池设计，使得很容易搭建高吞吐量的消息服务端。

如何使用Netty进行RPC服务器的开发呢？实际不难，下面我就简单的说明一下技术原理：

> 1. 定义RPC请求消息、应答消息结构，里面要包括RPC的接口定义模块、包括远程调用的类名、方法名称、参数结构、参数值等信息。
>
> 2. 服务端初始化的时候通过IoC容器加载RPC接口定义和RPC接口实现类对象的映射关系，然后等待客户端发起调用请求。
>
> 3. 客户端发起的RPC消息里面包含，远程调用的类名、方法名称、参数类型、参数值等信息，通过网络，以字节流的方式送给RPC服务端，RPC服务端接收到字节流的请求之后，去对应的容器里面，查找客户端接口映射的具体实现对象。
>
> 4. RPC服务端找到实现对象的参数信息，通过反射机制创建该对象的实例，并返回调用处理结果，最后封装成RPC应答消息通知到客户端。
>
> 5. 客户端通过网络，收到字节流形式的RPC应答消息，进行拆包、解析之后，显示远程调用结果。

如何应对拆包粘包？

> TLV形式的消息包，前面四个字节是长度，如果整个消息包长度小于四个字节，说明无效包，丢掉不管，如果可读取长度小于此前读取的消息长度，说明后面拆包了，丢弃，对于粘包，反正只读这么长的，后面不管。

序列化方式？

> 通过对象的类构建schema(被缓存)，根据schema进行序列化

服务注册如何实现？

> RpcServer在启动时，会把自己扫描@RpcService注解得到的“接口+版本号”和服务端所在的ip+port注册到Z.K.中，以/registry/“接口+版本号”/adresss-00000000x为路径，前两个是持久的，后面adress是顺序临时节点，其数据值是提供这个服务类实现实例的ip+port。这样做是为了让多机服务端集群提供服务时，某台机挂了，它这个/registry/“接口+版本号”路径下的adress-0000000x就没有了，不用再被发现然后提供服务。

服务发现如何实现？

> 客户端生成一个它所请求这个接口的动态代理对象，动态代理对象在封装完RpcRequest请求后会把接口名和版本号拼接在一起作为要找的服务名。RpcProxy已经被ioc容器注入了配置给它的zk注册与发现中心的对象ZooKeeperServiceDiscovery了，马上就能在/registry下查找是否有刚拼接好的服务名。然后获取/registry/“接口+版本号” 下所有子节点的列表，为空表示没服务，如果有1个那就只读到这个节点上的ip+port，也即提供这个服务的服务端地址，如果有多个那就ThreadLocalRandom.current().nextInt随机获取一个。

有负载均衡吗？

> 有，上面发现多个临时address-00000000x节点通过ThreadLocalRandom.current().nextInt随机获取就是一个简单的负载均衡。