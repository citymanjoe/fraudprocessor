package com.telcoilng.fraudprocessor.processors.interswitch.filter;

import org.jpos.iso.*;
import org.jpos.tlv.TLVList;
import org.jpos.util.LogEvent;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Converts the outgoing ISOMsg from internal jPOS-CMF specs to Postilion specs.
 */
@SuppressWarnings("unused")
public class PostOutgoing implements ISOFilter {

    @Override
    public ISOMsg filter(ISOChannel channel, ISOMsg message, LogEvent evt) throws VetoException {

        ISOMsg r;
        try {
            r = (ISOMsg) message.clone();

            r.set(0, "0" + r.getMTI().substring(1)); // convert to 08xx format for Postilion

            if (!r.getMTI().startsWith("08")){
                r.set(3,"50"+message.getString(3).substring(2));
            }


            // keep rightmost 6 digits of STAN, with zero padding
            String stan = r.getString(11);
            stan = (stan.length() < 6) ? ISOUtil.zeropad(stan, 6) : stan.substring(stan.length() - 6);
            r.set(11, stan);

            if (message.hasField(4)) {

                ISOAmount amount = (ISOAmount) message.getComponent(4);

                r.set(4, amount.getAmountAsLegacyString());
                r.set(49, amount.getCurrencyCodeAsString());
            }

            if (message.hasField(12)) {

                Date d = ISODate.parseISODate(message.getString(7));

                r.set(12, ISODate.formatDate(d, "HHmmss"));
                r.set(13, ISODate.formatDate(d, "MMdd"));
            }

            if (message.hasField(22)) {

                PosDataCode pdc = PosDataCode.valueOf(message.getBytes(22));

                String pinEntry = "2";

                if (pdc.hasVerificationMethod(PosDataCode.VerificationMethod.ONLINE_PIN))
                    pinEntry = "1";
                else
                if (pdc.hasVerificationMethod(PosDataCode.VerificationMethod.UNKNOWN))
                    pinEntry = "0";

                String panEntry = "00";

                if (pdc.hasReadingMethod(PosDataCode.ReadingMethod.PHYSICAL))
                    panEntry = "01";
                else
                if (pdc.hasReadingMethod(PosDataCode.ReadingMethod.MAGNETIC_STRIPE))
                    panEntry = "02";
                else
                if (pdc.hasReadingMethod(PosDataCode.ReadingMethod.BARCODE))
                    panEntry = "03";
                else
                if (pdc.hasReadingMethod(PosDataCode.ReadingMethod.ICC))
                    panEntry = "05";
                else
                if (pdc.hasReadingMethod(PosDataCode.ReadingMethod.CONTACTLESS))
                    panEntry = "07";

                r.set(22, panEntry + pinEntry);
            }

            if (message.hasField(41) && message.getString(41).length() > 8)
                r.set(41, message.getString(41).substring(0, 8));

            if (message.hasField(43)) {
                ISOMsg subfields = (ISOMsg) message.getComponent(43);
                String f43 = ISOUtil.padright(subfields.getString(2), 23, ' ')
                        + ISOUtil.padright(subfields.getString(4), 13, ' ')
                        + ISOUtil.padright(subfields.getString(5), 2, ' ')
                        + ISOUtil.padright(subfields.getString(7), 2, ' ');

                r.set(43, f43);
            }

            if (message.hasField(13)) {
                Date d = ISODate.parseISODate(message.getString(7));
                r.set(12, ISODate.formatDate(d, "HHmmss"));
            }

            if (message.hasField(25)) {
                r.set(25, message.getString(25));
            }

            String f39 = message.getString(39);
            if (f39 != null) {
                f39 = (f39.length() < 2) ? ISOUtil.zeropad(f39, 2) : f39.substring(f39.length() - 2);
                r.set(39, f39);
            }

            if (message.hasField(55)) {
                r.set(22, "051"); //AA fixed for testing, TODO: migrate tto POSDataCode
            }

            if (r.getMTI().contains("200")) {
                r.set(18, "6013"); //fetch from db
                r.set(23, "001");
                r.set(25, "00");
                r.set(26, message.getString(26));
                r.set(28, "D00000000");
                r.set(30, "C00000000");
                r.set(32, "111137"); //Acquiring institution code //fetch from db
                r.set(33, "111111"); //forwarding institution code

                String[] serviceRestrictionCode = message.getString(35).split("D");
                r.set(40,serviceRestrictionCode[1].substring(4,7)); //get service code from track 2
                r.set(42,"2GACE0000000001");
                r.set(56,"1510");
                r.set(59,"0282496282");
                r.set(123, message.getString(123));

                r.set(98,"3FAB0001");
                r.set(100,"627821"); //GA account  Bank code
                r.set(103,"0123484558"); //GA Bank account number

                r.set("127.02",r.getString(37).substring(2));
                r.set("127.13", "       000000 566"); //POS Geographic Data
                r.set("127.20", getSettlementDate()); //POS Geographic Data
                r.set("127.33","6008");

            }

            if (message.hasField(55)) {
                TLVList tlv = new TLVList();
                tlv.unpack(message.getBytes(55));
                r.set("127.25", buildRequestICCData(tlv, message));
                r.unset(55);
            }

            r.unset(46);

            //TODO - need to have proper mapping
            ISOMsg f122 = (ISOMsg)message.getComponent(122);
            if(f122!=null) {
                r.set(26, f122.getString(26));
                r.set(28, f122.getString(28));
//                r.set(40, f122.getString(40));
                r.set(123, f122.getString(123));
                r.unset(122);
            }
        }
        catch (Exception e) {
            evt.addMessage("--- ss_post_outgoing filter ---");
            evt.addMessage(e);
            throw new VetoException(e.getMessage());
        }

        return r;
    }

