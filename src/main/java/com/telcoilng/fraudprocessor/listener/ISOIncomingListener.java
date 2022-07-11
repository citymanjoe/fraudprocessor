package com.telcoilng.fraudprocessor.listener;

import com.telcoilng.fraudprocessor.Entity.TerminalKeys;
import com.telcoilng.fraudprocessor.Entity.Terminals;
import com.telcoilng.fraudprocessor.cache.StaticKeyMap;
import com.telcoilng.fraudprocessor.cache.TerminalCache;
import com.telcoilng.fraudprocessor.exception.MacException;
import com.telcoilng.fraudprocessor.keyexchange.KeyExchange;
import com.telcoilng.fraudprocessor.repository.TerminalKeyRepository;
import com.telcoilng.fraudprocessor.transactions.model.Transaction;
import com.telcoilng.fraudprocessor.transactions.service.TranLog;
import com.telcoilng.fraudprocessor.utils.Utils;
import com.telcoilng.fraudprocessor.utils.constants.TransactionType;
import com.telcoilng.fraudprocessor.validation.MessageValidator;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISORequestListener;
import org.jpos.iso.ISOSource;
import org.jpos.transaction.Context;
import org.jpos.util.Log;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.persistence.EntityExistsException;
import java.io.IOException;
import java.util.Date;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.telcoilng.fraudprocessor.utils.Utils.generateMAC;
import static com.telcoilng.fraudprocessor.utils.Utils.hexStringToByteArray;
import static com.telcoilng.fraudprocessor.utils.constants.Constants.VALIDATION_FAILED;


@Service
public class ISOIncomingListener extends Log implements ISORequestListener, TransactionType {
    Logger log = LoggerFactory.getLogger(ISOIncomingListener.class);
    ISOMsg response;

    @Autowired
    KeyExchange keyExchange;
    @Autowired
    MessageValidator validator;
    @Autowired
    ProcessTransaction processTransaction;
    @Autowired
    TerminalKeyRepository terminalKeyRepository;
    @Autowired
    TranLog logTransaction;

    Transaction transaction = new Transaction();

    ExecutorService executorService = Executors.newCachedThreadPool();



    @Override
    public boolean process(ISOSource source, ISOMsg m) {
        info("ISO transaction request:");
        try {
            Context transactionContext = new Context();
            Terminals terminals;
            transactionContext.put("TIMESTAMP",new Date());
            response = (ISOMsg) m.clone();
            //Terminal exists validation
            if (TerminalCache.terminalsHashMap.containsKey(m.getString(41))){
                terminals = TerminalCache.terminalsHashMap.get(m.getString(41));
            }
            else {
                terminals = validator._validateTerminalMerchantClient().apply(m);
            }

            if (Objects.isNull(terminals)){
                info("Terminal exist Error or Terminal not active or Merchant not active or Client not active");
                response.set(39,"91");
                throw new EntityExistsException("Terminal Ecist Error or Terminal not active or Merchant not active or Client not active");
            }

            //save terminal to cache
            executorService.execute(() -> TerminalCache.terminalsHashMap.put(m.getString(41),terminals));
            transactionContext.put("TERMINAL",terminals);


            switch (m.getMTI()){
                case _0100:
                    if (Boolean.TRUE.equals(validator._0100Validator().apply(m))){
                        transaction = logTransaction.createTransaction.apply(terminals,m);
                        transactionContext.put("TRANSACTION",transaction);
                        Context result = processTransaction.processTransaction(m, transactionContext);
                        response = result.get("RESPONSE");
                        executorService.execute(()->logTransaction.updateTransaction.apply(transactionContext,response));
                    }
                    else
                        log.info(VALIDATION_FAILED);
                    break;
                case _0200:
                    if (Boolean.TRUE.equals(validator._0200Validator().apply(m))){
                        transaction = logTransaction.createTransaction.apply(terminals,m);
                        transactionContext.put("TRANSACTION",transaction);
                        Context result = processTransaction.processTransaction(m, transactionContext);
                        response = result.get("RESPONSE");
                        executorService.execute(()->logTransaction.updateTransaction.apply(transactionContext,response));


                    }
                    else
                        log.info(VALIDATION_FAILED);
                    break;
                case _0420:
                    if (Boolean.TRUE.equals(validator._0420Validator().apply(m))){
                        transaction = logTransaction.getOriginal(m);

                        transactionContext.put("TRANSACTION",transaction);
                        Context result = processTransaction.processTransaction(m, transactionContext);
                        response = result.get("RESPONSE");
                        logTransaction.updateTransaction.apply(transactionContext,response);
                    }
                    else
                        log.info(VALIDATION_FAILED);
                    break;
                case _0800:
                    if (Boolean.TRUE.equals(validator._0800Validator().apply(m))){
                        response = keyExchange.doKeyExchange(m,transactionContext);
                    }
                    else
                        log.info(VALIDATION_FAILED);
                    break;
                default:
                    log.info("Transaction type not supported for transaction mto : {}",m.getMTI());
                    response.setResponseMTI();
                    response.set(39,"91");
                    break;
            }


        } catch (ISOException e) {
            log.error("Error message {}",e.getMessage());
        }
        finally {
            try {
                ISOMsg isoMsgResp = computeMAC(response);
                if (!isoMsgResp.isResponse())
                    isoMsgResp.setResponseMTI();
                source.send(isoMsgResp);
                executorService.execute(() -> log.info("response is {}",isoMsgResp.getString(39)));
            } catch (IOException | ISOException | MacException | DecoderException e) {
                log.info("Error sending response is {}",e.getMessage());
            }
        }
        return true;
    }



