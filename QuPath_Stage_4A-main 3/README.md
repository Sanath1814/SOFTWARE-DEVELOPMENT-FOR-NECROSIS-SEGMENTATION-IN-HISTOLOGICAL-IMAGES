# QuPath Ergonomic Tool Bar
## Qupath
**QuPath is open source software for bioimage analysis**.

Features include:

* Lots of tools to annotate and view images, including whole slide & microscopy images
* Workflows for brightfield & fluorescence image analysis
* New algorithms for common tasks, including cell segmentation, tissue microarray dearraying
* Interactive machine learning for object & pixel classification
* Customization, batch-processing & data interrogation by scripting
* Easy integration with other tools, including ImageJ

To **download QuPath**, go to the [Latest Releases](https://github.com/qupath/qupath/releases/latest) page.

For **documentation**, see [https://qupath.readthedocs.io](https://qupath.readthedocs.io)

For **help & support**, try [image.sc](https://forum.image.sc/tag/qupath) or the [links here](https://qupath.readthedocs.io/en/latest/docs/starting/help.html)

To **build QuPath from source** see [here](https://qupath.readthedocs.io/en/latest/docs/reference/building.html).

**If you find QuPath useful in work that you publish, please [_cite the publication_](https://qupath.readthedocs.io/en/latest/docs/intro/citing.html)!**

*QuPath is an academic project intended for research use only.*
*The software has been made freely available under the terms of the [GPLv3](https://github.com/qupath/qupath/blob/main/LICENSE) in the hope it is useful for this purpose, and to make analysis methods open and transparent.*

## Ergonomic Tool Bar
**Ergonomic Tool Bar** is a QuPath extension that provides a more user-friendly 
interface for the QuPath tools. It is designed to make the tools more accessible 
and easier to use.

### Features
- **Trimming** : The trimming tool allows you to select the cell in few seconds
- **Brush** : The pencil tool allows you to select tiles in few seconds to
annotate the image
- **Automatic Annotation** : The automatic annotation will divide the image into
a grid and annotate the tiles using python AI Scripts
- **Export tiles** : The export tiles tool will export the tiles to a folder
and to view these tiles in a Viewer to correct the annotations to improve the
ground truth of the AI model.

### Installation
1. Download the latest release from the [releases page](https://scm.univ-tours.fr/22005460t/QuPath_Stage_4A)
2. Unzip the downloaded file
3. Open QuPath.exe
4. Open the extension : Extensions -> Ergonomic Tool Bar -> Show Tool Bar

### Development setup
1. Clone the repository
2. Open the projet in your IDE
3. Run the makefile to build the project OR add the following include-extra in the `external` folder
```
[includeBuild]
../qupath-extension-ergonomic-tool-bar/

[dependencies]
io.github.qupath:qupath-extension-ergonomic-tool-bar:0.2.0
org.locationtech.jts:jts-core:1.20.0
org.slf4j:slf4j-api:2.0.16
io.github.qupath:qupath-core:0.6.0-rc1
io.github.qupath:qupath-extension-processing:0.6.0-rc1
```
4. Configure your project to build with gradle, the gradle files are in the `external` folder
5. Execute the jpackage task to build the project

* The extension code is in the `qupath-extension-ergonomic-tool-bar/src` folder