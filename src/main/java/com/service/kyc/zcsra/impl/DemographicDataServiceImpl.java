package com.service.kyc.zcsra.impl;

import com.DTO.KYC.zcsra.*;
import com.config.SYSENV;
import com.dao.kyc.response.zcsra.DemographicResponse;
import com.dao.kyc.response.zcsra.ResponseData;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.helper.EncryptUtil;
import com.helper.Mapper;
import com.helper.PdfUtil;
import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.models.kyc.DemographicDataEntity;
import com.repository.Kyc.DemographicDataRepository;
import com.service.kyc.zcsra.DemographicDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.awt.*;
import java.io.*;
import java.security.PrivateKey;
import java.util.Base64;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class DemographicDataServiceImpl implements DemographicDataService {
    private final DemographicDataRepository demographicDataRepository;
    private final SYSENV sysenv;
    private final ObjectMapper objectMapper;
    private final PdfUtil pdfUtil;
    @Override
    public DemographicDataEntity getDemographicDataByZanId(DemographicDataRequestPayload payload) {
        DemographicDataEntity demographicDataEntity = getDemographicDataByZanId(payload.getZanid());
        if (Objects.isNull(demographicDataEntity)) {
            log.error("No Demographic Data found in our Local Repository for ZanID {} the request is redirected to zcsra", payload.getZanid());
            return getDemographicDataDirectFromZCSRA(payload);
        }
        return demographicDataEntity;
    }

    @Override
    public DemographicDataEntity getDemographicDataByNames(Names names) {
        return demographicDataRepository
                .findByPrsnFirstNameAndPrsnLastName(
                        names.getFirstName(),  names.getLastName());
    }

    @Override
    public DemographicDataEntity getDemographicDataDirectFromZCSRA(DemographicDataRequestPayload payload){
        RestTemplate template = new RestTemplate();
        SignedDemographicDataRequestPayload signedPayload = getSignedDemographicDataRequestPayload(payload);
        String finalPayload = Mapper.toJson(signedPayload);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> request = new HttpEntity<>(finalPayload, headers);
        ResponseEntity<DemographicResponse> response = template.postForEntity(sysenv.ZCSRA_API_BASE_URL_DEMOGRAPHIC,
                request, DemographicResponse.class);
        log.info("Response from ZCSRA: {}", response);
        if (response.getStatusCode() == HttpStatus.OK){
            ResponseData data = Objects.requireNonNull(response.getBody()).getPayload().getData();
            log.info("Response Data: {}", data);
            DemographicDataEntity demographicDataEntity = new DemographicDataEntity();
            demographicDataEntity.setResponseId(data.getResponseId());
            demographicDataEntity.setPrsnId(data.getPrsnId());
            demographicDataEntity.setPrsnFirstName(data.getPrsnFirstName());
            demographicDataEntity.setPrsnLastName(data.getPrsnLastName());
            demographicDataEntity.setPrsnSex(data.getPrsnSex());
            demographicDataEntity.setPrsnNationalityInd(data.getPrsnNationalityInd());
            demographicDataEntity.setPrsnBirthPlace(data.getPrsnBirthPlace());
            demographicDataEntity.setPrsnBirthNationality(data.getPrsnBirthNationality());
            demographicDataEntity.setPrsnEyeColor(data.getPrsnEyeColor());
            demographicDataEntity.setPrsnProofOfBirthType(data.getPrsnProofOfBirthType());
            demographicDataEntity.setPrsnOccupation(data.getPrsnOccupation());
            demographicDataEntity.setPrsnResAddress(data.getPrsnResAddress());
            demographicDataEntity.setPrsnResDistrict(data.getPrsnResDistrict());
            demographicDataEntity.setPrsnResWard(data.getPrsnResWard());
            demographicDataEntity.setPrsnResTownVillage(data.getPrsnResTownVillage());
            demographicDataEntity.setPrsnResHousePlot(data.getPrsnResHousePlot());
            demographicDataEntity.setPrsnPostAddress(data.getPrsnPostAddress());
            demographicDataEntity.setPrsnPostCode(data.getPrsnPostCode());
            demographicDataEntity.setPrsnPhoto(data.getPrsnPhoto());
            demographicDataEntity.setPrsnKinType(data.getPrsnKinType());
            demographicDataEntity.setPrsnKinName(data.getPrsnKinName());
            demographicDataEntity.setPrsnKinPhone(data.getPrsnKinPhone());
            demographicDataEntity.setPrsnKinAddress(data.getPrsnKinAddress());
            demographicDataEntity.setPrsnSpouseId(data.getPrsnSpouseId());
            demographicDataEntity.setPrsnSpouseFullName(data.getPrsnSpouseFullName());
            demographicDataEntity.setPrsnFatherFullName(data.getPrsnFatherFullName());
            demographicDataEntity.setPrsnMotherFullName(data.getPrsnMotherFullName());
            demographicDataEntity.setPrsnFatherNationality(data.getPrsnFatherNationality());
            demographicDataEntity.setPrsnMotherNationality(data.getPrsnMotherNationality());
            demographicDataEntity.setPrsnFatherBirthNationality(data.getPrsnFatherBirthNationality());
            demographicDataEntity.setPrsnMotherBirthNationality(data.getPrsnMotherBirthNationality());
            demographicDataEntity.setPrsnFatherBirthPlace(data.getPrsnFatherBirthPlace());
            demographicDataEntity.setPrsnMotherBirthPlace(data.getPrsnMotherBirthPlace());
            demographicDataEntity.setPrsnFatherAlive(data.isPrsnFatherAlive());
            demographicDataEntity.setPrsnMotherAlive(data.isPrsnMotherAlive());
            demographicDataEntity.setPrsnFeatures(data.getPrsnFeatures());
            demographicDataEntity.setPrsnMaritalStatus(data.getPrsnMaritalStatus());
            demographicDataEntity.setPrsnLivingTimeInZan(data.getPrsnLivingTimeInZan());
            demographicDataEntity.setPrsnPostCountry(data.getPrsnPostCountry());
            demographicDataEntity.setPrsnPersonIdNum(data.getPrsnPersonIdNum());
            demographicDataEntity.setPrsnEmails(data.getPrsnEmails());
            demographicDataEntity.setPrsnPlaceShehia(data.getPrsnPlaceShehia());
            demographicDataEntity.setPrsnSignature(data.getPrsnSignature());
            log.info("DemographicDataEntity to be Save: {}", demographicDataEntity);
            return demographicDataRepository.save(demographicDataEntity);

        }
        return null;
    }

    @Override
    public DemographicDataEntity getDemographicDataFromZCSRAByBiometric(BiometricRequestPayload payload) {
        RestTemplate template = new RestTemplate();
        ResponseEntity<DemographicDataEntity> response = template.postForEntity("url", payload, DemographicDataEntity.class);
        return response.getBody();
    }

    @Override
    public byte[] getDemographicDataInPdfForm(DemographicDataEntity payload) {
        Document document = new Document();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            PdfWriter.getInstance(document, outputStream);
            document.open();

            // Define fonts and colors
            Font headerFont = new Font(Font.TIMES_ROMAN, 14, Font.BOLD, Color.WHITE);
            Font titleFont = new Font(Font.TIMES_ROMAN, 12, Font.BOLD);
            Font normalFont = new Font(Font.TIMES_ROMAN, 10);
            Font boldFont = new Font(Font.TIMES_ROMAN, 10, Font.BOLD);
            Color brandColor = new Color(43, 99, 49); // Brand color for headers
            Color sectionColor = new Color(20, 181, 217); // Section background color

            // Add Header Table (Logo on the left, Signature and Photo on the right)
            PdfPTable headerTable = new PdfPTable(2);
            headerTable.setWidthPercentage(100);

            // Company Logo on the left
            PdfPCell logoCell = new PdfPCell();
            logoCell.setBorder(PdfPCell.NO_BORDER);
            String logoPath = "src/main/resources/static/assets/images/tcb.jpg";
            Image logo = Image.getInstance(logoPath);
            logo.scaleToFit(100, 50);
            logoCell.addElement(logo);
            headerTable.addCell(logoCell);

            // Signature and Photo on the right
            PdfPCell imageCell = new PdfPCell();
            imageCell.setBorder(PdfPCell.NO_BORDER);
            if (payload.getPrsnPhoto() != null) {
                Image photo = Image.getInstance(Base64.getDecoder().decode(payload.getPrsnPhoto()));
                photo.scaleToFit(100, 100);
                imageCell.addElement(photo);
            }
            if (payload.getPrsnSignature() != null) {
                Image signature = Image.getInstance(Base64.getDecoder().decode(payload.getPrsnSignature()));
                signature.scaleToFit(100, 30);
                imageCell.addElement(signature);
            }
            headerTable.addCell(imageCell);
            document.add(headerTable);
            document.add(Chunk.NEWLINE);
            PdfPCell titleCell = new PdfPCell(new Phrase("ZCSRA DEMOGRAPHIC DATA", headerFont));
            titleCell.setBackgroundColor(brandColor);
            titleCell.setPadding(10);
            titleCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            titleCell.setBorder(PdfPCell.NO_BORDER);
            PdfPTable titleTable = new PdfPTable(1);
            titleTable.setWidthPercentage(100);
            titleTable.addCell(titleCell);
            document.add(titleTable);

            document.add(Chunk.NEWLINE);
            personalInformationSection(document,payload, titleFont, boldFont, normalFont, sectionColor);
            document.add(Chunk.NEWLINE);
            residentialInformationSection(document, "Residential Information", payload, titleFont, boldFont, normalFont, sectionColor, brandColor);
            document.add(Chunk.NEWLINE);
            familyInformationSection(document, "Family Information", payload, titleFont, boldFont, normalFont, sectionColor, brandColor);
            document.add(Chunk.NEWLINE);
            additionalInformationSection(document, payload, titleFont, boldFont, normalFont, sectionColor, brandColor);
            document.close();
            return outputStream.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
            log.error(e.getMessage());
        }
        return null;
    }
    private void additionalInformationSection(Document document, DemographicDataEntity payload, Font titleFont, Font boldFont, Font normalFont, Color sectionColor, Color brandColor) throws DocumentException {
        // Add section title with background
        PdfPCell sectionTitleCell = new PdfPCell(new Phrase("Additional Information", titleFont));
        sectionTitleCell.setBackgroundColor(sectionColor);
        sectionTitleCell.setPadding(10);
        sectionTitleCell.setBorder(PdfPCell.NO_BORDER);
        PdfPTable sectionTitleTable = new PdfPTable(1);
        sectionTitleTable.setWidthPercentage(100);
        sectionTitleTable.addCell(sectionTitleCell);
        document.add(sectionTitleTable);

        // Add data table with two rows (Header + Values)
        PdfPTable dataTable = new PdfPTable(2);
        dataTable.setWidthPercentage(100);
        // Additional Information
        dataTable.addCell(pdfUtil.createHeaderCell("Occupation", boldFont));
        dataTable.addCell(createValueCell(getValueOrDefault(payload.getPrsnOccupation()), normalFont));
        dataTable.addCell(createHeaderCell("Email", boldFont));
        dataTable.addCell(createValueCell(getValueOrDefault(payload.getPrsnEmails()), normalFont));
        dataTable.addCell(createHeaderCell("Nationality Indicator", boldFont));
        dataTable.addCell(createValueCell(getValueOrDefault(payload.getPrsnNationalityInd()), normalFont));
        dataTable.addCell(createHeaderCell("Marital Status", boldFont));
        dataTable.addCell(createValueCell(getValueOrDefault(payload.getPrsnMaritalStatus()), normalFont));
        dataTable.addCell(createHeaderCell("Kin Name", boldFont));
        dataTable.addCell(createValueCell(getValueOrDefault(payload.getPrsnKinName()), normalFont));
        dataTable.addCell(createHeaderCell("Kin Address", boldFont));
        dataTable.addCell(createValueCell(getValueOrDefault(payload.getPrsnKinAddress()), normalFont));
        dataTable.addCell(createHeaderCell("Kin Phone", boldFont));
        dataTable.addCell(createValueCell(getValueOrDefault(payload.getPrsnKinPhone()), normalFont));
        document.add(dataTable);
    }

    private void familyInformationSection(Document document, String sectionTitle, DemographicDataEntity payload, Font titleFont, Font boldFont, Font normalFont, Color sectionColor, Color brandColor) throws DocumentException {
        // Add section title with background
        PdfPCell sectionTitleCell = new PdfPCell(new Phrase(sectionTitle, titleFont));
        sectionTitleCell.setBackgroundColor(sectionColor);
        sectionTitleCell.setPadding(10);
        sectionTitleCell.setBorder(PdfPCell.NO_BORDER);
        PdfPTable sectionTitleTable = new PdfPTable(1);
        sectionTitleTable.setWidthPercentage(100);
        sectionTitleTable.addCell(sectionTitleCell);
        document.add(sectionTitleTable);

        // Add data table with two rows (Header + Values)
        PdfPTable dataTable = new PdfPTable(6);
        dataTable.setWidthPercentage(100);

        // Family Information
        dataTable.addCell(createHeaderCell("Father's Name", boldFont));
        dataTable.addCell(createValueCell(getValueOrDefault(payload.getPrsnFatherFullName()), normalFont));
        dataTable.addCell(createHeaderCell("Father's Nationality", boldFont));
        dataTable.addCell(createValueCell(getValueOrDefault(payload.getPrsnFatherNationality()), normalFont));
        dataTable.addCell(createHeaderCell("Father's Birth Place", boldFont));
        dataTable.addCell(createValueCell(getValueOrDefault(payload.getPrsnFatherBirthPlace()), normalFont));
        dataTable.addCell(createHeaderCell("Mother's Name", boldFont));
        dataTable.addCell(createValueCell(getValueOrDefault(payload.getPrsnMotherFullName()), normalFont));
        dataTable.addCell(createHeaderCell("Mother's Nationality", boldFont));
        dataTable.addCell(createValueCell(getValueOrDefault(payload.getPrsnMotherNationality()), normalFont));
        dataTable.addCell(createHeaderCell("Mother's Birth Place", boldFont));
        dataTable.addCell(createValueCell(getValueOrDefault(payload.getPrsnMotherBirthPlace()), normalFont));
        document.add(dataTable);
    }

    private void residentialInformationSection(Document document, String sectionTitle, DemographicDataEntity payload, Font titleFont, Font boldFont, Font normalFont, Color sectionColor, Color brandColor) throws DocumentException {
        // Add section title with background
        PdfPCell sectionTitleCell = new PdfPCell(new Phrase(sectionTitle, titleFont));
        sectionTitleCell.setBackgroundColor(sectionColor);
        sectionTitleCell.setPadding(10);
        sectionTitleCell.setBorder(PdfPCell.NO_BORDER);
        PdfPTable sectionTitleTable = new PdfPTable(1);
        sectionTitleTable.setWidthPercentage(100);
        sectionTitleTable.addCell(sectionTitleCell);
        document.add(sectionTitleTable);

        // Add data table with two rows (Header + Values)
        PdfPTable dataTable = new PdfPTable(8);
        dataTable.setWidthPercentage(100);

        // Residential Information
        dataTable.addCell(createHeaderCell("Residential Address", boldFont));
        dataTable.addCell(createValueCell(getValueOrDefault(payload.getPrsnResAddress()), normalFont));
        dataTable.addCell(createHeaderCell("District", boldFont));
        dataTable.addCell(createValueCell(getValueOrDefault(payload.getPrsnResDistrict()), normalFont));
        dataTable.addCell(createHeaderCell("Ward", boldFont));
        dataTable.addCell(createValueCell(getValueOrDefault(payload.getPrsnResWard()), normalFont));
        dataTable.addCell(createHeaderCell("Town/Village", boldFont));
        dataTable.addCell(createValueCell(getValueOrDefault(payload.getPrsnResTownVillage()), normalFont));
        dataTable.addCell(createHeaderCell("House Plot", boldFont));
        dataTable.addCell(createValueCell(getValueOrDefault(payload.getPrsnResHousePlot()), normalFont));
        dataTable.addCell(createHeaderCell("Postal Address", boldFont));
        dataTable.addCell(createValueCell(getValueOrDefault(payload.getPrsnPostCode()), normalFont));
        dataTable.addCell(createHeaderCell("Post Code", boldFont));
        dataTable.addCell(createValueCell(getValueOrDefault(payload.getPrsnPostAddress()), normalFont));
        dataTable.addCell(createHeaderCell("Post Country", boldFont));
        dataTable.addCell(createValueCell(getValueOrDefault(payload.getPrsnPostCountry()), normalFont));
        document.add(dataTable);
    }

    private void personalInformationSection(Document document, DemographicDataEntity payload, Font titleFont, Font boldFont, Font normalFont, Color sectionColor) throws DocumentException {
        // Add section title with background
        document.add(Chunk.NEWLINE);
        PdfPCell sectionTitleCell = new PdfPCell(new Phrase("Personal Information", titleFont));
        sectionTitleCell.setBackgroundColor(sectionColor);
        sectionTitleCell.setPadding(10);
        sectionTitleCell.setBorder(PdfPCell.NO_BORDER);
        PdfPTable sectionTitleTable = new PdfPTable(1);
        sectionTitleTable.setWidthPercentage(100);
        sectionTitleTable.addCell(sectionTitleCell);
        document.add(sectionTitleTable);
        document.add(Chunk.NEWLINE);
        // Add data table with two rows (Header + Values)
        PdfPTable dataTable = new PdfPTable(2);
        dataTable.setWidthPercentage(100);

        // Header Row
        dataTable.addCell(createHeaderCell("Zan ID", boldFont));
        dataTable.addCell(createValueCell(getValueOrDefault(String.valueOf(payload.getPrsnId())), normalFont));
        dataTable.addCell(createHeaderCell("First Name", boldFont));
        dataTable.addCell(createValueCell(getValueOrDefault(payload.getPrsnFirstName()), normalFont));
        dataTable.addCell(createHeaderCell("Last Name", boldFont));
        dataTable.addCell(createValueCell(getValueOrDefault(payload.getPrsnLastName()), normalFont));
        dataTable.addCell(createHeaderCell("Sex", boldFont));
        dataTable.addCell(createValueCell(getValueOrDefault(payload.getPrsnSex()), normalFont));
        dataTable.addCell(createHeaderCell("Birth Date", boldFont));
        dataTable.addCell(createValueCell(getValueOrDefault(payload.getPrsnBirthDate()), normalFont));
        dataTable.addCell(createHeaderCell("Birth Place", boldFont));
        dataTable.addCell(createValueCell(getValueOrDefault(payload.getPrsnBirthPlace()), normalFont));
        dataTable.addCell(createHeaderCell("Birth Nationality", boldFont));
        dataTable.addCell(createValueCell(getValueOrDefault(payload.getPrsnBirthNationality()), normalFont));
        dataTable.addCell(createHeaderCell("Eye Color", boldFont));
        dataTable.addCell(createValueCell(getValueOrDefault(payload.getPrsnEyeColor()), normalFont));
        document.add(dataTable);
    }
    private PdfPCell createHeaderCell(String content, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(content, font));
        cell.setPadding(5);
        return cell;
    }
