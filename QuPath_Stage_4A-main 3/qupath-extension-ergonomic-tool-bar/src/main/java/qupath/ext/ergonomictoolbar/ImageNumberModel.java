package qupath.ext.ergonomictoolbar;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;

/**
 * Model for the number of images before and after the current image.
 */
public class ImageNumberModel {
  private final IntegerProperty imageBefore;
  private final IntegerProperty imageAfter;
  private final IntegerProperty total;

  /**
   * Constructor.
   *
   * @param imageBefore Number of images before the current image.
   * @param imageAfter Number of images after the current image.
   * @param total Total number of images.
   */
  public ImageNumberModel(int imageBefore, int imageAfter, int total) {
    this.imageBefore = new SimpleIntegerProperty(imageBefore);
    this.imageAfter = new SimpleIntegerProperty(imageAfter);
    this.total = new SimpleIntegerProperty(total);
  }

  /**
   * Get the current value of the "imageBefore" property.
   *
   * @return the value of the "imageBefore" property.
   */
  public int getImageBefore() {
    return imageBefore.get();
  }

  /**
   * Set the value of the "imageBefore" property.
   *
   * @param value the new value of the "imageBefore" property.
   */
  public void setImageBefore(int value) {
    imageBefore.set(value);
  }

  /**
   * Get the "imageBefore" property.
   *
   * @return the property of "imageBefore"
   */
  public IntegerProperty imageBeforeProperty() {
    return imageBefore;
  }

  /**
   * Get the current value of the "imageAfter" property.
   *
   * @return The value of the "imageAfter" property.
   */
  public int getImageAfter() {
    return imageAfter.get();
  }

  /**
   * Set the value of the "imageBefore" property.
   *
   * @param value The new value of the "imageAfter" property.
   */
  public void setImageAfter(int value) {
    imageAfter.set(value);
  }

  /**
   * Get the "imageAfter" property.
   *
   * @return the property of "imageAfter"
   */
  public IntegerProperty imageAfterProperty() {
    return imageAfter;
  }

  /**
   * Get the current value of the "total" property.
   *
   * @return the value of the "total" property
   */
  public int getTotal() {
    return total.get();
  }

  /**
   * Set the value of the "total" property.
   *
   * @param value The new value of the "total" property.
   */
  public void setTotal(int value) {
    total.set(value);
  }

  /**
   * Get the "imageAfter" property.
   *
   * @return the property of "imageAfter"
   */
  public IntegerProperty totalProperty() {
    return total;
  }
}