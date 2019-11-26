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
package com.alibaba.dubbo.rpc.protocol.dubbo;

import com.alibaba.dubbo.common.utils.StringUtils;
import com.alibaba.dubbo.remoting.RemotingException;
import com.alibaba.dubbo.remoting.exchange.ResponseFuture;
import com.alibaba.dubbo.rpc.Result;
import com.alibaba.dubbo.rpc.RpcException;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 通过该类适配ResponseFuture获取最终调用结果
 * 通过调用RpcContext.getContext().getFuture().get()
 * 方法最终异步获取到结果
 */
public class FutureAdapter<V> implements Future<V> {

    private final ResponseFuture future;

    public FutureAdapter(ResponseFuture future) {
        this.future = future;
    }

    public ResponseFuture getFuture() {
        return future;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return false;
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public boolean isDone() {
        return future.isDone();
    }

    @Override
    @SuppressWarnings("unchecked")
    public V get() throws InterruptedException, ExecutionException {
        try {
            //阻塞获取ResponseFuture中的调用结果
            return (V) (((Result) future.get()).recreate());
        } catch (RemotingException e) {
            throw new ExecutionException(e.getMessage(), e);
        } catch (Throwable e) {
            throw new RpcException(e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        //超时时间
        int timeoutInMillis = (int) TimeUnit.MILLISECONDS.convert(timeout, unit);
        try {
            //超时等待获取结果
            return (V) (((Result) future.get(timeoutInMillis)).recreate());
        } catch (com.alibaba.dubbo.remoting.TimeoutException e) {
            throw new TimeoutException(StringUtils.toString(e));
        } catch (RemotingException e) {
            throw new ExecutionException(e.getMessage(), e);
        } catch (Throwable e) {
            throw new RpcException(e);
        }
    }

}
