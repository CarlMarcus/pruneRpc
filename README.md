# PruneRpc

自己实现的简单版RPC框架。

使用：

> 1. 提供服务需在api包下定义服务接口，在server包下实现相应的服务接口
> 2. 同一接口可有多种实现，需在用@RpcService注解标注时指明不同版本号

局域网内分布式调用10000次，TPS为15.686。

单机伪分布式调用10000次，TPS为69.386。

序列化工具选型参考：https://github.com/eishay/jvm-serializers/wiki