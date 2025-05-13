package qupath.ext.ergonomictoolbar.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Utility class for file operations.
 */
public class FileUtils {
  //public static final String FILE_SAVE =
  // "\QuPath_Stage_4A\qupath-app\QuPath_ErgonomicToolbar_Extension\save";
  public static final Path FILE_PATH_SAVE =
      Path.of(System.getProperty("user.dir") + "/QuPath_ErgonomicToolbar_Extension/save/");
  //public static final String FILE_PATH_AREAS =
  // "qupath-extension-ergonomic-tool-bar/src/main/java/qupath
  // /ext/ergonomictoolbar/save/annotation_areas.txt";
  public static final Path FILE_PATH_AREAS = Path.of(FILE_PATH_SAVE + "annotation_areas.txt");
  //public static final String FILE_PATH_TILE =
  // "qupath-extension-ergonomic-tool-bar/src/main/java/qupath
  // /ext/ergonomictoolbar/save/tile_area.txt";
  public static final Path FILE_PATH_TILE = Path.of(FILE_PATH_SAVE + "tile_area.txt");
  //public static final String FILE_PATH_MODEL =
  // "qupath-extension-ergonomic-tool-bar/src/main/java/qupath
  // /ext/ergonomictoolbar/save/model_path.txt";
  public static final Path FILE_PATH_MODEL = Path.of(FILE_PATH_SAVE + "model_path.txt");
  //public static final String FILE_PATH_EXTRACTED_IMAGES =
  // "qupath-extension-ergonomic-tool-bar/src/main/java/qupath
  // /ext/ergonomictoolbar/save/extracted_images";
  public static final Path FILE_PATH_EXTRACTED_IMAGES =
      Path.of(FILE_PATH_SAVE + "extracted_images");
  //public static final String FILE_PATH_VIEWER =
  // "qupath-extension-ergonomic-tool-bar/src/main/resources/qupath/ext
  // /ergonomictoolbar/controllers/python/viewer.exe";
  public static final Path FILE_PATH_VIEWER = Path.of(System.getProperty("user.dir")
      + "/QuPath_ErgonomicToolbar_Extension/src/main/resources"
      + "/qupath/ext/ergonomictoolbar/controllers/python/viewer.exe");


  public static final String NECROTIC_SUBDIRECTORY = "Necrotic";
  public static final String VIABLE_SUBDIRECTORY = "Viable";
  public static final String OTHER_SUBDIRECTORY = "Other";


