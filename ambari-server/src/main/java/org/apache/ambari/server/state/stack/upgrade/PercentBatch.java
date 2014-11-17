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
package org.apache.ambari.server.state.stack.upgrade;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * Upgrade batch that should happen by percentage.  After each percentage
 * completes, continue to the next step.
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name="percent")
public class PercentBatch extends Batch {

  @XmlElement(name="percent")
  public int percent = 100;

  @Override
  public Type getType() {
    return Batch.Type.PERCENT;
  }

  @Override
  public List<Set<String>> getHostGroupings(Set<String> hosts) {

    List<Set<String>> groupings = new ArrayList<Set<String>>();

    int count = Double.valueOf(Math.ceil(
        (double) percent / 100 * hosts.size())).intValue();

    Set<String> set = new HashSet<String>();
    groupings.add(set);
    int i = 1;
    for (String host : hosts) {
      set.add(host);
      if (i < hosts.size() && 0 == (i++ % count)) {
        set = new HashSet<String>();
        groupings.add(set);
      }
    }

    return groupings;
  }


}
