package qupath.ext.ergonomictoolbar.controllers;

import static java.lang.Math.round;
import static qupath.ext.ergonomictoolbar.Ia.createImageWithRequest;
import static qupath.lib.gui.scripting.QPEx.getQuPath;
import static qupath.lib.scripting.QP.getCurrentHierarchy;
import static qupath.lib.scripting.QP.getCurrentImageData;
import static qupath.lib.scripting.QP.getProject;
import static qupath.lib.scripting.QP.getSelectedObject;

import java.awt.Shape;
import java.io.InputStream;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import javax.imageio.ImageIO;

import javafx.scene.control.Button;
import javafx.scene.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import qupath.ext.ergonomictoolbar.ExtensionManagement;
import qupath.ext.ergonomictoolbar.Ia;
import qupath.ext.ergonomictoolbar.ToolbarFeatures;
import qupath.ext.ergonomictoolbar.utils.AlertUtils;
import qupath.ext.ergonomictoolbar.utils.FileUtils;
import qupath.ext.ergonomictoolbar.utils.TilerUtils;
import qupath.ext.ergonomictoolbar.utils.WindowUtils;
import qupath.fx.dialogs.Dialogs;
import qupath.lib.gui.QuPathGUI;
import qupath.lib.gui.commands.Commands;
import qupath.lib.gui.viewer.QuPathViewer;
import qupath.lib.gui.viewer.QuPathViewerListener;
import qupath.lib.images.ImageData;
import qupath.lib.images.servers.ImageServer;
import qupath.lib.objects.PathAnnotationObject;
import qupath.lib.objects.PathObject;
import qupath.lib.objects.hierarchy.PathObjectHierarchy;
import qupath.lib.objects.hierarchy.events.PathObjectHierarchyEvent;
import qupath.lib.objects.hierarchy.events.PathObjectHierarchyListener;
import qupath.lib.objects.hierarchy.events.PathObjectSelectionListener;

/**
 * Controller that manage the actions of the toolbar
 */
public class ToolbarController implements PathObjectSelectionListener, QuPathViewerListener, PathObjectHierarchyListener {
  private final Logger logger = LoggerFactory.getLogger(ExtensionManagement.class);

  private static ToolbarController instance;
  private Stage toolbar;

  @FXML
  private ComboBox<String> ComboBox_Areas = new ComboBox<>();
  @FXML
  private ImageView ImageView_LockStatus = new ImageView();
  @FXML
  private ImageView ImageView_Predefined_Shape = new ImageView();
  @FXML
  private Text Text_Necrose = new Text();
  @FXML
  private Label Label_Rate = new Label();
  @FXML
  private Label Label_Area = new Label();
  @FXML
  private Label Label_AreaMagnitude = new Label();
  @FXML
  private Label Label_Area_Percentage = new Label();
  @FXML
  private Label Label_Area_Percentage_Sign = new Label();
  @FXML
  private Button Button_IA = new Button();
  @FXML
  private Button Button_ExportTiles = new Button();
  @FXML
  private Button Button_AutomaticAnnotation = new Button();
  @FXML
  private Button Button_MergeAnnotation = new Button();
  @FXML
  private Button Button_ModelManagement = new Button();

  private static Image closedLockImage;
  private static Image openLockImage;
  private static Image squareImage;
  private static Image rectangleImage;
  private static Image circleImage;

  private static boolean verticalOrientation = true;
  private static boolean predefinedAnnotationInCreation = false;
  private static boolean inCreation = false;
  private static final ArrayList<String> shapesList = new ArrayList<>();
  private static int selectedShape = 0;
  private static final double predefinedRectangleAspectRatio = 4.0 / 3.0;
  private static int annotationsNumber;

