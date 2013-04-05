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
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.controller.utilities.StreamProvider;
import org.junit.Assert;
import org.junit.Test;

public class HttpPropertyProviderTest {
  private static final String PROPERTY_ID_CLUSTER_NAME = PropertyHelper.getPropertyId("HostRoles", "cluster_name");
  private static final String PROPERTY_ID_HOST_NAME = PropertyHelper.getPropertyId("HostRoles", "host_name");
  private static final String PROPERTY_ID_COMPONENT_NAME = PropertyHelper.getPropertyId("HostRoles", "component_name");
  
  private static final String PROPERTY_ID_NAGIOS_ALERTS = PropertyHelper.getPropertyId("HostRoles", "nagios_alerts");

  @Test
  public void testReadNagiosServer() throws Exception {

    Resource resource = doPopulate("NAGIOS_SERVER", Collections.<String>emptySet());
    
    Assert.assertNotNull("Expected non-null for 'nagios_alerts'",
      resource.getPropertyValue(PROPERTY_ID_NAGIOS_ALERTS));
  }
  
  @Test
  public void testReadNotRequested() throws Exception {
   
   Set<String> propertyIds = new HashSet<String>();
   propertyIds.add(PropertyHelper.getPropertyId("HostRoles", "state"));
   propertyIds.add(PROPERTY_ID_COMPONENT_NAME);
   
   Resource resource = doPopulate("NAGIOS_SERVER", propertyIds);
   
   Assert.assertNull("Expected null for 'nagios_alerts'",
     resource.getPropertyValue(PROPERTY_ID_NAGIOS_ALERTS));    
  }
  
  @Test
  public void testReadWithRequested() throws Exception {
    
   Set<String> propertyIds = new HashSet<String>();
   propertyIds.add(PropertyHelper.getPropertyId("HostRoles", "nagios_alerts"));
   propertyIds.add(PROPERTY_ID_COMPONENT_NAME);
   
   Resource resource = doPopulate("NAGIOS_SERVER", propertyIds);
   
   Assert.assertNotNull("Expected non-null for 'nagios_alerts'",
     resource.getPropertyValue(PROPERTY_ID_NAGIOS_ALERTS));        
  }
  
  
  @Test
  public void testReadGangliaServer() throws Exception {
    
    Resource resource = doPopulate("GANGLIA_SERVER", Collections.<String>emptySet());

    // !!! GANGLIA_SERVER has no current http lookup
    Assert.assertNull("Expected null, was: " +
      resource.getPropertyValue(PROPERTY_ID_NAGIOS_ALERTS),
      resource.getPropertyValue(PROPERTY_ID_NAGIOS_ALERTS));
  }
  
  private Resource doPopulate(String componentName, Set<String> requestProperties) throws Exception {
    
    HttpProxyPropertyProvider propProvider = new HttpProxyPropertyProvider(
       new TestStreamProvider(),
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

    @Override
    public InputStream readFrom(String spec) throws IOException {
      return new ByteArrayInputStream("PROPERTY_TEST".getBytes());
    }
  }
  
  
}
