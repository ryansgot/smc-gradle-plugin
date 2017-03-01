package com.fsryan.gradle.smc

import java.nio.file.Files
import java.util.logging.Level
import java.util.logging.Logger

abstract class UriChecker {

    File outputDir
    String outputFilename
    URI uri

    Logger logger

    protected UriChecker(File outputDir, String outputFilename, URI uri) {
        this.outputDir = outputDir
        this.outputFilename = outputFilename
        this.uri = uri
        logger = Logger.getLogger(getClass().getSimpleName())
    }

    static UriChecker get(File outputDir, String outputFilename, String uriStr) {
        URI uri = null
        try {
            uri = new URI(uriStr)
        } catch (Exception e) {
            Logger.getLogger(UriChecker.class.getSimpleName()).log(Level.WARNING, "invalid uri: " + uriStr + "; will download zip")
            return new UnsuccessfulUriChecker()
        }

        switch (uri.scheme) {
            case "http":
                // intentionally falling through
            case "https":
                return new HttpDownloadUriChecker(outputDir, outputFilename, uri)
            case "file":
                return new FileUriChecker(outputDir, outputFilename, uri)
        }

        Logger.getLogger(UriChecker.class.getSimpleName()).log(Level.WARNING, "Unsupported uri scheme: " + uri.scheme)
        return new UnsuccessfulUriChecker()
    }

    abstract URI prepareURI(boolean move)
    abstract boolean ok()
}

class HttpDownloadUriChecker extends UriChecker {

    private boolean ok

    private HttpDownloadUriChecker(File outputDir, String outputFilename, URI smcUri) {
        super(outputDir, outputFilename, smcUri)
        ok = smcUri != null && downloadToBuildDir()
    }

    @Override
    URI prepareURI(boolean move) {
        return ok ? new URI("file://" + downloadDestinationPath()) : null
    }

    @Override
    boolean ok() {
        return ok
    }

    private boolean downloadToBuildDir() {
        logger.log(Level.INFO, "Downloading " + uri + " to " + downloadDestinationPath())
        try {
            def smcJarFile = new File(downloadDestinationPath()).newOutputStream()
            smcJarFile << uri.toURL().openStream()
            smcJarFile.close()
            return true
        } catch (Exception e) {
            logger.log(Level.WARNING, "Download of " + uri + " failed", e)
        }
        return false
    }

    private String downloadDestinationPath() {
        return outputDir.absolutePath + File.separator + outputFilename
    }
}

class FileUriChecker extends UriChecker {

    private boolean ok

    private FileUriChecker(File outputDir, String outputFilename, URI uri) {
        super(outputDir, outputFilename, uri)
        ok = uri != null && new File(uri.path).exists()
        if (!ok) {
            logger.log(Level.WARNING, uri == null ? "smcUri was null" : "file " + uri.path + " does not exist")
        }
    }

    @Override
    URI prepareURI(boolean move) {
        return ok ? move ? moveResult() : uri : null
    }

    @Override
    boolean ok() {
        return ok
    }

    private URI moveResult() {
        File source = new File(uri.path)
        File destination = new File(outputDir.absolutePath + File.separator + outputFilename)

        if (source.equals(destination)) {
            return uri
        }

        try {
            Files.move(source.toPath(), destination.toPath())
            return destination.toURI()
        } catch (Exception e) {
            logger.log(Level.WARNING, "Could not move " + source + " to " + destination)
        }
        return null
    }
}

class UnsuccessfulUriChecker extends UriChecker {

    private UnsuccessfulUriChecker() {
        super(null, null, null)
    }

    @Override
    URI prepareURI(boolean move) {
        return null
    }

    @Override
    boolean ok() {
        return false
    }
}