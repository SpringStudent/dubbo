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
package com.alibaba.dubbo.registry;

import com.alibaba.dubbo.common.URL;

import java.util.List;

/**
 * RegistryService. (SPI, Prototype, ThreadSafe)
 *
 * @see com.alibaba.dubbo.registry.Registry
 * @see com.alibaba.dubbo.registry.RegistryFactory#getRegistry(URL)
 */
public interface RegistryService {

    /**
     * 注册数据，例如：提供者服务，使用者地址，路由规则，覆盖规则和其他数据。
     * <p>
     * 注册需要支持如下规则<br>
     * 1. URL设置check = false参数时。注册失败，不会抛出异常而会在后台重试。否则将会抛出异常<br>
     * 2. URL设置dynamic = false参数时，他需要被永久存储，否则当注册着异常退出，他应该被删除<br>
     * 3. URL设置category=routers，这意味分类存储，默认类型为providers，数据将会被分类部分通知<br>
     * 4. 当注册中心被重启了，比如网络抖动，数据不会丢失，包括自动从broken line处删除数据<br>
     * 5. 允许具有相同URL但不同参数的URL共存，它们不能相互覆盖。<br>
     *
     * @param url  注册信息，不允许为空， e.g: dubbo://10.20.153.10/com.alibaba.foo.BarService?version=1.0.0&application=kylin
     */
    void register(URL url);

    /**取消注册
     *
     * <p>
     * 取消注册被要求支持如下规则<br>
     * 1. 由于设置了dynamic = false存储的属于，当找不到注册信息数据，将会抛出异常，其他情况将会忽略<br>
     * 2. 根据完整的网址匹配取消注册。<br>
     *
     * @param url 注册信息，不允许为空， e.g: dubbo://10.20.153.10/com.alibaba.foo.BarService?version=1.0.0&application=kylin
     */
    void unregister(URL url);

    /**
     * 订阅合适的注册数据，当注册过的数据修改时，自动推送
     * <p>
     * 订阅需要遵循的规则<br>
     * 1. URL设置check = false参数时。 注册失败时，不会在后台引发异常并重试该异常。<br>
     * 2. 当URL设置了 category=routers，将会通知指定类型的数据。多个分类用逗号分隔，并允许星号匹配，这表示已订阅所有分类数据。<br>
     * 3. 允许将接口，组，版本和分类器作为条件查询，例如：interface = com.alibaba.foo.BarService＆version = 1.0.0<br>
     * 4. 查询条件允许星号匹配，订阅所有接口的所有数据包的所有版本，例如 ：interface = *＆group = *＆version = *＆classifier = *<br>
     * 5. 当注册表重新启动并且出现网络抖动时，有必要自动恢复订阅请求。<br>
     * 6. 允许具有相同URL但不同参数的URL共存，它们不能相互覆盖。<br>
     * 7. 当第一个通知完成并且返回后，订阅程序必须被阻塞<br>
     *
     * @param url      订阅条件，不允许为空， e.g. consumer://10.20.153.10/com.alibaba.foo.BarService?version=1.0.0&application=kylin
     * @param listener 事件变化的监听器，不允许为空
     */
    void subscribe(URL url, NotifyListener listener);

    /**
     * 取消订阅
     * <p>
     * 取消订阅要遵循的规则<br>
     * 1. 如果没有订阅，则直接忽略它。<br>
     * 2. 取消订阅完整的URL匹配。<br>
     *
     * @param url      Subscription condition, not allowed to be empty, e.g. consumer://10.20.153.10/com.alibaba.foo.BarService?version=1.0.0&application=kylin
     * @param listener A listener of the change event, not allowed to be empty
     */
    void unsubscribe(URL url, NotifyListener listener);

    /**
     * Query the registered data that matches the conditions. Corresponding to the push mode of the subscription, this is the pull mode and returns only one result.
     *
     * 查找匹配条件的注册过的数据.对于订阅的推送模式，这是请求模式将会返回一个结果
     * @param url Query condition, is not allowed to be empty, e.g. consumer://10.20.153.10/com.alibaba.foo.BarService?version=1.0.0&application=kylin
     * @return  注册信息列表，可以为空，含义与参数相同{@link com.alibaba.dubbo.registry.NotifyListener#notify(List<URL>)}.
     * @see com.alibaba.dubbo.registry.NotifyListener#notify(List)
     */
    List<URL> lookup(URL url);

}