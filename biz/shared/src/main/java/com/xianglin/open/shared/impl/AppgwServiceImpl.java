package com.xianglin.open.shared.impl;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.caucho.hessian.client.HessianConnectionFactory;
import com.xianglin.gateway.common.service.spi.JSONGatewayService;
import com.xianglin.gateway.common.service.spi.model.ServiceRequest;
import com.xianglin.gateway.common.service.spi.model.ServiceResponse;
import com.xianglin.open.shared.AppgwService;
import com.xianglin.open.shared.exception.OpenException;
import com.xianglin.open.util.AppSessionContext;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.common.util.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by wanglei on 2017/11/21.
 */
@Service("appgwService")
public class AppgwServiceImpl implements AppgwService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Reference(version = "1.0.0",group = "appgw-appserv")
    private JSONGatewayService appservService;

    /**
     * 调用系统服务通用方法（针对appserv项目）
     *
     * @param interfaceName 接口名
     * @param method        方法名
     * @param result        返回结果 类型
     * @param paras         变长参数
     * @return
     */
    @Override
    public <T> T service(Class interfaceName, String method, Class<T> result, Object... paras) throws OpenException{
        logger.info("appser service interfaceName:{},method:{},paras:{}", interfaceName.getName(), method, ArrayUtils.toString(paras, null));
        try {
            ServiceRequest<String> req = new ServiceRequest();
            req.setServiceId(getMethod(interfaceName, method,paras));
            JSONArray array = new JSONArray();
            if (paras.length > 0) {
                for (Object p : paras) {
                    array.add(p);
                }
            }
            req.setRequestData(array.toJSONString());
            req.setSessionId(AppSessionContext.ofSessionId("open default session"));
            ServiceResponse<String> resp = appservService.service(req);
            logger.info("appser service interfaceName:{},method:{},result:{}", interfaceName.getName(), method, resp);
            if (resp.isSuccess()) {
                Field resultFiels = resp.getClass().getDeclaredField("result");
                resultFiels.setAccessible(true);
                Object o = resultFiels.get(resp);
                if (o instanceof JSONObject) {
                    return JSON.parseObject(o.toString(), result);
                } else {
                    return (T) resp.getResult();
                }
            }else{
                throw new OpenException(resp.getCode()+",",resp.getTips());
            }
        } catch (OpenException e) {
            throw e;
        }catch (Exception e) {
            logger.warn("appser service", e);
        }
        return null;
    }

    @Override
    public <T> List<T> serviceList(Class interfaceName, String method, Class<T> geneType, Object... paras) throws OpenException{
        logger.info("appser service interfaceName:{},method:{},paras:{}", interfaceName.getName(), method, ArrayUtils.toString(paras, null));
        try {
            ServiceRequest<String> req = new ServiceRequest();
            req.setServiceId(getMethod(interfaceName, method,paras));
            JSONArray array = new JSONArray();
            if (paras.length > 0) {
                for (Object p : paras) {
                    array.add(p);
                }
            }
            req.setRequestData(array.toJSONString());
            req.setSessionId(AppSessionContext.ofSessionId("open default session"));
            ServiceResponse<String> resp = appservService.service(req);
            logger.info("appser service interfaceName:{},method:{},result:{}", interfaceName.getName(), method, resp);
            if (resp.isSuccess()) {
                Field resultFiels = resp.getClass().getDeclaredField("result");
                resultFiels.setAccessible(true);
                Object o = resultFiels.get(resp);
                Type resultType = new TypeReference<List<T>>() {
                }.getType();
                return JSON.parseObject(o.toString(), resultType);
            }else{
                throw new OpenException(resp.getCode()+",",resp.getTips());
            }
        } catch (OpenException e) {
            throw e;
        } catch (Exception e) {
            logger.warn("appser service", e);
        }
        return null;
    }

    /**
     * 区方法全名
     *
     * @param c
     * @param methodName
     * @return
     */
    private String getMethod(Class c, String methodName,Object[] paras) {
        String mName = "";
//        Method[] methods = c.getMethods();
//        for (Method m : methods) {
//            if (StringUtils.equals(methodName, m.getName())) {
//        paras[0].getClass()

                mName = c.getName() + "." + methodName + "." + getMethodSign(Arrays.stream(paras).map(Object::getClass).collect(Collectors.toList()));
//                break;
//            }
//        }
        return mName;
    }

    /**
     * 取方法参数签名
     *
     * @param paramTypes
     * @return
     */
    private String getMethodSign(List<Type> paramTypes) {
        StringBuilder builder = new StringBuilder("");
        if (!CollectionUtils.isEmpty(paramTypes)) {
            for (Type type : paramTypes) {
                builder.append(type.toString());
            }
        }
        String base64 = DigestUtils.md5Hex(builder.toString());
        return StringUtils.substring(base64, 0, 8);
    }

    public static void main(String[] args) {

    }
}
