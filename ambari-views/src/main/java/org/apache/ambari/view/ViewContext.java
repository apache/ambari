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

package org.apache.ambari.view;

import java.util.Map;

/**
 * Context object available to the view components to provide access to
 * the view and instance attributes as well as run time information about
 * the current execution context.
 */
public interface ViewContext {

  /**
   * Key for mapping a view context as a property.
   */
  public static final String CONTEXT_ATTRIBUTE = "ambari-view-context";

  /**
   * Get the current user name.
   *
   * @return the current user name
   */
  public String getUsername();

  /**
   * Get the view name.
   *
   * @return the view name
   */
  public String getViewName();

  /**
   * Get the view instance name.
   *
   * @return the view instance name
   */
  public String getInstanceName();

  /**
   * Get the property values specified to create the view instance.
   *
   * @return the view instance property values
   */
  public Map<String, String> getProperties();

  /**
   * Save an instance data value for the given key.
   *
   * @param key    the key
   * @param value  the value
   */
  public void putInstanceData(String key, String value);

  /**
   * Get the instance data value for the given key.
   *
   * @param key  the key
   *
   * @return the instance data value
   */
  public String getInstanceData(String key);

  /**
   * Get the instance data values.
   *
   * @return the view instance property values
   */
  public Map<String, String> getInstanceData();

  /**
   * Remove the instance data value for the given key.
   *
   * @param key  the key
   */
  public void removeInstanceData(String key);

  /**
   * Get a property for the given key from the ambari configuration.
   *
   * @param key  the property key
   *
   * @return the property value; null indicates that the configuration contains no mapping for the key
   */
  public String getAmbariProperty(String key);

  /**
   * Get the view resource provider for the given resource type.
   *
   * @param type  the resource type
   *
   * @return the resource provider
   */
  public ResourceProvider<?> getResourceProvider(String type);

  /**
   * Get a URL stream provider.
   *
   * @return a stream provider
   */
  public URLStreamProvider getURLStreamProvider();
}
