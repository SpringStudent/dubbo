package com.alibaba.dubbo.study.day01.xml.service;

import com.alibaba.dubbo.study.day03.callback.CallbackListener;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author 周宁
 * @Date 2019-10-12 13:37
 */
public class EchoServiceImpl implements EchoService {

    private final Map<String, CallbackListener> listeners = new ConcurrentHashMap<String, CallbackListener>();

    public EchoServiceImpl() {
        Thread t = new Thread(() -> {
            while (true) {
                try {
                    for (Map.Entry<String, CallbackListener> entry : listeners.entrySet()) {
                        try {
                            entry.getValue().changed(getChanged(entry.getKey()));
                        } catch (Throwable t1) {
                            listeners.remove(entry.getKey());
                        }
                    }
                    Thread.sleep(5000); // 定时触发变更通知
                } catch (Throwable t1) { // 防御容错
                    t1.printStackTrace();
                }
            }
        });
        t.setDaemon(true);
        t.start();
    }

    @Override
    public String echo(String message) {
        return "[" + new SimpleDateFormat("HH:mm:ss").format(new Date()) + "] recive:" + message;
    }

    @Override
    public void addListener(String key, CallbackListener listener) {
        listeners.put(key, listener);
        listener.changed("好吧"); // 发送变更通知
    }

    private String getChanged(String key) {
        return key+": " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
    }

}
