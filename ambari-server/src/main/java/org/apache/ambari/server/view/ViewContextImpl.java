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
import org.apache.ambari.server.configuration.ComponentSSLConfiguration;
import org.apache.ambari.server.orm.entities.PermissionEntity;
import org.apache.ambari.server.orm.entities.ViewEntity;
import org.apache.ambari.server.orm.entities.ViewInstanceEntity;
import org.apache.ambari.server.view.configuration.ParameterConfig;
import org.apache.ambari.server.view.configuration.ViewConfig;
import org.apache.ambari.server.view.events.EventImpl;
import org.apache.ambari.server.view.persistence.DataStoreImpl;
import org.apache.ambari.server.view.persistence.DataStoreModule;
import org.apache.ambari.view.DataStore;
import org.apache.ambari.view.MaskException;
import org.apache.ambari.view.Masker;
import org.apache.ambari.view.ResourceProvider;
import org.apache.ambari.view.SecurityException;
import org.apache.ambari.view.SystemException;
import org.apache.ambari.view.URLStreamProvider;
import org.apache.ambari.server.controller.internal.AppCookieManager;
import org.apache.ambari.view.ImpersonatorSetting;
import org.apache.ambari.view.ViewContext;
import org.apache.ambari.view.ViewController;
import org.apache.ambari.view.ViewDefinition;
import org.apache.ambari.view.ViewInstanceDefinition;
import org.apache.ambari.view.events.Event;
import org.apache.ambari.view.events.Listener;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.exception.ParseErrorException;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * View context implementation.
 */
public class ViewContextImpl implements ViewContext, ViewController {

  /**
   * Logger.
   */
  private static final Log LOG = LogFactory.getLog(ViewContextImpl.class);

  /**
   * The associated view definition.
   */
  private final ViewEntity viewEntity;

  /**
   * The associated view definition.
   */
  private final ViewInstanceEntity viewInstanceEntity;

  /**
   * The view registry.
   */
  private final ViewRegistry viewRegistry;

  /**
   * The available stream provider.
   */
  private final ViewURLStreamProvider streamProvider;

  /**
   * The data store.
   */
  private DataStore dataStore = null;

  /**
   * Masker for properties.
   */
  private Masker masker;

  private final VelocityContext velocityContext;

  // ---- Constructors -------------------------------------------------------

  /**
   * Construct a view context from the given view instance entity.
   *
   * @param viewInstanceEntity  the view entity
   * @param viewRegistry        the view registry
   */
  public ViewContextImpl(ViewInstanceEntity viewInstanceEntity, ViewRegistry viewRegistry) {
    this(viewInstanceEntity.getViewEntity(), viewInstanceEntity, viewRegistry);
  }

  /**
   * Construct a view context from the given view entity.
   *
   * @param viewEntity    the view entity
   * @param viewRegistry  the view registry
   */
  public ViewContextImpl(ViewEntity viewEntity, ViewRegistry viewRegistry) {
    this(viewEntity, null, viewRegistry);
  }

  private ViewContextImpl(ViewEntity viewEntity, ViewInstanceEntity viewInstanceEntity, ViewRegistry viewRegistry) {
    this.viewEntity         = viewEntity;
    this.viewInstanceEntity = viewInstanceEntity;
    this.viewRegistry       = viewRegistry;
    this.streamProvider     = ViewURLStreamProvider.getProvider();
    this.masker             = getMasker(viewEntity.getClassLoader(), viewEntity.getConfiguration());
    this.velocityContext    = initVelocityContext();
  }

  // ----- ViewContext -------------------------------------------------------

  @Override
  public String getViewName() {
    return viewEntity.getCommonName();
  }

  @Override
  public ViewDefinition getViewDefinition() {
    return viewEntity;
  }

  @Override
  public String getInstanceName() {
    return viewInstanceEntity == null ? null : viewInstanceEntity.getName();
  }

  @Override
  public ViewInstanceDefinition getViewInstanceDefinition() {
    return viewInstanceEntity;
  }

  @Override
  public Map<String, String> getProperties() {
    if (viewInstanceEntity == null) {
      return null;
    } else {
      Map<String, String> properties = viewInstanceEntity.getPropertyMap();

      // unmasking
      for (Entry<String, String> entry: properties.entrySet()) {
        ParameterConfig parameterConfig = null;
        for (ParameterConfig paramConfig : viewEntity.getConfiguration().getParameters()) {
          if (StringUtils.equals(paramConfig.getName(), entry.getKey())) {
            parameterConfig = paramConfig;
            break;
          }
        }
        if (parameterConfig == null || !parameterConfig.isMasked()) {
          properties.put(entry.getKey(), entry.getValue());
        } else {
          try {
            properties.put(entry.getKey(), masker.unmask(entry.getValue()));
          } catch (MaskException e) {
            LOG.error("Failed to unmask view property", e);
          }
        }
      }

      // parametrizing

      String rawValue;
      for (String key : properties.keySet()) {
        rawValue = properties.get(key);
        try {
          properties.put(key, parameterize(rawValue));
        } catch (ParseErrorException ex) {
          LOG.warn(String.format("Error during parsing '%s' parameter. Leaving original value.", key));
        }
      }
      return Collections.unmodifiableMap(properties);
    }
  }

