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

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.actionmanager.HostRoleStatus;
import org.apache.ambari.server.agent.CommandReport;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.ServiceComponentSupport;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.UpgradeContext;
import org.apache.commons.collections.CollectionUtils;

import com.google.inject.Inject;

/**
 * Changes oozie-env (adds -Dhdp.version to $HADOOP_OPTS variable)
 * and oozie-site (removes oozie.service.ELService.ext.functions.*) during upgrade
 */
public class OozieConfigCalculation extends AbstractUpgradeServerAction {

  private static final String FALCON_SERVICE_NAME = "FALCON";
  @Inject
  private ServiceComponentSupport serviceComponentSupport;

  private static final String OOZIE_ENV_TARGET_CONFIG_TYPE = "oozie-env";
  private static final String OOZIE_SITE_TARGET_CONFIG_TYPE = "oozie-site";
  private static final String ELSERVICE_PROPERTIES_NAME_PREFIX = "oozie.service.ELService.ext.functions.";
  private static final String CONTENT_PROPERTY_NAME = "content";
  private boolean oozie_env_updated = false;
  private boolean oozie_site_updated = false;

  @Override
  public CommandReport execute(ConcurrentMap<String, Object> requestSharedDataContext)
    throws AmbariException, InterruptedException {
    String clusterName = getExecutionCommand().getClusterName();
    Cluster cluster = getClusters().getCluster(clusterName);
    StringBuilder stdOutBuilder = new StringBuilder();

    try {
      changeOozieEnv(cluster, stdOutBuilder);
    } catch (Exception e) {
      return  createCommandReport(0, HostRoleStatus.FAILED,"{}",
          String.format("Source type %s not found", OOZIE_ENV_TARGET_CONFIG_TYPE), "");
    }

    UpgradeContext upgradeContext = getUpgradeContext(cluster);
    StackId targetStackId = upgradeContext.getTargetStack();


    if (!serviceComponentSupport.isServiceSupported(FALCON_SERVICE_NAME, targetStackId.getStackName(), targetStackId.getStackVersion())) {
      try {
        removeFalconPropertiesFromOozieSize(cluster, stdOutBuilder);
      } catch (AmbariException e) {
        return createCommandReport(0, HostRoleStatus.FAILED, "{}",
            String.format("Source type %s not found", OOZIE_SITE_TARGET_CONFIG_TYPE), "");
      }
    }

    if (oozie_env_updated || oozie_site_updated) {
      agentConfigsHolder.updateData(cluster.getClusterId(), cluster.getHosts().stream().map(Host::getHostId).collect(Collectors.toList()));
    }

    return createCommandReport(0, HostRoleStatus.COMPLETED, "{}",
                  stdOutBuilder.toString(), "");
  }

  /**
   * Changes oozie-site (removes oozie.service.ELService.ext.functions.*)
   */
  private void removeFalconPropertiesFromOozieSize(Cluster cluster, StringBuilder stringBuilder) throws AmbariException {
    Config config = cluster.getDesiredConfigByType(OOZIE_SITE_TARGET_CONFIG_TYPE);

    if (config == null) {
      throw new AmbariException(String.format("Target config not found %s", OOZIE_SITE_TARGET_CONFIG_TYPE));
    }

    Map<String, String> properties = config.getProperties();
    List<String> propertiesToRemove = properties.keySet().stream().filter(
        s -> s.startsWith(ELSERVICE_PROPERTIES_NAME_PREFIX)).collect(Collectors.toList());

    if (!CollectionUtils.isEmpty(propertiesToRemove)) {
      stringBuilder.append(String.format("Removed following properties from %s: %s", OOZIE_SITE_TARGET_CONFIG_TYPE, propertiesToRemove));
      stringBuilder.append(System.lineSeparator());
      properties.keySet().removeAll(propertiesToRemove);
      oozie_site_updated = true;
    } else {
      stringBuilder.append(String.format("No properties with prefix %s found in %s", ELSERVICE_PROPERTIES_NAME_PREFIX, OOZIE_ENV_TARGET_CONFIG_TYPE));
      stringBuilder.append(System.lineSeparator());
      return;
    }

    config.setProperties(properties);
    config.save();
  }

  /**
   * Changes oozie-env (adds -Dhdp.version to $HADOOP_OPTS variable)
   */
  private void changeOozieEnv(Cluster cluster, StringBuilder stringBuilder) throws AmbariException {

    Config config = cluster.getDesiredConfigByType(OOZIE_ENV_TARGET_CONFIG_TYPE);

    if (config == null) {
      throw new AmbariException(String.format("Target config not found %s", OOZIE_ENV_TARGET_CONFIG_TYPE));
    }

    Map<String, String> properties = config.getProperties();
    String oldContent = properties.get(CONTENT_PROPERTY_NAME);

    String newContent = processPropertyValue(oldContent);

    if (newContent.equals(oldContent)) {
      stringBuilder.append("-Dhdp.version option has been already added to $HADOOP_OPTS variable");
      stringBuilder.append(System.lineSeparator());
      return;
    } else {
      properties.put(CONTENT_PROPERTY_NAME, newContent);
      oozie_env_updated = true;
      stringBuilder.append(String.format("Added -Dhdp.version to $HADOOP_OPTS variable at %s", OOZIE_ENV_TARGET_CONFIG_TYPE));
      stringBuilder.append(System.lineSeparator());
    }

    config.setProperties(properties);
    config.save();
  }

  public static String processPropertyValue(String oldContent) {
    // For regex simplicity, will not work with multiline export definitions that are
    // split on few strings using \ character
    // False negative should be a less bit of trouble (just duplicate exports/option definitions)
    // than false positive (broken Oozie after upgrade)
    Pattern regex = Pattern.compile("^export HADOOP_OPTS=.*-Dhdp.version=.*$", Pattern.MULTILINE);
    Matcher regexMatcher = regex.matcher(oldContent);
    if (regexMatcher.find()) {
      return oldContent;
    } else {
      StringBuilder newContent = new StringBuilder(oldContent);
      newContent.append("\n").append(
        "export HADOOP_OPTS=\"-Dhdp.version=$HDP_VERSION $HADOOP_OPTS\" "
      );
      return newContent.toString();
    }
  }
}
