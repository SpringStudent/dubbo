package com.alibaba.dubbo.study.day02.adaptive;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.extension.ExtensionLoader;

/**
 * @author 周宁
 * @Date 2019-10-14 20:43
 */
public class AdaptvieTest {

    public static void main(String[] args) {
        SimpleExt simpleExt = ExtensionLoader.getExtensionLoader(SimpleExt.class).getAdaptiveExtension();
        System.out.println(simpleExt.simple(URL.valueOf("dubbo://127.0.0.1:9092" + "?key=sec"), "asda"));
    }
}
