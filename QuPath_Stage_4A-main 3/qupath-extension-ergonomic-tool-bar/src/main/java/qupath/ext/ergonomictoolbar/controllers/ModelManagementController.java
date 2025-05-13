package qupath.ext.ergonomictoolbar.controllers;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.text.Text;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qupath.ext.ergonomictoolbar.ExtensionManagement;
import qupath.ext.ergonomictoolbar.ImageNumberModel;
import qupath.ext.ergonomictoolbar.utils.AlertUtils;
import qupath.ext.ergonomictoolbar.utils.FileUtils;
import qupath.ext.ergonomictoolbar.utils.WindowUtils;
import qupath.fx.dialogs.Dialogs;

/**
 * Class that allows to manage the model.
 */
public class ModelManagementController implements Initializable {
  private static final Logger logger = LoggerFactory.getLogger(ExtensionManagement.class);

  private WatchService watchService;
  private WatchKey watchKey;
  private Thread watcherThread;

  private static ModelManagementController instance;
  private Stage modelManagementStage;

  @FXML private Label labelCurrentPath = new Label();
  @FXML private Label labelLastUpdate = new Label();
  @FXML private Label labelError = new Label();
  @FXML private TableView<ImageNumberModel> tableViewImagesNumber = new TableView<>();
  @FXML private TableColumn<ImageNumberModel, Integer> tableColumnLastModel = new TableColumn<>();
  @FXML private TableColumn<ImageNumberModel, Integer> tableColumnNew = new TableColumn<>();
  @FXML private TableColumn<ImageNumberModel, Integer> tableColumnTotal = new TableColumn<>();

  /**
   * Constructor for the ModelManagementController.
   */
  public ModelManagementController() {
    instance = this;

    // Set up columns with a centered cell factory
    instance.tableColumnLastModel.setCellValueFactory(
        new PropertyValueFactory<>("necrosisBefore"));
    instance.tableColumnNew.setCellValueFactory(
        new PropertyValueFactory<>("necrosisAfter"));
    instance.tableColumnTotal.setCellValueFactory(
        new PropertyValueFactory<>("total"));

    // Set cell factories for the columns to center-align the text
    instance.tableColumnLastModel.setCellFactory(
        column -> createCenteredTableCell());
    instance.tableColumnNew.setCellFactory(
        column -> createCenteredTableCell());
    instance.tableColumnTotal.setCellFactory(
        column -> createCenteredTableCell());

    updateModelFilePathLabel();

    // Check if the file at FILE_PATH_MODEL is not empty
    if (!FileUtils.isFileEmpty(FileUtils.FILE_PATH_MODEL.toAbsolutePath())) {
      updateDateLabel();
      updateTableView();

      // Start a background thread to watch for changes in the folder
      Thread watcherThread = new Thread(new FolderWatcher());
      // Optional: Make the thread a daemon thread (runs in the background)
      watcherThread.setDaemon(true);
      watcherThread.start();
    }
  }

  /**
   * Get the instance of the ModelManagementController.
   *
   * @return an instance of the ModelManagementController
   */
  public static ModelManagementController getInstance() {
    if (instance == null) {
      new ModelManagementController();
    }
    return instance;
  }

