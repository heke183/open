package com.xianglin.open.shared.impl;

import com.alibaba.dubbo.common.utils.CollectionUtils;
import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.xianglin.appserv.common.service.facade.app.PersonalService;
import com.xianglin.appserv.common.service.facade.app.SystemParaService;
import com.xianglin.appserv.common.service.facade.model.Response;
import com.xianglin.appserv.common.service.facade.model.vo.SysParaVo;
import com.xianglin.appserv.common.service.facade.model.vo.UserVo;
import com.xianglin.cif.common.service.facade.CustomersInfoService;
import com.xianglin.cif.common.service.facade.enums.AccountRoleTypeEnum;
import com.xianglin.cif.common.service.facade.model.CustomersDTO;
import com.xianglin.cif.common.service.facade.model.RoleDTO;
import com.xianglin.open.shared.OauthManager;
import com.xianglin.open.util.MD5;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.StringRedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;


@Service
public class OauthManagerImpl implements OauthManager {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Reference
    private SystemParaService systemParaService;

    @Autowired
    @Resource(name = "redisCache")
    private RedisTemplate<String,String> redisCache;

    @Reference
    private PersonalService personalService;

    @Reference
    private CustomersInfoService customersInfoService;

    /**
     * code缓存前缀
     */
    private final String CODE_PRIFIX = "OAUTH_CODE-";

    private final String ACCESS_TOKEN_PRIFIX = "OAUTH_ACCESS_TOKEN-";

    @Override
    public String queryCode(final String appid, Long partyId) {
        String code = "";
        try {

            Response<SysParaVo> resp = systemParaService.queryPara("app_partners_list");
            if (resp.getResult() != null) {
                JSONObject apps = JSON.parseObject(resp.getResult().getValue());
                if (apps.containsKey(appid)) {
                    final Map<String, String> redisMap = new HashMap<>();
                    redisMap.put("appid", appid);
                    redisMap.put("secret", apps.getString(appid));
                    redisMap.put("partyId", partyId + "");
                    redisMap.put("openid", MD5.encode32(appid + partyId));
                    redisCache.opsForValue().set("testkey","test2222");
                    String value = redisCache.opsForValue().get("testkey");
                    String redisCode = UUID.randomUUID().toString();
                    String redisKey = CODE_PRIFIX + redisCode;
                    redisCache.opsForHash().putAll(redisKey,redisMap);
                    redisCache.expire(redisKey,5*60, TimeUnit.SECONDS);
                    code = redisCode;
                }
            }
        } catch (Exception e) {
            logger.warn("queryCode", e);
        }
        return code;
    }

    @Override
    public Map<String, String> queryAccessToken(final String appid, final String secret, final String code) {
        Map<String, String> result = new HashMap<>();
        try {
            String redisKey = CODE_PRIFIX + code;
            final Map<String, String> redisMap = redisCache.<String,String>opsForHash().entries(redisKey);
            if (redisMap == null || redisMap.isEmpty()) {
                logger.info("code {} 已过期", code);
            }
            if (StringUtils.equals(appid, redisMap.get("appid")) && StringUtils.equals(secret, redisMap.get("secret"))) {
                String partyId = redisMap.get("partyId");
                String accessToken = UUID.randomUUID().toString();
                String accessKey = ACCESS_TOKEN_PRIFIX + accessToken + "";
                redisCache.opsForHash().putAll(accessKey,redisMap);
                redisCache.expire(accessKey,2*3600, TimeUnit.SECONDS);
                result.put("access_token", accessToken);
                result.put("openid", redisMap.get("openid"));
                result.put("expires_in", 2 * 3600 + "");
            } else {
                logger.info("error appid or secret appid:{},secret:{} ", appid, secret);
            }
            if (result.isEmpty()) {
                result.put("errcode", "4000102");
                result.put("errmsg", "error code,appid or secreet");
            }
        } catch (Exception e) {
            logger.warn("queryCode", e);
            result.put("errcode", "4000999");
            result.put("errmsg", "nuknow error");
        }
        return result;
    }

