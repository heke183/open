package com.xianglin.open.web.controller;

import com.xianglin.open.shared.Oauth2Manager;
import com.xianglin.open.shared.OauthManager;
import com.xianglin.open.shared.exception.OpenException;
import com.xianglin.open.shared.exception.ResultEnum;
import com.xianglin.open.shared.model.Oauth2Account;
import com.xianglin.open.shared.model.Oauth2Channel;
import com.xianglin.open.util.Oauth2Utils;
import com.xianglin.open.web.utils.JsonResult;
import com.xianglin.open.web.utils.ResponseUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@RequestMapping("/oauth2")
@RestController
public class Oauth2Controller {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private Oauth2Manager oauth2Manager;

    /**
     * 通过appid获取code
     *
     * @param request
     */
    @RequestMapping("/authorize")
    public Map<String,String> authorize(HttpServletRequest request) {
        JsonResult<String> resp = ResponseUtil.executeResponse(() -> {
            Map<String, String> paras = queryPara(request,"000001");
            Oauth2Channel channel = oauth2Manager.queryChannel(paras);
            return channel.getCode();
        });
        Map<String,String> result = new HashMap<>();
        result.put("errorCode",resp.getCode());
        result.put("errorMsg",resp.getMessage());
        result.put("code",resp.getContent());
        return result;
    }

    /**
     * 获取授权令牌
     *
     * @param request httt请求
     */
    @RequestMapping("/accessToken")
    public Map<String,String> accessToken(HttpServletRequest request) {
        JsonResult<Oauth2Account> resp = ResponseUtil.executeResponse(() -> {
            logger.info("accessToken request {}",request.getQueryString());
            Map<String,String> paras = queryPara(request,"000002");
            return oauth2Manager.queryAccessToken(paras);
        });
        Map<String,String> result = new HashMap<>();
        result.put("errorCode",resp.getCode());
        result.put("errorMsg",resp.getMessage());
        if(resp.isSuccess()){
            result.put("accessToken",resp.getContent().getAccessToken());
            result.put("expiresIn","7200");
            result.put("refreshToken",resp.getContent().getRefreshToken());
            result.put("key",resp.getContent().getKey());
        }
        return result;
    }

    /**
     * 刷新授权令牌
     *
     * @param request
     */
    @RequestMapping("/refreshToken")
    public Map<String,String> refreshToken(HttpServletRequest request) {
        JsonResult<Oauth2Account> resp = ResponseUtil.executeResponse(() -> {
            logger.info("refreshToken {}",request.getQueryString());
            Map<String, String> paras = queryPara(request,"000003");
            return oauth2Manager.refreshToke(paras);
        });
        Map<String,String> result = new HashMap<>();
        result.put("errorCode",resp.getCode());
        result.put("errorMsg",resp.getMessage());
        if(resp.isSuccess()){
            result.put("expiresIn","7200");
            result.put("accessToken",resp.getContent().getAccessToken());
            result.put("refreshToken",resp.getContent().getRefreshToken());
            result.put("key",resp.getContent().getKey());
        }
        return result;
    }

    /**
     * 刷新授权令牌
     *
     * @param request
     */
    @RequestMapping("/api")
    public Map<String,String> api(HttpServletRequest request) {
        JsonResult<String> resp = ResponseUtil.executeResponse(() -> {
            logger.info("query api {}",request.getQueryString());
            Map<String, String> paras = Arrays.stream(request.getQueryString().split("&")).collect(Collectors.toMap(s -> StringUtils.substringBefore(s,"="), e -> StringUtils.substringAfter(e,"=")));
//                    .stream()
//                    .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue()[0]));
            String trxnCode = request.getParameter("trxnCode");
            String appId = paras.get("appId");
            if (StringUtils.isEmpty(appId)) {
                throw new OpenException(ResultEnum.ERROR_400004);
            }
            return oauth2Manager.doService(paras);
        });
        Map<String,String> result = new HashMap<>();
        result.put("errorCode",resp.getCode());
        result.put("errorMsg",resp.getMessage());
        result.put("content",resp.getContent());
        return result;
    }

    /**请求参数校验及封装
     * @param request
     * @param funcCode
     * @return
     * @throws OpenException
     */
    private Map<String, String> queryPara(HttpServletRequest request,String funcCode) throws OpenException{
        try {
//        Map<String, String> paras = request.getParameterMap().entrySet()
//                .stream()
//                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue()[0]));
//            Map<String, String> paras = new HashMap<>();
            String queryString = request.getQueryString();
            Map<String, String> paras = Arrays.stream(queryString.split("&")).collect(Collectors.toMap(s -> StringUtils.substringBefore(s,"="), e -> StringUtils.substringAfter(e,"=")));


            String trxnCode = request.getParameter("trxnCode");
            String appId = paras.get("appId");
            if (StringUtils.isEmpty(appId)) {
                throw new OpenException(ResultEnum.ERROR_400004);
            }
            if (!StringUtils.equals(trxnCode, funcCode)) {
                throw new OpenException(ResultEnum.ERROR_400001);
            }
            return paras;
        } catch (Exception e) {
            logger.warn("",e);
            throw new OpenException(ResultEnum.ERROR_400001);
        }
    }

}
