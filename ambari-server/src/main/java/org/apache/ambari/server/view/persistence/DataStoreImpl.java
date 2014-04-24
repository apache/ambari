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

package org.apache.ambari.server.view.persistence;

import org.apache.ambari.server.orm.entities.ViewEntityEntity;
import org.apache.ambari.server.orm.entities.ViewInstanceEntity;
import org.apache.ambari.view.DataStore;
import org.apache.ambari.view.PersistenceException;
import org.eclipse.persistence.dynamic.DynamicClassLoader;
import org.eclipse.persistence.dynamic.DynamicEntity;
import org.eclipse.persistence.dynamic.DynamicType;
import org.eclipse.persistence.jpa.dynamic.JPADynamicHelper;
import org.eclipse.persistence.jpa.dynamic.JPADynamicTypeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Query;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;


/**
 * A data store implementation that uses dynamic JPA entities to
 * persist view entities to the Ambari database.
 */
public class DataStoreImpl implements DataStore {

  /**
   * JPA entity manager
   */
  @Inject
  EntityManagerFactory entityManagerFactory;

  /**
   * The dynamic class loader.
   */
  @Inject
  DynamicClassLoader classLoader;

  /**
   * The dynamic helper.
   */
  @Inject
  JPADynamicHelper jpaDynamicHelper;

  /**
   * A factory to get a schema manager.
   */
  @Inject
  SchemaManagerFactory schemaManagerFactory;

  /**
   * The view instance.
   */
  @Inject
  ViewInstanceEntity viewInstanceEntity;

  /**
   * Map of dynamic entity names keyed by view entity class.
   */
  private final Map<Class, String> entityClassMap = new HashMap<Class, String>();

  /**
   * Map of entity primary key fields keyed by dynamic entity name.
   */
  private final Map<String, ViewEntityEntity> entityMap = new HashMap<String, ViewEntityEntity>();

  /**
   * Map of dynamic entity type builders keyed by dynamic entity name.
   */
  private final Map<String, JPADynamicTypeBuilder> typeBuilderMap = new HashMap<String, JPADynamicTypeBuilder>();

  /**
   * Indicates whether or not the data store has been initialized.
   */
  private volatile boolean initialized = false;

  /**
   * The logger.
   */
  protected final static Logger LOG = LoggerFactory.getLogger(DataStoreImpl.class);


  // ----- DataStore ---------------------------------------------------------

  @Override
  public void store(Object entity) throws PersistenceException {
    checkInitialize();

    EntityManager em = getEntityManager();
    try {
      em.getTransaction().begin();
      try {
        persistEntity(entity, em, new HashSet<DynamicEntity>());
        em.getTransaction().commit();
      } catch (Exception e) {
        if (em.getTransaction()!= null) {
          em.getTransaction().rollback();
        }
        throwPersistenceException("Caught exception trying to store view entity " + entity, e);
      }
    } finally {
      em.close();
    }
  }

  @Override
  public void remove(Object entity) throws PersistenceException {
    checkInitialize();

    EntityManager em = getEntityManager();
    try {
      Class       clazz = entity.getClass();
      String      id    = getIdFieldName(clazz);
      DynamicType type  = getDynamicEntityType(clazz);

      if (type != null) {
        try {
          Map<String, Object> properties    = getEntityProperties(entity);
          DynamicEntity       dynamicEntity = em.getReference(type.getJavaClass(), properties.get(id));

          if (dynamicEntity != null) {
            em.getTransaction().begin();
            try {
              em.remove(dynamicEntity);
              em.getTransaction().commit();
            } catch (Exception e) {
              if (em.getTransaction()!= null) {
                em.getTransaction().rollback();
              }
              throwPersistenceException("Caught exception trying to remove view entity " + entity, e);
            }
          }

        } catch (Exception e) {
          throwPersistenceException("Caught exception trying to remove view entity " + entity, e);
        }
      }
    } finally {
      em.close();
    }
  }

  @Override
  public <T> T find(Class<T> clazz, Object primaryKey) throws PersistenceException {
    checkInitialize();

    EntityManager em = getEntityManager();
    try {
      DynamicEntity dynamicEntity = null;
      DynamicType   type          = getDynamicEntityType(clazz);

      if (type != null) {
        dynamicEntity = em.find(type.getJavaClass(), primaryKey);
      }
      return dynamicEntity == null ? null : toEntity(clazz, type, dynamicEntity);
    } catch (Exception e) {
      throwPersistenceException("Caught exception trying to find " +
          clazz.getName() + " where key=" + primaryKey, e);
    } finally {
      em.close();
    }
    return null;
  }

