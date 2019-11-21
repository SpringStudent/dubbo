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
package com.alibaba.dubbo.rpc.cluster.loadbalance;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.RpcStatus;

import java.util.List;
import java.util.Random;

/**
 * LeastActiveLoadBalance
 *
 */
public class LeastActiveLoadBalance extends AbstractLoadBalance {

    public static final String NAME = "leastactive";

    private final Random random = new Random();

    @Override
    protected <T> Invoker<T> doSelect(List<Invoker<T>> invokers, URL url, Invocation invocation) {
        //invokers数量
        int length = invokers.size();
        //最小活跃数
        int leastActive = -1;
        //具有最小活跃数的服务提供者(invoker)的数量
        int leastCount = 0;
        // leastIndexs 用于记录具有相同“最小活跃数”的 Invoker 在 invokers 列表中的下标信息
        int[] leastIndexs = new int[length];
        //预热权重的总和
        int totalWeight = 0;
        //初始值，用于比较
        int firstWeight = 0;
        //每个调用者都具有相同的权重值？
        boolean sameWeight = true;
        for (int i = 0; i < length; i++) {
            Invoker<T> invoker = invokers.get(i);
            //获取 Invoker 对应的活跃数
            int active = RpcStatus.getStatus(invoker.getUrl(), invocation.getMethodName()).getActive();
            //权重
            int afterWarmup = getWeight(invoker, invocation);
            //发现更小的活跃数，重新开始
            if (leastActive == -1 || active < leastActive) {
                //使用当前活跃数更新leastActive
                leastActive = active;
                //更新 leastCount 为 1
                leastCount = 1;
                //记录下标到leastIndex
                leastIndexs[0] = i;
                //totalWeight权重赋值
                totalWeight = afterWarmup;
                //记录第一个调用者的权重
                firstWeight = afterWarmup;
                //重置，每个调用者都具有相同的权重值？
                sameWeight = true;
                //当前 Invoker 的活跃数 active 与最小活跃数 leastActive 相同
            } else if (active == leastActive) {
                //在 leastIndexs 中记录下当前 Invoker 在 invokers 集合中的下标
                leastIndexs[leastCount++] = i;
                //累加权重
                totalWeight += afterWarmup;
                //检测当前 Invoker 的权重与 firstWeight 是否相等，
                //不相等则将 sameWeight 置为 false
                if (sameWeight && i > 0
                        && afterWarmup != firstWeight) {
                    sameWeight = false;
                }
            }
        }
        // 断言（leastCount> 0）
        if (leastCount == 1) {
            // 如果我们恰好有一个具有最小活动值的调用程序，则直接返回此调用程序。
            return invokers.get(leastIndexs[0]);
        }
        //有多个 Invoker 具有相同的最小活跃数，但它们之间的权重不同
        if (!sameWeight && totalWeight > 0) {
            // 如果（并非每个调用者都具有相同的权重且至少一个调用者的权重大于0），请根据totalWeight随机选择。
            int offsetWeight = random.nextInt(totalWeight) + 1;
            //循环让随机数减去具有最小活跃数的 Invoker 的权重值，
            // 当 offset 小于等于0时，返回相应的 Invoker
            for (int i = 0; i < leastCount; i++) {
                int leastIndex = leastIndexs[i];
                offsetWeight -= getWeight(invokers.get(leastIndex), invocation);
                if (offsetWeight <= 0)
                    return invokers.get(leastIndex);
            }
        }
        // 如果权重相同或权重为0时，则从leastIndexes中返回一个 Invoker
        return invokers.get(leastIndexs[random.nextInt(leastCount)]);
    }
}
