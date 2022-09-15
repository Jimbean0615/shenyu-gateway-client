package com.jimbean.shenyu.client.dubbo.annotation;

import com.jimbean.shenyu.client.dubbo.register.DubboGatewayImportBeanDefinitionRegistrar;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.*;

/**
 * @author zhangjb <br/>
 * @date 2022-08-08 17:00 <br/>
 * @email: <a href="mailto:zhangjb03@gmail.com">zhangjb</a> <br/>
 */
@Target({ ElementType.TYPE, ElementType.ANNOTATION_TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(DubboGatewayImportBeanDefinitionRegistrar.class)
public @interface DubboGatewayScanner {

	@AliasFor("basePackages")
	String[] value() default {};

	@AliasFor("value")
	String[] basePackages() default {};

	Class<?>[] basePackageClasses() default {};
}
