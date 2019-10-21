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

import java.util.Map;

/**
 * 注册中心配置，用于配置连接注册中心相关信息。
 * 同时如果有多个不同的注册中心，可以声明多个 <dubbo:registry> 标签，
 * 并在 <dubbo:service> 或 <dubbo:reference> 的 registry 属性指定使用的注册中心。
 * @export
 */
public class RegistryConfig extends AbstractConfig {

    public static final String NO_AVAILABLE = "N/A";
    private static final long serialVersionUID = 5508512956753757169L;
    // register center address
    /**
     * 注册中心地址
     */
    private String address;

    // username to login register center
    /**
     * 登录注册中心的用户名
     */
    private String username;

    // password to login register center
    /**
     * 登录注册中心的密码
     */
    private String password;

    // default port for register center
    /**
     * 注册中心端口号
     */
    private Integer port;

    // protocol for register center
    /**
     * 注册中心使用协议
     */
    private String protocol;

    // client impl
    /**
     * 网络传输方式，可选netty、mina、grizzly、http
     */
    private String transporter;
    /**
     * 默认dubbo协议是netty、http协议是servlet
     */
    private String server;
    /**
     * 默认dubbo协议是netty
     */
    private String client;
    /**
     * 容错支持，默认值failover，失败自动重试，
     * 对应<dubbo:service 、<dubbo:reference、<dubbo:provider、<dubbo:consumer
     * 标签cluster属性，容错机制一共有以下几种
     */
    private String cluster;
    /**
     * 默认值 dubbo，服务注册分组，跨组服务不会相互影响，且不能相互调用，适合于环境隔离
     */
    private String group;
    /**
     * 版本号
     */
    private String version;

    // request timeout in milliseconds for register center
    /**
     *  request timeout in milliseconds for register center
     *  注册中心请求超时时间(毫秒)
     */
    private Integer timeout;

    // session timeout in milliseconds for register center
    /**
     * session timeout in milliseconds for register center
     * 注册中心会话超时时间(毫秒)，
     * 用于检测提供者非正常断线后的脏数据，
     * 比如用心跳检测的实现，此时间就是心跳间隔，
     * 不同注册中心实现不一样。
     */
    private Integer session;

    // file for saving register center dynamic list
    /**
     * 使用文件缓存注册中心地址列表及服务提供者列表，
     * 应用重启时将基于此文件恢复，
     * 注意：两个注册中心不能使用同一文件存储
     */
    private String file;

    // wait time before stop
    /**
     * wait time before stop
     * 停止时等待通知完成时间(毫秒)
     */
    private Integer wait;

    // whether to check if register center is available when boot up
    /**
     *  注册中心不存在时，是否报错
     */
    private Boolean check;

    // whether to allow dynamic service to register on the register center
    /**
     *服务是否动态注册，如果设为false，
     * 注册后将显示为disable状态，需人工启用，
     * 并且服务提供者停止时，也不会自动取消注册，需人工禁用。
     */
    private Boolean dynamic;

    // whether to export service on the register center
    /**
     * 是否向此注册中心注册服务，
     * 如果设为false，将只订阅，不注册
     */
    private Boolean register;

    // whether allow to subscribe service on the register center
    /**
     *是否向此注册中心订阅服务，
     * 如果设为false，将只注册，不订阅
     */
    private Boolean subscribe;

    // customized parameters
    /**
     * 其他定制化的参数
     */
    private Map<String, String> parameters;

    // if it's default
    /**
     * 是否默认
     */
    private Boolean isDefault;

    public RegistryConfig() {
    }

    public RegistryConfig(String address) {
        setAddress(address);
    }

    public RegistryConfig(String address, String protocol) {
        setAddress(address);
        setProtocol(protocol);
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        checkName("protocol", protocol);
        this.protocol = protocol;
    }

    @Parameter(excluded = true)
    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        checkName("username", username);
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        checkLength("password", password);
        this.password = password;
    }

    /**
     * @return wait
     * @see com.alibaba.dubbo.config.ProviderConfig#getWait()
     * @deprecated
     */
    @Deprecated
    public Integer getWait() {
        return wait;
    }

    /**
     * @param wait
     * @see com.alibaba.dubbo.config.ProviderConfig#setWait(Integer)
     * @deprecated
     */
    @Deprecated
    public void setWait(Integer wait) {
        this.wait = wait;
        if (wait != null && wait > 0)
            System.setProperty(Constants.SHUTDOWN_WAIT_KEY, String.valueOf(wait));
    }

    public Boolean isCheck() {
        return check;
    }

    public void setCheck(Boolean check) {
        this.check = check;
    }

    public String getFile() {
        return file;
    }

    public void setFile(String file) {
        checkPathLength("file", file);
        this.file = file;
    }

    /**
     * @return transport
     * @see #getTransporter()
     * @deprecated
     */
    @Deprecated
    @Parameter(excluded = true)
    public String getTransport() {
        return getTransporter();
    }

    /**
     * @param transport
     * @see #setTransporter(String)
     * @deprecated
     */
    @Deprecated
    public void setTransport(String transport) {
        setTransporter(transport);
    }

    public String getTransporter() {
        return transporter;
    }

    public void setTransporter(String transporter) {
        checkName("transporter", transporter);
        /*if(transporter != null && transporter.length() > 0 && ! ExtensionLoader.getExtensionLoader(Transporter.class).hasExtension(transporter)){
            throw new IllegalStateException("No such transporter type : " + transporter);
        }*/
        this.transporter = transporter;
    }

    public String getServer() {
        return server;
    }

    public void setServer(String server) {
        checkName("server", server);
        /*if(server != null && server.length() > 0 && ! ExtensionLoader.getExtensionLoader(Transporter.class).hasExtension(server)){
            throw new IllegalStateException("No such server type : " + server);
        }*/
        this.server = server;
    }

    public String getClient() {
        return client;
    }

    public void setClient(String client) {
        checkName("client", client);
        /*if(client != null && client.length() > 0 && ! ExtensionLoader.getExtensionLoader(Transporter.class).hasExtension(client)){
            throw new IllegalStateException("No such client type : " + client);
        }*/
        this.client = client;
    }

    public Integer getTimeout() {
        return timeout;
    }

    public void setTimeout(Integer timeout) {
        this.timeout = timeout;
    }

    public Integer getSession() {
        return session;
    }

    public void setSession(Integer session) {
        this.session = session;
    }

    public Boolean isDynamic() {
        return dynamic;
    }

    public void setDynamic(Boolean dynamic) {
        this.dynamic = dynamic;
    }

    public Boolean isRegister() {
        return register;
    }

    public void setRegister(Boolean register) {
        this.register = register;
    }

    public Boolean isSubscribe() {
        return subscribe;
    }

    public void setSubscribe(Boolean subscribe) {
        this.subscribe = subscribe;
    }

    public String getCluster() {
        return cluster;
    }

    public void setCluster(String cluster) {
        this.cluster = cluster;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, String> parameters) {
        checkParameterName(parameters);
        this.parameters = parameters;
    }

    public Boolean isDefault() {
        return isDefault;
    }

    public void setDefault(Boolean isDefault) {
        this.isDefault = isDefault;
    }

}