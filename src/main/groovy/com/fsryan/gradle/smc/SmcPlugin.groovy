package com.fsryan.gradle.smc

import org.gradle.api.*
import org.gradle.api.plugins.*
import java.util.zip.*

class SmcPlugin implements Plugin<Project> {

    private static final String DEFAULT_SMC_DIR = "smc_6_6_0"
    private static final String SMC_JAR_ZIP_ENTRY_NAME = DEFAULT_SMC_DIR + File.separator + 'bin' + File.separator + 'Smc.jar'
    private static final String STATEMAP_JAR_ZIP_ENTRY_NAME = DEFAULT_SMC_DIR + File.separator + 'lib' + File.separator + 'Java' + File.separator + 'statemap.jar'
    private static final String DEFAULT_SMC_ZIP = "smc_6_6_0.zip"
    private static final String DEFAULT_SMC_URI = "http://pilotfiber.dl.sourceforge.net/project/smc/smc/6_6_0/" + DEFAULT_SMC_ZIP

    void apply(Project project) {

        project.extensions.create("smc", SmcExtension)

        project.task('smcGenerate') << {
            if (!isCompatible(project)) {
                throw new IllegalStateException("Your project must be applying the java, com.android.application, or com.android.library plugins")
            }
            println project.smc.smcUri

            boolean smcUriUndefined = project.smc.smcUri == null
            boolean smcJarFileDoesNotExist = !smcUriUndefined && !file(project.smc.smcUri).exists
            String libsDirectory = project.smc.libsDirectory
            boolean extractStatemapJar = project.smc.putStatemapJarOnClasspath && !new File(libsDirectory + File.separator + 'statemap.jar').exists()
            if (smcUriUndefined || smcJarFileDoesNotExist || extractStatemapJar) {
                String buildDir = project.buildDir.absolutePath
                unzipZip(smcUriUndefined || smcJarFileDoesNotExist, buildDir, extractStatemapJar, libsDirectory)
            } else {
                println "executing state machine generation using Smc.jar: " + project.smc.smcUri
            }
        }
    }

    private static boolean isCompatible(Project project) {
        PluginContainer pc = project.plugins
        return pc.hasPlugin('java') || pc.hasPlugin('com.android.application') || pc.hasPlugin('com.android.library')
    }

    private static void unzipZip(boolean extractSmcJar, String buildDir, boolean extractStatemapJar, String statemapJarDestinationDir) {
        int numFilesToExtract = extractSmcJar && extractStatemapJar ? 2 : extractSmcJar || extractStatemapJar ? 1 : 0
        if (numFilesToExtract == 0) {
            return
        }

        println "Downloading and unzipping"
        InputStream dl = new URL(DEFAULT_SMC_URI).openStream()
        BufferedInputStream bin = new BufferedInputStream(dl)
        ZipInputStream zin = new ZipInputStream(bin)
        ZipEntry ze = null
        int numFilesExtracted = 0
        while ((ze = zin.getNextEntry()) != null) {
            if (ze.getName().equals(SMC_JAR_ZIP_ENTRY_NAME) && extractSmcJar) {
                extractFromZip(zin, ze, buildDir + File.separator + "Smc.jar")
                numFilesExtracted++
            }
            if (ze.getName().equals(STATEMAP_JAR_ZIP_ENTRY_NAME)) {
                extractFromZip(zin, ze, statemapJarDestinationDir + File.separator + "statemap.jar")
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

    private static void extractFromZip(ZipInputStream zin, ZipEntry ze, String destination) {
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

    private static boolean ensureDirectoryExistsForFile(String filename) {
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
     *      assumptions about the machine on which this plugin is running.
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

    String smSource = "sm"
}