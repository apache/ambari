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

package org.apache.ambari.view.capacityscheduler;

import org.apache.ambari.view.ViewInstanceDefinition;
import org.apache.ambari.view.validation.Validator;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.*;

public class PropertyValidatorTest {
  @Test
  public void testValidatePropertyOk() throws Exception {
    ViewInstanceDefinition instanceDefinition =
        getViewInstanceDefinition("http://hostname.com:8080/api/v1/clusters/Cluster");
    PropertyValidator propertyValidator = new PropertyValidator();

    assertTrue(propertyValidator.validateProperty(
        PropertyValidator.AMBARI_SERVER_URL, instanceDefinition,
        Validator.ValidationContext.PRE_CREATE).isValid());
  }

  @Test
  public void testValidatePropertyHttps() throws Exception {
    ViewInstanceDefinition instanceDefinition =
        getViewInstanceDefinition("https://hostname.com:8080/api/v1/clusters/Cluster");
    PropertyValidator propertyValidator = new PropertyValidator();

    assertTrue(propertyValidator.validateProperty(
        PropertyValidator.AMBARI_SERVER_URL, instanceDefinition,
        Validator.ValidationContext.PRE_CREATE).isValid());
  }

  @Test
  public void testValidatePropertyNoPort() throws Exception {
    ViewInstanceDefinition instanceDefinition =
        getViewInstanceDefinition("http://hostname.com/api/v1/clusters/Cluster");
    PropertyValidator propertyValidator = new PropertyValidator();

    assertFalse(propertyValidator.validateProperty(
        PropertyValidator.AMBARI_SERVER_URL, instanceDefinition,
        Validator.ValidationContext.PRE_CREATE).isValid());
  }

  @Test
  public void testValidatePropertyNoProtocol() throws Exception {
    ViewInstanceDefinition instanceDefinition =
        getViewInstanceDefinition("hostname.com:8080/api/v1/clusters/Cluster");
    PropertyValidator propertyValidator = new PropertyValidator();

    assertFalse(propertyValidator.validateProperty(
        PropertyValidator.AMBARI_SERVER_URL, instanceDefinition,
        Validator.ValidationContext.PRE_CREATE).isValid());
  }

  @Test
  public void testValidatePropertyNoCluster() throws Exception {
    ViewInstanceDefinition instanceDefinition =
        getViewInstanceDefinition("http://hostname.com:8080");
    PropertyValidator propertyValidator = new PropertyValidator();

    assertFalse(propertyValidator.validateProperty(
        PropertyValidator.AMBARI_SERVER_URL, instanceDefinition,
        Validator.ValidationContext.PRE_CREATE).isValid());
  }

  @Test
  public void testValidatePropertyNoClusterName() throws Exception {
    ViewInstanceDefinition instanceDefinition =
        getViewInstanceDefinition("http://hostname.com:8080/api/v1/clusters/");
    PropertyValidator propertyValidator = new PropertyValidator();

    assertFalse(propertyValidator.validateProperty(
        PropertyValidator.AMBARI_SERVER_URL, instanceDefinition,
        Validator.ValidationContext.PRE_CREATE).isValid());
  }

  @Test
  public void testValidatePropertyMisspell() throws Exception {
    ViewInstanceDefinition instanceDefinition =
        getViewInstanceDefinition("http://hostname.com:8080/api/v1/clAsters/MyCluster");
    PropertyValidator propertyValidator = new PropertyValidator();

    assertFalse(propertyValidator.validateProperty(
        PropertyValidator.AMBARI_SERVER_URL, instanceDefinition,
        Validator.ValidationContext.PRE_CREATE).isValid());
  }

  @Test
  public void testValidatePropertyOnlyHostname() throws Exception {
    ViewInstanceDefinition instanceDefinition =
        getViewInstanceDefinition("hostname.com");
    PropertyValidator propertyValidator = new PropertyValidator();

    assertFalse(propertyValidator.validateProperty(
        PropertyValidator.AMBARI_SERVER_URL, instanceDefinition,
        Validator.ValidationContext.PRE_CREATE).isValid());
  }

  @Test
  public void shouldValidateUrlsWithHyphenInHostName() throws Exception {
    ViewInstanceDefinition instanceDefinition =
      getViewInstanceDefinition("http://sub-domain.hostname.com:8080/api/v1/clusters/Cluster");
    PropertyValidator propertyValidator = new PropertyValidator();

    assertTrue(propertyValidator.validateProperty(
      PropertyValidator.AMBARI_SERVER_URL, instanceDefinition,
      Validator.ValidationContext.PRE_CREATE).isValid());
  }

  @Test
  public void shouldValidateUrlWithNonStandardTLDs() throws Exception {
    ViewInstanceDefinition instanceDefinition =
      getViewInstanceDefinition("http://cl1-node.nova:8080/api/v1/clusters/Cluster");
    PropertyValidator propertyValidator = new PropertyValidator();

    assertTrue(propertyValidator.validateProperty(
      PropertyValidator.AMBARI_SERVER_URL, instanceDefinition,
      Validator.ValidationContext.PRE_CREATE).isValid());
  }

  @Test
  public void shouldValidateLocalhost() throws Exception {
    ViewInstanceDefinition instanceDefinition =
      getViewInstanceDefinition("http://localhost:8080/api/v1/clusters/Cluster");
    PropertyValidator propertyValidator = new PropertyValidator();

    assertTrue(propertyValidator.validateProperty(
      PropertyValidator.AMBARI_SERVER_URL, instanceDefinition,
      Validator.ValidationContext.PRE_CREATE).isValid());

  }

  private ViewInstanceDefinition getViewInstanceDefinition(String ambariServerUrl) {
    ViewInstanceDefinition instanceDefinition = createNiceMock(ViewInstanceDefinition.class);
    Map<String, String> map = new HashMap<String, String>();
    expect(instanceDefinition.getPropertyMap()).andReturn(map).anyTimes();
    replay(instanceDefinition);

    map.put(PropertyValidator.AMBARI_SERVER_URL, ambariServerUrl);
    return instanceDefinition;
  }
}