package qupath.ext.ergonomictoolbar.controllers;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.util.ResourceBundle;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
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
 * Controller for the PresetAreasModification.fxml file.
 */
public class PresetAreasModificationController implements Initializable {
  private static final Logger logger = LoggerFactory.getLogger(ExtensionManagement.class);

  private static PresetAreasModificationController instance;
  private Stage presetAreasModificationStage;

  @FXML private TableColumn<String, String> tableColumnArea = new TableColumn<>();
  @FXML private TableView<String> tableViewAreas = new TableView<>();
  @FXML private TextField textFieldArea = new TextField();
  @FXML private Label labelError = new Label();

  /**
   * Get the instance of the PresetAreasModificationController.
   *
   * @return The instance of the PresetAreasModificationController.
   */
  public static PresetAreasModificationController getInstance() {
    if (instance == null) {
      new PresetAreasModificationController();
    }
    return instance;
  }

  /**
   * Constructor for the PresetAreasModificationController.
   */
  public PresetAreasModificationController() {
    instance = this;

    // Set the handler for key press events in the TableView
    instance.tableViewAreas.setOnKeyPressed(this::handleKeyPressed);

    // Set the handler for key press events in the text field
    instance.textFieldArea.setOnKeyPressed(this::handleKeyPressed);

    // Set the close request handler
    Platform.runLater(this::setCloseRequestHandler);
  }

  @Override
  public void initialize(URL location, ResourceBundle resources) {
  }

  /**
   * Open the predefined areas modification stage.
   */
  public void createStage() {
    try {
      String fxmlPath = "fxml/PresetAreasModification.fxml";

      // Load the FXML file
      var url = getClass().getResource(fxmlPath);
      FXMLLoader loader = new FXMLLoader(url);
      loader.setController(new PresetAreasModificationController());

      // Create a scene with the loaded FXML file
      Scene scene = new Scene(loader.load());

      // Set the main scene
      instance.presetAreasModificationStage = new Stage();
      instance.presetAreasModificationStage.initModality(Modality.APPLICATION_MODAL);
      instance.presetAreasModificationStage.setScene(scene);
      instance.presetAreasModificationStage.setTitle("Preset Annotations Areas");
      instance.presetAreasModificationStage.initStyle(StageStyle.UTILITY);
      instance.presetAreasModificationStage.setResizable(false);

      if (!Files.exists(FileUtils.FILE_PATH_AREAS)) {
        FileUtils.createFile(FileUtils.FILE_PATH_AREAS);
        FileUtils.addStringToFile("1", FileUtils.FILE_PATH_AREAS);
      }

      // Initialize the TableView with data from the file
      updateTableView();

      WindowUtils.setToForeground(instance.presetAreasModificationStage);

      // Display the main scene
      instance.presetAreasModificationStage.show();
    } catch (IOException e) {
      Dialogs.showErrorMessage("Extension Error", "GUI loading failed");
      logger.error("Unable to load extension interface FXML", e);
    }
  }

  /**
   * Delete the stage.
   */
  private void deleteStage() {
    // Check if the stage exists
    if (instance.presetAreasModificationStage != null) {
      // Check if the stage is showing
      if (instance.presetAreasModificationStage.isShowing()) {
        // Hide the stage
        instance.presetAreasModificationStage.hide();
      }
      // Close the stage
      instance.presetAreasModificationStage.close();
      instance.presetAreasModificationStage = null;
    }
  }

  @FXML private void addAreaToTableView() {
    String newArea = instance.textFieldArea.getText();

    // Check that the input is a positive float number
    String formattedArea = StringUtils.formatNumber(newArea);
    if (formattedArea == null) {
      instance.labelError.setText("Enter a positive integer or decimal value");
      return;
    }

    // Check if the new area is not already in the areas list
    if (FileUtils.isStringPresentInFile(formattedArea, FileUtils.FILE_PATH_AREAS)) {
      instance.labelError.setText("This area is already in the areas list");
      return;
    }

    // Add the new string to the list
    if (!Files.exists(FileUtils.FILE_PATH_AREAS)) {
      FileUtils.createFile(FileUtils.FILE_PATH_AREAS);
    }
    FileUtils.addStringToFile(formattedArea, FileUtils.FILE_PATH_AREAS);

    // Update the TableView
    updateTableView();

    // Clear the TextField after adding
    instance.textFieldArea.clear();
    instance.labelError.setText("");
  }

  @FXML private void deleteSelectedCell() {
    int selectedIndex = instance.tableViewAreas.getSelectionModel().getSelectedIndex();

    // Check if a cell is selected
    if (selectedIndex < 0) {
      instance.labelError.setText("Select an area to delete.");
      return;
    }

    if (instance.tableViewAreas.getItems().size() <= 1) {
      instance.labelError.setText("You must have at least one area.");
      return;
    }

    // Remove the selected string from the file and TableView
    if (!Files.exists(FileUtils.FILE_PATH_AREAS)) {
      FileUtils.createFile(FileUtils.FILE_PATH_AREAS);
    }
    FileUtils.removeStringFromFile(
        instance.tableViewAreas.getItems().get(selectedIndex), FileUtils.FILE_PATH_AREAS);

    instance.tableViewAreas.getItems().remove(selectedIndex);
    instance.labelError.setText("");

    // Remove the selected value from the toolbar combobox
    ToolbarController.getInstance().updatePresetAreasCombobox();
  }

  /**
   * Refresh the TableView with updated data from the file.
   * Clear the current items and add all strings from the file.
   * Sort the TableView using the numeric comparator.
   */
  private static void updateTableView() {
    instance.tableViewAreas.getItems().clear();
    instance.tableViewAreas.getItems().addAll(
        FileUtils.readStringsFromFile(FileUtils.FILE_PATH_AREAS));
    instance.tableColumnArea.setCellValueFactory(
        cellData -> new SimpleStringProperty(cellData.getValue() + " mm²"));
    instance.tableViewAreas.getItems().sort(StringUtils.numericStringComparator);
  }

  /**
   * Handle key press events in the TableView.
   * If the DELETE key is pressed, delete the selected cell.
   *
   * @param event The KeyEvent to handle.
   */
  private void handleKeyPressed(KeyEvent event) {
    if (event.getCode() == KeyCode.DELETE) {
      instance.deleteSelectedCell();
    } else if (event.getCode() == KeyCode.ENTER) {
      instance.addAreaToTableView();
    }
  }

  /**
   * Set the close request handler to display a message on the console when the window is closed.
   */
  private void setCloseRequestHandler() {
    instance.presetAreasModificationStage.setOnCloseRequest(event -> {
      ToolbarController.getInstance().updatePresetAreasCombobox();
      instance.deleteStage();
    });
  }
}