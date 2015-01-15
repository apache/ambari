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

import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.utilities.PredicateBuilder;
import org.apache.ambari.server.orm.dao.ArtifactDAO;
import org.apache.ambari.server.orm.entities.ArtifactEntity;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.easymock.Capture;
import org.junit.Before;
import org.junit.Test;

import javax.persistence.EntityManager;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.createStrictMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * ArtifactResourceProvider unit tests.
 */
public class ArtifactResourceProviderTest {

  private ArtifactDAO dao = createStrictMock(ArtifactDAO.class);
  private EntityManager em = createStrictMock(EntityManager.class);
  private AmbariManagementController controller = createStrictMock(AmbariManagementController.class);
  private Request request = createStrictMock(Request.class);
  private Clusters clusters = createStrictMock(Clusters.class);
  private Cluster cluster = createStrictMock(Cluster.class);
  private ArtifactEntity entity = createMock(ArtifactEntity.class);
  private ArtifactEntity entity2 = createMock(ArtifactEntity.class);

  ArtifactResourceProvider resourceProvider;

  @Before
  public void setUp() throws Exception {
    reset(dao, em, controller, request, clusters, cluster, entity, entity2);
    resourceProvider = new ArtifactResourceProvider(controller);
    setPrivateField(resourceProvider, "artifactDAO", dao);
  }

  @Test
  public void testGetResources_instance() throws Exception {
    Set<String> propertyIds = new HashSet<String>();
    TreeMap<String, String> foreignKeys = new TreeMap<String, String>();
    foreignKeys.put("cluster", "500");

    Map<String, Object> artifact_data = Collections.<String, Object>singletonMap("foo", "bar");

    Map<String, String> responseForeignKeys = new HashMap<String, String>();
    responseForeignKeys.put("cluster", "500");

    // expectations
    expect(controller.getClusters()).andReturn(clusters).anyTimes();
    expect(clusters.getCluster("test-cluster")).andReturn(cluster).anyTimes();
    expect(clusters.getClusterById(500L)).andReturn(cluster).anyTimes();
    expect(cluster.getClusterId()).andReturn(500L).anyTimes();
    expect(cluster.getClusterName()).andReturn("test-cluster").anyTimes();

    expect(request.getPropertyIds()).andReturn(propertyIds).anyTimes();

    expect(dao.findByNameAndForeignKeys(eq("test-artifact"), eq(foreignKeys))).andReturn(entity).once();
    expect(entity.getArtifactName()).andReturn("test-artifact").anyTimes();
    expect(entity.getForeignKeys()).andReturn(responseForeignKeys).anyTimes();
    expect(entity.getArtifactData()).andReturn(artifact_data).anyTimes();

    // end of expectation setting
    replay(dao, em, controller, request, clusters, cluster, entity, entity2);

    // test
    PredicateBuilder pb = new PredicateBuilder();
    Predicate predicate = pb.begin().property("Artifacts/cluster_name").equals("test-cluster").and().
        property("Artifacts/artifact_name").equals("test-artifact").end().toPredicate();

    Set<Resource> response = resourceProvider.getResources(request, predicate);
    assertEquals(1, response.size());
    Resource resource = response.iterator().next();
    assertEquals("test-artifact", resource.getPropertyValue("Artifacts/artifact_name"));
    assertEquals("test-cluster", resource.getPropertyValue("Artifacts/cluster_name"));
    assertEquals("bar", resource.getPropertyValue("artifact_data/foo"));
  }

