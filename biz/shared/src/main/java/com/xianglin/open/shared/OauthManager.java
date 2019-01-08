package com.xianglin.open.shared;

import java.util.Map;

/**
 * 授权服务管理
 */
public interface OauthManager {

    /**根据appid取code
     * @param appid
     * @param partyId
     * @return
     */
    String queryCode(final String appid,Long partyId);

    /**查询accessToken 信息
     * @param appid
     * @param secret
     * @param code
     * @return
     */
    Map<String, String> queryAccessToken(String appid, String secret, String code);

    /**根据openId 和accessToke 查询用户信息
     * @param openId
     * @param accessToke
     * @return
     */
    Map<String, String> queryUserInfo(String openId, String accessToke);

    /**根据code查询用户信息
     * @param appid
     * @param secret
     * @param code
     * @return
     */
    Map<String, String> queryUserInfoV2(String appid, String secret, String code);
}
