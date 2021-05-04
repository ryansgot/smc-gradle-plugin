# smc-gradle-plugin
Gradle plugin for the State Machine Compiler (SMC) that makes it easier to use in a gradle project. See [SMC Gradle Plugin Examples](https://github.com/ryansgot/smc-gradle-plugin-example) for sample projects.

## What is the State Machine Compiler?
The SMC project is hosted on sourceforge here: http://smc.sourceforge.net/. It serves to generate state machine code for you via supplying a DSL. Please see the website for more information.

## Why Make a plugin for it?
Normally, use of SMC involves:

1. Downloading the zip
2. Unzipping it and extracting the Smc.jar to a directory in which you will run and the statemap.jar inot a directory that will link to your application code
3. Writing your state machine in the SMC DSL
4. Running the Smc.jar with some arguments to create your generated source and/or documentation artifacts into your source
5. Compiling/assembling/running your project

This process is overly-manual when you have tools like gradle that are extensible enough to do those steps for you. Furthermore, the source you would actually like to control is the DSL--not the generated code. Therefore, smc-gradle-plugin does the following for you:

1. Downloads the zip if necessary (you can configure this to not happen)
2. Extracts only the necessary files from the zip, including either the Smc.jar or statemap.jar or both
3. Inspects your source for .sm (by default) files.
4. For each .sm file, it generates the state machine source in the appropriate generated directory and optionally, generates the state machine table and graphviz dot file
5. Adds the generated source directory to the appropriate source set

## Support

- Android (Java/Kotlin)
- Java
- Kotlin
- Groovy (maybe)

## How to use
The smc-gradle-plugin affects the task dependency graph automatically for you with the necessary tasks to download and extract the necessary jar files for producing and linking the statemap library code for you. It also contains a task for running the Smc.jar. Because of this, all you have to do is apply the plugin in your project. The only stipulation is the smc gradle plugin needs to be applied after the android application/library or java plugin.

## Gradle Setup
Add the following to your root project's build.gradle file:
```groovy
buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath 'com.fsryan.gradle.smc:smc:0.1.0'
    }
}
```
Add the following to your app module's build.gradle file:
```groovy
apply plugin: 'com.android.application'  // or 'java' or 'com.android.library'
apply plugin: 'smc'

/* ... */

smc {
    // smcUri is the uri where you have put the Smc.jar file on your device.
    // If you don't have this, or if you configure it wrong, then the SMC download site's version will be downloaded.
    smcUri = 'file://' + rootProject.projectDir.absolutePath + File.separator + 'bin' + File.separator + 'Smc.jar'
    // The below assumes that there is a file stored at bin/Smc.jar which is relative to the root project's directory
    // statemapJarUri is the uri where you have put the statemap.jar file on your device.
    // If you don't have this, or if you configure it wrong, then the SMC download site's version will be downloaded.
    // The below assumes that there is a file stored at libs/statemap.jar which is relative to the root project's directory
    statemapJarUri = 'file://' + rootProject.projectDir.absolutePath + File.separator + 'libs' + File.separator + 'statemap.jar'

    // If the statemap.jar file gets downloaded, then it will be put here
    libsDirectory = "libs"  // <-- the default, you should have dependencies { implementation fileTree(dir: 'libs', include: ['*.jar']) }
    // This is the subdirectory containing the state machine sources--you can view it as though it is a source set
    smSrcDir = "sm"         // <-- the default, you should have src/main/sm and then use a directory structure that matches your java package structure
    graphVizLevel = 2       // <-- generated graphviz diagram (there are three levels of detail, 0, 1, and 2 and -1 means "don't generate")
    outputHtmlTable = true  // <-- generate an HTML table representation
}
```

## Suggestions?
- Feel free to add an issue with a suggestion to make this better.
