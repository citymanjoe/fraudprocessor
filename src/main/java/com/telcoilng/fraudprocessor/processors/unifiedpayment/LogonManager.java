/*
 * jPOS Project [http://jpos.org]
 * Copyright (C) 2000-2017 jPOS Software SRL
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.telcoilng.fraudprocessor.processors.unifiedpayment;

import org.jdom2.Element;
import org.jdom2.output.XMLOutputter;
import org.jpos.core.Configuration;
import org.jpos.core.ConfigurationException;
import org.jpos.iso.*;
import org.jpos.iso.packager.XMLPackager;
import org.jpos.q2.QBeanSupport;
import org.jpos.space.Space;
import org.jpos.space.SpaceFactory;
import org.jpos.space.SpaceUtil;
import org.jpos.util.NameRegistrar;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.util.Date;

@SuppressWarnings("unchecked")
public class LogonManager extends QBeanSupport implements Runnable {
    Space sp;
    Space psp;
    MUX mux;
    long timeout;
    long echoInterval;
    long logonInterval;
    long initialDelay;
    String readyKey;
    ISOMsg logonMsg;
    ISOMsg logoffMsg;
    ISOMsg echoMsg;
    Date now = new Date();
    public static final String TRACE = "UNIFIEDPAYMENT_TRACE";
    public static final String LOGON = "UNIFIEDPAYMENT_LOGON.";
    public static final String ECHO  = "UNIFIEDPAYMENT_ECHO.";
    public static final String FDR_KEY = "UNIFIEDPAYMENT_KEY";
    private boolean isSignOn = false;

    public void initService () throws ConfigurationException {
        log.info("Initial unified Payment");
        Configuration cfg = getConfiguration();
        sp       = SpaceFactory.getSpace (cfg.get ("space", ""));
        psp      = SpaceFactory.getSpace (cfg.get ("persistent-space", ""));
        timeout  = cfg.getLong ("timeout", 30000);
        echoInterval  = cfg.getLong ("echo-interval", 30000);
        logonInterval = cfg.getLong ("logon-interval", 86400000L);
        initialDelay  = cfg.getLong ("initial-delay", 1000L);
        readyKey      = cfg.get ("channel-ready");
        Element config = getPersist();
        logonMsg      =  getMsg ("logon", config);
        logoffMsg     =  getMsg ("logoff", config);
        echoMsg       =  getMsg ("echo", config);
    }
    public void startService () {
        try {
            mux  = (MUX) NameRegistrar.get ("mux." + cfg.get ("mux"));
        } catch (NameRegistrar.NotFoundException e) {
            getLog().warn (e);
        }
        new Thread (this).start();
    }
    public void run () {
        while (running()) {
            Object sessionId = sp.rd (readyKey, 60000);
            if (sessionId == null) {
                getLog().info ("Channel " + readyKey + " not ready");
                continue;
            }
            try {
                if (!sessionId.equals (sp.rdp (LOGON+readyKey))) {
                    if (!isSignOn){
                        doSignOn();
                        isSignOn = true;
                    }
                    doEcho();
//                    doLogon (sessionId);
                    Thread.sleep (initialDelay);
                } else if (sp.rdp (ECHO+readyKey) == null) {
                    doEcho ();
                }
            } catch (Throwable t) {
                getLog().warn (t);
            }
            ISOUtil.sleep (10000);
        }
    }
    public void stopService() {
        try {
            doLogoff();
        } catch (Throwable t) {
            getLog().warn (t);
        }
    }

    public void doSignOn() throws ISOException{
        SpaceUtil.wipe (sp, LOGON+readyKey);
        mux.request (createMsg ("001", logoffMsg), 1000);
    }

    private void doLogon (Object sessionId) throws ISOException {
        ISOMsg resp = mux.request (createMsg ("001", logonMsg), timeout);
        if (resp != null && "0000".equals (resp.getString(39))) {   // RC will come in CMF, hence 0000
            SpaceUtil.wipe (sp, LOGON+readyKey);
            sp.out (LOGON+readyKey, sessionId, logonInterval);
            getLog().info (
                "Logon successful (session ID " + sessionId.toString() + ")"
            );
        }
    }

    private void doLogoff () throws ISOException {
        SpaceUtil.wipe (sp, LOGON+readyKey);
        mux.request (createMsg ("002", logoffMsg), 1000);
    }

    private void doEcho () throws ISOException {
        ISOMsg resp = mux.request (createMsg ("301", echoMsg), timeout);
        if (resp != null) {
            sp.out (ECHO+readyKey, new Object(), echoInterval);
        }
    }

    private ISOMsg createMsg (String msgType, ISOMsg merge) throws ISOException
    {
        long traceNumber = SpaceUtil.nextLong (psp, TRACE) % 1000000;
        ISOMsg m = new ISOMsg("0800");                                // use CMF specs for MTI
        log.info("To Create ISO message for unified Payment: "+ m.getMTI() + "|" + m.getString(0));
        m.set(7, ISODate.getDateTime(new Date()));
        m.set(11, ISOUtil.zeropad (Long.toString(traceNumber), 6));   // we can leave STAN with 6 figures
        m.set(12, ISODate.getTime(now));
        m.set(13, ISODate.getDate(now));
        m.set(70, msgType);
        if (merge != null)
            m.merge (merge);
        return m;
    }

    // Gets isomsg chunk for each type of network message: logonMsg, logoffMsg. echoMsg
    private ISOMsg getMsg (String name, Element config) throws ConfigurationException
    {
        log.info("Get Message for unified Payment");
        ISOMsg m = new ISOMsg();
        Element e = config.getChild (name);
        if (e != null)
            e = e.getChild ("isomsg");
        if (e != null) {
            try {
                XMLPackager p = new XMLPackager();
                p.setLogger (getLog().getLogger(), getLog().getRealm()
                    + "-config-" + name);
                m.setPackager (p);

                ByteArrayOutputStream os = new ByteArrayOutputStream ();
                OutputStreamWriter writer = new OutputStreamWriter (os);
                XMLOutputter out = new XMLOutputter ();
                out.output (e, writer);
                writer.close ();
                m.unpack (os.toByteArray());
            } catch (Exception ex) {
                throw new ConfigurationException (ex);
            }
        }
        return m;
    }
}

