package qupath.ext.ergonomictoolbar;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javax.imageio.ImageIO;
import qupath.ext.ergonomictoolbar.utils.AlertUtils;
import qupath.ext.ergonomictoolbar.utils.AnnotationUtils;
import qupath.ext.ergonomictoolbar.utils.FileUtils;
import qupath.ext.ergonomictoolbar.utils.TilerUtils;
import qupath.lib.gui.QuPathGUI;
import qupath.lib.images.ImageData;
import qupath.lib.images.servers.ImageServer;
import qupath.lib.objects.PathObject;
import qupath.lib.objects.PathObjects;
import qupath.lib.objects.classes.PathClass;
import qupath.lib.objects.hierarchy.PathObjectHierarchy;
import qupath.lib.regions.RegionRequest;
import qupath.lib.roi.interfaces.ROI;
import qupath.lib.scripting.QP;

/**
 * Class that contains the methods for the automatic annotation of the images.
 */
public class Ia {
  private static final int NB_THREADS = Runtime.getRuntime().availableProcessors();

  //To define the space between tiles, please change the "spacing" value right below
  //A low value means you might need to zoom in order to make tiles more identifiable
  //A high value will make the Ia lose more information dring the analysis
  //A null or negative value will make tiles superpose, while functional, it's best to avoid that
  //1 is the desired minimum, 20 is a good value for visibility
  private static final int spacing = 20;

  private static int tiles_count = 0;

  /**
   * Separate a "Tumor Area" annotation into 2 categories (viable tumor and necrosis).
   */
  public static void automaticAnnotation() {
    try {
      // Deleting of old images
      FileUtils.deleteDirectory(FileUtils.FILE_PATH_SAVE, "extracted_images");
      //FileUtils.deleteDirectory(FileUtils.FILE_PATH_SAVE, "/batch_images.zip");

      PathObjectHierarchy hierarchy = QP.getCurrentHierarchy();

      // Check if a project is opened
      if (hierarchy == null) {
        AlertUtils.noOpenProject();
        return;
      }

      Collection<PathObject> annotations = hierarchy.getAnnotationObjects();

      // Check if there is no tumor area present in the annotations
      if (!AnnotationUtils.isAnnotationClassPresent(annotations, "Tumor Area")) {
        AlertUtils.noTumorArea();
        return;
      }

      if (FileUtils.readStringsFromFile(FileUtils.FILE_PATH_MODEL).isEmpty()) {
        AlertUtils.noModelPath();
        return;
      }

      AlertUtils.computationStarted();
      QuPathGUI gui = QuPathGUI.getInstance();
      ImageData<BufferedImage> imageData = gui.getImageData();
      startAnnotationTask(imageData, annotations);
    } catch (IOException e) {
      AlertUtils.computationCanceled();
    }
  }

  /**
   * Method that create a task for processing annotations.
   *
   * @param imageData   The data containing the image to which the annotations belong.
   * @param annotations A collection of annotations to be processed.
   */
  private static void startAnnotationTask(ImageData<BufferedImage> imageData,
                                          Collection<PathObject> annotations) {
    // Create a new task for processing annotations
    Task<Void> task = new Task<>() {
      @Override
      protected Void call() {
        // Run the annotation processing on the JavaFX application thread
        Platform.runLater(() -> processAnnotations(imageData, annotations));
        return null;
      }

      @Override
      protected void succeeded() {
        AlertUtils.computationCompleted();
      }

      @Override
      protected void failed() {
        AlertUtils.computationFailed();
      }
    };

    // Create the thread that will do the task
    Thread thread = new Thread(task);
    thread.setDaemon(true);
    thread.start();
  }

  /**
   * Method that create the tiles for the annotations.
   *
   * @param imageData   The data containing the image to which the annotations belong.
   * @param annotations A collection of annotations to be processed.
   * @return the list of annotated tiles
   */
  public static List<PathObject> getAnnotedTiles(ImageData<BufferedImage> imageData,
                                                 Collection<PathObject> annotations) {
    AnnotationUtils.removeClassAnnotations(annotations, "Tumor");
    AnnotationUtils.removeClassAnnotations(annotations, "Necrosis");
    AnnotationUtils.removeClassAnnotations(annotations, "Other");

    List<PathObject> allTiles = new ArrayList<>();

    // Iterate over each annotation
    for (PathObject annotation : annotations) {
      // Check if the annotation is classified as "Tumor Area"
      if (annotation.getPathClass() != null
          && Objects.equals(annotation.getPathClass().getName(), "Tumor Area")) {
        // Create tiles
        List<PathObject> tiles = TilerUtils.tilesCreator(imageData, annotation, 224, spacing);

        // If no tiles are created, show an alert indicating the selected area is too small
        if (tiles == null) {
          AlertUtils.selectedAreaTooSmall();
        } else {
          allTiles.addAll(tiles);
        }
      }
    }

    return allTiles;
  }

