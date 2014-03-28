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
import com.google.inject.Inject;
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
import org.apache.ambari.server.orm.entities.ViewInstanceEntity;
import org.apache.ambari.server.orm.entities.ViewParameterEntity;
import org.apache.ambari.server.orm.entities.ViewResourceEntity;
import org.apache.ambari.server.view.configuration.InstanceConfig;
import org.apache.ambari.server.view.configuration.ParameterConfig;
import org.apache.ambari.server.view.configuration.PropertyConfig;
import org.apache.ambari.server.view.configuration.ResourceConfig;
import org.apache.ambari.server.view.configuration.ViewConfig;
import org.apache.ambari.view.ViewContext;
import org.apache.ambari.view.ViewResourceHandler;
import org.eclipse.jetty.webapp.WebAppContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import java.beans.IntrospectionException;
import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Registry for view and view instance definitions.
 */
public class ViewRegistry {

  /**
   * Constants
   */
  private static final String VIEW_XML = "view.xml";

  /**
   * Mapping of view names to view definitions.
   */
  private Map<String, ViewEntity> viewDefinitions = new HashMap<String, ViewEntity>();

  /**
   * Mapping of view instances to view definition and instance name.
   */
  private Map<ViewEntity, Map<String, ViewInstanceEntity>> viewInstanceDefinitions = new HashMap<ViewEntity, Map<String, ViewInstanceEntity>>();

  /**
   * Mapping of view names to sub-resources.
   */
  private final Map<String, Set<SubResourceDefinition>> subResourceDefinitionsMap = new HashMap<String, Set<SubResourceDefinition>>();

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
   * Static initialization of DAO.
   *
   * @param vDAO  view data access object
   * @param iDAO  view instance data access object
   */
  @Inject
  public static void init(ViewDAO vDAO, ViewInstanceDAO iDAO) {
    viewDAO     = vDAO;
    instanceDAO = iDAO;
  }


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
   *
   * @return the view definition for the given name
   */
  public ViewEntity getDefinition(String viewName) {
    return viewDefinitions.get(viewName);
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
    return definition == null ? null : viewInstanceDefinitions.get(definition).values();
  }

