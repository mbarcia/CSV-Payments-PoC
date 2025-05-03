package com.example.poc.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.resource.spi.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

@ApplicationScoped
public class HybridResourceLoader implements ResourceLoader {

    private static final Logger LOG = LoggerFactory.getLogger(HybridResourceLoader.class);

    // Configurable external directory - defaults to "./csv" relative to working directory
    @ConfigProperty(defaultValue = "./csv")
    String externalCsvDir;

    /**
     * Gets a resource, prioritizing external files over JAR resources.
     *
     * @param path Path to the resource, can be a simple filename or subfolder/filename
     * @return URL to the resource, or null if not found
     */
    public URL getResource(String path) {
        LOG.debug("Looking for resource: {}", path);

        try {
            // 1. First try as external file
            File externalFile = resolveExternalPath(path);
            if (externalFile.exists()) {
                LOG.debug("Found as external file: {}", externalFile);
                return externalFile.toURI().toURL();
            } else {
                LOG.debug("External file not found: {}", externalFile);
            }

            // 2. Then try as classpath resource (for proof of concept/testing)
            String normalizedPath = path.startsWith("/") ? path.substring(1) : path;
            URL classpathResource = Thread.currentThread().getContextClassLoader()
                    .getResource(normalizedPath);

            if (classpathResource != null) {
                LOG.debug("Found as classpath resource: {}", classpathResource);
                return classpathResource;
            } else {
                LOG.debug("Classpath resource not found: {}", normalizedPath);
            }

            LOG.warn("Resource not found in any location: {}", path);
            return null;

        } catch (MalformedURLException e) {
            LOG.error("Error creating URL", e);
            return null;
        }
    }

    /**
     * Lists all resources in a directory, combining external files and JAR resources.
     * External files take precedence if filenames conflict.
     *
     * @param directory Directory to list
     * @return List of resource URLs
     */
    public List<URL> listResources(String directory) {
        LOG.debug("Listing resources in: {}", directory);
        Map<String, URL> resourceMap = new HashMap<>(); // Use map to handle duplicates

        try {
            // 1. First list external files
            File externalDir = resolveExternalPath(directory);
            if (externalDir.exists() && externalDir.isDirectory()) {
                LOG.debug("Listing external files in: {}", externalDir);
                File[] files = externalDir.listFiles();
                if (files != null) {
                    for (File file : files) {
                        if (file.isFile()) {
                            LOG.debug("Found external file: {}", file.getName());
                            resourceMap.put(file.getName(), file.toURI().toURL());
                        }
                    }
                }
            } else {
                LOG.debug("External directory not found: {}", externalDir);
            }

            // 2. Then list classpath resources
            String normalizedDir = directory.startsWith("/") ? directory.substring(1) : directory;
            if (!normalizedDir.endsWith("/")) {
                normalizedDir += "/";
            }

            // Try to list resources in the classpath
            try {
                URL dirUrl = Thread.currentThread().getContextClassLoader().getResource(normalizedDir);
                if (dirUrl != null) {
                    // Handle directory in filesystem
                    if ("file".equals(dirUrl.getProtocol())) {
                        File dir = new File(dirUrl.toURI());
                        File[] files = dir.listFiles();
                        if (files != null) {
                            for (File file : files) {
                                if (file.isFile() && !resourceMap.containsKey(file.getName())) {
                                    LOG.debug("Found classpath file: {}", file.getName());
                                    resourceMap.put(file.getName(), file.toURI().toURL());
                                }
                            }
                        }
                    }
                    // Handle directory in JAR
                    else if ("jar".equals(dirUrl.getProtocol())) {
                        String jarPath = dirUrl.getPath();
                        String jarFilePath = jarPath.substring(5, jarPath.indexOf("!"));
                        String dirPath = jarPath.substring(jarPath.indexOf("!") + 2);

                        try (JarFile jarFile = new JarFile(URLDecoder.decode(jarFilePath, StandardCharsets.UTF_8))) {
                            Enumeration<JarEntry> entries = jarFile.entries();
                            while (entries.hasMoreElements()) {
                                JarEntry entry = entries.nextElement();
                                if (!entry.isDirectory() && entry.getName().startsWith(dirPath)) {
                                    String fileName = entry.getName().substring(dirPath.length());
                                    if (!fileName.contains("/") && !resourceMap.containsKey(fileName)) {
                                        URL url = Thread.currentThread().getContextClassLoader()
                                                .getResource(entry.getName());
                                        if (url != null) {
                                            LOG.debug("Found JAR resource: {}", fileName);
                                            resourceMap.put(fileName, url);
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    LOG.debug("Directory URL not found in classpath: {}", normalizedDir);
                }
            } catch (Exception e) {
                LOG.warn("Error listing classpath resources", e);
            }

            // Convert map to list
            List<URL> result = new ArrayList<>(resourceMap.values());
            LOG.debug("Total resources found: {}", result.size());
            return result;

        } catch (Exception e) {
            LOG.error("Error listing resources", e);
            return Collections.emptyList();
        }
    }

    /**
     * Resolves a path against the configured external directory.
     * Handles both absolute and relative paths appropriately.
     */
    private File resolveExternalPath(String path) {
        // If path is absolute, use it directly
        File file = new File(path);
        if (file.isAbsolute()) {
            return file;
        }

        // For relative paths, resolve against configured external directory
        return new File(externalCsvDir, path);
    }

    /**
     * Diagnostic method to check resource availability
     */
    public void diagnoseResourceAccess(String path) {
        LOG.info("==== RESOURCE ACCESS DIAGNOSIS ====");
        LOG.info("Looking for resource: {}", path);
        LOG.info("Working directory: {}", new File(".").getAbsolutePath());
        LOG.info("Configured external CSV dir: {}", externalCsvDir);

        // Check external file
        File externalFile = resolveExternalPath(path);
        LOG.info("External file path: {}", externalFile.getAbsolutePath());
        LOG.info("External file exists: {}", externalFile.exists());

        // Check classpath resource
        String normalizedPath = path.startsWith("/") ? path.substring(1) : path;
        URL classpathResource = Thread.currentThread().getContextClassLoader().getResource(normalizedPath);
        LOG.info("Classpath resource path: {}", normalizedPath);
        LOG.info("Classpath resource exists: {}", classpathResource != null);
        if (classpathResource != null) {
            LOG.info("Classpath resource URL: {}", classpathResource);
        }

        LOG.info("==== END DIAGNOSIS ====");
    }
}