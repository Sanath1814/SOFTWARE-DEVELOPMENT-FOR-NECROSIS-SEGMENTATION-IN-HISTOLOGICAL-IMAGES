package qupath.ext.ergonomictoolbar.utils;

import java.util.Objects;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

/**
 * Utility class to display custom alerts.
 */
public class AlertUtils {
  /**
   * Specify that there is no model path.
   */
  public static void noModelPath() {
    createCustomAlert("You must select a folder path for the model before using this feature.");
  }

  /**
   * Specify that the calculation started.
   */
  public static void computationStarted() {
    createCustomAlert("Work in progress... Please wait until the end.");
  }

  /**
   * Specify that there is no qupath instance.
   */
  public static void noQuPath() {
    createCustomAlert("There is a problem with QuPath.");
  }

  /**
   * Specify that there is no qupath instance.
   */
  public static void noGui() {
    createCustomAlert("There is a problem with QuPath GUI.");
  }

  /**
   * Specify that there is no qupath viewer currently.
   */
  public static void noViewer() {
    createCustomAlert("There is a problem with the QuPath Viewer.");
  }

  /**
   * Specify that there is no open project.
   */
  public static void noOpenProject() {
    createCustomAlert("Open a project to use this feature.");
  }

  /**
   * Specify that there is no image open.
   */
  public static void noImageOpen() {
    createCustomAlert("Open an image to use this feature.");
  }

  /**
   * Specify that there is no image data.
   */
  public static void noImageData() {
    createCustomAlert("No image data.");
  }

  /**
   * Specify that there is no annotation selected.
   */
  public static void noAnnotationSelected() {
    createCustomAlert("Select an annotation to use this feature.");
  }

  /**
   * Specify that the selected area is not a "Tumor Area" annotation.
   */
  public static void noTumorArea() {
    createCustomAlert("Select a \"Tumor Area\" annotation to use this feature.");
  }

  /**
   * Specify that the selected area is too small to work with it.
   */
  public static void selectedAreaTooSmall() {
    createCustomAlert("Select a bigger area to use this feature.");
  }

  /**
   * Display an error when the project backup failed.
   */
  public static void projectBackupCompleted() {
    createCustomAlert("Project backup saved successfully.");
  }

  /**
   * Display an error when the project backup failed.
   */
  public static void projectBackupFailed() {
    createCustomAlert("Project backup failed successfully 😁.");
  }

  /**
   * Display an confirmation of the computation completion.
   */
  public static void computationCompleted() {
    createCustomAlert("The operation was successful.");
  }

  /**
   * Display an error when the computation failed.
   */
  public static void computationFailed() {
    createCustomAlert("There was an error during computation.");
  }

  /**
   * Display an error when the computation failed.
   */
  public static void computationCanceled() {
    createCustomAlert("The computation has been canceled.");
  }

  /**
   * Display a confirmation of the deleting of the extracted_images directory.
   */
  public static void deletingDirectorySuccessful() {
    createCustomAlert("Ça a marché dossier vidé.");
    //createCustomAlert("The operation was successful.");
  }

  /**
   * Display an error when the predefined size is too big.
   */
  public static void selectedAreaTooBig() {
    createCustomAlert("The selected area is bigger than the image.\n"
        + "The annotation will be resized to fit in the image.");
  }

  /**
   * Display an error when the tiles of the annotation are not correct.
   */
  public static void annotationTilesError() {
    createCustomAlert("There is a problem with the tiles of the annotation.");
  }

  /**
   * Display an error when the tiles of the annotation are not correct.
   */
  private static void createCustomAlert(String description) {
    // Create a new Stage
    Stage alert = new Stage();
    alert.initStyle(StageStyle.UNDECORATED);  // Remove window decorations

    // Load the image from the resources
    Image attentionImage = new Image(Objects.requireNonNull(
        AlertUtils.class.getResourceAsStream(
            "/qupath/ext/ergonomictoolbar/controllers/img/attention.png")));
    ImageView imageView = new ImageView(attentionImage);
    imageView.setFitHeight(30);  // Set the height of the image
    imageView.setFitWidth(30);   // Set the width of the image

    // Create the text label
    Label descriptionLabel = new Label(description);

    // Create an HBox to hold the text and the image
    HBox content = new HBox(imageView, descriptionLabel);
    content.setSpacing(15);  // Add some spacing between the text and the image
    content.setStyle("-fx-alignment: center;");

    // Create a VBox to hold the HBox
    VBox root = new VBox(content);
    root.setStyle("-fx-padding: 20; -fx-alignment: center; -fx-border-color: black;");

    // Create a scene with the content
    Scene scene = new Scene(root);

    // Set the scene to the stage
    alert.setScene(scene);

    // Position the stage at the center of the screen
    alert.centerOnScreen();

    // Create a PauseTransition to close the custom alert after a short time
    PauseTransition delay = new PauseTransition(Duration.millis(1500));
    delay.setOnFinished(e -> Platform.runLater(() -> {
      alert.hide();
      System.out.println("Custom alert hidden");
    }));

    // Show the custom alert
    alert.show();
    delay.play();
  }
}
