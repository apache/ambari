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

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import com.google.inject.Singleton;

import org.apache.ambari.server.orm.cache.HostConfigMapping;
import org.apache.ambari.server.orm.cache.HostConfigMappingImpl;
import org.apache.ambari.server.orm.entities.HostConfigMappingEntity;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.persist.Transactional;

/**
 * Used for host configuration mapping operations.
 */
@Singleton
public class HostConfigMappingDAO {
  @Inject
  Provider<EntityManager> entityManagerProvider;
  @Inject
  DaoUtils daoUtils;
  

  private final ReadWriteLock gl = new ReentrantReadWriteLock();
  private Map<String, Set<HostConfigMapping>> hostConfigMappingByHost;
  
  private volatile boolean cacheLoaded;
  
  private void populateCache() {
    
    if (!cacheLoaded) {
      gl.writeLock().lock();
      try {
        gl.writeLock().lock();
        try {
          if (hostConfigMappingByHost == null) {
            hostConfigMappingByHost = new WeakHashMap<String, Set<HostConfigMapping>>();
            
            TypedQuery<HostConfigMappingEntity> query = entityManagerProvider.get().createQuery(
                "SELECT entity FROM HostConfigMappingEntity entity",
                HostConfigMappingEntity.class);

            List<HostConfigMappingEntity> hostConfigMappingEntities = daoUtils.selectList(query);
            
            for (HostConfigMappingEntity hostConfigMappingEntity : hostConfigMappingEntities) {

              Set<HostConfigMapping> setByHost = hostConfigMappingByHost.get((hostConfigMappingEntity.getHostName()));
              
              if (setByHost == null) {
                setByHost = new HashSet<HostConfigMapping>();
                hostConfigMappingByHost.put(hostConfigMappingEntity.getHostName(), setByHost);
              }
       
              HostConfigMapping hostConfigMapping = buildHostConfigMapping(hostConfigMappingEntity);
              setByHost.add(hostConfigMapping);
            } 
          }
        } finally {
          gl.writeLock().unlock();
        }
      } finally {
        gl.writeLock().unlock();
      }
      
      cacheLoaded = true;

    }
    
  }
  

  @Transactional
  public void create(HostConfigMapping hostConfigMapping) {
    
    populateCache();
    
    //create in db
    entityManagerProvider.get().persist(buildHostConfigMappingEntity(hostConfigMapping));
    
    //create in cache
    Set<HostConfigMapping> set = hostConfigMappingByHost.get(hostConfigMapping.getHostName());
    if (set == null){
      set = new HashSet<HostConfigMapping>();
      hostConfigMappingByHost.put(hostConfigMapping.getHostName(), set);
    }
    
    set.add(hostConfigMapping);
  }

  @Transactional
  public HostConfigMapping merge(HostConfigMapping hostConfigMapping) {
    
    populateCache();
    
    Set<HostConfigMapping> set = hostConfigMappingByHost.get(hostConfigMapping.getHostName());
    if (set == null){
      set = new HashSet<HostConfigMapping>();
      hostConfigMappingByHost.put(hostConfigMapping.getHostName(), set);
    }
    
    //Update object in set
    set.remove(hostConfigMapping);
    set.add(hostConfigMapping);
    
    entityManagerProvider.get().merge(buildHostConfigMappingEntity(hostConfigMapping));
    
    return hostConfigMapping;
  }

  @Transactional
  public Set<HostConfigMapping> findByType(final long clusterId, String hostName, final String type) {
    
    populateCache();
    
    if (!hostConfigMappingByHost.containsKey(hostName))
      return Collections.emptySet();
      
    Set<HostConfigMapping> set = new HashSet<HostConfigMapping>(hostConfigMappingByHost.get(hostName));
     
    CollectionUtils.filter(set, new Predicate() {
        
      @Override
      public boolean evaluate(Object arg0) {
                  
        return ((HostConfigMapping) arg0).getClusterId().equals(clusterId) 
            && ((HostConfigMapping) arg0).getType().equals(type);
      }
    });

    return set;
  }

  @Transactional
  public HostConfigMapping findSelectedByType(final long clusterId,
      String hostName, final String type) {
    
    populateCache();
    
    if (!hostConfigMappingByHost.containsKey(hostName))
      return null;
    
    Set<HostConfigMapping> set = new HashSet<HostConfigMapping>(hostConfigMappingByHost.get(hostName));
    
    HostConfigMapping result = (HostConfigMapping) CollectionUtils.find(set, new Predicate() {
      
      @Override
      public boolean evaluate(Object arg0) {
        
        return ((HostConfigMapping) arg0).getClusterId().equals(clusterId) 
            && ((HostConfigMapping) arg0).getType().equals(type)
            && ((HostConfigMapping) arg0).getSelected() > 0;
      }
    });
    
    
    return result;
    
  }

  @Transactional
  public Set<HostConfigMapping> findSelected(final long clusterId, String hostName) {
    
    populateCache();
    
    if (!hostConfigMappingByHost.containsKey(hostName))
      return Collections.emptySet();
      
    
    Set<HostConfigMapping> set = new HashSet<HostConfigMapping>(hostConfigMappingByHost.get(hostName));
    
    CollectionUtils.filter(set, new Predicate() {
      
      @Override
      public boolean evaluate(Object arg0) {
        return ((HostConfigMapping) arg0).getClusterId().equals(clusterId) 
            && ((HostConfigMapping) arg0).getSelected() > 0;
      }
    });
    
    return set;
  }

