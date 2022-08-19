package com.jimbean.shenyu.client.dubbo.wrapper;

/**
 * dynamic create implement class
 *
 * @author zhangjb
 */
public interface DubboApiWrapperFactory {

	Class<?> make(String uriPrefix, Class<?> interfaceClass) throws Exception;
}
