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

package org.apache.ambari.view.pig.resources;

import org.apache.ambari.view.ViewContext;
import org.apache.ambari.view.pig.persistence.Storage;
import org.apache.ambari.view.pig.persistence.utils.FilteringStrategy;
import org.apache.ambari.view.pig.persistence.utils.Indexed;
import org.apache.ambari.view.pig.persistence.utils.ItemNotFound;
import org.apache.ambari.view.pig.persistence.utils.StorageUtil;

import java.util.List;

/**
 * CRUD resource manager
 * @param <T> Data type with ID
 */
abstract public class CRUDResourceManager<T extends Indexed> {
    private Storage storage = null;

    protected final Class<T> resourceClass;

    public CRUDResourceManager(Class<T> responseClass) {
        this.resourceClass = responseClass;
    }
    // CRUD operations

    public T create(T object) {
        object.setId(null);
        return this.save(object);
    }

    public T read(String id) throws ItemNotFound {
        T object = null;
        object = getPigStorage().load(this.resourceClass, Integer.parseInt(id));
        if (!checkPermissions(object))
            throw new ItemNotFound();
        return object;
    }

    public List<T> readAll(FilteringStrategy filteringStrategy) {
        return getPigStorage().loadAll(this.resourceClass, filteringStrategy);
    }

    public T update(T newObject, String id) throws ItemNotFound {
        newObject.setId(id);
        this.save(newObject);
        return newObject;
    }

    public void delete(String resourceId) throws ItemNotFound {
        int id = Integer.parseInt(resourceId);
        if (!getPigStorage().exists(this.resourceClass, id)) {
            throw new ItemNotFound();
        }
        getPigStorage().delete(this.resourceClass, id);
    }

    // UTILS

    protected T save(T object) {
        getPigStorage().store(object);
        return object;
    }

    protected Storage getPigStorage() {
        if (storage == null) {
            storage = StorageUtil.getStorage(getContext());
        }
        return storage;
    }

    protected abstract boolean checkPermissions(T object);
    protected abstract ViewContext getContext();
}
