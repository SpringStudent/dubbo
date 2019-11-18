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
package com.alibaba.dubbo.rpc.cluster.router.condition;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.common.utils.NetUtils;
import com.alibaba.dubbo.common.utils.StringUtils;
import com.alibaba.dubbo.common.utils.UrlUtils;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.RpcException;
import com.alibaba.dubbo.rpc.cluster.Router;
import com.alibaba.dubbo.rpc.cluster.router.AbstractRouter;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ConditionRouter
 */
public class ConditionRouter extends AbstractRouter {

    private static final Logger logger = LoggerFactory.getLogger(ConditionRouter.class);
    private static final int DEFAULT_PRIORITY = 2;
    /**
     *分组正则匹配
     */
    private static Pattern ROUTE_PATTERN = Pattern.compile("([&!=,]*)\\s*([^&!=,\\s]+)");
    /**
     * 当路由结果为空时，是否强制执行，如果不强制执行，路由结果为空的路由规则将自动失效，可不填，缺省为 false 。
     */
    private final boolean force;
    /**
     * 消费者匹配条件集合，通过解析【条件表达式 rule 的 `=>` 之前半部分】
     */
    private final Map<String, MatchPair> whenCondition;
    /**
     * 提供者地址列表的过滤条件，通过解析【条件表达式 rule 的 `=>` 之后半部分】
     */
    private final Map<String, MatchPair> thenCondition;

