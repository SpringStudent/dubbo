package com.alibaba.dubbo.study.day02.dubbospi;

/**
 * @author 周宁
 * @Date 2019-10-14 16:39
 */
public class HelloServiceImpl implements HelloService{
    @Override
    public void say() {
        System.out.println("i am okay");
    }
}
