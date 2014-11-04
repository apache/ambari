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

import java.util.List;

/**
 * Models the data representation of an upgrade
 */
public class UpgradeEntity {

  private Long m_id = null;
  private List<UpgradeItemEntity> m_items;

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
   * @return the upgrade items
   */
  public List<UpgradeItemEntity> getUpgradeItems() {
    return m_items;
  }

  /**
   * @param items the upgrade items
   */
  public void setUpgradeItems(List<UpgradeItemEntity> items) {
    m_items = items;
  }

}
