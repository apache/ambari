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
  /**
   * predicate compiler
   */
  private static PredicateCompiler predicateCompiler = new PredicateCompiler();

  /**
   * host group name
   */
  private String hostGroupName;
  /**
   * hosts contained associated with the host group
   */
  private final Collection<String> hostNames = new HashSet<String>();

  /**
   * explicitly specified host count
   */
  private int requested_count = 0;

  /**
   * host group scoped configuration
   */
  Configuration configuration;

  /**
   * explicitly specified host predicate string
   */
  String predicateString;

  /**
   * compiled host predicate
   */
  Predicate predicate;


  /**
   * Constructor
   *
   * @param hostGroupName  host group name
   */
  public HostGroupInfo(String hostGroupName) {
    this.hostGroupName = hostGroupName;
  }

  /**
   * Get the host group name.
   *
   * @return host group name
   */
  public String getHostGroupName() {
    return hostGroupName;
  }

  /**
   * Get the collection of user specified host names for the host group.
   * If the user specified a count instead of host names then an empty
   * collection is returned.
   *
   * @return collection of user specified host names; will never be null
   */
  public Collection<String> getHostNames() {
    // needs to be an exclusive lock, not a read lock because collection
    // shouldn't change while copying elements into the new set instance
    synchronized (hostNames) {
      return new HashSet<String>(hostNames);
    }
  }

  /**
   * Get the requested host count.
   * This is either the user specified value or
   * the number of explicitly specified host names specified by the user.
   *
   * @return number of requested hosts for the group
   */
  public int getRequestedHostCount() {
    synchronized (hostNames) {
      return requested_count == 0 ? hostNames.size() : requested_count;
    }
  }

  /**
   * Associate a single host name to the host group.
   *
   * @param hostName  the host name to associate with the host group
   */
  public void addHost(String hostName) {
    synchronized(hostNames) {
      hostNames.add(hostName);
    }
  }

  /**
   * Associate multiple host names to the host group.
   *
   * @param hosts  collection of host names to associate with the host group
   */
  public void addHosts(Collection<String> hosts) {
    synchronized (hostNames) {
      for (String host : hosts) {
        addHost(host);
      }
    }
  }

  /**
   * Set the requested host count for the host group.
   *
   * @param num  requested host count
   */
  public void setRequestedCount(int num) {
    requested_count = num;
  }

  /**
   * Set host group configuration for the host group.
   *
   * @param configuration configuration instance
   */
  public void setConfiguration(Configuration configuration) {
    this.configuration = configuration;
  }

  /**
   * Get the host group configuration associated with the host group.
   *
   * @return associated host group scoped configuration or null if no configuration
   *         is specified for the host group
   */
  public Configuration getConfiguration() {
    return configuration;
  }

  /**
   * Set the host predicate for the host group.
   *
   * @param predicateString  host predicate as a string
   *
   * @throws InvalidQueryException if compilation of the predicate fails
   */
  public void setPredicate(String predicateString) throws InvalidQueryException {
    this.predicate = predicateCompiler.compile(predicateString);
    this.predicateString = predicateString;
  }

  /**
   * Get the compiled host predicate for the host group.
   *
   * @return the compiled host predicate or null if no predicate was specified
   */
  public Predicate getPredicate() {
    return predicate;
  }

  /**
   * Get the host predicate string for the host group.
   *
   * @return the host predicate string or null if no predicate was specified
   */
  public String getPredicateString() {
    return predicateString;
  }
}