  @Override
  public void putInstanceData(String key, String value) {
    checkInstance();
    viewInstanceEntity.putInstanceData(key, value);
    try {
      viewRegistry.updateViewInstance(viewInstanceEntity);
    } catch (SystemException e) {
      String msg = "Caught exception updating the view instance.";
      LOG.error(msg, e);
      throw new IllegalStateException(msg, e);
    }
  }

  @Override
  public String getInstanceData(String key) {
    return viewInstanceEntity == null ? null :
        viewInstanceEntity.getInstanceDataMap().get(key);
  }

  @Override
  public Map<String, String> getInstanceData() {
    return viewInstanceEntity == null ? null :
        Collections.unmodifiableMap(viewInstanceEntity.getInstanceDataMap());
  }

  @Override
  public void removeInstanceData(String key) {
    checkInstance();
    viewRegistry.removeInstanceData(viewInstanceEntity, key);
  }

  @Override
  public String getAmbariProperty(String key) {
    return viewInstanceEntity == null ? null :
        viewInstanceEntity.getViewEntity().getAmbariProperty(key);
  }

  @Override
  public ResourceProvider<?> getResourceProvider(String type) {
    return viewInstanceEntity == null ? null :
        viewInstanceEntity.getResourceProvider(type);
  }

  @Override
  public String getUsername() {
    return viewInstanceEntity != null ? viewInstanceEntity.getUsername() : null;
  }

  @Override
  public void hasPermission(String userName, String permissionName) throws org.apache.ambari.view.SecurityException {

    if (userName == null || userName.length() == 0) {
      throw new SecurityException("No user name specified.");
    }

    if (permissionName == null || permissionName.length() == 0) {
      throw new SecurityException("No permission name specified.");
    }

    if (viewInstanceEntity == null) {
      throw new SecurityException("There is no instance associated with the view context");
    }

    PermissionEntity permissionEntity = viewEntity.getPermission(permissionName);

    if (permissionEntity == null) {
      throw new SecurityException("The permission " + permissionName + " is not defined for " + viewEntity.getName());
    }

    if (!viewRegistry.hasPermission(permissionEntity, viewInstanceEntity.getResource(), userName)) {
      throw new SecurityException("The user " + userName + " has not been granted permission " + permissionName);
    }
  }

  @Override
  public URLStreamProvider getURLStreamProvider() {
    return streamProvider;
  }

  @Override
  public synchronized DataStore getDataStore() {
    if (viewInstanceEntity != null) {
      if (dataStore == null) {
        Injector injector = Guice.createInjector(new DataStoreModule(viewInstanceEntity));
        dataStore = injector.getInstance(DataStoreImpl.class);
      }
    }
    return dataStore;
  }

  @Override
  public Collection<ViewDefinition> getViewDefinitions() {
    return Collections.<ViewDefinition>unmodifiableCollection(viewRegistry.getDefinitions());
  }

  @Override
  public Collection<ViewInstanceDefinition> getViewInstanceDefinitions() {
    Collection<ViewInstanceEntity> instanceDefinitions = new HashSet<ViewInstanceEntity>();
    for (ViewEntity viewEntity : viewRegistry.getDefinitions()) {
      instanceDefinitions.addAll(viewRegistry.getInstanceDefinitions(viewEntity));
    }
    return Collections.<ViewInstanceDefinition>unmodifiableCollection(instanceDefinitions);
  }

  @Override
  public ViewController getController() {
    return this;
  }

  @Override
  public HttpImpersonatorImpl getHttpImpersonator() {
    return new HttpImpersonatorImpl(this, this.streamProvider.getAppCookieManager());
  }

  @Override
  public ImpersonatorSetting getImpersonatorSetting() {
    return new ImpersonatorSettingImpl(this);
  }

  // ----- ViewController ----------------------------------------------------

  @Override
  public void fireEvent(String eventId, Map<String, String> eventProperties) {
    Event event = viewInstanceEntity == null ?
        new EventImpl(eventId, eventProperties, viewEntity) :
        new EventImpl(eventId, eventProperties, viewInstanceEntity);

    viewRegistry.fireEvent(event);
  }

