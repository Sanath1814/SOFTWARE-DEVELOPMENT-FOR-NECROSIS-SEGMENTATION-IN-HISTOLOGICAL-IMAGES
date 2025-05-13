package qupath.ext.ergonomictoolbar.controllers;

import static qupath.lib.scripting.QP.getCurrentHierarchy;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.stage.Window;
import qupath.ext.ergonomictoolbar.utils.AlertUtils;
import qupath.lib.gui.QuPathGUI;
import qupath.lib.gui.viewer.QuPathViewer;
import qupath.lib.objects.classes.PathClass;
import qupath.lib.objects.classes.PathClassFactory;
import qupath.process.gui.commands.SimpleThresholdCommand;

/**
 * This class contains methods to automate the thresholding process in QuPath.
 */
public class AutomaticThresholding {

  /**
   * This method automates the thresholding process in QuPath.
   *
   * @param defaultValue The default value of the spinner.
   * @return The spinner with the default value.
   */
  private static Spinner<Double> findSpinnerByDefaultValue(double defaultValue) {
    for (Window window : Stage.getWindows()) {
      if (window instanceof Stage stage) {
        Scene scene = stage.getScene();
        if (scene != null) {
          for (Node node : scene.getRoot().lookupAll(".spinner")) {
            if (node instanceof Spinner<?> spinner) {
              if (spinner.getValue().equals(defaultValue)) {
                return (Spinner<Double>) spinner;
              }
            }
          }
        }
      }
    }
    return null;
  }

  /**
   * This method finds a ComboBox with a specific item.
   *
   * @param targetItem The item to find in the ComboBox.
   * @return The ComboBox with the target item.
   */
  private static ComboBox<String> findComboBoxWithItem(String targetItem) {
    for (Window window : Stage.getWindows()) {
      if (window instanceof Stage stage) {
        Scene scene = stage.getScene();
        if (scene != null) {
          for (Node node : scene.getRoot().lookupAll(".combo-box")) {
            if (node instanceof ComboBox<?> comboBox) {
              if (comboBox.getItems().contains(targetItem)) {
                return (ComboBox<String>) comboBox;
              }
            }
          }
        }
      }
    }
    return null;
  }

  /**
   * This method finds a button with a specific text.
   *
   * @param buttonText The text of the button to find.
   * @return The button with the target text.
   */
  private static Button findButton(String buttonText) {
    for (Window window : Stage.getWindows()) {
      if (window instanceof Stage stage) {
        Scene scene = stage.getScene();
        if (scene != null) {
          for (Node node : scene.getRoot().lookupAll(".button")) {
            if (node instanceof Button button) {
              String text = button.getText();
              if (text != null && text.equalsIgnoreCase(buttonText)) {
                return button;
              }
            }
          }
        }
      }
    }
    return null;
  }


  /**
   * This method finds a TextField with a specific prompt.
   *
   * @param prompt The prompt of the TextField to find.
   * @return The TextField with the target prompt.
   */
  private static TextField findTextFieldByPrompt(String prompt) {
    for (Window window : Stage.getWindows()) {
      if (window instanceof Stage stage) {
        Scene scene = stage.getScene();
        if (scene != null) {
          for (Node node : scene.getRoot().lookupAll(".text-field")) {
            if (node instanceof TextField textField) {
              if (textField.getPromptText().equals(prompt)) {
                return textField;
              }
            }
          }
        }
      }
    }
    return null;
  }

