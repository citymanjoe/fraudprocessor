package com.telcoilng.fraudprocessor.processors;


import org.jpos.iso.ISOMsg;
import org.jpos.transaction.Context;


public interface QueryHost {
    Context queryHost (Context context);
}
