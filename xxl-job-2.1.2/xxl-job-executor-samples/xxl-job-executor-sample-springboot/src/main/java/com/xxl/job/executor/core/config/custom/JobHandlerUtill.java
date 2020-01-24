package com.xxl.job.executor.core.config.custom;

import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.executor.XxlJobExecutor;
import com.xxl.job.core.util.XxlJobRemotingUtil;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.util.CollectionUtils;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @Classname JavaObjectUtill
 * @author: LUOBINGKAI
 * @Description TODO
 * @Date 2020/1/23 18:35
 */
public class JobHandlerUtill {
    public static final AtomicInteger atomicInteger = new AtomicInteger(0);
    public static final Map<Integer, Class<?>> addClzMap = new TreeMap<>();

    public static Class<?> getClz() throws Exception {

        String code = "public class HelloWorld" + atomicInteger.incrementAndGet() + " {\n" +
                "    @com.xxl.job.core.handler.annotation.XxlJob(\"demoJobHandler" + atomicInteger.get() + "\")\n" +
                "    public com.xxl.job.core.biz.model.ReturnT<String> demoJobHandler" + atomicInteger.get() + "(String param) throws Exception {\n" +
                "\t\tfor(int i=0; i < 1; i++){\n" +
                "\t\t\t       System.out.println(\"Hello World!\");\n" +
                "\t\t}\n" +
                "   return com.xxl.job.core.biz.model.ReturnT.SUCCESS;\n" +
                "    }\n" +
                "}";
        CustomStringJavaCompiler compiler = new CustomStringJavaCompiler(code);
        if (!compiler.compiler()) {
            throw new Exception("编译失败  " + compiler.getCompilerMessage());
        }
        Class<?> clz = compiler.getClz();
        return clz;
    }

    public static void removeClz() {
        if (CollectionUtils.isEmpty(addClzMap) || addClzMap.size() <= 1) {
            return;
        }
        // 保留最后一个
        Iterator<Map.Entry<Integer, Class<?>>> it = addClzMap.entrySet().iterator();
        int size = addClzMap.size();
        int i = 0;
        while (it.hasNext()) {
            if ((++i) > size - 1) {
                return;
            }
            Map.Entry<Integer, Class<?>> next = it.next();
            Integer jonInfoId = next.getKey();
            Class<?> oldClz = next.getValue();

            it.remove();
            // 将admin 旧的job删除
            String adminAddresses = ApplicationContextIni.getApplicationContext().getEnvironment().getProperty("xxl.job.admin.addresses");
            String accessToken = ApplicationContextIni.getApplicationContext().getEnvironment().getProperty("xxl.job.accessToken");
            String url = adminAddresses + "/api/remove";
            XxlJobRemotingUtil.postBody(url, accessToken, jonInfoId, 3);

            // 理论上来讲线程map是必须由admin触发才会有记录的，保险起见，调用原生的方法优雅删除稳妥
            XxlJobExecutor.removeJobThread(jonInfoId, "need to remove");
            ConfigurableApplicationContext configurableApplicationContext = (ConfigurableApplicationContext) ApplicationContextIni.getApplicationContext();
            DefaultListableBeanFactory defaultListableBeanFactory = (DefaultListableBeanFactory) configurableApplicationContext.getBeanFactory();
            defaultListableBeanFactory.removeBeanDefinition(oldClz.getSimpleName());
        }
    }
}
