package com.xianglin.open.shared.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * oauth2.0缓存账户信息
 */
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Oauth2Account {

    /**
     *客户唯一编号
     */
    private String openId;

    /**
     *客户手机号
     */
    private String mobileNo;

    /**
     *渠道openId
     */
    private String channelCustomerId;

    /**
     * 用户partyId
     */
    private Long partyId;
    /**
     *访问令牌
     */
    private String accessToken;

    /**
     *访问令牌过期时间
     */
    private long accessTokenExpireTime = System.currentTimeMillis();

    /**
     *刷新令牌
     */
    private String refreshToken;

    /**
     *刷新令牌过期时间
     */
    private long refreshTokenExpireTime = System.currentTimeMillis();

    /**
     * 传输密钥
     */
    private String key;
}
