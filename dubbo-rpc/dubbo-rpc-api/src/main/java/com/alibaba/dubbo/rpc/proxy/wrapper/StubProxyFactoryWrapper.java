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
package com.alibaba.dubbo.rpc.proxy.wrapper;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.Version;
import com.alibaba.dubbo.common.bytecode.Wrapper;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.common.utils.ConfigUtils;
import com.alibaba.dubbo.common.utils.NetUtils;
import com.alibaba.dubbo.common.utils.ReflectUtils;
import com.alibaba.dubbo.common.utils.StringUtils;
import com.alibaba.dubbo.rpc.Exporter;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.Protocol;
import com.alibaba.dubbo.rpc.ProxyFactory;
import com.alibaba.dubbo.rpc.RpcException;
import com.alibaba.dubbo.rpc.service.GenericService;

import java.lang.reflect.Constructor;

/**
 * StubProxyFactoryWrapper
 */
public class StubProxyFactoryWrapper implements ProxyFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(StubProxyFactoryWrapper.class);

    private final ProxyFactory proxyFactory;

    private Protocol protocol;

    public StubProxyFactoryWrapper(ProxyFactory proxyFactory) {
        this.proxyFactory = proxyFactory;
    }

    public void setProtocol(Protocol protocol) {
        this.protocol = protocol;
    }

    @Override
    public <T> T getProxy(Invoker<T> invoker, boolean generic) throws RpcException {
        return proxyFactory.getProxy(invoker, generic);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public <T> T getProxy(Invoker<T> invoker) throws RpcException {
        //委托JavassistProxyFactory或者JdkProxyFactory创建代理对象
        T proxy = proxyFactory.getProxy(invoker);
        //
        if (GenericService.class != invoker.getInterface()) {
            String stub = invoker.getUrl().getParameter(Constants.STUB_KEY, invoker.getUrl().getParameter(Constants.LOCAL_KEY));
            if (ConfigUtils.isNotEmpty(stub)) {
                //获取接口类型
                Class<?> serviceType = invoker.getInterface();
                //本地存根配置判断
                if (ConfigUtils.isDefault(stub)) {
                    if (invoker.getUrl().hasParameter(Constants.STUB_KEY)) {
                        stub = serviceType.getName() + "Stub";
                    } else {
                        stub = serviceType.getName() + "Local";
                    }
                }
                try {
                    //加载本地存根类
                    Class<?> stubClass = ReflectUtils.forName(stub);
                    //判断本地存根类是否实现了serviceType，没有抛出异常
                    if (!serviceType.isAssignableFrom(stubClass)) {
                        throw new IllegalStateException("The stub implementation class " + stubClass.getName() + " not implement interface " + serviceType.getName());
                    }
                    try {
                        //查找本地存根类是否有 serviceType的构造函数
                        Constructor<?> constructor = ReflectUtils.findConstructor(stubClass, serviceType);
                        //有的话使用proxy对象创建stub对象返回
                        proxy = (T) constructor.newInstance(new Object[]{proxy});
                        //如果dubbo.stub.event 配置为true，export
                        URL url = invoker.getUrl();
                        if (url.getParameter(Constants.STUB_EVENT_KEY, Constants.DEFAULT_STUB_EVENT)) {
                            url = url.addParameter(Constants.STUB_EVENT_METHODS_KEY, StringUtils.join(Wrapper.getWrapper(proxy.getClass()).getDeclaredMethodNames(), ","));
                            url = url.addParameter(Constants.IS_SERVER_KEY, Boolean.FALSE.toString());
                            try {
                                export(proxy, (Class) invoker.getInterface(), url);
                            } catch (Exception e) {
                                LOGGER.error("export a stub service error.", e);
                            }
                        }
                    } catch (NoSuchMethodException e) {
                        throw new IllegalStateException("No such constructor \"public " + stubClass.getSimpleName() + "(" + serviceType.getName() + ")\" in stub implementation class " + stubClass.getName(), e);
                    }
                } catch (Throwable t) {
                    LOGGER.error("Failed to create stub implementation class " + stub + " in consumer " + NetUtils.getLocalHost() + " use dubbo version " + Version.getVersion() + ", cause: " + t.getMessage(), t);
                    // ignore
                }
            }
        }
        return proxy;
    }

    @Override
    public <T> Invoker<T> getInvoker(T proxy, Class<T> type, URL url) throws RpcException {
        return proxyFactory.getInvoker(proxy, type, url);
    }

    private <T> Exporter<T> export(T instance, Class<T> type, URL url) {
        return protocol.export(proxyFactory.getInvoker(instance, type, url));
    }

}
