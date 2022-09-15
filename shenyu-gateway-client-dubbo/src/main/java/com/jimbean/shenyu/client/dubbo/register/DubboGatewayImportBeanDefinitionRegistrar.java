package com.jimbean.shenyu.client.dubbo.register;

import com.jimbean.shenyu.client.core.annotation.GateWay;
import com.jimbean.shenyu.client.core.helper.HttpHelper;
import com.jimbean.shenyu.client.dubbo.annotation.DubboGatewayScanner;
import com.jimbean.shenyu.client.dubbo.wrapper.ApiWrapperFactory;
import org.apache.shenyu.client.dubbo.common.annotation.ShenyuDubboClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.Ordered;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author zhangjb <br/>
 * @date 2022-08-08 16:22 <br/>
 * @email: <a href="mailto:zhangjb@c5game.com">zhangjb</a> <br/>
 */
public class DubboGatewayImportBeanDefinitionRegistrar
        implements ImportBeanDefinitionRegistrar, ResourceLoaderAware, EnvironmentAware, BeanFactoryAware, Ordered {

    private static final Logger LOG = LoggerFactory.getLogger(DubboGatewayImportBeanDefinitionRegistrar.class);

    private BeanFactory beanFactory;
    private Environment environment;
    private ResourceLoader resourceLoader;

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
        try {
            initCustomGatewayProperties();
            registerWrapper(importingClassMetadata, registry);
        } catch (Exception e) {
            LOG.error("registerBeanDefinitions fail: ", e);
        }
    }

    public void registerWrapper(AnnotationMetadata metadata, BeanDefinitionRegistry registry) throws Exception {
        LinkedHashSet<BeanDefinition> candidateComponents = new LinkedHashSet<>();
        ClassPathScanningCandidateComponentProvider scanner = getScanner();
        scanner.setResourceLoader(this.resourceLoader);
        scanner.addIncludeFilter(new AnnotationTypeFilter(GateWay.class));
        Set<String> basePackages = getBasePackages(metadata);
        for (String basePackage : basePackages) {
            candidateComponents.addAll(scanner.findCandidateComponents(basePackage));
        }
        Class<?> classWrapper = null;
        AnnotatedBeanDefinition beanDefinition;
        AnnotationMetadata annotationMetadata;
        for (BeanDefinition candidateComponent : candidateComponents) {
            if (candidateComponent instanceof AnnotatedBeanDefinition) {
                beanDefinition = (AnnotatedBeanDefinition) candidateComponent;
                annotationMetadata = beanDefinition.getMetadata();
                String contextPath = (String) annotationMetadata.getAnnotationAttributes(GateWay.class.getCanonicalName()).get("contextPath");
                String path = annotationMetadata.getAnnotationAttributes(
                        ShenyuDubboClient.class.getCanonicalName()) == null ? "" : (String) annotationMetadata.getAnnotationAttributes(ShenyuDubboClient.class.getCanonicalName()).get("path");

                ApiWrapperFactory apiWrapperFactory = SpringFactoriesLoader.loadFactories(ApiWrapperFactory.class, getClassLoader()).get(0);
                classWrapper = apiWrapperFactory.make(contextPath + path, Class.forName(candidateComponent.getBeanClassName()));

                if (null != classWrapper) {
                    registry.registerBeanDefinition(classWrapper.getSimpleName(),
                            BeanDefinitionBuilder.genericBeanDefinition(classWrapper).getBeanDefinition());
                } else {
                    if (LOG.isWarnEnabled()) {
                        LOG.warn("BeanClassName:[{}] has not method mapping", candidateComponent.getBeanClassName());
                    }
                }
            }
        }
    }

    private ClassLoader getClassLoader() {
        if (this.resourceLoader != null) {
            return this.resourceLoader.getClassLoader();
        }
        return ClassUtils.getDefaultClassLoader();
    }

    protected ClassPathScanningCandidateComponentProvider getScanner() {
        return new ClassPathScanningCandidateComponentProvider(false, this.environment) {
            @Override
            protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
                boolean isCandidate = false;
                if (beanDefinition.getMetadata().isIndependent()) {
                    if (!beanDefinition.getMetadata().isAnnotation()) {
                        isCandidate = true;
                    }
                }
                return isCandidate;
            }
        };
    }

    protected Set<String> getBasePackages(AnnotationMetadata importingClassMetadata) {
        Map<String, Object> attributes = importingClassMetadata
                .getAnnotationAttributes(DubboGatewayScanner.class.getCanonicalName());

        Set<String> basePackages = new HashSet<>();
        for (String pkg : (String[]) attributes.get("value")) {
            if (StringUtils.hasText(pkg)) {
                basePackages.add(pkg);
            }
        }
        for (String pkg : (String[]) attributes.get("basePackages")) {
            if (StringUtils.hasText(pkg)) {
                basePackages.add(pkg);
            }
        }
        for (Class<?> clazz : (Class[]) attributes.get("basePackageClasses")) {
            basePackages.add(ClassUtils.getPackageName(clazz));
        }

        if (basePackages.isEmpty()) {
            basePackages.add(ClassUtils.getPackageName(importingClassMetadata.getClassName()));
        }
        return basePackages;
    }

    private void initCustomGatewayProperties(){
        String endpoint = environment.getProperty("gateway.endpoint", "http://localhost:9195");
        Boolean signEnabled = Boolean.parseBoolean(environment.getProperty("gateway.sign.enabled", "false"));
        String appKey = environment.getProperty("gateway.sign.appKey", "");
        String appSecret = environment.getProperty("gateway.sign.appSecret", "");
        HttpHelper.INSTANCE.retrofitProperties(endpoint, signEnabled, appKey, appSecret);
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
