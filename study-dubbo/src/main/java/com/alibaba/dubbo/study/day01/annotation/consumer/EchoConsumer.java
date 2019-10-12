package com.alibaba.dubbo.study.day01.annotation.consumer;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.study.day01.annotation.service.EchoService;
import org.springframework.stereotype.Component;

/**
 * @author 周宁
 * @Date 2019-10-12 13:57
 */
@Component
public class EchoConsumer {

    @Reference(interfaceClass=EchoService.class)
    private EchoService echoService;

    public String getMessage(String name){
        return echoService.echo(name);
    }
}
