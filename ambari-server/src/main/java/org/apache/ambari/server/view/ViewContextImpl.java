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

import org.apache.ambari.server.configuration.ComponentSSLConfiguration;
import org.apache.ambari.server.orm.entities.ViewInstanceEntity;
import org.apache.ambari.view.ResourceProvider;
import org.apache.ambari.view.URLStreamProvider;
import org.apache.ambari.view.ViewContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * View context implementation.
 */
public class ViewContextImpl implements ViewContext {

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
  private final URLStreamProvider streamProvider;


  // ---- Constructors -------------------------------------------------------

  /**
   * Construct a view context from the given view entity.
   *
   * @param viewInstanceEntity  the view entity
   * @param viewRegistry        the view registry
   */
  public ViewContextImpl(ViewInstanceEntity viewInstanceEntity, ViewRegistry viewRegistry) {
    this.viewInstanceEntity = viewInstanceEntity;
    this.viewRegistry       = viewRegistry;
    this.streamProvider     = ViewURLStreamProvider.getProvider();
  }


  // ----- ViewContext -------------------------------------------------------

  @Override
  public String getViewName() {
    return viewInstanceEntity.getViewName();
  }

  @Override
  public String getInstanceName() {
    return viewInstanceEntity.getName();
  }

  @Override
  public Map<String, String> getProperties() {
    return Collections.unmodifiableMap(viewInstanceEntity.getPropertyMap());
  }

  @Override
  public void putInstanceData(String key, String value) {
    viewInstanceEntity.putInstanceData(key, value);
    viewRegistry.updateViewInstance(viewInstanceEntity);
  }

  @Override
  public String getInstanceData(String key) {
    return viewInstanceEntity.getInstanceDataMap().get(key);
  }

  @Override
  public Map<String, String> getInstanceData() {
    return Collections.unmodifiableMap(viewInstanceEntity.getInstanceDataMap());
  }

  @Override
  public void removeInstanceData(String key) {
    viewInstanceEntity.removeInstanceData(key);
    viewRegistry.updateViewInstance(viewInstanceEntity);
  }

  @Override
  public String getAmbariProperty(String key) {
    return viewInstanceEntity.getViewEntity().getAmbariProperty(key);
  }

  @Override
  public ResourceProvider<?> getResourceProvider(String type) {
    return viewInstanceEntity.getResourceProvider(type);
  }

  @Override
  public String getUsername() {
    SecurityContext ctx = SecurityContextHolder.getContext();
    Authentication authentication = ctx == null ? null : ctx.getAuthentication();
    Object principal = authentication == null ? null : authentication.getPrincipal();

    String username;
    if (principal instanceof UserDetails) {
      username = ((UserDetails)principal).getUsername();
    } else {
      username = principal == null ? "" :principal.toString();
    }
    return username;
  }

  @Override
  public URLStreamProvider getURLStreamProvider() {
    return streamProvider;
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
  }
}
