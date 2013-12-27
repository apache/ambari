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


import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.RepositoryRequest;
import org.apache.ambari.server.controller.RepositoryResponse;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceProvider;
import org.apache.ambari.server.controller.utilities.PredicateBuilder;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Test;

public class RepositoryResourceProviderTest {
  
  private static final String VAL_STACK_NAME = "HDP";
  private static final String VAL_STACK_VERSION = "0.2";
  private static final String VAL_OS = "centos6";
  private static final String VAL_REPO_ID = "HDP-0.2";
  private static final String VAL_REPO_NAME = "HDP1";
  private static final String VAL_BASE_URL = "http://foo.com";

  @Test
  public void testGetResources() throws Exception{
    Resource.Type type = Resource.Type.Repository;

    AmbariManagementController managementController = EasyMock.createMock(AmbariManagementController.class);

    RepositoryResponse rr = new RepositoryResponse(VAL_BASE_URL, VAL_OS,
        VAL_REPO_ID, VAL_REPO_NAME, null, null);
    rr.setStackName(VAL_STACK_NAME);
    rr.setStackVersion(VAL_STACK_VERSION);
    Set<RepositoryResponse> allResponse = new HashSet<RepositoryResponse>();
    allResponse.add(rr);
    
    // set expectations
    expect(managementController.getRepositories(EasyMock.<Set<RepositoryRequest>>anyObject())).andReturn(allResponse).times(1);

    // replay
    replay(managementController);

    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
        type,
        PropertyHelper.getPropertyIds(type),
        PropertyHelper.getKeyPropertyIds(type),
        managementController);

    Set<String> propertyIds = new HashSet<String>();
    propertyIds.add(RepositoryResourceProvider.STACK_NAME_PROPERTY_ID);
    propertyIds.add(RepositoryResourceProvider.STACK_VERSION_PROPERTY_ID);
    propertyIds.add(RepositoryResourceProvider.REPOSITORY_NAME_PROPERTY_ID);
    propertyIds.add(RepositoryResourceProvider.REPOSITORY_BASE_URL_PROPERTY_ID);
    propertyIds.add(RepositoryResourceProvider.REPOSITORY_OS_TYPE_PROPERTY_ID);
    propertyIds.add(RepositoryResourceProvider.REPO_ID_PROPERTY_ID);

    Predicate predicate =
        new PredicateBuilder().property(RepositoryResourceProvider.STACK_NAME_PROPERTY_ID).equals(VAL_STACK_NAME)
          .and().property(RepositoryResourceProvider.STACK_VERSION_PROPERTY_ID).equals(VAL_STACK_VERSION)
          .toPredicate();
    
    // create the request
    Request request = PropertyHelper.getReadRequest(propertyIds);

    // get all ... no predicate
    Set<Resource> resources = provider.getResources(request, predicate);

    Assert.assertEquals(allResponse.size(), resources.size());
    
    for (Resource resource : resources) {
      Object o = resource.getPropertyValue(RepositoryResourceProvider.STACK_NAME_PROPERTY_ID);
      Assert.assertEquals(VAL_STACK_NAME, o);
      
      o = resource.getPropertyValue(RepositoryResourceProvider.STACK_VERSION_PROPERTY_ID);
      Assert.assertEquals(VAL_STACK_VERSION, o);
      
      o = resource.getPropertyValue(RepositoryResourceProvider.REPOSITORY_NAME_PROPERTY_ID);
      Assert.assertEquals(o, VAL_REPO_NAME);
      
      o = resource.getPropertyValue(RepositoryResourceProvider.REPOSITORY_BASE_URL_PROPERTY_ID);
      Assert.assertEquals(o, VAL_BASE_URL);
      
      o = resource.getPropertyValue(RepositoryResourceProvider.REPOSITORY_OS_TYPE_PROPERTY_ID);
      Assert.assertEquals(o, VAL_OS);
      
      o = resource.getPropertyValue(RepositoryResourceProvider.REPO_ID_PROPERTY_ID);
      Assert.assertEquals(o, VAL_REPO_ID);

    }

    // verify
    verify(managementController);
  }
  
  @Test
  public void testUpdateResources() throws Exception {
    Resource.Type type = Resource.Type.Repository;

    AmbariManagementController managementController = EasyMock.createMock(AmbariManagementController.class);

    RepositoryResponse rr = new RepositoryResponse(VAL_BASE_URL, VAL_OS,
        VAL_REPO_ID, VAL_REPO_NAME, null, null);
    Set<RepositoryResponse> allResponse = new HashSet<RepositoryResponse>();
    allResponse.add(rr);    
    
    // set expectations
    expect(managementController.getRepositories(EasyMock.<Set<RepositoryRequest>>anyObject())).andReturn(allResponse).times(1);
    managementController.updateRespositories(EasyMock.<Set<RepositoryRequest>>anyObject());

    // replay
    replay(managementController);
    
    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
        type,
        PropertyHelper.getPropertyIds(type),
        PropertyHelper.getKeyPropertyIds(type),
        managementController);

    // add the property map to a set for the request.
    Map<String, Object> properties = new LinkedHashMap<String, Object>();
    properties.put(RepositoryResourceProvider.REPOSITORY_BASE_URL_PROPERTY_ID, "http://garbage.eu.co");

    // create the request
    Request request = PropertyHelper.getUpdateRequest(properties, null);
    
    Predicate predicate =
        new PredicateBuilder().property(RepositoryResourceProvider.STACK_NAME_PROPERTY_ID).equals(VAL_STACK_NAME)
          .and().property(RepositoryResourceProvider.STACK_VERSION_PROPERTY_ID).equals(VAL_STACK_VERSION)
          .toPredicate();
    
    provider.updateResources(request, predicate);    
    
    // verify
    verify(managementController);
  }

}
