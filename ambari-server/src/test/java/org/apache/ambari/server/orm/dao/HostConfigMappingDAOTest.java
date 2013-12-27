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
package org.apache.ambari.server.orm.dao;

import java.util.Date;
import java.util.List;

import junit.framework.Assert;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.entities.HostConfigMappingEntity;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.persist.PersistService;

/**
 * Tests host config mapping DAO and Entities
 */
public class HostConfigMappingDAOTest {

  private Injector injector;
  private HostConfigMappingDAO hostConfigMappingDAO;
  
  @Before
   public void setup() throws AmbariException{
    injector = Guice.createInjector(new InMemoryDefaultTestModule());
    injector.getInstance(GuiceJpaInitializer.class);
    
    hostConfigMappingDAO = injector.getInstance(HostConfigMappingDAO.class);
  }

  @After
  public void teardown() throws AmbariException {
    injector.getInstance(PersistService.class).stop();
  }
  
  private HostConfigMappingEntity createEntity(long clusterId, String host, String type, String version) throws Exception {
    HostConfigMappingEntity entity = new HostConfigMappingEntity();
    entity.setClusterId(Long.valueOf(clusterId));
    entity.setCreateTimestamp(Long.valueOf(System.currentTimeMillis()));
    entity.setHostName(host);
    entity.setSelected(1);
    entity.setType(type);
    entity.setVersion(version);
    entity.setUser("_test");
    
    hostConfigMappingDAO.create(entity);
    
    return entity;
  }
  
  @Test
  public void testCreate() throws Exception {
    createEntity(1L, "h1", "global", "v1");
  }
  

  @Test
  public void testFindByType() throws Exception {
    HostConfigMappingEntity source = createEntity(1L, "h1", "global", "v1");
    
    List<HostConfigMappingEntity> target = hostConfigMappingDAO.findByType(1L, "h1", "global");

    Assert.assertEquals("Expected one result", 1, target.size());
    Assert.assertEquals("Expected version 'v1'", source.getVersion(), target.get(0).getVersion());
  }
  
  @Test
  public void testMerge() throws Exception {
    HostConfigMappingEntity source = createEntity(1L, "h1", "global", "v1");
    
    List<HostConfigMappingEntity> target = hostConfigMappingDAO.findByType(1L, "h1", "global");

    Assert.assertEquals("Expected one result", 1, target.size());
    Assert.assertEquals("Expected version 'v1'", source.getVersion(), target.get(0).getVersion());
    Assert.assertEquals("Expected selected flag 1", 1, target.get(0).isSelected());
    
    HostConfigMappingEntity toChange = target.get(0);
    
    toChange.setSelected(0);
    
    hostConfigMappingDAO.merge(toChange);
    
    target = hostConfigMappingDAO.findByType(1L, "h1", "global");

    Assert.assertEquals("Expected one result", 1, target.size());
    Assert.assertEquals("Expected version 'v1'", source.getVersion(), target.get(0).getVersion());
    Assert.assertEquals("Expected selected flag 0", 0, target.get(0).isSelected());
  }
  
  @Test
  public void testFindSelected() throws Exception {
    createEntity(1L, "h1", "global", "version1");
    HostConfigMappingEntity entity2 = createEntity(1L, "h1", "core-site", "version1");
    
    List<HostConfigMappingEntity> targets = hostConfigMappingDAO.findSelected(1L, "h1");
    Assert.assertEquals("Expected two entities", 2, targets.size());
    
    entity2.setSelected(0);
    hostConfigMappingDAO.merge(entity2);
    
    createEntity(1L, "h1", "core-site", "version2");

    targets = hostConfigMappingDAO.findSelected(1L, "h1");
    Assert.assertEquals("Expected two entities", 2, targets.size());
  }
  
  @Test
  public void testFindSelectedByType() throws Exception {
    HostConfigMappingEntity entity1 = createEntity(1L, "h1", "global", "version1");
    
    HostConfigMappingEntity target = hostConfigMappingDAO.findSelectedByType(1L, "h1", "core-site");
    Assert.assertNull("Expected null entity for type 'core-site'", target);
    
    target = hostConfigMappingDAO.findSelectedByType(1L, "h1", "global");
    Assert.assertNotNull("Expected non-null entity for type 'global'", target);
    Assert.assertEquals("Expected version to be '" + entity1.getVersion() + "'", entity1.getVersion(), target.getVersion());
    
    target.setSelected(0);
    hostConfigMappingDAO.merge(target);
    
    HostConfigMappingEntity entity2 = createEntity(1L, "h1", "global", "version2");
    
    target = hostConfigMappingDAO.findSelectedByType(1L, "h1", "global");
    Assert.assertNotNull("Expected non-null entity for type 'global'", target);
    
    Assert.assertEquals("Expected version to be '" + entity2.getVersion() + "'", entity2.getVersion(), target.getVersion());
    
    Assert.assertEquals("Expected instance equality", entity2, target);
  }
  
  @Test
  public void testFindSelectedHostsByType() throws Exception {
    HostConfigMappingEntity entity1 = createEntity(1L, "h1", "global", "v1");
    HostConfigMappingEntity entity2 = createEntity(1L, "h2", "global", "v1");
    
    List<HostConfigMappingEntity> list = hostConfigMappingDAO.findSelectedHostsByType(1L, "global");
    Assert.assertEquals("Expected two hosts", 2, list.size());
    
    entity2.setSelected(0);
    hostConfigMappingDAO.merge(entity2);
    
    list = hostConfigMappingDAO.findSelectedHostsByType(1L, "global");
    Assert.assertEquals("Expected one host", 1, list.size());
    
    
    list = hostConfigMappingDAO.findSelectedHostsByType(1L, "core-site");
    Assert.assertEquals("Expected no matching hosts", 0, list.size());
    
    entity1.setSelected(0);
    hostConfigMappingDAO.merge(entity1);
    
    list = hostConfigMappingDAO.findSelectedHostsByType(1L, "global");
    Assert.assertEquals("Expected no selected hosts", 0, list.size());
  }
  

  
  
  
  
}
