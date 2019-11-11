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
package com.alibaba.dubbo.registry.integration;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.Version;
import com.alibaba.dubbo.common.extension.ExtensionLoader;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.common.utils.NetUtils;
import com.alibaba.dubbo.common.utils.StringUtils;
import com.alibaba.dubbo.registry.NotifyListener;
import com.alibaba.dubbo.registry.Registry;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.Protocol;
import com.alibaba.dubbo.rpc.RpcException;
import com.alibaba.dubbo.rpc.RpcInvocation;
import com.alibaba.dubbo.rpc.cluster.Cluster;
import com.alibaba.dubbo.rpc.cluster.Configurator;
import com.alibaba.dubbo.rpc.cluster.ConfiguratorFactory;
import com.alibaba.dubbo.rpc.cluster.Router;
import com.alibaba.dubbo.rpc.cluster.RouterFactory;
import com.alibaba.dubbo.rpc.cluster.directory.AbstractDirectory;
import com.alibaba.dubbo.rpc.cluster.directory.StaticDirectory;
import com.alibaba.dubbo.rpc.cluster.support.ClusterUtils;
import com.alibaba.dubbo.rpc.protocol.InvokerWrapper;
import com.alibaba.dubbo.rpc.support.RpcUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * RegistryDirectory
 */
public class RegistryDirectory<T> extends AbstractDirectory<T> implements NotifyListener {

    private static final Logger logger = LoggerFactory.getLogger(RegistryDirectory.class);
    /**
     * 集群策略，默认为failover。
     */
    private static final Cluster cluster = ExtensionLoader.getExtensionLoader(Cluster.class).getAdaptiveExtension();
    /**
     * 路由工厂，可以通过监控中心或治理中心配置。
     */
    private static final RouterFactory routerFactory = ExtensionLoader.getExtensionLoader(RouterFactory.class).getAdaptiveExtension();
    /**
     * 配置实现工厂类
     */
    private static final ConfiguratorFactory configuratorFactory = ExtensionLoader.getExtensionLoader(ConfiguratorFactory.class).getAdaptiveExtension();
    /**
     * 服务key，默认为服务接口名。com.alibaba.dubbo.registry.RegistryService，注册中心在Dubbo中也是使用服务暴露。
     */
    private final String serviceKey; // Initialization at construction time, assertion not null
    /**
     * 服务提供者接口类，例如interface com.alibaba.dubbo.demo.DemoService
     */
    private final Class<T> serviceType; // Initialization at construction time, assertion not null
    /**
     * 服务消费者URL中的所有属性。
     */
    private final Map<String, String> queryMap; // Initialization at construction time, assertion not null
    /**
     * 注册中心URL，只保留消息消费者URL查询属性，也就是queryMap。
     */
    private final URL directoryUrl; // Initialization at construction time, assertion not null, and always assign non null value
    /**
     * 引用服务提供者方法数组。
     */
    private final String[] serviceMethods;
    /**
     * 多个服务组
     */
    private final boolean multiGroup;
    /**
     * 协议
     */
    private Protocol protocol; // Initialization at the time of injection, the assertion is not null
    /**
     * 注册中心
     */
    private Registry registry; // Initialization at the time of injection, the assertion is not null
    private volatile boolean forbidden = false;

    private volatile URL overrideDirectoryUrl; // Initialization at construction time, assertion not null, and always assign non null value

    private volatile URL registeredConsumerUrl;

    /**
     * override rules
     * Priority: override>-D>consumer>provider
     * Rule one: for a certain provider <ip:port,timeout=100>
     * Rule two: for all providers <* ,timeout=5000>
     */
    /**
     * 配置信息。
     */
    private volatile List<Configurator> configurators; // The initial value is null and the midway may be assigned to null, please use the local variable reference

    // Map<url, Invoker> cache service url to invoker mapping.
    /**
     * 服务URL对应的Invoker(服务提供者调用器)。
     */
    private volatile Map<String, Invoker<T>> urlInvokerMap; // The initial value is null and the midway may be assigned to null, please use the local variable reference
    /**
     * methodInvokerMap; methodName : List< Invoker< T >>，
     * dubbo:method 对应的Invoker缓存表。
     */
    // Map<methodName, Invoker> cache service method to invokers mapping.
    private volatile Map<String, List<Invoker<T>>> methodInvokerMap; // The initial value is null and the midway may be assigned to null, please use the local variable reference
    /**
     * 缓存invokerUrls到invokers的映射
     */
    // Set<invokerUrls> cache invokeUrls to invokers mapping.
    private volatile Set<URL> cachedInvokerUrls; // The initial value is null and the midway may be assigned to null, please use the local variable reference

