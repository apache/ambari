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

import com.google.common.collect.FluentIterable;
import com.google.common.collect.Sets;
import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.persist.Transactional;
import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.api.resources.ResourceInstanceFactoryImpl;
import org.apache.ambari.server.api.resources.SubResourceDefinition;
import org.apache.ambari.server.api.resources.ViewExternalSubResourceDefinition;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.api.services.ViewExternalSubResourceService;
import org.apache.ambari.server.api.services.ViewSubResourceService;
import org.apache.ambari.server.configuration.ComponentSSLConfiguration;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.AmbariServer;
import org.apache.ambari.server.controller.AmbariSessionManager;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceProvider;
import org.apache.ambari.server.events.ServiceInstalledEvent;
import org.apache.ambari.server.events.publishers.AmbariEventPublisher;
import org.apache.ambari.server.orm.dao.MemberDAO;
import org.apache.ambari.server.orm.dao.PermissionDAO;
import org.apache.ambari.server.orm.dao.PrincipalDAO;
import org.apache.ambari.server.orm.dao.PrivilegeDAO;
import org.apache.ambari.server.orm.dao.RemoteAmbariClusterDAO;
import org.apache.ambari.server.orm.dao.ResourceDAO;
import org.apache.ambari.server.orm.dao.ResourceTypeDAO;
import org.apache.ambari.server.orm.dao.UserDAO;
import org.apache.ambari.server.orm.dao.ViewDAO;
import org.apache.ambari.server.orm.dao.ViewInstanceDAO;
import org.apache.ambari.server.orm.entities.GroupEntity;
import org.apache.ambari.server.orm.entities.MemberEntity;
import org.apache.ambari.server.orm.entities.PermissionEntity;
import org.apache.ambari.server.orm.entities.PrincipalEntity;
import org.apache.ambari.server.orm.entities.PrivilegeEntity;
import org.apache.ambari.server.orm.entities.RemoteAmbariClusterEntity;
import org.apache.ambari.server.orm.entities.ResourceEntity;
import org.apache.ambari.server.orm.entities.ResourceTypeEntity;
import org.apache.ambari.server.orm.entities.UserEntity;
import org.apache.ambari.server.orm.entities.ViewEntity;
import org.apache.ambari.server.orm.entities.ViewEntityEntity;
import org.apache.ambari.server.orm.entities.ViewInstanceDataEntity;
import org.apache.ambari.server.orm.entities.ViewInstanceEntity;
import org.apache.ambari.server.orm.entities.ViewParameterEntity;
import org.apache.ambari.server.orm.entities.ViewResourceEntity;
import org.apache.ambari.server.security.SecurityHelper;
import org.apache.ambari.server.security.authorization.AuthorizationHelper;
import org.apache.ambari.server.security.authorization.ClusterInheritedPermissionHelper;
import org.apache.ambari.server.security.authorization.ResourceType;
import org.apache.ambari.server.security.authorization.RoleAuthorization;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.stack.OsFamily;
import org.apache.ambari.server.utils.VersionUtils;
import org.apache.ambari.server.view.configuration.AutoInstanceConfig;
import org.apache.ambari.server.view.configuration.EntityConfig;
import org.apache.ambari.server.view.configuration.InstanceConfig;
import org.apache.ambari.server.view.configuration.ParameterConfig;
import org.apache.ambari.server.view.configuration.PermissionConfig;
import org.apache.ambari.server.view.configuration.PersistenceConfig;
import org.apache.ambari.server.view.configuration.PropertyConfig;
import org.apache.ambari.server.view.configuration.ResourceConfig;
import org.apache.ambari.server.view.configuration.ViewConfig;
import org.apache.ambari.server.view.validation.ValidationException;
import org.apache.ambari.view.AmbariStreamProvider;
import org.apache.ambari.view.ClusterType;
import org.apache.ambari.view.Masker;
import org.apache.ambari.view.SystemException;
import org.apache.ambari.view.View;
import org.apache.ambari.view.ViewContext;
import org.apache.ambari.view.ViewDefinition;
import org.apache.ambari.view.ViewInstanceDefinition;
import org.apache.ambari.view.ViewResourceHandler;
import org.apache.ambari.view.cluster.Cluster;
import org.apache.ambari.view.events.Event;
import org.apache.ambari.view.events.Listener;
import org.apache.ambari.view.validation.Validator;
import org.apache.log4j.PropertyConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.beans.IntrospectionException;
import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Registry for view and view instance definitions.
 */
@Singleton
public class ViewRegistry {

  /**
   * Constants
   */
  private static final String EXTRACTED_ARCHIVES_DIR = "work";
  private static final String EXTRACT_COMMAND = "extract";
  private static final String ALL_VIEWS_REG_EXP = ".*";
  protected static final int DEFAULT_REQUEST_CONNECT_TIMEOUT = 5000;
  protected static final int DEFAULT_REQUEST_READ_TIMEOUT    = 10000;
  private static final String VIEW_AMBARI_VERSION_REGEXP = "^((\\d+\\.)?)*(\\*|\\d+)$";
  private static final String VIEW_LOG_FILE = "view.log4j.properties";
  public static final String API_PREFIX = "/api/v1/clusters/";

  /**
   * Thread pool
   */
  private static ExecutorService executorService;

  /**
   * Mapping of view names to view definitions.
   */
  private Map<String, ViewEntity> viewDefinitions = new HashMap<String, ViewEntity>();

  /**
   * Mapping of view instances to view definition and instance name.
   */
  private Map<ViewEntity, Map<String, ViewInstanceEntity>> viewInstanceDefinitions =
      new HashMap<ViewEntity, Map<String, ViewInstanceEntity>>();

  /**
   * Mapping of view names to sub-resources.
   */
  private final Map<String, Set<SubResourceDefinition>> subResourceDefinitionsMap =
      new ConcurrentHashMap<String, Set<SubResourceDefinition>>();

  /**
   * Mapping of view types to resource providers.
   */
  private final Map<Resource.Type, ResourceProvider> resourceProviders =
      new ConcurrentHashMap<Resource.Type, ResourceProvider>();

  /**
   * Mapping of view names to registered listeners.
   */
  private final Map<String, Set<Listener>> listeners =
      new ConcurrentHashMap<String, Set<Listener>>();

  /**
   * The singleton view registry instance.
   */
  private static ViewRegistry singleton;

  /**
   * The logger.
   */
  protected final static Logger LOG = LoggerFactory.getLogger(ViewRegistry.class);

  /**
   * View data access object.
   */
  @Inject
  ViewDAO viewDAO;

  /**
   * View instance data access object.
   */
  @Inject
  ViewInstanceDAO instanceDAO;

  /**
   * User data access object.
   */
  @Inject
  UserDAO userDAO;

  /**
   * Group member data access object.
   */
  @Inject
  MemberDAO memberDAO;

  /**
   * Privilege data access object.
   */
  @Inject
  PrivilegeDAO privilegeDAO;

  /**
   * Helper with security related utilities.
   */
  @Inject
  SecurityHelper securityHelper;

  /**
   * Resource data access object.
   */
  @Inject
  ResourceDAO resourceDAO;

  /**
   * Resource type data access object.
   */
  @Inject
  ResourceTypeDAO resourceTypeDAO;

  /**
   * Principal data access object.
   */
  @Inject
  PrincipalDAO principalDAO;

  /**
   * Permission data access objects
   */
  @Inject
  PermissionDAO permissionDAO;

  /**
   * The Ambari managed clusters.
   */
  @Inject
  Provider<Clusters> clustersProvider;

  /**
   * Ambari meta info.
   */
  @Inject
  Provider<AmbariMetaInfo> ambariMetaInfoProvider;

  /**
   * Ambari configuration.
   */
  @Inject
  Configuration configuration;

  /**
   * The handler list.
   */
  @Inject
  ViewInstanceHandlerList handlerList;

  /**
   * The view extractor;
   */
  @Inject
  ViewExtractor extractor;

  /**
   * The view archive utility.
   */
  @Inject
  ViewArchiveUtility archiveUtility;

  /**
   * The Ambari session manager.
   */
  @Inject
  AmbariSessionManager ambariSessionManager;

  /**
   * Registry for Remote Ambari Cluster
   */
  @Inject
  RemoteAmbariClusterRegistry remoteAmbariClusterRegistry;

