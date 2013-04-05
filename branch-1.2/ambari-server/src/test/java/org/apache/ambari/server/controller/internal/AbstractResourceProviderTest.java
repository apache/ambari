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

package org.apache.ambari.server.controller.internal;

import org.apache.ambari.server.controller.ActionRequest;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.ClusterRequest;
import org.apache.ambari.server.controller.ConfigurationRequest;
import org.apache.ambari.server.controller.HostRequest;
import org.apache.ambari.server.controller.RequestStatusRequest;
import org.apache.ambari.server.controller.ServiceComponentHostRequest;
import org.apache.ambari.server.controller.ServiceComponentRequest;
import org.apache.ambari.server.controller.ServiceRequest;
import org.apache.ambari.server.controller.TaskStatusRequest;
import org.apache.ambari.server.controller.UserRequest;
import org.apache.ambari.server.controller.spi.Resource;
import org.easymock.EasyMock;
import org.easymock.IArgumentMatcher;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.easymock.EasyMock.createMock;

/**
 * Resource provider tests.
 */
public class AbstractResourceProviderTest {

  @Test
  public void testCheckPropertyIds() {
    Set<String> propertyIds = new HashSet<String>();
    propertyIds.add("foo");
    propertyIds.add("cat1/foo");
    propertyIds.add("cat2/bar");
    propertyIds.add("cat2/baz");
    propertyIds.add("cat3/sub1/bam");
    propertyIds.add("cat4/sub2/sub3/bat");
    propertyIds.add("cat5/subcat5/map");

    Map<Resource.Type, String> keyPropertyIds = new HashMap<Resource.Type, String>();

    AmbariManagementController managementController = createMock(AmbariManagementController.class);

    AbstractResourceProvider provider =
        (AbstractResourceProvider) AbstractResourceProvider.getResourceProvider(
            Resource.Type.Service,
            propertyIds,
            keyPropertyIds,
            managementController);

    Set<String> unsupported = provider.checkPropertyIds(Collections.singleton("foo"));
    Assert.assertTrue(unsupported.isEmpty());

    // note that key is not in the set of known property ids.  We allow it if its parent is a known property.
    // this allows for Map type properties where we want to treat the entries as individual properties
    Assert.assertTrue(provider.checkPropertyIds(Collections.singleton("cat5/subcat5/map/key")).isEmpty());

    unsupported = provider.checkPropertyIds(Collections.singleton("bar"));
    Assert.assertEquals(1, unsupported.size());
    Assert.assertTrue(unsupported.contains("bar"));

    unsupported = provider.checkPropertyIds(Collections.singleton("cat1/foo"));
    Assert.assertTrue(unsupported.isEmpty());

    unsupported = provider.checkPropertyIds(Collections.singleton("cat1"));
    Assert.assertTrue(unsupported.isEmpty());
  }

  @Test
  public void testGetPropertyIds() {
    Set<String> propertyIds = new HashSet<String>();
    propertyIds.add("p1");
    propertyIds.add("foo");
    propertyIds.add("cat1/foo");
    propertyIds.add("cat2/bar");
    propertyIds.add("cat2/baz");
    propertyIds.add("cat3/sub1/bam");
    propertyIds.add("cat4/sub2/sub3/bat");

    Map<Resource.Type, String> keyPropertyIds = new HashMap<Resource.Type, String>();

    AmbariManagementController managementController = createMock(AmbariManagementController.class);

    AbstractResourceProvider provider =
        (AbstractResourceProvider) AbstractResourceProvider.getResourceProvider(
            Resource.Type.Service,
            propertyIds,
            keyPropertyIds,
            managementController);

    Set<String> supportedPropertyIds = provider.getPropertyIds();
    Assert.assertTrue(supportedPropertyIds.containsAll(propertyIds));
  }


  // ----- helper methods ----------------------------------------------------

  /**
   * Equals check that accounts for nulls.
   *
   * @param left   the left object
   * @param right  the right object
   *
   * @return true if the left and right object are equal or both null
   */
  private static boolean eq(Object left, Object right) {
    return  left == null ? right == null : right != null && left.equals(right);
  }


