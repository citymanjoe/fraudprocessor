package com.telcoilng.fraudprocessor.repository;

import com.telcoilng.fraudprocessor.Entity.Merchants;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MerchantsRepository extends JpaRepository<Merchants, Long> {

}