  @Override
  public <T> Collection<T> findAll(Class<T> clazz, String whereClause) throws PersistenceException {
    checkInitialize();

    EntityManager em = getEntityManager();
    try {
      Collection<T> resources = new HashSet<T>();
      DynamicType   type      = getDynamicEntityType(clazz);

      if (type != null) {
        try {
          Query query = em.createQuery(getSelectStatement(clazz, whereClause));

          List dynamicEntities = query.getResultList();

          for (Object dynamicEntity : dynamicEntities) {
            resources.add(toEntity(clazz, type, (DynamicEntity) dynamicEntity));
          }
        } catch (Exception e) {
          throwPersistenceException("Caught exception trying to find " +
              clazz.getName() + " where " + whereClause, e);
        }
      }
      return resources;
    } finally {
      em.close();
    }
  }


  // ----- helper methods ----------------------------------------------------

  // lazy initialize the data store
  private void checkInitialize() throws PersistenceException {
    if (!initialized) {
      synchronized (this) {
        if (!initialized) {
          initialized = true;
          try {
            for (ViewEntityEntity viewEntityEntity : viewInstanceEntity.getEntities()){

              String className = viewEntityEntity.getClassName();
              Class  clazz     = classLoader.loadClass(className);
              String name      = getEntityName(viewEntityEntity);

              entityMap.put(name, viewEntityEntity);
              entityClassMap.put(clazz, name);
            }

            configureTypes(jpaDynamicHelper, classLoader);

          } catch (Exception e) {
            throwPersistenceException("Can't initialize data store for view " +
                viewInstanceEntity.getViewName() + "." + viewInstanceEntity.getName(), e);
          }
        }
      }
    }
  }

  // configure the dynamic types for the entities defined for the associated view
  private void configureTypes(JPADynamicHelper helper, DynamicClassLoader dcl)
      throws IntrospectionException, PersistenceException, NoSuchFieldException {

    // create a dynamic type builder for each declared view entity type
    for (Map.Entry<Class, String> entry: entityClassMap.entrySet()) {
      String entityName = entry.getValue();
      Class<?> javaType = dcl.createDynamicClass(entityName);
      String tableName  = getTableName(entityMap.get(entityName));

      JPADynamicTypeBuilder typeBuilder = new JPADynamicTypeBuilder(javaType, null, tableName);
      typeBuilderMap.put(entityName, typeBuilder);
    }

    // add the direct mapped properties to the dynamic type builders
    for (Map.Entry<Class, String> entry: entityClassMap.entrySet()) {

      Class                 clazz       = entry.getKey();
      String                entityName  = entry.getValue();
      JPADynamicTypeBuilder typeBuilder = typeBuilderMap.get(entityName);

      Map<String, PropertyDescriptor> descriptorMap = getDescriptorMap(clazz);

      for (Map.Entry<String, PropertyDescriptor> descriptorEntry : descriptorMap.entrySet()) {
        String             propertyName = descriptorEntry.getKey();
        PropertyDescriptor descriptor   = descriptorEntry.getValue();

        if (propertyName.equals(entityMap.get(entityName).getIdProperty())) {
          typeBuilder.setPrimaryKeyFields(propertyName);
        }

        Class<?> propertyType = descriptor.getPropertyType();

        if (isDirectMappingType(propertyType)) {
          typeBuilder.addDirectMapping(propertyName, propertyType, propertyName);
        }
      }
    }

    // add the relationships to the dynamic type builders
    for (Map.Entry<Class, String> entry: entityClassMap.entrySet()) {

      Class                 clazz       = entry.getKey();
      String                entityName  = entry.getValue();
      JPADynamicTypeBuilder typeBuilder = typeBuilderMap.get(entityName);

      Map<String, PropertyDescriptor> descriptorMap = getDescriptorMap(clazz);

      for (Map.Entry<String, PropertyDescriptor> descriptorEntry : descriptorMap.entrySet()) {
        String propertyName = descriptorEntry.getKey();
        PropertyDescriptor descriptor = descriptorEntry.getValue();
        if (propertyName.equals(entityMap.get(entityName).getIdProperty())) {
          typeBuilder.setPrimaryKeyFields(propertyName);
        }

        Class<?> propertyType = descriptor.getPropertyType();
        String refEntityName = entityClassMap.get(propertyType);

        if (refEntityName == null) {
          if (Collection.class.isAssignableFrom(propertyType)) {

            String tableName = getTableName(entityMap.get(entityName)) + "_" + propertyName;

            Class<?> parameterizedTypeClass = getParameterizedTypeClass(clazz, propertyName);

            refEntityName = entityClassMap.get(parameterizedTypeClass);

            if (refEntityName == null) {
              typeBuilder.addDirectCollectionMapping(propertyName, tableName, propertyName,
                  parameterizedTypeClass, entityMap.get(entityName).getIdProperty());
            } else {
              DynamicType refType = typeBuilderMap.get(refEntityName).getType();
              typeBuilder.addManyToManyMapping(propertyName, refType, tableName);
            }
          }
        } else {
          DynamicType refType = typeBuilderMap.get(refEntityName).getType();
          typeBuilder.addOneToOneMapping(propertyName, refType, propertyName);
        }
      }
    }

    DynamicType[] types = new DynamicType[ typeBuilderMap.size()];
    int i = typeBuilderMap.size() - 1;
    for (JPADynamicTypeBuilder typeBuilder : typeBuilderMap.values()) {
      types[i--] = typeBuilder.getType();
    }
    helper.addTypes(true, true, types);

    // extend the tables if needed (i.e. attribute added to the view entity)
    schemaManagerFactory.getSchemaManager(helper.getSession()).extendDefaultTables(true);
  }

