package com.fsryan.gradle.smc

class SmOutputDirFinder {

    String buildTypeName
    File buildDir
    String sourceDir
    File generatedSourceDir
    String smSourceDir

    SmOutputDirFinder(String buildTypeName, File buildDir, String sourceDir, File generatedSourceDir, String smSrcDir) {
        this.buildTypeName = buildTypeName == null ? "" : buildTypeName
        this.buildDir = buildDir
        this.sourceDir = sourceDir
        this.generatedSourceDir = generatedSourceDir
        this.smSourceDir = smSrcDir
    }

    File getSrcOutputDir() {
        return generatedSourceDir
    }

    File getArtifactOutputDir() {
        return new File(buildDir.absolutePath + File.separator + "outputs" + File.separator + smSourceDir + File.separator + buildTypeName)
    }
}