  /**
   * Remote Ambari Cluster Dao
   */
  @Inject
  RemoteAmbariClusterDAO remoteAmbariClusterDAO;


 // ----- Constructors -----------------------------------------------------

  /**
   * Create the view registry.
   */
  @Inject
  public ViewRegistry(AmbariEventPublisher publisher) {
    publisher.register(this);
  }


// ----- ViewRegistry ------------------------------------------------------

  /**
   * Registry main method.
   *
   * @param args  the command line arguments
   */
  public static void main(String[] args) {

    if (args.length >= 2) {

      if (args[0].equals(EXTRACT_COMMAND)) {

        String     archivePath = args[1];
        ViewModule viewModule  = new ViewModule();

        try {
          if (extractViewArchive(archivePath, viewModule, true)) {
            System.exit(0);
          }
        } catch (Exception e) {
          String msg = "Caught exception extracting view archive " + archivePath + ".";
          LOG.error(msg, e);
          System.exit(2);
        }
      }
    }
    System.exit(1);
  }

  /**
   * Get the collection of all the view definitions.
   *
   * @return the collection of view definitions
   */
  public Collection<ViewEntity> getDefinitions() {
    return viewDefinitions.values();
  }

  /**
   * Get a view definition for the given name.
   *
   * @param viewName  the view name
   * @param version   the version
   *
   * @return the view definition for the given name
   */
  public ViewEntity getDefinition(String viewName, String version) {
    return getDefinition(ViewEntity.getViewName(viewName, version));
  }

  /**
   * Get the view definition for the given resource type.
   *
   * @param resourceTypeEntity  the resource type
   *
   * @return the view definition for the given resource type or null
   */
  public ViewEntity getDefinition(ResourceTypeEntity resourceTypeEntity) {

    for (ViewEntity viewEntity : viewDefinitions.values()) {
      if (viewEntity.isDeployed()) {
        if (viewEntity.getResourceType().equals(resourceTypeEntity)) {
          return viewEntity;
        }
      }
    }
    return null;
  }

  /**
   * Add a view definition to the registry.
   *
   * @param definition  the definition
   */
  public void addDefinition(ViewEntity definition) {
    viewDefinitions.put(definition.getName(), definition);
  }

  /**
   * Get the collection of view instances for the given view definition.
   *
   * @param definition  the view definition
   *
   * @return the collection of view instances for the view definition
   */
  public Collection<ViewInstanceEntity> getInstanceDefinitions(ViewEntity definition) {
    if (definition != null) {
      Map<String, ViewInstanceEntity> instanceEntityMap = viewInstanceDefinitions.get(definition);
      if (instanceEntityMap != null) {
        return instanceEntityMap.values();
      }
    }
    return Collections.emptyList();
  }

  /**
   * Get the instance definition for the given view name and instance name.
   *
   * @param viewName      the view name
   * @param version       the version
   * @param instanceName  the instance name
   *
   * @return the view instance definition for the given view and instance name
   */
  public ViewInstanceEntity getInstanceDefinition(String viewName, String version, String instanceName) {
    Map<String, ViewInstanceEntity> viewInstanceDefinitionMap =
        viewInstanceDefinitions.get(getDefinition(viewName, version));

    return viewInstanceDefinitionMap == null ? null : viewInstanceDefinitionMap.get(instanceName);
  }

  /**
   * Add an instance definition for the given view definition.
   *
   * @param definition          the owning view definition
   * @param instanceDefinition  the instance definition
   */
  public void addInstanceDefinition(ViewEntity definition, ViewInstanceEntity instanceDefinition) {
    Map<String, ViewInstanceEntity> instanceDefinitions = viewInstanceDefinitions.get(definition);
    if (instanceDefinitions == null) {
      instanceDefinitions = new HashMap<String, ViewInstanceEntity>();
      viewInstanceDefinitions.put(definition, instanceDefinitions);
    }

    View view = definition.getView();
    if (view != null) {
      view.onCreate(instanceDefinition);
    }
    instanceDefinitions.put(instanceDefinition.getName(), instanceDefinition);
  }

  /**
   * Remove an instance definition for the given view definition.
   *
   * @param definition    the owning view definition
   * @param instanceName  the instance name
   */
  public void removeInstanceDefinition(ViewEntity definition, String instanceName) {
    Map<String, ViewInstanceEntity> instanceDefinitions = viewInstanceDefinitions.get(definition);
    if (instanceDefinitions != null) {

      ViewInstanceEntity instanceDefinition = instanceDefinitions.get(instanceName);
      if (instanceDefinition != null) {
        View view = definition.getView();
        if (view != null) {
          view.onDestroy(instanceDefinition);
        }
        instanceDefinitions.remove(instanceName);
      }
    }
  }

  /**
   * Init the singleton instance.
   *
   * @param singleton  the view registry
   */
  public static void initInstance(ViewRegistry singleton) {
    ViewRegistry.singleton = singleton;
  }

  /**
   * Get the view registry singleton.
   *
   * @return  the view registry
   */
  public static ViewRegistry getInstance() {
    return singleton;
  }

  /**
   * Get the sub-resource definitions for the given view name.
   *
   * @param viewName  the instance name
   * @param version   the version
   *
   * @return the set of sub-resource definitions
   */
  public Set<SubResourceDefinition> getSubResourceDefinitions(
      String viewName, String version) {


    viewName = ViewEntity.getViewName(viewName, version);

    return subResourceDefinitionsMap.get(viewName);
  }
  /**
   * Read all view archives.
   */
  public void readViewArchives() {
    readViewArchives(false, false, ALL_VIEWS_REG_EXP);
  }

  /**
   * Read only view archives with names corresponding to given regular expression.
   *
   * @param viewNameRegExp view name regular expression
   */
  public void readViewArchives(String viewNameRegExp) {
    readViewArchives(false, false, viewNameRegExp);
  }

  /**
   * Determine whether or not the given view instance exists.
   *
   * @param instanceEntity  the view instance entity
   *
   * @return true if the the given view instance exists; false otherwise
   */
  public boolean instanceExists(ViewInstanceEntity instanceEntity) {

    ViewEntity viewEntity = getDefinition(instanceEntity.getViewName());

    return viewEntity != null &&
        (getInstanceDefinition(viewEntity.getCommonName(), viewEntity.getVersion(), instanceEntity.getName()) != null);
  }

  /**
   * Install the given view instance with its associated view.
   *
   * @param instanceEntity  the view instance entity
   *
   * @throws ValidationException       if the given instance fails the validation checks
   * @throws IllegalArgumentException  if the view associated with the given instance
   *                                   does not exist
   * @throws SystemException           if the instance can not be installed
   */
  public void installViewInstance(ViewInstanceEntity instanceEntity)
      throws ValidationException, IllegalArgumentException, SystemException {
    ViewEntity viewEntity = getDefinition(instanceEntity.getViewName());

    if (viewEntity != null) {
      String instanceName = instanceEntity.getName();
      String viewName     = viewEntity.getCommonName();
      String version      = viewEntity.getVersion();

      if (getInstanceDefinition(viewName, version, instanceName) == null) {
        if (LOG.isDebugEnabled()) {
          LOG.debug("Creating view instance " + viewName + "/" +
              version + "/" + instanceName);
        }

        instanceEntity.validate(viewEntity, Validator.ValidationContext.PRE_CREATE);

        setPersistenceEntities(instanceEntity);

        ViewInstanceEntity persistedInstance = mergeViewInstance(instanceEntity, viewEntity.getResourceType());

        instanceEntity.setViewInstanceId(persistedInstance.getViewInstanceId());
        syncViewInstance(instanceEntity, persistedInstance);

        try {
          // bind the view instance to a view
          bindViewInstance(viewEntity, instanceEntity);
        } catch (Exception e) {
          String message = "Caught exception installing view instance.";
          LOG.error(message, e);
          throw new IllegalStateException(message, e);
        }
        // update the registry
        addInstanceDefinition(viewEntity, instanceEntity);

        // add the web app context
        handlerList.addViewInstance(instanceEntity);
      }
    } else {
      String message = "Attempt to install an instance for an unknown view " +
          instanceEntity.getViewName() + ".";

      LOG.error(message);
      throw new IllegalArgumentException(message);
    }
  }

