package com.alibaba.dubbo.study.day04;

/**
 * @author 周宁
 * @Date 2019-11-22 14:40
 */
public class MyStubServiceImpl implements MyStubService{

    @Override
    public String stubTest() {
        return "stub";
    }
}
