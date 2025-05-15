package io.github.gongding.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public class PasswordUtils {
    //指定哈希算法为SHA-256
    private static final String HASH_ALGORITHM = "SHA-256";
    //指定盐值的长度（字节数）
    private static final int SALT_LENGTH = 16;

    /**
     * 生成随机的盐值
     * @return 返回一个十六进制字符串表示的盐值
     */
    public static String generateSalt() {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[SALT_LENGTH];
        random.nextBytes(salt);
        return bytesToHex(salt);
    }

    /**
     * 对密码进行加盐哈希
     * @param password 要哈希的密码
     * @param salt 用于加盐的盐值（十六进制字符串）
     * @return 返回哈希后的密码（十六进制字符串）
     */
    public static String hashPassword(String password, String salt) {
        try {
            MessageDigest md = MessageDigest.getInstance(HASH_ALGORITHM);
            md.update(hexToBytes(salt));
            byte[] hashedBytes = md.digest(password.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hashedBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("不支持的安全算法：" + HASH_ALGORITHM, e);
        }
    }

    /**
     * 将字节数组转换为十六进制字符串
     * @param bytes 要转换的字节数组
     * @return 返回转换后的十六进制字符串
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }

    /**
     * 将十六进制字符串转换为字节数组
     * @param hex 要转换的十六进制字符串
     * @return 返回转换后的字节数组
     */
    private static byte[] hexToBytes(String hex) {
        byte[] result = new byte[hex.length() / 2];
        for (int i = 0; i < hex.length() / 2; i++) {
            int high = Integer.parseInt(hex.substring(i * 2, i * 2 + 1), 16);
            int low = Integer.parseInt(hex.substring(i * 2 + 1, i * 2 + 2), 16);
            result[i] = (byte) (high * 16 + low);
        }
        return result;
    }
}
