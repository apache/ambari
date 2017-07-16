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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.controller.internal.Stack;
import org.apache.ambari.server.state.PropertyInfo;
import org.apache.ambari.server.topology.Blueprint;
import org.apache.ambari.server.topology.ClusterTopology;
import org.apache.ambari.server.topology.Configuration;
import org.apache.ambari.server.topology.HostGroup;
import org.apache.ambari.server.topology.InvalidTopologyException;
import org.easymock.EasyMock;
import org.easymock.EasyMockRule;
import org.easymock.EasyMockSupport;
import org.easymock.Mock;
import org.easymock.TestSubject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class RequiredConfigPropertiesValidatorTest extends EasyMockSupport {

  @Rule
  public EasyMockRule mocks = new EasyMockRule(this);

  @Mock
  private ClusterTopology clusterTopologyMock;

  @Mock
  private Configuration topologyConfigurationMock;

  @Mock
  private Blueprint blueprintMock;

  @Mock
  private Stack stackMock;

  private Map<String, Map<String, String>> topologyConfigurationMap = new HashMap<>();
  private Collection<String> bpServices = new HashSet<>();
  private Map<String, HostGroup> hostGroups = new HashMap<>();
  private Map<String, Collection<String>> missingProps = new HashMap<>();

  @TestSubject
  private RequiredConfigPropertiesValidator testSubject = new RequiredConfigPropertiesValidator();

  @Before
  public void setup() {
    resetAll();

    EasyMock.expect(clusterTopologyMock.getConfiguration()).andReturn(topologyConfigurationMock);
    EasyMock.expect(topologyConfigurationMock.getFullProperties(1)).andReturn(topologyConfigurationMap);

    EasyMock.expect(clusterTopologyMock.getBlueprint()).andReturn(blueprintMock).anyTimes();
    EasyMock.expect(blueprintMock.getHostGroups()).andReturn(hostGroups);
    EasyMock.expect(blueprintMock.getServices()).andReturn(bpServices);
    EasyMock.expect(blueprintMock.getStack()).andReturn(stackMock).anyTimes();
  }

  @Test
  public void testShouldValidationFailWhenRequiredConfigTypesAreMissing() throws Exception {

    // GIVEN

    // services in the blueprint
    bpServices.addAll(Arrays.asList("KERBEROS", "OOZIE"));

    // required properties for listed services
    EasyMock.expect(stackMock.getRequiredConfigurationProperties("KERBEROS")).
      andReturn(Arrays.asList(
        new Stack.ConfigProperty("kerberos-env", "realm", "value"),
        new Stack.ConfigProperty("kerberos-env", "kdc_type", "value"),
        new Stack.ConfigProperty("krb5-conf", "domains", "node.dc1.consul")));

    EasyMock.expect(stackMock.getRequiredConfigurationProperties("OOZIE"))
      .andReturn(Collections.EMPTY_LIST);


    // configuration from the blueprint / cluster creation template
    topologyConfigurationMap.put("kerberos-env", new HashMap<String, String>());
    topologyConfigurationMap.get("kerberos-env").put("realm", "etwas");
    topologyConfigurationMap.get("kerberos-env").put("kdc_type", "mit-kdc");

    missingProps.put("krb5-conf", Arrays.asList("domains"));

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
  public void testShouldValidationFailWhenNotAllRequiredPropertiesAreProvided() throws Exception {
    // GIVEN

    // services in the blueprint
    bpServices.addAll(Arrays.asList("KERBEROS", "OOZIE"));

    // required properties for listed services
    EasyMock.expect(stackMock.getRequiredConfigurationProperties("KERBEROS")).
      andReturn(Arrays.asList(
        new Stack.ConfigProperty("kerberos-env", "realm", "value"),
        new Stack.ConfigProperty("kerberos-env", "kdc_type", "value"), // this is missing!
        new Stack.ConfigProperty("krb5-conf", "domains", "smthg")));

    EasyMock.expect(stackMock.getRequiredConfigurationProperties("OOZIE"))
      .andReturn(Collections.EMPTY_LIST);


    // configuration from the blueprint / cluster creation template
    topologyConfigurationMap.put("kerberos-env", new HashMap<String, String>());
    topologyConfigurationMap.get("kerberos-env").put("realm", "etwas");

    topologyConfigurationMap.put("krb5-conf", new HashMap<String, String>());
    topologyConfigurationMap.get("krb5-conf").put("domains", "smthg");

    missingProps.put("kerberos-env", Arrays.asList("kdc_type"));

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
  public void testShouldValidationPassWhenAllRequiredPropertiesAreProvided() throws Exception {
    // GIVEN

    // services in the blueprint
    bpServices.addAll(Arrays.asList("KERBEROS"));

    // configuration from the blueprint / cluster creation template
    topologyConfigurationMap.put("kerberos-env", new HashMap<String, String>());
    topologyConfigurationMap.get("kerberos-env").put("realm", "etwas");
    topologyConfigurationMap.get("kerberos-env").put("kdc_type", "value");

    topologyConfigurationMap.put("krb5-conf", new HashMap<String, String>());
    topologyConfigurationMap.get("krb5-conf").put("domains", "smthg");

    // required properties for listed services
    EasyMock.expect(stackMock.getRequiredConfigurationProperties("KERBEROS")).
      andReturn(Arrays.asList(
        new Stack.ConfigProperty("kerberos-env", "realm", "value"),
        new Stack.ConfigProperty("kerberos-env", "kdc_type", "value"),
        new Stack.ConfigProperty("krb5-conf", "domains", "smthg")));

    replayAll();

    // WHEN

    testSubject.validate(clusterTopologyMock);


    // THEN
    // no exceptions thrown

  }

  @Test
  public void testShouldValidationPassWhenPasswordTypeRequiredPropertiesAreMissing() throws Exception {
    // GIVEN

    // services in the blueprint
    bpServices.addAll(Arrays.asList("KNOX"));

    Set<PropertyInfo.PropertyType> passwordTypeSet = new HashSet<>();
    passwordTypeSet.add(PropertyInfo.PropertyType.PASSWORD);

    Stack.ConfigProperty passworProp = new Stack.ConfigProperty("knox-env", "knox_master_secret", "pwd");
    passworProp.setPropertyTypes(passwordTypeSet);

    // required properties for listed services
    EasyMock.expect(stackMock.getRequiredConfigurationProperties("KNOX")).
      andReturn(Arrays.asList(
        passworProp, // this should be ignored
        new Stack.ConfigProperty("knox-env", "knox_user", "kuser")
      ));


    // configuration from the blueprint / cluster creation template
    topologyConfigurationMap.put("knox-env", new HashMap<String, String>());
    topologyConfigurationMap.get("knox-env").put("knox_user", "etwas");

    replayAll();

    // WHEN
    testSubject.validate(clusterTopologyMock);

    // THEN
    // validation passes

  }
}