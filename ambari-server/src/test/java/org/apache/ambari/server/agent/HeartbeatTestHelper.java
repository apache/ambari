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
package org.apache.ambari.server.agent;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import com.google.inject.persist.UnitOfWork;
import junit.framework.Assert;
import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.Role;
import org.apache.ambari.server.RoleCommand;
import org.apache.ambari.server.actionmanager.ActionDBAccessor;
import org.apache.ambari.server.actionmanager.ActionManager;
import org.apache.ambari.server.actionmanager.Request;
import org.apache.ambari.server.actionmanager.RequestFactory;
import org.apache.ambari.server.actionmanager.Stage;
import org.apache.ambari.server.actionmanager.StageFactory;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.HostsMap;
import org.apache.ambari.server.events.publishers.AmbariEventPublisher;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.OrmTestHelper;
import org.apache.ambari.server.orm.dao.ClusterDAO;
import org.apache.ambari.server.orm.dao.HostDAO;
import org.apache.ambari.server.orm.dao.ResourceTypeDAO;
import org.apache.ambari.server.orm.dao.StackDAO;
import org.apache.ambari.server.orm.entities.ClusterEntity;
import org.apache.ambari.server.orm.entities.HostEntity;
import org.apache.ambari.server.orm.entities.ResourceEntity;
import org.apache.ambari.server.orm.entities.ResourceTypeEntity;
import org.apache.ambari.server.orm.entities.StackEntity;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.RepositoryVersionState;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.fsm.InvalidStateTransitionException;
import org.apache.ambari.server.state.svccomphost.ServiceComponentHostStartEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.apache.ambari.server.agent.DummyHeartbeatConstants.DummyCluster;
import static org.apache.ambari.server.agent.DummyHeartbeatConstants.DummyHostname1;
import static org.apache.ambari.server.agent.DummyHeartbeatConstants.DummyOSRelease;
import static org.apache.ambari.server.agent.DummyHeartbeatConstants.DummyOs;
import static org.apache.ambari.server.agent.DummyHeartbeatConstants.DummyStackId;
import static org.apache.ambari.server.agent.DummyHeartbeatConstants.HBASE;
import static org.easymock.EasyMock.createMockBuilder;
import static org.easymock.EasyMock.createNiceMock;

@Singleton
public class HeartbeatTestHelper {

  @Inject
  Clusters clusters;

  @Inject
  Injector injector;

  @Inject
  AmbariMetaInfo metaInfo;

  @Inject
  ActionDBAccessor actionDBAccessor;

  @Inject
  ClusterDAO clusterDAO;

  @Inject
  StackDAO stackDAO;

  @Inject
  UnitOfWork unitOfWork;

  @Inject
  ResourceTypeDAO resourceTypeDAO;

  @Inject
  OrmTestHelper helper;

  @Inject
  private HostDAO hostDAO;

  @Inject
  private StageFactory stageFactory;

  public final static StackId HDP_22_STACK = new StackId("HDP", "2.2.0");

  public static InMemoryDefaultTestModule getTestModule() {
    return new InMemoryDefaultTestModule(){

      @Override
      protected void configure() {
        getProperties().put("recovery.type", "FULL");
        getProperties().put("recovery.lifetime_max_count", "10");
        getProperties().put("recovery.max_count", "4");
        getProperties().put("recovery.window_in_minutes", "23");
        getProperties().put("recovery.retry_interval", "2");
        super.configure();
      }
    };
  }

  public HeartBeatHandler getHeartBeatHandler(ActionManager am, ActionQueue aq)
      throws InvalidStateTransitionException, AmbariException {
    HeartBeatHandler handler = new HeartBeatHandler(clusters, aq, am, injector);
    Register reg = new Register();
    HostInfo hi = new HostInfo();
    hi.setHostName(DummyHostname1);
    hi.setOS(DummyOs);
    hi.setOSRelease(DummyOSRelease);
    reg.setHostname(DummyHostname1);
    reg.setResponseId(0);
    reg.setHardwareProfile(hi);
    reg.setAgentVersion(metaInfo.getServerVersion());
    handler.handleRegistration(reg);
    return handler;
  }