  // persist the given view entity to the entity manager and
  // return the corresponding dynamic entity
  private DynamicEntity persistEntity(Object entity, EntityManager em, Set<DynamicEntity> persistSet)
      throws PersistenceException, IntrospectionException, InvocationTargetException,
      IllegalAccessException, NoSuchFieldException {
    DynamicEntity dynamicEntity  = null;
    Class         clazz          = entity.getClass();
    String        id             = getIdFieldName(clazz);

    Map<String, Object> properties = getEntityProperties(entity);

    DynamicType type = getDynamicEntityType(clazz);

    if (type != null) {
      dynamicEntity  = em.find(type.getJavaClass(), properties.get(id));

      boolean create = dynamicEntity == null;

      if (create) {
        dynamicEntity = type.newDynamicEntity();
      }

      // has this entity already been accounted for?
      if (persistSet.contains(dynamicEntity)) {
        return dynamicEntity;
      }

      persistSet.add(dynamicEntity);

      for (String propertyName : type.getPropertiesNames()) {
        if (properties.containsKey(propertyName)) {
          Object value = properties.get(propertyName);
          if (value != null) {
            Class<?> valueClass = value.getClass();

            if (Collection.class.isAssignableFrom(valueClass)) {

              Class<?>           typeClass  = getParameterizedTypeClass(clazz, propertyName);
              Collection<Object> collection = dynamicEntity.get(propertyName);

              collection.clear();

              for (Object collectionValue : (Collection) value) {

                if (getDynamicEntityType(typeClass)!= null ) {
                  collectionValue = persistEntity(collectionValue, em, persistSet);
                }
                if (collectionValue != null) {
                  collection.add(collectionValue);
                }
              }
            } else {
              if (getDynamicEntityType(valueClass)!= null ) {
                value = persistEntity(value, em, persistSet);
              }
              if (value != null) {
                dynamicEntity.set(propertyName, value);
              }
            }
          }
        }
      }

      if (create) {
        em.persist(dynamicEntity);
      }
    }
    return dynamicEntity;
  }

  // convert the given dynamic entity to a view entity
  private <T> T toEntity(Class<T> clazz, DynamicType type, DynamicEntity entity)
      throws IntrospectionException, InvocationTargetException,
      IllegalAccessException, InstantiationException, NoSuchFieldException {
    T resource = clazz.newInstance();

    Map<String, Object> properties = new HashMap<String, Object>();

    for (String propertyName : type.getPropertiesNames()) {
      properties.put(propertyName, entity.get(propertyName));
    }
    setEntityProperties(resource, properties);

    return resource;
  }

  // build a JPA select statement from the given view entity class and where clause
  private <T> String getSelectStatement(Class<T> clazz, String whereClause)
      throws IntrospectionException {
    StringBuilder stringBuilder = new StringBuilder();
    String        entityName    = entityClassMap.get(clazz);

    stringBuilder.append("SELECT e FROM ").append(entityName).append(" e");
    if (whereClause != null) {
      stringBuilder.append(" WHERE");

      Set<String>     propertyNames = getPropertyNames(clazz);
      StringTokenizer tokenizer     = new StringTokenizer(whereClause, " \t\n\r\f+-*/=><()\"", true);
      boolean         quoted        = false;

      while (tokenizer.hasMoreElements()) {
        String token = tokenizer.nextToken();

        quoted = quoted ^ token.equals("\"");

        if (propertyNames.contains(token) && !quoted) {
          stringBuilder.append(" e.").append(token);
        } else {
          stringBuilder.append(token);
        }
      }
    }
    return stringBuilder.toString();
  }

  // get a map of properties from the given view entity
  private Map<String, Object> getEntityProperties(Object entity)
      throws IntrospectionException, InvocationTargetException, IllegalAccessException {
    Map<String, Object> properties = new HashMap<String, Object>();

    for (PropertyDescriptor pd : Introspector.getBeanInfo(entity.getClass()).getPropertyDescriptors()) {
      String name       = pd.getName();
      Method readMethod = pd.getReadMethod();
      if (readMethod != null) {
        properties.put(name, readMethod.invoke(entity));
      }
    }
    return properties;
  }

