package com.service;

import com.models.ThirdPartyTxn;
import com.repository.ThirdPartyTxnRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.*;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@Slf4j
public class FileReaderService {
    @Autowired
    ThirdPartyTxnRepository thirdPartyTxnRepository;
    @Autowired
    @Qualifier("gwdb")
    JdbcTemplate gwJdbcTemplate;


    public  List<ThirdPartyTxn> processFile(String type, MultipartFile file) {
        List<ThirdPartyTxn> txns = new ArrayList<>();
        switch (type) {
            case "VODA_AIRTIME_FILE":
                txns = reconVodacomAirtimeNormal(file);
                break;
            case "VODA_BUNDLE_FILE":
                txns = reconVodacomAirtimeBundle(file);
                break;
                default:
                    log.info("File type not recognized");
                break;

        }
        return txns;
    }

    public  List<ThirdPartyTxn> reconVodacomAirtimeBundle(@NotNull MultipartFile file) {
        log.info("reconVodacomAirtimeBundle:{}", file.getOriginalFilename());
        List<ThirdPartyTxn> txns = new ArrayList<>();

        try (InputStream fileIs = file.getInputStream()) {

            Workbook workbook = WorkbookFactory.create(fileIs);
            log.info("reconVodacomAirtimeBundle:{}", workbook.getNumberOfSheets());

            for (Sheet sheet : workbook) {
                for (Row row : sheet) {
                    try {
                        if (row.getRowNum() >= 3) {
                            log.info("reconVodacomAirtimeBundle StringCellValue 5:{}",  row.getCell(5).getStringCellValue());
                            log.info("reconVodacomAirtimeBundle StringCellValue 0:{}", getCellValue( row.getCell(0)));

                            ThirdPartyTxn data = new ThirdPartyTxn();
                            data.setTxn_type("VODACOM");
                            data.setTtype("UTILITY");
                            data.setIdentifier("OUT");
                            data.setFile_name(file.getOriginalFilename());
                            data.setTxnid( getCellValue(row.getCell(5)).replace("\"",""));
                            data.setTxndate(com.helper.DateUtil.strToDate(getCellValue(row.getCell(0)),"yyyy-MM-dd HH:mm:ss"));
                            data.setFile_txndate(com.helper.DateUtil.strToDate(getCellValue(row.getCell(0)),"yyyy-MM-dd HH:mm:ss"));
                            data.setMnoTxns_status(getCellValue(row.getCell(1)));
                            data.setAmount(new BigDecimal(getCellValue(row.getCell(4))));
                            data.setCurrency("TZS");
                            data.setReceiptNo(getCellValue(row.getCell(5)));
                            data.setPrevious_balance(BigDecimal.ZERO);
                            data.setPost_balance(BigDecimal.ZERO);
                            data.setDescription("Success");
                            data.setTxdestinationaccount(getCellValue( row.getCell(2)));
                            data.setSourceAccount(getCellValue( row.getCell(2)));
                            List<Map<String,Object>> tpx = getGwTransaction(data.getTxnid());
                            log.info("GW transaction processed:{}",tpx);
                            if(!tpx.isEmpty()) {
                                data.setSourceAccount(tpx.get(0).get("txsourceAccount").toString());
                                data.setDescription((tpx.get(0).get("description").toString()));
                                data.setAcct_no(tpx.get(0).get("txsourceAccount").toString());
                                data.setStatus(tpx.get(0).get("txstatus").toString());
                                data.setDocode(tpx.get(0).get("txtype").toString());
                                data.setTxdestinationaccount(tpx.get(0).get("txdestinationAccount").toString());
                                data.setTxndate(com.helper.DateUtil.strToDate(tpx.get(0).get("txdate").toString()));
                            }else {

                            }
                            txns.add(data);
                            log.info("Thirdpart transaction object:{}",data);
                            thirdPartyTxnRepository.save(data);;
                        }
                    } catch (Exception e) {
                        log.info("Thirdpart transaction process failed:{}",e);
                    }
                }
            }

        } catch (Exception  e) {
            log.error(e.getMessage(),e);
        }
        log.info("the end InputStream:{}", txns.size());
        return txns;
    }

