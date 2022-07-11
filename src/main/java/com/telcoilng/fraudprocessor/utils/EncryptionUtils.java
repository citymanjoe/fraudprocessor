package com.telcoilng.fraudprocessor.utils;

import org.bouncycastle.crypto.CryptoException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

/**
 * Created by ... on 24/11/2018.
 */
public final class EncryptionUtils {

    private EncryptionUtils() {

    }

    public static byte[] tdesEncryptECB(byte[] data, byte[] keyBytes) throws CryptoException {
        try {
            byte[] key;
            if (keyBytes.length == 16) {
                key = new byte[24];
                System.arraycopy(keyBytes, 0, key, 0, 16);
                System.arraycopy(keyBytes, 0, key, 16, 8);
            } else {
                key = keyBytes;
            }

            Cipher cipher = Cipher.getInstance("DESede/ECB/NoPadding");
            cipher.init(1, new SecretKeySpec(key, "DESede"));
            return cipher.doFinal(data);
        } catch (InvalidKeyException | NoSuchPaddingException | BadPaddingException | IllegalBlockSizeException | NoSuchAlgorithmException var6) {
            String msg = "Could not TDES encrypt ";
            throw new CryptoException(msg, var6);
        }
    }

    public static byte[] tdesDecryptECB(byte[] data, byte[] keyBytes) throws CryptoException {
        try {
            byte[] key;
            if (keyBytes.length == 16) {
                key = new byte[24];
                System.arraycopy(keyBytes, 0, key, 0, 16);
                System.arraycopy(keyBytes, 0, key, 16, 8);
            } else {
                key = keyBytes;
            }

            Cipher cipher = Cipher.getInstance("DESede/ECB/NoPadding");
            cipher.init(2, new SecretKeySpec(key, "DESede"));
            return cipher.doFinal(data);
        } catch (InvalidKeyException | NoSuchPaddingException | BadPaddingException | IllegalBlockSizeException | NoSuchAlgorithmException var6) {
            String msg = "Could not TDES decrypt ";
            throw new CryptoException(msg, var6);
        }
    }

    public static byte[] generateKeyCheckValue(byte[] key) throws CryptoException {
        byte[] zeroBytes = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        byte[] fullKeyCheck = tdesEncryptECB(zeroBytes, key);
        byte[] keyCheckValue = new byte[3];
        System.arraycopy(fullKeyCheck, 0, keyCheckValue, 0, keyCheckValue.length);

        return keyCheckValue;
    }

    public static byte[] exclusiveOr(byte[] data1, byte[] data2) {
        byte[] result = new byte[Math.min(data1.length, data2.length)];
        for (int i = 0; i < result.length; i++) {
            result[i] = (byte) (((int) data1[i]) ^ ((int) data2[i]));
        }
        return result;
    }
}