  @Override
  public void registerListener(Listener listener, String viewName) {
    viewRegistry.registerListener(listener, viewName, null);
  }

  @Override
  public void registerListener(Listener listener, String viewName, String viewVersion) {
    viewRegistry.registerListener(listener, viewName, viewVersion);
  }

  @Override
  public void unregisterListener(Listener listener, String viewName) {
    viewRegistry.unregisterListener(listener, viewName, null);
  }

  @Override
  public void unregisterListener(Listener listener, String viewName, String viewVersion) {
    viewRegistry.unregisterListener(listener, viewName, viewVersion);
  }

  // ----- helper methods ----------------------------------------------------

  // check for an associated instance
  private void checkInstance() {
    if (viewInstanceEntity == null) {
      throw new IllegalStateException("No instance is associated with the context.");
    }
  }

  private Masker getMasker(ClassLoader cl, ViewConfig viewConfig) {
    try {
      return viewConfig.getMaskerClass(cl).newInstance();
    } catch (Exception e) {
      throw new InstantiationError("Could not create masker instance.");
    }
  }

  /**
   * Parameterize string using VelocityContext instance
   *
   * @param raw original string with parameters in formal or shorthand notation
   *
   * @return parameterized string
   *
   * @throws ParseErrorException if original string cannot be parsed by Velocity
   */
  private String parameterize(String raw) throws ParseErrorException {
    Writer templateWriter = new StringWriter();
    Velocity.evaluate(velocityContext, templateWriter, raw, raw);
    return templateWriter.toString();
  }

  /**
   * Instantiate and initialize context for parameters processing using Velocity.
   *
   * @return initialized context instance
   */
  private VelocityContext initVelocityContext() {
    VelocityContext context = new VelocityContext();
    context.put("username",
        new ParameterResolver() {
          @Override
          protected String getValue() {
            return viewContext.getUsername();
          }
        });
    context.put("viewName",
        new ParameterResolver() {
          @Override
          protected String getValue() {
            return viewContext.getViewName();
          }
        });
    context.put("instanceName",
        new ParameterResolver() {
          @Override
          protected String getValue() {
            return viewContext.getInstanceName();
          }
        });
    return context;
  }

  // ----- Inner class : ViewURLStreamProvider -------------------------------

  /**
   * Wrapper around internal URL stream provider.
   */
  protected static class ViewURLStreamProvider implements URLStreamProvider {
    private static final int DEFAULT_REQUEST_CONNECT_TIMEOUT = 5000;
    private static final int DEFAULT_REQUEST_READ_TIMEOUT    = 10000;

    /**
     * Internal stream provider.
     */
    private final org.apache.ambari.server.controller.internal.URLStreamProvider streamProvider;

    // ----- Constructor -----------------------------------------------------

    protected ViewURLStreamProvider(org.apache.ambari.server.controller.internal.URLStreamProvider streamProvider) {
      this.streamProvider = streamProvider;
    }


    // ----- URLStreamProvider -----------------------------------------------

    @Override
    public InputStream readFrom(String spec, String requestMethod, String params, Map<String, String> headers)
        throws IOException {
      // adapt to org.apache.ambari.server.controller.internal.URLStreamProvider processURL signature
      Map<String, List<String>> headerMap = new HashMap<String, List<String>>();
      for (Map.Entry<String, String> entry : headers.entrySet()) {
        headerMap.put(entry.getKey(), Collections.singletonList(entry.getValue()));
      }
      return streamProvider.processURL(spec, requestMethod, params, headerMap).getInputStream();
    }


    // ----- helper methods --------------------------------------------------

    /**
     * Factory method.
     *
     * @return a new URL stream provider.
     */
    protected static ViewURLStreamProvider getProvider() {
      ComponentSSLConfiguration configuration = ComponentSSLConfiguration.instance();
      org.apache.ambari.server.controller.internal.URLStreamProvider streamProvider =
          new org.apache.ambari.server.controller.internal.URLStreamProvider(
              DEFAULT_REQUEST_CONNECT_TIMEOUT, DEFAULT_REQUEST_READ_TIMEOUT,
              configuration.getTruststorePath(),
              configuration.getTruststorePassword(),
              configuration.getTruststoreType());
      return new ViewURLStreamProvider(streamProvider);
    }

    protected AppCookieManager getAppCookieManager() {
      return streamProvider.getAppCookieManager();
    }
  }

  // ----- Inner class : ParameterResolver -------------------------------

  /**
   * Represents basic parameter resolver to obtain fields of ViewContext at runtime.
   */
  private abstract class ParameterResolver {

    protected final ViewContext viewContext = ViewContextImpl.this;

    protected abstract String getValue();

    @Override
    public String toString() {
      String value = getValue();
      return value == null ? "" : value;
    }
  }
}
