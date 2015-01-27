/*
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

package org.apache.ambari.server.serveraction.kerberos;

import com.google.common.collect.Maps;
import com.google.inject.Inject;
import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.actionmanager.HostRoleStatus;
import org.apache.ambari.server.agent.CommandReport;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.ConfigurationRequest;
import org.apache.ambari.server.serveraction.AbstractServerAction;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

/**
 * UpdateKerberosConfigServerAction is implementation of ServerAction that updates service configs
 * while enabling Kerberos
 */
public class UpdateKerberosConfigsServerAction extends AbstractServerAction {

  private final static Logger LOG =
    LoggerFactory.getLogger(UpdateKerberosConfigsServerAction.class);

  private HashMap<String, Map<String, String>> configurations = new HashMap<String, Map<String, String>>();

  @Inject
  private AmbariManagementController controller;

  /**
   * Executes this ServerAction
   * <p/>
   * This is typically called by the ServerActionExecutor in it's own thread, but there is no
   * guarantee that this is the case.  It is expected that the ExecutionCommand and HostRoleCommand
   * properties are set before calling this method.
   *
   * @param requestSharedDataContext a Map to be used a shared data among all ServerActions related
   *                                 to a given request
   * @return a CommandReport declaring the status of the task
   * @throws org.apache.ambari.server.AmbariException
   *
   * @throws InterruptedException
   */
  @Override
  public CommandReport execute(ConcurrentMap<String, Object> requestSharedDataContext)
    throws AmbariException, InterruptedException {

    CommandReport commandReport = null;

    String clusterName = getExecutionCommand().getClusterName();
    Clusters clusters = controller.getClusters();
    Cluster cluster = clusters.getCluster(clusterName);

    String dataDirectoryPath = getCommandParameterValue(getCommandParameters(), KerberosServerAction.DATA_DIRECTORY);

    // If the data directory path is set, attempt to process further, else assume there is no work to do
    if (dataDirectoryPath != null) {
      File dataDirectory = new File(dataDirectoryPath);

      // If the data directory exists, attempt to process further, else assume there is no work to do
      if (dataDirectory.exists()) {
        KerberosActionDataFileReader indexReader = null;
        KerberosConfigDataFileReader configReader = null;

        try {
          // If the action data file exists, iterate over the records to find the identity-specific
          // configuration settings to update
          File indexFile = new File(dataDirectory, KerberosActionDataFile.DATA_FILE_NAME);
          if (indexFile.exists()) {
            indexReader = new KerberosActionDataFileReader(indexFile);

            for (Map<String, String> record : indexReader) {
              String principal = record.get(KerberosActionDataFile.PRINCIPAL);
              String principalConfig = record.get(KerberosActionDataFile.PRINCIPAL_CONFIGURATION);
              String[] principalTokens = principalConfig.split("/");
              if (principalTokens.length == 2) {
                String principalConfigType = principalTokens[0];
                String principalConfigProp = principalTokens[1];
                addConfigTypePropVal(principalConfigType, principalConfigProp, principal);
              }

              String keytabPath = record.get(KerberosActionDataFile.KEYTAB_FILE_PATH);
              String keytabConfig = record.get(KerberosActionDataFile.KEYTAB_FILE_CONFIGURATION);
              String[] keytabTokens = keytabConfig.split("/");
              if (keytabTokens.length == 2) {
                String keytabConfigType = keytabTokens[0];
                String keytabConfigProp = keytabTokens[1];
                addConfigTypePropVal(keytabConfigType, keytabConfigProp, keytabPath);
              }
            }
          }

          // If the config data file exists, iterate over the records to find the (explicit)
          // configuration settings to update
          File configFile = new File(dataDirectory, KerberosConfigDataFile.DATA_FILE_NAME);
          if (configFile.exists()) {
            configReader = new KerberosConfigDataFileReader(configFile);
            for (Map<String, String> record : configReader) {
              String configType = record.get(KerberosConfigDataFile.CONFIGURATION_TYPE);
              String configKey = record.get(KerberosConfigDataFile.KEY);
              String configVal = record.get(KerberosConfigDataFile.VALUE);
              addConfigTypePropVal(configType, configKey, configVal);
            }
          }

          for (Map.Entry<String, Map<String, String>> entry : configurations.entrySet()) {
            updateConfigurationPropertiesForCluster(
                cluster,
                entry.getKey(),     // configType
                entry.getValue(),   // properties
                true,               // updateIfExists
                true,               // createNew
                "update services configs to enable kerberos");
          }
        } catch (IOException e) {
          String message = "Could not update services configs to enable kerberos";
          actionLog.writeStdErr(message);
          LOG.error(message, e);
          commandReport = createCommandReport(1, HostRoleStatus.FAILED, "{}", actionLog.getStdOut(),
              actionLog.getStdErr());
        } finally {
          if (indexReader != null && !indexReader.isClosed()) {
            try {
              indexReader.close();
            } catch (Throwable t) {
              // ignored
            }
          }
          if (configReader != null && !configReader.isClosed()) {
            try {
              configReader.close();
            } catch (Throwable t) {
              // ignored
            }
          }
        }
      }
    }

    return (commandReport == null)
        ? createCommandReport(0, HostRoleStatus.COMPLETED, "{}", actionLog.getStdOut(), actionLog.getStdErr())
        : commandReport;
  }


