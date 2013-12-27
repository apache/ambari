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


import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.configuration.ComponentSSLConfiguration;
import org.apache.ambari.server.configuration.ComponentSSLConfigurationTest;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.controller.utilities.StreamProvider;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class HttpPropertyProviderTest {
  private static final String PROPERTY_ID_CLUSTER_NAME = PropertyHelper.getPropertyId("HostRoles", "cluster_name");
  private static final String PROPERTY_ID_HOST_NAME = PropertyHelper.getPropertyId("HostRoles", "host_name");
  private static final String PROPERTY_ID_COMPONENT_NAME = PropertyHelper.getPropertyId("HostRoles", "component_name");
  
  private static final String PROPERTY_ID_NAGIOS_ALERTS = PropertyHelper.getPropertyId("HostRoles", "nagios_alerts");

  private ComponentSSLConfiguration configuration;

  @Parameterized.Parameters
  public static Collection<Object[]> configs() {
    ComponentSSLConfiguration configuration1 =
        ComponentSSLConfigurationTest.getConfiguration("tspath", "tspass", "tstype", false, false);

    ComponentSSLConfiguration configuration2 =
        ComponentSSLConfigurationTest.getConfiguration("tspath", "tspass", "tstype", true, false);

    ComponentSSLConfiguration configuration3 =
        ComponentSSLConfigurationTest.getConfiguration("tspath", "tspass", "tstype", false, true);

    return Arrays.asList(new Object[][]{
        {configuration1},
        {configuration2},
        {configuration3}
    });
  }


  public HttpPropertyProviderTest(ComponentSSLConfiguration configuration) {
    this.configuration = configuration;
  }

  @Test
  public void testReadNagiosServer() throws Exception {

    TestStreamProvider streamProvider = new TestStreamProvider(false);

    Resource resource = doPopulate("NAGIOS_SERVER", Collections.<String>emptySet(), streamProvider);
    
    Assert.assertNotNull("Expected non-null for 'nagios_alerts'",
      resource.getPropertyValue(PROPERTY_ID_NAGIOS_ALERTS));

    Assert.assertEquals(configuration.isNagiosSSL(), streamProvider.getLastSpec().startsWith("https"));
  }
  
  @Test
  public void testReadNotRequested() throws Exception {
   
   Set<String> propertyIds = new HashSet<String>();
   propertyIds.add(PropertyHelper.getPropertyId("HostRoles", "state"));
   propertyIds.add(PROPERTY_ID_COMPONENT_NAME);
   
   Resource resource = doPopulate("NAGIOS_SERVER", propertyIds, new TestStreamProvider(false));
   
   Assert.assertNull("Expected null for 'nagios_alerts'",
     resource.getPropertyValue(PROPERTY_ID_NAGIOS_ALERTS));    
  }
  
  @Test
  public void testReadWithRequested() throws Exception {
    
   Set<String> propertyIds = new HashSet<String>();
   propertyIds.add(PropertyHelper.getPropertyId("HostRoles", "nagios_alerts"));
   propertyIds.add(PROPERTY_ID_COMPONENT_NAME);
   
   Resource resource = doPopulate("NAGIOS_SERVER", propertyIds, new TestStreamProvider(false));
   
   Assert.assertNotNull("Expected non-null for 'nagios_alerts'",
     resource.getPropertyValue(PROPERTY_ID_NAGIOS_ALERTS));        
  }
  
  @Test
  public void testReadWithRequestedFail() throws Exception {
    
   Set<String> propertyIds = new HashSet<String>();
   propertyIds.add(PropertyHelper.getPropertyId("HostRoles", "nagios_alerts"));
   propertyIds.add(PROPERTY_ID_COMPONENT_NAME);
   
   Resource resource = doPopulate("NAGIOS_SERVER", propertyIds, new TestStreamProvider(true));

   Assert.assertNull("Expected null for 'nagios_alerts'",
       resource.getPropertyValue(PROPERTY_ID_NAGIOS_ALERTS));        
  }

  @SuppressWarnings("rawtypes")
  @Test
  public void testReadWithRequestedJson() throws Exception {

    Set<String> propertyIds = new HashSet<String>();
    propertyIds.add(PropertyHelper.getPropertyId("HostRoles", "nagios_alerts"));
    propertyIds.add(PROPERTY_ID_COMPONENT_NAME);
    Resource resource = doPopulate("NAGIOS_SERVER", propertyIds, new TestStreamProvider(false));
    Object propertyValue = resource.getPropertyValue(PROPERTY_ID_NAGIOS_ALERTS);

    Assert.assertNotNull("Expected non-null for 'nagios_alerts'", propertyValue);
    Assert.assertTrue("Expected Map for parsed JSON", propertyValue instanceof Map);
    
    Object alertsEntry = ((Map) propertyValue).get("alerts");
    Object hostcountsEntry = ((Map) propertyValue).get("hostcounts");
    
    Assert.assertNotNull("Expected non-null for 'alerts' entry", alertsEntry);
    Assert.assertNotNull("Expected non-null for 'hostcounts' entry", hostcountsEntry);
    Assert.assertTrue("Expected List type for 'alerts' entry", alertsEntry instanceof List);
    Assert.assertTrue("Expected Map type for 'hostcounts' entry", hostcountsEntry instanceof Map);
    
    List alertsList = (List) alertsEntry;
    Map hostcountsMap = (Map) hostcountsEntry;
    
    Assert.assertEquals("Expected number of entries in 'alerts' is 1", 1, alertsList.size());
    Assert.assertTrue("Expected Map type for 'alerts' element", alertsList.get(0) instanceof Map);
    Assert.assertEquals("Body", ((Map) alertsList.get(0)).get("Alert Body"));
    
    Assert.assertEquals("Expected number of entries in 'hostcounts' is 2", 2, hostcountsMap.size());
    Assert.assertEquals("1", hostcountsMap.get("up_hosts"));
    Assert.assertEquals("0", hostcountsMap.get("down_hosts"));
  }

  @Test
  public void testReadGangliaServer() throws Exception {
    
    Resource resource = doPopulate("GANGLIA_SERVER", Collections.<String>emptySet(), new TestStreamProvider(false));

    // !!! GANGLIA_SERVER has no current http lookup
    Assert.assertNull("Expected null, was: " +
      resource.getPropertyValue(PROPERTY_ID_NAGIOS_ALERTS),
      resource.getPropertyValue(PROPERTY_ID_NAGIOS_ALERTS));
  }
  
  private Resource doPopulate(String componentName,
      Set<String> requestProperties, StreamProvider streamProvider) throws Exception {

    HttpProxyPropertyProvider propProvider = new HttpProxyPropertyProvider(
       streamProvider, configuration,
       PROPERTY_ID_CLUSTER_NAME,
       PROPERTY_ID_HOST_NAME,
       PROPERTY_ID_COMPONENT_NAME);
    
    Resource resource = new ResourceImpl(Resource.Type.HostComponent);

    resource.setProperty(PROPERTY_ID_HOST_NAME, "ec2-54-234-33-50.compute-1.amazonaws.com");
    resource.setProperty(PROPERTY_ID_COMPONENT_NAME, componentName);
    
    Request request = PropertyHelper.getReadRequest(requestProperties);
    
    propProvider.populateResources(Collections.singleton(resource), request, null);

    return resource;
  }
  
  private static class TestStreamProvider implements StreamProvider {
    private boolean throwError = false;
    private String lastSpec = null;
    private boolean isLastSpecUpdated;

    private TestStreamProvider(boolean throwErr) {
      throwError = throwErr;
    }
    
    @Override
    public InputStream readFrom(String spec) throws IOException {
      if (!isLastSpecUpdated)
        lastSpec = spec;
      
      isLastSpecUpdated = false;

      if (throwError) {
        throw new IOException("Fake error");
      }
      
      String responseStr = "{\"alerts\": [{\"Alert Body\": \"Body\"}],"
          + " \"hostcounts\": {\"up_hosts\":\"1\", \"down_hosts\":\"0\"}}";
        return new ByteArrayInputStream(responseStr.getBytes("UTF-8"));
    }

    public String getLastSpec() {
      return lastSpec;
    }

    @Override
    public InputStream readFrom(String spec, String requestMethod, String params) throws IOException {
      lastSpec = spec + "?" + params;
      isLastSpecUpdated = true;
      return readFrom(spec);
    }
  }
  
}
