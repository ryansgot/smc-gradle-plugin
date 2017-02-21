package com.fsryan.gradle.smc

class SmCompiler {

    private File srcDir
    private String smcJarFile
    private String smSrcDir
    private File buildDir
    private boolean generateDotFile
    private boolean generateHtmlTable

    SmCompiler(File srcDir, String smcJarFile, String smSrcDir, File buildDir, boolean generateDotFile, boolean generateHtmlTable) {
        this.srcDir = srcDir
        this.smcJarFile = smcJarFile
        this.smSrcDir = smSrcDir
        this.buildDir = buildDir
        this.generateDotFile = generateDotFile
        this.generateHtmlTable = generateHtmlTable
    }

    void execute() {
        String baseDir = srcDir.absolutePath
        String searchDir = srcDir.parentFile.absolutePath + File.separator + smSrcDir
        File searchDirFile = new File(searchDir)
        if (searchDirFile.exists() && !searchDirFile.isDirectory()) {
            return
        }

        try {
            for (String smFile : new FileNameByRegexFinder().getFileNames(searchDir, /.*\.sm/)) {
                File outputDir = new File(baseDir + smFile.substring(searchDir.length())).parentFile
                outputDir.mkdirs()
                createOutputs(new SmcCommander(smcJarFile, smFile, outputDir.absolutePath, buildDir, smSrcDir))
            }
        } catch (FileNotFoundException fnfe) {
            // TODO: come up with a logging solution
            fnfe.printStackTrace()
        }
    }

    private void createOutputs(SmcCommander commander) {
        commander.generateStateMachine().execute()
        if (generateDotFile) {
            commander.generateDotFile().execute()
        }
        if (generateHtmlTable) {
            commander.generateHtmlTable().execute()
        }
    }
}