  /**
   * Get the instance definition for the given view nam,e and instance name.
   *
   * @param viewName      the view name
   * @param instanceName  the instance name
   *
   * @return the view instance definition for the given view and instance name
   */
  public ViewInstanceEntity getInstanceDefinition(String viewName, String instanceName) {
    Map<String, ViewInstanceEntity> viewInstanceDefinitionMap =
        viewInstanceDefinitions.get(getDefinition(viewName));

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
      instanceDefinitions.remove(instanceName);
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
   *
   * @return the set of sub-resource definitions
   */
  public synchronized Set<SubResourceDefinition> getSubResourceDefinitions(String viewName) {

    Set<SubResourceDefinition> subResourceDefinitions = subResourceDefinitionsMap.get(viewName);

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
   */
  public Set<ViewInstanceEntity> readViewArchives(Configuration configuration) {

    File   viewDir = configuration.getViewsDir();
    File[] files   = viewDir.listFiles();

    Set<ViewInstanceEntity> instanceDefinitions = new HashSet<ViewInstanceEntity>();

    if (files != null) {
      for (final File fileEntry : files) {
        if (!fileEntry.isDirectory()) {
          try {
            ClassLoader cl = URLClassLoader.newInstance(new URL[]{fileEntry.toURI().toURL()});

            InputStream    configStream     = cl.getResourceAsStream(VIEW_XML);
            JAXBContext    jaxbContext      = JAXBContext.newInstance(ViewConfig.class);
            Unmarshaller   jaxbUnmarshaller = jaxbContext.createUnmarshaller();
            ViewConfig     viewConfig       = (ViewConfig) jaxbUnmarshaller.unmarshal(configStream);
            ViewEntity viewDefinition       = installView(viewConfig, configuration, cl, fileEntry.getAbsolutePath());

            for (InstanceConfig instanceConfig : viewConfig.getInstances()) {
              ViewInstanceEntity viewInstanceDefinition = new ViewInstanceEntity(viewDefinition, instanceConfig);

              for (PropertyConfig propertyConfig : instanceConfig.getProperties()) {
                viewInstanceDefinition.putProperty(propertyConfig.getKey(), propertyConfig.getValue());
              }

              _installViewInstance(viewDefinition, viewInstanceDefinition);
              instanceDefinitions.add(viewInstanceDefinition);
            }
          } catch (Exception e) {
            LOG.error("Caught exception loading view from " + fileEntry.getAbsolutePath(), e);
          }
        }
      }
      try {

        instanceDefinitions.addAll(persistViews());
      } catch (ClassNotFoundException e) {
        LOG.error("Caught exception persisting views.", e);
      }
    }
    return instanceDefinitions;
  }

  /**
   * Install a view instance for the view with the given view name.
   *
   * @param instanceEntity  the view instance entity
   */
  public void installViewInstance(ViewInstanceEntity instanceEntity){
    String viewName       = instanceEntity.getViewName();
    ViewEntity viewEntity = getDefinition(viewName);

    if (viewEntity != null) {
      String instanceName = instanceEntity.getName();

      if (getInstanceDefinition(viewName, instanceName) == null) {
        if (LOG.isDebugEnabled()) {
          LOG.debug("Creating view instance " + viewName + "/" + instanceName);
        }
        instanceDAO.create(instanceEntity);
        try {
          _installViewInstance(viewEntity, instanceEntity);
        } catch (ClassNotFoundException e) {
          LOG.error("Caught exception installing view instance.", e);
        }
      }
    }
  }

  /**
   * Update a view instance for the view with the given view name.
   *
   * @param instanceEntity  the view instance entity
   */
  public void updateViewInstance(ViewInstanceEntity instanceEntity) {
    String       viewName   = instanceEntity.getViewName();
    ViewEntity   viewEntity = getDefinition(viewName);

    if (viewEntity != null) {
      String instanceName = instanceEntity.getName();
      ViewInstanceEntity entity = getInstanceDefinition(viewName, instanceName);
      if (entity != null) {
        if (LOG.isDebugEnabled()) {
          LOG.debug("Updating view instance " + viewName + "/" + instanceName);
        }

        entity.setProperties(instanceEntity.getProperties());
        entity.setData(instanceEntity.getData());

        instanceDAO.merge(entity);
      }
    }
  }

  /**
   * Uninstall a view instance for the view with the given view name.
   *
   * @param instanceEntity  the view instance entity
   */
  public void uninstallViewInstance(ViewInstanceEntity instanceEntity) {

    String       viewName   = instanceEntity.getViewName();
    ViewEntity   viewEntity = getDefinition(viewName);

    if (viewEntity != null) {
      String instanceName = instanceEntity.getName();
      if (getInstanceDefinition(viewName, instanceName) != null) {

        if (LOG.isDebugEnabled()) {
          LOG.debug("Deleting view instance " + viewName + "/" + instanceName);
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
   */
  public WebAppContext getWebAppContext(ViewInstanceEntity viewInstanceDefinition) {
    ViewEntity viewDefinition = viewInstanceDefinition.getViewEntity();

    WebAppContext context = new WebAppContext(viewDefinition.getArchive(), viewInstanceDefinition.getContextPath());
    context.setClassLoader(viewDefinition.getClassLoader());
    context.setAttribute(ViewContext.CONTEXT_ATTRIBUTE, new ViewContextImpl(viewInstanceDefinition, this));
    return context;
  }


  // ----- helper methods ----------------------------------------------------

  /**
   * Clear the registry.
   */
  protected void clear() {
    viewDefinitions.clear();
    viewInstanceDefinitions.clear();
    subResourceDefinitionsMap.clear();
  }

  // install a new view definition
  private ViewEntity installView(ViewConfig viewConfig, Configuration ambariConfig,
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
    addDefinition(viewDefinition);
    return viewDefinition;
  }

  // install a view instance definition
  private void _installViewInstance(ViewEntity viewDefinition,
                                           ViewInstanceEntity viewInstanceDefinition)
      throws ClassNotFoundException {

    ViewContext viewInstanceContext = new ViewContextImpl(viewInstanceDefinition, this);

    ViewExternalSubResourceService externalSubResourceService =
        new ViewExternalSubResourceService(viewDefinition.getExternalResourceType(), viewInstanceDefinition);

    viewInstanceDefinition.addService(ResourceConfig.EXTERNAL_RESOURCE_PLURAL_NAME, externalSubResourceService);

    Collection<ViewSubResourceDefinition> resourceDefinitions = viewDefinition.getResourceDefinitions().values();
    for (ViewSubResourceDefinition resourceDefinition : resourceDefinitions) {

      Resource.Type  type           = resourceDefinition.getType();
      ResourceConfig resourceConfig = resourceDefinition.getResourceConfiguration();

      ViewResourceHandler viewResourceService =
          new ViewSubResourceService(type, viewDefinition.getName(), viewInstanceDefinition.getName());

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
    addInstanceDefinition(viewDefinition, viewInstanceDefinition);
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

  // make sure that the views in the ambari db match the registry
  private Set<ViewInstanceEntity> persistViews() throws ClassNotFoundException {

    Set<ViewInstanceEntity> instanceDefinitions = new HashSet<ViewInstanceEntity>();
    Set<String> persistedViews = new HashSet<String>();

    for (ViewEntity viewEntity : viewDAO.findAll()) {
      String name = viewEntity.getName();
      if (!ViewRegistry.getInstance().viewDefinitions.containsKey(name)) {

        System.out.println("removing view " + name);
        viewDAO.remove(viewEntity);
      } else {
        persistedViews.add(name);

        ViewEntity viewDefinition = ViewRegistry.getInstance().viewDefinitions.get(name);

        for (ViewInstanceEntity viewInstanceEntity : viewEntity.getInstances()){
          if (viewDefinition.getInstanceDefinition(viewInstanceEntity.getName()) == null) {
            viewInstanceEntity.setViewEntity(viewDefinition);
            _installViewInstance(viewDefinition, viewInstanceEntity);
            instanceDefinitions.add(viewInstanceEntity);
          }
        }
      }
    }

    // persist new views
    for (ViewEntity definition : viewDefinitions.values() ) {
      String viewName = definition.getName();

      if (!persistedViews.contains(viewName)) {
        if (LOG.isDebugEnabled()) {
          LOG.debug("Creating View " + viewName + ".");
        }
        viewDAO.create(definition);
      }
    }
    return instanceDefinitions;
  }
}
