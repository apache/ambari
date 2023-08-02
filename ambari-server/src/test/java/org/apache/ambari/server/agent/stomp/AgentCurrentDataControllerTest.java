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
package org.apache.ambari.server.agent.stomp;

import static org.apache.ambari.server.agent.DummyHeartbeatConstants.DATANODE;
import static org.apache.ambari.server.agent.DummyHeartbeatConstants.DummyCluster;
import static org.apache.ambari.server.agent.DummyHeartbeatConstants.DummyClusterId;
import static org.apache.ambari.server.agent.DummyHeartbeatConstants.DummyHostname1;
import static org.apache.ambari.server.agent.DummyHeartbeatConstants.DummyHostname2;
import static org.apache.ambari.server.agent.DummyHeartbeatConstants.HDFS;
import static org.apache.ambari.server.agent.DummyHeartbeatConstants.HDFS_CLIENT;
import static org.apache.ambari.server.agent.DummyHeartbeatConstants.JOBTRACKER;
import static org.apache.ambari.server.agent.DummyHeartbeatConstants.MAPREDUCE;
import static org.apache.ambari.server.agent.DummyHeartbeatConstants.NAMENODE;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.H2DatabaseCleaner;
import org.apache.ambari.server.actionmanager.ActionManager;
import org.apache.ambari.server.actionmanager.ActionManagerTestHelper;
import org.apache.ambari.server.agent.AgentSessionManager;
import org.apache.ambari.server.agent.HeartbeatTestHelper;
import org.apache.ambari.server.agent.RecoveryConfigComponent;
import org.apache.ambari.server.agent.stomp.dto.AlertCluster;
import org.apache.ambari.server.agent.stomp.dto.ClusterConfigs;
import org.apache.ambari.server.agent.stomp.dto.Hash;
import org.apache.ambari.server.agent.stomp.dto.HostLevelParamsCluster;
import org.apache.ambari.server.agent.stomp.dto.HostRepositories;
import org.apache.ambari.server.agent.stomp.dto.MetadataCluster;
import org.apache.ambari.server.agent.stomp.dto.TopologyCluster;
import org.apache.ambari.server.agent.stomp.dto.TopologyComponent;
import org.apache.ambari.server.agent.stomp.dto.TopologyHost;
import org.apache.ambari.server.audit.AuditLogger;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.ClusterRequest;
import org.apache.ambari.server.controller.ConfigurationRequest;
import org.apache.ambari.server.events.AgentConfigsUpdateEvent;
import org.apache.ambari.server.events.AlertDefinitionsAgentUpdateEvent;
import org.apache.ambari.server.events.HostLevelParamsUpdateEvent;
import org.apache.ambari.server.events.MetadataUpdateEvent;
import org.apache.ambari.server.events.TopologyUpdateEvent;
import org.apache.ambari.server.events.publishers.STOMPUpdatePublisher;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.OrmTestHelper;
import org.apache.ambari.server.orm.dao.AlertDefinitionDAO;
import org.apache.ambari.server.orm.entities.AlertDefinitionEntity;
import org.apache.ambari.server.security.TestAuthenticationFactory;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.ConfigFactory;
import org.apache.ambari.server.state.ConfigHelper;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.alert.Scope;
import org.apache.ambari.server.state.alert.SourceType;
import org.apache.ambari.server.state.configgroup.ConfigGroup;
import org.apache.ambari.server.state.configgroup.ConfigGroupFactory;
import org.apache.ambari.server.utils.EventBusSynchronizer;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;

public class AgentCurrentDataControllerTest {

  private static final Logger LOG = LoggerFactory.getLogger(AgentCurrentDataControllerTest.class);
  private Injector injector;
  private Clusters clusters;
  private ConfigFactory configFactory;
  private ConfigGroupFactory configGroupFactory;
  private AgentCurrentDataController agentCurrentDataController;

  private static final String AGENT_CONFIGS_DEFAULT_SECTION = "agentConfig";

  @Inject
  private Configuration config;

  @Inject
  private HeartbeatTestHelper heartbeatTestHelper;

  @Inject
  private ActionManagerTestHelper actionManagerTestHelper;

  @Inject
  private AuditLogger auditLogger;

  @Inject
  private OrmTestHelper helper;

  @Inject
  private AmbariManagementController managementController;

  @Inject
  private ConfigHelper configHelper;

  @Inject
  private AlertDefinitionDAO alertDefinitionDAO;

