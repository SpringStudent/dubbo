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
package com.alibaba.dubbo.config.spring.schema;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.extension.ExtensionLoader;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.common.utils.ReflectUtils;
import com.alibaba.dubbo.common.utils.StringUtils;
import com.alibaba.dubbo.config.ArgumentConfig;
import com.alibaba.dubbo.config.ConsumerConfig;
import com.alibaba.dubbo.config.MethodConfig;
import com.alibaba.dubbo.config.ProtocolConfig;
import com.alibaba.dubbo.config.ProviderConfig;
import com.alibaba.dubbo.config.RegistryConfig;
import com.alibaba.dubbo.config.spring.ReferenceBean;
import com.alibaba.dubbo.config.spring.ServiceBean;
import com.alibaba.dubbo.rpc.Protocol;

import org.springframework.beans.PropertyValue;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.config.TypedStringValue;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * AbstractBeanDefinitionParser
 *
 * @export
 */
public class DubboBeanDefinitionParser implements BeanDefinitionParser {

    private static final Logger logger = LoggerFactory.getLogger(DubboBeanDefinitionParser.class);
    private static final Pattern GROUP_AND_VERION = Pattern.compile("^[\\-.0-9_a-zA-Z]+(\\:[\\-.0-9_a-zA-Z]+)?$");
    private final Class<?> beanClass;
    private final boolean required;

    public DubboBeanDefinitionParser(Class<?> beanClass, boolean required) {
        this.beanClass = beanClass;
        this.required = required;
    }

