package com.telcoilng.fraudprocessor.transactions.repository;

import com.telcoilng.fraudprocessor.transactions.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction,Long> {
    Optional<Transaction>findTransactionByde37AndDe11(String rrn, String stan);
}
