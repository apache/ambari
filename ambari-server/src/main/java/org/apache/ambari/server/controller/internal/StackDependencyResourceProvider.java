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

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.controller.spi.NoSuchParentResourceException;
import org.apache.ambari.server.controller.spi.NoSuchResourceException;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.RequestStatus;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceAlreadyExistsException;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.spi.UnsupportedPropertyException;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.state.AutoDeployInfo;
import org.apache.ambari.server.state.DependencyInfo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Resource provider for Stack Dependency resource.
 */
public class StackDependencyResourceProvider extends AbstractResourceProvider {

  // ----- Property ID constants ---------------------------------------------

  protected static final String STACK_NAME_ID =
      PropertyHelper.getPropertyId("Dependencies", "stack_name");
  protected static final String STACK_VERSION_ID =
      PropertyHelper.getPropertyId("Dependencies", "stack_version");
  protected static final String DEPENDENT_SERVICE_NAME_ID =
      PropertyHelper.getPropertyId("Dependencies", "dependent_service_name");
  protected static final String DEPENDENT_COMPONENT_NAME_ID =
      PropertyHelper.getPropertyId("Dependencies", "dependent_component_name");
  protected static final String SERVICE_NAME_ID =
      PropertyHelper.getPropertyId("Dependencies", "service_name");
  protected static final String COMPONENT_NAME_ID =
      PropertyHelper.getPropertyId("Dependencies", "component_name");
  protected static final String SCOPE_ID =
      PropertyHelper.getPropertyId("Dependencies", "scope");
  protected static final String AUTO_DEPLOY_ENABLED_ID = PropertyHelper
      .getPropertyId("auto_deploy", "enabled");
  protected static final String AUTO_DEPLOY_LOCATION_ID = PropertyHelper
      .getPropertyId("auto_deploy", "location");

  // Primary Key Fields
  private static Set<String> pkPropertyIds =
      new HashSet<String>(Arrays.asList(new String[]{
          SERVICE_NAME_ID, COMPONENT_NAME_ID}));

  /**
   * Provides stack information
   */
  private static AmbariMetaInfo ambariMetaInfo;


  // ----- Constructors ----------------------------------------------------

  /**
   * Constructor.
   *
   * @param propertyIds    the property ids
   * @param keyPropertyIds the key property ids
   */
  protected StackDependencyResourceProvider(Set<String> propertyIds,
                                            Map<Resource.Type, String> keyPropertyIds) {
    super(propertyIds, keyPropertyIds);
  }

  /**
   * Static initialization.
   *
   * @param metaInfo meta info instance
   */
  public static void init(AmbariMetaInfo metaInfo) {
    ambariMetaInfo = metaInfo;
  }


  // ----- ResourceProvider ------------------------------------------------

  @Override
  protected Set<String> getPKPropertyIds() {
    return pkPropertyIds;
  }

