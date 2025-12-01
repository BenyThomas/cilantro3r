package com.models;


import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.Date;

@Entity
@Table(name = "thirdpartytxns")
public class ThirdPartyTxn {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String txn_type;
    private String ttype;
    private String txnid;
    private Date txndate;
    private String sourceAccount;
    private String receiptNo;
    private BigDecimal amount;
    private BigDecimal charge;
    private String description;
    private String currency;
    private String mnoTxns_status;
    private String terminal;
    private String txdestinationaccount;
    private String acct_no;
    private String status;
    private BigDecimal post_balance;
    private BigDecimal previous_balance;
    private String file_name;
    private String pan;
    private String identifier;
    private String docode_desc;
    private String docode;
    private Date file_txndate;
    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTxn_type() {
        return txn_type;
    }

    public void setTxn_type(String txn_type) {
        this.txn_type = txn_type;
    }

    public String getTtype() {
        return ttype;
    }

    public void setTtype(String ttype) {
        this.ttype = ttype;
    }

    public String getTxnid() {
        return txnid;
    }

    public void setTxnid(String txnid) {
        this.txnid = txnid;
    }

    public Date getTxndate() {
        return txndate;
    }

    public void setTxndate(Date txndate) {
        this.txndate = txndate;
    }

    public String getSourceAccount() {
        return sourceAccount;
    }

    public void setSourceAccount(String sourceAccount) {
        this.sourceAccount = sourceAccount;
    }

    public String getReceiptNo() {
        return receiptNo;
    }

    public void setReceiptNo(String receiptNo) {
        this.receiptNo = receiptNo;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getCharge() {
        return charge;
    }

    public void setCharge(BigDecimal charge) {
        this.charge = charge;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getMnoTxns_status() {
        return mnoTxns_status;
    }

    public void setMnoTxns_status(String mnoTxns_status) {
        this.mnoTxns_status = mnoTxns_status;
    }

    public String getTerminal() {
        return terminal;
    }

    public void setTerminal(String terminal) {
        this.terminal = terminal;
    }

    public String getTxdestinationaccount() {
        return txdestinationaccount;
    }

    public void setTxdestinationaccount(String txdestinationaccount) {
        this.txdestinationaccount = txdestinationaccount;
    }

    public String getAcct_no() {
        return acct_no;
    }

    public void setAcct_no(String acct_no) {
        this.acct_no = acct_no;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public BigDecimal getPost_balance() {
        return post_balance;
    }

    public void setPost_balance(BigDecimal post_balance) {
        this.post_balance = post_balance;
    }

    public BigDecimal getPrevious_balance() {
        return previous_balance;
    }

    public void setPrevious_balance(BigDecimal previous_balance) {
        this.previous_balance = previous_balance;
    }

    public String getFile_name() {
        return file_name;
    }

    public void setFile_name(String file_name) {
        this.file_name = file_name;
    }

    public String getPan() {
        return pan;
    }

    public void setPan(String pan) {
        this.pan = pan;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getDocode_desc() {
        return docode_desc;
    }

    public void setDocode_desc(String docode_desc) {
        this.docode_desc = docode_desc;
    }

    public String getDocode() {
        return docode;
    }

    public void setDocode(String docode) {
        this.docode = docode;
    }

    public Date getFile_txndate() {
        return file_txndate;
    }

    public void setFile_txndate(Date file_txndate) {
        this.file_txndate = file_txndate;
    }

    @Override
    public String toString() {
        return "ThirdPartyTxn{" +
                "id=" + id +
                ", txn_type='" + txn_type + '\'' +
                ", ttype='" + ttype + '\'' +
                ", txnid='" + txnid + '\'' +
                ", txndate=" + txndate +
                ", sourceAccount='" + sourceAccount + '\'' +
                ", receiptNo='" + receiptNo + '\'' +
                ", amount=" + amount +
                ", charge=" + charge +
                ", description='" + description + '\'' +
                ", currency='" + currency + '\'' +
                ", mnoTxns_status='" + mnoTxns_status + '\'' +
                ", terminal='" + terminal + '\'' +
                ", txdestinationaccount='" + txdestinationaccount + '\'' +
                ", acct_no='" + acct_no + '\'' +
                ", status='" + status + '\'' +
                ", post_balance=" + post_balance +
                ", previous_balance=" + previous_balance +
                ", file_name='" + file_name + '\'' +
                ", pan='" + pan + '\'' +
                ", identifier='" + identifier + '\'' +
                ", docode_desc='" + docode_desc + '\'' +
                ", docode='" + docode + '\'' +
                '}';
    }
}
