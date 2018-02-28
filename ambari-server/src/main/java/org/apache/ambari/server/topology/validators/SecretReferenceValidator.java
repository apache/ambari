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
package org.apache.ambari.server.topology.validators;

import java.util.Map;

import org.apache.ambari.server.topology.ClusterTopology;
import org.apache.ambari.server.topology.InvalidTopologyException;
import org.apache.ambari.server.utils.SecretReference;

/**
 * Secret references are not allowed in blueprints.
 * @see SecretReference
 */
public class SecretReferenceValidator implements TopologyValidator {

  @Override
  public void validate(ClusterTopology topology) throws InvalidTopologyException {
    // we don't want to include default stack properties so we can't use full properties
    Map<String, Map<String, String>> clusterConfigurations = topology.getConfiguration().getProperties();

    // we need to have real passwords, not references
    if (clusterConfigurations != null) {
      StringBuilder errorMessage = new StringBuilder();
      boolean containsSecretReferences = false;
      for (Map.Entry<String, Map<String, String>> configEntry : clusterConfigurations.entrySet()) {
        String configType = configEntry.getKey();
        Map<String, String> configEntryValue = configEntry.getValue();
        if (configEntryValue != null) {
          for (Map.Entry<String, String> propertyEntry : configEntryValue.entrySet()) {
            String propertyName = propertyEntry.getKey();
            String propertyValue = propertyEntry.getValue();
            if (propertyValue != null && SecretReference.isSecret(propertyValue)) {
              errorMessage.append(String.format("  Config:%s Property:%s\n", configType, propertyName));
              containsSecretReferences = true;
            }
          }
        }
      }
      if (containsSecretReferences) {
        throw new InvalidTopologyException("Secret references are not allowed in blueprints, " +
          "replace following properties with real passwords:\n" + errorMessage);
      }
    }
  }

}
