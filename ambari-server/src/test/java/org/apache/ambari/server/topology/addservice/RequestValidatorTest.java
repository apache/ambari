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
package org.apache.ambari.server.topology.addservice;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.actionmanager.ActionManager;
import org.apache.ambari.server.actionmanager.RequestFactory;
import org.apache.ambari.server.controller.AddServiceRequest;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.internal.Stack;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.ConfigHelper;
import org.apache.ambari.server.state.SecurityType;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.topology.ConfigRecommendationStrategy;
import org.apache.ambari.server.topology.Configuration;
import org.apache.ambari.server.topology.SecurityConfiguration;
import org.apache.ambari.server.topology.StackFactory;
import org.easymock.EasyMockSupport;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

public class RequestValidatorTest extends EasyMockSupport {

  private final AddServiceRequest request = createNiceMock(AddServiceRequest.class);
  private final Cluster cluster = createMock(Cluster.class);
  private final AmbariManagementController controller = createNiceMock(AmbariManagementController.class);
  private final ConfigHelper configHelper = createMock(ConfigHelper.class);
  private final StackFactory stackFactory = createNiceMock(StackFactory.class);
  private final RequestValidator validator = new RequestValidator(request, cluster, controller, configHelper, stackFactory);

  @Before
  public void setUp() {
    validator.setState(RequestValidator.State.INITIAL);
    expect(cluster.getClusterName()).andReturn("TEST").anyTimes();
    expect(cluster.getServices()).andStubReturn(ImmutableMap.of());
    expect(request.getServices()).andStubReturn(ImmutableSet.of());
    expect(request.getComponents()).andStubReturn(ImmutableSet.of());
  }

  @After
  public void tearDown() {
    resetAll();
  }

  @Test
  public void cannotConstructInvalidRequestInfo() {
    assertThrows(IllegalStateException.class, () -> validator.createValidServiceInfo(null, null));

    Stack stack = simpleMockStack();
    Map<String, Map<String, Set<String>>> newServices = someNewServices();
    Configuration config = Configuration.newEmpty();

    validator.setState(RequestValidator.State.INITIAL.with(stack));
    assertThrows(IllegalStateException.class, () -> validator.createValidServiceInfo(null, null));
    validator.setState(validator.getState().with(config));
    assertThrows(IllegalStateException.class, () -> validator.createValidServiceInfo(null, null));

    validator.setState(RequestValidator.State.INITIAL.withNewServices(newServices));
    assertThrows(IllegalStateException.class, () -> validator.createValidServiceInfo(null, null));
    validator.setState(validator.getState().with(stack));
    assertThrows(IllegalStateException.class, () -> validator.createValidServiceInfo(null, null));

    validator.setState(RequestValidator.State.INITIAL.with(config));
    assertThrows(IllegalStateException.class, () -> validator.createValidServiceInfo(null, null));
    validator.setState(validator.getState().withNewServices(newServices));
    assertThrows(IllegalStateException.class, () -> validator.createValidServiceInfo(null, null));
  }

  @Test
  public void canConstructValidRequestInfo() {
    validator.setState(
      RequestValidator.State.INITIAL
        .withNewServices(someNewServices())
        .with(simpleMockStack())
        .with(Configuration.newEmpty())
    );
    ActionManager actionManager = createNiceMock(ActionManager.class);
    RequestFactory requestFactory = createNiceMock(RequestFactory.class);
    replayAll();

    AddServiceInfo addServiceInfo = validator.createValidServiceInfo(actionManager, requestFactory);
    assertNotNull(addServiceInfo);
    assertSame(request, addServiceInfo.getRequest());
    assertEquals(cluster.getClusterName(), addServiceInfo.clusterName());
    assertSame(validator.getState().getConfig(), addServiceInfo.getConfig());
    assertSame(validator.getState().getStack(), addServiceInfo.getStack());
    assertEquals(validator.getState().getNewServices(), addServiceInfo.newServices());
  }

  @Test
  public void cannotConstructTwice() {
    ActionManager actionManager = createNiceMock(ActionManager.class);
    RequestFactory requestFactory = createNiceMock(RequestFactory.class);
    replayAll();

    validator.setState(
      RequestValidator.State.INITIAL
        .withNewServices(someNewServices())
        .with(simpleMockStack())
        .with(Configuration.newEmpty())
    );
    validator.createValidServiceInfo(actionManager, requestFactory);
    assertThrows(IllegalStateException.class, () -> validator.createValidServiceInfo(actionManager, requestFactory));
  }

