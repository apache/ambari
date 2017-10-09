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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.actionmanager.HostRoleStatus;
import org.apache.ambari.server.agent.CommandReport;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Config;

/**
 * Append hive-env config type with HIVE_HOME and HIVE_CONF_DIR variables if they are absent
 */
public class HiveEnvClasspathAction extends AbstractUpgradeServerAction {
  private static final String TARGET_CONFIG_TYPE = "hive-env";
  private static final String CONTENT_PROPERTY_NAME = "content";

  private static final String HIVE_HOME = "HIVE_HOME";
  private static final String HIVE_CONF_DIR = "HIVE_CONF_DIR";

  private static final String HIVE_HOME_APPEND = "export HIVE_HOME=${HIVE_HOME:-{{hive_home_dir}}}";
  private static final String HIVE_CONF_DIR_APPEND = "export HIVE_CONF_DIR=${HIVE_CONF_DIR:-{{hive_config_dir}}}";

  private static final String VERIFY_REGEXP = "^\\s*export\\s(?<property>%s|%s)\\s*=\\s*.*$";

  @Override
  public CommandReport execute(ConcurrentMap<String, Object> requestSharedDataContext)
    throws AmbariException, InterruptedException {


    String clusterName = getExecutionCommand().getClusterName();
    Cluster cluster = getClusters().getCluster(clusterName);
    Config config = cluster.getDesiredConfigByType(TARGET_CONFIG_TYPE);

    if (config == null) {
      return  createCommandReport(0, HostRoleStatus.FAILED,"{}",
        String.format("Source type %s not found", TARGET_CONFIG_TYPE), "");
    }

    Pattern regex = Pattern.compile(String.format(VERIFY_REGEXP, HIVE_HOME, HIVE_CONF_DIR), Pattern.MULTILINE);

    Map<String, String> properties = config.getProperties();
    String oldContent = properties.get(CONTENT_PROPERTY_NAME);

    Matcher regexMatcher = regex.matcher(oldContent);
    boolean isHiveHomeFound = false;
    boolean isHiveConfFound = false;

    while (regexMatcher.find()){
      String propertyName = regexMatcher.group("property");

      if (propertyName.equalsIgnoreCase(HIVE_CONF_DIR)){
        isHiveConfFound = true;
      } else if (propertyName.equalsIgnoreCase(HIVE_HOME)){
        isHiveHomeFound = true;
      }
    }

    StringBuilder stringBuilder = new StringBuilder(oldContent);

    if (!isHiveConfFound) {
      stringBuilder.append("\n").append(HIVE_CONF_DIR_APPEND);
    }

    if (!isHiveHomeFound) {
      stringBuilder.append("\n").append(HIVE_HOME_APPEND);
    }

    String newContent = stringBuilder.toString();

    if (newContent.equals(oldContent)) {
      return  createCommandReport(0, HostRoleStatus.COMPLETED, "{}",
        String.format("%s/%s option has been already added to hive-env", HIVE_HOME, HIVE_CONF_DIR), "");
    } else {
      properties.put(CONTENT_PROPERTY_NAME, newContent);
    }

    config.setProperties(properties);
    config.save();

    return createCommandReport(0, HostRoleStatus.COMPLETED, "{}",
      String.format("Added %s, %s to content at %s", HIVE_CONF_DIR, HIVE_HOME, TARGET_CONFIG_TYPE), "");

  }

}
