package com.alibaba.dubbo.study.day01.api;

import com.alibaba.dubbo.config.ApplicationConfig;
import com.alibaba.dubbo.config.ReferenceConfig;
import com.alibaba.dubbo.config.RegistryConfig;
import com.alibaba.dubbo.study.day01.api.service.EchoService;

/**
 * @author 周宁
 * @Date 2019-10-12 14:18
 */
public class EchoConsumer {

    public static void main(String[] args) {
        ReferenceConfig<EchoService> reference = new ReferenceConfig<>();
        reference.setApplication(new ApplicationConfig("java-echo-consumer"));
        reference.setRegistry(new RegistryConfig("zookeeper://127.0.0.1:2181"));
        reference.setInterface(EchoService.class);
        EchoService echoService = reference.get();
        System.out.println(echoService.echo("好吧"));
    }
}
