package qupath.ext.ergonomictoolbar;

import static qupath.lib.gui.scripting.QPEx.getQuPath;
import static qupath.lib.scripting.QP.getCurrentHierarchy;
import static qupath.lib.scripting.QP.refreshIDs;

import java.awt.Shape;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Objects;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.paint.Color;
import javax.imageio.ImageIO;
import qupath.ext.ergonomictoolbar.controllers.AnnotationCreationController;
import qupath.ext.ergonomictoolbar.controllers.ToolbarController;
import qupath.ext.ergonomictoolbar.utils.AlertUtils;
import qupath.ext.ergonomictoolbar.utils.AnnotationUtils;
import qupath.ext.ergonomictoolbar.utils.FileUtils;
import qupath.lib.gui.QuPathGUI;
import qupath.lib.gui.viewer.QuPathViewer;
import qupath.lib.gui.viewer.QuPathViewerListener;
import qupath.lib.gui.viewer.tools.PathTools;
import qupath.lib.images.ImageData;
import qupath.lib.images.servers.ImageServer;
import qupath.lib.images.servers.PixelCalibration;
import qupath.lib.objects.PathAnnotationObject;
import qupath.lib.objects.PathObject;
import qupath.lib.objects.PathObjects;
import qupath.lib.objects.classes.PathClass;
import qupath.lib.objects.hierarchy.PathObjectHierarchy;
import qupath.lib.regions.ImagePlane;
import qupath.lib.roi.ROIs;
import qupath.lib.roi.interfaces.ROI;
import qupath.lib.scripting.QP;

/**
 * Class that contains the features of the toolbar.
 */
public class ToolbarFeatures {
  /**
   * Method that allows to change the class of an annotation if CTRL Q or CTRL W
   * pressed and to save it as a .tif in a folder if CTRL Q pressed.
   */
  public static void addActionOnCtrlQandW() {
    if (getCurrentHierarchy() != null) {
      Collection<PathObject> selectedAnnotations =
          getCurrentHierarchy().getSelectionModel().getSelectedObjects();

      if (selectedAnnotations != null) {
        KeyCombination ctrlQ = new KeyCodeCombination(KeyCode.Q, KeyCombination.CONTROL_DOWN);
        KeyCombination ctrlW = new KeyCodeCombination(KeyCode.W, KeyCombination.CONTROL_DOWN);
        getQuPath().getStage().getScene().setOnKeyPressed(event -> {
          if (ctrlQ.match(event) || ctrlW.match(event)) {
            String folderPath = null;

            if (!FileUtils.readStringsFromFile(FileUtils.FILE_PATH_MODEL).isEmpty()) {
              folderPath = FileUtils.readStringsFromFile(FileUtils.FILE_PATH_MODEL).get(0);
            }

            for (PathObject annotation : selectedAnnotations) {
              String className = annotation.getPathClass().getName();
              String subfolderPath;

              QuPathGUI gui = QuPathGUI.getInstance();
              ImageData<BufferedImage> imageData = gui.getImageData();
              ImageServer<BufferedImage> server = imageData.getServer();

              if (Objects.equals(className, "Tumor")) {
                if (ctrlW.match(event)) {
                  annotation.setPathClass(PathClass.fromString("Necrosis"));
                }

                if (ctrlQ.match(event)) {
                  // We check that there is a path for the model
                  if (folderPath != null) {
                    annotation.setPathClass(PathClass.fromString("Necrosis"));
                    subfolderPath = folderPath + "/Necrotic";
                    saveAnnotationAsTiff(server, annotation, subfolderPath);
                  } else {
                    AlertUtils.noModelPath();
                  }
                }
              } else if (Objects.equals(className, "Necrosis")) {

                if (ctrlW.match(event)) {
                  annotation.setPathClass(PathClass.fromString("Tumor"));
                }

                if (ctrlQ.match(event)) {

                  // We check that there is a path for the model
                  if (folderPath != null) {
                    annotation.setPathClass(PathClass.fromString("Tumor"));
                    subfolderPath = folderPath + "/Viable";
                    saveAnnotationAsTiff(server, annotation, subfolderPath);
                  } else {
                    AlertUtils.noModelPath();
                  }
                }
              }
            }
            refreshIDs();
          }
        });
      }
    }
  }

