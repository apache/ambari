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

package org.apache.ambari.server.security.authorization;

/**
 * ResourceType is an enumeration of resources one which authorization tests may be performed.
 * <p/>
 * This data matches the <i>generic</i> values from the <code>adminresourcetype</code> table in the Ambari
 * database.  The enum name represents the <code>adminresourcetype.resource_type_name</code> value
 * and the internal value represents the <code>adminresourcetype.</code> value
 */
public enum ResourceType {
  AMBARI(1),
  CLUSTER(2),
  VIEW(3);

  private final int id;

  /**
   * Constructor
   *
   * @param id the ID value for this ResourceType
   */
  private ResourceType(int id) {
    this.id = id;
  }

  /**
   * Get's ID value for this ResourceType.
   * <p/>
   * This value represents the <code>adminresourcetype.resource_type_id</code> value for the resource type.
   *
   * @return an integer
   */
  public int getId() {
    return id;
  }
}
