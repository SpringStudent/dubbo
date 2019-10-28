package com.alibaba.dubbo.study.day01.xml;

import com.alibaba.dubbo.study.day01.xml.service.EchoService;
import com.alibaba.dubbo.study.day03.callback.CallbackListener;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.io.IOException;

/**
 * @author 周宁
 * @Date 2019-10-12 13:36
 */
public class EchoConsumer {

    public static void main(String[] args) throws IOException {
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("echo-consumer.xml");

        EchoService echoService = context.getBean(EchoService.class);

        System.out.println(echoService.echo("降温了各位"));

        echoService.addListener("foo.bar", msg -> System.out.println("changed:"+msg));
        System.in.read();
    }
}
