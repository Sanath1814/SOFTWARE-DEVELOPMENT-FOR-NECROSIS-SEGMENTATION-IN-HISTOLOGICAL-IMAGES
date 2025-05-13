package qupath.ext.ergonomictoolbar;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.TopologyException;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.locationtech.jts.geom.prep.PreparedGeometryFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qupath.lib.objects.PathAnnotationObject;
import qupath.lib.objects.PathObject;
import qupath.lib.objects.PathObjects;
import qupath.lib.objects.PathTileObject;
import qupath.lib.roi.GeometryTools;
import qupath.lib.roi.interfaces.ROI;

/**
 * Modified version of qupath.lib.objects.utils.Tiler.
 * Made to avoid tiles overlap and allow better human vision of our Ia's analysis
 * A attempt was made at making this class a subclass of Tiler, but due to Tiler's constructor and
 * attributes being private and the file read only, it is unfortunately impossible
 * A attribute "spacing" was added, it represents the gap beetween the different tiles
 * AlternativeTiler's and Builder's constructors were adapted to take this attribute into account
 * Function "public List createGeometries(Geometry parent)" has been modified to put
 * space between tiles, 'spacing' pixels exactly, see line 212
 *
 * <p>Copyright of the original file right below
 *
 *
 * <p>This file is part of QuPath.
 * %%
 * Copyright (C) 2023 QuPath developers, The University of Edinburgh
 * %%
 * QuPath is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * <p>QuPath is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * <p>You should have received a copy of the GNU General Public License
 * along with QuPath.  If not, see <<a href="https://www.gnu.org/licenses/">...</a>>.
 * #L%
 */
public class AlternativeTiler {
  private static final Logger logger = LoggerFactory.getLogger(AlternativeTiler.class);
  private final int tileWidth;
  private final int tileHeight;
  private final boolean cropToParent;
  private final boolean filterByCentroid;
  private final AlternativeTiler.TileAlignment alignment;
  private final int spacing;

  /**
   * Constructor.
   *
   * @param tileWidth        tile width in pixels.
   * @param tileHeight       tile height in pixels.
   * @param cropToParent     controls whether tiles should be cropped to fit
   *                         within the parent object.
   * @param alignment        controls where the tiling begins, and consequently where any
   *                         cropping or overlaps will occur if the region being tiled is
   *                         not an exact multiple of the tile size.
   * @param filterByCentroid controls whether tiles whose centroid is outwith
   *                         the parent object will be removed from the
   *                         output.
   */
  private AlternativeTiler(int tileWidth, int tileHeight,
                           boolean cropToParent, AlternativeTiler.TileAlignment alignment,
                           boolean filterByCentroid, int spacing) {
    if (tileWidth <= 0 || tileHeight <= 0) {
      throw new IllegalArgumentException("tileWidth and tileHeight must be > 0, but were "
          + tileWidth + " and " + tileHeight);
    }
    this.tileWidth = tileWidth;
    this.tileHeight = tileHeight;
    this.cropToParent = cropToParent;
    this.alignment = alignment;
    this.filterByCentroid = filterByCentroid;
    this.spacing = spacing;
  }

  private static Function<Geometry, Geometry> createTileFilter(PreparedGeometry parent,
                                                               boolean cropToParent,
                                                               boolean filterByCentroid) {
    return (Geometry tile) -> {
      // straightforward case 1:
      // if there's no intersection, we're in the bounding box but not
      // the parent - skip tile
      if (!parent.intersects(tile)) {
        return null;
      }
      // straightforward case 2:
      // tile is cleanly within roi - return unchanged
      if (parent.covers(tile)) {
        return tile;
      }

      // cropping:
      if (cropToParent) {
        // crop the tile to fit the parent
        try {
          return tile.intersection(parent.getGeometry());
        } catch (TopologyException e) {
          logger.warn("Exception calculating tile intersection - tile will be skipped", e);
          return null;
        }
      } else if (!filterByCentroid || parent.contains(tile.getCentroid())) {
        // return tile unchanged if we aren't filtering based on centroids,
        // or it'd be included anyway
        return tile;
      }
      // otherwise, skip tile
      return null;
    };
  }

