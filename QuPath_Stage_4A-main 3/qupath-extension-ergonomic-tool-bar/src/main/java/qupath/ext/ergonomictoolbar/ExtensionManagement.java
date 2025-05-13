package qupath.ext.ergonomictoolbar;

import static qupath.lib.gui.scripting.QPEx.getQuPath;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.Property;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qupath.ext.ergonomictoolbar.controllers.PresetAreasModificationController;
import qupath.ext.ergonomictoolbar.controllers.TileModificationController;
import qupath.ext.ergonomictoolbar.controllers.ToolbarController;
import qupath.ext.ergonomictoolbar.utils.FileUtils;
import qupath.fx.prefs.controlsfx.PropertyItemBuilder;
import qupath.lib.common.Version;
import qupath.lib.gui.QuPathGUI;
import qupath.lib.gui.extensions.GitHubProject;
import qupath.lib.gui.extensions.QuPathExtension;
import qupath.lib.gui.prefs.PathPrefs;

/**
 * This is a demo to provide a template for creating a new QuPath extension.
 *
 * <p>It doesn't do much - it just shows how to add a menu item and a preference.
 * See the code and comments below for more info.
 *
 * <p><b>Important!</b> For your extension to work in QuPath, you need to make sure the
 * name &amp; package of this class is consistent with the file
 * <pre>
 *     /resources/META-INF/services/qupath.lib.gui.extensions.QuPathExtension
 * </pre>
 */
public class ExtensionManagement implements QuPathExtension, GitHubProject {
  private static final Logger logger = LoggerFactory.getLogger(ExtensionManagement.class);

  /**
   * Display name of the extension.
   */
  private static final String EXTENSION_NAME = "Ergonomic Toolbar - Polytech Tours";

  /**
   * Short description, used under 'Extensions > Installed extensions'.
   */
  private static final String EXTENSION_DESCRIPTION =
      "Annotate all of your images faster thanks to this toolbar";

  /**
   * QuPath version that the extension is designed to work with.
   * This allows QuPath to inform the user if it seems to be incompatible.
   */
  private static final Version EXTENSION_QUPATH_VERSION = Version.parse("v0.5.0");

  /**
   * GitHub repo where you can find this extension.
   * Find updates here.
   */
  private static final GitHubRepo EXTENSION_REPOSITORY =
      GitHubRepo.create(EXTENSION_NAME, "22005460",
          "https://scm.univ-tours.fr/22005460t/QuPath_Stage_4A");
  /**
   * A 'persistent preference' - showing how to create a property that is stored whenever
   * QuPath is closed. This preference will be managed in the main QuPath GUI preferences
   * window.
   */
  private static final BooleanProperty enableExtensionProperty =
      PathPrefs.createPersistentPreference("enableExtension", true);
  /**
   * Another 'persistent preference'.
   * This one will be managed using a GUI element created by the extension.
   */
  private static final Property<Integer> numThreadsProperty =
      PathPrefs.createPersistentPreference("demo.num.threads", 1).asObject();
  private static MenuItem toolbarVisibilityMenuItem;
  /**
   * Flag indicating whether the extension is already installed.
   */
  private boolean isInstalled = false;

  /**
   * An example of how to expose persistent preferences to other classes in your extension.
   *
   * @return The persistent preference, so that it can be read or set somewhere else.
   */
  public static Property<Integer> numThreadsProperty() {
    return numThreadsProperty;
  }

  /**
   * Opens a web page with written and video tutorials.
   */
  private static void goToTutorialsPage() {
    String url = "https://github.com/Theos22/QuPath_Stage_4A/tree/main/Tutorials";

    try {
      // Create an instance of Desktop
      Desktop desktop = Desktop.getDesktop();

      // Check if Desktop supports the BROWSE action
      if (desktop.isSupported(Desktop.Action.BROWSE)) {
        // Open the specified URL in the default browser
        desktop.browse(new URI(url));

        // Smaller QuPath windows
        ToolbarController.getInstance().getToolbarStage().setIconified(true);
        getQuPath().getStage().setIconified(true);
      } else {
        System.out.println("There was a problem accessing your web browser.");
      }
    } catch (Exception e) {
      logger.debug("There was a problem reaching the following web page : {}", url);
      e.printStackTrace();
    }
  }

  public static MenuItem getToolbarVisibilityMenuItem() {
    return toolbarVisibilityMenuItem;
  }