  /**
   * Constructor for the ToolbarController
   */
  public ToolbarController() {
    instance = this;

    // Check if an instance of QuPath does not already exist
    if (getQuPath() != null) {
      // Add a listener on the viewer
      getQuPath().getViewer().addViewerListener(this);

      // Check if image data already exist
      if (getQuPath().getImageData() != null) {
        // Add a listener on the selection of path objects
        getQuPath().getImageData().getHierarchy().getSelectionModel()
            .addPathObjectSelectionListener(this);

        // Add a listener on the hierarchy of path objects
        getQuPath().getImageData().getHierarchy().addListener(this);
      }
    }

    // Check if there is at least an annotation
    if (getCurrentHierarchy() != null) {
      // Get the number of annotations on the current image
      annotationsNumber = getCurrentHierarchy().getAnnotationObjects().size();
    }

    // Load the images
    URL closedLockUrl =
        getClass().getResource("/qupath/ext/ergonomictoolbar/controllers/img/closed_lock.png");
    assert closedLockUrl != null;
    closedLockImage = new Image(closedLockUrl.toString());

    URL openLockUrl =
        getClass().getResource("/qupath/ext/ergonomictoolbar/controllers/img/open_lock.png");
    assert openLockUrl != null;
    openLockImage = new Image(openLockUrl.toString());

    URL squareUrl =
        getClass().getResource("/qupath/ext/ergonomictoolbar/controllers/img/square.png");
    assert squareUrl != null;
    squareImage = new Image(squareUrl.toString());

    URL rectangleUrl =
        getClass().getResource("/qupath/ext/ergonomictoolbar/controllers/img/rectangle.png");
    assert rectangleUrl != null;
    rectangleImage = new Image(rectangleUrl.toString());

    URL circleUrl =
        getClass().getResource("/qupath/ext/ergonomictoolbar/controllers/img/circle.png");
    assert circleUrl != null;
    circleImage = new Image(circleUrl.toString());

    instance.ImageView_LockStatus.setImage(closedLockImage);
    instance.ImageView_Predefined_Shape.setImage(squareImage);

    // Fill the shapes list
    shapesList.add("Square");
    shapesList.add("Rectangle");
    shapesList.add("Circle");

    if (FileUtils.readStringsFromFile(FileUtils.FILE_PATH_TILE).isEmpty()) {
      FileUtils.addStringToFile("0.5", FileUtils.FILE_PATH_TILE);
    }

    if (FileUtils.readStringsFromFile(FileUtils.FILE_PATH_AREAS).isEmpty()) {
      FileUtils.addStringToFile("1", FileUtils.FILE_PATH_AREAS);
    }

    updatePresetAreasCombobox();
    updateLockUnlockButtonImage(getSelectedObject());

    ToolbarFeatures.displayNecrosisRate();

    try {
      FileUtils.deleteDirectory(FileUtils.FILE_PATH_SAVE, "extracted_images");
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    getQuPath().getStage().setOnCloseRequest(event -> {
      try {
        FileUtils.deleteDirectory(FileUtils.FILE_PATH_SAVE, "extracted_images");
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    });
  }

  /**
   * Getter of inCreation
   *
   * @return inCreation
   */
  public static boolean isInCreation() {
    return inCreation;
  }

  /**
   * Setter of inCreation
   *
   * @param b the new boolean value
   */
  public static void setInCreation(boolean b) {
    inCreation = b;
  }

  /**
   * Getter of predefinedAnnotationInCreation
   *
   * @return predefinedAnnotationInCreation
   */
  public static boolean isPredefinedAnnotationInCreation() {
    return predefinedAnnotationInCreation;
  }

  /**
   * Setter of predefinedAnnotationInCreation
   *
   * @param b the new boolean value
   */
  public static void setPredifinedAnnotationInCreation(boolean b) {
    predefinedAnnotationInCreation = b;
  }

  /**
   * Getter of shapesList
   *
   * @return shapesList
   */
  public static List<String> getShapesList() {
    return shapesList;
  }

  /**
   * Getter of selectedShape
   *
   * @return selectedShape
   */
  public static int getSelectedShape() {
    return selectedShape;
  }

  /**
   * Getter of predefinedRectangleAspectRatio
   *
   * @return predefinedRectangleAspectRatio
   */
  public static double getPredefinedRectangleAspectRatio() {
    return predefinedRectangleAspectRatio;
  }

  /**
   * Get the instance of the ToolbarController
   *
   * @return the instance of the ToolbarController
   */
  public static ToolbarController getInstance() {
    if (instance == null) {
      new ToolbarController();
    }
    return instance;
  }

  /**
   * Getter of ComboBox_Areas
   *
   * @return ComboBox_Areas
   */
  public ComboBox<String> getComboBox_Areas() {
    return ComboBox_Areas;
  }

  /**
   * Getter of Label_Rate
   *
   * @return Label_Rate
   */
  public Label getLabel_Rate() {
    return Label_Rate;
  }

  /**
   * Get the stage of the toolbar
   *
   * @return the stage of the toolbar
   */
  public Stage getToolbarStage() {
    return instance.toolbar;
  }

  /**
   * Create a new stage for the toolbar
   */
  private void createStage() {
    try {
      String fxmlPath = "fxml/" +
          (verticalOrientation ? "VerticalInterface.fxml" : "HorizontalInterface.fxml");

      // Load the FXML file
      var url = getClass().getResource(fxmlPath);
      FXMLLoader loader = new FXMLLoader(url);
      loader.setController(getInstance());

      // Create a scene with the loaded FXML file
      Scene scene = new Scene(loader.load());

      // Create a new stage
      instance.toolbar = new Stage();

      // Set the stage
      instance.toolbar.setScene(scene);
      instance.toolbar.setResizable(false);
      instance.toolbar.initStyle(StageStyle.UTILITY);

      WindowUtils.setToForeground(instance.toolbar);

      // The QuPath window is the owner of the toolbar window
      instance.toolbar.initOwner(getQuPath().getStage());

      // Display the stage
      instance.toolbar.show();

      // Add an event handler for the close request
      instance.toolbar.setOnCloseRequest(event -> {
        toggleStageVisibility();
        event.consume(); // Prevent the default close behavior
      });

      ToolbarFeatures.addHierarchyListener();
      ToolbarFeatures.addViewerListener();
      ToolbarFeatures.addActionOnCtrlQandW();
      ToolbarFeatures.addActionOnCtrlZ();
    } catch (IOException e) {
      Dialogs.showErrorMessage("Extension Error", "GUI loading failed");
      logger.error("Unable to load extension interface FXML", e);
    }

    updatePresetAreasCombobox();
    updateLockUnlockButtonImage(getSelectedObject());
  }

  /**
   * Delete the stage
   */
  private void deleteStage() {
    // Check if the stage exists
    if (instance.toolbar != null) {
      // Check if the stage is showing
      if (instance.toolbar.isShowing()) {
        // Hide the stage
        instance.toolbar.hide();
      }

      // Close the stage
      instance.toolbar.close();
      instance.toolbar = null;
    }
  }

  /**
   * Switch between showing and hiding toolbar
   */
  public void toggleStageVisibility() {
    if (instance.toolbar == null) {
      ToolbarController.getInstance().createStage();
      ExtensionManagement.getToolbarVisibilityMenuItem().setText("Hide Toolbar");
    } else {
      ToolbarController.getInstance().deleteStage();
      ExtensionManagement.getToolbarVisibilityMenuItem().setText("Show Toolbar");
    }
  }

  // Detourage
  @FXML
  public void createAutoAnnotation(){
    AutomaticDetection.createAutoAnnotation();
  }

  // Polygonal
  @FXML
  private void createPolygonalAnnotation() {
    ToolbarFeatures.createPolygonalAnnotation();
  }

  // Free size
  @FXML
  private void createRectangularAnnotation() {
    ToolbarFeatures.createFreeSizeAnnotation();
  }

  // Predefined
  @FXML
  private void createPredefinedSizedAnnotation() {
    ToolbarFeatures.createPredefinedSizedAnnotation();
  }

  @FXML private void createSelectionBrushAnnotation() { ToolbarFeatures.createSelectionBrushAnnotation();}

  /**
   * Method that allows to change the current form for predefined annotation with the right button
   */
  @FXML
  private void rightArrowClick() {
    if (selectedShape < 0 || selectedShape >= shapesList.size()) {
      System.out.println("Index out of range in selectedShape");
    } else {
      if (selectedShape < shapesList.size() - 1) {
        selectedShape++;
      } else {
        selectedShape = 0;
      }
    }

    switch (shapesList.get(selectedShape)) {
      case "Square" -> ImageView_Predefined_Shape.setImage(squareImage);
      case "Rectangle" -> ImageView_Predefined_Shape.setImage(rectangleImage);
      case "Circle" -> ImageView_Predefined_Shape.setImage(circleImage);
    }
  }

  /**
   * Method that allows to change the current form for predefined annotation with the left button
   */
  @FXML
  private void leftArrowClick() {
    if (selectedShape < 0 || selectedShape >= shapesList.size()) {
      System.out.println("Index out of range in selectedShape");
    } else {
      if (selectedShape > 0) {
        selectedShape--;
      } else {
        selectedShape = shapesList.size() - 1;
      }
    }

    switch (shapesList.get(selectedShape)) {
      case "Square" -> ImageView_Predefined_Shape.setImage(squareImage);
      case "Rectangle" -> ImageView_Predefined_Shape.setImage(rectangleImage);
      case "Circle" -> ImageView_Predefined_Shape.setImage(circleImage);
    }
  }

  /**
   * Display a window that allows the user to change the current name or description of the selected annotation.
   */
  @FXML
  private void changeAnnotationNameAndDescription() {
    // Check that an image is open
    PathObjectHierarchy hierarchy = getCurrentHierarchy();
    if (hierarchy == null) {
      AlertUtils.noImageOpen();
      return;
    }

    // Check that an annotation is selected
    PathObject pathObject = hierarchy.getSelectionModel().getSelectedObject();
    PathAnnotationObject selectedAnnotation = (PathAnnotationObject) pathObject;
    if (selectedAnnotation == null) {
      AlertUtils.noAnnotationSelected();
      return;
    }

    AnnotationRenamingController.getInstance().createStage();
  }

  // Change class

  /**
   * Display a window that allows the user to change the current class of the selected annotation.
   */
  @FXML
  private void changeAnnotationClass() {
    // Check that an image is open
    PathObjectHierarchy hierarchy = getCurrentHierarchy();
    if (hierarchy == null) {
      AlertUtils.noImageOpen();
      return;
    }

    if (hierarchy.getSelectionModel() == null) return;

    // Check that an annotation is selected
    PathObject selectedObject = hierarchy.getSelectionModel().getSelectedObject();
    PathAnnotationObject selectedAnnotation = (PathAnnotationObject) selectedObject;
    if (selectedAnnotation == null) {
      AlertUtils.noAnnotationSelected();
      return;
    }

    AnnotationClassModificationController.getInstance().createStage();
  }

  // Filling

  /**
   * Toggle the fill annotations option in the overlay settings.
   */
  @FXML
  private void toggleAnnotationsFilling() {
    QuPathViewer viewer = QuPathGUI.getInstance().getViewer();

    // Check that an image is open
    if (getCurrentHierarchy() == null) {
      AlertUtils.noImageOpen();
      return;
    }

    // Check that the viewer exists
    if (viewer == null) {
      AlertUtils.noViewer();
      return;
    }

    // Toggle the fill annotations option in the overlay settings
    boolean areAnnotationsFilled =
        !QuPathGUI.getInstance().getOverlayOptions().getFillAnnotations();
    viewer.getOverlayOptions().setFillAnnotations(areAnnotationsFilled);
  }

  // Name display

  /**
   * Toggle the display of annotation names in the overlay settings.
   */
  @FXML
  private void toggleAnnotationNameDisplay() {
    QuPathViewer viewer = QuPathGUI.getInstance().getViewer();

    // Check that an image is open
    if (getCurrentHierarchy() == null) {
      AlertUtils.noImageOpen();
      return;
    }

    // Check that the viewer exists
    if (viewer == null) {
      AlertUtils.noViewer();
      return;
    }

    // Toggle the display of annotation names in the overlay settings
    boolean areNamesDisplayed = !QuPathGUI.getInstance().getOverlayOptions().getShowNames();
    viewer.getOverlayOptions().setShowNames(areNamesDisplayed);
  }

  // Lock status

  /**
   * Toggle the lock status of an annotation between locked and unlocked.
   * If an annotation is selected, its lock status is toggled and the interface is updated.
   * If no annotation is selected, an alert is displayed.
   */
  @FXML
  private void toggleAnnotationLock() {
    // Check that an annotation is selected
    PathAnnotationObject selectedAnnotation = (PathAnnotationObject) getSelectedObject();
    if (selectedAnnotation == null) {
      AlertUtils.noAnnotationSelected();
      instance.ImageView_LockStatus.setImage(closedLockImage);
      return;
    }

    // Toggle the lock status
    selectedAnnotation.setLocked(!selectedAnnotation.isLocked());

    // Change the button image accordingly
    updateLockUnlockButtonImage(selectedAnnotation);

    // Force the annotation update to reflect the change in the user interface
    if (getCurrentHierarchy() != null) {
      getCurrentHierarchy().fireHierarchyChangedEvent(selectedAnnotation);
    }
  }

  /**
   * Show/hide the panel for the Ia's management
   */
  @FXML
  private void showManagementIA() {
    if (!Button_ExportTiles.isVisible()) {
      Button_IA.setText("Hide IA");

      Button_ExportTiles.setVisible(true);
      Button_ExportTiles.setManaged(true);

      Button_AutomaticAnnotation.setVisible(true);
      Button_AutomaticAnnotation.setManaged(true);

      Button_MergeAnnotation.setVisible(true);
      Button_MergeAnnotation.setManaged(true);

      Button_ModelManagement.setVisible(true);
      Button_ModelManagement.setManaged(true);

      Text_Necrose.setVisible(true);
      Text_Necrose.setManaged(true);

      Label_Rate.setVisible(true);
      Label_Rate.setManaged(true);

      instance.toolbar.sizeToScene();
    }
    else {
      Button_IA.setText("Show IA");

      Button_ExportTiles.setVisible(false);
      Button_ExportTiles.setManaged(false);

      Button_AutomaticAnnotation.setVisible(false);
      Button_AutomaticAnnotation.setManaged(false);

      Button_MergeAnnotation.setVisible(false);
      Button_MergeAnnotation.setManaged(false);

      Button_ModelManagement.setVisible(false);
      Button_ModelManagement.setManaged(false);

      Text_Necrose.setVisible(false);
      Text_Necrose.setManaged(false);

      Label_Rate.setVisible(false);
      Label_Rate.setManaged(false);

      instance.toolbar.sizeToScene();
    }


  }

  // Annotation(s) area

  // Automatic annotation
  @FXML
  private void automaticAnnotation() {
    Ia.automaticAnnotation();
  }

  // Merge annotation
  @FXML
  private void mergeTumorAndNecrosis() {
    Ia.mergeTumorAndNecrosis();
  }

  // Model Management

  /**
   * Display the stage to manage the model for the IA
   */
  @FXML
  private void modelManagement() {
    ModelManagementController.getInstance().createStage();
  }

  // Rate

  // Turn

  /**
   * Toggle the orientation of the toolbar.
   * Calls the method to toggle the stage orientation.
   */
  @FXML
  private void toggleToolbarOrientation() {
    verticalOrientation = !verticalOrientation;

    // Check if the stage exists
    if (instance.toolbar != null) {
      deleteStage();
    }
    ToolbarController.getInstance().createStage();
  }

  // Save

  /**
   * Save the current project.
   * If there is an image data loaded, prompts the user to save the image data.
   * Displays an alert indicating whether the backup was successful or failed.
   */
  @FXML
  private void saveProject() {
    // Check that a QuPath instance exists
    if (getQuPath() == null) {
      AlertUtils.noQuPath();
      return;
    }

    // Check that a project is open
    if (getProject() == null) {
      AlertUtils.noOpenProject();
      return;
    }

    // Check that there is at least an image open within the currently-active viewer
    if (getQuPath().getImageData() == null) {
      AlertUtils.noImageData();
      return;
    }

    // Check if the project backup was successful
    if (Commands.promptToSaveImageData(getQuPath(), getQuPath().getImageData(), true)) {
      AlertUtils.projectBackupCompleted();
    } else {
      AlertUtils.projectBackupFailed();
    }
  }

  /**
   * Update the comboBox with the areas in the text file
   */
  public void updatePresetAreasCombobox() {
    // Ensure instance is not null
    if (instance.toolbar == null) {
      return;
    }

    // Add values to the predefined area list
    ObservableList<String> areaList = FXCollections.observableArrayList();
    List<String> areas = FileUtils.readStringsFromFile(FileUtils.FILE_PATH_AREAS);

    areas.sort(Comparator.comparingDouble(Double::parseDouble));

    for (String area : areas) {
      areaList.add(area + " mm²");
    }

    instance.getComboBox_Areas().setItems(areaList);
    instance.getComboBox_Areas().setStyle("-fx-font-size: 8px;");
    instance.getComboBox_Areas().getSelectionModel().selectFirst();
  }

  /**
   * Update the lock image on the lock/unlock function button.
   *
   * @param pathObjectSelected The image selected in the application
   */
  public void updateLockUnlockButtonImage(PathObject pathObjectSelected) {
    if (instance.toolbar == null || instance.ImageView_LockStatus == null || inCreation) {
      return;
    }

    if (pathObjectSelected instanceof PathAnnotationObject annotation) {
      if (annotation.isLocked()) {
        instance.ImageView_LockStatus.setImage(closedLockImage);
      } else {
        instance.ImageView_LockStatus.setImage(openLockImage);
      }
    } else {
      instance.ImageView_LockStatus.setImage(
          closedLockImage); // Display closed lock by default
    }
  }

  @Override
  public void imageDataChanged(QuPathViewer viewer, ImageData<BufferedImage> imageDataOld,
                               ImageData<BufferedImage> imageDataNew) {
    // Check if a QuPath instance exists
    if (getQuPath() == null) return;

    // Check if image data exist
    if (getQuPath().getImageData() == null) return;

    // Remove former listener and add a new one
    getQuPath().getImageData().getHierarchy().getSelectionModel()
        .removePathObjectSelectionListener(this);
    getQuPath().getImageData().getHierarchy().getSelectionModel()
        .addPathObjectSelectionListener(this);

    // Remove former listener and add a new one
    getQuPath().getImageData().getHierarchy().removeListener(this);
    getQuPath().getImageData().getHierarchy().addListener(this);
  }

  @Override
  public void visibleRegionChanged(QuPathViewer viewer, Shape shape) {

  }

  @Override
  public void selectedObjectChanged(QuPathViewer viewer, PathObject pathObjectSelected) {

  }

  @Override
  public void viewerClosed(QuPathViewer viewer) {

  }

  @Override
  public void hierarchyChanged(PathObjectHierarchyEvent event) {
    PathObjectHierarchy hierarchy = getCurrentHierarchy();
    PathObject selectedAnnotation = getSelectedObject();

    if (hierarchy != null) {
      switch (event.getEventType()) {
        case ADDED -> {
          if (annotationsNumber < hierarchy.getAnnotationObjects().size()) {
            ToolbarFeatures.createAnnotation(
                event.getChangedObjects().get(event.getChangedObjects().size() - 1));
          }

          if (selectedAnnotation != null) {
            if (TilerUtils.getTiledAnnotations().contains(selectedAnnotation.getID())) {
              double area = Double.parseDouble(
                  FileUtils.readStringsFromFile(FileUtils.FILE_PATH_TILE).get(0));
              TilerUtils.annotationTilesCreator(selectedAnnotation, hierarchy,
                  1000 * Math.sqrt(area), 1000 * Math.sqrt(area));
            }
          }
        }

        case CHANGE_OTHER -> {
          if (selectedAnnotation != null && selectedAnnotation.nChildObjects() > 0) {
            // Get the tiles
            Collection<PathObject> childObjects = selectedAnnotation.getChildObjects();

            // Delete the tiles
            selectedAnnotation.removeChildObjects(childObjects);
          }
        }
      }
    }
    updateLockUnlockButtonImage(selectedAnnotation);
  }

  @Override
  public void selectedPathObjectChanged(PathObject pathObjectSelected, PathObject previousObject,
                                        Collection<PathObject> allSelected) {
    updateLockUnlockButtonImage(getSelectedObject());

    if (pathObjectSelected == null) {
      instance.Label_Area.setText("...");
      instance.Label_AreaMagnitude.setText("");
      instance.Label_Area_Percentage.setText("...");
      instance.Label_Area_Percentage_Sign.setText("");
      return;
    }

    double area;
    double imageArea = 0;
    double areaPercentage;
    double annotationsArea = 0;

    Set<PathObject> selectedObjects =
        getCurrentHierarchy().getSelectionModel().getSelectedObjects();
    ImageData<BufferedImage> imageData = getCurrentImageData();

    for (PathObject po : selectedObjects) {
      annotationsArea += po.getROI().getArea() *
          imageData.getServer().getPixelCalibration().getPixelWidthMicrons() *
          imageData.getServer().getPixelCalibration().getPixelHeightMicrons();
      imageArea = imageData.getServer().getHeight() * imageData.getServer().getPixelCalibration().getPixelHeightMicrons() * imageData.getServer().getWidth() * imageData.getServer().getPixelCalibration().getPixelWidthMicrons();
    }

    areaPercentage = round((annotationsArea/imageArea) * 100);
    instance.Label_Area_Percentage.setText(String.valueOf(areaPercentage));



    if (annotationsArea < 10000) {
      area = (double) round(annotationsArea * 100) / 100;
      instance.Label_Area.setText(String.valueOf(area));
      instance.Label_AreaMagnitude.setText(" μm²");
    } else {
      double roundArea = (double) round(annotationsArea / 1000) * 1000;
      area = roundArea / 1000000;
      instance.Label_Area.setText(String.valueOf(area));
      instance.Label_AreaMagnitude.setText(" mm²");
    }

    instance.Label_Area_Percentage_Sign.setText("%");
  }

  /**
   * Export annotated tiles in the file "datasets"
   * and open the viewer to manage the tiles.
   *
   * @param actionEvent Action associated with the ExportTile button
   */
  @FXML
  public void exportTile(ActionEvent actionEvent) {
    File datasetsDirectory = new File('.' + "/datasets/");

    // Create the directory Viewer/datasets/ if it doesn't exists
    if (!datasetsDirectory.exists()) {
      FileUtils.createDirectory(Path.of('.' + "/datasets/"));
    }


    // Get the current image data
    ImageData<BufferedImage> imageData = getCurrentImageData();
    if (imageData == null) {
      AlertUtils.noImageData();
      return;
    }
    ImageServer<BufferedImage> imageServer = imageData.getServer();

    // Check if the current hierarchy is null
    if (getCurrentHierarchy() == null) {
      AlertUtils.noImageData();
      return;
    }

    // Get the selected annotations
    List<PathObject> selectedTiles =
        new ArrayList<>(getCurrentHierarchy().getSelectionModel().getSelectedObjects());
    if (selectedTiles.isEmpty()) {
      AlertUtils.noAnnotationSelected();
      return;
    }
    selectedTiles.sort(Comparator.comparingDouble((PathObject o) -> o.getROI().getBoundsY())
        .thenComparingDouble(o -> o.getROI().getBoundsX()));
    int lineLength = 0;
    double lastY = selectedTiles.get(0).getROI().getBoundsY();
    for (PathObject tile : selectedTiles) {
      if (tile.getROI().getBoundsY() != lastY) {
        lineLength = 0;
        lastY = tile.getROI().getBoundsY();
      }
      lineLength++;
    }
    int x = 0;
    int y = 0;
    int tileCounter = 0;
    // Iterate over all selected annotation objects
    for (PathObject tile : selectedTiles) {
      // Get the tiles
      BufferedImage image;
      try {
        image = createImageWithRequest(imageServer, tile.getROI());
      } catch (IOException e) {
        e.printStackTrace();
        return;
      }
      String name;
      String fn = new File(imageServer.getPath()).getName();
      String classe = tile.getDisplayedName();
      name =
          String.format("tile-%s-%s-%d-x%d-y%d-%s", fn, LocalDate.now(), tileCounter, x, y, classe);
      x++;
      if (x == lineLength) {
        x = 0;
        y++;
      }
      tileCounter++;
      try {
        ImageIO.write(image, "tiff", new File("./datasets/" + name + ".tiff"));
      } catch (IOException e) {
        System.err.println("Erreur lors de l'exportation de l'image : " + e.getMessage());
        e.printStackTrace();
      }
    }

    try {
      URL resourceUrl = ToolbarController.class.getResource("python/viewer.exe");
      if (resourceUrl == null) {
          throw new IOException("Resource not found: /python/viewer.py");
      }

      Path tempScriptPath;
      if (new File("viewer.exe").exists()) {
        // Remove the file if it exists
        if (!new File("viewer.exe").delete()) {
          throw new IOException("Failed to delete existing temp file");
        }
      }
      if (! new File("viewer.exe").exists()) {
        try (InputStream in = resourceUrl.openStream()) {
          tempScriptPath = Files.createFile(Path.of("./viewer.exe"));
          Files.copy(in, tempScriptPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new IOException("Failed to copy resource to temp file", e);
        }
        // Check if the file exists
        if (!tempScriptPath.toFile().exists()) {
            throw new IOException("Failed to create temp file");
        }

        // Check if the file is executable
        if (!tempScriptPath.toFile().setExecutable(true)) {
            throw new IOException("Failed to set executable permission on temp file");
        }
      } else {
        tempScriptPath = Path.of("viewer.exe");
      }

      String NASPath = new File("./datasets/").getAbsolutePath();
      NASPath = NASPath.substring(0, NASPath.length() - "./datasets/".length());
      String ModelPath = FileUtils.readStringsFromFile(FileUtils.FILE_PATH_MODEL).getFirst();
      String[] command = {
          "cmd.exe", "/c", tempScriptPath.toString(), NASPath, ModelPath
      };
      System.out.println("Command: " + String.join(" ", command));

      ProcessBuilder pb = new ProcessBuilder(command);
      pb.redirectErrorStream(true);

      Process process = pb.start();
      process.waitFor();
      System.out.println("Viewer.exe started");
    } catch (IOException | InterruptedException e) {
        e.printStackTrace();
        throw new RuntimeException("Error executing viewer.exe", e);
    }
  }
}
