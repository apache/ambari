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

import com.google.inject.Inject;
import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.actionmanager.HostRoleStatus;
import org.apache.ambari.server.agent.CommandReport;
import org.apache.ambari.server.serveraction.AbstractServerAction;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Config;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * During stack upgrade, update lzo codec path in mapreduce.application.classpath and
 * at tez.cluster.additional.classpath.prefix to look like
 * /usr/hdp/${hdp.version}/hadoop/lib/hadoop-lzo-0.6.0.${hdp.version}.jar
 */
public class FixLzoCodecPath extends AbstractServerAction {

  /**
   * Lists config types and properties that may contain lzo codec path
   */
  private static final HashMap<String, String []> TARGET_PROPERTIES = new HashMap<String, String []>() {{
    put("mapred-site", new String [] {"mapreduce.application.classpath"});
    put("tez-site", new String [] {"tez.cluster.additional.classpath.prefix"});
  }};

  @Inject
  private Clusters clusters;

  @Override
  public CommandReport execute(ConcurrentMap<String, Object> requestSharedDataContext)
    throws AmbariException, InterruptedException {
    String clusterName = getExecutionCommand().getClusterName();
    Cluster cluster = clusters.getCluster(clusterName);

    ArrayList<String> modifiedProperties = new ArrayList<>();

    for (Map.Entry<String, String[]> target : TARGET_PROPERTIES.entrySet()) {
      Config config = cluster.getDesiredConfigByType(target.getKey());
      if (config == null) {
        continue; // Config not found, skip it
      }
      Map<String, String> properties = config.getProperties();
      for (String propertyName : target.getValue()) {
        String oldContent = properties.get(propertyName);
        String newContent = fixLzoJarPath(oldContent);

        if (! newContent.equals(oldContent)) {
          properties.put(propertyName, newContent);
          modifiedProperties.add(propertyName);
        }
      }
      config.setProperties(properties);
      config.persist(false);
    }
    if (modifiedProperties.isEmpty()) {
      return createCommandReport(0, HostRoleStatus.COMPLETED, "{}",
        "No properties require lzo codec path fixes", "");
    } else {
      return createCommandReport(0, HostRoleStatus.COMPLETED, "{}",
        String.format("Fixed lzo codec path value at property [%s] to " +
          "use ${hdp.version} instead of hardcoded HDP version.",
          StringUtils.join(modifiedProperties, ", ")), "");
    }

  }

  public static String fixLzoJarPath(String oldPropertyValue) {
    // Makes sure that LZO codec path uses ${hdp.version} instead of hardcoded hdp version,
    // so it replaces variations of /usr/hdp/2.3.4.0-3485/hadoop/lib/hadoop-lzo-0.6.0.2.3.4.0-3485.jar
    // with /usr/hdp/${hdp.version}/hadoop/lib/hadoop-lzo-0.6.0.${hdp.version}.jar
    return oldPropertyValue.replaceAll(
      "(/usr/hdp/)[^\\\\:]+(/hadoop/lib/hadoop-lzo-(\\d\\.)+)(\\$\\{hdp.version\\}|(\\d\\.){3}\\d-\\d+)(\\.jar)",
      "$1\\$\\{hdp.version\\}$2\\$\\{hdp.version\\}$6");
  }
}
