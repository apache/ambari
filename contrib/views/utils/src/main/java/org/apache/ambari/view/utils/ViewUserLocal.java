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

package org.apache.ambari.view.utils;

import org.apache.ambari.view.ViewContext;

import java.util.HashMap;
import java.util.Map;

/**
 * Manages end user specific objects.
 * Ensures that the instance of specific class is exists only in one instance for
 * specific user of specific instance.
 * @param <T> user-local class
 */
public class ViewUserLocal<T> {
  private static Map<Class, Map<String, Object>> viewSingletonObjects = new HashMap<Class, Map<String, Object>>();
  private final Class<T> tClass;

  public ViewUserLocal(Class<T> tClass) {
    this.tClass = tClass;
  }

  /**
   * Initial value of user-local class. Value can be set either by initialValue() on first get() call,
   * or directly by calling set() method. Default initial value is null, it can be changed by overriding
   * this method.
   * @param context initial value usually based on user properties provided by View Context
   * @return initial value of user-local variable
   */
  protected synchronized T initialValue(ViewContext context) {
    return null;
  }

  /**
   * Returns user-local instance.
   * If instance of class is not present yet for user, calls initialValue to create it.
   * @param context View context that provides instance and user names.
   * @return instance
   */
  public T get(ViewContext context) {
    if (!viewSingletonObjects.containsKey(tClass)) {
      viewSingletonObjects.put(tClass, new HashMap<String, Object>());
    }

    Map<String, Object> instances = viewSingletonObjects.get(tClass);

    if (!instances.containsKey(getTagName(context))) {
      instances.put(getTagName(context), initialValue(context));
    }
    return (T) instances.get(getTagName(context));
  }

  /**
   * Method for directly setting user-local singleton instances.
   * @param obj new variable value for current user
   * @param context ViewContext that provides username and instance name
   */
  public void set(T obj, ViewContext context) {
    if (!viewSingletonObjects.containsKey(tClass)) {
      viewSingletonObjects.put(tClass, new HashMap<String, Object>());
    }

    Map<String, Object> instances = viewSingletonObjects.get(tClass);
    instances.put(getTagName(context), obj);
  }

  /**
   * Returns unique key for Map to store a user-local variable.
   * @param context ViewContext
   * @return Unique identifier of pair instance-user.
   */
  private String getTagName(ViewContext context) {
    if (context == null) {
      return "<null>";
    }
    return String.format("%s:%s", context.getInstanceName(), context.getUsername());
  }

  /**
   * Method for testing purposes, intended to clear the cached user-local instances.
   * Method should not normally be called from production code.
   * @param tClass classname instances of which should be dropped
   */
  public static void dropAllConnections(Class tClass) {
    Map<String, Object> instances = viewSingletonObjects.get(tClass);
    if (instances != null) {
      viewSingletonObjects.get(tClass).clear();
    }
  }

  /**
   * Method for testing purposes, intended to clear the cached user-local instances.
   * Drops all classes of user-local variables.
   * Method should not normally be called from production code.
   */
  public static void dropAllConnections() {
    viewSingletonObjects.clear();
  }
}