  private InMemoryDefaultTestModule module;

  @Before
  public void setup() throws Exception {
    module = HeartbeatTestHelper.getTestModule();
    injector = Guice.createInjector(module);
    injector.getInstance(GuiceJpaInitializer.class);
    clusters = injector.getInstance(Clusters.class);
    injector.injectMembers(this);
    agentCurrentDataController = new AgentCurrentDataController(injector);
    configFactory = injector.getInstance(ConfigFactory.class);
    configGroupFactory = injector.getInstance(ConfigGroupFactory.class);
    EasyMock.replay(auditLogger, injector.getInstance(STOMPUpdatePublisher.class));

    EventBusSynchronizer.synchronizeAmbariEventPublisher(injector);
    EventBusSynchronizer.synchronizeAlertEventPublisher(injector);
    EventBusSynchronizer.synchronizeSTOMPUpdatePublisher(injector);
  }

  @After
  public void teardown() throws Exception {
    H2DatabaseCleaner.clearDatabaseAndStopPersistenceService(injector);
    EasyMock.reset(auditLogger);
  }

  @Test
  public void testRegistrationRecoveryConfig() throws Exception {
    Cluster cluster = heartbeatTestHelper.getDummyCluster();

    AgentSessionManager agentSessionManager = injector.getInstance(AgentSessionManager.class);
    for (Host host : cluster.getHosts()) {
      agentSessionManager.register(host.getHostName(), host);
    }

    Service hdfs = heartbeatTestHelper.addService(cluster, HDFS);

    hdfs.addServiceComponent(DATANODE).setRecoveryEnabled(true);
    hdfs.getServiceComponent(DATANODE).addServiceComponentHost(DummyHostname1);

    hdfs.addServiceComponent(NAMENODE).setRecoveryEnabled(true);
    hdfs.getServiceComponent(NAMENODE).addServiceComponentHost(DummyHostname1);

    hdfs.addServiceComponent(HDFS_CLIENT);
    hdfs.getServiceComponent(HDFS_CLIENT).addServiceComponentHost(DummyHostname1);

    // Create helper after creating service to avoid race condition caused by asynchronous recovery configs
    // timestamp invalidation (RecoveryConfigHelper.handleServiceComponentInstalledEvent())
    ActionManager am = actionManagerTestHelper.getMockActionManager();
    replay(am);
    Clusters fsm = clusters;

    HostLevelParamsUpdateEvent hostLevelParamsUpdateEvent =
        agentCurrentDataController.getCurrentHostLevelParams(DummyHostname1, new Hash(""));

    assertNotNull(hostLevelParamsUpdateEvent.getHostLevelParamsClusters());
    assertEquals(1, hostLevelParamsUpdateEvent.getHostLevelParamsClusters().size());
    assertEquals(DummyClusterId, hostLevelParamsUpdateEvent.getHostLevelParamsClusters().keySet().iterator().next());

    HostLevelParamsCluster hostLevelParamsCluster =
        hostLevelParamsUpdateEvent.getHostLevelParamsClusters().get(DummyClusterId);

    List<RecoveryConfigComponent> recoveryConfigComponents =
        hostLevelParamsCluster.getRecoveryConfig().getEnabledComponents();

    List<String> enabledComponents = recoveryConfigComponents.stream().map(c -> c.getComponentName()).collect(Collectors.toList());

    assertEquals(2, enabledComponents.size());
    assertTrue(enabledComponents.contains(NAMENODE));
    assertTrue(enabledComponents.contains(DATANODE));
  }

  @Test
  public void testRegistrationAgentConfig() throws Exception {
    clusters.addHost(DummyHostname1);

    MetadataUpdateEvent metadataUpdateEvent =
        agentCurrentDataController.getCurrentMetadata(new Hash(""));

    assertNotNull(metadataUpdateEvent. getMetadataClusters());
    assertEquals(1, metadataUpdateEvent.getMetadataClusters().size());
    String clusterId = "-1";
    assertEquals(clusterId, metadataUpdateEvent.getMetadataClusters().keySet().iterator().next());

    MetadataCluster metadataCluster = metadataUpdateEvent.getMetadataClusters().get(clusterId);

    SortedMap<String, SortedMap<String, String>> config = metadataCluster.getAgentConfigs();
    assertFalse(config.isEmpty());
    assertTrue(config.containsKey(AGENT_CONFIGS_DEFAULT_SECTION));

    Map<String,String> defaultAgentConfigsMap = config.get(AGENT_CONFIGS_DEFAULT_SECTION);

    assertTrue("false".equals(defaultAgentConfigsMap.get(Configuration.CHECK_REMOTE_MOUNTS.getKey())));
    assertTrue(defaultAgentConfigsMap.containsKey(Configuration.CHECK_MOUNTS_TIMEOUT.getKey()));
    assertTrue("0".equals(defaultAgentConfigsMap.get(Configuration.CHECK_MOUNTS_TIMEOUT.getKey())));
    assertTrue("true".equals(defaultAgentConfigsMap.get(Configuration.ENABLE_AUTO_AGENT_CACHE_UPDATE.getKey())));
  }

