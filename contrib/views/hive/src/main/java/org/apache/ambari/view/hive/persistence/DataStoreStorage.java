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

package org.apache.ambari.view.hive.persistence;

import org.apache.ambari.view.PersistenceException;
import org.apache.ambari.view.ViewContext;
import org.apache.ambari.view.hive.persistence.utils.FilteringStrategy;
import org.apache.ambari.view.hive.persistence.utils.Indexed;
import org.apache.ambari.view.hive.persistence.utils.ItemNotFound;
import org.apache.ambari.view.hive.persistence.utils.OnlyOwnersFilteringStrategy;
import org.apache.ambari.view.hive.utils.ServiceFormattedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.WebApplicationException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Engine for storing objects to context DataStore storage
 */
public class DataStoreStorage implements Storage {
  private final static Logger LOG =
      LoggerFactory.getLogger(DataStoreStorage.class);

  protected ViewContext context;

  /**
   * Constructor
   * @param context View Context instance
   */
  public DataStoreStorage(ViewContext context) {
    this.context = context;
  }

  @Override
  public synchronized void store(Class model, Indexed obj) {
    try {
      if (obj.getId() == null) {
        String id = nextIdForEntity(context, model);
        obj.setId(id);
      }
      context.getDataStore().store(obj);
    } catch (PersistenceException e) {
      throw new ServiceFormattedException("Error while saving object to DataStorage", e);
    }
  }

  private static synchronized String nextIdForEntity(ViewContext context, Class aClass) {
    // auto increment id implementation
    String lastId = context.getInstanceData(aClass.getName());
    int newId;
    if (lastId == null) {
      newId = 1;
    } else {
      newId = Integer.parseInt(lastId) + 1;
    }
    context.putInstanceData(aClass.getName(), String.valueOf(newId));
    return String.valueOf(newId);
  }

  @Override
  public synchronized <T extends Indexed> T load(Class<T> model, Object id) throws ItemNotFound {
    LOG.debug(String.format("Loading %s #%s", model.getName(), id));
    try {
      T obj = context.getDataStore().find(model, id);
      if (obj != null) {
        return obj;
      } else {
        throw new ItemNotFound();
      }
    } catch (PersistenceException e) {
      throw new ServiceFormattedException("Error while finding object in DataStorage", e);
    }
  }

  @Override
  public synchronized <T extends Indexed> List<T> loadAll(Class<? extends T> model, FilteringStrategy filter) {
    LinkedList<T> list = new LinkedList<T>();
    LOG.debug(String.format("Loading all %s-s", model.getName()));
    try {
      //TODO: use WHERE statement instead of this ugly filter
      for(T item: context.getDataStore().findAll(model, filter.whereStatement())) {
        list.add(item);
      }
    } catch (PersistenceException e) {
      throw new ServiceFormattedException("Error while finding all objects in DataStorage", e);
    }
    return list;
  }

  @Override
  public <T extends Indexed> List<T> loadWhere(Class<T> model, String where) {
    LOG.debug(String.format("Loading all %s-s", model.getName()));
    try {
      return new ArrayList<T>(context.getDataStore().findAll(model, where));
    } catch (PersistenceException e) {
      throw new ServiceFormattedException("Error while finding objects in DataStorage; where = " + where, e);
    }
  }

  @Override
  public synchronized <T extends Indexed> List<T> loadAll(Class<T> model) {
    return loadAll(model, new OnlyOwnersFilteringStrategy(this.context.getUsername()));
  }

  @Override
  public synchronized void delete(Class model, Object id) throws ItemNotFound {
    LOG.debug(String.format("Deleting %s:%s", model.getName(), id));
    Object obj = load(model, id);
    try {
      context.getDataStore().remove(obj);
    } catch (PersistenceException e) {
      throw new ServiceFormattedException("Error while removing object from DataStorage", e);
    }
  }

  @Override
  public boolean exists(Class model, Object id) {
    try {
      return context.getDataStore().find(model, id) != null;
    } catch (PersistenceException e) {
      throw new ServiceFormattedException("Error while finding object in DataStorage", e);
    }
  }

  public static void storageSmokeTest(ViewContext context) {
    try {
      SmokeTestEntity entity = new SmokeTestEntity();
      entity.setData("42");
      DataStoreStorage storage = new DataStoreStorage(context);
      storage.store(SmokeTestEntity.class, entity);

      if (entity.getId() == null) throw new ServiceFormattedException("Ambari Views instance data DB doesn't work properly (auto increment id doesn't work)", null);
      Object id = entity.getId();
      SmokeTestEntity entity2 = storage.load(SmokeTestEntity.class, id);
      boolean status = entity2.getData().compareTo("42") == 0;
      storage.delete(SmokeTestEntity.class, id);
      if (!status) throw new ServiceFormattedException("Ambari Views instance data DB doesn't work properly", null);
    } catch (WebApplicationException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new ServiceFormattedException(ex.getMessage(), ex);
    }
  }

  public static class SmokeTestEntity implements Indexed {
    private String id = null;
    private String data = null;

    public String getId() {
      return id;
    }

    public void setId(String id) {
      this.id = id;
    }

    public String getData() {
      return data;
    }

    public void setData(String data) {
      this.data = data;
    }
  }
}
