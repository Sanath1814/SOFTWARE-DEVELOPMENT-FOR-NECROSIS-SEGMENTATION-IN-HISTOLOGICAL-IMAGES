package qupath.ext.ergonomictoolbar.utils;

import javafx.stage.Stage;
import qupath.lib.gui.QuPathGUI;

/**
 * Utility class for managing windows.
 */
public class WindowUtils {
  /**
   * Method to set the stage to the foreground.
   *
   * @param annotationRenamingStage the stage to set to the foreground
   */
  public static void setToForeground(Stage annotationRenamingStage) {
    Stage quPathStage = QuPathGUI.getInstance().getStage();

    // Add a listener on the current stage focus property
    quPathStage.focusedProperty().addListener((observableValue, onHidden, onShown) -> {
      // Check if it is hidden or have the focus
      if (onHidden) {
        annotationRenamingStage.setAlwaysOnTop(false);
      }

      // Check if it is shown
      if (onShown) {
        annotationRenamingStage.setAlwaysOnTop(true);
      }
    });
  }
}
