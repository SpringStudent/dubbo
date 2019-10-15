package com.alibaba.dubbo.study.day02.javaspi;

/**
 * @author 周宁
 * @Date 2019-10-14 16:31
 */
public class PrintHelloWorldServiceImpl implements PrintService{
    @Override
    public void printInfo() {
        System.out.println("hello world");
    }
}
