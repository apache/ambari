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

package org.apache.ambari.server.stack;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.ambari.server.state.ServiceComponentHost;

/**
 * Wrapper around a collection of hosts for components.  Some components
 * also have master/secondary designators.
 */
public class HostsType {
  /**
   * List of HA hosts (master - secondaries pairs), if any.
   */
  private final List<HaHosts> haHosts;

  /**
   * Ordered collection of hosts.  This represents all hosts where an upgrade
   * or downgrade must occur.  For an upgrade, all hosts are set.  For a downgrade,
   * this list may be a subset of all hosts where the downgrade MUST be executed.
   * That is to say, a downgrade only occurs where the current version is not
   * the target version.
   */
  private LinkedHashSet<String> hosts;

  /**
   * Unhealthy hosts are those which are explicitly put into maintenance mode.
   * If there is a host which is not heartbeating (or is generally unhealthy)
   * but not in maintenance mode, then the prerequisite upgrade checks will let
   * the administrator know that it must be put into maintenance mode before an
   * upgrade can begin.
   */
  public List<ServiceComponentHost> unhealthy = new ArrayList<>();

  public boolean hasMasters() {
    return !getMasters().isEmpty();
  }

  public List<HaHosts> getHaHosts() {
    return haHosts;
  }

  /**
   * Order the hosts so that for each HA host the secondaries come first.
   * For example: [sec1, sec2, master1, sec3, sec4, master2]
   */
  public void arrangeHostSecondariesFirst() {
    this.hosts = getHaHosts().stream()
      .flatMap(each -> Stream.concat(each.getSecondaries().stream(), Stream.of(each.getMaster())))
      .collect(toCollection(LinkedHashSet::new));
  }

  public boolean hasMastersAndSecondaries() {
    return !getMasters().isEmpty() && !getSecondaries().isEmpty();
  }

  /**
   * A master and secondary host(s). In HA mode there is one master and one secondary host,
   * in federated mode there can be more than one secondaries.
   */
  public static class HaHosts {
    private final String master;
    private final List<String> secondaries;

    public HaHosts(String master, List<String> secondaries) {
      if (master == null) {
        throw new IllegalArgumentException("Master host is missing");
      }
      this.master = master;
      this.secondaries = secondaries;
    }

    public String getMaster() {
      return master;
    }

    public List<String> getSecondaries() {
      return secondaries;
    }
  }

  public static HostsType from(String master, String secondary, LinkedHashSet<String> hosts) {
    return master == null
      ? normal(hosts)
      : new HostsType(singletonList(new HaHosts(master, secondary != null ? singletonList(secondary) : emptyList())), hosts);

  }

  public static HostsType highAvailability(String master, String secondary, LinkedHashSet<String> hosts) {
    return new HostsType(singletonList(new HaHosts(master, singletonList(secondary))), hosts);
  }

  public static HostsType guessHighAvailability(LinkedHashSet<String> hosts) {
    if (hosts.isEmpty()) {
      throw new IllegalArgumentException("Cannot guess HA, empty hosts.");
    }
    String master = hosts.iterator().next();
    List<String> secondaries = hosts.stream().skip(1).collect(toList());
    return new HostsType(singletonList(new HaHosts(master, secondaries)), hosts);
  }

  public static HostsType federated(List<HaHosts> haHosts, LinkedHashSet<String> hosts) {
    return new HostsType(haHosts, hosts);
  }

  public static HostsType normal(LinkedHashSet<String> hosts) {
    return new HostsType(emptyList(), hosts);
  }

  public static HostsType normal(String... hosts) {
    return new HostsType(emptyList(), new LinkedHashSet<>(asList(hosts)));
  }

  public static HostsType single(String host) {
    return HostsType.normal(host);
  }

  private HostsType(List<HaHosts> haHosts, LinkedHashSet<String> hosts) {
    this.haHosts = haHosts;
    this.hosts = hosts;
  }

  public LinkedHashSet<String> getMasters() {
    return haHosts.stream().map(each -> each.getMaster()).collect(toCollection(LinkedHashSet::new));
  }

  public LinkedHashSet<String> getSecondaries() {
    return haHosts.stream().flatMap(each -> each.getSecondaries().stream()).collect(toCollection(LinkedHashSet::new));
  }

  public Set<String> getHosts() {
    return hosts;
  }

  public void setHosts(LinkedHashSet<String> hosts) {
    this.hosts = hosts;
  }
}
