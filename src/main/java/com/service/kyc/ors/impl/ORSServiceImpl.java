package com.service.kyc.ors.impl;

import com.DTO.KYC.ors.*;
import com.DTO.KYC.ors.response.*;
import com.config.SYSENV;
import com.dao.kyc.response.ors.AllResponseDTO;
import com.dao.kyc.response.ors.AttachmentResponseDTO;
import com.dao.kyc.response.ors.ClassifierResponseDTO;
import com.dao.kyc.response.ors.ResponseDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.helper.Mapper;
import com.helper.ORSApiEndpoints;
import com.helper.PdfUtil;
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.kernel.pdf.canvas.draw.SolidLine;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.property.TextAlignment;
import com.itextpdf.layout.property.UnitValue;
import com.itextpdf.layout.property.VerticalAlignment;
import com.lowagie.text.BadElementException;
import com.models.kyc.ors.*;
import com.repository.Kyc.ors.*;
import com.service.kyc.ors.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.Field;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ORSServiceImpl implements ORSService {
    private final RestTemplate restTemplate;
    private final ORSApiEndpoints apiEndpoints;
    private final ObjectMapper mapper;
    private final CompanyService companyService;
    private final ClassifierService classifierService;
    private final AttachmentService attachmentService;
    private final CMDetailedInfoRepository cmDetailedInfoRepository;
    private final DirectorEntityRepository directorEntityRepository;
    private final SYSENV sysenv;
    private final PdfUtil pdfUtil;
    private final ResponseRepository responseRepository;
    private final ShareholderRepository shareholderRepository;
    private final IssuedShareCapitalRepository issuedShareCapitalRepository;
    private final GroupCompanySecretaryRepository groupCompanySecretaryRepository;
    private final GroupCompanyRepository groupCompanyRepository;
    private final GroupShareCapitalRepository groupShareCapitalRepository;
    private final GroupRegOfficeRepository groupRegOfficeRepository;
    private final ShareholderSharesRepository shareholderSharesRepository;
    private final CompanyRepository companyRepository;
    private final BusinessActivityRepository businessActivityRepository;
    private final AttachmentRepository attachmentRepository;

    @Override
    public FullResponse getEntityInfoByType(PayloadDTO payloadDTO) {
        payloadDTO.setApiKey(apiEndpoints.getApiKye());
        String url =apiEndpoints.getBaseUrl() + apiEndpoints.getEntity();
        String payload = Mapper.toJsonJackson(payloadDTO);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(payload, headers);
        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            log.info("Response: Status={}, Headers={}, Body={}", response.getStatusCode(), response.getHeaders(), response.getBody());
            if (response.getBody() != null) {

                return mapper.readValue(response.getBody(), FullResponse.class);
            } else {
                log.error("Received empty response body from the ORS API");
                return null;
            }
        } catch (Exception e) {
            log.error("getEntityInfoByType exception: {}", e.getMessage(), e);
            return null;
        }
    }

    @Override
    public List<ClassifierData> getClassifiedList(String apiKey) {
        ParameterizedTypeReference<List<ClassifierData>> typeRef = new ParameterizedTypeReference<List<ClassifierData>>() {};
        ResponseEntity<List<ClassifierData>> response = restTemplate.exchange(
                apiEndpoints.getClassifierList(),
                HttpMethod.POST,
                new HttpEntity<>(apiKey), typeRef);
        return response.getBody();
    }

    @Override
    public ClassifierData getClassifier(ClassifierPayload payload) {
        return restTemplate.postForObject(apiEndpoints.getClassifier(),
                payload, ClassifierData.class);
    }

    @Override
    public List<AttachmentData> getAttachmentList(PayloadDTO payloadDTO) {
        ParameterizedTypeReference<List<AttachmentData>> typeRef = new ParameterizedTypeReference<List<AttachmentData>>() {};
        try{
            ResponseEntity<List<AttachmentData>> response =
                    restTemplate.exchange(apiEndpoints.getBaseUrl()+ apiEndpoints.getAttachmentList(),
                            HttpMethod.POST, new HttpEntity<>(payloadDTO),typeRef);
            return response.getBody();

        }catch (Exception e){
            log.error("getAttachmentList exception: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public AttachmentAccessData getAttachmentAccess(AttachmentPayload payload) {

        return restTemplate.postForObject(apiEndpoints.getAttachment(),
                payload, AttachmentAccessData.class);
    }

    @Override
    public AllResponseDTO<AttachmentResponseDTO<AttachmentData>, ClassifierResponseDTO<ClassifierData>, ResponseDTO<CompanyData>> findCompiledResponse(PayloadDTO payloadDTO) {
        AllResponseDTO<AttachmentResponseDTO<AttachmentData>, ClassifierResponseDTO<ClassifierData>, ResponseDTO<CompanyData>> allResponseDTO
                = new AllResponseDTO<>();
        Optional<ORSResponseEntity> data = responseRepository.findByCertificate(String.valueOf(payloadDTO.getRegistrationNumber()));
        if (data.isPresent()) {
            String companyJson = Mapper.toJsonJackson(data);
            ResponseDTO<CompanyData> companyData = ResponseDTO.fromJson(companyJson, CompanyData.class);
            allResponseDTO.setEntity(companyData);
            String attachmentJson = Mapper.toJsonJackson(attachmentService.findAttachmentListFromDB(payloadDTO));
            AttachmentResponseDTO<AttachmentData> attachments = AttachmentResponseDTO.fromJson(attachmentJson, AttachmentData.class);
            allResponseDTO.setAttachments(attachments);
        }else {
            ORSResponseEntity entity = companyService.findEntityDetailsFromORS(payloadDTO);
            String companyJson = Mapper.toJsonJackson(entity);
            ResponseDTO<CompanyData> responseDTO = ResponseDTO.fromJson(companyJson, CompanyData.class);
            allResponseDTO.setEntity(responseDTO);
            String attachmentJson = Mapper.toJsonJackson(attachmentService.findAttachmentListFromORS(payloadDTO));
            AttachmentResponseDTO<AttachmentData> attachments = AttachmentResponseDTO.fromJson(attachmentJson, AttachmentData.class);
            allResponseDTO.setAttachments(attachments);
            String classifierJson = Mapper.toJsonJackson(classifierService.findClassifiersFromORS(payloadDTO));
            ClassifierResponseDTO<ClassifierData> classifiers = ClassifierResponseDTO.fromJson(classifierJson, ClassifierData.class);
            allResponseDTO.setClassifiers(classifiers);
        }

        return allResponseDTO;
    }

    @Override
    public byte[] getEntityDetailsInPdf(CompanyData companyData) {
        return null;
    }

    @Override
    public String convertToPdfAndSave(CompanyData companyData) {
        String attachmentUrl = apiEndpoints.getAttachmentServerUrl()+apiEndpoints.getAttachmentSaveEndpoint();
        try {
            PdfFont timesNewRoman = PdfFontFactory.createFont(StandardFonts.TIMES_ROMAN);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            com.itextpdf.kernel.pdf.PdfWriter writer = new com.itextpdf.kernel.pdf.PdfWriter(outputStream);
            PdfDocument pdfDocument = new PdfDocument(writer);
            com.itextpdf.layout.Document document = new com.itextpdf.layout.Document(pdfDocument);

            float rightPadding = 40;
            float bottomMargin = 40;
            float leftMargin = 40;
            float headerHeight = 40; // Space for header including logos

            // Set Document Margins (content starts after header)
            document.setMargins(headerHeight, rightPadding, bottomMargin, leftMargin);

            // Load logos (unchanged)
            String tcbLogoPath = sysenv.TCB_LOGO_PATH;
            String brelaLogoPath = sysenv.BRELA_LOGO_PATH;
            Image tcbLogo = new Image(ImageDataFactory.create(tcbLogoPath)).scaleToFit(100, 50);
            Image brelaLogo = new Image(ImageDataFactory.create(brelaLogoPath)).scaleToFit(100, 50);

            // Create a Header Table
            com.itextpdf.layout.element.Table headerTable = new com.itextpdf.layout.element.Table(UnitValue.createPercentArray(new float[]{1, 1})).useAllAvailableWidth();
            headerTable.setBorder(Border.NO_BORDER);

            // Left Cell: TCB Logo
            com.itextpdf.layout.element.Cell leftCell = new com.itextpdf.layout.element.Cell().add(tcbLogo)
                    .setBorder(Border.NO_BORDER)
                    .setVerticalAlignment(VerticalAlignment.MIDDLE)
                    .setTextAlignment(TextAlignment.LEFT);
            headerTable.addCell(leftCell);

            // Right Cell: Brela Logo
            com.itextpdf.layout.element.Cell rightCell = new com.itextpdf.layout.element.Cell().add(brelaLogo)
                    .setBorder(Border.NO_BORDER)
                    .setVerticalAlignment(VerticalAlignment.MIDDLE)
                    .setTextAlignment(TextAlignment.RIGHT);
            headerTable.addCell(rightCell);

            // Add Header Table to Document
            document.add(headerTable);

            // Add a horizontal separator line below the header
            document.add(new LineSeparator(new SolidLine()).setMarginBottom(10));

            // Example Content
            document.add(new Paragraph(companyData.getLegalName()+" "+ companyData.getCertNumber())
                    .setFontSize(14)
                    .setBold()
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(10));

            // Add some spacing
            document.add(new Paragraph("\n"));

            // Add Company Information

            document.add(new Paragraph("Company Name: " + companyData.getLegalName()).setBold());
            document.add(new Paragraph("Registration Number: " + companyData.getCertNumber()));
            document.add(new Paragraph("\n")); // Add some space

            // Add Directors Table
            List<DirectorData> directorData = companyData.getCMDetailedInfo().getDirectors();
            if (directorData != null && !directorData.isEmpty()) {
                document.add(new Paragraph("Directors").setBold().setFontSize(10).setFont(timesNewRoman));
                document.add(createTable(directorData,timesNewRoman));
                document.add(new Paragraph("\n"));
            }

            // Add Shareholders Table
            if (directorData != null && !directorData.isEmpty()) {
                document.add(new Paragraph("Shareholders").setBold().setFontSize(14));
                document.add(createTable(directorData,timesNewRoman));
                document.add(new Paragraph("\n"));
            }

            // Add Activities Table
            if (directorData != null && !directorData.isEmpty()) {
                document.add(new Paragraph("Company Activities").setBold().setFontSize(14));
                document.add(createTable(directorData,timesNewRoman));
                document.add(new Paragraph("\n"));
            }

            // Draw Border Around Page
            PdfCanvas canvas = new PdfCanvas(pdfDocument.getFirstPage());
            canvas.setStrokeColor(ColorConstants.BLUE)
                    .setLineWidth(2)
                    .rectangle(leftMargin, bottomMargin, pdfDocument.getDefaultPageSize().getWidth() - (leftMargin + rightPadding),
                            pdfDocument.getDefaultPageSize().getHeight() - (bottomMargin + headerHeight))
                    .stroke();

            document.close();
            String content =Base64.getEncoder().encodeToString(outputStream.toByteArray());
            AttachmentAccessDTO accessDTO = new AttachmentAccessDTO();
            accessDTO.setContent(content);
            accessDTO.setFilename(companyData.getLegalName()+"-"+companyData.getCertNumber()+".pdf");
            accessDTO.setSize(12);
            accessDTO.setProjectName("");
            accessDTO.setEntityRegNo(companyData.getCertNumber());
            accessDTO.setProjectCode("");
            accessDTO.setContentType("application/pdf");
            HttpEntity<String> attachmentEntity = apiEndpoints.attachmentEntity(accessDTO);
            ResponseEntity<String> response2 = restTemplate.postForEntity(attachmentUrl, attachmentEntity, String.class);
            return response2.getBody();
        } catch (Exception e) {
            log.error("Error while converting data to pdf {} ",e.getMessage());
        }
        return "";
    }

    @Override
    public Map<String, Object> findEntityDetails(PayloadDTO payloadDTO) {
        Map<String, Object> data = findCompiledResponse(payloadDTO.getRegistrationNumber());
        // Check if any of the critical data fields are missing
        if (data.get("response") != null && data.get("info") != null && data.get("directors") != null &&
                data.get("shareholders") != null && data.get("shareCapital") != null && data.get("secretary") != null &&
                data.get("shareholderShares") != null) {
            return data;
        }
        HttpEntity<String> entity = apiEndpoints.createEntity(payloadDTO);
        ResponseEntity<String> response = apiEndpoints.sendRequest(entity,restTemplate, apiEndpoints.getBaseUrl(), apiEndpoints.getEntity());
        ResponseEntity<String> resAtt = apiEndpoints.sendRequest(entity, restTemplate, apiEndpoints.getBaseUrl(), apiEndpoints.getAttachmentList());

        return save(response.getBody(), resAtt.getBody(),payloadDTO);

    }

    @Override
    public CompanyData getEntityInfo(String certNo) {
        return null;
    }

    /**
     * Creates a table dynamically from a list of objects.
     * Uses reflection to extract field names and values.
     */
    private <T> Table createTable(List<T> dataList, PdfFont font) throws BadElementException {
        if (dataList == null || dataList.isEmpty()) {
            return new Table(1).addCell(new Cell().add(new Paragraph("No data available").setTextAlignment(TextAlignment.CENTER)).setFont(font).setFontSize(9));
        }

        // Use reflection to get field names
        Field[] fields = dataList.get(0).getClass().getDeclaredFields();
        Table table = new Table(fields.length);
        table.setWidth(UnitValue.createPercentValue(100));

        // Add table headers
        for (Field field : fields) {
            table.addHeaderCell(new Cell().add(new Paragraph(field.getName().toUpperCase()).setBold().setBackgroundColor(ColorConstants.LIGHT_GRAY).setFont(font)).setFontSize(10));
        }

        // Add table rows
        for (T item : dataList) {
//            field.getName().toUpperCase()).setBold().setBackgroundColor(ColorConstants.LIGHT_GRAY)
//            value != null ? value.toString() : "").setTextAlignment(TextAlignment.LEFT)
            for (Field field : fields) {
                field.setAccessible(true);
                try {
                    Object value = field.get(item);
                    table.addCell(new Cell().add(new Paragraph(value != null ? value.toString() : "").setTextAlignment(TextAlignment.LEFT).setFont(font)).setFontSize(10));
                } catch (IllegalAccessException e) {
                    table.addCell(new Cell().add(new Paragraph("N/A").setFont(font).setFontSize(10)));
                }
            }
        }

        return table;
    }


    private Map<String, Object> findCompiledResponse(Long cert){
        Optional<ORSResponseEntity> orsResponse = responseRepository.findByCertificate(String.valueOf(cert));
        Optional<CompanyEntity> company = companyRepository.findCompanyEntityByCertNumber(String.valueOf(cert));
        Optional<CMDetailedInfoEntity> info = cmDetailedInfoRepository.findByCertificate(String.valueOf(cert));
        List<DirectorEntity> directors = directorEntityRepository.findByCertificate(String.valueOf(cert));
        List<ShareholderEntity> shareholders = shareholderRepository.findByCertificate(String.valueOf(cert));
        Optional<IssuedShareCapitalEntity> shareCapital = issuedShareCapitalRepository.findByCertificate(String.valueOf(cert));
        Optional<GroupCompanySecretaryEntity> secretary = groupCompanySecretaryRepository.findByCertificate(String.valueOf(cert));
        List<ShareholderSharesEntity> shareholderShares = shareholderSharesRepository.findByCertificate(String.valueOf(cert));
        Optional<GroupRegOfficeEntity> groupRegOffice = groupRegOfficeRepository.findByCertificate(String.valueOf(cert));
        Optional<GroupCompanyEntity> groupCompany = groupCompanyRepository.findByCertificate(String.valueOf(cert));
        List<BusinessActivityEntity> activities = businessActivityRepository.findByCertificate(String.valueOf(cert));
        Optional<GroupShareCapitalEntity> groupShareCapital = groupShareCapitalRepository.findByCertificate(String.valueOf(cert));
        List<AttachmentEntity> attachmentEntities = attachmentRepository.findAttachmentEntitiesByRegistrationNumber(String.valueOf(cert));
        Map<String, Object> response = new HashMap<>();
        response.put("response",orsResponse.orElse(null));
        response.put("company",company.orElse(null));
        response.put("info", info.orElse(null));
        response.put("directors", directors.isEmpty() ? null : directors);
        response.put("shareholders", shareholders.isEmpty() ? null : shareholders );
        response.put("shareholderShares", shareholderShares.isEmpty() ? null : shareholderShares);
        response.put("shareCapital", shareCapital.orElse(null));
        response.put("activities", activities.isEmpty() ? null : activities);
        response.put("secretary", secretary.orElse(null));
        response.put("groupShareCapital", groupShareCapital.orElse(null));
        response.put("groupRegOffice", groupRegOffice.orElse(null));
        response.put("groupCompany", groupCompany.orElse(null));
        response.put("attachments", attachmentEntities.isEmpty() ? null : attachmentEntities);
        return response;
    }

    private Map<String, Object> save(String dataToSave, String attachmentToSave, PayloadDTO payloadDTO) {
        Map<String, Object> result = new HashMap<>();
        String attachmentUrl = apiEndpoints.getAttachmentServerUrl()+apiEndpoints.getAttachmentSaveEndpoint();
        try {
            ResponseDTO<CompanyData> converted =ResponseDTO.fromJson(dataToSave, CompanyData.class);
            ResponseDTO<List<AttachmentData>> convertedAttachment =ResponseDTO.fromJsonAsList(attachmentToSave, AttachmentData.class);
            if (converted == null || converted.getData() == null || !converted.isSuccess()) {
                result.put("status", "ERROR");
                result.put("message", converted != null ? converted.getMessage() : "Unknown Error Occurred while fetching Data from Brela");
                result.put("resultCode", converted != null ? converted.getResultCode() : -1);
                return result;
            }
            if (convertedAttachment==null || convertedAttachment.getData()==null || !convertedAttachment.isSuccess() || convertedAttachment.getData().isEmpty()) {
                result.put("status", "ERROR");
                result.put("Message", "Failed to fetch Attachments from Brela");
                result.put("resultCode", -1);
                return result;
            }
            AttachmentPayload payload = new AttachmentPayload();
            AttachmentAccessDTO accessDTO = new AttachmentAccessDTO();
            payload.setRegistrationNumber(payloadDTO.getRegistrationNumber());
            payload.setEntityType(payloadDTO.getEntityType());
            accessDTO.setEntityRegNo(converted.getData().getCertNumber());
            accessDTO.setProjectName("CILANTRO");
            accessDTO.setProjectCode("BRELA-TCB");
            List<AttachmentData> data = new ArrayList<>();
            for (AttachmentData att :convertedAttachment.getData()){
                payload.setAttachmentId(att.getAttachmentId());
                accessDTO.setFilename(att.getFileName());
                accessDTO.setThirdPartId(att.getAttachmentId());
                accessDTO.setSize(Long.parseLong(att.getFileSize() != null && !att.getFileSize().isEmpty() ? att.getFileSize() : "0"));
                accessDTO.setContentType(att.getFileType());
                HttpEntity<String> entity = apiEndpoints.createAttachmentAccessEntity(payload);
                ResponseEntity<String> access = apiEndpoints.sendRequest(entity,restTemplate, apiEndpoints.getBaseUrl(), apiEndpoints.getAttachment());
                if (access.getStatusCode().is2xxSuccessful() && access.getBody() != null) {
                    AttachmentAccessData accessData = mapper.readValue(access.getBody(), AttachmentAccessData.class);
                    if (accessData != null && accessData.isSuccess()) {
                        accessDTO.setContent(accessData.getFileContent());
                        HttpEntity<String> attachmentEntity = apiEndpoints.attachmentEntity(accessDTO);
                        ResponseEntity<String> response2 = restTemplate.postForEntity(attachmentUrl, attachmentEntity, String.class);
                        att.setLocalAttachmentId(response2.getBody());
                        data.add(att);

                    }else {
                        result.put("status", "ERROR");
                        result.put("Message", "Failed to convert fetched Attachment Access into usable format");
                        result.put("resultCode", -1);
                    }

                }else {
                    result.put("status", "ERROR");
                    result.put("Message", "Failed to fetch Attachment Access Entity from Brela");
                    result.put("resultCode", -1);

                }
            }
            ORSResponseEntity orsResponse = new ORSResponseEntity();
            orsResponse.setSuccess(converted.isSuccess());
            orsResponse.setMessage(converted.getMessage());
            orsResponse.setResultCode(converted.getResultCode());
            orsResponse.setCertificate(converted.getData().getCertNumber());
            CompanyEntity company = mapper.convertValue(converted.getData(), CompanyEntity.class);
            String localFileId = convertToPdfAndSave(converted.getData());
            company.setLocalFileId(localFileId);
            CMDetailedInfoEntity info = mapper.convertValue(converted.getData().getCMDetailedInfo(), CMDetailedInfoEntity.class);
            info.setCertificate(company.getCertNumber());
            List<DirectorEntity> directorEntities = converted.getData().getCMDetailedInfo().getDirectors().
                    stream().map(dir -> {
                        DirectorEntity director = mapper.convertValue(dir, DirectorEntity.class);
                        director.setCertificate(company.getCertNumber());
                        return director;
                    }).collect(Collectors.toList());
            List<ShareholderEntity> shareholderEntities = converted.getData().getCMDetailedInfo().getShareholders()
                    .stream().map(share-> {
                        ShareholderEntity shareholder = mapper.convertValue(share, ShareholderEntity.class);
                        shareholder.setCertificate(company.getCertNumber());
                        return shareholder;
                    }).collect(Collectors.toList());
            List<BusinessActivityEntity> activities = converted.getData().getCMDetailedInfo().getBusinessActivities()
                    .stream().map(act->{
                        BusinessActivityEntity businessActivity = mapper.convertValue(act, BusinessActivityEntity.class);
                        businessActivity.setCertificate(company.getCertNumber());
                        return businessActivity;
                    }).collect(Collectors.toList());
            GroupCompanySecretaryEntity secretary = mapper.convertValue(converted.getData().getCMDetailedInfo().getGroupCompanySecretaries(),
                    GroupCompanySecretaryEntity.class);
            secretary.setCertificate(company.getCertNumber());
            IssuedShareCapitalEntity capitalShare = mapper.convertValue(converted.getData().getCMDetailedInfo().getIssuedShareCapitals(),
                    IssuedShareCapitalEntity.class);
            capitalShare.setCertificate(company.getCertNumber());
            GroupCompanyEntity groupCompany = mapper.convertValue(converted.getData().getCMDetailedInfo().getGroupCompanyInfo(),GroupCompanyEntity.class);
            groupCompany.setCertificate(company.getCertNumber());
            GroupShareCapitalEntity groupShareCapital = mapper.convertValue(converted.getData().getCMDetailedInfo().getGroupShareCapital(),GroupShareCapitalEntity.class);
            groupShareCapital.setCertificate(company.getCertNumber());
            GroupRegOfficeEntity groupRegOffice = mapper.convertValue(converted.getData().getCMDetailedInfo().getGroupRegOffice(),GroupRegOfficeEntity.class);
            groupRegOffice.setCertificate(company.getCertNumber());
            List<ShareholderSharesEntity> shareholderShares = converted.getData().getCMDetailedInfo().getShareholderShares()
                            .stream().map(share -> {
                                ShareholderSharesEntity shareholderShare = mapper.convertValue(share, ShareholderSharesEntity.class);
                                shareholderShare.setCertificate(company.getCertNumber());
                                return shareholderShare;
                    }).collect(Collectors.toList());
            List<AttachmentEntity> attachmentEntities = data.stream()
                    .map(attach -> mapper.convertValue(attach, AttachmentEntity.class))
                    .peek(att -> att.setRegistrationNumber(company.getCertNumber()))
                    .collect(Collectors.toList());
            result.put("response", responseRepository.save(orsResponse));
            result.put("company", companyRepository.save(company));
            result.put("info", cmDetailedInfoRepository.save(info));
            result.put("directors", directorEntityRepository.saveAll(directorEntities));
            result.put("shareholders", shareholderRepository.saveAll(shareholderEntities));
            result.put("shareholderShares",shareholderSharesRepository.saveAll(shareholderShares));
            result.put("shareCapital", issuedShareCapitalRepository.save(capitalShare));
            result.put("activities",businessActivityRepository.saveAll(activities));
            result.put("secretary",groupCompanySecretaryRepository.save(secretary));
            result.put("groupCompany", groupCompanyRepository.save(groupCompany));
            result.put("groupShareCapital", groupShareCapitalRepository.save(groupShareCapital));
            result.put("groupRegOffice", groupRegOfficeRepository.save(groupRegOffice));
            result.put("attachments", attachmentRepository.saveAll(attachmentEntities));
            return result;

        } catch (Exception e) {
            log.info("Error While processing Response From Brela: {}", e.getMessage());
        }
        return result;
    }
}
