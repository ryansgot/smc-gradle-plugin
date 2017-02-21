package com.fsryan.gradle.smc

import org.gradle.api.*

class SmcPlugin implements Plugin<Project> {

    private static final String LOCAL_SMC_JAR_FILENAME = "Smc.jar"
    private static final String LOCAL_STATEMAP_JAR_FILENAME = "statemap.jar"

    private static final String DEFAULT_SMC_DIR = "smc_6_6_0"
    private static final String SMC_JAR_ZIP_ENTRY_NAME = DEFAULT_SMC_DIR + File.separator + 'bin' + File.separator + 'Smc.jar'
    private static final String STATEMAP_JAR_ZIP_ENTRY_NAME = DEFAULT_SMC_DIR + File.separator + 'lib' + File.separator + 'Java' + File.separator + 'statemap.jar'
    private static final String DEFAULT_SMC_ZIP = "smc_6_6_0.zip"
    private static final String DEFAULT_SMC_URI = "http://pilotfiber.dl.sourceforge.net/project/smc/smc/6_6_0/" + DEFAULT_SMC_ZIP

    void apply(Project project) {

        project.extensions.create("smc", SmcExtension)

        // TODO: nail down the actual URI

        project.task('getSmc') << {
            UriChecker smcUriChecker = UriChecker.get(project.buildDir, LOCAL_SMC_JAR_FILENAME, project.smc.smcUri)
            if (smcUriChecker.ok()) {
                project.smc.smcUri = smcUriChecker.prepareURI(false).toString()
            }
            
            File libsDirectory = new File(project.smc.libsDirectory)
            UriChecker statemapUriChecker = UriChecker.get(libsDirectory, LOCAL_STATEMAP_JAR_FILENAME, project.smc.statemapJarUri)
            if (statemapUriChecker.ok()) {
                project.smc.statemapJarUri = statemapUriChecker.prepareURI(true)
            }
            
            new Unzipper(project.buildDir, new File(libsDirectory)).execute(!smcUriChecker.ok(), !statemapUriChecker.ok())
        }

        project.task('generateStateMachines') << {
            String smcJarFile = new File(new URI(project.smc.smcUri).path)
            project.sourceSets.all { ss ->
                println ss
                ss.java.srcDirs.each { dir ->
                    new SmCompiler(dir, smcJarFile, project.smc.smSrcDir, project.buildDir, project.smc.generateDotFile, project.smc.generateHtmlTable).execute()
                }
            }
        }

        project.getTasksByName('generateStateMachines', false).each { t ->
            t.dependsOn('getSmc')
        }
    }
}