  /**
   * This method automates the thresholding process in QuPath.
   */
  public static void automateThresholding() {
    // Check if an image is open
    if (getCurrentHierarchy() == null) {
      AlertUtils.noImageOpen();
      return;
    }

    // Check if GUI is available
    QuPathGUI gui = QuPathGUI.getInstance();
    if (gui == null) {
      AlertUtils.noGui();
      return;
    }

    // Check if viewer is available
    QuPathViewer viewer = gui.getViewer();
    if (viewer == null) {
      AlertUtils.noViewer();
      return;
    }

    // Open Thresholding window
    SimpleThresholdCommand s = new SimpleThresholdCommand(gui);
    s.run();

    // Run UI updates in JavaFX thread
    Platform.runLater(() -> {


      // Enter Classifier Name
      TextField classifierNameField = findTextFieldByPrompt("Enter name");
      if (classifierNameField != null) {
        classifierNameField.setText("classifierpolytech");
      }
      // Set Smoothing Sigma and Threshold
      Spinner<Double> smoothingSigmaSpinner = findSpinnerByDefaultValue(0.0);
      if (smoothingSigmaSpinner != null) {
        smoothingSigmaSpinner.getValueFactory().setValue(2.0);
        smoothingSigmaSpinner.increment(0);
      }

      Spinner<Double> thresholdSpinner = findSpinnerByDefaultValue(0.0);
      if (thresholdSpinner != null) {
        thresholdSpinner.getValueFactory().setValue(230.0);
        thresholdSpinner.increment(0);
      }

      // Set Below Threshold to Tumor Area
      ComboBox<PathClass> belowThresholdComboBox = findSecondPathClassComboBox();
      if (belowThresholdComboBox != null) {
        PathClass tumorAreaClass = PathClassFactory.getPathClass("Tumor Area");
        belowThresholdComboBox.getSelectionModel().select(tumorAreaClass);
        belowThresholdComboBox.fireEvent(new javafx.event.ActionEvent());
      }

      // Set Region to Everywhere
      ComboBox<String> regionComboBox = findComboBoxWithItem("Everywhere");
      if (regionComboBox != null) {
        regionComboBox.getSelectionModel().select("Everywhere");
      }



      // Click Save
      Button saveButton = findButton("Save");
      if (saveButton != null) {
        saveButton.fire();
      }
      else {
        System.out.println("Bouton 'Save' introuvable");
      }

      Platform.runLater(() -> {
        closeLoadPixel(); // Close the "Load pixel classifier" window after all actions
      });

    });
  }

  /**
   * This method closes the thresholding window.
   */
  public static void closeThresholdingWindow() {
    for (Window window : Stage.getWindows()) {
      if (window instanceof Stage stage) {
        if (stage.getTitle() != null && stage.getTitle().equalsIgnoreCase("Create threshold")) {
          stage.hide();
          return;
        }
      }
    }
  }

  /**
   * This method finds the second PathClass ComboBox.
   *
   * @return The second PathClass ComboBox.
   */
  private static ComboBox<PathClass> findSecondPathClassComboBox() {
    int count = 0;
    for (Window window : Stage.getWindows()) {
      if (window instanceof Stage stage) {
        Scene scene = stage.getScene();
        if (scene != null) {
          for (Node node : scene.getRoot().lookupAll(".combo-box")) {
            if (node instanceof ComboBox<?> comboBox) {
              if (!comboBox.getItems().isEmpty()
                  && comboBox.getItems().get(0) instanceof PathClass) {
                count++;
                if (count == 2) {
                  return (ComboBox<PathClass>) comboBox;
                }
              }
            }
          }
        }
      }
    }
    return null;
  }

  /**
   * This method closes the "Load pixel classifier" window.
   */
  public static void closeLoadPixel() {
    for (Window window : Stage.getWindows()) {
      if (window instanceof Stage stage) {
        if (stage.getTitle() != null && stage.getTitle().equalsIgnoreCase("Create thresholder")) {
          stage.hide(); // Close the window
          return; // Exit once found
        }
      }
    }
  }

  /**
   * This method finds a button in a popup window.
   *
   * @param windowTitle The title of the popup window.
   * @param buttonText  The text of the button to find.
   * @return The button with the target text.
   */
  private static Button findButtonInPopup(String windowTitle, String buttonText) {
    for (Window window : Stage.getWindows()) {
      if (window instanceof Stage stage) {
        if (stage.getTitle() != null && stage.getTitle().equalsIgnoreCase(windowTitle)) {
          Scene scene = stage.getScene();
          if (scene != null) {
            for (Node node : scene.getRoot().lookupAll(".button")) {
              if (node instanceof Button button) {
                if (button.getText().equalsIgnoreCase(buttonText)) {
                  return button;
                }
              }
            }
          }
        }
      }
    }
    return null;
  }
}
