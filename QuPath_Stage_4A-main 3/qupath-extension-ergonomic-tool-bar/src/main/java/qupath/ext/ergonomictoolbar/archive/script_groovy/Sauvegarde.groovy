/**
 * @author Baptiste
 */
def qupath = getQuPath()
def imageData = getCurrentImageData()
def entry = getProjectEntry()
entry.saveImageData(imageData)
project.syncChanges()
qupath.refreshProject()
qupath.setReadOnly(true)