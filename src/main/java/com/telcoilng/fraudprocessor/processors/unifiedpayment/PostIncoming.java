package com.telcoilng.fraudprocessor.processors.unifiedpayment;

import org.jpos.iso.*;
import org.jpos.space.Space;
import org.jpos.space.SpaceFactory;
import org.jpos.util.LogEvent;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * Convert the incoming unifiedpayment ISOMsg to the internal jPOS-CMF specs.
 * Based on PostBridge 8 specification, as of 2017
 */
@SuppressWarnings("unused")
public class PostIncoming implements ISOFilter {

    private static final long FIVE_MINUTES = 5*60*1000L;
    private static Map<String,String> map = new HashMap<>();
    private Space sp = SpaceFactory.getSpace();

    static {
        // jPOS-CMF and unifiedpayment use the same account-types in PCODE
        // There's no need to consider them in this mapping
        map.put ("0100.30", "2100.30");           // Available balance inquiry
        map.put ("0100.31", "2100.31");           // Balance inquiry
        map.put ("0100.00", "2100.00");           // Authorization
        map.put ("0100.60", "2100.60");           // Authorization
        map.put ("0200.00", "2200.00");           // Purchase

        map.put ("0420.00", "2420.00");           // Purchase/auth Reversal
        map.put ("0420.01", "2420.01");           // Cash Withdrawal Reversal

    }

    static Map<String, Integer> RM = new HashMap<String, Integer>(); // Reading Method
    static Map<String, Integer> PE = new HashMap<String, Integer>(); // POS Environment
    static Map<String, Integer> VM = new HashMap<String, Integer>(); // Verification Method

    static {

        RM.put("00", PosDataCode.ReadingMethod.UNKNOWN.intValue());
        RM.put("01", PosDataCode.ReadingMethod.PHYSICAL.intValue());
        RM.put("02", PosDataCode.ReadingMethod.MAGNETIC_STRIPE_FAILED.intValue());  // BBB is this FAILED or not FAILED?
        RM.put("03", PosDataCode.ReadingMethod.BARCODE.intValue());
        RM.put("05", PosDataCode.ReadingMethod.ICC.intValue());
        RM.put("79", PosDataCode.ReadingMethod.PHYSICAL.intValue() |
                     PosDataCode.ReadingMethod.FALLBACK.intValue());
        RM.put("80", PosDataCode.ReadingMethod.MAGNETIC_STRIPE.intValue() |
                     PosDataCode.ReadingMethod.FALLBACK.intValue());
        RM.put("81", PosDataCode.ReadingMethod.PHYSICAL.intValue());
        RM.put("90", PosDataCode.ReadingMethod.MAGNETIC_STRIPE.intValue());

        // BBB I think these should be taken from POST-25, right?
        PE.put("01", PosDataCode.POSEnvironment.ATTENDED.intValue());
        PE.put("05", PosDataCode.POSEnvironment.ATTENDED.intValue());
        PE.put("79", PosDataCode.POSEnvironment.ATTENDED.intValue());
        PE.put("80", PosDataCode.POSEnvironment.ATTENDED.intValue());
        PE.put("81", PosDataCode.POSEnvironment.E_COMMERCE.intValue());
        PE.put("90", PosDataCode.POSEnvironment.ATTENDED.intValue());

        VM.put("0",  PosDataCode.VerificationMethod.UNKNOWN.intValue());
        VM.put("1",  PosDataCode.VerificationMethod.ONLINE_PIN.intValue());
        VM.put("2",  PosDataCode.VerificationMethod.NONE.intValue());
        VM.put("8",  PosDataCode.VerificationMethod.NONE.intValue());
        VM.put("9",  PosDataCode.VerificationMethod.OFFLINE_PIN_ENCRYPTED.intValue());
    }

    private void mapMtiAndPCode (String mti, String pcode, ISOMsg m) throws ISOException {

        if (pcode != null && pcode.length() >= 6) {

            String txType =    pcode.substring(0, 2);
            String accType1 =  pcode.substring(2, 4);
            String accType2 =  pcode.substring(4, 6);

            String s = map.get (mti + "." + txType);
            if (s != null) {
                StringTokenizer st = new StringTokenizer(s, ".");
                mti = st.nextToken();
                m.setMTI(mti);
                m.set(3, st.nextToken() + accType1 + accType2);
            }
        }

        //AA: by default just convert mti start from version 0 to version 2
        //not the most efficient way for now but I don't see the need to reject unmapped and have to list them all

    }

