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

import com.google.common.reflect.TypeToken;
import com.google.inject.Inject;
import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.agent.CommandReport;
import org.apache.ambari.server.controller.KerberosHelper;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.state.kerberos.KerberosComponentDescriptor;
import org.apache.ambari.server.state.kerberos.KerberosDescriptor;
import org.apache.ambari.server.state.kerberos.KerberosIdentityDescriptor;
import org.apache.ambari.server.state.kerberos.KerberosServiceDescriptor;
import org.apache.ambari.server.utils.StageUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class AbstractPrepareKerberosServerAction extends KerberosServerAction {
  private final static Logger LOG = LoggerFactory.getLogger(AbstractPrepareKerberosServerAction.class);

  /**
   * KerberosHelper
   */
  @Inject
  private KerberosHelper kerberosHelper;

  @Inject
  private KerberosIdentityDataFileWriterFactory kerberosIdentityDataFileWriterFactory;

  @Override
  protected CommandReport processIdentity(Map<String, String> identityRecord, String evaluatedPrincipal, KerberosOperationHandler operationHandler, Map<String, String> kerberosConfiguration, Map<String, Object> requestSharedDataContext) throws AmbariException {
    throw new UnsupportedOperationException();
  }

  protected void processServiceComponentHosts(Cluster cluster, KerberosDescriptor kerberosDescriptor, List<ServiceComponentHost> schToProcess,
                                              Collection<String> identityFilter, String dataDirectory,
                                              Map<String, Map<String, String>> kerberosConfigurations) throws AmbariException {

    actionLog.writeStdOut("Processing Kerberos identities and configurations");

    if (!schToProcess.isEmpty()) {
      if(dataDirectory == null) {
        String message = "The data directory has not been set.  Generated data can not be stored.";
        LOG.error(message);
        throw new AmbariException(message);
      }

      // Create the file used to store details about principals and keytabs to create
      File identityDataFile = new File(dataDirectory, KerberosIdentityDataFileWriter.DATA_FILE_NAME);

      // Group ServiceComponentHosts with their relevant hosts so we can create the relevant host-based
      // configurations once per host, rather than for every ServiceComponentHost we encounter
      Map<String, List<ServiceComponentHost>> hostServiceComponentHosts = new HashMap<String, List<ServiceComponentHost>>();
      for (ServiceComponentHost sch : schToProcess) {
        String hostName = sch.getHostName();
        List<ServiceComponentHost> serviceComponentHosts = hostServiceComponentHosts.get(hostName);

        if (serviceComponentHosts == null) {
          serviceComponentHosts = new ArrayList<ServiceComponentHost>();
          hostServiceComponentHosts.put(hostName, serviceComponentHosts);
        }

        serviceComponentHosts.add(sch);
      }

      Map<String, String> kerberosDescriptorProperties = kerberosDescriptor.getProperties();
      KerberosIdentityDataFileWriter kerberosIdentityDataFileWriter = null;

      try {
        for (Map.Entry<String, List<ServiceComponentHost>> entry : hostServiceComponentHosts.entrySet()) {
          String hostName = entry.getKey();
          List<ServiceComponentHost> serviceComponentHosts = entry.getValue();

          // Calculate the current host-specific configurations. These will be used to replace
          // variables within the Kerberos descriptor data
          Map<String, Map<String, String>> configurations = kerberosHelper.calculateConfigurations(cluster, hostName, kerberosDescriptorProperties);

          try {
            // Iterate over the components installed on the current host to get the service and
            // component-level Kerberos descriptors in order to determine which principals,
            // keytab files, and configurations need to be created or updated.
            for (ServiceComponentHost sch : serviceComponentHosts) {
              String serviceName = sch.getServiceName();
              String componentName = sch.getServiceComponentName();

              KerberosServiceDescriptor serviceDescriptor = kerberosDescriptor.getService(serviceName);

              if (serviceDescriptor != null) {
                List<KerberosIdentityDescriptor> serviceIdentities = serviceDescriptor.getIdentities(true);

                // Lazily create the KerberosIdentityDataFileWriter instance...
                if (kerberosIdentityDataFileWriter == null) {
                  actionLog.writeStdOut(String.format("Writing Kerberos identity data metadata file to %s", identityDataFile.getAbsolutePath()));
                  kerberosIdentityDataFileWriter = kerberosIdentityDataFileWriterFactory.createKerberosIdentityDataFileWriter(identityDataFile);
                }

                // Add service-level principals (and keytabs)
                kerberosHelper.addIdentities(kerberosIdentityDataFileWriter, serviceIdentities,
                    identityFilter, hostName, serviceName, componentName, kerberosConfigurations, configurations);

                KerberosComponentDescriptor componentDescriptor = serviceDescriptor.getComponent(componentName);

                if (componentDescriptor != null) {
                  List<KerberosIdentityDescriptor> componentIdentities = componentDescriptor.getIdentities(true);

                  // Calculate the set of configurations to update and replace any variables
                  // using the previously calculated Map of configurations for the host.
                  kerberosHelper.mergeConfigurations(kerberosConfigurations,
                      componentDescriptor.getConfigurations(true), configurations);

                  // Add component-level principals (and keytabs)
                  kerberosHelper.addIdentities(kerberosIdentityDataFileWriter, componentIdentities,
                      identityFilter, hostName, serviceName, componentName, kerberosConfigurations, configurations);
                }
              }
            }
          } catch (IOException e) {
            String message = String.format("Failed to write index file - %s", identityDataFile.getAbsolutePath());
            LOG.error(message, e);
            actionLog.writeStdOut(message);
            actionLog.writeStdErr(message + "\n" + e.getLocalizedMessage());
            throw new AmbariException(message, e);
          }
        }
      }
      finally {
        if (kerberosIdentityDataFileWriter != null) {
          // Make sure the data file is closed
          try {
            kerberosIdentityDataFileWriter.close();
          } catch (IOException e) {
            String message = "Failed to close the index file writer";
            LOG.warn(message, e);
            actionLog.writeStdOut(message);
            actionLog.writeStdErr(message + "\n" + e.getLocalizedMessage());
          }
        }
      }
    }
  }

  protected Map<String, ? extends Collection<String>> getServiceComponentFilter() {
    String serializedValue = getCommandParameterValue(SERVICE_COMPONENT_FILTER);

    if(serializedValue != null) {
      Type type = new TypeToken<Map<String, ? extends Collection<String>>>() {}.getType();
      return StageUtils.getGson().fromJson(serializedValue, type);
    }
    else {
      return null;
    }
  }

  protected Collection<String> getIdentityFilter() {
    String serializedValue = getCommandParameterValue(IDENTITY_FILTER);

    if(serializedValue != null) {
      Type type = new TypeToken<Collection<String>>() {}.getType();
      return StageUtils.getGson().fromJson(serializedValue, type);
    }
    else {
      return null;
    }
  }
}
