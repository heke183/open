package com.xianglin.open.shared.impl;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.xianglin.appserv.common.service.facade.AppLoginService;
import com.xianglin.appserv.common.service.facade.app.SystemParaService;
import com.xianglin.appserv.common.service.facade.model.vo.LoginVo;
import com.xianglin.cif.common.service.facade.ChannelAccountService;
import com.xianglin.cif.common.service.facade.model.ChannelAccountDTO;
import com.xianglin.cif.common.service.facade.model.ChannelDTO;
import com.xianglin.open.shared.AppgwService;
import com.xianglin.open.shared.Oauth2Manager;
import com.xianglin.open.shared.exception.OpenException;
import com.xianglin.open.shared.exception.ResultEnum;
import com.xianglin.open.shared.model.Oauth2Account;
import com.xianglin.open.shared.model.Oauth2Channel;
import com.xianglin.open.util.AppSessionContext;
import com.xianglin.open.util.MD5;
import com.xianglin.open.util.Oauth2Utils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.FileEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;


@Service
public class Oauth2ManagerImpl implements Oauth2Manager {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final String OAUTH2_PRIFIX = "OPEN_OAUTH2_";
    /**
     * 交易号校验
     */
    private final String OPEN_ORDER_CHECK_SET = "OPEN_ORDER_CHECK_SET";
    /**
     * 用于存放session信息
     */
    private final String SESSION_PRIFIX = "GLOBAL-";

    @Reference
    private SystemParaService systemParaService;

    @Autowired
    @Resource(name = "redisCache")
    private RedisTemplate<String, String> redisCache;

    @Autowired
    @Resource(name = "redisSession")
    private RedisTemplate<String, String> redisSession;

    @Reference()
    private ChannelAccountService channelAccountService;

    @Autowired
    private AppgwService appgwService;

    @Override
    public Oauth2Channel queryChannel(Map<String, String> paras) throws OpenException {
        Oauth2Channel chennel;
        try {
            String appId = paras.get("appId");
            ChannelDTO dto = channelAccountService.selectChannel(appId).getResult();
            if (dto == null) {
                throw new OpenException(ResultEnum.ERROR_400012);
            }
            chennel = Oauth2Channel.builder().channelCode(dto.getCode())
                    .appId(appId).privateKey(dto.getRsaPrivateKey()).publicKey(dto.getRsaPublicKey()).build();
            checkPara(paras, chennel.getPrivateKey());

            String code = MD5.encode32(UUID.randomUUID().toString());
            chennel.setCode(code);
            chennel.setCodeExpireTime(System.currentTimeMillis() + 6 * 60 * 1000);
            updateCacheChannnel(appId, chennel);
            String backCode = new String(Base64.getEncoder().encode(Oauth2Utils.encryptPublicRSA(code.getBytes(), chennel.getPublicKey())));
            chennel.setCode(backCode);
        } catch (OpenException e) {
            throw e;
        } catch (Exception e) {
            logger.warn("queryChannel", e);
            throw new OpenException(ResultEnum.ERROR_400006);
        }
        return chennel;
    }

    @Override
    public Oauth2Account queryAccessToken(Map<String, String> paras) throws OpenException {
        //校验渠道
        //校验请求信息
        //生成ttoken key 等信息
        Oauth2Account account;
        try {
            String appId = paras.get("appId");
            Oauth2Channel channel = queryCacheChannel(appId);
            if (channel == null || channel.getCodeExpireTime() < System.currentTimeMillis()) {
                throw new OpenException(ResultEnum.ERROR_400007);
            }
            //
            checkPara(paras, channel.getPrivateKey());

            //取用户信息
            String content = paras.get("content");
            logger.info("queryAccessToken {}",content);
            String contentValue = new String(Oauth2Utils.dencryptPublicRSA(Base64.getDecoder().decode(content), channel.getPublicKey()));
            JSONObject object = JSON.parseObject(contentValue);

            String channelMobile = object.getString("mobileNo");
            String channelCustomerId = object.getString("openId");

            ChannelAccountDTO dto = ChannelAccountDTO.builder().channelId(channel.getAppId()).channelCustomerId(channelCustomerId).build();
            dto = channelAccountService.register(dto).getResult();

            //创建返回信息
            String accessToken = MD5.encode32(UUID.randomUUID().toString());
            String refreshToken = MD5.encode32(UUID.randomUUID().toString());
            String key = MD5.encode32(UUID.randomUUID().toString());
            account = new Oauth2Account();
            account.setAccessToken(accessToken);
            account.setAccessTokenExpireTime(System.currentTimeMillis() + 7200 * 1000);
            account.setRefreshToken(refreshToken);
            account.setRefreshTokenExpireTime(System.currentTimeMillis() + 2 * 24 * 3600 * 1000);
            account.setKey(key);
            account.setMobileNo(channelMobile);
            account.setPartyId(dto.getPartyId());
            account.setOpenId(dto.getOpenId());
            account.setChannelCustomerId(dto.getChannelCustomerId());
            updateCacheAccount(appId, accessToken, account);

            //生产加盟key
            String backCKey = new String(Base64.getEncoder().encode(Oauth2Utils.encryptPublicRSA(key.getBytes(), channel.getPublicKey())));
            account.setKey(backCKey);
        } catch (OpenException e) {
            throw e;
        } catch (Exception e) {
            logger.warn("queryChannel", e);
            throw new OpenException(ResultEnum.ERROR_400006);
        }
        return account;
    }

