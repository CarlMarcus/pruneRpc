package com.github.PruneRpc.client;

import com.github.PruneRpc.consumer.RpcProxy;
import com.github.PruneRpc.api.HelloService;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class HelloClient {

    public static void main(String[] args) throws Exception {
        ApplicationContext context = new ClassPathXmlApplicationContext("spring.xml");
        RpcProxy rpcProxy = context.getBean(RpcProxy.class);

        HelloService helloService = rpcProxy.create(HelloService.class);

        HelloService helloService2 = rpcProxy.create(HelloService.class, "server.hello2");

        for(int i=0; i<10000; i++) {
            String result = helloService.hello("World");
            System.out.println(result);
            String result2 = helloService2.hello("世界");
            System.out.println(result2);
        }

        System.exit(0);
    }
}
