package com.alibaba.dubbo.study.day03.eventnotify;

/**
 * @author 周宁
 * @Date 2019-10-25 17:26
 */
public class OnReturnCallbackImpl implements OnReturnCallback {


    @Override
    public void onreturn(String echo, String message) {
        System.out.println("onreturn:" + echo + message);
    }
}
