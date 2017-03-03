package com.fsryan.gradle.smc

import java.util.logging.Level
import java.util.logging.Logger
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

class Unzipper {

    private static Logger logger = Logger.getLogger(Unzipper.class.getSimpleName())

    private File buildDir
    private File statemapJarDestinationDir
    
    Unzipper(File buildDir, File statemapJarDestinationDir) {
        this.buildDir = buildDir
        this.statemapJarDestinationDir = statemapJarDestinationDir
    }

    UnzipSummary execute(boolean extractSmcJar, boolean extractStatemapJar) throws IOException {
        int numFilesToExtract = extractSmcJar && extractStatemapJar ? 2 : extractSmcJar || extractStatemapJar ? 1 : 0
        if (numFilesToExtract == 0) {
            return new UnzipSummary(null, null);
        }

        UnzipSummary ret = new UnzipSummary(null, null)
        InputStream dl = null
        BufferedInputStream bin = null
        ZipInputStream zin = null
        int numFilesExtracted = 0
        try {
            dl = findRealZipUrl(new URL(SmcPlugin.DEFAULT_SMC_URI)).openStream()
            bin = new BufferedInputStream(dl)
            ZipEntry ze = null
            zin = new ZipInputStream(bin)
            while ((ze = zin.getNextEntry()) != null) {
                if (ze.getName().equals(SmcPlugin.SMC_JAR_ZIP_ENTRY_NAME) && extractSmcJar) {
                    ret.smcJarUri = extractFromZip(zin, ze, new File(buildDir.absolutePath + File.separator + "Smc.jar"))
                    numFilesExtracted++
                }
                if (ze.getName().equals(SmcPlugin.STATEMAP_JAR_ZIP_ENTRY_NAME) && extractStatemapJar) {
                    ret.statemapJarUri = extractFromZip(zin, ze, new File(statemapJarDestinationDir.absolutePath + File.separator + "statemap.jar"))
                    numFilesExtracted++
                }
                if (numFilesToExtract == numFilesExtracted) {
                    break
                }
            }
        } catch (IOException ioe) {
            logger.log(Level.ALL, "failed to download/extract zip", ioe)
            throw ioe
        } finally {
            if (dl != null) {
                dl.close()
            }
            if (bin != null) {
                bin.close()
            }
            if (zin != null) {
                zin.close()
            }
        }

        if (numFilesToExtract != numFilesExtracted) {
            throw new IllegalStateException("download and extraction unsuccessful")
        }

        return ret
    }

    private URI extractFromZip(ZipInputStream zin, ZipEntry ze, File outputFile) {
        logger.log(Level.INFO, "Extracting " + ze.getName() + " into " + outputFile)
        if (!outputFile.parentFile.isDirectory() && !outputFile.parentFile.mkdirs()) {
            throw new IllegalStateException("Cannot create directory for output file: " + outputFile)
        }
        OutputStream out = new FileOutputStream(outputFile)
        byte[] buffer = new byte[8192]
        int len
        while ((len = zin.read(buffer)) != -1) {
            out.write(buffer, 0, len)
        }
        out.close()
        return outputFile.toURI()
    }

    private URL findRealZipUrl(URL url) {
        HttpURLConnection conn = url.openConnection()
        conn.followRedirects = false
        conn.requestMethod = 'HEAD'
        if(conn.responseCode in [301,302]) {
            if (conn.headerFields.'Location') {
                String redirect = conn.headerFields.Location.first().replace("https://", "http://")
                logger.log(Level.WARNING, "zip url redirected: "  + redirect)
                return findRealZipUrl(redirect.toURL())
            } else {
                throw new RuntimeException('Failed to follow redirect')
            }
        }
        return url
    }
}

class UnzipSummary {
    URI smcJarUri
    URI statemapJarUri

    UnzipSummary(URI smcJarUri, URI statemapJarUri) {
        this.smcJarUri = smcJarUri
        this.statemapJarUri = statemapJarUri
    }
}
