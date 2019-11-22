package com.alibaba.dubbo.study.day04;

import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.io.IOException;

/**
 * @author 周宁
 * @Date 2019-10-12 13:36
 */
public class StubConsumer {

    public static void main(String[] args) throws IOException {
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("stub-consumer.xml");

        MyStubService myStubService = context.getBean(MyStubService.class);

        myStubService.stubTest();

        System.in.read();
    }
}
