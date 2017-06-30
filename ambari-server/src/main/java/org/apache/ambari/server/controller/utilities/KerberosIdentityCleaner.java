/*
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
package org.apache.ambari.server.controller.utilities;

import static org.apache.ambari.server.state.kerberos.AbstractKerberosDescriptor.nullToEmpty;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.controller.KerberosHelper;
import org.apache.ambari.server.events.ServiceComponentUninstalledEvent;
import org.apache.ambari.server.events.publishers.AmbariEventPublisher;
import org.apache.ambari.server.serveraction.kerberos.Component;
import org.apache.ambari.server.serveraction.kerberos.KerberosMissingAdminCredentialsException;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.SecurityType;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.kerberos.KerberosComponentDescriptor;
import org.apache.ambari.server.state.kerberos.KerberosDescriptor;
import org.apache.ambari.server.state.kerberos.KerberosIdentityDescriptor;
import org.apache.ambari.server.state.kerberos.KerberosServiceDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class KerberosIdentityCleaner {
  private final static Logger LOG = LoggerFactory.getLogger(KerberosIdentityCleaner.class);
  private final AmbariEventPublisher eventPublisher;
  private final KerberosHelper kerberosHelper;
  private final Clusters clusters;

  @Inject
  public KerberosIdentityCleaner(AmbariEventPublisher eventPublisher, KerberosHelper kerberosHelper, Clusters clusters) {
    this.eventPublisher = eventPublisher;
    this.kerberosHelper = kerberosHelper;
    this.clusters = clusters;
  }

  public void register() {
    this.eventPublisher.register(this);
  }

  /**
   * Removes kerberos identities (principals and keytabs) after a component was uninstalled.
   * Keeps the identity if either the principal or the keytab is used by an other service
   */
  @Subscribe
  public void componentRemoved(ServiceComponentUninstalledEvent event) throws KerberosMissingAdminCredentialsException {
    try {
      Cluster cluster = clusters.getCluster(event.getClusterId());
      if (cluster.getSecurityType() != SecurityType.KERBEROS) {
        return;
      }
      KerberosComponentDescriptor descriptor = componentDescriptor(cluster, event.getServiceName(), event.getComponentName());
      if (descriptor == null) {
        LOG.info("No kerberos descriptor for {}", event);
        return;
      }
      List<String> identitiesToRemove = identityNames(skipSharedIdentities(descriptor.getIdentitiesSkipReferences(), cluster, event));
      LOG.info("Deleting identities {} after an event {}",  identitiesToRemove, event);
      kerberosHelper.deleteIdentity(cluster, new Component(event.getHostName(), event.getServiceName(), event.getComponentName()), identitiesToRemove);
    } catch (Exception e) {
      LOG.error("Error while deleting kerberos identity after an event: " + event, e);
    }
  }

  private KerberosComponentDescriptor componentDescriptor(Cluster cluster, String serviceName, String componentName) throws AmbariException {
    KerberosServiceDescriptor serviceDescriptor = kerberosHelper.getKerberosDescriptor(cluster).getService(serviceName);
    return serviceDescriptor == null ? null : serviceDescriptor.getComponent(componentName);
  }

  private List<String> identityNames(List<KerberosIdentityDescriptor> identities) {
    List<String> result = new ArrayList<>();
    for (KerberosIdentityDescriptor each : identities) { result.add(each.getName()); }
    return result;
  }

  private List<KerberosIdentityDescriptor> skipSharedIdentities(List<KerberosIdentityDescriptor> candidates, Cluster cluster, ServiceComponentUninstalledEvent event) throws AmbariException {
    List<KerberosIdentityDescriptor> activeIdentities = activeIdentities(cluster, kerberosHelper.getKerberosDescriptor(cluster), event);
    List<KerberosIdentityDescriptor> result = new ArrayList<>();
    for (KerberosIdentityDescriptor candidate : candidates) {
      if (!candidate.isShared(activeIdentities)) {
        result.add(candidate);
      } else {
        LOG.debug("Skip removing shared identity: {}", candidate.getName());
      }
    }
    return result;
  }

  private List<KerberosIdentityDescriptor> activeIdentities(Cluster cluster, KerberosDescriptor root, ServiceComponentUninstalledEvent event) {
    List<KerberosIdentityDescriptor> result = new ArrayList<>();
    result.addAll(nullToEmpty(root.getIdentities()));
    for (Map.Entry<String, Service> serviceEntry : cluster.getServices().entrySet()) {
      KerberosServiceDescriptor serviceDescriptor = root.getService(serviceEntry.getKey());
      if (serviceDescriptor == null) {
        continue;
      }
      result.addAll(nullToEmpty(serviceDescriptor.getIdentities()));
      for (String componentName : serviceEntry.getValue().getServiceComponents().keySet()) {
        if (!sameComponent(event, componentName, serviceEntry.getKey())) {
          result.addAll(serviceDescriptor.getComponentIdentities(componentName));
        }
      }
    }
    return result;
  }

  private boolean sameComponent(ServiceComponentUninstalledEvent event, String componentName, String serviceName) {
    return event.getServiceName().equals(serviceName) && event.getComponentName().equals(componentName);
  }
}

