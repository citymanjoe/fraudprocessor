package com.telcoilng.fraudprocessor.processors.unifiedpayment;

import org.jpos.iso.*;
import org.jpos.iso.channel.PostChannel;
import org.jpos.iso.packager.GenericPackager;
import org.jpos.tlv.TLVList;
import org.jpos.util.LogEvent;

import java.io.IOException;
import java.math.BigDecimal;
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

            // keep rightmost 6 digits of STAN, with zero padding
            String stan = r.getString(11);
            stan = (stan.length() < 6) ? ISOUtil.zeropad(stan, 6) : stan.substring(stan.length() - 6);
            r.set(11, stan);
            if (message.hasField(3)&&message.getString(3).startsWith("00")){
                r.set(3,"01"+message.getString(3).substring(2));
            }

            if (message.hasField(4)) {

                String amountVal = message.getString(4);

                ISOAmount amount = new ISOAmount(4,566,new BigDecimal(amountVal.substring(0,amountVal.length()-2)+"."+amountVal.substring(amountVal.length()-2)));

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

            if (message.hasField((26))) {
                r.set(18, message.getString(26));
//                r.unset(26);
            }

            if (message.hasField(41) && message.getString(41).length() > 8)
                r.set(41, message.getString(41).substring(0, 8));



            if (message.hasField(13)) {

                Date d = ISODate.parseISODate(message.getString(7));

                r.set(12, ISODate.formatDate(d, "HHmmss"));
            }

            if (message.hasField(25)) {
                //r.set(56, message.getString(25));
                //r.unset(25);
                r.set(25, message.getString(25));
            }

            String f39 = message.getString(39);
            if (f39 != null) {
                f39 = (f39.length() < 2) ? ISOUtil.zeropad(f39, 2) : f39.substring(f39.length() - 2);
                r.set(39, f39);
            }


            if (message.hasField(55)) {
                //For BCX they use standard field 55 for ICC
                //                r.set("127.25", message.getBytes(55));
                //                r.unset(55);
                r.set(22, "051"); //AA fixed for testing, TODO: migrate tto POSDataCode
            }

            if (message.hasField(56)) {
                String field33 = "000000";
                if (message.hasField(33)){
                    field33 = message.getString(33);
                }

                r.unset(56); // For postilion this means Message reason code and is optional
                // original data elements should go in field 90
                StringBuilder originalDataElements = new StringBuilder(42);
                String f56 = message.getString(56);
                originalDataElements.append('0') //replace mti version number
                        //Original message type (positions 1 - 4) - the message type identifier of the original message of
                        //the transaction being reversed.
                        .append(f56.substring(1, 4))
                        //Original systems trace audit number (positions 5 - 10) - the systems trace audit number (field
                        //11) of the original message.
                        .append(f56.substring(10, 16))
                        /*Original transmission date and time (positions 11 - 20) - the transmission date and time (field
                          7) of the original message
                        */
                        .append(f56.substring(20, 30))
                        /*Original acquirer institution ID code (position 21 - 31) - the acquirer institution ID code (field
                        32) of the original message (right justified with leading zeroes).
                        */

                        .append("00000")
                        .append(field33)
                        .append("00000")
                        .append(message.getString(32).trim().replace(" ",""));

                        /* AA: CMF does not have original forwarding institution ID,
                        Original forwarding institution ID code (position 32 - 42) - the forwarding institution ID code
                        (field 33) of the original message (right justified with leading zeroes).
                        */
                r.set(90, originalDataElements.toString());
            }

            if (r.getMTI().contains("200")) {
                r.set(18, "6010");
                r.set(25, "00");
                r.set(26, "04");
                r.set(28, "D00000000");
                r.set(123, "511101513344101");

            }

            if (message.hasField(55)) {
                String f55 = message.getString(55);
                TLVList tlv = new TLVList();
                tlv.unpack(ISOUtil.hex2byte(f55));

//                tlv.unpack(message.getBytes(55));
                r.set("127.25", buildRequestICCData(tlv, message));
                r.unset(55);
            }

            if (message.hasField(23)){
                r.set(23,message.getString(23));
            }

            r.unset(46);

            //TODO - need to have proper mapping
            ISOMsg f122 = (ISOMsg)message.getComponent(122);
            if(f122!=null) {
//            	r.set(26, f122.getString(26));
                r.set(28, f122.getString(28));
                r.set(40, f122.getString(40));
                r.set(123, f122.getString(123));
                r.unset(122);
            }

            if (r.getMTI().contains("200")&&r.hasField(2)){
                r.set(103,"87001510"); //vendor ID. make this configurble
                if (r.getString(2).startsWith("95010")){
                    r.unset(127);
                    r.set(62,"00698WD010133307032743655");
                }
            }

            if (r.hasField(128))
                r.unset(128);
        }
        catch (Exception e) {
            evt.addMessage("--- ss_post_outgoing filter ---");
            evt.addMessage(e);
            throw new VetoException(e.getMessage());
        }

        return r;
    }
    private static String buildRequestICCData(TLVList tlv, ISOMsg isoMsg) {

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
            sb.append(String.format("<InterfaceDeviceSerialNumber>%s</InterfaceDeviceSerialNumber>", tlv.getString(0x9F1E)));


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


    public static void main(String...args){
        Date now = new Date();
        try {
            GenericPackager packager = new GenericPackager("/Users/joshua-omonigho/IdeaProjects/dest-station-unifiedpayment/modules/ds-unifiedpayment/src/dist/cfg/postpack.xml");
            PostChannel channel = new PostChannel("52.56.66.162",30482,packager);
//            channel.connect();

            if (true){
                ISOMsg msg = new ISOMsg();
                msg.set(0,"0200");
                msg.set(2, "5199110731026859");
                msg.set(3, "010000");
                msg.set(4, "000000001000");
                msg.set(7, "0307115826");
                msg.set(11, "000012");   // we can leave STAN with 6 figures
                msg.set(12, "115826");
                msg.set(13, "0307");
                msg.set(14, "2112");
                msg.set(18, "6010");
                msg.set(22, "051");
                msg.set(23, "000");
                msg.set(25, "00");
                msg.set(26, "06");
                msg.set(28, "C00000000");
                msg.set(32, "111129");
                msg.set(35, "5199110731026859D2112221002435282");
                msg.set(37, "220307115826");
                msg.set(40,"221");
                msg.set(41,"20442R11");
                msg.set(42,"2UP1LA000003506");
                msg.set(43,"CINTRUST MICROFINANCE BOY           LANG");
                msg.set(49,"566");
                msg.set(52,ISOUtil.hex2byte("BF0DFE03B3A44537"));
                msg.set(56,"1510");
                msg.set(59,"00022124733");
                msg.set(98,"20442R11");
                msg.set(103,"87001530");
                msg.set(123,"510111511344101");
                msg.set(55,"9F2608E41509E1106E117C9F2701809F10120110A50003020000000000000000000000FF9F370485E86F299F3602009F950504C00088009A032203079C01009F02060000000010005F2A020566820239009F1A0205669F34034103029F3303E0F8C89F3501228407A00000000410105F340100");
                String f55 = "9F2608E41509E1106E117C9F2701809F10120110A50003020000000000000000000000FF9F370485E86F299F3602009F950504C00088009A032203079C01009F02060000000010005F2A020566820239009F1A0205669F34034103029F3303E0F8C89F3501228407A00000000410105F340100";
                TLVList tlv = new TLVList();
                tlv.unpack(ISOUtil.hex2byte(f55));
                msg.set("127.25", buildRequestICCData(tlv, msg));
                msg.unset(55);

                channel.send(msg);

                msg.dump(System.out,"");

                ISOMsg resp = channel.receive();
                resp.dump(System.out,"");
                System.out.println(resp);

            }
        } catch (ISOException | IOException e) {
            e.printStackTrace();
        }
    }

}
