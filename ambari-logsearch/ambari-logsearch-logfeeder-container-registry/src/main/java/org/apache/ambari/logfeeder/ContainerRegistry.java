/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.ambari.logfeeder;

import java.util.Map;

/**
 * Responsible of register or drop new / existing containers.
 * @param <METADATA_TYPE> type of metadata - could be docker or other container implementation
 */
public interface ContainerRegistry<METADATA_TYPE extends ContainerMetadata> {

  /**
   * Register process of running containers
   */
  void register();

  /**
   * Holds container metadata per log component type and container id.
   * @return container metadata
   */
  Map<String, Map<String, METADATA_TYPE>> getContainerMetadataMap();

}
