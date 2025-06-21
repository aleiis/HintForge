package dev.aleiis.hintforge.model;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;

public class ExternalFile {

    private String originalFileName;
    private String sourcePath;
    private String relativePath;

    public ExternalFile() {}

    public ExternalFile(String source, String originalFileName) {
        setSourcePath(source);
        setOriginalFileName(originalFileName);
        setRelativePath(null);
    }

    public String getOriginalFileName() {
        return originalFileName;
    }

    public void setOriginalFileName(String originalFileName) {
        if (originalFileName == null || originalFileName.isBlank()) {
            throw new IllegalArgumentException("originalFileName cannot be null or blank");
        }
        this.originalFileName = originalFileName;
    }

    public String getSourcePath() {
        return sourcePath;
    }

    public void setSourcePath(String sourcePath) {
        if (sourcePath == null || sourcePath.isBlank()) {
            throw new IllegalArgumentException("The source path cannot be null or blank");
        }
        try {
            Path.of(sourcePath);
        } catch (InvalidPathException e) {
            throw new IllegalArgumentException("The provided source path is not a valid path: " + sourcePath, e);
        }
        this.sourcePath = sourcePath;
    }

    public String getRelativePath() {
        return relativePath;
    }

    public void setRelativePath(String relativePath) {
        this.relativePath = relativePath;
    }

    /**
     * Reads the content of the file.
     * 
     * If relativePath is defined, the file is read relative to homeFolder, if not, the file is read from source.
     * 
     * @param homeFolder base folder for relative paths
     * 
     * @return the content of the file, or null if there was an error
     * @throws IOException if an error occurs while reading the file
     */
    public String readContent(Path homeFolder) throws IOException {
        Path readPath;
        if (relativePath != null && !relativePath.isBlank()) {
            readPath = homeFolder.resolve(relativePath);
        } else {
            readPath = Path.of(sourcePath);
        }

        return Files.readString(readPath);
    }
}