    public RegistryDirectory(Class<T> serviceType, URL url) {
        super(url);
        //参数校验
        if (serviceType == null)
            throw new IllegalArgumentException("service type is null.");
        if (url.getServiceKey() == null || url.getServiceKey().length() == 0)
            throw new IllegalArgumentException("registry serviceKey is null.");
        this.serviceType = serviceType;
        //获取注册中心URL的serviceKey：com.alibaba.dubbo.registry.RegistryService。
        this.serviceKey = url.getServiceKey();
        //获取zookeeper://xxx中的refer属性的值，应该像如下这样子
        //application=echo-consumer&check=false&dubbo=2.0.2&echo.async=true&echo.retries=3&
        //interface=com.alibaba.dubbo.study.day01.xml.service.EchoService&methods=echo,addListener&
        // pid=24572&register.ip=169.254.22.149&side=consumer&timestamp=1573453015233
        this.queryMap = StringUtils.parseQueryString(url.getParameterAndDecoded(Constants.REFER_KEY));
        //overrideDirectoryUrl和directoryUrl属性赋值;移除注册中心URL的监控中心以及其他属性值，只保留消息消费者的配置属性。
        //zookeeper://127.0.0.1:2181/com.alibaba.dubbo.registry.RegistryService?application=echo-consumer&check=false&
        //dubbo=2.0.2&echo.async=true&echo.retries=3&interface=com.alibaba.dubbo.study.day01.xml.service.EchoService&
        //methods=echo,addListener&pid=24924&register.ip=169.254.22.149&side=consumer&timestamp=1573453320189
        this.overrideDirectoryUrl = this.directoryUrl = url.setPath(url.getServiceInterface()).clearParameters().addParameters(queryMap).removeParameter(Constants.MONITOR_KEY);
        //获取directoryUrl中的group属性
        String group = directoryUrl.getParameter(Constants.GROUP_KEY, "");
        //是否为多组配置
        this.multiGroup = group != null && ("*".equals(group) || group.contains(","));
        //获取methods属性，都是方法名称
        String methods = queryMap.get(Constants.METHODS_KEY);
        //数组
        this.serviceMethods = methods == null ? null : Constants.COMMA_SPLIT_PATTERN.split(methods);
    }

    /**
     * Convert override urls to map for use when re-refer.
     * 将替代网址转换为映射以供重新引用时使用
     * Send all rules every time, the urls will be reassembled and calculated
     * 每次发送所有规则，网址将重新组合并计算
     *
     * @param urls Contract:
     *             </br>1.override://0.0.0.0/...( or override://ip:port...?anyhost=true)&para1=value1... means global rules (all of the providers take effect)
     *             </br>2.override://ip:port...?anyhost=false Special rules (only for a certain provider)
     *             </br>3.override:// rule is not supported... ,needs to be calculated by registry itself.
     *             </br>4.override://0.0.0.0/ without parameters means clearing the override
     * @return
     */
    public static List<Configurator> toConfigurators(List<URL> urls) {
        if (urls == null || urls.isEmpty()) {
            return Collections.emptyList();
        }

        List<Configurator> configurators = new ArrayList<Configurator>(urls.size());
        for (URL url : urls) {
            //如果是empty://跳出循环 清空configurators
            if (Constants.EMPTY_PROTOCOL.equals(url.getProtocol())) {
                configurators.clear();
                break;
            }
            //获取url中的所有参数
            Map<String, String> override = new HashMap<String, String>(url.getParameters());
            //The anyhost parameter of override may be added automatically, it can't change the judgement of changing url
            //override的anyhost参数将会被自动添加，他不能更改url的判断
            override.remove(Constants.ANYHOST_KEY);
            if (override.size() == 0) {
                configurators.clear();
                continue;
            }
            //添加
            configurators.add(configuratorFactory.getConfigurator(url));
        }
        Collections.sort(configurators);
        return configurators;
    }

    public void setProtocol(Protocol protocol) {
        this.protocol = protocol;
    }