  @Test
  public void testRegistrationAgentTopology() throws Exception {
    Cluster cluster = heartbeatTestHelper.getDummyCluster();

    clusters.addHost(DummyHostname2);
    clusters.mapHostToCluster(DummyHostname2, DummyCluster);

    Service hdfs = heartbeatTestHelper.addService(cluster, HDFS);
    hdfs.addServiceComponent(DATANODE).setRecoveryEnabled(true);
    hdfs.getServiceComponent(DATANODE).addServiceComponentHost(DummyHostname1);
    hdfs.getServiceComponent(DATANODE).addServiceComponentHost(DummyHostname2);

    hdfs.addServiceComponent(NAMENODE).setRecoveryEnabled(true);
    hdfs.getServiceComponent(NAMENODE).addServiceComponentHost(DummyHostname1);

    hdfs.addServiceComponent(HDFS_CLIENT);
    hdfs.getServiceComponent(HDFS_CLIENT).addServiceComponentHost(DummyHostname1);
    hdfs.getServiceComponent(HDFS_CLIENT).addServiceComponentHost(DummyHostname2);

    TopologyUpdateEvent topologyUpdateEvent =
        agentCurrentDataController.getCurrentTopology(new Hash(""));

    assertNotNull(topologyUpdateEvent);
    assertNotNull(topologyUpdateEvent.getClusters());
    assertEquals(1, topologyUpdateEvent.getClusters().size());
    assertEquals(DummyClusterId, topologyUpdateEvent.getClusters().keySet().iterator().next());

    TopologyCluster topologyCluster = topologyUpdateEvent.getClusters().get(DummyClusterId);

    assertEquals("Initial topology should contain 3 service components (DataNode, NameNode, HdfsClient)",
        3, topologyCluster.getTopologyComponents().size());
    assertEquals("Initial topology should contain 2 hosts (host1, host2)",
        2, topologyCluster.getTopologyHosts().size());

    Set<TopologyComponent> topologyComponents = topologyCluster.getTopologyComponents();
    Set<TopologyHost> topologyHosts = topologyCluster.getTopologyHosts();

    assertTrue(topologyHosts.stream().anyMatch(h -> h.getHostName().equals(DummyHostname1)));
    assertTrue(topologyHosts.stream().anyMatch(h -> h.getHostName().equals(DummyHostname2)));

    Long host1Id = topologyHosts.stream().filter(h -> h.getHostName().equals(DummyHostname1)).findFirst().get().getHostId();
    Long host2Id = topologyHosts.stream().filter(h -> h.getHostName().equals(DummyHostname2)).findFirst().get().getHostId();

    assertTrue(topologyComponents.stream().anyMatch(c -> c.getComponentName().equals(NAMENODE)));
    assertTrue(topologyComponents.stream().anyMatch(c -> c.getComponentName().equals(DATANODE)));
    assertTrue(topologyComponents.stream().anyMatch(c -> c.getComponentName().equals(HDFS_CLIENT)));

    TopologyComponent namenodeTopologyComponent = topologyComponents.stream().filter(c -> c.getComponentName().equals(NAMENODE)).findFirst().get();
    TopologyComponent datanodeTopologyComponent = topologyComponents.stream().filter(c -> c.getComponentName().equals(DATANODE)).findFirst().get();
    TopologyComponent hdfsClientTopologyComponent = topologyComponents.stream().filter(c -> c.getComponentName().equals(HDFS_CLIENT)).findFirst().get();

    assertEquals(1, namenodeTopologyComponent.getHostIds().size());
    assertEquals(2, datanodeTopologyComponent.getHostIds().size());
    assertEquals(2, hdfsClientTopologyComponent.getHostIds().size());

    // hostnames are excess in topologyComponent, because we have hostId and hostname in topologyHost
    assertEquals(0, namenodeTopologyComponent.getHostNames().size());
    assertEquals(0, datanodeTopologyComponent.getHostNames().size());
    assertEquals(0, hdfsClientTopologyComponent.getHostNames().size());

    assertEquals(HDFS, namenodeTopologyComponent.getServiceName());
    assertEquals(HDFS, datanodeTopologyComponent.getServiceName());
    assertEquals(HDFS, hdfsClientTopologyComponent.getServiceName());

    assertTrue(namenodeTopologyComponent.getHostIds().contains(host1Id));
    assertTrue(datanodeTopologyComponent.getHostIds().contains(host1Id));
    assertTrue(datanodeTopologyComponent.getHostIds().contains(host2Id));
    assertTrue(hdfsClientTopologyComponent.getHostIds().contains(host1Id));
    assertTrue(hdfsClientTopologyComponent.getHostIds().contains(host2Id));
  }