  @Transactional
  public Set<HostConfigMapping> findSelectedByHosts(long clusterId, Collection<String> hostNames) {
    
    populateCache();

    if (hostNames == null || hostNames.isEmpty()) {
      return Collections.emptySet();
    }
    
    
    HashSet<HostConfigMapping> result = new HashSet<HostConfigMapping>();
    


    for (final String hostName : hostNames) {
      
      if (!hostConfigMappingByHost.containsKey(hostName))
        continue;
      
      Set<HostConfigMapping> set = new HashSet<HostConfigMapping>(hostConfigMappingByHost.get(hostName));
      
      CollectionUtils.filter(set, new Predicate() {
        
        @Override
        public boolean evaluate(Object arg0) {
          return ((HostConfigMapping) arg0).getHostName().equals(hostName) && 
              ((HostConfigMapping) arg0).getSelected() > 0;
        }
      });
      
      result.addAll(set);
    } 
    
    return result;
  }


  @Transactional
  public Map<String, List<HostConfigMapping>> findSelectedHostsByTypes(final long clusterId,
                                                                             Collection<String> types) {
    
    populateCache();

    
    Map<String, List<HostConfigMapping>> mappingsByType = new HashMap<String, List<HostConfigMapping>>();
    
    for (String type : types) {
      if (!mappingsByType.containsKey(type)) {
        mappingsByType.put(type, new ArrayList<HostConfigMapping>());
      }
    }

    if (!types.isEmpty()) {
      List<HostConfigMapping> mappings = new ArrayList<HostConfigMapping>();

      for (Set<HostConfigMapping> entries : hostConfigMappingByHost.values()) {
        
        for (HostConfigMapping entry : entries) {
          
          if (types.contains(entry.getType()) && entry.getClusterId().equals(clusterId))
            mappings.add(new HostConfigMappingImpl(entry));
          
        }
      }

      for (HostConfigMapping mapping : mappings) {
        mappingsByType.get(mapping.getType()).add(mapping);
      }
    }
    
    

    return mappingsByType;
  }

  @Transactional
  public List<HostConfigMappingEntity> findAll() {
    return daoUtils.selectAll(entityManagerProvider.get(), HostConfigMappingEntity.class);
  }

  /**
   * @param clusterId
   * @param hostName
   */
  @Transactional
  public void removeHost(final long clusterId, String hostName) {
    
    populateCache();
    
    Set<HostConfigMapping> set = hostConfigMappingByHost.get(hostName);
    
    //Remove from cache items with clusterId
    CollectionUtils.filter(set, new Predicate() {
      
      @Override
      public boolean evaluate(Object arg0) {
        return !((HostConfigMapping) arg0).getClusterId().equals(clusterId);
      }
    });
    
    //delete from db
    TypedQuery<HostConfigMappingEntity> query = entityManagerProvider.get().createQuery(
      "SELECT entity FROM HostConfigMappingEntity entity " +
      "WHERE entity.clusterId = ?1 AND entity.hostName = ?2",
      HostConfigMappingEntity.class);
             
    List<HostConfigMappingEntity> list = daoUtils.selectList(query, Long.valueOf(clusterId), hostName);
        
    for (HostConfigMappingEntity entity : list) {
      entityManagerProvider.get().remove(entity);
    }
    
    
  }

  public HostConfigMappingEntity buildHostConfigMappingEntity(HostConfigMapping hostConfigMapping) {
    
    HostConfigMappingEntity hostConfigMappingEntity = new HostConfigMappingEntity();
    
    hostConfigMappingEntity.setClusterId(hostConfigMapping.getClusterId());
    hostConfigMappingEntity.setCreateTimestamp(hostConfigMapping.getCreateTimestamp());
    hostConfigMappingEntity.setHostName(hostConfigMapping.getHostName());
    hostConfigMappingEntity.setSelected(hostConfigMapping.getSelected());
    hostConfigMappingEntity.setServiceName(hostConfigMapping.getServiceName());
    hostConfigMappingEntity.setType(hostConfigMapping.getType());
    hostConfigMappingEntity.setUser(hostConfigMapping.getUser());
    hostConfigMappingEntity.setVersion(hostConfigMapping.getVersion());
    
    return hostConfigMappingEntity;
  }
  
  public HostConfigMapping buildHostConfigMapping(
      HostConfigMappingEntity hostConfigMappingEntity) {
    
    HostConfigMapping hostConfigMapping = new HostConfigMappingImpl();
    
    hostConfigMapping.setClusterId(hostConfigMappingEntity.getClusterId());
    hostConfigMapping.setCreateTimestamp(hostConfigMappingEntity.getCreateTimestamp());
    hostConfigMapping.setHostName(hostConfigMappingEntity.getHostName());
    hostConfigMapping.setServiceName(hostConfigMappingEntity.getServiceName());
    hostConfigMapping.setType(hostConfigMappingEntity.getType());
    hostConfigMapping.setUser(hostConfigMappingEntity.getUser());
    hostConfigMapping.setSelected(hostConfigMappingEntity.isSelected());
    hostConfigMapping.setVersion(hostConfigMappingEntity.getVersion());
    
    return hostConfigMapping;
  }

}
