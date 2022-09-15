package com.jimbean.shenyu.client.dubbo.wrapper;

/**
 * dynamic create implement class
 *
 * @author zhangjb
 */
public interface ApiWrapperFactory {

	Class<?> make(String uriPrefix, Class<?> interfaceClass) throws Exception;
}
