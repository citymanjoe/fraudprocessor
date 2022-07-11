package com.telcoilng.fraudprocessor.Entity;

import lombok.*;

import javax.persistence.*;
import java.util.Date;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Entity(name = "terminals")
public class Terminals {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(unique = true)
    private String terminalId;
    private String terminalSerialNumber;
    private TerminalType terminalType;
    private int numberOfPaymentTime;
    private String actualTerminalName;
    private String preferredTerminalName;
    private String description;
    @Column(columnDefinition="TEXT")
    private String image;
    private String terminalName;
    private String terminalAmount;
    private String amountPaid;
    private String amountLeft;
    private StatusType status;
    private Date issuedDate;
    private boolean deleted;
    private String assignedBy;
    private String assignedTo;
    private String userId;
    private Date dateCreated;
    private Date dateUpdated;
    private String updatedBy;
    private boolean assignedFlag;
    @ManyToOne
    private Merchants merchants;

}


