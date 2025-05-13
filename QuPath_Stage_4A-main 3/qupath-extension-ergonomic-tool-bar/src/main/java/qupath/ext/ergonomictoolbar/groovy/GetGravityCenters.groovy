package qupath.ext.ergonomictoolbar.groovy

import qupath.lib.objects.PathObject

import static qupath.lib.scripting.QP.*

// Récupérer les données de l'image et la hiérarchie
def hierarchy = getCurrentHierarchy()

// Récupérer toutes les annotations de la hiérarchie
def annotations = hierarchy.getAnnotationObjects()

annotations.removeIf { annotation ->
    annotation.getName() != "Tumor zone"
}


// Tableaux pour stocker les objets en fonction de leur classification
def tumors = []
def necroses = []

// Parcourir toutes les annotations
annotations.each { annotation ->

    // Récupérer les objets enfants (sous-annotations)
    def children = annotation.getChildObjects()

    // Parcourir tous les objets enfants
    children.each { child ->

        // Vérifier si l'objet enfant est une instance de PathObject
        if (child instanceof PathObject) {
            // Obtenir la classification de l'objet enfant (qui est un Set)
            def classifications = child.getClassifications()

            // Vérifier si la ROI est nulle
            def roi = child.getROI()
            if (roi != null) {
                def cal = getCurrentServer().getPixelCalibration()
                double pixelWidth = cal.getPixelWidthMicrons()
                double pixelHeight = cal.getPixelHeightMicrons()

                // Coordonnées du centroïde en micromètres
                def centroidX = roi.getCentroidX() * pixelWidth
                def centroidY = roi.getCentroidY() * pixelHeight

                // Vérifier la classification de l'objet
                if (classifications.contains("Tumor")) {
                    tumors.add([centroidX, centroidY])
                } else if (classifications.contains("Necrosis")) {
                    necroses.add([centroidX, centroidY])
                }
            }
        }
    }
}

out.println(tumors)
out.println(necroses)

// Mettre à jour la hiérarchie des annotations si nécessaire
fireHierarchyUpdate()