  /**
   * Calculate right-aligned start position.
   *
   * @param tileDim the size of the tile
   * @param parentDim the size of the parent object
   * @return the offset
   */
  private static double calculateRightAlignedStartOffset(final int tileDim,
                                                         final double parentDim) {
    double mod = parentDim % tileDim;
    if (mod == 0) {
      return 0;
    }
    return -(tileDim - mod);
  }

  /**
   * Calculate offset for symmetric tiling where the tiles cannot extend beyond the parent bounds.
   *
   * @param tileDim the size of the tile
   * @param parentDim the size of the parent object
   * @return the offset
   */
  private static double calculateInteriorOffset(final int tileDim, final double parentDim) {
    double mod = parentDim % tileDim;
    if (mod == 0) {
      return 0;
    }
    return mod / 2;
  }

  /**
   * Calculate offset for symmetric tiling where the tiles can extend beyond the parent bounds.
   *
   * @param tileDim the size of the tile
   * @param parentDim the size of the parent object
   * @return the offset
   */
  private static double calculateExteriorOffset(final int tileDim, final double parentDim) {
    double mod = parentDim % tileDim;
    if (mod == 0) {
      return 0;
    }
    return -(tileDim - mod) / 2;
  }

  /**
   * Create a new builder to generate square tiles.
   *
   * @param tileSize the width and height of the tiles, in pixels
   * @return a new builder
   */
  public static AlternativeTiler.Builder builder(int tileSize, int spacing) {
    return builder(tileSize, tileSize, spacing);
  }

  /**
   * Create a new builder to generate rectangular tiles.
   *
   * @param tileWidth  the width of the tiles, in pixels
   * @param tileHeight the height of the tiles, in pixels
   * @return a new builder
   */
  public static AlternativeTiler.Builder builder(int tileWidth, int tileHeight, int spacing) {
    return new AlternativeTiler.Builder(tileWidth, tileHeight, spacing);
  }

  /**
   * Create a new builder initialized with the settings from an existing AlternativeTiler.
   * Because tilers are immutable, this is the only way to change the settings.
   *
   * @param tiler the tiler that provides initial settings
   * @return a new builder
   */
  public static AlternativeTiler.Builder builder(AlternativeTiler tiler) {
    return new AlternativeTiler.Builder(tiler);
  }

  /**
   * Get the width of output tiles.
   *
   * @return the width in pixels
   */
  public int getTileWidth() {
    return tileWidth;
  }

  /**
   * Get the height of output tiles.
   *
   * @return the height in pixels
   */
  public int getTileHeight() {
    return tileHeight;
  }

  /**
   * Check if the tiler is set to crop the output to the input parent.
   *
   * @return whether the tiler is set to crop output to the parent object
   */
  public boolean getCropToParent() {
    return cropToParent;
  }

  /**
   * Get the tiling alignment.
   *
   * @return The current setting
   */
  public AlternativeTiler.TileAlignment getAlignment() {
    return alignment;
  }

  /**
   * Check if the tiler will filter the output based on whether the centroid
   * of tiles lies within the .
   *
   * @return The current setting
   */
  public boolean getFilterByCentroid() {
    return filterByCentroid;
  }