    public void setRegistry(Registry registry) {
        this.registry = registry;
    }

    public void subscribe(URL url) {
        //consumer://169.254.22.149/com.alibaba.dubbo.study.day01.xml.service.EchoService?application=echo-consumer&
        //category=providers,configurators,routers&check=false&dubbo=2.0.2&echo.async=true&echo.retries=3&
        //interface=com.alibaba.dubbo.study.day01.xml.service.EchoService&methods=echo,addListener&pid=24092&
        //side=consumer&timestamp=1573453550695
        setConsumerUrl(url);
        //ZookeeperRegistry的subscribe调用，这一步操作会建立对zk的节点目录providers、configurators、routers的监听
        //如果这些zk的这些目录内容发生改变则会反馈到RegistryDirectory的notify方法!!!
        //逻辑我就不用讲了吧，同服务暴露的流程一致！
        registry.subscribe(url, this);
    }

    @Override
    public void destroy() {
        if (isDestroyed()) {
            return;
        }

        // unregister.
        try {
            if (getRegisteredConsumerUrl() != null && registry != null && registry.isAvailable()) {
                registry.unregister(getRegisteredConsumerUrl());
            }
        } catch (Throwable t) {
            logger.warn("unexpected error when unregister service " + serviceKey + "from registry" + registry.getUrl(), t);
        }

        // unsubscribe.
        try {
            if (getConsumerUrl() != null && registry != null && registry.isAvailable()) {
                registry.unsubscribe(getConsumerUrl(), this);
            }
        } catch (Throwable t) {
            logger.warn("unexpeced error when unsubscribe service " + serviceKey + "from registry" + registry.getUrl(), t);
        }
        super.destroy(); // must be executed after unsubscribing
        try {
            destroyAllInvokers();
        } catch (Throwable t) {
            logger.warn("Failed to destroy service " + serviceKey, t);
        }
    }

    @Override
    public synchronized void notify(List<URL> urls) {
        //携带提供者元数据的url
        List<URL> invokerUrls = new ArrayList<URL>();
        //携带路由元数据的url
        List<URL> routerUrls = new ArrayList<URL>();
        //携带配置元数据的url
        List<URL> configuratorUrls = new ArrayList<URL>();
        //遍历routers、configurators、providers目录下的元数据配置
        for (URL url : urls) {
            String protocol = url.getProtocol();
            String category = url.getParameter(Constants.CATEGORY_KEY, Constants.DEFAULT_CATEGORY);
            //如果category类型为routers 或者url的协议为route
            if (Constants.ROUTERS_CATEGORY.equals(category)
                    || Constants.ROUTE_PROTOCOL.equals(protocol)) {
                routerUrls.add(url);
                //如果category类型为configurators 或者 协议类型为override
            } else if (Constants.CONFIGURATORS_CATEGORY.equals(category)
                    || Constants.OVERRIDE_PROTOCOL.equals(protocol)) {
                configuratorUrls.add(url);
                //如果category类型为providers
            } else if (Constants.PROVIDERS_CATEGORY.equals(category)) {
                invokerUrls.add(url);
            } else {
                logger.warn("Unsupported category " + category + " in notified url: " + url + " from registry " + getUrl().getAddress() + " to consumer " + NetUtils.getLocalHost());
            }
        }
        //
        //转换configuratorsUrl为List<Configurator>对象
        if (configuratorUrls != null && !configuratorUrls.isEmpty()) {
            this.configurators = toConfigurators(configuratorUrls);
        }
        // routers
        //转换routerUrls对象为List<Router>
        if (routerUrls != null && !routerUrls.isEmpty()) {
            List<Router> routers = toRouters(routerUrls);
            if (routers != null) { // null - do nothing
                setRouters(routers);
            }
        }
        //
        List<Configurator> localConfigurators = this.configurators; // local reference
        // merge override parameters
        // 合并override的参数
        this.overrideDirectoryUrl = directoryUrl;
        if (localConfigurators != null && !localConfigurators.isEmpty()) {
            for (Configurator configurator : localConfigurators) {
                this.overrideDirectoryUrl = configurator.configure(overrideDirectoryUrl);
            }
        }
        // providers
        refreshInvoker(invokerUrls);
    }

