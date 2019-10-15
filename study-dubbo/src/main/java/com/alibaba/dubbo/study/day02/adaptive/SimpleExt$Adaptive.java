package com.alibaba.dubbo.study.day02.adaptive;

import com.alibaba.dubbo.common.extension.ExtensionLoader;

public class SimpleExt$Adaptive implements com.alibaba.dubbo.study.day02.adaptive.SimpleExt {
    public java.lang.String simple(com.alibaba.dubbo.common.URL arg0, java.lang.String arg1) {
        if (arg0 == null) throw new IllegalArgumentException("url == null");
        com.alibaba.dubbo.common.URL url = arg0;
        String extName = url.getParameter("key", "ext");
        if (extName == null)
            throw new IllegalStateException("Fail to get extension(com.alibaba.dubbo.study.day02.adaptive.SimpleExt) name from url(" + url.toString() + ") use keys([key])");
        com.alibaba.dubbo.study.day02.adaptive.SimpleExt extension = (com.alibaba.dubbo.study.day02.adaptive.SimpleExt) ExtensionLoader.getExtensionLoader(com.alibaba.dubbo.study.day02.adaptive.SimpleExt.class).getExtension(extName);
        return extension.simple(arg0, arg1);
    }
}
