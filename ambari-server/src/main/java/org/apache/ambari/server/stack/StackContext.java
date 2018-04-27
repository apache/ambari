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

package org.apache.ambari.server.stack;

import org.apache.ambari.server.metadata.ActionMetadata;
import org.apache.ambari.server.orm.dao.MetainfoDAO;
import org.apache.ambari.server.state.stack.OsFamily;

/**
 * Provides external functionality to the Stack framework.
 */
public class StackContext {
  /**
   * Action meta data functionality
   */
  private ActionMetadata actionMetaData;

  /**
   * Constructor.
   *
   * @param metaInfoDAO     metainfo data access object
   * @param actionMetaData  action meta data
   * @param osFamily        OS family information
   */
  public StackContext(MetainfoDAO metaInfoDAO, ActionMetadata actionMetaData, OsFamily osFamily) {
    this.actionMetaData = actionMetaData;
  }

  /**
   * Register a service check.
   *
   * @param serviceName  name of the service
   */
  public void registerServiceCheck(String serviceName) {
    actionMetaData.addServiceCheckAction(serviceName);
  }
}
