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
package com.alibaba.dubbo.common.bytecode;

import com.alibaba.dubbo.common.utils.ClassHelper;
import com.alibaba.dubbo.common.utils.ReflectUtils;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Proxy.
 */

public abstract class Proxy {
    public static final InvocationHandler RETURN_NULL_INVOKER = new InvocationHandler() {
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            return null;
        }
    };
    public static final InvocationHandler THROW_UNSUPPORTED_INVOKER = new InvocationHandler() {
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            throw new UnsupportedOperationException("Method [" + ReflectUtils.getName(method) + "] unimplemented.");
        }
    };
    private static final AtomicLong PROXY_CLASS_COUNTER = new AtomicLong(0);
    private static final String PACKAGE_NAME = Proxy.class.getPackage().getName();
    //类加载器 key(接口名称组成的字符串) 实例
    private static final Map<ClassLoader, Map<String, Object>> ProxyCacheMap = new WeakHashMap<ClassLoader, Map<String, Object>>();

    private static final Object PendingGenerationMarker = new Object();

    protected Proxy() {
    }

    /**
     * Get proxy.
     *
     * @param ics interface class array.
     * @return Proxy instance.
     */
    public static Proxy getProxy(Class<?>... ics) {
        return getProxy(ClassHelper.getClassLoader(Proxy.class), ics);
    }

    /**
     * 获取proxy对象
     *
     * @param cl  class loader.
     * @param ics interface class array.
     * @return Proxy instance.
     */
    public static Proxy getProxy(ClassLoader cl, Class<?>... ics) {
        //如果接口数量超过了65535抛出异常，说吧你是不是学的jdk的可重入锁的源码！
        if (ics.length > 65535)
            throw new IllegalArgumentException("interface limit exceeded");
        StringBuilder sb = new StringBuilder();
        //遍历接口列表
        for (int i = 0; i < ics.length; i++) {
            String itf = ics[i].getName();
            //竟然不是接口抛出异常
            if (!ics[i].isInterface())
                throw new RuntimeException(itf + " is not a interface.");
            //加载接口类
            Class<?> tmp = null;
            try {
                tmp = Class.forName(itf, false, cl);
            } catch (ClassNotFoundException e) {
            }
            //不是同一个类加载器加载的clss抛出异常
            if (tmp != ics[i])
                throw new IllegalArgumentException(ics[i] + " is not visible from class loader");

            sb.append(itf).append(';');
        }

        // use interface class name list as key.
        // 拼接所有的接口名称后 toString作为key
        String key = sb.toString();

        // get cache by class loader.

        Map<String, Object> cache;
        synchronized (ProxyCacheMap) {
            // key 为classLoader value为cache
            cache = ProxyCacheMap.get(cl);
            if (cache == null) {
                cache = new HashMap<String, Object>();
                ProxyCacheMap.put(cl, cache);
            }
        }

        Proxy proxy = null;
        //加锁
        synchronized (cache) {
            do {
                //从缓存中获取 Reference<Proxy> 实例
                //取到了调用get方法返回proxy
                Object value = cache.get(key);
                if (value instanceof Reference<?>) {
                    proxy = (Proxy) ((Reference<?>) value).get();
                    if (proxy != null)
                        return proxy;
                }
                //并发控制,保证只有一个线程可以后续操作
                if (value == PendingGenerationMarker) {
                    try {
                        cache.wait();
                    } catch (InterruptedException e) {
                    }
                } else {
                    cache.put(key, PendingGenerationMarker);
                    break;
                }
            }
            while (true);
        }
        //id自增器
        long id = PROXY_CLASS_COUNTER.getAndIncrement();
        String pkg = null;
        //代理类
        ClassGenerator ccp = null;
        //Proxy实现类创建ccp的对象
        ClassGenerator ccm = null;
        try {
            // 使用当前ClassLoader创建 ClassGenerator 对象，
            ccp = ClassGenerator.newInstance(cl);
            Set<String> worked = new HashSet<String>();
            List<Method> methods = new ArrayList<Method>();
            for (int i = 0; i < ics.length; i++) {
                //如果ics不是public的接口，则需要判断接口是否同包
                if (!Modifier.isPublic(ics[i].getModifiers())) {
                    //获取包名称
                    String npkg = ics[i].getPackage().getName();
                    if (pkg == null) {
                        //赋值pkg
                        pkg = npkg;
                    } else {
                        if (!pkg.equals(npkg))
                            throw new IllegalArgumentException("non-public interfaces from different packages");
                    }
                }
                //添加接口到ccp中
                ccp.addInterface(ics[i]);
                //遍历接口的方法
                for (Method method : ics[i].getMethods()) {
                    //获取方法签名
                    String desc = ReflectUtils.getDesc(method);
                    // 如果方法描述字符串已在 worked 中，则忽略。考虑这种情况，
                    // A 接口和 B 接口中包含一个完全相同的方法
                    if (worked.contains(desc))
                        continue;
                    worked.add(desc);

                    int ix = methods.size();
                    //获取方法返回值
                    Class<?> rt = method.getReturnType();
                    //获取方法的参数列表
                    Class<?>[] pts = method.getParameterTypes();
                    // 生成 Object[] args = new Object[1...N]
                    StringBuilder code = new StringBuilder("Object[] args = new Object[").append(pts.length).append("];");
                    for (int j = 0; j < pts.length; j++)
                        // 生成 args[1...N] = ($w)$1...N;
                        code.append(" args[").append(j).append("] = ($w)$").append(j + 1).append(";");
                    // 生成 InvokerHandler 接口的 invoker 方法调用语句，如下：
                    // 生成 InvokerHandler 接口的 invoker 方法调用语句，如下
                    code.append(" Object ret = handler.invoke(this, methods[" + ix + "], args);");
                    // 返回值不为 void
                    if (!Void.TYPE.equals(rt))
                        // 生成返回语句，形如 return (java.lang.String) ret;
                        code.append(" return ").append(asArgument(rt, "ret")).append(";");

                    methods.add(method);
                    // 添加方法名、访问控制符、参数列表、方法代码等信息到 ClassGenerator 中
                    // 说白了就是拼接成为public Integer getName(Integer arg0){...}这种代码字符串
                    ccp.addMethod(method.getName(), method.getModifiers(), rt, pts, method.getExceptionTypes(), code.toString());
                }
            }

            if (pkg == null)
                pkg = PACKAGE_NAME;

            // create ProxyInstance class.
            //  构建接口代理类名称：pkg + ".proxy" + id，比如 org.apache.dubbo.proxy0
            String pcn = pkg + ".proxy" + id;
            //设置className
            ccp.setClassName(pcn);
            //添加静态成员变量
            ccp.addField("public static java.lang.reflect.Method[] methods;");
            //生成 private java.lang.reflect.InvocationHandler handler;
            ccp.addField("private " + InvocationHandler.class.getName() + " handler;");
            // 为接口代理类添加带有 InvocationHandler 参数的构造方法，比如：
            // porxy0(java.lang.reflect.InvocationHandler arg0) {
            //     handler=$1;
            // }
            ccp.addConstructor(Modifier.PUBLIC, new Class<?>[]{InvocationHandler.class}, new Class<?>[0], "handler=$1;");
            //添加默认构造方法
            ccp.addDefaultConstructor();
            //生成接口代理类的对象
            Class<?> clazz = ccp.toClass();
            //赋值methods属性
            clazz.getField("methods").set(null, methods.toArray(new Method[0]));
            //构建 Proxy 子类名称，比如 Proxy1，Proxy2 等
            String fcn = Proxy.class.getName() + id;
            ccm = ClassGenerator.newInstance(cl);
            ccm.setClassName(fcn);
            ccm.addDefaultConstructor();
            ccm.setSuperClass(Proxy.class);
            // 为 Proxy 的抽象方法 newInstance 生成实现代码，形如：
            // public Object newInstance(java.lang.reflect.InvocationHandler h) {
            //     return new org.apache.dubbo.proxy0($1);
            // }
            ccm.addMethod("public Object newInstance(" + InvocationHandler.class.getName() + " h){ return new " + pcn + "($1); }");
            // 通过反射创建 Proxy 实例
            Class<?> pc = ccm.toClass();
            proxy = (Proxy) pc.newInstance();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        } finally {
            // release ClassGenerator
            if (ccp != null)
                ccp.release();
            if (ccm != null)
                ccm.release();
            synchronized (cache) {
                if (proxy == null)
                    cache.remove(key);
                else
                    // 写缓存
                    cache.put(key, new WeakReference<Proxy>(proxy));
                //唤醒其他等待线程
                cache.notifyAll();
            }
        }
        return proxy;
    }

    private static String asArgument(Class<?> cl, String name) {
        if (cl.isPrimitive()) {
            if (Boolean.TYPE == cl)
                return name + "==null?false:((Boolean)" + name + ").booleanValue()";
            if (Byte.TYPE == cl)
                return name + "==null?(byte)0:((Byte)" + name + ").byteValue()";
            if (Character.TYPE == cl)
                return name + "==null?(char)0:((Character)" + name + ").charValue()";
            if (Double.TYPE == cl)
                return name + "==null?(double)0:((Double)" + name + ").doubleValue()";
            if (Float.TYPE == cl)
                return name + "==null?(float)0:((Float)" + name + ").floatValue()";
            if (Integer.TYPE == cl)
                return name + "==null?(int)0:((Integer)" + name + ").intValue()";
            if (Long.TYPE == cl)
                return name + "==null?(long)0:((Long)" + name + ").longValue()";
            if (Short.TYPE == cl)
                return name + "==null?(short)0:((Short)" + name + ").shortValue()";
            throw new RuntimeException(name + " is unknown primitive type.");
        }
        return "(" + ReflectUtils.getName(cl) + ")" + name;
    }

    /**
     * get instance with default handler.
     *
     * @return instance.
     */
    public Object newInstance() {
        return newInstance(THROW_UNSUPPORTED_INVOKER);
    }

    /**
     * get instance with special handler.
     *
     * @return instance.
     */
    abstract public Object newInstance(InvocationHandler handler);
}
