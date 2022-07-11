package com.telcoilng.fraudprocessor.processors.nibss.service;

import com.telcoilng.fraudprocessor.Entity.NibssKeys;
import com.telcoilng.fraudprocessor.Entity.Station;
import com.telcoilng.fraudprocessor.repository.NibssKeysRepository;
import com.telcoilng.fraudprocessor.repository.StationRepository;
import com.telcoilng.fraudprocessor.server.SocketFactory;
import com.telcoilng.fraudprocessor.utils.EncryptionUtil;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jpos.iso.*;
import org.jpos.iso.Currency;
import org.jpos.iso.channel.PostChannel;
import org.jpos.iso.packager.GenericPackager;
import org.jpos.space.Space;
import org.jpos.space.SpaceFactory;
import org.jpos.transaction.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;

import static com.telcoilng.fraudprocessor.utils.EncryptionUtil.generateHash256Value;
import static com.telcoilng.fraudprocessor.utils.EncryptionUtil.hexStringToByteArray;

@Service
@Slf4j
@RequiredArgsConstructor
public class NibssServiceImpl implements NibssService {
    @Value("${nibssHostIp}")
    String nibssHostIp;
    @Value("${nibssHostPort}")
    int nibssHostPort;

    private final StationRepository stationRepository;
    private final NibssKeysRepository nibssKeysRepository;
    @SneakyThrows
    @Override
    public Context toNibss(Context context) {
        ISOMsg req = context.get("REQUEST");

        try {
            Station station = stationRepository.findByName("nibss");
            NibssKeys nibssKeys = nibssKeysRepository.findByTerminalId(req.getString(41));

            if (Objects.isNull(nibssKeys)||nibssKeys.getETmk().isEmpty()||nibssKeys.getETsk().isEmpty()||nibssKeys.getETpk().isEmpty()){
                log.error("keys should not be null. for terminal {}",req.getString(41));
                throw new Exception("Nibss keys not complete");
            }

            EncryptionUtil util = new EncryptionUtil();
            String ctmk = station.getZpk().toUpperCase();    //newZPK is the ctmk //work on encrypting it later
            String clearTmk = util.decryptPinBlock(nibssKeys.getETmk(),ctmk).toUpperCase();
            String clearTsk = util.decryptPinBlock(nibssKeys.getETsk(),clearTmk).toUpperCase();

            ISOMsg nibssRequest = createNibssRequest(req, clearTsk);


            PostChannel channel = new PostChannel(nibssHostIp,nibssHostPort, new GenericPackager("nibss.xml"));
            SocketFactory sslSocketFactory = new SocketFactory();
            sslSocketFactory.setKeyStore("nibss.ks");
            sslSocketFactory.setKeyPassword("jposjpos");
            sslSocketFactory.setPassword("jposjpos");

            channel.setSocketFactory(sslSocketFactory);
            channel.connect();

            if (channel.isConnected()){
                channel.send(nibssRequest);

                ISOMsg resp = channel.receive();
                resp.dump(System.out,"");
                channel.disconnect();
                ISOMsg response = filter(resp);
                context.put("RESPONSE",response);
                return context;
//                ctx.put("RESPONSE",response);

            }
            else
                throw new Exception("Channel not connected");


        }catch (Exception e){
            log.error("Error is {}",e.getMessage());
            req.setResponseMTI();
            req.set(39,"91");
            context.put("RESPONSE",req);
            return context;
        }


    }
    public ISOMsg createNibssRequest(ISOMsg message, String sessionKey) throws ISOException {
        ISOMsg isoMsg = new ISOMsg();
        isoMsg.setPackager(new GenericPackager("cfg/nibss.xml"));
        isoMsg.setMTI("0"+message.getMTI().substring(1));
        isoMsg.set(2,message.getString(2));
        isoMsg.set(3,message.getString(3));

        if (message.hasField(4)) {

            ISOAmount amount = (ISOAmount) message.getComponent(4);

            isoMsg.set(4, amount.getAmountAsLegacyString());
            isoMsg.set(49, amount.getCurrencyCodeAsString());
        }
        isoMsg.set(7, message.getString(7));
        isoMsg.set(11, message.getString(11).substring(6,12));
        isoMsg.set(12, message.getString(12).substring(8));
        isoMsg.set(13, message.getString(12).substring(4,8));
        if (message.hasField(14))
            isoMsg.set(14, message.getString(14));

        if (message.hasField(26))
            isoMsg.set(18, message.getString(26));
        else
            isoMsg.set(18, "5251"); //temp fix
        isoMsg.set(22, "051");

        if (message.hasField(23))
            isoMsg.set(23, message.getString(23));
        else
            isoMsg.set(23, "000");

        isoMsg.set(25, "00");
        isoMsg.set(28, "D00000000");
        isoMsg.set(32, "111129");
        isoMsg.set(35, message.getString(35));
        isoMsg.set(37, message.getString(37));

        if (message.hasField(122)){
            ISOMsg subfields = (ISOMsg) message.getComponent(122);
            isoMsg.set(40,subfields.getString(40));
        }


        isoMsg.set(41, message.getString(41).substring(0,8));
        isoMsg.set(42, message.getString(42));


        ISOMsg subfields = (ISOMsg) message.getComponent(43);
        String f43 = ISOUtil.padright(subfields.getString(2), 23, ' ')
                + ISOUtil.padright(subfields.getString(4), 13, ' ')
                + ISOUtil.padright(subfields.getString(5), 2, ' ')
                + ISOUtil.padright(subfields.getString(7), 2, ' ');
        isoMsg.set(43, f43);

        if (message.hasField(52)){
            isoMsg.set(52,message.getString(52));
        }

        isoMsg.set(55, message.getString(55));

        if (message.hasField(59))
            isoMsg.set(59,message.getString(59));
        isoMsg.set(123,"511101513344101");
        isoMsg.set(128,"AA8F79C32F1933A4CB24D0AA62C173F76C37D9D83A766A9FB2A57647CA5D6005");
        //reversal request
        if (isoMsg.getMTI().startsWith("04")){//reversal
            if (message.hasField(90)){
                StringBuilder sb = new StringBuilder(42);
                sb.append(message.getString(90).substring(0,4));
                sb.append(isoMsg.getString(11));
                sb.append(isoMsg.getString(7));
                sb.append("00000");
                sb.append(isoMsg.getString(32));
                sb.append("00000000000");

                isoMsg.set(90,sb.toString());

            }
            if (message.hasField(95))
                isoMsg.set(95,message.getString(95));


        }

        byte[] bites = isoMsg.pack();

        final int length = bites.length;
        final byte[] temp = new byte[length - 64];
        if (length >= 64) {
            System.arraycopy(bites, 0, temp, 0, length - 64);
        }
        final String hashHex = generateHash256Value(temp, hexStringToByteArray(sessionKey));
        isoMsg.set(128,hashHex);
//        isoMsg.dump(System.out,"");
        return isoMsg;
    }


