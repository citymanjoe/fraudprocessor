package com.telcoilng.fraudprocessor.server;

import org.jpos.iso.GenericSSLSocketFactory;

public class SocketFactory extends GenericSSLSocketFactory {
    @Override
    protected String getPassword() {
        return "jpospos";
    }
}
