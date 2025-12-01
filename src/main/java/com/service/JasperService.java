/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.service;

/**
 *
 * @author melleji.mollel
 */
//import com.helper.DateUtil;
import com.DTO.IBANK.Customer;
import com.DTO.IBANK.TransferApprovers;
import com.config.SYSENV;
import com.helper.DateUtil;
import com.lowagie.text.pdf.PdfWriter;
import com.repository.Recon_M;
import com.repository.ReportRepo;
import com.zaxxer.hikari.HikariDataSource;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.*;
import javax.servlet.http.HttpServletResponse;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.export.HtmlExporter;
import net.sf.jasperreports.engine.export.JRCsvExporter;
import net.sf.jasperreports.engine.export.JRPdfExporter;
import net.sf.jasperreports.engine.export.ooxml.JRDocxExporter;
import net.sf.jasperreports.engine.export.ooxml.JRXlsxExporter;
import net.sf.jasperreports.engine.util.JRLoader;
import net.sf.jasperreports.export.SimpleDocxExporterConfiguration;
import net.sf.jasperreports.export.SimpleDocxReportConfiguration;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleHtmlExporterConfiguration;
import net.sf.jasperreports.export.SimpleHtmlExporterOutput;
import net.sf.jasperreports.export.SimpleHtmlReportConfiguration;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;
import net.sf.jasperreports.export.SimplePdfExporterConfiguration;
import net.sf.jasperreports.export.SimplePdfReportConfiguration;
import net.sf.jasperreports.export.SimpleWriterExporterOutput;
import net.sf.jasperreports.export.SimpleXlsxReportConfiguration;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 *
 * @author samichael
 */
@Service
public class JasperService {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(JasperReport.class);

    @Autowired
    @Qualifier("amgwConnection")
    HikariDataSource dataSourcePartners;

    @Autowired
    @Qualifier("jdbcCbsLive")
    JdbcTemplate jdbcRUBIKONTemplate;

    @Autowired
    @Qualifier("amgwdb")
    JdbcTemplate jdbcTemplate;



    @Autowired
    SYSENV SYSENV;

//    @Autowired
//    TransferService transferService;
    @Autowired
    XapiWebService xapiWebService;

