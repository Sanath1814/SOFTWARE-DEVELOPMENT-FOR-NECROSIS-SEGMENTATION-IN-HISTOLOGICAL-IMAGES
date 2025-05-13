package qupath.ext.ergonomictoolbar.controllers;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
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
import qupath.ext.ergonomictoolbar.utils.WindowUtils;
import qupath.fx.dialogs.Dialogs;
import qupath.lib.objects.PathAnnotationObject;
import qupath.lib.scripting.QP;

/**
 * Controller for renaming annotations in QuPath.
 * This class handles the UI interactions for renaming annotations.
 */
public class AnnotationRenamingController extends AnchorPane implements Initializable {
  private static final Logger logger = LoggerFactory.getLogger(ExtensionManagement.class);

  private static AnnotationRenamingController instance;
  private Stage annotationRenamingStage;

  @FXML private TextField textFieldName = new TextField();
  @FXML private TextField textFieldDescription = new TextField();
  @FXML private Label labelError = new Label();

  /**
   * Get the instance of the controller.
   *
   * @return The instance of the controller.
   */
  public static AnnotationRenamingController getInstance() {
    if (instance == null) {
      new AnnotationRenamingController();
    }
    return instance;
  }

  /**
   * Constructor for the controller.
   */
  public AnnotationRenamingController() {
    instance = this;

    // Allow validation by pressing the "Enter" key
    instance.textFieldName.setOnKeyPressed(keyEvent -> {
      if (keyEvent.getCode() == KeyCode.ENTER) {
        validateNewName();
      }
    });
    instance.textFieldDescription.setOnKeyPressed(keyEvent -> {
      if (keyEvent.getCode() == KeyCode.ENTER) {
        validateNewName();
      }
    });
  }

  /**
   * Create the stage for the annotation renaming window.
   */
  public void createStage() {
    try {
      // Check if the stage for annotation name and description change is already created
      if (instance.annotationRenamingStage == null) {
        String fxmlPath = "fxml/AnnotationRenaming.fxml";

        // Load the FXML file
        var url = getClass().getResource(fxmlPath);
        FXMLLoader loader = new FXMLLoader(url);
        loader.setController(getInstance());

        // Create a scene with the loaded FXML file
        Scene scene = new Scene(loader.load());

        // Get the controller from the FXMLLoader
        //instance = loader.getController();

        // Set the main scene
        instance.annotationRenamingStage = new Stage();
        instance.annotationRenamingStage.initModality(Modality.APPLICATION_MODAL);
        instance.annotationRenamingStage.setScene(scene);
        instance.annotationRenamingStage.setTitle("Change the annotation name and description");
        instance.annotationRenamingStage.initStyle(StageStyle.UTILITY);
        instance.annotationRenamingStage.setResizable(false);
      }

      resetElements();

      // Display the current name and description of the annotation in
      // the annotation renaming window
      PathAnnotationObject selectedAnnotation = (PathAnnotationObject) QP.getSelectedObject();
      if (selectedAnnotation != null) {
        if (selectedAnnotation.getName() != null) {
          instance.textFieldName.setText(selectedAnnotation.getName());
        }
        if (selectedAnnotation.getDescription() != null) {
          instance.textFieldDescription.setText(selectedAnnotation.getDescription());
        }
      }

      // Bring the window to the foreground
      WindowUtils.setToForeground(instance.annotationRenamingStage);

      // Display the main scene
      instance.annotationRenamingStage.showAndWait();
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
    if (instance.annotationRenamingStage != null) {
      // Check if the stage is showing
      if (instance.annotationRenamingStage.isShowing()) {
        // Hide the stage
        instance.annotationRenamingStage.hide();
      }
      // Close the stage
      instance.annotationRenamingStage.close();
      instance.annotationRenamingStage = null;
    }
  }

  /**
   * Reset every element of the window to their default value.
   */
  private static void resetElements() {
    // Reset the elements of the window
    instance.labelError.setText("");
  }

  /**
   * Validate and save the new name and description for the selected annotation.
   */
  @FXML private void validateNewName() {
    PathAnnotationObject selectedAnnotation = (PathAnnotationObject) QP.getSelectedObject();
    if (selectedAnnotation == null) {
      instance.labelError.setText("No annotation selected.");
      return;
    }

    String newName = instance.textFieldName.getText();
    String newDescription = instance.textFieldDescription.getText();

    // Validate the new name and description
    if (newName.isEmpty()) {
      instance.labelError.setText("Name cannot be empty");
      return;
    }

    selectedAnnotation.setName(newName);
    selectedAnnotation.setDescription(newDescription);

    // Refresh annotation properties in QuPath
    QP.refreshIDs();

    // Close the window once the annotation is updated
    deleteStage();
  }

  /**
   * Initialize the controller.
   * This method is called automatically after the FXML file has been loaded.
   *
   * @param location The location used to resolve relative paths for the root object,
   *                 or null if the location is not known.
   * @param resources The resources used to localize the root object,
   *                  or null if the root object was not localized.
   */
  @Override
  public void initialize(URL location, ResourceBundle resources) {

  }
}
