package ngo.cong.thao.s2o_pro.ai.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class GeminiRequest {
    private List<Content> contents;

    public GeminiRequest(String text) {
        this.contents = List.of(new Content(List.of(new Part(text))));
    }

    @Data
    public static class Content {
        private List<Part> parts;
        public Content(List<Part> parts) { this.parts = parts; }
    }

    @Data
    public static class Part {
        private String text;
        public Part(String text) { this.text = text; }
    }
}