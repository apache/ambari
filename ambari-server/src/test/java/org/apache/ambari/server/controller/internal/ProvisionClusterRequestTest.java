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

package org.apache.ambari.server.controller.internal;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.powermock.api.easymock.PowerMock.replay;
import static org.powermock.api.easymock.PowerMock.reset;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.controller.spi.ResourceProvider;
import org.apache.ambari.server.security.encryption.CredentialStoreType;
import org.apache.ambari.server.state.quicklinksprofile.QuickLinksProfileBuilderTest;
import org.apache.ambari.server.topology.BlueprintV2;
import org.apache.ambari.server.topology.BlueprintV2Factory;
import org.apache.ambari.server.topology.Configuration;
import org.apache.ambari.server.topology.Credential;
import org.apache.ambari.server.topology.HostGroupInfo;
import org.apache.ambari.server.topology.InvalidTopologyTemplateException;
import org.apache.ambari.server.topology.TopologyRequest;
import org.apache.ambari.server.topology.TopologyTemplate;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.google.common.collect.ImmutableMap;

/**
 * Unit tests for ProvisionClusterRequest.
 */
public class ProvisionClusterRequestTest {

  private static final String CLUSTER_NAME = "cluster_name";
  private static final String BLUEPRINT_NAME = "blueprint_name";
  private static final Map<String, Object> REQUEST_PROPERTIES = ImmutableMap.of(ClusterResourceProvider.CLUSTER_NAME_PROPERTY_ID, CLUSTER_NAME);

  private static final BlueprintV2Factory blueprintFactory = createNiceMock(BlueprintV2Factory.class);
  private static final BlueprintV2 blueprint = createNiceMock(BlueprintV2.class);
  private static final ResourceProvider hostResourceProvider = createNiceMock(ResourceProvider.class);
  private static final Configuration blueprintConfig = Configuration.createEmpty();

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Before
  public void setUp() throws Exception {
    reset(blueprintFactory, blueprint, hostResourceProvider);

    BaseClusterRequest.setBlueprintFactory(blueprintFactory);
    BaseClusterRequest.setHostResourceProvider(hostResourceProvider);

    expect(blueprintFactory.getBlueprint(BLUEPRINT_NAME)).andReturn(blueprint).anyTimes();
    expect(blueprint.getConfiguration()).andReturn(blueprintConfig).anyTimes();
    expect(hostResourceProvider.checkPropertyIds(Collections.singleton("Hosts/host_name"))).andReturn(Collections.emptySet()).anyTimes();

    replay(blueprintFactory, blueprint, hostResourceProvider);
  }

  @After
  public void tearDown() {
    verify(blueprintFactory, blueprint, hostResourceProvider);
  }

  @Test
  public void testHostNameSpecified() throws Exception {
    TopologyTemplate topology = createBlueprintRequestPropertiesNameOnly(BLUEPRINT_NAME);

    ProvisionClusterRequest provisionClusterRequest = new ProvisionClusterRequest(topology, REQUEST_PROPERTIES, null);

    assertEquals(CLUSTER_NAME, provisionClusterRequest.getClusterName());
    assertEquals(TopologyRequest.Type.PROVISION, provisionClusterRequest.getType());
    assertEquals(String.format("Provision Cluster '%s'", CLUSTER_NAME) , provisionClusterRequest.getDescription());
    assertSame(blueprint, provisionClusterRequest.getBlueprint());
    Map<String, HostGroupInfo> hostGroupInfo = provisionClusterRequest.getHostGroupInfo();
    assertEquals(1, hostGroupInfo.size());

    // group1
    // host info
    HostGroupInfo group1Info = hostGroupInfo.get("group1");
    assertEquals("group1", group1Info.getHostGroupName());
    assertEquals(1, group1Info.getHostNames().size());
    assertTrue(group1Info.getHostNames().contains("host1.mydomain.com"));
    assertEquals(1, group1Info.getRequestedHostCount());
    assertNull(group1Info.getPredicate());
    // configuration
    Configuration group1Configuration = group1Info.getConfiguration();
    assertNull(group1Configuration.getParentConfiguration());
    assertEquals(1, group1Configuration.getProperties().size());
    Map<String, String> group1TypeProperties = group1Configuration.getProperties().get("foo-type");
    assertEquals(2, group1TypeProperties.size());
    assertEquals("prop1Value", group1TypeProperties.get("hostGroup1Prop1"));
    assertEquals("prop2Value", group1TypeProperties.get("hostGroup1Prop2"));
    assertTrue(group1Configuration.getAttributes().isEmpty());
  }

