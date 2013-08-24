//Authored by Ajay Solanky
//Copyright Julep Beauty Inc. 2013

package models;

import play.db.ebean.Model;
import play.Logger;
import java.util.*;
import java.io.*;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class Transaction extends Model {

    @Id
    public Integer id;

    @Basic
    protected Integer uploadID;
    protected String sessionID;
    protected String batchID;
    protected String batchPostDay;
    protected String transactionProcessingTimestampGMT;
    protected String batchCompletion;
    protected String reportingGroup;
    protected String presenter;
    protected String merchant;
    protected String merchantID;
    protected String litlePaymentID;
    protected String parentLitlePaymentID;
    protected String merchantOrderNumber;
    protected String customerID;
    protected String txnType;
    protected String purchaseCurrency;
    @Column(columnDefinition = "float")
    protected Float purchaseAmount;
    protected String paymentType;
    protected String bin;
    @Column(columnDefinition = "integer")
    protected Integer accountSuffix;
    protected String responseReasonCode;
    protected String responseReasonMessage;
    protected String avs;
    protected String fraudCheckSumResponse;
    protected String payerID;
    protected String merchantTransactionID;
    protected String affiliate;
    protected String campaign;
    protected String merchantGroupingID;

    public static Model.Finder<Integer,Transaction> find = new Finder<Integer,Transaction>(Integer.class,Transaction.class);

    public Transaction(Integer upid, String line) {
        String[] split = FileHelper.splitLine(line);
        for (int i = 0; i<split.length;i++) {
            split[i] = FileHelper.stripQuotes(split[i]);
        }
        uploadID = upid;
        sessionID = split[0];
        batchID = split[1];
        batchPostDay = split[2];
        transactionProcessingTimestampGMT = split[3];
        batchCompletion = split[4];
        reportingGroup = split[5];
        presenter = split[6];
        merchant = split[7];
        merchantID = split[8];
        litlePaymentID = split[9];
        parentLitlePaymentID = split[10];
        merchantOrderNumber = split[11];
        customerID = split[12];
        txnType = split[13];
        purchaseCurrency = split[14];
        purchaseAmount = Float.parseFloat(split[15]);
        paymentType = split[16];
        bin = split[17];
        accountSuffix = Integer.parseInt(split[18]);
        responseReasonCode = split[19];
        responseReasonMessage = split[20];
        avs = split[21];
        fraudCheckSumResponse = split[22];
        payerID = split[23];
        merchantTransactionID = split[24];
        affiliate = split[25];
        campaign = split[26];
        merchantGroupingID = split[27];
    }

}
