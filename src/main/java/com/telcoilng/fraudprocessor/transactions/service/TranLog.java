package com.telcoilng.fraudprocessor.transactions.service;

import com.telcoilng.fraudprocessor.Entity.Terminals;
import com.telcoilng.fraudprocessor.transactions.model.Transaction;
import com.telcoilng.fraudprocessor.transactions.repository.TransactionRepository;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOUtil;
import org.jpos.transaction.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.persistence.EntityExistsException;
import java.util.Date;
import java.util.HashMap;
import java.util.Objects;
import java.util.function.BiFunction;

@Service
public class TranLog {
    Logger log = LoggerFactory.getLogger(TranLog.class);
    @Autowired
    TransactionRepository transactionRepository;

    private static HashMap<String,String> tranTypeMap = new HashMap<>();


    static {
        tranTypeMap.put("100.30","Balance Inquiry");
        tranTypeMap.put("100.31","Balance");
        tranTypeMap.put("100.00","Balance Inquiry");
        tranTypeMap.put("100.60","Balance Inquiry");
        tranTypeMap.put("200.00","POS Purchase");
        tranTypeMap.put("200.4G","MOMO Purchase");
        tranTypeMap.put("200.01","Cash Advance");
        tranTypeMap.put("200.09","Purchase Cashback");
        tranTypeMap.put("200.20","Refund");
        tranTypeMap.put("220.61","POS Purchase");
        tranTypeMap.put("420.00","Reversal purchase");
        tranTypeMap.put("420.60","Refund Purchase");
        tranTypeMap.put("420.61","Pre-auth Reversal Purchase");

    }

    public BiFunction<Terminals, ISOMsg, Transaction> createTransaction = (terminal, msg) -> {
        Transaction transaction = Transaction.builder()
                .terminals(terminal)
                .mti(msg.getString(0))
                .de2(msg.hasField(2) ? msg.getString(2) : null)
                .de3(msg.hasField(3) ? msg.getString(3) :null )
                .de4(msg.hasField(4 )? msg.getString(4) :null )
                .de7(new Date())
                .de11(msg.hasField(11)? msg.getString(11) :null )
                .de12(msg.hasField(12)? msg.getString(12) :null )
                .de13(msg.hasField(13)? msg.getString(13) :null )
                .de14(msg.hasField(14)? msg.getString(14) :null )
                .de18(msg.hasField(18)? msg.getString(18) :null )
                .de22(msg.hasField(22)? msg.getString(22) :null )
                .de23(msg.hasField(23)? msg.getString(23) :null )
                .de25(msg.hasField(25)? msg.getString(25) :null )
                .de26(msg.hasField(26)? msg.getString(26) :null )
                .de28(msg.hasField(28)? msg.getString(28) :null )
                .de32(msg.hasField(32)? msg.getString(32) :null )
                .de33(msg.hasField(33)? msg.getString(33) :null )
                .de35(msg.hasField(35)? msg.getString(35) :null )
                .de37(msg.hasField(37)? msg.getString(37) :null )
                .de38(msg.hasField(38)? msg.getString(38) :null )
                .de40(msg.hasField(40)? msg.getString(40) :null )
                .de41(msg.hasField(41)? msg.getString(41) :null )
                .de42(msg.hasField(42)? msg.getString(42) :null )
                .de43(msg.hasField(43)? msg.getString(43) :null )
                .de49(msg.hasField(49)? msg.getString(49) :null )
                .de59(msg.hasField(59)? msg.getString(59) :null )
                .de61(msg.hasField(61)? msg.getString(61) :null )
                .de90(msg.hasField(90)? msg.getString(90) :null )
                .de95(msg.hasField(95)? msg.getString(95) :null )
                .de123(msg.hasField(123)? msg.getString(123) :null )
                .createdAt(new Date())
                .build();

        //mask pan
        transaction.setDe2(ISOUtil.protect(transaction.getDe2(),'*'));
        transaction.setDe35(ISOUtil.protect(transaction.getDe35(),'*'));

        if (terminal.getTerminalType()!=null)
            transaction.setTerminalType(terminal.getTerminalType().name());

        if (tranTypeMap.containsKey(msg.getString(0).substring(1)+"."+msg.getString(3).substring(0,2)))
            transaction.setChannel(tranTypeMap.get(msg.getString(0).substring(1)+"."+msg.getString(3).substring(0,2)));


        return transactionRepository.save(transaction);

    };

    public BiFunction<Context,ISOMsg,Transaction> updateTransaction = (context, msg) -> {
        Transaction transaction = context.get("TRANSACTION");
        String processedBy = "";
        if (Objects.nonNull(context.get("DS")))
            processedBy = context.getString("DS");


        if (transaction==null){
            log.error("Transaction RRN {} and STNA {} not founf", msg.getString(37),msg.getString(11));
            return null;
        }
        else {
            if (msg.hasField(39)){
                log.info("Response from host is {}",msg.getString(39));
                transaction.setResponsecode(msg.getString(39));
            }
            if (msg.hasField(38))
                transaction.setDe38(msg.getString(38));
            transaction.setProcessedBy(processedBy);

            if (msg.getString(0).startsWith("04")){
                transaction.setMti(msg.getString(0));
                if (msg.hasField(39)){
                    if (msg.getString(39).equals("00"))
                        transaction.setReversed(true);
                    else
                        transaction.setReversed(false);

                }
            }
            return transactionRepository.save(transaction);

        }



    };

    public Transaction getOriginal (ISOMsg msg){
        Transaction transaction = transactionRepository.findTransactionByde37AndDe11(msg.getString(37)
                        ,msg.getString(11))
                .orElse(null);
        if (transaction==null||transaction.isReversed())
            throw new EntityExistsException("Oriransaction not found or already reversed");
        else
            return transaction;

    }


}
