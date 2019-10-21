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

import com.alibaba.dubbo.common.extension.ExtensionLoader;
import com.alibaba.dubbo.common.serialize.Serialization;
import com.alibaba.dubbo.common.status.StatusChecker;
import com.alibaba.dubbo.common.threadpool.ThreadPool;
import com.alibaba.dubbo.config.support.Parameter;
import com.alibaba.dubbo.remoting.Codec;
import com.alibaba.dubbo.remoting.Dispatcher;
import com.alibaba.dubbo.remoting.Transporter;
import com.alibaba.dubbo.remoting.exchange.Exchanger;
import com.alibaba.dubbo.remoting.telnet.TelnetHandler;
import com.alibaba.dubbo.rpc.Protocol;

import java.util.Map;

/**
 * 服务提供者协议配置。对应的配置类： org.apache.dubbo.config.ProtocolConfig。同时，如果需要支持多协议，
 * 可以声明多个 <dubbo:protocol> 标签，并在 <dubbo:service> 中通过 protocol 属性指定使用的协议。
 *
 * @export
 */
public class ProtocolConfig extends AbstractConfig {

    private static final long serialVersionUID = 6913423882496634749L;

    // protocol name
    /**
     * 协议名称
     */
    private String name;

    // service IP address (when there are multiple network cards available)
    /**
     * 服务主机名，多网卡选择或指定VIP及域名时使用，
     * 为空则自动查找本机IP，-建议不要配置，让Dubbo自动获取本机IP
     */
    private String host;

    // service port
    /**
     * 服务端口
     */
    private Integer port;

    // context path
    private String contextpath;

    // thread pool
    /**
     * 线程池类型，可选：fixed/cached
     */
    private String threadpool;

    // thread pool size (fixed size)
    /**
     * 服务线程池大小(固定大小)
     */
    private Integer threads;

    // IO thread pool size (fixed size)
    /**
     * io线程池大小(固定大小)
     */
    private Integer iothreads;

    // thread pool's queue length
    /**
     * 线程池队列大小，当线程池满时，
     * 排队等待执行的队列大小，建议不要设置，
     * 当线程池满时应立即失败，重试其它服务提供机器，而不是排队，除非有特殊需求。
     */
    private Integer queues;

    // max acceptable connections
    /**
     * 服务提供方最大可接受连接数
     */
    private Integer accepts;

    // protocol codec
    /**
     * 协议编码方式
     */
    private String codec;

    // serialization
    /**
     * 协议序列化方式，当协议支持多种序列化方式时使用，比如：dubbo协议的dubbo,hessian2,java,compactedjava，以及http协议的json等
     */
    private String serialization;

    // charset
    /**
     * 序列化编码
     */
    private String charset;

    // payload max length
    /**
     * 请求及响应数据包大小限制，单位：字节
     */
    private Integer payload;

    // buffer size
    /**
     * 网络读写缓冲区大小
     */
    private Integer buffer;

    // heartbeat interval
    /**
     * 心跳间隔，对于长连接，当物理层断开时，
     * 比如拔网线，TCP的FIN消息来不及发送，
     * 对方收不到断开事件，此时需要心跳来帮助检查连接是否已断开
     */
    private Integer heartbeat;

    // access log
    /**
     * 设为true，将向logger中输出访问日志，
     * 也可填写访问日志文件路径，直接把访问日志输出到指定文件
     */
    private String accesslog;

    // transfort
    /**
     * 协议的服务端和客户端实现类型，比如：dubbo协议的mina,netty等，可以分拆为server和client配置
     */
    private String transporter;

    // how information is exchanged
    private String exchanger;

    // thread dispatch mode
    /**
     * 协议的消息派发方式，用于指定线程模型，比如：dubbo协议的all, direct, message, execution, connection等
     */
    private String dispatcher;

    // networker
    private String networker;

    // sever impl
    /**
     * 协议的服务器端实现类型，比如：dubbo协议的mina,netty等，http协议的jetty,servlet等
     */
    private String server;

    // client impl

    /**
     * 协议的客户端实现类型，比如：dubbo协议的mina,netty等
     */
    private String client;

    // supported telnet commands, separated with comma.
    /**
     * 所支持的telnet命令，多个命令用逗号分隔
     */
    private String telnet;

    // command line prompt
    private String prompt;

    // status check
    private String status;

    // whether to register
    private Boolean register;

    // parameters
    // 是否长连接
    // TODO add this to provider config
    private Boolean keepAlive;

    // TODO add this to provider config
    private String optimizer;

    private String extension;

    // parameters
    private Map<String, String> parameters;

    // if it's default
    private Boolean isDefault;

    public ProtocolConfig() {
    }

    public ProtocolConfig(String name) {
        setName(name);
    }

    public ProtocolConfig(String name, int port) {
        setName(name);
        setPort(port);
    }

    @Parameter(excluded = true)
    public String getName() {
        return name;
    }

    public void setName(String name) {
        checkName("name", name);
        this.name = name;
        if (id == null || id.length() == 0) {
            id = name;
        }
    }