    @Override
    public Map<String, String> queryUserInfo(String openId, final String accessToke) {
        Map<String, String> result = new HashMap<>();
        try {
            String redisKey = ACCESS_TOKEN_PRIFIX + accessToke;
            final Map<String, String> redisMap = redisCache.<String,String>opsForHash().entries(redisKey);
            if (redisMap == null || redisMap.isEmpty()) {
                logger.info("accessToke {} 已过期", accessToke);
            }
            String redisOpenid = redisMap.get("openid");
            if (!StringUtils.equals(openId, redisOpenid)) {
                logger.info("openid error {} redisOpenid:{}", openId, redisOpenid);
            }
            String partyId = redisMap.get("partyId");
            result = queryUserInfo(Long.valueOf(partyId));
            result.put("openid", openId);
            if (result.isEmpty()) {
                result.put("errcode", "4000004");
                result.put("errmsg", "error access_token or openid");
            }
        } catch (Exception e) {
            logger.warn("queryCode", e);
            result.put("errcode", "4000999");
            result.put("errmsg", "nuknow error");
        }
        return result;
    }

    @Override
    public Map<String, String> queryUserInfoV2(String appid, String secret, final String code) {
        Map<String, String> result = new HashMap<>();
        try {
            String redisKey = CODE_PRIFIX + code;
            final Map<String, String> redisMap = redisCache.<String,String>opsForHash().entries(redisKey);

            if (redisMap == null || redisMap.isEmpty()) {
                logger.info("code {} 已过期", code);
            }
            String redisAppid = redisMap.get("appid");
            String redisSecret = redisMap.get("secret");
            if (StringUtils.equals(redisAppid, appid) || StringUtils.equals(redisSecret, secret)) {
                String partyId = redisMap.get("partyId");
                String redisOpenid = redisMap.get("openid");
                result = queryUserInfo(Long.valueOf(partyId));
                result.put("openid", redisMap.get("openid"));
            }
            String partyId = redisMap.get("partyId");
            UserVo user = personalService.queryUser(Long.valueOf(partyId)).getResult();
            if (user != null) {
                result.put("mobile_phone", user.getLoginName());
                result.put("head_img", user.getHeadImg());
                result.put("name", user.getTrueName());
                result.put("openid", redisMap.get("openid"));
            }
            if (result.isEmpty()) {
                result.put("errcode", "4000102");
                result.put("errmsg", "error appid secret or code");
            }
        } catch (Exception e) {
            logger.warn("queryCode", e);
            result.put("errcode", "4000999");
            result.put("errmsg", "nuknow error");
        }
        return result;
    }

    /**
     * @param partyId
     * @return
     */
    private Map<String, String> queryUserInfo(Long partyId) {
        Map<String, String> userInfo = new HashMap<>();
        UserVo user = personalService.queryUser(Long.valueOf(partyId)).getResult();
        if (user != null) {
            userInfo.put("mobile_phone", user.getLoginName());
            userInfo.put("head_img", user.getHeadImg());
            userInfo.put("name", user.getTrueName());
        }
        CustomersDTO customersDTO = customersInfoService.selectByPartyId(partyId).getResult();
        String nodeManager = "N";
        if (customersDTO != null) {
            userInfo.put("id_number", customersDTO.getCredentialsNumber());
            userInfo.put("gender", customersDTO.getGender());
            userInfo.put("name", customersDTO.getCustomerName());
            if(CollectionUtils.isNotEmpty(customersDTO.getRoleDTOs())){
                for(RoleDTO role:customersDTO.getRoleDTOs()){
                    if(StringUtils.equals(role.getRoleCode(), AccountRoleTypeEnum.NODE_MANAGER.name())){
                        nodeManager = "Y";
                        break;
                    }
                }
            }
        }
        userInfo.put("node_manager", nodeManager);
        return userInfo;
    }
}
