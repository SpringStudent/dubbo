package com.alibaba.dubbo.study.day01.api.service;


import com.alibaba.dubbo.config.annotation.Service;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author 周宁
 * @Date 2019-10-12 13:37
 */
@Service
public class EchoServiceImpl implements EchoService {

    @Override
    public String echo(String message) {
        return "[" + new SimpleDateFormat("HH:mm:ss").format(new Date()) + "] recive:" + message;
    }
}