  /**
   * Save an annotation as a tiff image.
   *
   * @param server      The image server containing the source data.
   * @param annotation  The annotation object to export. Must not be null and
   *                    must contain a valid ROI.
   * @param outputPath  The directory where the image will be saved. The directory must exist.
   * @throws RuntimeException if an error occurs during the export process
   */
  private static void saveAnnotationAsTiff(ImageServer<BufferedImage> server, PathObject annotation,
                                           String outputPath) {
    try {
      // Create an image from the annotation
      BufferedImage image = Ia.createImageWithRequest(server, annotation.getROI(), 20);
      String tileName = annotation.getPathClass().getName() + countFilesInDirectory(outputPath);

      File outputFile = new File(outputPath, tileName + ".tif");
      ImageIO.write(image, "TIFF", outputFile);

    } catch (IOException e) {
      throw new RuntimeException("Error while saving image", e);
    }
  }

  /**
   * Counts the number of files in a specified directory.
   *
   * @param folderPath The path to the folder in which files should be counted.
   *                   Must be a valid directory path, not null.
   * @return The number of files in the specified directory.
   *         Returns 0 if the directory is empty or contains no files.
   * @throws IllegalArgumentException If the specified path is not a directory.
   */
  public static int countFilesInDirectory(String folderPath) {
    File folder = new File(folderPath);
    // Vérifiez si le chemin est un répertoire
    if (!folder.isDirectory()) {
      throw new IllegalArgumentException("Le chemin spécifié n'est pas un répertoire.");
    }

    // Liste des fichiers dans le répertoire
    File[] files = folder.listFiles(File::isFile);

    // Vérifiez si la liste n'est pas nulle et renvoyez le nombre de fichiers
    if (files != null) {
      return files.length;
    } else {
      return 0;
    }
  }

  /**
   * Actualize the necrosis rate when ctrl z is pressed.
   */
  public static void addActionOnCtrlZ() {
    // Add the event handler for CTRL + Z
    KeyCombination ctrlZ = new KeyCodeCombination(KeyCode.Z, KeyCombination.CONTROL_DOWN);
    getQuPath().getStage().getScene().setOnKeyPressed(event -> {
      if (ctrlZ.match(event)) {
        displayNecrosisRate();
      }
    });
  }

  /**
   * Add listener for any change on the hierarchy.
   */
  public static void addHierarchyListener() {
    if (QP.getCurrentHierarchy() != null) {
      QP.getCurrentHierarchy().addListener(observable -> displayNecrosisRate());
    }
  }

  /**
   * Add listener for change on the viewer.
   */
  public static void addViewerListener() {
    getQuPath().getViewer().addViewerListener(new QuPathViewerListener() {
      @Override
      public void imageDataChanged(QuPathViewer viewer, ImageData<BufferedImage> imageDataOld,
                                   ImageData<BufferedImage> imageDataNew) {
        displayNecrosisRate();
        addHierarchyListener();
        addActionOnCtrlZ();
        ToolbarFeatures.addActionOnCtrlQandW();
      }

      @Override
      public void visibleRegionChanged(QuPathViewer viewer, Shape shape) {
        displayNecrosisRate();
        addActionOnCtrlZ();
        ToolbarFeatures.addActionOnCtrlQandW();
      }

      @Override
      public void selectedObjectChanged(QuPathViewer viewer, PathObject pathObjectSelected) {
        displayNecrosisRate();
        addActionOnCtrlZ();
        ToolbarFeatures.addActionOnCtrlQandW();
      }

      @Override
      public void viewerClosed(QuPathViewer viewer) {
        // ...
      }
    });
  }

