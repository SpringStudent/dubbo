package com.alibaba.dubbo.study.day02.adaptive;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.extension.Adaptive;
import com.alibaba.dubbo.common.extension.SPI;

/**
 * @author 周宁
 * @Date 2019-10-14 20:42
 */
@SPI("ext")
public interface SimpleExt {

    @Adaptive("key")
    String simple(URL url, String s);
}
