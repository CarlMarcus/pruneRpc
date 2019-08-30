package com.github.PruneRpc.server;

import com.github.PruneRpc.api.HelloService;
import com.github.PruneRpc.api.Person;
import com.github.PruneRpc.provider.RpcService;

@RpcService(value = HelloService.class, version = "server.hello2")
public class HelloServiceImpl2 implements HelloService {

    @Override
    public String hello(String name) {
        return "你好! " + name;
    }

    @Override
    public String hello(Person person) {
        return "你好! " + person.getFirstName() + " " + person.getLastName();
    }
}
