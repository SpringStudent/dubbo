package com.alibaba.dubbo.study.day02.dubbospi;

import com.alibaba.dubbo.common.extension.ExtensionLoader;

/**
 * @author 周宁
 * @Date 2019-10-14 16:40
 */
public class SPITest {

    public static void main(String[] args) {
        HelloService helloService = ExtensionLoader.getExtensionLoader(HelloService.class)
                .getExtension("impl");
        helloService.say();
    }
}
