package qupath.ext.ergonomictoolbar.controllers;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qupath.ext.ergonomictoolbar.ExtensionManagement;
import qupath.ext.ergonomictoolbar.ToolbarFeatures;
import qupath.ext.ergonomictoolbar.utils.WindowUtils;
import qupath.fx.dialogs.Dialogs;
import qupath.lib.objects.PathAnnotationObject;
import qupath.lib.objects.classes.PathClass;
import qupath.lib.scripting.QP;

/**
 * Controller for modifying the class of an annotation in QuPath.
 * This class handles the UI interactions for updating annotation classes.
 */
public class AnnotationClassModificationController implements Initializable {
  private static final Logger logger = LoggerFactory.getLogger(ExtensionManagement.class);

  private static AnnotationClassModificationController instance;
  private Stage annotationClassModificationStage;

  @FXML private ComboBox<String> comboBoxClasses = new ComboBox<>();
  @FXML private Label labelError = new Label();

  /**
   * Get the instance of the AnnotationClassModificationController.
   *
   * @return The instance of the AnnotationClassModificationController.
   */
  public static AnnotationClassModificationController getInstance() {
    if (instance == null) {
      new AnnotationClassModificationController();
    }
    return instance;
  }

  /**
   * Constructor for the AnnotationClassModificationController.
   */
  public AnnotationClassModificationController() {
    instance = this;

    // Add a listener to detect scene initialization on errorLabel
    instance.labelError.sceneProperty().addListener((observable, oldScene, newScene) -> {
      if (newScene != null) {
        newScene.windowProperty().addListener((obs, oldWindow, newWindow) -> {
          if (newWindow instanceof Stage stage) {
            stage.setOnCloseRequest(event -> resetElements());
          }
        });
      }
    });

    // Allow validation by pressing the "Enter" key
    instance.comboBoxClasses.setOnKeyPressed(keyEvent -> {
      if (keyEvent.getCode() == KeyCode.ENTER) {
        confirmNewClass();
      }
    });
  }

  /**
   * Create the stage for the annotation class modification.
   */
  public void createStage() {
    try {
      // Check if the stage for annotation class modification is already created
      if (instance.annotationClassModificationStage == null) {
        String fxmlPath = "fxml/AnnotationClassModification.fxml";

        // Load the FXML file
        var url = getClass().getResource(fxmlPath);
        FXMLLoader loader = new FXMLLoader(url);
        loader.setController(new AnnotationClassModificationController());

        // Get the controller from the FXMLLoader
        instance = loader.getController();

        // Set the main scene
        instance.annotationClassModificationStage = new Stage();
        instance.annotationClassModificationStage.initModality(Modality.APPLICATION_MODAL);

        // Create a scene with the loaded FXML file
        Scene scene = new Scene(loader.load());
        instance.annotationClassModificationStage.setScene(scene);
        instance.annotationClassModificationStage.setTitle("Change the annotation class");
        instance.annotationClassModificationStage.initStyle(StageStyle.UTILITY);
        instance.annotationClassModificationStage.setResizable(false);
      }

      // Fill the combo box with the QuPath classes
      fillComboBox();

      // Display the current class of the annotation in the annotation class modification window
      PathAnnotationObject selectedAnnotation = (PathAnnotationObject) QP.getSelectedObject();
      if (selectedAnnotation != null && selectedAnnotation.getPathClass() != null) {
        instance.comboBoxClasses.setValue(selectedAnnotation.getPathClass().getName());
      }

      // Bring the window to the foreground
      WindowUtils.setToForeground(instance.annotationClassModificationStage);

      // Display the main scene
      instance.annotationClassModificationStage.show();
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
    if (instance.annotationClassModificationStage != null) {
      // Check if the stage is showing
      if (instance.annotationClassModificationStage.isShowing()) {
        // Hide the stage
        instance.annotationClassModificationStage.hide();
      }
      // Close the stage
      instance.annotationClassModificationStage.close();
      instance.annotationClassModificationStage = null;
    }
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
    instance.labelError.setText("");
  }

  /**
   * Method for modifying the class of an annotation.
   */
  @FXML
  private void confirmNewClass() {
    PathAnnotationObject selectedAnnotation = (PathAnnotationObject) QP.getSelectedObject();
    // Check that an annotation has been selected
    if (selectedAnnotation == null) {
      instance.labelError.setText("No annotation selected.");
      return;
    }

    // Check if a class has been selected
    if (instance.comboBoxClasses.getValue() == null) {
      instance.labelError.setText("No new class selected");
      return;
    }

    selectedAnnotation.setPathClass(PathClass.fromString(instance.comboBoxClasses.getValue()));

    // Refresh annotation properties in QuPath
    QP.refreshIDs();

    // Reset the elements of the window
    resetElements();

    ToolbarFeatures.displayNecrosisRate();

    // Close the window once the annotation is updated
    deleteStage();
  }

  /**
   * Initialize the controller.
   *
   * @param location The location used to resolve relative paths for the root object,
   *                 or {@code null} if the location is not known.
   *
   * @param resources The resources used to localize the root object,
   *                  or {@code null} if the root object was not localized.
   */
  @Override
  public void initialize(URL location, ResourceBundle resources) {

  }
}