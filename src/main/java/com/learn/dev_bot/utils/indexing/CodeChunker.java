package com.learn.dev_bot.utils.indexing;

import com.learn.dev_bot.utils.ai.RagSettings;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

@Component
public class CodeChunker {
    private final TokenTextSplitter splitter;
    private final CodeFileFilter fileFilter;

    public CodeChunker(
        @Value("${app.indexing.chunk-size:800}") int chunkSize,
        CodeFileFilter fileFilter) {

        int chunkTokens = Math.max(50, chunkSize / 4);

        this.splitter = TokenTextSplitter.builder()
                .withChunkSize(chunkTokens).build();
        this.fileFilter = fileFilter;
    }

    public List<Document> chunkFile (String repoId, String filePath, String content) {
        if (content == null || content.isBlank())
            return List.of();

        String language = fileFilter.detectLanguage(filePath);
        String header = "// File: %s\n".formatted(filePath);

        Document source = new Document(header + content, baseMetaData(repoId, filePath, language));
        List<Document> split = splitter.apply(List.of(source));

        return IntStream.range(0, split.size())
                .mapToObj(i -> withChunkIndex(split.get(i), repoId, filePath, language, i))
                .toList();
    }

    private static Document withChunkIndex (
            Document chunk,
            String repoId,
            String filePath,
            String language,
            int chunkIndex) {

        Map<String, Object> metaData = new HashMap<>(chunk.getMetadata());

        metaData.put(RagSettings.METADATA_REPO_ID, repoId);
        metaData.put("filePath", filePath);
        metaData.put("language", language);

        return new Document(chunk.getText(), metaData);
    }

    public static Map<String, Object> baseMetaData(String repoId, String filePath, String language) {
        Map<String, Object> metaData = new HashMap<>();

        metaData.put(RagSettings.METADATA_REPO_ID, repoId);
        metaData.put("filePath", filePath);
        metaData.put("language", language);

        return metaData;
    }
}
