/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.dubbo.rpc.proxy;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.utils.ReflectUtils;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.ProxyFactory;
import com.alibaba.dubbo.rpc.RpcException;
import com.alibaba.dubbo.rpc.service.EchoService;
import com.alibaba.dubbo.rpc.service.GenericService;

/**
 * AbstractProxyFactory
 */
public abstract class AbstractProxyFactory implements ProxyFactory {

    @Override
    public <T> T getProxy(Invoker<T> invoker) throws RpcException {
        return getProxy(invoker, false);
    }

    @Override
    public <T> T getProxy(Invoker<T> invoker, boolean generic) throws RpcException {
        //接口数组
        Class<?>[] interfaces = null;
        //获取接口列表
        String config = invoker.getUrl().getParameter("interfaces");
        if (config != null && config.length() > 0) {
            //使用,切分接口列表
            String[] types = Constants.COMMA_SPLIT_PATTERN.split(config);
            if (types != null && types.length > 0) {
                //设置服务接口类和 EchoService.class 到 interfaces 中
                interfaces = new Class<?>[types.length + 2];
                interfaces[0] = invoker.getInterface();
                interfaces[1] = EchoService.class;
                //加载接口类放到interfaces[]数组中
                for (int i = 0; i < types.length; i++) {
                    interfaces[i + 1] = ReflectUtils.forName(types[i]);
                }
            }
        }
        //如果接口为null,放入下面这两个接口
        if (interfaces == null) {
            interfaces = new Class<?>[]{invoker.getInterface(), EchoService.class};
        }
        //为http和hessian调用添加支持 添加了GenericService
        if (!invoker.getInterface().equals(GenericService.class) && generic) {
            int len = interfaces.length;
            Class<?>[] temp = interfaces;
            interfaces = new Class<?>[len + 1];
            System.arraycopy(temp, 0, interfaces, 0, len);
            interfaces[len] = GenericService.class;
        }
        return getProxy(invoker, interfaces);
    }

    public abstract <T> T getProxy(Invoker<T> invoker, Class<?>[] types);

}
