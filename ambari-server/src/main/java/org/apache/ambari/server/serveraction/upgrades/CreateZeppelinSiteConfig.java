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
package org.apache.ambari.server.serveraction.upgrades;


import java.util.Map;
import java.util.concurrent.ConcurrentMap;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.actionmanager.HostRoleStatus;
import org.apache.ambari.server.agent.CommandReport;
import org.apache.ambari.server.serveraction.AbstractServerAction;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.ConfigFactory;
import org.apache.ambari.server.state.ConfigHelper;

import com.google.inject.Inject;

public class CreateZeppelinSiteConfig extends AbstractServerAction {
  public static final String VERSION_TAG = "version1";
  public static final String ZEPPELIN_SITE_CONFIG = "zeppelin-site";
  public static final String ZEPPELIN_CONFIG_CONFIG = "zeppelin-config";

  @Inject
  private Clusters clusters;

  @Inject
  private ConfigFactory configFactory;

  @Inject
  private ConfigHelper configHelper;

  @Override
  public CommandReport execute(ConcurrentMap<String, Object> requestSharedDataContext)
          throws AmbariException, InterruptedException {
    String clusterName = getExecutionCommand().getClusterName();
    Cluster cluster = clusters.getCluster(clusterName);

    Config zeppelinConfConfig = cluster.getDesiredConfigByType(ZEPPELIN_CONFIG_CONFIG);
    Config zeppelinSiteConfig = cluster.getDesiredConfigByType(ZEPPELIN_SITE_CONFIG);

    if (zeppelinConfConfig == null) {
      return createCommandReport(0, HostRoleStatus.COMPLETED, "{}",
              String.format("changes are not required"), "");
    }

    String output = "";
    Map<String, String> zeppelinConfProperties = zeppelinConfConfig.getProperties();
    zeppelinConfProperties.put("zeppelin.config.storage.class","org.apache.zeppelin.storage.FileSystemConfigStorage");
    if (zeppelinSiteConfig != null) {
      output += String.format("copying properties from config %s to %s" + System.lineSeparator(), ZEPPELIN_CONFIG_CONFIG, ZEPPELIN_SITE_CONFIG);
      zeppelinConfProperties.putAll(zeppelinSiteConfig.getProperties());
      zeppelinSiteConfig.setProperties(zeppelinConfProperties);
      zeppelinSiteConfig.save();
    } else {
      output += String.format("creating new config %s" + System.lineSeparator(), ZEPPELIN_SITE_CONFIG);
      Config zeppelinSite = configFactory.createNew(cluster.getDesiredStackVersion(), cluster, ZEPPELIN_SITE_CONFIG, VERSION_TAG,
              zeppelinConfProperties, zeppelinConfConfig.getPropertiesAttributes());
      cluster.addConfig(zeppelinSite);
    }

    output += String.format("removing %s config" + System.lineSeparator(), ZEPPELIN_CONFIG_CONFIG);
    configHelper.removeConfigsByType(cluster, ZEPPELIN_CONFIG_CONFIG);


    if (output.isEmpty()) {
      output = "change not required";
    }

    return createCommandReport(0, HostRoleStatus.COMPLETED, "{}",
            output, "");
  }
}
