package com.github.PruneRpc.client;

import com.github.PruneRpc.consumer.RpcProxy;
import com.github.PruneRpc.api.HelloService;
import com.github.PruneRpc.api.Person;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

// 依赖 api和consumer 包
public class HelloClient2 {

    public static void main(String[] args) throws Exception {
        ApplicationContext context = new ClassPathXmlApplicationContext("spring.xml");
        RpcProxy rpcProxy = context.getBean(RpcProxy.class);

        HelloService helloService = rpcProxy.create(HelloService.class);
        for (int i=0; i<10000; i++) {
            String result = helloService.hello(new Person("Carl", "Marcus"));
            System.out.println(result);
        }

        System.exit(0);
    }
}
