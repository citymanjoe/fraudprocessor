package com.telcoilng.fraudprocessor.listener;

import com.telcoilng.fraudprocessor.Entity.*;
import com.telcoilng.fraudprocessor.cache.SchemeCache;
import com.telcoilng.fraudprocessor.cache.StaticKeyMap;
import com.telcoilng.fraudprocessor.cache.StationCache;
import com.telcoilng.fraudprocessor.processors.QueryHost;
import com.telcoilng.fraudprocessor.processors.nibss.service.NibssService;
import com.telcoilng.fraudprocessor.repository.NibssKeysRepository;
import com.telcoilng.fraudprocessor.repository.RoutingRuleRepository;
import com.telcoilng.fraudprocessor.repository.SchemeRepository;
import com.telcoilng.fraudprocessor.repository.StationRepository;
import com.telcoilng.fraudprocessor.utils.CryptoUtil;
import com.telcoilng.fraudprocessor.utils.CryptoUtilImpl;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.DecoderException;
import org.bouncycastle.crypto.CryptoException;
import org.jpos.iso.ISOMsg;
import org.jpos.transaction.Context;
import org.jpos.transaction.ContextConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.naming.NameNotFoundException;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ProcessTransactionImpl implements ProcessTransaction {

    private final RoutingRuleRepository routingRuleRepository;
    private final SchemeRepository schemeRepository;
    private final StationRepository stationRepository;
    private final NibssKeysRepository nibssKeysRepository;
    private final NibssService nibssService;
    private final QueryHost queryHost;
    ExecutorService executorService = Executors.newCachedThreadPool();

    @Autowired
    public ProcessTransactionImpl(RoutingRuleRepository routingRuleRepository, SchemeRepository schemeRepository, StationRepository stationRepository, NibssKeysRepository nibssKeysRepository, NibssService nibssService, QueryHost queryHost) {
        this.routingRuleRepository = routingRuleRepository;
        this.schemeRepository = schemeRepository;
        this.stationRepository = stationRepository;
        this.nibssKeysRepository = nibssKeysRepository;
        this.nibssService = nibssService;
        this.queryHost = queryHost;
    }

    @Override
    public Context processTransaction(ISOMsg msg, Context context) {
        log.info("Transaction Processing with context");
        try {
            ISOMsg response = (ISOMsg) msg.clone();
            Terminals terminals = context.get("TERMINAL");
            context.log("About processing transaction for terminal id "+msg.getString(41));

            List<RoutingRule> routingRules = routingRuleRepository.findByUserIdAndDeleted(terminals.getUserId(),false)
                    .stream()
//                    .filter(routingRule -> !routingRule.is_deleted())
                    .sorted(Comparator.comparing(RoutingRule::getPrecedence))
                    .collect(Collectors.toList());

            if (routingRules.isEmpty()){
                log.info("routing rules is empty for user {}", terminals.getUserId());
                response.set(39, "92");
                context.put("RESPONSE",response);
                return context;
            }

            String destinationStation = "";
            for (RoutingRule rule : routingRules){
                context.log("rule size is "+routingRules.size());
                switch (rule.getType()) {
                    case "SCHEME":
                        destinationStation = getSchemeRoute(destinationStation,rule,context,msg);
                        break;
                    default:
                        context.put(ContextConstants.DESTINATION.toString(), "Routing Error");
                        throw new NameNotFoundException("Routing rule not found");

                }
                if (!destinationStation.isEmpty()){
                    log.info("routing transaction to {}",destinationStation);
                    context.log("routing transaction to "+destinationStation);
                    break;
                }
                log.info("searching for route");

                context.log("searching for route");

            }

            //if routing rule not found, set destination to NIBSS
            if (destinationStation.isEmpty()){
                context.log("routing rule not found");
                context.put(ContextConstants.DESTINATION.toString(), "Routing Error");
                throw new Exception ("INTERNAL_ROUTING_ERROR");
            }

            //do pin translation
            if (msg.hasField(52)){
                doPinTranslation(context, msg, destinationStation);
            }

            context.put("REQUEST",msg);
            context.put("DS", destinationStation);

        } catch (Exception e){
            log.info(e.getMessage());
            msg.set(39,"91");
            context.put("RESPONSE",msg);
            return context;
        }


        if (context.get("DS").equals("nibss")){
            return nibssService.toNibss(context);
        }
        return queryHost.queryHost(context);

    }

    private String getSchemeRoute(String destinationStation,RoutingRule route,Context ctx,ISOMsg msg) {
        //get scheme list
        //get route for the scheme
        List<Scheme> schemes;
        if (SchemeCache.schemeListMap.containsKey("SCHEME")) {
            schemes = SchemeCache.schemeListMap.get("SCHEME");
        }
        else{
            schemes = schemeRepository.findAll();
            ctx.log(schemes);
            executorService.execute(() -> SchemeCache.schemeListMap.put("SCHEME",schemes));
        }

        String scheme = "";
        for (int count = 0; count <schemes.size(); count++){
            String regex  = schemes.get(count).getRegex();
            ctx.log("regex gotten is "+regex);
            Pattern p = Pattern.compile(regex);
            //fix
            if (Objects.isNull(msg.getString(2))){
                ctx.log("Pan not present");
                break;
            }
            Matcher m = p.matcher(msg.getString(2));
            if (m.matches()){
                scheme = schemes.get(count).getName();
                ctx.log("scheme gotten is "+scheme);
                break;
            }
            ctx.log("scheme not found ");
        }
        //scheme gotten
        if (scheme.equalsIgnoreCase(route.getScheme())){
            if (route.getMaximum_amount()!=null||route.getMinimum_amount()!=null){
                BigDecimal minimumAmmount = new BigDecimal(route.getMinimum_amount());
                BigDecimal maximumAmount = new BigDecimal(route.getMaximum_amount());
                BigDecimal transactionAmount = new BigDecimal(msg.getString(4)
                        .substring(0,msg.getString(4).length()-2)+"."+msg.getString(4).substring(msg.getString(4).length()-2));

                if (transactionAmount.compareTo(minimumAmmount) > 0 && transactionAmount.compareTo(maximumAmount)<0){
                    destinationStation = route.getStationId().getName();
                    ctx.log("Destination set to  "+destinationStation);

                }
            }
            else
                destinationStation = route.getStationId().getName();

        }
        if (!destinationStation.isEmpty())
            ctx.log("Destination set to  "+destinationStation);
        return destinationStation;
    }

    private ISOMsg doPinTranslation(Context ctx,ISOMsg msg,String destination) throws Exception {
        String[] sourceName = msg.getString(123).split(",");
        ctx.log("Source name is "+sourceName[1]+"_SS");

        //if the ds is pointing to nibss
        if (destination.equals("nibss")){
            //do nibss pin translation here
            Station station;
            if(StationCache.stationHashMap.containsKey(destination))
                station = StationCache.stationHashMap.get(destination);
            else {
                station = stationRepository.findByName(destination);
                executorService.execute(()->StationCache.stationHashMap.put(destination,station));

            }
            String ctmk = station.getZpk();    //newZPK is the ctmk //work on encrypting it later
            NibssKeys nibssKeys = nibssKeysRepository.findByTerminalId(msg.getString(41));


            if (Objects.isNull(nibssKeys)||nibssKeys.getETmk().isEmpty()||nibssKeys.getETsk().isEmpty()||nibssKeys.getETpk().isEmpty()){
                //return 96
                throw new Exception("Nibss Keys not found or complete. do key Exchange again");
            }
            else {
                try {
                    ctx.log("ctmk  is"+ctmk);
                    ctx.log("encrypted masterkey is"+nibssKeys.getETmk());
                    ctx.log("encrypted sessionkey is"+nibssKeys.getETsk());
                    ctx.log("encrypted pinkey is"+nibssKeys.getETpk()   );

                    String translatedPin = translateForNibss(msg, ctmk, nibssKeys);
                    ctx.log("translated nibss pinblock is "+translatedPin);
                } catch (CryptoException | DecoderException e) {
                    ctx.log(e.getLocalizedMessage());
                    e.printStackTrace();
                }
            }
        }
        else {
            CryptoUtil cryptoUtil = new CryptoUtilImpl();
            String translatedPin = cryptoUtil.translatePin(msg.getString(52),StaticKeyMap.terminalKeys.get(msg.getString(41)).getTpk(),
                    StationCache.stationHashMap.get(destination).getZpk());
            ctx.log("SSM Pin translated from "+msg.getString(52)+"to "+translatedPin);
            msg.unset(52);
            msg.set(52,translatedPin.toUpperCase());
        }

        return msg;

    }

    private String translateForNibss(ISOMsg msg, String ctmk, NibssKeys nibssKeys) throws CryptoException, DecoderException {
        CryptoUtilImpl util = new CryptoUtilImpl();
        String clearTmk = util.decryptPinBlock(nibssKeys.getETmk(), ctmk).toUpperCase();
        String zpk = util.decryptPinBlock(nibssKeys.getETpk(),clearTmk).toUpperCase();
        String tpk = StaticKeyMap.terminalKeys.get(msg.getString(41)).getTpk();

        System.out.println("clear pinkey is "+tpk);

        String translatedPin = util.translatePin(msg.getString(52), tpk,zpk);


        msg.set(52,translatedPin.toUpperCase());

        return translatedPin;
    }

}