  // set the properties on the given view entity from the given map of properties; convert all
  // DynamicEntity values to their associated view entity types
  private void setEntityProperties(Object entity, Map<String, Object> properties)
      throws IntrospectionException, InvocationTargetException, IllegalAccessException,
      InstantiationException, NoSuchFieldException {
    for (PropertyDescriptor pd : Introspector.getBeanInfo(entity.getClass()).getPropertyDescriptors()) {
      String name = pd.getName();
      if (properties.containsKey(name)) {

        Method writeMethod = pd.getWriteMethod();
        if (writeMethod != null) {

          Object value = properties.get(name);

          if (value instanceof Collection) {
            Set<Object> newCollection = new HashSet<Object>();

            for (Object collectionValue: (Collection)value) {

              if (collectionValue instanceof DynamicEntity) {

                Class<?> clazz = entity.getClass();
                Class<?> parameterizedTypeClass = getParameterizedTypeClass(clazz, pd.getName());

                collectionValue = toEntity(parameterizedTypeClass,
                    getDynamicEntityType(parameterizedTypeClass), (DynamicEntity) collectionValue);
              }
              if ( collectionValue != null) {
                newCollection.add(collectionValue);
              }
            }
            writeMethod.invoke(entity, newCollection);
          } else {
            if (value instanceof DynamicEntity) {

              Class<?> clazz = pd.getPropertyType();

              value = toEntity(clazz, getDynamicEntityType(clazz), (DynamicEntity) value);
            }
            if ( value != null) {
              writeMethod.invoke(entity, value);
            }
          }
        }
      }
    }
  }

  // determine whether or not a property of the given type should be a direct mapping in the dynamic entity
  private boolean isDirectMappingType(Class<?> propertyType) {
    return !Collection.class.isAssignableFrom(propertyType) && entityClassMap.get(propertyType) == null;
  }

  // get the dynamic entity type from the given view entity class
  private DynamicType getDynamicEntityType(Class clazz) {
    JPADynamicTypeBuilder builder = typeBuilderMap.get(entityClassMap.get(clazz));

    return builder == null ? null : builder.getType();
  }

  // get the id field name for the given view entity class
  private String getIdFieldName(Class clazz) throws PersistenceException {
    if (entityClassMap.containsKey(clazz)){
      String entityName = entityClassMap.get(clazz);
      if (entityMap.containsKey(entityName)) {
        return entityMap.get(entityName).getIdProperty();
      }
    }
    throw new PersistenceException("The class " + clazz.getName() + "is not registered as an entity.");
  }

  // get a descriptor map for the given bean class
  private static Map<String, PropertyDescriptor> getDescriptorMap(Class<?> clazz) throws IntrospectionException {
    Map<String, PropertyDescriptor> descriptorMap = new HashMap<String, PropertyDescriptor>();

    for (PropertyDescriptor pd : Introspector.getBeanInfo(clazz).getPropertyDescriptors()) {
      String name = pd.getName();
      if (pd.getReadMethod() != null && !name.equals("class")) {
        descriptorMap.put(name, pd);
      }
    }
    return descriptorMap;
  }

  // get the property names for the given view entity class
  private static Set<String> getPropertyNames(Class clazz) throws IntrospectionException {
    Set<String> propertyNames = new HashSet<String>();
    for (PropertyDescriptor pd : Introspector.getBeanInfo(clazz).getPropertyDescriptors()) {
      propertyNames.add(pd.getName());
    }
    return propertyNames;
  }

  // get the parameterized type class for the given field of the given class
  private static Class<?> getParameterizedTypeClass(Class clazz, String fieldName) throws NoSuchFieldException {
    Field field = clazz.getDeclaredField(fieldName);
    ParameterizedType parameterizedType = (ParameterizedType) field.getGenericType();
    return (Class<?>) parameterizedType.getActualTypeArguments()[0];
  }

  // throw a new persistence exception and log the error
  private static void throwPersistenceException(String msg, Exception e) throws PersistenceException {
    LOG.error(msg, e);
    throw new PersistenceException(msg, e);
  }

  // get a table name for the given view entity
  private static String getTableName(ViewEntityEntity entity) {
    return (getEntityName(entity)).toUpperCase();
  }

  // get a dynamic entity name for the given view entity
  private static String getEntityName(ViewEntityEntity entity) {
    String   className = entity.getClassName();
    String[] parts     = className.split("\\.");

    return parts[parts.length - 1] + entity.getId();
  }

  // get an entity manager
  private EntityManager getEntityManager() {
    return entityManagerFactory.createEntityManager();
  }
}