  // ----- inner classes -----------------------------------------------------

  /**
   * Utility class for getting various AmbariManagmentController request related matchers.
   */
  public static class Matcher
  {
    public static ClusterRequest getClusterRequest(
        Long clusterId, String clusterName, String stackVersion, Set<String> hostNames)
    {
      EasyMock.reportMatcher(new ClusterRequestMatcher(clusterId, clusterName, stackVersion, hostNames));
      return null;
    }

    public static ConfigurationRequest getConfigurationRequest(
        String clusterName, String type, String tag, Map<String, String> configs)
    {
      EasyMock.reportMatcher(new ConfigurationRequestMatcher(clusterName, type, tag, configs));
      return null;
    }

    public static RequestStatusRequest getRequestRequest(Long requestId)
    {
      EasyMock.reportMatcher(new RequestRequestMatcher(requestId));
      return null;
    }

    public static Set<ActionRequest> getActionRequestSet(String clusterName, String serviceName, String actionName)
    {
      EasyMock.reportMatcher(new ActionRequestSetMatcher(clusterName, serviceName, actionName));
      return null;
    }

    public static Set<ServiceComponentRequest> getComponentRequestSet(String clusterName, String serviceName,
                                                                      String componentName,
                                                                      Map<String, String> configVersions,
                                                                      String desiredState)
    {
      EasyMock.reportMatcher(new ComponentRequestSetMatcher(clusterName, serviceName, componentName,
          configVersions, desiredState));
      return null;
    }

    public static Set<ConfigurationRequest> getConfigurationRequestSet(String clusterName, String type,
                                                                       String tag, Map<String, String> configs)
    {
      EasyMock.reportMatcher(new ConfigurationRequestSetMatcher(clusterName, type, tag, configs));
      return null;
    }

    public static Set<HostRequest> getHostRequestSet(String hostname, String clusterName,
                                                     Map<String, String> hostAttributes)
    {
      EasyMock.reportMatcher(new HostRequestSetMatcher(hostname, clusterName, hostAttributes));
      return null;
    }

    public static Set<ServiceComponentHostRequest> getHostComponentRequestSet(
        String clusterName, String serviceName, String componentName, String hostName,
        Map<String, String> configVersions, String desiredState)
    {
      EasyMock.reportMatcher(new HostComponentRequestSetMatcher(
          clusterName, serviceName, componentName, hostName, configVersions, desiredState));
      return null;
    }

    public static Set<ServiceRequest> getServiceRequestSet(String clusterName, String serviceName,
                                                           Map<String, String> configVersions, String desiredState)
    {
      EasyMock.reportMatcher(new ServiceRequestSetMatcher(clusterName, serviceName, configVersions, desiredState));
      return null;
    }

    public static Set<TaskStatusRequest> getTaskRequestSet(Long requestId, Long taskId)
    {
      EasyMock.reportMatcher(new TaskRequestSetMatcher(requestId, taskId));
      return null;
    }

    public static Set<UserRequest> getUserRequestSet(String name)
    {
      EasyMock.reportMatcher(new UserRequestSetMatcher(name));
      return null;
    }
  }

  /**
   * Matcher for a ClusterRequest.
   */
  public static class ClusterRequestMatcher extends ClusterRequest implements IArgumentMatcher {

    public ClusterRequestMatcher(Long clusterId, String clusterName, String stackVersion, Set<String> hostNames) {
      super(clusterId, clusterName, stackVersion, hostNames);
    }

    @Override
    public boolean matches(Object o) {
      return o instanceof ClusterRequest &&
          eq(((ClusterRequest) o).getClusterId(), getClusterId()) &&
          eq(((ClusterRequest) o).getClusterName(), getClusterName()) &&
          eq(((ClusterRequest) o).getStackVersion(), getStackVersion()) &&
          eq(((ClusterRequest) o).getHostNames(), getHostNames());
    }

    @Override
    public void appendTo(StringBuffer stringBuffer) {
      stringBuffer.append("ClusterRequestMatcher(" + super.toString() + ")");
    }
  }

