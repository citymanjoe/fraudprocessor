package com.telcoilng.fraudprocessor.processors.nibss.service;

import org.jpos.iso.ISOMsg;
import org.jpos.transaction.Context;

public interface NibssService {
    Context toNibss(Context context);
}
