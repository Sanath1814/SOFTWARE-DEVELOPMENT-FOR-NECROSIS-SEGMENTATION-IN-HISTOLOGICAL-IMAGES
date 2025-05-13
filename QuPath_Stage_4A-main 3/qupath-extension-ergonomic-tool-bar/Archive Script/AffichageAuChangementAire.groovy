/**
 * @author Baptiste
 */
import javafx.scene.control.Label
import javafx.stage.Stage
import javafx.scene.layout.HBox
import javafx.scene.Scene
import qupath.lib.objects.hierarchy.events.PathObjectSelectionListener
import qupath.lib.gui.measure.ObservableMeasurementTableData

MyDialog md = new MyDialog(getQuPath())
md.run()

// Implement the interface, so that change in selection triggers method inside the class
class MyDialog implements PathObjectSelectionListener {

    QuPathGUI qupath
    Label label

    MyDialog(QuPathGUI qupath) {
        this.qupath = qupath
        this.label = new Label("Something")
    }
    void run() {
        qupath.getImageData().getHierarchy().getSelectionModel().addPathObjectSelectionListener(this)
        Platform.runLater(() -> {
            Stage dialog = new Stage()
            dialog.setScene(new Scene(new HBox(label)))
            dialog.showAndWait()
            dialog.setOnHiding() {
                qupath.getImageData().getHierarchy().getSelectionModel().removePathObjectSelectionListener(this)
            }
        })
    }
    @Override
    public void selectedPathObjectChanged(PathObject pathObjectSelected, PathObject previousObject, Collection<PathObject> allSelected) {
        // Here goes your selection change logic
        if (pathObjectSelected == null)
            label.setText("No selection")
        else {
            ImageData imageData = getCurrentImageData()

            Collection<PathObject> tissues = getAnnotationObjects()
            ObservableMeasurementTableData ob = new ObservableMeasurementTableData();
            ob.setImageData(imageData, tissues);
            def area = "Area µm^2"
            def annotationArea = ob.getNumericValue(getCurrentHierarchy().getSelectionModel().getSelectedObject(), area)
            label.setText("Names: " + (annotationArea/1000000).round(2) + "\n")
        }
    }
}