  @Test
  public void testRegistrationAgentHostLevelParams() throws Exception {
    Cluster cluster = heartbeatTestHelper.getDummyCluster();

    clusters.addHost(DummyHostname2);
    clusters.mapHostToCluster(DummyHostname2, DummyCluster);

    AgentSessionManager agentSessionManager = injector.getInstance(AgentSessionManager.class);
    for (Host host : cluster.getHosts()) {
      agentSessionManager.register(host.getHostName(), host);
    }

    Service hdfs = heartbeatTestHelper.addService(cluster, HDFS);
    hdfs.addServiceComponent(DATANODE).setRecoveryEnabled(true);
    hdfs.getServiceComponent(DATANODE).addServiceComponentHost(DummyHostname1);
    hdfs.getServiceComponent(DATANODE).addServiceComponentHost(DummyHostname2);

    hdfs.addServiceComponent(NAMENODE).setRecoveryEnabled(true);
    hdfs.getServiceComponent(NAMENODE).addServiceComponentHost(DummyHostname1);

    hdfs.addServiceComponent(HDFS_CLIENT);
    hdfs.getServiceComponent(HDFS_CLIENT).addServiceComponentHost(DummyHostname1);
    hdfs.getServiceComponent(HDFS_CLIENT).addServiceComponentHost(DummyHostname2);

    HostLevelParamsUpdateEvent hostLevelParamsUpdateEvent =
        agentCurrentDataController.getCurrentHostLevelParams(DummyHostname1, new Hash(""));

    assertNotNull(hostLevelParamsUpdateEvent);
    assertEquals(1, hostLevelParamsUpdateEvent.getHostLevelParamsClusters().size());
    assertEquals(DummyClusterId, hostLevelParamsUpdateEvent.getHostLevelParamsClusters().keySet().iterator().next());

    HostLevelParamsCluster hostLevelParamsCluster =
        hostLevelParamsUpdateEvent.getHostLevelParamsClusters().get(DummyClusterId);

    HostRepositories hostRepositories = hostLevelParamsCluster.getHostRepositories();

    assertEquals(1, hostRepositories.getRepositories().size());
    assertEquals(3, hostRepositories.getComponentRepos().size());

    assertTrue(hostRepositories.getComponentRepos().keySet().contains(DATANODE));
    assertTrue(hostRepositories.getComponentRepos().keySet().contains(NAMENODE));
    assertTrue(hostRepositories.getComponentRepos().keySet().contains(HDFS_CLIENT));

    Long repoId = hostRepositories.getRepositories().firstKey();
    for (String componentName : hostRepositories.getComponentRepos().keySet()) {
      assertEquals(repoId, hostRepositories.getComponentRepos().get(componentName));
    }
  }

