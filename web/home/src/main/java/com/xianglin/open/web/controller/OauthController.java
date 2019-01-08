package com.xianglin.open.web.controller;

import com.xianglin.open.shared.OauthManager;
import com.xianglin.open.util.AppSessionContext;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/oauth")
public class OauthController {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private OauthManager oauthManager;

    /**
     * 通过appid获取code
     * @param request
     * @param response
     */
    @RequestMapping("/code")
    public void queryCode(HttpServletRequest request, HttpServletResponse response) {
        try {
            Long partyId = AppSessionContext.ofPartyId(null);
//            partyId = 11000795L;
            if (partyId == null) {
                response.getWriter().print("<script language=javascript>try{window.android.login();}catch(err){ try{window.webkit.messageHandlers.toLogin.postMessage(null);}catch(err){toLogin();} }</script>");
                return;
            }
            String appid = request.getParameter("appid");
            String redirectUri = URLDecoder.decode(request.getParameter("redirect_uri"), "UTF-8");
            String state = request.getParameter("state");

            StringBuilder sendUrl = new StringBuilder().append(redirectUri);
            if (StringUtils.isEmpty(appid) || StringUtils.isEmpty(redirectUri)) {
                logger.info("appid 或 redirectUri为空，appid:{},redirectUri:{}", appid, redirectUri);
            }
            if (StringUtils.contains(redirectUri, "?")) {
                sendUrl.append("&");
            } else {
                sendUrl.append("?");
            }
            sendUrl.append("state=").append(state);
            String code = oauthManager.queryCode(appid, partyId);
            sendUrl.append("&code=").append(code);
            response.sendRedirect(sendUrl.toString());
        } catch (Exception e) {
            logger.warn("queryCode", e);
        }
    }

    /**
     * 查询accessToken和openid
     *
     * @param request
     * @param response
     */
    @RequestMapping("/access_token")
    @ResponseBody
    public Map<String, String> accessToken(HttpServletRequest request, HttpServletResponse response) {
        Map<String, String> result = new HashMap<>();
        try {
            String appid = request.getParameter("appid");
            String secret = request.getParameter("secret");
            String code = request.getParameter("code");
            if (StringUtils.isEmpty(appid) || StringUtils.isEmpty(secret) || StringUtils.isEmpty(code)) {
                result.put("errcode", "4000901");
                result.put("errmsg", "appid secret or code is empty");
            }
            result = oauthManager.queryAccessToken(appid, secret, code);
        } catch (Exception e) {
            logger.warn("queryCode", e);
            result.put("errcode", "4000999");
            result.put("errmsg", "nuknow error");
        }
        return result;
    }

    /**
     * 通过accessToken和openId查询用户信息
     *
     */
    @RequestMapping("/userinfo")
    @ResponseBody
    public Map<String, String> userinfo(String openid, String access_token) {
        Map<String, String> result = new HashMap<>();
        try {
            if (StringUtils.isEmpty(openid) || StringUtils.isEmpty(access_token)) {
                result.put("errcode", "4000903");
                result.put("errmsg", "openid or accessTooken is empty");
            } else {
                result = oauthManager.queryUserInfo(openid, access_token);
            }
        } catch (Exception e) {
            logger.warn("userinfo", e);
            result.put("errcode", "4000999");
            result.put("errmsg", "nuknow error");
        }
        return result;
    }

    /**
     * 通过code查询用户信息
     */
    @RequestMapping("/codeUserInfo")
    @ResponseBody
    public Map<String, String> userinfo(String code, String appid, String secret) {
        Map<String, String> result = new HashMap<>(2);
        try {
            if (StringUtils.isEmpty(code) || StringUtils.isEmpty(appid) || StringUtils.isEmpty(secret)) {
                result.put("errcode", "4000902");
                result.put("errmsg", "code appid or secret is empty");
            } else {
                result = oauthManager.queryUserInfoV2(appid, secret, code);
            }
        } catch (Exception e) {
            logger.warn("userinfo", e);
            result.put("errcode", "4000999");
            result.put("errmsg", "nuknow error");
        }
        return result;
    }
}
