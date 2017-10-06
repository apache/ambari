/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.server.resources;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.namespace.QName;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.stack.ConfigurationInfo;
import org.apache.ambari.server.stack.ConfigurationModule;
import org.apache.ambari.server.stack.ModuleFileUnmarshaller;
import org.apache.ambari.server.state.PropertyInfo;
import org.apache.ambari.server.state.stack.ConfigurationXml;
import org.apache.ambari.server.utils.XmlUtils;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

public class ResourceLevelClusterSettingManager {
  /**
   * Used to unmarshal a configuration file to an object representation
   */
  private static ModuleFileUnmarshaller unmarshaller = new ModuleFileUnmarshaller();

  private String clusterSettingsPath;
  private static final String CLUSTER_SETTINGS_FILE_NAME = "cluster-settings.xml";
  private static final String clusterSettingsConfigType = "cluster-settings";
  private static File clusterSettingsFile;
  private final static Logger LOG = LoggerFactory.getLogger(ResourceLevelClusterSettingManager.class);
  private Map<String, Map<String, PropertyInfo>> clusterSettingsMap = new ConcurrentHashMap<>();
  private Map<String, ConfigurationModule> configurationModules = new HashMap<>();

  @AssistedInject
  public ResourceLevelClusterSettingManager(@Assisted("resourcesDirPath") String resourcesDirPath) {
    clusterSettingsPath = resourcesDirPath;
    clusterSettingsFile = new File(clusterSettingsPath + File.separator + CLUSTER_SETTINGS_FILE_NAME);
    populateClusterSettingsXml();
  }

  public Collection<PropertyInfo> getClusterSettingsMap() {
    return configurationModules.get("cluster-settings").getModuleInfo().getProperties();
  }

  /**
   * Obtain a collection of of configuration modules representing each configuration
   * file contained in this configuration directory.
   *
   * @return collection of configuration modules
   */
  public Collection<ConfigurationModule> getConfigurationModules() {
    return configurationModules.values();
  }

  /**
   * Parses 'cluster-settings.xml' during ambari-server boostrap and (re)start
   * Reads from /var/lib/ambari-server/resources
   *
   * @throws java.io.IOException
   */
  private void populateClusterSettingsXml() {
    ConfigurationXml config = null;
    try {
      config = unmarshaller.unmarshal(ConfigurationXml.class, clusterSettingsFile);
      ConfigurationInfo configInfo = new ConfigurationInfo(parseProperties(config, clusterSettingsFile.getName()),
              parseAttributes(config));
      ConfigurationModule module = new ConfigurationModule(clusterSettingsConfigType, configInfo);
      configurationModules.put(clusterSettingsConfigType, module);
    } catch (Exception e) {
      e.printStackTrace();
    }

  }

  /**
   * Parse a configurations type attributes.
   *
   * @param configuration object representation of a configuration file
   * @return collection of attributes for the configuration type
   */
  private Map<String, String> parseAttributes(ConfigurationXml configuration) {
    Map<String, String> attributes = new HashMap<>();
    for (Map.Entry<QName, String> attribute : configuration.getAttributes().entrySet()) {
      attributes.put(attribute.getKey().getLocalPart(), attribute.getValue());
    }
    return attributes;
  }

  /**
   * Parse a configurations properties.
   *
   * @param configuration object representation of a configuration file
   * @param fileName      configuration file name
   * @return collection of properties
   */
  private Collection<PropertyInfo> parseProperties(ConfigurationXml configuration, String fileName)
          throws FileNotFoundException, RuntimeException {
    List<PropertyInfo> props = new ArrayList<>();
    for (PropertyInfo pi : configuration.getProperties()) {
      pi.setFilename(fileName);
      if (pi.getPropertyTypes().contains(PropertyInfo.PropertyType.VALUE_FROM_PROPERTY_FILE)) {
        if (clusterSettingsPath != null || clusterSettingsPath.isEmpty()) {
          String propertyFileType = pi.getPropertyValueAttributes().getPropertyFileType();
          if (clusterSettingsFile.exists() && clusterSettingsFile.isFile()) {
            try {
              String propertyValue = FileUtils.readFileToString(clusterSettingsFile);
              boolean valid = true;
              switch (propertyFileType.toLowerCase()) {
                case "xml":
                  if (!XmlUtils.isValidXml(propertyValue)) {
                    valid = false;
                    LOG.error("Failed to load value from property file. Property file {} is not a valid XML file",
                            clusterSettingsFile);
                  }
                  break;
                case "json": // Not supporting JSON as of now.
                case "text": // fallthrough
                default:
                  throw new AmbariException("'" + propertyFileType + "' type file not supported for '"
                          + clusterSettingsConfigType + "'. File Path : " + clusterSettingsFile.getAbsolutePath());
              }
              if (valid) {
                pi.setValue(propertyValue);
              }
            } catch (IOException e) {
              LOG.error("Failed to load value from property file {}. Error Message {}",
                      clusterSettingsFile.getAbsolutePath(), e.getMessage());
            }
          } else {
            throw new FileNotFoundException("Failed to find '" + CLUSTER_SETTINGS_FILE_NAME + "' file with path : "
                    + clusterSettingsFile);
          }
        } else {
          throw new RuntimeException("Failed to load value from property file. Properties directory {} does not exist"
                  + clusterSettingsFile.getAbsolutePath());
        }
      }
      props.add(pi);
    }
    return props;
  }
}
