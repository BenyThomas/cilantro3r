/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.service;

import com.DTO.EFT.EftPacs00400102Req;
import com.DTO.EFT.EftPacs00800102OutgoingReq;
import com.config.SYSENV;
import com.helper.DateUtil;
import com.helper.SignRequest;
import com.repository.BanksRepo;
import com.repository.TransactionRepo;
import com.prowidesoftware.swift.model.mx.MxPacs00200103;
import com.prowidesoftware.swift.model.mx.MxPacs00400102;
import com.prowidesoftware.swift.model.mx.MxPacs00800102;
import com.prowidesoftware.swift.model.mx.MxPain00100103;
import com.prowidesoftware.swift.model.mx.dic.AccountIdentification4Choice;
import com.prowidesoftware.swift.model.mx.dic.ActiveCurrencyAndAmount;
import com.prowidesoftware.swift.model.mx.dic.ActiveOrHistoricCurrencyAndAmount;
import com.prowidesoftware.swift.model.mx.dic.AmountType3Choice;
import com.prowidesoftware.swift.model.mx.dic.BranchAndFinancialInstitutionIdentification4;
import com.prowidesoftware.swift.model.mx.dic.CashAccount16;
import com.prowidesoftware.swift.model.mx.dic.ChargeBearerType1Code;
import com.prowidesoftware.swift.model.mx.dic.ClearingSystemIdentification3Choice;
import com.prowidesoftware.swift.model.mx.dic.CreditTransferTransactionInformation10;
import com.prowidesoftware.swift.model.mx.dic.CreditTransferTransactionInformation11;
import com.prowidesoftware.swift.model.mx.dic.CustomerCreditTransferInitiationV03;
import com.prowidesoftware.swift.model.mx.dic.FIToFICustomerCreditTransferV02;
import com.prowidesoftware.swift.model.mx.dic.FinancialInstitutionIdentification7;
import com.prowidesoftware.swift.model.mx.dic.GenericAccountIdentification1;
import com.prowidesoftware.swift.model.mx.dic.GroupHeader32;
import com.prowidesoftware.swift.model.mx.dic.GroupHeader33;
import com.prowidesoftware.swift.model.mx.dic.GroupHeader38;
import com.prowidesoftware.swift.model.mx.dic.OrganisationIdentification4;
import com.prowidesoftware.swift.model.mx.dic.OriginalGroupInformation3;
import com.prowidesoftware.swift.model.mx.dic.OriginalTransactionReference13;
import com.prowidesoftware.swift.model.mx.dic.Party6Choice;
import com.prowidesoftware.swift.model.mx.dic.PartyIdentification32;
import com.prowidesoftware.swift.model.mx.dic.PaymentIdentification1;
import com.prowidesoftware.swift.model.mx.dic.PaymentIdentification3;
import com.prowidesoftware.swift.model.mx.dic.PaymentInstructionInformation3;
import com.prowidesoftware.swift.model.mx.dic.PaymentMethod3Code;
import com.prowidesoftware.swift.model.mx.dic.PaymentReturnV02;
import com.prowidesoftware.swift.model.mx.dic.PaymentTransactionInformation27;
import com.prowidesoftware.swift.model.mx.dic.PaymentTypeInformation21;
import com.prowidesoftware.swift.model.mx.dic.PaymentTypeInformation22;
import com.prowidesoftware.swift.model.mx.dic.Purpose2Choice;
import com.prowidesoftware.swift.model.mx.dic.RemittanceInformation5;
import com.prowidesoftware.swift.model.mx.dic.ReturnReason5Choice;
import com.prowidesoftware.swift.model.mx.dic.ReturnReasonInformation9;
import com.prowidesoftware.swift.model.mx.dic.ServiceLevel8Choice;
import com.prowidesoftware.swift.model.mx.dic.SettlementInformation13;
import com.prowidesoftware.swift.model.mx.dic.SettlementMethod1Code;
import com.prowidesoftware.swift.model.mx.dic.StructuredRemittanceInformation7;
import com.queue.QueueProducer;
import java.io.IOException;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 *
 * @author melleji.mollel
 */
@Component
public class ISO20022Service {

    @Autowired
    BanksRepo banksRepo;

    @Autowired
    TransactionRepo transactionRepo;

    @Autowired
    SYSENV systemVariables;

////    @Autowired
//    EftRepo eftRepo;

    @Autowired
    QueueProducer queProducer;

    @Autowired
    SignRequest sign;
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(ISO20022Service.class);

