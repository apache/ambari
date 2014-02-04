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

package org.apache.ambari.server.controller.internal;

import org.apache.ambari.server.controller.spi.NoSuchParentResourceException;
import org.apache.ambari.server.controller.spi.NoSuchResourceException;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.RequestStatus;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceAlreadyExistsException;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.spi.UnsupportedPropertyException;
import org.apache.ambari.server.view.ViewDefinition;
import org.apache.ambari.server.view.ViewInstanceDefinition;
import org.apache.ambari.server.view.ViewRegistry;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Resource provider for view instances.
 */
public class ViewInstanceResourceProvider extends AbstractResourceProvider {

  /**
   * View instance property id constants.
   */
  public static final String VIEW_NAME_PROPERTY_ID        = "ViewInstanceInfo/view_name";
  public static final String INSTANCE_NAME_PROPERTY_ID    = "ViewInstanceInfo/instance_name";
  public static final String PROPERTIES_PROPERTY_ID       = "ViewInstanceInfo/properties";
  public static final String SERVLET_MAPPINGS_PROPERTY_ID = "ViewInstanceInfo/servlet_mappings";

  /**
   * The key property ids for a view instance resource.
   */
  private static Map<Resource.Type, String> keyPropertyIds = new HashMap<Resource.Type, String>();
  static {
    keyPropertyIds.put(Resource.Type.View, VIEW_NAME_PROPERTY_ID);
    keyPropertyIds.put(Resource.Type.ViewInstance, INSTANCE_NAME_PROPERTY_ID);
  }

  /**
   * The property ids for a view instance resource.
   */
  private static Set<String> propertyIds = new HashSet<String>();
  static {
    propertyIds.add(VIEW_NAME_PROPERTY_ID);
    propertyIds.add(INSTANCE_NAME_PROPERTY_ID);
    propertyIds.add(PROPERTIES_PROPERTY_ID);
    propertyIds.add(SERVLET_MAPPINGS_PROPERTY_ID);
  }


  // ----- Constructors ------------------------------------------------------

  /**
   * Construct a view instance resource provider.
   */
  public ViewInstanceResourceProvider() {
    super(propertyIds, keyPropertyIds);
  }


  // ----- ResourceProvider --------------------------------------------------

  @Override
  public RequestStatus createResources(Request request)
      throws SystemException, UnsupportedPropertyException,
             ResourceAlreadyExistsException, NoSuchParentResourceException {
    throw new UnsupportedOperationException("Not yet supported.");
  }

  @Override
  public Set<Resource> getResources(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {

    Set<Resource> resources    = new HashSet<Resource>();
    ViewRegistry  viewRegistry = ViewRegistry.getInstance();
    Set<String>   requestedIds = getRequestPropertyIds(request, predicate);

    Set<Map<String, Object>> propertyMaps = getPropertyMaps(predicate);
    if (propertyMaps.isEmpty()) {
      propertyMaps.add(Collections.<String, Object>emptyMap());
    }

    for (Map<String, Object> propertyMap : propertyMaps) {

      String viewName = (String) propertyMap.get(VIEW_NAME_PROPERTY_ID);
      String instanceName = (String) propertyMap.get(INSTANCE_NAME_PROPERTY_ID);

      for (ViewDefinition viewDefinition : viewRegistry.getDefinitions()){
        if (viewName == null || viewName.equals(viewDefinition.getName())) {
          for (ViewInstanceDefinition viewInstanceDefinition : viewRegistry.getInstanceDefinitions(viewDefinition)) {
            if (instanceName == null || instanceName.equals(viewInstanceDefinition.getName())) {
              Resource resource = new ResourceImpl(Resource.Type.ViewInstance);

              setResourceProperty(resource, VIEW_NAME_PROPERTY_ID, viewDefinition.getName(), requestedIds);
              setResourceProperty(resource, INSTANCE_NAME_PROPERTY_ID, viewInstanceDefinition.getName(), requestedIds);
              setResourceProperty(resource, PROPERTIES_PROPERTY_ID,
                  viewInstanceDefinition.getProperties(), requestedIds);
              setResourceProperty(resource, SERVLET_MAPPINGS_PROPERTY_ID,
                  viewInstanceDefinition.getServletMappings(), requestedIds);

              resources.add(resource);
            }
          }
        }
      }
    }
    return resources;
  }

  @Override
  public RequestStatus updateResources(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {
    throw new UnsupportedOperationException("Not yet supported.");
  }

  @Override
  public RequestStatus deleteResources(Predicate predicate)
      throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {
    throw new UnsupportedOperationException("Not yet supported.");
  }

  @Override
  public Map<Resource.Type, String> getKeyPropertyIds() {
    return keyPropertyIds;
  }


  // ----- AbstractResourceProvider ------------------------------------------

  @Override
  protected Set<String> getPKPropertyIds() {
    return new HashSet<String>(keyPropertyIds.values());
  }
}
