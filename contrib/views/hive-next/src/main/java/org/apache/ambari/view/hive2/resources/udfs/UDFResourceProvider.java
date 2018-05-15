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

package org.apache.ambari.view.hive2.resources.udfs;

import org.apache.ambari.view.*;
import org.apache.ambari.view.hive2.persistence.utils.ItemNotFound;
import org.apache.ambari.view.hive2.persistence.utils.OnlyOwnersFilteringStrategy;
import org.apache.ambari.view.hive2.utils.SharedObjectsFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Resource provider for udf
 */
public class UDFResourceProvider implements ResourceProvider<UDF> {
  @Inject
  ViewContext context;

  protected UDFResourceManager resourceManager = null;
  protected final static Logger LOG =
      LoggerFactory.getLogger(UDFResourceProvider.class);


  protected synchronized UDFResourceManager getResourceManager() {
    if (resourceManager == null) {
      resourceManager = new UDFResourceManager(new SharedObjectsFactory(context), context);
    }
    return resourceManager;
  }

  @Override
  public UDF getResource(String resourceId, Set<String> properties) throws SystemException, NoSuchResourceException, UnsupportedPropertyException {
    try {
      return getResourceManager().read(resourceId);
    } catch (ItemNotFound itemNotFound) {
      throw new NoSuchResourceException(resourceId);
    }
  }

  @Override
  public Set<UDF> getResources(ReadRequest readRequest) throws SystemException, NoSuchResourceException, UnsupportedPropertyException {
    if (context == null) {
      return new HashSet<UDF>();
    }
    return new HashSet<UDF>(getResourceManager().readAll(
        new OnlyOwnersFilteringStrategy(this.context.getUsername())));
  }

  @Override
  public void createResource(String s, Map<String, Object> stringObjectMap) throws SystemException, ResourceAlreadyExistsException, NoSuchResourceException, UnsupportedPropertyException {
    UDF item = null;
    try {
      item = new UDF(stringObjectMap);
    } catch (InvocationTargetException e) {
      throw new SystemException("error on creating resource", e);
    } catch (IllegalAccessException e) {
      throw new SystemException("error on creating resource", e);
    }
    getResourceManager().create(item);
  }

  @Override
  public boolean updateResource(String resourceId, Map<String, Object> stringObjectMap) throws SystemException, NoSuchResourceException, UnsupportedPropertyException {
    UDF item = null;
    try {
      item = new UDF(stringObjectMap);
    } catch (InvocationTargetException e) {
      throw new SystemException("error on updating resource", e);
    } catch (IllegalAccessException e) {
      throw new SystemException("error on updating resource", e);
    }
    try {
      getResourceManager().update(item, resourceId);
    } catch (ItemNotFound itemNotFound) {
      throw new NoSuchResourceException(resourceId);
    }
    return true;
  }

  @Override
  public boolean deleteResource(String resourceId) throws SystemException, NoSuchResourceException, UnsupportedPropertyException {
    try {
      getResourceManager().delete(resourceId);
    } catch (ItemNotFound itemNotFound) {
      throw new NoSuchResourceException(resourceId);
    }
    return true;
  }
}