    /*
    CREATE AN EQUIVALENT MT103 MESSAGE USING ISO20022 WITH OUTGOING BATCH TRANSACTION
    * {1:F01FOOBARC0AXXX0344000050}{2:I103BANKANC0XXXXN}{4:"
     *	:20:TBEXO12345
     *  :23B:CRED
     *  :32A:150519USD23453,
     *	:50K:/01111001759234567890
     *	JOE DOE
     *	310 Field Road, NY
     *	:53B:/00010013800002001234
     *	FOO BANK
     *	:59:/00013500510020179998
     *	TEST CORP
     *	Nellis ABC, NV
     *	:71A:OUR
     *	:72:/TIPO/422
     *	-}";
     */
    public String generateMxPacs00800102RawXml(EftPacs00800102OutgoingReq req, String bankCode) throws IOException {

        /*
    	 * Initialize the MX object
         */
        String content = null;
        MxPacs00800102 mx = new MxPacs00800102();
        mx.setFIToFICstmrCdtTrf(new FIToFICustomerCreditTransferV02().setGrpHdr(new GroupHeader33()));
        mx.getFIToFICstmrCdtTrf().getGrpHdr().setMsgId(req.getMsgId());
        mx.getFIToFICstmrCdtTrf().getGrpHdr().setCreDtTm(req.getCreDtTm());
        mx.getFIToFICstmrCdtTrf().getGrpHdr().setNbOfTxs(req.getNbOfTxs());
        mx.getFIToFICstmrCdtTrf().getGrpHdr().setTtlIntrBkSttlmAmt(new ActiveCurrencyAndAmount().setCcy(req.getTtlIntrBkSttlmCcy()).setValue(req.getTtlIntrBkSttlmAmt()));
        mx.getFIToFICstmrCdtTrf().getGrpHdr().setIntrBkSttlmDt(req.getCreDtTm());
        /*
                        * Settlement Information
         */
        mx.getFIToFICstmrCdtTrf().getGrpHdr().setSttlmInf(new SettlementInformation13());
        mx.getFIToFICstmrCdtTrf().getGrpHdr().getSttlmInf().setSttlmMtd(SettlementMethod1Code.CLRG).setClrSys(new ClearingSystemIdentification3Choice().setPrtry("ACH"));

        /*
* Instructing Agent
         */
        mx.getFIToFICstmrCdtTrf().getGrpHdr().setInstgAgt(
                (new BranchAndFinancialInstitutionIdentification4()).setFinInstnId(
                        (new FinancialInstitutionIdentification7()).setBIC(systemVariables.SENDER_BIC)));

        /*
* Transaction Identification
         */
 /*
LOOP THE BANK TRANSACTIONS AND GET ALL CUSTOMERS
         */
        for (int i = 0; i < req.getCdtTrfTxInf().size(); i++) {
            /*
    * Payment Transaction Information
             */
            CreditTransferTransactionInformation11 cti = new CreditTransferTransactionInformation11();

            cti.setPmtId(new PaymentIdentification3());
            cti.getPmtId().setInstrId(req.getCdtTrfTxInf().get(i).getTxId());
            cti.getPmtId().setEndToEndId(req.getCdtTrfTxInf().get(i).getTxId());
            cti.getPmtId().setTxId(req.getCdtTrfTxInf().get(i).getTxId());
            cti.setPmtTpInf(new PaymentTypeInformation21().setSvcLvl(new ServiceLevel8Choice().setCd("SEPA")));//setSvcLvl(new ServiceLevel8Choice());

            /*
    * Transaction Amount
             */
            ActiveCurrencyAndAmount amount = new ActiveCurrencyAndAmount();
            amount.setCcy("TZS");
            amount.setValue(req.getCdtTrfTxInf().get(i).getAmount());
            cti.setIntrBkSttlmAmt(amount);

            /*
    * Transaction Value Date
             */
            //cti.setIntrBkSttlmDt(getXMLGregorianCalendarNow());
            /*
    * Transaction Charges
             */
            cti.setChrgBr(ChargeBearerType1Code.SLEV);//ACCP//RJCK//.........

            /*
    * Orderer Name & Address
             */
            cti.setDbtr(new PartyIdentification32());
            cti.getDbtr().setNm(req.getCdtTrfTxInf().get(i).getSenderName());
//                    cti.getDbtr().setPstlAdr((new PostalAddress6()).addAdrLine("310 Field Road, NY"));
            cti.getDbtr().setId(new Party6Choice().setOrgId(new OrganisationIdentification4().setBICOrBEI(systemVariables.SENDER_BIC)));
            /*
* Orderer Account
             */
            cti.setDbtrAcct(
                    (new CashAccount16()).setId(
                            (new AccountIdentification4Choice().setIBAN(req.getCdtTrfTxInf().get(i).getSenderAccount()))));
            /*
* Order Financial Institution
             */
            cti.setDbtrAgt(
                    (new BranchAndFinancialInstitutionIdentification4()).setFinInstnId(
                            (new FinancialInstitutionIdentification7()).setBIC(systemVariables.SENDER_BIC)));

            /*
* Beneficiary Institution
             */
            cti.setCdtrAgt((new BranchAndFinancialInstitutionIdentification4()).setFinInstnId((new FinancialInstitutionIdentification7()).setBIC(req.getCdtTrfTxInf().get(i).getBeneficiaryBIC())));

            /*
* Beneficiary Name & Address
             */
            cti.setCdtr(new PartyIdentification32());
            cti.getCdtr().setNm(req.getCdtTrfTxInf().get(i).getBeneficiaryName());
            cti.getCdtr().setId(new Party6Choice().setOrgId(new OrganisationIdentification4().setBICOrBEI(req.getCdtTrfTxInf().get(i).getBeneficiaryBIC())));
//                    cti.getCdtr().setPstlAdr((new PostalAddress6().addAdrLine("Nellis ABC, NV")));

            /*
* Beneficiary Account
             */
            cti.setCdtrAcct(
                    (new CashAccount16()).setId(
                            (new AccountIdentification4Choice()).setIBAN(req.getCdtTrfTxInf().get(i).getBeneficiaryAcct())));
            cti.setRmtInf(new RemittanceInformation5().addUstrd(req.getCdtTrfTxInf().get(i).getPurpose()));
            mx.getFIToFICstmrCdtTrf().addCdtTrfTxInf(cti);

            /*
* Print the generated message in its XML format
             */
//        System.out.println(mx.message());
        }
        content = mx.message().replace("Doc:", "").replace(":Doc", "").replace(" >", ">").replace("encoding=\"UTF-8\"", "encoding=\"UTF-8\"?").replace(" ?>", ">").trim() + "\r";
        //LOGGER.info(content);
        //signEftRequest(content, bankCode);
//System.out.println(content);

//                } else {
//                    content = "Null";
//                }
//            }
//        }
        return signEftRequest(content, bankCode);
    }