  /**
   * Matcher for a ConfigurationRequest.
   */
  public static class ConfigurationRequestMatcher extends ConfigurationRequest implements IArgumentMatcher {

    public ConfigurationRequestMatcher(String clusterName, String type, String tag, Map<String, String> configs) {
      super(clusterName, type, tag, configs);
    }

    @Override
    public boolean matches(Object o) {
      return o instanceof ConfigurationRequest &&
          eq(((ConfigurationRequest) o).getClusterName(), getClusterName()) &&
          eq(((ConfigurationRequest) o).getType(), getType()) &&
          eq(((ConfigurationRequest) o).getVersionTag(), getVersionTag()) &&
          eq(((ConfigurationRequest) o).getConfigs(), getConfigs());

    }

    @Override
    public void appendTo(StringBuffer stringBuffer) {
      stringBuffer.append("ConfigurationRequestMatcher(" + super.toString() + ")");
    }
  }

  /**
   * Matcher for a RequestStatusRequest.
   */
  public static class RequestRequestMatcher extends RequestStatusRequest implements IArgumentMatcher {

    public RequestRequestMatcher(Long requestId) {
      super(requestId, "");
    }

    @Override
    public boolean matches(Object o) {

      return o instanceof RequestStatusRequest &&
          eq(((RequestStatusRequest) o).getRequestId(), getRequestId());
    }

    @Override
    public void appendTo(StringBuffer stringBuffer) {
      stringBuffer.append("RequestRequestMatcher(" + super.toString() + ")");
    }
  }

  /**
   * Matcher for a ActionRequest set containing a single request.
   */
  public static class ActionRequestSetMatcher extends HashSet<ActionRequest> implements IArgumentMatcher {

    private final ActionRequest actionRequest;

    public ActionRequestSetMatcher(String clusterName, String serviceName, String actionName) {
      this.actionRequest = new ActionRequest(clusterName, serviceName, actionName, null);
      add(this.actionRequest);
    }

    @Override
    public boolean matches(Object o) {
      if (!(o instanceof Set)) {
        return false;
      }

      Set set = (Set) o;

      if (set.size() != 1) {
        return false;
      }

      Object request = set.iterator().next();

      return request instanceof ActionRequest &&
          eq(((ActionRequest) request).getClusterName(), actionRequest.getClusterName()) &&
          eq(((ActionRequest) request).getServiceName(), actionRequest.getServiceName()) &&
          eq(((ActionRequest) request).getActionName(), actionRequest.getActionName());
    }

    @Override
    public void appendTo(StringBuffer stringBuffer) {
      stringBuffer.append("ActionRequestSetMatcher(" + actionRequest + ")");
    }
  }

  /**
   * Matcher for a ServiceComponentRequest set containing a single request.
   */
  public static class ComponentRequestSetMatcher extends HashSet<ServiceComponentRequest> implements IArgumentMatcher {

    private final ServiceComponentRequest serviceComponentRequest;

    public ComponentRequestSetMatcher(String clusterName, String serviceName, String componentName,
                                   Map<String, String> configVersions, String desiredState) {
      this.serviceComponentRequest =
          new ServiceComponentRequest(clusterName, serviceName, componentName, configVersions, desiredState);
      add(this.serviceComponentRequest);
    }

    @Override
    public boolean matches(Object o) {

      if (!(o instanceof Set)) {
        return false;
      }

      Set set = (Set) o;

      if (set.size() != 1) {
        return false;
      }

      Object request = set.iterator().next();

      return request instanceof ServiceComponentRequest &&
          eq(((ServiceComponentRequest) request).getClusterName(), serviceComponentRequest.getClusterName()) &&
          eq(((ServiceComponentRequest) request).getServiceName(), serviceComponentRequest.getServiceName()) &&
          eq(((ServiceComponentRequest) request).getComponentName(), serviceComponentRequest.getComponentName()) &&
          eq(((ServiceComponentRequest) request).getConfigVersions(), serviceComponentRequest.getConfigVersions()) &&
          eq(((ServiceComponentRequest) request).getDesiredState(), serviceComponentRequest.getDesiredState());
    }