  @Override
  public Set<Resource> getResources(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException,
             NoSuchResourceException, NoSuchParentResourceException {

    Set<Resource> resources = new HashSet<Resource>();
    Set<Map<String, Object>> requestProps = getPropertyMaps(predicate);

    for (Map<String, Object> properties : requestProps) {
      try {
        resources.addAll(getDependencyResources(properties,
            getRequestPropertyIds(request, predicate)));
      } catch (NoSuchResourceException e) {
        if (requestProps.size() == 1) {
          throw e;
        }
      } catch (NoSuchParentResourceException e) {
        if (requestProps.size() == 1) {
          throw e;
        }
      }
    }
    return resources;
  }

  @Override
  public RequestStatus createResources(Request request)
      throws SystemException, UnsupportedPropertyException,
      ResourceAlreadyExistsException, NoSuchParentResourceException {

    // should not get here as service doesn't allow POST and should return 405
    throw new SystemException("Stack resources are read only");
  }

  @Override
  public RequestStatus updateResources(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException,
      NoSuchResourceException, NoSuchParentResourceException {

    // should not get here as service doesn't allow PUT and should return 405
    throw new SystemException("Stack resources are read only");
  }

  @Override
  public RequestStatus deleteResources(Predicate predicate)
      throws SystemException, UnsupportedPropertyException,
      NoSuchResourceException, NoSuchParentResourceException {

    // should not get here as service doesn't allow DELETE and should return 405
    throw new SystemException("Stack resources are read only");
  }


  // ----- Instance Methods ------------------------------------------------

  /**
   * Get dependencies resources.
   *
   * @param properties    request properties
   * @param requestedIds  requested id's
   *
   * @return collection of dependency resources
   *
   * @throws SystemException an internal error occurred
   * @throws NoSuchParentResourceException parent resource not found
   * @throws NoSuchResourceException dependency resource not found
   */
  private Collection<Resource> getDependencyResources(Map<String, Object> properties, Set<String> requestedIds)
      throws SystemException, NoSuchParentResourceException, NoSuchResourceException {

    final String stackName = (String) properties.get(STACK_NAME_ID);
    final String version = (String) properties.get(STACK_VERSION_ID);
    final String dependentService = (String) properties.get(DEPENDENT_SERVICE_NAME_ID);
    final String dependentComponent = (String)  properties.get(DEPENDENT_COMPONENT_NAME_ID);
    final String dependencyName = (String) properties.get(COMPONENT_NAME_ID);

    List<DependencyInfo> dependencies = new ArrayList<DependencyInfo>();
    if (dependencyName != null) {
      dependencies.add(getResources(new Command<DependencyInfo>() {
        @Override
        public DependencyInfo invoke() throws AmbariException {
          return ambariMetaInfo.getComponentDependency(stackName, version, dependentService,
              dependentComponent, dependencyName);
        }
      }));
    } else {
      dependencies.addAll(getResources(new Command<List<DependencyInfo>>() {
        @Override
        public List<DependencyInfo> invoke() throws AmbariException {
          return ambariMetaInfo.getComponentDependencies(stackName, version,
              dependentService, dependentComponent);
        }
      }));
    }

    Collection<Resource> resources = new ArrayList<Resource>();
    for (DependencyInfo dependency : dependencies) {
      if (dependency != null) {
        resources.add(toResource(dependency, stackName, version, dependentService,
            dependentComponent, requestedIds));
      }
    }
    return resources;
  }

  /**
   * Create a resource instance with dependency information.
   *
   * @param dependency          dependency name
   * @param stackName           stack name
   * @param version             stack version
   * @param dependentService    dependent service
   * @param dependentComponent  dependent component
   * @param requestedIds        requested id's
   *
   * @return a new Resource instance for the dependency
   */
  private Resource toResource(DependencyInfo dependency, String stackName,
                              String version, String dependentService,
                              String dependentComponent, Set<String> requestedIds) {

    Resource resource = new ResourceImpl(Resource.Type.StackServiceComponentDependency);

    setResourceProperty(resource, SERVICE_NAME_ID, dependency.getServiceName(), requestedIds);
    setResourceProperty(resource, COMPONENT_NAME_ID, dependency.getComponentName(), requestedIds);
    setResourceProperty(resource, STACK_NAME_ID, stackName, requestedIds);
    setResourceProperty(resource, STACK_VERSION_ID, version, requestedIds);
    setResourceProperty(resource, DEPENDENT_SERVICE_NAME_ID, dependentService, requestedIds);
    setResourceProperty(resource, DEPENDENT_COMPONENT_NAME_ID, dependentComponent, requestedIds);
    setResourceProperty(resource, SCOPE_ID, dependency.getScope(), requestedIds);

    AutoDeployInfo autoDeployInfo = dependency.getAutoDeploy();
    if (autoDeployInfo != null) {
      setResourceProperty(resource, AUTO_DEPLOY_ENABLED_ID,
          autoDeployInfo.isEnabled(), requestedIds);

      if (autoDeployInfo.getCoLocate() != null) {
        setResourceProperty(resource, AUTO_DEPLOY_LOCATION_ID,
            autoDeployInfo.getCoLocate(), requestedIds);
      }
    }
    return resource;
  }
}
