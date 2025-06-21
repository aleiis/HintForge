package dev.aleiis.hintforge.assistant;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import dev.aleiis.hintforge.model.DslProfile;
import dev.aleiis.hintforge.model.ExternalFile;
import dev.aleiis.hintforge.preference.PreferenceManager;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.EmbeddingStore;

public abstract class Assistant {

	private static String EMBEDDING_MODEL_NAME = "text-embedding-3-small";
	
	private final Path homeFolder;
	private final String openAiApiKey;
	private final DslProfile dsl;
	private final String modelName;
	private EmbeddingStore<TextSegment> embeddingStore = null;
	private final SyntaxVerifier verifier;

	public Assistant(String openAiApiKey, DslProfile dsl) {
		this(openAiApiKey, dsl, "gpt-4o-mini");
	}

	public Assistant(String openAiApiKey, DslProfile dsl, String modelName) {
		this(PreferenceManager.getInstance().getHomeFolder(), openAiApiKey, dsl, modelName);
	}

	public Assistant(Path homeFolder, String openAiApiKey, DslProfile dsl, String modelName) {
		this.homeFolder = homeFolder;
		this.openAiApiKey = openAiApiKey;
		this.dsl = dsl;
		this.modelName = modelName;
		this.verifier = new SyntaxVerifier(dsl.getStandaloneSetupClass(), dsl.getFileExtension());
	}
	
	public void setEmbeddingStore(EmbeddingStore<TextSegment> embeddingStore) {
		this.embeddingStore = embeddingStore;
	}

	protected OpenAiChatModel buildOpenAiChatModel(Double temperature) {
		return OpenAiChatModel.builder().apiKey(openAiApiKey).modelName(modelName).temperature(temperature)
				.build();
	}

	protected ContentRetriever buildContentRetriever(EmbeddingStore<TextSegment> embeddingStore,
			EmbeddingModel embeddingModel, Integer maxResults, Double minScore) {
		return EmbeddingStoreContentRetriever.builder().embeddingStore(embeddingStore).embeddingModel(embeddingModel)
				.maxResults(maxResults).minScore(minScore).build();
	}

	protected <T> T buildAssistant(Class<T> assistantClass, Double temperature) {
		if (this.embeddingStore != null) {
			EmbeddingModel embeddingModel = OpenAiEmbeddingModel.builder().apiKey(openAiApiKey).modelName(EMBEDDING_MODEL_NAME)
					.build();
			ContentRetriever contentRetriever = buildContentRetriever(embeddingStore, embeddingModel, 5, 0.75);
			return AiServices.builder(assistantClass).chatLanguageModel(buildOpenAiChatModel(temperature))
					.contentRetriever(contentRetriever).build();
		} else {
			return AiServices.builder(assistantClass).chatLanguageModel(buildOpenAiChatModel(temperature)).build();
		}
	}
	
	protected String trimContextOverlap(String code, int offset, String suggestion) {
		String codeBeforeOffset = code.substring(0, offset);
		String codeAfterOffset = code.substring(offset);

		int prefixLength = 0;
		int maxPrefixLength = Math.min(suggestion.length(), codeBeforeOffset.length());
		for (int i = 1; i <= maxPrefixLength; i++) {
			if (suggestion.startsWith(codeBeforeOffset.substring(codeBeforeOffset.length() - i))) {
				prefixLength = i;
			}
		}

		suggestion = (suggestion.length() - prefixLength > 0) ? suggestion.substring(prefixLength) : "";

		int suffixLength = 0;
		int maxSuffixLength = Math.min(suggestion.length(), codeAfterOffset.length());
		for (int i = 1; i <= maxSuffixLength; i++) {
			if (suggestion.endsWith(codeAfterOffset.substring(0, i))) {
				suffixLength = i;
			}
		}

		return suggestion.substring(0, suggestion.length() - suffixLength);
	}

	protected Path getHomeFolder() {
		return homeFolder;
	}

	protected DslProfile getDslProfile() {
		return dsl;
	}
	
	protected String buildExamplesString() {
		List<ExternalFile> scriptExamples = dsl.getScriptExamples();
		StringBuilder examples = new StringBuilder();
		for (int i = 0; i < scriptExamples.size(); i++) {
			try {
				String content = scriptExamples.get(i).readContent(homeFolder);
				examples.append(String.format("### Example %d\n%s\n", i + 1, content));
			} catch (IOException e) {
				e.printStackTrace();
				return null;
			}
		}
		return examples.toString();
	}
	
	protected SyntaxVerifier getVerifier() {
		return verifier;
	}
}
