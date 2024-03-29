package com.github.PruneRpc.registry;

import org.I0Itec.zkclient.ZkClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 基于 ZooKeeper 的服务注册接口实现
 */
public class ZooKeeperServiceRegistry implements ServiceRegistry {

    private static final Logger LOGGER = LoggerFactory.getLogger(ZooKeeperServiceRegistry.class);

    private final ZkClient zkClient;

    public ZooKeeperServiceRegistry(String zkAddress) {
        // 创建 ZooKeeper 客户端
        // ZK_SESSION_TIMEOUT：5000
        // ZK_CONNECTION_TIMEOUT 1000
        zkClient = new ZkClient(zkAddress, Constant.ZK_SESSION_TIMEOUT, Constant.ZK_CONNECTION_TIMEOUT);
        LOGGER.debug("Service registry (Z.K. Implementation) created.");
    }

    @Override
    public void register(String serviceName, String serviceAddress) {
        // 创建 registry 节点（持久）
        String registryPath = Constant.ZK_REGISTRY_PATH; // /registry
        if (!zkClient.exists(registryPath)) {
            zkClient.createPersistent(registryPath);
            LOGGER.debug("create registry node: {}", registryPath);
        }
        // 创建 service 节点（持久）
        String servicePath = registryPath + "/" + serviceName; // /registry/com.xxx.rpc.demo.api.HelloService
        if (!zkClient.exists(servicePath)) {
            zkClient.createPersistent(servicePath);
            LOGGER.debug("create service node: {}", servicePath);
        }
        // 创建 address 节点（顺序临时）
        String addressPath = servicePath + "/address-";
        String addressNode = zkClient.createEphemeralSequential(addressPath, serviceAddress);
        LOGGER.debug("create address node: {}", addressNode);
        /*
         *  类似于 /registry/com.github.PruneRpc.api.HelloService/address-0000000001
         */
    }
}