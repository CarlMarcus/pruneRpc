package com.github.PruneRpc.server;

import com.github.PruneRpc.api.AddTwoNumber;
import com.github.PruneRpc.provider.RpcService;

@RpcService(AddTwoNumber.class)
public class AddTwoNumberImpl implements AddTwoNumber {
    @Override
    public int add(int num1, int num2) {
        return num1+num2;
    }
}
