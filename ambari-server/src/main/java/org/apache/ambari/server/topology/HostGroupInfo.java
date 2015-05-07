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

package org.apache.ambari.server.topology;

import org.apache.ambari.server.api.predicate.InvalidQueryException;
import org.apache.ambari.server.api.predicate.PredicateCompiler;
import org.apache.ambari.server.controller.spi.Predicate;

import java.util.Collection;
import java.util.HashSet;

/**
 * Host Group information specific to a cluster instance.
 */
public class HostGroupInfo {

  private static PredicateCompiler predicateCompiler = new PredicateCompiler();

  private String hostGroupName;
  /**
   * Hosts contained associated with the host group
   */
  private Collection<String> hostNames = new HashSet<String>();

  private int requested_count = 0;

  Configuration configuration;

  String predicateString;
  Predicate predicate;


  public HostGroupInfo(String hostGroupName) {
    this.hostGroupName = hostGroupName;
  }

  public String getHostGroupName() {
    return hostGroupName;
  }

  public Collection<String> getHostNames() {
    return new HashSet<String>(hostNames);
  }

  public int getRequestedHostCount() {
    return requested_count == 0 ? hostNames.size() : requested_count;
  }

  public void addHost(String hostName) {
    hostNames.add(hostName);
  }

  public void addHosts(Collection<String> hosts) {
    for (String host : hosts) {
      addHost(host);
    }
  }

  public void setRequestedCount(int num) {
    requested_count = num;
  }

  //todo: constructor?
  public void setConfiguration(Configuration configuration) {
    this.configuration = configuration;
  }

  public Configuration getConfiguration() {
    return configuration;
  }

  public void setPredicate(String predicateString) throws InvalidQueryException {
    this.predicate = predicateCompiler.compile(predicateString);
    this.predicateString = predicateString;
  }

  public Predicate getPredicate() {
    return predicate;
  }

  public String getPredicateString() {
    return predicateString;
  }
}
