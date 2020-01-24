package com.xxl.job.executor.core.config.custom01;

import sun.misc.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

public class DynaCompTest
{
    public static void main(String[] args) throws Exception {
        String fullName = "com.seeyon.proxy.MyClass";
        File file = new File("/Users/yangyu/Downloads/myclass");
        InputStream in = new FileInputStream(file);
        byte[] bytes = IOUtils.readFully(in, -1, false);
        String src = new String(bytes);
        in.close();

        System.out.println(src);
        DynamicEngine de = DynamicEngine.getInstance();
        Object instance =  de.javaCodeToObject(fullName,src.toString());
        System.out.println(instance);
    }
}