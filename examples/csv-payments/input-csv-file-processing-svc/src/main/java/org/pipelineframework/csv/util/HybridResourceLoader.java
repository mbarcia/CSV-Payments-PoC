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
import org.jboss.logging.Logger;

@ApplicationScoped
public class HybridResourceLoader implements ResourceLoader {

  private static final Logger LOG = Logger.getLogger(HybridResourceLoader.class);

  // Configurable external directory - defaults to "./" (current directory) relative to working directory
  @ConfigProperty(defaultValue = "./")
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
        LOG.debugf("Found CSV directory: %s", resolvedExternalCsvDir);
      } else {
        // If csv directory doesn't exist, create the resolved path anyway
        resolvedExternalCsvDir = csvDir.getPath();
        LOG.debugf("CSV directory not found, using path: %s", resolvedExternalCsvDir);
      }
    } else {
      // Fallback to the configured value
      resolvedExternalCsvDir = externalCsvDir;
      LOG.warnf("Could not find project root, using configured CSV dir: %s", resolvedExternalCsvDir);
    }
  }

  /**
   * Gets a resource, prioritizing external files over JAR resources.
   *
   * @param path Path to the resource, can be a simple filename or subfolder/filename
   * @return URL to the resource, or null if not found
   */
  public URL getResource(String path) {
    LOG.debugf("Looking for resource: %s", path);

    try {
      // 1. First try as external file
      File externalFile = resolveExternalPath(path);
      if (externalFile.exists()) {
        LOG.debugf("Found as external file: %s", externalFile);
        return externalFile.toURI().toURL();
      } else {
        LOG.debugf("External file not found: %s", externalFile);
      }

      // 2. Then try as classpath resource (for proof of concept/testing)
      String normalizedPath = path.startsWith("/") ? path.substring(1) : path;
      URL classpathResource =
          Thread.currentThread().getContextClassLoader().getResource(normalizedPath);

      if (classpathResource != null) {
        LOG.debugf("Found as classpath resource: %s", classpathResource);
        return classpathResource;
      } else {
        LOG.debugf("Classpath resource not found: %s", normalizedPath);
      }

      LOG.warnf("Resource not found in any location: %s", path);
      return null;

    } catch (MalformedURLException e) {
      LOG.errorf("Error creating URL", e);
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

    // Special case: if the path is exactly the same as the resolved external directory's name,
    // return the resolvedExternalCsvDir directly
    File resolvedDir = new File(resolvedExternalCsvDir);
    if (path.equals(resolvedDir.getName())) {
      return resolvedDir;
    }

    // For relative paths, resolve against resolved external directory
    return new File(resolvedExternalCsvDir, path);
  }

  /** Diagnostic method to check resource availability */
  public void diagnoseResourceAccess(String path) {
    LOG.infof("==== RESOURCE ACCESS DIAGNOSIS ====");
    LOG.infof("Looking for resource: %s", path);
    LOG.infof("Working directory: %s", new File(".").getAbsolutePath());
    LOG.infof("Configured external CSV dir: %s", externalCsvDir);
    LOG.infof("Resolved external CSV dir: %s", resolvedExternalCsvDir);

    // Check external file
    File externalFile = resolveExternalPath(path);
    LOG.infof("External file path: %s", externalFile.getAbsolutePath());
    LOG.infof("External file exists: %s", externalFile.exists());

    // Check classpath resource
    String normalizedPath = path.startsWith("/") ? path.substring(1) : path;
    URL classpathResource =
        Thread.currentThread().getContextClassLoader().getResource(normalizedPath);
    LOG.infof("Classpath resource path: %s", normalizedPath);
    LOG.infof("Classpath resource exists: %s", classpathResource != null);
    if (classpathResource != null) {
      LOG.infof("Classpath resource URL: %s", classpathResource);
    }

    LOG.infof("==== END DIAGNOSIS ====");
  }
}
