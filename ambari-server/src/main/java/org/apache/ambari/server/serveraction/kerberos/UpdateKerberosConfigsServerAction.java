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

import com.google.inject.Inject;
import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.actionmanager.HostRoleStatus;
import org.apache.ambari.server.agent.CommandReport;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.serveraction.AbstractServerAction;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.ConfigHelper;
import org.apache.ambari.server.state.SecurityType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

/**
 * UpdateKerberosConfigServerAction is implementation of ServerAction that updates service configs
 * while enabling Kerberos
 */
public class UpdateKerberosConfigsServerAction extends AbstractServerAction {

  private final static Logger LOG = LoggerFactory.getLogger(UpdateKerberosConfigsServerAction.class);

  @Inject
  private AmbariManagementController controller;

  @Inject
  private ConfigHelper configHelper;

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
   * @throws InterruptedException
   */
  @Override
  public CommandReport execute(ConcurrentMap<String, Object> requestSharedDataContext)
      throws AmbariException, InterruptedException {

    CommandReport commandReport = null;

    String clusterName = getExecutionCommand().getClusterName();
    Clusters clusters = controller.getClusters();
    Cluster cluster = clusters.getCluster(clusterName);

    String authenticatedUserName = getCommandParameterValue(getCommandParameters(), KerberosServerAction.AUTHENTICATED_USER_NAME);
    String dataDirectoryPath = getCommandParameterValue(getCommandParameters(), KerberosServerAction.DATA_DIRECTORY);
    HashMap<String, Map<String, String>> propertiesToSet = new HashMap<String, Map<String, String>>();
    HashMap<String, Collection<String>> propertiesToRemove = new HashMap<String, Collection<String>>();

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
                addConfigTypePropVal(propertiesToSet, principalConfigType, principalConfigProp, principal);
              }

              String keytabPath = record.get(KerberosActionDataFile.KEYTAB_FILE_PATH);
              String keytabConfig = record.get(KerberosActionDataFile.KEYTAB_FILE_CONFIGURATION);
              String[] keytabTokens = keytabConfig.split("/");
              if (keytabTokens.length == 2) {
                String keytabConfigType = keytabTokens[0];
                String keytabConfigProp = keytabTokens[1];
                addConfigTypePropVal(propertiesToSet, keytabConfigType, keytabConfigProp, keytabPath);
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
              String configOp = record.get(KerberosConfigDataFile.OPERATION);

              if (KerberosConfigDataFile.OPERATION_TYPE_REMOVE.equals(configOp)) {
                removeConfigTypeProp(propertiesToRemove, configType, configKey);
              } else {
                addConfigTypePropVal(propertiesToSet, configType, configKey, configVal);
              }
            }
          }

          if (!propertiesToSet.isEmpty()) {
            String configNote = cluster.getSecurityType() == SecurityType.KERBEROS
                ? "Enabling Kerberos"
                : "Disabling Kerberos";

            for (Map.Entry<String, Map<String, String>> entry : propertiesToSet.entrySet()) {
              String type = entry.getKey();

              configHelper.updateConfigType(cluster, controller, type, entry.getValue(), propertiesToRemove.get(type),
                  authenticatedUserName, configNote);
            }
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
   * Gets a property from the given commandParameters
   *
   * @param commandParameters map of command parameters
   * @param propertyName      property name to find value for
   * @return value of given proeprty name, would return <code>null</code>
   * if the provided commandParameters is null or  if the requested property is not found
   * in commandParams
   */
  private static String getCommandParameterValue(Map<String, String> commandParameters, String propertyName) {
    return ((commandParameters == null) || (propertyName == null)) ? null : commandParameters.get(propertyName);
  }

  /**
   * Adds a property to properties of a given service config type
   *
   * @param configurations a map of configurations
   * @param configType     service config type
   * @param prop           property to be added
   * @param val            value for the property
   */
  private void addConfigTypePropVal(HashMap<String, Map<String, String>> configurations, String configType, String prop, String val) {
    Map<String, String> configTypePropsVal = configurations.get(configType);
    if (configTypePropsVal == null) {
      configTypePropsVal = new HashMap<String, String>();
      configurations.put(configType, configTypePropsVal);
    }
    configTypePropsVal.put(prop, val);
    actionLog.writeStdOut(String.format("Setting property %s/%s: %s", configType, prop, (val == null) ? "<null>" : val));
  }

  /**
   * Removes a property from the set of properties of a given service config type
   *
   * @param configurations a map of configurations
   * @param configType     service config type
   * @param prop           property to be removed
   */
  private void removeConfigTypeProp(HashMap<String, Collection<String>> configurations, String configType, String prop) {
    Collection<String> configTypeProps = configurations.get(configType);
    if (configTypeProps == null) {
      configTypeProps = new HashSet<String>();
      configurations.put(configType, configTypeProps);
    }
    configTypeProps.add(prop);
    actionLog.writeStdOut(String.format("Removing property %s/%s", configType, prop));
  }
}
