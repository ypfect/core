package com.overstar.core.dubbo;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;

/**
 * @Description  dubbo 自动装配
 * @Author stanley.yu
 * @Date 2019/9/24 21:11
 */
@ConditionalOnProperty(name = {"dubbo.name","dubbo.version","dubbo.zookeeper.address"})
@ImportResource({ "classpath:dubbo-interfaces.xml" })
//@Configuration
public class DubboAutoConfiguration {
    public DubboAutoConfiguration() {
    }
}
