package com.xianglin.open.shared.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * oauth2.0缓存渠道信息
 */
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Oauth2Channel {

    /**
     *渠道id
     */
    private String appId;

    /**
     * 渠道RSA公钥，保存在服务端
     */
    private String publicKey;

    /**
     *渠道私钥
     */
    private String privateKey;

    /**
     * 渠道码
     */
    private String channelCode;

    /**
     *签到授权码
     */
    private String code;

    /**
     *渠道授权码过期时间
     */
    private Long codeExpireTime = System.currentTimeMillis();

}
