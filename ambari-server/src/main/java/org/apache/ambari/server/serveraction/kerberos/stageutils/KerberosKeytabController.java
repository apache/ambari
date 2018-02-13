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

package org.apache.ambari.server.serveraction.kerberos.stageutils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.orm.dao.KerberosKeytabDAO;
import org.apache.ambari.server.orm.dao.KerberosKeytabPrincipalDAO;
import org.apache.ambari.server.orm.entities.KerberosKeytabEntity;
import org.apache.ambari.server.orm.entities.KerberosKeytabPrincipalEntity;
import org.apache.ambari.server.orm.entities.KerberosPrincipalEntity;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * Helper class to construct convenient wrappers around database entities related to kerberos.
 */
@Singleton
public class KerberosKeytabController {
  @Inject
  private KerberosKeytabDAO kerberosKeytabDAO;

  @Inject
  private KerberosKeytabPrincipalDAO kerberosKeytabPrincipalDAO;

  /**
   * Tries to find keytab by keytab path in destination filesystem.
   *
   * @param file keytab path
   * @return found keytab or null
   */
  public ResolvedKerberosKeytab getKeytabByFile(String file) {
    return getKeytabByFile(file, true);
  }

  /**
   * Tries to find keytab by keytab path in destination filesystem.
   *
   * @param file keytab path
   * @param resolvePrincipals include resolved principals
   * @return found keytab or null
   */
  public ResolvedKerberosKeytab getKeytabByFile(String file, boolean resolvePrincipals) {
    return fromKeytabEntity(kerberosKeytabDAO.find(file), resolvePrincipals);
  }

  /**
   * Returns all keytabs managed by ambari.
   *
   * @return all keytabs
   */
  public Set<ResolvedKerberosKeytab> getAllKeytabs() {
    return fromKeytabEntities(kerberosKeytabDAO.findAll());
  }

  /**
   * Returns all keytabs that contains given principal.
   *
   * @param rkp principal to filter keytabs by
   * @return set of keytabs found
   */
  public Set<ResolvedKerberosKeytab> getFromPrincipal(ResolvedKerberosPrincipal rkp) {
    return fromKeytabEntities(kerberosKeytabDAO.findByPrincipalAndHost(rkp.getPrincipal(), rkp.getHostId()));
  }

  /**
   * Returns keytabs with principals filtered by host, principal name or service(and component) names.
   *
   * @param serviceComponentFilter service-component filter
   * @param hostFilter host filter
   * @param identityFilter identity(principal) filter
   * @return set of keytabs found
   */
  public Set<ResolvedKerberosKeytab> getFilteredKeytabs(Map<String, Collection<String>> serviceComponentFilter,
                                                        Set<String> hostFilter, Collection<String> identityFilter) {
    if (serviceComponentFilter == null && hostFilter == null && identityFilter == null) {
      return getAllKeytabs();
    }
    List<KerberosKeytabPrincipalDAO.KerberosKeytabPrincipalFilter> filters = splitServiceFilter(serviceComponentFilter);
    for (KerberosKeytabPrincipalDAO.KerberosKeytabPrincipalFilter filter : filters) {
      filter.setHostNames(hostFilter);
      filter.setPrincipals(identityFilter);
    }

    Set<ResolvedKerberosPrincipal> filteredPrincipals = fromPrincipalEntities(kerberosKeytabPrincipalDAO.findByFilters(filters));
    HashMap<String, ResolvedKerberosKeytab> resultMap = new HashMap<>();
    for (ResolvedKerberosPrincipal principal : filteredPrincipals) {
      if (!resultMap.containsKey(principal.getKeytabPath())) {
        resultMap.put(principal.getKeytabPath(), getKeytabByFile(principal.getKeytabPath(), false));
      }
      ResolvedKerberosKeytab keytab = resultMap.get(principal.getKeytabPath());
      keytab.addPrincipal(principal);
    }
    return Sets.newHashSet(resultMap.values());
  }

