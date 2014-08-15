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
package org.apache.ambari.server.state.alert;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.controller.RootServiceResponseFactory.Components;
import org.apache.ambari.server.controller.RootServiceResponseFactory.Services;
import org.apache.ambari.server.orm.dao.AlertDefinitionDAO;
import org.apache.ambari.server.orm.entities.AlertDefinitionEntity;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * The {@link AlertDefinitionHash} class is used to generate an MD5 hash for a
 * list of {@link AlertDefinitionEntity}s. It is used in order to represent the
 * state of a group of definitions by using
 * {@link AlertDefinitionEntity#getHash()}
 */
@Singleton
public class AlertDefinitionHash {

  /**
   * Logger.
   */
  private final static Logger LOG = LoggerFactory.getLogger(AlertDefinitionHash.class);

  /**
   * The hash returned when there are no definitions to hash.
   */
  public static String NULL_MD5_HASH = "37a6259cc0c1dae299a7866489dff0bd";

  /**
   * DAO for retrieving {@link AlertDefinitionEntity} instances.
   */
  @Inject
  private AlertDefinitionDAO m_definitionDao;

  @Inject
  private AlertDefinitionFactory m_factory;

  /**
   * All clusters.
   */
  @Inject
  private Clusters m_clusters;

  /**
   * !!! TODO: this class needs some thoughts on locking
   */
  private ReadWriteLock m_lock = new ReentrantReadWriteLock();

  /**
   * The hashes for all hosts for any cluster. The key is the hostname and the
   * value is a map between cluster name and hash.
   */
  private Map<String, Map<String, String>> m_hashes = new HashMap<String, Map<String, String>>();

  /**
   * Gets a unique hash value reprssenting all of the alert definitions that
   * should be scheduled to run on a given host.
   * <p/>
   * This will not include alert definitions where the type is defined as
   * {@link SourceType#AGGREGATE} since aggregate definitions are not scheduled
   * to run on agent hosts.
   * <p/>
   * Hash values from this method are cached.
   *
   * @param clusterName
   *          the cluster name (not {@code null}).
   * @param hostName
   *          the host name (not {@code null}).
   * @return the unique hash or {@value #NULL_MD5_HASH} if none.
   */
  public String getHash(String clusterName, String hostName) {
    Map<String, String> clusterMapping = m_hashes.get(hostName);
    if (null == clusterMapping) {
      clusterMapping = new ConcurrentHashMap<String, String>();
      m_hashes.put(hostName, clusterMapping);
    }

    String hash = clusterMapping.get(hostName);
    if (null != hash) {
      return hash;
    }

    hash = hash(clusterName, hostName);
    clusterMapping.put(clusterName, hash);

    return hash;
  }

  /**
   * Gets a mapping between cluster and alert definition hashes for all of the
   * clusters that the given host belongs to.
   *
   * @param hostName
   *          the host name (not {@code null}).
   * @return a mapping between cluster and alert definition hash or an empty map
   *         (never @code null).
   * @see #getHash(String, String)
   * @throws AmbariException
   */
  public Map<String, String> getHashes(String hostName) throws AmbariException {
    Set<Cluster> clusters = m_clusters.getClustersForHost(hostName);
    if (null == clusters || clusters.size() == 0) {
      return Collections.emptyMap();
    }

    Map<String, String> hashes = new HashMap<String, String>();
    for (Cluster cluster : clusters) {
      String clusterName = cluster.getClusterName();
      String hash = getHash(clusterName, hostName);
      hashes.put(clusterName, hash);
    }

    return hashes;
  }

  /**
   * Invalidate all cached hashes causing subsequent lookups to recalculate.
   */
  public void invalidateAll() {
    m_hashes.clear();
  }

  /**
   * Invalidates the cached hash for the specified agent host across all
   * clusters.
   *
   * @param hostName
   *          the host to invalidate the cache for (not {@code null}).
   */
  public void invalidate(String hostName) {
    m_hashes.remove(hostName);
  }

  /**
   * Invalidates the cached hash for the specified agent host in the specified
   * cluster.
   *
   * @param clusterName
   *          the name of the cluster (not {@code null}).
   * @param hostName
   *          the host to invalidate the cache for (not {@code null}).
   */
  public void invalidate(String clusterName, String hostName) {
    Map<String, String> clusterMapping = m_hashes.get(hostName);
    if (null != clusterMapping) {
      clusterMapping.remove(clusterName);
    }
  }

  /**
   * Gets whether the alert definition has for the specified host has been
   * calculated and cached.
   *
   * @param hostName
   *          the host.
   * @return {@code true} if the hash was calculated; {@code false} otherwise.
   */
  public boolean isHashCached(String clusterName, String hostName) {
    if (null == clusterName || null == hostName) {
      return false;
    }

    Map<String, String> clusterMapping = m_hashes.get(hostName);
    if (null == clusterMapping) {
      return false;
    }

    return clusterMapping.containsKey(clusterName);
  }

  /**
   * Gets the alert definitions for the specified host. This will include the
   * following types of alert definitions:
   * <ul>
   * <li>Service/Component alerts</li>
   * <li>Service alerts where the host is a MASTER</li>
   * <li>Host alerts that are not bound to a service</li>
   * </ul>
   *
   * @param clusterName
   *          the cluster name (not {@code null}).
   * @param hostName
   *          the host name (not {@code null}).
   * @return the alert definitions for the host, or an empty set (never
   *         {@code null}).
   */
  public List<AlertDefinition> getAlertDefinitions(
      String clusterName,
      String hostName) {

    Set<AlertDefinitionEntity> entities = getAlertDefinitionEntities(
        clusterName, hostName);

    List<AlertDefinition> definitions = new ArrayList<AlertDefinition>(
        entities.size());

    for (AlertDefinitionEntity entity : entities) {
      definitions.add(m_factory.coerce(entity));
    }

    return definitions;
  }


  /**
   * Gets the alert definition entities for the specified host. This will include the
   * following types of alert definitions:
   * <ul>
   * <li>Service/Component alerts</li>
   * <li>Service alerts where the host is a MASTER</li>
   * <li>Host alerts that are not bound to a service</li>
   * </ul>
   *
   * @param clusterName
   *          the cluster name (not {@code null}).
   * @param hostName
   *          the host name (not {@code null}).
   * @return the alert definitions for the host, or an empty set (never
   *         {@code null}).
   */
  private Set<AlertDefinitionEntity> getAlertDefinitionEntities(
      String clusterName,
      String hostName) {
    Set<AlertDefinitionEntity> definitions = new HashSet<AlertDefinitionEntity>();

    try {
      Cluster cluster = m_clusters.getCluster(clusterName);
      if (null == cluster) {
        LOG.warn("Unable to get alert definitions for the missing cluster {}",
            clusterName);

        return Collections.emptySet();
      }

      long clusterId = cluster.getClusterId();
      List<ServiceComponentHost> serviceComponents = cluster.getServiceComponentHosts(hostName);
      if (null == serviceComponents || serviceComponents.size() == 0) {
        LOG.warn(
            "Unable to get alert definitions for {} since there are no service components defined",
            hostName);

        return Collections.emptySet();
      }

      for (ServiceComponentHost serviceComponent : serviceComponents) {
        String serviceName = serviceComponent.getServiceName();
        String componentName = serviceComponent.getServiceComponentName();

        // add all alerts for this service/component pair
        definitions.addAll(m_definitionDao.findByServiceComponent(
            clusterId, serviceName, componentName));
      }

      // for every service, get the master components and see if the host
      // is a master
      Set<String> services = new HashSet<String>();
      for (Entry<String, Service> entry : cluster.getServices().entrySet()) {
        Service service = entry.getValue();
        Map<String, ServiceComponent> components = service.getServiceComponents();
        for (Entry<String, ServiceComponent> component : components.entrySet()) {
          if (component.getValue().isMasterComponent()) {
            Map<String, ServiceComponentHost> hosts = component.getValue().getServiceComponentHosts();

            if( hosts.containsKey( hostName ) ){
              services.add(service.getName());
            }
          }
        }
      }

      // add all service scoped alerts
      if( services.size() > 0 ){
        definitions.addAll(m_definitionDao.findByServiceMaster(clusterId,
            services));
      }

      // add any alerts not bound to a service (host level alerts)
      definitions.addAll(m_definitionDao.findAgentScoped(clusterId));
    } catch (AmbariException ambariException) {
      LOG.error("Unable to get alert definitions", ambariException);
      return Collections.emptySet();
    }

    return definitions;
  }

  /**
   * Invalidate the hashes of any host that would be affected by the specified
   * definition.
   *
   * @param definition
   *          the definition to use to find the hosts to invlidate (not
   *          {@code null}).
   * @return the hosts that were invalidated, or an empty set (never
   *         {@code null}).
   */
  public Set<String> invalidateHosts(AlertDefinitionEntity definition) {
    long clusterId = definition.getClusterId();
    Set<String> invalidatedHosts = new HashSet<String>();

    Cluster cluster = null;
    Map<String, Host> hosts = null;
    String clusterName = null;
    try {
      cluster = m_clusters.getClusterById(clusterId);
      if (null != cluster) {
        clusterName = cluster.getClusterName();
        hosts = m_clusters.getHostsForCluster(clusterName);
      }

      if (null == cluster) {
        LOG.warn("Unable to lookup cluster with ID {}", clusterId);
      }
    } catch (Exception exception) {
      LOG.error("Unable to lookup cluster with ID {}", clusterId, exception);
    }

    if (null == cluster) {
      return invalidatedHosts;
    }

    // intercept host agent alerts; they affect all hosts
    String definitionServiceName = definition.getServiceName();
    String definitionComponentName = definition.getComponentName();
    if (Services.AMBARI.equals(definitionServiceName)
        && Components.AMBARI_AGENT.equals(definitionComponentName)) {

      invalidateAll();
      invalidatedHosts.addAll(hosts.keySet());
      return invalidatedHosts;
    }

    // find all hosts that have the matching service and component
    for (String hostName : hosts.keySet()) {
      List<ServiceComponentHost> hostComponents = cluster.getServiceComponentHosts(hostName);
      if (null == hostComponents || hostComponents.size() == 0) {
        continue;
      }

      // if a host has a matching service/component, invalidate it
      for (ServiceComponentHost component : hostComponents) {
        String serviceName = component.getServiceName();
        String componentName = component.getServiceComponentName();
        if (serviceName.equals(definitionServiceName)
            && componentName.equals(definitionComponentName)) {
          invalidate(clusterName, hostName);
          invalidatedHosts.add(hostName);
        }
      }
    }

    // get the service that this alert definition is associated with
    Map<String, Service> services = cluster.getServices();
    Service service = services.get(definitionServiceName);
    if (null == service) {
      LOG.warn("The alert definition {} has an unknown service of {}",
          definition.getDefinitionName(), definitionServiceName);

      return invalidatedHosts;
    }

    // get all master components of the definition's service; any hosts that
    // run the master should be invalidated as well
    Map<String, ServiceComponent> components = service.getServiceComponents();
    if (null != components) {
      for (Entry<String, ServiceComponent> component : components.entrySet()) {
        if (component.getValue().isMasterComponent()) {
          Map<String, ServiceComponentHost> componentHosts = component.getValue().getServiceComponentHosts();
          if (null != componentHosts) {
            for (String componentHost : componentHosts.keySet()) {
              invalidate(clusterName, componentHost);
              invalidatedHosts.add(componentHost);
            }
          }
        }
      }
    }

    return invalidatedHosts;
  }

  /**
   * Calculates a unique hash value representing all of the alert definitions
   * that should be scheduled to run on a given host. Alerts of type
   * {@link SourceType#AGGREGATE} are not included in the hash since they are
   * not run on the agents.
   *
   * @param clusterName
   *          the cluster name (not {@code null}).
   * @param hostName
   *          the host name (not {@code null}).
   * @return the unique hash or {@value #NULL_MD5_HASH} if none.
   */
  private String hash(String clusterName, String hostName) {
    Set<AlertDefinitionEntity> definitions = getAlertDefinitionEntities(
        clusterName,
        hostName);

    // no definitions found for this host, don't bother hashing
    if( null == definitions || definitions.size() == 0 ) {
      return NULL_MD5_HASH;
    }

    // strip out all AGGREGATE types
    Iterator<AlertDefinitionEntity> iterator = definitions.iterator();
    while (iterator.hasNext()) {
      if (SourceType.AGGREGATE.equals(iterator.next().getSourceType())) {
        iterator.remove();
      }
    }

    // build the UUIDs
    List<String> uuids = new ArrayList<String>(definitions.size());
    for (AlertDefinitionEntity definition : definitions) {
      uuids.add(definition.getHash());
    }

    // sort the UUIDs so that the digest is created with bytes in the same order
    Collections.sort(uuids);

    try {
      MessageDigest digest = MessageDigest.getInstance("MD5");
      for (String uuid : uuids) {
        digest.update(uuid.getBytes());
      }

      byte[] hashBytes = digest.digest();
      return Hex.encodeHexString(hashBytes);
    } catch (NoSuchAlgorithmException nsae) {
      LOG.warn("Unable to calculate MD5 alert definition hash", nsae);
      return NULL_MD5_HASH;
    }
  }
}