  @Test
  public void testRegistrationAgentConfigs() throws Exception {
    String testProperty = "ipc.client.connect.max.retries";
    String testPropertyValueDefaultGroup = "30";
    String testPropertyValueCustomGroup = "10";

    SecurityContextHolder.getContext().setAuthentication(TestAuthenticationFactory.createAdministrator("admin"));

    Cluster cluster = heartbeatTestHelper.getDummyCluster();

    clusters.addHost(DummyHostname2);
    clusters.mapHostToCluster(DummyHostname2, DummyCluster);
    clusters.getHosts().forEach(h -> clusters.updateHostMappings(h));

    AgentSessionManager agentSessionManager = injector.getInstance(AgentSessionManager.class);
    for (Host host : cluster.getHosts()) {
      agentSessionManager.register(host.getHostName(), host);
    }

    Service hdfs = heartbeatTestHelper.addService(cluster, HDFS);
    hdfs.addServiceComponent(DATANODE).setRecoveryEnabled(true);
    hdfs.getServiceComponent(DATANODE).addServiceComponentHost(DummyHostname1);
    hdfs.getServiceComponent(DATANODE).addServiceComponentHost(DummyHostname2);

    // create default config
    ConfigurationRequest cr1 = new ConfigurationRequest();
    cr1.setClusterName(DummyHostname1);
    cr1.setType("core-site");
    cr1.setVersionTag("version1");
    cr1.setProperties(Collections.singletonMap(testProperty, testPropertyValueDefaultGroup));

    final ClusterRequest clusterRequestDup =
        new ClusterRequest(cluster.getClusterId(), DummyCluster,
            cluster.getDesiredStackVersion().getStackVersion(), null);

    clusterRequestDup.setDesiredConfig(Collections.singletonList(cr1));
    managementController.updateClusters(new HashSet<ClusterRequest>() {{
      add(clusterRequestDup);
    }}, null);

    createConfigGroup(cluster, Arrays.asList(DummyHostname1), Collections.singletonMap(testProperty, testPropertyValueCustomGroup));
    configHelper.updateAgentConfigs(Collections.singleton(DummyCluster));

    // should be used config the value from config group
    AgentConfigsUpdateEvent agentConfigsUpdateEvent1 =
        agentCurrentDataController.getCurrentConfigs(DummyHostname1, new Hash(""));

    // should be used the general config value
    AgentConfigsUpdateEvent agentConfigsUpdateEvent2 =
        agentCurrentDataController.getCurrentConfigs(DummyHostname2, new Hash(""));

    assertNotNull(agentConfigsUpdateEvent1);
    assertNotNull(agentConfigsUpdateEvent2);

    Long host1Id = cluster.getHost(DummyHostname1).getHostId();
    Long host2Id = cluster.getHost(DummyHostname2).getHostId();
    assertEquals(host1Id, agentConfigsUpdateEvent1.getHostId());
    assertEquals(host2Id, agentConfigsUpdateEvent2.getHostId());

    assertEquals(1, agentConfigsUpdateEvent1.getClustersConfigs().size());
    assertEquals(1, agentConfigsUpdateEvent2.getClustersConfigs().size());

    assertEquals(DummyClusterId, agentConfigsUpdateEvent1.getClustersConfigs().firstKey());
    assertEquals(DummyClusterId, agentConfigsUpdateEvent2.getClustersConfigs().firstKey());

    ClusterConfigs clusterConfigs1 = agentConfigsUpdateEvent1.getClustersConfigs().get(DummyClusterId);
    ClusterConfigs clusterConfigs2 = agentConfigsUpdateEvent2.getClustersConfigs().get(DummyClusterId);

    assertEquals(2, clusterConfigs1.getConfigurations().size());
    assertEquals(2, clusterConfigs2.getConfigurations().size());

    assertEquals(testPropertyValueCustomGroup, clusterConfigs1.getConfigurations().get("core-site").get(testProperty));
    assertEquals(testPropertyValueDefaultGroup, clusterConfigs2.getConfigurations().get("core-site").get(testProperty));
  }

