package com.alibaba.dubbo.study.day02.dubbospi;

import com.alibaba.dubbo.common.extension.SPI;

/**
 * @author 周宁
 * @Date 2019-10-14 16:38
 */
@SPI("impl")
public interface HelloService {

    void say();
}
