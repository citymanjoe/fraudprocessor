package com.telcoilng.fraudprocessor.repository;

import com.telcoilng.fraudprocessor.Entity.Station;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StationRepository extends JpaRepository<Station,Long> {
    Station findByName(String stationName);
}
