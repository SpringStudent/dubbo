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

package com.alibaba.dubbo.common.extension;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 扩展接口标记
 * <p/>
 * 扩展配置文件的更改<br/>
 * 以<code> Protocol </ code>为例，其配置文件'META-INF / dubbo / com.xxx.Protocol'从以下内容更改：<br/>
 * <pre>
 *     com.foo.XxxProtocol
 *     com.foo.YyyProtocol
 * </pre>
 * <p>
 * to key-value pair <br/>
 * <pre>
 *     xxx=com.foo.XxxProtocol
 *     yyy=com.foo.YyyProtocol
 * </pre>
 * <br/>
 * 发生此更改的原因是：
 * <p>
 * 如果在扩展实现中有静态字段或方法引用的第三方库，则如果第三方库不存在，则其类将无法初始化。
 * 在这种情况下，如果使用以前的格式，dubbo将无法找出扩展名，因此无法将扩展名映射到异常信息。
 * <p/>
 * 例如
 * <p>
 * 无法加载Extension（“ mina”）。 当用户配置为使用Mina时，dubbo将告知无法加载扩展，而不是报告哪个提取扩展实现失败和提取原因。
 * </p>
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface SPI {

    /**
     * default extension name
     * 默认扩展实现的名称
     */
    String value() default "";

}