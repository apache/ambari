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

package org.apache.ambari.view.huetoambarimigration.resources.scripts;

import org.apache.ambari.view.ViewContext;
import org.apache.ambari.view.huetoambarimigration.resources.PersonalCRUDResourceManager;
import org.apache.ambari.view.huetoambarimigration.resources.scripts.models.MigrationResponse;
import org.apache.log4j.Logger;

/**
 * Object that provides CRUD operations for script objects
 */
public class MigrationResourceManager extends PersonalCRUDResourceManager<MigrationResponse> {
  final Logger logger = Logger.getLogger(MigrationResourceManager.class);

  /**
   * Constructor
   * @param context View Context instance
   */
  public MigrationResourceManager(ViewContext context) {
    super(MigrationResponse.class, context);
  }

  @Override
  public MigrationResponse create(MigrationResponse object) {

    super.create(object);

    return object;
  }

  private void createDefaultScriptFile(MigrationResponse object) {
    getMigrationStorage().store(object);
  }

}