  @Test
  public void testHostCountSpecified() throws Exception {
    TopologyTemplate topology = createBlueprintRequestPropertiesCountOnly(BLUEPRINT_NAME);
    topology.getHostGroups().iterator().next().setHostPredicate(null);

    ProvisionClusterRequest provisionClusterRequest = new ProvisionClusterRequest(topology, REQUEST_PROPERTIES, null);

    assertEquals(CLUSTER_NAME, provisionClusterRequest.getClusterName());
    assertEquals(TopologyRequest.Type.PROVISION, provisionClusterRequest.getType());
    assertEquals(String.format("Provision Cluster '%s'", CLUSTER_NAME) , provisionClusterRequest.getDescription());
    assertSame(blueprint, provisionClusterRequest.getBlueprint());
    Map<String, HostGroupInfo> hostGroupInfo = provisionClusterRequest.getHostGroupInfo();
    assertEquals(1, hostGroupInfo.size());

    // group2
    HostGroupInfo group2Info = hostGroupInfo.get("group2");
    assertEquals("group2", group2Info.getHostGroupName());
    assertTrue(group2Info.getHostNames().isEmpty());
    assertEquals(5, group2Info.getRequestedHostCount());
    assertNull(group2Info.getPredicate());
    // configuration
    Configuration group2Configuration = group2Info.getConfiguration();
    assertNull(group2Configuration.getParentConfiguration());
    assertEquals(1, group2Configuration.getProperties().size());
    Map<String, String> group2TypeProperties = group2Configuration.getProperties().get("foo-type");
    assertEquals(1, group2TypeProperties.size());
    assertEquals("prop1Value", group2TypeProperties.get("hostGroup2Prop1"));
    //attributes
    Map<String, Map<String, Map<String, String>>> group2Attributes = group2Configuration.getAttributes();
    assertEquals(1, group2Attributes.size());
    Map<String, Map<String, String>> group2Type1Attributes = group2Attributes.get("foo-type");
    assertEquals(1, group2Type1Attributes.size());
    Map<String, String> group2Type1Prop1Attributes = group2Type1Attributes.get("attribute1");
    assertEquals(1, group2Type1Prop1Attributes.size());
    assertEquals("attribute1Prop10-value", group2Type1Prop1Attributes.get("hostGroup2Prop10"));
  }

  @Test
  public void testMultipleGroups() throws Exception {
    TopologyTemplate topology = createBlueprintRequestProperties(BLUEPRINT_NAME);
    ProvisionClusterRequest provisionClusterRequest = new ProvisionClusterRequest(topology, REQUEST_PROPERTIES, null);

    assertEquals(CLUSTER_NAME, provisionClusterRequest.getClusterName());
    assertEquals(TopologyRequest.Type.PROVISION, provisionClusterRequest.getType());
    assertEquals(String.format("Provision Cluster '%s'", CLUSTER_NAME) , provisionClusterRequest.getDescription());
    assertSame(blueprint, provisionClusterRequest.getBlueprint());
    Map<String, HostGroupInfo> hostGroupInfo = provisionClusterRequest.getHostGroupInfo();
    assertEquals(2, hostGroupInfo.size());

    // group1
    // host info
    HostGroupInfo group1Info = hostGroupInfo.get("group1");
    assertEquals("group1", group1Info.getHostGroupName());
    assertEquals(1, group1Info.getHostNames().size());
    assertTrue(group1Info.getHostNames().contains("host1.mydomain.com"));
    assertEquals(1, group1Info.getRequestedHostCount());
    assertNull(group1Info.getPredicate());
    // configuration
    Configuration group1Configuration = group1Info.getConfiguration();
    assertNull(group1Configuration.getParentConfiguration());
    assertEquals(1, group1Configuration.getProperties().size());
    Map<String, String> group1TypeProperties = group1Configuration.getProperties().get("foo-type");
    assertEquals(2, group1TypeProperties.size());
    assertEquals("prop1Value", group1TypeProperties.get("hostGroup1Prop1"));
    assertEquals("prop2Value", group1TypeProperties.get("hostGroup1Prop2"));
    assertTrue(group1Configuration.getAttributes().isEmpty());

    // group2
    HostGroupInfo group2Info = hostGroupInfo.get("group2");
    assertEquals("group2", group2Info.getHostGroupName());
    assertTrue(group2Info.getHostNames().isEmpty());
    assertEquals(5, group2Info.getRequestedHostCount());
    assertNotNull(group2Info.getPredicate());
    // configuration
    Configuration group2Configuration = group2Info.getConfiguration();
    assertNull(group2Configuration.getParentConfiguration());
    assertEquals(1, group2Configuration.getProperties().size());
    Map<String, String> group2TypeProperties = group2Configuration.getProperties().get("foo-type");
    assertEquals(1, group2TypeProperties.size());
    assertEquals("prop1Value", group2TypeProperties.get("hostGroup2Prop1"));
    //attributes
    Map<String, Map<String, Map<String, String>>> group2Attributes = group2Configuration.getAttributes();
    assertEquals(1, group2Attributes.size());
    Map<String, Map<String, String>> group2Type1Attributes = group2Attributes.get("foo-type");
    assertEquals(1, group2Type1Attributes.size());
    Map<String, String> group2Type1Prop1Attributes = group2Type1Attributes.get("attribute1");
    assertEquals(1, group2Type1Prop1Attributes.size());
    assertEquals("attribute1Prop10-value", group2Type1Prop1Attributes.get("hostGroup2Prop10"));
  }

