package com.momentum.backend.config;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

// Encrypts the LearnTech API key before it's saved in the database (AES-GCM,
// with a key made from jwt.secret), so a copy of the database alone can't
// reveal it. A new jwt.secret means the admin enters the key again.
public class Encryption {

    private final SecretKeySpec key;
    private final SecureRandom random = new SecureRandom();

    public Encryption(Config config) {
        try {
            // SHA-256 turns the secret, whatever its length, into exactly 32 bytes, the size AES needs.
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(("momentum-settings:" + config.get("jwt.secret")).getBytes(StandardCharsets.UTF_8));
            this.key = new SecretKeySpec(hash, "AES");
        } catch (Exception e) {
            throw new IllegalStateException("Could not set up encryption", e);
        }
    }

    // Gives back the encrypted text, safe to store.
    public String encrypt(String plainText) {
        try {
            // A new random starting value (IV) every time, so the same key never
            // encrypts to the same text twice. It is stored in front of the result.
            byte[] iv = new byte[12];
            random.nextBytes(iv);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(128, iv));
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            byte[] result = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, result, 0, iv.length);
            System.arraycopy(encrypted, 0, result, iv.length, encrypted.length);
            return Base64.getEncoder().encodeToString(result);
        } catch (Exception e) {
            throw new IllegalStateException("Could not encrypt", e);
        }
    }

    // Gives back the original text. Throws if it was changed or made with a different secret.
    public String decrypt(String encryptedText) {
        try {
            byte[] data = Base64.getDecoder().decode(encryptedText);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, data, 0, 12));
            byte[] plain = cipher.doFinal(data, 12, data.length - 12);
            return new String(plain, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Could not decrypt the saved API settings", e);
        }
    }
}
