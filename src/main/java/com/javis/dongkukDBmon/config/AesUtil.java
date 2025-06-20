package com.javis.dongkukDBmon.config;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;


public class AesUtil {
    private static final String ALGORITHM = "AES";

    public static String encrypt(String key, String plainText) throws Exception {
        if (!(key.length() == 16 || key.length() == 24 || key.length() == 32)) {
            throw new IllegalArgumentException("AES 키는 16/24/32 바이트만 가능합니다. 현재 길이: " + key.length());
        }

        Cipher cipher = Cipher.getInstance(ALGORITHM);
        SecretKeySpec keySpec = new SecretKeySpec(key.getBytes("UTF-8"), ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, keySpec);
        byte[] encrypted = cipher.doFinal(plainText.getBytes("UTF-8"));
        return Base64.getEncoder().encodeToString(encrypted);
    }


    public static String decrypt(String key, String encryptedText) throws Exception {
        if (!(key.length() == 16 || key.length() == 24 || key.length() == 32)) {
            throw new IllegalArgumentException("AES 키는 16/24/32 바이트만 가능합니다. 현재 길이: " + key.length());
        }

        try {
            // Base64로 인코딩된 값인지 확인
            byte[] decoded = Base64.getDecoder().decode(encryptedText);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(key.getBytes("UTF-8"), ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, keySpec);
            byte[] decrypted = cipher.doFinal(decoded);
            return new String(decrypted, "UTF-8");

        } catch (IllegalArgumentException | javax.crypto.BadPaddingException e) {
            // Base64 디코딩 실패 or 복호화 실패 (잘못된 암호문이거나 그냥 평문일 수 있음)
            return encryptedText; // 평문 그대로 반환
        }
    }

}
