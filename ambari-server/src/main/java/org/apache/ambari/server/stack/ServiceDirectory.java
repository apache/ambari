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

package org.apache.ambari.server.stack;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.state.stack.ServiceMetainfoXml;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBException;
import java.io.File;

/**
 * Encapsulates IO operations on a stack definition service directory.
 */
public class ServiceDirectory extends StackDefinitionDirectory {
  /**
   * metrics file
   */
  private File metricsFile;

  /**
   * alerts file
   */
  private File alertsFile;

  /**
   * package directory path
   */
  private String packageDir;

  /**
   * service metainfo file object representation
   */
  private ServiceMetainfoXml metaInfoXml;

  /**
   * services root directory name
   */
  public static final String SERVICES_FOLDER_NAME = "services";

  /**
   * package directory name
   */
  private static final String PACKAGE_FOLDER_NAME = "package";

  /**
   * service metainfo file name
   */
  private static final String SERVICE_METAINFO_FILE_NAME = "metainfo.xml";

  /**
   * stack definition file unmarshaller
   */
  ModuleFileUnmarshaller unmarshaller = new ModuleFileUnmarshaller();

  /**
   * logger instance
   */
  private final static Logger LOG = LoggerFactory.getLogger(ServiceDirectory.class);


  /**
   * Constructor.
   *
   * @param servicePath  path of the service directory
   * @throws AmbariException if unable to parse the service directory
   */
  public ServiceDirectory(String servicePath) throws AmbariException {
    super(servicePath);
    parsePath();

    File mf = new File(directory.getAbsolutePath()
        + File.separator + AmbariMetaInfo.SERVICE_METRIC_FILE_NAME);
    metricsFile = mf.exists() ? mf : null;

    File af = new File(directory.getAbsolutePath()
        + File.separator + AmbariMetaInfo.SERVICE_ALERT_FILE_NAME);
    alertsFile = af.exists() ? af : null;
  }

  /**
   * Obtain the package directory path.
   *
   * @return package directory path
   */
  public String getPackageDir() {
    return packageDir;
  }

  /**
   * Obtain the metrics file.
   *
   * @return metrics file
   */
  public File getMetricsFile() {
    return metricsFile;
  }

  /**
   * Obtain the alerts file.
   *
   * @return alerts file
   */
  public File getAlertsFile() {
    return alertsFile;
  }

  /**
   * Obtain the service metainfo file object representation.
   *
   * @return
   * Obtain the service metainfo file object representation
   */
  public ServiceMetainfoXml getMetaInfoFile() {
    return metaInfoXml;
  }

  /**
   * Parse the service directory.
   *
   * @throws AmbariException if unable to parse the service directory
   */
  private void parsePath() throws AmbariException {

    File serviceDir = new File(getAbsolutePath());
    File stackVersionDir = serviceDir.getParentFile().getParentFile();
    File stackDir = stackVersionDir.getParentFile();

    String stackId = String.format("%s-%s", stackDir.getName(), stackVersionDir.getName());

    File absPackageDir = new File(getAbsolutePath() + File.separator + PACKAGE_FOLDER_NAME);
    if (absPackageDir.isDirectory()) {
      packageDir = absPackageDir.getPath().substring(stackDir.getParentFile().getPath().length() + 1);
      LOG.debug(String.format("Service package folder for service %s for stack %s has been resolved to %s",
          serviceDir.getName(), stackId, packageDir));
    } else {
      //todo: this seems like it should be an error case
      LOG.debug(String.format("Service package folder %s for service %s for stack %s does not exist.",
          absPackageDir, serviceDir.getName(), stackId));
    }
    parseMetaInfoFile();
  }

  /**
   * Unmarshal the metainfo file into its object representation.
   *
   * @throws AmbariException if the metainfo file doesn't exist or
   *                         unable to unmarshal the metainfo file
   */
  private void parseMetaInfoFile() throws AmbariException {
    File f = new File(getAbsolutePath() + File.separator + SERVICE_METAINFO_FILE_NAME);
    if (! f.exists()) {
      throw new AmbariException(String.format("Stack Definition Service at '%s' doesn't contain a metainfo.xml file",
          f.getAbsolutePath()));
    }

    try {
      metaInfoXml = unmarshaller.unmarshal(ServiceMetainfoXml.class, f);
    } catch (JAXBException e) {
      throw new AmbariException(String.format("Unable to parse service metainfo.xml file '%s' ", f.getAbsolutePath()), e);
    }
  }
}
