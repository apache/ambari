/*
  Licensed to the Apache Software Foundation (ASF) under one
  or more contributor license agreements.  See the NOTICE file
  distributed with this work for additional information
  regarding copyright ownership.  The ASF licenses this file
  to you under the Apache License, Version 2.0 (the
  "License"); you may not use this file except in compliance
  with the License.  You may obtain a copy of the License at
  <p>
  http://www.apache.org/licenses/LICENSE-2.0
  <p>
  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
 */
package org.apache.ambari.server.serveraction.upgrades;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.actionmanager.HostRoleStatus;
import org.apache.ambari.server.agent.CommandReport;
import org.apache.ambari.server.orm.dao.ArtifactDAO;
import org.apache.ambari.server.orm.entities.ArtifactEntity;
import org.apache.ambari.server.serveraction.AbstractServerAction;
import org.apache.ambari.server.state.kerberos.KerberosConfigurationDescriptor;
import org.apache.ambari.server.state.kerberos.KerberosDescriptor;
import org.apache.ambari.server.state.kerberos.KerberosDescriptorFactory;
import org.apache.ambari.server.state.kerberos.KerberosServiceDescriptor;

import com.google.inject.Inject;


/**
 * Update Kerberos Descriptor Storm properties when upgrading to Storm 1.0
 */
public class StormUpgradeKerberosDescriptorConfig extends AbstractServerAction {


  public static final String KERBEROS_DESCRIPTOR = "kerberos_descriptor";
  public static final String STORM = "STORM";
  public static final String NEW_PART = "org.apache";
  public static final String OLD_PART = "backtype";

  @Inject
  ArtifactDAO artifactDAO;

  protected StormUpgradeKerberosDescriptorConfig() {
  }

  /**
   * Lists config types and properties that may be replaced "backtype" to "org.apache"
   */
  private static final HashMap<String, String[]> TARGET_PROPERTIES = new HashMap<String, String[]>() {{
    put("storm-site", new String[]{
        "nimbus.authorizer",
        "storm.principal.tolocal",
        "drpc.authorizer"
    });

  }};

  /**
   * Update Kerberos Descriptor Storm properties when upgrading to Storm 1.0
   * <p/>
   * Finds the relevant artifact entities and iterates through them to process each independently.
   */
  @Override
  public CommandReport execute(ConcurrentMap<String, Object> requestSharedDataContext)
      throws AmbariException, InterruptedException {

    String msg = "";

    List<ArtifactEntity> artifactEntities = artifactDAO.findByName(KERBEROS_DESCRIPTOR);

    if (artifactEntities != null) {
      for (ArtifactEntity artifactEntity : artifactEntities) {
        if (artifactEntity == null) {
          continue;
        }
        msg = updateKerberosDescriptorConfigurations(msg, artifactEntity);
      }
    } else {
      msg = msg + MessageFormat.format("{0} not found\n", KERBEROS_DESCRIPTOR);
    }

    msg = MessageFormat.format("{0}\n Successfully replace properties", msg);
    return createCommandReport(0, HostRoleStatus.COMPLETED, "{}", msg, "");
  }

  private String updateKerberosDescriptorConfigurations(String msg, ArtifactEntity artifactEntity) {
    Map<String, Object> data = artifactEntity.getArtifactData();

    if (data != null) {
      final KerberosDescriptor kerberosDescriptor = new KerberosDescriptorFactory().createInstance(data);

      if (kerberosDescriptor != null) {
        // Get the service that needs to be updated
        KerberosServiceDescriptor serviceDescriptor = kerberosDescriptor.getService(STORM);

        if (serviceDescriptor != null) {
          for (String configType : TARGET_PROPERTIES.keySet()) {
            // Get the configuration that needs to be updated
            KerberosConfigurationDescriptor configurationDescriptor = serviceDescriptor.getConfiguration(configType);

            if (configurationDescriptor != null) {
              for (String key : TARGET_PROPERTIES.get(configType)) {
                // Get the configuration properties that needs to be updated
                String value = configurationDescriptor.getProperty(key);
                if (value != null) {
                  value = value.replace(OLD_PART, NEW_PART);
                  configurationDescriptor.putProperty(key, value);
                  msg = msg + MessageFormat.format("{0}={1}, \n", key, value);
                } else {
                  msg = msg + MessageFormat.format("{0} not found in {1}\n", key, configType);
                }

                artifactEntity.setArtifactData(kerberosDescriptor.toMap());
                artifactDAO.merge(artifactEntity);
              }
            } else {
              msg = msg + MessageFormat.format("{0} not found\n", configType);
            }
          }
        } else {
          msg = msg + MessageFormat.format("{0} not found\n", STORM);
        }
        artifactEntity.setArtifactData(kerberosDescriptor.toMap());
        artifactDAO.merge(artifactEntity);
      } else {
        msg = msg + "KerberosDescriptor not created from data";
      }
    } else {
      msg = msg + MessageFormat.format("{0} not found\n", "artifactData");
    }
    return msg;
  }
}