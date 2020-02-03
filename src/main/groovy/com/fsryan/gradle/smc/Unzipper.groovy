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
        if (!extractSmcJar && !extractStatemapJar) {
            return new UnzipSummary(null, null);
        }

        boolean extractedSmcJar = false
        boolean extractedStatemapJar = false

        Exception exceptionEncountered = null

        UnzipSummary ret = new UnzipSummary(null, null)
        InputStream dl = null
        BufferedInputStream bin = null
        ZipInputStream zin = null
        try {
            dl = findRealZipUrl(new URL(SmcPlugin.DEFAULT_SMC_URI)).openStream()
            bin = new BufferedInputStream(dl)
            ZipEntry ze = null
            zin = new ZipInputStream(bin)
            while ((ze = zin.getNextEntry()) != null) {
                logger.log(Level.INFO, "Inspecting file in downloaded zip: ${ze.name}")
                if (ze.getName().equals(SmcPlugin.SMC_JAR_ZIP_ENTRY_NAME) && extractSmcJar) {
                    logger.log(Level.INFO, "Found Smc.jar file in downloaded zip")
                    ret.smcJarUri = extractFromZip(zin, ze, new File(buildDir.absolutePath + File.separator + "Smc.jar"))
                    extractedSmcJar = true
                }
                if (ze.getName().equals(SmcPlugin.STATEMAP_JAR_ZIP_ENTRY_NAME) && extractStatemapJar) {
                    logger.log(Level.INFO, "Found statemap.jar file in downloaded zip")
                    ret.statemapJarUri = extractFromZip(zin, ze, new File(statemapJarDestinationDir.absolutePath + File.separator + "statemap.jar"))
                    extractedStatemapJar = true
                }
                if (extractSmcJar == extractedSmcJar && extractStatemapJar == extractedStatemapJar) {
                    break
                }
            }
        } catch (Exception e) {
            exceptionEncountered = e
            logger.log(Level.ALL, "failed to download/extract zip", e)
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

        if (extractSmcJar != extractedSmcJar) {
            if (exceptionEncountered != null) {
                logger.log(Level.ALL, "failed to extract Smc.jar file", exceptionEncountered)
            } else {
                logger.log(Level.ALL, "failed to extract Smc.jar file with no exception encountered")
            }
        }

        if (extractedStatemapJar != extractedStatemapJar) {
            if (exceptionEncountered != null) {
                logger.log(Level.ALL, "failed to extract statemap.jar file", exceptionEncountered)
            } else {
                logger.log(Level.ALL, "failed to extract statemap.jar file with no exception encountered")
            }
        }

        if (extractSmcJar != extractedSmcJar || extractedStatemapJar != extractedStatemapJar) {
            if (exceptionEncountered != null) {
                logger.log(Level.ALL, "download and extraction unsuccessful", exceptionEncountered)
            } else {
                throw new IllegalStateException("download and extraction unsuccessful")
            }
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
