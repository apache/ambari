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

package org.apache.ambari.server.actionmanager;


import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.Role;
import org.apache.ambari.server.RoleCommand;
import org.apache.ambari.server.agent.AgentCommand.AgentCommandType;
import org.apache.ambari.server.agent.ExecutionCommand;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.ConfigFactory;
import org.apache.ambari.server.state.ConfigHelper;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.svccomphost.ServiceComponentHostStartEvent;
import org.apache.ambari.server.utils.StageUtils;
import org.codehaus.jettison.json.JSONException;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.inject.Guice;
import com.google.inject.Injector;

import junit.framework.Assert;

public class ExecutionCommandWrapperTest {

  private static final String HOST1 = "dev01.ambari.apache.org";
  private static final String CLUSTER1 = "c1";
  private static final String CLUSTER_VERSION_TAG = "clusterVersion";
  private static final String SERVICE_VERSION_TAG = "serviceVersion";
  private static final String HOST_VERSION_TAG = "hostVersion";
  private static final String GLOBAL_CONFIG = "global";
  private static final String SERVICE_SITE_CONFIG = "service-site";
  private static final String SERVICE_SITE_NAME1 = "ssn1";
  private static final String SERVICE_SITE_NAME2 = "ssn2";
  private static final String SERVICE_SITE_NAME3 = "ssn3";
  private static final String SERVICE_SITE_NAME4 = "ssn4";
  private static final String SERVICE_SITE_NAME5 = "ssn5";
  private static final String SERVICE_SITE_NAME6 = "ssn6";
  private static final String SERVICE_SITE_VAL1 = "ssv1";
  private static final String SERVICE_SITE_VAL1_S = "ssv1_s";
  private static final String SERVICE_SITE_VAL2 = "ssv2";
  private static final String SERVICE_SITE_VAL2_H = "ssv2_h";
  private static final String SERVICE_SITE_VAL3 = "ssv3";
  private static final String SERVICE_SITE_VAL4 = "ssv4";
  private static final String SERVICE_SITE_VAL5 = "ssv5";
  private static final String SERVICE_SITE_VAL5_S = "ssv5_s";
  private static final String SERVICE_SITE_VAL6_H = "ssv6_h";
  private static final String GLOBAL_NAME1 = "gn1";
  private static final String GLOBAL_NAME2 = "gn2";
  private static final String GLOBAL_CLUSTER_VAL1 = "gcv1";
  private static final String GLOBAL_CLUSTER_VAL2 = "gcv2";
  private static final String GLOBAL_VAL1 = "gv1";

  private static Map<String, String> GLOBAL_CLUSTER;
  private static Map<String, String> SERVICE_SITE_CLUSTER;
  private static Map<String, String> SERVICE_SITE_SERVICE;
  private static Map<String, String> SERVICE_SITE_HOST;
  private static Map<String, Map<String, String>> CONFIG_ATTRIBUTES;

  private static Injector injector;
  private static Clusters clusters;
  private static ConfigFactory configFactory;
  private static ConfigHelper configHelper;
  private static StageFactory stageFactory;

