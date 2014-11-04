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
package org.apache.ambari.server.orm.entities;

import org.apache.ambari.server.state.UpgradeState;

/**
 * Models a single upgrade item as part of an entire {@link UpgradeEntity}
 */
public class UpgradeItemEntity {

  private Long m_id = null;
  private UpgradeState m_state = UpgradeState.NONE;

  /**
   * @return the id
   */
  public Long getId() {
    return m_id;
  }

  /**
   * @param id the id
   */
  public void setId(Long id) {
    m_id = id;
  }

  /**
   * @param state the state
   */
  public void setState(UpgradeState state) {
    m_state = state;
  }

  /**
   * @return the state
   */
  public UpgradeState getState() {
    return m_state;
  }



}
