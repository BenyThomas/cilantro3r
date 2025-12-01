package com.DTO.tips;

public class TipsPaymentFullObject {
    public String txnType;
    public String sourceAcct;
    public String destinationAcct;
    public String amount;
    public String currency;
    public String beneficiaryName;
    public String beneficiaryBic;
    public String beneficiaryContact;
    public String senderPhone;
    public String senderAddress;
    public String senderName;
    public String reference;
    public String txid;
    public String batchReference;
    public String instrId;
    public String purpose;
    public String swiftMessage;
    public String direction;
    public String initiatedBy;
    public String branchNo;
    public String cbsStatus;
    public String fspCategory;

    public String getTxnType() {
        return txnType;
    }

    public void setTxnType(String txnType) {
        this.txnType = txnType;
    }

    public String getSourceAcct() {
        return sourceAcct;
    }

    public void setSourceAcct(String sourceAcct) {
        this.sourceAcct = sourceAcct;
    }

    public String getDestinationAcct() {
        return destinationAcct;
    }

    public void setDestinationAcct(String destinationAcct) {
        this.destinationAcct = destinationAcct;
    }

    public String getAmount() {
        return amount;
    }

    public void setAmount(String amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getBeneficiaryName() {
        return beneficiaryName;
    }

    public void setBeneficiaryName(String beneficiaryName) {
        this.beneficiaryName = beneficiaryName;
    }

    public String getBeneficiaryBic() {
        return beneficiaryBic;
    }

    public void setBeneficiaryBic(String beneficiaryBic) {
        this.beneficiaryBic = beneficiaryBic;
    }

    public String getBeneficiaryContact() {
        return beneficiaryContact;
    }

    public void setBeneficiaryContact(String beneficiaryContact) {
        this.beneficiaryContact = beneficiaryContact;
    }

    public String getSenderPhone() {
        return senderPhone;
    }

    public void setSenderPhone(String senderPhone) {
        this.senderPhone = senderPhone;
    }

    public String getSenderAddress() {
        return senderAddress;
    }

    public void setSenderAddress(String senderAddress) {
        this.senderAddress = senderAddress;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public String getTxid() {
        return txid;
    }

    public void setTxid(String txid) {
        this.txid = txid;
    }

    public String getBatchReference() {
        return batchReference;
    }

    public void setBatchReference(String batchReference) {
        this.batchReference = batchReference;
    }

    public String getInstrId() {
        return instrId;
    }

    public void setInstrId(String instrId) {
        this.instrId = instrId;
    }

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }

    public String getSwiftMessage() {
        return swiftMessage;
    }

    public void setSwiftMessage(String swiftMessage) {
        this.swiftMessage = swiftMessage;
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    public String getInitiatedBy() {
        return initiatedBy;
    }

    public void setInitiatedBy(String initiatedBy) {
        this.initiatedBy = initiatedBy;
    }

    public String getBranchNo() {
        return branchNo;
    }

    public void setBranchNo(String branchNo) {
        this.branchNo = branchNo;
    }

    public String getCbsStatus() {
        return cbsStatus;
    }

    public void setCbsStatus(String cbsStatus) {
        this.cbsStatus = cbsStatus;
    }

    public String getFspCategory() {
        return fspCategory;
    }

    public void setFspCategory(String fspCategory) {
        this.fspCategory = fspCategory;
    }

    @Override
    public String toString() {
        return "TipsPaymentFullObject{" +
                "txnType='" + txnType + '\'' +
                ", sourceAcct='" + sourceAcct + '\'' +
                ", destinationAcct='" + destinationAcct + '\'' +
                ", amount='" + amount + '\'' +
                ", currency='" + currency + '\'' +
                ", beneficiaryName='" + beneficiaryName + '\'' +
                ", beneficiaryBic='" + beneficiaryBic + '\'' +
                ", beneficiaryContact='" + beneficiaryContact + '\'' +
                ", senderPhone='" + senderPhone + '\'' +
                ", senderAddress='" + senderAddress + '\'' +
                ", senderName='" + senderName + '\'' +
                ", reference='" + reference + '\'' +
                ", txid='" + txid + '\'' +
                ", batchReference='" + batchReference + '\'' +
                ", instrId='" + instrId + '\'' +
                ", purpose='" + purpose + '\'' +
                ", swiftMessage='" + swiftMessage + '\'' +
                ", direction='" + direction + '\'' +
                ", initiatedBy='" + initiatedBy + '\'' +
                ", branchNo='" + branchNo + '\'' +
                ", cbsStatus='" + cbsStatus + '\'' +
                ", fspCategory='" + fspCategory + '\'' +
                '}';
    }
}
