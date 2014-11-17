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

import java.util.List;
import java.util.Set;

import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;

/**
 * Base class to identify how a component should be upgraded (optional)
 */
@XmlSeeAlso(value={CountBatch.class, PercentBatch.class, ConditionalBatch.class})
public abstract class Batch {

  /**
   * @return the batch type
   */
  public abstract Type getType();

  /**
   * Identifies the type of batch
   */
  public enum Type {
    /**
     * Batch by <i>n</i> instance at a time
     */
    COUNT,
    /**
     * Batch by <i>x</i>% at a time
     */
    PERCENT,
    /**
     * Batch by an inital <i>x</i>%, then after confirmation batch <i>y</i>% at a time.
     */
    CONDITIONAL
  }

  /**
   * @param hosts all the hosts
   * @return a list of host sets defined by the specific batching
   */
  public abstract List<Set<String>> getHostGroupings(Set<String> hosts);
}
