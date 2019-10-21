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

import com.alibaba.dubbo.config.annotation.Argument;
import com.alibaba.dubbo.config.support.Parameter;

import java.io.Serializable;

/**
 * 该标签为 <dubbo:method> 的子标签，用于方法参数的特征描述，比如：
 * <dubbo:method name="findXxx" timeout="3000" retries="2">
 *     <dubbo:argument index="0" callback="true" />
 * </dubbo:method>
 */
public class ArgumentConfig implements Serializable {

    private static final long serialVersionUID = -2165482463925213595L;

    //argument: index -1 represents not set
    /**
     * 参数索引
     */
    private Integer index = -1;

    //argument type
    /**
     * 通过参数类型查找参数的index
     */
    private String type;

    //callback interface
    /**
     * 参数是否为callback接口，如果为callback，服务提供方将生成反向代理，
     * 可以从服务提供方反向调用消费方，通常用于事件推送.
     */
    private Boolean callback;

    public ArgumentConfig() {
    }

    public ArgumentConfig(Argument argument) {
        this.index = argument.index();
        this.type = argument.type();
        this.callback = argument.callback();
    }

    @Parameter(excluded = true)
    public Integer getIndex() {
        return index;
    }

    public void setIndex(Integer index) {
        this.index = index;
    }

    @Parameter(excluded = true)
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setCallback(Boolean callback) {
        this.callback = callback;
    }

    public Boolean isCallback() {
        return callback;
    }

}
