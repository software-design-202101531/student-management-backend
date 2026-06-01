package com.school.studentmanagement.report.pdf;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.school.studentmanagement.global.exception.BusinessException;
import com.school.studentmanagement.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.helper.W3CDom;
import org.springframework.stereotype.Component;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.io.ByteArrayOutputStream;
import java.util.Map;

/**
 * Thymeleaf HTML 템플릿을 렌더링한 뒤 openhtmltopdf로 PDF 바이트를 만든다.
 * - 한글은 임베드 폰트가 없으면 깨지므로 NanumGothic(OFL)을 useFont로 등록한다.
 * - Thymeleaf의 HTML5 출력(void 태그를 self-closing 하지 않음)을 openhtmltopdf의 엄격한 XML 파서가
 *   거부하므로, jsoup으로 파싱 후 W3C DOM으로 변환해 withW3cDocument로 넘긴다.
 */
@Component
@RequiredArgsConstructor
public class PdfRenderer {

    private static final String FONT_PATH = "/fonts/NanumGothic-Regular.ttf";
    private static final String FONT_FAMILY = "Nanum Gothic";

    private final SpringTemplateEngine templateEngine;

    public byte[] render(String templateName, Map<String, Object> model) {
        Context context = new Context();
        context.setVariables(model);
        String html = templateEngine.process(templateName, context);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            org.jsoup.nodes.Document jsoupDoc = Jsoup.parse(html);
            jsoupDoc.outputSettings().syntax(org.jsoup.nodes.Document.OutputSettings.Syntax.xml);
            org.w3c.dom.Document w3cDoc = new W3CDom().fromJsoup(jsoupDoc);

            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFont(() -> getClass().getResourceAsStream(FONT_PATH), FONT_FAMILY);
            builder.withW3cDocument(w3cDoc, "/");
            builder.toStream(out);
            builder.run();
            return out.toByteArray();
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.REPORT_EXPORT_FAILED);
        }
    }
}
