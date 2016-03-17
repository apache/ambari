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
import org.apache.ambari.server.utils.StageUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.*;

import org.apache.ambari.server.stack.StackManager;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.assertFalse;

/**
 * TaskResourceProvider tests.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest( {ClientConfigResourceProvider.class, StageUtils.class} )
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

    Predicate predicate = new PredicateBuilder().property(
            ClientConfigResourceProvider.COMPONENT_CLUSTER_NAME_PROPERTY_ID).equals("c1").toPredicate();

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
    DesiredConfig desiredConfig = createNiceMock(DesiredConfig.class);
    Host host = createNiceMock(Host.class);
    Service service = createNiceMock(Service.class);
    ServiceComponent serviceComponent = createNiceMock(ServiceComponent.class);
    ServiceComponentHost serviceComponentHost = createNiceMock(ServiceComponentHost.class);
    ServiceOsSpecific serviceOsSpecific = createNiceMock(ServiceOsSpecific.class);
    ConfigHelper configHelper = createNiceMock(ConfigHelper.class);
    Configuration configuration = PowerMock.createStrictMockAndExpectNew(Configuration.class);
    Map<String, String> configMap = createNiceMock(Map.class);

    File mockFile = PowerMock.createNiceMock(File.class);
    Runtime runtime = createMock(Runtime.class);
    Process process = createNiceMock(Process.class);

    Map<String, DesiredConfig> desiredConfigMap = new HashMap<String, DesiredConfig>();
    desiredConfigMap.put("hive-site", desiredConfig);
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
    String displayName = "Pig Client";
    String hostName = "Host100";
    String desiredState = "INSTALLED";

    String stackName = "S1";
    String stackVersion = "V1";

    String stackRoot="/tmp/stacks/S1/V1";
    String packageFolder="PIG/package";

    if (System.getProperty("os.name").contains("Windows")) {
      stackRoot = "\\tmp\\stacks\\S1\\V1";
      packageFolder = "PIG\\package";
    }

    File stackRootFile = new File(stackRoot);
    HashMap<String, Host> hosts = new HashMap<String, Host>();
    hosts.put(hostName, host);
    HashMap<String, Service> services = new HashMap<String, Service>();
    services.put(serviceName,service);
    HashMap<String, ServiceComponent> serviceComponentMap = new HashMap<String, ServiceComponent>();
    serviceComponentMap.put(componentName,serviceComponent);
    HashMap<String, ServiceComponentHost> serviceComponentHosts = new HashMap<String, ServiceComponentHost>();
    serviceComponentHosts.put(componentName, serviceComponentHost);
    HashMap<String, ServiceOsSpecific> serviceOsSpecificHashMap = new HashMap<String, ServiceOsSpecific>();
    serviceOsSpecificHashMap.put("key",serviceOsSpecific);

    ServiceComponentHostResponse shr1 = new ServiceComponentHostResponse(clusterName, serviceName, componentName, displayName, hostName, desiredState, "", null, null, null);

    Set<ServiceComponentHostResponse> responses = new LinkedHashSet<ServiceComponentHostResponse>();
    responses.add(shr1);
    
    Map<String, String> returnConfigMap = new HashMap<String, String>();
    returnConfigMap.put(Configuration.SERVER_TMP_DIR_KEY, Configuration.SERVER_TMP_DIR_DEFAULT);
    returnConfigMap.put(Configuration.AMBARI_PYTHON_WRAP_KEY, Configuration.AMBARI_PYTHON_WRAP_DEFAULT);
    
    // set expectations
    expect(managementController.getConfigHelper()).andReturn(configHelper);
    expect(managementController.getAmbariMetaInfo()).andReturn(ambariMetaInfo).anyTimes();
    expect(managementController.getClusters()).andReturn(clusters).anyTimes();
    expect(clusters.getCluster(clusterName)).andReturn(cluster).anyTimes();
    expect(configHelper.getEffectiveConfigProperties(cluster, configTags)).andReturn(properties);
    expect(clusterConfig.getType()).andReturn(Configuration.HIVE_CONFIG_TAG).anyTimes();
    expect(configHelper.getEffectiveConfigAttributes(cluster, configTags)).andReturn(attributes);
    expect(configMap.get(Configuration.SERVER_TMP_DIR_KEY)).andReturn(Configuration.SERVER_TMP_DIR_DEFAULT);
    expect(configMap.get(Configuration.AMBARI_PYTHON_WRAP_KEY)).andReturn(Configuration.AMBARI_PYTHON_WRAP_DEFAULT);
    expect(configuration.getConfigsMap()).andReturn(returnConfigMap);
    expect(configuration.getResourceDirPath()).andReturn("/tmp/stacks/S1/V1");
    expect(configuration.getJavaVersion()).andReturn(8);
    expect(configuration.areHostsSysPrepped()).andReturn("false");
    expect(configuration.isAgentStackRetryOnInstallEnabled()).andReturn("false");
    expect(configuration.getAgentStackRetryOnInstallCount()).andReturn("5");
    expect(configuration.getExternalScriptTimeout()).andReturn(Integer.parseInt(Configuration.EXTERNAL_SCRIPT_TIMEOUT_DEFAULT));
    Map<String,String> props = new HashMap<String, String>();
    props.put(Configuration.HIVE_METASTORE_PASSWORD_PROPERTY, "pass");
    props.put("key","value");
    expect(clusterConfig.getProperties()).andReturn(props);
    expect(configHelper.getEffectiveDesiredTags(cluster, hostName)).andReturn(allConfigTags);
    expect(cluster.getClusterName()).andReturn(clusterName);
    expect(managementController.getHostComponents((Set<ServiceComponentHostRequest>) anyObject())).andReturn(responses).anyTimes();
    expect(cluster.getCurrentStackVersion()).andReturn(stackId);

    PowerMock.mockStaticPartial(StageUtils.class, "getClusterHostInfo");
    Map<String, Set<String>> clusterHostInfo = new HashMap<String, Set<String>>();
    Set<String> all_hosts = new HashSet<String>(Arrays.asList("Host100","Host101","Host102"));
    Set<String> some_hosts = new HashSet<String>(Arrays.asList("0-1","2"));
    Set<String> ohter_hosts = new HashSet<String>(Arrays.asList("0,1"));
    Set<String> clusterHostTypes = new HashSet<String>(Arrays.asList("nm_hosts", "hs_host",
            "namenode_host", "rm_host", "snamenode_host", "slave_hosts", "zookeeper_hosts"));
    for (String hostTypes: clusterHostTypes) {
      if (hostTypes.equals("slave_hosts")) {
        clusterHostInfo.put(hostTypes, ohter_hosts);
      } else {
        clusterHostInfo.put(hostTypes, some_hosts);
      }
    }
    Map<String, Host> stringHostMap = new HashMap<String, Host>();
    stringHostMap.put(hostName, host);
    clusterHostInfo.put("all_hosts",all_hosts);
    expect(StageUtils.getClusterHostInfo(cluster)).andReturn(clusterHostInfo);

    expect(stackId.getStackName()).andReturn(stackName).anyTimes();
    expect(stackId.getStackVersion()).andReturn(stackVersion).anyTimes();

    expect(ambariMetaInfo.getComponent(stackName, stackVersion, serviceName, componentName)).andReturn(componentInfo);
    expect(ambariMetaInfo.getService(stackName, stackVersion, serviceName)).andReturn(serviceInfo);
    expect(serviceInfo.getServicePackageFolder()).andReturn(packageFolder);
    expect(ambariMetaInfo.getComponent((String) anyObject(), (String) anyObject(),
            (String) anyObject(), (String) anyObject())).andReturn(componentInfo).anyTimes();
    expect(componentInfo.getCommandScript()).andReturn(commandScriptDefinition);
    expect(componentInfo.getClientConfigFiles()).andReturn(clientConfigFileDefinitionList);
    expect(cluster.getConfig("hive-site", null)).andReturn(clusterConfig);
    expect(cluster.getDesiredConfigs()).andReturn(desiredConfigMap);
    expect(clusters.getHost(hostName)).andReturn(host);

    HashMap<String, String> rcaParams = new HashMap<String, String>();
    rcaParams.put("key","value");
    expect(managementController.getRcaParameters()).andReturn(rcaParams).anyTimes();
    expect(ambariMetaInfo.getService(stackName, stackVersion, serviceName)).andReturn(serviceInfo);
    expect(serviceInfo.getOsSpecifics()).andReturn(new HashMap<String, ServiceOsSpecific>()).anyTimes();
    Set<String> userSet = new HashSet<String>();
    userSet.add("hdfs");
    expect(configHelper.getPropertyValuesWithPropertyType(stackId, PropertyInfo.PropertyType.USER, cluster)).andReturn(userSet);
    PowerMock.expectNew(File.class, new Class<?>[]{String.class}, anyObject(String.class)).andReturn(mockFile).anyTimes();
    PowerMock.createNiceMockAndExpectNew(PrintWriter.class, anyObject());
    expect(mockFile.getParent()).andReturn("");
    PowerMock.mockStatic(Runtime.class);
    expect(mockFile.exists()).andReturn(true);
    String commandLine = "ambari-python-wrap /tmp/stacks/S1/V1/PIG/package/null generate_configs null " +
            "/tmp/stacks/S1/V1/PIG/package /var/lib/ambari-server/tmp/structured-out.json " +
            "INFO /var/lib/ambari-server/tmp";

    if (System.getProperty("os.name").contains("Windows")) {
      String absoluteStackRoot = stackRootFile.getAbsolutePath();
      commandLine = "ambari-python-wrap " + absoluteStackRoot +
              "\\PIG\\package\\null generate_configs null " +
              absoluteStackRoot + "\\PIG\\package /var/lib/ambari-server/tmp\\structured-out.json " +
              "INFO /var/lib/ambari-server/tmp";
    }

    ProcessBuilder processBuilder = PowerMock.createNiceMock(ProcessBuilder.class);
    PowerMock.expectNew(ProcessBuilder.class,Arrays.asList(commandLine.split("\\s+"))).andReturn(processBuilder).once();
    expect(processBuilder.start()).andReturn(process).once();
    InputStream inputStream = new ByteArrayInputStream("some logging info".getBytes());
    expect(process.getInputStream()).andReturn(inputStream);

    // replay
    replay(managementController, clusters, cluster, ambariMetaInfo, stackId, componentInfo, commandScriptDefinition,
            clusterConfig, host, service, serviceComponent, serviceComponentHost, serviceInfo, configHelper,
            runtime, process, configMap);
    PowerMock.replayAll();

    Set<Resource> resources = provider.getResources(request, predicate);
    assertFalse(resources.isEmpty());

    // verify
    verify(managementController, clusters, cluster, ambariMetaInfo, stackId, componentInfo,commandScriptDefinition,
            clusterConfig, host, service, serviceComponent, serviceComponentHost, serviceInfo, configHelper,
            runtime, process);
    PowerMock.verifyAll();
  }

  @Test
  public void testGetResourcesFromCommonServices() throws Exception {
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
    DesiredConfig desiredConfig = createNiceMock(DesiredConfig.class);
    Host host = createNiceMock(Host.class);
    Service service = createNiceMock(Service.class);
    ServiceComponent serviceComponent = createNiceMock(ServiceComponent.class);
    ServiceComponentHost serviceComponentHost = createNiceMock(ServiceComponentHost.class);
    ServiceOsSpecific serviceOsSpecific = createNiceMock(ServiceOsSpecific.class);
    ConfigHelper configHelper = createNiceMock(ConfigHelper.class);
    Configuration configuration = PowerMock.createStrictMockAndExpectNew(Configuration.class);
    Map<String, String> configMap = createNiceMock(Map.class);

    File mockFile = PowerMock.createNiceMock(File.class);
    Runtime runtime = createMock(Runtime.class);
    Process process = createNiceMock(Process.class);

    Map<String, DesiredConfig> desiredConfigMap = new HashMap<String, DesiredConfig>();
    desiredConfigMap.put("hive-site", desiredConfig);
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
    String displayName = "Pig Client";
    String hostName = "Host100";
    String desiredState = "INSTALLED";

    String stackName = "S1";
    String stackVersion = "V1";

    String stackRoot="/tmp/stacks/S1/V1";
    String packageFolder= StackManager.COMMON_SERVICES + "/PIG/package";
    String commonServicesPath = "/var/lib/ambari-server/src/main/resources/common-services";

    if (System.getProperty("os.name").contains("Windows")) {
      stackRoot = "\\tmp\\stacks\\S1\\V1";
      packageFolder = StackManager.COMMON_SERVICES + "\\PIG\\package";
    }

    File stackRootFile = new File(stackRoot);
    HashMap<String, Host> hosts = new HashMap<String, Host>();
    hosts.put(hostName, host);
    HashMap<String, Service> services = new HashMap<String, Service>();
    services.put(serviceName,service);
    HashMap<String, ServiceComponent> serviceComponentMap = new HashMap<String, ServiceComponent>();
    serviceComponentMap.put(componentName,serviceComponent);
    HashMap<String, ServiceComponentHost> serviceComponentHosts = new HashMap<String, ServiceComponentHost>();
    serviceComponentHosts.put(componentName, serviceComponentHost);
    HashMap<String, ServiceOsSpecific> serviceOsSpecificHashMap = new HashMap<String, ServiceOsSpecific>();
    serviceOsSpecificHashMap.put("key",serviceOsSpecific);

    ServiceComponentHostResponse shr1 = new ServiceComponentHostResponse(clusterName, serviceName, componentName, displayName, hostName, desiredState, "", null, null, null);

    Set<ServiceComponentHostResponse> responses = new LinkedHashSet<ServiceComponentHostResponse>();
    responses.add(shr1);
    
    Map<String, String> returnConfigMap = new HashMap<String, String>();
    returnConfigMap.put(Configuration.SERVER_TMP_DIR_KEY, Configuration.SERVER_TMP_DIR_DEFAULT);
    returnConfigMap.put(Configuration.AMBARI_PYTHON_WRAP_KEY, Configuration.AMBARI_PYTHON_WRAP_DEFAULT);
    
    // set expectations
    expect(managementController.getConfigHelper()).andReturn(configHelper);
    expect(managementController.getAmbariMetaInfo()).andReturn(ambariMetaInfo).anyTimes();
    expect(managementController.getClusters()).andReturn(clusters).anyTimes();
    expect(clusters.getCluster(clusterName)).andReturn(cluster).anyTimes();
    expect(configHelper.getEffectiveConfigProperties(cluster, configTags)).andReturn(properties);
    expect(clusterConfig.getType()).andReturn(Configuration.HIVE_CONFIG_TAG).anyTimes();
    expect(configHelper.getEffectiveConfigAttributes(cluster, configTags)).andReturn(attributes);
    expect(configMap.get(Configuration.SERVER_TMP_DIR_KEY)).andReturn(Configuration.SERVER_TMP_DIR_DEFAULT);
    expect(configMap.get(Configuration.AMBARI_PYTHON_WRAP_KEY)).andReturn(Configuration.AMBARI_PYTHON_WRAP_DEFAULT);
    expect(configuration.getConfigsMap()).andReturn(returnConfigMap);
    expect(configuration.getResourceDirPath()).andReturn("/var/lib/ambari-server/src/main/resources");
    expect(configuration.getJavaVersion()).andReturn(8);
    expect(configuration.areHostsSysPrepped()).andReturn("false");
    expect(configuration.isAgentStackRetryOnInstallEnabled()).andReturn("false");
    expect(configuration.getAgentStackRetryOnInstallCount()).andReturn("5");
    expect(configuration.getExternalScriptTimeout()).andReturn(Integer.parseInt(Configuration.EXTERNAL_SCRIPT_TIMEOUT_DEFAULT));

    Map<String,String> props = new HashMap<String, String>();
    props.put(Configuration.HIVE_METASTORE_PASSWORD_PROPERTY, "pass");
    props.put("key","value");
    expect(clusterConfig.getProperties()).andReturn(props);
    expect(configHelper.getEffectiveDesiredTags(cluster, hostName)).andReturn(allConfigTags);
    expect(cluster.getClusterName()).andReturn(clusterName);
    expect(managementController.getHostComponents((Set<ServiceComponentHostRequest>) anyObject())).andReturn(responses).anyTimes();
    expect(cluster.getCurrentStackVersion()).andReturn(stackId);

    PowerMock.mockStaticPartial(StageUtils.class, "getClusterHostInfo");
    Map<String, Set<String>> clusterHostInfo = new HashMap<String, Set<String>>();
    Set<String> all_hosts = new HashSet<String>(Arrays.asList("Host100","Host101","Host102"));
    Set<String> some_hosts = new HashSet<String>(Arrays.asList("0-1","2"));
    Set<String> ohter_hosts = new HashSet<String>(Arrays.asList("0,1"));
    Set<String> clusterHostTypes = new HashSet<String>(Arrays.asList("nm_hosts", "hs_host",
            "namenode_host", "rm_host", "snamenode_host", "slave_hosts", "zookeeper_hosts"));
    for (String hostTypes: clusterHostTypes) {
      if (hostTypes.equals("slave_hosts")) {
        clusterHostInfo.put(hostTypes, ohter_hosts);
      } else {
        clusterHostInfo.put(hostTypes, some_hosts);
      }
    }
    Map<String, Host> stringHostMap = new HashMap<String, Host>();
    stringHostMap.put(hostName, host);
    clusterHostInfo.put("all_hosts",all_hosts);
    expect(StageUtils.getClusterHostInfo(cluster)).andReturn(clusterHostInfo);

    expect(stackId.getStackName()).andReturn(stackName).anyTimes();
    expect(stackId.getStackVersion()).andReturn(stackVersion).anyTimes();

    expect(ambariMetaInfo.getComponent(stackName, stackVersion, serviceName, componentName)).andReturn(componentInfo);
    expect(ambariMetaInfo.getService(stackName, stackVersion, serviceName)).andReturn(serviceInfo);
    expect(serviceInfo.getServicePackageFolder()).andReturn(packageFolder);
    expect(ambariMetaInfo.getComponent((String) anyObject(), (String) anyObject(),
            (String) anyObject(), (String) anyObject())).andReturn(componentInfo).anyTimes();
    expect(componentInfo.getCommandScript()).andReturn(commandScriptDefinition);
    expect(componentInfo.getClientConfigFiles()).andReturn(clientConfigFileDefinitionList);
    expect(cluster.getConfig("hive-site", null)).andReturn(clusterConfig);
    expect(cluster.getDesiredConfigs()).andReturn(desiredConfigMap);
    expect(clusters.getHost(hostName)).andReturn(host);

    HashMap<String, String> rcaParams = new HashMap<String, String>();
    rcaParams.put("key","value");
    expect(managementController.getRcaParameters()).andReturn(rcaParams).anyTimes();
    expect(ambariMetaInfo.getService(stackName, stackVersion, serviceName)).andReturn(serviceInfo);
    expect(serviceInfo.getOsSpecifics()).andReturn(new HashMap<String, ServiceOsSpecific>()).anyTimes();
    Set<String> userSet = new HashSet<String>();
    userSet.add("hdfs");
    expect(configHelper.getPropertyValuesWithPropertyType(stackId, PropertyInfo.PropertyType.USER, cluster)).andReturn(userSet);
    PowerMock.expectNew(File.class, new Class<?>[]{String.class}, anyObject(String.class)).andReturn(mockFile).anyTimes();
    PowerMock.createNiceMockAndExpectNew(PrintWriter.class, anyObject());
    expect(mockFile.getParent()).andReturn("");
    PowerMock.mockStatic(Runtime.class);
    expect(mockFile.exists()).andReturn(true);
    String commandLine = "ambari-python-wrap " + commonServicesPath + "/PIG/package/null generate_configs null " +
            commonServicesPath + "/PIG/package /var/lib/ambari-server/tmp/structured-out.json " +
            "INFO /var/lib/ambari-server/tmp";

    if (System.getProperty("os.name").contains("Windows")) {
      String absoluteStackRoot = stackRootFile.getAbsolutePath();
      commandLine = "ambari-python-wrap " + commonServicesPath +
              "\\PIG\\package\\null generate_configs null " +
              commonServicesPath + "\\PIG\\package /var/lib/ambari-server/tmp\\structured-out.json " +
              "INFO /var/lib/ambari-server/tmp";
    }

    ProcessBuilder processBuilder = PowerMock.createNiceMock(ProcessBuilder.class);
    PowerMock.expectNew(ProcessBuilder.class,Arrays.asList(commandLine.split("\\s+"))).andReturn(processBuilder).once();
    expect(processBuilder.start()).andReturn(process).once();
    InputStream inputStream = new ByteArrayInputStream("some logging info".getBytes());
    expect(process.getInputStream()).andReturn(inputStream);

    // replay
    replay(managementController, clusters, cluster, ambariMetaInfo, stackId, componentInfo, commandScriptDefinition,
            clusterConfig, host, service, serviceComponent, serviceComponentHost, serviceInfo, configHelper,
            runtime, process, configMap);
    PowerMock.replayAll();

    Set<Resource> resources = provider.getResources(request, predicate);
    assertFalse(resources.isEmpty());

    // verify
    verify(managementController, clusters, cluster, ambariMetaInfo, stackId, componentInfo,commandScriptDefinition,
            clusterConfig, host, service, serviceComponent, serviceComponentHost, serviceInfo, configHelper,
            runtime, process);
    PowerMock.verifyAll();
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

    Predicate predicate = new PredicateBuilder().property(
            ClientConfigResourceProvider.COMPONENT_COMPONENT_NAME_PROPERTY_ID).equals("HDFS_CLIENT").toPredicate();
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
