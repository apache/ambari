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

package org.apache.ambari.server.view;

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.apache.ambari.server.orm.entities.ViewEntity;
import org.apache.ambari.server.orm.entities.ViewInstanceDataEntity;
import org.apache.ambari.server.orm.entities.ViewInstanceEntity;
import org.apache.ambari.server.view.configuration.EntityConfig;
import org.apache.ambari.server.view.configuration.PersistenceConfig;
import org.apache.ambari.server.view.persistence.DataStoreImpl;
import org.apache.ambari.server.view.persistence.DataStoreModule;
import org.apache.ambari.view.DataStore;
import org.apache.ambari.view.migration.EntityConverter;
import org.apache.ambari.view.PersistenceException;
import org.apache.ambari.view.migration.ViewDataMigrationContext;
import org.apache.ambari.view.migration.ViewDataMigrationException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeanUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * View data migration context implementation.
 */
public class ViewDataMigrationContextImpl implements ViewDataMigrationContext {

  /**
   * Logger.
   */
  private static final Log LOG = LogFactory.getLog(ViewDataMigrationContextImpl.class);

  /**
   * The data store of origin(source) view instance with source data.
   */
  private DataStore originDataStore;

  /**
   * The data store of current(target) view instance to store data.
   */
  private DataStore currentDataStore;

  /**
   * The origin view instance definition.
   */
  private final ViewInstanceEntity originInstanceDefinition;

  /**
   * The current view instance definition.
   */
  private final ViewInstanceEntity currentInstanceDefinition;

  /**
   * Constructor.
   *
   * @param originInstanceDefinition    the origin view instance definition
   * @param currentInstanceDefinition   the current view instance definition
   */
  public ViewDataMigrationContextImpl(ViewInstanceEntity originInstanceDefinition,
                                      ViewInstanceEntity currentInstanceDefinition) {
    this.originInstanceDefinition = originInstanceDefinition;
    this.currentInstanceDefinition = currentInstanceDefinition;
  }

  /**
   * Instantiates the data store associated with the instance.
   *
   * @param instanceDefinition the view instance definition
   * @return the data store object associated with view instance
   */
  protected DataStore getDataStore(ViewInstanceEntity instanceDefinition) {
    Injector originInjector = Guice.createInjector(new DataStoreModule(instanceDefinition));
    return originInjector.getInstance(DataStoreImpl.class);
  }

  @Override
  public int getCurrentDataVersion() {
    return currentInstanceDefinition.getViewEntity().getConfiguration().getDataVersion();
  }

  @Override
  public int getOriginDataVersion() {
    return originInstanceDefinition.getViewEntity().getConfiguration().getDataVersion();
  }

  @Override
  public DataStore getOriginDataStore() {
    if (originDataStore == null) {
      originDataStore = getDataStore(originInstanceDefinition);
    }
    return originDataStore;
  }

  @Override
  public DataStore getCurrentDataStore() {
    if (currentDataStore == null) {
      currentDataStore = getDataStore(currentInstanceDefinition);
    }
    return currentDataStore;
  }

  @Override
  public void putCurrentInstanceData(String user, String key, String value) {
    putInstanceData(currentInstanceDefinition, user, key, value);
  }

  @Override
  public void copyAllObjects(Class originEntityClass, Class currentEntityClass)
      throws ViewDataMigrationException {
    copyAllObjects(originEntityClass, currentEntityClass, new EntityConverter() {
      @Override
      public void convert(Object orig, Object dest) {
          BeanUtils.copyProperties(orig, dest);
      }
    });
  }

  @Override
  public void copyAllObjects(Class originEntityClass, Class currentEntityClass, EntityConverter entityConverter)
      throws ViewDataMigrationException {
    try{
      for (Object origInstance : getOriginDataStore().findAll(originEntityClass, null)) {
        Object newInstance = currentEntityClass.newInstance();
        entityConverter.convert(origInstance, newInstance);
        getCurrentDataStore().store(newInstance);
      }
    } catch (PersistenceException | InstantiationException | IllegalAccessException e) {
      String msg = "Error occured during copying data. Persistence entities are not compatible.";
      LOG.error(msg);
      throw new ViewDataMigrationException(msg, e);
    }
  }

