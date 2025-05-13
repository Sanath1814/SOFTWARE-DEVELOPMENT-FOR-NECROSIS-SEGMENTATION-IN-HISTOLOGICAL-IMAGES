package qupath.ext.ergonomictoolbar.utils;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import qupath.ext.ergonomictoolbar.AlternativeTiler;
import qupath.lib.gui.QuPathGUI;
import qupath.lib.images.ImageData;
import qupath.lib.images.servers.PixelCalibration;
import qupath.lib.objects.PathObject;
import qupath.lib.objects.hierarchy.PathObjectHierarchy;
import qupath.lib.objects.utils.Tiler;

/**
 * Utility class for tiling annotations.
 */
public class TilerUtils {
  private static final ArrayList<UUID> tiledAnnotations = new ArrayList<>();

  /**
   * Method that creates all the tiles from an annotation.
   */
  public static void annotationTilesCreator(PathObject pathObject, PathObjectHierarchy hierarchy,
                                            double tileWidthInMicrons, double tileHeightInMicrons) {
    ImageData<BufferedImage> imageData = QuPathGUI.getInstance().getImageData();

    PixelCalibration cal = imageData.getServer().getPixelCalibration();

    double tileWidthInPixels = tileWidthInMicrons / cal.getPixelWidthMicrons();
    double tileHeightInPixels = tileHeightInMicrons / cal.getPixelHeightMicrons();

    Tiler.Builder tilerBuilder = Tiler.builder((int) tileWidthInPixels, (int) tileHeightInPixels);

    Tiler tiler = tilerBuilder.build();

    List<PathObject> tiles = tiler.createTiles(pathObject.getROI());

    // Add the tiles to the hierarchy
    hierarchy.addObjects(tiles);

    // Add the tiles as children of the selected object
    for (PathObject tile : tiles) {
      pathObject.addChildObject(tile);
    }
  }

  /**
   * Method that creates all the tiles from an annotation.
   */
  public static List<PathObject> tilesCreator(ImageData<BufferedImage> imageData,
                                              PathObject annotation, int tileSize, int spacing) {
    // Get the pixel calibration information from the image data
    PixelCalibration cal = imageData.getServer().getPixelCalibration();

    // Calculate the average microns per pixel
    double micronsPerPixel = (cal.getPixelWidthMicrons() + cal.getPixelHeightMicrons()) / 2.0;

    // Convert the tile size from microns to pixels
    int tileSizePixels = (int) (tileSize / micronsPerPixel);

    // Check if the microns per pixel or tile size in pixels are invalid
    if (micronsPerPixel <= 0 || tileSizePixels <= 0) {
      return null;
    }

    // Start the tiling of our selection, with our spacing value
    AlternativeTiler.Builder tilerBuilder =
        AlternativeTiler.builder(tileSizePixels, tileSizePixels, spacing);
    AlternativeTiler tiler = tilerBuilder.build();
    List<PathObject> tiles = tiler.createTiles(annotation.getROI());

    int num = 100;

    // Label each tile with a unique name
    for (PathObject tile : tiles) {
      tile.setName("t" + num);
      num++;
    }

    return tiles;
  }

  /**
   * Method that creates all the tiles from an annotation.
   *
   * @return the list of tiles
   */
  public static ArrayList<UUID> getTiledAnnotations() {
    return tiledAnnotations;
  }

  /**
   * Method that adds a tiled annotation to the list.
   *
   * @param pathObject the annotation to add
   */
  public static void addTiledAnnotation(PathObject pathObject) {
    if (!tiledAnnotations.contains(pathObject.getID())) {
      tiledAnnotations.add(pathObject.getID());
    }
  }

  /**
   * Method that removes a tiled annotation from the list.
   *
   * @param pathObject the annotation to remove
   */
  public static void removeTiledAnnotation(PathObject pathObject) {
    tiledAnnotations.remove(pathObject.getID());
  }
}
