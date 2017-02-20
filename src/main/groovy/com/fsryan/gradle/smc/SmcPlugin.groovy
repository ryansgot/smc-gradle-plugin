package com.fsryan.gradle.smc

import org.gradle.api.*
import org.gradle.api.plugins.*
import org.gradle.internal.impldep.com.esotericsoftware.minlog.Log

import java.util.zip.*

class SmcPlugin implements Plugin<Project> {

    private static final String DEFAULT_SMC_DIR = "smc_6_6_0"
    private static final String SMC_JAR_ZIP_ENTRY_NAME = DEFAULT_SMC_DIR + File.separator + 'bin' + File.separator + 'Smc.jar'
    private static final String STATEMAP_JAR_ZIP_ENTRY_NAME = DEFAULT_SMC_DIR + File.separator + 'lib' + File.separator + 'Java' + File.separator + 'statemap.jar'
    private static final String DEFAULT_SMC_ZIP = "smc_6_6_0.zip"
    private static final String DEFAULT_SMC_URI = "http://pilotfiber.dl.sourceforge.net/project/smc/smc/6_6_0/" + DEFAULT_SMC_ZIP

    void apply(Project project) {

        project.extensions.create("smc", SmcExtension)

        // TODO: nail down the actual URI

        project.task('getSmc') << {
            String libsDirectory = project.smc.libsDirectory
            URI smcJarUri = new SmcUriChecker(project.buildDir, project.smc.smcUri).prepareSmcJarURI()
            boolean shouldExtractSmcJar = true
            if (smcJarUri != null) {
                shouldExtractSmcJar = false
                project.smc.smcUri = smcJarUri.toString()
            }
            boolean extractStatemapJar = project.smc.putStatemapJarOnClasspath && !new File(libsDirectory + File.separator + 'statemap.jar').exists()
            if (shouldExtractSmcJar || extractStatemapJar) {
                new Unzipper(project.buildDir, new File(libsDirectory)).unzipZip(shouldExtractSmcJar, extractStatemapJar)
            }
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

class SmcExtension {
    /**
     * <p>
     *     The default behavior:
     *     <ol>
     *         <li>
     *             downloads a zip of the latest smc
     *         </li>
     *         <li>
     *             unzips it
     *         </li>
     *         <li>
     *             runs the Smc.jar
     *         </li>
     *         <li>
     *             deletes the zip and the unzipped directory
     *          </li>
     *      </ol>
     *      But this is mainly for continuous integration builds, in order to make no additional
     *      assumptions about the setup of the machine on which it's running
     * </p>
     * <p>
     *     In order have more efficient local builds, you should:
     *     <ol>
     *         <li>
     *             download the zip
     *         </li>
     *         <li>
     *             unzip it yourself
     *         </li>
     *         <li>
     *             create a local property with the file path of the Smc.jar file
     *         </li>
     *         <li>
     *             Assign the value of that local property to this smcUri
     *         </li>
     *     </ol>
     * </p>
     */
    String smcUri
    /**
     * <p>
     *     Defaults to true. When true, the zip hosted on sourceforge is downloaded, the statemap.jar
     *     file will be extracted and then put into your libs directory. When false, the statemap.jar
     *     will not be extracted.
     * </p>
     * <p>
     *     If you have a different version of the statemap.jar in {@link #libsDirectory}, then this
     *     value will be ignored
     * </p>
     */
    boolean putStatemapJarOnClasspath = true
    /**
     * <p>
     *     Defaults to "libs". If the directory named does not exist, and {@link #putStatemapJarOnClasspath}
     *     is true, then the directory will be created. However, if you have not added the .jar files in
     *     this directory to the classpath via dependencies, then the statemap.jar will not be added to the
     *     classpath.
     * </p>
     */
    String libsDirectory = "libs"

    /**
     * <p>
     *     Defaults to "sm". This directory is relative to each of your source sets. For example, the default
     *     source set 'main' would have main->java and main->sm. Equivalently, you can have test->sm or
     *     debug->sm or release->sm.
     * </p>
     * <p>
     *     You should not configure sm as a property of any sourceSet. The plugin just assumes that, as source,
     *     your state machine's DSL files could be treated just like any other source in your gradle build.
     * </p>
     * <p>
     *     Your state machine's DSL file should be in a directory in keeping with the java package structure. So
     *     if you want to have MyStateMachine.java generated in the com.my.state.machine package of the main java
     *     source set, then you need to your state machine DSL, by default, must be in this file:
     *     src/main/sm/com/my/state/machine/MyStateMachine.sm
     * </p>
     */
    String smSrcDir = "sm"

    /**
     * <p>
     *     Defaults to false. If you want to generate the .dot (graphviz) file in your ${buildDir}/outputs/${smSrcDir}
     *     directory so that you can generate graphs using the graphviz application, then flip this to true.
     * </p>
     * <p>
     *     Installation of graphviz is system-dependent. Therefore, no attempt is made to transform this .dot file
     *     into a .png or .pdf . . . yet.
     * </p>
     */
    boolean generateDotFile = false

    /**
     * <p>
     *     Defaults to false. If you want to generate the .html ile in your ${buildDir}/outputs/${smSrcDir}
     *     directory so that you can view a table of all the states, actions, transitions, and guards in your browser,
     *     then flip this to true
     * </p>
     */
    boolean generateHtmlTable = false
}

class SmcUriChecker {

    private File buildDir
    private URI smcUri

    SmcUriChecker(File buildDir, String smcUriStr) {
        this.buildDir = buildDir
        smcUri = smcUriStr == null ? null : new URI(smcUriStr)
    }

    URI prepareSmcJarURI() {
        if (smcUri == null) {
            return null
        }

        if ((smcUri.scheme.equals("http") || smcUri.scheme.equals("https")) && downloadToBuildDir()) {
            return new URI("file://" + downloadDestinationPath())
        } else if (smcUri.scheme.equals("file")) {
            return new File(smcUri.path).exists() ? smcUri : null
        }

        // TODO: warn user that the configured smcUri is not of correct type or something
        return null
    }

    private boolean downloadToBuildDir() {
        // TODO: warn downloading
        try {
            def smcJarFile = new File(downloadDestinationPath()).newOutputStream()
            smcJarFile << smcUri.toURL().openStream()
            smcJarFile.close()
        } catch (Exception e) {
            // TODO: error that download failed
            return false
        }
        return true
    }

    private String downloadDestinationPath() {
        return buildDir.absolutePath + File.separator + "Smc.jar"
    }
}

class Unzipper {

    private File buildDir
    private File statemapJarDestinationDir

    Unzipper(File buildDir, File statemapJarDestinationDir) {
        this.buildDir = buildDir
        this.statemapJarDestinationDir = statemapJarDestinationDir
    }

    void unzipZip(boolean extractSmcJar, boolean extractStatemapJar) {
        int numFilesToExtract = extractSmcJar && extractStatemapJar ? 2 : extractSmcJar || extractStatemapJar ? 1 : 0
        if (numFilesToExtract == 0) {
            println "Nothing to do"
            return
        }

        println "Downloading and unzipping"
        InputStream dl = new URL(SmcPlugin.DEFAULT_SMC_URI).openStream()
        BufferedInputStream bin = new BufferedInputStream(dl)
        ZipInputStream zin = new ZipInputStream(bin)
        ZipEntry ze = null
        int numFilesExtracted = 0
        while ((ze = zin.getNextEntry()) != null) {
            if (ze.getName().equals(SmcPlugin.SMC_JAR_ZIP_ENTRY_NAME) && extractSmcJar) {
                extractFromZip(zin, ze, buildDir.absolutePath + File.separator + "Smc.jar")
                numFilesExtracted++
            }
            if (ze.getName().equals(SmcPlugin.STATEMAP_JAR_ZIP_ENTRY_NAME)) {
                extractFromZip(zin, ze, statemapJarDestinationDir.absolutePath + File.separator + "statemap.jar")
                numFilesExtracted++
            }
            if (numFilesToExtract == numFilesExtracted) {
                break
            }
        }
        dl.close()
        bin.close()
        zin.close()
    }

    private void extractFromZip(ZipInputStream zin, ZipEntry ze, String destination) {
        println "Extracting " + ze.getName() + " into " + destination
        if (!ensureDirectoryExistsForFile(destination)) {
            println "Could not make directory for file: " + destination
        }
        OutputStream out = new FileOutputStream(destination)
        byte[] buffer = new byte[8192]
        int len
        while ((len = zin.read(buffer)) != -1) {
            out.write(buffer, 0, len)
        }
        out.close()
    }

    private boolean ensureDirectoryExistsForFile(String filename) {
        StringBuilder builtDirectory = new StringBuilder()
        String[] splitFilename = filename.split(File.separator)
        for (int i = 0; i < splitFilename.length - 1; i++) {
            if (i != 0) {
                builtDirectory.append(File.separator)
            }
            builtDirectory.append(splitFilename[i])
        }
        File outputDir = new File(builtDirectory.toString())
        return outputDir.isDirectory() || outputDir.mkdirs()
    }
}

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

    String generateDotFile() {
        buildArtifactsDir.mkdirs()
        return "java -jar " + smcJarFile + " -graph -d " + buildArtifactsDir.absolutePath + " " + inputFile
    }

    String generateHtmlTable() {
        buildArtifactsDir.mkdirs()
        return "java -jar " + smcJarFile + " -table -d " + buildArtifactsDir.absolutePath + " " + inputFile
    }
}