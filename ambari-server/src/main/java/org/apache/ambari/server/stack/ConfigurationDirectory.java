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

import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.state.ConfigHelper;
import org.apache.ambari.server.state.PropertyInfo;
import org.apache.ambari.server.state.stack.ConfigurationXml;
import org.apache.ambari.server.utils.JsonUtils;
import org.apache.ambari.server.utils.XmlUtils;
import org.apache.commons.io.FileUtils;
import org.eclipse.jetty.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.namespace.QName;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.bind.JAXBException;
import javax.xml.bind.UnmarshalException;
import org.xml.sax.SAXParseException;

/**
 * Encapsulates IO operations on a stack definition configuration directory.
 */
public class ConfigurationDirectory extends StackDefinitionDirectory {
  /**
   * Used to unmarshal a stack definition configuration to an object representation
   */
  private static ModuleFileUnmarshaller unmarshaller = new ModuleFileUnmarshaller();

  /**
   * Map of configuration type to configuration module.
   * One entry for each configuration file in this configuration directory.
   */
  private Map<String, ConfigurationModule> configurationModules = new HashMap<String, ConfigurationModule>();

  /**
   * Logger instance
   */
  private final static Logger LOG = LoggerFactory.getLogger(ConfigurationDirectory.class);

  /**
   * Properties directory
   */
  protected File propertiesDirFile;

  /**
   * Constructor.
   *
   * @param directoryName  configuration directory name
   */
  public ConfigurationDirectory(String directoryName, String propertiesDirectoryName) {
    super(directoryName);
    if(!StringUtil.isBlank(propertiesDirectoryName)) {
      propertiesDirFile = new File(propertiesDirectoryName);
    }
    parsePath();
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
   * Parse the configuration directory.
   */
  private void parsePath() {
    File[] configFiles = directory.listFiles(AmbariMetaInfo.FILENAME_FILTER);
    if (configFiles != null) {
      for (File configFile : configFiles) {
        if (configFile.getName().endsWith(AmbariMetaInfo.SERVICE_CONFIG_FILE_NAME_POSTFIX)) {
          String configType = ConfigHelper.fileNameToConfigType(configFile.getName());
          ConfigurationXml config = null;
          try {
            config = unmarshaller.unmarshal(ConfigurationXml.class, configFile);
            ConfigurationInfo configInfo = new ConfigurationInfo(parseProperties(config,
                configFile.getName()), parseAttributes(config));
            ConfigurationModule module = new ConfigurationModule(configType, configInfo);
            configurationModules.put(configType, module);
          } catch (Exception e) {
            String error = null;
            if (e instanceof JAXBException || e instanceof UnmarshalException || e instanceof SAXParseException) {
              error = "Could not parse XML " + configFile + ": " + e;
            } else {
              error = "Could not load configuration for " + configFile;
            }
            config = new ConfigurationXml();
            config.setValid(false);
            config.addError(error);
            ConfigurationInfo configInfo = new ConfigurationInfo(parseProperties(config,
                configFile.getName()), parseAttributes(config));
            configInfo.setValid(false);
            configInfo.addError(error);
            ConfigurationModule module = new ConfigurationModule(configType, configInfo);
            configurationModules.put(configType, module);
          }
        }
      }
    }
  }

  /**
   * Parse a configurations properties.
   *
   * @param configuration  object representation of a configuration file
   * @param fileName       configuration file name
   *
   * @return  collection of properties
   */
  private Collection<PropertyInfo> parseProperties(ConfigurationXml configuration, String fileName) {
  List<PropertyInfo> props = new ArrayList<PropertyInfo>();
  for (PropertyInfo pi : configuration.getProperties()) {
    pi.setFilename(fileName);
    if(pi.getPropertyTypes().contains(PropertyInfo.PropertyType.VALUE_FROM_PROPERTY_FILE)) {
      if(propertiesDirFile != null && propertiesDirFile.exists() && propertiesDirFile.isDirectory() ) {
        String propertyFileName = pi.getPropertyValueAttributes().getPropertyFileName();
        String propertyFileType = pi.getPropertyValueAttributes().getPropertyFileType();
        String propertyFilePath = propertiesDirFile.getAbsolutePath() + File.separator + propertyFileName;
        File propertyFile = new File(propertyFilePath);
        if (propertyFile.exists() && propertyFile.isFile()) {
          try {
            String propertyValue = FileUtils.readFileToString(propertyFile);
            switch (propertyFileType.toLowerCase()) {
              case "xml" :
                if (!XmlUtils.isValidXml(propertyValue)) {
                  LOG.error("Failed to load value from property file. Property file {} is not a valid XML file", propertyFilePath);
                  break;
                }
                pi.setValue(propertyValue);
                break;
              case "json":
                if(!JsonUtils.isValidJson(propertyValue)) {
                  LOG.error("Failed to load value from property file. Property file {} is not a valid JSON file", propertyFilePath);
                  break;
                }
              case "text":
              default:
                pi.setValue(propertyValue);
                break;
            }
          } catch (IOException e) {
            LOG.error("Failed to load value from property file {}. Error Message {}", propertyFilePath, e.getMessage());
          }
        } else {
          LOG.error("Failed to load value from property file. Properties file {} does not exist", propertyFilePath);
        }
      } else {
        LOG.error("Failed to load value from property file. Properties directory {} does not exist", propertiesDirFile.getAbsolutePath());
      }
    }
    props.add(pi);
  }
  return props; }

  /**
   * Parse a configurations type attributes.
   *
   * @param configuration  object representation of a configuration file
   *
   * @return  collection of attributes for the configuration type
   */
  private Map<String, String> parseAttributes(ConfigurationXml configuration) {
    Map<String, String> attributes = new HashMap<String, String>();
    for (Map.Entry<QName, String> attribute : configuration.getAttributes().entrySet()) {
      attributes.put(attribute.getKey().getLocalPart(), attribute.getValue());
    }
    return attributes;
  }
}
