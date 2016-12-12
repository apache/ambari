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

package org.apache.ambari.server.controller.gsinstaller;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.apache.ambari.server.controller.internal.AbstractProviderModule;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceProvider;

/**
 * A provider module implementation that uses the GSInstaller resource provider.
 */
public class GSInstallerProviderModule extends AbstractProviderModule implements GSInstallerStateProvider{

  private final ClusterDefinition clusterDefinition;

  private static final Map<String, String> PORTS = new HashMap<String, String>();

  static {
    PORTS.put("NAMENODE",           "50070");
    PORTS.put("DATANODE",           "50075");
    PORTS.put("JOBTRACKER",         "50030");
    PORTS.put("TASKTRACKER",        "50060");
    PORTS.put("HBASE_MASTER",       "60010");
    PORTS.put("HBASE_REGIONSERVER", "60030");
  }

  private static final int TIMEOUT = 5000;


  // ----- Constructors ------------------------------------------------------

  public GSInstallerProviderModule() {
    clusterDefinition = new ClusterDefinition(this);
  }


  // ----- GSInstallerStateProvider ------------------------------------------

  @Override
  public boolean isHealthy(String hostName, String componentName) {
    String port = PORTS.get(componentName);
    if (port != null) {
      StringBuilder sb = new StringBuilder();
      sb.append("http://").append(hostName);
      sb.append(":").append(port);

      try {
        HttpURLConnection connection = (HttpURLConnection) new URL(sb.toString()).openConnection();

        connection.setRequestMethod("HEAD");
        connection.setConnectTimeout(TIMEOUT);
        connection.setReadTimeout(TIMEOUT);

        int code = connection.getResponseCode();

        return code >= 200 && code <= 399;
      } catch (IOException exception) {
        return false;
      }
    }
    return true;
  }


  // ----- utility methods ---------------------------------------------------

  @Override
  protected ResourceProvider createResourceProvider(Resource.Type type) {
    return GSInstallerResourceProvider.getResourceProvider(type, clusterDefinition);
  }
}
