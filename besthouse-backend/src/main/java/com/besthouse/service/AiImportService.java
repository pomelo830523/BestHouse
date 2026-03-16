package com.besthouse.service;

import com.besthouse.dto.AiImportResultDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.Base64;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class AiImportService {

    @Value("${gemini.api-key}")
    private String apiKey;

    private static final String GEMINI_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=";

    private static final String PROMPT = """
            請從這張台灣房屋物件截圖中擷取以下資訊，以 JSON 格式回覆。
            若某欄位截圖中找不到，請填 null。
            價格單位統一用「萬」（例如 1580 代表 1580萬）。
            坪數用純數字（例如 45.32）。
            parkingType 只能填以下其中一個值：
              NONE（無車位）、FLAT（平面）、RAMP_FLAT（坡道平面）、MECHANICAL（機械）、RAMP_MECHANICAL（坡道機械）。
            只回覆 JSON，不要任何說明或 markdown 標記：
            {
              "nickname": "物件標題",
              "address": "地址",
              "communityName": "社區或大樓名稱",
              "builder": "建商",
              "houseAgeYear": 屋齡整數,
              "floor": 所在樓層整數,
              "totalFloor": 總樓層整數,
              "buildAreaPing": 總坪數字,
              "indoorPing": 室內坪數字,
              "bedroomCount": 房數整數,
              "livingRoomCount": 廳數整數,
              "bathroomCount": 衛浴數整數,
              "totalPrice": 總價萬數字,
              "parkingType": "NONE",
              "parkingPrice": 車位價格萬數字,
              "monthlyFee": 管理費元月數字,
              "listingUrl": "物件網址如果截圖中有"
            }
            """;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AiImportResultDto extractFromImage(MultipartFile file) throws Exception {
        String base64 = Base64.getEncoder().encodeToString(file.getBytes());
        String mimeType = file.getContentType() != null ? file.getContentType() : "image/jpeg";

        Map<String, Object> imagePart = Map.of(
                "inlineData", Map.of("mimeType", mimeType, "data", base64)
        );
        Map<String, Object> textPart = Map.of("text", PROMPT);
        Map<String, Object> content  = Map.of("parts", List.of(imagePart, textPart));
        Map<String, Object> body     = Map.of("contents", List.of(content));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        ResponseEntity<String> response = restTemplate.exchange(
                GEMINI_URL + apiKey, HttpMethod.POST, entity, String.class
        );

        JsonNode root = objectMapper.readTree(response.getBody());
        String text = root.path("candidates").get(0)
                .path("content").path("parts").get(0)
                .path("text").asText();

        // 去掉 Gemini 有時會加的 markdown code block
        text = text.replaceAll("(?s)```json\\s*", "").replaceAll("(?s)```\\s*", "").trim();

        log.info("Gemini 解析結果: {}", text);
        return objectMapper.readValue(text, AiImportResultDto.class);
    }
}
