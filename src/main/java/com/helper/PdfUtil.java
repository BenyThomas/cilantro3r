package com.helper;

import com.lowagie.text.Font;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import org.springframework.stereotype.Component;

@Component
public class PdfUtil {
    public PdfPCell createHeaderCell(String headerText, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(headerText, font));
        cell.setPadding(5);
        return cell;
    }
    public PdfPCell createValueCell(String content, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(content, font));
        cell.setPadding(5);
        return cell;
    }
    public String getValueOrDefault(String value){
        return value == null ? "N/A" : value;
    }
}
