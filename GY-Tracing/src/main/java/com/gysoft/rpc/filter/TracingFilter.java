package com.gysoft.rpc.filter;


import com.alibaba.dubbo.common.extension.Activate;
import com.alibaba.dubbo.rpc.*;

/**
 * @author duofei
 * @date 2019/10/25
 */
@Activate(group = "consumer")
public class TracingFilter implements Filter {

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        long start = System.currentTimeMillis();
        Result result = invoker.invoke(invocation);
        System.out.println("我执行了这么久呢：" + (System.currentTimeMillis() - start));
        return result;
    }
}
