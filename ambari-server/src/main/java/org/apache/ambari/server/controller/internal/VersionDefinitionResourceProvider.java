/*
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

import java.net.URL;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.StaticallyInject;
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
import org.apache.ambari.server.orm.dao.RepositoryVersionDAO;
import org.apache.ambari.server.orm.dao.StackDAO;
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
import org.apache.ambari.server.orm.entities.StackEntity;
import org.apache.ambari.server.security.authorization.ResourceType;
import org.apache.ambari.server.security.authorization.RoleAuthorization;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.StackInfo;
import org.apache.ambari.server.state.repository.VersionDefinitionXml;
import org.apache.ambari.server.state.stack.upgrade.RepositoryVersionHelper;
import org.apache.commons.io.IOUtils;

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * The {@link VersionDefinitionResourceProvider} class deals with managing Version Definition
 * files.
 */
@StaticallyInject
public class VersionDefinitionResourceProvider extends AbstractAuthorizedResourceProvider {

  public static final String VERSION_DEF                             = "VersionDefinition";
  protected static final String VERSION_DEF_ID                       = "VersionDefinition/id";

  public static final String VERSION_DEF_STACK_NAME                  = "VersionDefinition/stack_name";
  public static final String VERSION_DEF_STACK_VERSION               = "VersionDefinition/stack_version";

  protected static final String VERSION_DEF_TYPE_PROPERTY_ID         = "VersionDefinition/type";
  protected static final String VERSION_DEF_DEFINITION_URL           = "VersionDefinition/version_url";
  protected static final String VERSION_DEF_FULL_VERSION             = "VersionDefinition/repository_version";
  protected static final String VERSION_DEF_RELEASE_VERSION          = "VersionDefinition/release/version";
  protected static final String VERSION_DEF_RELEASE_BUILD            = "VersionDefinition/release/build";
  protected static final String VERSION_DEF_RELEASE_NOTES            = "VersionDefinition/release/notes";
  protected static final String VERSION_DEF_RELEASE_COMPATIBLE_WITH  = "VersionDefinition/release/compatible_with";
    protected static final String VERSION_DEF_AVAILABLE_SERVICES     = "VersionDefinition/services";

  @Inject
  private static RepositoryVersionDAO s_repoVersionDAO;

  @Inject
  private static Provider<AmbariMetaInfo> s_metaInfo;

  @Inject
  private static Provider<RepositoryVersionHelper> s_repoVersionHelper;

  @Inject
  private static StackDAO s_stackDAO;

  /**
   * Key property ids
   */
  private static final Set<String> PK_PROPERTY_IDS = Sets.newHashSet(
      VERSION_DEF_ID,
      VERSION_DEF_STACK_NAME,
      VERSION_DEF_STACK_VERSION,
      VERSION_DEF_FULL_VERSION);

  /**
   * The property ids for an version definition resource.
   */
  private static final Set<String> PROPERTY_IDS = Sets.newHashSet(
      VERSION_DEF_ID,
      VERSION_DEF_TYPE_PROPERTY_ID,
      VERSION_DEF_DEFINITION_URL,
      VERSION_DEF_FULL_VERSION,
      VERSION_DEF_RELEASE_NOTES,
      VERSION_DEF_RELEASE_COMPATIBLE_WITH,
      VERSION_DEF_RELEASE_VERSION,
      VERSION_DEF_RELEASE_BUILD,
      VERSION_DEF_AVAILABLE_SERVICES);

  /**
   * The key property ids for an version definition resource.
   */
  private static final Map<Resource.Type, String> KEY_PROPERTY_IDS = new HashMap<Resource.Type, String>();

  static {
    KEY_PROPERTY_IDS.put(Resource.Type.VersionDefinition, VERSION_DEF_ID);
  }

  /**
   * Constructor.
   */
  VersionDefinitionResourceProvider() {
    super(PROPERTY_IDS, KEY_PROPERTY_IDS);

    setRequiredCreateAuthorizations(EnumSet.of(RoleAuthorization.AMBARI_MANAGE_STACK_VERSIONS));

    setRequiredGetAuthorizations(EnumSet.of(
        RoleAuthorization.AMBARI_MANAGE_STACK_VERSIONS));
  }

  @Override
  protected RequestStatus createResourcesAuthorized(final Request request)
      throws SystemException,
      UnsupportedPropertyException, ResourceAlreadyExistsException,
      NoSuchParentResourceException {

    Set<Map<String, Object>> requestProperties = request.getProperties();

    if (requestProperties.size() > 1) {
      throw new SystemException("Cannot process more than one file per request");
    }

    final Map<String, Object> properties = requestProperties.iterator().next();
    if (!properties.containsKey(VERSION_DEF_DEFINITION_URL)) {
      throw new SystemException(String.format("%s is required", VERSION_DEF_DEFINITION_URL));
    }

    RepositoryVersionEntity entity = createResources(new Command<RepositoryVersionEntity>() {
      @Override
      public RepositoryVersionEntity invoke() throws AmbariException {

        String definitionUrl = (String) properties.get(VERSION_DEF_DEFINITION_URL);

        RepositoryVersionEntity entity = toRepositoryVersionEntity(definitionUrl);

        RepositoryVersionResourceProvider.validateRepositoryVersion(s_repoVersionDAO,
            s_metaInfo.get(), entity);

        s_repoVersionDAO.create(entity);

        return entity;
      }
    });

    notifyCreate(Resource.Type.VersionDefinition, request);

    RequestStatusImpl status = new RequestStatusImpl(null,
        Collections.singleton(toResource(entity, Collections.<String>emptySet())));

    return status;
  }

