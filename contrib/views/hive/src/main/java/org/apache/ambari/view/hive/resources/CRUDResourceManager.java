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

package org.apache.ambari.view.hive.resources;

import org.apache.ambari.view.ViewContext;
import org.apache.ambari.view.hive.persistence.Storage;
import org.apache.ambari.view.hive.persistence.utils.FilteringStrategy;
import org.apache.ambari.view.hive.persistence.utils.Indexed;
import org.apache.ambari.view.hive.persistence.utils.ItemNotFound;
import org.apache.ambari.view.hive.persistence.utils.StorageUtil;
import org.apache.ambari.view.hive.utils.ServiceFormattedException;

import java.util.List;

/**
 * CRUD resource manager
 * @param <T> Data type with ID
 */
abstract public class CRUDResourceManager<T extends Indexed> {
  //TODO: refactor: generic parameter gets Fabric for Indexed objects, not objects itself
  private Storage storage = null;

  protected final Class<? extends T> resourceClass;

  /**
   * Constructor
   * @param resourceClass model class
   */
  public CRUDResourceManager(Class<? extends T> resourceClass) {
    this.resourceClass = resourceClass;
  }
  // CRUD operations

  /**
   * Create operation
   * @param object object
   * @return model object
   */
  protected T create(T object) {
    object.setId(null);
    return this.save(object);
  }

  /**
   * Read operation
   * @param id identifier
   * @return model object
   * @throws org.apache.ambari.view.hive.persistence.utils.ItemNotFound
   */
  protected T read(Integer id) throws ItemNotFound {
    T object = null;
    object = getStorage().load(this.resourceClass, id);
    if (!checkPermissions(object))
      throw new ItemNotFound();
    return object;
  }

  /**
   * Read all objects
   * @param filteringStrategy filtering strategy
   * @return list of filtered objects
   */
  protected List<T> readAll(FilteringStrategy filteringStrategy) {
    return getStorage().loadAll(this.resourceClass, filteringStrategy);
  }

  /**
   * Update operation
   * @param newObject new object
   * @param id identifier of previous object
   * @return model object
   * @throws org.apache.ambari.view.hive.persistence.utils.ItemNotFound
   */
  protected T update(T newObject, Integer id) throws ItemNotFound {
    newObject.setId(id);
    this.save(newObject);
    return newObject;
  }

  /**
   * Delete operation
   * @param resourceId object identifier
   * @throws org.apache.ambari.view.hive.persistence.utils.ItemNotFound
   */
  protected void delete(Integer resourceId) throws ItemNotFound {
    if (!getStorage().exists(this.resourceClass, resourceId)) {
      throw new ItemNotFound();
    }
    getStorage().delete(this.resourceClass, resourceId);
  }

  // UTILS

  protected T save(T object) {
    getStorage().store(resourceClass, object);
    return object;
  }

  protected Storage getStorage() {
    if (storage == null) {
      storage = StorageUtil.getInstance(getContext()).getStorage();
    }
    return storage;
  }

  protected abstract boolean checkPermissions(T object);
  protected abstract ViewContext getContext();

  protected void cleanupAfterErrorAndThrowAgain(Indexed object, ServiceFormattedException e) {
    try {
      delete(object.getId());
    } catch (ItemNotFound itemNotFound) {
      throw new ServiceFormattedException("Error in creation, during clean up: " + itemNotFound.toString(), itemNotFound);
    }
    throw e;
  }
}
