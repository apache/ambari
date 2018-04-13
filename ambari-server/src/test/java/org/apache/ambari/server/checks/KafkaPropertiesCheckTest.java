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
package org.apache.ambari.server.checks;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.controller.PrereqCheckRequest;
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.DesiredConfig;
import org.apache.ambari.server.state.RepositoryType;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceInfo;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.StackInfo;
import org.apache.ambari.server.state.repository.ClusterVersionSummary;
import org.apache.ambari.server.state.repository.VersionDefinitionXml;
import org.apache.ambari.server.state.stack.PrereqCheckStatus;
import org.apache.ambari.server.state.stack.PrerequisiteCheck;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.google.inject.Provider;


/**
 * Unit tests for KafkaPropertiesCheck
 *
 */
public class KafkaPropertiesCheckTest {


  private Clusters m_clusters = EasyMock.createMock(Clusters.class);
  private Map<String, String> m_configMap = new HashMap<>();

  @Mock
  private ClusterVersionSummary m_clusterVersionSummary;

  @Mock
  private VersionDefinitionXml m_vdfXml;

  @Mock
  private RepositoryVersionEntity m_repositoryVersion;

  private KafkaPropertiesCheck m_kafkaPropertiresCheck = null;
  final Map<String, Service> m_services = new HashMap<>();

  static String serviceName = "KAFKA";

  @Before
  public void setup() throws Exception {
    MockitoAnnotations.initMocks(this);

    m_configMap.put("inter.broker.protocol.version", "0.0.1");
    m_configMap.put("log.message.format.version", "1.0.0");

    Cluster cluster = EasyMock.createMock(Cluster.class);
    final AmbariMetaInfo m_ami = EasyMock.createMock(AmbariMetaInfo.class);
    final StackInfo stackInfo = EasyMock.createMock(StackInfo.class);
    final StackId stackId = new StackId("HDP", "2.3");
    final ServiceInfo serviceInfo = EasyMock.createMock(ServiceInfo.class);

    Config config = EasyMock.createMock(Config.class);
    final Map<String, Service> services = new HashMap<>();
    final Service service = EasyMock.createMock(Service.class);

    services.put(serviceName, service);

    Map<String, DesiredConfig> desiredMap = new HashMap<>();
    DesiredConfig dc = EasyMock.createMock(DesiredConfig.class);
    desiredMap.put("kafka-broker", dc);

    expect(dc.getTag()).andReturn("").anyTimes();
    expect(config.getProperties()).andReturn(m_configMap).anyTimes();
    expect(cluster.getServices()).andReturn(services).anyTimes();
    expect(cluster.getService(serviceName)).andReturn(service).anyTimes();
    expect(cluster.getDesiredConfigs()).andReturn(desiredMap).anyTimes();
    expect(cluster.getDesiredConfigByType((String) anyObject())).andReturn(config).anyTimes();
    expect(cluster.getConfig((String) anyObject(), (String) anyObject())).andReturn(config).anyTimes();
    expect(cluster.getDesiredStackVersion()).andReturn(stackId).anyTimes();
    expect(cluster.getCurrentStackVersion()).andReturn(stackId).anyTimes();
    expect(m_clusters.getCluster((String) anyObject())).andReturn(cluster).anyTimes();

    expect(m_ami.getStack((StackId) anyObject())).andReturn(stackInfo).anyTimes();
    expect(m_ami.getServices((String) anyObject(), (String) anyObject())).andReturn(new HashMap<String, ServiceInfo>()).anyTimes();
    expect(stackInfo.getService((String) anyObject())).andReturn(serviceInfo).anyTimes();
    expect(serviceInfo.getVersion()).andReturn("0.0.1.2.3").anyTimes();


    replay(m_ami, m_clusters, cluster, dc, config, stackInfo, serviceInfo);

    m_kafkaPropertiresCheck = new KafkaPropertiesCheck();
    m_kafkaPropertiresCheck.clustersProvider = new Provider<Clusters>() {
      @Override
      public Clusters get() {
        return m_clusters;
      }
    };

    m_kafkaPropertiresCheck.ambariMetaInfo = new Provider<AmbariMetaInfo>() {
      @Override
      public AmbariMetaInfo get() {
        return m_ami;
      }
    };

    m_services.clear();

    Mockito.when(m_repositoryVersion.getType()).thenReturn(RepositoryType.STANDARD);
    Mockito.when(m_repositoryVersion.getRepositoryXml()).thenReturn(m_vdfXml);
    Mockito.when(m_repositoryVersion.getVersion()).thenReturn("2.6.5.1");
    Mockito.when(m_vdfXml.getClusterSummary(Mockito.any(Cluster.class))).thenReturn(m_clusterVersionSummary);
    Mockito.when(m_clusterVersionSummary.getAvailableServiceNames()).thenReturn(m_services.keySet());
  }

