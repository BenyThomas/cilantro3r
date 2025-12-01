/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.helper;

import com.DTO.EFT.EftBulkPaymentResp;
import com.DTO.EFT.EftBulkPaymentReq;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

import com.cilantro.CilantroApplication;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.LoggerFactory;
import org.springframework.web.multipart.MultipartFile;

/**
 *
 * @author melleji.mollel
 */
public class EftCSVHelper {

    public static String TYPE = "application/vnd.ms-excel";
    public static String SUPPORTING_DOC_TYPE = "application/pdf";
    private boolean isAmountGreaterThan20m = false;
    private boolean isBeneficiaryWellFormatted = true;
    static String[] HEADERs = {"senderAccount", "Amount", "BeneficiaryAccount", "BeneficiaryName", "BeneficiaryBic", "PaymentPurpose"};
    private final Validator factory = Validation.buildDefaultValidatorFactory().getValidator();
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(CilantroApplication.class);

    public boolean hasCSVFormat(MultipartFile file) {
        System.out.println("CONTENT TYPE: " + file.getContentType());
        if (!TYPE.equals(file.getContentType()) || !file.getContentType().equals("application/octet-stream")) {
            return false;
        }

        return true;
    }

    public boolean hasPDFFormat(MultipartFile file) {
        if (!SUPPORTING_DOC_TYPE.equals(file.getContentType())) {
            return false;
        }

        return true;
    }

    public List<EftBulkPaymentReq> csvToEftBulkPayment(InputStream is) {

        BigDecimal limitAmout = new BigDecimal("20000000");
        try (BufferedReader fileReader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                CSVParser csvParser = new CSVParser(fileReader, CSVFormat.DEFAULT.withFirstRecordAsHeader().withIgnoreHeaderCase().withTrim());) {
            List<EftBulkPaymentReq> eftBulkPayments = new ArrayList<>();
            Iterable<CSVRecord> csvRecords = csvParser.getRecords();
            for (CSVRecord csvRecord : csvRecords) {
                if (csvRecord.get("amount") != null) {
                    EftBulkPaymentReq eftBulkPayment = new EftBulkPaymentReq(
                            csvRecord.get("senderAccount"),
                            csvRecord.get("amount"),
                            csvRecord.get("beneficiaryAccount"),
                            csvRecord.get("beneficiaryName"),
                            csvRecord.get("beneficiaryBic"),
                            csvRecord.get("paymentPurpose")
                    );
                    eftBulkPayments.add(eftBulkPayment);
                }
            }

            return eftBulkPayments;
        } catch (IOException e) {
            throw new RuntimeException("fail to parse CSV file: " + e.getMessage());
        }
    }