  /**
   * Method that starts the processing annotations.
   *
   * @param imageData   The data containing the image to which the annotations belong.
   * @param annotations A collection of annotations to be processed.
   */
  private static void processAnnotations(ImageData<BufferedImage> imageData,
                                         Collection<PathObject> annotations) {
    List<PathObject> allTiles = getAnnotedTiles(imageData, annotations);

    // If there are any tiles created, process them in batches
    if (!allTiles.isEmpty()) {
      processTileBatch(imageData, allTiles);
    }

    ToolbarFeatures.displayNecrosisRate();
  }

  /**
   * Method that do the processing for a batch.
   *
   * @param imageData The data containing the image to which the annotations belong.
   * @param tiles     The list of annotated tiles to be processed
   */
  private static void processTileBatch(ImageData<BufferedImage> imageData, List<PathObject> tiles) {
    List<PathObject> annotationTiles = new ArrayList<>();
    ImageServer<BufferedImage> server = imageData.getServer();

    File zipFile = createZipFile(server, tiles);
    Map<String, String> results = executePythonScript(zipFile);
    boolean b = zipFile.delete();
    if (!b) {
      System.err.println("Error deleting ZIP file");
    }

    // Process each tile and create annotations based on the results
    for (PathObject tile : tiles) {
      ROI tileRoi = tile.getROI();
      PathObject annotationTile = PathObjects.createAnnotationObject(tileRoi);
      String tileName = tile.getName() + ".tif";
      String result = results.get(tileName);

      if (result != null) {
        try {
          int classification = Integer.parseInt(result);

          // Set the path class based on the classification result
          if (classification == 0) {
            annotationTile.setPathClass(PathClass.fromString("Tumor"));
          } else if (classification == 1) {
            annotationTile.setPathClass(PathClass.fromString("Necrosis"));
          } else if (classification == 2) {
            annotationTile.setPathClass(PathClass.fromString("Other"));
          }
        } catch (NumberFormatException e) {
          System.err.println("Number format error for file " + tileName + ": " + result);
        }
      }
      annotationTiles.add(annotationTile);
    }
    imageData.getHierarchy().addObjects(annotationTiles);
  }

