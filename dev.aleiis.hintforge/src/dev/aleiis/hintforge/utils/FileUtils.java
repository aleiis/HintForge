package dev.aleiis.hintforge.utils;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

public class FileUtils {

	public static List<String> getSubfolderNames(Path parentFolder) throws IOException {
		List<String> folderNames = new ArrayList<>();

		try (DirectoryStream<Path> stream = Files.newDirectoryStream(parentFolder)) {
			for (Path entry : stream) {
				if (Files.isDirectory(entry)) {
					folderNames.add(entry.getFileName().toString());
				}
			}
		}

		return folderNames;
	}

	public static List<String> getFileNames(Path parentFolder) throws IOException {
		List<String> fileNames = new ArrayList<>();

		try (DirectoryStream<Path> stream = Files.newDirectoryStream(parentFolder)) {
			for (Path file : stream) {
				if (!Files.isDirectory(file)) {
					fileNames.add(file.getFileName().toString());
				}
			}
		}

		return fileNames;
	}

	public static boolean isOutsideDirectory(Path filePath, Path folderPath) throws IOException {
		Path normalizedFolder = Files.exists(folderPath) ? folderPath.toRealPath() : folderPath.toAbsolutePath().normalize();
		Path normalizedFile = Files.exists(filePath) ? filePath.toRealPath() : filePath.toAbsolutePath().normalize();
		return !normalizedFile.startsWith(normalizedFolder);
	}

	public static void deleteDirectoryRecursively(Path folderPath) throws IOException {
		Files.walkFileTree(folderPath, new SimpleFileVisitor<Path>() {

			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				Files.delete(file);
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
				Files.delete(dir);
				return FileVisitResult.CONTINUE;
			}

		});

	}
}