    @Override
    @SuppressWarnings("unchecked")
    public ISOMsg filter(ISOChannel channel, ISOMsg message, LogEvent evt) throws VetoException {

        ISOMsg m = (ISOMsg) message.clone();

        evt.addMessage ("--- filtered message --");
        evt.addMessage (m);

        try {

            mapMtiAndPCode(message.getMTI(), message.getString(3), m);

            if (message.hasField(11))
                m.set(11, "000000" + message.getString(11));       // STAN: expand from N6 to N12


            Date now = new Date();
            if (!m.hasField(7)) {
                // POS doesn't send transmission date - we need to use local system time
                m.set(7, ISODate.getDateTime(now));
                m.set(12, ISODate.formatDate(now, "yyyyMMddHHmmss"));
                m.set(13, ISODate.formatDate(now, "yyyyMM"));
            }

            Date d = null;
            if (message.hasField(12) && message.hasField(13)) {
                d = ISODate.parseISODate(message.getString(13) + message.getString(12));
            } else if (message.hasField(7)) {
                d = ISODate.parseISODate(message.getString(7));
            }

            if (d != null) {
                m.set(12, ISODate.formatDate(d, "yyyyMMddHHmmss"));
                m.set(13, ISODate.formatDate(now, "yyyyMM"));
            }

            if (m.hasField(22)) {
                m.set(22, getPDC(message.getString(22)).getBytes());    // POST-22: POS Entry Mode / CMF-22: POS Data Code
            }
            m.set(26, message.getString(18));                       // MCC

            if (message.hasField(39)) {
                m.set(39,message.getString(39));
            }

            if (message.hasField(56)) {
                m.set(25, message.getString(56));
                m.unset(56);
            }

            // unifiedpayment-90: Original data elements N42 (used for reversals, adjustments, etc)
            String f90 = message.getString(90);
            if (/* message.getMTI().startsWith("04") && */          // unifiedpayment handles 0100 and 0200 adjustments
                f90 != null && f90.length() >= 42)
            {
                StringBuilder sb = new StringBuilder("2");          // 2XXX CMF MTI's
                sb.append(f90.substring(1, 4));                     // rest of the MTI
                sb.append("000000").append(f90.substring(4, 10));   // STAN: expand from N6 to N12

                Date originalDate = ISODate.parseISODate(f90.substring(10, 20));
                sb.append(ISODate.formatDate(originalDate, "yyyyMMddHHmmss"));  // CCYYMMDDhhmmss in CMF
                sb.append(f90.substring(32, 42));                   // Original acquiring institution ID

                String cmf56= sb.toString();
                evt.addMessage("bit56 CMF = " + cmf56);
                m.set(56, cmf56);
            }

            m.set(52, message.getBytes(52));                                // PIN data/block b8 (BBB unifiedpayment says hex 16 so it may be ASCII hex and need to be converted to binary)
            m.set(70, message.getString(70));                               // Network management, logon, etc. Shouldn't appear here

            if (message.hasField(48)) {                                     // POST-48 Additional Data
                m.set(59, message.getString(48));
            }

            if (message.hasField(112)) {
                ISOMsg m112 = (ISOMsg) message.getComponent(112);
                m.set(59, new String(m112.pack()));
            }

            if (message.hasField("127.25")) {
                //TODO: create TLV value and set to DE-055
                m.unset("127.25");
            }

        } catch (Exception e) {
            evt.addMessage (e);
        }
        return m;
    }

    private PosDataCode getPDC(String f22) {

        if (f22 == null || f22.length() < 3)
            f22 = "000";

        String panMethod = f22.substring(0, 2);
        String pinMethod = f22.substring(2, 3);

        int rm = 0;
        int pe = 0;
        int vm = 0;

        if (RM.containsKey(panMethod))
            rm = RM.get(panMethod);
        if (PE.containsKey(panMethod))
            pe = PE.get(panMethod);
        if (VM.containsKey(pinMethod))
            vm = VM.get(pinMethod);

        return new PosDataCode(
          rm,
          vm,
          pe,
          PosDataCode.SecurityCharacteristic.UNKNOWN.intValue()
        );
    }
}