private PdfPCell createHeaderCell(String content, Font font, Color bgColor) {
        PdfPCell cell = new PdfPCell(new Phrase(content, font));
        cell.setBackgroundColor(bgColor);
        cell.setPadding(5);
        return cell;
    }

    private PdfPCell createValueCell(String content, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(content, font));
        cell.setPadding(5);
        return cell;
    }

    private String getValueOrDefault(String value){
        return value == null ? "N/A" : value;
    }

    @Override
    public SignedDemographicDataRequestPayload getSignedDemographicDataRequestPayload(DemographicDataRequestPayload payload) {
        SignedDemographicDataRequestPayload signedPayload = new SignedDemographicDataRequestPayload();
        signedPayload.setPayload(payload);
        String strPayload = Mapper.toJson(payload);
        log.info("Signed Payload That is not used: {}", strPayload);
        PrivateKey privateKey = EncryptUtil.loadPrivateKey(sysenv.ZCSRA_PRIVATE_KEY_PATH,
                sysenv.ZCSRA_PRIVATE_KEY_ALIAS, sysenv.ZCSRA_PRIVATE_KEY_PASS);
        String signature = EncryptUtil.signZCSRAPayload(strPayload, privateKey);
        log.info("Signature: {}", signature);
        signedPayload.setSignature(signature);
//        PublicKey pk = EncryptUtil.loadPublicKey(sysenv.ZCSRA_PUBLIC_KEY_PATH, sysenv.ZCSRA_PUBLIC_KEY_ALIAS, sysenv.ZCSRA_PUBLIC_KEY_PASS);
//        if (pk != null) {
//            if (EncryptUtil.verifyZCSRASignature(str, signature, pk)) {
//                signedPayload.setSignature(signature);
//            }
//        }
//        log.info("Signature produced: {}", signature);
//        if (signature == null) {
//            log.error("Failed to get the signature from the certificate");
//            return null;
//        }
//        Objects.requireNonNull(signedPayload).setSignature(signature);
//        log.info("Signed Payload: {}", signedPayload);

        return signedPayload;
    }

    @Override
    public DemographicDataEntity getDemographicDataByZanId(String zanId) {
        return demographicDataRepository.findByPrsnId(Integer.parseInt(zanId));
    }
}
