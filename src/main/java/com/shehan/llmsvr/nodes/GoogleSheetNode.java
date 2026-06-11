package com.shehan.llmsvr.nodes;

import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleRefreshTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.*;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.shehan.llmsvr.dtos.MessageBatch;
import com.shehan.llmsvr.dtos.NodeResult;
import com.shehan.llmsvr.dtos.WorkflowMessage;
import com.shehan.llmsvr.helper.NodeConfigUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class GoogleSheetNode implements WorkflowNode {

    private final Map<String, String> refreshTokenStore = new ConcurrentHashMap<>();
    private final Map<String, String> folderSheetStore = new ConcurrentHashMap<>();

    @Override
    public String getType() {
        return "google.sheet";
    }

    @Override
    public NodeResult execute(MessageBatch input, Map<String, Object> config) {
        try {
            String clientId = NodeConfigUtil.getInputProp(config, "clientId", "");
            String clientSecret = NodeConfigUtil.getInputProp(config, "clientSecret", "");
            String code = NodeConfigUtil.getInputProp(config, "googleAuthCode", "");
            String targetFolderId = NodeConfigUtil.getInputProp(config, "googleDriver", "");
            String existingSheetId = NodeConfigUtil.getInputProp(config, "existingSheetId", "");

            if (input.getItems().isEmpty()) return NodeResult.error(new MessageBatch(List.of()));
            WorkflowMessage msg = input.getItems().get(0);

            String accessToken = resolveAccessToken(clientId, clientSecret, code);
            GoogleCredentials credentials = GoogleCredentials.create(new AccessToken(accessToken, null));
            HttpCredentialsAdapter adapter = new HttpCredentialsAdapter(credentials);

            Sheets sheetsService = new Sheets.Builder(
                    GoogleNetHttpTransport.newTrustedTransport(),
                    GsonFactory.getDefaultInstance(), adapter)
                    .setApplicationName("LLM-Service").build();

            String spreadsheetId;
            boolean append = false;

            if (existingSheetId != null && !existingSheetId.isEmpty()) {
                spreadsheetId = existingSheetId;
                clearSpreadsheet(sheetsService, spreadsheetId);
            } else if (targetFolderId != null && !targetFolderId.isEmpty()) {
                String cachedSheetId = folderSheetStore.get(targetFolderId);
                if (cachedSheetId != null) {
                    spreadsheetId = cachedSheetId;
                    append = true;
                } else {
                    Drive driveService = buildDriveService(adapter);
                    String foundSheetId = findExistingSheet(driveService, targetFolderId);
                    if (foundSheetId != null) {
                        spreadsheetId = foundSheetId;
                        append = true;
                    } else {
                        spreadsheetId = createNewSheet(driveService, targetFolderId);
                    }
                    folderSheetStore.put(targetFolderId, spreadsheetId);
                }
            } else {
                Drive driveService = buildDriveService(adapter);
                spreadsheetId = createNewSheet(driveService, null);
            }

            // Extract rows into structured tabular data List<List<Object>>
            List<List<Object>> rows = parseTabularContent(msg.getData());
            log.info("Parsed {} rows for sheet {}", rows.size(), spreadsheetId);

            if (rows.isEmpty()) {
                log.warn("No contents parsed, skipping sheet write");
                return NodeResult.error(new MessageBatch(List.of()));
            }

            if (append) {
                appendSheetData(sheetsService, spreadsheetId, rows);
            } else {
                writeSheetData(sheetsService, spreadsheetId, rows);
            }

            Map<String, Object> resultData = new HashMap<>(msg.getData());
            resultData.put("spreadsheetId", spreadsheetId);
            resultData.put("spreadsheetUrl", "https://docs.google.com/spreadsheets/d/" + spreadsheetId);

            return NodeResult.complected("success", new MessageBatch(
                    List.of(new WorkflowMessage(resultData))
            ));

        } catch (Exception e) {
            log.error("Failed to process Google Sheet Node: ", e);
            return NodeResult.error(new MessageBatch(List.of()));
        }
    }

    private Drive buildDriveService(HttpCredentialsAdapter adapter) throws Exception {
        return new Drive.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(), adapter)
                .setApplicationName("LLM-Service").build();
    }

    private String findExistingSheet(Drive driveService, String folderId) throws Exception {
        String query = String.format(
                "'%s' in parents and mimeType='application/vnd.google-apps.spreadsheet' and name contains 'AI Output Data' and trashed=false",
                folderId
        );
        List<File> files = driveService.files().list()
                .setQ(query)
                .setFields("files(id, name)")
                .setOrderBy("createdTime desc")
                .setPageSize(1)
                .execute()
                .getFiles();

        if (files != null && !files.isEmpty()) {
            log.info("Found existing sheet in folder: {}", files.get(0).getId());
            return files.get(0).getId();
        }
        return null;
    }

    private String createNewSheet(Drive driveService, String folderId) throws Exception {
        File fileMetadata = new File();
        fileMetadata.setName("AI Output Data - " + System.currentTimeMillis());
        fileMetadata.setMimeType("application/vnd.google-apps.spreadsheet");
        if (folderId != null && !folderId.isEmpty()) {
            fileMetadata.setParents(Collections.singletonList(folderId));
        }
        File created = driveService.files().create(fileMetadata).setFields("id").execute();
        log.info("Created new spreadsheet: {}", created.getId());
        return created.getId();
    }

    private void clearSpreadsheet(Sheets sheetsService, String spreadsheetId) throws Exception {
        ClearValuesRequest requestBody = new ClearValuesRequest();
        // Clears data out of the first sheet globally across standard structural bound limits
        sheetsService.spreadsheets().values()
                .clear(spreadsheetId, "Sheet1!A1:Z1000", requestBody)
                .execute();
    }

    private void writeSheetData(Sheets sheetsService, String spreadsheetId, List<List<Object>> values) throws Exception {
        ValueRange body = new ValueRange().setValues(values);
        sheetsService.spreadsheets().values()
                .update(spreadsheetId, "Sheet1!A1", body)
                .setValueInputOption("USER_ENTERED")
                .execute();
    }

    private void appendSheetData(Sheets sheetsService, String spreadsheetId, List<List<Object>> values) throws Exception {
        ValueRange body = new ValueRange().setValues(values);
        sheetsService.spreadsheets().values()
                .append(spreadsheetId, "Sheet1!A1", body)
                .setValueInputOption("USER_ENTERED")
                .setInsertDataOption("INSERT_ROWS")
                .execute();
    }

    @SuppressWarnings("unchecked")
    private List<List<Object>> parseTabularContent(Map<String, Object> data) {
        List<List<Object>> sheetRows = new ArrayList<>();

        Object extractedRaw = data.get("extractedData");
        if (extractedRaw instanceof Map<?, ?> extractedMap && !extractedMap.isEmpty()) {
            Map<String, Object> extracted = (Map<String, Object>) extractedMap;

            if (extracted.containsKey("rows") && extracted.get("rows") instanceof List<?> rowsList) {
                return parseGenericListToRows(rowsList);
            }
            if (extracted.containsKey("sections") && extracted.get("sections") instanceof List<?> sectionList) {
                return parseGenericListToRows(sectionList);
            }

            List<Object> headers = new ArrayList<>();
            List<Object> targetValues = new ArrayList<>();
            extracted.forEach((k, v) -> {
                if (v != null && !(v instanceof Map) && !(v instanceof List)) {
                    headers.add(capitalizeKey(k));
                    targetValues.add(v.toString());
                }
            });
            if (!headers.isEmpty()) {
                sheetRows.add(headers);
                sheetRows.add(targetValues);
                return sheetRows;
            }
        }

        // Case 2: LLM Markdown text response fallbacks (look for standard text/markdown structures)
        String responseContent = extractResponseContent(data);
        if (responseContent == null || responseContent.isBlank()) {
            if (data.containsKey("text")) {
                responseContent = data.get("text").toString();
            }
        }

        if (responseContent != null && !responseContent.isBlank()) {
            return parseMarkdownTableText(responseContent);
        }

        return sheetRows;
    }

    @SuppressWarnings("unchecked")
    private List<List<Object>> parseGenericListToRows(List<?> recordsList) {
        List<List<Object>> computedRows = new ArrayList<>();
        if (recordsList.isEmpty()) return computedRows;

        Set<String> uniqueHeaders = new LinkedHashSet<>();
        List<Map<String, Object>> normalizedMaps = new ArrayList<>();

        // Parse list objects out into flatter structures if valid objects are visible
        for (Object recordItem : recordsList) {
            if (recordItem instanceof Map<?, ?> mapItem) {
                Map<String, Object> stringMap = (Map<String, Object>) mapItem;
                Map<String, Object> cleanRowMap = new HashMap<>();
                stringMap.forEach((k, v) -> {
                    if (v != null && !(v instanceof Map) && !(v instanceof List)) {
                        String capitalizedKey = capitalizeKey(k);
                        uniqueHeaders.add(capitalizedKey);
                        cleanRowMap.put(capitalizedKey, v);
                    }
                });
                if (!cleanRowMap.isEmpty()) normalizedMaps.add(cleanRowMap);
            } else if (recordItem instanceof List<?> lineList) {
                // If it's a list of lists, pass it directly down as raw array parameters
                computedRows.add(new ArrayList<>(lineList));
            }
        }

        // Apply map structural processing if a structural key definition schema was set
        if (!uniqueHeaders.isEmpty()) {
            List<String> orderedHeaders = new ArrayList<>(uniqueHeaders);
            computedRows.add(new ArrayList<>(orderedHeaders)); // Header row

            for (Map<String, Object> mapRow : normalizedMaps) {
                List<Object> rowDataValues = new ArrayList<>();
                for (String header : orderedHeaders) {
                    rowDataValues.add(mapRow.getOrDefault(header, ""));
                }
                computedRows.add(rowDataValues);
            }
        }
        return computedRows;
    }

    private List<List<Object>> parseMarkdownTableText(String text) {
        List<List<Object>> tableRows = new ArrayList<>();
        String[] lines = text.split("\n");

        for (String line : lines) {
            String trimmed = line.trim();
            // Match structural markdown table indicators e.g., | Header 1 | Header 2 |
            if (trimmed.startsWith("|") && trimmed.endsWith("|")) {
                // Skip separator rows e.g., |---|---|
                if (trimmed.contains("---") || trimmed.contains("-:-")) {
                    continue;
                }
                String[] segments = trimmed.split("\\|");
                List<Object> parsedCells = new ArrayList<>();

                // Index starts at 1 because splitting "| Cell A | Cell B |" results in an empty first element
                for (int i = 1; i < segments.length; i++) {
                    parsedCells.add(segments[i].trim().replace("**", ""));
                }
                if (!parsedCells.isEmpty()) {
                    tableRows.add(parsedCells);
                }
            }
        }
        return tableRows;
    }

    private String capitalizeKey(String key) {
        if (key == null || key.isEmpty()) return key;
        String spaced = key.replaceAll("([A-Z])", " $1").trim();
        return Character.toUpperCase(spaced.charAt(0)) + spaced.substring(1);
    }

    @SuppressWarnings("unchecked")
    private String extractResponseContent(Map<String, Object> data) {
        try {
            Object responseRaw = data.get("response");
            if (!(responseRaw instanceof Map<?, ?> responseMap)) return null;

            Object kwargsRaw = responseMap.get("kwargs");
            if (!(kwargsRaw instanceof Map<?, ?> kwargsMap)) return null;

            Object content = kwargsMap.get("content");
            if (content != null && !content.toString().isBlank()) {
                return content.toString();
            }
        } catch (Exception e) {
            log.warn("Failed to extract response content: {}", e.getMessage());
        }
        return null;
    }

    private String resolveAccessToken(String clientId, String clientSecret, String code) throws Exception {
        String storedRefreshToken = refreshTokenStore.get(clientId);

        if (storedRefreshToken != null) {
            GoogleTokenResponse response = new GoogleRefreshTokenRequest(
                    GoogleNetHttpTransport.newTrustedTransport(),
                    GsonFactory.getDefaultInstance(),
                    storedRefreshToken, clientId, clientSecret).execute();
            return response.getAccessToken();
        }

        GoogleTokenResponse tokenResponse = new GoogleAuthorizationCodeTokenRequest(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                "https://oauth2.googleapis.com/token",
                clientId, clientSecret, code, "postmessage").execute();

        String refreshToken = tokenResponse.getRefreshToken();
        if (refreshToken != null && !refreshToken.isEmpty()) {
            refreshTokenStore.put(clientId, refreshToken);
        }

        return tokenResponse.getAccessToken();
    }
}