    /**
     * 将invokerURL列表转换为Invoker Map。 转换规则如下：
     * 1.If URL has been converted to invoker, it is no longer re-referenced and obtained directly from the cache, and notice that any parameter changes in the URL will be re-referenced.
     * 2.If the incoming invoker list is not empty, it means that it is the latest invoker list
     * 3.If the list of incoming invokerUrl is empty, It means that the rule is only a override rule or a route rule, which needs to be re-contrasted to decide whether to re-reference.
     *
     * 1.如果已将URL转换为调用程序invoker，则将不再重新引用该URL并直接从缓存，并且请注意，URL中的任何参数更改都将被重新引用。
     * 2.如果传入的调用者列表不为空，则表示它是最新的调用者列表
     * 3.如果传入的invokerUrl列表为空，则表示该规则只是覆盖规则或路由规则，需要重新进行比较以决定是否重新引用。
     * @param invokerUrls this parameter can't be null
     */
    // TODO: 2017/8/31 FIXME The thread pool should be used to refresh the address, otherwise the task may be accumulated.
    private void refreshInvoker(List<URL> invokerUrls) {
        // invokerUrls 仅有一个元素，且 url 协议头为 empty，此时表示禁用所有服务
        if (invokerUrls != null && invokerUrls.size() == 1 && invokerUrls.get(0) != null
                && Constants.EMPTY_PROTOCOL.equals(invokerUrls.get(0).getProtocol())) {
            //设置 forbidden 为 true 禁止访问
            this.forbidden = true;
            this.methodInvokerMap = null;
            //销毁所有的invoker
            destroyAllInvokers();
        } else {
            //设置允许访问标识
            this.forbidden = false;
            //将urlInvokerMap的值赋值给oldUrlInvokerMap
            Map<String, Invoker<T>> oldUrlInvokerMap = this.urlInvokerMap;
            // 如果invokerUrls为空，并且已缓存的invokerUrls不为空，将缓存中的invoker url添加到invokerUrls中，这里可以说明如果providers目录未发送变化
            if (invokerUrls.isEmpty() && this.cachedInvokerUrls != null) {
                invokerUrls.addAll(this.cachedInvokerUrls);
            } else {
                //将invokeUrls加入到缓存中,方便比较
                this.cachedInvokerUrls = new HashSet<URL>();
                this.cachedInvokerUrls.addAll(invokerUrls);
            }
            if (invokerUrls.isEmpty()) {
                return;
            }
            //将invokerUrls转换成为invoker
            Map<String, Invoker<T>> newUrlInvokerMap = toInvokers(invokerUrls);
            //将 newUrlInvokerMap 转成方法名到 Invoker 列表的映射
            Map<String, List<Invoker<T>>> newMethodInvokerMap = toMethodInvokers(newUrlInvokerMap);
            // 如果newUrlInvokerMap为空打印错误日志并返回
            if (newUrlInvokerMap == null || newUrlInvokerMap.size() == 0) {
                logger.error(new IllegalStateException("urls to invokers error .invokerUrls.size :" + invokerUrls.size() + ", invoker.size :0. urls :" + invokerUrls.toString()));
                return;
            }
            this.methodInvokerMap = multiGroup ? toMergeMethodInvokerMap(newMethodInvokerMap) : newMethodInvokerMap;
            this.urlInvokerMap = newUrlInvokerMap;
            try {
                destroyUnusedInvokers(oldUrlInvokerMap, newUrlInvokerMap); // Close the unused Invoker
            } catch (Exception e) {
                logger.warn("destroyUnusedInvokers error. ", e);
            }
        }
    }