    public String testGenerateMxPacs00800102RawXml(EftPacs00800102OutgoingReq req) throws IOException {

        /*
    	 * Initialize the MX object
         */
        String content = null;

        /*
		 * Initialize main message content main objects
         */
 /*
        *Get LOCAL BANKS
         */
//        List<Map<String, Object>> banks = banksRepo.getLocalBanksList();
//        if (banks != null) {
//            for (int j = 0; j < banks.size(); j++) {
        /*
            GET LIST OF TRANSACTIONS PER BANK
         */
//                List<Map<String, Object>> txns = eftRepo.getEftBatchTransactionPerBank(banks.get(j).get("swift_code").toString());
        /*
        * General Information
         */
//                if (txns != null && txns.size() > 0) {
        MxPacs00800102 mx = new MxPacs00800102();
        mx.setFIToFICstmrCdtTrf(new FIToFICustomerCreditTransferV02().setGrpHdr(new GroupHeader33()));
        mx.getFIToFICstmrCdtTrf().getGrpHdr().setMsgId(req.getMsgId());
        mx.getFIToFICstmrCdtTrf().getGrpHdr().setCreDtTm(req.getCreDtTm());
        mx.getFIToFICstmrCdtTrf().getGrpHdr().setNbOfTxs(req.getNbOfTxs());
        mx.getFIToFICstmrCdtTrf().getGrpHdr().setTtlIntrBkSttlmAmt(new ActiveCurrencyAndAmount().setCcy(req.getTtlIntrBkSttlmCcy()).setValue(req.getTtlIntrBkSttlmAmt()));
        mx.getFIToFICstmrCdtTrf().getGrpHdr().setIntrBkSttlmDt(req.getCreDtTm());
        /*
                        * Settlement Information
         */
        mx.getFIToFICstmrCdtTrf().getGrpHdr().setSttlmInf(new SettlementInformation13());
        mx.getFIToFICstmrCdtTrf().getGrpHdr().getSttlmInf().setSttlmMtd(SettlementMethod1Code.CLRG).setClrSys(new ClearingSystemIdentification3Choice().setPrtry("ACH"));

        /*
* Instructing Agent
         */
        mx.getFIToFICstmrCdtTrf().getGrpHdr().setInstgAgt(
                (new BranchAndFinancialInstitutionIdentification4()).setFinInstnId(
                        (new FinancialInstitutionIdentification7()).setBIC("TAPBTZT0")));

        /*
* Transaction Identification
         */
 /*
LOOP THE BANK TRANSACTIONS AND GET ALL CUSTOMERS
         */
        for (int i = 0; i < req.getCdtTrfTxInf().size(); i++) {
            /*
    * Payment Transaction Information
             */
            CreditTransferTransactionInformation11 cti = new CreditTransferTransactionInformation11();

            cti.setPmtId(new PaymentIdentification3());
            cti.getPmtId().setInstrId(req.getCdtTrfTxInf().get(i).getTxId());
            cti.getPmtId().setEndToEndId(req.getCdtTrfTxInf().get(i).getTxId());
            cti.getPmtId().setTxId(req.getCdtTrfTxInf().get(i).getTxId());
            cti.setPmtTpInf(new PaymentTypeInformation21().setSvcLvl(new ServiceLevel8Choice().setCd("SEPA")));//setSvcLvl(new ServiceLevel8Choice());

            /*
    * Transaction Amount
             */
            ActiveCurrencyAndAmount amount = new ActiveCurrencyAndAmount();
            amount.setCcy("TZS");
            amount.setValue(req.getCdtTrfTxInf().get(i).getAmount());
            cti.setIntrBkSttlmAmt(amount);

            /*
    * Transaction Value Date
             */
            //cti.setIntrBkSttlmDt(getXMLGregorianCalendarNow());
            /*
    * Transaction Charges
             */
            cti.setChrgBr(ChargeBearerType1Code.SLEV);//ACCP//RJCK//.........

            /*
    * Orderer Name & Address
             */
            cti.setDbtr(new PartyIdentification32());
            cti.getDbtr().setNm(req.getCdtTrfTxInf().get(i).getSenderName());
//                    cti.getDbtr().setPstlAdr((new PostalAddress6()).addAdrLine("310 Field Road, NY"));
            cti.getDbtr().setId(new Party6Choice().setOrgId(new OrganisationIdentification4().setBICOrBEI("TAPBTZT0")));
            /*
* Orderer Account
             */
            cti.setDbtrAcct(
                    (new CashAccount16()).setId(
                            (new AccountIdentification4Choice().setIBAN(req.getCdtTrfTxInf().get(i).getSenderAccount()))));
            /*
* Order Financial Institution
             */
            cti.setDbtrAgt(
                    (new BranchAndFinancialInstitutionIdentification4()).setFinInstnId(
                            (new FinancialInstitutionIdentification7()).setBIC("TAPBTZT0")));

            /*
* Beneficiary Institution
             */
            cti.setCdtrAgt((new BranchAndFinancialInstitutionIdentification4()).setFinInstnId((new FinancialInstitutionIdentification7()).setBIC(req.getCdtTrfTxInf().get(i).getBeneficiaryBIC())));

            /*
* Beneficiary Name & Address
             */
            cti.setCdtr(new PartyIdentification32());
            cti.getCdtr().setNm(req.getCdtTrfTxInf().get(i).getBeneficiaryName());
            cti.getCdtr().setId(new Party6Choice().setOrgId(new OrganisationIdentification4().setBICOrBEI(req.getCdtTrfTxInf().get(i).getBeneficiaryBIC())));
//                    cti.getCdtr().setPstlAdr((new PostalAddress6().addAdrLine("Nellis ABC, NV")));

            /*
* Beneficiary Account
             */
            cti.setCdtrAcct(
                    (new CashAccount16()).setId(
                            (new AccountIdentification4Choice()).setIBAN(req.getCdtTrfTxInf().get(i).getBeneficiaryAcct())));
            cti.setRmtInf(new RemittanceInformation5().addUstrd(req.getCdtTrfTxInf().get(i).getPurpose()));
            mx.getFIToFICstmrCdtTrf().addCdtTrfTxInf(cti);

            /*
* Print the generated message in its XML format
             */
//        System.out.println(mx.message());
        }
        content = mx.message().replace("Doc:", "").replace(":Doc", "").replace(" >", ">").replace("encoding=\"UTF-8\"", "encoding=\"UTF-8\"?").replace(" ?>", ">").trim() + "\r";
        //LOGGER.info(content);
        //signEftRequest(content, bankCode);
//System.out.println(content);

//                } else {
//                    content = "Null";
//                }
//            }
//        }
        return (content);
    }

