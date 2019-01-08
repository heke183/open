package com.xianglin.open.util;

import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.regex.Pattern;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;

import org.springframework.util.Assert;

/**
 *   <B>说       明</B>:加解密工具基础支持类。
 *   
 *   <p>该类作为其他加解密工具的支持类，仅包内可见。
 */
class EncryptionBase {

	/**
	 * 匹配需要初始化向量的转换名称的模式。
	 */
	private static final Pattern TRANSFORMATION_NOTNEEDIV = Pattern.compile("^[^/]+/(ECB)/[^/]+$");

	/**
	 * 生成一个随机的密钥。
	 * 
	 * @param algorithm 所请求的密钥算法的标准名称。
	 * @param keySize 密钥长度。
	 * @return 
	 *      字节数组形式的密钥。
	 * @throws IllegalStateException 如果不支持algorithm对应的算法。
	 */
	public static byte[] generateKey(String algorithm, int keySize){
		try {
			KeyGenerator keyGenerator = KeyGenerator.getInstance(algorithm);
			keyGenerator.init(keySize);
			SecretKey secretKey = keyGenerator.generateKey();
			return secretKey.getEncoded();
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalArgumentException(e);
		}
	}

	/**
	 * 处理数据。
	 * 
	 * @param data 待处理数据。
	 * @param secretKey 密钥。
	 * @param transformation 转换的名称，例如 DES/CBC/PKCS5Padding。
	 * @param opmode Cipher的操作模式，如:ENCRYPT_MODE、DECRYPT_MODE、WRAP_MODE 或 UNWRAP_MOD。
	 * @param parameterSpec 参数规范。
	 * @return 
	 *      处理后的数据。
	 * @throws IllegalStateException 在处理过程中发生任何异常。
	 */
	public static byte[] processData(byte[] data, Key secretKey, String transformation, int opmode, AlgorithmParameterSpec parameterSpec){
		Assert.notNull(data, "要进行处理的数据不能null!");
		Assert.notNull(secretKey, "密钥不能为null!");
		Assert.hasLength(transformation, "转换名称不能为null或空!");
		Assert.notNull(parameterSpec, "算法参数不能为null!");
		try {
			Cipher cipher = Cipher.getInstance(transformation);
			//判断Cipher的工作模式是否需要初始化向量。
			boolean isNeedIV = !TRANSFORMATION_NOTNEEDIV.matcher(cipher.getAlgorithm()).matches();
			if(isNeedIV){
				cipher.init(opmode, secretKey,parameterSpec);
			}else{
				cipher.init(opmode, secretKey);
			}
			return cipher.doFinal(data);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalArgumentException(e);
		} catch (InvalidKeyException e) {
			throw new IllegalArgumentException(e);
		} catch (Exception e) {
			throw new IllegalStateException(e);
		} 
	}

	/**
	 * 根据算法通过密钥内容得到密钥对象。
	 * 
	 * @param keySpec 密钥内容规范。
	 * @param algorithm 算法名称。
	 * @return
	 *      密钥对象。
	 * @throws IllegalArgumentException 如果keySpec为null或不合法，或者algorithm为null或不被支持。
	 */
	public static Key toKey(KeySpec keySpec, String algorithm){
		try {
			Assert.notNull(keySpec, "密钥内容规范不能为null!");
			Assert.notNull(algorithm, "请求的密钥算法标准名称不能为null!");
			SecretKeyFactory keyFactory = SecretKeyFactory.getInstance(algorithm);
			return keyFactory.generateSecret(keySpec);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalArgumentException(e);
		} catch (InvalidKeySpecException e) {
			throw new IllegalArgumentException(e);
		}
	}
	
	/**
	 * 将字节数组形式的私钥转换成私钥对象。
	 * 
	 * @param key 字节数组形式的私钥。
	 * @param algorithm 算法名称。
	 * @return
	 *      私钥对象。
	 * @throws IllegalArgumentException
	 *      如果key为null或者algorithm为null； 
	 *      如果密钥材料不合法或者系统不支持algorithm算法。
	 */
	public static PrivateKey toPrivateKey(byte[] key, String algorithm){
		Assert.notNull(key, "要进行转换的字节数组形式的私钥不能为null!");
		Assert.notNull(algorithm, "算法名称不能为null!");
		try {
			PKCS8EncodedKeySpec pkcs8KeySpec = new PKCS8EncodedKeySpec(key);
			KeyFactory keyFactory = KeyFactory.getInstance(algorithm);
			return keyFactory.generatePrivate(pkcs8KeySpec);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalArgumentException(e);
		} catch (InvalidKeySpecException e) {
			throw new IllegalArgumentException(e);
		}
	}

	/**
	 * 将字节数组形式的公钥转换成公钥对象。
	 * 
	 * @param key 字节数组形式的公钥。
	 * @param algorithm 算法名称。
	 * @return
	 *      公钥对象。
	 * @throws IllegalArgumentException
	 *      如果key为null或者algorithm为null； 
	 *      如果密钥材料不合法或者系统不支持algorithm算法。
	 */
	public static PublicKey toPublicKey(byte[] key, String algorithm){
		Assert.notNull(algorithm, "算法名称不能为null!");
		Assert.notNull(key, "要进行转换的字节数组形式的公钥不能为null!");
		try {
			X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(key);
			KeyFactory keyFactory = KeyFactory.getInstance(algorithm);
			return keyFactory.generatePublic(x509KeySpec);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalArgumentException(e);
		} catch (InvalidKeySpecException e) {
			throw new IllegalArgumentException(e);
		}
	}

	private EncryptionBase(){}

}
