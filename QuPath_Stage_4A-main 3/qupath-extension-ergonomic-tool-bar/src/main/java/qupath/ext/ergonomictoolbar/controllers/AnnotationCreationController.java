package qupath.ext.ergonomictoolbar.controllers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qupath.ext.ergonomictoolbar.ExtensionManagement;
import qupath.ext.ergonomictoolbar.utils.AlertUtils;
import qupath.ext.ergonomictoolbar.utils.AnnotationUtils;
import qupath.ext.ergonomictoolbar.utils.FileUtils;
import qupath.ext.ergonomictoolbar.utils.TilerUtils;
import qupath.ext.ergonomictoolbar.utils.WindowUtils;
import qupath.fx.dialogs.Dialogs;
import qupath.lib.objects.PathAnnotationObject;
import qupath.lib.objects.PathObject;
import qupath.lib.objects.classes.PathClass;
import qupath.lib.objects.hierarchy.PathObjectHierarchy;
import qupath.lib.objects.hierarchy.events.PathObjectSelectionModel;
import qupath.lib.scripting.QP;

/**
 * Controller for creating and modifying annotations in QuPath.
 * This class handles the UI interactions for creating annotations with specific properties.
 */
public class AnnotationCreationController extends AnchorPane {
  private static final Logger logger = LoggerFactory.getLogger(ExtensionManagement.class);

  private static AnnotationCreationController instance;
  private Stage annotationCreationStage;
  private PathObject annotation;


  /** FXML elements. **/
  @FXML private TextField textFieldName = new TextField();
  @FXML private ComboBox<String> comboBoxClasses = new ComboBox<>();
  @FXML private CheckBox checkBoxLockStatus = new CheckBox();
  @FXML private CheckBox checkboxTiledstatus = new CheckBox();
  @FXML private Label labelError = new Label();

  /**
   * Set the PathObject to be modified.
   *
   * @param pathObject The PathObject to be modified.
   */
  public void setAnnotation(PathObject pathObject) {
    if (pathObject instanceof PathAnnotationObject) {
      instance.annotation = pathObject;
    } else {
      instance.annotation = null;
    }
  }

  /**
   * Get the instance of the controller.
   *
   * @return The instance of the controller.
   */
  public static AnnotationCreationController getInstance() {
    if (instance == null) {
      new AnnotationCreationController();
    }
    return instance;
  }

  /**
   * Initialize the controller.
   * This method is called automatically after the FXML file has been loaded.
   */
  public AnnotationCreationController() {
    instance = this;

    // Add a listener to detect scene initialization on errorLabel
    instance.labelError.sceneProperty().addListener((observable, oldScene, newScene) -> {
      if (newScene != null) {
        newScene.windowProperty().addListener((obs, oldWindow, newWindow) -> {
          if (newWindow instanceof Stage stage) {
            stage.setOnCloseRequest(event -> handleWindowClose());
          }
        });
      }
    });

    // Allow validation by pressing the "Enter" key
    instance.comboBoxClasses.setOnKeyPressed(keyEvent -> {
      if (keyEvent.getCode() == KeyCode.ENTER) {
        createAnnotation();
      }
    });

    instance.textFieldName.setOnKeyPressed(keyEvent -> {
      if (keyEvent.getCode() == KeyCode.ENTER) {
        createAnnotation();
      }
    });
  }

  /**
   * Create the stage for the annotation creation.
   */
  public void createStage() {
    try {
      if (instance.annotationCreationStage == null) {
        String fxmlPath = "fxml/AnnotationCreation.fxml";

        // Load the FXML file
        var url = getClass().getResource(fxmlPath);
        FXMLLoader loader = new FXMLLoader(url);
        loader.setController(new AnnotationCreationController());

        // Get the controller from the FXMLLoader
        instance = loader.getController();

        // Set the main scene
        instance.annotationCreationStage = new Stage();
        instance.annotationCreationStage.initModality(Modality.APPLICATION_MODAL);

        // Create a scene with the loaded FXML file
        Scene scene = new Scene(loader.load());
        instance.annotationCreationStage.setScene(scene);
        instance.annotationCreationStage.setTitle("Annotation Properties");
        instance.annotationCreationStage.initStyle(StageStyle.UTILITY);
        instance.annotationCreationStage.setResizable(false);
      }

      // Fill the combo box with the QuPath classes
      fillComboBox();

      // Set the lock status check box to false if it is a predefined size annotation
      if (ToolbarController.isPredefinedAnnotationInCreation()) {
        instance.checkBoxLockStatus.setSelected(false);
      }

      // Bring the window to the foreground
      WindowUtils.setToForeground(instance.annotationCreationStage);

      // Display the main scene
      instance.annotationCreationStage.show();

      // Set the close request handler
      instance.annotationCreationStage.setOnCloseRequest(event -> handleWindowClose());
    } catch (IOException e) {
      // Handle the error if the FXML file fails to load
      Dialogs.showErrorMessage("Extension Error", "GUI loading failed");
      logger.error("Unable to load extension interface FXML", e);
    }
  }

