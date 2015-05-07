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
 * distributed under the License is distribut
 * ed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.topology;

import java.util.List;

/**
 * Response to a host offer.
 */
public class HostOfferResponse {
  public enum Answer {ACCEPTED, DECLINED_PREDICATE, DECLINED_DONE}

  private final Answer answer;
  private final String hostGroupName;
  private final long hostRequestId;
  private final List<TopologyTask> tasks;

  public HostOfferResponse(Answer answer) {
    if (answer == Answer.ACCEPTED) {
      throw new IllegalArgumentException("For accepted response, hostgroup name and tasks must be set");
    }
    this.answer = answer;
    this.hostRequestId = -1;
    this.hostGroupName = null;
    this.tasks = null;
  }

  public HostOfferResponse(Answer answer, long hostRequestId, String hostGroupName, List<TopologyTask> tasks) {
    this.answer = answer;
    this.hostRequestId = hostRequestId;
    this.hostGroupName = hostGroupName;
    this.tasks = tasks;
  }

  public Answer getAnswer() {
    return answer;
  }

  public long getHostRequestId() {
    return hostRequestId;
  }

  //todo: for now assumes a host was added
  //todo: perhaps a topology modification object that modifies a passed in topology structure?
  public String getHostGroupName() {
    return hostGroupName;
  }

  public List<TopologyTask> getTasks() {
    return tasks;
  }
}
