package com.fsryan.gradle.smc

import org.gradle.api.*
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.compile.JavaCompile

class SmcPlugin implements Plugin<Project> {

    private static final String DEFAULT_SMC_DIR = "smc_6_6_0"
    private static final String SMC_JAR_ZIP_ENTRY_NAME = DEFAULT_SMC_DIR + File.separator + 'bin' + File.separator + 'Smc.jar'
    private static final String STATEMAP_JAR_ZIP_ENTRY_NAME = DEFAULT_SMC_DIR + File.separator + 'lib' + File.separator + 'Java' + File.separator + 'statemap.jar'
    private static final String DEFAULT_SMC_ZIP = "smc_6_6_0.zip"
    /*package*/ static final String DEFAULT_SMC_URI = "http://pilotfiber.dl.sourceforge.net/project/smc/smc/6_6_0/" + DEFAULT_SMC_ZIP

    @Override
    void apply(Project project) {

        project.extensions.create("smc", SmcExtension)
        def getSmcTask = project.tasks.create(GetSmcTask.NAME, GetSmcTask)

        if (isAndroidProject(project)) {
            findAndroidVariants(project).all { v ->
                def generationTask = createGenerationTask(project, v)
                generationTask.dependsOn(getSmcTask)
                v.registerJavaGeneratingTask(generationTask, generationTask.generatedSourceDir)
                v.javaCompiler.dependsOn(generationTask)
            }
        } else {    // <-- assume java for now
            def generationTask = createGenerationTask(project, null)
            generationTask.dependsOn(getSmcTask)
            project.tasks.withType(JavaCompile).all { t ->
                t.dependsOn(generationTask)
            }

            retrieveSourceSets(project).findAll({ !it.name.contains('test') && !it.name.contains('Test') }).forEach { ss ->
                // in a plain java project, this does not get added to a source directory by default
                ss.java.srcDirs += generationTask.generatedSourceDir
            }
        }
    }

    static def findAndroidVariants(Project p) {
        if (!isAndroidProject(p)) {
            throw new IllegalArgumentException("Cannot get android variants if not Android project")
        }
        return p.android.hasProperty('applicationVariants') ? p.android.applicationVariants : p.android.libraryVariants
    }

    static def createGenerationTask(Project p, v) {
        File generatedSourceDir = getVariantSourceDir(p, v)
        String buildType = v == null ? null : v.buildType.name
        String flavor = v == null ? null : v.flavorName
        String taskName = generateStateMachineSourcesTaskName(buildType, flavor)
        return p.tasks.create(taskName, GenerateStateMachineCodeTask) { t ->
            t.generatedSourceDir = generatedSourceDir
            t.buildFlavorName = flavor
            t.buildTypeName = buildType
        }
    }

    static SmcExtension smcExtOf(Project p) {
        return p.extensions.findByType(SmcExtension)
    }

    static String generateStateMachineSourcesTaskName(String buildTypeName, String flavorName) {
        buildTypeName = buildTypeName == null ? "" : buildTypeName.capitalize()
        flavorName = flavorName == null ? "" : flavorName.capitalize()
        return "generate${flavorName}${buildTypeName}StateMachineSources"
    }

    static def getVariantSourceDir(Project p, v) {
        if (!isAndroidProject(p)) {
            return new File(p.buildDir, "generated-src" + File.separator + smcExtOf(p).smSrcDir)
        }
        File smOutputDir = new File(p.buildDir, "generated" + File.separator + "source" + File.separator + smcExtOf(p).smSrcDir)
        return new File(smOutputDir, v.dirName)
    }

    static def retrieveSourceSets(Project p) {
        return isAndroidProject(p) ? p.android.sourceSets : p.sourceSets
    }

    static def isAndroidProject(Project project) {
        return isAndroidApplication(project) || isAndroidLibrary(project)
    }

    static def isAndroidApplication(Project project) {
        return project.plugins.hasPlugin("com.android.application")
    }

    static def isAndroidLibrary(Project project) {
        return project.plugins.hasPlugin("com.android.library")
    }
}

class GetSmcTask extends DefaultTask {

    static final String NAME = "getSmc"

    private static final String LOCAL_SMC_JAR_FILENAME = "Smc.jar"
    private static final String LOCAL_STATEMAP_JAR_FILENAME = "statemap.jar"

    @TaskAction
    void getSmc() {
        def smcExt = SmcPlugin.smcExtOf(project)
        UriChecker smcUriChecker = UriChecker.get(project.buildDir, LOCAL_SMC_JAR_FILENAME, smcExt.smcUri)
        if (smcUriChecker.ok()) {
            smcExt.smcUri = smcUriChecker.prepareURI(false).toString()
        }

        File libsDirectory = new File(smcExt.libsDirectory)
        UriChecker statemapUriChecker = UriChecker.get(libsDirectory, LOCAL_STATEMAP_JAR_FILENAME, smcExt.statemapJarUri)
        if (statemapUriChecker.ok()) {
            smcExt.statemapJarUri = statemapUriChecker.prepareURI(true)
        }

        UnzipSummary us = new Unzipper(project.buildDir, libsDirectory).execute(!smcUriChecker.ok(), !statemapUriChecker.ok())
        smcExt.smcUri = us.smcJarUri == null ? smcExt.smcUri : us.smcJarUri
        smcExt.statemapJarUri = us.statemapJarUri == null ? smcExt.statemapJarUri : us.statemapJarUri
    }
}

class GenerateStateMachineCodeTask extends DefaultTask {

    String buildTypeName
    String buildFlavorName
    @OutputDirectory
    File generatedSourceDir

    @TaskAction
    void performGenerateTask() {
        SmcExtension smcExt = SmcPlugin.smcExtOf(project)
        String smcJarFile = new File(new URI(smcExt.smcUri).path)
        SmcPlugin.retrieveSourceSets(project).all { ss ->
            for (String dir : ss.java.srcDirs) {
                if (new File(dir) == generatedSourceDir) {
                    continue
                }
                SmOutputDirFinder outputDirFinder = new SmOutputDirFinder(buildTypeName, project.buildDir, dir, generatedSourceDir, smcExt.smSrcDir)
                new SmCompiler(new File(dir), smcJarFile, outputDirFinder, smcExt).execute()
            }
        }
    }
}
