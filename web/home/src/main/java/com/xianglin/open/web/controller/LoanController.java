package com.xianglin.open.web.controller;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSONObject;
import com.xianglin.cif.common.service.facade.ChannelAccountService;
import com.xianglin.cif.common.service.facade.model.ChannelDTO;
import com.xianglin.loanbiz.common.service.facade.UserInfoFacade;
import com.xianglin.loanbiz.common.service.facade.dto.ApprovalReqDTO;
import com.xianglin.loanbiz.common.service.facade.dto.RequestDTO;
import com.xianglin.loanbiz.common.service.facade.dto.ResponseDTO;
import com.xianglin.loanbiz.common.service.facade.dto.ZyUserInfoDTO;
import com.xianglin.loanbiz.common.service.facade.enums.Constants;
import com.xianglin.loanbiz.common.service.facade.enums.ProductTypeEnum;
import com.xianglin.loanbiz.common.service.facade.enums.ResponseEnum;
import com.xianglin.open.shared.exception.ResultEnum;
import com.xianglin.open.util.AESUtils;
import com.xianglin.open.util.MD5;
import com.xianglin.open.web.utils.JsonResult;

/**
 * Created by zhangyong on 2018/6/29.
 */
@Controller
@RequestMapping("/loan")
public class LoanController {
	private static final Logger LOGGER = LoggerFactory.getLogger(LoanController.class);
	@Reference(timeout = 2000)
	private UserInfoFacade userInfoFacade;

	private static String APPID = "6192186563c4fec6a38757c94bf6c211";
	private static String CHANNEL_PREFIX = "ZHONGYUAN_CHANNEL";
	private static String CALL_BACK_STATUS_PREFIX = "LoanController.userStatusCallBack";
	private static String CALL_BACK_USERINFO_PREFIX = "LoanController.userInfoCallBack";

	@Resource(name = "redisCache")
	private RedisTemplate<String, String> redisCache;

	@Reference()
	private ChannelAccountService channelAccountService;

	private ChannelDTO checkParam() {
		ChannelDTO channelDTO = null;
		String channel = redisCache.opsForValue().get(CHANNEL_PREFIX + APPID);
		if (channel == null) {
			channelDTO = channelAccountService.selectChannel(APPID).getResult();
			channel = JSONObject.toJSONString(channelDTO);
			redisCache.opsForValue().set(CHANNEL_PREFIX + APPID, channel);//防止缓存穿透
			redisCache.expire(CHANNEL_PREFIX + APPID, 60, TimeUnit.SECONDS);

		}
		if (StringUtils.isEmpty(channel) || StringUtils.equals("null", channel)) {
			return null;
		}

		return JSONObject.parseObject(channel, ChannelDTO.class);
	}

