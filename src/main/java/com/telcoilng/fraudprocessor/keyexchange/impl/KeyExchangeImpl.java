package com.telcoilng.fraudprocessor.keyexchange.impl;

import com.telcoilng.fraudprocessor.Entity.Terminals;
import com.telcoilng.fraudprocessor.Entity.TerminalKeys;
import com.telcoilng.fraudprocessor.cache.StaticKeyMap;
import com.telcoilng.fraudprocessor.keyexchange.KeyExchange;
import com.telcoilng.fraudprocessor.repository.TerminalRepository;
import com.telcoilng.fraudprocessor.repository.TerminalKeyRepository;
import com.telcoilng.fraudprocessor.utils.EncryptionUtils;
import com.telcoilng.fraudprocessor.utils.constants.TransactionType;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Hex;
import org.bouncycastle.crypto.CryptoException;
import org.bouncycastle.util.encoders.DecoderException;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOUtil;
import org.jpos.transaction.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@Slf4j
public class KeyExchangeImpl implements KeyExchange, TransactionType {
    private static final String ERROR_RESPONSE  = "Kindly perform masterKey request";
    ExecutorService executors = Executors.newCachedThreadPool();
    @Value("${useHsm}")
    boolean useHsm;
    private static final String LEADING_ZEROES  = "00000000000000000000000000000000";
    private final TerminalKeyRepository terminalKeyRepository;
    private final TerminalRepository terminalsRepository;
    @Value("${zmk}")
    String zmkKey;
    KeyGenerator generator;

