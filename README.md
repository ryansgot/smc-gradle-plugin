# smc-gradle-plugin
Gradle plugin for the State Machine Compiler (SMC) that makes it easier to use in a gradle project

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

- Android
- Java (see known issues below)
- Groovy (maybe)

## Known Issues
- In a java project, `./gradlew clean compileJava` results in missing classes at compile time, but `./gradlew clean; ./gradlew compileJava` work as separate commands.