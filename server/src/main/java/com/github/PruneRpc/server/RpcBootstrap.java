package com.github.PruneRpc.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class RpcBootstrap {

    private static final Logger LOGGER = LoggerFactory.getLogger(RpcBootstrap.class);

    public static void main(String[] args) {
        LOGGER.debug("start server");
        new ClassPathXmlApplicationContext("spring.xml");
        /*
         * ClassPathXmlApplicationContext是一种IOC容器，从XML文件中加载已被定义的bean并创建和初始化bean对象
         */
    }
}
