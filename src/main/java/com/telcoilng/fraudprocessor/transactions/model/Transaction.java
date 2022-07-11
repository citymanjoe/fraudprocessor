package com.telcoilng.fraudprocessor.transactions.model;


import com.telcoilng.fraudprocessor.Entity.PaymentStatus;
import com.telcoilng.fraudprocessor.Entity.Terminals;
import lombok.*;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Getter
@Setter
@Table(name = "transactions")
public class Transaction implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String mti;
    private String de2;
    private String de3;
    private String de4;
    private Date de7;
    private String de11;
    private String de12;
    private String de13;
    private String de14;
    private String de18;
    private String de22;
    private String de23;
    private String de25;
    private String de26;
    private String de28;
    private String de32;
    private String de33;
    private String de35;
    @Column(unique = true)
    private String de37;
    private String de38;
    private String de40;
    private String de41;
    private String de42;
    private String de43;
    private String de49;
    private String de59;
    private String de61;
    private String de90;
    private String de95;
    private String de123;
    private String responsecode;
    private Date createdAt;
    private String processedBy;
    private String transactionCategory;
    private PaymentStatus paymentStatus;
    private String terminalType;
    private String paymentMethod;
    private String channel;
    @ManyToOne
    private Terminals terminals;
    private boolean reversed;
}