    public ConditionRouter(URL url) {
        this.url = url;
        //获取url中的priority属性
        this.priority = url.getParameter(Constants.PRIORITY_KEY, DEFAULT_PRIORITY);
        //获取force属性
        this.force = url.getParameter(Constants.FORCE_KEY, false);
        try {
            //获取rule属性
            String rule = url.getParameterAndDecoded(Constants.RULE_KEY);
            if (rule == null || rule.trim().length() == 0) {
                throw new IllegalArgumentException("Illegal route rule!");
            }
            //替换rule字符串中的consumer.属性和provider.属性
            rule = rule.replace("consumer.", "").replace("provider.", "");
            int i = rule.indexOf("=>");
            //分割消费者和提供者规则
            String whenRule = i < 0 ? null : rule.substring(0, i).trim();
            String thenRule = i < 0 ? rule.trim() : rule.substring(i + 2).trim();
            Map<String, MatchPair> when = StringUtils.isBlank(whenRule) || "true".equals(whenRule) ? new HashMap<String, MatchPair>() : parseRule(whenRule);
            Map<String, MatchPair> then = StringUtils.isBlank(thenRule) || "false".equals(thenRule) ? null : parseRule(thenRule);
            // 注意：应在业务级别上确定“何时条件”是否可以为空。
            this.whenCondition = when;
            this.thenCondition = then;
        } catch (ParseException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    /**
     * 结束后返回类似如下格式的字符串
     * {
     *     "host": {
     *         "matches": ["2.2.2.2"],
     *         "mismatches": ["1.1.1.1"]
     *     },
     *     "method": {
     *         "matches": ["hello"],
     *         "mismatches": ["echo"]
     *     }
     * }
     */
    private static Map<String, MatchPair> parseRule(String rule)
            throws ParseException {
        //空字符串返回空的Map
        Map<String, MatchPair> condition = new HashMap<String, MatchPair>();
        if (StringUtils.isBlank(rule)) {
            return condition;
        }
        // Key-Value pair, stores both match and mismatch conditions
        MatchPair pair = null;
        // Multiple values
        Set<String> values = null;
        //通过正则表达式匹配路由规则"([&!=,]*)\\s*([^&!=,\\s]+)"
        //第一个括号内的表达式用于匹配"&", "!", "=" 和 "," 等符号
        //第二括号内的用于匹配英文字母，数字等字符。举个例子说明一下：
        //host = 2.2.2.2 & host != 1.1.1.1 & method = hello
        // 匹配结果如下：
        //     括号一      括号二
        // 1.  null       host
        // 2.   =         2.2.2.2
        // 3.   &         host
        // 4.   !=        1.1.1.1
        // 5.   &         method
        // 6.   =         hello
        final Matcher matcher = ROUTE_PATTERN.matcher(rule);
        //正则匹配
        while (matcher.find()) {
            //获取括号1内容匹配的结果
            String separator = matcher.group(1);
            //获取括号2内容匹配结果
            String content = matcher.group(2);
            // Start part of the condition expression.
            //分隔符为空，表示匹配的是表达式的开始部分
            if (separator == null || separator.length() == 0) {
                //创建MatchPair对象
                pair = new MatchPair();
                // 存储 <匹配项, MatchPair> 键值对，比如 <host, MatchPair>
                condition.put(content, pair);
            }
            // The KV part of the condition expression
            // 如果分隔符为 &，表明接下来也是一个条件
            else if ("&".equals(separator)) {
                //如果&是null，创建PatchPair
                if (condition.get(content) == null) {
                    pair = new MatchPair();
                    condition.put(content, pair);
                } else {
                    pair = condition.get(content);
                }
            }
            // The Value in the KV part.
            // 如果分隔符为=
            else if ("=".equals(separator)) {
                if (pair == null)
                    throw new ParseException("Illegal route rule \""
                            + rule + "\", The error char '" + separator
                            + "' at index " + matcher.start() + " before \""
                            + content + "\".", matcher.start());

                values = pair.matches;
                // 将 content 存入到 MatchPair 的 matches 集合中
                values.add(content);
            }
            // The Value in the KV part.
            // 分隔符为!=
            else if ("!=".equals(separator)) {
                if (pair == null)
                    throw new ParseException("Illegal route rule \""
                            + rule + "\", The error char '" + separator
                            + "' at index " + matcher.start() + " before \""
                            + content + "\".", matcher.start());
                values = pair.mismatches;
                //// 将 content 存入到 MatchPair 的 mismatches 集合中
                values.add(content);
            }
            // The Value in the KV part, if Value have more than one items.
            // 分隔符为 ,
            else if (",".equals(separator)) { // Should be seperateed by ','
                if (values == null || values.isEmpty())
                    throw new ParseException("Illegal route rule \""
                            + rule + "\", The error char '" + separator
                            + "' at index " + matcher.start() + " before \""
                            + content + "\".", matcher.start());
                // 将 content 存入到上一步获取到的 values 中，可能是 matches，也可能是 mismatches
                values.add(content);
            } else {
                throw new ParseException("Illegal route rule \"" + rule
                        + "\", The error char '" + separator + "' at index "
                        + matcher.start() + " before \"" + content + "\".", matcher.start());
            }
        }
        return condition;
    }

    @Override
    public <T> List<Invoker<T>> route(List<Invoker<T>> invokers, URL url, Invocation invocation)
            throws RpcException {
        if (invokers == null || invokers.isEmpty()) {
            return invokers;
        }
        try {
            // 先对服务消费者条件进行匹配，如果匹配失败，表明当前消费者 url 不符合匹配规则，
            // 无需进行后续匹配，直接返回 Invoker 列表即可。比如下面的规则：
            // host = 10.20.153.10 => host = 10.0.0.10
            // 这条路由规则希望 IP 为 10.20.153.10 的服务消费者调用 IP 为 10.0.0.10 机器上的服务。
            // 当消费者 ip 为 10.20.153.11 时，matchWhen 返回 false，表明当前这条路由规则不适用于
            // 当前的服务消费者，此时无需再进行后续匹配，直接返回即可。
            // 代表需要进行路由规则！！！
            if (!matchWhen(url, invocation)) {
                return invokers;
            }
            List<Invoker<T>> result = new ArrayList<Invoker<T>>();
            //当服务提供者匹配条件未配置，表明对指定服务消费者禁用服务，即加入黑名单了
            if (thenCondition == null) {
                logger.warn("The current consumer in the service blacklist. consumer: " + NetUtils.getLocalHost() + ", service: " + url.getServiceKey());
                return result;
            }
            //这里把Invoker理解为服务提供者，现在使用服务消费者规则对Invoker列表进行匹配
            for (Invoker<T> invoker : invokers) {
                // 匹配成功，表明当前 Invoker 符合服务提供者匹配规则。
                // 此时将 Invoker 添加到 result 列表中
                if (matchThen(invoker.getUrl(), url)) {
                    result.add(invoker);
                }
            }
            // 返回匹配结果，如果 result 为空列表，且 force = true，表示强制返回空列表，
            // 否则路由结果为空的路由规则将自动失效
            if (!result.isEmpty()) {
                return result;
            } else if (force) {
                logger.warn("The route result is empty and force execute. consumer: " + NetUtils.getLocalHost() + ", service: " + url.getServiceKey() + ", router: " + url.getParameterAndDecoded(Constants.RULE_KEY));
                return result;
            }
        } catch (Throwable t) {
            logger.error("Failed to execute condition router rule: " + getUrl() + ", invokers: " + invokers + ", cause: " + t.getMessage(), t);
        }
        return invokers;
    }

    @Override
    public URL getUrl() {
        return url;
    }

    @Override
    public int compareTo(Router o) {
        if (o == null || o.getClass() != ConditionRouter.class) {
            return 1;
        }
        ConditionRouter c = (ConditionRouter) o;
        return this.priority == c.priority ? url.toFullString().compareTo(c.url.toFullString()) : (this.priority > c.priority ? 1 : -1);
    }

    boolean matchWhen(URL url, Invocation invocation) {
        //=>host!=1.23.123需要走路由规则所以返回true
        //whenCondition url为consumer://xxx  parama为null
        return whenCondition == null || whenCondition.isEmpty() || matchCondition(whenCondition, url, null, invocation);
    }

    private boolean matchThen(URL url, URL param) {
        return !(thenCondition == null || thenCondition.isEmpty()) && matchCondition(thenCondition, url, param, null);
    }

    private boolean matchCondition(Map<String, MatchPair> condition, URL url, URL param, Invocation invocation) {
        //服务提供者或者消费url转换成map
        Map<String, String> sample = url.toMap();
        boolean result = false;
        //遍历condition
        for (Map.Entry<String, MatchPair> matchPair : condition.entrySet()) {
            //key比如host ,method
            String key = matchPair.getKey();
            String sampleValue;
            //如果invocation不为null并且 key为method(s)
            if (invocation != null && (Constants.METHOD_KEY.equals(key) || Constants.METHODS_KEY.equals(key))) {
                //获取调用方法名称
                sampleValue = invocation.getMethodName();
            } else {
                //从服务提供者或消费者 url 中获取指定字段值，比如 host、application 等
                sampleValue = sample.get(key);
                if (sampleValue == null) {
                    // 尝试通过 default.xxx 获取相应的值
                    sampleValue = sample.get(Constants.DEFAULT_KEY_PREFIX + key);
                }
            }
            //如果sampleValue不为null
            if (sampleValue != null) {
                //调用matchPair的isMatch匹配
                if (!matchPair.getValue().isMatch(sampleValue, param)) {
                    // 只要有一个规则匹配失败，立即返回 false 结束方法逻辑
                    return false;
                } else {
                    result = true;
                }
            } else {
                // sampleValue 为空，表明服务提供者或消费者 url 中不包含相关字段。此时如果
                //  MatchPair 的 matches 不为空，表示匹配失败，返回 false。比如我们有这样
                //  一条匹配条件 loadbalance = random，假设 url 中并不包含 loadbalance 参数，
                //  此时 sampleValue = null。既然路由规则里限制了 loadbalance = random，
                //  但 sampleValue = null，明显不符合规则，因此返回 false
                if (!matchPair.getValue().matches.isEmpty()) {
                    return false;
                } else {
                    result = true;
                }
            }
        }
        return result;
    }

    private static final class MatchPair {

        final Set<String> matches = new HashSet<String>();
        final Set<String> mismatches = new HashSet<String>();

        private boolean isMatch(String value, URL param) {
            //matches非空 mismatches为空
            if (!matches.isEmpty() && mismatches.isEmpty()) {
                // 遍历 matches 集合，检测入参 value 是否能被 matches 集合元素匹配到。
                // 举个例子，如果 value = 10.20.153.11，matches = [10.20.153.*],
                // 此时 isMatchGlobPattern 方法返回 true
                for (String match : matches) {
                    if (UrlUtils.isMatchGlobPattern(match, value, param)) {
                        return true;
                    }
                }
                // 如果所有匹配项都无法匹配到入参，则返回 false
                return false;
            }
            //如果matches为空，mismatches非空
            if (!mismatches.isEmpty() && matches.isEmpty()) {
                // 只要入参被 mismatches 集合中的任意一个元素匹配到，就返回 false
                for (String mismatch : mismatches) {
                    if (UrlUtils.isMatchGlobPattern(mismatch, value, param)) {
                        return false;
                    }
                }
                return true;
            }
            //matches和mismatches都不为空
            if (!matches.isEmpty() && !mismatches.isEmpty()) {
                // matches 和 mismatches 均为非空，此时优先使用 mismatches 集合元素对入参进行匹配。
                // 只要 mismatches 集合中任意一个元素与入参匹配成功，就立即返回 false，结束方法逻辑
                for (String mismatch : mismatches) {
                    if (UrlUtils.isMatchGlobPattern(mismatch, value, param)) {
                        return false;
                    }
                }
                // mismatches 集合元素无法匹配到入参，此时使用 matches 继续匹配
                for (String match : matches) {
                    if (UrlUtils.isMatchGlobPattern(match, value, param)) {
                        return true;
                    }
                }
                return false;
            }
            return false;
        }
    }
}
