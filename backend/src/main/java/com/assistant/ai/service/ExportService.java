package com.assistant.ai.service;

import com.assistant.ai.model.Review;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.stereotype.Service;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class ExportService {

    public byte[] generateReviewPdf(Review review) {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);

            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                contentStream.beginText();
                contentStream.setFont(PDType1Font.HELVETICA_BOLD, 18);
                contentStream.newLineAtOffset(50, 750);
                contentStream.showText("AI Software Engineering Assistant - Code Review Report");
                contentStream.endText();

                // Draw standard horizontal line
                contentStream.setLineWidth(1f);
                contentStream.moveTo(50, 735);
                contentStream.lineTo(550, 735);
                contentStream.stroke();

                // Project and score details
                contentStream.beginText();
                contentStream.setFont(PDType1Font.HELVETICA_BOLD, 12);
                contentStream.newLineAtOffset(50, 700);
                contentStream.showText("Project ID: " + review.getProject().getId());
                contentStream.newLineAtOffset(0, -20);
                contentStream.showText("Overall Quality Score: " + review.getScore() + "/100");
                contentStream.newLineAtOffset(0, -20);
                contentStream.showText("Readability: " + review.getReadability() + " | Maintainability: " + review.getMaintainability());
                contentStream.newLineAtOffset(0, -20);
                contentStream.showText("Security: " + review.getSecurity() + " | Performance: " + review.getPerformance());
                contentStream.newLineAtOffset(0, -20);
                contentStream.showText("Architecture: " + review.getArchitecture());
                contentStream.endText();

                // Simple wrapper for comments text to fit page bounds
                contentStream.beginText();
                contentStream.setFont(PDType1Font.HELVETICA, 10);
                contentStream.newLineAtOffset(50, 580);
                contentStream.showText("Summary Report Details:");
                contentStream.newLineAtOffset(0, -20);

                String reportText = review.getReportContent();
                // Strip markdown formatting characters to prevent PDFBox errors
                String plainText = reportText.replaceAll("[*#`_-]", "");
                String[] lines = plainText.split("\\n");
                
                int yPos = 560;
                for (String line : lines) {
                    if (yPos < 80) break; // Avoid drawing off-page
                    String trimmed = line.trim();
                    if (trimmed.isEmpty()) continue;
                    
                    // Simple word wrapping for PDFBox
                    List<String> wrapped = wrapText(trimmed, 80);
                    for (String w : wrapped) {
                        contentStream.showText(w);
                        contentStream.newLineAtOffset(0, -15);
                        yPos -= 15;
                        if (yPos < 80) break;
                    }
                }
                contentStream.endText();
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            document.save(baos);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Error generating PDF", e);
        }
    }

    public byte[] generateCodeZip(String projectName, String codePrompt) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            // 1. pom.xml entry
            addZipEntry(zos, projectName + "-backend/pom.xml", 
                    "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                    "<project>\n" +
                    "  <modelVersion>4.0.0</modelVersion>\n" +
                    "  <groupId>com.app</groupId>\n" +
                    "  <artifactId>" + projectName.toLowerCase() + "</artifactId>\n" +
                    "  <version>1.0.0</version>\n" +
                    "</project>");

            // 2. Main app class
            addZipEntry(zos, projectName + "-backend/src/main/java/com/app/Application.java",
                    "package com.app;\n\n" +
                    "import org.springframework.boot.SpringApplication;\n" +
                    "import org.springframework.boot.autoconfigure.SpringBootApplication;\n\n" +
                    "@SpringBootApplication\n" +
                    "public class Application {\n" +
                    "    public static void main(String[] args) {\n" +
                    "        SpringApplication.run(Application.class, args);\n" +
                    "    }\n" +
                    "}");

            // 3. Controller
            addZipEntry(zos, projectName + "-backend/src/main/java/com/app/controller/DemoController.java",
                    "package com.app.controller;\n\n" +
                    "import org.springframework.web.bind.annotation.*;\n\n" +
                    "@RestController\n" +
                    "@RequestMapping(\"/api\")\n" +
                    "public class DemoController {\n" +
                    "    @GetMapping(\"/status\")\n" +
                    "    public String getStatus() {\n" +
                    "        return \"System operational for " + projectName + "\";\n" +
                    "    }\n" +
                    "}");

            // 4. React package.json
            addZipEntry(zos, projectName + "-frontend/package.json",
                    "{\n" +
                    "  \"name\": \"" + projectName.toLowerCase() + "-ui\",\n" +
                    "  \"dependencies\": {\n" +
                    "    \"react\": \"^19.0.0\",\n" +
                    "    \"tailwindcss\": \"^4.0.0\"\n" +
                    "  }\n" +
                    "}");

            // 5. React main component
            addZipEntry(zos, projectName + "-frontend/src/App.tsx",
                    "import React from 'react';\n\n" +
                    "export default function App() {\n" +
                    "  return (\n" +
                    "    <div className='p-6 bg-slate-900 text-white min-h-screen'>\n" +
                    "      <h1 className='text-3xl font-bold'>Welcome to " + projectName + " UI</h1>\n" +
                    "      <p className='mt-2 text-slate-400'>Build created by AI Code Generator</p>\n" +
                    "    </div>\n" +
                    "  );\n" +
                    "}");

            zos.finish();
        } catch (IOException e) {
            throw new RuntimeException("Error generating ZIP", e);
        }
        return baos.toByteArray();
    }

    private void addZipEntry(ZipOutputStream zos, String path, String content) throws IOException {
        ZipEntry entry = new ZipEntry(path);
        zos.putNextEntry(entry);
        zos.write(content.getBytes());
        zos.closeEntry();
    }

    private List<String> wrapText(String text, int charLimit) {
        List<String> list = new ArrayList<>();
        String[] words = text.split(" ");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (sb.length() + word.length() + 1 > charLimit) {
                list.add(sb.toString());
                sb = new StringBuilder();
            }
            if (sb.length() > 0) sb.append(" ");
            sb.append(word);
        }
        if (sb.length() > 0) {
            list.add(sb.toString());
        }
        return list;
    }
}
