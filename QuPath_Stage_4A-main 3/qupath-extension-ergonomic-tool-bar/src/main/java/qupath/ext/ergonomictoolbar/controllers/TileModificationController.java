package qupath.ext.ergonomictoolbar.controllers;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.util.List;
import java.util.ResourceBundle;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qupath.ext.ergonomictoolbar.ExtensionManagement;
import qupath.ext.ergonomictoolbar.utils.FileUtils;
import qupath.ext.ergonomictoolbar.utils.StringUtils;
import qupath.ext.ergonomictoolbar.utils.WindowUtils;
import qupath.fx.dialogs.Dialogs;

/**
* Controller for the modification of tiles.
*/
public class TileModificationController implements Initializable {
  private static final Logger logger = LoggerFactory.getLogger(ExtensionManagement.class);

  private static TileModificationController instance;
  private Stage tileModificationStage;

  @FXML private Label labelError = new Label();
  @FXML private Label labelCurrentArea = new Label();
  @FXML private TextField textFieldArea = new TextField();

  /**
   * Constructor for the TileModificationController.
   */
  public TileModificationController() {
    instance = this;

    // Allow updating by pressing the "Enter" key
    instance.textFieldArea.setOnKeyPressed(keyEvent -> {
      if (keyEvent.getCode() == KeyCode.ENTER) {
        updateArea();
      }
    });

    // Set the close request handler
    Platform.runLater(this::setCloseRequestHandler);
  }

  /**
   * Get the instance of the TileModificationController.
   *
   * @return an instance of TileModificationController
   */
  public static TileModificationController getInstance() {
    if (instance == null) {
      new TileModificationController();
    }
    return instance;
  }

  @Override
  public void initialize(URL location, ResourceBundle resources) {

  }

  /**
   * Open the tile modification stage.
   */
  public void createStage() {
    try {
      if (instance.tileModificationStage == null) {
        String fxmlPath = "fxml/TileModification.fxml";

        // Load the FXML file
        var url = getClass().getResource(fxmlPath);
        FXMLLoader loader = new FXMLLoader(url);
        loader.setController(new TileModificationController());
        instance = loader.getController();

        // Set the main scene
        instance.tileModificationStage = new Stage();
        instance.tileModificationStage.initModality(Modality.APPLICATION_MODAL);

        // Create a scene with the loaded FXML file
        Scene scene = new Scene(loader.load());
        instance.tileModificationStage.setScene(scene);
        instance.tileModificationStage.setTitle("Tile Area");
        instance.tileModificationStage.initStyle(StageStyle.UTILITY);
        instance.tileModificationStage.setResizable(false);
      }

      if (!Files.exists(FileUtils.FILE_PATH_TILE)) {
        FileUtils.createFile(FileUtils.FILE_PATH_TILE);
        FileUtils.addStringToFile("0.5", FileUtils.FILE_PATH_TILE);
      }

      updateLabel();

      WindowUtils.setToForeground(instance.tileModificationStage);

      // Display the main scene
      instance.tileModificationStage.show();
    } catch (IOException e) {
      Dialogs.showErrorMessage("Extension Error", "GUI loading failed");
      logger.error("Unable to load extension interface FXML", e);
    }
  }

  private void deleteStage() {
    // Check if the stage exists
    if (instance.tileModificationStage != null) {
      // Check if the stage is showing
      if (instance.tileModificationStage.isShowing()) {
        // Hide the stage
        instance.tileModificationStage.hide();
      }
      // Close the stage
      instance.tileModificationStage.close();
      instance.tileModificationStage = null;
    }
  }

  /**
   * Method that allows to update the label with the current width tile.
   */
  private static void updateLabel() {
    List<String> file = FileUtils.readStringsFromFile(FileUtils.FILE_PATH_TILE);

    if (file.isEmpty()) {
      instance.labelCurrentArea.setText("There is no current area.");
      return;
    }

    String areaValue = file.get(0);

    if (areaValue == null || areaValue.isEmpty()) {
      instance.labelCurrentArea.setText("There is no current area.");
      return;
    }

    // Vérifier si areaValue est un nombre (entier ou décimal)
    if (StringUtils.isNumeric(areaValue.replace(",", "."))) {
      instance.labelCurrentArea.setText("Current tile area : " + areaValue + " mm²");
    } else {
      instance.labelCurrentArea.setText("There is no current area.");
    }
  }

  /**
   * Update the width tile with the value of the text field.
   */
  @FXML private void updateArea() {
    String areaText = instance.textFieldArea.getText();

    String formattedArea = StringUtils.formatNumber(areaText);
    if (formattedArea == null) {
      instance.labelError.setText("Enter a positive integer or decimal value");
      return;
    }

    if (!Files.exists(FileUtils.FILE_PATH_TILE)) {
      FileUtils.createFile(FileUtils.FILE_PATH_TILE);
    }
    FileUtils.clearFile(FileUtils.FILE_PATH_TILE);
    FileUtils.addStringToFile(formattedArea, FileUtils.FILE_PATH_TILE);

    updateLabel();
    instance.labelError.setText("");
  }

  /**
   * Set the close request handler to display a message on the console when the window is closed.
   */
  private void setCloseRequestHandler() {
    instance.tileModificationStage.setOnCloseRequest(event -> {
      ToolbarController.getInstance().updatePresetAreasCombobox();
      deleteStage();
    });
  }
}