    public  List<ThirdPartyTxn> reconVodacomAirtimeNormal(@NotNull MultipartFile file) {
        log.info("reconVodacomAirtimeNormal:{}", file.getOriginalFilename());
        List<ThirdPartyTxn> txns = new ArrayList<>();

        try (InputStream fileIs = file.getInputStream()) {

            Workbook workbook = WorkbookFactory.create(fileIs);
            log.info("reconVodacomAirtimeNormal:{}", workbook.getNumberOfSheets());

            for (Sheet sheet : workbook) {
                for (Row row : sheet) {
                    try {
                        if (row.getRowNum() >= 3) {

                            ThirdPartyTxn data = new ThirdPartyTxn();
                            data.setTxn_type("VODACOM");
                            data.setTtype("UTILITY");
                            data.setIdentifier("OUT");
                            data.setFile_name(file.getOriginalFilename());
                            data.setTxnid( getCellValue(row.getCell(7)).replace("\"",""));
                            data.setTxndate(com.helper.DateUtil.strToDate(getCellValue(row.getCell(1)),"yyyy-MM-dd HH:mm:ss"));
                            data.setFile_txndate(com.helper.DateUtil.strToDate(getCellValue(row.getCell(1)),"yyyy-MM-dd HH:mm:ss"));
                            data.setMnoTxns_status("SUCCESS");
                            data.setAmount(new BigDecimal(getCellValue(row.getCell(5))).abs().divide(new BigDecimal(100),2, RoundingMode.HALF_UP));
                            data.setCurrency("TZS");
                            data.setReceiptNo(getCellValue(row.getCell(7)));
                            data.setPrevious_balance(new BigDecimal(getCellValue(row.getCell(4))).abs());
                            data.setPost_balance(new BigDecimal(getCellValue(row.getCell(6))).abs());
                            data.setDescription("Success");
                            data.setTxdestinationaccount(getCellValue( row.getCell(3)).replace("\"",""));
                            data.setSourceAccount(getCellValue( row.getCell(3)).replace("\"",""));
                            List<Map<String,Object>> tpx = getGwTransaction(data.getTxnid());
                            log.info("GW transaction processed:{}",tpx);
                            if(!tpx.isEmpty()) {
                                data.setSourceAccount(tpx.get(0).get("txsourceAccount").toString());
                                data.setDescription((tpx.get(0).get("description").toString()));
                                data.setAcct_no(tpx.get(0).get("txsourceAccount").toString());
                                data.setStatus(tpx.get(0).get("txstatus").toString());
                                data.setDocode(tpx.get(0).get("txtype").toString());
                                data.setTxdestinationaccount(tpx.get(0).get("txdestinationAccount").toString());
                                data.setTxndate(com.helper.DateUtil.strToDate(tpx.get(0).get("txdate").toString()));
                            }else {

                            }
                            txns.add(data);
                            log.info("Thirdpart transaction object:{}",data);
                            thirdPartyTxnRepository.save(data);;
                        }
                    } catch (Exception e) {
                        log.info("Thirdpart transaction process failed:{}",e);
                    }
                }
            }

        } catch (Exception  e) {
            log.error(e.getMessage(),e);
        }
        log.info("the end InputStream:{}", txns.size());
        return txns;
    }



    /*Other utility*/
    public List<Map<String,Object>> getGwTransaction(String reference) {
        String query = "SELECT * FROM tp_transaction x WHERE x.txid = ?";
        return gwJdbcTemplate.queryForList(query, reference);
    }

    public String getCellValue(Cell cell) {
        if (cell == null) {
            return "";
        }
        //log.info("----getCellValue:{} ------",cell.getCellType().name());
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                // Check if it's a date
                if (DateUtil.isCellDateFormatted(cell)) {
                    Date date = cell.getDateCellValue();
                    // Format the date as needed (MM/dd/yyyy HH:mm:ss)
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    return sdf.format(date);
                } else {
                    return String.valueOf((long) cell.getNumericCellValue()); // Handle non-date numeric values
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            case BLANK:
                return "";
            default:
                return "";
        }
    }
}