  /**
   * Create a list of {@link Geometry} tiles from the input. These may
   * not all be rectangular based on the settings used.
   *
   * @param parent the object that will be split into tiles.
   * @return a list of tiles
   */
  public List<Geometry> createGeometries(Geometry parent) {
    if (parent == null) {
      logger.warn(
          "AlternativeTiler.createGeometries() called with null parent - no tiles will be created");
      return new ArrayList<>();
    }

    Envelope boundingBox = parent.getEnvelopeInternal();

    double xstart = boundingBox.getMinX();
    double xend = boundingBox.getMaxX();
    switch (alignment) {
      case TOP_LEFT:
      case CENTER_LEFT:
      case BOTTOM_LEFT:
        break;
      case TOP_CENTER:
      case CENTER:
      case BOTTOM_CENTER:
        double boundingBoxWidth = xend - xstart;
        if (filterByCentroid) {
          // Shift 'inside' the parent
          xstart += calculateInteriorOffset(tileWidth, boundingBoxWidth);
        } else {
          // Shift 'outside' the parent
          xstart += calculateExteriorOffset(tileWidth, boundingBoxWidth);
        }
        break;
      case TOP_RIGHT:
      case CENTER_RIGHT:
      case BOTTOM_RIGHT:
        xstart += calculateRightAlignedStartOffset(tileWidth, xend - xstart);
        break;
      default:
        throw new IllegalStateException("Unexpected value: " + alignment);
    }

    double ystart = boundingBox.getMinY();
    double yend = boundingBox.getMaxY();
    switch (alignment) {
      case TOP_LEFT:
      case TOP_CENTER:
      case TOP_RIGHT:
        break;
      case CENTER_LEFT:
      case CENTER:
      case CENTER_RIGHT:
        double boundingBoxHeight = yend - ystart;
        if (filterByCentroid) {
          // Shift 'inside' the parent
          ystart += calculateInteriorOffset(tileHeight, boundingBoxHeight);
        } else {
          // Shift 'outside' the parent
          ystart += calculateExteriorOffset(tileHeight, boundingBoxHeight);
        }
        break;
      case BOTTOM_LEFT:
      case BOTTOM_CENTER:
      case BOTTOM_RIGHT:
        ystart += calculateRightAlignedStartOffset(tileHeight, yend - ystart);
        break;
      default:
        throw new IllegalStateException("Unexpected value: " + alignment);
    }

    //Here is the modified block of code
    //The spacing value defines the spacing beetween tiles, at pixel scaling
    List<Geometry> tiles = new ArrayList<>();
    for (int x = (int) xstart; x < xend; x += tileWidth) {
      for (int y = (int) ystart; y < yend; y += tileHeight) {
        tiles.add(GeometryTools.createRectangle(x, y, tileWidth - spacing, tileHeight - spacing));
      }
    }

    var preparedParent = PreparedGeometryFactory.prepare(parent);
    return tiles.parallelStream()
        .map(createTileFilter(preparedParent, cropToParent, filterByCentroid))
        .filter(g -> g != null)
        .collect(Collectors.toList());
  }

  /**
   * Create a list of {@link ROI} tiles from the input. These may
   * not all be rectangular based on the settings used.
   *
   * @param parent the object that will be split into tiles.
   * @return a list of tiles
   */
  public List<ROI> createRois(ROI parent) {
    return createGeometries(parent.getGeometry()).stream()
        .map(g -> GeometryTools.geometryToROI(g, parent.getImagePlane()))
        .collect(Collectors.toList());
  }

  /**
   * Create a list of {@link PathObject} tiles from the input. These may
   * not all be rectangular based on the settings used.
   *
   * @param parent  the object that will be split into tiles.
   * @param creator a function used to create the desired type
   *                of {@link PathObject}
   * @return a list of tiles
   */
  public List<PathObject> createObjects(ROI parent, Function<ROI, PathObject> creator) {
    return createRois(parent).stream().map(creator).collect(Collectors.toList());
  }

  /**
   * Create a list of {@link PathTileObject} tiles from the input. These may
   * not all be rectangular based on the settings used.
   *
   * @param parent the object that will be split into tiles.
   * @return a list of tiles
   */
  public List<PathObject> createTiles(ROI parent) {
    return createObjects(parent, PathObjects::createTileObject);
  }

  /**
   * Create a list of {@link PathAnnotationObject} tiles from the input. These may
   * not all be rectangular based on the settings used.
   *
   * @param parent the object that will be split into tiles.
   * @return a list of tiles
   */
  public List<PathObject> createAnnotations(ROI parent) {
    return createObjects(parent, PathObjects::createAnnotationObject);
  }

  /**
   * Enum representing the possible alignments for tiles.
   * A tile alignment of TOP_LEFT indicates that tiling should begin at the top left bounding box,
   * and if cropping is required then this will occur at the right and bottom.
   * An alignment of CENTER indicates that tiles may be cropped on all sides.
   */
  public enum TileAlignment {
    TOP_LEFT, TOP_CENTER, TOP_RIGHT,
    CENTER_LEFT, CENTER, CENTER_RIGHT,
    BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT
  }

  /**
   * Builder class for AlternativeTiler.
   */
  public static class Builder {

    private int tileWidth;
    private int tileHeight;
    private boolean cropToParent = true;
    private AlternativeTiler.TileAlignment alignment = AlternativeTiler.TileAlignment.CENTER;
    private boolean filterByCentroid = false;
    private int spacing;

    private Builder(int tileWidth, int tileHeight, int spacing) {
      this.tileWidth = tileWidth;
      this.tileHeight = tileHeight;
      this.spacing = spacing;
    }