  @Override
  public Set<Resource> getResources(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException,
      NoSuchResourceException, NoSuchParentResourceException {

    Set<Resource> results = new HashSet<Resource>();
    Set<String> requestPropertyIds = getRequestPropertyIds(request, predicate);

    if (null == predicate){
      List<RepositoryVersionEntity> versions = s_repoVersionDAO.findAllDefinitions();

      for (RepositoryVersionEntity entity : versions) {
        results.add(toResource(entity, requestPropertyIds));
      }

    } else {
      for (Map<String, Object> propertyMap : getPropertyMaps(predicate)) {
        String id = (String) propertyMap.get(VERSION_DEF_ID);
        if (null == id) {
          continue;
        }

        RepositoryVersionEntity entity = s_repoVersionDAO.findByPK(Long.parseLong(id));
        if (null != entity) {
          results.add(toResource(entity, requestPropertyIds));
        }
      }
    }
    return results;
  }

  @Override
  protected RequestStatus updateResourcesAuthorized(final Request request, Predicate predicate)
    throws SystemException, UnsupportedPropertyException,
      NoSuchResourceException, NoSuchParentResourceException {

    throw new SystemException("Cannot update Version Definitions");
  }

  @Override
  protected RequestStatus deleteResourcesAuthorized(Predicate predicate)
      throws SystemException, UnsupportedPropertyException,
      NoSuchResourceException, NoSuchParentResourceException {
    throw new SystemException("Cannot delete Version Definitions");
  }

  @Override
  protected Set<String> getPKPropertyIds() {
    return PK_PROPERTY_IDS;
  }

  @Override
  protected ResourceType getResourceType(Request request, Predicate predicate) {
    return ResourceType.AMBARI;
  }

  /**
   * Transforms a XML version defintion to an entity
   *
   * @param definitionUrl the String URL for loading
   * @return constructed entity
   * @throws AmbariException if some properties are missing or json has incorrect structure
   */
  protected RepositoryVersionEntity toRepositoryVersionEntity(String definitionUrl) throws AmbariException {
    final VersionDefinitionXml xml;
    final String xmlString;
    try {
      URL url = new URL(definitionUrl);

      xmlString = IOUtils.toString(url.openStream(), "UTF-8");

      xml = VersionDefinitionXml.load(xmlString);
    } catch (Exception e) {
      String err = String.format("Could not load url from %s.  %s",
          definitionUrl, e.getMessage());
      throw new AmbariException(err, e);
    }

    // !!! TODO validate parsed object graph

    RepositoryVersionEntity entity = new RepositoryVersionEntity();

    StackId stackId = new StackId(xml.release.stackId);

    StackEntity stackEntity = s_stackDAO.find(stackId.getStackName(), stackId.getStackVersion());

    entity.setStack(stackEntity);
    entity.setOperatingSystems(s_repoVersionHelper.get().serializeOperatingSystems(
        xml.repositoryInfo.getRepositories()));
    entity.setVersion(xml.release.getFullVersion());
    entity.setDisplayName(stackId, xml.release);
    entity.setType(xml.release.repositoryType);
    entity.setVersionUrl(definitionUrl);
    entity.setVersionXml(xmlString);
    entity.setVersionXsd(xml.xsdLocation);

    return entity;
  }

  /**
   * Convert the given {@link RepositoryVersionEntity} to a {@link Resource}.
   *
   * @param entity
   *          the entity to convert.
   * @param requestedIds
   *          the properties that were requested or {@code null} for all.
   * @return the resource representation of the entity (never {@code null}).
   */
  private Resource toResource(RepositoryVersionEntity entity, Set<String> requestedIds)
      throws SystemException {

    Resource resource = new ResourceImpl(Resource.Type.VersionDefinition);

    resource.setProperty(VERSION_DEF_ID, entity.getId());

    VersionDefinitionXml xml = null;
    try {
      xml = entity.getRepositoryXml();
    } catch (Exception e) {
      String msg = String.format("Could not load version definition %s", entity.getId());
      throw new SystemException(msg, e);
    }

    StackId stackId = new StackId(xml.release.stackId);

    // !!! these are needed for href
    resource.setProperty(VERSION_DEF_STACK_NAME, stackId.getStackName());
    resource.setProperty(VERSION_DEF_STACK_VERSION, stackId.getStackVersion());

    setResourceProperty(resource, VERSION_DEF_TYPE_PROPERTY_ID, entity.getType(), requestedIds);
    setResourceProperty(resource, VERSION_DEF_DEFINITION_URL, entity.getVersionUrl(), requestedIds);
    setResourceProperty(resource, VERSION_DEF_FULL_VERSION, entity.getVersion(), requestedIds);
    setResourceProperty(resource, VERSION_DEF_RELEASE_BUILD, xml.release.build, requestedIds);
    setResourceProperty(resource, VERSION_DEF_RELEASE_COMPATIBLE_WITH, xml.release.compatibleWith, requestedIds);
    setResourceProperty(resource, VERSION_DEF_RELEASE_NOTES, xml.release.releaseNotes, requestedIds);
    setResourceProperty(resource, VERSION_DEF_RELEASE_VERSION, xml.release.version, requestedIds);

      // !!! future do something with the manifest

    if (isPropertyRequested(VERSION_DEF_AVAILABLE_SERVICES, requestedIds)) {
      StackInfo stack = null;
      try {
        stack = s_metaInfo.get().getStack(stackId.getStackName(), stackId.getStackVersion());
      } catch (AmbariException e) {
        throw new SystemException(String.format("Could not load stack %s", stackId));
      }

      setResourceProperty(resource, VERSION_DEF_AVAILABLE_SERVICES, xml.getAvailableServices(stack), requestedIds);
    }

    return resource;
  }


}
