package com.github.PruneRpc.client;

import com.github.PruneRpc.consumer.RpcProxy;
import com.github.PruneRpc.api.HelloService;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class HelloClient3 {

    public static void main(String[] args) throws Exception {
        ApplicationContext context = new ClassPathXmlApplicationContext("spring.xml");
        RpcProxy rpcProxy = context.getBean(RpcProxy.class);

        int loopCount = 10000;

        long start = System.currentTimeMillis();

        HelloService helloService = rpcProxy.create(HelloService.class);
        for (int i = 0; i < loopCount; i++) {
            String result = helloService.hello("World");
            System.out.println(result);
        }

        long time = System.currentTimeMillis() - start;
        System.out.println("Loop counts: " + loopCount);
        System.out.println("Time: " + time + "ms");
        System.out.println("TPS: " + (double) loopCount / ((double) time / 1000));

        System.exit(0);
    }
}
