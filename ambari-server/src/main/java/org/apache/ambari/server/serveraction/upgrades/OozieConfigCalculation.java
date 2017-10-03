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
 * Changes oozie-env during upgrade (adds -Dhdp.version to $HADOOP_OPTS variable)
 */
public class OozieConfigCalculation extends AbstractUpgradeServerAction {
  private static final String TARGET_CONFIG_TYPE = "oozie-env";
  private static final String CONTENT_PROPERTY_NAME = "content";

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

    Map<String, String> properties = config.getProperties();
    String oldContent = properties.get(CONTENT_PROPERTY_NAME);

    String newContent = processPropertyValue(oldContent);

    if (newContent.equals(oldContent)) {
      return  createCommandReport(0, HostRoleStatus.COMPLETED, "{}",
        "-Dhdp.version option has been already added to $HADOOP_OPTS variable", "");
    } else {
      properties.put(CONTENT_PROPERTY_NAME, newContent);
    }

    config.setProperties(properties);
    config.save();

    return createCommandReport(0, HostRoleStatus.COMPLETED, "{}",
                  String.format("Added -Dhdp.version to $HADOOP_OPTS variable at %s", TARGET_CONFIG_TYPE), "");

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
