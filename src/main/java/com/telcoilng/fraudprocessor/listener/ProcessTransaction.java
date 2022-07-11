package com.telcoilng.fraudprocessor.listener;

import org.jpos.iso.ISOMsg;
import org.jpos.transaction.Context;

public interface ProcessTransaction {
    Context processTransaction(ISOMsg msg, Context context);
}
