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

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.apache.ambari.server.api.resources.ResourceInstanceFactoryImpl;
import org.apache.ambari.server.api.resources.SubResourceDefinition;
import org.apache.ambari.server.api.resources.ViewExternalSubResourceDefinition;
import org.apache.ambari.server.api.services.ViewExternalSubResourceService;
import org.apache.ambari.server.api.services.ViewSubResourceService;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.orm.dao.ViewDAO;
import org.apache.ambari.server.orm.dao.ViewInstanceDAO;
import org.apache.ambari.server.orm.entities.ViewEntity;
import org.apache.ambari.server.orm.entities.ViewEntityEntity;
import org.apache.ambari.server.orm.entities.ViewInstanceDataEntity;
import org.apache.ambari.server.orm.entities.ViewInstanceEntity;
import org.apache.ambari.server.orm.entities.ViewParameterEntity;
import org.apache.ambari.server.orm.entities.ViewResourceEntity;
import org.apache.ambari.server.view.configuration.EntityConfig;
import org.apache.ambari.server.view.configuration.InstanceConfig;
import org.apache.ambari.server.view.configuration.ParameterConfig;
import org.apache.ambari.server.view.configuration.PersistenceConfig;
import org.apache.ambari.server.view.configuration.PropertyConfig;
import org.apache.ambari.server.view.configuration.ResourceConfig;
import org.apache.ambari.server.view.configuration.ViewConfig;
import org.apache.ambari.view.SystemException;
import org.apache.ambari.view.View;
import org.apache.ambari.view.ViewContext;
import org.apache.ambari.view.ViewDefinition;
import org.apache.ambari.view.ViewResourceHandler;
import org.apache.ambari.view.events.Event;
import org.apache.ambari.view.events.Listener;
import org.eclipse.jetty.webapp.WebAppContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.beans.IntrospectionException;
import java.io.File;
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
   * Get the instance definition for the given view nam,e and instance name.
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

                ViewEntity viewDefinition = createViewDefinition(viewConfig, configuration, cl, archivePath);

                Set<ViewInstanceEntity> instanceDefinitions = new HashSet<ViewInstanceEntity>();

                for (InstanceConfig instanceConfig : viewConfig.getInstances()) {
                  try {
                    instanceDefinitions.add(createViewInstanceDefinition(viewDefinition, instanceConfig));
                  } catch (Exception e) {
                    LOG.error("Caught exception adding view instance for view " +
                        viewDefinition.getViewName(), e);
                  }
                }
                // ensure that the view entity matches the db
                instanceDefinitions.addAll(persistView(viewDefinition));

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
        instanceDAO.create(instanceEntity);
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
        if (LOG.isDebugEnabled()) {
          LOG.debug("Updating view instance " + viewName + "/" +
              version + "/" + instanceName);
        }
        entity.setLabel(instanceEntity.getLabel());
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
   */
  public void uninstallViewInstance(ViewInstanceEntity instanceEntity) {
    ViewEntity viewEntity = getDefinition(instanceEntity.getViewName());

    if (viewEntity != null) {
      String instanceName = instanceEntity.getName();
      String viewName     = viewEntity.getCommonName();
      String version      = viewEntity.getVersion();

      if (getInstanceDefinition(viewName, version, instanceName) != null) {

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
    View view = null;
    if (viewConfig.getView() != null) {
      view = getView(viewConfig.getViewClass(cl), new ViewContextImpl(viewDefinition, this));
    }
    viewDefinition.setView(view);

    return viewDefinition;
  }

  // create a new view instance definition
  protected ViewInstanceEntity createViewInstanceDefinition(ViewEntity viewDefinition, InstanceConfig instanceConfig)
      throws ClassNotFoundException, IllegalStateException {
    ViewInstanceEntity viewInstanceDefinition =
        new ViewInstanceEntity(viewDefinition, instanceConfig);

    for (PropertyConfig propertyConfig : instanceConfig.getProperties()) {
      viewInstanceDefinition.putProperty(propertyConfig.getKey(), propertyConfig.getValue());
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

  // persist the given view
  private Set<ViewInstanceEntity> persistView(ViewEntity viewDefinition)
      throws ClassNotFoundException {

    Set<ViewInstanceEntity> instanceDefinitions = new HashSet<ViewInstanceEntity>();

    String viewName = viewDefinition.getName();

    ViewEntity viewEntity = viewDAO.findByName(viewName);

    if (viewEntity == null) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Creating View " + viewName + ".");
      }
      viewDAO.create(viewDefinition);
    } else {
      for (ViewInstanceEntity viewInstanceEntity : viewEntity.getInstances()){
        ViewInstanceEntity instanceDefinition =
            viewDefinition.getInstanceDefinition(viewInstanceEntity.getName());

        if (instanceDefinition == null) {
          viewInstanceEntity.setViewEntity(viewDefinition);
          bindViewInstance(viewDefinition, viewInstanceEntity);
          instanceDefinitions.add(viewInstanceEntity);
        } else {
          // apply overrides to the in-memory view instance entities
          instanceDefinition.setLabel(viewInstanceEntity.getLabel());
          instanceDefinition.setData(viewInstanceEntity.getData());
          instanceDefinition.setProperties(viewInstanceEntity.getProperties());
          instanceDefinition.setEntities(viewInstanceEntity.getEntities());
        }
      }
    }
    return instanceDefinitions;
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

  /**
   * Static initialization of DAO.
   *
   * @param viewDAO      view data access object
   * @param instanceDAO  view instance data access object
   */
  public static void init(ViewDAO viewDAO, ViewInstanceDAO instanceDAO) {
    setViewDAO(viewDAO);
    setInstanceDAO(instanceDAO);
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
