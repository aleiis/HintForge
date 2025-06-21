package dev.aleiis.hintforge.model;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;

public class EmbeddingManager {
	
	private static String EMBEDDING_MODEL_NAME = "text-embedding-3-small";

	private List<String> embeddingIds = new ArrayList<>();
	private List<EmbeddableExternalFile> embeddedFiles = new ArrayList<>();
	private List<EmbeddableExternalFile> pendingEmbeddingFiles = new ArrayList<>();

	public EmbeddingManager() {
	}

	public List<String> getEmbeddingIds() {
		return List.copyOf(embeddingIds);
	}

	public void addFile(EmbeddableExternalFile file) {
		if (file == null || file.getSourcePath() == null || file.getSourcePath().isBlank()) {
			throw new IllegalArgumentException("file is null or has an invalid source");
		}
		if (file.isEmbedded()) {
			embeddedFiles.add(file);
		} else {
			pendingEmbeddingFiles.add(file);
		}
	}
	
	public void setFiles(List<EmbeddableExternalFile> files) {
		embeddedFiles.clear();
		pendingEmbeddingFiles.clear();
		files.stream().forEach(file -> this.addFile(file));
	}
	
	public List<EmbeddableExternalFile> getFiles() {
		List<EmbeddableExternalFile> files = new ArrayList<>();
		files.addAll(embeddedFiles);
		files.addAll(pendingEmbeddingFiles);
		return files;
	}
	
	public boolean hasPendingEmbeddings() {
		return !pendingEmbeddingFiles.isEmpty();
	}
	
	public void clearPendingEmbeddings() {
		this.pendingEmbeddingFiles.clear();
	}
	
	public void sync(EmbeddingStore<TextSegment> embeddingStore, Path homeFolder, String apiKey) throws IOException {
		
		// Delete embeddings from removed files
		List<String> actualIds = embeddedFiles.stream().flatMap(file -> file.getEmbeddingIds().stream()).toList();
		List<String> deletedIds = embeddingIds.stream().filter(id -> !actualIds.contains(id)).toList();
		
		if (!deletedIds.isEmpty()) {
			embeddingStore.removeAll(deletedIds);
			embeddingIds.removeAll(deletedIds);	
		}

		// Ingest new files
		EmbeddingModel embeddingModel = OpenAiEmbeddingModel.builder().apiKey(apiKey).modelName(EMBEDDING_MODEL_NAME)
				.build();

		for (EmbeddableExternalFile file : pendingEmbeddingFiles) {
			List<String> ids = file.calculateAndStoreEmbedding(homeFolder, embeddingStore, embeddingModel);
			this.embeddingIds.addAll(ids);
		}
		
		embeddedFiles.addAll(pendingEmbeddingFiles);
		pendingEmbeddingFiles.clear();
	}	
}
