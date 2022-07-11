package com.telcoilng.fraudprocessor.utils;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.crypto.CryptoException;
import org.bouncycastle.util.encoders.DecoderException;
import org.jpos.iso.ISOBasePackager;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.packager.GenericPackager;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class EncryptionUtil {

    public EncryptionUtil() {

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

    public static byte[] hexStringToByteArray(final String s) {
        final int len = s.length();
        final byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte)((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    public String decryptPinBlock(String pinBlock, String key) throws CryptoException {
        try {
            byte[] tmsKeyBytes = Hex.decodeHex(key.toCharArray());
            byte[] pinBlockBytes = Hex.decodeHex(pinBlock.toCharArray());

            byte[] clearPinBlockBytes = EncryptionUtil.tdesDecryptECB(pinBlockBytes, tmsKeyBytes);

            return new String(Hex.encodeHex(clearPinBlockBytes));
        } catch (DecoderException | org.apache.commons.codec.DecoderException e) {
            throw new CryptoException("Could not decode hex key", e);
        }
    }

    public String encryptPinBlock(String pinBlock, String key) throws CryptoException {
        if (StringUtils.isEmpty(pinBlock)) {
            return pinBlock;
        }
        byte[] clearPinBlockBytes;
        byte[] zpk;
        try {
            clearPinBlockBytes = Hex.decodeHex(pinBlock.toCharArray());
            zpk = Hex.decodeHex(key.toCharArray());
        } catch (DecoderException | org.apache.commons.codec.DecoderException e) {
            throw new CryptoException("Could not decode pin block for Threeline", e);
        }
        byte[] encryptedPinBlockBytes = EncryptionUtil.tdesEncryptECB(clearPinBlockBytes, zpk);
        return new String(Hex.encodeHex(encryptedPinBlockBytes));

    }

    public static String generateHash256Value(final byte[] iso, final byte[] key) {
        String hashText = null;
        try {
            final MessageDigest m = MessageDigest.getInstance("SHA-256");
            m.update(key, 0, key.length);
            m.update(iso, 0, iso.length);
            hashText = bytesToHex(m.digest());
            hashText = hashText.replace(" ", "");
        }
        catch (NoSuchAlgorithmException ex) {
            System.out.println("Hashing ");
        }
        if (hashText.length() < 64) {
            final int numberOfZeroes = 64 - hashText.length();
            String zeroes = "";
            String temp = hashText.toString();
            for (int i = 0; i < numberOfZeroes; ++i) {
                zeroes += "0";
            }
            temp = zeroes + temp;
            System.out.println("Utility :: generateHash256Value :: HashValue with zeroes: {}" + (Object)temp);
            return temp;
        }
        return hashText;
    }

    public static String bytesToHex(final byte[] bytes) {
        if (bytes != null) {
            final char[] hexArray = "0123456789ABCDEF".toCharArray();
            final char[] hexChars = new char[bytes.length * 2];
            for (int j = 0; j < bytes.length; ++j) {
                final int v = bytes[j] & 0xFF;
                hexChars[j * 2] = hexArray[v >>> 4];
                hexChars[j * 2 + 1] = hexArray[v & 0xF];
            }
            return new String(hexChars);
        }
        return "";
    }

}
