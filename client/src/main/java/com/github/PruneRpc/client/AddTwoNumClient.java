package com.github.PruneRpc.client;

import com.github.PruneRpc.api.AddTwoNumber;
import com.github.PruneRpc.consumer.RpcProxy;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class AddTwoNumClient {
    public static void main(String[] args) {
        ApplicationContext context = new ClassPathXmlApplicationContext("spring.xml");
        RpcProxy rpcProxy = context.getBean(RpcProxy.class);

        AddTwoNumber addTwoNumber = rpcProxy.create(AddTwoNumber.class);
        int res = addTwoNumber.add(2,4);
        System.out.println("add result: 2 + 4 = "+res);
    }
}
