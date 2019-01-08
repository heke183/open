package com.xianglin.open.shared;


import com.xianglin.open.shared.exception.OpenException;

import java.util.List;

/**
 * Created by wanglei on 2017/11/21.
 */
public interface AppgwService {

    /**调用系统服务通用方法（针对appserv项目）
     * 返回类型为object
     * @param interfaceName 接口名
     * @param method 方法名
     * @param result 返回结果 类型
     * @param paras 变长参数，需要按照实际参数顺畅传值
     * @param <T> 返回类型
     * @return
     */
     <T> T service(Class interfaceName, String method, Class<T> result, Object... paras) throws OpenException;

    /**调用app接口，针对法妞类型为list
     * @param interfaceName 接口名
     * @param method 方法名
     * @param geneType 返回类型
     * @param paras 参数
     * @param <T>
     * @return
     */
    <T> List<T> serviceList(Class interfaceName, String method, Class<T> geneType, Object... paras)  throws OpenException;
}
