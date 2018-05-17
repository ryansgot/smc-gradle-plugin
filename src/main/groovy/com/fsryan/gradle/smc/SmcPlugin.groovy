package com.fsryan.gradle.smc

import org.gradle.api.*
import org.gradle.api.tasks.compile.JavaCompile

class SmcPlugin implements Plugin<Project> {

    private static final String GET_SMC_TASK_NAME = "getSmc"

    private static final String LOCAL_SMC_JAR_FILENAME = "Smc.jar"
    private static final String LOCAL_STATEMAP_JAR_FILENAME = "statemap.jar"

    private static final String DEFAULT_SMC_DIR = "smc_6_6_0"
    private static final String SMC_JAR_ZIP_ENTRY_NAME = DEFAULT_SMC_DIR + File.separator + 'bin' + File.separator + 'Smc.jar'
    private static final String STATEMAP_JAR_ZIP_ENTRY_NAME = DEFAULT_SMC_DIR + File.separator + 'lib' + File.separator + 'Java' + File.separator + 'statemap.jar'
    private static final String DEFAULT_SMC_ZIP = "smc_6_6_0.zip"
    /*package*/ static final String DEFAULT_SMC_URI = "http://pilotfiber.dl.sourceforge.net/project/smc/smc/6_6_0/" + DEFAULT_SMC_ZIP

    void apply(Project project) {

        project.extensions.create("smc", SmcExtension)

        project.task(GET_SMC_TASK_NAME) << {
            UriChecker smcUriChecker = UriChecker.get(project.buildDir, LOCAL_SMC_JAR_FILENAME, (String) project.smc.smcUri)
            if (smcUriChecker.ok()) {
                project.smc.smcUri = smcUriChecker.prepareURI(false).toString()
            }
            
            File libsDirectory = new File((String) project.smc.libsDirectory)
            UriChecker statemapUriChecker = UriChecker.get(libsDirectory, LOCAL_STATEMAP_JAR_FILENAME, (String) project.smc.statemapJarUri)
            if (statemapUriChecker.ok()) {
                project.smc.statemapJarUri = statemapUriChecker.prepareURI(true)
            }
            
            UnzipSummary us = new Unzipper(project.buildDir, libsDirectory).execute(!smcUriChecker.ok(), !statemapUriChecker.ok())
            project.smc.smcUri = us.smcJarUri == null ? project.smc.smcUri : us.smcJarUri
            project.smc.statemapJarUri = us.statemapJarUri == null ? project.smc.statemapJarUri : us.statemapJarUri
        }

        project.afterEvaluate {
            if (!isAndroidProject(project)) {
                createGenerationTask(project, null)
            }
            if (project.plugins.hasPlugin("com.android.application")) {
                project.android.applicationVariants.all { v ->
                    createGenerationTask(project, v)
                }
            }
            if (project.plugins.hasPlugin("com.android.library")) {
                project.android.libraryVariants.all { v ->
                    createGenerationTask(project, v)
                }
            }
        }
    }

    def createGenerationTask(p, v) {
        File generatedSourceDir = getVariantSourceDir(p, v)
        String buildType = v == null ? null : v.buildType.name
        String flavor = v == null ? null : v.flavorName
        String taskName = generateStateMachineSourcesTaskName(buildType, flavor)
        p.task(taskName) << {
            performGenerateTask(p, buildType, flavor, generatedSourceDir)
        }
        p.getTasksByName(taskName, false).each { t ->
            if (v != null) {
                v.registerJavaGeneratingTask(t, generatedSourceDir)
            }
            t.dependsOn(GET_SMC_TASK_NAME)
        }

        if (v != null) {
            v.javaCompiler.dependsOn(taskName)
        } else {
            p.getTasks().withType(JavaCompile).all { t ->
                t.dependsOn(taskName)
            }
        }
    }

    private void performGenerateTask(Project p, String buildTypeName, String buildFlavorName, File generatedSourceDir) {
        String smcJarFile = new File(new URI(p.smc.smcUri).path)
        retrieveSourceSets(p).all { ss ->
            for (String dir : ss.java.srcDirs) {
                SmOutputDirFinder outputDirFinder = new SmOutputDirFinder(buildTypeName, p.buildDir, dir, generatedSourceDir, p.smc.smSrcDir)
                new SmCompiler(new File(dir), smcJarFile, outputDirFinder, (SmcExtension) p.smc).execute()
            }
        }
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
        File smOutputDir = new File(p.buildDir, "generated/source/" + p.smc.smSrcDir)
        return new File(smOutputDir, v.dirName)
    }

    def retrieveSourceSets(Project p) {
        return isAndroidProject(p) ? p.android.sourceSets : p.sourceSets
    }

    private boolean isAndroidProject(Project p) {
        return p.metaClass.hasProperty(p,'android')
    }
}