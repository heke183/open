package com.xianglin.open.util;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * MD5签名
 * 默认UTF-8
 *
 * @author zhangyong
 */
public class MD5 {
    private static final Logger LOG = LoggerFactory.getLogger(MD5.class);
    private static String GBK = "GBK";
    private String charSet = "UTF-8";

    MD5() {
    }

    MD5(String charSet) {
        this.charSet = charSet;
    }

    public static String md5GBK(String str) {
        try {
            return new MD5(GBK).code(str);
        } catch (Exception e) {
            LOG.error("", e);
        }
        return null;
    }

    public static String md5(String str) {
        try {
            return new MD5().code(str);
        } catch (Exception e) {
            LOG.error("", e);
        }
        return null;
    }

    /**
     * @param str
     * @return
     * @throws NoSuchAlgorithmException
     */
    public String code(String str) throws NoSuchAlgorithmException {
        MessageDigest alga;
        String myinfo = str;
        alga = MessageDigest.getInstance("MD5");
        try {
            alga.update(myinfo.getBytes(this.charSet));
            byte[] digesta = alga.digest();
            return parseByte2HexStr(digesta);
        } catch (Exception e) {
            LOG.error("", e);
        }
        return null;

    }

    /**
     * MD5的算法在RFC1321 中定义 utf-8
     * 在RFC 1321中，给出了Test suite用来检验你的实现是否正确：
     * MD5 ("") = d41d8cd98f00b204e9800998ecf8427e
     * MD5 ("a") = 0cc175b9c0f1b6a831c399e269772661
     * MD5 ("abc") = 900150983cd24fb0d6963f7d28e17f72
     * MD5 ("message digest") = f96b697d7cb7938d525a2f31aaf161d0
     * MD5 ("abcdefghijklmnopqrstuvwxyz") = c3fcd3d76192e4007dfb496cca67e13b
     */
    public static String encode(String str) throws NoSuchAlgorithmException {
        MessageDigest alga;
        String myinfo = str;
        alga = MessageDigest.getInstance("MD5");
        try {
            alga.update(myinfo.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            LOG.error("", e);
        }
        byte[] digesta = alga.digest();
        return parseByte2HexStr(digesta);
    }

    /**
     * 获取byte utf-8
     *
     * @param str
     * @return
     * @throws NoSuchAlgorithmException
     */
    public static byte[] getByte(String str) throws NoSuchAlgorithmException {
        MessageDigest alga;
        String myinfo = str;
        alga = MessageDigest.getInstance("MD5");
        try {
            alga.update(myinfo.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            LOG.error("", e);
        }
        return alga.digest();
    }

    public static String encode16(String str) throws NoSuchAlgorithmException {
        return encode(str).substring(8, 24);
    }

    public static String encode32(String str) throws NoSuchAlgorithmException {
        return encode(str);
    }

    public static String parseByte2HexStr(byte[] content) {

        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < content.length; i++) {
            String hex = Integer.toHexString(content[i] & 0xFF);
            if (hex.length() == 1) {
                hex = '0' + hex;

            }
            sb.append(hex);
        }
        return sb.toString();
    }

    /**
     * 没有key，只对value进行拼接
     *
     * @param params
     * @param gatewayKey
     * @param propertyName
     * @return
     */
    public static String getSignature(Map<String, Object> params, String gatewayKey, boolean propertyName) {
        List<String> keys = new ArrayList<>(params.keySet());
        Collections.sort(keys);
        StringBuilder sbuilder = new StringBuilder();
        boolean first = true;
        for (String key : keys) {
            Object value = params.get(key);
            if (!("sign".equals(key) || "signature".equals(key)) && value != null
                    && StringUtils.isNotEmpty(String.valueOf(value))) {
                if (!propertyName) {
                    sbuilder.append(value);
                } else {
                    if (first) {
                        sbuilder.append(key).append("=").append(value);
                        first = false;
                    } else {
                        sbuilder.append("&").append(key).append("=").append(value);

                    }
                }

            }
        }
        LOG.info("to md5 data :{}", sbuilder.toString());
        String signature = getString(gatewayKey, propertyName, sbuilder);
        if (signature != null) {
            return signature;
        }
        return null;
    }

    private static String getString(String gatewayKey, boolean propertyName, StringBuilder sbuilder) {
        try {
            String mdsString;
            if (propertyName) {
                mdsString = sbuilder.append("&key=" + gatewayKey).toString();
            } else {
                mdsString = sbuilder.append(gatewayKey).toString();
            }
            String signature = md5(mdsString);
            LOG.info(" md5 data :{}", signature);
            return signature;
        } catch (Exception e) {
            LOG.error("", e);
        }
        return null;
    }

    /**
     * 签名
     *
     * @param params
     * @param gatewayKey
     * @return
     */
    public static String getSignatureString(Map<String, Object> params, String gatewayKey, boolean propertyName) {
        List<String> keys = new ArrayList<>(params.keySet());
        Collections.sort(keys);
        StringBuilder sbuilder = new StringBuilder();
        boolean first = true;
        for (String key : keys) {
            Object value = params.get(key);
            if (!("sign".equals(key) || "signature".equals(key)) && value != null
                    && StringUtils.isNotEmpty(String.valueOf(value))) {

                if (first) {
                    sbuilder.append(key).append("=").append(value);
                    first = false;
                } else {
                    sbuilder.append("&").append(key).append("=").append(value);
                }

            }
        }
        LOG.info("to md5 data :{}", sbuilder.toString());
        String signature = getString(gatewayKey, propertyName, sbuilder);
        return null;
    }

    /**
     * 生成签名串，参数为空时，不参与签名
     *
     * @param params
     * @return
     * @author zhangyong
     */
    public static String getSignatureString(Map<String, Object> params, String gatewayKey) {
        return getSignatureString(params, gatewayKey, true);
    }

    public static void main(String[] args) {
        LOG.info(md5("xiaolingxiaolin"));
    }
}

