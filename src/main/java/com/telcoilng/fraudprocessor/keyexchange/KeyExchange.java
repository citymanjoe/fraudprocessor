package com.telcoilng.fraudprocessor.keyexchange;

import org.jpos.iso.ISOMsg;
import org.jpos.transaction.Context;

public interface KeyExchange {
    ISOMsg doKeyExchange(ISOMsg request, Context context);
}
