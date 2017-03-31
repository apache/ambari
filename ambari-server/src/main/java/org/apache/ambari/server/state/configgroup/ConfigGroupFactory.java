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
package org.apache.ambari.server.state.configgroup;

import java.util.Map;

import org.apache.ambari.server.orm.entities.ConfigGroupEntity;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.Host;

import com.google.inject.assistedinject.Assisted;

public interface ConfigGroupFactory {
  /**
   * Creates and saves a new {@link ConfigGroup}.
   *
   * @param cluster
   * @param name
   * @param tag
   * @param description
   * @param configs
   * @param hosts
   * @param serviceName
   * @return
   */
  ConfigGroup createNew(@Assisted("cluster") Cluster cluster, @Assisted("name") String name,
      @Assisted("tag") String tag, @Assisted("description") String description,
      @Assisted("configs") Map<String, Config> configs, @Assisted("hosts") Map<Long, Host> hosts);

  /**
   * Instantiates a {@link ConfigGroup} fron an existing, persisted entity.
   *
   * @param cluster
   * @param entity
   * @return
   */
  ConfigGroup createExisting(Cluster cluster, ConfigGroupEntity entity);
}