  @Test
  public void reportsUnknownStackFromRequest() throws Exception {
    StackId requestStackId = new StackId("HDP", "123");
    expect(request.getStackId()).andReturn(Optional.of(requestStackId)).anyTimes();
    expect(stackFactory.createStack(requestStackId.getStackName(), requestStackId.getStackVersion(), controller)).andThrow(new AmbariException("Stack not found"));
    replayAll();

    IllegalArgumentException e = assertThrows(IllegalArgumentException.class, validator::validateStack);
    assertTrue(e.getMessage().contains(requestStackId.toString()));
    assertNull(validator.getState().getStack());
  }

  @Test
  public void reportsUnknownStackFromCluster() throws Exception {
    StackId clusterStackId = new StackId("CLUSTER", "555");
    expect(request.getStackId()).andReturn(Optional.empty()).anyTimes();
    expect(cluster.getCurrentStackVersion()).andReturn(clusterStackId);
    expect(stackFactory.createStack(clusterStackId.getStackName(), clusterStackId.getStackVersion(), controller)).andThrow(new AmbariException("Stack not found"));
    replayAll();

    IllegalStateException e = assertThrows(IllegalStateException.class, validator::validateStack);
    assertTrue(e.getMessage().contains(clusterStackId.toString()));
    assertNull(validator.getState().getStack());
  }

  @Test
  public void useClusterStackIfAbsentInRequest() throws Exception {
    StackId clusterStackId = new StackId("CLUSTER", "123");
    Stack expectedStack = createNiceMock(Stack.class);
    expect(request.getStackId()).andReturn(Optional.empty()).anyTimes();
    expect(cluster.getCurrentStackVersion()).andReturn(clusterStackId);
    expect(stackFactory.createStack(clusterStackId.getStackName(), clusterStackId.getStackVersion(), controller)).andReturn(expectedStack);
    replayAll();

    validator.validateStack();

    assertSame(expectedStack, validator.getState().getStack());
  }

  @Test
  public void acceptsKnownServices() {
    expect(request.getServices()).andReturn(ImmutableSet.of(AddServiceRequest.Service.of("KAFKA")));
    validator.setState(RequestValidator.State.INITIAL.with(simpleMockStack()));
    replayAll();

    validator.validateServicesAndComponents();

    Map<String, Map<String, Set<String>>> expectedNewServices = ImmutableMap.of(
      "KAFKA", ImmutableMap.of()
    );
    assertEquals(expectedNewServices, validator.getState().getNewServices());
  }

  @Test
  public void acceptsKnownComponents() {
    expect(request.getComponents()).andReturn(ImmutableSet.of(AddServiceRequest.Component.of("KAFKA_BROKER", "c7401.ambari.apache.org")));
    validator.setState(RequestValidator.State.INITIAL.with(simpleMockStack()));
    replayAll();

    validator.validateServicesAndComponents();

    Map<String, Map<String, Set<String>>> expectedNewServices = ImmutableMap.of(
      "KAFKA", ImmutableMap.of("KAFKA_BROKER", ImmutableSet.of("c7401.ambari.apache.org"))
    );
    assertEquals(expectedNewServices, validator.getState().getNewServices());
  }

  @Test
  public void rejectsUnknownService() {
    String serviceName = "UNKNOWN_SERVICE";
    expect(request.getServices()).andReturn(ImmutableSet.of(AddServiceRequest.Service.of(serviceName)));
    validator.setState(RequestValidator.State.INITIAL.with(simpleMockStack()));
    replayAll();

    IllegalArgumentException e = assertThrows(IllegalArgumentException.class, validator::validateServicesAndComponents);
    assertTrue(e.getMessage().contains(serviceName));
    assertNull(validator.getState().getNewServices());
  }

