package com.fsryan.gradle.smc

import java.util.logging.Level
import java.util.logging.Logger

class SmCompiler {

    private static final Logger logger = Logger.getLogger(SmCompiler.class.simpleName)

    private File srcDir
    private String smcJarFile
    private String smSrcDir
    private File buildDir
    private int graphVizLevel
    private boolean generateHtmlTable

    SmCompiler(File srcDir, String smcJarFile, String smSrcDir, File buildDir, int graphVizLevel, boolean generateHtmlTable) {
        this.srcDir = srcDir
        this.smcJarFile = smcJarFile
        this.smSrcDir = smSrcDir
        this.buildDir = buildDir
        this.graphVizLevel = Math.min(2, graphVizLevel)
        this.generateHtmlTable = generateHtmlTable
    }

    void execute() {
        String baseDir = buildDir.absolutePath + File.separator + "generated-src"
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
            logger.log(Level.WARNING, "not found: " + searchDir)
        }
    }

    private void createOutputs(SmcCommander commander) {
        commander.generateStateMachine().execute()
        if (generateDotFile()) {
            commander.generateDotFile(graphVizLevel).execute()
        }
        if (generateHtmlTable) {
            commander.generateHtmlTable().execute()
        }
    }

    private boolean generateDotFile() {
        return graphVizLevel >= 0
    }
}