    @Parameter(excluded = true)
    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        checkName("host", host);
        this.host = host;
    }

    @Parameter(excluded = true)
    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    @Deprecated
    @Parameter(excluded = true)
    public String getPath() {
        return getContextpath();
    }

    @Deprecated
    public void setPath(String path) {
        setContextpath(path);
    }

    @Parameter(excluded = true)
    public String getContextpath() {
        return contextpath;
    }

    public void setContextpath(String contextpath) {
        checkPathName("contextpath", contextpath);
        this.contextpath = contextpath;
    }

    public String getThreadpool() {
        return threadpool;
    }

    public void setThreadpool(String threadpool) {
        checkExtension(ThreadPool.class, "threadpool", threadpool);
        this.threadpool = threadpool;
    }

    public Integer getThreads() {
        return threads;
    }

    public void setThreads(Integer threads) {
        this.threads = threads;
    }

    public Integer getIothreads() {
        return iothreads;
    }

    public void setIothreads(Integer iothreads) {
        this.iothreads = iothreads;
    }

    public Integer getQueues() {
        return queues;
    }

    public void setQueues(Integer queues) {
        this.queues = queues;
    }

    public Integer getAccepts() {
        return accepts;
    }

    public void setAccepts(Integer accepts) {
        this.accepts = accepts;
    }

    public String getCodec() {
        return codec;
    }

    public void setCodec(String codec) {
        if ("dubbo".equals(name)) {
            checkMultiExtension(Codec.class, "codec", codec);
        }
        this.codec = codec;
    }

    public String getSerialization() {
        return serialization;
    }

    public void setSerialization(String serialization) {
        if ("dubbo".equals(name)) {
            checkMultiExtension(Serialization.class, "serialization", serialization);
        }
        this.serialization = serialization;
    }

    public String getCharset() {
        return charset;
    }

    public void setCharset(String charset) {
        this.charset = charset;
    }

    public Integer getPayload() {
        return payload;
    }

    public void setPayload(Integer payload) {
        this.payload = payload;
    }

    public Integer getBuffer() {
        return buffer;
    }

    public void setBuffer(Integer buffer) {
        this.buffer = buffer;
    }

    public Integer getHeartbeat() {
        return heartbeat;
    }

    public void setHeartbeat(Integer heartbeat) {
        this.heartbeat = heartbeat;
    }

    public String getServer() {
        return server;
    }

    public void setServer(String server) {
        if ("dubbo".equals(name)) {
            checkMultiExtension(Transporter.class, "server", server);
        }
        this.server = server;
    }

    public String getClient() {
        return client;
    }

    public void setClient(String client) {
        if ("dubbo".equals(name)) {
            checkMultiExtension(Transporter.class, "client", client);
        }
        this.client = client;
    }

    public String getAccesslog() {
        return accesslog;
    }

    public void setAccesslog(String accesslog) {
        this.accesslog = accesslog;
    }

    public String getTelnet() {
        return telnet;
    }

    public void setTelnet(String telnet) {
        checkMultiExtension(TelnetHandler.class, "telnet", telnet);
        this.telnet = telnet;
    }

    @Parameter(escaped = true)
    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        checkMultiExtension(StatusChecker.class, "status", status);
        this.status = status;
    }

    public Boolean isRegister() {
        return register;
    }

    public void setRegister(Boolean register) {
        this.register = register;
    }

    public String getTransporter() {
        return transporter;
    }

    public void setTransporter(String transporter) {
        checkExtension(Transporter.class, "transporter", transporter);
        this.transporter = transporter;
    }

    public String getExchanger() {
        return exchanger;
    }

    public void setExchanger(String exchanger) {
        checkExtension(Exchanger.class, "exchanger", exchanger);
        this.exchanger = exchanger;
    }

    /**
     * typo, switch to use {@link #getDispatcher()}
     *
     * @deprecated {@link #getDispatcher()}
     */
    @Deprecated
    @Parameter(excluded = true)
    public String getDispather() {
        return getDispatcher();
    }

    /**
     * typo, switch to use {@link #getDispatcher()}
     *
     * @deprecated {@link #setDispatcher(String)}
     */
    @Deprecated
    public void setDispather(String dispather) {
        setDispatcher(dispather);
    }

    public String getDispatcher() {
        return dispatcher;
    }

    public void setDispatcher(String dispatcher) {
        checkExtension(Dispatcher.class, "dispacther", dispatcher);
        this.dispatcher = dispatcher;
    }

    public String getNetworker() {
        return networker;
    }

    public void setNetworker(String networker) {
        this.networker = networker;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, String> parameters) {
        this.parameters = parameters;
    }

    public Boolean isDefault() {
        return isDefault;
    }

    public void setDefault(Boolean isDefault) {
        this.isDefault = isDefault;
    }

    public Boolean getKeepAlive() {
        return keepAlive;
    }

    public void setKeepAlive(Boolean keepAlive) {
        this.keepAlive = keepAlive;
    }

    public String getOptimizer() {
        return optimizer;
    }

    public void setOptimizer(String optimizer) {
        this.optimizer = optimizer;
    }

    public String getExtension() {
        return extension;
    }

    public void setExtension(String extension) {
        this.extension = extension;
    }

    public void destroy() {
        if (name != null) {
            ExtensionLoader.getExtensionLoader(Protocol.class).getExtension(name).destroy();
        }
    }
}
