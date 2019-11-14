package com.alibaba.dubbo.rpc.cluster;

import com.alibaba.dubbo.common.extension.ExtensionLoader;

public class ConfiguratorFactory$Adaptive implements com.alibaba.dubbo.rpc.cluster.ConfiguratorFactory {
    public com.alibaba.dubbo.rpc.cluster.Configurator getConfigurator(com.alibaba.dubbo.common.URL arg0) {
        if (arg0 == null) throw new IllegalArgumentException("url == null");
        com.alibaba.dubbo.common.URL url = arg0;
        String extName = url.getProtocol();
        if (extName == null)
            throw new IllegalStateException("Fail to get extension(com.alibaba.dubbo.rpc.cluster.ConfiguratorFactory) name from url(" + url.toString() + ") use keys([protocol])");
        com.alibaba.dubbo.rpc.cluster.ConfiguratorFactory extension = (com.alibaba.dubbo.rpc.cluster.ConfiguratorFactory) ExtensionLoader.getExtensionLoader(com.alibaba.dubbo.rpc.cluster.ConfiguratorFactory.class).getExtension(extName);
        return extension.getConfigurator(arg0);
    }
}