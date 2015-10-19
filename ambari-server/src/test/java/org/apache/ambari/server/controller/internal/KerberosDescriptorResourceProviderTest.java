package org.apache.ambari.server.controller.internal;

import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.UnsupportedPropertyException;
import org.apache.ambari.server.orm.dao.KerberosDescriptorDAO;
import org.apache.ambari.server.orm.entities.KerberosDescriptorEntity;
import org.apache.ambari.server.topology.KerberosDescriptorFactory;
import org.apache.ambari.server.topology.KerberosDescriptorImpl;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.easymock.EasyMockRule;
import org.easymock.Mock;
import org.easymock.MockType;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.easymock.EasyMock.anyString;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.reset;

/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

public class KerberosDescriptorResourceProviderTest {

  private static final String TEST_KERBEROS_DESCRIPTOR_NAME = "descriptor-name-0";
  private static final String TEST_KERBEROS_DESCRIPTOR = "descriptor";
  public static final String KERBEROS_DESCRIPTORS_KERBEROS_DESCRIPTOR_NAME = "KerberosDescriptors/kerberos_descriptor_name";

  @Rule
  public EasyMockRule mocks = new EasyMockRule(this);

  @Mock(type = MockType.STRICT)
  private KerberosDescriptorDAO kerberosDescriptorDAO;

  @Mock(type = MockType.STRICT)
  private KerberosDescriptorFactory kerberosDescriptorFactory;

  @Mock(type = MockType.STRICT)
  private Request request;

  private KerberosDescriptorResourceProvider kerberosDescriptorResourceProvider;

  @Before
  public void before() {
    reset(request);

  }

  @Test(expected = UnsupportedPropertyException.class)
  public void testCreateShouldThrowExceptionWhenNoDescriptorProvided() throws Exception {

    // GIVEN
    EasyMock.expect(request.getProperties()).andReturn(requestPropertySet(KERBEROS_DESCRIPTORS_KERBEROS_DESCRIPTOR_NAME,
        TEST_KERBEROS_DESCRIPTOR_NAME)).times(3);
    EasyMock.expect(request.getRequestInfoProperties()).andReturn(requestInfoPropertyMap("", "")).times(2);
    EasyMock.replay(request);

    kerberosDescriptorResourceProvider = new KerberosDescriptorResourceProvider(kerberosDescriptorDAO,
        kerberosDescriptorFactory, Collections.EMPTY_SET, Collections.EMPTY_MAP, null);

    // WHEN
    kerberosDescriptorResourceProvider.createResources(request);

    // THEN
    // exception is thrown
  }

  @Test(expected = UnsupportedPropertyException.class)
  public void testCreateShouldThrowExceptionWhenNoNameProvided() throws Exception {

    // GIVEN
    EasyMock.expect(request.getProperties()).andReturn(emptyRequestPropertySet()).times(2);
    EasyMock.replay(request);

    kerberosDescriptorResourceProvider = new KerberosDescriptorResourceProvider(kerberosDescriptorDAO,
        kerberosDescriptorFactory, Collections.EMPTY_SET, Collections.EMPTY_MAP, null);

    // WHEN
    kerberosDescriptorResourceProvider.createResources(request);

    // THEN
    // exception is thrown
  }


  @Test
  public void testShoudCreateResourceWhenNameAndDescriptorProvided() throws Exception {

    // GIVEN
    kerberosDescriptorResourceProvider = new KerberosDescriptorResourceProvider(kerberosDescriptorDAO,
        kerberosDescriptorFactory, Collections.EMPTY_SET, Collections.EMPTY_MAP, null);

    EasyMock.expect(request.getProperties())
        .andReturn(requestPropertySet(KERBEROS_DESCRIPTORS_KERBEROS_DESCRIPTOR_NAME, TEST_KERBEROS_DESCRIPTOR_NAME))
        .times(3);
    EasyMock.expect(request.getRequestInfoProperties())
        .andReturn(requestInfoPropertyMap(Request.REQUEST_INFO_BODY_PROPERTY, TEST_KERBEROS_DESCRIPTOR))
        .times(3);
    EasyMock.expect(kerberosDescriptorFactory.createKerberosDescriptor(anyString(), anyString()))
        .andReturn(new KerberosDescriptorImpl(TEST_KERBEROS_DESCRIPTOR_NAME, TEST_KERBEROS_DESCRIPTOR));

    Capture<KerberosDescriptorEntity> entityCapture = EasyMock.newCapture();
    kerberosDescriptorDAO.create(capture(entityCapture));

    EasyMock.replay(request, kerberosDescriptorFactory, kerberosDescriptorDAO);

    // WHEN
    kerberosDescriptorResourceProvider.createResources(request);

    // THEN
    Assert.assertNotNull(entityCapture.getValue());
    Assert.assertEquals("The resource name is invalid!", TEST_KERBEROS_DESCRIPTOR_NAME, entityCapture.getValue()
        .getName());

  }

  private Set<Map<String, Object>> emptyRequestPropertySet() {
    return Collections.emptySet();
  }


  private Map<String, String> requestInfoPropertyMap(String propertyKey, String propertyValue) {
    Map<String, String> propsMap = new HashMap<>();
    propsMap.put(propertyKey, propertyValue);
    return propsMap;
  }

  private Set<Map<String, Object>> requestPropertySet(String testPropertyKey, String testPropertyValue) {
    Set<Map<String, Object>> invalidProps = new HashSet<>();
    Map<String, Object> invalidMap = new HashMap<>();
    invalidMap.put(testPropertyKey, testPropertyValue);
    invalidProps.add(invalidMap);
    return invalidProps;
  }

}