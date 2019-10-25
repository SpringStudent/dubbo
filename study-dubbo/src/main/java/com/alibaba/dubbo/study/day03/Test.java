package com.alibaba.dubbo.study.day03;

import com.alibaba.dubbo.common.extension.ExtensionLoader;
import com.alibaba.dubbo.rpc.Protocol;

/**
 * @author 周宁
 * @Date 2019-10-22 13:34
 */
public class Test {

    public static void main(String[] args) {
        Protocol protocol = ExtensionLoader.getExtensionLoader(Protocol.class).getAdaptiveExtension();

        System.out.println(protocol);
    }
}
