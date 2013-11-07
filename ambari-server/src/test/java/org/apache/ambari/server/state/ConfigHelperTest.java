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
package org.apache.ambari.server.state;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.persist.PersistService;
import com.google.inject.persist.Transactional;
import junit.framework.Assert;
import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.ClusterRequest;
import org.apache.ambari.server.controller.ConfigurationRequest;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.state.configgroup.ConfigGroup;
import org.apache.ambari.server.state.configgroup.ConfigGroupFactory;
import org.apache.ambari.server.state.host.HostImpl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class ConfigHelperTest {
  private Clusters clusters;
  private AmbariMetaInfo metaInfo;
  private Injector injector;
  private String clusterName;
  private Cluster cluster;
  private ConfigGroupFactory configGroupFactory;
  private ConfigFactory configFactory;
  private ConfigHelper configHelper;
  private AmbariManagementController managementController;

  @Before
  public void setup() throws  Exception {
    injector = Guice.createInjector(new InMemoryDefaultTestModule());
    injector.getInstance(GuiceJpaInitializer.class);
    clusters = injector.getInstance(Clusters.class);
    metaInfo = injector.getInstance(AmbariMetaInfo.class);
    configFactory = injector.getInstance(ConfigFactory.class);
    configGroupFactory = injector.getInstance(ConfigGroupFactory.class);
    configHelper = injector.getInstance(ConfigHelper.class);
    managementController = injector.getInstance(AmbariManagementController.class);

    metaInfo.init();
    clusterName = "c1";
    clusters.addCluster(clusterName);
    cluster = clusters.getCluster(clusterName);
    cluster.setDesiredStackVersion(new StackId("HDP-2.0.6"));
    Assert.assertNotNull(cluster);
    clusters.addHost("h1");
    clusters.addHost("h2");
    Assert.assertNotNull(clusters.getHost("h1"));
    Assert.assertNotNull(clusters.getHost("h2"));
    clusters.getHost("h1").persist();
    clusters.getHost("h2").persist();

    // core-site
    ConfigurationRequest cr = new ConfigurationRequest();
    cr.setClusterName(clusterName);
    cr.setType("core-site");
    cr.setVersionTag("version1");
    cr.setProperties(new HashMap<String, String>() {{
      put("ipc.client.connect.max.retries", "30");
      put("fs.trash.interval", "30");
    }});

    final ClusterRequest clusterRequest1 =
      new ClusterRequest(cluster.getClusterId(), clusterName,
        cluster.getDesiredStackVersion().getStackVersion(), null);

    clusterRequest1.setDesiredConfig(cr);
    managementController.updateClusters(new HashSet<ClusterRequest>()
    {{ add(clusterRequest1); }}, null);

    // global
    cr.setType("global");
    cr.setVersionTag("version1");
    cr.setProperties(new HashMap<String, String>() {{
      put("dfs_namenode_name_dir", "/hadoop/hdfs/namenode");
      put("namenode_heapsize", "1024");
    }});

    final ClusterRequest clusterRequest2 =
      new ClusterRequest(cluster.getClusterId(), clusterName,
        cluster.getDesiredStackVersion().getStackVersion(), null);

    clusterRequest2.setDesiredConfig(cr);
    managementController.updateClusters(new HashSet<ClusterRequest>()
    {{ add(clusterRequest2); }}, null);
  }

  @After
  public void tearDown() {
    injector.getInstance(PersistService.class).stop();
  }

  @Transactional
  private Long addConfigGroup(String name, String tag, List<String> hosts,
         List<Config> configs) throws AmbariException {

    Map<String, Host> hostMap = new HashMap<String, Host>();
    Map<String, Config> configMap = new HashMap<String, Config>();

    for (String hostname : hosts) {
      Host host = clusters.getHost(hostname);
      hostMap.put(host.getHostName(), host);
    }

    for (Config config : configs) {
      configMap.put(config.getType(), config);
    }

    ConfigGroup configGroup = configGroupFactory.createNew(cluster, name,
      tag, "", configMap, hostMap);

    configGroup.persist();
    cluster.addConfigGroup(configGroup);

    return configGroup.getId();
  }

  @Test
  public void testEffectiveTagsForHost() throws Exception {
    final Config config = new ConfigImpl("core-site");
    config.setVersionTag("version122");

    Map<String, String> properties = new HashMap<String, String>();
    properties.put("a", "b");
    properties.put("c", "d");
    config.setProperties(properties);

    Long groupId = addConfigGroup("g1", "t1", new ArrayList<String>() {{
      add("h1");}}, new ArrayList<Config>() {{ add(config); }});

    Assert.assertNotNull(groupId);

    Map<String, Map<String, String>> configTags = configHelper
      .getEffectiveDesiredTags(cluster, "h1");

    Assert.assertNotNull(configTags);
    Map<String, String> tagsWithOverrides = configTags.get("core-site");
    Assert.assertNotNull(tagsWithOverrides);
    Assert.assertTrue(tagsWithOverrides.containsKey(ConfigHelper.CLUSTER_DEFAULT_TAG));
    Assert.assertEquals("version1", tagsWithOverrides.get(ConfigHelper.CLUSTER_DEFAULT_TAG));
    Assert.assertTrue(tagsWithOverrides.containsKey(groupId.toString()));
    Assert.assertEquals("version122", tagsWithOverrides.get(groupId.toString()));
  }

  @Test
  public void testEffectivePropertiesWithOverrides() throws Exception {
    final Config config1 = new ConfigImpl("core-site");
    config1.setVersionTag("version122");

    Map<String, String> properties = new HashMap<String, String>();
    properties.put("a", "b");
    properties.put("c", "d");
    config1.setProperties(properties);

    final Config config2 = new ConfigImpl("global");
    config2.setVersionTag("version122");
    Map<String, String> properties2 = new HashMap<String, String>();
    properties2.put("namenode_heapsize", "1111");
    config2.setProperties(properties2);

    Long groupId = addConfigGroup("g1", "t1", new ArrayList<String>() {{
        add("h1");}}, new ArrayList<Config>() {{ add(config1); add(config2);
      }});

    Assert.assertNotNull(groupId);

    Map<String, Map<String, String>> propertyMap = configHelper
      .getEffectiveConfigProperties(cluster,
        configHelper.getEffectiveDesiredTags(cluster, "h1"));

    Assert.assertNotNull(propertyMap);
    Assert.assertTrue(propertyMap.containsKey("global"));
    Map<String, String> globalProps = propertyMap.get("global");
    Assert.assertEquals("1111", globalProps.get("namenode_heapsize"));
    Assert.assertEquals("/hadoop/hdfs/namenode", globalProps.get("dfs_namenode_name_dir"));
    Assert.assertTrue(propertyMap.containsKey("core-site"));
    Map<String, String> coreProps = propertyMap.get("core-site");
    Assert.assertTrue(coreProps.containsKey("a"));
    Assert.assertTrue(coreProps.containsKey("c"));
    Assert.assertEquals("30", coreProps.get("ipc.client.connect.max.retries"));
  }
}
