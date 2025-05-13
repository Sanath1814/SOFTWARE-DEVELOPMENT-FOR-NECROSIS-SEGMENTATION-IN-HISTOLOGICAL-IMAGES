/**
 * @author Theo
 */
import qupath.lib.roi.RectangleROI
import qupath.lib.objects.PathAnnotationObject
import qupath.lib.images.servers.PixelCalibration

// Obtenir les données de l'image et la calibration des pixels
def imageData = getCurrentImageData()
PixelCalibration cal = imageData.getServer().getPixelCalibration()

// Taille en millimètres
double widthInMillimeters = 1.0
double heightInMillimeters = 1.0

// Convertir 1mm en pixels
double widthInPixels = widthInMillimeters / cal.getPixelWidthMicrons() * 1000
double heightInPixels = heightInMillimeters / cal.getPixelHeightMicrons() * 1000

// Obtenir la zone visible de l'image pour positionner le rectangle
def viewer = getCurrentViewer()
def bounds = viewer.getDisplayedImageBounds()

// Calcul de la position centrale du rectangle
double x = bounds.getMinX() + (bounds.getWidth() - widthInPixels) / 2
double y = bounds.getMinY() + (bounds.getHeight() - heightInPixels) / 2

// Créer le rectangle et l'annotation
RectangleROI rectangleROI = new RectangleROI(x, y, widthInPixels, heightInPixels)
PathAnnotationObject annotation = new PathAnnotationObject(rectangleROI)

// Ajouter l'annotation à l'image
addObject(annotation)