  @Test
  public void rejectsUnknownComponent() {
    String componentName = "UNKNOWN_COMPONENT";
    expect(request.getComponents()).andReturn(ImmutableSet.of(AddServiceRequest.Component.of(componentName, "c7401.ambari.apache.org")));
    validator.setState(RequestValidator.State.INITIAL.with(simpleMockStack()));
    replayAll();

    IllegalArgumentException e = assertThrows(IllegalArgumentException.class, validator::validateServicesAndComponents);
    assertTrue(e.getMessage().contains(componentName));
    assertNull(validator.getState().getNewServices());
  }

  @Test
  public void rejectsExistingServiceForService() {
    String serviceName = "KAFKA";
    expect(cluster.getServices()).andReturn(ImmutableMap.of(serviceName, createNiceMock(Service.class))).anyTimes();
    expect(request.getServices()).andReturn(ImmutableSet.of(AddServiceRequest.Service.of(serviceName)));
    validator.setState(RequestValidator.State.INITIAL.with(simpleMockStack()));
    replayAll();

    IllegalArgumentException e = assertThrows(IllegalArgumentException.class, validator::validateServicesAndComponents);
    assertTrue(e.getMessage().contains(serviceName));
    assertNull(validator.getState().getNewServices());
  }

  @Test
  public void rejectsExistingServiceForComponent() {
    String serviceName = "KAFKA";
    String componentName = "KAFKA_BROKER";
    expect(cluster.getServices()).andReturn(ImmutableMap.of(serviceName, createNiceMock(Service.class))).anyTimes();
    expect(request.getComponents()).andReturn(ImmutableSet.of(AddServiceRequest.Component.of(componentName, "c7401.ambari.apache.org")));
    validator.setState(RequestValidator.State.INITIAL.with(simpleMockStack()));
    replayAll();

    IllegalArgumentException e = assertThrows(IllegalArgumentException.class, validator::validateServicesAndComponents);
    assertTrue(e.getMessage().contains(serviceName));
    assertTrue(e.getMessage().contains(componentName));
    assertNull(validator.getState().getNewServices());
  }

  @Test
  public void rejectsEmptyServiceAndComponentList() {
    replayAll();

    assertThrows(IllegalArgumentException.class, validator::validateServicesAndComponents);
    assertNull(validator.getState().getNewServices());
  }

  @Test
  public void acceptsKnownHosts() {
    Set<String> requestHosts = ImmutableSet.of("c7401.ambari.apache.org", "c7402.ambari.apache.org");
    Set<String> otherHosts = ImmutableSet.of("c7403.ambari.apache.org", "c7404.ambari.apache.org");
    Set<String> clusterHosts = Sets.union(requestHosts, otherHosts);
    expect(cluster.getHostNames()).andReturn(clusterHosts).anyTimes();
    validator.setState(RequestValidator.State.INITIAL.withNewServices(ImmutableMap.of(
      "KAFKA", ImmutableMap.of("KAFKA_BROKER", requestHosts)
    )));
    replayAll();

    validator.validateHosts();
  }

  @Test
  public void rejectsUnknownHosts() {
    Set<String> clusterHosts = ImmutableSet.of("c7401.ambari.apache.org", "c7402.ambari.apache.org");
    Set<String> otherHosts = ImmutableSet.of("c7403.ambari.apache.org", "c7404.ambari.apache.org");
    Set<String> requestHosts = ImmutableSet.copyOf(Sets.union(clusterHosts, otherHosts));
    expect(cluster.getHostNames()).andReturn(clusterHosts).anyTimes();
    validator.setState(RequestValidator.State.INITIAL.withNewServices(ImmutableMap.of(
      "KAFKA", ImmutableMap.of("KAFKA_BROKER", requestHosts)
    )));
    replayAll();

    IllegalArgumentException e = assertThrows(IllegalArgumentException.class, validator::validateHosts);
    assertTrue(e.getMessage(), e.getMessage().contains("host"));
  }

  @Test
  public void acceptsAbsentSecurityWhenClusterHasKerberos() {
    expect(cluster.getSecurityType()).andReturn(SecurityType.KERBEROS).anyTimes();
    expect(request.getSecurity()).andReturn(Optional.empty()).anyTimes();
    replayAll();

    validator.validateSecurity();
  }

  @Test
  public void acceptsAbsentSecurityWhenClusterHasNone() {
    expect(cluster.getSecurityType()).andReturn(SecurityType.NONE).anyTimes();
    expect(request.getSecurity()).andReturn(Optional.empty()).anyTimes();
    replayAll();

    validator.validateSecurity();
  }

