package com.alibaba.dubbo.study.day02.adaptive;

import com.alibaba.dubbo.common.URL;

/**
 * @author 周宁
 * @Date 2019-10-14 20:53
 */
public class SecondExtImpl implements SimpleExt{
    @Override
    public String simple(URL url, String s) {
        return "second"+s;
    }
}