    public JasperPrint jasperPrint(String jasperResFile, Map<String, Object> parameters, Connection conn) throws IOException {
        JasperPrint jasperPrint = null;
        try {
            LOGGER.info("Jasper file path: {}", jasperResFile);
            try (InputStream jasperStream = this.getClass().getResourceAsStream(jasperResFile)) {
                JasperReport jasperReport = (JasperReport) JRLoader.loadObject(jasperStream);
                jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, conn);
                jasperStream.close();
            }
        } catch (Exception ex) {
            LOGGER.error("jasper file loader ", ex);
        }
        return jasperPrint;
    }

    public JasperPrint jasperPrint(String jasperResFile, Map<String, Object> parameters, JRBeanCollectionDataSource source) throws IOException {
        //deposit account statement
        JasperPrint jasperPrint = null;
        try {
            LOGGER.info("Jasper file path: {} ", jasperResFile);

            try (InputStream jasperStream = this.getClass().getResourceAsStream(jasperResFile)) {
                JasperReport jasperReport = (JasperReport) JRLoader.loadObject(jasperStream);
                jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, source);
                jasperStream.close();
            }
        } catch (JRException ex) {
            LOGGER.error("jasper file loader ERROR.{}", ex);

        }
        return jasperPrint;
    }

    private void exportPdf(JasperPrint jasperPrint, HttpServletResponse response, String destName) throws IOException {
        response.setContentType("application/pdf");
        response.setHeader("Content-disposition", "attachment; filename=" + destName + ".pdf");
        JRPdfExporter exporter = new JRPdfExporter();
        exporter.setExporterInput(new SimpleExporterInput(jasperPrint));
        //ByteArrayOutputStream pdfOutputStream = new ByteArrayOutputStream();

        final OutputStream out = response.getOutputStream();
        exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(out));
        SimplePdfReportConfiguration reportConfig = new SimplePdfReportConfiguration();
        reportConfig.setSizePageToContent(true);
        reportConfig.setForceLineBreakPolicy(false);
        exporter.setConfiguration(reportConfig);
        SimplePdfExporterConfiguration exportConfig = new SimplePdfExporterConfiguration();
        // exportConfig.setEncrypted(true);
        //exportConfig.set128BitKey(true);
        //exportConfig.setUserPassword("tpb");
        //exportConfig.setOwnerPassword("tpb");
        exportConfig.setPermissions(PdfWriter.ALLOW_COPY);
        exportConfig.setPermissions(PdfWriter.ALLOW_PRINTING);
        exportConfig.setMetadataAuthor("tcb");
        exporter.setConfiguration(exportConfig);
        try {
            exporter.exportReport();
            out.flush();
            out.close();
            response.flushBuffer();
        } catch (JRException ex) {
            out.flush();
            out.close();
            response.flushBuffer();
            LOGGER.info("exportPdf {}", ex);
        }
    }

    private void exportPdf(JasperPrint jasperPrint, HttpServletResponse response, String username, String password, String destName) throws IOException {
        response.setContentType("application/pdf");
        response.setHeader("Content-disposition", "attachment; filename=" + destName + ".pdf");
        JRPdfExporter exporter = new JRPdfExporter();
        exporter.setExporterInput(new SimpleExporterInput(jasperPrint));
        final OutputStream out = response.getOutputStream();
        exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(out));
        SimplePdfReportConfiguration reportConfig = new SimplePdfReportConfiguration();
        reportConfig.setSizePageToContent(true);
        reportConfig.setForceLineBreakPolicy(false);
        exporter.setConfiguration(reportConfig);
        SimplePdfExporterConfiguration exportConfig = new SimplePdfExporterConfiguration();
        exportConfig.setEncrypted(true);
        exportConfig.set128BitKey(true);
        exportConfig.setUserPassword(username);
        exportConfig.setOwnerPassword(password);
        exportConfig.setPermissions(PdfWriter.ALLOW_COPY);
        exportConfig.setPermissions(PdfWriter.ALLOW_PRINTING);
        exportConfig.setMetadataAuthor("tpb");
        exporter.setConfiguration(exportConfig);
        try {
            exporter.exportReport();
            out.flush();
            out.close();
            response.flushBuffer();
        } catch (JRException ex) {
            out.flush();
            out.close();
            response.flushBuffer();
            LOGGER.info("exportPdf {}", ex);
        }
    }

    public ByteArrayOutputStream exportPdfToStream(JasperPrint jasperPrint, String username, String password) throws IOException {
        JRPdfExporter exporter = new JRPdfExporter();
        exporter.setExporterInput(new SimpleExporterInput(jasperPrint));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(out));
        SimplePdfReportConfiguration reportConfig = new SimplePdfReportConfiguration();
        reportConfig.setSizePageToContent(true);
        reportConfig.setForceLineBreakPolicy(false);
        exporter.setConfiguration(reportConfig);
        SimplePdfExporterConfiguration exportConfig = new SimplePdfExporterConfiguration();
        exportConfig.setEncrypted(true);
        exportConfig.set128BitKey(true);
        exportConfig.setUserPassword(username);
        exportConfig.setOwnerPassword(password);
        exportConfig.setPermissions(PdfWriter.ALLOW_COPY);
        exportConfig.setPermissions(PdfWriter.ALLOW_PRINTING);
        exportConfig.setMetadataAuthor("tpb");
        exporter.setConfiguration(exportConfig);
        try {
            exporter.exportReport();
            out.flush();
            out.close();
        } catch (JRException ex) {
            out.flush();
            out.close();
            LOGGER.info("exportPdfStream {}", ex);
        }
        return out;
    }

    public ByteArrayOutputStream exportPdfToStream(JasperPrint jasperPrint) throws IOException {
        JRPdfExporter exporter = new JRPdfExporter();
        exporter.setExporterInput(new SimpleExporterInput(jasperPrint));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(out));
        SimplePdfReportConfiguration reportConfig = new SimplePdfReportConfiguration();
        reportConfig.setSizePageToContent(true);
        reportConfig.setForceLineBreakPolicy(false);
        exporter.setConfiguration(reportConfig);
        SimplePdfExporterConfiguration exportConfig = new SimplePdfExporterConfiguration();
        exportConfig.setEncrypted(true);
        exportConfig.set128BitKey(true);
        exportConfig.setPermissions(PdfWriter.ALLOW_COPY);
        exportConfig.setPermissions(PdfWriter.ALLOW_PRINTING);
        exportConfig.setMetadataAuthor("tpb");
        exporter.setConfiguration(exportConfig);
        try {
            exporter.exportReport();
            out.flush();
            out.close();
        } catch (JRException ex) {
            out.flush();
            out.close();
            LOGGER.info("exportPdfStream {}", ex);
        }
        return out;
    }

    public ByteArrayOutputStream exportPdfMergedToStream(List<JasperPrint> jasperPrint, String username, String password) throws IOException {
        JRPdfExporter exporter = new JRPdfExporter();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        //load multiple printers
        exporter.setExporterInput(SimpleExporterInput.getInstance(jasperPrint));
        exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(out));
        SimplePdfReportConfiguration reportConfig = new SimplePdfReportConfiguration();
        reportConfig.setSizePageToContent(true);
        reportConfig.setForceLineBreakPolicy(false);
        exporter.setConfiguration(reportConfig);
        SimplePdfExporterConfiguration exportConfig = new SimplePdfExporterConfiguration();
        exportConfig.setEncrypted(true);
        exportConfig.set128BitKey(true);
        exportConfig.setUserPassword(username);
        exportConfig.setOwnerPassword(password);
        exportConfig.setPermissions(PdfWriter.ALLOW_COPY);
        exportConfig.setPermissions(PdfWriter.ALLOW_PRINTING);
        exportConfig.setMetadataAuthor("tpb");
        exporter.setConfiguration(exportConfig);
        try {
            exporter.exportReport();
            out.flush();
            out.close();
        } catch (JRException ex) {
            out.flush();
            out.close();
            LOGGER.info("exportPdfStream {}", ex);
        }
        return out;
    }

    public ByteArrayOutputStream exportPdfMergedToStream(List<JasperPrint> jasperPrint) throws IOException {
        JRPdfExporter exporter = new JRPdfExporter();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        //load multiple printers
        exporter.setExporterInput(SimpleExporterInput.getInstance(jasperPrint));
        exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(out));
        SimplePdfReportConfiguration reportConfig = new SimplePdfReportConfiguration();
        reportConfig.setSizePageToContent(true);
        reportConfig.setForceLineBreakPolicy(false);
        exporter.setConfiguration(reportConfig);
        SimplePdfExporterConfiguration exportConfig = new SimplePdfExporterConfiguration();
        exportConfig.setEncrypted(true);
        exportConfig.set128BitKey(true);
        exportConfig.setPermissions(PdfWriter.ALLOW_COPY);
        exportConfig.setPermissions(PdfWriter.ALLOW_PRINTING);
        exportConfig.setMetadataAuthor("tpb");
        exporter.setConfiguration(exportConfig);
        try {
            exporter.exportReport();
            out.flush();
            out.close();
        } catch (JRException ex) {
            out.flush();
            out.close();
            LOGGER.info("exportPdfStream {}", ex);
        }
        return out;
    }

    private void exportExcel(JasperPrint jasperPrint, HttpServletResponse response, String destName) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment;filename=" + destName + ".xlsx");
        JRXlsxExporter exporter = new JRXlsxExporter();
