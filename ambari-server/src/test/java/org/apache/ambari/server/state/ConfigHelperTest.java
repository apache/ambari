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

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

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
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;



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
    cr.setPropertiesAttributes(new HashMap<String, Map<String, String>>() {{
      Map<String, String> attrs = new HashMap<String, String>();
      attrs.put("ipc.client.connect.max.retries", "1");
      attrs.put("fs.trash.interval", "2");
      put("attribute1", attrs);
    }});

    final ClusterRequest clusterRequest1 =
      new ClusterRequest(cluster.getClusterId(), clusterName,
        cluster.getDesiredStackVersion().getStackVersion(), null);

    clusterRequest1.setDesiredConfig(Collections.singletonList(cr));
    managementController.updateClusters(new HashSet<ClusterRequest>()
    {{ add(clusterRequest1); }}, null);

    // flume-conf

    ConfigurationRequest cr2 = new ConfigurationRequest();
    cr2.setClusterName(clusterName);
    cr2.setType("flume-conf");
    cr2.setVersionTag("version1");


    final ClusterRequest clusterRequest2 =
      new ClusterRequest(cluster.getClusterId(), clusterName,
        cluster.getDesiredStackVersion().getStackVersion(), null);

    clusterRequest2.setDesiredConfig(Collections.singletonList(cr2));
    managementController.updateClusters(new HashSet<ClusterRequest>()
    {{ add(clusterRequest2); }}, null);

    // global
    cr.setType("global");
    cr.setVersionTag("version1");
    cr.setProperties(new HashMap<String, String>() {{
      put("dfs_namenode_name_dir", "/hadoop/hdfs/namenode");
      put("namenode_heapsize", "1024");
    }});
    cr.setPropertiesAttributes(new HashMap<String, Map<String, String>>() {{
      Map<String, String> attrs = new HashMap<String, String>();
      attrs.put("dfs_namenode_name_dir", "3");
      attrs.put("namenode_heapsize", "4");
      put("attribute2", attrs);
    }});

    final ClusterRequest clusterRequest3 =
      new ClusterRequest(cluster.getClusterId(), clusterName,
        cluster.getDesiredStackVersion().getStackVersion(), null);

    clusterRequest3.setDesiredConfig(Collections.singletonList(cr));
    managementController.updateClusters(new HashSet<ClusterRequest>()
    {{ add(clusterRequest3); }}, null);
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
    config.setTag("version122");

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
    config1.setTag("version122");

    Map<String, String> properties = new HashMap<String, String>();
    properties.put("a", "b");
    properties.put("c", "d");
    config1.setProperties(properties);

    final Config config2 = new ConfigImpl("global");
    config2.setTag("version122");
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

  @Test
  public void testEffectivePropertiesAttributesWithOverrides() throws Exception {
    final Config config1 = new ConfigImpl("core-site");
    config1.setTag("version122");

    Map<String, String> attributes = new HashMap<String, String>();
    attributes.put("fs.trash.interval", "11");
    attributes.put("b", "y");
    Map<String, Map<String, String>> config1Attributes = new HashMap<String, Map<String, String>>();
    config1Attributes.put("attribute1", attributes);
    config1.setPropertiesAttributes(config1Attributes);

    final Config config2 = new ConfigImpl("global");
    config2.setTag("version122");
    attributes = new HashMap<String, String>();
    attributes.put("namenode_heapsize", "z");
    attributes.put("c", "q");
    Map<String, Map<String, String>> config2Attributes = new HashMap<String, Map<String, String>>();
    config2Attributes.put("attribute2", attributes);
    config2.setPropertiesAttributes(config2Attributes);

    Long groupId = addConfigGroup("g1", "t1", new ArrayList<String>() {{
      add("h1");}}, new ArrayList<Config>() {{ add(config1); add(config2);
    }});

    Assert.assertNotNull(groupId);

    Map<String, Map<String, Map<String, String>>> effectiveAttributes = configHelper
        .getEffectiveConfigAttributes(cluster,
          configHelper.getEffectiveDesiredTags(cluster, "h1"));

    Assert.assertNotNull(effectiveAttributes);
    Assert.assertEquals(3, effectiveAttributes.size());

    Assert.assertTrue(effectiveAttributes.containsKey("global"));
    Map<String, Map<String, String>> globalAttrs = effectiveAttributes.get("global");
    Assert.assertEquals(1, globalAttrs.size());
    Assert.assertTrue(globalAttrs.containsKey("attribute2"));
    Map<String, String> attribute2Occurances = globalAttrs.get("attribute2");
    Assert.assertEquals(3, attribute2Occurances.size());
    Assert.assertTrue(attribute2Occurances.containsKey("namenode_heapsize"));
    Assert.assertEquals("z", attribute2Occurances.get("namenode_heapsize"));
    Assert.assertTrue(attribute2Occurances.containsKey("dfs_namenode_name_dir"));
    Assert.assertEquals("3", attribute2Occurances.get("dfs_namenode_name_dir"));
    Assert.assertTrue(attribute2Occurances.containsKey("c"));
    Assert.assertEquals("q", attribute2Occurances.get("c"));

    Assert.assertTrue(effectiveAttributes.containsKey("core-site"));
    Map<String, Map<String, String>> coreAttrs = effectiveAttributes.get("core-site");
    Assert.assertEquals(1, coreAttrs.size());
    Assert.assertTrue(coreAttrs.containsKey("attribute1"));
    Map<String, String> attribute1Occurances = coreAttrs.get("attribute1");
    Assert.assertEquals(3, attribute1Occurances.size());
    Assert.assertTrue(attribute1Occurances.containsKey("ipc.client.connect.max.retries"));
    Assert.assertEquals("1", attribute1Occurances.get("ipc.client.connect.max.retries"));
    Assert.assertTrue(attribute1Occurances.containsKey("fs.trash.interval"));
    Assert.assertEquals("11", attribute1Occurances.get("fs.trash.interval"));
    Assert.assertTrue(attribute1Occurances.containsKey("b"));
    Assert.assertEquals("y", attribute1Occurances.get("b"));
  }

  @Test
  public void testCloneAttributesMap() throws Exception {
    // init
    Map<String, Map<String, String>> targetAttributesMap = new HashMap<String, Map<String, String>>();
    Map<String, String> attributesValues = new HashMap<String, String>();
    attributesValues.put("a", "1");
    attributesValues.put("b", "2");
    attributesValues.put("f", "3");
    attributesValues.put("q", "4");
    targetAttributesMap.put("attr", attributesValues);
    Map<String, Map<String, String>> sourceAttributesMap = new HashMap<String, Map<String, String>>();
    attributesValues = new HashMap<String, String>();
    attributesValues.put("a", "5");
    attributesValues.put("f", "6");
    sourceAttributesMap.put("attr", attributesValues);
    attributesValues = new HashMap<String, String>();
    attributesValues.put("f", "7");
    attributesValues.put("q", "8");
    sourceAttributesMap.put("attr1", attributesValues);

    // eval
    configHelper.cloneAttributesMap(sourceAttributesMap, targetAttributesMap);

    // verification
    Assert.assertEquals(2, targetAttributesMap.size());
    Assert.assertTrue(targetAttributesMap.containsKey("attr"));
    Assert.assertTrue(targetAttributesMap.containsKey("attr1"));
    Map<String, String> attributes = targetAttributesMap.get("attr");
    Assert.assertEquals(4, attributes.size());
    Assert.assertEquals("5", attributes.get("a"));
    Assert.assertEquals("2", attributes.get("b"));
    Assert.assertEquals("6", attributes.get("f"));
    Assert.assertEquals("4", attributes.get("q"));
    attributes = targetAttributesMap.get("attr1");
    Assert.assertEquals(2, attributes.size());
    Assert.assertEquals("7", attributes.get("f"));
    Assert.assertEquals("8", attributes.get("q"));
  }

  @Test
  public void testCloneAttributesMap_sourceIsNull() throws Exception {
    // init
    Map<String, Map<String, String>> targetAttributesMap = new HashMap<String, Map<String, String>>();
    Map<String, String> attributesValues = new HashMap<String, String>();
    attributesValues.put("a", "1");
    attributesValues.put("b", "2");
    attributesValues.put("f", "3");
    attributesValues.put("q", "4");
    targetAttributesMap.put("attr", attributesValues);
    Map<String, Map<String, String>> sourceAttributesMap = null;

    // eval
    configHelper.cloneAttributesMap(sourceAttributesMap, targetAttributesMap);

    // verification
    // No exception should be thrown
    // targetMap should not be changed
    Assert.assertEquals(1, targetAttributesMap.size());
    Assert.assertTrue(targetAttributesMap.containsKey("attr"));
    Map<String, String> attributes = targetAttributesMap.get("attr");
    Assert.assertEquals(4, attributes.size());
    Assert.assertEquals("1", attributes.get("a"));
    Assert.assertEquals("2", attributes.get("b"));
    Assert.assertEquals("3", attributes.get("f"));
    Assert.assertEquals("4", attributes.get("q"));
  }

  @Test
  public void testCloneAttributesMap_targetIsNull() throws Exception {
    // init
    Map<String, Map<String, String>> targetAttributesMap = null;
    Map<String, Map<String, String>> sourceAttributesMap = new HashMap<String, Map<String, String>>();
    Map<String, String> attributesValues = new HashMap<String, String>();
    attributesValues.put("a", "5");
    attributesValues.put("f", "6");
    sourceAttributesMap.put("attr", attributesValues);
    attributesValues = new HashMap<String, String>();
    attributesValues.put("f", "7");
    attributesValues.put("q", "8");
    sourceAttributesMap.put("attr1", attributesValues);

    // eval
    configHelper.cloneAttributesMap(sourceAttributesMap, targetAttributesMap);

    // verification
    // No exception should be thrown
    // sourceMap should not be changed
    Assert.assertEquals(2, sourceAttributesMap.size());
    Assert.assertTrue(sourceAttributesMap.containsKey("attr"));
    Assert.assertTrue(sourceAttributesMap.containsKey("attr1"));
    Map<String, String> attributes = sourceAttributesMap.get("attr");
    Assert.assertEquals(2, attributes.size());
    Assert.assertEquals("5", attributes.get("a"));
    Assert.assertEquals("6", attributes.get("f"));
    attributes = sourceAttributesMap.get("attr1");
    Assert.assertEquals(2, attributes.size());
    Assert.assertEquals("7", attributes.get("f"));
    Assert.assertEquals("8", attributes.get("q"));
  }

  @Test
  public void testMergeAttributes() throws Exception {
    Map<String, Map<String, String>> persistedAttributes = new HashMap<String, Map<String, String>>();
    Map<String, String> persistedFinalAttrs = new HashMap<String, String>();
    persistedFinalAttrs.put("a", "true");
    persistedFinalAttrs.put("c", "true");
    persistedFinalAttrs.put("d", "true");
    persistedAttributes.put("final", persistedFinalAttrs);
    Map<String, Map<String, String>> confGroupAttributes = new HashMap<String, Map<String, String>>();
    Map<String, String> confGroupFinalAttrs = new HashMap<String, String>();
    confGroupFinalAttrs.put("b", "true");
    confGroupAttributes.put("final", confGroupFinalAttrs);
    Map<String, String> confGroupProperties = new HashMap<String, String>();
    confGroupProperties.put("a", "any");
    confGroupProperties.put("b", "any");
    confGroupProperties.put("c", "any");

    Config overrideConfig = new ConfigImpl(cluster, "type", confGroupProperties, confGroupAttributes, injector);

    Map<String, Map<String, String>> result
        = configHelper.overrideAttributes(overrideConfig, persistedAttributes);

    Assert.assertNotNull(result);
    Assert.assertEquals(1, result.size());
    Map<String, String> finalResultAttributes = result.get("final");
    Assert.assertNotNull(finalResultAttributes);
    Assert.assertEquals(2, finalResultAttributes.size());
    Assert.assertEquals("true", finalResultAttributes.get("b"));
    Assert.assertEquals("true", finalResultAttributes.get("d"));
  }

  @Test
  public void testMergeAttributes_noAttributeOverrides() throws Exception {
    Map<String, Map<String, String>> persistedAttributes = new HashMap<String, Map<String, String>>();
    Map<String, String> persistedFinalAttrs = new HashMap<String, String>();
    persistedFinalAttrs.put("a", "true");
    persistedFinalAttrs.put("c", "true");
    persistedFinalAttrs.put("d", "true");
    persistedAttributes.put("final", persistedFinalAttrs);
    Map<String, Map<String, String>> confGroupAttributes = new HashMap<String, Map<String, String>>();
    Map<String, String> confGroupProperties = new HashMap<String, String>();
    confGroupProperties.put("a", "any");
    confGroupProperties.put("b", "any");
    confGroupProperties.put("c", "any");

    Config overrideConfig = new ConfigImpl(cluster, "type", confGroupProperties, confGroupAttributes, injector);

    Map<String, Map<String, String>> result
        = configHelper.overrideAttributes(overrideConfig, persistedAttributes);

    Assert.assertNotNull(result);
    Assert.assertEquals(1, result.size());
    Map<String, String> finalResultAttributes = result.get("final");
    Assert.assertNotNull(finalResultAttributes);
    Assert.assertEquals(1, finalResultAttributes.size());
    Assert.assertEquals("true", finalResultAttributes.get("d"));
  }

  @Test
  public void testMergeAttributes_nullAttributes() throws Exception {
    Map<String, Map<String, String>> persistedAttributes = new HashMap<String, Map<String, String>>();
    Map<String, String> persistedFinalAttrs = new HashMap<String, String>();
    persistedFinalAttrs.put("a", "true");
    persistedFinalAttrs.put("c", "true");
    persistedFinalAttrs.put("d", "true");
    persistedAttributes.put("final", persistedFinalAttrs);
    Map<String, String> confGroupProperties = new HashMap<String, String>();
    confGroupProperties.put("a", "any");
    confGroupProperties.put("b", "any");
    confGroupProperties.put("c", "any");

    Config overrideConfig = new ConfigImpl(cluster, "type", confGroupProperties, null, injector);

    Map<String, Map<String, String>> result
        = configHelper.overrideAttributes(overrideConfig, persistedAttributes);

    Assert.assertNotNull(result);
    Assert.assertEquals(1, result.size());
    Map<String, String> finalResultAttributes = result.get("final");
    Assert.assertNotNull(finalResultAttributes);
    Assert.assertEquals(3, finalResultAttributes.size());
    Assert.assertEquals("true", finalResultAttributes.get("a"));
    Assert.assertEquals("true", finalResultAttributes.get("c"));
    Assert.assertEquals("true", finalResultAttributes.get("d"));
  }

  @Test
  public void testMergeAttributes_nullProperties() throws Exception {
    Map<String, Map<String, String>> persistedAttributes = new HashMap<String, Map<String, String>>();
    Map<String, String> persistedFinalAttrs = new HashMap<String, String>();
    persistedFinalAttrs.put("a", "true");
    persistedFinalAttrs.put("c", "true");
    persistedFinalAttrs.put("d", "true");
    persistedAttributes.put("final", persistedFinalAttrs);
    Map<String, Map<String, String>> confGroupAttributes = new HashMap<String, Map<String, String>>();
    Map<String, String> confGroupFinalAttrs = new HashMap<String, String>();
    confGroupFinalAttrs.put("b", "true");
    confGroupAttributes.put("final", confGroupFinalAttrs);

    Config overrideConfig = new ConfigImpl(cluster, "type", null, confGroupAttributes, injector);

    Map<String, Map<String, String>> result
        = configHelper.overrideAttributes(overrideConfig, persistedAttributes);

    Assert.assertNotNull(result);
    Assert.assertEquals(1, result.size());
    Map<String, String> finalResultAttributes = result.get("final");
    Assert.assertNotNull(finalResultAttributes);
    Assert.assertEquals(4, finalResultAttributes.size());
    Assert.assertEquals("true", finalResultAttributes.get("a"));
    Assert.assertEquals("true", finalResultAttributes.get("b"));
    Assert.assertEquals("true", finalResultAttributes.get("c"));
    Assert.assertEquals("true", finalResultAttributes.get("d"));
  }

  @Test
  public void testCalculateIsStaleConfigs() throws Exception {

    Map<String, HostConfig> schReturn = new HashMap<String, HostConfig>();
    HostConfig hc = new HostConfig();
    // Put a different version to check for change
    hc.setDefaultVersionTag("version2");
    schReturn.put("flume-conf", hc);
    // set up mocks
    ServiceComponentHost sch = createNiceMock(ServiceComponentHost.class);
    // set up expectations
    expect(sch.getActualConfigs()).andReturn(schReturn).times(3);
    expect(sch.getHostName()).andReturn("h1").times(6);
    expect(sch.getClusterId()).andReturn(1l).times(3);
    expect(sch.getServiceName()).andReturn("FLUME").times(3);
    expect(sch.getServiceComponentName()).andReturn("FLUME_HANDLER").times(3);
    replay(sch);
    // Cluster level config changes
    Assert.assertTrue(configHelper.isStaleConfigs(sch));
    HostConfig hc2 = new HostConfig();
    hc2.setDefaultVersionTag("version1");
    schReturn.put("flume-conf", hc2);
    // invalidate cache to test new sch
    configHelper.invalidateStaleConfigsCache();
    // Cluster level same configs
    Assert.assertFalse(configHelper.isStaleConfigs(sch));
    // Cluster level same configs but group specific configs for host have been updated
    List<String> hosts = new ArrayList<String>();
    hosts.add("h1");
    List<Config> configs = new ArrayList<Config>();
    configs.add(new ConfigImpl("flume-conf"));
    addConfigGroup("configGroup1", "FLUME", hosts, configs);
    Assert.assertTrue(configHelper.isStaleConfigs(sch));
    verify(sch);
  }
}
