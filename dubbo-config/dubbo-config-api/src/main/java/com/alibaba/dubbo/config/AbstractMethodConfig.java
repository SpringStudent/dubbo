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
package com.alibaba.dubbo.config;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.config.support.Parameter;
import com.alibaba.dubbo.rpc.cluster.LoadBalance;

import java.util.Map;

/**
 * 封装了一些方法级别的相关属性：
 * 同时该标签为 <dubbo:service> 或 <dubbo:reference> 的子标签，用于控制到方法级。
 * @export
 */
public abstract class AbstractMethodConfig extends AbstractConfig {

    private static final long serialVersionUID = 1L;

    // timeout for remote invocation in milliseconds
    /**
     * 方法调用超时时间(毫秒)
     */
    protected Integer timeout;

    // retry times
    /**
     * 远程服务调用重试次数，不包括第一次调用，不需要重试请设为0
     */
    protected Integer retries;

    // max concurrent invocations
    /**
     * 每服务消费者最大并发调用
     */
    protected Integer actives;

    // load balance
    /**
     * 负载均衡策略，可选值：random,roundrobin,leastactive，分别表示：随机，轮询，最少活跃调用
     */
    protected String loadbalance;

    // whether to async
    /**
     * 是否异步执行，不可靠异步，只是忽略返回值，不阻塞执行线程
     */
    protected Boolean async;

    // whether to ack async-sent
    /**
     * 异步调用时，标记sent=true时，表示网络已发出数据
     */
    protected Boolean sent;

    // the name of mock class which gets called when a service fails to execute
    /**
     * 服务执行失败时调用的模拟类的名称
     */
    protected String mock;

    // merger
    protected String merger;

    // cache
    /**
     * 以调用参数为key，缓存返回结果，可选：lru, threadlocal, jcache等
     */
    protected String cache;

    // validation
    /**
     * 是否启用JSR303标准注解验证，如果启用，将对方法参数上的注解进行校验
     */
    protected String validation;

    // customized parameters
    protected Map<String, String> parameters;

    /**
     * forks for forking cluster
     */
    protected Integer forks;

    public Integer getForks() {
        return forks;
    }

    public void setForks(Integer forks) {
        this.forks = forks;
    }

    public Integer getTimeout() {
        return timeout;
    }

    public void setTimeout(Integer timeout) {
        this.timeout = timeout;
    }

    public Integer getRetries() {
        return retries;
    }

    public void setRetries(Integer retries) {
        this.retries = retries;
    }

    public String getLoadbalance() {
        return loadbalance;
    }

    public void setLoadbalance(String loadbalance) {
        checkExtension(LoadBalance.class, "loadbalance", loadbalance);
        this.loadbalance = loadbalance;
    }

    public Boolean isAsync() {
        return async;
    }

    public void setAsync(Boolean async) {
        this.async = async;
    }

    public Integer getActives() {
        return actives;
    }

    public void setActives(Integer actives) {
        this.actives = actives;
    }

    public Boolean getSent() {
        return sent;
    }

    public void setSent(Boolean sent) {
        this.sent = sent;
    }

    @Parameter(escaped = true)
    public String getMock() {
        return mock;
    }

    public void setMock(Boolean mock) {
        if (mock == null) {
            setMock((String) null);
        } else {
            setMock(String.valueOf(mock));
        }
    }

    public void setMock(String mock) {
        if (mock == null) {
            return;
        }

        if (mock.startsWith(Constants.RETURN_PREFIX) || mock.startsWith(Constants.THROW_PREFIX + " ")) {
            checkLength("mock", mock);
        } else if (mock.startsWith(Constants.FAIL_PREFIX) || mock.startsWith(Constants.FORCE_PREFIX)) {
            checkNameHasSymbol("mock", mock);
        } else {
            checkName("mock", mock);
        }
        this.mock = mock;
    }

    public String getMerger() {
        return merger;
    }

    public void setMerger(String merger) {
        this.merger = merger;
    }

    public String getCache() {
        return cache;
    }

    public void setCache(String cache) {
        this.cache = cache;
    }

    public String getValidation() {
        return validation;
    }

    public void setValidation(String validation) {
        this.validation = validation;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, String> parameters) {
        checkParameterName(parameters);
        this.parameters = parameters;
    }

}
