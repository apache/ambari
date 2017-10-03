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
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Config;
import org.apache.commons.lang.StringUtils;


/**
 * During stack upgrade, update lzo codec path in mapreduce.application.classpath and
 * at tez.cluster.additional.classpath.prefix to look like
 * /usr/hdp/${hdp.version}/hadoop/lib/hadoop-lzo-0.6.0.${hdp.version}.jar
 */
public class FixOozieAdminUsers extends AbstractUpgradeServerAction {
  private static final String TARGET_OOZIE_CONFIG_TYPE = "oozie-env";
  private static final String OOZIE_ADMIN_USERS_PROP = "oozie_admin_users";
  private static final String FALCON_CONFIG_TYPE = "falcon-env";
  private static final String FALCON_USER_PROP = "falcon_user";


  @Override
  public CommandReport execute(ConcurrentMap<String, Object> requestSharedDataContext)
    throws AmbariException, InterruptedException {
    String clusterName = getExecutionCommand().getClusterName();
    Cluster cluster = getClusters().getCluster(clusterName);
    Config oozieConfig = cluster.getDesiredConfigByType(TARGET_OOZIE_CONFIG_TYPE);
    Config falconConfig = cluster.getDesiredConfigByType(FALCON_CONFIG_TYPE);

    if (falconConfig == null) {
      return  createCommandReport(0, HostRoleStatus.COMPLETED, "{}",
              "Falcon configuration unavailable", "");

    }
    if (oozieConfig == null) {
      return  createCommandReport(0, HostRoleStatus.COMPLETED, "{}",
              "Oozie configuration unavailable", "");
    }

    Map<String, String> oozieProperties = oozieConfig.getProperties();
    String currentOozieAdmins = oozieProperties.get(OOZIE_ADMIN_USERS_PROP);
    if (currentOozieAdmins.isEmpty()) {
      currentOozieAdmins = "";
    }
    Map<String, String> falconProperties = falconConfig.getProperties();
    String falconUser = falconProperties.get(FALCON_USER_PROP);
    if (StringUtils.isBlank(falconUser)) {
      return  createCommandReport(0, HostRoleStatus.COMPLETED, "{}",
              "Falcon user not set", "");
    }

    if (currentOozieAdmins.indexOf(falconUser) >= 0) {
      return  createCommandReport(0, HostRoleStatus.COMPLETED, "{}",
              "Falcon user already member of Oozie admins", "");
    }

    String newOozieAdminUsers = currentOozieAdmins + "," + falconUser;

    oozieProperties.put(OOZIE_ADMIN_USERS_PROP, newOozieAdminUsers);

    oozieConfig.setProperties(oozieProperties);
    oozieConfig.save();

    return createCommandReport(0, HostRoleStatus.COMPLETED, "{}",
            String.format("Set oozie admin users to %s", newOozieAdminUsers), "");
  }

}