    public String generateMxPacs00800102CITIRawXml(EftPacs00800102OutgoingReq req, String bankCode) throws IOException {

        /*
    	 * Initialize the MX object
         */
        String content = null;

        /*
		 * Initialize main message content main objects
         */
 /*
        *Get LOCAL BANKS
         */
//        List<Map<String, Object>> banks = banksRepo.getLocalBanksList();
//        if (banks != null) {
//            for (int j = 0; j < banks.size(); j++) {
        /*
            GET LIST OF TRANSACTIONS PER BANK
         */
//                List<Map<String, Object>> txns = eftRepo.getEftBatchTransactionPerBank(banks.get(j).get("swift_code").toString());
        /*
        * General Information
         */
//                if (txns != null && txns.size() > 0) {
        MxPacs00800102 mx = new MxPacs00800102();
        mx.setFIToFICstmrCdtTrf(new FIToFICustomerCreditTransferV02().setGrpHdr(new GroupHeader33()));
        mx.getFIToFICstmrCdtTrf().getGrpHdr().setMsgId(req.getMsgId());
        mx.getFIToFICstmrCdtTrf().getGrpHdr().setCreDtTm(req.getCreDtTm());
        mx.getFIToFICstmrCdtTrf().getGrpHdr().setNbOfTxs(req.getNbOfTxs());
        mx.getFIToFICstmrCdtTrf().getGrpHdr().setTtlIntrBkSttlmAmt(new ActiveCurrencyAndAmount().setCcy(req.getTtlIntrBkSttlmCcy()).setValue(req.getTtlIntrBkSttlmAmt()));
        mx.getFIToFICstmrCdtTrf().getGrpHdr().setIntrBkSttlmDt(req.getCreDtTm());
        /*
                        * Settlement Information
         */
        mx.getFIToFICstmrCdtTrf().getGrpHdr().setSttlmInf(new SettlementInformation13());
        mx.getFIToFICstmrCdtTrf().getGrpHdr().getSttlmInf().setSttlmMtd(SettlementMethod1Code.CLRG).setClrSys(new ClearingSystemIdentification3Choice().setPrtry("ACH"));

        /*
* Instructing Agent
         */
        mx.getFIToFICstmrCdtTrf().getGrpHdr().setInstgAgt(
                (new BranchAndFinancialInstitutionIdentification4()).setFinInstnId(
                        (new FinancialInstitutionIdentification7()).setBIC(systemVariables.SENDER_BIC)));

        /*
* Transaction Identification
         */
 /*
LOOP THE BANK TRANSACTIONS AND GET ALL CUSTOMERS
         */
        for (int i = 0; i < req.getCdtTrfTxInf().size(); i++) {
            /*
    * Payment Transaction Information
             */
            CreditTransferTransactionInformation11 cti = new CreditTransferTransactionInformation11();

            cti.setPmtId(new PaymentIdentification3());
            cti.getPmtId().setInstrId(req.getCdtTrfTxInf().get(i).getTxId());
            cti.getPmtId().setEndToEndId(req.getCdtTrfTxInf().get(i).getTxId());
            cti.getPmtId().setTxId(req.getCdtTrfTxInf().get(i).getTxId());
            cti.setPmtTpInf(new PaymentTypeInformation21().setSvcLvl(new ServiceLevel8Choice().setCd("BKTR")));//setSvcLvl(new ServiceLevel8Choice());

            /*
    * Transaction Amount
             */
            ActiveCurrencyAndAmount amount = new ActiveCurrencyAndAmount();
            amount.setCcy("TZS");
            amount.setValue(req.getCdtTrfTxInf().get(i).getAmount());
            cti.setIntrBkSttlmAmt(amount);

            /*
    * Transaction Value Date
             */
            //cti.setIntrBkSttlmDt(getXMLGregorianCalendarNow());
            /*
    * Transaction Charges
             */
            cti.setChrgBr(ChargeBearerType1Code.SLEV);//ACCP//RJCK//.........

            /*
    * Orderer Name & Address
             */
            cti.setDbtr(new PartyIdentification32());
            cti.getDbtr().setNm(req.getCdtTrfTxInf().get(i).getSenderName());
//                    cti.getDbtr().setPstlAdr((new PostalAddress6()).addAdrLine("310 Field Road, NY"));
            cti.getDbtr().setId(new Party6Choice().setOrgId(new OrganisationIdentification4().setBICOrBEI("CITITZTZ")));
            /*
* Orderer Account
             */
            cti.setDbtrAcct(
                    (new CashAccount16()).setId(
                            (new AccountIdentification4Choice().setOthr(new GenericAccountIdentification1().setId(req.getCdtTrfTxInf().get(i).getSenderAccount())))));
            /*
* Order Financial Institution
             */
            cti.setDbtrAgt(
                    (new BranchAndFinancialInstitutionIdentification4()).setFinInstnId(
                            (new FinancialInstitutionIdentification7()).setBIC("CITITZTZ")));

            /*
* Beneficiary Institution
             */
            cti.setCdtrAgt((new BranchAndFinancialInstitutionIdentification4()).setFinInstnId((new FinancialInstitutionIdentification7()).setBIC(req.getCdtTrfTxInf().get(i).getBeneficiaryBIC())));

            /*
* Beneficiary Name & Address
             */
            cti.setCdtr(new PartyIdentification32());
            cti.getCdtr().setNm(req.getCdtTrfTxInf().get(i).getBeneficiaryName());
            cti.getCdtr().setId(new Party6Choice().setOrgId(new OrganisationIdentification4().setBICOrBEI(req.getCdtTrfTxInf().get(i).getBeneficiaryBIC())));
//                    cti.getCdtr().setPstlAdr((new PostalAddress6().addAdrLine("Nellis ABC, NV")));

            /*
* Beneficiary Account
             */
            cti.setCdtrAcct(
                    (new CashAccount16()).setId(
                            (new AccountIdentification4Choice()).setIBAN(req.getCdtTrfTxInf().get(i).getBeneficiaryAcct())));
            cti.setRmtInf(new RemittanceInformation5().addUstrd(req.getCdtTrfTxInf().get(i).getPurpose()));
            mx.getFIToFICstmrCdtTrf().addCdtTrfTxInf(cti);

            /*
* Print the generated message in its XML format
             */
//        System.out.println(mx.message());
        }
        content = mx.message().replace("Doc:", "").replace(":Doc", "").replace(" >", ">").replace("encoding=\"UTF-8\"", "encoding=\"UTF-8\"?").replace(" ?>", ">").trim() + "\r";
        //LOGGER.info(content);
        //signEftRequest(content, bankCode);
//System.out.println(content);

//                } else {
//                    content = "Null";
//                }
//            }
//        }
        return signEftRequest(content, bankCode);
    }

