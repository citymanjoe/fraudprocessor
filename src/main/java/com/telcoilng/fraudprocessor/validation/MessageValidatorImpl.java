package com.telcoilng.fraudprocessor.validation;

import com.telcoilng.fraudprocessor.Entity.StatusType;
import com.telcoilng.fraudprocessor.Entity.Terminals;
import com.telcoilng.fraudprocessor.repository.TerminalRepository;
import org.jpos.iso.ISOMsg;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.function.Function;

@Service
public class MessageValidatorImpl implements MessageValidator {
    @Autowired
    TerminalRepository terminalsRepository;


    @Override
    public Function<ISOMsg, Terminals> _validateTerminalMerchantClient() {

        return isoMsg -> terminalsRepository.findTerminalsByTerminalIdAndStatus(isoMsg.getString(41), StatusType.ACTIVATED)
                .orElse(null);
    }

    @Override
    public Function<ISOMsg, Boolean> _0800Validator() {
        int[] _8088Fields = {0,7,11,12,13,41};
        return isoMsg -> isoMsg.hasFields(_8088Fields);
    }

    @Override
    public Function<ISOMsg, Boolean> _0200Validator() {
        int[] mandatory = {2,3,4,7,11,12,13,22,32,37,41,42,43};
        int[] optional = {2,13,14,17,18,20,24,23,25,26,28,32,33,35,36,37,40,41,42,43,45,46,49,52,55,56,59,60,63,90,95,99,111,112,122,123,128};

        return isoMsg -> {
            for (int fields:mandatory){
                if (!isoMsg.hasField(fields)){
                    System.out.println("Mandatory field missing for transaction :: field "+fields);
                    return false;
                }
            }

            for (int field:optional){
                if (!isoMsg.hasField(field)){
                    System.out.println("Optional Field not present for transaction :: field "+field);
                }
            }
            return true;
        };

    }

    @Override
    public Function<ISOMsg, Boolean> _0100Validator() {
        int[] mandatory = {2,3,4,7,11,12,13,22,32,37,41,42,43};
        int[] optional = {2,13,14,17,18,20,24,23,25,26,28,32,33,35,36,37,40,41,42,43,45,46,49,52,55,56,59,60,63,90,95,99,111,112,122,123,128};

        return isoMsg -> {
            for (int fields:mandatory){
                if (!isoMsg.hasField(fields)){
                    System.out.println("Mandatory field missing for transaction :: field "+fields);
                    return false;
                }
            }

            for (int field:optional){
                if (!isoMsg.hasField(field)){
                    System.out.println("Optional Field not present for transaction :: field "+field);
                }
            }
            return true;
        };
    }

    @Override
    public Function<ISOMsg, Boolean> _0420Validator() {
        int[] mandatory = {2,3,4,7,11,12,22,41};
        int[] optional = {2,13,14,17,18,20,24,23,25,26,28,32,33,35,36,37,38,40,41,42,43,45,46,49,52,55,56,59,60,63,90,95,99,111,112,122,123,128};

        return isoMsg -> {
            for (int fields:mandatory){
                if (!isoMsg.hasField(fields)){
                    System.out.println("Mandatory field missing for transaction :: field "+fields);
                    return false;
                }
            }

            for (int field:optional){
                if (!isoMsg.hasField(field)){
                    System.out.println("Optional Field not present for transaction :: field "+field);
                }
            }
            return true;
        };

        //todo perform terminal id/merchant/client validation
        //todo perform transactioin exists validation
    }

    @Override
    public Function<ISOMsg, Boolean> _1100Validator() {
        int[] mandatory = {2,3,4,7,11,12,22,41};
        int[] optional = {2,13,14,17,18,20,24,23,25,26,28,32,33,35,36,37,38,40,41,42,43,45,46,49,52,55,56,59,60,63,90,95,99,111,112,122,123,128};

        return isoMsg -> {
            for (int fields:mandatory){
                if (!isoMsg.hasField(fields)){
                    System.out.println("Mandatory field missing for transaction :: field "+fields);
                    return false;
                }
            }

            for (int field:optional){
                if (!isoMsg.hasField(field)){
                    System.out.println("Optional Field not present for transaction :: field "+field);
                }
            }
            return true;
        };

        //todo perform terminal id/merchant/client validation
        //todo perform transactioin exists validation
    }

}