  @Test
  public void testGetResources_collection() throws Exception {
    Set<String> propertyIds = new HashSet<String>();
    TreeMap<String, String> foreignKeys = new TreeMap<String, String>();
    foreignKeys.put("cluster", "500");

    List<ArtifactEntity> entities = new ArrayList<ArtifactEntity>();
    entities.add(entity);
    entities.add(entity2);

    Map<String, Object> artifact_data = Collections.<String, Object>singletonMap("foo", "bar");
    Map<String, Object> artifact_data2 = Collections.<String, Object>singletonMap("foo2", "bar2");

    Map<String, String> responseForeignKeys = new HashMap<String, String>();
    responseForeignKeys.put("cluster", "500");

    // expectations
    expect(controller.getClusters()).andReturn(clusters).anyTimes();
    expect(clusters.getCluster("test-cluster")).andReturn(cluster).anyTimes();
    expect(clusters.getClusterById(500L)).andReturn(cluster).anyTimes();
    expect(cluster.getClusterId()).andReturn(500L).anyTimes();
    expect(cluster.getClusterName()).andReturn("test-cluster").anyTimes();

    expect(request.getPropertyIds()).andReturn(propertyIds).anyTimes();

    expect(dao.findByForeignKeys(eq(foreignKeys))).andReturn(entities).anyTimes();
    expect(entity.getArtifactName()).andReturn("test-artifact").anyTimes();
    expect(entity.getForeignKeys()).andReturn(responseForeignKeys).anyTimes();
    expect(entity.getArtifactData()).andReturn(artifact_data).anyTimes();
    expect(entity2.getArtifactName()).andReturn("test-artifact2").anyTimes();
    expect(entity2.getForeignKeys()).andReturn(responseForeignKeys).anyTimes();
    expect(entity2.getArtifactData()).andReturn(artifact_data2).anyTimes();

    // end of expectation setting
    replay(dao, em, controller, request, clusters, cluster, entity, entity2);

    // test
    PredicateBuilder pb = new PredicateBuilder();
    Predicate predicate = pb.begin().property("Artifacts/cluster_name").equals("test-cluster").end().toPredicate();

    Set<Resource> response = resourceProvider.getResources(request, predicate);
    assertEquals(2, response.size());

    boolean artifact1Returned = false;
    boolean artifact2Returned = false;
    for (Resource resource : response) {
      if (resource.getPropertyValue("Artifacts/artifact_name").equals("test-artifact")) {
        artifact1Returned = true;
        assertEquals("bar", resource.getPropertyValue("artifact_data/foo"));
        assertEquals("test-cluster", resource.getPropertyValue("Artifacts/cluster_name"));
      } else if (resource.getPropertyValue("Artifacts/artifact_name").equals("test-artifact2")) {
        artifact2Returned = true;
        assertEquals("bar2", resource.getPropertyValue("artifact_data/foo2"));
        assertEquals("test-cluster", resource.getPropertyValue("Artifacts/cluster_name"));
      } else {
        fail("unexpected artifact name");
      }
    }
    assertTrue(artifact1Returned);
    assertTrue(artifact2Returned);
  }

  @Test
  public void testCreateResource() throws Exception {
    Capture<ArtifactEntity> createEntityCapture = new Capture<ArtifactEntity>();

    Map<String, Object> artifact_data = Collections.<String, Object>singletonMap("foo", "bar");

    TreeMap<String, String> foreignKeys = new TreeMap<String, String>();
    foreignKeys.put("cluster", "500");

    Map<String, Object> properties = new HashMap<String, Object>();
    properties.put("Artifacts/artifact_name", "test-artifact");
    properties.put("Artifacts/cluster_name", "test-cluster");
    properties.put("artifact_data/foo", "bar");
    Set<Map<String, Object>> requestProperties = Collections.singleton(properties);

    // expectations
    expect(request.getProperties()).andReturn(requestProperties).anyTimes();
    expect(controller.getClusters()).andReturn(clusters).anyTimes();
    expect(clusters.getCluster("test-cluster")).andReturn(cluster).anyTimes();
    expect(clusters.getClusterById(500L)).andReturn(cluster).anyTimes();
    expect(cluster.getClusterId()).andReturn(500L).anyTimes();
    expect(cluster.getClusterName()).andReturn("test-cluster").anyTimes();

    // check to see if entity already exists
    expect(dao.findByNameAndForeignKeys(eq("test-artifact"), eq(foreignKeys))).andReturn(null).once();
    // create
    dao.create(capture(createEntityCapture));

    // end of expectation setting
    replay(dao, em, controller, request, clusters, cluster, entity, entity2);

    resourceProvider.createResources(request);

    ArtifactEntity createEntity = createEntityCapture.getValue();
    assertEquals("test-artifact", createEntity.getArtifactName());
    assertEquals(createEntity.getArtifactData(), artifact_data);
    assertEquals(foreignKeys, createEntity.getForeignKeys());
  }


  private void setPrivateField(Object o, String field, Object value) throws Exception{
    Class<?> c = o.getClass();
    Field f = c.getDeclaredField(field);
    f.setAccessible(true);
    f.set(o, value);
  }
}
