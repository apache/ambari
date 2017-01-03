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

package org.apache.ambari.view.hive20.persistence;

import org.apache.ambari.view.PersistenceException;
import org.apache.ambari.view.ViewContext;
import org.apache.ambari.view.hive20.persistence.utils.FilteringStrategy;
import org.apache.ambari.view.hive20.persistence.utils.Indexed;
import org.apache.ambari.view.hive20.persistence.utils.ItemNotFound;
import org.apache.ambari.view.hive20.persistence.utils.OnlyOwnersFilteringStrategy;
import org.apache.ambari.view.hive20.utils.ServiceFormattedException;
import org.apache.commons.beanutils.BeanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.Transient;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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
      Indexed newBean = (Indexed) BeanUtils.cloneBean(obj);
      preprocessEntity(newBean);
      context.getDataStore().store(newBean);
      obj.setId(newBean.getId());
    } catch (Exception e) {
      throw new ServiceFormattedException("S020 Data storage error", e);
    }
  }

  private void preprocessEntity(Indexed obj) {
    cleanTransientFields(obj);
  }

  private void cleanTransientFields(Indexed obj) {
    for (Method m : obj.getClass().getMethods()) {
      Transient aTransient = m.getAnnotation(Transient.class);
      if (aTransient != null && m.getName().startsWith("set")) {
        try {
          m.invoke(obj, new Object[]{ null });
        } catch (IllegalAccessException e) {
          throw new ServiceFormattedException("S030 Data storage error", e);
        } catch (InvocationTargetException e) {
          throw new ServiceFormattedException("S030 Data storage error", e);
        }
      }
    }
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
      throw new ServiceFormattedException("S040 Data storage error", e);
    }
  }

  @Override
  public synchronized <T extends Indexed> List<T> loadAll(Class<? extends T> model, FilteringStrategy filter) {
    LinkedList<T> list = new LinkedList<T>();
    LOG.debug(String.format("Loading all %s-s", model.getName()));
    try {
      for(T item: context.getDataStore().findAll(model, filter.whereStatement())) {
        list.add(item);
      }
    } catch (PersistenceException e) {
      throw new ServiceFormattedException("S050 Data storage error", e);
    }
    return list;
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
      throw new ServiceFormattedException("S060 Data storage error", e);
    }
  }

  @Override
  public boolean exists(Class model, Object id) {
    try {
      return context.getDataStore().find(model, id) != null;
    } catch (PersistenceException e) {
      throw new ServiceFormattedException("S070 Data storage error", e);
    }
  }
}
