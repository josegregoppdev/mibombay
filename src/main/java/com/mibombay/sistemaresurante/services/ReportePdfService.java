package com.mibombay.sistemaresurante.services;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.ByteArrayOutputStream;
import java.util.Locale;
import java.util.Map;

@Service
public class ReportePdfService {

    private final TemplateEngine templateEngine;

    public ReportePdfService(TemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    public byte[] generarPdf(String templateName, Map<String, Object> datos) {
        try {
            Context ctx = new Context(Locale.forLanguageTag("es"), datos);
            String html = templateEngine.process(templateName, ctx);

            ByteArrayOutputStream os = new ByteArrayOutputStream();
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(html, "classpath:/templates/pdf/");
            builder.toStream(os);
            builder.run();

            return os.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error generando PDF: " + e.getMessage(), e);
        }
    }
}
