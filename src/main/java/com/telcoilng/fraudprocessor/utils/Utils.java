package com.telcoilng.fraudprocessor.utils;

import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.crypto.CryptoException;
import org.bouncycastle.util.encoders.DecoderException;
import org.bouncycastle.util.encoders.Hex;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

import static com.telcoilng.fraudprocessor.utils.EncryptionUtil.bytesToHex;

public class Utils {



    public static String getMac(String seed, byte[] macDataBytes) throws Exception{
        byte [] keyBytes = h2b(seed);
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        digest.update(keyBytes, 0, keyBytes.length);
        digest.update(macDataBytes, 0, macDataBytes.length);
        byte[] hashedBytes = digest.digest();
        String hashText = b2h(hashedBytes);
        hashText = hashText.replace(" ", "");

        if (hashText.length() < 64) {
            int numberOfZeroes = 64 - hashText.length();
            String zeroes = "";
            String temp = hashText.toString();
            for (int i = 0; i < numberOfZeroes; i++)
                zeroes = zeroes + "0";
            temp = zeroes + temp;
            //Log.i("Utility :: generateHash256Value :: HashValue with zeroes: ", temp);

            return temp;
        }

        return hashText;
    }


    public static byte[] h2b(String hex)
    {
        if ((hex.length() & 0x01) == 0x01)
            throw new IllegalArgumentException();
        byte[] bytes = new byte[hex.length() / 2];
        for (int idx = 0; idx < bytes.length; ++idx) {
            int hi = Character.digit((int) hex.charAt(idx * 2), 16);
            int lo = Character.digit((int) hex.charAt(idx * 2 + 1), 16);
            if ((hi < 0) || (lo < 0))
                throw new IllegalArgumentException();
            bytes[idx] = (byte) ((hi << 4) | lo);
        }
        return bytes;
    }

    public static String b2h(byte[] bytes)
    {
        char[] hex = new char[bytes.length * 2];
        for (int idx = 0; idx < bytes.length; ++idx) {
            int hi = (bytes[idx] & 0xF0) >>> 4;
            int lo = (bytes[idx] & 0x0F);
            hex[idx * 2] = (char) (hi < 10 ? '0' + hi : 'A' - 10 + hi);
            hex[idx * 2 + 1] = (char) (lo < 10 ? '0' + lo : 'A' - 10 + lo);
        }
        return new String(hex);
    }

    public static String leftPad(String str, int len, char pad) {
        if(str == null)
            return null;
        StringBuilder sb = new StringBuilder();
        while (sb.length() + str.length() < len) {
            sb.append(pad);
        }
        sb.append(str);
        String paddedString = sb.toString();
        return paddedString;
    }

    public static String rightPad(String str, int len, char pad) {

        if(str == null)
            return null;
        StringBuilder sb = new StringBuilder();
        sb.append(str);
        while (sb.length() < len) {
            sb.append(pad);
        }
        String paddedString = sb.toString();
        return paddedString;
    }

    public static byte[] generateHash256Value(final byte[] iso, final byte[] key) throws org.apache.commons.codec.DecoderException {
        String hashText = null;
        try {
            final MessageDigest m = MessageDigest.getInstance("SHA-256");
            m.update(key, 0, key.length);
            m.update(iso, 0, iso.length);
            hashText = b2h(m.digest());
            hashText = hashText.replace(" ", "");
        }
        catch (NoSuchAlgorithmException ex) {
            System.out.println("Hashing " );
        }
        if (hashText.length() < 64) {
            final int numberOfZeroes = 64 - hashText.length();
            String zeroes = "";
            String temp = hashText.toString();
            for (int i = 0; i < numberOfZeroes; ++i) {
                zeroes += "0";
            }
            temp = zeroes + temp;
            System.out.println("Utility :: generateHash256Value :: HashValue with zeroes: {}"+ (Object)temp);
            return org.apache.commons.codec.binary.Hex.decodeHex(temp);
        }
        return org.apache.commons.codec.binary.Hex.decodeHex(hashText);
    }

    public static boolean validateMAC(final ISOMsg msg, final String SessionKey) throws ISOException, org.apache.commons.codec.DecoderException {
        final String requestfield128 = msg.getString(128);
        final byte[] sessionKeyBytes = h2b(SessionKey);
        final byte[] bites = msg.pack();
        final int length = bites.length;
        final byte[] temp = new byte[length - 64];
        if (length >= 64) {
            System.arraycopy(bites, 0, temp, 0, length - 64);
        }
        String correctfield128 = Hex.toHexString(generateHash256Value(temp, sessionKeyBytes));
        return Objects.equals(requestfield128.toUpperCase(), correctfield128.toUpperCase());
    }

    public static String decryptKey(String encryptedKey, String key) throws CryptoException, org.apache.commons.codec.DecoderException {
        try {
            byte[] tmsKeyBytes = org.apache.commons.codec.binary.Hex.decodeHex(key.toCharArray());
            byte[] pinBlockBytes = org.apache.commons.codec.binary.Hex.decodeHex(encryptedKey.toCharArray());

            byte[] clearPinBlockBytes = EncryptionUtils.tdesDecryptECB(pinBlockBytes, tmsKeyBytes);

            return new String(org.apache.commons.codec.binary.Hex.encodeHex(clearPinBlockBytes));
        } catch (DecoderException e) {
            throw new CryptoException("Could not decode hex key", e);
        }
    }

    public String encryptPinBlock(String pinBlock, String key) throws CryptoException, org.apache.commons.codec.DecoderException {
        if (StringUtils.isEmpty(pinBlock)) {
            return pinBlock;
        }
        byte[] clearPinBlockBytes;
        byte[] zpk;
        try {
            clearPinBlockBytes = org.apache.commons.codec.binary.Hex.decodeHex(pinBlock.toCharArray());
            zpk = org.apache.commons.codec.binary.Hex.decodeHex(key.toCharArray());
        } catch (DecoderException e) {
            throw new CryptoException("Could not decode pin block for Threeline", e);
        }

        byte[] encryptedPinBlockBytes = EncryptionUtils.tdesEncryptECB(clearPinBlockBytes, zpk);

        return new String(org.apache.commons.codec.binary.Hex.encodeHex(encryptedPinBlockBytes));

    }

    public static byte[] hexStringToByteArray(final String s) {
        final int len = s.length();
        final byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte)((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    public static String  generateMAC(final ISOMsg msg, final byte[] sessionKeyBytes) throws ISOException {
        final byte[] bites = msg.pack();
        final int length = bites.length;
        final byte[] temp = new byte[length - 64];
        if (length >= 64) {
            System.arraycopy(bites, 0, temp, 0, length - 64);
        }
        return generateHash256ValueString(temp, sessionKeyBytes);

    }


    private static String generateHash256ValueString(final byte[] iso, final byte[] key) {
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
            String temp = hashText;
            for (int i = 0; i < numberOfZeroes; ++i) {
                zeroes += "0";
            }
            temp = zeroes + temp;
            System.out.println("Utility :: generateHash256Value :: HashValue with zeroes: {}" + temp);
            return temp;
        }
        return hashText;
    }



}
