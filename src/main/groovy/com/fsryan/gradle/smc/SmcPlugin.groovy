package com.fsryan.gradle.smc

import org.gradle.api.*
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
                println "adding $getSmcTask.name as dependency of $generationTask.name"
                generationTask.dependsOn(getSmcTask)
                v.registerJavaGeneratingTask(generationTask, generationTask.generatedSourceDir)

                println "adding $generationTask.name as dependency of $v.javaCompiler"
                v.javaCompiler.dependsOn(generationTask)
            }
        } else {    // <-- assume java for now
            def generationTask = createGenerationTask(project, null)
            println "adding $getSmcTask.name as dependency of $generationTask.name"
            generationTask.dependsOn(getSmcTask)
            project.tasks.withType(JavaCompile).all { t ->
                println "adding $generationTask.name as dependency of $t.name"
                t.dependsOn(generationTask)
            }
            retrieveSourceSets(project).all { ss ->
                if (!ss.name.contains('test') && ss.name.contains('Test')) {
                    // in a plain java project, this does not get added to a source directory by default
                    def generatedSrc = new File(project.buildDir, 'generated-src' + File.separator + 'java')
                    println "adding to source set $ss.name: $generatedSrc"
                    ss.java.srcDirs += generatedSrc
                }
            }
        }
    }

    static def findAndroidVariants(Project p) {
        if (!isAndroidProject(p)) {
            throw new IllegalArgumentException("Cannot get android variants if not Android project")
        }
        return p.android.hasProperty('applicationVariants') ? p.android.applicationVariants : p.android.libraryVariants
    }

    def createGenerationTask(Project p, v) {
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

    private String generateStateMachineSourcesTaskName(String buildTypeName, String flavorName) {
        buildTypeName = buildTypeName == null ? "" : buildTypeName.capitalize()
        flavorName = flavorName == null ? "" : flavorName.capitalize()
        return "generate${flavorName}${buildTypeName}StateMachineSources"
    }

    def getVariantSourceDir(Project p, v) {
        if (!isAndroidProject(p)) {
            return new File(p.buildDir, "generated-src" + File.separator + "java")
        }
        File smOutputDir = new File(p.buildDir, "generated/source/" + smcExtOf(p).smSrcDir)
        return new File(smOutputDir, v.dirName)
    }

    static def retrieveSourceSets(Project p) {
        return isAndroidProject(p) ? p.android.sourceSets : p.sourceSets
    }

    static boolean isAndroidProject(Project p) {
        return p.metaClass.hasProperty(p,'android')
    }
}

class GetSmcTask extends DefaultTask {

    static final String NAME = "getSmc"

    private static final String LOCAL_SMC_JAR_FILENAME = "Smc.jar"
    private static final String LOCAL_STATEMAP_JAR_FILENAME = "statemap.jar"

    @TaskAction
    void getSmc() {
        def smcExt = SmcPlugin.smcExtOf(project)
        println("getSmcTask found smc ext: $smcExt")

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
    File generatedSourceDir

    @TaskAction
    void performGenerateTask() {
        SmcExtension smcExt = SmcPlugin.smcExtOf(project)
        String smcJarFile = new File(new URI(smcExt.smcUri).path)
        SmcPlugin.retrieveSourceSets(project).all { ss ->
            for (String dir : ss.java.srcDirs) {
                SmOutputDirFinder outputDirFinder = new SmOutputDirFinder(buildTypeName, project.buildDir, dir, generatedSourceDir, smcExt.smSrcDir)
                new SmCompiler(new File(dir), smcJarFile, outputDirFinder, smcExt).execute()
            }
        }
    }
}