  /**
   * Method that allows to load and display the stage for managing the model.
   */
  public void createStage() {
    try {
      // Check if the model management stage is not already initialized
      if (instance.modelManagementStage == null) {
        String fxmlPath = "fxml/ModelManagement.fxml";

        // Load the FXML file
        var url = getClass().getResource(fxmlPath);
        FXMLLoader loader = new FXMLLoader(url);
        loader.setController(getInstance());

        // Create a scene with the loaded FXML file
        Scene scene = new Scene(loader.load());

        // Get the controller from the FXMLLoader
        //instance = loader.getController();

        instance.modelManagementStage = new Stage();
        // Make the stage modal (blocks input to other windows)
        instance.modelManagementStage.initModality(Modality.APPLICATION_MODAL);
        // Set the scene for the stage
        instance.modelManagementStage.setScene(scene);
        instance.modelManagementStage.setTitle("Model Management");
        // Set the style of the stage
        instance.modelManagementStage.initStyle(StageStyle.UTILITY);
        instance.modelManagementStage.setResizable(false);
      }

      WindowUtils.setToForeground(instance.modelManagementStage);
      instance.modelManagementStage.showAndWait();
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
    if (instance.modelManagementStage != null) {
      // Check if the stage is showing
      if (instance.modelManagementStage.isShowing()) {
        // Hide the stage
        instance.modelManagementStage.hide();
      }
      // Close the stage
      instance.modelManagementStage.close();
      instance.modelManagementStage = null;
    }
  }

  /**
   * method that starts the updating of the model with a thread.
   */
  @FXML private void startTaskNewModel() {
    // Check if the file path for the model is not empty
    if (!FileUtils.isFileEmpty(FileUtils.FILE_PATH_MODEL.toAbsolutePath())) {
      AlertUtils.computationStarted();
      // Create a new Task to handle the processing of the model
      Task<Void> task = new Task<>() {
        @Override
        protected Void call() {
          // Execute the annotation processing code on the JavaFX Application thread
          Platform.runLater(() -> {
            try {
              updateModel();
            } catch (IOException | InterruptedException e) {
              throw new RuntimeException(e);
            }
          });
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

      Thread thread = new Thread(task);
      thread.setDaemon(true);
      thread.start();

    } else {
      // Set an error message if no model path is selected
      instance.labelError.setText("You must select a path for the model");
    }
  }

  /**
   * Method that updates the path to the model.
   */
  public void updateModelFilePathLabel() {
    if (instance.labelCurrentPath == null) {
      return;
    }

    if (FileUtils.isFileEmpty(FileUtils.FILE_PATH_MODEL.toAbsolutePath())) {
      instance.labelCurrentPath.setText("No current path");
    } else {
      instance.labelCurrentPath.setText("Current path : "
          + FileUtils.readStringsFromFile(FileUtils.FILE_PATH_MODEL.toAbsolutePath()).get(0));
    }
  }

  /**
   * Method that updates the tableView.
   */
  public void updateTableView() {
    if (!FileUtils.isFileEmpty(FileUtils.FILE_PATH_MODEL.toAbsolutePath())) {
      String path = FileUtils.readStringsFromFile(
          FileUtils.FILE_PATH_MODEL.toAbsolutePath()).get(0);
      loadDirectoryData(path, FileUtils.getCreationDate(
          new File(path + "/model.pth")));
    }
  }

  /**
   * Method that update the date of the model.
   */
  public void updateDateLabel() {
    if (instance.labelLastUpdate == null) {
      return;
    }

    if (FileUtils.isFileEmpty(FileUtils.FILE_PATH_MODEL.toAbsolutePath())) {
      instance.labelLastUpdate.setText("");
    } else {
      String path = FileUtils.readStringsFromFile(
          FileUtils.FILE_PATH_MODEL.toAbsolutePath()).get(0);
      LocalDateTime creationDate = FileUtils.getCreationDate(new File(path + "/model.pth"));

      // Define the format for displaying the date and time
      DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d MMMM yyyy ' - ' HH:mm");

      // Format the creation date using the defined formatter
      String formattedDateTime = creationDate.format(formatter);

      instance.labelLastUpdate.setText("Last Update : " + formattedDateTime);
    }
  }

  /**
   * Method that loads data from folders in order to update the tableView.
   */
  private void loadDirectoryData(String trainingSetPath, LocalDateTime lastModelDate) {
    // Create File objects for the base directory and subdirectories
    File trainingSetDirectory = new File(trainingSetPath);
    File necroticDirectory = new File(trainingSetDirectory, FileUtils.NECROTIC_SUBDIRECTORY);
    File viableDirectory = new File(trainingSetDirectory, FileUtils.VIABLE_SUBDIRECTORY);

    // Check if both subdirectories exist
    if (!necroticDirectory.isDirectory() || !viableDirectory.isDirectory()) {
      logger.error("One or more subdirectories are missing.");
      return;
    }

    // Count the number of files before and after the specified date in each directory
    int necroticBefore = FileUtils.countFilesBeforeDate(necroticDirectory, lastModelDate);
    int necroticAfter = FileUtils.countFilesAfterDate(necroticDirectory, lastModelDate);
    int viableBefore = FileUtils.countFilesBeforeDate(viableDirectory, lastModelDate);
    int viableAfter = FileUtils.countFilesAfterDate(viableDirectory, lastModelDate);

    // Clear previous items from the TableView
    instance.tableViewImagesNumber.getItems().clear();

    // Create a new ObservableList with the updated data
    ObservableList<ImageNumberModel> data = FXCollections.observableArrayList(
            new ImageNumberModel(necroticBefore, necroticAfter, necroticBefore + necroticAfter),
            new ImageNumberModel(viableBefore, viableAfter, viableBefore + viableAfter)
    );

    // Set the new data into the TableView
    instance.tableViewImagesNumber.setItems(data);
  }

  /**
   * Method that open a directory chooser in order to change the path model.
   */
  @FXML private void changeModelPath() throws IOException {
    DirectoryChooser dc = new DirectoryChooser();
    dc.setTitle("Select a folder for the model");
    File selectedDirectory = dc.showDialog(instance.modelManagementStage);

    // Check if a directory was selected
    if (selectedDirectory == null) {
      return;
    }

    // Verify that the selected directory contains the expected subdirectories
    String[] requiredSubdirectories = {FileUtils.VIABLE_SUBDIRECTORY,
        FileUtils.NECROTIC_SUBDIRECTORY};
    for (String subdirectory : requiredSubdirectories) {
      FileUtils.createDirectory(Path.of(selectedDirectory + "\\" + subdirectory));
    }

    String modelPath = selectedDirectory.getAbsolutePath();

    // Clear the contents of FILE_PATH_MODEL if it is not empty
    if (!Files.exists(FileUtils.FILE_PATH_MODEL.toAbsolutePath())) {
      FileUtils.createFile(FileUtils.FILE_PATH_MODEL.toAbsolutePath());
    } else if (!FileUtils.isFileEmpty(FileUtils.FILE_PATH_MODEL.toAbsolutePath())) {
      FileUtils.clearFile(FileUtils.FILE_PATH_MODEL.toAbsolutePath());
    }

    FileUtils.addStringToFile(modelPath, FileUtils.FILE_PATH_MODEL.toAbsolutePath());
    instance.labelError.setText("");
    updateTableView();
    updateModelFilePathLabel();
    updateDateLabel();

    // Stop the previous watcher thread if it exists and is still running
    if (instance.watcherThread != null && instance.watcherThread.isAlive()) {
      instance.watcherThread.interrupt(); // Interrupt the watcher thread
      try {
        instance.watcherThread.join(); // Wait for the thread to terminate
      } catch (InterruptedException e) {
        // Log an error if the thread join operation fails
        logger.error("Failed to join watcher thread", e);
      }
    }

    // Start a new watcher thread to monitor the new directory
    instance.watcherThread = new Thread(new FolderWatcher());
    instance.watcherThread.setDaemon(true); // Set the thread as a daemon thread
    instance.watcherThread.start(); // Start the new watcher thread
  }

  /**
   * Method that allows to create cell with a center text.
   */
  private TableCell<ImageNumberModel, Integer> createCenteredTableCell() {
    // Create a new TableCell instance for Integer values
    return new TableCell<>() {
      // Create a Text object for displaying cell content
      private final Text text = new Text();
      {
        text.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        text.setWrappingWidth(100);
        setGraphic(text);
        setAlignment(Pos.CENTER);
        setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
      }

      @Override
      protected void updateItem(Integer item, boolean empty) {
        // Call the superclass method to ensure proper behavior
        super.updateItem(item, empty);

        // Check if the cell is empty or the item is null
        if (empty || item == null) {
          text.setText("");
        } else {
          text.setText(item.toString());
        }
      }
    };
  }


  /**
   * Method that allows to update the model with current images.
   */
  public void updateModel() throws IOException, InterruptedException {
    // We stop the previous listener folder
    if (instance.watchService != null) {
      try {
        instance.watchKey.cancel();
        instance.watchService.close();
      } catch (IOException e) {
        logger.error("Failed to close watch service", e);
      }
    }

    instance.labelError.setText("");

    // Extract the Python script to a temporary file
    URL scriptUrl = ModelManagementController.class.getResource("python/TrainingModel.py");
    if (scriptUrl == null) {
      throw new RuntimeException("Python script not found in JAR");
    }

    Path tempScriptPath = Files.createTempFile("TrainingModel", ".py");
    try (InputStream in = scriptUrl.openStream()) {
      Files.copy(in, tempScriptPath, StandardCopyOption.REPLACE_EXISTING);
    }


    String nasPath = FileUtils.readStringsFromFile(
        FileUtils.FILE_PATH_MODEL.toAbsolutePath()).get(0);

    // Define the command to execute the Python script with the model path as an argument
    String[] command = {
      "python", // Python interpreter
      tempScriptPath.toString(),
      nasPath // Argument to the script (model path)
    };

    ProcessBuilder pb = new ProcessBuilder(command);
    pb.redirectErrorStream(true);

    StringBuilder output = new StringBuilder();
    try {
      Process process = pb.start();

      // Read the output of the process
      try (BufferedReader reader =
               new BufferedReader(new InputStreamReader(process.getInputStream()))) {
        String line;

        // Append each line of output to the StringBuilder
        while ((line = reader.readLine()) != null) {
          output.append(line).append("\n");
        }
      }
      process.waitFor();

    } catch (IOException | InterruptedException e) {
      throw new RuntimeException("Error executing Python script", e);
    } finally {
      // Clean up the temporary file
      Files.deleteIfExists(tempScriptPath);
    }
    //System.out.println(output);
    updateDateLabel();
    updateTableView();

    // Start a new watcher thread to monitor the new directory
    instance.watcherThread = new Thread(new FolderWatcher());
    instance.watcherThread.setDaemon(true); // Set the thread as a daemon thread
    instance.watcherThread.start(); // Start the new watcher thread
  }

  /**
   * Method that allows to put a listener to the model path.
   */
  public void addListenerToFolderModel() {
    // Close the existing WatchService if it is already running
    if (instance.watchService != null) {
      try {
        instance.watchKey.cancel();
        instance.watchService.close();
      } catch (IOException e) {
        logger.error("Failed to close watch service", e);
      }
    }

    Path path = Path.of(FileUtils.readStringsFromFile(
        FileUtils.FILE_PATH_MODEL.toAbsolutePath()).get(0));

    try {
      instance.watchService = FileSystems.getDefault().newWatchService();
      instance.watchKey = path.register(instance.watchService,
          ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
      logger.info("Watching directory " + path);

      while (true) {
        WatchKey key;
        try {
          // Block and wait for a watch key to be available
          key = instance.watchService.take();
        } catch (InterruptedException e) {
          // Log and exit if the watch service is interrupted
          logger.info("Watch service interrupted");
          return;
        } catch (ClosedWatchServiceException e) {
          // Log and exit if the watch service is closed
          logger.info("Watch service closed");
          return;
        }

        // Process all events for the key
        for (WatchEvent<?> event : key.pollEvents()) {
          WatchEvent.Kind<?> kind = event.kind();

          // Skip overflow events
          if (kind == OVERFLOW) {
            continue;
          }

          updateTableView();
          updateDateLabel();
        }

        // Reset the key to continue receiving events
        boolean valid = key.reset();

        if (!valid) {
          break;
        }
      }
    } catch (IOException e) {
      logger.error("Failed to set up watch service", e);
    }
  }

  /**
   * Class in order to put a listener on a folder.
   */
  public class FolderWatcher implements Runnable {
    @Override
    public void run() {
      addListenerToFolderModel();
    }
  }

  @Override
  public void initialize(URL location, ResourceBundle resources) {
    // Initialiser les colonnes du tableau dans initialize, pas dans le constructeur
    instance.tableColumnLastModel.setCellValueFactory(new PropertyValueFactory<>("imageBefore"));
    instance.tableColumnNew.setCellValueFactory(new PropertyValueFactory<>("imageAfter"));
    instance.tableColumnTotal.setCellValueFactory(new PropertyValueFactory<>("total"));

    instance.tableColumnLastModel.setCellFactory(column -> createCenteredTableCell());
    instance.tableColumnNew.setCellFactory(column -> createCenteredTableCell());
    instance.tableColumnTotal.setCellFactory(column -> createCenteredTableCell());

    updateModelFilePathLabel();

    if (!FileUtils.isFileEmpty(FileUtils.FILE_PATH_MODEL.toAbsolutePath())) {
      updateDateLabel();
      updateTableView();

      Thread watcherThread = new Thread(new FolderWatcher());
      watcherThread.setDaemon(true);
      watcherThread.start();
    }
  }
}