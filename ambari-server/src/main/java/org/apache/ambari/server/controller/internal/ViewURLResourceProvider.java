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

import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.StaticallyInject;
import org.apache.ambari.server.controller.predicate.EqualsPredicate;
import org.apache.ambari.server.controller.spi.NoSuchParentResourceException;
import org.apache.ambari.server.controller.spi.NoSuchResourceException;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.RequestStatus;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceAlreadyExistsException;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.spi.UnsupportedPropertyException;
import org.apache.ambari.server.orm.dao.ViewURLDAO;
import org.apache.ambari.server.orm.entities.ViewEntity;
import org.apache.ambari.server.orm.entities.ViewInstanceEntity;
import org.apache.ambari.server.orm.entities.ViewURLEntity;
import org.apache.ambari.server.security.authorization.RoleAuthorization;
import org.apache.ambari.server.view.ViewRegistry;
import org.apache.ambari.server.view.validation.ValidationException;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Resource provider for view URLs.
 */
@SuppressWarnings("Duplicates")
@StaticallyInject
public class ViewURLResourceProvider extends AbstractAuthorizedResourceProvider {

  /**
   * view URL property id constants.
   */
  public static final String URL_NAME_PROPERTY_ID                       = "ViewUrlInfo/url_name";
  public static final String URL_SUFFIX_PROPERTY_ID                     = "ViewUrlInfo/url_suffix";
  public static final String VIEW_INSTANCE_VERSION_PROPERTY_ID          = "ViewUrlInfo/view_instance_version";
  public static final String VIEW_INSTANCE_NAME_PROPERTY_ID             = "ViewUrlInfo/view_instance_name";
  public static final String VIEW_INSTANCE_COMMON_NAME_PROPERTY_ID      = "ViewUrlInfo/view_instance_common_name";

  /**
   * The key property ids for a view URL resource.
   */
  private static Map<Resource.Type, String> keyPropertyIds = new HashMap<Resource.Type, String>();
  static {
    keyPropertyIds.put(Resource.Type.ViewURL, URL_NAME_PROPERTY_ID);
  }

  /**
   * The property ids for a view URL resource.
   */
  private static Set<String> propertyIds = new HashSet<String>();
  static {
    propertyIds.add(URL_NAME_PROPERTY_ID);
    propertyIds.add(URL_SUFFIX_PROPERTY_ID);
    propertyIds.add(VIEW_INSTANCE_VERSION_PROPERTY_ID);
    propertyIds.add(VIEW_INSTANCE_NAME_PROPERTY_ID);
    propertyIds.add(VIEW_INSTANCE_COMMON_NAME_PROPERTY_ID);
  }

  @Inject
  private static ViewURLDAO viewURLDAO;

  // ----- Constructors ------------------------------------------------------

  /**
   * Construct a view URL resource provider.
   */

  public ViewURLResourceProvider() {
    super(propertyIds, keyPropertyIds);

    EnumSet<RoleAuthorization> requiredAuthorizations = EnumSet.of(RoleAuthorization.AMBARI_MANAGE_VIEWS);
    setRequiredCreateAuthorizations(requiredAuthorizations);
    setRequiredDeleteAuthorizations(requiredAuthorizations);
    setRequiredUpdateAuthorizations(requiredAuthorizations);
  }


  // ----- ResourceProvider --------------------------------------------------

  @Override
  protected RequestStatus createResourcesAuthorized(Request request)
      throws SystemException, UnsupportedPropertyException,
             ResourceAlreadyExistsException, NoSuchParentResourceException {
    for (Map<String, Object> properties : request.getProperties()) {
      createResources(getCreateCommand(properties));
    }
    notifyCreate(Resource.Type.ViewURL, request);

    return getRequestStatus(null);
  }

