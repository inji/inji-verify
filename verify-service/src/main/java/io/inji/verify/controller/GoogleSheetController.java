package io.inji.verify.controller;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.inji.verify.services.GoogleSheetService;

@RestController
@RequestMapping("/api/sheets")
public class GoogleSheetController {

    @Autowired
    private GoogleSheetService googleSheetService;

    private String generateNextSNo() throws IOException, GeneralSecurityException {
        List<List<Object>> data = googleSheetService.getSheetData("Sheet1!A:F");
        if (data == null || data.isEmpty()) {
            return "1";
        }

        int lastSNo = 0;

        for (List<Object> row : data) {
            if (row.isEmpty() || row.get(0) == null) {
                continue;
            }

            try {
                String sNoStr = row.get(0).toString().trim();
                if (!sNoStr.matches("\\d+")) {
                    continue;
                }

                int currentSNo = Integer.parseInt(sNoStr);
                lastSNo = Math.max(lastSNo, currentSNo);
            } catch (NumberFormatException e) {
                System.err.println("Error parsing s.no (skipping row): " + e.getMessage());
            }
        }

        return String.valueOf(lastSNo + 1);
    }

    @PostMapping("/append")
    public ResponseEntity<String> appendData(@RequestBody Map<String, Object> rowData) throws GeneralSecurityException {
        try {
            List<List<Object>> existingData = googleSheetService.getSheetData("Sheet1!B:E");

            String fullName = rowData.getOrDefault("fullName", "").toString().trim();
            String email = rowData.getOrDefault("email", "").toString().trim();
            String organisation = rowData.getOrDefault("organisation", "").toString().trim();
            String designation = rowData.getOrDefault("designation", "").toString().trim();

            // Only fullName and email are required
            if (fullName.isEmpty() || email.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error: Full Name and Email are required fields.");
            }

            for (List<Object> row : existingData) {
                if (row.size() >= 2) {
                    String existingFullName = row.get(0).toString().trim();
                    String existingEmail = row.get(1).toString().trim();

                    if (existingFullName.equalsIgnoreCase(fullName)
                            && existingEmail.equalsIgnoreCase(email)) {
                        return ResponseEntity.status(HttpStatus.CONFLICT).body("Individual already registered.");
                    }
                }
            }

            List<Object> values = new ArrayList<>();
            values.add(generateNextSNo());
            values.add(fullName);
            values.add(email);
            values.add(organisation.isEmpty() ? "" : organisation);
            values.add(designation.isEmpty() ? "" : designation);
            long timestampMillis = Instant.now().toEpochMilli();
            LocalDateTime localDateTime = Instant.ofEpochMilli(timestampMillis)
                    .atZone(ZoneId.of("Asia/Kolkata"))
                    .toLocalDateTime();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            String formattedDateTime = localDateTime.format(formatter);
            values.add(formattedDateTime);

            List<List<Object>> dataToAppend = new ArrayList<>();
            dataToAppend.add(values);

            googleSheetService.appendData("Sheet1!A:F", dataToAppend);
            return ResponseEntity.ok("Data has been added to Google Sheets!");

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error appending data: " + e.getMessage());
        }
    }

}
