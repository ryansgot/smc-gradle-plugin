package com.fsryan.gradle.smc

class SmcCommander {

    private String smcJarFile
    private String inputFile
    private File javaOutputDir
    private File artifactOutputDir

    SmcCommander(String smcJarFile, String inputFile, File javaOutputDir, File artifactOutputDir) {
        this.smcJarFile = smcJarFile
        this.inputFile = inputFile
        this.javaOutputDir = javaOutputDir
        this.artifactOutputDir = artifactOutputDir
    }

    String generateStateMachine() {
        String command = "java -jar " + smcJarFile + " -java -d " + javaOutputDir.absolutePath + " " + inputFile
        println "generate command: " + command
        return command
    }

    String generateDotFile(int level) {
        String command = "java -jar " + smcJarFile + " -graph -glevel " + level + "-d " + artifactOutputDir.absolutePath + " " + inputFile
        println "generate dot file command: " + command
        return command
    }

    String generateHtmlTable() {
        String command = "java -jar " + smcJarFile + " -table -d " + artifactOutputDir.absolutePath + " " + inputFile
        println "generate html table command: " + command
        return command
    }
}
