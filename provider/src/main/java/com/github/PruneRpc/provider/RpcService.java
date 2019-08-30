package com.github.PruneRpc.provider;

import org.springframework.stereotype.Component;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * RPC 服务注解（标注在服务实现类上）
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Component //可被spring扫描，把普通pojo实例化到spring容器中，相当于配置文件中的<bean id="" class=""/>
public @interface RpcService {

    /**
     * 服务接口类
     */
    Class<?> value();

    /**
     * 服务版本号
     */
    String version() default "";
}