    @Override
    public Oauth2Account refreshToke(Map<String, String> paras) throws OpenException {
        Oauth2Account account;
        try {
            String appId = paras.get("appId");
            Oauth2Channel channel = queryCacheChannel(appId);
            checkPara(paras, channel.getPrivateKey());

            //取用户信息
            String content = paras.get("content");
            logger.info("refreshToke {}",content);
            String contentValue = new String(Oauth2Utils.dencryptPublicRSA(Base64.getDecoder().decode(content), channel.getPublicKey()));
            JSONObject object = JSON.parseObject(contentValue);

            //创建返回信息
            String accessToken = object.getString("accessToken");
            String refreshToken = object.getString("refreshToken");
            String openId = object.getString("openId");
            account = queryCacheAccount(appId, accessToken);

            if (!StringUtils.equals(openId, account.getChannelCustomerId())) {
                throw new OpenException(ResultEnum.ERROR_400008);
            }

            if (!StringUtils.equals(refreshToken, account.getRefreshToken())) {
                throw new OpenException(ResultEnum.ERROR_400009);
            }

            if (channel == null || account.getRefreshTokenExpireTime() < System.currentTimeMillis()) {
                throw new OpenException(ResultEnum.ERROR_400010);
            }

            accessToken = MD5.encode32(UUID.randomUUID().toString());
            refreshToken = MD5.encode32(UUID.randomUUID().toString());
            String key = MD5.encode32(UUID.randomUUID().toString());
            account = new Oauth2Account();
            account.setAccessToken(accessToken);
            account.setAccessTokenExpireTime(System.currentTimeMillis() + 7200 * 1000);
            account.setRefreshToken(refreshToken);
            account.setRefreshTokenExpireTime(System.currentTimeMillis() + 2 * 24 * 3600 * 1000);
            account.setKey(key);
            updateCacheAccount(appId, accessToken, account);

            //生产加盟key
            String backCKey = new String(Base64.getEncoder().encode(Oauth2Utils.encryptPublicRSA(key.getBytes(), channel.getPublicKey())));
            account.setKey(backCKey);
        } catch (OpenException e) {
            throw e;
        } catch (Exception e) {
            logger.warn("queryChannel", e);
            throw new OpenException(ResultEnum.ERROR_400006);
        }
        return account;
    }

    @Override
    public String doService(Map<String, String> paras) throws OpenException {
        String result = null;
        try {
            String appId = paras.get("appId");
            Oauth2Channel channel = queryCacheChannel(appId);
            checkPara(paras, channel.getPrivateKey());

            String accessToken = paras.get("accessToken");
            Oauth2Account account = queryCacheAccount(appId, accessToken);
            if (channel == null || account.getAccessTokenExpireTime() < System.currentTimeMillis()) {
                throw new OpenException(ResultEnum.ERROR_400013);
            }

            String content = paras.get("content");
            String contentValue = Oauth2Utils.decryptAES(content, account.getKey());
            JSONObject object = JSON.parseObject(contentValue);

            if (StringUtils.equals(account.getOpenId(), object.getString("openId"))) {
                throw new OpenException(ResultEnum.ERROR_400008);
            }

            //app注册同步
            LoginVo appVo = LoginVo.builder().partyId(account.getPartyId())
                    .loginName(channel.getChannelCode()+account.getMobileNo()).showName(object.getString("name")).build();
            appgwService.service(AppLoginService.class,"channelRegister",LoginVo.class,appVo);
            //用户注册
            String sessionId = UUID.randomUUID().toString();

            HashMap<String, String> sessionMap = new HashMap<>();
            sessionMap.put(AppSessionContext.SESSION_ATTR_PRIFIX+"partyId", account.getPartyId()+"");
            sessionMap.put(AppSessionContext.SESSION_ATTR_PRIFIX+"login_name", account.getMobileNo());

            String sessionKey = SESSION_PRIFIX + sessionId;
            redisSession.opsForHash().putAll(sessionKey, sessionMap);
            redisSession.expire(sessionKey, 3L, TimeUnit.DAYS);

            JSONObject res = new JSONObject();
            res.put("OPENSESSION", sessionId);
            result = Oauth2Utils.encryptAES(res.toJSONString(), account.getKey());
            logger.info("res :{},result:{}",res.toJSONString(),result);
        } catch (OpenException e) {
            throw e;
        } catch (Exception e) {
            logger.warn("queryChannel", e);
            throw new OpenException(ResultEnum.ERROR_400006);
        }
        return result;
    }

