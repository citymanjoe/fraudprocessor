package com.telcoilng.fraudprocessor.utils;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.crypto.CryptoException;
import org.bouncycastle.util.encoders.DecoderException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CryptoUtilImpl implements CryptoUtil {
    private final Logger logger = LoggerFactory.getLogger(CryptoUtilImpl.class);
    @Override
    public String translatePin(String pinblock, String tpk, String zpk) {
        String translatedPin = "";

        try {
            translatedPin = decryptPinBlock(pinblock,tpk);
            translatedPin = encryptPinBlock(translatedPin,zpk);
        } catch (CryptoException e) {
            e.printStackTrace();
            logger.info(e.getLocalizedMessage());
        }



        return translatedPin;
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
        logger.info("The pin block bytes {} ", pinBlock);
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
}