    {
        try {
            generator = KeyGenerator.getInstance("DES");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    @Autowired
    public KeyExchangeImpl(TerminalKeyRepository terminalKeyRepository, TerminalRepository terminalsRepository) {
        this.terminalKeyRepository = terminalKeyRepository;
        this.terminalsRepository = terminalsRepository;
    }


    @Override
    public ISOMsg doKeyExchange(ISOMsg request, Context context) {
        ISOMsg r = (ISOMsg) request.clone();
        if (!useHsm){
            try {
                String processingCode = request.getString(3).substring(0, 2);
                switch (processingCode) {
                    case masterKeyRequest:
                        generator = KeyGenerator.getInstance("AES");
                        generator.init(128);
                        SecretKey clearTmk = generator.generateKey();
                        SecretKey clearTsk = generator.generateKey();
                        SecretKey clearTpk = generator.generateKey();


                        String tmkKey = ISOUtil.byte2hex(clearTmk.getEncoded()).toUpperCase();//get the key in Hex

                        String tmkkcv = encrypt(LEADING_ZEROES, tmkKey).toUpperCase().substring(0, 6);//get the kcv

                        byte[] tmkUnderZMK = ISOUtil.hex2byte(encrypt(tmkKey, zmkKey));//encrypt tmk with zmk


                        String tskKey = ISOUtil.byte2hex((clearTsk.getEncoded())).toUpperCase(); //get tsk Hex

                        String tskKcv = encrypt(LEADING_ZEROES, tskKey).toUpperCase().substring(0, 6);//get the kcv


                        String tpkKey = ISOUtil.byte2hex(clearTpk.getEncoded()).toUpperCase();
                        String tpkKcv = encrypt(LEADING_ZEROES, tpkKey).toUpperCase().substring(0, 6);

                        TerminalKeys terminalKeys = TerminalKeys
                                .builder()
                                .terminalid(request.getString(41))
                                .tmk(tmkKey).tmkKcv(tmkkcv)
                                .tsk(tskKey).tskKcv(tskKcv)
                                .tpk(tpkKey).tpkKcv(tpkKcv)
                                .build();

                        //update if present or else save a new record if not present
                        terminalKeyRepository.findByTerminalid(terminalKeys.getTerminalid())
                                .ifPresentOrElse(terminalKeys1 -> {
                                            terminalKeys1.setTerminalid(terminalKeys.getTerminalid());
                                            terminalKeys1.setTmk(terminalKeys.getTmk());
                                            terminalKeys1.setTmkKcv(terminalKeys.getTmkKcv());
                                            terminalKeys1.setTsk(terminalKeys.getTsk());
                                            terminalKeys1.setTskKcv(terminalKeys.getTskKcv());
                                            terminalKeys1.setTpk(terminalKeys.getTpk());
                                            terminalKeys1.setTpkKcv(terminalKeys.getTpkKcv());
                                            terminalKeyRepository.save(terminalKeys1);
                                        }
                                        , () -> terminalKeyRepository.save(terminalKeys));


                        //putting this in a map should not affect the trasnaction flow
                        StaticKeyMap.terminalKeys.remove(terminalKeys.getTerminalid());
                        executors.execute(() -> StaticKeyMap.terminalKeys.put(terminalKeys.getTerminalid(), terminalKeys));

                        r.set(53, buildField53(tmkUnderZMK, ISOUtil.hex2byte(tmkkcv)));
                        r.set(39, "00");
                        break;
                    case sessionKey: {
                        TerminalKeys terminalKeys1;
                        if (StaticKeyMap.terminalKeys.containsKey(request.getString(41)))
                            terminalKeys1 = StaticKeyMap.terminalKeys.get(request.getString(41));
                        else
                            terminalKeys1 = terminalKeyRepository.findByTerminalid(request.getString(41)).orElse(null);

                        if (Objects.isNull(terminalKeys1)) {
                            log.info(ERROR_RESPONSE);
                            throw new ISOException("Kindly perform TMK request");
                        }

                        byte[] tskUnderTMK = ISOUtil.hex2byte(encrypt(terminalKeys1.getTsk(), terminalKeys1.getTmk()));
                        r.set(53, buildField53(tskUnderTMK, ISOUtil.hex2byte(terminalKeys1.getTskKcv())));
                        r.set(39, "00");
                        break;
                    }
                    case pinKey: {
                        TerminalKeys terminalKeys1;
                        if (StaticKeyMap.terminalKeys.containsKey(request.getString(41)))
                            terminalKeys1 = StaticKeyMap.terminalKeys.get(request.getString(41));
                        else
                            terminalKeys1 = terminalKeyRepository.findByTerminalid(request.getString(41)).orElse(null);

                        if (Objects.isNull(terminalKeys1)) {
                            log.info(ERROR_RESPONSE);
                            throw new ISOException("Kindly perform TMK request");
                        }

                        byte[] tpkUnderTMK = ISOUtil.hex2byte(encrypt(terminalKeys1.getTpk(), terminalKeys1.getTmk()));
                        r.set(53, buildField53(tpkUnderTMK, ISOUtil.hex2byte(terminalKeys1.getTpkKcv())));
                        r.set(39, "00");


                        break;
                    }
                    case parameterDownload:
                        Terminals terminal = context.get("TERMINAL");

                        String postParameters = "02014"+getTimeZoneDate("Africa/Lagos")
                                +"03"+padLeftZeros(terminal.getMerchants().getMerchantId().length()) + terminal.getMerchants().getMerchantId()
                                +"04"+padLeftZeros("60".length()) + "60"
                                +"05"+padLeftZeros(terminal.getMerchants().getCurrencyCode().length()) + terminal.getMerchants().getCurrencyCode()
                                +"06"+padLeftZeros(terminal.getMerchants().getCurrencyCode().length()) + terminal.getMerchants().getCurrencyCode()
                                +"07"+padLeftZeros("60".length()) + "60"
                                +"52"+padLeftZeros(terminal.getMerchants().getMerchantNameAndLocation().length()) + terminal.getMerchants().getMerchantNameAndLocation()
                                +"08"+padLeftZeros(terminal.getMerchants().getMerchantCategoryCode().length()) + terminal.getMerchants().getMerchantCategoryCode();
                        if(r.hasField(62))
                            postParameters = r.getString(62)+postParameters;
                        r.set(62, postParameters);
                        r.set(39,"00");


                        break;
                    default:
                        log.info("Transaction type not found for request mti {} and processing code {}", request.getMTI(), request.getString(3));
                        break;
                }

            } catch (NoSuchAlgorithmException | ISOException | CryptoException e) {
                log.info(e.getMessage());
                r.set(39,"96");
            }

        }
        else
            return null;
            //todo implement logic for using hsm

        try {
            r.setResponseMTI();
        } catch (ISOException e) {
            e.printStackTrace();
        }
        return r;

    }

    private byte[] buildField53(byte[] key, byte[] kcv) throws ISOException {

        return ISOUtil.hex2byte(ISOUtil.padright(ISOUtil.byte2hex(key) + ISOUtil.byte2hex(kcv).substring(0, 6), 96, '0')
        );
    }
    public String encrypt(String pinBlock, String key) throws CryptoException {
        if (StringUtils.hasText(pinBlock)) {
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
        byte[] encryptedPinBlockBytes = EncryptionUtils.tdesEncryptECB(clearPinBlockBytes, zpk);
        return new String(Hex.encodeHex(encryptedPinBlockBytes));

    }
    private String getTimeZoneDate(String timeZone){
        Date date = new Date();
        DateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");

        //set timezone here
        dateFormat.setTimeZone(TimeZone.getTimeZone(timeZone));

        return dateFormat.format(date);
    }
    private String padLeftZeros(final int input) {
        int length = 3;
        String inputString = String.valueOf(input);
        if (inputString.length() >= length) {
            return inputString;
        }
        final StringBuilder sb = new StringBuilder();
        while (sb.length() < length - inputString.length()) {
            sb.append('0');
        }
        sb.append(inputString);
        return sb.toString();
    }


}
