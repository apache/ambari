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

import java.beans.IntrospectionException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.apache.ambari.server.api.resources.ResourceInstanceFactoryImpl;
import org.apache.ambari.server.api.resources.SubResourceDefinition;
import org.apache.ambari.server.api.resources.ViewExternalSubResourceDefinition;
import org.apache.ambari.server.api.services.ViewExternalSubResourceService;
import org.apache.ambari.server.api.services.ViewSubResourceService;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.orm.dao.MemberDAO;
import org.apache.ambari.server.orm.dao.PrivilegeDAO;
import org.apache.ambari.server.orm.dao.ResourceDAO;
import org.apache.ambari.server.orm.dao.ResourceTypeDAO;
import org.apache.ambari.server.orm.dao.UserDAO;
import org.apache.ambari.server.orm.dao.ViewDAO;
import org.apache.ambari.server.orm.dao.ViewInstanceDAO;
import org.apache.ambari.server.orm.entities.GroupEntity;
import org.apache.ambari.server.orm.entities.MemberEntity;
import org.apache.ambari.server.orm.entities.PermissionEntity;
import org.apache.ambari.server.orm.entities.PrivilegeEntity;
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
import org.apache.ambari.server.security.authorization.AmbariGrantedAuthority;
import org.apache.ambari.server.view.configuration.EntityConfig;
import org.apache.ambari.server.view.configuration.InstanceConfig;
import org.apache.ambari.server.view.configuration.ParameterConfig;
import org.apache.ambari.server.view.configuration.PermissionConfig;
import org.apache.ambari.server.view.configuration.PersistenceConfig;
import org.apache.ambari.server.view.configuration.PropertyConfig;
import org.apache.ambari.server.view.configuration.ResourceConfig;
import org.apache.ambari.server.view.configuration.ViewConfig;
import org.apache.ambari.view.MaskException;
import org.apache.ambari.view.Masker;
import org.apache.ambari.view.SystemException;
import org.apache.ambari.view.View;
import org.apache.ambari.view.ViewContext;
import org.apache.ambari.view.ViewDefinition;
import org.apache.ambari.view.ViewResourceHandler;
import org.apache.ambari.view.events.Event;
import org.apache.ambari.view.events.Listener;
import org.apache.commons.lang.StringUtils;
import org.eclipse.jetty.webapp.WebAppContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;

/**
 * Registry for view and view instance definitions.
 */
public class ViewRegistry {

  /**
   * Constants
   */
  private static final String VIEW_XML = "view.xml";
  private static final String ARCHIVE_CLASSES_DIR = "WEB-INF/classes";
  private static final String ARCHIVE_LIB_DIR = "WEB-INF/lib";
  private static final String EXTRACTED_ARCHIVES_DIR = "work";

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
      new HashMap<String, Set<SubResourceDefinition>>();

  /**
   * Mapping of view names to registered listeners.
   */
  private final Map<String, List<Listener>> listeners =
      new HashMap<String, List<Listener>>();

  /**
   * Helper class.
   */
  private ViewRegistryHelper helper = new ViewRegistryHelper();

  /**
   * The singleton view registry instance.
   */
  private static final ViewRegistry singleton = new ViewRegistry();

  /**
   * The logger.
   */
  protected final static Logger LOG = LoggerFactory.getLogger(ViewRegistry.class);

  /**
   * View data access object.
   */
  private static ViewDAO viewDAO;

  /**
   * View instance data access object.
   */
  private static ViewInstanceDAO instanceDAO;

  /**
   * User data access object.
   */
  private static UserDAO userDAO;

  /**
   * Group member data access object.
   */
  private static MemberDAO memberDAO;

  /**
   * Privilege data access object.
   */
  private static PrivilegeDAO privilegeDAO;

  /**
   * Helper with security related utilities.
   */
  private static SecurityHelper securityHelper;

  /**
   * Resource data access object.
   */
  private static ResourceDAO resourceDAO;

