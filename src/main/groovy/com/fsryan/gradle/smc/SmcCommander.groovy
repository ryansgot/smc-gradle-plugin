package com.fsryan.gradle.smc

/**
 * Created by ryan on 2/21/17.
 */
class SmcCommander {

    private String smcJarFile
    private String inputFile
    private String javaOutputDir
    private File buildArtifactsDir

    SmcCommander(String smcJarFile, String inputFile, String javaOutputDir, File buildDir, String artifactDir) {
        this.smcJarFile = smcJarFile
        this.inputFile = inputFile
        this.javaOutputDir = javaOutputDir
        buildArtifactsDir = new File(buildDir.absolutePath + File.separator + "outputs" + File.separator + artifactDir)
    }

    String generateStateMachine() {
        return "java -jar " + smcJarFile + " -java -d " + javaOutputDir + " " + inputFile
    }

    String generateDotFile(int level) {
        buildArtifactsDir.mkdirs()
        return "java -jar " + smcJarFile + " -graph -glevel " + level + "-d " + buildArtifactsDir.absolutePath + " " + inputFile
    }

    String generateHtmlTable() {
        buildArtifactsDir.mkdirs()
        return "java -jar " + smcJarFile + " -table -d " + buildArtifactsDir.absolutePath + " " + inputFile
    }
}
