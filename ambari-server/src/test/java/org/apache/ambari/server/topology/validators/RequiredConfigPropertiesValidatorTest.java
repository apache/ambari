/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.topology.validators;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.ambari.server.controller.StackV2;
import org.apache.ambari.server.topology.BlueprintV2;
import org.apache.ambari.server.topology.ClusterTopology;
import org.apache.ambari.server.topology.Configuration;
import org.apache.ambari.server.topology.HostGroupV2;
import org.apache.ambari.server.topology.HostGroupV2Impl;
import org.apache.ambari.server.topology.InvalidTopologyException;
import org.apache.ambari.server.topology.Service;
import org.easymock.EasyMock;
import org.easymock.EasyMockRule;
import org.easymock.EasyMockSupport;
import org.easymock.Mock;
import org.easymock.TestSubject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class RequiredConfigPropertiesValidatorTest extends EasyMockSupport {

  @Rule
  public EasyMockRule mocks = new EasyMockRule(this);

  @Mock
  private ClusterTopology clusterTopologyMock;

  @Mock
  private Configuration topologyConfigurationMock;

  @Mock
  private BlueprintV2 blueprintMock;

  @Mock
  private StackV2 stackMock;

  @Mock
  private Service kerberosMock;

  @Mock
  private HostGroupV2Impl slaveHostGroupMock;

  @Mock
  private HostGroupV2Impl masterHostGroupMock;

  @Mock
  private Configuration slaveHostGroupConfigurationMock;

  @Mock
  private Configuration masterHostGroupConfigurationMock;


  private Map<String, Map<String, String>> topologyConfigurationMap = new HashMap<>();
  private Map<String, Map<String, String>> masterHostGroupConfigurationMap = new HashMap<>();
  private Map<String, Map<String, String>> slaveHostGroupConfigurationMap = new HashMap<>();
  private Collection<String> bpServices = new HashSet<>();
  private Collection<Service> slaveHostGroupServices = new HashSet<>();
  private Collection<Service> masterHostGroupServices = new HashSet<>();
  private Map<String, HostGroupV2> hostGroups = new HashMap<>();

  private Map<String, Collection<String>> missingProps = new TreeMap<>();

  @TestSubject
  private RequiredConfigPropertiesValidator testSubject = new RequiredConfigPropertiesValidator();

  /**
   * Assembles the basic default fixture of the test:
   * The blueprint has 2 hostgroups, both of them contains the KERBEROS service (or KERBEROS_CLIENT)
   * The changing items are the configurations, that my come from the topology (bp + cct) or the hostg roups
   */

  @Before
  public void setup() {
    resetAll();

    EasyMock.expect(clusterTopologyMock.getConfiguration()).andReturn(topologyConfigurationMock);
    EasyMock.expect(topologyConfigurationMock.getFullProperties(1)).andReturn(topologyConfigurationMap);

    EasyMock.expect(clusterTopologyMock.getBlueprint()).andReturn(blueprintMock).anyTimes();

    EasyMock.expect(blueprintMock.getHostGroups()).andReturn(hostGroups);

    EasyMock.expect(masterHostGroupMock.getName()).andReturn("master").anyTimes();
    EasyMock.expect(masterHostGroupMock.getConfiguration()).andReturn(masterHostGroupConfigurationMock).anyTimes();
    EasyMock.expect(masterHostGroupMock.getServices()).andReturn(masterHostGroupServices);

    EasyMock.expect(slaveHostGroupMock.getName()).andReturn("slave").anyTimes();
    EasyMock.expect(slaveHostGroupMock.getConfiguration()).andReturn(slaveHostGroupConfigurationMock).anyTimes();
    EasyMock.expect(slaveHostGroupMock.getServices()).andReturn(slaveHostGroupServices);

    EasyMock.expect(kerberosMock.getName()).andReturn("KERBEROS").anyTimes();
    EasyMock.expect(kerberosMock.getType()).andReturn("KERBEROS").anyTimes();
    EasyMock.expect(kerberosMock.getStack()).andReturn(stackMock).anyTimes();

    // there are 2 hostgroups to be considered by the test
    hostGroups.put("master", masterHostGroupMock);
    hostGroups.put("slave", slaveHostGroupMock);

    // services in the blueprint
    bpServices.addAll(Lists.newArrayList("KERBEROS", "OOZIE"));

    // host group services
    masterHostGroupServices.addAll(Collections.singletonList(kerberosMock));
    slaveHostGroupServices.addAll(Collections.singletonList(kerberosMock));

    EasyMock.expect(masterHostGroupConfigurationMock.getProperties()).andReturn(masterHostGroupConfigurationMap);
    EasyMock.expect(slaveHostGroupConfigurationMock.getProperties()).andReturn(slaveHostGroupConfigurationMap);

    // services in the blueprint
    bpServices.addAll(Lists.newArrayList("KERBEROS", "OOZIE"));

    // required properties for listed services
    EasyMock.expect(stackMock.getRequiredConfigurationProperties("KERBEROS")).
      andReturn(Lists.newArrayList(
        new StackV2.ConfigProperty("kerberos-env", "realm", "value"),
        new StackV2.ConfigProperty("kerberos-env", "kdc_type", "value"), // this is missing!
        new StackV2.ConfigProperty("krb5-conf", "domains", "smthg"))).anyTimes();

    EasyMock.expect(stackMock.getRequiredConfigurationProperties("OOZIE")).andReturn(Collections.EMPTY_LIST);

  }

  @Test
  public void testShouldValidationFailWhenNoHostGroupConfigurationProvidedAndRequiredConfigTypesAreMissing() throws Exception {

    // GIVEN
    // all the configuration comes from the bp, cct hg configs are empty
    topologyConfigurationMap.put("kerberos-env", new HashMap<>());
    topologyConfigurationMap.get("kerberos-env").put("realm", "etwas");
    topologyConfigurationMap.get("kerberos-env").put("kdc_type", "mit-kdc");

    // note, that the krb-5 config type is missing! (see the required properties in the fixture!)
    missingProps.put("slave", new TreeSet<>(Collections.singletonList("domains")));
    missingProps.put("master", new TreeSet<>(Collections.singletonList("domains")));

    replayAll();

    // WHEN
    String expectedMsg = String.format("Missing required properties.  Specify a value for these properties in the blueprint or cluster creation template configuration. %s", missingProps);
    String actualMsg = "";
    try {
      testSubject.validate(clusterTopologyMock);
    } catch (InvalidTopologyException e) {
      actualMsg = e.getMessage();
    }

    // THEN
    // Exception is thrown, as the krb5-conf typeeis not provided
    Assert.assertEquals("The exception message should be the expected one", expectedMsg, actualMsg);
  }

  @Test
  public void testShouldValidationFailWhenNoHostGroupConfigurationProvidedAndRequiredPropertiesAreMissing() throws Exception {
    // GIVEN

    // configuration from the blueprint / cluster creation template
    topologyConfigurationMap.put("kerberos-env", new HashMap<>());
    topologyConfigurationMap.get("kerberos-env").put("realm", "etwas");

    // note, that tehe kdc_type is mssing from the operational config

    topologyConfigurationMap.put("krb5-conf", new HashMap<>());
    topologyConfigurationMap.get("krb5-conf").put("domains", "smthg");

    missingProps.put("master", Collections.singletonList("kdc_type"));
    missingProps.put("slave", Collections.singletonList("kdc_type"));

    replayAll();

    // WHEN
    String expectedMsg = String.format("Missing required properties.  Specify a value for these properties in the blueprint or cluster creation template configuration. %s", missingProps);
    String actualMsg = "";
    try {
      testSubject.validate(clusterTopologyMock);
    } catch (InvalidTopologyException e) {
      actualMsg = e.getMessage();
    }

    // THEN
    // Exception is thrown, as the krb5-conf typee is not provideds
    Assert.assertEquals("The exception message should be the expected one", expectedMsg, actualMsg);

  }


  @Test
  public void testShouldValidationFailWhenHostGroupConfigurationProvidedAndRequiredConfigTypesAreMissingFromBothHostgroups() throws Exception {
    // GIVEN
    // configuration come in the host groups, there are missing config types in both hostgroups
    missingProps.put("master", Sets.newTreeSet(Lists.newArrayList("kdc_type", "domains", "realm")));
    missingProps.put("slave", Sets.newTreeSet(Lists.newArrayList("kdc_type", "domains", "realm")));

    replayAll();

    // WHEN
    String expectedMsg = String.format("Missing required properties.  Specify a value for these properties in the blueprint or cluster creation template configuration. %s", missingProps);
    String actualMsg = "";
    try {
      testSubject.validate(clusterTopologyMock);
    } catch (InvalidTopologyException e) {
      actualMsg = e.getMessage();
    }

    // THEN
    // Exception is thrown, as the krb5-conf typee is not provided
    Assert.assertEquals("The exception message should be the expected one", expectedMsg, actualMsg);
  }

  @Test
  public void testShouldValidationFailWhenHostGroupConfigurationProvidedAndRequiredConfigTypesAreMissingFromSlaveHostgroup() throws Exception {
    // GIVEN
    // configuration come in the host groups, there are missing config types in both hostgroups
    masterHostGroupConfigurationMap.put("kerberos-env", new HashMap<>());
    masterHostGroupConfigurationMap.get("kerberos-env").put("realm", "etwas");
    masterHostGroupConfigurationMap.get("kerberos-env").put("kdc_type", "mit-kdc");
    masterHostGroupConfigurationMap.put("krb5-conf", new HashMap<>());
    masterHostGroupConfigurationMap.get("krb5-conf").put("domains", "smthg");

    missingProps.put("slave", Sets.newTreeSet(Lists.newArrayList("kdc_type", "domains", "realm")));

    replayAll();

    // WHEN
    String expectedMsg = String.format("Missing required properties.  Specify a value for these properties in the blueprint or cluster creation template configuration. %s", missingProps);
    String actualMsg = "";
    try {
      testSubject.validate(clusterTopologyMock);
    } catch (InvalidTopologyException e) {
      actualMsg = e.getMessage();
    }

    // THEN
    // Exception is thrown, as the krb5-conf typee is not provideds
    Assert.assertEquals("The exception message should be the expected one", expectedMsg, actualMsg);
  }

  @Test
  public void testShouldValidationPassWhenAllRequiredPropertiesAreProvidedInHostGroupConfiguration() throws Exception {
    // GIVEN

    masterHostGroupConfigurationMap.put("kerberos-env", new HashMap<>());
    masterHostGroupConfigurationMap.get("kerberos-env").put("realm", "etwas");
    masterHostGroupConfigurationMap.get("kerberos-env").put("kdc_type", "mit-kdc");
    masterHostGroupConfigurationMap.put("krb5-conf", new HashMap<>());
    masterHostGroupConfigurationMap.get("krb5-conf").put("domains", "smthg");

    slaveHostGroupConfigurationMap.put("kerberos-env", new HashMap<>());
    slaveHostGroupConfigurationMap.get("kerberos-env").put("realm", "etwas");
    slaveHostGroupConfigurationMap.get("kerberos-env").put("kdc_type", "mit-kdc");
    slaveHostGroupConfigurationMap.put("krb5-conf", new HashMap<>());
    slaveHostGroupConfigurationMap.get("krb5-conf").put("domains", "smthg");

    replayAll();

    // WHEN

    testSubject.validate(clusterTopologyMock);

    // THEN
    // no exceptions thrown

  }


  @Test
  public void testShouldValidationPassWhenAllRequiredPropertiesAreProvidedInTopologyConfiguration() throws Exception {
    // GIVEN
    // configuration from the blueprint / cluster creation template
    topologyConfigurationMap.put("kerberos-env", new HashMap<>());
    topologyConfigurationMap.get("kerberos-env").put("realm", "etwas");
    topologyConfigurationMap.get("kerberos-env").put("kdc_type", "value");

    topologyConfigurationMap.put("krb5-conf", new HashMap<>());
    topologyConfigurationMap.get("krb5-conf").put("domains", "smthg");

    replayAll();

    // WHEN
    testSubject.validate(clusterTopologyMock);

    // THEN
    // no exceptions thrown

  }

}