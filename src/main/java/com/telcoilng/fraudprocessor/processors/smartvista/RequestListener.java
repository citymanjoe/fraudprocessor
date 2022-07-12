package com.telcoilng.fraudprocessor.processors.smartvista;

import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISORequestListener;
import org.jpos.iso.ISOSource;
import org.jpos.util.Log;

public class RequestListener extends Log implements ISORequestListener {

    @Override
    public boolean process(ISOSource source, ISOMsg reqMsg) {

        try {

            if (reqMsg.isResponse())
                return false;

            if (reqMsg.getMTI().equals("2800")) {

                info("Network Management request received.");

                ISOMsg logonMsg = (ISOMsg) reqMsg.clone();

                logonMsg.setResponseMTI();
                logonMsg.set(39, "0000");
                
                source.send(logonMsg);

                info("Network management response sent.");

                return true;
            }
            else {

                warn("Unsupported request received from Postilion: " + reqMsg.getMTI());
            }
        }
        catch (Exception e) {
            error(e);          
        }

        return false;
    }
}