  @Override
  public void copyAllInstanceData() {
    for (Map.Entry<String, Map<String, String>> userData : getOriginInstanceDataByUser().entrySet()) {
      for (Map.Entry<String, String> entry : userData.getValue().entrySet()) {
        putCurrentInstanceData(userData.getKey(), entry.getKey(), entry.getValue());
      }
    }
  }

  @Override
  public ViewInstanceEntity getOriginInstanceDefinition() {
    return originInstanceDefinition;
  }

  @Override
  public Map<String, Class> getOriginEntityClasses() {
    ViewEntity viewDefinition = originInstanceDefinition.getViewEntity();
    return getPersistenceClassesOfView(viewDefinition);
  }

  @Override
  public Map<String, Class> getCurrentEntityClasses() {
    ViewEntity viewDefinition = currentInstanceDefinition.getViewEntity();
    return getPersistenceClassesOfView(viewDefinition);
  }

  /**
   * Get persistence entities of the view instance.
   *
   * @param viewDefinition   the view definition.
   * @return the mapping of entity class name to the class objects,
   * loaded by the classloader of view version.
   */
  private static Map<String, Class> getPersistenceClassesOfView(ViewEntity viewDefinition) {
    PersistenceConfig persistence = viewDefinition.getConfiguration().getPersistence();

    HashMap<String, Class> classes = new HashMap<>();
    for (EntityConfig c : persistence.getEntities()) {
      try {
        Class entity = viewDefinition.getClassLoader().loadClass(c.getClassName());
        classes.put(c.getClassName(), entity);
      } catch (ClassNotFoundException e) {
        e.printStackTrace();
      }
    }
    return classes;
  }

  @Override
  public ViewInstanceEntity getCurrentInstanceDefinition() {
    return currentInstanceDefinition;
  }

  @Override
  public Map<String, Map<String, String>> getOriginInstanceDataByUser() {
    return getInstanceDataByUser(originInstanceDefinition);
  }

  @Override
  public void putOriginInstanceData(String user, String key, String value) {
    putInstanceData(originInstanceDefinition, user, key, value);
  }

  @Override
  public Map<String, Map<String, String>> getCurrentInstanceDataByUser() {
    return getInstanceDataByUser(currentInstanceDefinition);
  }

  /**
   * Save an instance data value for the given key owned by given user.
   *
   * @param instanceDefinition  the view instance definition
   * @param user                the owner of the data value
   * @param name                the name
   * @param value               the value
   */
  private static void putInstanceData(ViewInstanceEntity instanceDefinition, String user, String name, String value) {
    ViewInstanceDataEntity viewInstanceDataEntity = new ViewInstanceDataEntity();
    viewInstanceDataEntity.setViewName(instanceDefinition.getViewName());
    viewInstanceDataEntity.setViewInstanceName(instanceDefinition.getName());
    viewInstanceDataEntity.setName(name);
    viewInstanceDataEntity.setUser(user);
    viewInstanceDataEntity.setValue(value);
    viewInstanceDataEntity.setViewInstanceEntity(instanceDefinition);

    instanceDefinition.getData().add(viewInstanceDataEntity);
  }

  /**
   * Get the instance data in the mapping of user owning data to the key-value data.
   *
   * @param instanceDefinition   the view instance definition
   * @return mapping of the data owner to the instance data entries
   */
  private static Map<String, Map<String, String>> getInstanceDataByUser(ViewInstanceEntity instanceDefinition) {
    Map<String, Map<String, String>> instanceDataByUser = new HashMap<>();
    for (ViewInstanceDataEntity entity : instanceDefinition.getData()) {

      if (!instanceDataByUser.containsKey(entity.getUser())) {
        instanceDataByUser.put(entity.getUser(), new HashMap<String, String>());
      }
      instanceDataByUser.get(entity.getUser()).put(entity.getName(), entity.getValue());
    }
    return  instanceDataByUser;
  }
}
