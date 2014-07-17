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

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.utilities.PredicateBuilder;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.orm.dao.AlertDefinitionDAO;
import org.apache.ambari.server.orm.entities.AlertDefinitionEntity;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.alert.MetricAlert;
import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * AlertDefinition tests
 */
public class AlertDefinitionResourceProviderTest {

  AlertDefinitionDAO dao = null;
  
  @Before
  public void before() {
    dao = EasyMock.createStrictMock(AlertDefinitionDAO.class);
    
    AlertDefinitionResourceProvider.init(dao);
  }
  
  @Test
  public void testGetResourcesNoPredicate() throws Exception {
    AlertDefinitionResourceProvider provider = createProvider(null);
    
    Request request = PropertyHelper.getReadRequest("AlertDefinition/cluster_name",
        "AlertDefinition/id");
    
    EasyMock.expect(dao.findAll()).andReturn(getMockEntities());

    EasyMock.replay(dao);
    
    Set<Resource> results = provider.getResources(request, null);
    
    assertEquals(0, results.size());
  }  

  @Test
  public void testGetResourcesClusterPredicate() throws Exception {
    Request request = PropertyHelper.getReadRequest(
        AlertDefinitionResourceProvider.ALERT_DEF_CLUSTER_NAME,
        AlertDefinitionResourceProvider.ALERT_DEF_ID,
        AlertDefinitionResourceProvider.ALERT_DEF_NAME);
    
    AmbariManagementController amc = EasyMock.createMock(AmbariManagementController.class);
    Clusters clusters = EasyMock.createMock(Clusters.class);
    Cluster cluster = EasyMock.createMock(Cluster.class);
    EasyMock.expect(amc.getClusters()).andReturn(clusters).atLeastOnce();
    EasyMock.expect(clusters.getCluster(EasyMock.<String>anyObject())).andReturn(cluster).atLeastOnce();
    EasyMock.expect(cluster.getClusterId()).andReturn(Long.valueOf(1)).anyTimes();
    
    Predicate predicate = new PredicateBuilder().property(
        AlertDefinitionResourceProvider.ALERT_DEF_CLUSTER_NAME).equals("c1").toPredicate();    
    
    EasyMock.expect(dao.findAll(1L)).andReturn(getMockEntities());

    EasyMock.replay(amc, clusters, cluster, dao);
    
    AlertDefinitionResourceProvider provider = createProvider(amc);    
    Set<Resource> results = provider.getResources(request, predicate);
    
    assertEquals(1, results.size());
    
    Resource r = results.iterator().next();
    
    Assert.assertEquals("my_def", r.getPropertyValue(AlertDefinitionResourceProvider.ALERT_DEF_NAME));
  }
  
  @Test
  public void testGetSingleResource() throws Exception {
    Request request = PropertyHelper.getReadRequest(
        AlertDefinitionResourceProvider.ALERT_DEF_CLUSTER_NAME,
        AlertDefinitionResourceProvider.ALERT_DEF_ID,
        AlertDefinitionResourceProvider.ALERT_DEF_NAME,
        AlertDefinitionResourceProvider.ALERT_DEF_SOURCE_TYPE);
    
    AmbariManagementController amc = EasyMock.createMock(AmbariManagementController.class);
    Clusters clusters = EasyMock.createMock(Clusters.class);
    Cluster cluster = EasyMock.createMock(Cluster.class);
    EasyMock.expect(amc.getClusters()).andReturn(clusters).atLeastOnce();
    EasyMock.expect(clusters.getCluster(EasyMock.<String>anyObject())).andReturn(cluster).atLeastOnce();
    EasyMock.expect(cluster.getClusterId()).andReturn(Long.valueOf(1)).anyTimes();
    
    Predicate predicate = new PredicateBuilder().property(
        AlertDefinitionResourceProvider.ALERT_DEF_CLUSTER_NAME).equals("c1")
          .and().property(AlertDefinitionResourceProvider.ALERT_DEF_ID).equals("1").toPredicate();    
    
    EasyMock.expect(dao.findById(1L)).andReturn(getMockEntities().get(0));

    EasyMock.replay(amc, clusters, cluster, dao);
    
    AlertDefinitionResourceProvider provider = createProvider(amc);    
    Set<Resource> results = provider.getResources(request, predicate);
    
    assertEquals(1, results.size());
    
    Resource r = results.iterator().next();
    
    Assert.assertEquals("my_def", r.getPropertyValue(AlertDefinitionResourceProvider.ALERT_DEF_NAME));
    Assert.assertEquals("metric", r.getPropertyValue(AlertDefinitionResourceProvider.ALERT_DEF_SOURCE_TYPE));
    Assert.assertNotNull(r.getPropertyValue("AlertDefinition/metric"));
    Assert.assertEquals(MetricAlert.class, r.getPropertyValue("AlertDefinition/metric").getClass());
  }
  
  private AlertDefinitionResourceProvider createProvider(AmbariManagementController amc) {
    return new AlertDefinitionResourceProvider(
        PropertyHelper.getPropertyIds(Resource.Type.AlertDefinition),
        PropertyHelper.getKeyPropertyIds(Resource.Type.AlertDefinition),
        amc);
  }
  
  private List<AlertDefinitionEntity> getMockEntities() {
    AlertDefinitionEntity entity = new AlertDefinitionEntity();
    entity.setClusterId(Long.valueOf(1L));
    entity.setComponentName(null);
    entity.setDefinitionId(Long.valueOf(1L));
    entity.setDefinitionName("my_def");
    entity.setEnabled(true);
    entity.setHash("tmphash");
    entity.setScheduleInterval(Long.valueOf(2L));
    entity.setServiceName(null);
    entity.setSourceType("metric");
    entity.setSource("{'jmx': 'beanName/attributeName', 'host': '{{aa:123445}}'}");
    
    return Arrays.asList(entity);
  }
  
}
