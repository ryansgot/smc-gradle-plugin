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
        if (!javaOutputDir.isDirectory() && !javaOutputDir.mkdirs()) {
            throw new IllegalStateException("could not make directory for generated source output: " + javaOutputDir)
        }
        return "java -jar " + smcJarFile + " -java -d " + javaOutputDir.absolutePath + " " + inputFile
    }

    String generateDotFile(int level) {
        if (!artifactOutputDir.isDirectory() && !artifactOutputDir.mkdirs()) {
            throw new IllegalStateException("could not make directory for generated artifact output: " + javaOutputDir)
        }
        return "java -jar " + smcJarFile + " -graph -glevel " + level + " -d " + artifactOutputDir.absolutePath + " " + inputFile
    }

    String generateHtmlTable() {
        if (!artifactOutputDir.isDirectory() && !artifactOutputDir.mkdirs()) {
            throw new IllegalStateException("could not make directory for generated artifact output: " + javaOutputDir)
        }
        return "java -jar " + smcJarFile + " -table -d " + artifactOutputDir.absolutePath + " " + inputFile
    }
}