    @SuppressWarnings("unchecked")
    private static BeanDefinition parse(Element element, ParserContext parserContext, Class<?> beanClass, boolean required) {
        RootBeanDefinition beanDefinition = new RootBeanDefinition();
        beanDefinition.setBeanClass(beanClass);
        beanDefinition.setLazyInit(false);
        //获取id属性
        String id = element.getAttribute("id");
        //如果未配置id并且id为必须项
        if ((id == null || id.length() == 0) && required) {
            //获得name属性
            String generatedBeanName = element.getAttribute("name");
            if (generatedBeanName == null || generatedBeanName.length() == 0) {
                //如果是ProtocolConfig类型
                if (ProtocolConfig.class.equals(beanClass)) {
                    //配置名称为"dubbo"
                    generatedBeanName = "dubbo";
                } else {
                    //获取interface属性配置
                    generatedBeanName = element.getAttribute("interface");
                }
            }
            //使用beanClass名称作为generatedBeanName
            if (generatedBeanName == null || generatedBeanName.length() == 0) {
                generatedBeanName = beanClass.getName();
            }
            //给id 赋值
            id = generatedBeanName;
            int counter = 2;
            //如果spring容器中包含id名称的bean重新生成
            while (parserContext.getRegistry().containsBeanDefinition(id)) {
                id = generatedBeanName + (counter++);
            }
        }
        //目测只有RerenceConfig没有id了吧
        if (id != null && id.length() > 0) {
            //bean重复判断
            if (parserContext.getRegistry().containsBeanDefinition(id)) {
                throw new IllegalStateException("Duplicate spring bean id " + id);
            }
            //向BeanDefinitionRegistry上注册bean的定义！
            parserContext.getRegistry().registerBeanDefinition(id, beanDefinition);
            //统一的添加id属性
            beanDefinition.getPropertyValues().addPropertyValue("id", id);
        }
        if (ProtocolConfig.class.equals(beanClass)) {
            /**
             * 更新属性有protocol的所有bean的protocol属性
             */
            for (String name : parserContext.getRegistry().getBeanDefinitionNames()) {
                BeanDefinition definition = parserContext.getRegistry().getBeanDefinition(name);
                PropertyValue property = definition.getPropertyValues().getPropertyValue("protocol");
                if (property != null) {
                    Object value = property.getValue();
                    if (value instanceof ProtocolConfig && id.equals(((ProtocolConfig) value).getName())) {
                        definition.getPropertyValues().addPropertyValue("protocol", new RuntimeBeanReference(id));
                    }
                }
            }
        } else if (ServiceBean.class.equals(beanClass)) {
            /**
             * 解析<dubbo:service
             */
            //如果class属性不为null
            String className = element.getAttribute("class");

            /**
             *                      解析子节点，有些配置可能是：
             *                     <dubbo:service class="com.alihealth.dubbo.api.drugInfo.service.DemoService"
             *                          executes="10" >
             *                         <property  ref="demoService" name="ref"></property>
             *                         <property  value="1.0.0" name="version"></property>
             *                      </dubbo:service>
             */
            if (className != null && className.length() > 0) {
                RootBeanDefinition classDefinition = new RootBeanDefinition();
                classDefinition.setBeanClass(ReflectUtils.forName(className));
                classDefinition.setLazyInit(false);
                parseProperties(element.getChildNodes(), classDefinition);
                //添加了个ref属性 其值为classDefinition
                beanDefinition.getPropertyValues().addPropertyValue("ref", new BeanDefinitionHolder(classDefinition, id + "Impl"));
            }
        } else if (ProviderConfig.class.equals(beanClass)) {
            /**
             * <dubbo:provider 配置解析，可以解析类似如下
             *     <dubbo:provider id="default_provider" registry="dubbo_registry" timeout="60000">
             *         <dubbo:service interface="com.xxx.XxxService"></dubbo:service>
             *         <dubbo:service interface="com.xxx.XxxService"></dubbo:service>
             *     </dubbo:provider>
             */
            parseNested(element, parserContext, ServiceBean.class, true, "service", "provider", id, beanDefinition);
        } else if (ConsumerConfig.class.equals(beanClass)) {
            /**
             * <dubbo:consumer 配置解析,可以解析类似如下
             *     <dubbo:consumer registry="dubbo_registry" check="false" timeout="60000" >
             *         <dubbo:reference interface=""/>
             *         <dubbo:reference interface=""/>
             *     </dubbo:consumer>
             */
            parseNested(element, parserContext, ReferenceBean.class, false, "reference", "consumer", id, beanDefinition);
        }
        //属性
        Set<String> props = new HashSet<String>();
        ManagedMap parameters = null;
        //获取当前beanClass的所有方法
        for (Method setter : beanClass.getMethods()) {
            String name = setter.getName();
            //给如ServiceConfig注入值时只能注入set方法
            if (name.length() > 3 && name.startsWith("set")
                    && Modifier.isPublic(setter.getModifiers())
                    && setter.getParameterTypes().length == 1) {
                //set方法的参数类型
                Class<?> type = setter.getParameterTypes()[0];
                //截取属性名称比如setProtocol这里截取到的就是protocol
                String propertyName = name.substring(3, 4).toLowerCase() + name.substring(4);
                String property = StringUtils.camelToSplitName(propertyName, "-");
                //添加到props属性集合
                props.add(property);
                Method getter = null;
                try {
                    //该属性的get方法
                    getter = beanClass.getMethod("get" + name.substring(3), new Class<?>[0]);
                } catch (NoSuchMethodException e) {
                    try {
                        //boolean类型的属性的get方法可能是isDefault这种的
                        getter = beanClass.getMethod("is" + name.substring(3), new Class<?>[0]);
                    } catch (NoSuchMethodException e2) {
                    }
                }
                //如果没有getter方法或者getter方法不是public或者没有返回值，进入下一次循环
                if (getter == null
                        || !Modifier.isPublic(getter.getModifiers())
                        || !type.equals(getter.getReturnType())) {
                    continue;
                }

                if ("parameters".equals(property)) {
                    /**
                     *
                     *
                     *  如果有parameters这样子的属性需要解析类似的配置
                     *  <dubbo:protocol name="jms">
                     *     <dubbo:parameter key="queue" value="your_queue" />
                     * </dubbo:protocol>
                     *
                     */
                    parameters = parseParameters(element.getChildNodes(), beanDefinition);

                } else if ("methods".equals(property)) {
                    /**
                     * 如果有methods这样子的属性需要解析类似
                     * <dubbo:reference interface="com.xxx.XxxService">
                     *      <dubbo:method name="methodXXX" timeout="2000"/>
                     * </dubbo:reference>
                     */
                    parseMethods(id, element.getChildNodes(), beanDefinition, parserContext);
                } else if ("arguments".equals(property)) {
                    /**
                     * 如果有arguments属性需要解析类似
                     * <dubbo:method name="findXxx" timeout="3000" retries="2">
                     *     <dubbo:argument index="0" callback="true" />
                     * </dubbo:method>
                     *
                     */
                    parseArguments(id, element.getChildNodes(), beanDefinition, parserContext);
                } else {
                    //获取element的property的属性值
                    String value = element.getAttribute(property);
                    if (value != null) {
                        value = value.trim();
                        if (value.length() > 0) {
                            //如果属性值是"N/A"表示不发布到任何注册中心!
                            if ("registry".equals(property) && RegistryConfig.NO_AVAILABLE.equalsIgnoreCase(value)) {
                                RegistryConfig registryConfig = new RegistryConfig();
                                registryConfig.setAddress(RegistryConfig.NO_AVAILABLE);
                                beanDefinition.getPropertyValues().addPropertyValue(property, registryConfig);
                            } else if ("registry".equals(property) && value.indexOf(',') != -1) {
                                //如果是多个","分隔的注册中心！
                                parseMultiRef("registries", value, beanDefinition, parserContext);
                            } else if ("provider".equals(property) && value.indexOf(',') != -1) {
                                //如果是多个","分隔的提供者
                                parseMultiRef("providers", value, beanDefinition, parserContext);
                            } else if ("protocol".equals(property) && value.indexOf(',') != -1) {
                                //多个","分隔的协议暴露
                                parseMultiRef("protocols", value, beanDefinition, parserContext);
                            } else {
                                Object reference;
                                //基本类型的
                                if (isPrimitive(type)) {
                                    if ("async".equals(property) && "false".equals(value)
                                            || "timeout".equals(property) && "0".equals(value)
                                            || "delay".equals(property) && "0".equals(value)
                                            || "version".equals(property) && "0.0.0".equals(value)
                                            || "stat".equals(property) && "-1".equals(value)
                                            || "reliable".equals(property) && "false".equals(value)) {
                                        // backward compatibility for the default value in old version's xsd
                                        value = null;
                                    }
                                    /**
                                     * 兼容旧版本xsd中的default值,以上配置的值在xsd中有配置defalt值
                                     *  <xsd:attribute name="version" type="xsd:string" use="optional" default="0.0.0">
                                     */
                                    reference = value;
                                } else if ("protocol".equals(property)
                                        //如果属性为 protocol 那么要判断protocol对应的拓展点配置有没有
                                        && ExtensionLoader.getExtensionLoader(Protocol.class).hasExtension(value)
                                        //检查当前使用的协议是否已经解析过 可能在这里被解析过<dubbo:protocol name="dubbo"
                                        && (!parserContext.getRegistry().containsBeanDefinition(value)
                                        || !ProtocolConfig.class.getName().equals(parserContext.getRegistry().getBeanDefinition(value).getBeanClassName()))) {
                                    if ("dubbo:provider".equals(element.getTagName())) {
                                        logger.warn("Recommended replace <dubbo:provider protocol=\"" + value + "\" ... /> to <dubbo:protocol name=\"" + value + "\" ... />");
                                    }
                                    // backward compatibility
                                    ProtocolConfig protocol = new ProtocolConfig();
                                    protocol.setName(value);
                                    reference = protocol;
                                } else if ("onreturn".equals(property)) {
                                    //方法返回后执行
                                    int index = value.lastIndexOf(".");
                                    String returnRef = value.substring(0, index);
                                    String returnMethod = value.substring(index + 1);
                                    reference = new RuntimeBeanReference(returnRef);
                                    beanDefinition.getPropertyValues().addPropertyValue("onreturnMethod", returnMethod);
                                } else if ("onthrow".equals(property)) {
                                    //回调异常执行的方法
                                    int index = value.lastIndexOf(".");
                                    String throwRef = value.substring(0, index);
                                    String throwMethod = value.substring(index + 1);
                                    reference = new RuntimeBeanReference(throwRef);
                                    beanDefinition.getPropertyValues().addPropertyValue("onthrowMethod", throwMethod);
                                } else if ("oninvoke".equals(property)) {
                                    //方法执行前拦截
                                    int index = value.lastIndexOf(".");
                                    String invokeRef = value.substring(0, index);
                                    String invokeRefMethod = value.substring(index + 1);
                                    reference = new RuntimeBeanReference(invokeRef);
                                    beanDefinition.getPropertyValues().addPropertyValue("oninvokeMethod", invokeRefMethod);
                                } else {
                                    if ("ref".equals(property) && parserContext.getRegistry().containsBeanDefinition(value)) {
                                        BeanDefinition refBean = parserContext.getRegistry().getBeanDefinition(value);
                                        //ref的值必须为单例模式
                                        if (!refBean.isSingleton()) {
                                            throw new IllegalStateException("The exported service ref " + value + " must be singleton! Please set the " + value + " bean scope to singleton, eg: <bean id=\"" + value + "\" scope=\"singleton\" ...>");
                                        }
                                    }
                                    //声明为运行时类引用
                                    reference = new RuntimeBeanReference(value);
                                }
                                beanDefinition.getPropertyValues().addPropertyValue(propertyName, reference);
                            }
                        }
                    }
                }
            }
        }
        //获取element的所有属性信息
        //所有剩余不认识的属性配置均认为是parameters，可有意见
        NamedNodeMap attributes = element.getAttributes();
        int len = attributes.getLength();
        for (int i = 0; i < len; i++) {
            Node node = attributes.item(i);
            String name = node.getLocalName();
            //剩余未解析的属性
            if (!props.contains(name)) {
                if (parameters == null) {
                    parameters = new ManagedMap();
                }
                //属性的值
                String value = node.getNodeValue();
                parameters.put(name, new TypedStringValue(value, String.class));
            }
        }
        if (parameters != null) {
            beanDefinition.getPropertyValues().addPropertyValue("parameters", parameters);
        }
        return beanDefinition;
    }

