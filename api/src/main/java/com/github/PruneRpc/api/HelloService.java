package com.github.PruneRpc.api;

public interface HelloService {

    String hello(String name);

    String hello(Person person);
}