    private Map<String, List<Invoker<T>>> toMergeMethodInvokerMap(Map<String, List<Invoker<T>>> methodMap) {
        Map<String, List<Invoker<T>>> result = new HashMap<String, List<Invoker<T>>>();
        //遍历methodmap key为方法名称 value为List<Invoker>
        for (Map.Entry<String, List<Invoker<T>>> entry : methodMap.entrySet()) {
            String method = entry.getKey();
            List<Invoker<T>> invokers = entry.getValue();
            //key为group value为List<Invoker>
            Map<String, List<Invoker<T>>> groupMap = new HashMap<String, List<Invoker<T>>>();
            for (Invoker<T> invoker : invokers) {
                String group = invoker.getUrl().getParameter(Constants.GROUP_KEY, "");
                List<Invoker<T>> groupInvokers = groupMap.get(group);
                if (groupInvokers == null) {
                    groupInvokers = new ArrayList<Invoker<T>>();
                    groupMap.put(group, groupInvokers);
                }
                groupInvokers.add(invoker);
            }
            // 如果 groupMap 中仅包含一组键值对，此时直接取出该键值对的值即可
            if (groupMap.size() == 1) {
                result.put(method, groupMap.values().iterator().next());
                // groupMap.size() > 1 成立，表示 groupMap 中包含多组键值对，比如：
                // {
                //     "group1": [invoker1, invoker2, invoker3, ...],
                //     "group2": [invoker4, invoker5, invoker6, ...]
                // }
            } else if (groupMap.size() > 1) {
                List<Invoker<T>> groupInvokers = new ArrayList<Invoker<T>>();
                // 通过集群类合并每个分组对应的 Invoker 列表
                for (List<Invoker<T>> groupList : groupMap.values()) {
                    groupInvokers.add(cluster.join(new StaticDirectory<T>(groupList)));
                }
                result.put(method, groupInvokers);
            } else {
                result.put(method, invokers);
            }
        }
        return result;
    }

    /**
     * @param urls
     * @return null : no routers ,do nothing
     * else :routers list
     */
    private List<Router> toRouters(List<URL> urls) {
        List<Router> routers = new ArrayList<Router>();
        if (urls == null || urls.isEmpty()) {
            return routers;
        }
        if (urls != null && !urls.isEmpty()) {
            for (URL url : urls) {
                if (Constants.EMPTY_PROTOCOL.equals(url.getProtocol())) {
                    continue;
                }
                String routerType = url.getParameter(Constants.ROUTER_KEY);
                if (routerType != null && routerType.length() > 0) {
                    url = url.setProtocol(routerType);
                }
                try {
                    Router router = routerFactory.getRouter(url);
                    if (!routers.contains(router))
                        routers.add(router);
                } catch (Throwable t) {
                    logger.error("convert router url to router error, url: " + url, t);
                }
            }
        }
        return routers;
    }

    /**
     * Turn urls into invokers, and if url has been refer, will not re-reference.
     * 将url转换为调用程序，如果已引用url，则不会重新引用。
     * @param urls
     * @return invokers
     */
    private Map<String, Invoker<T>> toInvokers(List<URL> urls) {
        Map<String, Invoker<T>> newUrlInvokerMap = new HashMap<String, Invoker<T>>();
        if (urls == null || urls.isEmpty()) {
            return newUrlInvokerMap;
        }
        Set<String> keys = new HashSet<String>();
        //获取服务消费端配置的协议
        String queryProtocols = this.queryMap.get(Constants.PROTOCOL_KEY);
        for (URL providerUrl : urls) {
            // If protocol is configured at the reference side, only the matching protocol is selected
            // 如果在消费端配置了协议，则仅选择匹配的协议
            if (queryProtocols != null && queryProtocols.length() > 0) {
                boolean accept = false;
                String[] acceptProtocols = queryProtocols.split(",");
                for (String acceptProtocol : acceptProtocols) {
                    if (providerUrl.getProtocol().equals(acceptProtocol)) {
                        accept = true;
                        break;
                    }
                }
                // 若服务消费者协议头不被消费者所支持，则忽略当前 providerUrl
                if (!accept) {
                    continue;
                }
            }
            //忽略empty协议
            if (Constants.EMPTY_PROTOCOL.equals(providerUrl.getProtocol())) {
                continue;
            }
            //获取消费端是否支持服务端的的协议，不支持打印错误日志
            if (!ExtensionLoader.getExtensionLoader(Protocol.class).hasExtension(providerUrl.getProtocol())) {
                logger.error(new IllegalStateException("Unsupported protocol " + providerUrl.getProtocol() + " in notified url: " + providerUrl + " from registry " + getUrl().getAddress() + " to consumer " + NetUtils.getLocalHost()
                        + ", supported protocol: " + ExtensionLoader.getExtensionLoader(Protocol.class).getSupportedExtensions()));
                continue;
            }
            //合并Url
            URL url = mergeUrl(providerUrl);
            String key = url.toFullString();
            //忽略重复的Url
            if (keys.contains(key)) {
                continue;
            }
            keys.add(key);
            // 将本地url与invoker的缓存 赋值给localUrlInvokerMap
            Map<String, Invoker<T>> localUrlInvokerMap = this.urlInvokerMap;
            //获取缓存中该url对应的invoker
            Invoker<T> invoker = localUrlInvokerMap == null ? null : localUrlInvokerMap.get(key);
            //不存在与缓存中
            if (invoker == null) {
                try {
                    boolean enabled = true;
                    //如果url中配置了disable属性
                    if (url.hasParameter(Constants.DISABLED_KEY)) {
                        enabled = !url.getParameter(Constants.DISABLED_KEY, false);
                    } else {
                        enabled = url.getParameter(Constants.ENABLED_KEY, true);
                    }
                    //如果没有禁用 调用通过protocol.refer方法获取Invoker
                    if (enabled) {
                        invoker = new InvokerDelegate<T>(protocol.refer(serviceType, url), url, providerUrl);
                    }
                } catch (Throwable t) {
                    logger.error("Failed to refer invoker for interface:" + serviceType + ",url:(" + url + ")" + t.getMessage(), t);
                }
                if (invoker != null) { // Put new invoker in cache
                    newUrlInvokerMap.put(key, invoker);
                }
            } else {
                //加入缓存
                newUrlInvokerMap.put(key, invoker);
            }
        }
        keys.clear();
        return newUrlInvokerMap;
    }

