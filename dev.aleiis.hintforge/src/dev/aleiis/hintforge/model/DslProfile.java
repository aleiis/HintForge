package dev.aleiis.hintforge.model;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;

public class DslProfile {

    private String name;
    private String description;
    private ExternalFile xtextFile;
    private String fileExtension;
    private String standaloneSetupClass = null;
    private List<ExternalFile> scriptExamples = new ArrayList<>();
    private EmbeddingManager embeddingManager = new EmbeddingManager();
    private ContextAwareCompletionConfig codeCompletionConfig = new ContextAwareCompletionConfig();
    private IdentifierSuggestionConfig identifierSuggestionConfig = new IdentifierSuggestionConfig();

    public DslProfile() {}

    public DslProfile(String name, String extension, ExternalFile xtextFile) {
        setName(name);
        setFileExtension(extension);
        setXtextFile(xtextFile);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("DSL profile name cannot be empty");
        }
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = (description != null) ? description : "";
    }

    public ExternalFile getXtextFile() {
        return xtextFile;
    }

    public void setXtextFile(ExternalFile xtextFile) {
        if (xtextFile == null || xtextFile.getSourcePath() == null || xtextFile.getSourcePath().isBlank()) {
            throw new IllegalArgumentException("xtextFile must have a valid source");
        }
        this.xtextFile = xtextFile;
    }

    public String getFileExtension() {
        return fileExtension;
    }

    public void setFileExtension(String ext) {
        if (ext == null || ext.isBlank()) {
            throw new IllegalArgumentException("File extension cannot be empty");
        }
        this.fileExtension = ext;
    }
    
    public String getStandaloneSetupClass() {
        return standaloneSetupClass;
    }

    public void setStandaloneSetupClass(String standaloneSetupClass) {
        if (standaloneSetupClass == null || standaloneSetupClass.isBlank()) {
            throw new IllegalArgumentException("The StandaloneSetup class name cannot be empty");
        }
        this.standaloneSetupClass = standaloneSetupClass;
    }

    public List<ExternalFile> getScriptExamples() {
        return scriptExamples;
    }

    public void setScriptExamples(List<ExternalFile> scriptExamples) {
        this.scriptExamples = (scriptExamples != null) ? scriptExamples : new ArrayList<>();
    }

	public EmbeddingManager getEmbeddingManager() {
        return embeddingManager;
    }

    public void setEmbeddingManager(EmbeddingManager embeddingManager) {
        this.embeddingManager = (embeddingManager != null) ? embeddingManager : new EmbeddingManager();
    }

	public ContextAwareCompletionConfig getCodeCompletionConfig() {
        return codeCompletionConfig;
    }

    public void setCodeCompletionConfig(ContextAwareCompletionConfig codeCompletionConfig) {
        this.codeCompletionConfig = (codeCompletionConfig != null) ? codeCompletionConfig : new ContextAwareCompletionConfig();
    }
    
	public IdentifierSuggestionConfig getIdentifierSuggestionConfig() {
        return identifierSuggestionConfig;
    }

    public void setIdentifierSuggestionConfig(IdentifierSuggestionConfig identifierSuggestionConfig) {
        this.identifierSuggestionConfig = (identifierSuggestionConfig != null) ? identifierSuggestionConfig : new IdentifierSuggestionConfig();
    }

    public static DslProfile[] fromJson(String json) {
        if (json != null && !json.isEmpty()) {
            DslProfile[] profiles = new Gson().fromJson(json, DslProfile[].class);
            return profiles;
        }
        return new DslProfile[0];
    }

    public static String toJson(DslProfile[] profiles) {
        return new Gson().toJson(profiles);
    }
}