    /**
     * 参数校验，包括参数范围校验，签名校验
     *
     * @param paras
     * @param privateKey
     * @throws Exception
     */
    private void checkPara(Map<String, String> paras, String privateKey) throws Exception {
        /*
        1,校验接口编号
        2，校验时间戳
        3，校验唯一数
        4，校验签名
         */
        String nonce = paras.get("nonce");
        Long timestamp = Long.valueOf(paras.get("timestamp"));
        if (timestamp > System.currentTimeMillis() + 300 * 10000 || timestamp < System.currentTimeMillis() - 300 * 10000) {
            throw new OpenException(ResultEnum.ERROR_400002);
        }

        String appId = paras.get("appId");
        String checkNo = appId + nonce;
        boolean flag = redisCache.opsForSet().isMember(OPEN_ORDER_CHECK_SET, checkNo);
        if (flag) {
            throw new OpenException(ResultEnum.ERROR_400003);
        } else {
            redisCache.opsForSet().add(OPEN_ORDER_CHECK_SET, checkNo);
            redisCache.expire(OPEN_ORDER_CHECK_SET, 2, TimeUnit.HOURS);
        }

        String sign = paras.get("sign");
        paras.remove("sign");
        String lastSign = Oauth2Utils.getSign(paras, privateKey);
        if (StringUtils.equals(sign, lastSign)) {
            logger.info("sign form para is {},and last sign is {}", sign, lastSign);
            throw new OpenException(ResultEnum.ERROR_400005);
        }

    }

    /**
     * 缓存渠道信息
     *
     * @param appId
     * @param info
     */
    private void updateCacheChannnel(String appId, Oauth2Channel info) {
        String key = OAUTH2_PRIFIX + appId;
        redisCache.opsForValue().set(key, JSON.toJSONString(info));
        redisCache.expire(key, 1L, TimeUnit.DAYS);
    }

    /**
     * 查询缓存中渠道信息
     *
     * @param appId
     * @return
     */
    public Oauth2Channel queryCacheChannel(String appId) {
        String key = OAUTH2_PRIFIX + appId;
        String content = redisCache.opsForValue().get(key);
        if (StringUtils.isEmpty(content)) {
            return null;
        }
        return JSON.parseObject(content, Oauth2Channel.class);
    }

    /**
     * 缓存用户信息
     *
     * @param appId
     * @param info
     */
    private void updateCacheAccount(String appId, String accessToken, Oauth2Account info) {
        String key = OAUTH2_PRIFIX + appId + "_" + accessToken;
        redisCache.opsForValue().set(key, JSON.toJSONString(info));
        redisCache.expire(key, 2L, TimeUnit.DAYS);
    }

    /**
     * 查询缓存中token信息
     *
     * @param appId
     * @param accessToken
     * @return
     */
    public Oauth2Account queryCacheAccount(String appId, String accessToken) {
        String key = OAUTH2_PRIFIX + appId + "_" + accessToken;
        String content = redisCache.opsForValue().get(key);
        return JSON.parseObject(content, Oauth2Account.class);
    }

    // 将输入流转换成字符串
    private static String inStream2String(InputStream is) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buf = new byte[1024];
        int len = -1;
        while ((len = is.read(buf)) != -1) {
            baos.write(buf, 0, len);
        }
        return new String(baos.toByteArray());
    }

    public static void main(String[] args) throws Exception{
        HttpClient client = new DefaultHttpClient();
        HttpPost post = new HttpPost("https://appfile-dev.xianglin.cn/file/upload");
        File file = new File("D:\\111.png");
        String message = "This is a multipart post";
//        ByteArrayEntity entity = new ByteArrayEntity(Fi);
//        InputStreamEntity entity = new InputStreamEntity(new FileInputStream(file));
        FileEntity entity = new FileEntity(file);
//        entity.setContentType("application/json; charset=UTF-8;boundary=gc0p4Jq0M2Yt08jU534c0p");
        entity.setChunked(true);
//        post.setEntity(entity);
        HttpResponse response = client.execute(post);
        System.out.println(inStream2String(response.getEntity().getContent()));
    }
}