    private boolean assertMac (ISOMsg m) throws MacException, DecoderException, ISOException {
        TerminalKeys keys;
        if (StaticKeyMap.terminalKeys.containsKey(m.getString(41))){
            keys = StaticKeyMap.terminalKeys.get(m.getString(41));
        }
        else {
            keys = terminalKeyRepository.findByTerminalid(m.getString(41)).orElse(null);
            if (keys==null){
                log.info("Terminal keys not found to be used to validate MAC");
                throw new MacException("Terminal Keys not found to be used to validate MAC");
            }


        }
        String sessionKey = keys.getTsk();
        if (sessionKey.isEmpty()||sessionKey.isBlank()){
            log.info("Session Key not found, recommend key Exchange");
            throw new MacException("Session Key not found to be used to validate MAC");

        }
        return Utils.validateMAC(m, sessionKey);

    }


    private ISOMsg computeMAC(ISOMsg msg) throws MacException, DecoderException, ISOException {

        if (msg==null){
            log.error("keys not present or empty.");
            throw new MacException("Keys not present or empty");
        }

        TerminalKeys keys;
        if (StaticKeyMap.terminalKeys.containsKey(msg.getString(41))){
            keys = StaticKeyMap.terminalKeys.get(msg.getString(41));
        }
        else {
            keys = terminalKeyRepository.findByTerminalid(msg.getString(41)).orElse(null);
            if (keys==null){
                log.info("Terminal keys not found to be used to validate MAC {}",msg.getString(41));
                throw new MacException("Terminal Keys not found to be used to validate MAC");
            }


        }
        String sessionKey = keys.getTsk();
        if (sessionKey.isEmpty()||sessionKey.isBlank()){
            log.info("Session Key not found, recommend key Exchange");
            throw new MacException("Session Key not found to be used to validate MAC");

        }
        String hmac = "";
        if (msg.hasField(64)){
            log.info("Field 64 found");
            hmac = generateMAC(msg,hexStringToByteArray(sessionKey));

            msg.set(64, Hex.decodeHex(hmac));
        }
        else if (msg.hasField(128)){
            log.info("Field 128 found");
            hmac = generateMAC(msg,hexStringToByteArray(sessionKey));
            msg.set(128,Hex.decodeHex(hmac));
        }
        else {
            msg.unset(3);
            log.info("HMAC calculation skipped.");
        }

        if (msg.hasField(127))
            msg.unset(127);


        return msg;

    }
}