  @Test(expected = IllegalArgumentException.class)
  public void test_NoHostGroupInfo() throws Exception {
    TopologyTemplate topology = createBlueprintRequestProperties(BLUEPRINT_NAME);
    topology.getHostGroups().clear();

    // should result in an exception
    new ProvisionClusterRequest(topology, REQUEST_PROPERTIES, null);
  }

  @Test
  public void testCredentials() throws Exception {
    TopologyTemplate topology = createBlueprintRequestProperties(BLUEPRINT_NAME);
    Credential credential = new Credential("testAlias", "testPrincipal", "testKey", CredentialStoreType.TEMPORARY);
    topology.setCredentials(Collections.singleton(credential));

    ProvisionClusterRequest provisionClusterRequest = new ProvisionClusterRequest(topology, REQUEST_PROPERTIES, null);

    assertEquals(provisionClusterRequest.getCredentialsMap().get("testAlias").getAlias(), "testAlias");
    assertEquals(provisionClusterRequest.getCredentialsMap().get("testAlias").getPrincipal(), "testPrincipal");
    assertEquals(provisionClusterRequest.getCredentialsMap().get("testAlias").getKey(), "testKey");
    assertEquals(provisionClusterRequest.getCredentialsMap().get("testAlias").getType().name(), "TEMPORARY");
  }