    private static boolean isPrimitive(Class<?> cls) {
        return cls.isPrimitive() || cls == Boolean.class || cls == Byte.class
                || cls == Character.class || cls == Short.class || cls == Integer.class
                || cls == Long.class || cls == Float.class || cls == Double.class
                || cls == String.class || cls == Date.class || cls == Class.class;
    }

    @SuppressWarnings("unchecked")
    private static void parseMultiRef(String property, String value, RootBeanDefinition beanDefinition,
                                      ParserContext parserContext) {
        String[] values = value.split("\\s*[,]+\\s*");
        ManagedList list = null;
        for (int i = 0; i < values.length; i++) {
            String v = values[i];
            if (v != null && v.length() > 0) {
                if (list == null) {
                    list = new ManagedList();
                }
                list.add(new RuntimeBeanReference(v));
            }
        }
        beanDefinition.getPropertyValues().addPropertyValue(property, list);
    }

    /***
     * 该方法主要用来解析类似
     *   <dubbo:provider id="default_provider" registry="dubbo_registry" timeout="60000">
     *          <dubbo:service interface="com.xxx.XxxService"></dubbo:service>
     *          <dubbo:service interface="com.xxx.XxxService"></dubbo:service>
     *   </dubbo:provider>
     *   配置的服务提供者，这些服务提供者的provider属性配置为当前父标签解析出来的provider bean定义
     *
     *   等价于如下这种
     *
     *   <dubbo:service provider="default_provider" ref="PetrolApiService" interface="com.gysoft.api.project.petrol.api.PetrolApiService" register="true"/>
     */
    private static void parseNested(Element element, ParserContext parserContext, Class<?> beanClass, boolean required, String tag, String property, String ref, BeanDefinition beanDefinition) {
        NodeList nodeList = element.getChildNodes();
        if (nodeList != null && nodeList.getLength() > 0) {
            boolean first = true;
            for (int i = 0; i < nodeList.getLength(); i++) {
                Node node = nodeList.item(i);
                if (node instanceof Element) {
                    //如果标签是<dubbo:service 或者<dubbo:reference
                    if (tag.equals(node.getNodeName())
                            || tag.equals(node.getLocalName())) {
                        if (first) {
                            first = false;
                            //获取当前<dubbo:provider的 default属性
                            String isDefault = element.getAttribute("default");
                            if (isDefault == null || isDefault.length() == 0) {
                                //如果没有设置为default则设置为false
                                beanDefinition.getPropertyValues().addPropertyValue("default", "false");
                            }
                        }
                        /**
                         * 解析<dubbo:service或者<dubbo:reference
                         */
                        BeanDefinition subDefinition = parse((Element) node, parserContext, beanClass, required);
                        if (subDefinition != null && ref != null && ref.length() > 0) {
                            //设置<dubbo:service的provider属性为当前<dubbo:provider对应的bean
                            //设置<dubbo:service的consumer属性为当前<dubbo:consumer对应的bean
                            subDefinition.getPropertyValues().addPropertyValue(property, new RuntimeBeanReference(ref));
                        }
                    }
                }
            }
        }
    }