    /**
     * Merge url parameters. the order is: override > -D >Consumer > Provider
     *
     * @param providerUrl
     * @return
     */
    private URL mergeUrl(URL providerUrl) {
        providerUrl = ClusterUtils.mergeUrl(providerUrl, queryMap); // Merge the consumer side parameters

        List<Configurator> localConfigurators = this.configurators; // local reference
        if (localConfigurators != null && !localConfigurators.isEmpty()) {
            for (Configurator configurator : localConfigurators) {
                providerUrl = configurator.configure(providerUrl);
            }
        }

        providerUrl = providerUrl.addParameter(Constants.CHECK_KEY, String.valueOf(false)); // Do not check whether the connection is successful or not, always create Invoker!

        // The combination of directoryUrl and override is at the end of notify, which can't be handled here
        this.overrideDirectoryUrl = this.overrideDirectoryUrl.addParametersIfAbsent(providerUrl.getParameters()); // Merge the provider side parameters

        if ((providerUrl.getPath() == null || providerUrl.getPath().length() == 0)
                && "dubbo".equals(providerUrl.getProtocol())) { // Compatible version 1.0
            //fix by tony.chenl DUBBO-44
            String path = directoryUrl.getParameter(Constants.INTERFACE_KEY);
            if (path != null) {
                int i = path.indexOf('/');
                if (i >= 0) {
                    path = path.substring(i + 1);
                }
                i = path.lastIndexOf(':');
                if (i >= 0) {
                    path = path.substring(0, i);
                }
                providerUrl = providerUrl.setPath(path);
            }
        }
        return providerUrl;
    }

    private List<Invoker<T>> route(List<Invoker<T>> invokers, String method) {
        Invocation invocation = new RpcInvocation(method, new Class<?>[0], new Object[0]);
        //获取routers路由
        List<Router> routers = getRouters();
        //路由不为Null
        if (routers != null) {
            for (Router router : routers) {
                // If router's url not null and is not route by runtime,we filter invokers here
                //如果路由器的url不为null并且不在运行时路由，则在此处过滤调用程序
                if (router.getUrl() != null && !router.getUrl().getParameter(Constants.RUNTIME_KEY, false)) {
                    invokers = router.route(invokers, getConsumerUrl(), invocation);
                }
            }
        }
        return invokers;
    }