  @Test
  public void testRegistrationAgentAlerts() throws Exception {
    String datanodeAlertDefinitionName = "Alert Definition datanode";
    String jobtrackerAlertDefinitionName = "Alert Definition jobtracker";
    Cluster cluster = heartbeatTestHelper.getDummyCluster();

    clusters.addHost(DummyHostname2);
    clusters.mapHostToCluster(DummyHostname2, DummyCluster);
    clusters.getHosts().forEach(h -> clusters.updateHostMappings(h));

    AgentSessionManager agentSessionManager = injector.getInstance(AgentSessionManager.class);
    for (Host host : cluster.getHosts()) {
      agentSessionManager.register(host.getHostName(), host);
    }

    Service hdfs = heartbeatTestHelper.addService(cluster, HDFS);
    Service mapreduce = heartbeatTestHelper.addService(cluster, MAPREDUCE);
    hdfs.addServiceComponent(DATANODE);
    hdfs.getServiceComponent(DATANODE).addServiceComponentHost(DummyHostname1);

    mapreduce.addServiceComponent(JOBTRACKER);
    mapreduce.getServiceComponent(JOBTRACKER).addServiceComponentHost(DummyHostname2);

    AlertDefinitionEntity definition1 = new AlertDefinitionEntity();
    definition1.setDefinitionName(datanodeAlertDefinitionName);
    definition1.setServiceName(HDFS);
    definition1.setComponentName(DATANODE);
    definition1.setClusterId(cluster.getClusterId());
    definition1.setHash(UUID.randomUUID().toString());
    definition1.setScheduleInterval(60);
    definition1.setScope(Scope.HOST);
    definition1.setSource("{\"type\" : \"SCRIPT\"}");
    definition1.setSourceType(SourceType.SCRIPT);
    alertDefinitionDAO.create(definition1);

    AlertDefinitionEntity definition2 = new AlertDefinitionEntity();
    definition2.setDefinitionName(jobtrackerAlertDefinitionName);
    definition2.setServiceName(MAPREDUCE);
    definition2.setComponentName(JOBTRACKER);
    definition2.setClusterId(cluster.getClusterId());
    definition2.setHash(UUID.randomUUID().toString());
    definition2.setScheduleInterval(60);
    definition2.setScope(Scope.HOST);
    definition2.setSource("{\"type\" : \"SCRIPT\"}");
    definition2.setSourceType(SourceType.SCRIPT);
    alertDefinitionDAO.create(definition2);

    AlertDefinitionsAgentUpdateEvent alertDefinitionsAgentUpdateEvent1 =
        agentCurrentDataController.getAlertDefinitions(DummyHostname1, new Hash(""));
    AlertDefinitionsAgentUpdateEvent alertDefinitionsAgentUpdateEvent2 =
        agentCurrentDataController.getAlertDefinitions(DummyHostname2, new Hash(""));

    assertNotNull(alertDefinitionsAgentUpdateEvent1);
    assertNotNull(alertDefinitionsAgentUpdateEvent2);

    Long host1Id = cluster.getHost(DummyHostname1).getHostId();
    Long host2Id = cluster.getHost(DummyHostname2).getHostId();
    assertEquals(host1Id, alertDefinitionsAgentUpdateEvent1.getHostId());
    assertEquals(host2Id, alertDefinitionsAgentUpdateEvent2.getHostId());

    assertEquals(1, alertDefinitionsAgentUpdateEvent1.getClusters().size());
    assertEquals(1, alertDefinitionsAgentUpdateEvent2.getClusters().size());

    Long clusterId = Long.valueOf(DummyClusterId);
    assertEquals(clusterId, alertDefinitionsAgentUpdateEvent1.getClusters().keySet().iterator().next());
    assertEquals(clusterId, alertDefinitionsAgentUpdateEvent2.getClusters().keySet().iterator().next());

    AlertCluster alertCluster1 = alertDefinitionsAgentUpdateEvent1.getClusters().get(clusterId);
    AlertCluster alertCluster2 = alertDefinitionsAgentUpdateEvent2.getClusters().get(clusterId);

    assertEquals(1, alertCluster1.getAlertDefinitions().size());
    assertEquals(1, alertCluster2.getAlertDefinitions().size());

    assertEquals(1, alertCluster1.getAlertDefinitions().size());
    assertEquals(1, alertCluster2.getAlertDefinitions().size());

    assertEquals(datanodeAlertDefinitionName, alertCluster1.getAlertDefinitions().iterator().next().getName());
    assertEquals(jobtrackerAlertDefinitionName, alertCluster2.getAlertDefinitions().iterator().next().getName());
  }

  private ConfigGroup createConfigGroup(Cluster cluster, List<String> hostNames, Map<String, String> properties)
      throws AmbariException {
    Map<String, Map<String, String>> propertiesAttributes = new HashMap<>();
    Config config = configFactory.createNew(cluster, "core-site", "testversion", properties, propertiesAttributes);

    Map<String, Config> configs = new HashMap<>();

    Map<Long, Host> hosts = new HashMap<>();
    for (String hostName : hostNames) {
      Host host = clusters.getHost(hostName);
      hosts.put(host.getHostId(), host);
    }

    configs.put(config.getType(), config);

    ConfigGroup configGroup = configGroupFactory.createNew(cluster, "HDFS", "cg-test",
        "HDFS", "New HDFS configs for h1", configs, hosts);

    cluster.addConfigGroup(configGroup);
    return configGroup;
  }
}