  @Test(expected = IllegalArgumentException.class)
  public void test_GroupInfoMissingName() throws Exception {
    TopologyTemplate topology = createBlueprintRequestProperties(BLUEPRINT_NAME);
    topology.getHostGroups().iterator().next().setName(null);

    // reset default host resource provider expectations to none
    reset(hostResourceProvider, blueprintFactory);
    replay(hostResourceProvider, blueprintFactory);
    // should result in an exception
    new ProvisionClusterRequest(topology, REQUEST_PROPERTIES, null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void test_NoHostsInfo() throws Exception {
    TopologyTemplate topology = createBlueprintRequestProperties(BLUEPRINT_NAME);
    topology.getHostGroups().forEach(hg -> hg.setHosts(null));

    reset(blueprintFactory);
    replay(blueprintFactory);
    // should result in an exception
    new ProvisionClusterRequest(topology, REQUEST_PROPERTIES, null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void test_NoHostNameOrHostCount() throws Exception {
    TopologyTemplate topology = createBlueprintRequestProperties(BLUEPRINT_NAME);
    // remove fqdn property for a group that contains fqdn not host_count
    topology.getHostGroups().forEach(
      hg -> hg.getHosts().forEach(
        h -> h.setFqdn(null)
      )
    );

    reset(blueprintFactory);
    replay(blueprintFactory);
    // should result in an exception
    new ProvisionClusterRequest(topology, REQUEST_PROPERTIES, null);
  }


  @Test(expected = IllegalArgumentException.class)
  public void testInvalidPredicateProperty() throws Exception {
    reset(hostResourceProvider, blueprintFactory);
    // checkPropertyIds() returns invalid property names
    expect(hostResourceProvider.checkPropertyIds(Collections.singleton("Hosts/host_name"))).
      andReturn(Collections.singleton("Hosts/host_name"));
    replay(hostResourceProvider, blueprintFactory);

    // should result in an exception due to invalid property in host predicate
    new ProvisionClusterRequest(createBlueprintRequestProperties(BLUEPRINT_NAME), REQUEST_PROPERTIES, null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testHostNameAndCountSpecified() throws Exception {
    // reset host resource provider expectations to none since we are not specifying a host predicate
    reset(hostResourceProvider, blueprintFactory);
    replay(hostResourceProvider, blueprintFactory);

    TopologyTemplate topology = createBlueprintRequestPropertiesNameOnly(BLUEPRINT_NAME);
    topology.getHostGroups().iterator().next().setHostCount(5);
    // should result in an exception due to both host name and host count being specified
    new ProvisionClusterRequest(topology, REQUEST_PROPERTIES, null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testHostNameAndPredicateSpecified() throws Exception {
    // reset host resource provider expectations to none since we are not specifying a host predicate
    reset(hostResourceProvider, blueprintFactory);
    replay(hostResourceProvider, blueprintFactory);

    TopologyTemplate topology = createBlueprintRequestPropertiesNameOnly(BLUEPRINT_NAME);
    topology.getHostGroups().iterator().next().setHostPredicate("Hosts/host_name=myTestHost");
    // should result in an exception due to both host name and host count being specified
    new ProvisionClusterRequest(topology, REQUEST_PROPERTIES, null);
  }

  @Test
  public void testQuickLinksProfile_NoDataInRequest() throws Exception {
    TopologyTemplate topology = createBlueprintRequestProperties(BLUEPRINT_NAME);
    ProvisionClusterRequest request = new ProvisionClusterRequest(topology, REQUEST_PROPERTIES, null);
    assertNull("No quick links profile is expected", request.getQuickLinksProfileJson());
  }

  @Test
  public void testQuickLinksProfile_OnlyGlobalFilterDataInRequest() throws Exception {
    TopologyTemplate topology = createBlueprintRequestProperties(BLUEPRINT_NAME);

    Map<String, Object> properties = new HashMap<>(REQUEST_PROPERTIES);
    properties.put(ProvisionClusterRequest.QUICKLINKS_PROFILE_FILTERS_PROPERTY,
        Collections.singleton(QuickLinksProfileBuilderTest.filter(null, null, true)));

    ProvisionClusterRequest request = new ProvisionClusterRequest(topology, properties, null);
    assertEquals("Quick links profile doesn't match expected",
        "{\"filters\":[{\"visible\":true}],\"services\":[]}",
        request.getQuickLinksProfileJson());
  }

  @Test
  public void testQuickLinksProfile_OnlyServiceFilterDataInRequest() throws Exception {
    TopologyTemplate topology = createBlueprintRequestProperties(BLUEPRINT_NAME);

    Map<String, Object> properties = new HashMap<>(REQUEST_PROPERTIES);
    Map<String, String> filter = QuickLinksProfileBuilderTest.filter(null, null, true);
    Map<String, Object> hdfs = QuickLinksProfileBuilderTest.service("HDFS", null, Collections.singleton(filter));
    Set<Map<String, Object>> services = Collections.singleton(hdfs);
    properties.put(ProvisionClusterRequest.QUICKLINKS_PROFILE_SERVICES_PROPERTY, services);

    ProvisionClusterRequest request = new ProvisionClusterRequest(topology, properties, null);
    assertEquals("Quick links profile doesn't match expected",
        "{\"filters\":[],\"services\":[{\"name\":\"HDFS\",\"components\":[],\"filters\":[{\"visible\":true}]}]}",
        request.getQuickLinksProfileJson());
  }

  @Test
  public void testQuickLinksProfile_BothGlobalAndServiceLevelFilters() throws Exception {
    TopologyTemplate topology = createBlueprintRequestProperties(BLUEPRINT_NAME);

    Map<String, Object> properties = new HashMap<>(REQUEST_PROPERTIES);
    properties.put(ProvisionClusterRequest.QUICKLINKS_PROFILE_FILTERS_PROPERTY,
      Collections.singleton(QuickLinksProfileBuilderTest.filter(null, null, true)));

    Map<String, String> filter = QuickLinksProfileBuilderTest.filter(null, null, true);
    Map<String, Object> hdfs = QuickLinksProfileBuilderTest.service("HDFS", null, Collections.singleton(filter));
    Set<Map<String, Object>> services = Collections.singleton(hdfs);
    properties.put(ProvisionClusterRequest.QUICKLINKS_PROFILE_SERVICES_PROPERTY, services);

    ProvisionClusterRequest request = new ProvisionClusterRequest(topology, properties, null);
    assertEquals("Quick links profile doesn't match expected",
        "{\"filters\":[{\"visible\":true}],\"services\":[{\"name\":\"HDFS\",\"components\":[],\"filters\":[{\"visible\":true}]}]}",
        request.getQuickLinksProfileJson());
  }

  @Test(expected = InvalidTopologyTemplateException.class)
  public void testQuickLinksProfile_InvalidRequestData() throws Exception {
    TopologyTemplate topology = createBlueprintRequestProperties(BLUEPRINT_NAME);

    Map<String, Object> properties = new HashMap<>(REQUEST_PROPERTIES);
    properties.put(ProvisionClusterRequest.QUICKLINKS_PROFILE_SERVICES_PROPERTY, "Hello World!");

    new ProvisionClusterRequest(topology, properties, null);
  }

  public static TopologyTemplate createBlueprintRequestProperties(String blueprintName) {
    TopologyTemplate topology = new TopologyTemplate();
    topology.setBlueprint(blueprintName);

    Collection<TopologyTemplate.HostGroup> hostGroups = new ArrayList<>();
    hostGroups.add(createHostGroupWithHosts());
    hostGroups.add(createHostGroupWithHostCount());
    topology.setHostGroups(hostGroups); // must come after host group creation, since it processes

    // createClusterConfig(); TODO what's up with it?

    return topology;
  }

  private static TopologyTemplate createBlueprintRequestPropertiesNameOnly(String blueprintName) {
    TopologyTemplate topology = new TopologyTemplate();
    topology.setBlueprint(blueprintName);

    Collection<TopologyTemplate.HostGroup> hostGroups = new ArrayList<>();
    hostGroups.add(createHostGroupWithHosts());
    topology.setHostGroups(hostGroups); // must come after host group creation, since it processes

    // createClusterConfig(); TODO what's up with it?

    return topology;
  }

  public static TopologyTemplate createBlueprintRequestPropertiesCountOnly(String blueprintName) {
    TopologyTemplate topology = new TopologyTemplate();
    topology.setBlueprint(blueprintName);

    Collection<TopologyTemplate.HostGroup> hostGroups = new ArrayList<>();
    hostGroups.add(createHostGroupWithHostCount());
    topology.setHostGroups(hostGroups); // must come after host group creation, since it processes

    // createClusterConfig();

    return topology;
  }

  private static TopologyTemplate.HostGroup createHostGroupWithHostCount() {
    TopologyTemplate.HostGroup hg = new TopologyTemplate.HostGroup();

    hg.setName("group2");
    hg.setHostCount(5);
    hg.setHostPredicate("Hosts/host_name=myTestHost");

    // host group scoped configuration
    // version 2 configuration syntax
    hg.getConfiguration().setProperty("foo-type", "hostGroup2Prop1", "prop1Value");
    hg.getConfiguration().setAttribute("foo-type", "hostGroup2Prop10", "attribute1", "attribute1Prop10-value");

    return hg;
  }

  private static TopologyTemplate.HostGroup createHostGroupWithHosts() {
    TopologyTemplate.HostGroup hg = new TopologyTemplate.HostGroup();

    hg.setName("group1");

    Collection<TopologyTemplate.Host> hosts = new ArrayList<>();
    hg.setHosts(hosts);

    TopologyTemplate.Host host = new TopologyTemplate.Host();
    host.setFqdn("host1.myDomain.com");
    hosts.add(host);

    // host group scoped configuration
    // version 1 configuration syntax
    hg.getConfiguration().setProperty("foo-type", "hostGroup1Prop1", "prop1Value");
    hg.getConfiguration().setProperty("foo-type", "hostGroup1Prop2", "prop2Value");

    return hg;
  }

  private static Configuration createClusterConfig() {
    Configuration clusterConfig = Configuration.createEmpty();
    clusterConfig.setProperty("someType", "property1", "someValue");
    clusterConfig.setAttribute("someType", "property1", "attribute1", "someAttributePropValue");
    return clusterConfig;
  }

}