  /**
   * This function split serviceComponentFilter to two filters, one with specific components, and another one with service
   * only. Can return only one filter if filter contain only one type of mapping(whole service or component based)
   * or empty filter if no serviceComponentFilter provided.
   *
   * @param serviceComponentFilter
   * @return
   */
  private List<KerberosKeytabPrincipalDAO.KerberosKeytabPrincipalFilter> splitServiceFilter(Map<String, Collection<String>> serviceComponentFilter) {
    if (serviceComponentFilter != null && serviceComponentFilter.size() > 0) {
      Set<String> serviceSet = new HashSet<>();
      Set<String> componentSet = new HashSet<>();
      Set<String> serviceOnlySet = new HashSet<>();
      serviceSet.addAll(serviceComponentFilter.keySet());
      for (String serviceName : serviceSet) {
        Collection<String> serviceComponents = serviceComponentFilter.get(serviceName);
        if (serviceComponents.contains("*")) { // star means that this is filtered by whole SERVICE
          serviceOnlySet.add(serviceName);
          serviceSet.remove(serviceName); // remove service from regular
        } else {
          componentSet.addAll(serviceComponents);
        }
      }
      List<KerberosKeytabPrincipalDAO.KerberosKeytabPrincipalFilter> result = new ArrayList<>();
      if (serviceSet.size() > 0) {
        result.add(new KerberosKeytabPrincipalDAO.KerberosKeytabPrincipalFilter(
          null,
          serviceSet,
          componentSet,
          null
        ));
      }
      if (serviceOnlySet.size() > 0) {
        result.add(new KerberosKeytabPrincipalDAO.KerberosKeytabPrincipalFilter(
          null,
          serviceOnlySet,
          null,
          null
        ));
      }
      if (result.size() > 0) {
        return result;
      }
    }

    return Lists.newArrayList(new KerberosKeytabPrincipalDAO.KerberosKeytabPrincipalFilter(null,null,null,null));
  }

  private ResolvedKerberosKeytab fromKeytabEntity(KerberosKeytabEntity kke, boolean resolvePrincipals) {
    Set<ResolvedKerberosPrincipal> principals = resolvePrincipals ? fromPrincipalEntities(kke.getKerberosKeytabPrincipalEntities()) : new HashSet<>();
    return new ResolvedKerberosKeytab(
      kke.getKeytabPath(),
      kke.getOwnerName(),
      kke.getOwnerAccess(),
      kke.getGroupName(),
      kke.getGroupAccess(),
      principals,
      kke.isAmbariServerKeytab(),
      kke.isWriteAmbariJaasFile()
    );
  }

  private ResolvedKerberosKeytab fromKeytabEntity(KerberosKeytabEntity kke) {
    return fromKeytabEntity(kke, true);
  }

  private Set<ResolvedKerberosKeytab> fromKeytabEntities(Collection<KerberosKeytabEntity> keytabEntities) {
    ImmutableSet.Builder<ResolvedKerberosKeytab> builder = ImmutableSet.builder();
    for (KerberosKeytabEntity kkpe : keytabEntities) {
      builder.add(fromKeytabEntity(kkpe));
    }
    return builder.build();
  }

  private Set<ResolvedKerberosPrincipal> fromPrincipalEntities(Collection<KerberosKeytabPrincipalEntity> principalEntities) {
    ImmutableSet.Builder<ResolvedKerberosPrincipal> builder = ImmutableSet.builder();
    for (KerberosKeytabPrincipalEntity kkpe : principalEntities) {
      KerberosPrincipalEntity kpe = kkpe.getPrincipalEntity();
      ResolvedKerberosPrincipal rkp = new ResolvedKerberosPrincipal(
        kkpe.getHostId(),
        kkpe.getHostName(),
        kkpe.getPrincipalName(),
        kpe.isService(),
        kpe.getCachedKeytabPath(),
        kkpe.getKeytabPath(),
        kkpe.getServiceMappingAsMultimap());
      builder.add(rkp);
    }
    return builder.build();
  }
}
