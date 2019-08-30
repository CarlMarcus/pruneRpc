package com.github.PruneRpc.server;

import com.github.PruneRpc.api.HelloService;
import com.github.PruneRpc.api.Person;
import com.github.PruneRpc.provider.RpcService;

/**
 * 使用RpcService指定远程接口
 */
@RpcService(HelloService.class)
/*
 * 使用RpcService指定远程接口是哪个，因为实现类可能实现很多接口，必须要为框架指明远程接口是哪个
 */
public class HelloServiceImpl implements HelloService {

    @Override
    public String hello(String name) {
        return "Hello! " + name;
    }

    @Override
    public String hello(Person person) {
        return "Hello! " + person.getFirstName() + " " + person.getLastName();
    }
}
