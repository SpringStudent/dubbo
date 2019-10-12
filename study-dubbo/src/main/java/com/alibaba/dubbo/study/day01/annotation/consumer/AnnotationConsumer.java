package com.alibaba.dubbo.study.day01.annotation.consumer;

import com.alibaba.dubbo.config.ApplicationConfig;
import com.alibaba.dubbo.config.ConsumerConfig;
import com.alibaba.dubbo.config.RegistryConfig;
import com.alibaba.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * @author 周宁
 * @Date 2019-10-12 13:58
 */
public class AnnotationConsumer {

    public static void main(String[] args) {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(ConsumerConfiguration.class);

        EchoConsumer echoConsumer = context.getBean(EchoConsumer.class);

        System.out.println(echoConsumer.getMessage("干!"));
    }


    @Configuration
    //扫包路径必须不能包含provider的类不然报错
    @EnableDubbo(scanBasePackages = "com.alibaba.dubbo.study.day01.annotation.consumer")
    @ComponentScan(value = {"com.alibaba.dubbo.study.day01.annotation.consumer"})
    static class ConsumerConfiguration{
        @Bean
        public ConsumerConfig consumerConfig(){
            return new ConsumerConfig();
        }

        @Bean
        public ApplicationConfig applicationConfig(){
            ApplicationConfig applicationConfig = new ApplicationConfig();
            applicationConfig.setName("echo-annotation-consumer");
            return applicationConfig;
        }

        @Bean
        public RegistryConfig registryConfig(){
            RegistryConfig registryConfig = new RegistryConfig();
            registryConfig.setAddress("zookeeper://127.0.0.1:2181");
            return registryConfig;
        }
    }
}
