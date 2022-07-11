package com.telcoilng.fraudprocessor.processors.nibss.factory;

import org.jpos.iso.GenericSSLSocketFactory;

public class SocketFactory extends GenericSSLSocketFactory {
    @Override
    protected String getPassword() {
        return "jposjpos";
    }
}
