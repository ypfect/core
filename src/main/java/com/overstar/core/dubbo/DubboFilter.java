package com.overstar.core.dubbo;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.rpc.*;

/**
 * @Description
 * @Author stanley.yu
 * @Date 2019/9/24 21:30
 */
@Slf4j
public class DubboFilter implements Filter {
    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        Object[] args = invocation.getArguments();
        String argsJson = JSON.toJSONString(args);
        long start = System.currentTimeMillis();
        try {
            return invoker.invoke(invocation);
        } catch (RpcException re) {
            log.error("rpc error, args:[ {} ].", argsJson, re);
            throw re;
        } catch (Throwable e) {
            log.error(" rpc error, args:[ {} ].", argsJson, e);
            throw new RpcException("Dubbo server uncaught exception!", e);
        } finally {
            long spend = (System.currentTimeMillis() - start);
            int clientPort = RpcContext.getContext().getRemotePort();
            // 获取调用方IP地址
            String clientIP = RpcContext.getContext().getRemoteHost();
            log.info("DubboRPC info: API[ {} ], method[ {} ], args[ {} ], spendTime[ {} ms ], clientIp[ {} ], clientPort[ {} ].",
                    invoker.getInterface().getSimpleName(), invocation.getMethodName(), argsJson, spend, clientIP, clientPort);
        }
    }
}
