SUBMODULE_DIR = ./external/
DIST_DIR = external/build/dist/QuPath-`cat VERSION`
PYTHON_SCRIPTS = ./qupath-extension-ergonomic-tool-bar/src/main/resources/qupath/ext/ergonomictoolbar/controllers/python

get_submodule:
	if [ ! -d $(SUBMODULE_DIR) ]; then git submodule init && git submodule update --recursive --remote; fi
	git submodule update --init --recursive --remote
	touch $(SUBMODULE_DIR)/include-extra
	echo "[includeBuild]" \
		 "../qupath-extension-ergonomic-tool-bar/" \
		 "[dependencies]" \
		 "io.github.qupath:qupath-extension-ergonomic-tool-bar:0.2.0" \
		 "org.locationtech.jts:jts-core:1.20.0" \
		 "org.slf4j:slf4j-api:2.0.16" \
		 "io.github.qupath:qupath-core:0.6.0-rc1" \
		 "io.github.qupath:qupath-extension-processing:0.6.0-rc1" > $(SUBMODULE_DIR)/include-extra

create_python_env:
	if [ ! -d .venv ]; then python3 -m venv .venv; fi
	# Update pip
	if [ -d .venv/bin ]; then .venv/bin/pip install -U pip; fi
	if [ -d .venv/Scripts ]; then .venv/Scripts/pip install -U pip; fi
	# Install the required packages
	if [ -d .venv/bin ]; then .venv/bin/pip install -r requirements.txt; fi
	if [ -d .venv/Scripts ]; then .venv/Scripts/pip install -r requirements.txt; fi

build_python: create_python_env
	if [ -d .venv/bin ]; then .venv/bin/pyinstaller $(PYTHON_SCRIPTS)/Classification.py -F --clean; fi
	if [ -d .venv/Scripts ]; then .venv/Scripts/pyinstaller $(PYTHON_SCRIPTS)/Classification.py -F --clean; fi
	if [ -d .venv/bin ]; then .venv/bin/pyinstaller $(PYTHON_SCRIPTS)/viewer.py -F --clean; fi
	if [ -d .venv/Scripts ]; then .venv/Scripts/pyinstaller $(PYTHON_SCRIPTS)/viewer.py -F --clean; fi
	mv dist/* qupath-extension-ergonomic-tool-bar/src/main/resources/qupath/ext/ergonomictoolbar/controllers/python/

package: get_submodule build_python
	cd external && ./gradlew jpackage


build: package build_python

archive: build
	cd build/dist && zip -r QuPath-`cat ../../VERSION`.zip QuPath-`cat ../../VERSION`