  public ActionManager getMockActionManager() {
    ActionQueue actionQueueMock = createNiceMock(ActionQueue.class);
    Clusters clustersMock = createNiceMock(Clusters.class);
    Configuration configurationMock = createNiceMock(Configuration.class);

    ActionManager actionManager = createMockBuilder(ActionManager.class).
        addMockedMethod("getTasks").
        withConstructor((long)0, (long)0, actionQueueMock, clustersMock,
            actionDBAccessor, new HostsMap((String) null), unitOfWork,
            injector.getInstance(RequestFactory.class), configurationMock, createNiceMock(AmbariEventPublisher.class)).
        createMock();
    return actionManager;
  }

  public Cluster getDummyCluster()
      throws AmbariException {
    StackEntity stackEntity = stackDAO.find(HDP_22_STACK.getStackName(), HDP_22_STACK.getStackVersion());
    org.junit.Assert.assertNotNull(stackEntity);

    // Create the cluster
    ResourceTypeEntity resourceTypeEntity = new ResourceTypeEntity();
    resourceTypeEntity.setId(ResourceTypeEntity.CLUSTER_RESOURCE_TYPE);
    resourceTypeEntity.setName(ResourceTypeEntity.CLUSTER_RESOURCE_TYPE_NAME);
    resourceTypeEntity = resourceTypeDAO.merge(resourceTypeEntity);

    ResourceEntity resourceEntity = new ResourceEntity();
    resourceEntity.setResourceType(resourceTypeEntity);

    ClusterEntity clusterEntity = new ClusterEntity();
    clusterEntity.setClusterName(DummyCluster);
    clusterEntity.setClusterInfo("test_cluster_info1");
    clusterEntity.setResource(resourceEntity);
    clusterEntity.setDesiredStack(stackEntity);

    clusterDAO.create(clusterEntity);

    StackId stackId = new StackId(DummyStackId);

    Cluster cluster = clusters.getCluster(DummyCluster);

    cluster.setDesiredStackVersion(stackId);
    cluster.setCurrentStackVersion(stackId);
    helper.getOrCreateRepositoryVersion(stackId, stackId.getStackVersion());
    cluster.createClusterVersion(stackId, stackId.getStackVersion(), "admin",
        RepositoryVersionState.UPGRADING);

    Set<String> hostNames = new HashSet<String>(){{
      add(DummyHostname1);
    }};

    Map<String, String> hostAttributes = new HashMap<String, String>();
    hostAttributes.put("os_family", "redhat");
    hostAttributes.put("os_release_version", "6.3");

    List<HostEntity> hostEntities = new ArrayList<HostEntity>();
    for(String hostName : hostNames) {
      clusters.addHost(hostName);
      Host host = clusters.getHost(hostName);
      host.setHostAttributes(hostAttributes);
      host.persist();

      HostEntity hostEntity = hostDAO.findByName(hostName);
      Assert.assertNotNull(hostEntity);
      hostEntities.add(hostEntity);
    }
    clusterEntity.setHostEntities(hostEntities);
    clusters.mapHostsToCluster(hostNames, DummyCluster);

    return cluster;
  }

  public void populateActionDB(ActionDBAccessor db, String DummyHostname1, long requestId, long stageId) throws AmbariException {
    Stage s = stageFactory.createNew(requestId, "/a/b", DummyCluster, 1L, "heartbeat handler test",
        "clusterHostInfo", "commandParamsStage", "hostParamsStage");
    s.setStageId(stageId);
    String filename = null;
    s.addHostRoleExecutionCommand(DummyHostname1, Role.HBASE_MASTER,
        RoleCommand.START,
        new ServiceComponentHostStartEvent(Role.HBASE_MASTER.toString(),
            DummyHostname1, System.currentTimeMillis()), DummyCluster, HBASE, false, false);
    List<Stage> stages = new ArrayList<Stage>();
    stages.add(s);
    Request request = new Request(stages, clusters);
    db.persistActions(request);
  }

}
