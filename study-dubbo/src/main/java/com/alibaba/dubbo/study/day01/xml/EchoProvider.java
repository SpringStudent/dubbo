package com.alibaba.dubbo.study.day01.xml;

import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.io.IOException;

/**
 * @author 周宁
 * @Date 2019-10-12 13:36
 */
public class EchoProvider {

    public static void main(String[] args) throws IOException {
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("echo-provider.xml");
        context.start();
        System.in.read();
        //可以通过如下步骤在windows的dos窗口进行调试
        //1.telnet localhost 20880
        //2.invoke com.alibaba.dubbo.study.day01.xml.service.EchoService.echo("123")
    }
}