  /**
   * Set the active tool to the predefined selection brush tool.
   */
  public static void createSelectionBrushAnnotation() {
    // Check that an image is open
    if (getCurrentHierarchy() == null) {
      AlertUtils.noImageOpen();
      return;
    }

    // Check that the GUI exists
    QuPathGUI gui = QuPathGUI.getInstance();
    if (gui == null) {
      AlertUtils.noGui();
      return;
    }

    // Check that the viewer exists
    QuPathViewer viewer = gui.getViewer();
    if (viewer == null) {
      AlertUtils.noViewer();
      return;
    }

    // If you are already set to the brush tool
    if (viewer.getActiveTool() == PathTools.BRUSH) {
      // Switch to the move tool
      viewer.setActiveTool(PathTools.MOVE);
      gui.getToolManager().setSelectedTool(PathTools.MOVE);
      // Set the selection mode of the active tool on false
      gui.getToolManager().getSelectionModeAction().setSelected(false);
      ToolbarController.setInCreation(false);
    } else { // Otherwise
      // Set the active tool to the brush tool
      viewer.setActiveTool(PathTools.BRUSH);
      gui.getToolManager().setSelectedTool(PathTools.BRUSH);
      gui.getToolManager().getSelectionModeAction().setSelected(true);
      ToolbarController.setInCreation(true);
    }
  }

  /**
   * Set the active tool to the predefined polygon tool.
   */
  public static void createPolygonalAnnotation() {
    // Check that an image is open
    if (getCurrentHierarchy() == null) {
      AlertUtils.noImageOpen();
      return;
    }

    // Check that the GUI exists
    QuPathGUI gui = QuPathGUI.getInstance();
    if (gui == null) {
      AlertUtils.noGui();
      return;
    }

    // Check that the viewer exists
    QuPathViewer viewer = gui.getViewer();
    if (viewer == null) {
      AlertUtils.noViewer();
      return;
    }

    // If you are already set to the polygon ROI tool
    if (viewer.getActiveTool() == PathTools.POLYGON) {
      // Switch to the move tool
      viewer.setActiveTool(PathTools.MOVE);
      gui.getToolManager().setSelectedTool(PathTools.MOVE);
      ToolbarController.setInCreation(false);
    } else { // Otherwise
      // Set the active tool to the polygon ROI tool
      viewer.setActiveTool(PathTools.POLYGON);
      gui.getToolManager().setSelectedTool(PathTools.POLYGON);
      ToolbarController.setInCreation(true);
    }
  }

  /**
   * Set the active tool to the free-sized rectangle tool.
   */
  public static void createFreeSizeAnnotation() {
    // Check that an image is open
    if (getCurrentHierarchy() == null) {
      AlertUtils.noImageOpen();
      return;
    }

    // Check that the GUI exists
    QuPathGUI gui = QuPathGUI.getInstance();
    if (gui == null) {
      AlertUtils.noGui();
      return;
    }

    // Check that the viewer exists
    QuPathViewer viewer = gui.getViewer();
    if (viewer == null) {
      AlertUtils.noViewer();
      return;
    }

    // If you are already set to the rectangle ROI tool
    if (viewer.getActiveTool() == PathTools.RECTANGLE) {
      // Switch to the move tool
      viewer.setActiveTool(PathTools.MOVE);
      gui.getToolManager().setSelectedTool(PathTools.MOVE);
      ToolbarController.setInCreation(false);
    } else { // Otherwise
      // Set the active tool to the rectangle ROI tool
      viewer.setActiveTool(PathTools.RECTANGLE);
      gui.getToolManager().setSelectedTool(PathTools.RECTANGLE);
      ToolbarController.setInCreation(true);
    }
  }

