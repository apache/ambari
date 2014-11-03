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

import org.apache.ambari.server.view.configuration.ViewConfig;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.jar.JarInputStream;

/**
 * Helper class for basic view archive utility.
 */
public class ViewArchiveUtility {

  /**
   * Constants
   */
  private static final String VIEW_XML = "view.xml";


  // ----- ViewArchiveUtility ------------------------------------------------

  /**
   * Get the view configuration from the given archive file.
   *
   * @param archiveFile  the archive file
   *
   * @return the associated view configuration
   */
  public ViewConfig getViewConfigFromArchive(File archiveFile)
      throws MalformedURLException, JAXBException {
    ClassLoader cl = URLClassLoader.newInstance(new URL[]{archiveFile.toURI().toURL()});

    InputStream configStream      = cl.getResourceAsStream(VIEW_XML);
    JAXBContext jaxbContext       = JAXBContext.newInstance(ViewConfig.class);
    Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();

    return (ViewConfig) jaxbUnmarshaller.unmarshal(configStream);
  }

  /**
   * Get the view configuration from the extracted archive file.
   *
   * @param archivePath path to extracted archive
   *
   * @return the associated view configuration
   *
   * @throws JAXBException if xml is malformed
   * @throws java.io.FileNotFoundException if xml was not found
   */
  public ViewConfig getViewConfigFromExtractedArchive(String archivePath)
      throws JAXBException, FileNotFoundException {

    InputStream configStream      = new FileInputStream(new File(archivePath + File.separator + VIEW_XML));
    JAXBContext  jaxbContext      = JAXBContext.newInstance(ViewConfig.class);
    Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();

    return (ViewConfig) jaxbUnmarshaller.unmarshal(configStream);
  }

  /**
   * Get a new file instance for the given path.
   *
   * @param path  the path
   *
   * @return a new file instance
   */
  public File getFile(String path) {
    return new File(path);
  }

  /**
   * Get a new file output stream for the given file.
   *
   * @param file  the file
   *
   * @return a new file output stream
   */
  public FileOutputStream getFileOutputStream(File file) throws FileNotFoundException {
    return new FileOutputStream(file);
  }

  /**
   * Get a new jar file stream from the given file.
   *
   * @param file  the file
   *
   * @return a new jar file stream
   */
  public JarInputStream getJarFileStream(File file) throws IOException {
    return new JarInputStream(new FileInputStream(file));
  }
}
