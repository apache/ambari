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

package org.apache.ambari.server.utils;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Config;

import java.util.Map;

public class SecretReference {
  private String clusterName;
  private String configType;
  private Long version;
  private String value;
  private String reference;

  public SecretReference(String reference, String propertyName, Cluster cluster) throws AmbariException{
    String[] values = reference.split(":");
    clusterName = values[1];
    configType = values[2];
    version = Long.valueOf(values[3]);
    Config refConfig = cluster.getConfigByVersion(configType, version);

    if(refConfig == null)
      throw new AmbariException(String.format("Cluster: %s does not contain ConfigType: %s ConfigVersion: %s",
          cluster.getClusterName(), configType, version));
    Map<String, String> refProperties = refConfig.getProperties();
    if(!refProperties.containsKey(propertyName))
      throw new AmbariException(String.format("Cluster: %s ConfigType: %s ConfigVersion: %s does not contain property '%s'",
          cluster.getClusterName(), configType, version, propertyName));
    this.value = refProperties.get(propertyName);

    this.reference = reference;
  }

  public String getClusterName() {
    return clusterName;
  }

  public void setConfigType(String configType) {
    this.configType = configType;
  }

  public Long getVersion() {
    return version;
  }

  public String getValue() {
    return value;
  }

  public static boolean isSecret(String value) {
    String[] values = value.split(":");
    return values.length == 4 && values[0].equals("SECRET");
  }

  public static String generateStub(String clusterName, String configType, Long configVersion) {
    return "SECRET:" + clusterName + ":" + configType + ":" + configVersion.toString();
  }
}