  /**
   * Delete the stage.
   */
  private void deleteStage() {
    // Check if the stage exists
    if (instance.annotationCreationStage != null) {
      // Check if the stage is showing
      if (instance.annotationCreationStage.isShowing()) {
        // Hide the stage
        instance.annotationCreationStage.hide();
      }
      // Close the stage
      instance.annotationCreationStage.close();
      instance.annotationCreationStage = null;
    }
  }

  /**
   * Method called when the window is closed.
   */
  private void handleWindowClose() {
    PathObject pathObjectParent = instance.annotation.getParent();
    PathObjectHierarchy hierarchy = QP.getCurrentHierarchy();

    // Pour gérer le bug de hierarchy
    if (!AnnotationUtils.isPathObjectInHierarchy(instance.annotation) || pathObjectParent == null) {
      hierarchy.removeObject(hierarchy.getSelectionModel().getSelectedObject(), false);
    } else {
      hierarchy.removeObject(instance.annotation, false);

    }
    PathObjectSelectionModel selectionModel = hierarchy.getSelectionModel();
    selectionModel.clearSelection();

    // Reset every element of the window
    resetElements();
  }

  /**
   * Update the "classComboBox" with all the current classes from QuPath.
   */
  private void fillComboBox() {
    // Load all the classes from QuPath
    List<PathClass> pathClasses = QP.getProject().getPathClasses();
    List<String> classNames = new ArrayList<>();

    // Add the names of the classes to the list
    for (PathClass pc : pathClasses) {
      classNames.add(pc.getName());
    }

    // Set the names in the comboBox
    instance.comboBoxClasses.setItems(FXCollections.observableArrayList(classNames));
  }

  /**
   * Reset every element of the window to their default value.
   */
  private void resetElements() {
    // Reset the elements of the window
    instance.textFieldName.setText("");
    instance.comboBoxClasses.setValue("");
    instance.labelError.setText("");
    instance.checkBoxLockStatus.setSelected(true);
    instance.checkboxTiledstatus.setSelected(false);
  }

  /**
   * Create a new annotation with the specified properties.
   */
  @FXML private void createAnnotation() {
    if (instance.annotation == null) {
      instance.labelError.setText("No annotation are currently created.");
      return;
    }

    // Check if an image has been opened
    PathObjectHierarchy hierarchy = QP.getCurrentHierarchy();
    if (hierarchy == null) {
      instance.labelError.setText("No image open.");
      return;
    }

    // Check if an annotation has been selected
    PathObjectSelectionModel selectionModel = hierarchy.getSelectionModel();
    if (selectionModel == null) {
      instance.labelError.setText("No selection model.");
      return;
    }

    String newName = instance.textFieldName.getText();
    if (newName.isEmpty()) {
      instance.labelError.setText("Name cannot be empty.");
      return;
    }

    if (instance.comboBoxClasses.getValue() == null
        || Objects.equals(instance.comboBoxClasses.getValue(), "")) {
      instance.labelError.setText("No class selected");
      return;
    }

    // Modify the name of the selected annotation
    instance.annotation.setName(newName);

    // Modify the class of the selected annotation
    instance.annotation.setPathClass(PathClass.fromString(instance.comboBoxClasses.getValue()));

    // Modify the lock property of the selected annotation
    instance.annotation.setLocked(instance.checkBoxLockStatus.isSelected());

    // Create a tiled annotation with a "0.1mm" width/height if needed
    boolean tiled = instance.checkboxTiledstatus.isSelected();
    if (tiled) {
      // Définir le motif regex pour capturer la partie numérique
      String regex = "\\d+\\.\\d+|\\d+";
      Pattern pattern = Pattern.compile(regex);
      Matcher matcher = pattern.matcher(
          FileUtils.readStringsFromFile(FileUtils.FILE_PATH_TILE).get(0));

      if (matcher.find()) {
        double area = Double.parseDouble(matcher.group());
        TilerUtils.annotationTilesCreator(instance.annotation,
            hierarchy, 1000 * Math.sqrt(area),
            1000 * Math.sqrt(area));
        TilerUtils.addTiledAnnotation(instance.annotation);
      } else {
        AlertUtils.annotationTilesError();
        TilerUtils.removeTiledAnnotation(instance.annotation);
      }
    } else {
      TilerUtils.removeTiledAnnotation(annotation);
    }

    // Reset the elements of the window
    resetElements();

    QP.fireHierarchyUpdate();

    // Close the window once the annotation is updated
    deleteStage();
  }
}