    @Override
    public void appendTo(StringBuffer stringBuffer) {
      stringBuffer.append("ComponentRequestSetMatcher(" + serviceComponentRequest + ")");
    }
  }

  /**
   * Matcher for a ConfigurationRequest set containing a single request.
   */
  public static class ConfigurationRequestSetMatcher extends HashSet<ConfigurationRequest> implements IArgumentMatcher {

    private final ConfigurationRequest configurationRequest;

    public ConfigurationRequestSetMatcher(String clusterName, String type, String tag, Map<String, String> configs) {
      this.configurationRequest = new ConfigurationRequest(clusterName, type, tag, configs);
      add(this.configurationRequest);
    }

    @Override
    public boolean matches(Object o) {

      if (!(o instanceof Set)) {
        return false;
      }

      Set set = (Set) o;

      if (set.size() != 1) {
        return false;
      }

      Object request = set.iterator().next();

      return request instanceof ConfigurationRequest &&
          eq(((ConfigurationRequest) request).getClusterName(), configurationRequest.getClusterName()) &&
          eq(((ConfigurationRequest) request).getType(), configurationRequest.getType()) &&
          eq(((ConfigurationRequest) request).getVersionTag(), configurationRequest.getVersionTag()) &&
          eq(((ConfigurationRequest) request).getConfigs(), configurationRequest.getConfigs());
    }

    @Override
    public void appendTo(StringBuffer stringBuffer) {
      stringBuffer.append("ConfigurationRequestSetMatcher(" + configurationRequest + ")");
    }
  }

  /**
   * Matcher for a HostRequest set containing a single request.
   */
  public static class HostRequestSetMatcher extends HashSet<HostRequest> implements IArgumentMatcher {

    private final HostRequest hostRequest;

    public HostRequestSetMatcher(String hostname, String clusterName, Map<String, String> hostAttributes) {
      this.hostRequest = new HostRequest(hostname, clusterName, hostAttributes);
      add(this.hostRequest);
    }

    @Override
    public boolean matches(Object o) {
      if (!(o instanceof Set)) {
        return false;
      }

      Set set = (Set) o;

      if (set.size() != 1) {
        return false;
      }

      Object request = set.iterator().next();

      return request instanceof HostRequest &&
          eq(((HostRequest) request).getClusterName(), hostRequest.getClusterName()) &&
          eq(((HostRequest) request).getHostname(), hostRequest.getHostname()) &&
          eq(((HostRequest) request).getHostAttributes(), hostRequest.getHostAttributes());
    }

    @Override
    public void appendTo(StringBuffer stringBuffer) {
      stringBuffer.append("HostRequestSetMatcher(" + hostRequest + ")");
    }
  }

  /**
   * Matcher for a ServiceComponentHostRequest set containing a single request.
   */
  public static class HostComponentRequestSetMatcher extends HashSet<ServiceComponentHostRequest>
      implements IArgumentMatcher {

    private final ServiceComponentHostRequest hostComponentRequest;

    public HostComponentRequestSetMatcher(String clusterName, String serviceName, String componentName, String hostName,
                                      Map<String, String> configVersions, String desiredState) {
      this.hostComponentRequest =
          new ServiceComponentHostRequest(clusterName, serviceName, componentName,
              hostName, configVersions, desiredState);
      add(this.hostComponentRequest);
    }

    @Override
    public boolean matches(Object o) {

      if (!(o instanceof Set)) {
        return false;
      }

      Set set = (Set) o;

      if (set.size() != 1) {
        return false;
      }

      Object request = set.iterator().next();

      return request instanceof ServiceComponentHostRequest &&
          eq(((ServiceComponentHostRequest) request).getClusterName(), hostComponentRequest.getClusterName()) &&
          eq(((ServiceComponentHostRequest) request).getServiceName(), hostComponentRequest.getServiceName()) &&
          eq(((ServiceComponentHostRequest) request).getComponentName(), hostComponentRequest.getComponentName()) &&
          eq(((ServiceComponentHostRequest) request).getHostname(), hostComponentRequest.getHostname()) &&
          eq(((ServiceComponentHostRequest) request).getConfigVersions(), hostComponentRequest.getConfigVersions()) &&
          eq(((ServiceComponentHostRequest) request).getDesiredState(), hostComponentRequest.getDesiredState());
    }

    @Override
    public void appendTo(StringBuffer stringBuffer) {
      stringBuffer.append("HostComponentRequestSetMatcher(" + hostComponentRequest + ")");
    }
  }