    private static final long FIVE_MINUTES = 5*60*1000L;
    private static Map<String,String> map = new HashMap<>();
    private Space sp = SpaceFactory.getSpace();

    static {
        // jPOS-CMF and Postilion use the same account-types in PCODE
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
        if (mti.startsWith("0")) {
            m.setMTI("2" + mti.substring(1));
        }
    }

    public ISOMsg filter(ISOMsg message) throws ISOFilter.VetoException {

        ISOMsg m = (ISOMsg) message.clone();



        try {

            mapMtiAndPCode(message.getMTI(), message.getString(3), m);

            if (message.hasField(11))
                m.set(11, "000000" + message.getString(11));       // STAN: expand from N6 to N12

            if (message.hasField(4)) {
                ISOAmount amnt = new ISOAmount(4);
                String currency = message.getString(49);
                if (currency == null)
                    currency = "840";

                Currency c = ISOCurrency.getCurrency(currency);
                amnt.setValue(currency + Integer.toString(c.getDecimals()) + message.getString(4));
                m.set(amnt);

                // POST-28: Transaction Fee (acquirer fee) format: x+n8, where means C=credit, D=debit
                String fee = message.getString(28);
                if (fee != null && fee.length() == 9 && !"C00000000".equals(fee)) {
                    // issuer fee, going into CMF-46: N2 N13 N8 N13
                    // xn13 format: Credit/Debit, currency code(3), number of decimals(1) value(8)
                    StringBuilder sb = new StringBuilder(36);
                    sb.append("07");                           // N2: card acceptor service fee

                    sb.append(fee.substring(0, 1));
                    sb.append(amnt.getCurrencyCodeAsString());
                    sb.append(amnt.getScaleAsString());
                    sb.append(fee.substring(1));

                    sb.append("00000001");                     // N8 no conversion rate

                    sb.append(fee.substring(0, 1));             // repeat second N13...
                    sb.append(amnt.getCurrencyCodeAsString());
                    sb.append(amnt.getScaleAsString());
                    sb.append(fee.substring(1));               // ...reconciliation amount is the same

                    m.set(46, sb.toString());
                }
            }

            if (m.hasField(35) || m.hasField(45))               // if we have track2 or track1
                m.unset(new int[]{2, 14});                      // unset PAN and EXP (they'll be reconstructed)

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

            String f43 = message.getString(43);

            if (f43 != null && f43.length() >= 40) {

                ISOMsg m43 = new ISOMsg(43);              // our own version of f43 in CMF
                m43.set(2, f43.substring(0, 23).trim());  // name
                m43.set(4, f43.substring(23, 36).trim()); // city
                m43.set(5, f43.substring(36, 38).trim()); // state
                m43.set(7, f43.substring(38, 40).trim()); // country
                m.set(m43);
            }


            if (message.hasField(39)) {
                if (message.getString(39).equals("00")) {
                    m.set(39, "00" + message.getString(39));
                }
                else
                    m.set(39,message.getString(39));
            }




            if (message.hasField(56)) {
                m.set(25, message.getString(56));
                m.unset(56);
            }

            // Postilion-90: Original data elements N42 (used for reversals, adjustments, etc)
            String f90 = message.getString(90);
            if (/* message.getMTI().startsWith("04") && */          // Postilion handles 0100 and 0200 adjustments
                    f90 != null && f90.length() >= 42)
            {
                StringBuilder sb = new StringBuilder("2");          // 2XXX CMF MTI's
                sb.append(f90.substring(1, 4));                     // rest of the MTI
                sb.append("000000").append(f90.substring(4, 10));   // STAN: expand from N6 to N12

                Date originalDate = ISODate.parseISODate(f90.substring(10, 20));
                sb.append(ISODate.formatDate(originalDate, "yyyyMMddHHmmss"));  // CCYYMMDDhhmmss in CMF
                sb.append(f90.substring(32, 42));                   // Original acquiring institution ID

                String cmf56= sb.toString();
                m.set(56, cmf56);
            }

            m.set(52, message.getBytes(52));                                // PIN data/block b8 (BBB Postilion says hex 16 so it may be ASCII hex and need to be converted to binary)
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
            System.out.println("Error" +e.getMessage());;
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
