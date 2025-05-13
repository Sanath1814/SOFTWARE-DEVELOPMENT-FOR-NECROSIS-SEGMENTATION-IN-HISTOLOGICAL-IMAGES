package qupath.ext.ergonomictoolbar.controllers;

import static qupath.lib.scripting.QP.getCurrentHierarchy;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.stage.Stage;
import javafx.stage.Window;
import qupath.ext.ergonomictoolbar.utils.AlertUtils;
import qupath.lib.gui.QuPathGUI;
import qupath.lib.gui.viewer.QuPathViewer;
import qupath.process.gui.commands.ui.LoadResourceCommand;

/**
 * A class to automate creating annotation in QuPath.
 */
public class AutomaticDetection {
  /**
   * Create an automatic annotation using the pixel classifier "classifierpolytech".
   *
   * @return True if the operation was successful, False otherwise.
   */

  //a method to find comboboc containing classifiers names
  private static ComboBox<String> findClassifierComboBox() {
    for (Window window : Stage.getWindows()) {
      if (window instanceof Stage stage) {
        Scene scene = stage.getScene();
        if (scene != null) {
          for (Node node : scene.getRoot().lookupAll(".combo-box")) {
            if (node instanceof ComboBox<?> comboBox) {
              // Vérifier si la ComboBox contient "test"
              if (comboBox.getItems().contains("classifierpolytech")) {
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
   * Find the ComboBox to select the region for the automatic annotation.
   *
   * @return The ComboBox to select the region for the automatic annotation.
   */
  private static ComboBox<String> findRegionComboBox() {
    for (Window window : Stage.getWindows()) {
      if (window instanceof Stage stage) {
        Scene scene = stage.getScene();
        if (scene != null) {
          for (Node node : scene.getRoot().lookupAll(".combo-box")) {
            if (node instanceof ComboBox<?> comboBox) {
              if (comboBox.getItems().contains("Everywhere")) {
                return (ComboBox<String>) comboBox;
              }
            }
          }
        }
      }
    }
    return null;
  }
  //Method to find buttons
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
   * Find the ComboBox to select the parent objects.
   *
   * @return The ComboBox to select the parent objects.
   */
  private static ComboBox<String> findParentObjectsComboBox() {
    for (Window window : Stage.getWindows()) {
      if (window instanceof Stage stage) {
        Scene scene = stage.getScene();
        if (scene != null) {
          for (Node node : scene.getRoot().lookupAll(".combo-box")) {
            if (node instanceof ComboBox<?> comboBox) {
              if (comboBox.getItems().contains("Current selection")) {
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
   * Find the CheckBox to set new objects to selected.
   *
   * @return The CheckBox to set new objects to selected.
   */
  private static CheckBox findSetNewObjectsToSelectedCheckBox() {
    for (Window window : Stage.getWindows()) {
      if (window instanceof Stage stage) {
        Scene scene = stage.getScene();
        if (scene != null) {
          for (Node node : scene.getRoot().lookupAll(".check-box")) {
            if (node instanceof CheckBox checkBox) {
              if (checkBox.getText().equalsIgnoreCase("Set new objects to selected")) {
                return checkBox;
              }
              if (checkBox.getText().equalsIgnoreCase("Split objects")) {
                return checkBox;
              }
            }
          }
        }
      }
    }
    return null;
  }

  /**
   * Create an automatic annotation using the pixel classifier "test".
   */
  public static void createAutoAnnotation() {

    // Verify if there is an open image
    //*
    if (getCurrentHierarchy() == null) {
      AlertUtils.noImageOpen();
      return;
    }


    QuPathGUI gui = QuPathGUI.getInstance();
    if (gui == null) {
      AlertUtils.noGui();
      return;
    }


    QuPathViewer viewer = gui.getViewer();
    if (viewer == null) {
      AlertUtils.noViewer();
      return;
    }
    //*
    //Load Pixel Classifier view
    var commandLoad = LoadResourceCommand.createLoadPixelClassifierCommand(gui);
    commandLoad.run();



    Platform.runLater(() -> {
      ComboBox<String> classifierComboBox = findClassifierComboBox();
      //check if classifierpolytech exists
      if (classifierComboBox != null) {
        classifierComboBox.getSelectionModel().select("classifierpolytech");
      }
      // if no create it
      if (classifierComboBox == null) {
        AutomaticThresholding.automateThresholding();
      }

      ComboBox<String> regionComboBox = findRegionComboBox();
      if (regionComboBox != null) {
        regionComboBox.getSelectionModel().select("Any Annotation ROI");
      }

      Platform.runLater(() -> {
        Button createObjectsButton = findButton("Create objects");
        if (createObjectsButton != null) {
          createObjectsButton.fire();  // clic on create objects button
        }
      });

      Platform.runLater(() -> {
        // Sélect "Current selection" in "Choose parent objects"
        ComboBox<String> parentObjectsComboBox = findParentObjectsComboBox();
        if (parentObjectsComboBox != null) {
          parentObjectsComboBox.getSelectionModel().select("Current selection");
        }

        // Cliq on "OK" to valid parent object choice
        Button okButton = findButton("OK");
        if (okButton != null) {
          okButton.fire();
        }
      });

      Platform.runLater(() -> {
        // Find "Set new objects to selected" and check it
        CheckBox setSelectedCheckBox = findSetNewObjectsToSelectedCheckBox();
        if (setSelectedCheckBox != null) {
          setSelectedCheckBox.setSelected(true);
        }

        // Find "OK" Button To create annotation
        Button finalOkButton = findButton("OK");
        if (finalOkButton != null) {
          finalOkButton.fire();
        }
      });

      Platform.runLater(() -> {
        closeLoadPixelClassifier(); // Close the "Load pixel classifier" window after all actions
      });
    });
  }

  /**
   * Close the "Load pixel classifier" window.
   */
  public static void closeLoadPixelClassifier() {
    for (Window window : Stage.getWindows()) {
      if (window instanceof Stage stage) {
        if (stage.getTitle() != null && stage.getTitle().equalsIgnoreCase(
            "Load pixel classifier")) {
          stage.hide(); // Close the window
          return; // Exit once found
        }
      }
    }
  }
}
