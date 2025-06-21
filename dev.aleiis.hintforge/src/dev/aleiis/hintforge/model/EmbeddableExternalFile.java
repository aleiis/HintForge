package dev.aleiis.hintforge.model;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;

public class EmbeddableExternalFile extends ExternalFile {

	private List<String> embeddingIds = new ArrayList<>();

	public EmbeddableExternalFile() {
	}

	public EmbeddableExternalFile(String source, String originalFileName) {
		super(source, originalFileName);
	}

	public List<String> getEmbeddingIds() {
		return List.copyOf(embeddingIds);
	}

	public boolean isEmbedded() {
		return !this.embeddingIds.isEmpty();
	}

	public List<String> calculateAndStoreEmbedding(Path homeFolder, EmbeddingStore<TextSegment> embeddingStore,
			EmbeddingModel embeddingModel) throws IOException {
		Document document = Document.from(this.readContent(homeFolder));
		DocumentSplitter splitter = DocumentSplitters.recursive(1000, 200);
		List<TextSegment> segments = splitter.split(document);
		List<Embedding> embeddings = embeddingModel.embedAll(segments).content();
		List<String> ids = embeddingStore.addAll(embeddings, segments);		
		this.embeddingIds = ids;
		return ids;
	}
}
