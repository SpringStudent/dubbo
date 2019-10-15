package com.alibaba.dubbo.study.day02.javaspi;

import java.util.ServiceLoader;

/**
 * @author 周宁
 * @Date 2019-10-14 16:34
 */
public class SPITest {

    public static void main(String[] args) {
        ServiceLoader<PrintService> services = ServiceLoader.load(PrintService.class);
        for(PrintService ps : services){
            ps.printInfo();
        }
    }
}