    public String signEftRequest(String rawXML, String bankCode) {
        String rawXmlsigned = null;
        try {

            String signature = sign.CreateSignature(rawXML, systemVariables.PRIVATE_EFT_TACH_KEYPASS,
                    systemVariables.PRIVATE_EFT_TACH_KEY_ALIAS, systemVariables.PRIVATE_EFT_TACH_KEY_FILE_PATH);

            rawXmlsigned = rawXML + "|" + signature;
//            queProducer.sendToQueueEftFrmCBSToTACH(bankCode + DateUtil.now("yyyyMMddHms") + ".i^" + rawXmlsigned);
//            FileUtils.writeStringToFile(new File("C:\\Users\\HP\\Desktop\\BOT\\NEWCERT\\" + bankCode + DateUtil.now("yyyyMMddHms") + ".i"), rawXmlsigned, StandardCharsets.UTF_8);
            LOGGER.info("\nEFT SIGNED REQUEST FOR :{}\n{}", bankCode, rawXmlsigned);
        } catch (Exception ex) {
            LOGGER.error("Generating digital signature...{}", ex);
        }
        return rawXmlsigned;
    }

//    public void parseIso20022Message(String Message) throws Exception {
//        String lastLineOfMessage = Message.split("\\^")[1].substring(Message.split("\\^")[1].lastIndexOf("\n"));//get signature content 
//        if (lastLineOfMessage.contains("Document")) {
//            lastLineOfMessage = lastLineOfMessage.split("\\</Document>")[1];
//        }
//        LOGGER.info("parseIso20022Message {}", Message);
//        String eftMessageSinged = Message.split("\\^")[1];
//        String fileName = Message.split("\\^")[0];
//        String eftMessageContent = eftMessageSinged.split("\\|")[0];
//        String signature = eftMessageSinged.split("\\|")[1];
////                LOGGER.info("MESSAGE WITHOUT SIGNATURE {}", eftMessageContent);
////        System.out.println("SIGNATURE:"+signature);
//        boolean isEftMessageValid = sign.verifySignature(signature, eftMessageContent, systemVariables.PUBLIC_EFT_TACH_KEYPASS, systemVariables.PUBLIC_EFT_TACH_KEY_ALIAS, systemVariables.PUBLIC_EFT_TACH_KEY_FILE_PATH);
//        LOGGER.info("IS MESSAGE VALID? {}", isEftMessageValid);
//        if (isEftMessageValid) {
//            MxParser swiftMessage = new MxParser(eftMessageContent);
//            LOGGER.info("Message Type: {}", swiftMessage.analyzeMessage().getDocumentNamespace());
//            if (swiftMessage.analyzeMessage().getDocumentNamespace().equals("urn:iso:std:iso:20022:tech:xsd:pacs.008.001.02")) {
//                LOGGER.info("processPacs00800201Incoming");
//
//                processPacs00800201Incoming(eftMessageContent, fileName, false);
//            } else if (swiftMessage.analyzeMessage().getDocumentNamespace().equals("urn:iso:std:iso:20022:tech:xsd:pacs.002.001.03")) {
//                LOGGER.info("processPacs00200103Incoming");
//
//                processPacs00200103Incoming(eftMessageContent, fileName);
//            } else if (swiftMessage.analyzeMessage().getDocumentNamespace().equals("urn:iso:std:iso:20022:tech:xsd:pacs.004.001.02")) {
//                LOGGER.info("processPacs00400102");
//                processPacs00400102(eftMessageContent, fileName);
//            }
//        } else {
//            processPacs00800201Incoming(eftMessageContent, fileName, true);
//            System.out.println("RECEIVED FILE: " + fileName + " signature cannot be verified!!!!!!");
//        }
//    }