	@RequestMapping("userStatus")
	public JsonResult userStatusCallBack(HttpServletRequest request) {
		LOGGER.info("用户状态通知");
		LocalTime now = LocalTime.now();
		JsonResult result = new JsonResult();
		JSONObject rtObject = new JSONObject();
		rtObject.put("nonceStr", UUID.randomUUID());
		rtObject.put("dateTime", now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm:ss")));
		String applyNo = "";
		String signature = null;
		ChannelDTO dto = checkParam();
		if (dto == null) {
			result.setCode(ResultEnum.ERROR_400004.getCode());
			result.setMessage(ResultEnum.ERROR_400004.getCode());
			return result;
		}
		JSONObject jsonObject = null;
		try {
			String decryptStr = AESUtils.decrypt(getParam(request), dto.getRsaPrivateKey(), AESUtils.ECB_TRANSFORMATION);
			jsonObject = JSONObject.parseObject(decryptStr);
			LOGGER.info("收到通知请求:{}", jsonObject);
			if ((applyNo = jsonObject.getString("applyNo")) == null || jsonObject.getString("certNo") == null || (signature = jsonObject.getString("signature")) == null) {
				result.setCode(ResultEnum.ERROR_400005.getCode());
				result.setMessage(ResultEnum.ERROR_400005.getMessage());
				return result;
			}
		} catch (Exception e) {
			LOGGER.error("报文解析错误", e);
			result.setCode(ResultEnum.ERROR_400005.getCode());
			result.setMessage(ResultEnum.ERROR_400005.getMessage());
			return result;
		}
		try {
			//判断签名是否正确
			if (!signature.equals(getMd5Key(JSONObject.toJavaObject(jsonObject, HashMap.class), dto.getChannelKey()))) {
				result.setCode(ResultEnum.ERROR_400005.getCode());
				result.setMessage(ResultEnum.ERROR_400005.getMessage());
				return result;
			}
			if (redisCache.opsForValue().setIfAbsent(CALL_BACK_STATUS_PREFIX + applyNo, applyNo)) {
				RequestDTO<ApprovalReqDTO> requestDTO = new RequestDTO<>();
				ApprovalReqDTO approvalReqDTO = new ApprovalReqDTO();
				approvalReqDTO.setObjectId(applyNo);
				approvalReqDTO.setApprovalSuggest(jsonObject.getString("comments"));
//				ResponseDTO<Boolean> responseDTO = userInfoFacade.receiveUserStatus(null);
				redisCache.expire(CALL_BACK_STATUS_PREFIX + applyNo, 5, TimeUnit.SECONDS);
			} else {
				result.setCode(ResultEnum.ERROR_400003.getCode());
				result.setMessage(ResultEnum.ERROR_400003.getMessage());
			}
			signature = getMd5Key(JSONObject.toJavaObject(jsonObject, HashMap.class), dto.getChannelKey());
			jsonObject.put("signature", signature);
			result.setContent(jsonObject);
		} catch (Exception e) {
			result.setCode(ResultEnum.ERROR_400005.getCode());
			result.setMessage(ResultEnum.ERROR_400005.getCode());
		} finally {
			if (jsonObject != null) {
				redisCache.delete(CALL_BACK_STATUS_PREFIX + applyNo);
			}
		}
		return result;

	}

	@RequestMapping("userInfo")
	@ResponseBody
	public String userInfo(HttpServletRequest request, HttpServletResponse response) {
		LOGGER.info("用户信息通知");
		LocalDateTime now = LocalDateTime.now();
		Map<String, Object> result = new HashMap<>();
		result.put("code", ResultEnum.SUCCESS.getCode());
		result.put("message", ResultEnum.SUCCESS.getMessage());
		JSONObject rtObject = new JSONObject();
		rtObject.put("nonceStr", UUID.randomUUID());
		rtObject.put("dateTime", now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm:ss")));
		ChannelDTO channelDTO = checkParam();

		String signature = null;
		String certNo = null;
		JSONObject jsonObject = null;
		String resultStr = "";
		try {
			if (channelDTO == null) {
				result.put("code", ResultEnum.ERROR_400004.getCode());
				result.put("message", ResultEnum.ERROR_400004.getMessage());
			}else {
				String decryptStr = AESUtils.decrypt(getParam(request), channelDTO.getRsaPrivateKey(), AESUtils.ECB_TRANSFORMATION);
				jsonObject = JSONObject.parseObject(decryptStr);
				LOGGER.info("收到通知请求:{}", jsonObject);
				if ((certNo = jsonObject.getString("certId")) == null || null == jsonObject || (signature = jsonObject.getString("signature")) == null) {
					result.put("code", ResultEnum.ERROR_400005.getCode());
					result.put("message", "参数错误");
				} else {
					try {
						//判断签名是否正确
						ZyUserInfoDTO zyUserInfoDTO = JSONObject.toJavaObject(jsonObject, ZyUserInfoDTO.class);
						if (!signature.equals(getMd5Key(JSONObject.toJavaObject(jsonObject, Map.class), channelDTO.getChannelKey()))) {
							result.put("code", ResultEnum.ERROR_400005.getCode());
							result.put("message", ResultEnum.ERROR_400005.getMessage());
						} else {
							if (redisCache.opsForValue().setIfAbsent(CALL_BACK_USERINFO_PREFIX + certNo, certNo)) {
								RequestDTO<ZyUserInfoDTO> requestDTO = new RequestDTO<>();
								zyUserInfoDTO.setBusiType(ProductTypeEnum.SUIXIN.getCode());
								zyUserInfoDTO.setUpdater(Constants.XDTypeEnum.ZHONGYUAN.getDesc());
								//配偶字段调整 otherCustName变为spouseName
								zyUserInfoDTO.setSpousePhone(jsonObject.getString("otherTelphone"));
								zyUserInfoDTO.setSpouseName(jsonObject.getString("otherCustomerName"));
								zyUserInfoDTO.setSpouseCertNo(jsonObject.getString("otherCertId"));
								zyUserInfoDTO.setSpouseCertType(jsonObject.getString("otherCertType"));
								zyUserInfoDTO.setCertId(jsonObject.getString("certId").replace("x", "X"));
								requestDTO.setServiceParm(zyUserInfoDTO);

								ResponseDTO<ZyUserInfoDTO> responseDTO = userInfoFacade.receiveUserInfo(requestDTO);
								if (!ResponseEnum.SUCCESS.getCode().equals(responseDTO.getCode())) {
									result.put("code", responseDTO.getCode());
									result.put("message", responseDTO.getMsg());
								}
								redisCache.expire(CALL_BACK_USERINFO_PREFIX + certNo, 5, TimeUnit.SECONDS);
							} else {
								result.put("code", ResultEnum.ERROR_400003.getCode());
								result.put("message", ResultEnum.ERROR_400003.getMessage());
							}

						}

					} catch (Exception e) {
						LOGGER.error("回调错误：", e);
						result.put("code", ResultEnum.ERROR_500000.getCode());
						result.put("message", ResultEnum.ERROR_500000.getCode());
					} finally {
						if (jsonObject != null) {
							redisCache.delete(CALL_BACK_USERINFO_PREFIX + certNo);
						}
					}
				}
			}
		} catch (Exception e) {
			LOGGER.error("报文解析错误", e);
			result.put("code", ResultEnum.ERROR_400005.getCode());
			result.put("message", ResultEnum.ERROR_400005.getMessage());
		}

		signature = getMd5Key(JSONObject.toJavaObject(rtObject, Map.class), channelDTO.getChannelKey());
		rtObject.put("signature", signature);
		result.put("content", rtObject);
		resultStr = JSONObject.toJSONString(result);
		LOGGER.info("返回数据：{}", resultStr);
		return AESUtils.encrypt(resultStr, channelDTO.getRsaPrivateKey(), AESUtils.ECB_TRANSFORMATION);
	}

	private String getParam(HttpServletRequest request) {
		try {
			InputStream inputStream = request.getInputStream();
			String param = IOUtils.toString(inputStream, "UTF-8");
			LOGGER.info("原始请求：{}", param);
			return param;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	private String getMd5Key(Map<String, Object> map, String signatureKey) {
		if (StringUtils.isBlank(signatureKey)) {
			throw new RuntimeException("signatureKey 为空，请检查");
		}
		TreeSet<String> set = new TreeSet<>();
		set.addAll(map.keySet());
		StringBuilder stringBuilder = new StringBuilder();
		for (String key : set) {
			if ("signature".equals(key)) {
				continue;
			}
			stringBuilder.append(key);
			stringBuilder.append("=");
			stringBuilder.append(map.get(key));
			stringBuilder.append("&");
		}
		stringBuilder.append("key").append("=").append(signatureKey);
		LOGGER.info("待签名数据:{}",stringBuilder.toString());
		return MD5.md5(stringBuilder.toString()).toUpperCase();
	}

	public static void main(String[] args) {

		/*String sd ="JA";
		String s1 = "VA1";
		String s2 = sd+s1;
		String s3 = "JAVA1";
		String ss = new String("JAVA1");

		System.out.println(ss.intern() == s2);
		System.out.println(ss.intern() == s3);
		System.out.println(s2.intern() == s3);*/
/*		ZyUserInfoDTO zyUserInfoDTO = new ZyUserInfoDTO();
		zyUserInfoDTO.setCertNo("1231123123");
		zyUserInfoDTO.setCommadd("北京");
		Map<String, Object> map = BeanMap.create(zyUserInfoDTO);
		System.out.println(map);*/
		/*System.out.println(JSON.toJSONString(zyUserInfoDTO, new ValueFilter() {
			@Override
			public Object process(Object obj, String s, Object v) {
				if (v == null)
					return "";
				return v;
			}
		}, SerializerFeature.WriteNonStringKeyAsString));*/

		//System.out.println(AESUtils.decrypt("54DsCNMAnwKutdAac6onUzK/Ihzx0f7K5/UIv2M+boBq4t7xEKuweBz9zxkHIqruqkCjgqrvrOPpoe8Zk9Gm+4Md2XEWZlSIMUlLiEj1gaA=","1A75DE46E7556C92",AESUtils.ECB_TRANSFORMATION));
		//System.out.println(null == JSONObject.toJSONString(null));
		//	System.out.println(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm:ss")));
	}
}

