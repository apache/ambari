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
package org.apache.ambari.shell.commands;

import static org.apache.ambari.shell.support.TableRenderer.renderSingleMap;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.ambari.groovy.client.AmbariClient;
import org.apache.ambari.shell.completion.ConfigType;
import org.apache.ambari.shell.model.AmbariContext;
import org.apache.hadoop.conf.Configuration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.core.CommandMarker;
import org.springframework.shell.core.annotation.CliAvailabilityIndicator;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.core.annotation.CliOption;
import org.springframework.stereotype.Component;

/**
 * Configuration related commands used in the shell.
 *
 * @see org.apache.ambari.groovy.client.AmbariClient
 */
@Component
public class ConfigCommands implements CommandMarker {

  private AmbariClient client;
  private AmbariContext context;

  @Autowired
  public ConfigCommands(AmbariClient client, AmbariContext context) {
    this.client = client;
    this.context = context;
  }

  /**
   * Checks whether the configuration show command is available or not.
   *
   * @return true if available false otherwise
   */
  @CliAvailabilityIndicator("configuration show")
  public boolean isConfigShowCommandAvailable() {
    return context.isConnectedToCluster();
  }

  /**
   * Prints the desired configuration.
   */
  @CliCommand(value = "configuration show", help = "Prints the desired configuration")
  public String showConfig(@CliOption(key = "type", mandatory = true, help = "Type of the configuration") ConfigType configType) {
    String configTypeName = configType.getName();
    Map<String, Map<String, String>> configMap = client.getServiceConfigMap(configTypeName);
    return renderSingleMap(configMap.get(configTypeName), "KEY", "VALUE");
  }

  /**
   * Checks whether the configuration set command is available or not.
   *
   * @return true if available false otherwise
   */
  @CliAvailabilityIndicator("configuration set")
  public boolean isConfigSetCommandAvailable() {
    return context.isConnectedToCluster();
  }

  /**
   * Sets the desired configuration.
   */
  @CliCommand(value = "configuration set", help = "Sets the desired configuration")
  public String setConfig(@CliOption(key = "type", mandatory = true, help = "Type of the configuration") ConfigType configType,
    @CliOption(key = "url", help = "URL of the config") String url,
    @CliOption(key = "file", help = "File of the config") File file) throws IOException {
    Configuration configuration = new Configuration(false);
    if (file == null) {
      configuration.addResource(new URL(url));
    } else {
      configuration.addResource(new FileInputStream(file));
    }
    Map<String, String> config = new HashMap<String, String>();
    Iterator<Map.Entry<String, String>> iterator = configuration.iterator();
    while (iterator.hasNext()) {
      Map.Entry<String, String> entry = iterator.next();
      config.put(entry.getKey(), entry.getValue());
    }
    client.modifyConfiguration(configType.getName(), config);
    return "Restart is required!\n" + renderSingleMap(config, "KEY", "VALUE");
  }

  /**
   * Checks whether the configuration modify command is available or not.
   *
   * @return true if available false otherwise
   */
  @CliAvailabilityIndicator("configuration modify")
  public boolean isConfigModifyCommandAvailable() {
    return context.isConnectedToCluster();
  }

  /**
   * Modify the desired configuration.
   */
  @CliCommand(value = "configuration modify", help = "Modify the desired configuration")
  public String modifyConfig(@CliOption(key = "type", mandatory = true, help = "Type of the configuration") ConfigType configType,
    @CliOption(key = "key", mandatory = true, help = "Key of the config") String key,
    @CliOption(key = "value", mandatory = true, help = "Value of the config") String value) {
    String configTypeName = configType.getName();
    Map<String, String> config = client.getServiceConfigMap(configTypeName).get(configTypeName);
    config.put(key, value);
    client.modifyConfiguration(configTypeName, config);
    return "Restart is required!\n" + renderSingleMap(config, "KEY", "VALUE");
  }

  /**
   * Checks whether the configuration modify command is available or not.
   *
   * @return true if available false otherwise
   */
  @CliAvailabilityIndicator("configuration download")
  public boolean isConfigDownloadCommandAvailable() {
    return context.isConnectedToCluster();
  }

  /**
   * Modify the desired configuration.
   */
  @CliCommand(value = "configuration download", help = "Downloads the desired configuration")
  public String downloadConfig(@CliOption(key = "type", mandatory = true, help = "Type of the configuration") ConfigType configType) throws IOException {
    String configTypeName = configType.getName();
    Map<String, String> config = client.getServiceConfigMap(configTypeName).get(configTypeName);
    Configuration configuration = new Configuration(false);
    for (String key : config.keySet()) {
      configuration.set(key, config.get(key));
    }
    File file = new File(configTypeName);
    FileWriter writer = new FileWriter(file);
    configuration.writeXml(writer);
    return "Configuration saved to: " + file.getAbsolutePath();
  }

}