    private Builder(AlternativeTiler tiler) {
      this.tileWidth = tiler.tileWidth;
      this.tileHeight = tiler.tileHeight;
      this.cropToParent = tiler.cropToParent;
      this.alignment = tiler.alignment;
      this.filterByCentroid = tiler.filterByCentroid;
    }

    /**
     * Change the height of output tiles.
     *
     * @param tileHeight the new height in pixels
     * @return this builder
     */
    public AlternativeTiler.Builder tileHeight(int tileHeight) {
      this.tileHeight = tileHeight;
      return this;
    }

    /**
     * Change the width of output tiles.
     *
     * @param tileWidth the new width in pixels
     * @return this builder
     */
    public AlternativeTiler.Builder tileWidth(int tileWidth) {
      this.tileWidth = tileWidth;
      return this;
    }

    /**
     * Set whether the tiler is set to crop the output to the input parent.
     * Using this option can result in smaller and non-rectangular tiles.
     *
     * @param cropToParent the new setting
     * @return this builder
     */
    public AlternativeTiler.Builder cropTiles(boolean cropToParent) {
      this.cropToParent = cropToParent;
      return this;
    }

    /**
     * Set the tile alignment.
     *
     * @param alignment the new setting
     * @return this builder
     */
    public AlternativeTiler.Builder alignment(AlternativeTiler.TileAlignment alignment) {
      this.alignment = alignment;
      return this;
    }

    /**
     * Start tiles at the top left of the ROI bounding box.
     *
     * @return this builder
     */
    public AlternativeTiler.Builder alignTopLeft() {
      return alignment(AlternativeTiler.TileAlignment.TOP_LEFT);
    }

    /**
     * Start tiles at the top center of the ROI bounding box.
     *
     * @return this builder
     */
    public AlternativeTiler.Builder alignTopCenter() {
      return alignment(AlternativeTiler.TileAlignment.TOP_CENTER);
    }

    /**
     * Match tiles to the top right of the ROI bounding box.
     *
     * @return this builder
     */
    public AlternativeTiler.Builder alignTopRight() {
      return alignment(AlternativeTiler.TileAlignment.TOP_RIGHT);
    }

    /**
     * Match tiles to the center left of the ROI bounding box.
     *
     * @return this builder
     */
    public AlternativeTiler.Builder alignCenterLeft() {
      return alignment(AlternativeTiler.TileAlignment.CENTER_LEFT);
    }

    /**
     * Center tiles within the ROI bounding box.
     *
     * @return this builder
     */
    public AlternativeTiler.Builder alignCenter() {
      return alignment(AlternativeTiler.TileAlignment.CENTER);
    }

    /**
     * Match tiles to the center left of the ROI bounding box.
     *
     * @return this builder
     */
    public AlternativeTiler.Builder alignCenterRight() {
      return alignment(AlternativeTiler.TileAlignment.CENTER_RIGHT);
    }

    /**
     * Match tiles to the bottom left of the ROI bounding box.
     *
     * @return this builder
     */
    public AlternativeTiler.Builder alignBottomLeft() {
      return alignment(AlternativeTiler.TileAlignment.BOTTOM_LEFT);
    }

    /**
     * Start tiles at the bottom center of the ROI bounding box.
     *
     * @return this builder
     */
    public AlternativeTiler.Builder alignBottomCenter() {
      return alignment(AlternativeTiler.TileAlignment.BOTTOM_CENTER);
    }

    /**
     * Match tiles to the bottom right of the ROI bounding box.
     *
     * @return this builder
     */
    public AlternativeTiler.Builder alignBottomRight() {
      return alignment(AlternativeTiler.TileAlignment.BOTTOM_RIGHT);
    }

    /**
     * Set if the tiler will filter the output based on whether the centroid
     * of tiles lies within the parent.
     *
     * @param filterByCentroid the new setting
     * @return this builder
     */
    public AlternativeTiler.Builder filterByCentroid(boolean filterByCentroid) {
      this.filterByCentroid = filterByCentroid;
      return this;
    }

    /**
     * Build a tiler object with the current settings.
     *
     * @return a new tiler
     */
    public AlternativeTiler build() {
      return new AlternativeTiler(tileWidth, tileHeight, cropToParent, alignment, filterByCentroid,
          spacing);
    }

  }
}
