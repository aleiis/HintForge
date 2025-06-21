package dev.aleiis.hintforge.preference;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Stream;

import org.eclipse.jface.preference.IPreferenceStore;

import dev.aleiis.hintforge.Activator;
import dev.aleiis.hintforge.model.DslProfile;
import dev.aleiis.hintforge.model.EmbeddingManager;
import dev.aleiis.hintforge.model.ExternalFile;
import dev.aleiis.hintforge.utils.FileUtils;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;

/**
 * Manages the storage directory used by the HintForge plugin. Ensures the
 * designated home folder is valid, available, and not used by other
 * applications.
 */
public class PreferenceManager {

	private static PreferenceManager instance;

	private HashMap<String, InMemoryEmbeddingStore<TextSegment>> embeddingStores = new HashMap<>();
	private IPreferenceStore store = null;
	private Path homeFolder = null;
	private boolean isHomeFolderAvailable = false;

	private PreferenceManager(IPreferenceStore store, Path homeFolder) {
		this.store = store;
		this.homeFolder = homeFolder;
		this.isHomeFolderAvailable = checkHomeFolder(homeFolder);
	}

	public static synchronized PreferenceManager getInstance(IPreferenceStore store, Path homeFolder) {
		if (instance == null) {
			instance = new PreferenceManager(store, homeFolder);
		}
		return instance;
	}

	public static PreferenceManager getInstance() {
		if (instance == null) {
			throw new IllegalStateException("PreferenceManager not initialized.");
		}
		return instance;
	}

	/**
	 * Checks if the provided home folder path is already in use by HintForge or at
	 * least is not used by any other application.
	 * 
	 * A folder is considered in use by HintForge if it contains a file named '.hf'
	 * with HintForge's plugin ID. If the folder is empty it will be considered as
	 * not in use. Consequently, if the folder contains at least one file but no
	 * '.hf' (or it does not contains HintForge's plugin ID) it will be considered
	 * as in use by other application.
	 *
	 * @param homeFolder the path to the proposed home folder
	 * @return {@code true} if the folder is valid and now assigned to the plugin;
	 *         {@code false} otherwise
	 * @throws IOException if there is an error accessing or writing to the file
	 *                     system
	 */
	public static boolean checkHomeFolder(Path homeFolder) {
		try {
			if (!Files.exists(homeFolder)) {
				Files.createDirectories(homeFolder);
			}

			if (!Files.isDirectory(homeFolder)) {
				return false;
			}

			Path signature = homeFolder.resolve(".hf");

			/*
			 * If the '.hf' file does not exist, it is checked if the folder is empty.
			 */
			if (!Files.exists(signature)) {
				try (Stream<Path> stream = Files.list(homeFolder)) {
					boolean isEmpty = !stream.findFirst().isPresent();
					if (isEmpty) {
						Files.writeString(signature, Activator.PLUGIN_ID);
						return true;
					} else {
						return false;
					}
				}
			}

			/*
			 * If the '.hf' file exist, it is checked that contains HintForge's plugin ID
			 */
			if (Activator.PLUGIN_ID.equals(Files.readString(signature))) {
				return true;
			}

			return false;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}

	public boolean isHomeFolderAvailable() {
		return isHomeFolderAvailable;
	}

	public Path getHomeFolder() {
		return homeFolder;
	}

	/**
	 * First, it checks if the provided home folder path is already in use by
	 * HintForge or at least is not used by any other application.
	 * 
	 * If the folder is not in use, all the contents of the Home folder will be
	 * moved to the new location and the new location will be saved as the actual
	 * one.
	 *
	 * @param newHomeFolder the path to the proposed home folder
	 * @return {@code true} if the folder is valid and now assigned to the plugin;
	 *         {@code false} otherwise
	 * @throws IOException if there is an error accessing or writing to the file
	 *                     system
	 */
	public boolean moveHomeFolder(Path newHomeFolder) {
		if (!checkHomeFolder(newHomeFolder)) {
			return false;
		}

		if (this.isHomeFolderAvailable) {
			// 1. Copy the contents
			try {
				Files.walkFileTree(homeFolder, new SimpleFileVisitor<Path>() {
					@Override
					public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
						Path targetPath = newHomeFolder.resolve(homeFolder.relativize(dir));
						Files.createDirectories(targetPath);
						return FileVisitResult.CONTINUE;
					}

					@Override
					public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
						Path targetPath = newHomeFolder.resolve(homeFolder.relativize(file));
						Files.copy(file, targetPath, StandardCopyOption.REPLACE_EXISTING);
						return FileVisitResult.CONTINUE;
					}
				});
			} catch (IOException e) {
				e.printStackTrace();
				return false;
			}

			// 2. Delete old folder
			try {
				Files.walkFileTree(homeFolder, new SimpleFileVisitor<Path>() {
					@Override
					public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
						Files.delete(file);
						return FileVisitResult.CONTINUE;
					}

					@Override
					public FileVisitResult postVisitDirectory(Path d, IOException exc) throws IOException {
						Files.delete(d);
						return FileVisitResult.CONTINUE;
					}
				});
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		this.homeFolder = newHomeFolder;
		store.setValue("HOME_FOLDER", newHomeFolder.toString());
		this.isHomeFolderAvailable = true;
		return true;
	}

	public String getApiKey() {
		return store.getString("API_KEY");
	}

	public void setApiKey(String apiKey) {
		store.setValue("API_KEY", apiKey);
	}

	public String getModelName() {
		return store.getString("MODEL_NAME");
	}

	public void setModelName(String modelName) {
		store.setValue("MODEL_NAME", modelName);
	}

