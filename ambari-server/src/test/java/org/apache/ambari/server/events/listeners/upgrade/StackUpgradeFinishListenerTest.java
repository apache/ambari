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
package org.apache.ambari.server.events.listeners.upgrade;

import static org.easymock.EasyMock.expect;

import java.util.HashMap;
import java.util.Map;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.events.StackUpgradeFinishEvent;
import org.apache.ambari.server.events.publishers.VersionEventPublisher;
import org.apache.ambari.server.metadata.RoleCommandOrderProvider;
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
import org.apache.ambari.server.orm.entities.UpgradeEntity;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.easymock.EasyMockRunner;
import org.easymock.EasyMockSupport;
import org.easymock.Mock;
import org.easymock.MockType;
import org.easymock.TestSubject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.inject.Provider;


/**
 * StackVersionListener tests.
 */
@RunWith(EasyMockRunner.class)
public class StackUpgradeFinishListenerTest extends EasyMockSupport {

  private static final String INVALID_NEW_VERSION = "1.2.3.4-5678";
  private static final String VALID_NEW_VERSION = "2.4.0.0-1000";
  private static final String SERVICE_COMPONENT_NAME = "Some component name";
  private static final String SERVICE_NAME = "Service name";
  private static final Long CLUSTER_ID = 1L;
  private static final String UNKNOWN_VERSION = "UNKNOWN";
  private static final String VALID_PREVIOUS_VERSION = "2.2.0.0";
  private static final RepositoryVersionEntity DUMMY_REPOSITORY_VERSION_ENTITY = new RepositoryVersionEntity();
  private static final UpgradeEntity DUMMY_UPGRADE_ENTITY = new UpgradeEntity();
  public static final String STACK_NAME = "HDP-2.4.0.0";
  public static final String STACK_VERSION = "2.4.0.0";

  private Cluster cluster;
  private ServiceComponentHost sch;
  private Service service;
  private ServiceComponent serviceComponent;
  private VersionEventPublisher publisher = new VersionEventPublisher();

  @TestSubject
  private StackUpgradeFinishListener listener = new StackUpgradeFinishListener(publisher);

  @Mock(type = MockType.NICE)
  private Provider<RoleCommandOrderProvider> roleCommandOrderProviderProviderMock;

  @Before
  public void setup() throws Exception {
    cluster = createNiceMock(Cluster.class);
    serviceComponent = createNiceMock(ServiceComponent.class);
    service = createNiceMock(Service.class);
    Map<String, Service> services = new HashMap<>();
    services.put("mock_service",service);
    Map<String, ServiceComponent> components = new HashMap<>();
    components.put("mock_component", serviceComponent);

    expect(cluster.getServices()).andReturn(services);
    expect(service.getServiceComponents()).andReturn(components);
    serviceComponent.updateComponentInfo();
    service.updateServiceInfo();
  }

  @Test
  public void testupdateComponentInfo() throws AmbariException {
    replayAll();

    sendEventAndVerify();
  }


  private void sendEventAndVerify() {
    StackUpgradeFinishEvent event = new StackUpgradeFinishEvent(cluster);
    listener.onAmbariEvent(event);

    verifyAll();
  }
}