  @Test
  public void testApplicable() throws Exception {

    final Service service = EasyMock.createMock(Service.class);
    m_services.put("KAFKA", service);

    Cluster cluster = m_clusters.getCluster("cluster");
    EasyMock.reset(cluster);
    expect(cluster.getServices()).andReturn(m_services).anyTimes();
    expect(cluster.getCurrentStackVersion()).andReturn(new StackId("HDP-2.3")).anyTimes();
    replay(cluster);

    PrereqCheckRequest request = new PrereqCheckRequest("cluster");
    request.setTargetRepositoryVersion(m_repositoryVersion);

    assertTrue(m_kafkaPropertiresCheck.isApplicable(request));
  }

  @Test
  public void testNotApplicable() throws Exception {

    final Service service = EasyMock.createMock(Service.class);
    m_services.put("HDFS", service);

    Cluster cluster = m_clusters.getCluster("cluster");
    EasyMock.reset(cluster);
    expect(cluster.getServices()).andReturn(m_services).anyTimes();
    expect(cluster.getCurrentStackVersion()).andReturn(new StackId("HDP-2.3")).anyTimes();
    replay(cluster);

    PrereqCheckRequest request = new PrereqCheckRequest("cluster");
    request.setTargetRepositoryVersion(m_repositoryVersion);

    assertFalse(m_kafkaPropertiresCheck.isApplicable(request));
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testMissingProps() throws Exception {

    m_configMap.clear();

    PrerequisiteCheck check = new PrerequisiteCheck(null, null);
    m_kafkaPropertiresCheck.perform(check, new PrereqCheckRequest("cluster"));
    assertEquals(PrereqCheckStatus.FAIL, check.getStatus());
    assertEquals("The following Kafka properties should be set properly: inter.broker.protocol.version and log.message.format.version", check.getFailReason());


    m_configMap.put("inter.broker.protocol.version", "0.0.2");
    m_configMap.put("log.message.format.version", "1.0.0");
    check = new PrerequisiteCheck(null, null);
    m_kafkaPropertiresCheck.perform(check, new PrereqCheckRequest("cluster"));
    assertEquals(PrereqCheckStatus.FAIL, check.getStatus());

    m_configMap.clear();

    m_configMap.put("inter.broker.protocol.version", "0.0.1");
    m_configMap.put("log.message.format.version", "1.0.0");
    check = new PrerequisiteCheck(null, null);
    m_kafkaPropertiresCheck.perform(check, new PrereqCheckRequest("cluster"));
    assertEquals(PrereqCheckStatus.PASS, check.getStatus());

  }

  @SuppressWarnings("unchecked")
  @Test
  public void testNormal() throws Exception {

    PrerequisiteCheck check = new PrerequisiteCheck(null, null);
    m_kafkaPropertiresCheck.perform(check, new PrereqCheckRequest("cluster"));

    assertEquals(PrereqCheckStatus.PASS, check.getStatus());

  }

}