    private String getSettlementDate(){
        Date date = new Date();
        DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
        return dateFormat.format(date);
    }


    private String buildRequestICCData(TLVList tlv, ISOMsg isoMsg) {

        StringBuilder sb = new StringBuilder();

        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        sb.append("<IccData><IccRequest>");

        String type = isoMsg.getString(3).substring(0,2);
        //for BI override
        if("31".equals(type)) {
            sb.append(String.format("<AmountAuthorized>%s</AmountAuthorized>", "000000000000"));
        } else {
            if (tlv.hasTag(0x9F02))
                sb.append(String.format("<AmountAuthorized>%s</AmountAuthorized>", tlv.getString(0x9F02)));
        }


        if (tlv.hasTag(0x9F03))
            sb.append(String.format("<AmountOther>%s</AmountOther>", tlv.getString(0x9F03)));

        if (tlv.hasTag(0x4F))
            sb.append(String.format("<ApplicationIdentifier>%s</ApplicationIdentifier>", tlv.getString(0x4F)));

        if (tlv.hasTag(0x82))
            sb.append(String.format("<ApplicationInterchangeProfile>%s</ApplicationInterchangeProfile>", tlv.getString(0x82)));

        if (tlv.hasTag(0x9F36))
            sb.append(String.format("<ApplicationTransactionCounter>%s</ApplicationTransactionCounter>", tlv.getString(0x9F36)));

        if (tlv.hasTag(0x9F07))
            sb.append(String.format("<ApplicationUsageControl>%s</ApplicationUsageControl>", tlv.getString(0x9F07)));

        if (tlv.hasTag(0x9F26))
            sb.append(String.format("<Cryptogram>%s</Cryptogram>", tlv.getString(0x9F26)));

        if (tlv.hasTag(0x9F27))
            sb.append(String.format("<CryptogramInformationData>%s</CryptogramInformationData>", tlv.getString(0x9F27)));

        if (tlv.hasTag(0x8E))
            sb.append(String.format("<CvmList>%s</CvmList>", tlv.getString(0x8E)));

        if (tlv.hasTag(0x9F34))
            sb.append(String.format("<CvmResults>%s</CvmResults>", tlv.getString(0x9F34)));

        if (tlv.hasTag(0x9F1E))
            sb.append(String.format("<InterfaceDeviceSerialNumber>%s</InterfaceDeviceSerialNumber>", hexToAscii(tlv.getString(0x9F1E))));

        if (tlv.hasTag(0x9F10))
            sb.append(String.format("<IssuerApplicationData>%s</IssuerApplicationData>", tlv.getString(0x9F10)));

        if (tlv.hasTag(0x9F08))
            sb.append(String.format("<TerminalApplicationVersionNumber>%s</TerminalApplicationVersionNumber>", tlv.getString(0x9F08)));

        if (tlv.hasTag(0x9F33))
            sb.append(String.format("<TerminalCapabilities>%s</TerminalCapabilities>", tlv.getString(0x9F33)));

        if (tlv.hasTag(0x9F1A))
            sb.append(String.format("<TerminalCountryCode>%s</TerminalCountryCode>", tlv.getString(0x9F1A).substring(1)));

        if (tlv.hasTag(0x9F35))
            sb.append(String.format("<TerminalType>%s</TerminalType>", tlv.getString(0x9F35)));

        if (tlv.hasTag(0x95))
            sb.append(String.format("<TerminalVerificationResult>%s</TerminalVerificationResult>", tlv.getString(0x95)));

        if (tlv.hasTag(0x9F53))
            sb.append(String.format("<TransactionCategoryCode>%s</TransactionCategoryCode>", tlv.getString(0x9F53)));

        if (tlv.hasTag(0x5F2A))
            sb.append(String.format("<TransactionCurrencyCode>%s</TransactionCurrencyCode>", tlv.getString(0x5F2A).substring(1)));

        if (tlv.hasTag(0x9A))
            sb.append(String.format("<TransactionDate>%s</TransactionDate>", tlv.getString(0x9A)));

        appendICCTag(sb, tlv, 0x9F41, "TransactionSequenceCounter");
        //for BI override
        if("31".equals(type)) {
            appendICCTag(sb, "00", "TransactionType");
        } else {
            appendICCTag(sb, tlv, 0x9C, "TransactionType");
        }

        if (tlv.hasTag(0x9F37))
            sb.append(String.format("<UnpredictableNumber>%s</UnpredictableNumber>", tlv.getString(0x9F37)));

        sb.append("</IccRequest></IccData>");

        return sb.toString();
    }

    private static void appendICCTag(StringBuilder sb, TLVList tlv, int tag, String elementName) {

        if (tlv.hasTag(tag))
            sb.append(String.format("<%s>%s</%s>",
                    elementName, tlv.getString(tag), elementName));
    }
    private static void appendICCTag(StringBuilder sb, String value, String elementName) {

        if (value!=null)
            sb.append(String.format("<%s>%s</%s>",
                    elementName, value, elementName));
    }

    private static String hexToAscii(String hexStr) {
        StringBuilder output = new StringBuilder("");

        for (int i = 0; i < hexStr.length(); i += 2) {
            String str = hexStr.substring(i, i + 2);
            output.append((char) Integer.parseInt(str, 16));
        }

        return output.toString();
    }


}