//        SimpleXlsxReportConfiguration reportConfig = new SimpleXlsxReportConfiguration();
//        reportConfig.setSheetNames(new String[]{"Report 1"});
//        reportConfig.setDetectCellType(Boolean.FALSE);
//        reportConfig.setRemoveEmptySpaceBetweenColumns(Boolean.TRUE);
//        reportConfig.setIgnoreCellBackground(Boolean.TRUE);
//        reportConfig.setWhitePageBackground(Boolean.FALSE);
//        exporter.setConfiguration(reportConfig);
        final OutputStream out = response.getOutputStream();
        exporter.setExporterInput(new SimpleExporterInput(jasperPrint));
        exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(out));
        try {
            exporter.exportReport();
            out.flush();
            out.close();
            response.flushBuffer();
        } catch (JRException ex) {
            out.flush();
            out.close();
            response.flushBuffer();
            LOGGER.info("exportExcel {}", ex);
        }
    }
//    private void exportExcel(JasperPrint jasperPrint, HttpServletResponse response, String destName) throws IOException {
//        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
//        response.setHeader("Content-Disposition", "attachment;filename=" + destName + ".xlsx");
//        JRXlsxExporter exporter = new JRXlsxExporter();
//        SimpleXlsxReportConfiguration reportConfig = new SimpleXlsxReportConfiguration();
//        reportConfig.setSheetNames(new String[]{"Report 1"});
//        reportConfig.setDetectCellType(Boolean.TRUE);
//        reportConfig.setRemoveEmptySpaceBetweenColumns(Boolean.TRUE);
//        reportConfig.setIgnoreCellBackground(Boolean.TRUE);
//        reportConfig.setWhitePageBackground(Boolean.FALSE);
//        exporter.setConfiguration(reportConfig);
//        final OutputStream out = response.getOutputStream();
//        exporter.setExporterInput(new SimpleExporterInput(jasperPrint));
//        exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(out));
//        try {
//            exporter.exportReport();
//            out.flush();
//            out.close();
//            response.flushBuffer();
//        } catch (JRException ex) {
//            out.flush();
//            out.close();
//            response.flushBuffer();
//            LOGGER.info("exportExcel {}", ex);
//        }
//    }