  /**
   * Save a list of strings to a file.
   * This method will overwrite the file with the provided list of strings.
   *
   * @param strings List of strings to be saved to the file.
   */
  private static void saveStringsToFile(List<String> strings, Path filePath) {
    if (!Files.exists(filePath)) {
      System.err.println("Le fichier " + filePath + " n'existe pas.");
      return;
    }

    try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath.toString()))) {
      for (String str : strings) {
        writer.write(str);
        writer.newLine();
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /**
   * Read all strings from a file.
   * This method reads the file line by line and returns a list of strings.
   *
   * @return List of strings read from the file.
   */
  public static List<String> readStringsFromFile(Path filePath) {
    List<String> strings = new ArrayList<>();

    if (!Files.exists(filePath)) {
      System.err.println("Le fichier " + filePath + " n'existe pas.");
      return strings;
    }

    try (BufferedReader reader = new BufferedReader(new FileReader(filePath.toString()))) {
      String line;
      while ((line = reader.readLine()) != null) {
        strings.add(line);
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    return strings;
  }

  /**
   * Add a new string to the file.
   * This method reads the current strings from the file, adds the new string,
   * and saves all the strings back to the file.
   *
   * @param str The new string to be added to the file.
   */
  public static void addStringToFile(String str, Path filePath) {
    if (!Files.exists(filePath)) {
      System.err.println("This file does not exist : " + filePath);
      return;
    }

    List<String> strings = readStringsFromFile(filePath);
    strings.add(str);
    saveStringsToFile(strings, filePath);
  }

  /**
   * Remove a string from the file.
   * This method reads the current strings from the file, removes the specified string,
   * and saves the updated list of strings back to the file.
   *
   * @param str The string to be removed from the file.
   */
  public static void removeStringFromFile(String str, Path filePath) {
    if (!Files.exists(filePath)) {
      System.err.println("This file does not exist : " + filePath);
      return;
    }

    List<String> strings = readStringsFromFile(filePath);
    strings.remove(str);
    saveStringsToFile(strings, filePath);
  }

  /**
   * Check if the input string is already in the areas list.
   *
   * @param str      The area to check.
   * @param filePath The path of the file to check.
   * @return True if the string is already present, otherwise false.
   */
  public static boolean isStringPresentInFile(String str, Path filePath) {
    return Files.exists(filePath) && FileUtils.readStringsFromFile(filePath).contains(str);
  }

  /**
   * Method that allows to clear a file.
   */
  public static void clearFile(Path filePath) {
    if (!Files.exists(filePath)) {
      System.err.println("This file does not exist : " + filePath);
      return;
    }

    List<String> strings = new ArrayList<>();
    saveStringsToFile(strings, filePath);
  }

  /**
   * Method that allows to delete a directory.
   */
  public static void deleteDirectory(Path parentDirectory, String folderName) throws IOException {
    Path path =
        Paths.get(parentDirectory.toString(), folderName); // Convertit le nom du répertoire en Path

    // Vérifier si le répertoire existe
    if (!Files.exists(path)) {
      System.err.println("This file does not exist : " + path);
      return;
    }

    if (!Files.isDirectory(path)) {
      System.err.println("This file is not a directory : " + path);
      return;
    }

    // Utiliser Files.walkFileTree pour parcourir et supprimer les fichiers
    Files.walkFileTree(path, new SimpleFileVisitor<>() {
      @Override
      public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        Files.delete(file); // Supprimer le fichier
        return FileVisitResult.CONTINUE;
      }

      @Override
      public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
        Files.delete(dir); // Supprimer le répertoire après avoir supprimé tout son contenu
        return FileVisitResult.CONTINUE;
      }
    });
  }

  /**
   * Method that allows to know if a file is empty or not.
   */
  public static boolean isFileEmpty(Path filePath) {
    File file = new File(filePath.toString());
    return file.length() == 0;
  }

  /**
   * Method that allows to create a file.
   */
  public static void createFile(Path filePath) {
    try {
      if (Files.notExists(filePath)) {
        Files.createFile(filePath);
        System.out.println("File created : " + filePath);
      } else {
        System.out.println("File already exists : " + filePath);
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Method that allows to create a directory.
   */
  public static void createDirectory(Path directoryPath) {
    try {
      // Vérifier si le répertoire n'existe pas
      if (Files.notExists(directoryPath)) {
        // Créer le répertoire
        Files.createDirectories(directoryPath);
        System.out.println("Directory created: " + directoryPath);
      } else {
        System.out.println("Directory already exists: " + directoryPath);
      }
    } catch (IOException e) {
      // En cas d'erreur, lever une exception Runtime
      throw new RuntimeException("Failed to create directory: " + directoryPath, e);
    }
  }

  /**
   * Method that checks a directory has the expected sub-directories.
   */
  public static boolean checkSubdirectories(File directory, String[] requiredSubdirectories) {
    // Check if the provided path is a directory
    if (!directory.isDirectory()) {
      return false;
    }

    File[] files = directory.listFiles();

    // Return false if the directory cannot be read or is empty
    if (files == null) {
      return false;
    }

    // Create an array to track the presence of required subdirectories
    boolean[] areSubDirectoriesPresent = new boolean[requiredSubdirectories.length];

    // Iterate over all files and directories in the provided directory
    for (File file : files) {
      if (file.isDirectory()) {
        // Check if the directory matches any of the required subdirectories
        for (int i = 0; i < requiredSubdirectories.length; i++) {
          if (file.getName().equals(requiredSubdirectories[i])) {
            // Mark the corresponding subdirectory as present
            areSubDirectoriesPresent[i] = true;
          }
        }
      }
    }

    // Check if all required subdirectories are present
    for (boolean present : areSubDirectoriesPresent) {
      if (!present) {
        return false;
      }
    }
    return true;
  }

  /**
   * Method that count the files created before a date.
   */
  public static int countFilesBeforeDate(File directory, LocalDateTime lastModelDate) {
    if (directory == null || !directory.exists() || !directory.isDirectory()) {
      return 0;
    }

    return (int) listFiles(directory).stream()
        .filter(file -> getCreationDate(file).isBefore(lastModelDate))
        .count();
  }

  /**
   * Method that count the files created after a date.
   */
  public static int countFilesAfterDate(File directory, LocalDateTime lastModelDate) {
    if (directory == null || !directory.exists() || !directory.isDirectory()) {
      return 0;
    }

    return (int) listFiles(directory).stream()
        .filter(file -> getCreationDate(file).isAfter(lastModelDate))
        .count();
  }

  /**
   * Method that list all files from a directory into a List.
   */
  public static List<File> listFiles(File directory) {
    return Arrays.asList(Objects.requireNonNull(directory.listFiles()));
  }

  /**
   * Method that allows to get the creation date of a file as LocalDateTime.
   */
  public static LocalDateTime getCreationDate(File file) {
    // Check if the file object is null or does not exist
    if (file == null || !file.exists()) {
      return LocalDateTime.now();
    }

    // Check if the path is a file (not a directory)
    if (!file.isFile()) {
      return LocalDateTime.now();
    }
    try {
      Path filePath = file.toPath();
      BasicFileAttributes attrs = Files.readAttributes(filePath, BasicFileAttributes.class);
      return LocalDateTime.ofInstant(attrs.lastModifiedTime().toInstant(), ZoneId.systemDefault());
    } catch (IOException e) {
      return LocalDateTime.now();
    }
  }
}