  /**
   * Method that create the zip file with all the tile image ".tif".
   *
   * @param server The server used to retrieve and process the tiles' images.
   * @param tiles  A list of {@code PathObject} representing the tiles to
   *               be included in the ZIP file.
   * @return A File object representing the created ZIP file, named "batch_images.zip".
   * @throws RuntimeException If an error occurs during the creation of the
   *                          ZIP file or processing of tiles.
   */
  private static File createZipFile(ImageServer<BufferedImage> server,
                                    List<PathObject> tiles) {
    File zipFile = new File("batch_images.zip");
    int tilesPerThread = (int) Math.ceil((double) tiles.size() / NB_THREADS);
    List<Thread> listThreads = new ArrayList<>();

    // Synchronisation pour l'accès à ZipOutputStream
    final Object zipLock = new Object();

    try (FileOutputStream fos = new FileOutputStream(zipFile);
         ZipOutputStream zos = new ZipOutputStream(fos)) {

      // Diviser les tuiles en sous-listes et créer un thread pour chaque sous-liste
      for (int i = 0; i < NB_THREADS; i++) {
        int start = i * tilesPerThread;
        int end = Math.min(start + tilesPerThread, tiles.size());

        // Vérifier que start est inférieur à end avant de créer la sous-liste
        if (start < end) {
          List<PathObject> subList = tiles.subList(start, end);

          Thread thread = new Thread(() -> {
            for (PathObject tile : subList) {
              BufferedImage image;
              try {
                image = createImageWithRequest(server, tile.getROI(), 20);
              } catch (IOException e) {
                throw new RuntimeException(e);
              }
              String tileName = tile.getName();

              // S'assurer que le nom du fichier n'est pas null
              if (tileName != null) {
                // Créer un fichier temporaire pour l'image TIFF
                File oldFile;
                File tempFile = null;

                try {
                  oldFile = File.createTempFile(tileName, ".tif");
                  tempFile = new File(oldFile.getParent(), tileName + tiles_count + ".tif");
                  tiles_count++;
                  oldFile.delete();  // Supprimer l'ancien fichier temporaire

                  ImageIO.write(image, "TIFF", tempFile);

                  // Ajouter le fichier temporaire à l'archive ZIP
                  try (FileInputStream fis = new FileInputStream(tempFile)) {
                    synchronized (zipLock) {
                      ZipEntry zipEntry = new ZipEntry(tempFile.getName());
                      zos.putNextEntry(zipEntry);
                      byte[] buffer = new byte[1024];
                      int length;
                      while ((length = fis.read(buffer)) > 0) {
                        zos.write(buffer, 0, length);
                      }
                      zos.closeEntry();
                    }
                  }
                } catch (IOException e) {
                  e.printStackTrace();
                } finally {
                  if (tempFile != null) {
                    tempFile.delete();  // Supprimer le fichier temporaire après usage
                  }
                }
              }
            }
          });

          listThreads.add(thread);
          thread.start();
        }
      }

      // Attendre que tous les threads se terminent
      for (Thread thread : listThreads) {
        try {
          thread.join();
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }

    } catch (IOException e) {
      throw new RuntimeException("Erreur lors de la création du fichier ZIP", e);
    }
    tiles_count = 0;
    return zipFile;
  }


  /**
   * Method that create a image with a request of a ROI.
   *
   * @param server  The server used to access the image data.
   * @param tileRoi The ROI (region of interest) to extract from the image.
   * @param scale   The scaling factor to apply when creating the image
   * @return A BufferedImage object representing the extracted and scaled region of interest.
   * @throws IOException If an error occurs while reading the region from the image server.
   *
   */
  public static BufferedImage createImageWithRequest(ImageServer<BufferedImage> server, ROI tileRoi,
                                                     double scale) throws IOException {
    RegionRequest regionRequest = RegionRequest.createInstance(
        server.getPath(),
        scale,
        (int) tileRoi.getBoundsX(),
        (int) tileRoi.getBoundsY(),
        (int) tileRoi.getBoundsWidth(),
        (int) tileRoi.getBoundsHeight()
    );

    return server.readRegion(regionRequest);
  }

  /**
   * Method that create a image with a request of a ROI.
   *
   * @param server  The server used to access the image data. Must not be null.
   * @param tileRoi The ROI (region of interest) to extract from the image.
   * @return A BufferedImage object representing the extracted region of interest
   * @throws IOException IOException
   */
  public static BufferedImage createImageWithRequest(ImageServer<BufferedImage> server, ROI tileRoi)
      throws IOException {
    return Ia.createImageWithRequest(server, tileRoi, 1);
  }

  /**
   * Method that execute the python script of the Ia model.
   *
   * @param zipFile The file representing the ZIP file to be processed by the Python script.
   * @return A Map containing the results of the script execution.
   * @throws RuntimeException If an error occurs during script extraction, execution,
   *                          or output file reading.
   */
  private static Map<String, String> executePythonScript(File zipFile) {
    String nasPath = FileUtils.readStringsFromFile(FileUtils.FILE_PATH_MODEL).getFirst();

    // Extraire le script Python à un fichier temporaire
    URL scriptUrl = Ia.class.getResource("controllers/python/Classification.exe");
    if (scriptUrl == null) {
      throw new RuntimeException("Python script not found in JAR");
    }

    Path tempScriptPath;
    try {
      tempScriptPath = Files.createFile(Path.of("./Classification.exe"));
      try (InputStream in = scriptUrl.openStream()) {
        Files.copy(in, tempScriptPath, StandardCopyOption.REPLACE_EXISTING);
      }
    } catch (IOException e) {
      throw new RuntimeException("Error extracting Python script", e);
    }

    // Construire la commande pour exécuter le script Python
    String[] command = {
        "cmd.exe", "/c",
        tempScriptPath.toString(),
        zipFile.getAbsolutePath(),
        nasPath,
    };

    // Créer un ProcessBuilder pour exécuter la commande
    ProcessBuilder pb = new ProcessBuilder(command);
    pb.redirectErrorStream(true);

    // Définir le fichier de sortie
    File outputFile = new File(nasPath, "output.txt");

    try {
      // Démarrer le processus
      Process process = pb.start();

      // Écrire la sortie du processus dans le fichier existant
      try (BufferedReader reader = new BufferedReader(
          new InputStreamReader(process.getInputStream()));
           BufferedWriter writer = new BufferedWriter(
               new FileWriter(outputFile, true))) {  // true pour le mode append

        String line;
        while ((line = reader.readLine()) != null) {
          writer.write(line);
          writer.newLine();
        }
      }

      // Attendre la fin du processus
      process.waitFor();
    } catch (IOException | InterruptedException e) {
      throw new RuntimeException("Error executing Python script", e);
    } finally {
      // Supprimer le fichier temporaire
      try {
        Files.deleteIfExists(tempScriptPath);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    // Lire le fichier de sortie pour analyser les résultats
    Map<String, String> results = new HashMap<>();
    try (BufferedReader fileReader = new BufferedReader(new FileReader(outputFile))) {
      String line;
      while ((line = fileReader.readLine()) != null) {
        if (line.trim().isEmpty()) {
          continue;
        }

        // Diviser chaque ligne en paires clé-valeur
        String[] parts = line.split(":");
        if (parts.length == 2) {
          results.put(parts[0].trim(), parts[1].trim());
        }
      }
    } catch (IOException e) {
      throw new RuntimeException("Error reading output file", e);
    }
    return results;
  }

  /**
   * Merge "Tumor" and "Necrosis" annotations.
   */
  public static void mergeTumorAndNecrosis() {
    PathObjectHierarchy hierarchy = QP.getCurrentHierarchy();

    if (hierarchy == null) {
      AlertUtils.noImageOpen();
      return;
    }

    AnnotationUtils.mergeAnnotations("Tumor", hierarchy);
    AnnotationUtils.mergeAnnotations("Necrosis", hierarchy);
  }
}
