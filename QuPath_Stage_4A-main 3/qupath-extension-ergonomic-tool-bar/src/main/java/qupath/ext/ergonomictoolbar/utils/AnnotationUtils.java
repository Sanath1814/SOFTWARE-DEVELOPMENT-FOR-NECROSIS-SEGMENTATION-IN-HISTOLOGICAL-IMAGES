package qupath.ext.ergonomictoolbar.utils;

import static qupath.lib.scripting.QP.getCurrentHierarchy;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import qupath.ext.ergonomictoolbar.controllers.ToolbarController;
import qupath.lib.images.ImageData;
import qupath.lib.objects.PathObject;
import qupath.lib.objects.PathObjects;
import qupath.lib.objects.hierarchy.PathObjectHierarchy;
import qupath.lib.roi.RoiTools;
import qupath.lib.roi.interfaces.ROI;
import qupath.lib.scripting.QP;

/**
 * Utility class for annotations.
 */
public class AnnotationUtils {
  /**
   * Get the annotation area.
   */
  public static double checkAnnotationArea(double annotationArea,
                                           ImageData<BufferedImage> imageData) {
    double annotationWidth;
    double annotationHeight;
    double newAnnotationArea = 0;
    boolean warning = false;

    double imageWidth = imageData.getServer().getWidth()
        * imageData.getServer().getPixelCalibration().getPixelWidthMicrons() / 1000;
    double imageHeight = imageData.getServer().getHeight()
        * imageData.getServer().getPixelCalibration().getPixelHeightMicrons() / 1000;

    switch (ToolbarController.getShapesList().get(ToolbarController.getSelectedShape())) {
      case "Square" -> {
        annotationWidth = Math.sqrt(annotationArea);
        annotationHeight = Math.sqrt(annotationArea);

        if (annotationWidth > imageWidth) {
          warning = true;
          annotationWidth = imageWidth;
          imageHeight = imageWidth;
          annotationHeight = imageHeight;
        }
        if (annotationHeight > imageHeight) {
          warning = true;
          annotationHeight = imageHeight;
          imageWidth = imageHeight;
          annotationWidth = imageWidth;
        }

        newAnnotationArea = annotationWidth * annotationHeight;
      }
      case "Rectangle" -> {
        double predefinedRectangleAspectRatio =
            ToolbarController.getPredefinedRectangleAspectRatio();

        annotationWidth = Math.sqrt(annotationArea * predefinedRectangleAspectRatio);
        annotationHeight = Math.sqrt(annotationArea / predefinedRectangleAspectRatio);

        if (annotationWidth > imageWidth) {
          warning = true;
          annotationWidth = imageWidth;
          annotationHeight = imageWidth / predefinedRectangleAspectRatio;
        }
        if (annotationHeight > imageHeight) {
          warning = true;
          annotationHeight = imageHeight;
          annotationWidth = predefinedRectangleAspectRatio * annotationHeight;
        }

        newAnnotationArea = annotationWidth * annotationHeight;
      }
      case "Circle" -> {
        annotationWidth = 2 * Math.sqrt(annotationArea / Math.PI);

        if (annotationWidth > imageWidth) {
          annotationWidth = imageWidth;
        }
        if (annotationWidth > imageHeight) {
          annotationWidth = imageHeight;
        }

        newAnnotationArea = Math.PI * Math.pow(annotationWidth / 2, 2);
      }
      default -> {
        // Do nothing
      }
    }

    // Alert he user that the annotation has been resized
    if (warning) {
      AlertUtils.selectedAreaTooBig();
    }

    return newAnnotationArea;
  }

  /**
   * Get the annotation location.
   */
  public static int[] checkAnnotationLocation(int x, int y, int width, int height,
                                              ImageData<BufferedImage> imageData) {
    int imageWidth = imageData.getServer().getWidth();

    if (x < 0) {
      x = 0;
    }
    if (x + width > imageWidth) {
      x = imageWidth - width;
    }
    if (y < 0) {
      y = 0;
    }
    int imageHeight = imageData.getServer().getHeight();
    if (y + height > imageHeight) {
      y = imageHeight - height;
    }

    int[] topLeftCorner = new int[2];
    topLeftCorner[0] = x;
    topLeftCorner[1] = y;

    return topLeftCorner;
  }

  /**
   * Method that checks if there is an annotation with the wanted classification.
   *
   * @return true if there is an annotation with the wanted classification.
   */
  public static boolean isAnnotationClassPresent(Collection<PathObject> annotations,
                                                 String annotationClass) {
    for (PathObject annotation : annotations) {
      if (annotation.getPathClass() != null
          && Objects.equals(annotation.getPathClass().getName(), annotationClass)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Method that delete all "tumor", "necrosis" and "other" annotations.
   */
  public static void removeClassAnnotations(Collection<PathObject> annotations,
                                            String annotationClass) {
    for (PathObject annotation : annotations) {
      if (annotation.getPathClass() != null
          && Objects.equals(annotation.getPathClass().getName(), annotationClass)) {
        QP.setSelectedObject(annotation);
        QP.clearSelectedObjects(false);
      }
    }
  }

  /**
   * Method that delete all annotations.
   */
  public static boolean isPathObjectInHierarchy(PathObject pathObject) {
    if (pathObject == null) {
      return false;
    }

    while (pathObject.getParent() != null) {
      pathObject = pathObject.getParent();
    }

    PathObject root = getCurrentHierarchy().getRootObject();

    return pathObject.equals(root);
  }

  /**
   * Merge annotation of a certain class.
   */
  public static void mergeAnnotations(String nameClass, PathObjectHierarchy hierarchy) {
    // Get all annotations of the specified class
    List<PathObject> annotationsToMerge = hierarchy.getAnnotationObjects().stream()
        .filter(it -> it.getPathClass() != null && it.getPathClass().getName().equals(nameClass))
        .toList();

    // If there are multiple annotations to merge
    if (annotationsToMerge.size() > 1) {
      // Create a list to store ROIs of the same class
      List<ROI> roisToMerge = new ArrayList<>();

      // Add each ROI to the list
      for (PathObject annotation : annotationsToMerge) {
        roisToMerge.add(annotation.getROI());
      }

      // Merge the ROIs
      ROI mergedRoi = RoiTools.union(roisToMerge);

      // Create a new annotation with the merged ROI and the same class
      PathObject mergedAnnotation =
          PathObjects.createAnnotationObject(mergedRoi, annotationsToMerge.get(0).getPathClass());

      // We lock the new annotation
      mergedAnnotation.setLocked(true);

      // Add the new merged annotation to the hierarchy
      hierarchy.addObject(mergedAnnotation);

      // Remove the old annotations
      hierarchy.removeObjects(annotationsToMerge, true);

      // Refresh the display to show the merged annotation
      QP.fireHierarchyUpdate();
    }
  }
}
