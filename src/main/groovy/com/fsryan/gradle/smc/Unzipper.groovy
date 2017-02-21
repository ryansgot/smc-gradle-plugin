package com.fsryan.gradle.smc

import java.util.logging.Level
import java.util.logging.Logger
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

class Unzipper {

    private File buildDir
    private File statemapJarDestinationDir

    Unzipper(File buildDir, File statemapJarDestinationDir) {
        this.buildDir = buildDir
        this.statemapJarDestinationDir = statemapJarDestinationDir
    }

    void execute(boolean extractSmcJar, boolean extractStatemapJar) {
        Logger logger = Logger.getLogger(getClass())
        int numFilesToExtract = extractSmcJar && extractStatemapJar ? 2 : extractSmcJar || extractStatemapJar ? 1 : 0
        if (numFilesToExtract == 0) {
            logger.log(Level.INFO, "Not executing Zip download; already have necessary dependencies")
            return
        }

        logger.log(Level.INFO, "Downloading and unzipping")
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
            if (ze.getName().equals(SmcPlugin.STATEMAP_JAR_ZIP_ENTRY_NAME) && extractStatemapJar) {
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