    /*
    * Format crd accounts to match TPB accounts
     */
 /*
     * RETURN A TRANSACTION WITH A CERTAIN GIVEN REASON FROM THE OBJECT SET ON EftPacs00400102Req
     */
    public String generateMxPacs00400102SignedReq(EftPacs00400102Req req, String bankBIC) {
        String content = null;
        MxPacs00400102 mx = new MxPacs00400102();
        mx.setPmtRtr(new PaymentReturnV02().setGrpHdr(new GroupHeader38()));
        mx.getPmtRtr().getGrpHdr().setMsgId(req.getMessageId());
        mx.getPmtRtr().getGrpHdr().setCreDtTm(req.getCreateDateTime());
        mx.getPmtRtr().getGrpHdr().setNbOfTxs(req.getNbOfTxs());
        mx.getPmtRtr().getGrpHdr().setTtlRtrdIntrBkSttlmAmt(new ActiveCurrencyAndAmount().setCcy("TZS").setValue(req.getTotalReturnedIntrBnkSettlmntAmt()));
        mx.getPmtRtr().getGrpHdr().setIntrBkSttlmDt(req.getInterBankSettlmntDate());
        mx.getPmtRtr().getGrpHdr().setSttlmInf(new SettlementInformation13().setSttlmMtd(SettlementMethod1Code.CLRG)
                .setClrSys(new ClearingSystemIdentification3Choice().setPrtry(req.getClearingSystem())));
        mx.getPmtRtr().getGrpHdr().setInstgAgt(new BranchAndFinancialInstitutionIdentification4().setFinInstnId(new FinancialInstitutionIdentification7().setBIC(systemVariables.SENDER_BIC)));
        for (int i = 0; i < req.getTxInfo().size(); i++) {
            PaymentTransactionInformation27 txinfo = new PaymentTransactionInformation27();
            txinfo.setRtrId(req.getTxInfo().get(i).getReturnTxnId());
            txinfo.setOrgnlGrpInf(new OriginalGroupInformation3().setOrgnlMsgId(req.getTxInfo().get(i).getOriginalMsgId())
                    .setOrgnlMsgNmId(req.getTxInfo().get(i).getOriginalMsgNmId()));
            txinfo.setOrgnlInstrId(req.getTxInfo().get(i).getOriginalInstructionId());
            txinfo.setOrgnlEndToEndId(req.getTxInfo().get(i).getOriginalEndToEndId());
            txinfo.setOrgnlTxId(req.getTxInfo().get(i).getOriginalTxid());
            txinfo.setOrgnlIntrBkSttlmAmt(new ActiveOrHistoricCurrencyAndAmount().setCcy(req.getTxInfo().get(i).getOriginalInterBankSettmntCcy()).setValue(req.getTxInfo().get(i).getOriginalInterBankSettmntAmt()));
            txinfo.setRtrdIntrBkSttlmAmt(new ActiveCurrencyAndAmount().setCcy(req.getTxInfo().get(i).getReturnedInterBankSettmntCcy()).setValue(req.getTxInfo().get(i).getReturnedInterBankSettmntAmt()));
            txinfo.setChrgBr(ChargeBearerType1Code.SLEV);
            txinfo.getRtrRsnInf().add(0, new ReturnReasonInformation9().setOrgtr(new PartyIdentification32().setId(new Party6Choice().setOrgId(new OrganisationIdentification4().setBICOrBEI(systemVariables.PRIVATE_EFT_TACH_KEY_ALIAS)))).setRsn(new ReturnReason5Choice().setCd(req.getTxInfo().get(i).getReturnReasonInfomation())));//get(0).setOrgtr(new PartyIdentification32().setId(new Party6Choice().setOrgId(new OrganisationIdentification4().setBICOrBEI(systemVariables.SENDER_BIC))));
            txinfo.setOrgnlTxRef(new OriginalTransactionReference13().setIntrBkSttlmDt(DateUtil.convertDateToXmlGregorian(req.getTxInfo().get(i).getTxnDate(), "yyyy-MM-dd"))
                    .setPmtTpInf(new PaymentTypeInformation22().setSvcLvl(new ServiceLevel8Choice().setCd("SEPA")))
                    .setDbtr(new PartyIdentification32().setNm(req.getTxInfo().get(i).getSenderName())
                            .setId(new Party6Choice().setOrgId(new OrganisationIdentification4().setBICOrBEI(req.getTxInfo().get(i).getSenderBIC()))))
                    .setDbtrAcct(new CashAccount16().setId(new AccountIdentification4Choice().setIBAN(req.getTxInfo().get(i).getSenderAccount())))
                    .setDbtrAgt(new BranchAndFinancialInstitutionIdentification4().setFinInstnId(new FinancialInstitutionIdentification7().setBIC(req.getTxInfo().get(i).getSenderBIC())))
                    .setCdtrAgt((new BranchAndFinancialInstitutionIdentification4().setFinInstnId(new FinancialInstitutionIdentification7().setBIC(req.getTxInfo().get(i).getBeneficiaryBIC()))
                            .setFinInstnId(new FinancialInstitutionIdentification7().setBIC(req.getTxInfo().get(i).getBeneficiaryBIC())))
                    )
                    .setCdtr(new PartyIdentification32().setNm(req.getTxInfo().get(i).getBeneficiaryName())
                            .setId(new Party6Choice().setOrgId(new OrganisationIdentification4().setBICOrBEI(req.getTxInfo().get(i).getBeneficiaryBIC()))))
                    .setCdtrAcct(new CashAccount16().setId(new AccountIdentification4Choice().setIBAN(req.getTxInfo().get(i).getBeneficiaryAcct()))));
            mx.getPmtRtr().addTxInf(txinfo);
        }
        content = mx.message().replace("Doc:", "").replace(":Doc", "").replace(" >", ">").replace("encoding=\"UTF-8\"", "encoding=\"UTF-8\"?").replace(" ?>", ">").trim() + "\r";
        System.out.println("--------------------raw message from------------------------");
        System.out.println(content);
        System.out.println("--------------------end row message------------------------");

//        LOGGER.info("RAW XML MESSAGE: \n" + content);
//        return content;
        return signEftRequest(content, bankBIC);

    }

//    public void processPacs00800201Incoming(String eftMessageContent, String fileName, boolean isNotAllowedToProcess) {
//        /*
//         * its MT103 WITH SINGLE OR MULTIPLE ENTRIES [PACS.008.001.02]
//         */
//        EftPacs00800102Req pacs008 = new EftPacs00800102Req();
//        MxPacs00800102 mx = MxPacs00800102.parse(eftMessageContent);
//        pacs008.setMsgId(mx.getFIToFICstmrCdtTrf().getGrpHdr().getMsgId());
//        pacs008.setCreDtTm(mx.getFIToFICstmrCdtTrf().getGrpHdr().getCreDtTm());
//        pacs008.setNbOfTxs(mx.getFIToFICstmrCdtTrf().getGrpHdr().getNbOfTxs());
//        pacs008.setTtlIntrBkSttlmAmt(mx.getFIToFICstmrCdtTrf().getGrpHdr().getTtlIntrBkSttlmAmt().getValue());
//        pacs008.setTtlIntrBkSttlmCcy(mx.getFIToFICstmrCdtTrf().getGrpHdr().getTtlIntrBkSttlmAmt().getCcy());
//        pacs008.setIntrBkSttlmDt(mx.getFIToFICstmrCdtTrf().getGrpHdr().getIntrBkSttlmDt());
//        pacs008.setSttlmMtd(mx.getFIToFICstmrCdtTrf().getGrpHdr().getSttlmInf().getSttlmMtd().name());
//        pacs008.setInstdAgt(mx.getFIToFICstmrCdtTrf().getGrpHdr().getInstdAgt().getFinInstnId().getBIC());
//        pacs008.setOriginalMsgNmId("pacs.008.001.02");
//        int numberOfTxns = Integer.parseInt(pacs008.getNbOfTxs());
//        int count = 1;
////insert batch transaction transactions
//        eftRepo.insertEFTBacthTxnIncoming(pacs008, fileName);
//        for (CreditTransferTransactionInformation11 ctti : mx.getFIToFICstmrCdtTrf().getCdtTrfTxInf()) {
//            EftIncommingBulkPaymentsReq cti = new EftIncommingBulkPaymentsReq();
//            cti.setInstrId(ctti.getPmtId().getInstrId());
//            cti.setEndToEndId(ctti.getPmtId().getEndToEndId());
//            cti.setTxId(ctti.getPmtId().getTxId());
//            cti.setSvcLvlCd(ctti.getPmtTpInf().getSvcLvl().getCd());
//            cti.setAmount(ctti.getIntrBkSttlmAmt().getValue());
//            cti.setCurrency(ctti.getIntrBkSttlmAmt().getCcy());
//            cti.setChrgBr(ctti.getChrgBr().name());
//            cti.setSenderName(ctti.getDbtr().getNm());
////            if (ctti.getDbtr().getId().getOrgId().getBICOrBEI()== null) {
//            cti.setSenderBICorBEI(ctti.getDbtrAgt().getFinInstnId().getBIC());
////            } else {
////                cti.setSenderBICorBEI(ctti.getDbtr().getId().getOrgId().getBICOrBEI());
//
////            }
//            cti.setSenderAccount(ctti.getDbtrAcct().getId().getIBAN());
//            cti.setSenderBIC(ctti.getDbtrAgt().getFinInstnId().getBIC());
//            cti.setBeneficiaryBICorBEI(ctti.getCdtrAgt().getFinInstnId().getBIC());
////                    cti.setBeneficiaryAcct(ctti.getCdtrAcct().getId().getIBAN());
//            //  if (ctti.getDbtrAgt().getFinInstnId().getBIC().contains("CORUTZTZ") || ctti.getDbtr().getId().getOrgId().getBICOrBEI().contains("CORUTZTZ")) {
//            cti.setBeneficiaryAcct(ctti.getCdtrAcct().getId().getIBAN());
//            //}
//            cti.setBeneficiaryBIC(ctti.getCdtrAgt().getFinInstnId().getBIC());
//            cti.setBeneficiaryName(ctti.getCdtr().getNm());
//            cti.setPurpose(ctti.getRmtInf().getUstrd().get(0));
//            pacs008.setCdtTrfTxInf(cti);
//            //insert the transaction AND ADD TO QUEUE THE TRANSACIONS
//            int res = eftRepo.insertIncomingEFTBulkTransactions(pacs008, fileName, isNotAllowedToProcess);
//
//            if (res == 1 && (isNotAllowedToProcess == false) && ctti.getCdtrAgt().getFinInstnId().getBIC().equalsIgnoreCase(systemVariables.SENDER_BIC)) {
//                //send a transaction to QUEUE
//                //check if the transaction if PENSION TRANSACTION
//                if (!ctti.getPmtId().getEndToEndId().startsWith("P001P")) {//process only if its not pension ELSE DO NOTHING
//                    queProducer.sendToQueueEftIncomingToCBS(pacs008);
//                }
//            } else if (res == 2) {
//                //log the transaction on the exception table
//                LOGGER.info("The received is gepg transactions: reference:{},destinationAccount:{}, amount:{}", cti.getEndToEndId(), cti.getBeneficiaryAcct(), cti.getAmount());
//            } else if (res == -1) {
//                //TRANSACTION WAS NOT LOGGED ATTEMPT TO RE-LOG AGAIN
//                eftRepo.insertIncomingEFTBulkTransactions(pacs008, fileName, isNotAllowedToProcess);
//            }
//            count++;
//        }
//        if (count == numberOfTxns) {
//            //update the batch of transactions as completed
//        }
////            pacs008.setCdtTrfTxInf(cdtTrfTxInf);
//        LOGGER.info("PACS00800102 MESSAGE: {}", pacs008.toString());
//    }

