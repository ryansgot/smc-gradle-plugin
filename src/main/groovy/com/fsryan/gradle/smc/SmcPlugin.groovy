package com.fsryan.gradle.smc

import org.gradle.api.*
import org.gradle.api.tasks.compile.JavaCompile

import java.util.logging.Logger
import java.util.logging.Level

class SmcPlugin implements Plugin<Project> {

    private static final Logger logger = Logger.getLogger(SmcPlugin.class.getSimpleName())

    private static final String LOCAL_SMC_JAR_FILENAME = "Smc.jar"
    private static final String LOCAL_STATEMAP_JAR_FILENAME = "statemap.jar"

    private static final String DEFAULT_SMC_DIR = "smc_6_6_0"
    private static final String SMC_JAR_ZIP_ENTRY_NAME = DEFAULT_SMC_DIR + File.separator + 'bin' + File.separator + 'Smc.jar'
    private static final String STATEMAP_JAR_ZIP_ENTRY_NAME = DEFAULT_SMC_DIR + File.separator + 'lib' + File.separator + 'Java' + File.separator + 'statemap.jar'
    private static final String DEFAULT_SMC_ZIP = "smc_6_6_0.zip"
    /*package*/ static final String DEFAULT_SMC_URI = "http://pilotfiber.dl.sourceforge.net/project/smc/smc/6_6_0/" + DEFAULT_SMC_ZIP

    void apply(Project project) {

        project.extensions.create("smc", SmcExtension)

        project.task('getSmc') << {
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

//        if (!project.plugins.hasPlugin("com.android.application") && !project.plugins.hasPlugin("com.android.library")) {
//            Task gsmTask = createGenerateStateMachinesTask(project, null, null)
//            project.tasks.add(gsmTask)
//            project.tasks.withType(JavaCompile).each { t ->
//                t.dependsOn(gsmTask)
//            }
//        }

        if (project.plugins.hasPlugin("com.android.application")) {
            project.android.applicationVariants.all { v ->
                File smOutputDir = new File(project.buildDir, "generated/source/sm")
                File smOutput = new File(smOutputDir, v.dirName)

                project.android.sourceSets[new File(v.dirName).getName()].java.srcDirs += smOutput.path

                String buildTypeName = v.buildType.name == null ? "" : v.buildType.name.capitalize()
                String buildFlavorName = v.flavorName == null ? "" : v.flavorName.capitalize()
                String taskName = "generate" + buildFlavorName + buildTypeName + "StateMachines"
                project.task(taskName) << {
                    performGenerateTask(project, v.buildType.name, v.flavorName)
                }
                project.getTasksByName(taskName, false).each { t ->
                    t.dependsOn('getSmc')
                }
                v.javaCompiler.dependsOn(taskName)
            }
        }

        if (project.plugins.hasPlugin("com.android.library")) {
            project.android.libraryVariants.all { v ->
                File smOutputDir = project.file("build/source/sm")
                File smOutput = new File(smOutputDir, v.dirName)

                project.android.sourceSets[new File(v.dirName).getName()].java.srcDirs += smOutput.path

                String buildTypeName = v.buildType.name == null ? "" : v.buildType.name.capitalize()
                String buildFlavorName = v.flavorName == null ? "" : v.flavorName.capitalize()
                String taskName = "generate" + buildFlavorName + buildTypeName + "StateMachines"
                project.task(taskName) << {
                    performGenerateTask(project, v.buildType.name, v.flavorName)
                }
                project.getTasksByName(taskName, false).each { t ->
                    t.dependsOn('getSmc')
                }
                v.javaCompiler.dependsOn(taskName)
            }
        }
    }

    private void performGenerateTask(Project p, String buildTypeName, String buildFlavorName) {
        String smcJarFile = new File(new URI(p.smc.smcUri).path)
        if (p.android == null) {
            p.sourceSets.all { ss ->
                println ss
                for (String dir : ss.java.srcDirs) {
                    println "executing SmCompiler for java source set: ${dir}"
                    new SmCompiler(new File(dir), smcJarFile, p.buildDir, (SmcExtension) p.smc).execute()
                }
            }
        } else {
            p.android.sourceSets.all { ss ->
                println ss.java
                for (String dir : ss.java.srcDirs) {
                    SmOutputDirFinder outpuDirFinder = new SmAndroidOutputDirFinder(buildTypeName, p.buildDir, dir)
                    new SmCompiler(new File(dir), smcJarFile, outpuDirFinder, p.smc).execute()
                }
            }
        }
    }
}

abstract class SmOutputDirFinder {

    String sourceSetName
    File buildDir
    String sourceDir

    SmOutputDirFinder(String sourceSetName, File buildDir, String sourceDir) {
        this.sourceSetName = sourceSetName
        this.buildDir = buildDir
        this.sourceDir = sourceDir
    }

    abstract File getSrcOutputDir()

    File getArtifactOutputDir() {
        File ret = new File(buildDir.absolutePath + File.separator + "outputs" + File.separator + "sm" + File.separator + sourceSetName)
        ret.mkdirs()
        return ret
    }
}

class SmAndroidOutputDirFinder extends SmOutputDirFinder {

    SmAndroidOutputDirFinder(String sourceSetName, File buildDir, String sourceDir) {
        super(sourceSetName, buildDir, sourceDir)
    }

    @Override
    File getSrcOutputDir() {
        File ret = new File(buildDir.absolutePath + File.separator + "generated" + File.separator + "source" + File.separator + "sm" + File.separator + sourceSetName)
        ret.mkdirs()
        return ret
    }
}





