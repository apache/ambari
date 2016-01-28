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
import org.apache.ambari.server.state.ServiceInfo;
import org.apache.ambari.server.state.stack.ServiceMetainfoXml;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBException;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Encapsulates IO operations on a stack definition service directory.
 */
public abstract class ServiceDirectory extends StackDefinitionDirectory {
  /**
   * metrics file
   */
  private Map<String, File> metricsFileMap = new HashMap<String, File>();

  /**
   * alerts file
   */
  private File alertsFile;

  /**
   * theme file
   */
  private File themeFile;

  /**
   * kerberos descriptor file
   */
  private File kerberosDescriptorFile;

  /**
   * widgets descriptor file
   */
  private Map<String, File> widgetsDescriptorFileMap = new HashMap<String, File>();

  /**
   * package directory path
   */
  protected String packageDir;

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
  protected static final String PACKAGE_FOLDER_NAME = "package";

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
   * @param servicePath     path of the service directory
   * @throws AmbariException if unable to parse the service directory
   */
  public ServiceDirectory(String servicePath) throws AmbariException {
    super(servicePath);
    parsePath();

    File af = new File(directory.getAbsolutePath()
        + File.separator + AmbariMetaInfo.SERVICE_ALERT_FILE_NAME);
    alertsFile = af.exists() ? af : null;

    File kdf = new File(directory.getAbsolutePath()
        + File.separator + AmbariMetaInfo.KERBEROS_DESCRIPTOR_FILE_NAME);
    kerberosDescriptorFile = kdf.exists() ? kdf : null;

    if (metaInfoXml.getServices() != null) {
      for (ServiceInfo serviceInfo : metaInfoXml.getServices()) {
        File mf = new File(directory.getAbsolutePath()
                + File.separator + serviceInfo.getMetricsFileName());
        metricsFileMap.put(serviceInfo.getName(), mf.exists() ? mf : null);

        File wdf = new File(directory.getAbsolutePath()
                + File.separator + serviceInfo.getWidgetsFileName());
        widgetsDescriptorFileMap.put(serviceInfo.getName(), wdf.exists() ? wdf : null);
      }
    }

    File themeFile = new File(directory.getAbsolutePath() + File.separator + AmbariMetaInfo.SERVICE_THEME_FILE_NAME);
    this.themeFile = themeFile.exists() ? themeFile : null;
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
  public File getMetricsFile(String serviceName) {
    return metricsFileMap.get(serviceName);
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
   * Obtain theme file
   * @return theme file
   */
  public File getThemeFile() {
    return themeFile;
  }

  /**
   * Obtain the Kerberos Descriptor file.
   *
   * @return Kerberos Descriptor file
   */
  public File getKerberosDescriptorFile() {
    return kerberosDescriptorFile;
  }

  /**
   * Obtain the Widgets Descriptor file.
   *
   * @return Widgets Descriptor file
   */
  public File getWidgetsDescriptorFile(String serviceName) {
    return widgetsDescriptorFileMap.get(serviceName);
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
   */
  protected abstract void parsePath() throws AmbariException;

  /**
   * Unmarshal the metainfo file into its object representation.
   *
   * @throws AmbariException if the metainfo file doesn't exist or
   *                         unable to unmarshal the metainfo file
   */
  protected void parseMetaInfoFile() throws AmbariException {
    File f = new File(getAbsolutePath() + File.separator + SERVICE_METAINFO_FILE_NAME);
    if (! f.exists()) {
      throw new AmbariException(String.format("Stack Definition Service at '%s' doesn't contain a metainfo.xml file",
          f.getAbsolutePath()));
    }

    try {
      metaInfoXml = unmarshaller.unmarshal(ServiceMetainfoXml.class, f);
    } catch (JAXBException e) {
      metaInfoXml = new ServiceMetainfoXml();
      metaInfoXml.setValid(false);
      String msg = String.format("Unable to parse service metainfo.xml file '%s' ", f.getAbsolutePath());
      metaInfoXml.setErrors(msg);
      LOG.warn(msg);
      metaInfoXml.setSchemaVersion(getAbsolutePath().replace(f.getParentFile().getParentFile().getParent()+File.separator, ""));
    }
  }

}
