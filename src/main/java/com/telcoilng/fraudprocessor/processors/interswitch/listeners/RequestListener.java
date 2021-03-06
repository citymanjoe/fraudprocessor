package com.telcoilng.fraudprocessor.processors.interswitch.listeners;

import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISORequestListener;
import org.jpos.iso.ISOSource;
import org.jpos.util.Log;

public class RequestListener extends Log implements ISORequestListener {

    @Override
    public boolean process(ISOSource source, ISOMsg reqMsg) {

        try {
            info("ISW Request Code: " + reqMsg.getMTI() );
            if (reqMsg.isResponse())
                return false;

            if (reqMsg.getMTI().equals("2800")) {

                info("Network Management request received.");

                ISOMsg logonMsg = (ISOMsg) reqMsg.clone();

                logonMsg.setResponseMTI();
                logonMsg.set(39, "00");
                
                source.send(logonMsg);

                info("Network management response sent.");

                return true;
            }
            if (reqMsg.getMTI().equals("0100")) {

                info("Network Management request received.");

                ISOMsg logonMsg = (ISOMsg) reqMsg.clone();

                logonMsg.setResponseMTI();
                logonMsg.set(39, "00");

                source.send(logonMsg);

                info("Network management response sent.");

                return true;
            }
            else {

                warn("ISW Unsupported request received from Postilion: " + reqMsg.getMTI());
            }
        }
        catch (Exception e) {
            error(e);          
        }

        return false;
    }
}