    public EftBulkPaymentResp csvToEftBulkValidation(InputStream is, List<String> banks) {
        //Reponse message
        EftBulkPaymentResp paymentResp = new EftBulkPaymentResp();
        paymentResp.setStatusCode("0");
        paymentResp.setStatusMessage("No error Message");

        //eft fixed amount
        BigDecimal limitAmout = new BigDecimal("20000000");
        try (BufferedReader fileReader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                CSVParser csvParser = new CSVParser(fileReader, CSVFormat.DEFAULT.withFirstRecordAsHeader().withIgnoreHeaderCase().withTrim());) {
            List<EftBulkPaymentReq> eftBulkPayments = new ArrayList<>();
            Iterable<CSVRecord> csvRecords = csvParser.getRecords();
            for (CSVRecord csvRecord : csvRecords) {
                if (csvRecord.get("amount") != null) {
                    EftBulkPaymentReq eftBulkPayment = new EftBulkPaymentReq(
                            csvRecord.get("senderAccount"),
                            csvRecord.get("amount"),
                            csvRecord.get("beneficiaryAccount"),
                            csvRecord.get("beneficiaryName"),
                            csvRecord.get("beneficiaryBic"),
                            csvRecord.get("paymentPurpose")
                    );
                    eftBulkPayments.add(eftBulkPayment);
                }
            }
            String violError = "";
            int row = 2;
            for (EftBulkPaymentReq eftBulkPayment : eftBulkPayments) {
                System.out.println("EFT ROW " + row + " (" + eftBulkPayment + ")");
                //validate each row input
                Set<ConstraintViolation<EftBulkPaymentReq>> violations = this.factory.validate(eftBulkPayment);
                if (!violations.isEmpty()) {
                    for (ConstraintViolation<EftBulkPaymentReq> violation : violations) {
                        violError = violError + "Error at line " + row + " -> message: " + violation.getMessage() + " (" + eftBulkPayment + ")<br>";
                    }
                    paymentResp.setStatusCode("96");
                    paymentResp.setStatusMessage(violError);
                }
                //validate amount
                if (eftBulkPayment.getAmount() != null) {
                    BigDecimal amount2 = new BigDecimal(eftBulkPayment.getAmount());
                    int res = amount2.compareTo(limitAmout);
                    if (res > 0) {
                        System.out.println("AMOUNT ON FILE: " + eftBulkPayment.getAmount());
                        violError = violError + "Error at line " + row + " -> message: Amount is greater than 20million (" + eftBulkPayment + ")<br>";
                        paymentResp.setStatusCode("96");
                        paymentResp.setStatusMessage(violError);
                    }
                } else {
                    System.out.println("AMOUNT ON FILE: " + eftBulkPayment.getAmount());
                    violError = violError + "Error at line " + row + " -> message: Amount is NULL (" + eftBulkPayment + ")<br>";
                    paymentResp.setStatusCode("96");
                    paymentResp.setStatusMessage(violError);
                }
                //VALIDATE BIC
                LOGGER.info("banks List: {}",banks);
                LOGGER.info("eftBulkPayment.getBeneficiaryBic: {}",eftBulkPayment.getBeneficiaryBic());
                if (!banks.contains(eftBulkPayment.getBeneficiaryBic())) {
                    System.out.println("BENEFICIARY BIC ON FILE: " + eftBulkPayment.getBeneficiaryBic());
                    violError = violError + "Error at line " + row + " -> message: BENEFICIARY BIC DOESNOT EXISTS (" + eftBulkPayment + ")<br>";
                    paymentResp.setStatusCode("96");
                    paymentResp.setStatusMessage(violError);
                }

                row++;
            }

            return paymentResp;
        } catch (IOException e) {
            paymentResp.setStatusCode("96");
            paymentResp.setStatusCode("No error Message: IOException->" + e.getMessage());
            throw new RuntimeException("fail to parse CSV file: " + e.getMessage());
        }
    }

    public BigDecimal csvTotalAmount(InputStream is) {
        BigDecimal tAmt = new BigDecimal("0");
        try (BufferedReader fileReader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                CSVParser csvParser = new CSVParser(fileReader, CSVFormat.DEFAULT.withFirstRecordAsHeader().withIgnoreHeaderCase().withTrim());) {
            List<EftBulkPaymentReq> eftBulkPayments = new ArrayList<EftBulkPaymentReq>();
            Iterable<CSVRecord> csvRecords = csvParser.getRecords();
            for (CSVRecord csvRecord : csvRecords) {
                BigDecimal amount = new BigDecimal(csvRecord.get("amount"));
                amount.setScale(2, BigDecimal.ROUND_UP);
                tAmt = tAmt.add(amount);
            }
            return tAmt;
        } catch (IOException e) {
            throw new RuntimeException("fail to parse CSV file: " + e.getMessage());
        }
    }

    public int csvTotalNoOfTransactions(InputStream is) {
        int count = 0;
        try (BufferedReader fileReader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                CSVParser csvParser = new CSVParser(fileReader, CSVFormat.DEFAULT.withFirstRecordAsHeader().withIgnoreHeaderCase().withTrim());) {
            List<EftBulkPaymentReq> eftBulkPayments = new ArrayList<EftBulkPaymentReq>();
            Iterable<CSVRecord> csvRecords = csvParser.getRecords();
            for (CSVRecord csvRecord : csvRecords) {
                count++;
            }
            return count;
        } catch (IOException e) {
            throw new RuntimeException("fail to parse CSV file: " + e.getMessage());
        }
    }

    public boolean isAmountGreaterThan20m() {
        return isAmountGreaterThan20m;
    }

    public void setIsAmountGreaterThan20m(boolean isAmountGreaterThan20m) {
        this.isAmountGreaterThan20m = isAmountGreaterThan20m;
    }

    public boolean isIsBeneficiaryWellFormatted() {
        return isBeneficiaryWellFormatted;
    }

    public void setIsBeneficiaryWellFormatted(boolean isBeneficiaryWellFormatted) {
        this.isBeneficiaryWellFormatted = isBeneficiaryWellFormatted;
    }

}
