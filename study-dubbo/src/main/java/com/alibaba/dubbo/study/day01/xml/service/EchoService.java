package com.alibaba.dubbo.study.day01.xml.service;


import com.alibaba.dubbo.study.day03.callback.CallbackListener;

/**
 * @author 周宁
 * @Date 2019-10-12 13:36
 */
public interface EchoService {

    String echo(String message);

    void addListener(String key, CallbackListener listener);
}