  @Test
  public void acceptsMatchingKerberosSecurity() {
    expect(cluster.getSecurityType()).andReturn(SecurityType.KERBEROS).anyTimes();
    expect(request.getSecurity()).andReturn(Optional.of(new SecurityConfiguration(SecurityType.KERBEROS))).anyTimes();
    replayAll();

    validator.validateSecurity();
  }

  @Test
  public void acceptsMatchingNoneSecurity() {
    expect(cluster.getSecurityType()).andReturn(SecurityType.NONE).anyTimes();
    expect(request.getSecurity()).andReturn(Optional.of(SecurityConfiguration.NONE)).anyTimes();
    replayAll();

    validator.validateSecurity();
  }

  @Test
  public void rejectsNoneSecurityWhenClusterHasKerberos() {
    expect(cluster.getSecurityType()).andReturn(SecurityType.KERBEROS).anyTimes();
    expect(request.getSecurity()).andReturn(Optional.of(SecurityConfiguration.NONE)).anyTimes();
    replayAll();

    IllegalArgumentException e = assertThrows(IllegalArgumentException.class, validator::validateSecurity);
    assertTrue(e.getMessage().contains("KERBEROS"));
  }

  @Test
  public void rejectsKerberosSecurityWhenClusterHasNone() {
    expect(cluster.getSecurityType()).andReturn(SecurityType.NONE).anyTimes();
    expect(request.getSecurity()).andReturn(Optional.of(new SecurityConfiguration(SecurityType.KERBEROS))).anyTimes();
    replayAll();

    assertThrows(IllegalArgumentException.class, validator::validateSecurity);
    IllegalArgumentException e = assertThrows(IllegalArgumentException.class, validator::validateSecurity);
    assertTrue(e.getMessage().contains("KERBEROS"));
  }

  @Test
  public void combinesRequestConfigWithCluster() throws AmbariException {
    Configuration requestConfig = Configuration.newEmpty();
    requestConfig.setProperty("kafka-broker", "zookeeper.connect", "zookeeper.connect:request");
    requestConfig.setProperty("kafka-env", "custom_property", "custom_property:request");
    expect(request.getConfiguration()).andReturn(requestConfig.copy()).anyTimes();
    expect(request.getRecommendationStrategy()).andReturn(ConfigRecommendationStrategy.NEVER_APPLY).anyTimes();

    Configuration clusterConfig = Configuration.newEmpty();
    clusterConfig.setProperty("zookeeper-env", "zk_user", "zk_user:cluster_level");
    expect(configHelper.calculateExistingConfigs(cluster)).andReturn(clusterConfig.asPair()).anyTimes();

    Stack stack = simpleMockStack();
    Configuration stackConfig = Configuration.newEmpty();
    stackConfig.setProperty("zookeeper-env", "zk_user", "zk_user:stack_default");
    stackConfig.setProperty("zookeeper-env", "zk_log_dir", "zk_log_dir:stack_default");
    stackConfig.setProperty("kafka-broker", "zookeeper.connect", "zookeeper.connect:stack_default");
    expect(stack.getDefaultConfig()).andReturn(stackConfig).anyTimes();

    replayAll();

    validator.setState(RequestValidator.State.INITIAL.with(stack));
    validator.validateConfiguration();

    Configuration config = validator.getState().getConfig();
    verifyConfigOverrides(requestConfig, clusterConfig, Configuration.newEmpty() /* instead of stack config */, config);
  }