  /**
   * Create a ROI (Region of Interest) with predefined areas depending
   * on what is chosen ( square, rectangle or circle ).
   */
  public static void createPredefinedSizedAnnotation() {
    // Check that an image is open
    if (getCurrentHierarchy() == null) {
      AlertUtils.noImageOpen();
      return;
    }

    // Check that the QuPath GUI exists
    QuPathGUI gui = QuPathGUI.getInstance();
    if (gui == null) {
      AlertUtils.noGui();
      return;
    }

    // Check that the QuPath viewer exists
    QuPathViewer viewer = gui.getViewer();
    if (viewer == null) {
      AlertUtils.noViewer();
      return;
    }

    // Check that image data exists within the currently-used viewer
    ImageData<BufferedImage> imageData = gui.getImageData();
    if (imageData == null) {
      AlertUtils.noImageData();
      return;
    }

    String selectedAreaString =
        ToolbarController.getInstance().getComboBox_Areas().getSelectionModel().getSelectedItem();
    double selectedArea = Double.parseDouble(selectedAreaString.split(" ")[0]);

    // Resize the annotation if it's bigger than the image
    double resizedArea = AnnotationUtils.checkAnnotationArea(selectedArea, imageData);

    switch (ToolbarController.getShapesList().get(ToolbarController.getSelectedShape())) {
      case "Square" -> createPredefinedSquareAnnotation(resizedArea, viewer, imageData);
      case "Rectangle" -> createPredifinedRectangleAnnotation(resizedArea, viewer, imageData);
      case "Circle" -> createPredefinedCircleAnnotation(resizedArea, viewer, imageData);
      default -> {
        // Do nothing
      }
    }

    // Switch to the move tool as active tool
    viewer.setActiveTool(PathTools.MOVE);

    // Switch to the move tool on the GUI
    gui.getToolManager().setSelectedTool(PathTools.MOVE);
  }

  /**
   * Create a square ROI (Region of Interest) with predefined areas.
   *
   * @param area The area of the square annotation. Must be a positive value.
   * @param viewer The viewer where the annotation will be created.
   * @param imageData The data containing image and pixel calibration data.
   */
  private static void createPredefinedSquareAnnotation(double area, QuPathViewer viewer,
                                                       ImageData<BufferedImage> imageData) {
    // Calculate the dimensions of the rectangle based on the area and aspect ratio
    double width = Math.sqrt(area);
    double height = Math.sqrt(area);

    // Obtain pixel calibration
    PixelCalibration cal = imageData.getServer().getPixelCalibration();
    int widthInPixels = (int) (width / cal.getPixelWidthMicrons() * 1000);
    int heightInPixels = (int) (height / cal.getPixelHeightMicrons() * 1000);

    // Coordinates of the center of the viewer
    int centerX = (int) viewer.getCenterPixelX();
    int centerY = (int) viewer.getCenterPixelY();

    // Calculate the starting coordinates of the rectangle
    int x = centerX - widthInPixels / 2;
    int y = centerY - heightInPixels / 2;

    // Move the annotation if it's out of the image
    int[] topLeftCorner =
        AnnotationUtils.checkAnnotationLocation(x, y, widthInPixels, heightInPixels, imageData);

    // Obtain the ImagePlane
    ImagePlane imagePlane = ImagePlane.getDefaultPlane();

    // Use the createRectangleROI method to create the ROI
    ROI rectangleRoi =
        ROIs.createRectangleROI(topLeftCorner[0], topLeftCorner[1], widthInPixels, heightInPixels,
            imagePlane);

    // Create an annotation from the ROI
    PathObject annotation = PathObjects.createAnnotationObject(rectangleRoi);

    // Set boolean for the other functions
    ToolbarController.setInCreation(true);
    ToolbarController.setPredifinedAnnotationInCreation(true);

    // Add the annotation to the image
    imageData.getHierarchy().addObject(annotation);

    // Update the display
    //imageData.getHierarchy().fireHierarchyChangedEvent(ToolbarController.getInstance());
    viewer.repaint();
    getCurrentHierarchy().getSelectionModel().setSelectedObject(annotation);
  }

