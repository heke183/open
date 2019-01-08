package com.xianglin.open.util;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.util.JSONPObject;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

/**
 * 数字签名工具类
 */
public class Oauth2Utils {

    private static final Logger logger = LoggerFactory.getLogger(MD5.class);

    private static final String EQUAL_FLAG = "=";

    private static final String APPEND_FLAG = "&";

    public static String SHA1Encode(String input) throws Exception {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("SHA-256");
            return new String(md.digest(input.getBytes("UTF-8")));
        } catch (Exception e) {
            logger.warn("encode error", e);
            throw new Exception(e);
        }
    }

    /**
     * 利用私钥进行加签名
     * @param content 内容
     * @param privateKey 私钥
     * @return
     * @throws Exception
     */
    public static byte[] encryptPrivateRSA(byte[] content,String privateKey) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE, getPrivateKey(privateKey));
        return cipher.doFinal(content);
    }

    /**
     * 利用私钥进行解签
     *
     * @param content 待解密内容
     * @param publicKey 公钥
     * @return
     * @throws Exception
     */
    public static byte[] dencryptPublicRSA(byte[] content,String publicKey) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.DECRYPT_MODE, getPublicKey(publicKey));
        return cipher.doFinal(content);
    }

    /**
     * 利用私钥进行加签名
     *
     * @param content 内容
     * @param publicKey 公钥
     * @return
     * @throws Exception
     */
    public static byte[] encryptPublicRSA(byte[] content,String publicKey) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE, getPublicKey(publicKey));
        return cipher.doFinal(content);
    }

    /**
     * 利用私钥进行解签
     * @param content 内容
     * @param privateKey 私钥
     * @return
     * @throws Exception
     */
    public static byte[] dencryptPrivateRSA(byte[] content,String privateKey) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.DECRYPT_MODE, getPrivateKey(privateKey));
        return cipher.doFinal(content);
    }

    /**
     * 取私钥
     *
     * @param key
     * @return
     * @throws Exception
     */
    private static PrivateKey getPrivateKey(String key) throws Exception {
        byte[] keyBytes;
        keyBytes = Base64.getDecoder().decode(key);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePrivate(keySpec);
    }

    /**
     * 取公钥
     *
     * @param key
     * @return
     * @throws Exception
     */
    public static PublicKey getPublicKey(String key) throws Exception {
        byte[] keyBytes;
        keyBytes = Base64.getDecoder().decode(key);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PublicKey publicKey = keyFactory.generatePublic(keySpec);
        return publicKey;
    }


    /**
     * 生成AEK加密key
     *
     * @param keySeed
     * @return
     * @throws Exception
     */
    public static Key getAESKey(String keySeed) throws Exception {
        SecureRandom secureRandom = SecureRandom.getInstance("SHA1PRNG");
        secureRandom.setSeed(keySeed.getBytes());
        KeyGenerator generator = KeyGenerator.getInstance("AES");
        generator.init(secureRandom);
        return generator.generateKey();
    }

    /**AES加密
     * @param plainText
     * @param key
     * @return
     * @throws Exception
     */
    public static final String encryptAES(String plainText, String key) throws Exception {
        Key secretKey = getAESKey(key);
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        byte[] p = plainText.getBytes();
        byte[] result = cipher.doFinal(p);
       String encoded = Base64.getEncoder().encodeToString(result);
        BASE64Encoder encoder = new BASE64Encoder();
//        String encoded = encoder.encode(result);
//        return StringUtils.replacePattern(encoded,"\\\n","");
        return encoded;
    }

    /**AES解密
     * @param cipherText
     * @param key
     * @return
     * @throws Exception
     */
    public static final String decryptAES(String cipherText, String key) throws Exception {
        Key secretKey = getAESKey(key);
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, secretKey);
        BASE64Decoder decoder = new BASE64Decoder();
        byte[] c = decoder.decodeBuffer(cipherText);
        byte[] result = cipher.doFinal(c);
        return new String(result);
    }


    /**
     * 获取私钥签名
     *
     * @param paras
     * @return
     * @throws Exception
     */
    public static String getSign(Map<String, String> paras,String privateKey) throws Exception {
        StringBuilder buff = new StringBuilder();
        TreeMap<String, String> paraMap = new TreeMap<>(paras);
        paraMap.forEach((k, v) -> {
            try {
                buff.append(k).append("=").append(URLEncoder.encode(v, "UTF-8")).append(APPEND_FLAG);
            } catch (UnsupportedEncodingException e) {
                logger.warn("getSign", e);
            }
        });
        String content = buff.substring(0, buff.length() - 1);
        System.out.println(content);
        return new String(Base64.getEncoder().encode(encryptPrivateRSA(SHA1Encode(content).getBytes(),privateKey)));
    }

    /**生产密钥对
     * @throws Exception
     */
    public static void getRSAKey()throws Exception{
        KeyPairGenerator keyPairGenerator=KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        KeyPair pair = keyPairGenerator.generateKeyPair();
        System.out.println(Base64.getEncoder().encodeToString(pair.getPrivate().getEncoded()));
        System.out.println(Base64.getEncoder().encodeToString(pair.getPublic().getEncoded()));
    }


    public static void main(String[] args) throws Exception {
        /**
         * 服务的使用公玥加盟，客户端使用私钥解密
         */
        String publicKey = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQC04uo/twpBtYRnJ5DGoXiGNgtkJtluama2/Xmzvksv9aB1ZlWIa3W1JCPVg5Nb3EQSvbnu18lfeTVmn8eafgfoIrmOqQ90Qe3e/YovUrIGxLcHe/mFZP9Yg2UlW5D86Pln10t5rV5UcY37PavLVUU8BeW/+Rdf677hGlOjeYvv2wIDAQAB";

        String privateKey = "MIICdgIBADANBgkqhkiG9w0BAQEFAASCAmAwggJcAgEAAoGBALTi6j+3CkG1hGcnkMaheIY2C2Qm2W5qZrb9ebO+Sy/1oHVmVYhrdbUkI9WDk1vcRBK9ue7XyV95NWafx5p+B+giuY6pD3RB7d79ii9SsgbEtwd7+YVk/1iDZSVbkPzo+WfXS3mtXlRxjfs9q8tVRTwF5b/5F1/rvuEaU6N5i+/bAgMBAAECgYAA1K34vvF4HcpO4vqiPumbzDG/MwJ6pFh2bLGbZrtXrAwhnee0qbGvtEvJ1kHeASP65g4tj6YoHxlksEUta3jD0TLdridJTGTDVv1Dy8sRiDDF/hU9JVrzbl85hTLyEuoQ40g6wapfGsvl+ydAQX1aZDWvFxZS3GJQmHuBGh4aIQJBANvD59AFgOKwm3jXD6S7B+gDienO1JDTVo107ij672btv/osWjrC/NdAmqrD3V4K8ksbHjXSu/9bI2UFU4EETysCQQDStfeg7zNufvqWC8ITShFaUK1SLr+h6P/8Lz594l2S78Hu4dqlhyAtb5YcfUTv5+QKrJwBAKUFwVAqlvVKAQoRAkB5G5jgFmhUhKbpDPtd+IP+5BRYeNbDiPOluM6WXtMXkeW95DtFLrdPmBakDJQtgzLLGQKo/p0DuCHlRe4ip1FJAkEAhQEHxjY3KNBOLNhPMQ3X1kKGpRGNNQ52RyErORriJhXhPICkG0goL4X4IxOPqD+f2n0KHDfj8rYLfFa9jnEJgQJABHEGEaeIJtDqPyjfh5DTOkV8nilq/pPOyOE+R9x++xDADv4lCC99gj/LCVMt2JCdRHJ965KHGvbUn5AkknRP7w==";

        publicKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAyPJhbMQE2fktehR/draD79G9ymMiTuLkA+joWtFO9JYiPRS2XjU/ECJoDHL+nJJNrFz+8Z7emtZakxRKYuAVQgfvdR8svb9bnMGYLUV/IJCxFbuEwZMfoGr9jywT8WhNZFGRKhwl/yRer30cOcA4O90PA7woUj7G8RC4kXEZ4u6fMo/HvXV4JbSEsZS68MFzv8gOXNSRsD37RySgeg01jYMVl22iJ27MkOTWZk/cmfa+0v4hZhjJsqkoH7UNqw6SXCyMDulwgctG0Mf4Uy2+Jb1cmfpgJIHlG8fsE5Nk4ZpFDn2iZ9iUY5Y4yRqD2zHhztZ/iyWReAqlt67Sr39YNQIDAQAB";

        privateKey = "MIIEvAIBADANBgkqhkiG9w0BAQEFAASCBKYwggSiAgEAAoIBAQDI8mFsxATZ+S16FH92toPv0b3KYyJO4uQD6Oha0U70liI9FLZeNT8QImgMcv6ckk2sXP7xnt6a1lqTFEpi4BVCB+91Hyy9v1ucwZgtRX8gkLEVu4TBkx+gav2PLBPxaE1kUZEqHCX/JF6vfRw5wDg73Q8DvChSPsbxELiRcRni7p8yj8e9dXgltISxlLrwwXO/yA5c1JGwPftHJKB6DTWNgxWXbaInbsyQ5NZmT9yZ9r7S/iFmGMmyqSgftQ2rDpJcLIwO6XCBy0bQx/hTLb4lvVyZ+mAkgeUbx+wTk2ThmkUOfaJn2JRjljjJGoPbMeHO1n+LJZF4CqW3rtKvf1g1AgMBAAECggEAYd2aEUzOSBKRUXmWMozlaPEvi9gIJ39dKYJPV5vE5l4QQstJnkw0cHkxT47Z7gtbBO0txSNoquRmrGcfa1RTRhtzRglu0uLVjVzD7piAN64AcOMo3tX2ezxgTVBcTiBMQOVw259l6gMekj1Od9nkCVO6Mkl7IcMtRM6t9TeuMdTEhhVkoSxjKa1IHEmswGMzhaQkboLYQBzHi05d5hz2MBUPScelCuVjxM6JTdtwZ7i8HWS7mgL/0c7HZEVc6eDT2QiTTUkTXyoxw7u6vfaOxRGst2NUNXaLzrqWgKS1LOq1BWDOBuuBcPOcoV6hMyjIMC4GgklJUQzLFmAW+gTHqQKBgQDlzXZ58pC37d+HoZWW7O3x9ZreJtp/A876eMJYJfVclEor5/aPliWrCx+kq1+oixEegWqQOOKvftJEnVY+GBB2Wz7RTDHqZRPH/WvuvxfQ+0rfh/C3V8nSGXr2vNFuQlDZcVK0U5xyFEoh1CLI6QGGAQXoQFiNUn2ENfoilgzUowKBgQDf2ssImkwBN1TGD50GYvNI/1sASnN7YdmpyFIzHkjNdrOuMsEMKaw2Jlr4G3/yEmKB7474qvgCMtLy8svhf4ltt9eKwC3rAkCflQcgxr/Dhq4VPnBsA3oxioCiBw5FuVKMWmfwaD70UPUBgIf/2CNnErnGZwTPcu7FoT/NMnUVRwKBgAKzMUspgG3Iy7GKQnmtevY2zoUBq9uGLe5fcNkcSQ+3Zk+xwAHb38Fstgh3Qv5189OJ7biYEoHBWbMYriS4n6jkVY6b+JIqcdsNrTD26c64xR5vOHLPAz4Gsp0nhsZm3RBQ9onL2bC1cFGJbocxxaTPakRaTTAL4hGlmLTDUo9FAoGAAQVzha+Ghz7kYR7zHXNYHs4jcSXlzvtMTez/CwKpXF6dRT8wCEksYvbb1WPyun0A/AzGvzWwefoXYkpfScEWNGzxS83Cp97TwqagaLLfbxnvM2OcibGeXhl+qr6TfxfwW3mSHdOr7dtssrwOYslup+q69D/GEtg4ZKhurRX82MMCgYB5LH6irNZryT91/WTFiMDHzCJNfpIqpttPLzbSBzA4wWd5ov1Dus3mgIS7sLRfSO6Tl5I2Ni9reQgirUYMcytbGEZXRzyuYr27/04sMQWIZ6MFZAUXC9ILVI1fCmIZL/NOdJ/hi6ywRgFNmRZyb7sYPaALG6iiDPaYYfzyRB75Rg==";

      SecureRandom secureRandom = new SecureRandom();
      secureRandom.setSeed("1234567890123456".getBytes());
      byte[] rand  = new byte[999];
        secureRandom.nextBytes(rand);
        System.out.println(Base64.getEncoder().encodeToString(rand));
String ss = "pMBAusbsM3xHwI9s/5nzapcH3E2fkRI+OfcdF9L1jD2i+GD7+BpWUSap02twi/lR83TktPpV2M8a4ePw0/fS1a/CY3DkyIisx9IWPaFMiwOhdFV/JGeFIrPiyWw80SqioxMKyDfmGNHMxyvzIdOBITPQfSIoi6s1rGyEEjhLw92ujXF9AE+M5GBCyhvBVenwvZzg2/3HNY9pnN1XFUlcSB//+ssFITgbZZZX790B0nSudhILukUjlo8rd6CVm9mNU1sbNEeYFSSpoPAU5YXWXM/USdUXq4cU1OzUkowkOK62YfAVL1ufIUafzAdQzKiHsMcKcGGg5V6MicqA6MDCUsWsaMuGP1SFaKoENBnM9FwhGk+NLxjByd4ptPWK7WfXq3BnXo7ptJJMd5oaXVaAojA9n512/0YecQhYHle0MK/yICdIZsOle+p33VQkrMDB5ySVsYuxgRZ6eyXg+wYvR1zH5sXh/Uw4gcHfIs6moWe/iCCkxk1TNRQQUpiRsKhap5kRaiQOliNOlMZH3zjY4qnI2y5FPjEht16SjYlqkW2iVj8g1n+rXZOk4Tttep6yvZNja/Es2hB0b1uf7qCXR4l38hN6KqMPYZHkzZz9ZzPA3vMFFcNdcyOh8QAaWaoRSW93S6/DFbevI6d3SP1K3kftl5rAGo88MVhiR1NHv0ZgBnRA8GxaU65Ru9RdpdaD58IPSDU0birPdpXNQRVLTZfcxp0RS9zXcNmL43UVj+H6oxTQjvfn0FXAB6pDoxFHHAtzJwLTNCMfAAISfaSxG/luOLgH1Sbk60jsni9XpuA2sZLupwSipsm5fiGMzoE9LcpEQmieRL8Kf5V4vsQcmZO3ULbDBXRn4xMX2F9OREapGmciCFxjHF0vkHgetdzqbgEC6Co84WEIT6xIhtOFb986kRL8N+1R1/4T/SKhEcXyTUQA7i3mJ6duxvhkI7Ao7jl9nap+mAFl5yqyosKitzCn+ka4k+nBC2+MHuCMbw3JhgQR+Uj1iZ9ekQ2HeVAcpCzrKhI4ZJD0fouBO0MkrGN7PWOLu86Fcj2husvQUCgj98jYjMG9mRewezAhTXwB8d5E1QbMJY2umV9WIdt6RNKVXoLHYJUne40wNsXnlVVhox/2Su4ZJI3adFWReLeEERCfl/eQrMuwUeF1E+Kohw0kRamDShvBnPuJZ5UKVZNNiUrJR3z+5dSLtjDXgGtfdkpC4bVpkNbVxfovhvNR88j7P4cQdOhF1mCuvrdYNGJEgCNS/wpSndJuIOm1TFA7nMUV5nIWmVXwANNGCC8A0j1GIpFhtzI6/E+Lb5yzzX9hj3BMpMrN";
        System.out.println(ss.equals((Base64.getEncoder().encodeToString(rand))));
//        getRSAKey();
//        HashMap<String, String> map = new HashMap<String, String>() {
//            {
//                put("trxnCode", "23214213");
//                put("timestamp", "232132111d223321");
//                put("nonce", "232132132dd1");
//                put("appId", "2321sdfsfdf321321");
//                put("refreshToken", "232132132dsfdsafds1");
//
//            }
//        };
//
//        String result = getSign(map);
//        System.out.println(result);
//        String sign = "n1cu+k+7eNXrmExfKU8jHibLtSZWUMdmzYPklYA/LTVCq2iZpxJw4ElEFXw5Od1YjNOq05popZMXpGomnzKdUxAhtcuJuaYOt313HMgdcFcHWZx2HiluYajf5FYntE0r4BSBNrWumI0+W5mWU6FGiyhyhP7jktnp9Z3sVbvS2Ec=";
//
        String key = "hi test data  hi test data  hi test data  hi test data  hi test data  hi test data  hi test data  hi test data  hi test data  hi test data  ";
//        System.out.println(new String(Base64.getEncoder().encode(encryptPublicRSA(key.getBytes(),publicKey))));
        System.out.println(new String(Base64.getEncoder().encode(encryptPrivateRSA(key.getBytes(),privateKey))));

        String value = "XMaliI0PaAuakLFUkeIAtnKl8K/U4oTyjn+EIGQwk5bISAHiSYWnJbnmS0XU3XO86rbAPPSA9ZGlUrv+drXuVqgD8/wWDin2n5Rk3Mya3Eul01vwSqcjyavB9ZNE275+qy1FXE0L+XUCoaRwr60zWoImuMxoSMhJgLdfkGnE26su+P7C/C5v82F5eDSnMGqJY58yvOxJ6iNDtk8E1BhmN5qvupFDsgjDXCaqerAI8DMCezcrtlDu8U/MVJu5/3n6TX+7YwdS8Xp1BOcYIsUYtNgABNTlQF51+JFNUdlD8A1bY02o7p2Wz9+VGPnNITe0zVzLMGwrpwDN6p2jFVIawQ==";
//        org.apache.tomcat.util.codec.binary.Base64.decodeBase64()
//        System.out.println(new String(dencryptPrivateRSA(org.apache.tomcat.util.codec.binary.Base64.decodeBase64(value),privateKey)));
//
//        String value = "=lsiFGKGXRzwhmgh5WrWsY0CaihowadzYIr57qno4PmW0QFpvUMsYm+BYbhdfoYQUS0RK9h5wdNDb0QXI/2yCnH3szhVnB4I4s1oQ5deI1RlEKr9ymc0ZnSJlJCUXxTG4B+8pUfYS+kFNekv7gyceKL7HFC63O/PwSuxyFd3n1OVZwrYJq9yPPHZKJm+EG9r8UlvBP0V5DcVi1aFin8ZWfOBTLqg0OJ6zek3ngUtDTte06FRrfUQlecmX8GVEDx8wQAORDwi4rtu06lltOKVxS0m0TVTEcRHBMW0Zy3Nng6CG9tVnVC47+CnVrLDcH3EYE2+W57bdlVkZtxMkU7YuZA==";
//        System.out.println(Base64.getDecoder().decode(value).length);
        System.out.println(new String(dencryptPublicRSA(Base64.getDecoder().decode(value),publicKey)));
//
//        String content = "{name:21321,age:234}";
//        String sendContent = encryptAES(content,key);
//        System.out.println(sendContent);
//        System.out.println(decryptAES(sendContent,key));

//        String code = MD5.encode32(UUID.randomUUID().toString());
//        System.out.println(code);
//        String codeValue = new String(Base64.getEncoder().encode(Oauth2Utils.encryptPublicRSA(code.getBytes())));
//        System.out.println(codeValue);
//
//        code = new String(Oauth2Utils.dencryptPrivateRSA(Base64.getDecoder().decode(codeValue)));
//        System.out.println(code);


//        String val = "Qjvr8eUXhxBB7IwSSSYRrcVcGxFuZRQnc32y+wAd+hFtt4+exliF0aXjk0zmk1fTn17wDAajk4StZolfdcCLiHR91or+UZF238FnN3Gr5QDiCjveIOZ5kpncQUGXO/8jR7A3p21avmYboRnIhyoPu8QqYJtJ/qjD1GcCS8/9iKQ=";
//        System.out.println(new String(dencryptPrivateRSA(Base64.getDecoder().decode(val),privateKey)));

        System.out.println(System.currentTimeMillis());

        String content = "{\"OPENSESSION\":\"1552ba34-207b-431a-adb8-7b1f99a226de\"}";
//        System.out.println(Base64.getEncoder().encodeToString(encryptPrivateRSA(content.getBytes(),privateKey)));
//        System.out.println(new String(encryptPrivateRSA(Base64.getEncoder().encode(content.getBytes()),privateKey)));

//        value = "omwECy0ZhMskhyfnfUHoGg0PPZFBu6vP4m1Ntk4JDHDOYMu2FQmZnBNjjza5N3lKKApj4h1Wf2uk\\nEaU3QmCNQA==";
//        value = "omwECy0ZhMskhyfnfUHoGg0PPZFBu6vP4m1Ntk4JDHDOYMu2FQmZnBNjjza5N3lKKApj4h1Wf2ukEaU3QmCNQA==";
//        System.out.println(decryptAES(value,"8c7e73387c48925f2916e2c2379f07ce"));
//        value = "abc";
//        System.out.println(encryptAES(value,"8c7e73387c48925f2916e2c2379f07ce"));

        System.out.println("---------------");
//        value = "阿喀琉斯打飞机啊看过了可是大家分了分了klsfjlsafjlajelrjorjlejfklskdjflksadjfkldsajglsafsd;lfjlksgjslddfsd";
//        System.out.println(encryptAES(value,"8c7e73387c48925f2916e2c2379f07ce"));

    }
}
