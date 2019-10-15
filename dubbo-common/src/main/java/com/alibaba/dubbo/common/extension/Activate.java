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
 * 此注释对于根据给定条件自动激活某些扩展名很有用，例如：<code> @Activate </ code>可用于在有多个实现时加载某些
 * <code> Filter </ code>扩展名。
 * <ol>
 * <li>{@link Activate＃group（）}指定组条件。 框架SPI定义有效的组值。
 * <li>{@link Activate＃value（）}在{@link URL}条件中指定参数密钥。
 * </ol>
 * SPI提供者可以调用{@link ExtensionLoader＃getActivateExtension（URL，String，String）}来找出具有给定条件的所有激活的扩展。
 * @see SPI
 * @see URL
 * @see ExtensionLoader
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface Activate {
    /**
     * 当其中的一个group匹配时激活当前的扩展实现。通过{@link ExtensionLoader#getActivateExtension(URL, String, String)}
     * 传递的group参数将会被用来匹配
     *
     * @return group names to match
     * @see ExtensionLoader#getActivateExtension(URL, String, String)
     */
    String[] group() default {};

    /**
     * 当指定的键出现在URL的参数中时，激活当前扩展名。
     * <p>
     * 例如，给定<code> @Activate（“ cache，validation”）</ code>，
     * 仅当出现<code> cache </ code>或<code> validation </ code>键时，才返回当前扩展名 在网址的参数中。
     * </p>
     *
     * @return URL parameter keys
     * @see ExtensionLoader#getActivateExtension(URL, String)
     * @see ExtensionLoader#getActivateExtension(URL, String, String)
     */
    String[] value() default {};

    /**
     * Relative ordering info, optional
     *
     * @return extension list which should be put before the current one
     */
    String[] before() default {};

    /**
     * Relative ordering info, optional
     *
     * @return extension list which should be put after the current one
     */
    String[] after() default {};

    /**
     * Absolute ordering info, optional
     *
     * @return absolute ordering info
     */
    int order() default 0;
}