  /**
   * Install the extension in QuPath.
   *
   * @param qupath The currently running QuPathGUI instance.
   */
  @Override
  public void installExtension(QuPathGUI qupath) {
    // Check if it is already installed
    if (isInstalled) {
      logger.debug("{} is already installed", getName());
      return;
    }

    isInstalled = true;

    // Add preferences
    addPreference(qupath);
    addPreferenceToPane(qupath);

    // Add items to the Extensions menu
    addItemsToExtensionMenu(qupath);

    // Créer les dossiers et fichiers
    try {
      // Créer le répertoire de sauvegarde
      if (Files.notExists(FileUtils.FILE_PATH_SAVE)) {
        Files.createDirectories(FileUtils.FILE_PATH_SAVE);
        logger.info("Dossier de sauvegarde créé : {}", FileUtils.FILE_PATH_SAVE);
      }

      // Créer les fichiers si ils n'existent pas déjà
      FileUtils.createFile(FileUtils.FILE_PATH_AREAS);
      FileUtils.createFile(FileUtils.FILE_PATH_TILE);
      FileUtils.createFile(FileUtils.FILE_PATH_MODEL);
    } catch (IOException e) {
      logger.error("Erreur lors de la création des dossiers ou des fichiers", e);
    }
  }

  /**
   * Add a persistent preference to the QuPath preferences pane.
   * The description is used as a tooltip.
   *
   * @param qupath The currently running QuPathGUI instance.
   */
  private void addPreferenceToPane(QuPathGUI qupath) {
    var propertyItem = new PropertyItemBuilder<>(enableExtensionProperty, Boolean.class)
        .name("Enable extension")
        .category("Ergonomic Toolbar")
        .description("Enable our Ergonomic ToolBar Extension")
        .build();

    qupath.getPreferencePane()
        .getPropertySheet()
        .getItems()
        .add(propertyItem);
  }

  /**
   * Add a persistent preference.
   * This will be loaded whenever QuPath launches, with the value retained unless the
   * preferences are reset. However, users will not be able to edit it unless you
   * create a GUI element that corresponds with it
   *
   * @param qupath The currently running QuPathGUI instance.
   */
  private void addPreference(QuPathGUI qupath) {
    qupath.getPreferencePane().addPropertyPreference(
        enableExtensionProperty,
        Boolean.class,
        "Enable this extension",
        EXTENSION_NAME,
        "Enable this extension");
  }

  /**
   * Add a new command to a QuPath menu.
   *
   * @param qupath The currently running QuPathGUI instance
   */
  private void addItemsToExtensionMenu(QuPathGUI qupath) {

    // Add a Show/Hide Extension Sub-Menu
    toolbarVisibilityMenuItem = new MenuItem("Show Toolbar");
    toolbarVisibilityMenuItem.setOnAction(
        e -> ToolbarController.getInstance().toggleStageVisibility());
    toolbarVisibilityMenuItem.disableProperty().bind(enableExtensionProperty.not());
    // Get the Extensions menu
    Menu ergonomicToolbarMenu = qupath.getMenu("Extensions>" + EXTENSION_NAME, true);
    ergonomicToolbarMenu.getItems().add(toolbarVisibilityMenuItem);

    // Add a Predefined Annotation Sizes Sub-Menu
    MenuItem presetAreasMenuItem = new MenuItem("Annotation Areas");
    presetAreasMenuItem.setOnAction(
        e -> PresetAreasModificationController.getInstance().createStage());
    ergonomicToolbarMenu.getItems().add(presetAreasMenuItem);

    // Add a tile width Sub-Menu
    MenuItem tileAreaMenuItem = new MenuItem("Tile Area");
    tileAreaMenuItem.setOnAction(e -> TileModificationController.getInstance().createStage());
    ergonomicToolbarMenu.getItems().add(tileAreaMenuItem);

    // Add an Access to tutorials Sub-Menu
    MenuItem tutorialsMenuItem = new MenuItem("Tutorials (web)");
    tutorialsMenuItem.setOnAction(e -> goToTutorialsPage());
    ergonomicToolbarMenu.getItems().add(tutorialsMenuItem);
  }

  @Override
  public String getName() {
    return EXTENSION_NAME;
  }

  @Override
  public String getDescription() {
    return EXTENSION_DESCRIPTION;
  }

  @Override
  public Version getQuPathVersion() {
    return EXTENSION_QUPATH_VERSION;
  }

  @Override
  public GitHubRepo getRepository() {
    return EXTENSION_REPOSITORY;
  }
}