  /**
   * Update a view instance for the view with the given view name.
   *
   * @param instanceEntity  the view instance entity
   *
   * @throws ValidationException   if the given instance fails the validation checks
   * @throws SystemException       if the instance can not be updated
   */
  public void updateViewInstance(ViewInstanceEntity instanceEntity)
      throws ValidationException, SystemException {
    ViewEntity viewEntity = getDefinition(instanceEntity.getViewName());

    if (viewEntity != null) {
      instanceEntity.validate(viewEntity, Validator.ValidationContext.PRE_UPDATE);
      instanceDAO.merge(instanceEntity);

      syncViewInstance(instanceEntity);
    }
  }

  /**
   * Calls onUpdate hook on View class
   *
   * @param instanceEntity
   */
  public void updateView(ViewInstanceEntity instanceEntity){
    ViewEntity viewEntity = getDefinition(instanceEntity.getViewName());
    if(null != viewEntity && null != viewEntity.getView()){
      viewEntity.getView().onUpdate(instanceEntity);
    }
  }

  /**
   * Get a view instance entity for the given view name and instance name.
   *
   * @param viewName      the view name
   * @param instanceName  the instance name
   *
   * @return a view instance entity for the given view name and instance name.
   */
  public ViewInstanceEntity getViewInstanceEntity(String viewName, String instanceName) {
    return instanceDAO.findByName(viewName, instanceName);
  }

  /**
   * Uninstall a view instance for the view with the given view name.
   *
   * @param instanceEntity  the view instance entity
   * @throws IllegalStateException if the given instance is not in a valid state
   */
  @Transactional
  public void uninstallViewInstance(ViewInstanceEntity instanceEntity) throws IllegalStateException {
    ViewEntity viewEntity = getDefinition(instanceEntity.getViewName());

    if (viewEntity != null) {
      String instanceName = instanceEntity.getName();
      String viewName     = viewEntity.getCommonName();
      String version      = viewEntity.getVersion();

      if (getInstanceDefinition(viewName, version, instanceName) != null) {
        if (instanceEntity.isXmlDriven()) {
          throw new IllegalStateException("View instances defined via xml can't be deleted through api requests");
        }
        if (LOG.isDebugEnabled()) {
          LOG.debug("Deleting view instance " + viewName + "/" +
              version + "/" +instanceName);
        }
        List<PrivilegeEntity> instancePrivileges = privilegeDAO.findByResourceId(instanceEntity.getResource().getId());
        for (PrivilegeEntity privilegeEntity : instancePrivileges) {
          removePrivilegeEntity(privilegeEntity);
        }
        instanceDAO.remove(instanceEntity);
        viewEntity.removeInstanceDefinition(instanceName);
        removeInstanceDefinition(viewEntity, instanceName);

        // remove the web app context
        handlerList.removeViewInstance(instanceEntity);
      }
    }
  }

  /**
   * Remove the data entry keyed by the given key from the given instance entity.
   *
   * @param instanceEntity  the instance entity
   * @param key             the data key
   */
  @Transactional
  public void removeInstanceData(ViewInstanceEntity instanceEntity, String key) {
    ViewInstanceDataEntity dataEntity = instanceEntity.getInstanceData(key);
    if (dataEntity != null) {
      instanceDAO.removeData(dataEntity);
    }
    instanceEntity.removeInstanceData(key);
    instanceDAO.merge(instanceEntity);
  }

  /**
   * Copy all privileges from one view instance to another
   *
   * @param sourceInstanceEntity  the source instance entity
   * @param targetInstanceEntity  the target instance entity
   */
  @Transactional
  public void copyPrivileges(ViewInstanceEntity sourceInstanceEntity,
                             ViewInstanceEntity targetInstanceEntity) {
    LOG.debug("Copy all privileges from " + sourceInstanceEntity.getName() + " to " +
              targetInstanceEntity.getName());
    List<PrivilegeEntity> targetInstancePrivileges = privilegeDAO.findByResourceId(targetInstanceEntity.getResource().getId());
    if (targetInstancePrivileges.size() > 0) {
      LOG.warn("Old privileges will be removed for " + targetInstanceEntity.getName());
      for (PrivilegeEntity privilegeEntity : targetInstancePrivileges) {
        removePrivilegeEntity(privilegeEntity);
      }
    }

    List<PrivilegeEntity> sourceInstancePrivileges = privilegeDAO.findByResourceId(sourceInstanceEntity.getResource().getId());
    for (PrivilegeEntity privilegeEntity : sourceInstancePrivileges) {
      privilegeDAO.detach(privilegeEntity);
      privilegeEntity.setResource(targetInstanceEntity.getResource());
      privilegeEntity.setId(null);
      privilegeDAO.create(privilegeEntity);
      privilegeEntity.getPrincipal().getPrivileges().add(privilegeEntity);
    }
  }

  /**
   * Notify any registered listeners of the given event.
   *
   * @param event  the event
   */
  public void fireEvent(Event event) {

    ViewDefinition subject = event.getViewSubject();

    fireEvent(event, subject.getViewName());
    fireEvent(event, ViewEntity.getViewName(subject.getViewName(), subject.getVersion()));
  }

  /**
   * Register the given listener to listen for events from the view identified by the given name and version.
   *
   * @param listener     the listener
   * @param viewName     the view name
   * @param viewVersion  the view version; null indicates all versions
   */
  public synchronized void registerListener(Listener listener, String viewName, String viewVersion) {

    String name = viewVersion == null ? viewName : ViewEntity.getViewName(viewName, viewVersion);

    Set<Listener> listeners = this.listeners.get(name);

    if (listeners == null) {
      listeners = Sets.newSetFromMap(new ConcurrentHashMap<Listener, Boolean>());
      this.listeners.put(name, listeners);
    }

    listeners.add(listener);
  }

  /**
   * Un-register the given listener from the view identified by the given name and version.
   *
   * @param listener     the listener
   * @param viewName     the view name
   * @param viewVersion  the view version; null indicates all versions
   */
  public synchronized void unregisterListener(Listener listener, String viewName, String viewVersion) {

    String name = viewVersion == null ? viewName : ViewEntity.getViewName(viewName, viewVersion);

    Set<Listener> listeners = this.listeners.get(name);

    if (listeners != null) {
      listeners.remove(listener);
    }
  }

