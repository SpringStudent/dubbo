package com.alibaba.dubbo.study.day01.xml;

import com.alibaba.dubbo.study.day01.xml.service.EchoService;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * @author 周宁
 * @Date 2019-10-12 13:36
 */
public class EchoConsumer {

    public static void main(String[] args) {
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("echo-consumer.xml");

        EchoService echoService = context.getBean(EchoService.class);

        System.out.println(echoService.echo("降温了各位"));

    }
}