    /**
     * Transform the invokers list into a mapping relationship with a method
     * 得到 <methodName, Invoker 列表> 映射关系
     * @param invokersMap Invoker Map
     * @return Mapping relation between Invoker and method
     */
    private Map<String, List<Invoker<T>>> toMethodInvokers(Map<String, Invoker<T>> invokersMap) {
        //方法名 与 invoker的列表
        Map<String, List<Invoker<T>>> newMethodInvokerMap = new HashMap<String, List<Invoker<T>>>();
        List<Invoker<T>> invokersList = new ArrayList<Invoker<T>>();
        //invokersMap的非空判断
        if (invokersMap != null && invokersMap.size() > 0) {
            //遍历invokers
            for (Invoker<T> invoker : invokersMap.values()) {
                //获取url中的methods属性
                String parameter = invoker.getUrl().getParameter(Constants.METHODS_KEY);
                //如果存在methods属性
                if (parameter != null && parameter.length() > 0) {
                    //使用,分割methods属性值
                    String[] methods = Constants.COMMA_SPLIT_PATTERN.split(parameter);
                    if (methods != null && methods.length > 0) {
                        //遍历methods
                        for (String method : methods) {
                            //method方法名称不为null并且不是*
                            if (method != null && method.length() > 0
                                    && !Constants.ANY_VALUE.equals(method)) {
                                //根据方法名获取 Invoker 列表
                                List<Invoker<T>> methodInvokers = newMethodInvokerMap.get(method);
                                if (methodInvokers == null) {
                                    methodInvokers = new ArrayList<Invoker<T>>();
                                    newMethodInvokerMap.put(method, methodInvokers);
                                }
                                //存储 Invoker 到列表中
                                methodInvokers.add(invoker);
                            }
                        }
                    }
                }
                invokersList.add(invoker);
            }
        }
        //服务级别路由
        List<Invoker<T>> newInvokersList = route(invokersList, null);
        //存储 <*, newInvokersList> 映射关系
        newMethodInvokerMap.put(Constants.ANY_VALUE, newInvokersList);
        //如果RegistryDirectory的serviceMethods属性不为空
        if (serviceMethods != null && serviceMethods.length > 0) {
            for (String method : serviceMethods) {
                List<Invoker<T>> methodInvokers = newMethodInvokerMap.get(method);
                if (methodInvokers == null || methodInvokers.isEmpty()) {
                    methodInvokers = newInvokersList;
                }
                newMethodInvokerMap.put(method, route(methodInvokers, method));
            }
        }
        // 排序，转成不可变列表
        for (String method : new HashSet<String>(newMethodInvokerMap.keySet())) {
            List<Invoker<T>> methodInvokers = newMethodInvokerMap.get(method);
            Collections.sort(methodInvokers, InvokerComparator.getComparator());
            newMethodInvokerMap.put(method, Collections.unmodifiableList(methodInvokers));
        }
        return Collections.unmodifiableMap(newMethodInvokerMap);
    }

    /**
     * Close all invokers
     */
    private void destroyAllInvokers() {
        Map<String, Invoker<T>> localUrlInvokerMap = this.urlInvokerMap; // local reference
        if (localUrlInvokerMap != null) {
            for (Invoker<T> invoker : new ArrayList<Invoker<T>>(localUrlInvokerMap.values())) {
                try {
                    invoker.destroy();
                } catch (Throwable t) {
                    logger.warn("Failed to destroy service " + serviceKey + " to provider " + invoker.getUrl(), t);
                }
            }
            localUrlInvokerMap.clear();
        }
        methodInvokerMap = null;
    }

    /**
     * Check whether the invoker in the cache needs to be destroyed
     * If set attribute of url: refer.autodestroy=false, the invokers will only increase without decreasing,there may be a refer leak
     *
     * 检查是否需要销毁缓存中的调用程序
     * 如果将url的set属性设置为：refer.autodestroy = false，则调用者只会增加而不减少，可能会导致引用泄漏
     * @param oldUrlInvokerMap
     * @param newUrlInvokerMap
     */
    private void destroyUnusedInvokers(Map<String, Invoker<T>> oldUrlInvokerMap, Map<String, Invoker<T>> newUrlInvokerMap) {
        if (newUrlInvokerMap == null || newUrlInvokerMap.size() == 0) {
            destroyAllInvokers();
            return;
        }
        // check deleted invoker
        // 检查哪些url的invoker需要被删除
        List<String> deleted = null;
        if (oldUrlInvokerMap != null) {
            //新创建的invokers
            Collection<Invoker<T>> newInvokers = newUrlInvokerMap.values();
            // 检测 newInvokers 中是否包含老的 Invoker ，老的invoker不在新的之中，加入到deleted集合中！
            for (Map.Entry<String, Invoker<T>> entry : oldUrlInvokerMap.entrySet()) {
                if (!newInvokers.contains(entry.getValue())) {
                    if (deleted == null) {
                        deleted = new ArrayList<String>();
                    }
                    deleted.add(entry.getKey());
                }
            }
        }
        //deleted集合不为null
        if (deleted != null) {
            for (String url : deleted) {
                if (url != null) {
                    //根据url获取到要销毁的invoker
                    Invoker<T> invoker = oldUrlInvokerMap.remove(url);
                    if (invoker != null) {
                        try {
                            //调用Invoker的detroy方法
                            invoker.destroy();
                            if (logger.isDebugEnabled()) {
                                logger.debug("destroy invoker[" + invoker.getUrl() + "] success. ");
                            }
                        } catch (Exception e) {
                            logger.warn("destroy invoker[" + invoker.getUrl() + "] faild. " + e.getMessage(), e);
                        }
                    }
                }
            }
        }
    }