    public void processPacs00200103Incoming(String message, String fileName) {
        LOGGER.info("FILE RECEIVED FROM TACH: {}", message.trim());
        MxPacs00200103 mx = MxPacs00200103.parse(message);
        String status = "----";
        String batchRef = mx.getFIToFIPmtStsRpt().getOrgnlGrpInfAndSts().getOrgnlMsgId();
        String OriginalMessageType = mx.getFIToFIPmtStsRpt().getOrgnlGrpInfAndSts().getOrgnlMsgNmId();
        String ackString = batchRef + "^" + status + "^" + OriginalMessageType + "^" + FilenameUtils.getExtension(fileName) + "^" + message;
        queProducer.sendToQueueOutwardAcknowledgementByTACH(ackString);
    }

//    public void processPacs00400102(String message, String fileName) {
//        if ((FilenameUtils.getExtension(fileName)).equalsIgnoreCase("S")) {
//            MxPacs00400102 mx = MxPacs00400102.parse(message);
//            for (PaymentTransactionInformation27 ptx : mx.getPmtRtr().getTxInf()) {
//                String txnReference = ptx.getOrgnlEndToEndId();
//                String reversalReason = eftRepo.eftErrorCodes(ptx.getRtrRsnInf().get(0).getRsn().getCd());//GET REVERSAL MESSAGE
//                String reversalString = txnReference + "^" + reversalReason;
//                queProducer.sendToQueueOutwardReversal(reversalString);
//            }
//
//        }
//    }

