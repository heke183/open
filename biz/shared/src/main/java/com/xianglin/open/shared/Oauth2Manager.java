package com.xianglin.open.shared;

import com.xianglin.open.shared.exception.OpenException;
import com.xianglin.open.shared.model.Oauth2Account;
import com.xianglin.open.shared.model.Oauth2Channel;

import java.util.Map;

/**
 * 用于oauth2.0认证
 */
public interface Oauth2Manager {

    /**查询渠道信息
     * @param paras
     * @return
     * @throws OpenException
     */
    Oauth2Channel queryChannel(Map<String,String> paras) throws OpenException;

    /**获取访问令牌
     * @param appId
     * @param content
     * @return
     */
    Oauth2Account queryAccessToken(Map<String,String> paras)throws OpenException;

    /** 刷新accessToken
     * @param paras
     * @return
     * @throws OpenException
     */
    Oauth2Account refreshToke(Map<String,String> paras)throws OpenException;

    /** 业务处理服务
     * @param paras
     * @return 返回使用aes加盟后的结果
     * @throws OpenException
     */
    String doService(Map<String,String> paras)throws OpenException;

}
