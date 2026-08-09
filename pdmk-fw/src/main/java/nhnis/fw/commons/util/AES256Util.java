package nhnis.fw.commons.util;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AES256Util {

    private static Logger LOGGER = LoggerFactory.getLogger(AES256Util.class);

    private static final String ALG = "AES/CBC/PKCS5Padding";
    private static final String AES = "AES";

    private String iv;

    public String encrypt(String key, String plainText) throws Throwable {
        try {
            this.iv = key.substring(0, 16);
            Cipher cipher = Cipher.getInstance(ALG);
            SecretKeySpec keySpec = new SecretKeySpec(this.iv.getBytes(), AES);
            IvParameterSpec spec = new IvParameterSpec(this.iv.getBytes());
            cipher.init(1, keySpec, spec);
            byte[] encrypt = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypt);
        } catch (Exception e) {
            LOGGER.error("[AES256Util] Encrypt fail: {}", e.getMessage());
            throw e;
        }
    }

    public String decrypt(String key, String cipherText) throws Exception {
        try {
            this.iv = key.substring(0, 16);
            Cipher cipher = Cipher.getInstance(ALG);
            SecretKeySpec keySpec = new SecretKeySpec(this.iv.getBytes(), AES);
            IvParameterSpec spec = new IvParameterSpec(this.iv.getBytes());
            cipher.init(2, keySpec, spec);
            byte[] decodeBytes = Base64.getDecoder().decode(cipherText);
            byte[] decrypt = cipher.doFinal(decodeBytes);
            return new String(decrypt, StandardCharsets.UTF_8);
        } catch (Exception e) {
            LOGGER.error("[AES256Util] Decrypt fail: {}", e.getCause());
            throw e;
        }
    }
}
