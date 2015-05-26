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
import org.apache.ambari.server.view.configuration.ViewConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

/**
 * Extractor for view archives.
 */
public class ViewExtractor {

  /**
   * Constants
   */
  private static final String ARCHIVE_CLASSES_DIR = "WEB-INF/classes";
  private static final String ARCHIVE_LIB_DIR     = "WEB-INF/lib";
  private static final int    BUFFER_SIZE         = 1024;

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
          JarInputStream jarInputStream = archiveUtility.getJarFileStream(viewArchive);
          try {
            msg = "Extracting files from " + viewArchive.getName() + ".";
            view.setStatusDetail(msg);
            LOG.info(msg);

            // create the META-INF directory
            File metaInfDir = archiveUtility.getFile(archivePath + File.separator + "META-INF");
            if (!metaInfDir.mkdir()) {
              msg = "Could not create archive META-INF directory.";

              view.setStatusDetail(msg);
              LOG.error(msg);
              throw new ExtractionException(msg);
            }

            JarEntry jarEntry;
            while ((jarEntry = jarInputStream.getNextJarEntry())!= null){
              try {
                String   entryPath = archivePath + File.separator + jarEntry.getName();

                LOG.debug("Extracting " + entryPath);

                File entryFile = archiveUtility.getFile(entryPath);

                if (jarEntry.isDirectory()) {

                  LOG.debug("Making directory " + entryPath);

                  if (!entryFile.mkdir()) {
                    msg = "Could not create archive entry directory " + entryPath + ".";

                    view.setStatusDetail(msg);
                    LOG.error(msg);
                    throw new ExtractionException(msg);
                  }
                } else {

                  FileOutputStream fos = archiveUtility.getFileOutputStream(entryFile);
                  try {
                    LOG.debug("Begin copying from " + jarEntry.getName() + " to "+ entryPath);

                    byte[] buffer = new byte[BUFFER_SIZE];
                    int n;
                    while((n = jarInputStream.read(buffer)) > -1) {
                      fos.write(buffer, 0, n);
                    }
                    LOG.debug("Finish copying from " + jarEntry.getName() + " to "+ entryPath);

                  } finally {
                    fos.flush();
                    fos.close();
                  }
                }
              } finally {
                jarInputStream.closeEntry();
              }
            }
          } finally {
            jarInputStream.close();
          }
        } else {
          msg = "Could not create archive directory " + archivePath + ".";

          view.setStatusDetail(msg);
          LOG.error(msg);
          throw new ExtractionException(msg);
        }
      }

      ViewConfig viewConfig = archiveUtility.getViewConfigFromExtractedArchive(archivePath, false);

      return getArchiveClassLoader(viewConfig, archiveDir);

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
  private ClassLoader getArchiveClassLoader(ViewConfig viewConfig, File archiveDir)
      throws IOException {

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

    return new ViewClassLoader(viewConfig, urlList.toArray(new URL[urlList.size()]));
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
