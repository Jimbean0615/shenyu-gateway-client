package com.jimbean.shenyu.client.dubbo.wrapper;

import javassist.*;
import org.apache.shenyu.client.dubbo.common.annotation.ShenyuDubboClient;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.annotation.AnnotationUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * apache dubbo interface implement code generic
 *
 * @author zhangjb
 */
public class DubboApiWrapperFactory implements ApiWrapperFactory {

    private static AtomicLong WRAPPER_CLASS_COUNTER = new AtomicLong(0);
    private static AtomicLong WRAPPER_METHOD_COUNTER = new AtomicLong(0);
    private static final String VOID = "void";
    private static final Map<ClassLoader, ClassPool> POOL_MAP = new ConcurrentHashMap<ClassLoader, ClassPool>();

    public static ClassPool getClassPool(ClassLoader loader) {
        if (loader == null) {
            return ClassPool.getDefault();
        }

        ClassPool pool = POOL_MAP.get(loader);
        if (pool == null) {
            pool = new ClassPool(true);
            POOL_MAP.put(loader, pool);
        }
        return pool;
    }

    @Override
    public Class<?> make(String uriPrefix, Class<?> interfaceClass) throws Exception {
        long idx = WRAPPER_CLASS_COUNTER.getAndIncrement();
        ClassPool pool = getClassPool(Thread.currentThread().getContextClassLoader());
        CtClass ctClass = pool.makeClass(interfaceClass.getName() + "Impl$Automatic" + idx);
        ctClass.setInterfaces(new CtClass[]{pool.getCtClass(interfaceClass.getName())});

        String methodLogField = "private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(" + ctClass.getName() + ".class);";
        CtField logField = CtField.make(methodLogField, ctClass);
        ctClass.addField(logField);

        for (Method method : interfaceClass.getMethods()) {
            String methodName = method.getReturnType().getName();
            String returnTypeName = method.getReturnType().getName() + ".class";
            if (VOID.equalsIgnoreCase(methodName)) {
                returnTypeName = "null";
            }
            ShenyuDubboClient shenyuDubboClient = AnnotationUtils.findAnnotation(method, ShenyuDubboClient.class);

            int parameterLength = method.getParameters().length;
            Class<?>[] parameterTypes = method.getParameterTypes();
            String[] strParamNames = getMethodParamName(method);

            CtClass[] ctClassParameters = parameterLength == 0 ? null : new CtClass[parameterTypes.length];
            if (parameterLength > 0) {
                for (int i = 0; i < parameterTypes.length; i++) {
                    ctClassParameters[i] = pool.getCtClass(parameterTypes[i].getName());
                }
            }
            CtMethod ctMethod = new CtMethod(pool.getCtClass(method.getReturnType().getName()), method.getName(), ctClassParameters, ctClass);
            ctMethod.setModifiers(Modifier.PUBLIC);

            if (null != shenyuDubboClient) {
                CtClass ctClassVO = null;
                if (parameterLength > 1 || (parameterLength == 1 && parameterTypes[0].isAssignableFrom(List.class))) {
                    long midx = WRAPPER_METHOD_COUNTER.getAndIncrement();
                    ctClassVO = pool.makeClass(interfaceClass.getPackage().getName() + ".vo." + interfaceClass.getSimpleName() + toUpperCase(method.getName()) + "ComplexVO" + midx);
                    CtConstructor ctConstructorVO = new CtConstructor(ctClassParameters, ctClassVO);
                    ctConstructorVO.setModifiers(Modifier.PUBLIC);
                    StringBuilder strConstructor = new StringBuilder();
                    strConstructor.append("{ \n");
                    for (int j = 0; j < parameterLength; j++) {
                        CtField ctField = new CtField(pool.get(parameterTypes[j].getName()), strParamNames[j], ctClassVO);
                        ctField.setModifiers(Modifier.PRIVATE);
                        ctClassVO.addMethod(CtNewMethod.getter(getGetterMethod(strParamNames[j]), ctField));
                        ctClassVO.addMethod(CtNewMethod.setter(getSetterMethod(strParamNames[j]), ctField));
                        ctClassVO.addField(ctField);
                        strConstructor.append("$0." + strParamNames[j] + " = $" + (j + 1) + "; \n");
                    }
                    strConstructor.append("} \n");
                    ctConstructorVO.setBody(strConstructor.toString());
                    ctClassVO.addConstructor(ctConstructorVO);
                    ctClassVO.toClass();
                }

                String methodPath = shenyuDubboClient.path();
                String prefix = VOID.equalsIgnoreCase(methodName) ? "" : "result = ";
                StringBuilder strBody = new StringBuilder();
                strBody.append("{ \n");
                if (!VOID.equalsIgnoreCase(methodName)) {
                    strBody.append(methodName + " result = null;\n");
                }
                strBody.append("java.util.Map/*<String, Object>*/ headers = com.google.common.collect.Maps.newHashMap();\n");
                strBody.append("headers.put(\"traceid\", org.slf4j.MDC.get(\"traceid\")); \n");
                strBody.append("headers.put(\"access_token\", org.slf4j.MDC.get(\"access_token\")); \n");
                strBody.append("try { \n");
                if (parameterLength > 1) {
                    strBody.append(ctClassVO.getName() + " obj = new " + ctClassVO.getName() + "($$); \n");
                    strBody.append(prefix + "com.jimbean.shenyu.client.core.helper.HttpHelper.INSTANCE.postGateway(\"" + uriPrefix + methodPath + "\", headers, obj, " + returnTypeName + ");\n");
                } else if (parameterLength == 1) {
                    if (isBasicType(parameterTypes[0].getName())) {
                        strBody.append("java.util.Map/*<String, Object>*/ map = com.google.common.collect.Maps.newHashMap();\n");
                        strBody.append("map.put(\"" + strParamNames[0] + "\", $1); \n");
                        strBody.append(prefix + "com.jimbean.shenyu.client.core.helper.HttpHelper.INSTANCE.postGateway(\"" + uriPrefix + methodPath + "\", headers, map, " + returnTypeName + ");\n");
                    } else if (parameterTypes[0].isAssignableFrom(List.class)) {
                        strBody.append(ctClassVO.getName() + " obj = new " + ctClassVO.getName() + "($$); \n");
                        strBody.append(prefix + "com.jimbean.shenyu.client.core.helper.HttpHelper.INSTANCE.postGateway(\"" + uriPrefix + methodPath + "\", headers, obj, " + returnTypeName + ");\n");
                    } else {
                        strBody.append(prefix + "com.jimbean.shenyu.client.core.helper.HttpHelper.INSTANCE.postGateway(\"" + uriPrefix + methodPath + "\", headers, $1, " + returnTypeName + ");\n");
                    }
                } else {
                    strBody.append(prefix + "com.jimbean.shenyu.client.core.helper.HttpHelper.INSTANCE.getFromGateway(\"" + uriPrefix + methodPath + "\", headers, " + returnTypeName + ");\n");
                }
                strBody.append("} catch (java.lang.Exception e) { \n");
                strBody.append(" LOG.warn(\"" + method.getName() + " error: {}\", com.google.common.base.Throwables.getStackTraceAsString(e));\n");
                strBody.append(" } \n");
                strBody.append(" return" + (VOID.equalsIgnoreCase(methodName) ? "" : " result") + "; \n");
                strBody.append(" } ");
                ctMethod.setBody(strBody.toString());
                ctClass.addMethod(ctMethod);
            } else {
                StringBuilder strBody = new StringBuilder();
                strBody.append("{\n");
                strBody.append(" throw new java.lang.UnsupportedOperationException(\"Method: [" + method.getName() + "] don't allow to access!\"); \n");
                strBody.append(" } ");
                ctMethod.setBody(strBody.toString());
                ctClass.addMethod(ctMethod);
            }
        }
        return ctClass.toClass();
    }

    protected String[] getMethodParamName(Method method) {
        method.setAccessible(true);
        DefaultParameterNameDiscoverer discoverer = new DefaultParameterNameDiscoverer();
        return discoverer.getParameterNames(method);
    }

    private String toUpperCase(String lowerCase) {
        return lowerCase.substring(0, 1).toUpperCase() + lowerCase.substring(1);
    }

    private String getGetterMethod(String field) {
        return "get" + toUpperCase(field);
    }

    private String getSetterMethod(String field) {
        return "set" + toUpperCase(field);
    }

    private static final List<String> basicTypes = Arrays.asList(
            "java.lang.Boolean", "boolean",
            "java.lang.Character", "char",
            "java.lang.Byte", "byte",
            "java.lang.Short", "short",
            "java.lang.Integer", "int",
            "java.lang.Long", "lang",
            "java.lang.Float", "float",
            "java.lang.Double", "double",
            "java.lang.String",
            "[B");

    private boolean isBasicType(String typeName) {
        return basicTypes.contains(typeName);
    }
}
