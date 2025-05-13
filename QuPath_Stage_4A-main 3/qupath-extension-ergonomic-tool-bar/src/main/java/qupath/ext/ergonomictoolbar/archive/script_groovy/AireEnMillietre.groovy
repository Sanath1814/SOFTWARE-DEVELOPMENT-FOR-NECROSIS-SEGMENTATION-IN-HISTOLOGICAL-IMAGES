/**
 * @author Baptiste
 */
import qupath.lib.gui.measure.ObservableMeasurementTableData

// get the measurement table
def imageData = getCurrentImageData()
def tissues = getAnnotationObjects()
def ob = new ObservableMeasurementTableData()
ob.setImageData(imageData, tissues)

def area = "Area µm^2"

tissues.each { tissue ->
    annotationArea = ob.getNumericValue(tissue, area)
    println((annotationArea / 1000000).round(2))
}