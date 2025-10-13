/*
 * Copyright (c) 2023-2025 Mariano Barcia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.pipelineframework.csv.util;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class HybridResourceLoader implements ResourceLoader {

  private static final Logger LOG = LoggerFactory.getLogger(HybridResourceLoader.class);

  // Configurable external directory - defaults to \"./csv\" relative to working directory
  @ConfigProperty(defaultValue = "./csv")
  String externalCsvDir;

  // Resolved external directory
  private String resolvedExternalCsvDir;

  @PostConstruct
  void init() {
    // Resolve the external CSV directory relative to the project root
    File projectRoot = new File(".").getAbsoluteFile();
    while (projectRoot != null && !new File(projectRoot, "pom.xml").exists()) {
      projectRoot = projectRoot.getParentFile();
    }
    
    if (projectRoot != null) {
      // Look for the csv directory in the project root
      File csvDir = new File(projectRoot, "csv");
      if (csvDir.exists() && csvDir.isDirectory()) {
        resolvedExternalCsvDir = csvDir.getPath();
        LOG.debug("Found CSV directory: {}", resolvedExternalCsvDir);
      } else {
        // If csv directory doesn't exist, create the resolved path anyway
        resolvedExternalCsvDir = csvDir.getPath();
        LOG.debug("CSV directory not found, using path: {}", resolvedExternalCsvDir);
      }
    } else {
      // Fallback to the configured value
      resolvedExternalCsvDir = externalCsvDir;
      LOG.warn("Could not find project root, using configured CSV dir: {}", resolvedExternalCsvDir);
    }
  }

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
      URL classpathResource =
          Thread.currentThread().getContextClassLoader().getResource(normalizedPath);

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
   * Resolves a path against the configured external directory. Handles both absolute and relative
   * paths appropriately.
   */
  private File resolveExternalPath(String path) {
    // If path is absolute, use it directly
    File file = new File(path);
    if (file.isAbsolute()) {
      return file;
    }

    // Special case: if the path is exactly the same as the externalCsvDir, 
    // return the resolvedExternalCsvDir directly
    if (path.equals(externalCsvDir)) {
      return new File(resolvedExternalCsvDir);
    }

    // For relative paths, resolve against configured external directory
    return new File(resolvedExternalCsvDir, path);
  }

  /** Diagnostic method to check resource availability */
  public void diagnoseResourceAccess(String path) {
    LOG.info("==== RESOURCE ACCESS DIAGNOSIS ====");
    LOG.info("Looking for resource: {}", path);
    LOG.info("Working directory: {}", new File(".").getAbsolutePath());
    LOG.info("Configured external CSV dir: {}", externalCsvDir);
    LOG.info("Resolved external CSV dir: {}", resolvedExternalCsvDir);

    // Check external file
    File externalFile = resolveExternalPath(path);
    LOG.info("External file path: {}", externalFile.getAbsolutePath());
    LOG.info("External file exists: {}", externalFile.exists());

    // Check classpath resource
    String normalizedPath = path.startsWith("/") ? path.substring(1) : path;
    URL classpathResource =
        Thread.currentThread().getContextClassLoader().getResource(normalizedPath);
    LOG.info("Classpath resource path: {}", normalizedPath);
    LOG.info("Classpath resource exists: {}", classpathResource != null);
    if (classpathResource != null) {
      LOG.info("Classpath resource URL: {}", classpathResource);
    }

    LOG.info("==== END DIAGNOSIS ====");
  }
}
