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
import org.apache.ambari.view.hive20.persistence.utils.ItemNotFound;
import org.apache.ambari.view.hive20.persistence.utils.PersonalResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Callable;

/**
 * Resource manager that returns only user owned elements from DB
 * @param <T> Data type with ID and Owner
 */
public class PersonalCRUDResourceManager<T extends PersonalResource> extends CRUDResourceManager<T> {
  protected boolean ignorePermissions = false;

  private final static Logger LOG =
      LoggerFactory.getLogger(PersonalCRUDResourceManager.class);
  protected ViewContext context;

  /**
   * Constructor
   * @param resourceClass model class
   */
  public PersonalCRUDResourceManager(Class<? extends T> resourceClass, IStorageFactory storageFabric, ViewContext context) {
    super(resourceClass, storageFabric);
    this.context = context;
  }

  @Override
  public T update(T newObject, String id) throws ItemNotFound {
    T object = storageFactory.getStorage().load(this.resourceClass, id);
    if (object.getOwner().compareTo(this.context.getUsername()) != 0) {
      throw new ItemNotFound();
    }

    newObject.setOwner(this.context.getUsername());
    return super.update(newObject, id);
  }

  @Override
  public T save(T object) {
    if (!ignorePermissions) {
      // in threads permissions should be ignored,
      // because context.getUsername doesn't work. See BUG-27093.
      object.setOwner(this.context.getUsername());
    }
    return super.save(object);
  }

  @Override
  protected boolean checkPermissions(T object) {
    if (ignorePermissions) {
      return true;
    }
    return object.getOwner().compareTo(this.context.getUsername()) == 0;
  }

  /**
   * Execute action ignoring objects owner
   * @param actions callable to execute
   * @return value returned from actions
   * @throws Exception
   */
  public T ignorePermissions(Callable<T> actions) throws Exception {
    ignorePermissions = true;
    T result;
    try {
      result = actions.call();
    } finally {
      ignorePermissions = false;
    }
    return result;
  }

  protected String getUsername() {
    return context.getUsername();
  }
}
