package com.alibaba.dubbo.study.day04;

/**
 * @author 周宁
 * @Date 2019-11-22 14:42
 */
public class MyStubServiceStub implements MyStubService{

    private final MyStubService myStubService;

    public MyStubServiceStub(MyStubService myStubService){
        this.myStubService = myStubService;
    }

    @Override
    public String stubTest() {
        System.out.println("i can do anything before remote mystubService");
        try{
            System.out.println(myStubService.stubTest());
        }catch (Exception e){
            System.out.println("i can do anything while remote mystubService throw exception");
        }
        return null;
    }
}
