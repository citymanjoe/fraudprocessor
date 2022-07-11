package com.telcoilng.fraudprocessor.processors;

import lombok.extern.slf4j.Slf4j;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOUtil;
import org.jpos.iso.MUX;
import org.jpos.transaction.Context;
import org.jpos.util.NameRegistrar;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Objects;

@Service
@Slf4j
public class QueryHostImpl implements QueryHost {
    public static final long DEFAULT_THRESHOLD = 15000L;
    public static final long DEFAULT_TIMEOUT   = 30000L;


    @Override
    public Context queryHost(Context context) {
        ISOMsg request = context.get("REQUEST");

        try {
            log.info("Request to {}", (Object) context.get("DS"));
            request.dump(System.out,"");

            String destinationName = context.getString("DS");
            String muxName = "mux."+destinationName;

            MUX mux = (MUX) NameRegistrar.get(muxName);
            if (Objects.isNull(mux))
                throw new NullPointerException("Mux is null");

            boolean reachingTimeout = isReachingTimeout(context);
            if (isConnected(mux)){
                ISOMsg resp = mux.request(request,DEFAULT_TIMEOUT);
                if (resp!=null){
                    context.put("RESPONSE",resp);
                    return context;
                }
                /*else{
                    //auto reverse transaction
                    log.info("Reversing transaction for STAN {}, RRN {}, terminalID {}",
                            request.getString(11),request.getString(37),request.getString(41));
                    ISOMsg rev = getReversal(request);
                    if (rev!=null){
                        mux.send(rev);
                        context.put("REVERSED",Boolean.TRUE);

                    }
                }*/
            }
            throw new ISOException("Host Unreachable");




        }catch (NameRegistrar.NotFoundException | ISOException e) {
            log.info("Error sending payment  :: {}",e.getMessage());
            request.set(39,"91");
            context.put("RESPONSE",request);
            return context;

        }

    }

    protected boolean isReachingTimeout (Context ctx) {
        long start = ((Date) ctx.get ("TIMESTAMP")).getTime();
        long now = System.currentTimeMillis();
        boolean rc = (now - start) > DEFAULT_THRESHOLD;
        if (rc) {
            log.info("Message reaching timeout - not sending to remote station");
            log.info(now - start + "ms elapsed so far - threshold=" + DEFAULT_THRESHOLD);
        }
        return rc;
    }

    protected boolean isConnected (MUX mux) {
        if (mux.isConnected())
            return true;
        long timeout = System.currentTimeMillis() + 10000L;
        while (System.currentTimeMillis() < timeout) {
            if (mux.isConnected())
                return true;
            ISOUtil.sleep (500);
        }
        return false;
    }

    protected ISOMsg getReversal (ISOMsg m) throws ISOException {
        ISOMsg rev = (ISOMsg) m.clone();
        rev.setMTI (getReversalMTI());

        StringBuilder sb = new StringBuilder();
        sb.append(m.getMTI());
        sb.append (m.getString(11));
        sb.append (m.getString(12));
        sb.append (m.getString(32));
        rev.set (56, sb.toString());

        return fixupSAFMessage (rev);
    }
    protected ISOMsg fixupSAFMessage (ISOMsg m) throws ISOException {
        return m;
    }
    protected String getReversalMTI () {
        return "0420";
    }
}
