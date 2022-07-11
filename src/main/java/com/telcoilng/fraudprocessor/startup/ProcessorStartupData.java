package com.telcoilng.fraudprocessor.startup;

import com.telcoilng.fraudprocessor.Entity.RoutingRule;
import com.telcoilng.fraudprocessor.Entity.Scheme;
import com.telcoilng.fraudprocessor.Entity.Station;
import com.telcoilng.fraudprocessor.repository.RoutingRuleRepository;
import com.telcoilng.fraudprocessor.repository.SchemeRepository;
import com.telcoilng.fraudprocessor.repository.StationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j

public class ProcessorStartupData implements CommandLineRunner {
    private final StationRepository stationRepository;
    private final SchemeRepository schemeRepository;
    private final RoutingRuleRepository routingRuleRepository;
    @Override
    public void run(String... args) throws Exception {
        if (stationRepository.findById(1L).isPresent()){
            log.info("Stations already exist");
        }
        else {
            Station station = new Station();

            station.setStatus("ACTIVE");
            station.setName("unifiedpayment");
            station.setZpk("3cdde1cc6fdd225c9a8bc3eb065509a6".toUpperCase());
            station.setZmk("3cdde1cc6fdd225c9a8bc3eb065509a6".toUpperCase());
            stationRepository.save(station);

            Station station2 = new Station();
            station2.setStatus("ACTIVE");
            station2.setName("nibss");
            station2.setZpk("DBEECACCB4210977ACE73A1D873CA59F".toUpperCase());
            stationRepository.save(station2);

            Station station3 = new Station();
            station3.setStatus("ACTIVE");
            station3.setName("isw");
            station3.setZpk("DBEECACCB4210977ACE73A1D873CA59F".toUpperCase());
            stationRepository.save(station3);


            Scheme scheme = new Scheme();
            scheme.setName("MasterCard");
            scheme.setRegex("^5[1-5][0-9]{14}|^(222[1-9]|22[3-9]\\\\d|2[3-6]\\\\d{2}|27[0-1]\\\\d|2720)[0-9]{12}$");
            schemeRepository.save(scheme);

            Scheme scheme2 = new Scheme();
            scheme2.setName("MasterCard");
            scheme2.setRegex("^6799\\d{15,18}$");
            schemeRepository.save(scheme2);

            Scheme scheme3 = new Scheme();
            scheme3.setName("MasterCard");
            scheme3.setRegex("^6799\\d{0,100}$");
            schemeRepository.save(scheme3);

            Scheme scheme4 = new Scheme();
            scheme4.setName("Visa");
            scheme4.setRegex("^4[0-9]{12}(?:[0-9]{3})?$");
            schemeRepository.save(scheme4);

            Scheme scheme5 = new Scheme();
            scheme5.setName("Visa");
            scheme5.setRegex("^4\\d{15,18}$");
            schemeRepository.save(scheme5);

            Scheme scheme6 = new Scheme();
            scheme6.setName("Verve");
            scheme6.setRegex("^3[0-9]{12}(?:[0-9]{3})?$");
            schemeRepository.save(scheme6);

            Scheme scheme7 = new Scheme();
            scheme7.setName("Verve");
            scheme7.setRegex("^5061\\d{0,100}$");
            schemeRepository.save(scheme7);

            Station station1 = stationRepository.findByName("unifiedpayment");
            RoutingRule rule = new RoutingRule();
            rule.setDeleted(false);
            rule.setScheme("MasterCard");
            rule.setType("SCHEME");
            rule.setStationId(station1);
            rule.setPrecedence(1);
            routingRuleRepository.save(rule);

            RoutingRule rule2 = new RoutingRule();
            rule2.setDeleted(false);
            rule2.setScheme("Visa");
            rule2.setType("SCHEME");
            rule2.setStationId(station1);
            rule2.setPrecedence(2);
            routingRuleRepository.save(rule2);

            RoutingRule rule3 = new RoutingRule();
            rule3.setDeleted(false);
            rule3.setScheme("Verve");
            rule3.setType("SCHEME");
            rule3.setStationId(station1);
            rule3.setPrecedence(3);
            routingRuleRepository.save(rule3);
        }

    }
}