    private static void parseProperties(NodeList nodeList, RootBeanDefinition beanDefinition) {
        //如果存在子元素
        if (nodeList != null && nodeList.getLength() > 0) {
            for (int i = 0; i < nodeList.getLength(); i++) {
                //获取当前元素
                Node node = nodeList.item(i);
                if (node instanceof Element) {
                    //如果是<property元素
                    if ("property".equals(node.getNodeName())
                            || "property".equals(node.getLocalName())) {
                        /**
                         * <property name="" value=""/>
                         * <property name="" ref=""/>
                         *
                         */
                        String name = ((Element) node).getAttribute("name");
                        if (name != null && name.length() > 0) {
                            String value = ((Element) node).getAttribute("value");
                            String ref = ((Element) node).getAttribute("ref");
                            if (value != null && value.length() > 0) {
                                beanDefinition.getPropertyValues().addPropertyValue(name, value);
                            } else if (ref != null && ref.length() > 0) {
                                beanDefinition.getPropertyValues().addPropertyValue(name, new RuntimeBeanReference(ref));
                            } else {
                                throw new UnsupportedOperationException("Unsupported <property name=\"" + name + "\"> sub tag, Only supported <property name=\"" + name + "\" ref=\"...\" /> or <property name=\"" + name + "\" value=\"...\" />");
                            }
                        }
                    }
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static ManagedMap parseParameters(NodeList nodeList, RootBeanDefinition beanDefinition) {
        if (nodeList != null && nodeList.getLength() > 0) {
            ManagedMap parameters = null;
            for (int i = 0; i < nodeList.getLength(); i++) {
                Node node = nodeList.item(i);
                if (node instanceof Element) {
                    if ("parameter".equals(node.getNodeName())
                            || "parameter".equals(node.getLocalName())) {
                        /**
                         * <dubbo:parameter key="key1" value="value1" />
                         * <dubbo:parameter key="key2" value="value2" />
                         */
                        if (parameters == null) {
                            parameters = new ManagedMap();
                        }
                        String key = ((Element) node).getAttribute("key");
                        String value = ((Element) node).getAttribute("value");
                        boolean hide = "true".equals(((Element) node).getAttribute("hide"));
                        if (hide) {
                            key = Constants.HIDE_KEY_PREFIX + key;
                        }
                        parameters.put(key, new TypedStringValue(value, String.class));
                    }
                }
            }
            return parameters;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static void parseMethods(String id, NodeList nodeList, RootBeanDefinition beanDefinition,
                                     ParserContext parserContext) {
        if (nodeList != null && nodeList.getLength() > 0) {
            ManagedList methods = null;
            for (int i = 0; i < nodeList.getLength(); i++) {
                Node node = nodeList.item(i);
                if (node instanceof Element) {
                    Element element = (Element) node;
                    /**
                     * <dubbo:method name="method1" timeout="2000"/>
                     * <dubbo:method name="method2" timeout="2000"/>
                     */
                    if ("method".equals(node.getNodeName()) || "method".equals(node.getLocalName())) {
                        String methodName = element.getAttribute("name");
                        if (methodName == null || methodName.length() == 0) {
                            throw new IllegalStateException("<dubbo:method> name attribute == null");
                        }
                        if (methods == null) {
                            methods = new ManagedList();
                        }
                        //调用parse(Element element)方法解析method标签为MethodConfig的beanDefinition
                        BeanDefinition methodBeanDefinition = parse(((Element) node),
                                parserContext, MethodConfig.class, false);
                        String name = id + "." + methodName;
                        BeanDefinitionHolder methodBeanDefinitionHolder = new BeanDefinitionHolder(
                                methodBeanDefinition, name);
                        methods.add(methodBeanDefinitionHolder);
                    }
                }
            }
            if (methods != null) {
                //赋值给父标签的beanDefinition比如<dubbo:reference
                beanDefinition.getPropertyValues().addPropertyValue("methods", methods);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static void parseArguments(String id, NodeList nodeList, RootBeanDefinition beanDefinition,
                                       ParserContext parserContext) {
        if (nodeList != null && nodeList.getLength() > 0) {
            ManagedList arguments = null;
            for (int i = 0; i < nodeList.getLength(); i++) {
                Node node = nodeList.item(i);
                if (node instanceof Element) {
                    Element element = (Element) node;
                    /**
                     *     <dubbo:argument index="0" callback="true" />
                     */
                    if ("argument".equals(node.getNodeName()) || "argument".equals(node.getLocalName())) {
                        String argumentIndex = element.getAttribute("index");
                        if (arguments == null) {
                            arguments = new ManagedList();
                        }
                        BeanDefinition argumentBeanDefinition = parse(((Element) node),
                                parserContext, ArgumentConfig.class, false);
                        String name = id + "." + argumentIndex;
                        BeanDefinitionHolder argumentBeanDefinitionHolder = new BeanDefinitionHolder(
                                argumentBeanDefinition, name);
                        arguments.add(argumentBeanDefinitionHolder);
                    }
                }
            }
            if (arguments != null) {
                //加入到beanDefiniton中好了！
                beanDefinition.getPropertyValues().addPropertyValue("arguments", arguments);
            }
        }
    }

    @Override
    public BeanDefinition parse(Element element, ParserContext parserContext) {
        return parse(element, parserContext, beanClass, required);
    }

}