  /**
   * Matcher for a ServiceRequest set containing a single request.
   */
  public static class ServiceRequestSetMatcher extends HashSet<ServiceRequest> implements IArgumentMatcher {

    private final ServiceRequest serviceRequest;

    public ServiceRequestSetMatcher(
        String clusterName, String serviceName, Map<String, String> configVersions, String desiredState) {
      this.serviceRequest = new ServiceRequest(clusterName, serviceName, configVersions, desiredState);
      add(this.serviceRequest);
    }

    @Override
    public boolean matches(Object o) {
      if (!(o instanceof Set)) {
        return false;
      }

      Set set = (Set) o;

      if (set.size() != 1) {
        return false;
      }

      Object request = set.iterator().next();

      return request instanceof ServiceRequest &&
          eq(((ServiceRequest) request).getClusterName(), serviceRequest.getClusterName()) &&
          eq(((ServiceRequest) request).getServiceName(), serviceRequest.getServiceName()) &&
          eq(((ServiceRequest) request).getConfigVersions(), serviceRequest.getConfigVersions()) &&
          eq(((ServiceRequest) request).getDesiredState(), serviceRequest.getDesiredState());
    }

    @Override
    public void appendTo(StringBuffer stringBuffer) {
      stringBuffer.append("ServiceRequestSetMatcher(" + serviceRequest + ")");
    }
  }

  /**
   * Matcher for a TaskStatusRequest set containing a single request.
   */
  public static class TaskRequestSetMatcher extends HashSet<TaskStatusRequest> implements IArgumentMatcher {

    private final TaskStatusRequest taskStatusRequest;

    public TaskRequestSetMatcher(Long requestId, Long taskId) {
      this.taskStatusRequest = new TaskStatusRequest(requestId, taskId);
      add(this.taskStatusRequest);
    }

    @Override
    public boolean matches(Object o) {

      if (!(o instanceof Set)) {
        return false;
      }

      Set set = (Set) o;

      if (set.size() != 1) {
        return false;
      }

      Object request = set.iterator().next();

      return request instanceof TaskStatusRequest &&
          eq(((TaskStatusRequest) request).getRequestId(), taskStatusRequest.getRequestId());
    }

    @Override
    public void appendTo(StringBuffer stringBuffer) {
      stringBuffer.append("TaskRequestSetMatcher(" + taskStatusRequest + ")");
    }
  }

  /**
   * Matcher for a UserRequest set containing a single request.
   */
  public static class UserRequestSetMatcher extends HashSet<UserRequest> implements IArgumentMatcher {

    private final UserRequest userRequest;

    public UserRequestSetMatcher(String name) {
      this.userRequest = new UserRequest(name);
      add(this.userRequest);
    }

    @Override
    public boolean matches(Object o) {

      if (!(o instanceof Set)) {
        return false;
      }

      Set set = (Set) o;

      if (set.size() != 1) {
        return false;
      }

      Object request = set.iterator().next();

      return request instanceof UserRequest &&
          eq(((UserRequest) request).getUsername(), userRequest.getUsername());
    }

    @Override
    public void appendTo(StringBuffer stringBuffer) {
      stringBuffer.append("UserRequestSetMatcher(" + userRequest + ")");
    }
  }

  /**
   * A test observer that records the last event.
   */
  public static class TestObserver implements ResourceProviderObserver {

    ResourceProviderEvent lastEvent = null;

    @Override
    public void update(ResourceProviderEvent event) {
      lastEvent = event;
    }

    public ResourceProviderEvent getLastEvent() {
      return lastEvent;
    }
  }
}