  @Override
  public Set<Resource> getResources(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {

    Set<Resource> resources = Sets.newHashSet();
    Set<Map<String, Object>> propertyMaps = getPropertyMaps(predicate);

    for (Map<String, Object> propertyMap : propertyMaps) {
      String urlNameProperty = (String) propertyMap.get(URL_NAME_PROPERTY_ID);
      if (!Strings.isNullOrEmpty(urlNameProperty)) {
        Optional<ViewURLEntity> urlEntity = viewURLDAO.findByName(urlNameProperty);
        if(urlEntity.isPresent()){
          resources.add(toResource(urlEntity.get()));
        }
      } else {
        List<ViewURLEntity> urlEntities = viewURLDAO.findAll();
        for (ViewURLEntity urlEntity : urlEntities) {
          resources.add(toResource(urlEntity));
        }
      }
    }

    return resources;
  }

  @Override
  protected RequestStatus updateResourcesAuthorized(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {

    Iterator<Map<String,Object>> iterator = request.getProperties().iterator();
    if (iterator.hasNext()) {
      for (Map<String, Object> propertyMap : getPropertyMaps(iterator.next(), predicate)) {
        modifyResources(getUpdateCommand(propertyMap));
      }
    }
    notifyUpdate(Resource.Type.ViewInstance, request, predicate);

    return getRequestStatus(null);
  }

  @Override
  protected RequestStatus deleteResourcesAuthorized(Request request, Predicate predicate)
          throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {

      modifyResources(getDeleteCommand(predicate));
      notifyDelete(Resource.Type.ViewInstance, predicate);
      return getRequestStatus(null);

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


  // ----- helper methods ----------------------------------------------------

  /**
   * Converts a View URL entity into a Resource.
   * @param viewURLEntity
   * @return A resource object representing the URL
     */
  protected Resource toResource(ViewURLEntity viewURLEntity) {
    Resource   resource   = new ResourceImpl(Resource.Type.ViewURL);

    resource.setProperty(URL_NAME_PROPERTY_ID,viewURLEntity.getUrlName());
    resource.setProperty(URL_SUFFIX_PROPERTY_ID,viewURLEntity.getUrlSuffix());
    ViewInstanceEntity viewInstanceEntity = viewURLEntity.getViewInstanceEntity();
    if(viewInstanceEntity == null)
      return resource;
    ViewEntity viewEntity = viewInstanceEntity.getViewEntity();
    String viewName = viewEntity.getCommonName();
    String version  = viewEntity.getVersion();
    String name     = viewInstanceEntity.getName();
    resource.setProperty(VIEW_INSTANCE_NAME_PROPERTY_ID,name);
    resource.setProperty(VIEW_INSTANCE_VERSION_PROPERTY_ID,version);
    resource.setProperty(VIEW_INSTANCE_COMMON_NAME_PROPERTY_ID,viewName);
    return resource;
  }

  /**
   * Converts the incoming request into a View URL
   * @param properties The property map
   * @return The view URL
   * @throws AmbariException
     */
  private ViewURLEntity toEntity(Map<String, Object> properties) throws AmbariException {
    String name = (String) properties.get(URL_NAME_PROPERTY_ID);
    if (name == null || name.isEmpty()) {
      throw new IllegalArgumentException("The View URL is a required property.");
    }

    String suffix = (String) properties.get(URL_SUFFIX_PROPERTY_ID);
    String commonName = (String) properties.get(VIEW_INSTANCE_COMMON_NAME_PROPERTY_ID);
    String instanceName = (String) properties.get(VIEW_INSTANCE_NAME_PROPERTY_ID);
    String instanceVersion = (String) properties.get(VIEW_INSTANCE_VERSION_PROPERTY_ID);
    ViewRegistry viewRegistry = ViewRegistry.getInstance();
    ViewInstanceEntity instanceEntity = viewRegistry.getInstanceDefinition(commonName, instanceVersion, instanceName);

    ViewURLEntity urlEntity = new ViewURLEntity();
    urlEntity.setUrlName(name);
    urlEntity.setUrlSuffix(suffix);
    urlEntity.setViewInstanceEntity(instanceEntity);

    return urlEntity;

  }

  /**
   * Get the command to create the View URL
   * @param properties
   * @return A command to create the View URL instance
     */
  private Command<Void> getCreateCommand(final Map<String, Object> properties) {
    return new Command<Void>() {
      @Override
      public Void invoke() throws AmbariException {
        ViewRegistry       viewRegistry   = ViewRegistry.getInstance();

        ViewURLEntity urlEntity = toEntity(properties);

        ViewInstanceEntity viewInstanceEntity = urlEntity.getViewInstanceEntity();
        ViewEntity viewEntity = viewInstanceEntity.getViewEntity();
        String     viewName   = viewEntity.getCommonName();
        String     version    = viewEntity.getVersion();
        ViewEntity view       = viewRegistry.getDefinition(viewName, version);

        if ( view == null ) {
          throw new IllegalStateException("The view " + viewName + " is not registered.");
        }

        // the view must be in the DEPLOYED state to create an instance
        if (!view.isDeployed()) {
          throw new IllegalStateException("The view " + viewName + " is not loaded.");
        }

        ViewURLEntity viewUrl = viewInstanceEntity.getViewUrl();
        Optional<ViewURLEntity> savedUrl = viewURLDAO.findByName(urlEntity.getUrlName());

        if(savedUrl.isPresent()){
          throw new AmbariException("This view URL name exists, URL names should be unique");
        }

        if(viewUrl != null) {
          throw new AmbariException("The view instance selected already has a linked URL");
        }

        viewURLDAO.save(urlEntity);
        // Update the view with the URL
        viewInstanceEntity.setViewUrl(urlEntity);
        try {
          viewRegistry.updateViewInstance(viewInstanceEntity);
        } catch (ValidationException e) {
          throw new IllegalArgumentException(e.getMessage(), e);
        } catch (org.apache.ambari.view.SystemException e) {
          throw new AmbariException("Caught exception trying to update view URL.", e);
        }
        viewRegistry.updateView(viewInstanceEntity);
        return null;
      }
    };
  }

  /**
   * Get the command to update the View URL
   * @param properties
   * @return The update command
     */
  private Command<Void> getUpdateCommand(final Map<String, Object> properties) {
    return new Command<Void>() {
      @Override
      public Void invoke() throws AmbariException {
        ViewRegistry registry = ViewRegistry.getInstance();
        String name = (String) properties.get(URL_NAME_PROPERTY_ID);
        String suffix = (String) properties.get(URL_SUFFIX_PROPERTY_ID);
        Optional<ViewURLEntity> entity = viewURLDAO.findByName(name);
        if(!entity.isPresent()){
          throw new AmbariException("URL with name "+ name +"was not found");
        }
        entity.get().setUrlSuffix(suffix);
        viewURLDAO.update(entity.get());
        // update the instance to sync with the DB values
        ViewInstanceEntity viewInstanceEntity = entity.get().getViewInstanceEntity();
        try {
          registry.updateViewInstance(viewInstanceEntity);
        } catch (ValidationException e) {
          throw new IllegalArgumentException(e.getMessage(), e);
        } catch (org.apache.ambari.view.SystemException e) {
          throw new AmbariException("Caught exception trying to update view URL.", e);
        }
         registry.updateView(viewInstanceEntity);
        return null;
      }
    };
  }

  /**
   * Get the command to delete the View URL
   * @param predicate
   * @return The delete command
     */
  private Command<Void> getDeleteCommand(final Predicate predicate) {
    return new Command<Void>() {
      @Override
      public Void invoke() throws AmbariException {
        ViewRegistry viewRegistry = ViewRegistry.getInstance();
        Comparable deletedUrl = ((EqualsPredicate) predicate).getValue();
        String toDelete = deletedUrl.toString();
        Optional<ViewURLEntity> urlEntity = viewURLDAO.findByName(toDelete);
        if(!urlEntity.isPresent()){
          throw new AmbariException("The URL "+ toDelete +"does not exist");
        }
        ViewInstanceEntity viewInstanceEntity = urlEntity.get().getViewInstanceEntity();
        if(viewInstanceEntity != null) {
          viewInstanceEntity.clearUrl();

          try {
            viewRegistry.updateViewInstance(viewInstanceEntity);
          } catch (ValidationException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
          } catch (org.apache.ambari.view.SystemException e) {
            throw new AmbariException("Caught exception trying to update view URL.", e);
          }

          viewRegistry.updateView(viewInstanceEntity);
        }
        // Delete the url
        urlEntity.get().clearEntity();
        viewURLDAO.delete(urlEntity.get());
        return null;
      }
    };
  }

}