    public String generatePacs00100103Message(EftPacs00800102OutgoingReq req) {
        String content = null;
        MxPain00100103 mx = new MxPain00100103();
        mx.setCstmrCdtTrfInitn(new CustomerCreditTransferInitiationV03().setGrpHdr(new GroupHeader32()));
        mx.getCstmrCdtTrfInitn().getGrpHdr().setMsgId(req.getMsgId());
        mx.getCstmrCdtTrfInitn().getGrpHdr().setCreDtTm(req.getCreDtTm());
        mx.getCstmrCdtTrfInitn().getGrpHdr().setNbOfTxs(req.getNbOfTxs());
        mx.getCstmrCdtTrfInitn().getGrpHdr().setInitgPty(new PartyIdentification32().setNm("TPB BANK PLC"));
        for (int i = 0; i < req.getCdtTrfTxInf().size(); i++) {
            PaymentInstructionInformation3 pi = new PaymentInstructionInformation3();
            pi.setChrgBr(ChargeBearerType1Code.SLEV);
            pi.setChrgsAcct(new CashAccount16().setId(new AccountIdentification4Choice().setIBAN(req.getCdtTrfTxInf().get(i).getSenderAccount())));
            pi.setPmtInfId(req.getCdtTrfTxInf().get(i).getEndToEndId());
            pi.setPmtMtd(PaymentMethod3Code.TRF);
            pi.setDbtr(new PartyIdentification32().setNm(req.getCdtTrfTxInf().get(i).getSenderName()));
            pi.setDbtr(new PartyIdentification32().setId(new Party6Choice().setOrgId(new OrganisationIdentification4().setBICOrBEI("CITITZTZ"))));
            pi.setDbtrAcct(new CashAccount16().setId(new AccountIdentification4Choice().setIBAN(req.getCdtTrfTxInf().get(i).getSenderAccount())));
            pi.setDbtrAgt(new BranchAndFinancialInstitutionIdentification4().setFinInstnId(new FinancialInstitutionIdentification7().setBIC("CITITZTZ")));
            pi.addCdtTrfTxInf(new CreditTransferTransactionInformation10().setAmt(new AmountType3Choice().setInstdAmt(new ActiveOrHistoricCurrencyAndAmount().setCcy(req.getCdtTrfTxInf().get(i).getCurrency()).setValue(req.getCdtTrfTxInf().get(i).getAmount())))
                    .setCdtr(new PartyIdentification32().setNm(req.getCdtTrfTxInf().get(i).getBeneficiaryName()))
                    .setCdtrAcct(new CashAccount16().setId(new AccountIdentification4Choice().setIBAN(req.getCdtTrfTxInf().get(i).getBeneficiaryAcct())))
                    .setChrgBr(ChargeBearerType1Code.SLEV)
                    .setPurp(new Purpose2Choice().setCd(req.getCdtTrfTxInf().get(i).getPurpose()))
                    .setPmtId(new PaymentIdentification1().setEndToEndId(req.getCdtTrfTxInf().get(i).getEndToEndId()))
                    .setInstrForDbtrAgt(req.getCdtTrfTxInf().get(i).getPurpose())
                    .setUltmtCdtr(new PartyIdentification32().setId(new Party6Choice().setOrgId(new OrganisationIdentification4().setBICOrBEI("CITITZTZ"))))
                    .setUltmtDbtr(new PartyIdentification32().setId(new Party6Choice().setOrgId(new OrganisationIdentification4().setBICOrBEI("CITITZTZ"))))
                    .setRmtInf(new RemittanceInformation5().addStrd(new StructuredRemittanceInformation7().addAddtlRmtInf(req.getCdtTrfTxInf().get(i).getPurpose()))));
            mx.getCstmrCdtTrfInitn().addPmtInf(pi);
        }
        content = mx.message().replace("Doc:", "").replace(":Doc", "").replace(" >", ">").replace("encoding=\"UTF-8\"", "encoding=\"UTF-8\"?").replace(" ?>", ">").trim() + "\r";
        LOGGER.info("pain.001.001.03{}", content);
        return content;
    }
}