  @Test
  public void combinesRequestConfigWithClusterAndStack() throws AmbariException {
    Configuration requestConfig = Configuration.newEmpty();
    requestConfig.setProperty("kafka-broker", "zookeeper.connect", "zookeeper.connect:request");
    requestConfig.setProperty("kafka-env", "custom_property", "custom_property:request");
    expect(request.getConfiguration()).andReturn(requestConfig.copy()).anyTimes();
    expect(request.getRecommendationStrategy()).andReturn(ConfigRecommendationStrategy.ALWAYS_APPLY).anyTimes();

    Configuration clusterConfig = Configuration.newEmpty();
    clusterConfig.setProperty("zookeeper-env", "zk_user", "zk_user:cluster_level");
    expect(configHelper.calculateExistingConfigs(cluster)).andReturn(clusterConfig.asPair()).anyTimes();

    Stack stack = simpleMockStack();
    Configuration stackConfig = Configuration.newEmpty();
    stackConfig.setProperty("zookeeper-env", "zk_user", "zk_user:stack_default");
    stackConfig.setProperty("zookeeper-env", "zk_log_dir", "zk_log_dir:stack_default");
    stackConfig.setProperty("kafka-broker", "zookeeper.connect", "zookeeper.connect:stack_default");
    expect(stack.getDefaultConfig()).andReturn(stackConfig).anyTimes();

    replayAll();

    validator.setState(RequestValidator.State.INITIAL.with(stack));
    validator.validateConfiguration();

    Configuration config = validator.getState().getConfig();
    verifyConfigOverrides(requestConfig, clusterConfig, stackConfig, config);
  }

  @Test
  public void rejectsKerberosEnvChange() {
    Configuration requestConfig = Configuration.newEmpty();
    requestConfig.setProperty("kerberos-env", "some-property", "some-value");
    expect(request.getConfiguration()).andReturn(requestConfig.copy()).anyTimes();
    replayAll();

    assertThrows(IllegalArgumentException.class, validator::validateConfiguration);
  }

  @Test
  public void rejectsKrb5ConfChange() {
    Configuration requestConfig = Configuration.newEmpty();
    requestConfig.setProperty("krb5-conf", "some-property", "some-value");
    expect(request.getConfiguration()).andReturn(requestConfig.copy()).anyTimes();
    replayAll();

    assertThrows(IllegalArgumentException.class, validator::validateConfiguration);
  }

  private static void verifyConfigOverrides(Configuration requestConfig, Configuration clusterConfig, Configuration stackConfig, Configuration actualConfig) {
    requestConfig.getProperties().forEach(
      (type, properties) -> properties.forEach(
        (propertyName, propertyValue) -> assertEquals(type + "/" + propertyName, propertyValue, actualConfig.getPropertyValue(type, propertyName))
      )
    );
    clusterConfig.getProperties().forEach(
      (type, properties) -> properties.forEach(
        (propertyName, propertyValue) -> {
          if (!requestConfig.isPropertySet(type, propertyName)) {
            assertEquals(type + "/" + propertyName, propertyValue, actualConfig.getPropertyValue(type, propertyName));
          }
        }
      )
    );
    stackConfig.getProperties().forEach(
      (type, properties) -> properties.forEach(
        (propertyName, propertyValue) -> {
          if (!requestConfig.isPropertySet(type, propertyName) && !clusterConfig.isPropertySet(type, propertyName)) {
            assertEquals(type + "/" + propertyName, propertyValue, actualConfig.getPropertyValue(type, propertyName));
          }
        }
      )
    );
  }

  private Stack simpleMockStack() {
    Stack stack = createNiceMock(Stack.class);
    Set<String> stackServices = ImmutableSet.of("KAFKA", "ZOOKEEPER");
    expect(stack.getServices()).andReturn(stackServices).anyTimes();
    expect(stack.getServiceForComponent("KAFKA_BROKER")).andReturn("KAFKA").anyTimes();
    expect(stack.getServiceForComponent("ZOOKEEPER_SERVER")).andReturn("ZOOKEEPER").anyTimes();
    expect(stack.getServiceForComponent("ZOOKEEPER_CLIENT")).andReturn("ZOOKEEPER").anyTimes();
    return stack;
  }

  private static Map<String, Map<String, Set<String>>> someNewServices() {
    return ImmutableMap.of(
      "KAFKA", ImmutableMap.of("KAFKA_BROKER", ImmutableSet.of("c7401.ambari.apache.org"))
    );
  }

  private static <T extends Throwable> T assertThrows(Class<T> expectedException, Runnable code) {
    try {
      code.run();
    } catch (Throwable t) {
      if (expectedException.isInstance(t)) {
        return expectedException.cast(t);
      }
      throw new AssertionError("Expected exception: " + expectedException + " but " + t.getClass() + " was thrown instead");
    }

    throw new AssertionError("Expected exception: " + expectedException + ", but was not thrown");
  }

}