	public DslProfile[] getDslProfiles() {
		String json = store.getString("DSL_PROFILES");
		DslProfile[] profiles = null;
		if (json != null && !json.isEmpty()) {
			profiles = DslProfile.fromJson(json);
		}
		for (DslProfile profile : profiles) {
			if (!embeddingStores.containsKey(profile.getName())) {
				loadEmbeddingStore(profile);
			}
		}
		return profiles;
	}

	public void saveDslProfiles(DslProfile[] profiles) throws IOException {
		Path profilesFolderPath = homeFolder.resolve("profiles");
		Files.createDirectories(profilesFolderPath);

		List<String> oldProfiles = FileUtils.getSubfolderNames(profilesFolderPath);

		for (DslProfile profile : profiles) {
			Path profileFolder = profilesFolderPath.resolve(profile.getName());
			Files.createDirectories(profileFolder);
			oldProfiles.remove(profile.getName());

			Path xtextFilePath = homeFolder.resolve("profiles").resolve(profile.getName()).resolve("grammar.xtext");
			syncFile(profile.getXtextFile(), xtextFilePath);

			Path examplesFolder = homeFolder.resolve("profiles").resolve(profile.getName()).resolve("examples");
			syncFilesWithFolder(profile.getScriptExamples(), examplesFolder);

			String apiKey = getApiKey();
			EmbeddingManager embeddingManager = profile.getEmbeddingManager();
			if (embeddingManager.hasPendingEmbeddings() && (apiKey == null || apiKey.isBlank())) {
				embeddingManager.clearPendingEmbeddings();
			}
			Path documentationFolder = homeFolder.resolve("profiles").resolve(profile.getName()).resolve("doc").resolve("data");
			syncFilesWithFolder(embeddingManager.getFiles(), documentationFolder);
			embeddingManager.sync(getEmbeddingStore(profile), homeFolder, apiKey);
			saveEmbeddingStore(profile);
		}

		for (String deletedProfile : oldProfiles) {
			Path deletedProfilePath = profilesFolderPath.resolve(deletedProfile);
			FileUtils.deleteDirectoryRecursively(deletedProfilePath);
			embeddingStores.remove(deletedProfile);
		}

		store.setValue("DSL_PROFILES", DslProfile.toJson(profiles));
	}

	private void syncFilesWithFolder(List<? extends ExternalFile> files, Path folder) throws IOException {
		if (FileUtils.isOutsideDirectory(folder, homeFolder)) {
			throw new IllegalArgumentException("Destination folder is not inside the home folder");
		}

		Files.createDirectories(folder);

		List<String> oldExamples = FileUtils.getFileNames(folder);

		/*
		 * The unsecured examples will be saved and the already secured ones will be
		 * discarded from the must-delete list
		 */
		for (ExternalFile file : files) {
			String relativePath = file.getRelativePath();
			if (relativePath == null) {
				Path source = Path.of(file.getSourcePath());
				String fileName = source.getFileName().toString();
				int dotExtensionIndex = fileName.lastIndexOf(".");
				String newFileName = fileName.substring(0, dotExtensionIndex) + "_" + System.currentTimeMillis()
						+ fileName.substring(dotExtensionIndex);
				Path target = folder.resolve(newFileName);
				Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
				file.setRelativePath(homeFolder.relativize(target).toString());
			} else {
				Path examplePath = homeFolder.resolve(relativePath);
				oldExamples.removeIf(oldExample -> oldExample.equals(examplePath.getFileName().toString()));
			}
		}

		/*
		 * After checking all the examples, all the unrecognized examples are deleted
		 */
		for (String deletedExample : oldExamples) {
			Path deletedExamplePath = folder.resolve(deletedExample);
			Files.deleteIfExists(deletedExamplePath);
		}
	}

	private void syncFile(ExternalFile file, Path target) throws IOException {
		if (FileUtils.isOutsideDirectory(target, homeFolder)) {
			throw new IllegalArgumentException("Target path is not inside the home folder");
		}

		if (file.getRelativePath() == null && file.getSourcePath() != null && !file.getSourcePath().isBlank()) {
			Path source = Path.of(file.getSourcePath());
			Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
			file.setRelativePath(homeFolder.relativize(target).toString());
		}
	}

	public InMemoryEmbeddingStore<TextSegment> getEmbeddingStore(DslProfile profile) {
		String profileName = profile.getName();
		if (!embeddingStores.containsKey(profileName)) {
			embeddingStores.put(profileName, new InMemoryEmbeddingStore<>());
		}
		return embeddingStores.get(profileName);
	}

	private void loadEmbeddingStore(DslProfile profile) {
		Path embeddingStorePath = homeFolder.resolve("profiles").resolve(profile.getName()).resolve("doc")
				.resolve("embeddings.json");
		if (Files.exists(embeddingStorePath)) {
			InMemoryEmbeddingStore<TextSegment> deserializedStore = InMemoryEmbeddingStore.fromFile(embeddingStorePath);
			embeddingStores.put(profile.getName(), deserializedStore);
		} else {
			embeddingStores.put(profile.getName(), new InMemoryEmbeddingStore<>());
		}
	}

	private void saveEmbeddingStore(DslProfile profile) {
		Path embeddingStorePath = homeFolder.resolve("profiles").resolve(profile.getName()).resolve("doc")
				.resolve("embeddings.json");
		InMemoryEmbeddingStore<TextSegment> embeddingStore = embeddingStores.get(profile.getName());
		embeddingStore.serializeToFile(embeddingStorePath);
	}
}
