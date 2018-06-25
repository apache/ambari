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
package org.apache.ambari.server.state.host;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.Map;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.controller.MaintenanceStateHelper;
import org.apache.ambari.server.events.publishers.AmbariEventPublisher;
import org.apache.ambari.server.orm.dao.HostDAO;
import org.apache.ambari.server.orm.dao.HostStateDAO;
import org.apache.ambari.server.orm.entities.HostEntity;
import org.apache.ambari.server.orm.entities.HostStateEntity;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.ComponentInfo;
import org.apache.ambari.server.state.HostHealthStatus;
import org.apache.ambari.server.state.MaintenanceState;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.State;
import org.easymock.EasyMockSupport;
import org.junit.Test;
import org.powermock.reflect.Whitebox;

import com.google.common.collect.ImmutableList;
import com.google.gson.Gson;

public class HostImplTest extends EasyMockSupport {

  private AmbariMetaInfo ambariMetaInfo = createNiceMock(AmbariMetaInfo.class);

  @Test
  public void testGetHostAttributes() {
    HostEntity hostEntity = createNiceMock(HostEntity.class);
    HostStateEntity hostStateEntity = createNiceMock(HostStateEntity.class);
    HostDAO hostDAO  = createNiceMock(HostDAO.class);
    HostStateDAO hostStateDAO  = createNiceMock(HostStateDAO.class);

    Gson gson = new Gson();

    expect(hostEntity.getHostAttributes()).andReturn("{\"foo\": \"aaa\", \"bar\":\"bbb\"}").anyTimes();
    expect(hostEntity.getHostId()).andReturn(1L).anyTimes();
    expect(hostEntity.getHostName()).andReturn("host1").anyTimes();
    expect(hostEntity.getHostStateEntity()).andReturn(hostStateEntity).anyTimes();
    expect(hostDAO.findById(1L)).andReturn(hostEntity).atLeastOnce();

    replayAll();
    HostImpl host = new HostImpl(hostEntity, gson, hostDAO, hostStateDAO);

    Map<String, String> hostAttributes = host.getHostAttributes();
    assertEquals("aaa", hostAttributes.get("foo"));
    assertEquals("bbb", hostAttributes.get("bar"));

    host = new HostImpl(hostEntity, gson, hostDAO, hostStateDAO);

    hostAttributes = host.getHostAttributes();
    assertEquals("aaa", hostAttributes.get("foo"));
    assertEquals("bbb", hostAttributes.get("bar"));

    verifyAll();
  }

  @Test
  public void testGetHealthStatus() {

    HostEntity hostEntity = createNiceMock(HostEntity.class);
    HostStateEntity hostStateEntity = createNiceMock(HostStateEntity.class);
    HostDAO hostDAO  = createNiceMock(HostDAO.class);
    HostStateDAO hostStateDAO  = createNiceMock(HostStateDAO.class);

    Gson gson = new Gson();

    expect(hostEntity.getHostAttributes()).andReturn("{\"foo\": \"aaa\", \"bar\":\"bbb\"}").anyTimes();
    expect(hostEntity.getHostName()).andReturn("host1").anyTimes();
    expect(hostEntity.getHostId()).andReturn(1L).anyTimes();
    expect(hostEntity.getHostStateEntity()).andReturn(hostStateEntity).anyTimes();
    expect(hostDAO.findById(1L)).andReturn(hostEntity).anyTimes();
    expect(hostStateDAO.findByHostId(1L)).andReturn(hostStateEntity).atLeastOnce();

    replayAll();
    HostImpl host = new HostImpl(hostEntity, gson, hostDAO, hostStateDAO);

    host.getHealthStatus();

    host = new HostImpl(hostEntity, gson, hostDAO, hostStateDAO);

    host.getHealthStatus();

    verifyAll();
  }

  @Test
  public void canProcessComponentsFromMultipleStacks() throws AmbariException {
    // GIVEN
    Long clusterId = 1L;
    StackId hdpCore = new StackId("HDPCORE", "1.0.0");
    StackId ods = new StackId("ODS", "2.0.0");
    String hostName = "c7401.ambari.apache.org";

    Clusters clusters = createNiceMock(Clusters.class);
    Cluster cluster = createNiceMock(Cluster.class);
    HostEntity entity = createNiceMock(HostEntity.class);
    Gson gson = new Gson();
    HostDAO hostDAO = createNiceMock(HostDAO.class);
    HostStateDAO hostStateDAO = createNiceMock(HostStateDAO.class);
    HostStateEntity hostStateEntity = createNiceMock(HostStateEntity.class);
    MaintenanceStateHelper maintenanceStateHelper = createNiceMock(MaintenanceStateHelper.class);
    AmbariEventPublisher ambariEventPublisher = createNiceMock(AmbariEventPublisher.class);

    expect(clusters.getCluster(clusterId)).andReturn(cluster).anyTimes();
    expect(entity.getHostStateEntity()).andReturn(hostStateEntity).anyTimes();
    expect(entity.getClusterEntities()).andReturn(ImmutableList.of()).anyTimes();
    expect(entity.getHostId()).andReturn(1L).anyTimes();
    expect(entity.getHostName()).andReturn(hostName).anyTimes();
    expect(cluster.getDesiredStackVersion()).andReturn(hdpCore).anyTimes();
    expect(maintenanceStateHelper.getEffectiveState(anyObject(), anyObject())).andReturn(MaintenanceState.OFF).anyTimes();
    ambariEventPublisher.publish(anyObject());
    expectLastCall().anyTimes();

    List<ServiceComponentHost> components = ImmutableList.of(
      aComponent(hdpCore, "HDFS", "NAMENODE"),
      aComponent(ods, "HBASE", "HBASE_MASTER"),
      aComponent(hdpCore, "ZOOKEEPER", "ZOOKEEPER_SERVER"),
      aComponent(ods, "HBASE", "HBASE_REGIONSERVER")
    );
    expect(cluster.getServiceComponentHosts(hostName)).andReturn(components).anyTimes();

    replayAll();

    HostImpl subject = new HostImpl(entity, gson, hostDAO, hostStateDAO);
    Whitebox.setInternalState(subject, "ambariEventPublisher", ambariEventPublisher);
    Whitebox.setInternalState(subject, "clusters", clusters);
    Whitebox.setInternalState(subject, "ambariMetaInfo", ambariMetaInfo);
    Whitebox.setInternalState(subject, "maintenanceStateHelper", maintenanceStateHelper);
    subject.setStatus("INIT");

    // WHEN
    subject.calculateHostStatus(clusterId);

    // THEN
    assertEquals(HostHealthStatus.HealthStatus.HEALTHY.name(), subject.getStatus());
  }

  private ServiceComponentHost aComponent(StackId stackId, String service, String component) throws AmbariException {
    ServiceComponentHost sch = createNiceMock(ServiceComponentHost.class);
    expect(sch.getDesiredStackId()).andReturn(stackId).anyTimes();
    expect(sch.getServiceName()).andReturn(service).anyTimes();
    expect(sch.getServiceComponentName()).andReturn(component).anyTimes();
    expect(sch.getState()).andReturn(State.STARTED).anyTimes();
    ComponentInfo componentInfo = createNiceMock(ComponentInfo.class);
    expect(componentInfo.getCategory()).andReturn("MASTER").anyTimes();
    expect(ambariMetaInfo.getComponent(stackId.getStackName(), stackId.getStackVersion(), service, component)).andReturn(componentInfo);
    return sch;
  }
}
