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

package org.apache.ambari.view.hive20.resources;

import org.apache.ambari.view.ViewContext;
import org.apache.ambari.view.hive20.persistence.IStorageFactory;
import org.apache.ambari.view.hive20.persistence.utils.Indexed;

/**
 * Resource manager that doesn't restrict access (Allow all)
 * @param <T> Data type with ID
 */
public class SharedCRUDResourceManager<T extends Indexed> extends CRUDResourceManager<T> {
  protected ViewContext context;

  /**
   * Constructor
   * @param responseClass model class
   */
  public SharedCRUDResourceManager(Class<T> responseClass, IStorageFactory storageFabric) {
    super(responseClass, storageFabric);
  }

  @Override
  protected boolean checkPermissions(T object) {
    return true; //everyone has permission
  }
}