  @BeforeClass
  public static void setup() throws AmbariException {
    injector = Guice.createInjector(new InMemoryDefaultTestModule());
    injector.getInstance(GuiceJpaInitializer.class);
    configHelper = injector.getInstance(ConfigHelper.class);
    configFactory = injector.getInstance(ConfigFactory.class);
    stageFactory = injector.getInstance(StageFactory.class);

    clusters = injector.getInstance(Clusters.class);
    clusters.addHost(HOST1);
    clusters.addCluster(CLUSTER1, new StackId("HDP-0.1"));

    Cluster cluster1 = clusters.getCluster(CLUSTER1);

    SERVICE_SITE_CLUSTER = new HashMap<String, String>();
    SERVICE_SITE_CLUSTER.put(SERVICE_SITE_NAME1, SERVICE_SITE_VAL1);
    SERVICE_SITE_CLUSTER.put(SERVICE_SITE_NAME2, SERVICE_SITE_VAL2);
    SERVICE_SITE_CLUSTER.put(SERVICE_SITE_NAME3, SERVICE_SITE_VAL3);
    SERVICE_SITE_CLUSTER.put(SERVICE_SITE_NAME4, SERVICE_SITE_VAL4);

    SERVICE_SITE_SERVICE = new HashMap<String, String>();
    SERVICE_SITE_SERVICE.put(SERVICE_SITE_NAME1, SERVICE_SITE_VAL1_S);
    SERVICE_SITE_SERVICE.put(SERVICE_SITE_NAME5, SERVICE_SITE_VAL5_S);

    SERVICE_SITE_HOST = new HashMap<String, String>();
    SERVICE_SITE_HOST.put(SERVICE_SITE_NAME2, SERVICE_SITE_VAL2_H);
    SERVICE_SITE_HOST.put(SERVICE_SITE_NAME6, SERVICE_SITE_VAL6_H);

    GLOBAL_CLUSTER = new HashMap<String, String>();
    GLOBAL_CLUSTER.put(GLOBAL_NAME1, GLOBAL_CLUSTER_VAL1);
    GLOBAL_CLUSTER.put(GLOBAL_NAME2, GLOBAL_CLUSTER_VAL2);

    CONFIG_ATTRIBUTES = new HashMap<String, Map<String,String>>();

    //Cluster level global config
    configFactory.createNew(cluster1, GLOBAL_CONFIG, CLUSTER_VERSION_TAG, GLOBAL_CLUSTER, CONFIG_ATTRIBUTES);

    //Cluster level service config
    configFactory.createNew(cluster1, SERVICE_SITE_CONFIG, CLUSTER_VERSION_TAG, SERVICE_SITE_CLUSTER, CONFIG_ATTRIBUTES);

    //Service level service config
    configFactory.createNew(cluster1, SERVICE_SITE_CONFIG, SERVICE_VERSION_TAG, SERVICE_SITE_SERVICE, CONFIG_ATTRIBUTES);

    //Host level service config
    configFactory.createNew(cluster1, SERVICE_SITE_CONFIG, HOST_VERSION_TAG, SERVICE_SITE_HOST, CONFIG_ATTRIBUTES);

    ActionDBAccessor db = injector.getInstance(ActionDBAccessorImpl.class);

    createTask(db, 1, 1, HOST1, CLUSTER1);

  }

  private static void createTask(ActionDBAccessor db, long requestId, long stageId, String hostName, String clusterName) throws AmbariException {

    Stage s = stageFactory.createNew(requestId, "/var/log", clusterName, 1L, "execution command wrapper test", "clusterHostInfo", "commandParamsStage", "hostParamsStage");
    s.setStageId(stageId);
    s.addHostRoleExecutionCommand(hostName, Role.NAMENODE,
        RoleCommand.START,
        new ServiceComponentHostStartEvent(Role.NAMENODE.toString(),
            hostName, System.currentTimeMillis()), clusterName, "HDFS", false, false);
    List<Stage> stages = new ArrayList<Stage>();
    stages.add(s);
    Request request = new Request(stages, clusters);
    db.persistActions(request);
  }

