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
import org.apache.ambari.server.controller.KerberosHelper;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.ConfigHelper;
import org.apache.ambari.server.state.PropertyInfo;
import org.apache.ambari.server.state.SecurityState;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.kerberos.KerberosDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Matcher;

/**
 * PrepareEnableKerberosServerAction is a ServerAction implementation that prepares metadata needed
 * to enable Kerberos on the cluster.
 */
public class PrepareDisableKerberosServerAction extends AbstractPrepareKerberosServerAction {
  private final static Logger LOG = LoggerFactory.getLogger(PrepareDisableKerberosServerAction.class);

  /**
   * KerberosHelper
   */
  @Inject
  private KerberosHelper kerberosHelper;

  @Inject
  private ConfigHelper configHelper;

  @Inject
  private KerberosConfigDataFileWriterFactory kerberosConfigDataFileWriterFactory;


  /**
   * Called to execute this action.  Upon invocation, calls
   * {@link KerberosServerAction#processIdentities(Map)}
   * to iterate through the Kerberos identity metadata and call
   * {@link PrepareDisableKerberosServerAction#processIdentities(Map)}
   * for each identity to process.
   *
   * @param requestSharedDataContext a Map to be used a shared data among all ServerActions related
   *                                 to a given request
   * @return a CommandReport indicating the result of this action
   * @throws AmbariException
   * @throws InterruptedException
   */
  @Override
  public CommandReport execute(ConcurrentMap<String, Object> requestSharedDataContext) throws
      AmbariException, InterruptedException {

    Cluster cluster = getCluster();

    if (cluster == null) {
      throw new AmbariException("Missing cluster object");
    }

    KerberosDescriptor kerberosDescriptor = kerberosHelper.getKerberosDescriptor(cluster);
    Collection<String> identityFilter = getIdentityFilter();
    List<ServiceComponentHost> schToProcess = kerberosHelper.getServiceComponentHostsToProcess(cluster,
        kerberosDescriptor,
        getServiceComponentFilter(),
        null, identityFilter,
        new KerberosHelper.Command<Boolean, ServiceComponentHost>() {
          @Override
          public Boolean invoke(ServiceComponentHost sch) throws AmbariException {
            return (sch.getDesiredSecurityState() == SecurityState.UNSECURED) &&  (sch.getSecurityState() != SecurityState.UNSECURED);
          }
        });

    Map<String, Map<String, String>> kerberosConfigurations = new HashMap<String, Map<String, String>>();
    Map<String, String> commandParameters = getCommandParameters();
    String dataDirectory = getCommandParameterValue(commandParameters, DATA_DIRECTORY);

    int schCount = schToProcess.size();
    if (schCount == 0) {
      actionLog.writeStdOut("There are no components to process");
    } else if (schCount == 1) {
      actionLog.writeStdOut(String.format("Processing %d component", schCount));
    } else {
      actionLog.writeStdOut(String.format("Processing %d components", schCount));
    }

    processServiceComponentHosts(cluster, kerberosDescriptor, schToProcess, identityFilter, dataDirectory, kerberosConfigurations, false);

    // Add auth-to-local configurations to the set of changes
    Set<String> authToLocalProperties = kerberosDescriptor.getAllAuthToLocalProperties();
    if(authToLocalProperties != null) {
      for (String authToLocalProperty : authToLocalProperties) {
        Matcher m = KerberosDescriptor.AUTH_TO_LOCAL_PROPERTY_SPECIFICATION_PATTERN.matcher(authToLocalProperty);

        if (m.matches()) {
          String configType = m.group(1);
          String propertyName = m.group(2);

          if (configType == null) {
            configType = "";
          }

          // Add existing auth_to_local configuration, if set
          Map<String, String> configuration = kerberosConfigurations.get(configType);
          if (configuration != null) {
            configuration.put(propertyName, "DEFAULT");
          }
        }
      }
    }

    actionLog.writeStdOut("Determining configuration changes");
    // Ensure the cluster-env/security_enabled flag is set properly
    Map<String, String> clusterEnvProperties = kerberosConfigurations.get(KerberosHelper.SECURITY_ENABLED_CONFIG_TYPE);
    if (clusterEnvProperties == null) {
      clusterEnvProperties = new HashMap<String, String>();
      kerberosConfigurations.put(KerberosHelper.SECURITY_ENABLED_CONFIG_TYPE, clusterEnvProperties);
    }
    clusterEnvProperties.put(KerberosHelper.SECURITY_ENABLED_PROPERTY_NAME, "false");

    // If there are configurations to set, create a (temporary) data file to store the configuration
    // updates and fill it will the relevant configurations.
    if (!kerberosConfigurations.isEmpty()) {
      if(dataDirectory == null) {
        String message = "The data directory has not been set.  Generated data can not be stored.";
        LOG.error(message);
        throw new AmbariException(message);
      }

      Map<String, Collection<String>> configurationsToRemove = new HashMap<String, Collection<String>>();
      File configFile = new File(dataDirectory, KerberosConfigDataFileWriter.DATA_FILE_NAME);
      KerberosConfigDataFileWriter kerberosConfDataFileWriter = null;

      // Fill the configurationsToRemove map with all Kerberos-related configurations.  Values
      // needed to be kept will have new values from the stack definition and thus pruned from
      // this map.
      for (Map.Entry<String, Map<String, String>> entry : kerberosConfigurations.entrySet()) {
        configurationsToRemove.put(entry.getKey(), new HashSet<String>(entry.getValue().keySet()));
      }

      // Remove cluster-env from the set of configurations to remove since it has no default set
      // or properties and the logic below will remove all from this set - which is not desirable.
      configurationsToRemove.remove("cluster-env");

      if (!schToProcess.isEmpty()) {
        Set<String> visitedServices = new HashSet<String>();

        for (ServiceComponentHost sch : schToProcess) {
          String serviceName = sch.getServiceName();

          if (!visitedServices.contains(serviceName)) {
            StackId stackVersion = sch.getStackVersion();

            visitedServices.add(serviceName);

            if (stackVersion != null) {
              Set<PropertyInfo> serviceProperties = configHelper.getServiceProperties(stackVersion, serviceName, true);

              if (serviceProperties != null) {
                for (PropertyInfo propertyInfo : serviceProperties) {
                  String filename = propertyInfo.getFilename();

                  if (filename != null) {
                    String type = ConfigHelper.fileNameToConfigType(filename);
                    String propertyName = propertyInfo.getName();

                    Map<String, String> kerberosConfiguration = kerberosConfigurations.get(type);
                    if ((kerberosConfiguration != null) && (kerberosConfiguration.containsKey(propertyName))) {
                      kerberosConfiguration.put(propertyName, propertyInfo.getValue());
                    }

                    // Remove the relevant from the set of properties (for the given type) to remove
                    Collection<String> propertiesToRemove = configurationsToRemove.get(type);
                    if (propertiesToRemove != null) {
                      propertiesToRemove.remove(propertyName);
                    }
                  }
                }
              }
            }
          }
        }
      }

      actionLog.writeStdOut(String.format("Writing configuration changes metadata file to %s", configFile.getAbsolutePath()));
      try {
        kerberosConfDataFileWriter = kerberosConfigDataFileWriterFactory.createKerberosConfigDataFileWriter(configFile);

        for (Map.Entry<String, Map<String, String>> entry : kerberosConfigurations.entrySet()) {
          String type = entry.getKey();
          Map<String, String> properties = entry.getValue();
          Collection<String> propertiesToRemove = configurationsToRemove.get(type);

          if (properties != null) {
            for (Map.Entry<String, String> configTypeEntry : properties.entrySet()) {
              String propertyName = configTypeEntry.getKey();

              // Ignore properties that should be removed
              if ((propertiesToRemove == null) || !propertiesToRemove.contains(propertyName)) {
                String value = configTypeEntry.getValue();
                String operation = (value == null)
                    ? KerberosConfigDataFileWriter.OPERATION_TYPE_REMOVE
                    : KerberosConfigDataFileWriter.OPERATION_TYPE_SET;

                kerberosConfDataFileWriter.addRecord(type, propertyName, value, operation);
              }
            }
          }
        }

        // Declare which properties to remove from the configurations
        for (Map.Entry<String, Collection<String>> entry : configurationsToRemove.entrySet()) {
          String type = entry.getKey();
          Collection<String> properties = entry.getValue();

          if (properties != null) {
            for (String propertyName : properties) {
              kerberosConfDataFileWriter.addRecord(type, propertyName, null, KerberosConfigDataFileWriter.OPERATION_TYPE_REMOVE);
            }
          }
        }
      } catch (IOException e) {
        String message = String.format("Failed to write kerberos configurations file - %s", configFile.getAbsolutePath());
        LOG.error(message, e);
        actionLog.writeStdOut(message);
        actionLog.writeStdErr(message + "\n" + e.getLocalizedMessage());
        throw new AmbariException(message, e);
      } finally {
        if (kerberosConfDataFileWriter != null) {
          try {
            kerberosConfDataFileWriter.close();
          } catch (IOException e) {
            String message = "Failed to close the kerberos configurations file writer";
            LOG.warn(message, e);
            actionLog.writeStdOut(message);
            actionLog.writeStdErr(message + "\n" + e.getLocalizedMessage());
          }
        }
      }
    }

    return createCommandReport(0, HostRoleStatus.COMPLETED, "{}", actionLog.getStdOut(), actionLog.getStdErr());
  }
}

