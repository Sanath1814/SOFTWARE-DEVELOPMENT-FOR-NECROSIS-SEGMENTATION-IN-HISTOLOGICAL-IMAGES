/**
 * @author Enzo
 */
def anno = getCurrentHierarchy().getSelectionModel().getSelectedObject()
def name = anno.getName()
print "old name : " + name

anno.setName("machin")
name = anno.getName()
print "new name : " + name