  /**
   * Create a rectangle ROI (Region of Interest) with predefined areas.
   *
   * @param area The area of the rectangle annotation. Must be a positive value.
   * @param viewer The viewer where the annotation will be created.
   * @param imageData The data containing image and pixel calibration data.
   */
  private static void createPredifinedRectangleAnnotation(double area, QuPathViewer viewer,
                                                          ImageData<BufferedImage> imageData) {
    // Calculate the dimensions of the rectangle based on the area and aspect ratio
    double width = Math.sqrt(area * ToolbarController.getPredefinedRectangleAspectRatio());
    double height = area / width;

    // Obtain pixel calibration
    PixelCalibration cal = imageData.getServer().getPixelCalibration();
    int widthInPixels = (int) (width / cal.getPixelWidthMicrons() * 1000);
    int heightInPixels = (int) (height / cal.getPixelHeightMicrons() * 1000);

    // Get the center coordinates of the viewer
    int centerX = (int) viewer.getCenterPixelX();
    int centerY = (int) viewer.getCenterPixelY();

    // Calculate the starting coordinates of the rectangle
    int x = centerX - widthInPixels / 2;
    int y = centerY - heightInPixels / 2;

    // Move the annotation if it's out of the image
    int[] topLeftCorner =
        AnnotationUtils.checkAnnotationLocation(x, y, widthInPixels, heightInPixels, imageData);

    // Obtain the ImagePlane
    ImagePlane imagePlane = ImagePlane.getDefaultPlane();

    // Create the rectangular ROI
    ROI rectangleRoi =
        ROIs.createRectangleROI(topLeftCorner[0], topLeftCorner[1], widthInPixels, heightInPixels,
            imagePlane);

    // Create an annotation from the ROI
    PathObject annotation = PathObjects.createAnnotationObject(rectangleRoi);

    // Update control variables
    ToolbarController.setInCreation(true);
    ToolbarController.setPredifinedAnnotationInCreation(true);

    // Add the annotation to the image
    imageData.getHierarchy().addObject(annotation);

    // Update the display
    //imageData.getHierarchy().fireHierarchyChangedEvent(ToolbarController.getInstance());
    viewer.repaint();
    getCurrentHierarchy().getSelectionModel().setSelectedObject(annotation);
  }

  /**
   * Create a circle ROI (Region of Interest) with predefined areas.
   *
   *  @param area The area of the circle annotation. Must be a positive value.
   *  @param viewer The viewer where the annotation will be created.
   *  @param imageData The data containing image and pixel calibration data.
   */
  private static void createPredefinedCircleAnnotation(double area, QuPathViewer viewer,
                                                       ImageData<BufferedImage> imageData) {
    // Calculate the radius from the area
    double radius = Math.sqrt(area / Math.PI);

    // Obtain pixel calibration
    PixelCalibration cal = imageData.getServer().getPixelCalibration();
    int radiusInPixels = (int) (radius / cal.getPixelWidthMicrons() * 1000);

    // Get the center coordinates of the viewer
    int centerX = (int) viewer.getCenterPixelX();
    int centerY = (int) viewer.getCenterPixelY();

    // Calculate the top-left coordinates of the bounding box for the circle
    int x = centerX - radiusInPixels;
    int y = centerY - radiusInPixels;

    // Move the annotation if it's out of the image
    int[] topLeftCorner =
        AnnotationUtils.checkAnnotationLocation(x, y, 2 * radiusInPixels, 2 * radiusInPixels,
            imageData);

    // Obtain the ImagePlane
    ImagePlane imagePlane = ImagePlane.getDefaultPlane();

    // Create the circular ROI
    ROI circleRoi = ROIs.createEllipseROI(topLeftCorner[0], topLeftCorner[1], 2 * radiusInPixels,
        2 * radiusInPixels, imagePlane);

    // Create an annotation from the ROI
    PathObject annotation = PathObjects.createAnnotationObject(circleRoi);

    // Update control variables
    ToolbarController.setInCreation(true);
    ToolbarController.setPredifinedAnnotationInCreation(true);

    // Add the annotation to the image
    imageData.getHierarchy().addObject(annotation);

    // Update the display
    //imageData.getHierarchy().fireHierarchyChangedEvent(ToolbarController.getInstance());
    viewer.repaint();
    getCurrentHierarchy().getSelectionModel().setSelectedObject(annotation);
  }