  @Test
  public void testGetExecutionCommand() throws JSONException, AmbariException {


    Map<String, Map<String, String>> confs = new HashMap<String, Map<String, String>>();
    Map<String, String> configurationsGlobal = new HashMap<String, String>();
    configurationsGlobal.put(GLOBAL_NAME1, GLOBAL_VAL1);
    confs.put(GLOBAL_CONFIG, configurationsGlobal);

    Map<String, Map<String, String>> confTags = new HashMap<String, Map<String, String>>();
    Map<String, String> confTagServiceSite = new HashMap<String, String>();

    confTagServiceSite.put("tag", CLUSTER_VERSION_TAG);
    confTagServiceSite.put("service_override_tag", SERVICE_VERSION_TAG);
    confTagServiceSite.put("host_override_tag", HOST_VERSION_TAG);

    confTags.put(SERVICE_SITE_CONFIG, confTagServiceSite);

    Map<String, String> confTagGlobal = Collections.singletonMap("tag", CLUSTER_VERSION_TAG);

    confTags.put(GLOBAL_CONFIG, confTagGlobal);


    ExecutionCommand executionCommand = new ExecutionCommand();


    executionCommand.setClusterName(CLUSTER1);
    executionCommand.setTaskId(1);
    executionCommand.setRequestAndStage(1, 1);
    executionCommand.setHostname(HOST1);
    executionCommand.setRole("NAMENODE");
    executionCommand.setRoleParams(Collections.<String, String>emptyMap());
    executionCommand.setRoleCommand(RoleCommand.START);
    executionCommand.setConfigurations(confs);
    executionCommand.setConfigurationTags(confTags);
    executionCommand.setServiceName("HDFS");
    executionCommand.setCommandType(AgentCommandType.EXECUTION_COMMAND);
    executionCommand.setCommandParams(Collections.<String, String>emptyMap());

    String json = StageUtils.getGson().toJson(executionCommand, ExecutionCommand.class);

    ExecutionCommandWrapper execCommWrap = new ExecutionCommandWrapper(json);
    injector.injectMembers(execCommWrap);

    ExecutionCommand processedExecutionCommand = execCommWrap.getExecutionCommand();

    Map<String, String> serviceSiteConfig = processedExecutionCommand.getConfigurations().get(SERVICE_SITE_CONFIG);

    Assert.assertEquals(SERVICE_SITE_VAL1_S, serviceSiteConfig.get(SERVICE_SITE_NAME1));
    Assert.assertEquals(SERVICE_SITE_VAL2_H, serviceSiteConfig.get(SERVICE_SITE_NAME2));
    Assert.assertEquals(SERVICE_SITE_VAL3, serviceSiteConfig.get(SERVICE_SITE_NAME3));
    Assert.assertEquals(SERVICE_SITE_VAL4, serviceSiteConfig.get(SERVICE_SITE_NAME4));
    Assert.assertEquals(SERVICE_SITE_VAL5_S, serviceSiteConfig.get(SERVICE_SITE_NAME5));
    Assert.assertEquals(SERVICE_SITE_VAL6_H, serviceSiteConfig.get(SERVICE_SITE_NAME6));

    Map<String, String> globalConfig = processedExecutionCommand.getConfigurations().get(GLOBAL_CONFIG);

    Assert.assertEquals(GLOBAL_VAL1, globalConfig.get(GLOBAL_NAME1));
    Assert.assertEquals(GLOBAL_CLUSTER_VAL2, globalConfig.get(GLOBAL_NAME2));


    //Union of all keys of service site configs
    Set<String> serviceSiteKeys = new HashSet<String>();
    serviceSiteKeys.addAll(SERVICE_SITE_CLUSTER.keySet());
    serviceSiteKeys.addAll(SERVICE_SITE_SERVICE.keySet());
    serviceSiteKeys.addAll(SERVICE_SITE_HOST.keySet());

    Assert.assertEquals(serviceSiteKeys.size(), serviceSiteConfig.size());

  }

  @Test
  public void testGetMergedConfig() {
    Map<String, String> baseConfig = new HashMap<String, String>();

    baseConfig.put(SERVICE_SITE_NAME1, SERVICE_SITE_VAL1);
    baseConfig.put(SERVICE_SITE_NAME2, SERVICE_SITE_VAL2);
    baseConfig.put(SERVICE_SITE_NAME3, SERVICE_SITE_VAL3);
    baseConfig.put(SERVICE_SITE_NAME4, SERVICE_SITE_VAL4);
    baseConfig.put(SERVICE_SITE_NAME5, SERVICE_SITE_VAL5);

    Map<String, String> overrideConfig = new HashMap<String, String>();

    overrideConfig.put(SERVICE_SITE_NAME2, SERVICE_SITE_VAL2_H);
    overrideConfig.put(SERVICE_SITE_NAME6, SERVICE_SITE_VAL6_H);


    Map<String, String> mergedConfig = configHelper.getMergedConfig(baseConfig,
      overrideConfig);


    Set<String> configsKeys = new HashSet<String>();
    configsKeys.addAll(baseConfig.keySet());
    configsKeys.addAll(overrideConfig.keySet());

    Assert.assertEquals(configsKeys.size(), mergedConfig.size());

    Assert.assertEquals(SERVICE_SITE_VAL1, mergedConfig.get(SERVICE_SITE_NAME1));
    Assert.assertEquals(SERVICE_SITE_VAL2_H, mergedConfig.get(SERVICE_SITE_NAME2));
    Assert.assertEquals(SERVICE_SITE_VAL3, mergedConfig.get(SERVICE_SITE_NAME3));
    Assert.assertEquals(SERVICE_SITE_VAL4, mergedConfig.get(SERVICE_SITE_NAME4));
    Assert.assertEquals(SERVICE_SITE_VAL5, mergedConfig.get(SERVICE_SITE_NAME5));
    Assert.assertEquals(SERVICE_SITE_VAL6_H, mergedConfig.get(SERVICE_SITE_NAME6));
  }
}
