/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.server.controller.internal;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.ArrayList;
import java.util.List;

import com.google.inject.Inject;
import org.apache.ambari.server.StaticallyInject;
import org.apache.ambari.server.api.services.parsers.BodyParseException;
import org.apache.ambari.server.controller.spi.RequestStatus;
import org.apache.ambari.server.controller.spi.NoSuchParentResourceException;
import org.apache.ambari.server.controller.spi.NoSuchResourceException;
import org.apache.ambari.server.controller.spi.UnsupportedPropertyException;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.ResourceAlreadyExistsException;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.MpackResponse;
import org.apache.ambari.server.controller.MpackRequest;


import org.apache.ambari.server.controller.utilities.PredicateHelper;
import org.apache.ambari.server.orm.dao.MpackDAO;

import org.apache.ambari.server.orm.entities.MpackEntity;
import org.apache.ambari.server.state.Packlet;

/**
 * ResourceProvider for Mpack instances
 */
@StaticallyInject
public class MpackResourceProvider extends AbstractControllerResourceProvider {

  public static final String MPACK_ID = "MpackInfo/mpack_id";
  public static final String REGISTRY_ID = "MpackInfo/registry_id";
  public static final String MPACK_NAME = "MpackInfo/mpack_name";
  public static final String MPACK_VERSION = "MpackInfo/mpack_version";
  public static final String MPACK_URI = "MpackInfo/mpack_uri";
  public static final String PACKLETS = "MpackInfo/packlets";


  private static Set<String> pkPropertyIds = new HashSet<>(
          Arrays.asList(MPACK_ID));

  /**
   * The property ids for an mpack resource.
   */
  private static final Set<String> PROPERTY_IDS = new HashSet<>();

  /**
   * The key property ids for a mpack resource.
   */
  private static final Map<Resource.Type, String> KEY_PROPERTY_IDS = new HashMap<>();

  @Inject
  protected static MpackDAO mpackDAO;

  static {
    // properties
    PROPERTY_IDS.add(MPACK_ID);
    PROPERTY_IDS.add(REGISTRY_ID);
    PROPERTY_IDS.add(MPACK_NAME);
    PROPERTY_IDS.add(MPACK_VERSION);
    PROPERTY_IDS.add(MPACK_URI);
    PROPERTY_IDS.add(PACKLETS);

    // keys
    KEY_PROPERTY_IDS.put(Resource.Type.Mpack, MPACK_ID);

  }

  MpackResourceProvider(AmbariManagementController controller) {
    super(PROPERTY_IDS, KEY_PROPERTY_IDS, controller);
  }

  @Override
  protected Set<String> getPKPropertyIds() {
    return pkPropertyIds;
  }

  @Override
  public RequestStatus createResources(final Request request)
          throws SystemException, UnsupportedPropertyException,
          ResourceAlreadyExistsException, NoSuchParentResourceException, IllegalArgumentException {
    Set<Resource> associatedResources = new HashSet<>();
    try {
      MpackRequest mpackRequest = getRequest(request);
      if (mpackRequest == null)
        throw new BodyParseException("Please provide " + MPACK_NAME + " ," + MPACK_VERSION + " ," + MPACK_URI);
      MpackResponse response = getManagementController().registerMpack(mpackRequest);
      if (response != null) {
        notifyCreate(Resource.Type.Mpack, request);
        Resource resource = new ResourceImpl(Resource.Type.Mpack);
        resource.setProperty(MPACK_ID, response.getMpackId());
        resource.setProperty(REGISTRY_ID, response.getRegistryId());
        resource.setProperty(MPACK_NAME, response.getMpackName());
        resource.setProperty(MPACK_VERSION, response.getMpackVersion());
        resource.setProperty(MPACK_URI, response.getMpackUri());

        associatedResources.add(resource);
        return getRequestStatus(null, associatedResources);
      }
    } catch (IOException e) {
      e.printStackTrace();
    } catch (BodyParseException e1) {
      e1.printStackTrace();
    }
    return null;
  }

  public MpackRequest getRequest(Request request) {
    MpackRequest mpackRequest = new MpackRequest();
    Set<Map<String, Object>> properties = request.getProperties();
    for (Map propertyMap : properties) {
      //Mpack Download url is either given in the request body or is fetched using the registry id
      if (!propertyMap.containsKey(MPACK_URI) && !propertyMap.containsKey(REGISTRY_ID))
        return null;
      //Fetch Mpack Download Url using the given registry id
      else if (!propertyMap.containsKey(MPACK_URI)) {
        mpackRequest.setRegistryId((Long) propertyMap.get(REGISTRY_ID));
        mpackRequest.setMpackName((String) propertyMap.get(MPACK_NAME));
        mpackRequest.setMpackVersion((String) propertyMap.get(MPACK_VERSION));
      }
      //Directly download the mpack using the given url
      else
        mpackRequest.setMpackUri((String) propertyMap.get(MPACK_URI));
    }
    return mpackRequest;

  }

  @Override
  public Set<Resource> getResources(Request request, Predicate predicate)
          throws SystemException, UnsupportedPropertyException,
          NoSuchResourceException, NoSuchParentResourceException {

    Set<Resource> results = new LinkedHashSet<>();

    //Fetch all mpacks
    if (predicate == null) {
      List<MpackEntity> entities = mpackDAO.findAll();
      if (null == entities) {
        entities = Collections.emptyList();
      }
      for (MpackEntity entity : entities) {
        Resource resource = new ResourceImpl(Resource.Type.Mpack);
        resource.setProperty(MPACK_ID, entity.getMpackId());
        resource.setProperty(MPACK_NAME, entity.getMpackName());
        resource.setProperty(MPACK_VERSION, entity.getMpackVersion());
        resource.setProperty(MPACK_URI, entity.getMpackUri());
        resource.setProperty(REGISTRY_ID, entity.getRegistryId());
        results.add(resource);
      }
    } //Fetch a particular mpack based on id
    else {
      Map<String, Object> propertyMap = new HashMap<>(PredicateHelper.getProperties(predicate));
      if (propertyMap.containsKey(MPACK_ID)) {
        String mpackId = (String) propertyMap.get(MPACK_ID);
        MpackEntity entity = mpackDAO.findById(Long.parseLong((mpackId)));
        Resource resource = new ResourceImpl(Resource.Type.Mpack);
        if (null != entity) {
          resource.setProperty(MPACK_ID, entity.getMpackId());
          resource.setProperty(MPACK_NAME, entity.getMpackName());
          resource.setProperty(MPACK_VERSION, entity.getMpackVersion());
          resource.setProperty(MPACK_URI, entity.getMpackUri());
          resource.setProperty(REGISTRY_ID, entity.getRegistryId());

          ArrayList<Packlet> packletArrayList = getManagementController().getPacklets(entity.getMpackId());
          resource.setProperty(PACKLETS, packletArrayList);
          results.add(resource);
        }
      }
      if (results.isEmpty()) {
        throw new NoSuchResourceException(
                "The requested resource doesn't exist: " + predicate);
      }
    }

    return results;
  }

}