  /**
   * Display a window that allows the user to set the object properties.
   *
   * @param pathObject The PathObject whose properties are to be set.
   */
  public static void createAnnotation(PathObject pathObject) {
    // Check if the annotation is currently being created
    if (ToolbarController.isInCreation()) {
      // Check that the path object exists
      if (pathObject == null) {
        pathObject =
            (PathAnnotationObject) getCurrentHierarchy().getAnnotationObjects().toArray()[0];
        QP.fireHierarchyUpdate();
      }

      AnnotationCreationController.getInstance().createStage();
      AnnotationCreationController.getInstance().setAnnotation(pathObject);
    }

    // Set inCreation status to false to avoid the window from being displayed
    // if we use the QuPath button to create another annotation
    ToolbarController.setInCreation(false);
    ToolbarController.setPredifinedAnnotationInCreation(false);
  }


  /**
   * Method that calculate the necrosis rate et display it.
   */
  public static void displayNecrosisRate() {
    PathObjectHierarchy hierarchy = QP.getCurrentHierarchy();

    if (ToolbarController.getInstance().getToolbarStage() == null
        || ToolbarController.getInstance().getLabel_Rate() == null
        || hierarchy == null) {
      // Display "---" if there is no tumor area and set text color to black
      ToolbarController.getInstance().getLabel_Rate().setText("---");
      ToolbarController.getInstance().getLabel_Rate().setTextFill(Color.BLACK);
      return;
    }

    // Initialize area variables for different types of regions
    double tumorArea = 0;
    double viableTumorArea = 0;
    double necroticTumorArea = 0;

    Collection<PathObject> annotations = hierarchy.getAnnotationObjects();

    // Iterate over each annotation object
    for (PathObject annotation : annotations) {
      if (annotation.getPathClass() != null) {
        String annotationName = annotation.getPathClass().getName();

        // Sum up areas based on the annotation class
        switch (annotationName) {
          case "Tumor Area" -> tumorArea += annotation.getROI().getArea();
          case "Tumor" -> viableTumorArea += annotation.getROI().getArea();
          case "Necrosis" -> necroticTumorArea += annotation.getROI().getArea();
          default -> {
            // Do nothing
          }
        }
      }
    }

    // Calculate necrosis rate if there is a tumor area
    if (tumorArea != 0) {
      double necrosisRate = (necroticTumorArea / tumorArea) * 100;
      double viableTumorRate = (viableTumorArea / tumorArea) * 100;

      // Clamp necrosis rate and viable tumor rate within the range [0.0, 100.0]
      if (necrosisRate > 99.9) {
        necrosisRate = 100.0;
      }
      if (necrosisRate < 0.1) {
        necrosisRate = 0.0;
      }
      if (viableTumorRate > 99.9) {
        viableTumorRate = 100.0;
      }
      if (viableTumorRate < 0.1) {
        viableTumorRate = 0.0;
      }

      // Format the necrosis rate to two decimal places and display it
      String formattedRate = String.format("%.2f%%", necrosisRate);
      ToolbarController.getInstance().getLabel_Rate().setText(formattedRate);

      // Set the text color based on the necrosis rate value
      if (necrosisRate <= 20) {
        ToolbarController.getInstance().getLabel_Rate().setTextFill(javafx.scene.paint.Color.RED);
      } else if (necrosisRate <= 90) {
        ToolbarController.getInstance().getLabel_Rate()
            .setTextFill(javafx.scene.paint.Color.ORANGE);
      } else {
        ToolbarController.getInstance().getLabel_Rate().setTextFill(javafx.scene.paint.Color.GREEN);
      }
    } else {
      // Display "---" if there is no tumor area and set text color to black
      ToolbarController.getInstance().getLabel_Rate().setText("---");
      ToolbarController.getInstance().getLabel_Rate().setTextFill(Color.BLACK);
    }
  }
}