  /**
   * Resource type data access object.
   */
  private static ResourceTypeDAO resourceTypeDAO;

  // ----- Constructors ------------------------------------------------------

  /**
   * Hide the constructor for this singleton.
   */
  private ViewRegistry() {
  }


  // ----- ViewRegistry ------------------------------------------------------

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
      if (viewEntity.getResourceType().equals(resourceTypeEntity)) {
        return viewEntity;
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
    View view = definition.getView();
    if (view != null) {
      view.onDeploy(definition);
    }
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
  public synchronized Set<SubResourceDefinition> getSubResourceDefinitions(
      String viewName, String version) {

    viewName = ViewEntity.getViewName(viewName, version);

    Set<SubResourceDefinition> subResourceDefinitions =
        subResourceDefinitionsMap.get(viewName);

    if (subResourceDefinitions == null) {
      subResourceDefinitions = new HashSet<SubResourceDefinition>();
      ViewEntity definition = getDefinition(viewName);
      if (definition != null) {
        for (Resource.Type type : definition.getViewResourceTypes()) {
          subResourceDefinitions.add(new SubResourceDefinition(type));
        }
      }
      subResourceDefinitionsMap.put(viewName, subResourceDefinitions);
    }
    return subResourceDefinitions;
  }

  /**
   * Read the view archives.
   *
   * @param configuration  Ambari configuration
   *
   * @return the set of view instance definitions read from the archives
   *
   * @throws SystemException if the view archives can not be successfully read
   */
  public Set<ViewInstanceEntity> readViewArchives(Configuration configuration)
      throws SystemException {

    try {
      File viewDir = configuration.getViewsDir();

      Set<ViewInstanceEntity> allInstanceDefinitions = new HashSet<ViewInstanceEntity>();

      String extractedArchivesPath = viewDir.getAbsolutePath() +
          File.separator + EXTRACTED_ARCHIVES_DIR;

      if (ensureExtractedArchiveDirectory(extractedArchivesPath)) {
        File[] files = viewDir.listFiles();

        if (files != null) {
          for (File archiveFile : files) {
            if (!archiveFile.isDirectory()) {
              try {
                ViewConfig viewConfig = helper.getViewConfigFromArchive(archiveFile);

                String viewName    = ViewEntity.getViewName(viewConfig.getName(), viewConfig.getVersion());
                String archivePath = extractedArchivesPath + File.separator + viewName;

                // extract the archive and get the class loader
                ClassLoader cl = extractViewArchive(archiveFile, helper.getFile(archivePath));

                viewConfig = helper.getViewConfigFromExtractedArchive(archivePath);

                ViewEntity viewDefinition = createViewDefinition(viewConfig, configuration, cl, archivePath);

                Set<ViewInstanceEntity> instanceDefinitions = new HashSet<ViewInstanceEntity>();

                for (InstanceConfig instanceConfig : viewConfig.getInstances()) {
                  try {
                    ViewInstanceEntity instanceEntity = createViewInstanceDefinition(viewConfig, viewDefinition, instanceConfig);
                    instanceEntity.setXmlDriven(true);
                    instanceDefinitions.add(instanceEntity);
                  } catch (Exception e) {
                    LOG.error("Caught exception adding view instance for view " +
                        viewDefinition.getViewName(), e);
                  }
                }
                // ensure that the view entity matches the db
                syncView(viewDefinition, instanceDefinitions);

                // update the registry with the view
                addDefinition(viewDefinition);

                // update the registry with the view instances
                for (ViewInstanceEntity instanceEntity : instanceDefinitions) {
                  addInstanceDefinition(viewDefinition, instanceEntity);
                }

                allInstanceDefinitions.addAll(instanceDefinitions);
              } catch (Exception e) {
                LOG.error("Caught exception loading view from " + archiveFile.getAbsolutePath(), e);
              }
            }
          }
          removeUndeployedViews();
        }
      }
      return allInstanceDefinitions;
    } catch (Exception e) {
      throw new SystemException("Caught exception reading view archives.", e);
    }
  }

  /**
   * Install the given view instance with its associated view.
   *
   * @param instanceEntity  the view instance entity
   *
   * @throws IllegalStateException     if the given instance is not in a valid state
   * @throws IllegalArgumentException  if the view associated with the given instance
   *                                   does not exist
   */
  public void installViewInstance(ViewInstanceEntity instanceEntity)
      throws IllegalStateException, IllegalArgumentException {
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
        instanceEntity.validate(viewEntity);

        ResourceTypeEntity resourceTypeEntity = resourceTypeDAO.findByName(ViewEntity.getViewName(viewName, version));
        // create an admin resource to represent this view instance
        ResourceEntity resourceEntity = new ResourceEntity();
        resourceEntity.setResourceType(resourceTypeEntity);
        resourceDAO.create(resourceEntity);

        instanceEntity.setResource(resourceEntity);

        instanceDAO.merge(instanceEntity);

        ViewInstanceEntity persistedInstance = instanceDAO.findByName(ViewEntity.getViewName(viewName, version), instanceName);
        if (persistedInstance == null) {
          String message = "Instance  " + instanceEntity.getViewName() + " can not be found.";

          LOG.error(message);
          throw new IllegalStateException(message);
        }
        instanceEntity.setViewInstanceId(persistedInstance.getViewInstanceId());
        instanceEntity.setResource(persistedInstance.getResource());

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
   * @throws IllegalStateException if the given instance is not in a valid state
   */
  public void updateViewInstance(ViewInstanceEntity instanceEntity)
      throws IllegalStateException {
    ViewEntity viewEntity = getDefinition(instanceEntity.getViewName());

    if (viewEntity != null) {
      String instanceName = instanceEntity.getName();
      String viewName     = viewEntity.getCommonName();
      String version      = viewEntity.getVersion();

      ViewInstanceEntity entity = getInstanceDefinition(viewName, version, instanceName);

      if (entity != null) {
        if (entity.isXmlDriven()) {
          throw new IllegalStateException("View instances defined via xml can't be updated through api requests");
        }
        if (LOG.isDebugEnabled()) {
          LOG.debug("Updating view instance " + viewName + "/" +
              version + "/" + instanceName);
        }
        entity.setLabel(instanceEntity.getLabel());
        entity.setDescription(instanceEntity.getDescription());
        entity.setVisible(instanceEntity.isVisible());
        entity.setProperties(instanceEntity.getProperties());
        entity.setData(instanceEntity.getData());

        instanceEntity.validate(viewEntity);
        instanceDAO.merge(entity);
      }
    }
  }

  /**
   * Remove the data entry keyed by the given key from the given instance entity.
   *
   * @param instanceEntity  the instance entity
   * @param key             the data key
   */
  public void removeInstanceData(ViewInstanceEntity instanceEntity, String key) {
    ViewInstanceDataEntity dataEntity = instanceEntity.getInstanceData(key);
    if (dataEntity != null) {
      instanceDAO.removeData(dataEntity);
    }
    instanceEntity.removeInstanceData(key);
    instanceDAO.merge(instanceEntity);
  }

  /**
   * Uninstall a view instance for the view with the given view name.
   *
   * @param instanceEntity  the view instance entity
   * @throws IllegalStateException if the given instance is not in a valid state
   */
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
        instanceDAO.remove(instanceEntity);
        viewEntity.removeInstanceDefinition(instanceName);
        removeInstanceDefinition(viewEntity, instanceName);
      }
    }
  }

  /**
   * Get a WebAppContext for the given view instance.
   *
   * @param viewInstanceDefinition  the view instance definition
   *
   * @return a web app context
   *
   * @throws SystemException if an application context can not be obtained for the given view instance
   */
  public WebAppContext getWebAppContext(ViewInstanceEntity viewInstanceDefinition)
      throws SystemException{
    try {
      ViewEntity viewDefinition = viewInstanceDefinition.getViewEntity();

      WebAppContext context = new WebAppContext(viewDefinition.getArchive(), viewInstanceDefinition.getContextPath());
      context.setClassLoader(viewDefinition.getClassLoader());
      context.setAttribute(ViewContext.CONTEXT_ATTRIBUTE, new ViewContextImpl(viewInstanceDefinition, this));
      return context;
    } catch (Exception e) {
      throw new SystemException("Can't get application context for view " +
          viewInstanceDefinition.getViewEntity().getCommonName() + ".", e);
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

    List<Listener> listeners = this.listeners.get(name);

    if (listeners == null) {
      listeners = new LinkedList<Listener>();
      this.listeners.put(name, listeners);
    }

    listeners.add(listener);
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

    UserEntity userEntity = userDAO.findLocalUserByName(userName);

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
   * Determine whether or not the given view definition resource should be included
   * based on the permissions granted to the current user.
   *
   * @param definitionEntity  the view definition entity
   * @param readOnly        indicate whether or not this is for a read only operation
   *
   * @return true if the view instance should be included based on the permissions of the current user
   */
  public boolean includeDefinition(ViewEntity definitionEntity) {

    ViewRegistry viewRegistry = ViewRegistry.getInstance();

    for (GrantedAuthority grantedAuthority : securityHelper.getCurrentAuthorities()) {
      if (grantedAuthority instanceof AmbariGrantedAuthority) {

        AmbariGrantedAuthority authority = (AmbariGrantedAuthority) grantedAuthority;
        PrivilegeEntity privilegeEntity = authority.getPrivilegeEntity();
        Integer permissionId = privilegeEntity.getPermission().getId();

        // admin has full access
        if (permissionId.equals(PermissionEntity.AMBARI_ADMIN_PERMISSION)) {
          return true;
        }
      }
    }

    boolean allowed = false;

    for (ViewInstanceEntity instanceEntity: definitionEntity.getInstances()) {
      allowed |= viewRegistry.checkPermission(instanceEntity, true);
    }

    return allowed;
  }


  // ----- helper methods ----------------------------------------------------

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
   * Set the helper.
   *
   * @param helper  the helper
   */
  protected void setHelper(ViewRegistryHelper helper) {
    this.helper = helper;
  }

  // get a view entity for the given internal view name
  private ViewEntity getDefinition(String viewName) {
    return viewDefinitions.get(viewName);
  }

  // create a new view definition
  protected ViewEntity createViewDefinition(ViewConfig viewConfig, Configuration ambariConfig,
                                          ClassLoader cl, String archivePath)
      throws ClassNotFoundException, IntrospectionException {

    ViewEntity viewDefinition = new ViewEntity(viewConfig, ambariConfig, cl, archivePath);

    List<ParameterConfig> parameterConfigurations = viewConfig.getParameters();

    Collection<ViewParameterEntity> parameters = new HashSet<ViewParameterEntity>();
    for (ParameterConfig parameterConfiguration : parameterConfigurations) {
      ViewParameterEntity viewParameterEntity =  new ViewParameterEntity();

      viewParameterEntity.setViewName(viewDefinition.getName());
      viewParameterEntity.setName(parameterConfiguration.getName());
      viewParameterEntity.setDescription(parameterConfiguration.getDescription());
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
    viewDefinition.addResourceProvider(externalResourceType, viewExternalSubResourceProvider );

    ResourceInstanceFactoryImpl.addResourceDefinition(externalResourceType,
        new ViewExternalSubResourceDefinition(externalResourceType));

    Collection<ViewResourceEntity> resources = new HashSet<ViewResourceEntity>();
    for (ResourceConfig resourceConfiguration : resourceConfigurations) {
      ViewResourceEntity viewResourceEntity = new ViewResourceEntity();

      viewResourceEntity.setViewName(viewDefinition.getName());
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

        viewDefinition.addResourceProvider(type, new ViewSubResourceProvider(type, clazz, idProperty, viewDefinition));

        resources.add(viewResourceEntity);
      }
      viewDefinition.setResources(resources);
    }

    ResourceTypeEntity resourceTypeEntity = new ResourceTypeEntity();
    resourceTypeEntity.setName(viewDefinition.getName());

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
    viewDefinition.setMask(viewConfig.getMasker());

    return viewDefinition;
  }

  // create a new view instance definition
  protected ViewInstanceEntity createViewInstanceDefinition(ViewConfig viewConfig, ViewEntity viewDefinition, InstanceConfig instanceConfig)
      throws ClassNotFoundException, IllegalStateException, MaskException {
    ViewInstanceEntity viewInstanceDefinition =
        new ViewInstanceEntity(viewDefinition, instanceConfig);

    Masker masker = getMasker(viewConfig.getMaskerClass(viewDefinition.getClassLoader()));
    for (PropertyConfig propertyConfig : instanceConfig.getProperties()) {
      ParameterConfig parameterConfig = null;
      for (ParameterConfig paramConfig : viewConfig.getParameters()) {
        if (StringUtils.equals(paramConfig.getName(), propertyConfig.getKey())) {
          parameterConfig = paramConfig;
          break;
        }
      }
      if (parameterConfig != null && parameterConfig.isMasked()) {
        viewInstanceDefinition.putProperty(propertyConfig.getKey(),
            masker.mask(propertyConfig.getValue()));
      } else {
        viewInstanceDefinition.putProperty(propertyConfig.getKey(), propertyConfig.getValue());
      }
    }
    viewInstanceDefinition.validate(viewDefinition);

    bindViewInstance(viewDefinition, viewInstanceDefinition);
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

    setPersistenceEntities(viewInstanceDefinition);

    viewDefinition.addInstanceDefinition(viewInstanceDefinition);
  }

  // Set the entities defined in the view persistence element for the given view instance
  private static void setPersistenceEntities(ViewInstanceEntity viewInstanceDefinition) {
    ViewEntity        viewDefinition    = viewInstanceDefinition.getViewEntity();
    ViewConfig        viewConfig        = viewDefinition.getConfiguration();

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
   * @param view view config from xml
   * @param instanceDefinitions view instances from xml
   * @throws Exception
   */
  private void syncView(ViewEntity view,
                        Set<ViewInstanceEntity> instanceDefinitions)
      throws Exception {
    String viewName = view.getName();

    // get or create an admin resource type to represent this view
    ResourceTypeEntity resourceTypeEntity = resourceTypeDAO.findByName(viewName);
    if (resourceTypeEntity == null) {
      resourceTypeEntity = new ResourceTypeEntity();
      resourceTypeEntity.setName(view.getName());
      resourceTypeDAO.create(resourceTypeEntity);
    }
    view.setResourceType(resourceTypeEntity);

    ViewEntity persistedView = viewDAO.findByName(viewName);

    // if the view is not yet persisted ...
    if (persistedView == null) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Creating View " + viewName + ".");
      }

      for( ViewInstanceEntity instance : view.getInstances()) {

        // create an admin resource to represent this view instance
        ResourceEntity resourceEntity = new ResourceEntity();
        resourceEntity.setResourceType(resourceTypeEntity);
        resourceDAO.create(resourceEntity);

        instance.setResource(resourceEntity);
      }
      // ... merge it
      viewDAO.merge(view);

      persistedView = viewDAO.findByName(viewName);
      if (persistedView == null) {
        String message = "View  " + viewName + " can not be found.";

        LOG.error(message);
        throw new IllegalStateException(message);
      }
    }

    Map<String, ViewInstanceEntity> xmlInstanceEntityMap = new HashMap<String, ViewInstanceEntity>();
    for( ViewInstanceEntity instance : view.getInstances()) {
      xmlInstanceEntityMap.put(instance.getName(), instance);
    }

    view.setResourceType(persistedView.getResourceType());
    view.setPermissions(persistedView.getPermissions());

    // make sure that each instance of the view in the db is reflected in the given view
    for (ViewInstanceEntity persistedInstance : persistedView.getInstances()){
      String instanceName = persistedInstance.getName();

      ViewInstanceEntity instance =
          view.getInstanceDefinition(instanceName);

      if (persistedInstance.isXmlDriven() && !xmlInstanceEntityMap.containsKey(instanceName)) {
        instanceDAO.remove(persistedInstance);
        xmlInstanceEntityMap.remove(instanceName);
        continue;
      }
      xmlInstanceEntityMap.remove(instanceName);

      // if the persisted instance is not in the registry ...
      if (instance == null) {
        // ... create and add it
        instance = new ViewInstanceEntity(view, instanceName);
        bindViewInstance(view, instance);
        instanceDefinitions.add(instance);
      }
      instance.setViewInstanceId(persistedInstance.getViewInstanceId());

      if (instance.isXmlDriven()) {
        // override db data with data from {@InstanceConfig}
        persistedInstance.setLabel(instance.getLabel());
        persistedInstance.setDescription(instance.getDescription());
        persistedInstance.setVisible(instance.isVisible());
        persistedInstance.setIcon(instance.getIcon());
        persistedInstance.setIcon64(instance.getIcon64());
        persistedInstance.setProperties(instance.getProperties());

        instanceDAO.merge(persistedInstance);
      } else {
        // apply the persisted overrides to the in-memory instance
        view.removeInstanceDefinition(instanceName);
        view.addInstanceDefinition(persistedInstance);
      }

      instance.setResource(persistedInstance.getResource());
    }

    // these instances appear in the archive but not present in the db... add
    // them to db and registry
    for (ViewInstanceEntity instance : xmlInstanceEntityMap.values()) {
      // create an admin resource to represent this view instance
      ResourceEntity resourceEntity = new ResourceEntity();
      resourceEntity.setResourceType(resourceTypeEntity);
      resourceDAO.create(resourceEntity);
      instance.setResource(resourceEntity);

      instanceDAO.merge(instance);
      bindViewInstance(view, instance);
      instanceDefinitions.add(instance);
    }

  }

  // ensure that the extracted view archive directory exists
  private boolean ensureExtractedArchiveDirectory(String extractedArchivesPath) {
    File extractedArchiveDir = helper.getFile(extractedArchivesPath);

    if (!extractedArchiveDir.exists()) {
      if (!extractedArchiveDir.mkdir()) {
        LOG.error("Could not create extracted view archive directory " +
            extractedArchivesPath + ".");
        return false;
      }
    }
    return true;
  }

  // extract the given view archive to the given archive directory
  private ClassLoader extractViewArchive(File viewArchive, File archiveDir)
      throws IOException {

    // Skip if the archive has already been extracted
    if (!archiveDir.exists()) {

      String archivePath = archiveDir.getAbsolutePath();

      LOG.info("Creating archive folder " + archivePath + ".");

      if (archiveDir.mkdir()) {
        JarFile     viewJarFile = helper.getJarFile(viewArchive);
        Enumeration enumeration = viewJarFile.entries();

        LOG.info("Extracting files from " + viewArchive.getName() + ":");

        while (enumeration.hasMoreElements()) {
          JarEntry jarEntry  = (JarEntry) enumeration.nextElement();
          String   entryPath = archivePath + File.separator + jarEntry.getName();

          LOG.info("    " + entryPath);

          File entryFile = helper.getFile(entryPath);

          if (jarEntry.isDirectory()) {
            if (!entryFile.mkdir()) {
              LOG.error("Could not create archive entry directory " + entryPath + ".");
            }
          } else {
            InputStream is = viewJarFile.getInputStream(jarEntry);
            try {
              FileOutputStream fos = helper.getFileOutputStream(entryFile);
              try {
                while (is.available() > 0) {
                  fos.write(is.read());
                }
              } finally {
                fos.close();
              }
            } finally {
              is.close();
            }
          }
        }
      } else {
        LOG.error("Could not create archive directory " + archivePath + ".");
      }
    }
    return getArchiveClassLoader(archiveDir);
  }

  // get a class loader for the given archive directory
  private ClassLoader getArchiveClassLoader(File archiveDir)
      throws MalformedURLException {

    String    archivePath = archiveDir.getAbsolutePath();
    List<URL> urlList     = new LinkedList<URL>();

    // include the classes directory
    String classesPath = archivePath + File.separator + ARCHIVE_CLASSES_DIR;
    File   classesDir  = helper.getFile(classesPath);
    if (classesDir.exists()) {
      urlList.add(classesDir.toURI().toURL());
    }

    // include any libraries in the lib directory
    String libPath = archivePath + File.separator + ARCHIVE_LIB_DIR;
    File   libDir  = helper.getFile(libPath);
    if (libDir.exists()) {
      File[] files = libDir.listFiles();
      if (files != null) {
        for (final File fileEntry : files) {
          if (!fileEntry.isDirectory()) {
            urlList.add(fileEntry.toURI().toURL());
          }
        }
      }
    }

    // include the archive directory
    urlList.add(archiveDir.toURI().toURL());

    return URLClassLoader.newInstance(urlList.toArray(new URL[urlList.size()]));
  }

  // notify the view identified by the given view name of the given event
  private void fireEvent(Event event, String viewName) {
    List<Listener> listeners = this.listeners.get(viewName);

    if (listeners != null) {
      for (Listener listener : listeners) {
        listener.notify(event);
      }
    }
  }

  // check that the current user is authorized to access the given view instance resource
  private boolean checkAuthorization(ResourceEntity resourceEntity) {
    for (GrantedAuthority grantedAuthority : securityHelper.getCurrentAuthorities()) {
      if (grantedAuthority instanceof AmbariGrantedAuthority) {

        AmbariGrantedAuthority authority       = (AmbariGrantedAuthority) grantedAuthority;
        PrivilegeEntity privilegeEntity = authority.getPrivilegeEntity();
        Integer                permissionId    = privilegeEntity.getPermission().getId();

        // admin has full access
        if (permissionId.equals(PermissionEntity.AMBARI_ADMIN_PERMISSION)) {
          return true;
        }
        if (resourceEntity != null) {
          // VIEW.USE for the given view instance resource.
          if (privilegeEntity.getResource().equals(resourceEntity)) {
            if (permissionId.equals(PermissionEntity.VIEW_USE_PERMISSION)) {
              return true;
            }
          }
        }
      }
    }
    // TODO : should we log this?
    return false;
  }

  /**
   * Static initialization of DAO.
   *
   * @param viewDAO         view data access object
   * @param instanceDAO     view instance data access object
   * @param userDAO         user data access object
   * @param memberDAO       group member data access object
   * @param privilegeDAO    the privilege data access object
   * @param securityHelper  the security helper
   */
  public static void init(ViewDAO viewDAO, ViewInstanceDAO instanceDAO,
                          UserDAO userDAO, MemberDAO memberDAO, PrivilegeDAO privilegeDAO,
                          SecurityHelper securityHelper, ResourceDAO resourceDAO,
                          ResourceTypeDAO resourceTypeDAO) {
    setViewDAO(viewDAO);
    setInstanceDAO(instanceDAO);
    setUserDAO(userDAO);
    setMemberDAO(memberDAO);
    setPrivilegeDAO(privilegeDAO);
    setSecurityHelper(securityHelper);
    setResourceDAO(resourceDAO);
    setResourceTypeDAO(resourceTypeDAO);
  }

  /**
   * Set the view DAO.
   *
   * @param viewDAO  the view DAO
   */
  protected static void setViewDAO(ViewDAO viewDAO) {
    ViewRegistry.viewDAO = viewDAO;
  }

  /**
   * Set the instance DAO.
   *
   * @param instanceDAO  the instance DAO
   */
  protected static void setInstanceDAO(ViewInstanceDAO instanceDAO) {
    ViewRegistry.instanceDAO = instanceDAO;
  }

  /**
   * Set the user DAO.
   *
   * @param userDAO  the user DAO
   */
  protected static void setUserDAO(UserDAO userDAO) {
    ViewRegistry.userDAO = userDAO;
  }

  /**
   * Set the group member DAO.
   *
   * @param memberDAO  the group member DAO
   */
  protected static void setMemberDAO(MemberDAO memberDAO) {
    ViewRegistry.memberDAO = memberDAO;
  }

  /**
   * Set the privilege DAO.
   *
   * @param privilegeDAO  the privilege DAO
   */
  protected static void setPrivilegeDAO(PrivilegeDAO privilegeDAO) {
    ViewRegistry.privilegeDAO = privilegeDAO;
  }

  /**
   * Set the security helper.
   *
   * @param securityHelper  the security helper
   */
  protected static void setSecurityHelper(SecurityHelper securityHelper) {
    ViewRegistry.securityHelper = securityHelper;
  }

  /**
   * Set the resource DAO.
   *
   * @param resourceDAO the resource DAO
   */
  protected static void setResourceDAO(ResourceDAO resourceDAO) {
    ViewRegistry.resourceDAO = resourceDAO;
  }

  /**
   * Set the resource type DAO.
   *
   * @param resourceTypeDAO the resource type DAO.
   */
  protected static void setResourceTypeDAO(ResourceTypeDAO resourceTypeDAO) {
    ViewRegistry.resourceTypeDAO = resourceTypeDAO;
  }


  // ----- inner class : ViewRegistryHelper ----------------------------------

  /**
   * Registry helper class.
   */
  protected static class ViewRegistryHelper {

    /**
     * Get the view configuration from the given archive file.
     *
     * @param archiveFile  the archive file
     *
     * @return the associated view configuration
     */
    public ViewConfig getViewConfigFromArchive(File archiveFile)
        throws MalformedURLException, JAXBException {
      ClassLoader cl = URLClassLoader.newInstance(new URL[]{archiveFile.toURI().toURL()});

      InputStream  configStream     = cl.getResourceAsStream(VIEW_XML);
      JAXBContext  jaxbContext      = JAXBContext.newInstance(ViewConfig.class);
      Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();

      return (ViewConfig) jaxbUnmarshaller.unmarshal(configStream);
    }

    /**
     * Get the view configuration from the extracted archive file.
     *
     * @param archivePath path to extracted archive
     *
     * @return the associated view configuration
     *
     * @throws JAXBException if xml is malformed
     * @throws FileNotFoundException if xml was not found
     */
    public ViewConfig getViewConfigFromExtractedArchive(String archivePath)
        throws JAXBException, FileNotFoundException {

      InputStream configStream      = new FileInputStream(new File(archivePath + File.separator + VIEW_XML));
      JAXBContext  jaxbContext      = JAXBContext.newInstance(ViewConfig.class);
      Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();

      return (ViewConfig) jaxbUnmarshaller.unmarshal(configStream);
    }

    /**
     * Get a new file instance for the given path.
     *
     * @param path  the path
     *
     * @return a new file instance
     */
    public File getFile(String path) {
      return new File(path);
    }

    /**
     * Get a new file output stream for the given file.
     *
     * @param file  the file
     *
     * @return a new file output stream
     */
    public FileOutputStream getFileOutputStream(File file) throws FileNotFoundException {
      return new FileOutputStream(file);
    }

    /**
     * Get a new jar file instance from the given file.
     *
     * @param file  the file
     *
     * @return a new jar file instance
     */
    public JarFile getJarFile(File file) throws IOException {
      return new JarFile(file);
    }
  }
}
