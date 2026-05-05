package com.shehan.llmsvr.nodes;

import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleRefreshTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.docs.v1.Docs;
import com.google.api.services.docs.v1.model.*;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
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
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class GoogleDocNode implements WorkflowNode {

    private final Map<String, String> refreshTokenStore = new ConcurrentHashMap<>();
    private final Map<String, String> folderDocStore = new ConcurrentHashMap<>();

    @Override
    public String getType() {
        return "google.doc";
    }

    @Override
    public NodeResult execute(MessageBatch input, Map<String, Object> config) {
        try {
            String clientId       = NodeConfigUtil.getInputProp(config, "clientId", "");
            String clientSecret   = NodeConfigUtil.getInputProp(config, "clientSecret", "");
            String code           = NodeConfigUtil.getInputProp(config, "googleAuthCode", "");
            String targetFolderId = NodeConfigUtil.getInputProp(config, "googleDriver", "");
            String existingDocId  = NodeConfigUtil.getInputProp(config, "existingDocId", "");

            if (input.getItems().isEmpty()) return NodeResult.error(new MessageBatch(List.of()));
            WorkflowMessage msg = input.getItems().get(0);

            String accessToken = resolveAccessToken(clientId, clientSecret, code);
            GoogleCredentials credentials = GoogleCredentials.create(new AccessToken(accessToken, null));
            HttpCredentialsAdapter adapter = new HttpCredentialsAdapter(credentials);

            Docs docsService = new Docs.Builder(
                    GoogleNetHttpTransport.newTrustedTransport(),
                    GsonFactory.getDefaultInstance(), adapter)
                    .setApplicationName("LLM-Service").build();

            String documentId;
            boolean append = false;

            if (existingDocId != null && !existingDocId.isEmpty()) {
                documentId = existingDocId;
                clearDocument(docsService, documentId);
            } else if (targetFolderId != null && !targetFolderId.isEmpty()) {
                String cachedDocId = folderDocStore.get(targetFolderId);
                if (cachedDocId != null) {
                    documentId = cachedDocId;
                    append = true;
                } else {
                    Drive driveService = buildDriveService(adapter);
                    String foundDocId = findExistingDoc(driveService, targetFolderId);
                    if (foundDocId != null) {
                        documentId = foundDocId;
                        append = true;
                    } else {
                        documentId = createNewDoc(driveService, targetFolderId);
                    }
                    folderDocStore.put(targetFolderId, documentId);
                }
            } else {
                Drive driveService = buildDriveService(adapter);
                documentId = createNewDoc(driveService, null);
            }

            List<DocSection> sections = parseContent(msg.getData());
            log.info("Parsed {} sections for doc {}", sections.size(), documentId);

            if (sections.isEmpty()) {
                log.warn("No content parsed, skipping doc write");
                return NodeResult.error(new MessageBatch(List.of()));
            }

            if (append) {
                appendFormattedContent(docsService, documentId, sections);
            } else {
                writeFormattedContent(docsService, documentId, sections);
            }

            Map<String, Object> resultData = new HashMap<>(msg.getData());
            resultData.put("documentId", documentId);
            resultData.put("documentUrl", "https://docs.google.com/document/d/" + documentId);

            return NodeResult.complected("success", new MessageBatch(
                    List.of(new WorkflowMessage(resultData))
            ));

        } catch (Exception e) {
            log.error("Failed to process Google Doc Node: ", e);
            return NodeResult.error(new MessageBatch(List.of()));
        }
    }

    private Drive buildDriveService(HttpCredentialsAdapter adapter) throws Exception {
        return new Drive.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(), adapter)
                .setApplicationName("LLM-Service").build();
    }

    private String findExistingDoc(Drive driveService, String folderId) throws Exception {
        String query = String.format(
                "'%s' in parents and mimeType='application/vnd.google-apps.document' and name contains 'AI Output' and trashed=false",
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
            log.info("Found existing doc in folder: {}", files.get(0).getId());
            return files.get(0).getId();
        }
        return null;
    }

    private String createNewDoc(Drive driveService, String folderId) throws Exception {
        File fileMetadata = new File();
        fileMetadata.setName("AI Output - " + System.currentTimeMillis());
        fileMetadata.setMimeType("application/vnd.google-apps.document");
        if (folderId != null && !folderId.isEmpty()) {
            fileMetadata.setParents(Collections.singletonList(folderId));
        }
        File created = driveService.files().create(fileMetadata).setFields("id").execute();
        log.info("Created new doc: {}", created.getId());
        return created.getId();
    }

    private void clearDocument(Docs docsService, String documentId) throws Exception {
        Document doc = docsService.documents().get(documentId).execute();
        int endIndex = doc.getBody().getContent().stream()
                .mapToInt(el -> el.getEndIndex() != null ? el.getEndIndex() : 0)
                .max().orElse(1);

        if (endIndex > 2) {
            List<Request> requests = List.of(new Request().setDeleteContentRange(
                    new DeleteContentRangeRequest().setRange(
                            new Range().setStartIndex(1).setEndIndex(endIndex - 1)
                    )
            ));
            docsService.documents().batchUpdate(documentId,
                    new BatchUpdateDocumentRequest().setRequests(requests)).execute();
        }
    }

    private void writeFormattedContent(Docs docsService, String documentId, List<DocSection> sections) throws Exception {
        if (sections.isEmpty()) {
            log.warn("No sections to write, skipping");
            return;
        }

        List<Request> insertRequests = new ArrayList<>();
        List<TextRange> ranges = new ArrayList<>();
        int cursor = 1;

        for (DocSection section : sections) {
            int start = cursor;
            String text = section.text() + "\n";
            insertRequests.add(new Request().setInsertText(
                    new InsertTextRequest().setText(text).setLocation(new Location().setIndex(cursor))
            ));
            cursor += text.length();
            ranges.add(new TextRange(start, cursor - 1, section));
        }

        docsService.documents().batchUpdate(documentId,
                new BatchUpdateDocumentRequest().setRequests(insertRequests)).execute();

        List<Request> styleRequests = new ArrayList<>();
        for (TextRange range : ranges) {
            styleRequests.addAll(buildStyleRequests(range));
        }

        if (!styleRequests.isEmpty()) {
            docsService.documents().batchUpdate(documentId,
                    new BatchUpdateDocumentRequest().setRequests(styleRequests)).execute();
        }
    }

    private void appendFormattedContent(Docs docsService, String documentId, List<DocSection> sections) throws Exception {
        if (sections.isEmpty()) {
            log.warn("No sections to append, skipping");
            return;
        }

        Document doc = docsService.documents().get(documentId).execute();
        int endIndex = doc.getBody().getContent().stream()
                .mapToInt(el -> el.getEndIndex() != null ? el.getEndIndex() : 0)
                .max().orElse(1);

        int cursor = endIndex - 1;

        List<Request> insertRequests = new ArrayList<>();
        List<TextRange> ranges = new ArrayList<>();

        String separator = "\n―――――――――――――――――――\n";
        insertRequests.add(new Request().setInsertText(
                new InsertTextRequest().setText(separator).setLocation(new Location().setIndex(cursor))
        ));
        cursor += separator.length();

        for (DocSection section : sections) {
            int start = cursor;
            String text = section.text() + "\n";
            insertRequests.add(new Request().setInsertText(
                    new InsertTextRequest().setText(text).setLocation(new Location().setIndex(cursor))
            ));
            cursor += text.length();
            ranges.add(new TextRange(start, cursor - 1, section));
        }

        docsService.documents().batchUpdate(documentId,
                new BatchUpdateDocumentRequest().setRequests(insertRequests)).execute();

        List<Request> styleRequests = new ArrayList<>();
        for (TextRange range : ranges) {
            styleRequests.addAll(buildStyleRequests(range));
        }

        if (!styleRequests.isEmpty()) {
            docsService.documents().batchUpdate(documentId,
                    new BatchUpdateDocumentRequest().setRequests(styleRequests)).execute();
        }
    }

    private List<Request> buildStyleRequests(TextRange range) {
        List<Request> requests = new ArrayList<>();
        DocSection section = range.section();
        int start = range.start();
        int end = range.end();

        switch (section.type()) {
            case HEADING_1 -> requests.add(paragraphStyleRequest(start, end, "HEADING_1"));
            case HEADING_2 -> requests.add(paragraphStyleRequest(start, end, "HEADING_2"));
            case HEADING_3 -> requests.add(paragraphStyleRequest(start, end, "HEADING_3"));
            case BULLET -> requests.add(new Request().setCreateParagraphBullets(
                    new CreateParagraphBulletsRequest()
                            .setRange(new Range().setStartIndex(start).setEndIndex(end))
                            .setBulletPreset("BULLET_DISC_CIRCLE_SQUARE")
            ));
            case NUMBERED -> requests.add(new Request().setCreateParagraphBullets(
                    new CreateParagraphBulletsRequest()
                            .setRange(new Range().setStartIndex(start).setEndIndex(end))
                            .setBulletPreset("NUMBERED_DECIMAL_ALPHA_ROMAN")
            ));
            case BOLD -> requests.add(textStyleRequest(start, end, true, false));
            case ITALIC -> requests.add(textStyleRequest(start, end, false, true));
            case BOLD_ITALIC -> requests.add(textStyleRequest(start, end, true, true));
            case DIVIDER -> requests.add(new Request().setUpdateParagraphStyle(
                    new UpdateParagraphStyleRequest()
                            .setRange(new Range().setStartIndex(start).setEndIndex(end))
                            .setParagraphStyle(new ParagraphStyle()
                                    .setBorderBottom(new ParagraphBorder()
                                            .setColor(new OptionalColor().setColor(
                                                    new Color().setRgbColor(new RgbColor()
                                                            .setRed(0.8f).setGreen(0.8f).setBlue(0.8f))))
                                            .setWidth(new Dimension().setMagnitude(1.0).setUnit("PT"))
                                            .setPadding(new Dimension().setMagnitude(4.0).setUnit("PT"))
                                            .setDashStyle("SOLID")))
                            .setFields("borderBottom")
            ));
            case NORMAL -> {}
        }

        return requests;
    }

    private Request paragraphStyleRequest(int start, int end, String namedStyle) {
        return new Request().setUpdateParagraphStyle(
                new UpdateParagraphStyleRequest()
                        .setRange(new Range().setStartIndex(start).setEndIndex(end))
                        .setParagraphStyle(new ParagraphStyle().setNamedStyleType(namedStyle))
                        .setFields("namedStyleType")
        );
    }

    private Request textStyleRequest(int start, int end, boolean bold, boolean italic) {
        String fields = (bold && italic) ? "bold,italic" : bold ? "bold" : "italic";
        return new Request().setUpdateTextStyle(
                new UpdateTextStyleRequest()
                        .setRange(new Range().setStartIndex(start).setEndIndex(end))
                        .setTextStyle(new TextStyle().setBold(bold).setItalic(italic))
                        .setFields(fields)
        );
    }

    @SuppressWarnings("unchecked")
    private List<DocSection> parseContent(Map<String, Object> data) {
        List<DocSection> sections = new ArrayList<>();

        Object extractedRaw = data.get("extractedData");
        if (extractedRaw instanceof Map<?, ?> extractedMap && !extractedMap.isEmpty()) {
            Map<String, Object> extracted = (Map<String, Object>) extractedMap;

            if (extracted.containsKey("title")) {
                sections.add(new DocSection(SectionType.HEADING_1, extracted.get("title").toString()));
            }
            if (extracted.containsKey("subtitle")) {
                sections.add(new DocSection(SectionType.HEADING_2, extracted.get("subtitle").toString()));
            }
            if (extracted.containsKey("summary")) {
                sections.add(new DocSection(SectionType.NORMAL, extracted.get("summary").toString()));
            }
            if (extracted.containsKey("sections") && extracted.get("sections") instanceof List<?> sectionList) {
                for (Object item : sectionList) {
                    if (item instanceof Map<?, ?> sectionMap) {
                        Map<String, Object> s = (Map<String, Object>) sectionMap;
                        if (s.containsKey("heading")) {
                            sections.add(new DocSection(SectionType.HEADING_2, s.get("heading").toString()));
                        }
                        if (s.containsKey("subheading")) {
                            sections.add(new DocSection(SectionType.HEADING_3, s.get("subheading").toString()));
                        }
                        if (s.containsKey("body")) {
                            sections.add(new DocSection(SectionType.NORMAL, s.get("body").toString()));
                        }
                        if (s.containsKey("bullets") && s.get("bullets") instanceof List<?> bullets) {
                            for (Object b : bullets) {
                                sections.add(new DocSection(SectionType.BULLET, b.toString()));
                            }
                        }
                        if (s.containsKey("numbered") && s.get("numbered") instanceof List<?> numbered) {
                            for (Object n : numbered) {
                                sections.add(new DocSection(SectionType.NUMBERED, n.toString()));
                            }
                        }
                        sections.add(new DocSection(SectionType.DIVIDER, " "));
                    }
                }
            }
            if (extracted.containsKey("bullets") && extracted.get("bullets") instanceof List<?> bullets) {
                for (Object b : bullets) {
                    sections.add(new DocSection(SectionType.BULLET, b.toString()));
                }
            }

            if (sections.isEmpty()) {
                extracted.forEach((key, value) -> {
                    if (value != null && !value.toString().isBlank()) {
                        sections.add(new DocSection(SectionType.BOLD, capitalizeKey(key.toString())));
                        sections.add(new DocSection(SectionType.NORMAL, value.toString()));
                    }
                });
            }

            if (!sections.isEmpty()) return sections;
        }

        String responseContent = extractResponseContent(data);
        if (responseContent != null && !responseContent.isBlank()) {
            return parseResponseText(responseContent);
        }

        if (data.containsKey("text")) {
            sections.add(new DocSection(SectionType.NORMAL, data.get("text").toString()));
        }

        return sections;
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

    private List<DocSection> parseResponseText(String text) {
        List<DocSection> sections = new ArrayList<>();
        String[] lines = text.split("\n");

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) continue;

            if (trimmed.startsWith("# ")) {
                sections.add(new DocSection(SectionType.HEADING_1, trimmed.substring(2)));
            } else if (trimmed.startsWith("## ")) {
                sections.add(new DocSection(SectionType.HEADING_2, trimmed.substring(3)));
            } else if (trimmed.startsWith("### ")) {
                sections.add(new DocSection(SectionType.HEADING_3, trimmed.substring(4)));
            } else if (trimmed.startsWith("- ") || trimmed.startsWith("* ")) {
                sections.add(new DocSection(SectionType.BULLET, trimmed.substring(2)));
            } else if (trimmed.matches("^\\d+\\.\\s.*")) {
                sections.add(new DocSection(SectionType.NUMBERED, trimmed.replaceFirst("^\\d+\\.\\s", "")));
            } else if (trimmed.startsWith("**") && trimmed.endsWith("**")) {
                sections.add(new DocSection(SectionType.BOLD, trimmed.replace("**", "")));
            } else {
                sections.add(new DocSection(SectionType.NORMAL, trimmed));
            }
        }

        return sections;
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

    enum SectionType {
        HEADING_1, HEADING_2, HEADING_3,
        BULLET, NUMBERED,
        BOLD, ITALIC, BOLD_ITALIC,
        DIVIDER, NORMAL
    }

    record DocSection(SectionType type, String text) {}

    record TextRange(int start, int end, DocSection section) {}
}