  /**
   *
   * Updates service config properties of a cluster
   * @param cluster   the cluster for which to update service configs
   * @param configType   service config type to be updated
   * @param properties    map of service config properties
   * @param updateIfExists    flag indicating whether to update if a property already exists
   * @param createNewConfigType  flag indicating whether to create new service config
   *                             if the config type does not exist
   * @param note   a short note on change
   * @throws AmbariException if the operation fails
   */
  private void updateConfigurationPropertiesForCluster(
    Cluster cluster,
    String configType,
    Map<String, String> properties,
    boolean updateIfExists,
    boolean createNewConfigType,
    String note)
    throws AmbariException {

    String newTag = "version" + System.currentTimeMillis();
    String message;
    if ((properties != null) && (properties.size() > 0)) {
      Map<String, Config> all = cluster.getConfigsByType(configType);
      if (all == null || !all.containsKey(newTag)) {
        Map<String, String> oldConfigProperties;
        Config oldConfig = cluster.getDesiredConfigByType(configType);

        if (oldConfig == null && !createNewConfigType) {
          message = String.format("Config %s not found. Assuming service not installed. " +
              "Skipping configuration properties update", configType);
          actionLog.writeStdOut(message);
          LOG.info(message);
          return;
        } else if (oldConfig == null) {
          oldConfigProperties = new HashMap<String, String>();
          newTag = "version1";
        } else {
          oldConfigProperties = oldConfig.getProperties();
        }

        Map<String, String> mergedProperties =
          mergeProperties(oldConfigProperties, properties, updateIfExists);

        if (!Maps.difference(oldConfigProperties, mergedProperties).areEqual()) {
          message = String.format("Applying configuration with tag '%s' to " +
              "cluster '%s'", newTag, cluster.getClusterName());
          actionLog.writeStdOut(message);
          LOG.info(message);

          ConfigurationRequest cr = new ConfigurationRequest();
          cr.setClusterName(cluster.getClusterName());
          cr.setVersionTag(newTag);
          cr.setType(configType);
          cr.setProperties(mergedProperties);
          cr.setServiceConfigVersionNote(note);
          controller.createConfiguration(cr);

          Config baseConfig = cluster.getConfig(cr.getType(), cr.getVersionTag());
          if (baseConfig != null) {
            String authName = "kerberization";

            if (cluster.addDesiredConfig(authName, Collections.singleton(baseConfig)) != null) {
              String oldConfigString = (oldConfig != null) ? " from='" + oldConfig.getTag() + "'" : "";
              message = "cluster '" + cluster.getClusterName() + "' "
                  + "changed by: '" + authName + "'; "
                  + "type='" + baseConfig.getType() + "' "
                  + "tag='" + baseConfig.getTag() + "'"
                  + oldConfigString;
              LOG.info(message);
              actionLog.writeStdOut(message);
            }
          }
        } else {
          message = "No changes detected to config " + configType + ". Skipping configuration properties update";
          LOG.info(message);
          actionLog.writeStdOut(message);
        }
      }
    }
  }

  /**
   * Merges current properties and new properties
   * @param originalProperties  current properties
   * @param newProperties      new properties
   * @param updateIfExists    flag indicating whether to update if a property already exists
   * @return    merged properties
   */
  private static Map<String, String> mergeProperties(Map<String, String> originalProperties,
                                                     Map<String, String> newProperties,
                                                     boolean updateIfExists) {

    Map<String, String> properties = new HashMap<String, String>(originalProperties);
    for (Map.Entry<String, String> entry : newProperties.entrySet()) {
      if (!properties.containsKey(entry.getKey()) || updateIfExists) {
        properties.put(entry.getKey(), entry.getValue());
      }
    }
    return properties;
  }

  /**
   * Gets a property from the given commandParameters
   * @param commandParameters   map of command parameters
   * @param propertyName   property name to find value for
   * @return    value of given proeprty name, would return <code>null</code>
   * if the provided commandParameters is null or  if the requested property is not found
   * in commandParams
   */
  private static String getCommandParameterValue(Map<String, String> commandParameters, String propertyName) {
    return ((commandParameters == null) || (propertyName == null)) ? null : commandParameters.get(propertyName);
  }

  /**
   * Adds a property to properties of a given service config type
   * @param configtype   service config type
   * @param prop     property to be added
   * @param val   value for the proeprty
   */
  private void addConfigTypePropVal(String configtype, String prop, String val) {
    Map<String, String> configtypePropsVal = configurations.get(configtype);
    if (configtypePropsVal == null) {
      configtypePropsVal = new HashMap<String, String>();
      configurations.put(configtype, configtypePropsVal);
    }
    configtypePropsVal.put(prop, val);
  }

}
