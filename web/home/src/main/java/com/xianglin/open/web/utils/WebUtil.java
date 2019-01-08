/**
 * 
 */
package com.xianglin.open.web.utils;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.util.WebUtils;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * web工具类
 * 
 * @author pengpeng 2015年9月17日上午11:34:13
 */
public class WebUtil {

	/** logger */
	private static final Logger logger = LoggerFactory.getLogger(WebUtils.class);

	/** JSON_MIME_TYPE */
	private static final String MIME_TYPE_JSON = "application/json;charset=UTF-8";

	/** HEADER_CLIENT_IP */
	private static final String HEADER_CLIENT_IP = "";

	private static final Map<String, String> MIME_TYPE_MAP = new HashMap<String, String>();

	static {
		MIME_TYPE_MAP.put("jpg", "image/jpeg");
		MIME_TYPE_MAP.put("jpeg", "image/jpeg");
		MIME_TYPE_MAP.put("gif", "image/gif");
		MIME_TYPE_MAP.put("png", "image/png");
	}

	/**
	 * 根据后缀名取得对应的MimeType
	 * 
	 * @param suffix
	 * @return
	 */
	public static String getMimeType(String suffix) {
		suffix = StringUtils.trimToEmpty(suffix).toLowerCase();
		return MIME_TYPE_MAP.get(suffix);
	}

	/**
	 * 将json字符串写入http响应中
	 * 
	 * @param response
	 * @param message
	 */
	public static void writeJsonToResponse(HttpServletResponse response, String message) {
		writeToResponse(response, message, MIME_TYPE_JSON);
	}

	/**
	 * 将字符串写入http响应中
	 * 
	 * @param response
	 * @param message
	 * @param mimeType
	 */
	public static void writeToResponse(HttpServletResponse response, String message, String mimeType) {
		response.setContentType(mimeType);
//		 response.setHeader("Access-Control-Allow-Origin", "*");
		PrintWriter writer = null;
		try {
			writer = response.getWriter();
			writer.write(message);
			writer.flush();
		} catch (IOException e) {
			logger.error("writeJsonToResponse error!", e);
			throw new RuntimeException(e);
		} finally {
			IOUtils.closeQuietly(writer);
		}
	}

	/**
	 * 将data写入http响应中
	 * 
	 * @param response
	 * @param data
	 * @param mimeType
	 */
	public static void writeToResponse(HttpServletResponse response, byte[] data, String mimeType) {
		response.setContentType(mimeType);
		ServletOutputStream outputStream = null;
		try {
			outputStream = response.getOutputStream();
			outputStream.write(data);
			outputStream.flush();
		} catch (IOException e) {
			logger.error("writeJsonToResponse error!", e);
			throw new RuntimeException(e);
		} finally {
			IOUtils.closeQuietly(outputStream);
		}
	}

	/**
	 * 从HttpServletRequest中取指定名称的header
	 * 
	 * @param request
	 * @param header
	 * @param defaultValue
	 * @return
	 */
	public static String getHeader(HttpServletRequest request, String header, String defaultValue) {
		String result = StringUtils.trimToNull(request.getHeader(header));
		if (StringUtils.isEmpty(result)) {
			result = defaultValue;
		}
		return result;
	}

	/**
	 * 从HttpServletRequest中取指定名称的header
	 * 
	 * @param request
	 * @param header
	 * @return
	 */
	public static String getHeader(HttpServletRequest request, String header) {
		return StringUtils.trimToNull(request.getHeader(header));
	}

	/**
	 * 从HttpServletRequest中取指定名称的cookie
	 * 
	 * @param request
	 * @param cookieName
	 * @param defaultValue
	 * @return
	 */
	public static String getCookie(HttpServletRequest request, String cookieName, String defaultValue) {
		Cookie[] cookies = request.getCookies();
		if (cookies == null) {
			return defaultValue;
		}
		String result = null;
		for (Cookie cookie : cookies) {
			if (cookie != null && cookie.getName().equals(cookieName)) {
				result = StringUtils.trimToNull(cookie.getValue());
			}
		}
		if (StringUtils.isEmpty(result)) {
			result = defaultValue;
		}
		return result;
	}

	/**
	 * 从HttpServletRequest中取指定名称的cookie
	 * 
	 * @param request
	 * @param cookieName
	 * @return
	 */
	public static String getCookie(HttpServletRequest request, String cookieName) {
		return getCookie(request, cookieName, null);
	}

	/**
	 * 从HttpServletRequest中取指定名称的参数
	 * 
	 * @param request
	 * @param param
	 * @param defaultValue
	 * @return
	 */
	public static String getParam(HttpServletRequest request, String param, String defaultValue) {
		String result = StringUtils.trimToNull(request.getParameter(param));
		if (StringUtils.isEmpty(result)) {
			result = defaultValue;
		}
		return result;
	}

	/**
	 * 从HttpServletRequest中取指定名称的参数
	 * 
	 * @param request
	 * @param param
	 * @return
	 */
	public static String getParam(HttpServletRequest request, String param) {
		return StringUtils.trimToNull(request.getParameter(param));
	}

	/**
	 * 取得客户端ip地址
	 * 
	 * @param request
	 * @return
	 */
	public static String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if(StringUtils.isNotEmpty(ip) && !"unKnown".equalsIgnoreCase(ip)){
            int index = ip.indexOf(",");
            if(index != -1){
                return ip.substring(0,index);
            }else{
                return ip;
            }
        }
        ip = request.getHeader("X-Real-IP");
        if(StringUtils.isNotEmpty(ip) && !"unKnown".equalsIgnoreCase(ip)){
            return ip;
        }
        return request.getRemoteAddr();
	}
	/**
	 * 
	 * @param request
	 * @return
	 */
	public static  Map<String,Object> getRequestMap(HttpServletRequest request){
		logger.info(" get request string {}",request.getQueryString());
		Map<String,String[]> requestMap = request.getParameterMap();
		Map<String,Object> returnMap = new HashMap<>();
		for(String key:(Set<String>) requestMap.keySet()){
			String []values =  requestMap.get(key);
			returnMap.put(key, values[0]);
			logger.info(key+" = "+values[0] );
		}
		return returnMap;
	}
	
}
