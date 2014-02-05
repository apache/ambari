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
import org.apache.ambari.server.view.configuration.InstanceConfig;
import org.apache.ambari.server.view.configuration.PropertyConfig;
import org.apache.ambari.server.view.configuration.ResourceConfig;
import org.apache.ambari.server.view.configuration.ViewConfig;
import org.apache.ambari.view.ViewContext;
import org.apache.ambari.view.ViewResourceHandler;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.DelegatingFilterProxy;

import javax.servlet.http.HttpServlet;
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
  private Map<String, ViewDefinition> viewDefinitions = new HashMap<String, ViewDefinition>();

  /**
   * Mapping of view instances to view definition and instance name.
   */
  private Map<ViewDefinition, Map<String, ViewInstanceDefinition>> viewInstanceDefinitions = new HashMap<ViewDefinition, Map<String, ViewInstanceDefinition>>();

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
  public Collection<ViewDefinition> getDefinitions() {
    return viewDefinitions.values();
  }

  /**
   * Get a view definition for the given name.
   *
   * @param viewName  the view name
   *
   * @return the view definition for the given name
   */
  public ViewDefinition getDefinition(String viewName) {
    return viewDefinitions.get(viewName);
  }

  /**
   * Add a view definition to the registry.
   *
   * @param definition  the definition
   */
  public void addDefinition(ViewDefinition definition) {
    viewDefinitions.put(definition.getName(), definition);
  }

  /**
   * Get the collection of view instances for the given view definition.
   *
   * @param definition  the view definition
   *
   * @return the collection of view instances for the view definition
   */
  public Collection<ViewInstanceDefinition> getInstanceDefinitions(ViewDefinition definition) {
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
  public ViewInstanceDefinition getInstanceDefinition(String viewName, String instanceName) {
    Map<String, ViewInstanceDefinition> viewInstanceDefinitionMap =
        viewInstanceDefinitions.get(getDefinition(viewName));

    return viewInstanceDefinitionMap == null ? null : viewInstanceDefinitionMap.get(instanceName);
  }

  /**
   * Add an instance definition for the given view definition.
   *
   * @param definition          the owning view definition
   * @param instanceDefinition  the instance definition
   */
  public void addInstanceDefinition(ViewDefinition definition, ViewInstanceDefinition instanceDefinition) {
    Map<String, ViewInstanceDefinition> instanceDefinitions = viewInstanceDefinitions.get(definition);
    if (instanceDefinitions == null) {
      instanceDefinitions = new HashMap<String, ViewInstanceDefinition>();
      viewInstanceDefinitions.put(definition, instanceDefinitions);
    }
    instanceDefinitions.put(instanceDefinition.getName(), instanceDefinition);
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
      ViewDefinition definition = getDefinition(viewName);
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
   * @param configuration       Ambari configuration
   * @param rootContextHandler  the root servlet context handler
   * @param filterProxy         the security filter
   */
  public static void readViewArchives(Configuration configuration, ServletContextHandler rootContextHandler,
                                      DelegatingFilterProxy filterProxy) {

    File   viewDir = configuration.getViewsDir();
    File[] files   = viewDir.listFiles();

    if (files != null) {
      for (final File fileEntry : files) {
        if (!fileEntry.isDirectory()) {
          try {
            ClassLoader cl = URLClassLoader.newInstance(new URL[]{fileEntry.toURI().toURL()});

            InputStream    configStream     = cl.getResourceAsStream(VIEW_XML);
            JAXBContext    jaxbContext      = JAXBContext.newInstance(ViewConfig.class);
            Unmarshaller   jaxbUnmarshaller = jaxbContext.createUnmarshaller();
            ViewConfig     viewConfig       = (ViewConfig) jaxbUnmarshaller.unmarshal(configStream);
            ViewDefinition viewDefinition   = installView(viewConfig, cl, configuration);

            List<InstanceConfig> instances = viewConfig.getInstances();

            for (InstanceConfig instanceConfig : instances) {
              installViewInstance(viewDefinition, instanceConfig, cl, rootContextHandler, filterProxy, configuration);
            }
          } catch (Exception e) {
            LOG.error("Caught exception loading view from " + fileEntry.getAbsolutePath(), e);
          }
        }
      }
    }
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
  private static ViewDefinition installView(ViewConfig viewConfig, ClassLoader cl, Configuration ambariConfig)
      throws ClassNotFoundException, IntrospectionException {

    List<ResourceConfig> resourceConfigurations = viewConfig.getResources();

    ViewDefinition viewDefinition = new ViewDefinition(viewConfig, ambariConfig);

    Resource.Type externalResourceType = viewDefinition.getExternalResourceType();

    ViewExternalSubResourceProvider viewExternalSubResourceProvider =
        new ViewExternalSubResourceProvider(externalResourceType, viewDefinition);
    viewDefinition.addResourceProvider(externalResourceType, viewExternalSubResourceProvider );

    ResourceInstanceFactoryImpl.addResourceDefinition(externalResourceType,
        new ViewExternalSubResourceDefinition(externalResourceType));

    for (ResourceConfig resourceConfiguration : resourceConfigurations) {

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
      }
    }

    ViewRegistry.getInstance().addDefinition(viewDefinition);
    return viewDefinition;
  }

  // install a new view instance definition
  private static void installViewInstance(ViewDefinition viewDefinition,
                                   InstanceConfig instanceConfig,
                                   ClassLoader cl,
                                   ServletContextHandler root,
                                   DelegatingFilterProxy springSecurityFilter,
                                   Configuration ambariConfig) throws ClassNotFoundException {

    ViewInstanceDefinition viewInstanceDefinition = new ViewInstanceDefinition(viewDefinition, instanceConfig);

    List<PropertyConfig> propertyConfigs = instanceConfig.getProperties();

    for (PropertyConfig propertyConfig : propertyConfigs) {
      viewInstanceDefinition.addProperty(propertyConfig.getKey(), propertyConfig.getValue());
    }

    ViewContext viewInstanceContext = new ViewContextImpl(viewInstanceDefinition);

    ViewExternalSubResourceService externalSubResourceService =
        new ViewExternalSubResourceService(viewDefinition.getExternalResourceType(), viewInstanceDefinition);

    viewInstanceDefinition.addService(ResourceConfig.EXTERNAL_RESOURCE_PLURAL_NAME, externalSubResourceService);

    Collection<ViewSubResourceDefinition> resourceDefinitions = viewDefinition.getResourceDefinitions().values();
    for (ViewSubResourceDefinition resourceDefinition : resourceDefinitions) {

      Resource.Type  type           = resourceDefinition.getType();
      ResourceConfig resourceConfig = resourceDefinition.getResourceConfiguration();

      ViewResourceHandler viewResourceService =
          new ViewSubResourceService(type, viewDefinition.getName(), instanceConfig.getName());

      Object service = getService(resourceConfig.getServiceClass(cl), viewResourceService, viewInstanceContext);

      if (resourceConfig.isExternal()) {
        externalSubResourceService.addResourceService(resourceConfig.getName(), service);
      } else {
        viewInstanceDefinition.addService(viewDefinition.getResourceDefinition(type).getPluralName(),service);
        viewInstanceDefinition.addResourceProvider(type,
            getProvider(resourceConfig.getProviderClass(cl), viewInstanceContext));
      }
    }

    ViewConfig viewConfig = viewDefinition.getConfiguration();

    Map<String, Class<? extends HttpServlet>> servletPathMap = viewConfig.getServletPathMap(cl);
    Map<String, String> servletURLPatternMap = viewConfig.getServletURLPatternMap();

    for (Map.Entry<String, Class<? extends HttpServlet>> entry : servletPathMap.entrySet()) {
      HttpServlet servlet = getServlet(entry.getValue(), viewInstanceContext);
      ServletHolder sh = new ServletHolder(servlet);
      String servletName = entry.getKey();
      String pathSpec = "/views/" + viewDefinition.getName() + "/" +
          viewInstanceDefinition.getName() + servletURLPatternMap.get(servletName);
      root.addServlet(sh, pathSpec);
      viewInstanceDefinition.addServletMapping(servletName, pathSpec);

      if (ambariConfig.getApiAuthentication()) {
        root.addFilter(new FilterHolder(springSecurityFilter), pathSpec, 1);
      }
    }
    viewDefinition.addInstanceDefinition(viewInstanceDefinition);
    ViewRegistry.getInstance().addInstanceDefinition(viewDefinition, viewInstanceDefinition);
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

  // get the given servlet class from the given class loader; inject a context
  private static HttpServlet getServlet(Class<? extends HttpServlet> clazz,
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
}
