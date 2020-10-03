<p align="center">
<img src="src/main/resources/icons/app_icon.svg" height= "80" width="80" alt="Bounding Box Editor Icon"/>
<br/>
<img src="demo-media/logo-text.svg" height="30" alt="Bounding Box Editor"/>
</p>

<center>

[![Build](https://github.com/mfl28/BoundingBoxEditor/workflows/Build/badge.svg)](https://github.com/mfl28/BoundingBoxEditor/actions)
[![Codecov coverage (master)](https://codecov.io/gh/mfl28/BoundingBoxEditor/branch/master/graph/badge.svg)](https://codecov.io/gh/mfl28/BoundingBoxEditor)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=mfl28_BoundingBoxEditor&metric=alert_status)](https://sonarcloud.io/dashboard?id=mfl28_BoundingBoxEditor)
[![Language grade: Java](https://img.shields.io/lgtm/grade/java/g/mfl28/BoundingBoxEditor.svg?logo=lgtm&logoWidth=18)](https://lgtm.com/projects/g/mfl28/BoundingBoxEditor/context:java)
[![GitHub release (latest by date)](https://img.shields.io/github/v/release/mfl28/BoundingBoxEditor?label=release)](https://github.com/mfl28/BoundingBoxEditor/releases/latest)
[![License](https://img.shields.io/badge/license-GPLv3-informational)](LICENSE)
</center>

This is an image annotation desktop-application written in Java using the JavaFX application platform. It allows you to create bounding box annotations using rectangular and polygonal shapes. 
Annotations can be imported and saved from/to JSON files, [Pascal VOC](http://host.robots.ox.ac.uk/pascal/VOC/) format XML-files or [YOLO](https://pjreddie.com/darknet/yolo/) format TXT-files.

<p align="center">
  <img src="demo-media/demo_v2_0_0.png" align="center">
  </br>
  <em>Demo screenshot of release v2.0.0.</em>
</p>

## Main Features
* Create rectangular and polygonal bounding box annotations for objects in images
* Export and import  rectangular and polygonal bounding box annotations to and from JSON and XML files (using [Pascal VOC](http://host.robots.ox.ac.uk/pascal/VOC/) format)
* Export and import rectangular bounding box annotations using the [YOLO](https://pjreddie.com/darknet/yolo/) format
* Format validation and error reporting when importing annotations
* Nest bounding box labels (which is then reflected in the output XML-file if using Pascal VOC format)
* Easily and swiftly navigate and search the loaded image files via a side-panel with thumbnails
* Tag bounding boxes using tags defined in the Pascal VOC format (truncated, difficult, occluded, pose: *, action: *)
* Color-coded, searchable and fully dynamic object categories

## Latest Release 
[![GitHub release (latest by date)](https://img.shields.io/github/v/release/mfl28/BoundingBoxEditor?label=release&style=for-the-badge)](https://github.com/mfl28/BoundingBoxEditor/releases/latest)
![platform](https://img.shields.io/static/v1.svg?label=Platform&message=Linux%20|%20macOS%20|%20Win%20&style=for-the-badge)

Download the latest release installer or portable image (no installation required) of *Bounding Box Editor* for your operating system from the links below. These files were created using the
[jpackage](https://openjdk.java.net/jeps/343) packaging tool, the [Badass JLink Gradle plugin](https://github.com/beryx/badass-jlink-plugin) and [github-actions](.github/workflows/workflow.yml).

| OS            | Installer | Portable |
| ------------- | --------- | -------- |
| Linux | [deb](https://github.com/mfl28/BoundingBoxEditor/releases/download/v2.2.0/boundingboxeditor-installer-linux-deb.zip), [rpm](https://github.com/mfl28/BoundingBoxEditor/releases/download/v2.2.0/boundingboxeditor-installer-linux-rpm.zip)  | [image](https://github.com/mfl28/BoundingBoxEditor/releases/download/v2.2.0/boundingboxeditor-portable-linux.zip)|
| macOS | [dmg](https://github.com/mfl28/BoundingBoxEditor/releases/download/v2.2.0/boundingboxeditor-installer-macos-dmg.zip)          | [image](https://github.com/mfl28/BoundingBoxEditor/releases/download/v2.2.0/boundingboxeditor-portable-macos.zip) |
| Windows | [exe](https://github.com/mfl28/BoundingBoxEditor/releases/download/v2.2.0/boundingboxeditor-installer-windows-exe.zip)      | [image](https://github.com/mfl28/BoundingBoxEditor/releases/download/v2.2.0/boundingboxeditor-portable-windows.zip) | 

> **Note for Linux users with Hi-DPI screens**  
> Currently JavaFX does not follow the system-wide scale settings in some Linux distributions (see e.g. bug reports [here](https://bugs.openjdk.java.net/browse/JDK-8248126) and [here](https://bugs.openjdk.java.net/browse/JDK-8238077)). The easiest way to set the desired scaling manually is by setting the `GDK_SCALE` variable when starting the application from the terminal, for example:
> ```bash 
> GDK_SCALE=2x ./BoundingBoxEditor
> ``` 
## How to use the application
Please refer to the [User Manual](https://github.com/mfl28/BoundingBoxEditor/wiki#user-manual) in the Wiki for a detailed usage guide and presentation (including gifs) of the application's main functions.

## Using annotations for object detection
After having created annotations for your images, you can use the saved bounding boxes as ground-truths in the training and evaluation of neural networks in order to perform object-detection tasks. How this can be done for any kind of labeled objects using Python and the [Pytorch](https://pytorch.org/) deep learning library is shown exemplarily in the [Humpback Whale Fluke Detection - Jupyter notebook](https://nbviewer.jupyter.org/github/mfl28/MachineLearning/blob/master/notebooks/Humpback_Whale_Fluke_Detection.ipynb) which you can find in my [Machine Learning repo](https://github.com/mfl28/MachineLearning).

## How to build the application
The project uses [Gradle](https://gradle.org/) as build-system.
You will need to have Gradle version 5+ and a Java JDK version 11+ installed on your system, e.g. from [OpenJDK](https://openjdk.java.net/). 
After cloning the repository into a folder on your machine you may build the application from the root folder by opening a command line and using:
```bash
gradlew build # Add "-x test" to skip the UI-tests.
```
*Note:* The concrete way of invoking `gradlew` depends on your OS and used command line: 
* __Linux & MacOs__: `./gradlew ...`
* __Windows__:
  - Command Prompt: `gradlew ...`
  - Powershell: `.\gradlew ...`

## How to run the application
To run the app using Gradle, use:
```bash
gradlew run
```

## How to run the tests
The project comes equipped with automatic UI-tests which use [TestFX](https://github.com/TestFX/TestFX) and the [JUnit 5](https://junit.org/junit5/) testing frameworks. Due to some used functionality in the implemented tests it is (currently) not possible to run the tests in headless mode.

To run the tests, use :
```bash
gradlew test
```

## How to build the latest Linux image and installers using Docker
First build the Docker image from the cloned repo's root directory using:
```bash
docker image build -t bbeditor .
```
Then create a writable container layer over the image (without starting a container):
```bash
docker container create --name bbeditor bbeditor
```
Finally, copy the directory containing the build artifacts to the host:
```bash
docker container cp bbeditor:/artifacts .
```
> **Alternative**:  
> If you have a recent Docker version that supports BuildKit engine (version >= 19.03) you can do 
> the whole build using a one-line command:
>```bash
> DOCKER_BUILDKIT=1 docker image build --target artifacts --output type=local,dest=. . 
>```

## Acknowledgements
* [OpenJDK](https://openjdk.java.net/) (open-source implementation of the Java platform)
* [OpenJFX](https://openjfx.io/) (open-source implementation of the JavaFX platform)
* [ControlsFX](https://github.com/controlsfx/controlsfx) (used for progress dialogs)
* [Caffeine](https://github.com/ben-manes/caffeine) (used for caching of images)
* [Gson](https://github.com/google/gson) (used for JSON serialization & deserialization)
* [Apache Commons](https://commons.apache.org/) (used for ListOrderedMap data structure and String/Iterator utilities)
* [TestFX](https://github.com/TestFX/TestFX) (used for the tests)
* [JUnit 5](https://junit.org/junit5/) (used for the tests)
* [Jacoco](https://www.jacoco.org/jacoco/) (used for creating code coverage results)
* [LibSass Gradle Plugin](https://github.com/fgiannesini/LibSassGradlePlugin) (used to compile .scss style-files into [JavaFX supported] .css files)
* [Badass JLink Plugin](https://github.com/beryx/badass-jlink-plugin) (used to create modular runtime images of the application)
* [Gradle Modules Plugin](https://github.com/java9-moduqlarity/gradle-modules-plugin) (used to run the tests on the classpath)
* [Feather Icons](https://feathericons.com/)
* [Nord Color-Palette](https://github.com/arcticicestudio/nord)
* [Unsplash](https://unsplash.com/) (used as source for test- & demo-images)

<p align=center>
<a href=https://www.jetbrains.com/?from=BoundingBoxEditor>
<img src=https://upload.wikimedia.org/wikipedia/commons/1/1a/JetBrains_Logo_2016.svg>
</a>
</p>
<p align=center>Special thanks to <a href=https://www.jetbrains.com/?from=BoundingBoxEditor> JetBrains</a> for supporting this project with an Open Source license to their developer tools!
</p>

## License
This project is licensed under GPL v3. See [LICENSE](LICENSE).




