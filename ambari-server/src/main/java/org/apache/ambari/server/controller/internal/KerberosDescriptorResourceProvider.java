package org.apache.ambari.server.controller.internal;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.apache.ambari.server.controller.AmbariManagementController;
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
import org.apache.ambari.server.orm.dao.KerberosDescriptorDAO;
import org.apache.ambari.server.orm.entities.KerberosDescriptorEntity;
import org.apache.ambari.server.topology.KerberosDescriptor;
import org.apache.ambari.server.topology.KerberosDescriptorFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.google.inject.assistedinject.Assisted;

/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
public class KerberosDescriptorResourceProvider extends AbstractControllerResourceProvider {

  private static final Logger LOGGER = LoggerFactory.getLogger(KerberosDescriptorResourceProvider.class);

  private static final String KERBEROS_DESCRIPTOR_NAME_PROPERTY_ID =
      PropertyHelper.getPropertyId("KerberosDescriptors", "kerberos_descriptor_name");

  private static final String KERBEROS_DESCRIPTOR_TEXT_PROPERTY_ID =
      PropertyHelper.getPropertyId("KerberosDescriptors", "kerberos_descriptor_text");

  /**
   * The key property ids for a KerberosDescriptor resource.
   */
  private static Map<Resource.Type, String> keyPropertyIds = ImmutableMap.<Resource.Type, String>builder()
      .put(Resource.Type.KerberosDescriptor, KERBEROS_DESCRIPTOR_NAME_PROPERTY_ID)
      .build();

  /**
   * The property ids for a KerberosDescriptor resource.
   */
  private static Set<String> propertyIds = Sets.newHashSet(
      KERBEROS_DESCRIPTOR_NAME_PROPERTY_ID,
      KERBEROS_DESCRIPTOR_TEXT_PROPERTY_ID);

  private KerberosDescriptorDAO kerberosDescriptorDAO;

  private KerberosDescriptorFactory kerberosDescriptorFactory;

  // keep constructors hidden
  @Inject
  KerberosDescriptorResourceProvider(KerberosDescriptorDAO kerberosDescriptorDAO,
                                     KerberosDescriptorFactory kerberosDescriptorFactory,
                                     @Assisted AmbariManagementController managementController) {
    super(Resource.Type.KerberosDescriptor, propertyIds, keyPropertyIds, managementController);
    this.kerberosDescriptorDAO = kerberosDescriptorDAO;
    this.kerberosDescriptorFactory = kerberosDescriptorFactory;
  }

  @Override
  public Set<String> checkPropertyIds(Set<String> propertyIds) {
    LOGGER.debug("Skipping property id validation for kerberos descriptor resources");
    return Collections.emptySet();
  }

  @Override
  public RequestStatus createResources(Request request) throws SystemException, UnsupportedPropertyException,
      ResourceAlreadyExistsException, NoSuchParentResourceException {

    String name = getNameFromRequest(request);
    String descriptor = getRawKerberosDescriptorFromRequest(request);

    KerberosDescriptor kerberosDescriptor = kerberosDescriptorFactory.createKerberosDescriptor(name, descriptor);
    kerberosDescriptorDAO.create(kerberosDescriptor.toEntity());

    return getRequestStatus(null);
  }


  @Override
  public Set<Resource> getResources(Request request, Predicate predicate) throws SystemException,
      UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {

    List<KerberosDescriptorEntity> results = null;
    boolean applyPredicate = false;

    if (predicate != null) {
      Set<Map<String, Object>> requestProps = getPropertyMaps(predicate);
      if (requestProps.size() == 1) {
        String name = (String) requestProps.iterator().next().get(
            KERBEROS_DESCRIPTOR_NAME_PROPERTY_ID);

        if (name != null) {
          KerberosDescriptorEntity entity = kerberosDescriptorDAO.findByName(name);
          results = entity == null ? Collections.emptyList() :
              Collections.singletonList(entity);
        }
      }
    }

    if (results == null) {
      applyPredicate = true;
      results = kerberosDescriptorDAO.findAll();
    }

    Set<Resource> resources = new HashSet<>();
    Set<String> requestPropertyIds = getRequestPropertyIds(request, predicate);
    for (KerberosDescriptorEntity entity : results) {
      Resource resource = new ResourceImpl(Resource.Type.KerberosDescriptor);
      toResource(resource, entity, requestPropertyIds);

      if (predicate == null || !applyPredicate || predicate.evaluate(resource)) {
        resources.add(resource);
      }
    }

    if (predicate != null && resources.isEmpty()) {
      throw new NoSuchResourceException(
          "The requested resource doesn't exist: Kerberos Descriptor not found, " + predicate);
    }

    return resources;
  }

  private void toResource(Resource resource, KerberosDescriptorEntity entity, Set<String> requestPropertyIds) {
    setResourceProperty(resource, KERBEROS_DESCRIPTOR_NAME_PROPERTY_ID, entity.getName(), requestPropertyIds);
    setResourceProperty(resource, KERBEROS_DESCRIPTOR_TEXT_PROPERTY_ID, entity.getKerberosDescriptorText(), requestPropertyIds);
  }

  @Override
  public RequestStatus updateResources(Request request, Predicate predicate) throws SystemException,
      UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {
    throw new UnsupportedOperationException("Not yet implemented!");
  }

  @Override
  public RequestStatus deleteResources(Request request, Predicate predicate) throws SystemException,
      UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {

    Set<Resource> setResources = getResources(new RequestImpl(null, null, null, null), predicate);

    for (Resource resource : setResources) {
      final String kerberosDescriptorName =
          (String) resource.getPropertyValue(KERBEROS_DESCRIPTOR_NAME_PROPERTY_ID);
      LOGGER.debug("Deleting resource with name: {}", kerberosDescriptorName);
      kerberosDescriptorDAO.removeByName(kerberosDescriptorName);
    }

    return getRequestStatus(null);
  }

  @Override
  protected Set<String> getPKPropertyIds() {
    return Collections.emptySet();
  }

  private String getRawKerberosDescriptorFromRequest(Request request) throws UnsupportedPropertyException {
    if (request.getRequestInfoProperties() == null ||
        !request.getRequestInfoProperties().containsKey(Request.REQUEST_INFO_BODY_PROPERTY)) {
      LOGGER.error("Could not find the raw request body in the request: {}", request);
      throw new UnsupportedPropertyException(Resource.Type.KerberosDescriptor,
          Collections.singleton(Request.REQUEST_INFO_BODY_PROPERTY));
    }
    return request.getRequestInfoProperties().get(Request.REQUEST_INFO_BODY_PROPERTY);
  }

  private String getNameFromRequest(Request request) throws UnsupportedPropertyException {
    if (request.getProperties() == null || !request.getProperties().iterator().hasNext()) {
      LOGGER.error("There is no {} property id in the request {}", KERBEROS_DESCRIPTOR_NAME_PROPERTY_ID, request);
      throw new UnsupportedPropertyException(Resource.Type.KerberosDescriptor,
          Collections.singleton(KERBEROS_DESCRIPTOR_NAME_PROPERTY_ID));
    }
    return (String) request.getProperties().iterator().next().get(KERBEROS_DESCRIPTOR_NAME_PROPERTY_ID);
  }

}
