/*
 * Copyright © 2023-2025 Mariano Barcia
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

package io.github.mbarcia.csv.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.resource.spi.ConfigProperty;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    // For relative paths, resolve against configured external directory
    return new File(externalCsvDir, path);
  }

  /** Diagnostic method to check resource availability */
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
