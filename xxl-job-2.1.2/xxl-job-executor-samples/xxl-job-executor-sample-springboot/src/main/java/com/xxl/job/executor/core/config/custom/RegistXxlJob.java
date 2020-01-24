package com.xxl.job.executor.core.config.custom;

import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.executor.XxlJobExecutor;
import com.xxl.job.core.handler.annotation.XxlJob;
import com.xxl.job.core.handler.impl.MethodJobHandler;
import com.xxl.job.core.util.XxlJobRemotingUtil;
import groovy.util.logging.Log;
import groovy.util.logging.Slf4j;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;

import static com.xxl.job.core.executor.XxlJobExecutor.loadJobHandler;

/**
 * @Classname RegisXxlJob
 * @author: LUOBINGKAI
 * @Description TODO
 * @Date 2020/1/23 19:40
 */
@Slf4j
public class RegistXxlJob {

    public static void regist() throws Exception {
        ConfigurableApplicationContext configurableApplicationContext = (ConfigurableApplicationContext) ApplicationContextIni.getApplicationContext();
        // 获取bean工厂并转换为DefaultListableBeanFactory
        DefaultListableBeanFactory defaultListableBeanFactory = (DefaultListableBeanFactory) configurableApplicationContext.getBeanFactory();
        // 动态编译内存对象，获取对应的Class,注册进spring容器
        Class<?> aClass = JobHandlerUtill.getClz();
        //通过BeanDefinitionBuilder创建bean定义
        BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder.genericBeanDefinition(aClass);
        // 注册bean
        defaultListableBeanFactory.registerBeanDefinition(aClass.getSimpleName(), beanDefinitionBuilder.getRawBeanDefinition());

        Object bean = ApplicationContextIni.getApplicationContext().getBean(aClass);
        Method[] methods = bean.getClass().getDeclaredMethods();
        for (Method method : methods) {
            XxlJob xxlJob = AnnotationUtils.findAnnotation(method, XxlJob.class);
            if (xxlJob == null) {
                continue;
            }
            String name = xxlJob.value();
            if (name.trim().length() == 0) {
                throw new RuntimeException("xxl-job method-jobhandler name invalid, for[" + bean.getClass() + "#" + method.getName() + "] .");
            }
            if (loadJobHandler(name) != null) {
                throw new RuntimeException("xxl-job jobhandler[" + name + "] naming conflicts.");
            }
            if (!(method.getParameterTypes() != null && method.getParameterTypes().length == 1 && method.getParameterTypes()[0].isAssignableFrom(String.class))) {
                throw new RuntimeException("xxl-job method-jobhandler param-classtype invalid, for[" + bean.getClass() + "#" + method.getName() + "] , " +
                        "The correct method format like \" public ReturnT<String> execute(String param) \" .");
            }
            if (!method.getReturnType().isAssignableFrom(ReturnT.class)) {
                throw new RuntimeException("xxl-job method-jobhandler return-classtype invalid, for[" + bean.getClass() + "#" + method.getName() + "] , " +
                        "The correct method format like \" public ReturnT<String> execute(String param) \" .");
            }
            method.setAccessible(true);

            Method initMethod = null;
            Method destroyMethod = null;

            if (xxlJob.init().trim().length() > 0) {
                try {
                    initMethod = bean.getClass().getDeclaredMethod(xxlJob.init());
                    initMethod.setAccessible(true);
                } catch (NoSuchMethodException e) {
                    throw new RuntimeException("xxl-job method-jobhandler initMethod invalid, for[" + bean.getClass() + "#" + method.getName() + "] .");
                }
            }
            if (xxlJob.destroy().trim().length() > 0) {
                try {
                    destroyMethod = bean.getClass().getDeclaredMethod(xxlJob.destroy());
                    destroyMethod.setAccessible(true);
                } catch (NoSuchMethodException e) {
                    throw new RuntimeException("xxl-job method-jobhandler destroyMethod invalid, for[" + bean.getClass() + "#" + method.getName() + "] .");
                }
            }
            System.out.println("动态注册：" + name);
            new XxlJobExecutor().registJobHandler(name, new MethodJobHandler(bean, method, initMethod, destroyMethod));

            String adminAddresses = ApplicationContextIni.getApplicationContext().getEnvironment().getProperty("xxl.job.admin.addresses");
            String accessToken = ApplicationContextIni.getApplicationContext().getEnvironment().getProperty("xxl.job.accessToken");
            String url = adminAddresses + "/api/addStart";
            XxlJobInfoCustom xxlJobInfoCustom = new XxlJobInfoCustom();
            xxlJobInfoCustom.setJobGroup(Integer.valueOf(ApplicationContextIni.getApplicationContext().getEnvironment().getProperty("xxl.job.admin.executor.groupId")));
            xxlJobInfoCustom.setJobDesc(name);
            xxlJobInfoCustom.setExecutorRouteStrategy("FIRST");
            xxlJobInfoCustom.setExecutorBlockStrategy("SERIAL_EXECUTION");
            xxlJobInfoCustom.setJobCron("0 0 0 * * ? *");
            xxlJobInfoCustom.setGlueType("BEAN");
            xxlJobInfoCustom.setExecutorHandler(name);
            xxlJobInfoCustom.setExecutorFailRetryCount(0);
            xxlJobInfoCustom.setExecutorTimeout(0);
            xxlJobInfoCustom.setAuthor("luobingikai");
            ReturnT<String> returnObj = XxlJobRemotingUtil.postBody(url, accessToken, xxlJobInfoCustom, 3);
            if (returnObj.getCode() != ReturnT.SUCCESS_CODE || StringUtils.isEmpty(returnObj.getContent() )) {
                throw new Exception("生成新任务失败...." + returnObj);
            }
            System.out.println("........................"+returnObj);

            JobHandlerUtill.addClzMap.put(Integer.valueOf(returnObj.getContent()), aClass);
        }
    }
}
