//    private void exportHtml(JasperPrint jasperPrint, HttpServletResponse response, String destName) throws IOException {
//        response.setContentType("text/html");
//        response.setCharacterEncoding("utf-8");
//        response.setHeader("Content-Disposition", "attachment;filename=" + destName + ".html");
//        final OutputStream out = response.getOutputStream();
//        HtmlExporter exporter = new HtmlExporter();
//        SimpleHtmlExporterConfiguration expConfig = new SimpleHtmlExporterConfiguration();
//        /*//might be used in the future
//            String header = "<html>\n"
//                    + "<head>\n"
//                    + "  <title></title>\n"
//                    + "  <meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\"/>\n"
//                    + "  <link rel=\"stylesheet\" type=\"text/css\" href=\"css/jasper.css\" />\n"
//                    + "  <style type=\"text/css\">\n"
//                    + "    a {text-decoration: none}\n"
//                    + "  </style>\n"
//                    + "</head>\n";
//            config.setHtmlHeader(header);*/
//        SimpleHtmlReportConfiguration repoConfig = new SimpleHtmlReportConfiguration();
//        repoConfig.setEmbedImage(Boolean.TRUE);
//        repoConfig.setRemoveEmptySpaceBetweenRows(Boolean.TRUE);
//        repoConfig.setAccessibleHtml(Boolean.TRUE);
//        exporter.setConfiguration(repoConfig);
//        exporter.setConfiguration(expConfig);
//        exporter.setExporterInput(new SimpleExporterInput(jasperPrint));
//        exporter.setExporterOutput(new SimpleHtmlExporterOutput(out, "UTF-8"));
//        try {
//            exporter.exportReport();
//            out.flush();
//            out.close();
//            response.flushBuffer();
//        } catch (JRException ex) {
//            out.flush();
//            out.close();
//            response.flushBuffer();
//            LOGGER.info("exporthtml {}", ex);
//        }
//    }

    private void exportHtml(JasperPrint jasperPrint, HttpServletResponse response, String destName) throws IOException {
        response.setContentType("text/html");
        response.setCharacterEncoding("utf-8");
        response.setHeader("Content-Disposition", "attachment;filename=" + destName + ".html");
        final OutputStream out = response.getOutputStream();
        HtmlExporter exporter = new HtmlExporter();
        SimpleHtmlExporterConfiguration expConfig = new SimpleHtmlExporterConfiguration();
        /*//might be used in the future
            String header = "<html>\n"
                    + "<head>\n"
                    + "  <title></title>\n"
                    + "  <meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\"/>\n"
                    + "  <link rel=\"stylesheet\" type=\"text/css\" href=\"css/jasper.css\" />\n"
                    + "  <style type=\"text/css\">\n"
                    + "    a {text-decoration: none}\n"
                    + "  </style>\n"
                    + "</head>\n";
            config.setHtmlHeader(header);*/
        SimpleHtmlReportConfiguration repoConfig = new SimpleHtmlReportConfiguration();
        repoConfig.setEmbedImage(Boolean.TRUE);
        repoConfig.setRemoveEmptySpaceBetweenRows(Boolean.FALSE);
        repoConfig.setAccessibleHtml(Boolean.TRUE);
        exporter.setConfiguration(repoConfig);
        exporter.setConfiguration(expConfig);
        exporter.setExporterInput(new SimpleExporterInput(jasperPrint));
        exporter.setExporterOutput(new SimpleHtmlExporterOutput(out, "UTF-8"));
        try {
            exporter.exportReport();
            out.flush();
            out.close();
            response.flushBuffer();
        } catch (JRException ex) {
            out.flush();
            out.close();
            response.flushBuffer();
            LOGGER.info("exporthtml {}", ex);
        }
    }

    public String exportHtmlStr(JasperPrint jasperPrint) throws IOException {
        byte[] bytes = null;
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        HtmlExporter exporter = new HtmlExporter();
        SimpleHtmlExporterConfiguration expConfig = new SimpleHtmlExporterConfiguration();
        /*//might be used in the future
            String header = "<html>\n"
                    + "<head>\n"
                    + "  <title></title>\n"
                    + "  <meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\"/>\n"
                    + "  <link rel=\"stylesheet\" type=\"text/css\" href=\"css/jasper.css\" />\n"
                    + "  <style type=\"text/css\">\n"
                    + "    a {text-decoration: none}\n"
                    + "  </style>\n"
                    + "</head>\n";
            config.setHtmlHeader(header);*/
        SimpleHtmlReportConfiguration repoConfig = new SimpleHtmlReportConfiguration();
        repoConfig.setEmbedImage(Boolean.TRUE);
        repoConfig.setRemoveEmptySpaceBetweenRows(Boolean.FALSE);
        repoConfig.setAccessibleHtml(Boolean.TRUE);
        exporter.setConfiguration(repoConfig);
        exporter.setConfiguration(expConfig);
        exporter.setExporterInput(new SimpleExporterInput(jasperPrint));
        exporter.setExporterOutput(new SimpleHtmlExporterOutput(out, "UTF-8"));
        try {
            exporter.exportReport();
            out.flush();
            out.close();
            bytes = out.toByteArray();
        } catch (JRException ex) {
            out.flush();
            out.close();
            LOGGER.info("Export html {}", ex.getMessage());
        }
        return new String(bytes);
    }

    private void exportCSV(JasperPrint jasperPrint, HttpServletResponse response, String destName) throws IOException {
        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment;filename=" + destName + ".csv");
        JRCsvExporter exporter = new JRCsvExporter();
        final OutputStream out = response.getOutputStream();
        exporter.setExporterInput(new SimpleExporterInput(jasperPrint));
        exporter.setExporterOutput(new SimpleWriterExporterOutput(out));
        try {
            exporter.exportReport();
            out.flush();
            out.close();
            response.flushBuffer();
        } catch (JRException ex) {
            out.flush();
            out.close();
            response.flushBuffer();
            LOGGER.info("exportPdf {}", ex);
        }
    }

    private void exportWord(JasperPrint jasperPrint, HttpServletResponse response, String destName) throws IOException {
        response.setContentType("application/vnd.ms-word;charset=utf-8");
        response.setContentType("application/octet-stream");
        response.setHeader("Connection", "close");
        response.setHeader("Content-Disposition", "attachment;filename=" + destName + ".docx");
        JRDocxExporter exporter = new JRDocxExporter();
        SimpleDocxExporterConfiguration expConfig = new SimpleDocxExporterConfiguration();
        expConfig.setMetadataTitle("tpb");
        expConfig.setMetadataAuthor("tpb");
        expConfig.setMetadataSubject("tpb");
        exporter.setConfiguration(expConfig);
        SimpleDocxReportConfiguration repoConfig = new SimpleDocxReportConfiguration();
        repoConfig.setFlexibleRowHeight(Boolean.TRUE);
        //repoConfig.setFramesAsNestedTables(Boolean.TRUE);
        repoConfig.setNewLineAsParagraph(Boolean.TRUE);
        exporter.setConfiguration(repoConfig);
        final OutputStream out = response.getOutputStream();
        exporter.setExporterInput(new SimpleExporterInput(jasperPrint));
        exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(out));
        try {
            exporter.exportReport();
            out.flush();
            out.close();
            response.flushBuffer();
        } catch (JRException ex) {
            out.flush();
            out.close();
            response.flushBuffer();
            LOGGER.info("exportPdf {}", ex);
        }
    }

    public JasperPrint dailyReconReport(Map<String, String> form) throws IOException, SQLException {
        String jasperResFile = "/jasperReports/daily_report.jasper";
        String pfNo = form.get("pfNo");
        String employeeId = form.get("employeeId");
        String month = form.get("month");
        String year = form.get("year");
        String printedBy = form.get("printedBy");
        //String month = "september";
        //String year = "2020";
        Connection conn = dataSourcePartners.getConnection();
        Map<String, Object> parameters = new HashMap<>();

        parameters.put("RECON_DATE", "2020-07-21");
        parameters.put("TXN_TYPE", "B2C");

        JasperPrint print = jasperPrint(jasperResFile, parameters, conn);
        if (conn != null) {
            conn.close();
        }
        return print;
    }

    public String sampleReportRecon(Map<String, String> form, String exporterFileType, HttpServletResponse response, String destName) throws IOException, SQLException {
        JasperPrint print = dailyReconReport(form);
        return exportFileOption(print, exporterFileType, response, destName);

    }

    public String mnoBalance(Map<String, String> form, String exporterFileType, HttpServletResponse response, String destName) throws IOException, SQLException {
        JasperPrint print = mnoBalanceReport(form);
        return exportFileOption(print, exporterFileType, response, destName);

    }

    public JasperPrint mnoBalanceReport(Map<String, String> form) throws IOException, SQLException {
        String jasperResFile = "/iReports/account_balance.jasper";
        String startDate = form.get("txnDate");
        //String month = "september";
        //String year = "2020";
        Connection conn = dataSourcePartners.getConnection();
        Map<String, Object> parameters = new HashMap<>();

        parameters.put("I_BALANCE_DATE", startDate);
        JasperPrint print = jasperPrint(jasperResFile, parameters, conn);
        if (conn != null) {
            conn.close();
        }
        return print;
    }

    private void exportTofile(Map<String, Object> parameters, String jasperResFile, HttpServletResponse response, JRBeanCollectionDataSource source) throws JRException, IOException {
        try {
            //to be implemented in the future
            JasperPrint jasperPrint = jasperPrint(jasperResFile, parameters, source);
            final OutputStream outStream = response.getOutputStream();
            JasperExportManager.exportReportToPdfStream(jasperPrint, outStream);
        } catch (JRException e) {
            LOGGER.info("Jasper Report error.... {}", e);
        }
        LOGGER.info("Report generated....");
    }

    public String exportFileOption(JasperPrint print, String exporterFileType, HttpServletResponse response, String destName) throws IOException {
        String responseBody = "-1";
        if ("pdf".equals(exporterFileType)) {
            exportPdf(print, response, destName);
        } else if ("excel".equals(exporterFileType)) {
            exportExcel(print, response, destName);
        } else if ("csv".equals(exporterFileType)) {
            exportCSV(print, response, destName);
        } else if ("html".equals(exporterFileType)) {
            exportHtml(print, response, destName);
        } else if ("word".equals(exporterFileType)) {
            exportWord(print, response, destName);
        } else if ("preview".equals(exporterFileType)) {
            responseBody = exportHtmlStr(print);
        } else {
            LOGGER.info("{} file exporter extension type.", exporterFileType);
        }
        return responseBody;
    }



    public JasperPrint printCustomerinformation(Map<String, String> form) throws IOException {
        String jasperResFile = "/iReports/customer_inirmation.jasper";
        String account = form.get("account");
        String printedBy = form.get("printedBy");
        LOGGER.info("Generate report for account: {}", account);
        Map<String, Object> appv = getCustomerInformationFromCore(account);
        LOGGER.info("Approvers list: {}", appv);
        if (appv.get("ACCT_NM") == null) {
            List<TransferApprovers> appvs = new ArrayList<>();
            TransferApprovers a = new TransferApprovers();
            a.setNAME("Not Records");
            appvs.add(a);
            LOGGER.info("jasperResFile: {}", jasperResFile);
            LOGGER.info("printedBy: {}", printedBy);
            LOGGER.info("Size: {}", appvs.size());
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("CUST_NM", "No record");
            parameters.put("CUST_RIM", "No record");
            parameters.put("ACCT_NO", "Account does not exist.");
            parameters.put("BRANCH_NAME", "No record");
            parameters.put("CURRENCY", "No record");
            parameters.put("PHOTO", "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");
            parameters.put("SIGNATURE", "iVBORw0KGgoAAAANSUhEUgAABQgAAAMgCAYAAABvT48PAAAACXBIWXMAAAsTAAALEwEAmpwYAAAFGmlUWHRYTUw6Y29tLmFkb2JlLnhtcAAAAAAAPD94cGFja2V0IGJlZ2luPSLvu78iIGlkPSJXNU0wTXBDZWhpSHpyZVN6TlRjemtjOWQiPz4gPHg6eG1wbWV0YSB4bWxuczp4PSJhZG9iZTpuczptZXRhLyIgeDp4bXB0az0iQWRvYmUgWE1QIENvcmUgNi4wLWMwMDIgNzkuMTY0MzUyLCAyMDIwLzAxLzMwLTE1OjUwOjM4ICAgICAgICAiPiA8cmRmOlJERiB4bWxuczpyZGY9Imh0dHA6Ly93d3cudzMub3JnLzE5OTkvMDIvMjItcmRmLXN5bnRheC1ucyMiPiA8cmRmOkRlc2NyaXB0aW9uIHJkZjphYm91dD0iIiB4bWxuczp4bXA9Imh0dHA6Ly9ucy5hZG9iZS5jb20veGFwLzEuMC8iIHhtbG5zOmRjPSJodHRwOi8vcHVybC5vcmcvZGMvZWxlbWVudHMvMS4xLyIgeG1sbnM6cGhvdG9zaG9wPSJodHRwOi8vbnMuYWRvYmUuY29tL3Bob3Rvc2hvcC8xLjAvIiB4bWxuczp4bXBNTT0iaHR0cDovL25zLmFkb2JlLmNvbS94YXAvMS4wL21tLyIgeG1sbnM6c3RFdnQ9Imh0dHA6Ly9ucy5hZG9iZS5jb20veGFwLzEuMC9zVHlwZS9SZXNvdXJjZUV2ZW50IyIgeG1wOkNyZWF0b3JUb29sPSJBZG9iZSBQaG90b3Nob3AgMjEuMSAoTWFjaW50b3NoKSIgeG1wOkNyZWF0ZURhdGU9IjIwMjAtMDgtMTNUMTI6NDU6NTArMDM6MDAiIHhtcDpNb2RpZnlEYXRlPSIyMDIwLTA4LTEzVDE1OjU2OjEwKzAzOjAwIiB4bXA6TWV0YWRhdGFEYXRlPSIyMDIwLTA4LTEzVDE1OjU2OjEwKzAzOjAwIiBkYzpmb3JtYXQ9ImltYWdlL3BuZyIgcGhvdG9zaG9wOkNvbG9yTW9kZT0iMyIgcGhvdG9zaG9wOklDQ1Byb2ZpbGU9InNSR0IgSUVDNjE5NjYtMi4xIiB4bXBNTTpJbnN0YW5jZUlEPSJ4bXAuaWlkOjJlMzJkNWI3LTEzMjMtNDBiNi1iNDJmLTIwZTliYzlhZDY0MiIgeG1wTU06RG9jdW1lbnRJRD0ieG1wLmRpZDoyZTMyZDViNy0xMzIzLTQwYjYtYjQyZi0yMGU5YmM5YWQ2NDIiIHhtcE1NOk9yaWdpbmFsRG9jdW1lbnRJRD0ieG1wLmRpZDoyZTMyZDViNy0xMzIzLTQwYjYtYjQyZi0yMGU5YmM5YWQ2NDIiPiA8eG1wTU06SGlzdG9yeT4gPHJkZjpTZXE+IDxyZGY6bGkgc3RFdnQ6YWN0aW9uPSJjcmVhdGVkIiBzdEV2dDppbnN0YW5jZUlEPSJ4bXAuaWlkOjJlMzJkNWI3LTEzMjMtNDBiNi1iNDJmLTIwZTliYzlhZDY0MiIgc3RFdnQ6d2hlbj0iMjAyMC0wOC0xM1QxMjo0NTo1MCswMzowMCIgc3RFdnQ6c29mdHdhcmVBZ2VudD0iQWRvYmUgUGhvdG9zaG9wIDIxLjEgKE1hY2ludG9zaCkiLz4gPC9yZGY6U2VxPiA8L3htcE1NOkhpc3Rvcnk+IDwvcmRmOkRlc2NyaXB0aW9uPiA8L3JkZjpSREY+IDwveDp4bXBtZXRhPiA8P3hwYWNrZXQgZW5kPSJyIj8+S4zUMgAAe6lJREFUeJzt/X2wXel+F3Z+m+N7ygINB7CMZaxgkWgmchJRIOyRYw9yUmIwbScNWJnGGRkEk2agmskIMk1Ik9AD006GxlTkZGgztJ22mbYnbdJmZE9jy6Rv0LVpF5ZJX9Ke0aXQHZBBd0q2RXE6o1u6dW7d0vzx6HBPq8/e62WvvZ+91/p8qp5q++rsfX57n/2y1nc9z+956tGjRwEAAAAApulX1C4AAAAAAKhHQAgAAAAAEyYgBAAAAIAJExACAAAAwIQJCAEAAABgwgSEAAAAADBhAkIAAAAAmDABIQAAAABMmIAQAAAAACZMQAgAAAAAEyYgBAAAAIAJExACAAAAwIQJCAEAAABgwgSEAAAAADBhAkIAAAAAmDABIQAAAABMmIAQAAAAACZMQAgAAAAAEyYgBAAAAIAJExACAAAAwIQJCAEAAABgwgSEAAAAADBhAkIAAAAAmDABIQAAAABMmIAQAAAAACZMQAgAAAAAEyYgBAAAAIAJExACAAAAwIQJCAEAAABgwgSEAAAAADBhAkIAAAAAmDABIQAAAABMmIAQAAAAACZMQAgAAAAAEyYgBAAAAIAJExACAAAAwIQJCAEAAABgwgSEAAAAADBhAkIAAAAAmDABIQAAAABMmIAQAAAAACZMQAgAAAAAEyYgBAAAAIAJExACAAAAwIQJCAEAAABgwgSEAAAAADBhAkIAAAAAmDABIQAAAABMmIAQAAAAACZMQAgAAAAAEyYgBAAAAIAJExACAAAAwIQJCAEAAABgwgSEAAAAADBhAkIAAAAAmDABIQAAAABMmIAQAAAAACZMQAgAAAAAEyYgBAAAAIAJExACAAAAwIQJCAEAAABgwgSEAAAAADBhAkIAAAAAmDABIQAAAABMmIAQAAAAACZMQAgAAAAAEyYgBAAAAIAJExACAAAAwIQJCAEAAABgwgSEAAAAADBhAkIAAAAAmDABIQAAAABMmIAQAAAAACZMQAgAAAAAEyYgBAAAAIAJExACAAAAwIQJCAEAAABgwgSEAAAAADBhAkIAAAAAmDABIQAAAABMmIAQAAAAACZMQAgAAAAAEyYgBAAAAIAJExACAAAAwIQJCAEAAABgwgSEAAAAADBhAkIAAAAAmDABIQAAAABMmIAQAAAAACZMQAgAAAAAEyYgBAAAAIAJExACAAAAwIQJCAEAAABgwgSEAAAAADBhAkIAAAAAmDABIQAAAABMmIAQAAAAACZMQAgAAAAAEyYgBAAAAIAJExACAAAAwIQJCAEAAABgwgSEAAAAADBhAkIAAAAAmDABIQAAAABMmIAQAAAAACZMQAgAAAAAEyYgBAAAAIAJExACAAAAwIQJCAEAAABgwgSEAAAAADBhAkIAAAAAmDABIQAAAABMmIAQAAAAACZMQAgAAAAAEyYgBAAAAIAJExACAAAAwIQJCAEAAABgwgSEAAAAADBhAkIAAAAAmDABIQAAAABMmIAQAAAAACZMQAgAAAAAEyYgBAAAAIAJExACAAAAwIQJCAEAAABgwgSEAAAAADBhAkIAAAAAmDABIQAAAABMmIAQAAAAACZMQAgAAAAAEyYgBAAAAIAJExACAAAAwIQJCAEAAABgwgSEAAAAADBhAkIAAAAAmDABIQAAAABMmIAQAAAAACZMQAgAAAAAEyYgBAAAAIAJExACAAAAwIQJCAEAAABgwgSEAAAAADBhAkIAAAAAmDABIQAAAABMmIAQAAAAACZMQAgAAAAAEyYgBAAAAIAJExAC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ADBhAkIAAAAAmDABIQAAAABMmIAQAAAAACZMQAgAAAAAEyYgBAAAAIAJExACAAAAwIQJCAEAAABgwgSEAAAAADBhAkIAAAAAmDABIQAAAABMmIAQAAAAACZMQAgAAAAAEyYgBAAAAIAJExACAAAAwIQJCAEAAABgwgSEAAAAADBhAkIAAAAAmDABIQAAAABMmIAQAAAAACZMQAgAAAAAEyYgBAAAAIAJExACAAAAwIQJCAEAAABgwgSEAAAAADBhAkIAAAAAmDABIQAAAABMmIAQAAAAACZMQAgAAAAAEyYgBAAAAIAJExACAAAAwIQJCAEAAABgwgSEAAAAADBhAkIAAAAAmDABIQAAAABMmIAQAAAAACZMQAgAAAAAEyYgBAAAAIAJExACAAAAwIQJCAEAAABgwgSEAAAAADBhAkIAAAAAmDABIQAAAABMmIAQAAAAACZMQAgAAAAAEyYgBAAAAIAJExACAAAAwIQJCAEAAABgwgSEAAAAADBhAkIAAAAAmDABIQAAAABMmIAQAAAAACZMQAgAAAAAEyYgBAAAAIAJExACAAAAwIQJCAEAAABgwgSEAAAAADBhAkIAAAAAmDABIQAAAABMmIAQAAAAACZMQAgAAAAAEyYgBAAAAIAJExACAAAAwIQJCAEAAABgwgSEAAAAADBhAkIAAAAAmDABIQAAAABMmIAQAAAAACZMQAgAAAAAEyYgBAAAAIAJExACAAAAwIQJCAEAAABgwgSEAAAAADBhAkIAAAAAmDABIQAAAABMmIAQAAAAACZMQAgAAAAAEyYgBAAAAIAJExACAAAAwIQJCAEAAABgwgSEAAAAADBhAkIAAAAAmDABIQAAAABMmIAQAAAAACZMQAgAAAAAEyYgBAAAAIAJExACAAAAwIQJCAEAAABgwgSEAAAAADBhAkIAAAAAmDABIQAAAABMmIAQAAAAACZMQAgAAAAAEyYgBAAAAIAJExACAAAAwIQJCAEAAABgwgSEAAAAADBhAkIAAAAAmDABIQAAAABMmIAQAAAAACZMQAgAAAAAEyYgBAAAAIAJExACAAAAwIQJCAEAAABgwgSEAAAAADBhAkIAAAAAmDABIQAAAABMmIAQAAAAACZMQAgAAAAAEyYgBAAAAIAJExACAAAAwIQJCAEAAABgwgSEAAAAADBhAkIAAAAAmDABIQAAAABMmIAQAAAAACZMQAgAAAAAEyYgBAAAAIAJExACAAAAwIQJCAEAAABgwgSEAAAAADBhAkIAAAAAmDABIQAAAABMmIAQAAAAACZMQAgAAAAAEyYgBAAAAIAJExACAAAAwIQJCAEAAABgwgSEAAAAADBhAkIAAAAAmDABIQAAAABMmIAQAAAAACZMQAgAAAAAEyYgBAAAAIAJExACAAAAwIQJCAEAAABgwgSEAAAAADBhAkIAAAAAmDABIQAAAABMmIAQAAAAACZMQAgAAAAAEyYgBAAAAIAJExACAAAAwIQJCAEAAABgwgSEAAAAADBhAkIAAAAAmDABIQAAAABMmIAQAAAAACZMQAgAAAAAEyYgBAAAAIAJExACAAAAwIQJCAEAAABgwgSEAAAAADBhAkIAAAAAmDABIQAAAABMmIAQAAAAACZMQAgAAAAAEyYgBAAAAIAJExACAAAAwIQJCAEAAABgwgSEAAAAADBhAkIAAAAAmDABIQAAAABMmIAQAAAAACZMQAgAAAAAEyYgBAAAAIAJExACAAAAwIQJCAEAAABgwgSEAAAAADBhAkIAAAAAmDABIQAAAABMmIAQAAAAACZMQAgAAAAAEyYgBAAAAIAJExACAAAAwIQJCAEAAABgwgSEAAAAADBhAkIAAAAAmDABIQAAAABMmIAQAAAAACZMQAgAAAAAEyYgBAAAAIAJExACAAAAwIQJCAEAAABgwgSEAAAAADBhAkIAAAAAmDABIQAAAABMmIAQAAAAACZMQAgAAAAAEyYgBAAAAIAJExACAAAAwIQJCAEAAABgwgSEAAAAADBhAkIAAAAAmDABIQAAAABM2P8fvAnaFO+zaNwAAAAASUVORK5CYII=");
            parameters.put("PRINTED_AT", DateUtil.now("yyyy-MM-dd HH:mm:ss"));
            parameters.put("PRINTED_BY", printedBy);
            JasperPrint print = jasperPrint(jasperResFile, parameters, new JRBeanCollectionDataSource(appvs));
            return print;
        } else {
            List<TransferApprovers> appvs = new ArrayList<>();
            TransferApprovers a = new TransferApprovers();
            a.setNAME("Not Records");
            appvs.add(a);
            LOGGER.info("jasperResFile: {}", jasperResFile);
            LOGGER.info("account: {}", account);
            LOGGER.info("printedBy: {}", printedBy);
            LOGGER.info("Size: {}", appv.size());
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("CUST_NM", appv.get("ACCT_NM")+"");
            parameters.put("CUST_RIM", appv.get("CUST_NO")+"");
            parameters.put("ACCT_NO", appv.get("ACCT_NO")+"");
            parameters.put("BRANCH_NAME", appv.get("BU_NM")+"");
            parameters.put("CURRENCY", appv.get("CURRENCY_CODE")+"");
            parameters.put("PHOTO", appv.get("PHOTO")+"");
            parameters.put("SIGNATURE", appv.get("SIGNATURE")+"");
            parameters.put("PRINTED_AT", DateUtil.now("yyyy-MM-dd HH:mm:ss"));
            parameters.put("PRINTED_BY", printedBy);
            JasperPrint print = jasperPrint(jasperResFile, parameters, new JRBeanCollectionDataSource(appvs));
            return print;
        }
    }

    public String procCustomerinformation(Map<String, String> form, String exporterFileType, HttpServletResponse response, String destName) throws IOException {
        JasperPrint print = printCustomerinformation(form);
        return exportFileOption(print, exporterFileType, response, destName);
    }
    public List<Customer> getCustomerImages(String custId) {
        try {
            List<Customer> docs
                    = this.jdbcRUBIKONTemplate.query("SELECT CUST_ID,BINARY_IMAGE,IMAGE_TY,IMAGE_TY_DESC FROM V_CUSTOMER_IMAGES vci WHERE CUST_ID = ?", new Object[]{custId},
                            (ResultSet rs, int rowNum) -> {
                                Customer row = new Customer();
                                row.setName(rs.getString("CUST_ID"));
                                row.setCustId(rs.getString("CUST_ID"));
                                row.setPayload(Base64.encodeBase64String(rs.getBytes("BINARY_IMAGE")));
                                row.setPayloadType(rs.getString("IMAGE_TY_DESC"));
                                return row;
                            });
            return docs;
        } catch (DataAccessException e) {
            LOGGER.info(null, e);
            return null;
        }
    }


    public Map<String, Object> getCustomerInformationFromCore(String account) {
        try {
            Map<String, Object> map =  acctSummary(account);
            String custId = map.get("CUST_ID")+"";
            LOGGER.info("Customer cust id: {}",custId);
            if (custId != null) {
                List<Customer> cust = getCustomerImages(custId);
                            for (Customer customer : cust) {
                                if (customer.getPayloadType().equalsIgnoreCase("Photograph")) {
                                    map.put("PHOTO",customer.getPayload());
                                }
                                if (customer.getPayloadType().equalsIgnoreCase("Signature")) {
                                    map.put("SIGNATURE",customer.getPayload());
                                }
                            }
                        }
            return map;
        } catch (DataAccessException e) {
            LOGGER.info(null, e);
            return new HashMap<>();
        }
    }

    public Map<String, Object> acctSummary(String acct) {
        Map<String, Object> items = new HashMap<>();
        String query = "SELECT AC.CUST_NO, AC.CUST_ID, AC.ACCT_NM,AC.ACCT_NO,AC.OLD_ACCT_NO,AC.CRNCY_ID,\n"
                + "       P.PROD_CD,AC.MAIN_BRANCH_ID,CUR.CRNCY_CD_ISO AS CURRENCY_CODE,"
                + "         CUR.CRNCY_NM as CURRENCY_NAME,BU.BU_NM,BU.BU_CD \n"
                + "       ,DAS.LEDGER_BAL AS CURRENT_BALANCE,\n"
                + "       DAS.CLEARED_BAL AS AVAILABLE_BALANCE\n"
                + ",DAS.DR_INT_ACCRUED"
                + "      FROM V_ACCOUNTS AC \n"
                + "	JOIN CURRENCY CUR ON AC.CRNCY_ID =CUR.CRNCY_ID \n"
                + "	JOIN BUSINESS_UNIT BU ON BU.BU_ID = AC.MAIN_BRANCH_ID \n"
                + "	JOIN DEPOSIT_ACCOUNT_SUMMARY DAS ON DAS.ACCT_NO = AC.ACCT_NO \n"
                + "     JOIN PRODUCT P ON P.PROD_ID=AC.PROD_ID \n"
                + " WHERE (AC.ACCT_NO=? OR AC.OLD_ACCT_NO=?)";
        query = query.replace("\n", "");
        List<Map<String, Object>> acctSummary = this.jdbcRUBIKONTemplate.queryForList(query, new Object[]{acct, acct});
        if (acctSummary.size() > 0) {
            items = acctSummary.get(0);
        }
        return items;
    }


    public JasperPrint printCustomerinformationBalance(Map<String, String> form) throws IOException, ParseException {
        String jasperResFile = "/iReports/customer_account_balance.jasper";
        String acctNo = form.get("account");
        String startDate = form.get("startDate");
        String endDate = form.get("endDate");
        String printedBy = form.get("printedBy");

        JasperPrint print = null;

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("I_START_DATE",new Date());
        parameters.put("I_END_DATE",new Date());
        parameters.put("I_ACCT_NO", acctNo);
        parameters.put("USER_ROLE_ID", 1619L);
        try {
            Connection conn = this.jdbcRUBIKONTemplate.getDataSource().getConnection();
            print = jasperPrint(jasperResFile, parameters, conn);
            if (conn != null) {
                conn.close();
            }
            return print;
        } catch (SQLException ex) {
            LOGGER.error(null, ex);
            return print;
        }
    }


    public JasperPrint printCustomerinformationStatement(Map<String, String> form) throws IOException, ParseException {
        String jasperResFile = "/iReports/account_statement.jasper";
        String acctNo = form.get("account");
        String startDate = form.get("startDate");
        String endDate = form.get("endDate");
        String printedBy = form.get("printedBy");

        JasperPrint print = null;

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("I_START_DATE",DateUtil.strToDate(startDate,"yyyy-MM-dd"));
        parameters.put("I_END_DATE",DateUtil.strToDate(endDate,"yyyy-MM-dd"));
        parameters.put("I_ACCT_NO", acctNo);
        parameters.put("USER_ROLE_ID", 1619L);
        try {
            Connection conn = this.jdbcRUBIKONTemplate.getDataSource().getConnection();
            print = jasperPrint(jasperResFile, parameters, conn);
            if (conn != null) {
                conn.close();
            }
            return print;
        } catch (SQLException ex) {
            LOGGER.error(null, ex);
            return print;
        }
    }


    public String procCustomerinformationStatementSize(Map<String, String> form, String exporterFileType, HttpServletResponse response, String destName) throws IOException, ParseException {
        JasperPrint print = printCustomerinformationStatement(form);
        print.getPages().size();
        return "Customer will be charged amount of "+print.getPages().size()*500+" for "+print.getPages().size()+" pages";
    }
    
    public String procCustomerinformationBalanceSize(Map<String, String> form, String exporterFileType, HttpServletResponse response, String destName) throws IOException, ParseException {
        JasperPrint print = printCustomerinformationBalance(form);
        return "Customer will be charged amount of "+print.getPages().size()*1000+" for balance report";
    }


}