  /**
   * Determine whether or not the access specified by the given permission
   * is permitted for the given user on the view instance identified by
   * the given resource.
   *
   * @param permissionEntity  the permission entity
   * @param resourceEntity    the resource entity
   * @param userName          the user name
   *
   * @return true if the access specified by the given permission
   *         is permitted for the given user.
   */
  public boolean hasPermission(PermissionEntity permissionEntity, ResourceEntity resourceEntity, String userName) {

    UserEntity userEntity = userDAO.findUserByName(userName);

    if (userEntity == null) {
      return false;
    }

    if (privilegeDAO.exists(userEntity.getPrincipal(), resourceEntity, permissionEntity)) {
      return true;
    }

    List<MemberEntity> memberEntities = memberDAO.findAllMembersByUser(userEntity);

    for (MemberEntity memberEntity : memberEntities) {

      GroupEntity groupEntity = memberEntity.getGroup();

      if (privilegeDAO.exists(groupEntity.getPrincipal(), resourceEntity, permissionEntity)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Determine whether or not access to the view instance resource identified
   * by the given instance name should be allowed based on the permissions
   * granted to the current user.
   *
   * @param viewName      the view name
   * @param version       the view version
   * @param instanceName  the name of the view instance resource
   * @param readOnly      indicate whether or not this is for a read only operation
   *
   * @return true if the access to the view instance is allowed
   */
  public boolean checkPermission(String viewName, String version, String instanceName, boolean readOnly) {

    ViewInstanceEntity instanceEntity =
        instanceName == null ? null : getInstanceDefinition(viewName, version, instanceName);

    return checkPermission(instanceEntity, readOnly);
  }

  /**
   * Determine whether or not access to the given view instance should be allowed based
   * on the permissions granted to the current user.
   *
   * @param instanceEntity  the view instance entity
   * @param readOnly        indicate whether or not this is for a read only operation
   *
   * @return true if the access to the view instance is allowed
   */
  public boolean checkPermission(ViewInstanceEntity instanceEntity, boolean readOnly) {

    ResourceEntity resourceEntity = instanceEntity == null ? null : instanceEntity.getResource();

    return (resourceEntity == null && readOnly) || checkAuthorization(resourceEntity);
  }

  /**
   * Determine whether or not the current view user has admin rights.
   *
   * @return true if the current view user is an admin
   */
  public boolean checkAdmin() {
    return checkAuthorization(null);
  }

  /**
   * Determine whether or not the given view definition resource should be included
   * based on the permissions granted to the current user.
   *
   * @param definitionEntity  the view definition entity
   *
   * @return true if the view instance should be included based on the permissions of the current user
   */
  public boolean includeDefinition(ViewEntity definitionEntity) {

    if (checkPermission(null, false)) {
      return true;
    }

    for (ViewInstanceEntity instanceEntity: definitionEntity.getInstances()) {
      if (checkPermission(instanceEntity, true) ) {
        return true;
      }
    }
    return false;
  }

  /**
   * Set the properties of the given view instance from the given property set.
   *
   * @param instanceEntity  the view instance entity
   * @param properties      the view instance properties
   * @param viewConfig      the view configuration
   * @param classLoader     the class loader for the view
   *
   * @throws SystemException if the view instance properties can not be set
   */
  public void setViewInstanceProperties(ViewInstanceEntity instanceEntity, Map<String, String> properties,
                                        ViewConfig viewConfig, ClassLoader classLoader) throws SystemException {
    try {
      Masker masker = getMasker(viewConfig.getMaskerClass(classLoader));

      Map<String, ParameterConfig> parameterConfigMap = new HashMap<String, ParameterConfig>();
      for (ParameterConfig paramConfig : viewConfig.getParameters()) {
        parameterConfigMap.put(paramConfig.getName(), paramConfig);
      }
      for (Map.Entry<String, String> entry : properties.entrySet()) {
        String name  = entry.getKey();
        String value = entry.getValue();

        ParameterConfig parameterConfig = parameterConfigMap.get(name);

        if (parameterConfig != null && parameterConfig.isMasked()) {
          value = masker.mask(value);
        }
        instanceEntity.putProperty(name, value);
      }
    } catch (Exception e) {
      throw new SystemException("Caught exception while setting instance property.", e);
    }
  }

  /**
   * Get the cluster associated with the given view instance.
   *
   * @param viewInstance  the view instance
   *
   * @return the cluster
   */
  public Cluster getCluster(ViewInstanceDefinition viewInstance) {
    if (viewInstance != null) {
      String clusterId = viewInstance.getClusterHandle();

      if (clusterId != null && viewInstance.getClusterType() == ClusterType.LOCAL_AMBARI) {
        try {
          return new ClusterImpl(clustersProvider.get().getCluster(clusterId));
        } catch (AmbariException e) {
          LOG.warn("Could not find the cluster identified by " + clusterId + ".");
        }
      } else if(clusterId != null && viewInstance.getClusterType() == ClusterType.REMOTE_AMBARI){
        return remoteAmbariClusterRegistry.get(clusterId);
      }
    }
    return null;
  }

  /**
   * Receive notification that a new service has been added to a cluster.
   * </p>
   * Used for view instance auto creation.
   *
   * @param event  the service installed event
   */
  @Subscribe
  @AllowConcurrentEvents
  public void onAmbariEvent(ServiceInstalledEvent event) {

    Clusters clusters  = clustersProvider.get();
    Long     clusterId = event.getClusterId();

    try {
      org.apache.ambari.server.state.Cluster cluster = clusters.getClusterById(clusterId);
      String clusterName = cluster.getClusterName();

      StackId     stackId       = cluster.getCurrentStackVersion();
      Set<String> serviceNames  = cluster.getServices().keySet();

      for (ViewEntity viewEntity : getDefinitions()) {

        String             viewName   = viewEntity.getName();
        ViewConfig         viewConfig = viewEntity.getConfiguration();
        AutoInstanceConfig autoConfig = viewConfig.getAutoInstance();

        try {
          if (checkAutoInstanceConfig(autoConfig, stackId, event.getServiceName(), serviceNames)) {

            LOG.info("Auto creating instance of view " + viewName + " for cluster " + clusterName + ".");
            ViewInstanceEntity viewInstanceEntity = createViewInstanceEntity(viewEntity, viewConfig, autoConfig);
            viewInstanceEntity.setClusterHandle(clusterName);
            installViewInstance(viewInstanceEntity);
          }
        } catch (Exception e) {
          LOG.error("Can't auto create instance of view " + viewName + " for cluster " + clusterName +
              ".  Caught exception :" + e.getMessage(), e);
        }
      }
    } catch (AmbariException e) {
      LOG.warn("Unknown cluster id " + clusterId + ".");
    }
  }


  // ----- helper methods ----------------------------------------------------

  /**
   * Determine whether a new view instance should be automatically created and associated with
   * a cluster based on the given configuration and cluster state.
   *
   * @param autoConfig    the view instance auto creation configuration
   * @param stackId       the stack id of the cluster
   * @param serviceName   the name of the service added which triggered this check
   * @param serviceNames  the set of service names of the cluster
   *
   * @return true if a new view instance should be created
   */
  private boolean checkAutoInstanceConfig(AutoInstanceConfig autoConfig, StackId stackId,
                                          String serviceName, Set<String> serviceNames) {

    if (autoConfig != null) {
      List<String> autoCreateServices = autoConfig.getServices();

      if (autoCreateServices != null && autoCreateServices.contains(serviceName) &&
          serviceNames.containsAll(autoCreateServices)) {

        String configStackId = autoConfig.getStackId();

        if (configStackId != null) {
          StackId id = new StackId(configStackId);

          if (id.getStackName().equals(stackId.getStackName())) {

            String stackVersion       = stackId.getStackVersion();
            String configStackVersion = id.getStackVersion();

            // make sure that the configured stack version equals the cluster stack version (account for *)
            int compVal = 0;

            int index = configStackVersion.indexOf('*');
            if (index == -1) {
              compVal = VersionUtils.compareVersions(configStackVersion, stackVersion);
            } else  if (index > 0) {
              String[] parts = configStackVersion.substring(0, index).split("\\.");
              compVal = VersionUtils.compareVersions(configStackVersion, stackVersion, parts.length);
            }

            return compVal == 0;
          }
        }
      }
    }
    return false;
  }

  /**
   * Clear the registry.
   */
  protected void clear() {
    viewDefinitions.clear();
    viewInstanceDefinitions.clear();
    subResourceDefinitionsMap.clear();
    listeners.clear();
  }

  /**
   * Get the view resource provider mapping.
   *
   * @return the map of view resource providers
   */
  protected Map<Resource.Type, ResourceProvider> getResourceProviders() {
    return resourceProviders;
  }

  // get a view entity for the given internal view name
  private ViewEntity getDefinition(String viewName) {
    return viewDefinitions.get(viewName);
  }

  // setup the given view definition
  protected ViewEntity setupViewDefinition(ViewEntity viewDefinition, ClassLoader cl)
      throws ClassNotFoundException, IntrospectionException {

    ViewConfig viewConfig = viewDefinition.getConfiguration();

    viewDefinition.setClassLoader(cl);

    List<ParameterConfig> parameterConfigurations = viewConfig.getParameters();

    Collection<ViewParameterEntity> parameters = new HashSet<ViewParameterEntity>();

    String viewName = viewDefinition.getName();

    for (ParameterConfig parameterConfiguration : parameterConfigurations) {
      ViewParameterEntity viewParameterEntity =  new ViewParameterEntity();

      viewParameterEntity.setViewName(viewName);
      viewParameterEntity.setName(parameterConfiguration.getName());
      viewParameterEntity.setDescription(parameterConfiguration.getDescription());
      viewParameterEntity.setLabel(parameterConfiguration.getLabel());
      viewParameterEntity.setPlaceholder(parameterConfiguration.getPlaceholder());
      viewParameterEntity.setDefaultValue(parameterConfiguration.getDefaultValue());
      viewParameterEntity.setClusterConfig(parameterConfiguration.getClusterConfig());
      viewParameterEntity.setRequired(parameterConfiguration.isRequired());
      viewParameterEntity.setMasked(parameterConfiguration.isMasked());
      viewParameterEntity.setViewEntity(viewDefinition);
      parameters.add(viewParameterEntity);
    }
    viewDefinition.setParameters(parameters);

    List<ResourceConfig> resourceConfigurations = viewConfig.getResources();

    Resource.Type externalResourceType = viewDefinition.getExternalResourceType();

    ViewExternalSubResourceProvider viewExternalSubResourceProvider =
        new ViewExternalSubResourceProvider(externalResourceType, viewDefinition);
    viewDefinition.addResourceProvider(externalResourceType, viewExternalSubResourceProvider);

    resourceProviders.put(externalResourceType, viewExternalSubResourceProvider);

    ResourceInstanceFactoryImpl.addResourceDefinition(externalResourceType,
        new ViewExternalSubResourceDefinition(externalResourceType));

    Collection<ViewResourceEntity> resources = new HashSet<ViewResourceEntity>();
    for (ResourceConfig resourceConfiguration : resourceConfigurations) {
      ViewResourceEntity viewResourceEntity = new ViewResourceEntity();

      viewResourceEntity.setViewName(viewName);
      viewResourceEntity.setName(resourceConfiguration.getName());
      viewResourceEntity.setPluralName(resourceConfiguration.getPluralName());
      viewResourceEntity.setIdProperty(resourceConfiguration.getIdProperty());
      viewResourceEntity.setResource(resourceConfiguration.getResource());
      viewResourceEntity.setService(resourceConfiguration.getService());
      viewResourceEntity.setProvider(resourceConfiguration.getProvider());
      viewResourceEntity.setSubResourceNames(resourceConfiguration.getSubResourceNames());
      viewResourceEntity.setViewEntity(viewDefinition);

      ViewSubResourceDefinition resourceDefinition = new ViewSubResourceDefinition(viewDefinition, resourceConfiguration);
      viewDefinition.addResourceDefinition(resourceDefinition);

      Resource.Type type = resourceDefinition.getType();
      viewDefinition.addResourceConfiguration(type, resourceConfiguration);

      if (resourceConfiguration.isExternal()) {
        viewExternalSubResourceProvider.addResourceName(resourceConfiguration.getName());
      } else {
        ResourceInstanceFactoryImpl.addResourceDefinition(type, resourceDefinition);

        Class<?> clazz      = resourceConfiguration.getResourceClass(cl);
        String   idProperty = resourceConfiguration.getIdProperty();

        ViewSubResourceProvider provider = new ViewSubResourceProvider(type, clazz, idProperty, viewDefinition);
        viewDefinition.addResourceProvider(type, provider);
        resourceProviders.put(type, provider);

        resources.add(viewResourceEntity);
      }
      viewDefinition.setResources(resources);
    }

    ResourceTypeEntity resourceTypeEntity = new ResourceTypeEntity();
    resourceTypeEntity.setName(viewName);

    viewDefinition.setResourceType(resourceTypeEntity);

    List<PermissionConfig> permissionConfigurations = viewConfig.getPermissions();

    Collection<PermissionEntity> permissions = new HashSet<PermissionEntity>();
    for (PermissionConfig permissionConfiguration : permissionConfigurations) {
      PermissionEntity permissionEntity =  new PermissionEntity();

      permissionEntity.setPermissionName(permissionConfiguration.getName());
      permissionEntity.setResourceType(resourceTypeEntity);
      permissions.add(permissionEntity);
    }
    viewDefinition.setPermissions(permissions);

    View view = null;
    if (viewConfig.getView() != null) {
      view = getView(viewConfig.getViewClass(cl), new ViewContextImpl(viewDefinition, this));
    }
    viewDefinition.setView(view);
    Validator validator = null;
    if (viewConfig.getValidator() != null) {
      validator = getValidator(viewConfig.getValidatorClass(cl), new ViewContextImpl(viewDefinition, this));
    }
    viewDefinition.setValidator(validator);
    viewDefinition.setMask(viewConfig.getMasker());

    Set<SubResourceDefinition> subResourceDefinitions = new HashSet<SubResourceDefinition>();
    for (Resource.Type type : viewDefinition.getViewResourceTypes()) {
      subResourceDefinitions.add(new SubResourceDefinition(type));
    }
    subResourceDefinitionsMap.put(viewName, subResourceDefinitions);

    return viewDefinition;
  }

  // create a new view instance definition
  protected ViewInstanceEntity createViewInstanceDefinition(ViewConfig viewConfig, ViewEntity viewDefinition,
                                                            InstanceConfig instanceConfig)
      throws ValidationException, ClassNotFoundException, SystemException {
    ViewInstanceEntity viewInstanceDefinition = createViewInstanceEntity(viewDefinition, viewConfig, instanceConfig);
    viewInstanceDefinition.validate(viewDefinition, Validator.ValidationContext.PRE_CREATE);

    bindViewInstance(viewDefinition, viewInstanceDefinition);
    return viewInstanceDefinition;
  }

  // create a view instance from the given configuration
  private ViewInstanceEntity createViewInstanceEntity(ViewEntity viewDefinition, ViewConfig viewConfig,
                                                      InstanceConfig instanceConfig)
      throws SystemException {
    ViewInstanceEntity viewInstanceDefinition =
        new ViewInstanceEntity(viewDefinition, instanceConfig);

    Map<String, String> properties = new HashMap<String, String>();

    for (PropertyConfig propertyConfig : instanceConfig.getProperties()) {
      properties.put(propertyConfig.getKey(), propertyConfig.getValue());
    }
    setViewInstanceProperties(viewInstanceDefinition, properties, viewConfig, viewDefinition.getClassLoader());

    setPersistenceEntities(viewInstanceDefinition);

    return viewInstanceDefinition;
  }

  // bind a view instance definition to the given view definition
  protected void bindViewInstance(ViewEntity viewDefinition,
                                   ViewInstanceEntity viewInstanceDefinition)
      throws ClassNotFoundException {
    viewInstanceDefinition.setViewEntity(viewDefinition);

    ViewContext viewInstanceContext = new ViewContextImpl(viewInstanceDefinition, this);

    ViewExternalSubResourceService externalSubResourceService =
        new ViewExternalSubResourceService(viewDefinition.getExternalResourceType(), viewInstanceDefinition);

    viewInstanceDefinition.addService(ResourceConfig.EXTERNAL_RESOURCE_PLURAL_NAME, externalSubResourceService);

    Collection<ViewSubResourceDefinition> resourceDefinitions = viewDefinition.getResourceDefinitions().values();
    for (ViewSubResourceDefinition resourceDefinition : resourceDefinitions) {

      Resource.Type  type           = resourceDefinition.getType();
      ResourceConfig resourceConfig = resourceDefinition.getResourceConfiguration();

      ViewResourceHandler viewResourceService = new ViewSubResourceService(type, viewInstanceDefinition);

      ClassLoader cl = viewDefinition.getClassLoader();

      Object service = getService(resourceConfig.getServiceClass(cl), viewResourceService, viewInstanceContext);

      if (resourceConfig.isExternal()) {
        externalSubResourceService.addResourceService(resourceConfig.getName(), service);
      } else {
        viewInstanceDefinition.addService(viewDefinition.getResourceDefinition(type).getPluralName(),service);
        viewInstanceDefinition.addResourceProvider(type,
            getProvider(resourceConfig.getProviderClass(cl), viewInstanceContext));
      }
    }
    viewDefinition.addInstanceDefinition(viewInstanceDefinition);
  }

  // Set the entities defined in the view persistence element for the given view instance
  private static void setPersistenceEntities(ViewInstanceEntity viewInstanceDefinition) {
    ViewEntity viewDefinition = viewInstanceDefinition.getViewEntity();
    ViewConfig viewConfig     = viewDefinition.getConfiguration();

    Collection<ViewEntityEntity> entities = new HashSet<ViewEntityEntity>();

    if (viewConfig != null) {
      PersistenceConfig persistenceConfig = viewConfig.getPersistence();

      if (persistenceConfig != null) {
        for (EntityConfig entityConfiguration : persistenceConfig.getEntities()) {
          ViewEntityEntity viewEntityEntity = new ViewEntityEntity();

          viewEntityEntity.setViewName(viewDefinition.getName());
          viewEntityEntity.setViewInstanceName(viewInstanceDefinition.getName());
          viewEntityEntity.setClassName(entityConfiguration.getClassName());
          viewEntityEntity.setIdProperty(entityConfiguration.getIdProperty());
          viewEntityEntity.setViewInstance(viewInstanceDefinition);

          entities.add(viewEntityEntity);
        }
      }
    }
    viewInstanceDefinition.setEntities(entities);
  }

  // get the given service class from the given class loader; inject a handler and context
  private static <T> T getService(Class<T> clazz,
                                  final ViewResourceHandler viewResourceHandler,
                                  final ViewContext viewInstanceContext) {
    Injector viewInstanceInjector = Guice.createInjector(new AbstractModule() {
      @Override
      protected void configure() {
        bind(ViewResourceHandler.class)
            .toInstance(viewResourceHandler);
        bind(ViewContext.class)
            .toInstance(viewInstanceContext);
      }
    });
    return viewInstanceInjector.getInstance(clazz);
  }

  // get the given resource provider class from the given class loader; inject a context
  private static org.apache.ambari.view.ResourceProvider getProvider(
      Class<? extends org.apache.ambari.view.ResourceProvider> clazz,
      final ViewContext viewInstanceContext) {
    Injector viewInstanceInjector = Guice.createInjector(new AbstractModule() {
      @Override
      protected void configure() {
        bind(ViewContext.class)
            .toInstance(viewInstanceContext);
      }
    });
    return viewInstanceInjector.getInstance(clazz);
  }

  // get the given view class from the given class loader; inject a context
  private static View getView(Class<? extends View> clazz,
                              final ViewContext viewContext) {
    Injector viewInstanceInjector = Guice.createInjector(new AbstractModule() {
      @Override
      protected void configure() {
        bind(ViewContext.class)
            .toInstance(viewContext);
      }
    });
    return viewInstanceInjector.getInstance(clazz);
  }

  // get the given view validator class from the given class loader; inject a context
  private static Validator getValidator(Class<? extends Validator> clazz,
                              final ViewContext viewContext) {
    Injector viewInstanceInjector = Guice.createInjector(new AbstractModule() {
      @Override
      protected void configure() {
        bind(ViewContext.class)
            .toInstance(viewContext);
      }
    });
    return viewInstanceInjector.getInstance(clazz);
  }

  // create masker from given class; probably replace with injector later
  private static Masker getMasker(Class<? extends Masker> clazz) {
    try {
      return clazz.newInstance();
    } catch (Exception e) {
      LOG.error("Could not create masker instance", e);
    }
    return null;
  }

  // remove undeployed views from the ambari db
  private void removeUndeployedViews() {
    for (ViewEntity viewEntity : viewDAO.findAll()) {
      String name = viewEntity.getName();
      if (!ViewRegistry.getInstance().viewDefinitions.containsKey(name)) {
        try {
          viewDAO.remove(viewEntity);
        } catch (Exception e) {
          LOG.error("Caught exception undeploying view " + viewEntity.getName(), e);
        }
      }
    }
  }

  /**
   * Sync given view with data in DB. Ensures that view data in DB is updated,
   * all instances changes from xml config are reflected to DB
   *
   * @param view                 view config from xml
   * @param instanceDefinitions  view instances from xml
   *
   * @throws Exception if the view can not be synced
   */
  private void syncView(ViewEntity view,
                        Set<ViewInstanceEntity> instanceDefinitions)
      throws Exception {

    String      viewName      = view.getName();
    ViewEntity  persistedView = viewDAO.findByName(viewName);

    if (LOG.isDebugEnabled()) {
      LOG.debug("Syncing view " + viewName + ".");
    }

    // if the view is not yet persisted ...
    if (persistedView == null) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Creating view " + viewName + ".");
      }

      // create an admin resource type to represent this view
      ResourceTypeEntity resourceType = resourceTypeDAO.merge(view.getResourceType());

      for( ViewInstanceEntity instance : view.getInstances()) {
        instance.setResource(createViewInstanceResource(resourceType));
      }
      // ... merge the view
      view.setResourceType(resourceType);
      persistedView = viewDAO.merge(view);
    }

    view.setResourceType(persistedView.getResourceType());
    view.setPermissions(persistedView.getPermissions());

    // make sure that each instance of the view in the db is reflected in the given view
    for (ViewInstanceEntity persistedInstance : persistedView.getInstances()){

      String             instanceName = persistedInstance.getName();
      ViewInstanceEntity instance     = view.getInstanceDefinition(instanceName);

      // if the persisted instance is not in the view ...
      if (instance == null) {
        if (persistedInstance.isXmlDriven()) {
          // this instance was persisted from an earlier view.xml but has been removed...
          // remove it from the db
          instanceDAO.remove(persistedInstance);
        } else {
          // this instance was not specified in the view.xml but was added through the API...
          // bind it to the view and add it to the registry
          instanceDAO.merge(persistedInstance);
          bindViewInstance(view, persistedInstance);
          instanceDefinitions.add(persistedInstance);
        }
      } else {
        syncViewInstance(instance, persistedInstance);
      }
    }
    if (LOG.isDebugEnabled()) {
      LOG.debug("Syncing view " + viewName + " complete.");
    }
  }

  // sync the given view instance entity to the matching view instance entity in the registry
  private void syncViewInstance(ViewInstanceEntity instanceEntity) {
    String viewName     = instanceEntity.getViewDefinition().getViewName();
    String version      = instanceEntity.getViewDefinition().getVersion();
    String instanceName = instanceEntity.getInstanceName();

    ViewInstanceEntity registryEntry = getInstanceDefinition(viewName, version, instanceName);
    if (registryEntry != null) {
      syncViewInstance(registryEntry, instanceEntity);
    }
  }

  // sync a given view instance entity with another given view instance entity
  private void syncViewInstance(ViewInstanceEntity instance1, ViewInstanceEntity instance2) {
    instance1.setLabel(instance2.getLabel());
    instance1.setDescription(instance2.getDescription());
    instance1.setViewUrl(instance2.getViewUrl());
    instance1.setVisible(instance2.isVisible());
    instance1.setResource(instance2.getResource());
    instance1.setViewInstanceId(instance2.getViewInstanceId());
    instance1.setClusterHandle(instance2.getClusterHandle());
    instance1.setClusterType(instance2.getClusterType());
    instance1.setData(instance2.getData());
    instance1.setEntities(instance2.getEntities());
    instance1.setProperties(instance2.getProperties());
  }

  // create an admin resource for the given view instance entity and merge it
  @Transactional
  ViewInstanceEntity mergeViewInstance(ViewInstanceEntity instanceEntity, ResourceTypeEntity resourceTypeEntity) {
    // create an admin resource to represent this view instance
    instanceEntity.setResource(createViewInstanceResource(resourceTypeEntity));

    return instanceDAO.merge(instanceEntity);
  }

  // create an admin resource to represent a view instance
  private ResourceEntity createViewInstanceResource(ResourceTypeEntity resourceTypeEntity) {
    ResourceEntity resourceEntity = new ResourceEntity();
    resourceEntity.setResourceType(resourceTypeEntity);
    resourceDAO.create(resourceEntity);
    return resourceEntity;
  }

  // notify the view identified by the given view name of the given event
  private void fireEvent(Event event, String viewName) {
    Set<Listener> listeners = this.listeners.get(viewName);

    if (listeners != null) {
      for (Listener listener : listeners) {
        listener.notify(event);
      }
    }
  }

  // check that the current user is authorized to access the given view instance resource
  private boolean checkAuthorization(ResourceEntity resourceEntity) {
    Long resourceId = (resourceEntity == null) ? null : resourceEntity.getId();

    return (resourceId == null)
        ? AuthorizationHelper.isAuthorized(ResourceType.AMBARI, null, RoleAuthorization.AMBARI_MANAGE_VIEWS)
        : AuthorizationHelper.isAuthorized(ResourceType.VIEW, resourceId, RoleAuthorization.VIEW_USE);
  }

  // fire the onDeploy event.
  protected void onDeploy(ViewEntity definition) {
    View view = definition.getView();
    if (view != null) {
      view.onDeploy(definition);
    }
  }

  // remove a privilege entity.
  private void removePrivilegeEntity(PrivilegeEntity privilegeEntity) {

    PrincipalEntity principalEntity = privilegeEntity.getPrincipal();
    if (principalEntity != null) {
      principalEntity.removePrivilege(privilegeEntity);
    }

    privilegeDAO.remove(privilegeEntity);
  }


  /**
   * Extract a view archive at the specified path
   * @param path
   */
  public void readViewArchive(Path path) {

    File viewDir = configuration.getViewsDir();
    String extractedArchivesPath = viewDir.getAbsolutePath() +
            File.separator + EXTRACTED_ARCHIVES_DIR;

    File archiveFile = path.toAbsolutePath().toFile();
    if (extractor.ensureExtractedArchiveDirectory(extractedArchivesPath)) {
        try {
          final ViewConfig viewConfig = archiveUtility.getViewConfigFromArchive(archiveFile);
          String viewName = ViewEntity.getViewName(viewConfig.getName(), viewConfig.getVersion());
          final String extractedArchiveDirPath = extractedArchivesPath + File.separator + viewName;
          final File extractedArchiveDirFile = archiveUtility.getFile(extractedArchiveDirPath);
          final ViewEntity viewDefinition = new ViewEntity(viewConfig, configuration, extractedArchiveDirPath);
          addDefinition(viewDefinition);
          readViewArchive(viewDefinition, archiveFile, extractedArchiveDirFile, ambariMetaInfoProvider.get().getServerVersion());
        } catch (Exception e){
          LOG.error("Could not process archive at path "+path, e);
        }
    }

  }




  // read the view archives.
  private void readViewArchives(boolean systemOnly, boolean useExecutor,
                                String viewNameRegExp) {
    try {

      File viewDir = configuration.getViewsDir();
      String extractedArchivesPath = viewDir.getAbsolutePath() +
          File.separator + EXTRACTED_ARCHIVES_DIR;

      if (extractor.ensureExtractedArchiveDirectory(extractedArchivesPath)) {

        File[] files  = viewDir.listFiles();

        if (files != null) {

          Set<Runnable> extractionRunnables = new HashSet<Runnable>();

          final String serverVersion = ambariMetaInfoProvider.get().getServerVersion();

          for (final File archiveFile : files) {
            if (!archiveFile.isDirectory()) {

              try {
                final ViewConfig viewConfig = archiveUtility.getViewConfigFromArchive(archiveFile);

                String commonName = viewConfig.getName();
                String version    = viewConfig.getVersion();
                String viewName   = ViewEntity.getViewName(commonName, version);

                if (!viewName.matches(viewNameRegExp)) {
                  continue;
                }

                final String extractedArchiveDirPath = extractedArchivesPath + File.separator + viewName;
                final File extractedArchiveDirFile = archiveUtility.getFile(extractedArchiveDirPath);

                final ViewEntity viewDefinition = new ViewEntity(viewConfig, configuration, extractedArchiveDirPath);

                boolean systemView = viewDefinition.isSystem();

                if (!systemOnly || systemView) {
                  // update the registry with the view
                  addDefinition(viewDefinition);

                  // always load system views up front
                  if (systemView || !useExecutor || extractedArchiveDirFile.exists()) {
                    // if the archive is already extracted then load the view now
                    readViewArchive(viewDefinition, archiveFile, extractedArchiveDirFile, serverVersion);
                  } else {
                    // if the archive needs to be extracted then create a runnable to do it
                    extractionRunnables.add(new Runnable() {
                      @Override
                      public void run() {
                        readViewArchive(viewDefinition, archiveFile, extractedArchiveDirFile, serverVersion);
                      }
                    });
                  }
                }
              } catch (Exception e) {
                String msg = "Caught exception reading view archive " + archiveFile.getAbsolutePath();
                LOG.error(msg, e);
              }
            }
          }

          if (useExecutor && extractionRunnables.size() > 0) {
            final ExecutorService executorService = getExecutorService(configuration);

            for (Runnable runnable : extractionRunnables) {
              // submit a new task for each archive that needs extraction
              executorService.submit(runnable);
            }
          }

          if (configuration.isViewRemoveUndeployedEnabled()) {
            removeUndeployedViews();
          }
        }
      } else {
        LOG.error("Could not create extracted view archive directory " + extractedArchivesPath + ".");
      }
    } catch (Exception e) {
      LOG.error("Caught exception reading view archives.", e);
    }
  }

  // read a view archive
  private void readViewArchive(ViewEntity viewDefinition,
                               File archiveFile,
                               File extractedArchiveDirFile,
                               String serverVersion) {

    setViewStatus(viewDefinition, ViewEntity.ViewStatus.DEPLOYING, "Deploying " + extractedArchiveDirFile + ".");

    String extractedArchiveDirPath = extractedArchiveDirFile.getAbsolutePath();

    LOG.info("Reading view archive " + archiveFile + ".");

    try {
      // extract the archive and get the class loader
      ClassLoader cl = extractor.extractViewArchive(viewDefinition, archiveFile, extractedArchiveDirFile);

      configureViewLogging(viewDefinition,cl);

      ViewConfig viewConfig = archiveUtility.getViewConfigFromExtractedArchive(extractedArchiveDirPath,
          configuration.isViewValidationEnabled());

      viewDefinition.setConfiguration(viewConfig);

      if (checkViewVersions(viewDefinition, serverVersion)) {
        setupViewDefinition(viewDefinition, cl);

        Set<ViewInstanceEntity> instanceDefinitions = new HashSet<ViewInstanceEntity>();

        for (InstanceConfig instanceConfig : viewConfig.getInstances()) {
          ViewInstanceEntity instanceEntity = createViewInstanceDefinition(viewConfig, viewDefinition, instanceConfig);
          instanceEntity.setXmlDriven(true);
          instanceDefinitions.add(instanceEntity);
        }
        persistView(viewDefinition, instanceDefinitions);

        // add auto instance configurations if required
        addAutoInstanceDefinition(viewDefinition);

        setViewStatus(viewDefinition, ViewEntity.ViewStatus.DEPLOYED, "Deployed " + extractedArchiveDirPath + ".");

        LOG.info("View deployed: " + viewDefinition.getName() + ".");
      }
    } catch (Exception e) {
      String msg = "Caught exception loading view " + viewDefinition.getName();

      setViewStatus(viewDefinition, ViewEntity.ViewStatus.ERROR, msg + " : " + e.getMessage());
      LOG.error(msg, e);
    }
  }

  private void configureViewLogging(ViewEntity viewDefinition,ClassLoader cl) {
    URL resourceURL = cl.getResource(VIEW_LOG_FILE);
    if( null != resourceURL ){
      LOG.info("setting up logging for view {} as per property file {}",viewDefinition.getName(), resourceURL);
      PropertyConfigurator.configure(resourceURL);
    }
  }

  private void addAutoInstanceDefinition(ViewEntity viewEntity) {
    ViewConfig viewConfig = viewEntity.getConfiguration();
    String viewName = viewEntity.getViewName();

    AutoInstanceConfig autoInstanceConfig = viewConfig.getAutoInstance();
    if (autoInstanceConfig == null) {
      return;
    }

    List<String> services = autoInstanceConfig.getServices();
    List<String> permissions = autoInstanceConfig.getPermissions();

    Map<String, org.apache.ambari.server.state.Cluster> allClusters = clustersProvider.get().getClusters();
    for (org.apache.ambari.server.state.Cluster cluster : allClusters.values()) {

      String clusterName = cluster.getClusterName();
      StackId stackId = cluster.getCurrentStackVersion();
      Set<String> serviceNames = cluster.getServices().keySet();

      for (String service : services) {
        try {

          if (checkAutoInstanceConfig(autoInstanceConfig, stackId, service, serviceNames)) {
            LOG.info("Auto creating instance of view " + viewName + " for cluster " + clusterName + ".");
            ViewInstanceEntity viewInstanceEntity = createViewInstanceEntity(viewEntity, viewConfig, autoInstanceConfig);
            viewInstanceEntity.setClusterHandle(clusterName);
            installViewInstance(viewInstanceEntity);
            addClusterInheritedPermissions(viewInstanceEntity, permissions);
          }
        } catch (Exception e) {
          LOG.error("Can't auto create instance of view " + viewName + " for cluster " + clusterName +
            ".  Caught exception :" + e.getMessage(), e);
        }
      }
    }
  }

  /**
   * Validates principalTypes and creates privilege entities for each permission type for the view instance entity
   * resource.
   * @param viewInstanceEntity - view instance entity for which permission has to be set.
   * @param principalTypes - list of cluster inherited principal types
   */
  @Transactional
  private void addClusterInheritedPermissions(ViewInstanceEntity viewInstanceEntity, List<String> principalTypes) {
    List<String> validPermissions = FluentIterable.from(principalTypes)
      .filter(ClusterInheritedPermissionHelper.validPrincipalTypePredicate)
      .toList();

    for(String permission: validPermissions) {
      addClusterInheritedPermission(viewInstanceEntity, permission);
    }
  }

  private void addClusterInheritedPermission(ViewInstanceEntity viewInstanceEntity, String principalType) {
    ResourceEntity resource = viewInstanceEntity.getResource();
    List<PrincipalEntity> principals = principalDAO.findByPrincipalType(principalType);
    if (principals.size() == 0) {
      LOG.error("Failed to find principal for principal type '{}'", principalType);
      return;
    }

    PrincipalEntity principal = principals.get(0); // There will be only one principal associated with the principal type
    PermissionEntity permission = permissionDAO.findViewUsePermission();

    if (!privilegeDAO.exists(principal, resource, permission)) {
      PrivilegeEntity privilege = new PrivilegeEntity();
      privilege.setPrincipal(principal);
      privilege.setResource(resource);
      privilege.setPermission(permission);

      privilegeDAO.create(privilege);
    }
  }

  /**
   * Check the configured view max and min Ambari versions for the given view entity
   * against the given Ambari server version.
   *
   * @param view           the view
   * @param serverVersion  the server version
   *
   * @return true if the given server version >= min version && <= max version for the given view
   */
  protected boolean checkViewVersions(ViewEntity view, String serverVersion) {
    ViewConfig config = view.getConfiguration();

    return checkViewVersion(view, config.getMinAmbariVersion(), serverVersion, "minimum", -1, "less than") &&
           checkViewVersion(view, config.getMaxAmbariVersion(), serverVersion, "maximum", 1, "greater than");

  }

  // check the given version against the actual Ambari server version
  private boolean checkViewVersion(ViewEntity view, String version, String serverVersion, String label,
                                   int errValue, String errMsg) {

    if (version != null && !version.isEmpty()) {

      // make sure that the given version is a valid version string
      if (!version.matches(VIEW_AMBARI_VERSION_REGEXP)) {
        String msg = "The configured " + label + " Ambari version " + version + " for view " +
            view.getName() + " is not valid.";

        setViewStatus(view, ViewEntity.ViewStatus.ERROR, msg);
        LOG.error(msg);
        return false;
      }

      int index = version.indexOf('*');

      int compVal = index == -1 ? VersionUtils.compareVersions(serverVersion, version) :
                    index > 0 ? VersionUtils.compareVersions(serverVersion, version.substring(0, index), index) : 0;

      if (compVal == errValue) {
        String msg = "The Ambari server version " + serverVersion + " is " + errMsg + " the configured " + label +
            " Ambari version " + version + " for view " + view.getName();

        setViewStatus(view, ViewEntity.ViewStatus.ERROR, msg);
        LOG.error(msg);
        return false;
      }
    }
    return true;
  }

  // persist the given view and its instances
  @Transactional
  void persistView(ViewEntity viewDefinition, Set<ViewInstanceEntity> instanceDefinitions) throws Exception {
    // ensure that the view entity matches the db
    syncView(viewDefinition, instanceDefinitions);

    onDeploy(viewDefinition);

    // update the registry with the view instances
    for (ViewInstanceEntity instanceEntity : instanceDefinitions) {
      addInstanceDefinition(viewDefinition, instanceEntity);
      handlerList.addViewInstance(instanceEntity);
    }
  }

  // extract the view archive for the given path.
  protected static boolean extractViewArchive(String archivePath, ViewModule viewModule, boolean systemOnly)
      throws Exception {
    Injector injector = Guice.createInjector(viewModule);

    ViewExtractor      extractor      = injector.getInstance(ViewExtractor.class);
    ViewArchiveUtility archiveUtility = injector.getInstance(ViewArchiveUtility.class);
    Configuration      configuration  = injector.getInstance(Configuration.class);

    File viewDir = configuration.getViewsDir();

    String extractedArchivesPath = viewDir.getAbsolutePath() +
        File.separator + EXTRACTED_ARCHIVES_DIR;

    if (extractor.ensureExtractedArchiveDirectory(extractedArchivesPath)) {

      File archiveFile = archiveUtility.getFile(archivePath);

      ViewConfig viewConfig = archiveUtility.getViewConfigFromArchive(archiveFile);

      String commonName = viewConfig.getName();
      String version    = viewConfig.getVersion();
      String viewName   = ViewEntity.getViewName(commonName, version);

      String extractedArchiveDirPath = extractedArchivesPath + File.separator + viewName;
      File   extractedArchiveDirFile = archiveUtility.getFile(extractedArchiveDirPath);

      if (!extractedArchiveDirFile.exists()) {
        ViewEntity viewDefinition = new ViewEntity(viewConfig, configuration, extractedArchiveDirPath);

        if (!systemOnly || viewDefinition.isSystem()) {
          extractor.extractViewArchive(viewDefinition, archiveFile, extractedArchiveDirFile);
          return true;
        }
      }
    }
    return false;
  }

  // set the status of the given view.
  private void setViewStatus(ViewEntity viewDefinition, ViewEntity.ViewStatus status, String statusDetail) {
    viewDefinition.setStatus(status);
    viewDefinition.setStatusDetail(statusDetail);
  }

  // Get the view extraction thread pool
  private static synchronized ExecutorService getExecutorService(Configuration configuration) {
    if (executorService == null) {
      LinkedBlockingQueue<Runnable> queue = new LinkedBlockingQueue<Runnable>();

      ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(
          configuration.getViewExtractionThreadPoolCoreSize(),
          configuration.getViewExtractionThreadPoolMaxSize(),
          configuration.getViewExtractionThreadPoolTimeout(),
          TimeUnit.MILLISECONDS,
          queue);

      threadPoolExecutor.allowCoreThreadTimeOut(true);
      executorService = threadPoolExecutor;
    }
    return executorService;
  }

  /**
   * Factory method to create a view URL stream provider.
   *
   * @param viewContext  the view context
   *
   * @return a new view URL stream provider
   */
  protected ViewURLStreamProvider createURLStreamProvider(ViewContext viewContext) {
    ComponentSSLConfiguration sslConfiguration = ComponentSSLConfiguration.instance();
    org.apache.ambari.server.controller.internal.URLStreamProvider streamProvider =
        new org.apache.ambari.server.controller.internal.URLStreamProvider(
            configuration.getRequestConnectTimeout(),
            configuration.getRequestReadTimeout(),
            sslConfiguration.getTruststorePath(),
            sslConfiguration.getTruststorePassword(),
            sslConfiguration.getTruststoreType());
    return new ViewURLStreamProvider(viewContext, streamProvider);
  }

  /**
   * Factory method to create a view Ambari stream provider.
   *
   * @return a new view Ambari stream provider
   */
  protected ViewAmbariStreamProvider createAmbariStreamProvider() {
    ComponentSSLConfiguration sslConfiguration = ComponentSSLConfiguration.instance();
    org.apache.ambari.server.controller.internal.URLStreamProvider streamProvider =
        new org.apache.ambari.server.controller.internal.URLStreamProvider(
            configuration.getViewAmbariRequestConnectTimeout(),
            configuration.getViewAmbariRequestReadTimeout(),
            sslConfiguration.getTruststorePath(),
            sslConfiguration.getTruststorePassword(),
            sslConfiguration.getTruststoreType());
    return new ViewAmbariStreamProvider(streamProvider, ambariSessionManager, AmbariServer.getController());
  }

  /**
   * Get Remote Ambari Cluster Stream provider
   *
   * @param clusterName
   * @return
   */
  protected AmbariStreamProvider createRemoteAmbariStreamProvider(String clusterName){
    RemoteAmbariClusterEntity clusterEntity = remoteAmbariClusterDAO.findByName(clusterName);
    if(clusterEntity != null) {
      return new RemoteAmbariStreamProvider(getBaseurl(clusterEntity.getUrl()),
        clusterEntity.getUsername(),clusterEntity.getPassword(),
        configuration.getViewAmbariRequestConnectTimeout(),configuration.getViewAmbariRequestReadTimeout());
    }
    return null;
  }

  /**
   *  Get baseurl of the cluster
   *  baseurl wil be http://host:port
   *
   * @param url will be in format like http://host:port/api/v1/clusters/clusterName
   * @return baseurl
   */
  private String getBaseurl(String url){
    int index = url.indexOf(API_PREFIX);
    return url.substring(0,index);
  }




  /**
   * Module for stand alone view registry.
   */
  protected static class ViewModule extends AbstractModule {

    @Override
    protected void configure() {
      Configuration configuration = new Configuration();
      bind(Configuration.class).toInstance(configuration);
      bind(OsFamily.class).toInstance(new OsFamily(configuration));
    }
  }
}
