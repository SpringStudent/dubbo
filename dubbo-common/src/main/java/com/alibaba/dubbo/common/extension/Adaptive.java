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

import com.alibaba.dubbo.common.URL;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Provide helpful information for {@link ExtensionLoader} to inject dependency extension instance.
 *
 * 为{@link ExtensionLoader}提供有用的信息以注入依赖项扩展实例。
 * @see ExtensionLoader
 * @see URL
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface Adaptive {
    /**
     * 确定要注入的目标扩展目标扩展名由URL中传递的参数决定，参数名称由此方法指定。
     * <p>
     * 如果未从{@link URL}中找到指定的参数，则将使用默认扩展名进行依赖项注入（在其接口的{@link SPI}中指定）。
     * <p>
     * 例如，给定<code> String [] {“ key1”，“ key2”} </ code>：
     * <ol>
     * <li>在网址中找到参数“ key1”，将其值用作扩展名</li>
     * <li>如果在URL中找不到“ key1”（或其值为空），请尝试使用“ key2”作为扩展名</li>
     * <li>如果“ key2”也未出现，请使用默认扩展名</li>
     * <li>否则，抛出{@link IllegalStateException}</li>
     * </ol>
     * 如果没有在接口的{@link SPI}上提供默认扩展名，则使用以下规则从接口的类名生成一个名称：将大写字符的类名分成几部分，
     * 并用点号“。”分开，例如： 对于{@code com.alibaba.dubbo.xxx.YyyInvokerWrapper},其默认名称为
     * <code> String [] {“ yyy.invoker.wrapper”} </ code>。 此名称将用于从URL搜索参数。
     * @return parameter key names in URL
     */
    String[] value() default {};

}