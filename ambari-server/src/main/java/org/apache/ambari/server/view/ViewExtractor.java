/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.view;

import org.apache.ambari.server.orm.entities.ViewEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Extractor for view archives.
 */
public class ViewExtractor {

  /**
   * Constants
   */
  private static final String ARCHIVE_CLASSES_DIR = "WEB-INF/classes";
  private static final String ARCHIVE_LIB_DIR     = "WEB-INF/lib";

  @Inject
  ViewArchiveUtility archiveUtility;

  /**
   * The logger.
   */
  protected final static Logger LOG = LoggerFactory.getLogger(ViewExtractor.class);


  // ----- ViewExtractor -----------------------------------------------------

  /**
   * Extract the given view archive to the given archive directory.
   *
   * @param view         the view entity
   * @param viewArchive  the view archive file
   * @param archiveDir   the view archive directory
   *
   * @return the class loader for the archive classes
   *
   * @throws ExtractionException if the archive can not be extracted
   */
  public ClassLoader extractViewArchive(ViewEntity view, File viewArchive, File archiveDir)
      throws ExtractionException {

    String archivePath = archiveDir.getAbsolutePath();

    try {
      // Skip if the archive has already been extracted
      if (!archiveDir.exists()) {

        String msg = "Creating archive folder " + archivePath + ".";

        view.setStatusDetail(msg);
        LOG.info(msg);

        if (archiveDir.mkdir()) {
          JarFile viewJarFile = archiveUtility.getJarFile(viewArchive);
          Enumeration enumeration = viewJarFile.entries();

          msg = "Extracting files from " + viewArchive.getName() + ".";

          view.setStatusDetail(msg);
          LOG.info(msg);

          while (enumeration.hasMoreElements()) {
            JarEntry jarEntry  = (JarEntry) enumeration.nextElement();
            String   entryPath = archivePath + File.separator + jarEntry.getName();

            File entryFile = archiveUtility.getFile(entryPath);

            if (jarEntry.isDirectory()) {
              if (!entryFile.mkdir()) {
                msg = "Could not create archive entry directory " + entryPath + ".";

                view.setStatusDetail(msg);
                LOG.error(msg);
                throw new ExtractionException(msg);
              }
            } else {
              InputStream is = viewJarFile.getInputStream(jarEntry);
              try {
                FileOutputStream fos = archiveUtility.getFileOutputStream(entryFile);
                try {
                  while (is.available() > 0) {
                    fos.write(is.read());
                  }
                } finally {
                  fos.close();
                }
              } finally {
                is.close();
              }
            }
          }
        } else {
          msg = "Could not create archive directory " + archivePath + ".";

          view.setStatusDetail(msg);
          LOG.error(msg);
          throw new ExtractionException(msg);
        }
      }
      return getArchiveClassLoader(archiveDir);

    } catch (Exception e) {
      String msg = "Caught exception trying to extract the view archive " + archivePath + ".";

      view.setStatusDetail(msg);
      LOG.error(msg, e);
      throw new ExtractionException(msg, e);
    }
  }

  /**
   * Ensure that the extracted view archive directory exists.
   *
   * @param extractedArchivesPath  the path
   *
   * @return false if the directory does not exist and can not be created
   */
  public boolean ensureExtractedArchiveDirectory(String extractedArchivesPath) {

    File extractedArchiveDir = archiveUtility.getFile(extractedArchivesPath);

    return extractedArchiveDir.exists() || extractedArchiveDir.mkdir();
  }


  // ----- archiveUtility methods ----------------------------------------------------

  // get a class loader for the given archive directory
  private ClassLoader getArchiveClassLoader(File archiveDir)
      throws MalformedURLException {

    String    archivePath = archiveDir.getAbsolutePath();
    List<URL> urlList     = new LinkedList<URL>();

    // include the classes directory
    String classesPath = archivePath + File.separator + ARCHIVE_CLASSES_DIR;
    File   classesDir  = archiveUtility.getFile(classesPath);
    if (classesDir.exists()) {
      urlList.add(classesDir.toURI().toURL());
    }

    // include any libraries in the lib directory
    String libPath = archivePath + File.separator + ARCHIVE_LIB_DIR;
    File   libDir  = archiveUtility.getFile(libPath);
    if (libDir.exists()) {
      File[] files = libDir.listFiles();
      if (files != null) {
        for (final File fileEntry : files) {
          if (!fileEntry.isDirectory()) {
            urlList.add(fileEntry.toURI().toURL());
          }
        }
      }
    }

    // include the archive directory
    urlList.add(archiveDir.toURI().toURL());

    return URLClassLoader.newInstance(urlList.toArray(new URL[urlList.size()]));
  }


  // ----- inner class : ExtractionException ---------------------------------

  /**
   * General exception for view archive extraction.
   */
  public static class ExtractionException extends Exception {

    // ----- Constructors ----------------------------------------------------

    /**
     * Construct an extraction exception.
     *
     * @param msg  the exception message
     */
    public ExtractionException(String msg) {
      super(msg);
    }

    /**
     * Construct an extraction exception.
     *
     * @param msg        the exception message
     * @param throwable  the root cause
     */
    public ExtractionException(String msg, Throwable throwable) {
      super(msg, throwable);
    }
  }
}
