/**
 * @author Baptiste
 */
import qupath.ext.template.DemoExtension;

def test = new DemoExtension()

print test.getName()

print test.getDescription()

print test.getQuPathVersion()

print test.getVersion()

Platform.runLater {
    test.createStage()
}