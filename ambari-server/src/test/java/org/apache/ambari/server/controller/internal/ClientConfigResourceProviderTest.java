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

import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.*;
import org.apache.ambari.server.controller.spi.*;
import org.apache.ambari.server.controller.utilities.PredicateBuilder;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.state.*;
import org.apache.ambari.server.state.PropertyInfo;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.File;
import java.io.PrintWriter;
import java.util.*;

import static org.easymock.EasyMock.*;

/**
 * TaskResourceProvider tests.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest( {ClientConfigResourceProvider.class} )
public class ClientConfigResourceProviderTest {
  @Test
  public void testCreateResources() throws Exception {
    Resource.Type type = Resource.Type.ClientConfig;

    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    RequestStatusResponse response = createNiceMock(RequestStatusResponse.class);

    // replay
    replay(managementController, response);

    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
        type,
        PropertyHelper.getPropertyIds(type),
        PropertyHelper.getKeyPropertyIds(type),
        managementController);

    // add the property map to a set for the request.  add more maps for multiple creates
    Set<Map<String, Object>> propertySet = new LinkedHashSet<Map<String, Object>>();

    Map<String, Object> properties = new LinkedHashMap<String, Object>();

    // add properties to the request map
    properties.put(ClientConfigResourceProvider.COMPONENT_CLUSTER_NAME_PROPERTY_ID, "c1");
    properties.put(ClientConfigResourceProvider.COMPONENT_COMPONENT_NAME_PROPERTY_ID, "HDFS_CLIENT");
    properties.put(ClientConfigResourceProvider.COMPONENT_SERVICE_NAME_PROPERTY_ID, "HDFS");

    propertySet.add(properties);

    // create the request
    Request request = PropertyHelper.getCreateRequest(propertySet, null);

    try {
      provider.createResources(request);
      Assert.fail("Expected an UnsupportedOperationException");
    } catch (SystemException e) {
      // expected
    }

    // verify
    verify(managementController, response);
  }

  @Test
  public void testUpdateResources() throws Exception {
    Resource.Type type = Resource.Type.ClientConfig;

    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    RequestStatusResponse response = createNiceMock(RequestStatusResponse.class);

    // replay
    replay(managementController, response);

    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
        type,
        PropertyHelper.getPropertyIds(type),
        PropertyHelper.getKeyPropertyIds(type),
        managementController);

    // add the property map to a set for the request.
    Map<String, Object> properties = new LinkedHashMap<String, Object>();

    // create the request
    Request request = PropertyHelper.getUpdateRequest(properties, null);

    Predicate predicate = new PredicateBuilder().property(ClientConfigResourceProvider.COMPONENT_CLUSTER_NAME_PROPERTY_ID).equals("c1").
        toPredicate();

    try {
      provider.updateResources(request, predicate);
      Assert.fail("Expected an UnsupportedOperationException");
    } catch (SystemException e) {
      // expected
    }

    // verify
    verify(managementController, response);
  }

  @Test
  public void testGetResources() throws Exception {
    Resource.Type type = Resource.Type.ClientConfig;

    AmbariManagementController managementController = createNiceMock(AmbariManagementController.class);
    Clusters clusters = createNiceMock(Clusters.class);

    Cluster cluster = createNiceMock(Cluster.class);
    AmbariMetaInfo ambariMetaInfo = createNiceMock(AmbariMetaInfo.class);
    StackId stackId = createNiceMock(StackId.class);
    ComponentInfo componentInfo = createNiceMock(ComponentInfo.class);
    ServiceInfo serviceInfo = createNiceMock(ServiceInfo.class);
    CommandScriptDefinition commandScriptDefinition = createNiceMock(CommandScriptDefinition.class);
    Config clusterConfig = createNiceMock(Config.class);
    Host host = createNiceMock(Host.class);
    Service service = createNiceMock(Service.class);
    ServiceComponent serviceComponent = createNiceMock(ServiceComponent.class);
    ServiceComponentHost serviceComponentHost = createNiceMock(ServiceComponentHost.class);
    ServiceOsSpecific serviceOsSpecific = createNiceMock(ServiceOsSpecific.class);
    ConfigHelper configHelper = createNiceMock(ConfigHelper.class);

    File mockFile = PowerMock.createNiceMock(File.class);
    Runtime runtime = createMock(Runtime.class);
    Process process = createNiceMock(Process.class);

    Collection<Config> clusterConfigs = new HashSet<Config>();
    //Config clusterConfig = new ConfigImpl("config");
    clusterConfigs.add(clusterConfig);
    Map<String, Map<String, String>> allConfigTags = new HashMap<String, Map<String, String>>();
    Map<String, Map<String, String>> properties = new HashMap<String, Map<String, String>>();
    Map<String, Map<String, String>> configTags = new HashMap<String,
            Map<String, String>>();
    Map<String, Map<String, Map<String, String>>> attributes = new HashMap<String,
            Map<String, Map<String, String>>>();

    ClientConfigFileDefinition clientConfigFileDefinition = new ClientConfigFileDefinition();
    clientConfigFileDefinition.setDictionaryName("pig-env");
    clientConfigFileDefinition.setFileName("pig-env.sh");
    clientConfigFileDefinition.setType("env");
    List <ClientConfigFileDefinition> clientConfigFileDefinitionList = new LinkedList<ClientConfigFileDefinition>();
    clientConfigFileDefinitionList.add(clientConfigFileDefinition);

    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
        type,
        PropertyHelper.getPropertyIds(type),
        PropertyHelper.getKeyPropertyIds(type),
        managementController);

    // create the request
    Request request = PropertyHelper.getReadRequest(ClientConfigResourceProvider.COMPONENT_CLUSTER_NAME_PROPERTY_ID, "c1",
            ClientConfigResourceProvider.COMPONENT_COMPONENT_NAME_PROPERTY_ID,
            ClientConfigResourceProvider.COMPONENT_SERVICE_NAME_PROPERTY_ID);

    Predicate predicate = new PredicateBuilder().property(ClientConfigResourceProvider.COMPONENT_CLUSTER_NAME_PROPERTY_ID).equals("c1").
        toPredicate();

    String clusterName = "C1";
    String serviceName = "PIG";
    String componentName = "PIG";
    String hostName = "Host100";
    String desiredState = "INSTALLED";

    String stackName = "S1";
    String stackVersion = "V1";

    String stackRoot="/tmp/stacks/S1/V1";

    String packageFolder="PIG/package";

    HashMap<String, Host> hosts = new HashMap<String, Host>();
    hosts.put(hostName,host);
    HashMap<String, Service> services = new HashMap<String, Service>();
    services.put(serviceName,service);
    HashMap<String, ServiceComponent> serviceComponentMap = new HashMap<String, ServiceComponent>();
    serviceComponentMap.put(componentName,serviceComponent);
    HashMap<String, ServiceComponentHost> serviceComponentHosts = new HashMap<String, ServiceComponentHost>();
    serviceComponentHosts.put(componentName, serviceComponentHost);
    HashMap<String, ServiceOsSpecific> serviceOsSpecificHashMap = new HashMap<String, ServiceOsSpecific>();
    serviceOsSpecificHashMap.put("key",serviceOsSpecific);

    ServiceComponentHostResponse shr1 = new ServiceComponentHostResponse(clusterName, serviceName, componentName, hostName, desiredState, "", null, null, null);

    Set<ServiceComponentHostResponse> responses = new LinkedHashSet<ServiceComponentHostResponse>();
    responses.add(shr1);

    // set expectations
    expect(managementController.getConfigHelper()).andReturn(configHelper);
    expect(managementController.getAmbariMetaInfo()).andReturn(ambariMetaInfo).anyTimes();
    expect(managementController.getClusters()).andReturn(clusters).anyTimes();
    expect(clusters.getCluster(clusterName)).andReturn(cluster).anyTimes();
    expect(configHelper.getEffectiveConfigProperties(cluster, configTags)).andReturn(properties);
    expect(clusterConfig.getType()).andReturn(Configuration.HIVE_CONFIG_TAG).anyTimes();
    expect(configHelper.getEffectiveConfigAttributes(cluster, configTags)).andReturn(attributes);
    //!!!!
    Map<String,String> props = new HashMap<String, String>();
    props.put(Configuration.HIVE_METASTORE_PASSWORD_PROPERTY, "pass");
    props.put("key","value");
    expect(clusterConfig.getProperties()).andReturn(props);
    expect(configHelper.getEffectiveDesiredTags(cluster, hostName)).andReturn(allConfigTags);
    //!!!!
    expect(cluster.getClusterName()).andReturn(clusterName);
    expect(managementController.getHostComponents((Set<ServiceComponentHostRequest>) anyObject())).andReturn(responses).anyTimes();
    expect(cluster.getCurrentStackVersion()).andReturn(stackId);

    expect(stackId.getStackName()).andReturn(stackName).anyTimes();
    expect(stackId.getStackVersion()).andReturn(stackVersion).anyTimes();

    expect(ambariMetaInfo.getComponent(stackName, stackVersion, serviceName, componentName)).andReturn(componentInfo);
    expect(ambariMetaInfo.getServiceInfo(stackName, stackVersion, serviceName)).andReturn(serviceInfo);
    expect(serviceInfo.getServicePackageFolder()).andReturn(packageFolder);
    expect(ambariMetaInfo.getComponentCategory((String) anyObject(), (String) anyObject(),
            (String) anyObject(), (String) anyObject())).andReturn(componentInfo).anyTimes();
    expect(componentInfo.getCommandScript()).andReturn(commandScriptDefinition);
    expect(componentInfo.getClientConfigFiles()).andReturn(clientConfigFileDefinitionList);
    expect(ambariMetaInfo.getStackRoot()).andReturn(new File(stackRoot));
    expect(cluster.getAllConfigs()).andReturn(clusterConfigs);
    expect(clusters.getHostsForCluster(clusterName)).andReturn(hosts);
    expect(cluster.getServices()).andReturn(services);
    expect(service.getServiceComponents()).andReturn(serviceComponentMap);
    expect(serviceComponent.getName()).andReturn(componentName);
    expect(serviceComponent.getServiceComponentHosts()).andReturn(serviceComponentHosts);
    expect(clusters.getHost(hostName)).andReturn(host);

    HashMap<String, String> rcaParams = new HashMap<String, String>();
    rcaParams.put("key","value");
    expect(managementController.getRcaParameters()).andReturn(rcaParams).anyTimes();
    expect(ambariMetaInfo.getServiceInfo(stackName, stackVersion, serviceName)).andReturn(serviceInfo);
    expect(serviceInfo.getOsSpecifics()).andReturn(new HashMap<String, ServiceOsSpecific>()).anyTimes();
    Set<String> userSet = new HashSet<String>();
    userSet.add("hdfs");
    expect(configHelper.getPropertyValuesWithPropertyType(stackId, PropertyInfo.PropertyType.USER, cluster)).andReturn(userSet);
    PowerMock.expectNew(File.class, new Class<?>[]{String.class}, anyObject(String.class)).andReturn(mockFile).anyTimes();
    PowerMock.createNiceMockAndExpectNew(PrintWriter.class, anyObject());
    expect(mockFile.getParent()).andReturn("");
    PowerMock.mockStatic(Runtime.class);
    expect(Runtime.getRuntime()).andReturn(runtime);
    expect(mockFile.exists()).andReturn(true);
    expect(runtime.exec("ambari-python-wrap /tmp/stacks/S1/V1/PIG/package/null generate_configs null " +
            "/tmp/stacks/S1/V1/PIG/package /tmp/ambari-server/structured-out.json INFO /tmp/ambari-server"))
            .andReturn(process).once();

    // replay
    replay(managementController, clusters, cluster, ambariMetaInfo, stackId, componentInfo, commandScriptDefinition,
            clusterConfig, host, service, serviceComponent, serviceComponentHost, serviceInfo, configHelper,
            runtime, process);
    PowerMock.replayAll();

    provider.getResources(request, predicate);



    // verify
    verify(managementController, clusters, cluster, ambariMetaInfo, stackId, componentInfo,commandScriptDefinition,
            clusterConfig, host, service, serviceComponent, serviceComponentHost, serviceInfo, configHelper,
            runtime, process);
  }

  @Test
  public void testDeleteResources() throws Exception {
    Resource.Type type = Resource.Type.ClientConfig;

    AmbariManagementController managementController = createMock(AmbariManagementController.class);

    // replay
    replay(managementController);

    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
        type,
        PropertyHelper.getPropertyIds(type),
        PropertyHelper.getKeyPropertyIds(type),
        managementController);

    Predicate predicate = new PredicateBuilder().property(ClientConfigResourceProvider.COMPONENT_COMPONENT_NAME_PROPERTY_ID).equals("HDFS_CLIENT").
        toPredicate();
    try {
      provider.deleteResources(predicate);
      Assert.fail("Expected an UnsupportedOperationException");
    } catch (SystemException e) {
      // expected
    }

    // verify
    verify(managementController);
  }

}