    @Override
    public List<Invoker<T>> doList(Invocation invocation) {
        if (forbidden) {
            // 1. No service provider 2. Service providers are disabled
            throw new RpcException(RpcException.FORBIDDEN_EXCEPTION,
                    "No provider available from registry " + getUrl().getAddress() + " for service " + getConsumerUrl().getServiceKey() + " on consumer " + NetUtils.getLocalHost()
                            + " use dubbo version " + Version.getVersion() + ", please check status of providers(disabled, not registered or in blacklist).");
        }
        List<Invoker<T>> invokers = null;
        Map<String, List<Invoker<T>>> localMethodInvokerMap = this.methodInvokerMap; // local reference
        if (localMethodInvokerMap != null && localMethodInvokerMap.size() > 0) {
            String methodName = RpcUtils.getMethodName(invocation);
            Object[] args = RpcUtils.getArguments(invocation);
            if (args != null && args.length > 0 && args[0] != null
                    && (args[0] instanceof String || args[0].getClass().isEnum())) {
                invokers = localMethodInvokerMap.get(methodName + "." + args[0]); // The routing can be enumerated according to the first parameter
            }
            if (invokers == null) {
                invokers = localMethodInvokerMap.get(methodName);
            }
            if (invokers == null) {
                invokers = localMethodInvokerMap.get(Constants.ANY_VALUE);
            }
            if (invokers == null) {
                Iterator<List<Invoker<T>>> iterator = localMethodInvokerMap.values().iterator();
                if (iterator.hasNext()) {
                    invokers = iterator.next();
                }
            }
        }
        return invokers == null ? new ArrayList<Invoker<T>>(0) : invokers;
    }

    @Override
    public Class<T> getInterface() {
        return serviceType;
    }

    @Override
    public URL getUrl() {
        return this.overrideDirectoryUrl;
    }

    public URL getRegisteredConsumerUrl() {
        return registeredConsumerUrl;
    }

    public void setRegisteredConsumerUrl(URL registeredConsumerUrl) {
        this.registeredConsumerUrl = registeredConsumerUrl;
    }

    @Override
    public boolean isAvailable() {
        if (isDestroyed()) {
            return false;
        }
        Map<String, Invoker<T>> localUrlInvokerMap = urlInvokerMap;
        if (localUrlInvokerMap != null && localUrlInvokerMap.size() > 0) {
            for (Invoker<T> invoker : new ArrayList<Invoker<T>>(localUrlInvokerMap.values())) {
                if (invoker.isAvailable()) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Haomin: added for test purpose
     */
    public Map<String, Invoker<T>> getUrlInvokerMap() {
        return urlInvokerMap;
    }

    /**
     * Haomin: added for test purpose
     */
    public Map<String, List<Invoker<T>>> getMethodInvokerMap() {
        return methodInvokerMap;
    }

    private static class InvokerComparator implements Comparator<Invoker<?>> {

        private static final InvokerComparator comparator = new InvokerComparator();

        private InvokerComparator() {
        }

        public static InvokerComparator getComparator() {
            return comparator;
        }

        @Override
        public int compare(Invoker<?> o1, Invoker<?> o2) {
            return o1.getUrl().toString().compareTo(o2.getUrl().toString());
        }

    }

    /**
     * The delegate class, which is mainly used to store the URL address sent by the registry,and can be reassembled on the basis of providerURL queryMap overrideMap for re-refer.
     *
     * @param <T>
     */
    private static class InvokerDelegate<T> extends InvokerWrapper<T> {
        private URL providerUrl;

        public InvokerDelegate(Invoker<T> invoker, URL url, URL providerUrl) {
            super(invoker, url);
            this.providerUrl = providerUrl;
        }

        public URL getProviderUrl() {
            return providerUrl;
        }
    }
}
