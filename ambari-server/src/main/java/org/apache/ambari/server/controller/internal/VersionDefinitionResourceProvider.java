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

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.StaticallyInject;
import org.apache.ambari.server.api.resources.OperatingSystemResourceDefinition;
import org.apache.ambari.server.api.resources.RepositoryResourceDefinition;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.configuration.ComponentSSLConfiguration;
import org.apache.ambari.server.configuration.Configuration;
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
import org.apache.ambari.server.orm.dao.RepositoryVersionDAO;
import org.apache.ambari.server.orm.dao.StackDAO;
import org.apache.ambari.server.orm.entities.OperatingSystemEntity;
import org.apache.ambari.server.orm.entities.RepositoryEntity;
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
import org.apache.ambari.server.orm.entities.StackEntity;
import org.apache.ambari.server.security.authorization.ResourceType;
import org.apache.ambari.server.security.authorization.RoleAuthorization;
import org.apache.ambari.server.state.RepositoryType;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.StackInfo;
import org.apache.ambari.server.state.repository.VersionDefinitionXml;
import org.apache.ambari.server.state.stack.upgrade.RepositoryVersionHelper;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.JsonNodeFactory;
import org.codehaus.jackson.node.ObjectNode;

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
  public static final String VERSION_DEF_BASE64_PROPERTY             = "version_base64";
  public static final String VERSION_DEF_STACK_NAME                  = "VersionDefinition/stack_name";
  public static final String VERSION_DEF_STACK_VERSION               = "VersionDefinition/stack_version";

  protected static final String VERSION_DEF_ID                       = "VersionDefinition/id";
  protected static final String VERSION_DEF_TYPE_PROPERTY_ID         = "VersionDefinition/type";
  protected static final String VERSION_DEF_DEFINITION_URL           = "VersionDefinition/version_url";
  protected static final String VERSION_DEF_AVAILABLE_DEFINITION     = "VersionDefinition/available";
  protected static final String VERSION_DEF_DEFINITION_BASE64        = PropertyHelper.getPropertyId(VERSION_DEF, VERSION_DEF_BASE64_PROPERTY);

  protected static final String VERSION_DEF_FULL_VERSION             = "VersionDefinition/repository_version";
  protected static final String VERSION_DEF_RELEASE_VERSION          = "VersionDefinition/release/version";
  protected static final String VERSION_DEF_RELEASE_BUILD            = "VersionDefinition/release/build";
  protected static final String VERSION_DEF_RELEASE_NOTES            = "VersionDefinition/release/notes";
  protected static final String VERSION_DEF_RELEASE_COMPATIBLE_WITH  = "VersionDefinition/release/compatible_with";
  protected static final String VERSION_DEF_AVAILABLE_SERVICES       = "VersionDefinition/services";
  protected static final String VERSION_DEF_STACK_SERVICES           = "VersionDefinition/stack_services";
  protected static final String SHOW_AVAILABLE                       = "VersionDefinition/show_available";

  public static final String SUBRESOURCE_OPERATING_SYSTEMS_PROPERTY_ID  = new OperatingSystemResourceDefinition().getPluralName();

  @Inject
  private static RepositoryVersionDAO s_repoVersionDAO;

  @Inject
  private static Provider<AmbariMetaInfo> s_metaInfo;

  @Inject
  private static Provider<RepositoryVersionHelper> s_repoVersionHelper;

  @Inject
  private static StackDAO s_stackDAO;

  @Inject
  private static Configuration s_configuration;

  /**
   * Key property ids
   */
  private static final Set<String> PK_PROPERTY_IDS = Sets.newHashSet(
      VERSION_DEF_ID,
      VERSION_DEF_STACK_NAME,
      VERSION_DEF_STACK_VERSION,
      VERSION_DEF_FULL_VERSION
      );

  /**
   * The property ids for an version definition resource.
   */
  private static final Set<String> PROPERTY_IDS = Sets.newHashSet(
      VERSION_DEF_ID,
      VERSION_DEF_TYPE_PROPERTY_ID,
      VERSION_DEF_DEFINITION_URL,
      VERSION_DEF_DEFINITION_BASE64,
      VERSION_DEF_AVAILABLE_DEFINITION,
      VERSION_DEF_STACK_NAME,
      VERSION_DEF_STACK_VERSION,
      VERSION_DEF_FULL_VERSION,
      VERSION_DEF_RELEASE_NOTES,
      VERSION_DEF_RELEASE_COMPATIBLE_WITH,
      VERSION_DEF_RELEASE_VERSION,
      VERSION_DEF_RELEASE_BUILD,
      VERSION_DEF_AVAILABLE_SERVICES,
      VERSION_DEF_STACK_SERVICES,
      SUBRESOURCE_OPERATING_SYSTEMS_PROPERTY_ID,
      SHOW_AVAILABLE);

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
    setRequiredGetAuthorizations(EnumSet.of(RoleAuthorization.AMBARI_MANAGE_STACK_VERSIONS));
  }

  @Override
  protected RequestStatus createResourcesAuthorized(final Request request)
      throws SystemException,
      UnsupportedPropertyException, ResourceAlreadyExistsException,
      NoSuchParentResourceException {

    Set<Map<String, Object>> requestProperties = request.getProperties();

    if (requestProperties.size() > 1) {
      throw new IllegalArgumentException("Cannot process more than one file per request");
    }

    final Map<String, Object> properties = requestProperties.iterator().next();

    if (!properties.containsKey(VERSION_DEF_DEFINITION_URL) &&
        !properties.containsKey(VERSION_DEF_DEFINITION_BASE64) &&
        !properties.containsKey(VERSION_DEF_AVAILABLE_DEFINITION)) {
      throw new IllegalArgumentException(String.format("Creation method is not known.  %s or %s is required or upload the file directly",
          VERSION_DEF_DEFINITION_URL, VERSION_DEF_AVAILABLE_DEFINITION));
    }

    if (properties.containsKey(VERSION_DEF_DEFINITION_URL) && properties.containsKey(VERSION_DEF_DEFINITION_BASE64)) {
      throw new IllegalArgumentException(String.format("Specify ONLY the url with %s or upload the file directly",
          VERSION_DEF_DEFINITION_URL));
    }

    final boolean dryRun = request.isDryRunRequest();

    XmlHolder xmlHolder = createResources(new Command<XmlHolder>() {
      @Override
      public XmlHolder invoke() throws AmbariException {

        String definitionUrl = (String) properties.get(VERSION_DEF_DEFINITION_URL);
        String definitionBase64 = (String) properties.get(VERSION_DEF_DEFINITION_BASE64);
        String definitionName = (String) properties.get(VERSION_DEF_AVAILABLE_DEFINITION);

        XmlHolder holder = null;
        if (null != definitionUrl) {
          holder = loadXml(definitionUrl);
        } else if (null != definitionBase64) {
          holder = loadXml(Base64.decodeBase64(definitionBase64));
        } else if (null != definitionName) {
          VersionDefinitionXml xml = s_metaInfo.get().getVersionDefinition(definitionName);
          if (null == xml) {
            throw new AmbariException(String.format("Version %s not found", definitionName));
          }

          holder = new XmlHolder();
          holder.xml = xml;
          try {
            holder.xmlString = xml.toXml();
          } catch (Exception e) {
            throw new AmbariException(String.format("The available repository %s does not serialize", definitionName));
          }

        } else {
          throw new AmbariException("Cannot determine creation method");
        }

        toRepositoryVersionEntity(holder);

        if (!dryRun) {
          RepositoryVersionResourceProvider.validateRepositoryVersion(s_repoVersionDAO,
              s_metaInfo.get(), holder.entity);
        }

        checkForParent(holder);

        if (!dryRun) {
          s_repoVersionDAO.create(holder.entity);
        }

        return holder;
      }
    });

    final Resource res;

    if (dryRun) {
      // !!! dry runs imply that the whole entity should be provided.  this is usually
      // done via sub-resources, but that model breaks down since we don't have a saved
      // entity yet
      Set<String> ids = Sets.newHashSet(
        VERSION_DEF_TYPE_PROPERTY_ID,
        VERSION_DEF_FULL_VERSION,
        VERSION_DEF_RELEASE_BUILD,
        VERSION_DEF_RELEASE_COMPATIBLE_WITH,
        VERSION_DEF_RELEASE_NOTES,
        VERSION_DEF_RELEASE_VERSION,
        VERSION_DEF_AVAILABLE_SERVICES,
        VERSION_DEF_STACK_SERVICES);

      res = toResource(null, xmlHolder.xml, ids, false);

      addSubresources(res, xmlHolder.entity);
    } else {
      res = toResource(xmlHolder.entity, Collections.<String>emptySet());
      notifyCreate(Resource.Type.VersionDefinition, request);
    }

    RequestStatusImpl status = new RequestStatusImpl(null,
        Collections.singleton(res));

    return status;
  }

  @Override
  public Set<Resource> getResources(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException,
      NoSuchResourceException, NoSuchParentResourceException {

    Set<Resource> results = new HashSet<Resource>();
    Set<String> requestPropertyIds = getRequestPropertyIds(request, predicate);

    Set<Map<String, Object>> propertyMaps = getPropertyMaps(predicate);

    if (propertyMaps.isEmpty()) {
      List<RepositoryVersionEntity> versions = s_repoVersionDAO.findAllDefinitions();

      for (RepositoryVersionEntity entity : versions) {
        results.add(toResource(entity, requestPropertyIds));
      }
    } else {
      for (Map<String, Object> propertyMap : propertyMaps) {

        if (propertyMap.containsKey(SHOW_AVAILABLE) &&
            Boolean.parseBoolean(propertyMap.get(SHOW_AVAILABLE).toString())) {

          for (Entry<String, VersionDefinitionXml> entry : s_metaInfo.get().getVersionDefinitions().entrySet()) {
            results.add(toResource(entry.getKey(), entry.getValue(), requestPropertyIds, true));
          }

        } else {
          String id = (String) propertyMap.get(VERSION_DEF_ID);

          if (null != id) {
            if (NumberUtils.isDigits(id)) {

              RepositoryVersionEntity entity = s_repoVersionDAO.findByPK(Long.parseLong(id));
              if (null != entity) {
                results.add(toResource(entity, requestPropertyIds));
              }
            } else {
              VersionDefinitionXml xml = s_metaInfo.get().getVersionDefinition(id);

              if (null == xml) {
                throw new NoSuchResourceException(String.format("Could not find version %s",
                    id));
              }
              results.add(toResource(id, xml, requestPropertyIds, true));

            }
          } else {
            List<RepositoryVersionEntity> versions = s_repoVersionDAO.findAllDefinitions();

            for (RepositoryVersionEntity entity : versions) {
              results.add(toResource(entity, requestPropertyIds));
            }
          }

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

  /**
   * In the case of a patch, check if there is a parent repo.
   */
  private void checkForParent(XmlHolder holder) throws AmbariException {
    RepositoryVersionEntity entity = holder.entity;
    if (entity.getType() != RepositoryType.PATCH) {
      return;
    }

    List<RepositoryVersionEntity> entities = s_repoVersionDAO.findByStack(entity.getStackId());
    if (entities.isEmpty()) {
      throw new IllegalArgumentException(String.format("Patch %s was uploaded, but there are no repositories for %s",
          entity.getVersion(), entity.getStackId().toString()));
    }

    List<RepositoryVersionEntity> matching = new ArrayList<>();

    boolean emptyCompatible = StringUtils.isBlank(holder.xml.release.compatibleWith);

    for (RepositoryVersionEntity candidate : entities) {
      String baseVersion = candidate.getVersion();
      if (baseVersion.lastIndexOf('-') > -1) {
        baseVersion = baseVersion.substring(0,  baseVersion.lastIndexOf('-'));
      }

      if (emptyCompatible) {
        if (baseVersion.equals(holder.xml.release.version)) {
          matching.add(candidate);
        }
      } else {
        if (baseVersion.matches(holder.xml.release.compatibleWith)) {
          matching.add(candidate);
        }
      }
    }

    if (matching.isEmpty()) {
      String format = "No versions matched pattern %s";

      throw new IllegalArgumentException(String.format(format,
          emptyCompatible ? holder.xml.release.version : holder.xml.release.compatibleWith));
    } else if (matching.size() > 1) {
      Set<String> versions= new HashSet<>();
      for (RepositoryVersionEntity match : matching) {
        versions.add(match.getVersion());
      }

      throw new IllegalArgumentException(String.format("More than one repository matches patch %s: %s",
          entity.getVersion(), StringUtils.join(versions, ", ")));
    }

    RepositoryVersionEntity parent = matching.get(0);

    entity.setParent(parent);
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
   * Load the xml data from a posted Base64 stream
   * @param decoded the decoded Base64 data
   * @return the XmlHolder instance
   * @throws AmbariException
   */
  private XmlHolder loadXml(byte[] decoded) {
    XmlHolder holder = new XmlHolder();

    try {
      holder.xmlString = new String(decoded, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      holder.xmlString = new String(decoded);
    }

    try {
      holder.xml = VersionDefinitionXml.load(holder.xmlString);
    } catch (Exception e) {
      throw new IllegalArgumentException(e);
    }

    return holder;
  }

  /**
   * Load the xml data from a url
   * @param definitionUrl
   * @return the XmlHolder instance
   * @throws AmbariException
   */
  private XmlHolder loadXml(String definitionUrl) throws AmbariException {
    XmlHolder holder = new XmlHolder();
    holder.url = definitionUrl;

    int connectTimeout = s_configuration.getVersionDefinitionConnectTimeout();
    int readTimeout = s_configuration.getVersionDefinitionReadTimeout();

    try {
      URI uri = new URI(definitionUrl);
      InputStream stream = null;

      if (uri.getScheme().equalsIgnoreCase("file")) {
        stream = uri.toURL().openStream();
      } else {
        URLStreamProvider provider = new URLStreamProvider(connectTimeout, readTimeout,
            ComponentSSLConfiguration.instance());

        stream = provider.readFrom(definitionUrl);
      }

      holder.xmlString = IOUtils.toString(stream, "UTF-8");
      holder.xml = VersionDefinitionXml.load(holder.xmlString);
    } catch (Exception e) {
      String err = String.format("Could not load url from %s.  %s",
          definitionUrl, e.getMessage());
      throw new AmbariException(err, e);
    }

    return holder;
  }

  /**
   * Transforms a XML version defintion to an entity
   *
   * @throws AmbariException if some properties are missing or json has incorrect structure
   */
  protected void toRepositoryVersionEntity(XmlHolder holder) throws AmbariException {

    // !!! TODO validate parsed object graph

    RepositoryVersionEntity entity = new RepositoryVersionEntity();

    StackId stackId = new StackId(holder.xml.release.stackId);

    StackEntity stackEntity = s_stackDAO.find(stackId.getStackName(), stackId.getStackVersion());

    entity.setStack(stackEntity);
    entity.setOperatingSystems(s_repoVersionHelper.get().serializeOperatingSystems(
        holder.xml.repositoryInfo.getRepositories()));
    entity.setVersion(holder.xml.release.getFullVersion());
    entity.setDisplayName(stackId, holder.xml.release);
    entity.setType(holder.xml.release.repositoryType);
    entity.setVersionUrl(holder.url);
    entity.setVersionXml(holder.xmlString);
    entity.setVersionXsd(holder.xml.xsdLocation);

    holder.entity = entity;
  }

  private Resource toResource(String id, VersionDefinitionXml xml, Set<String> requestedIds, boolean fromAvailable) throws SystemException {

    Resource resource = new ResourceImpl(Resource.Type.VersionDefinition);
    resource.setProperty(VERSION_DEF_ID, id);
    if (fromAvailable) {
      resource.setProperty(SHOW_AVAILABLE, Boolean.TRUE);
    }

    StackId stackId = new StackId(xml.release.stackId);

    // !!! these are needed for href
    resource.setProperty(VERSION_DEF_STACK_NAME, stackId.getStackName());
    resource.setProperty(VERSION_DEF_STACK_VERSION, stackId.getStackVersion());

    StackInfo stack = null;
    try {
      stack = s_metaInfo.get().getStack(stackId.getStackName(), stackId.getStackVersion());
    } catch (AmbariException e) {
      throw new SystemException(String.format("Could not load stack %s", stackId));
    }

    setResourceProperty(resource, VERSION_DEF_TYPE_PROPERTY_ID, xml.release.repositoryType, requestedIds);
    setResourceProperty(resource, VERSION_DEF_FULL_VERSION, xml.release.version, requestedIds);
    setResourceProperty(resource, VERSION_DEF_RELEASE_BUILD, xml.release.build, requestedIds);
    setResourceProperty(resource, VERSION_DEF_RELEASE_COMPATIBLE_WITH, xml.release.compatibleWith, requestedIds);
    setResourceProperty(resource, VERSION_DEF_RELEASE_NOTES, xml.release.releaseNotes, requestedIds);
    setResourceProperty(resource, VERSION_DEF_RELEASE_VERSION, xml.release.version, requestedIds);

    setResourceProperty(resource, VERSION_DEF_AVAILABLE_SERVICES, xml.getAvailableServices(stack), requestedIds);
    setResourceProperty(resource, VERSION_DEF_STACK_SERVICES, xml.getStackServices(stack), requestedIds);

    return resource;
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

    StackInfo stack = null;

    if (isPropertyRequested(VERSION_DEF_AVAILABLE_SERVICES, requestedIds) ||
        isPropertyRequested(VERSION_DEF_STACK_SERVICES, requestedIds)) {
      try {
        stack = s_metaInfo.get().getStack(stackId.getStackName(), stackId.getStackVersion());
      } catch (AmbariException e) {
        throw new SystemException(String.format("Could not load stack %s", stackId));
      }
    }

    if (null != stack) {
      setResourceProperty(resource, VERSION_DEF_AVAILABLE_SERVICES, xml.getAvailableServices(stack), requestedIds);
      setResourceProperty(resource, VERSION_DEF_STACK_SERVICES, xml.getStackServices(stack), requestedIds);
    }

    return resource;
  }

  /**
   * Provide the dry-run entity with fake sub-resources.  These are not queryable by normal API.
   */
  private void addSubresources(Resource res, RepositoryVersionEntity entity) {
    JsonNodeFactory factory = JsonNodeFactory.instance;

    ArrayNode subs = factory.arrayNode();

    for (OperatingSystemEntity os : entity.getOperatingSystems()) {
      ObjectNode osBase = factory.objectNode();

      ObjectNode osElement = factory.objectNode();
      osElement.put(PropertyHelper.getPropertyName(OperatingSystemResourceProvider.OPERATING_SYSTEM_AMBARI_MANAGED_REPOS),
          os.isAmbariManagedRepos());
      osElement.put(PropertyHelper.getPropertyName(OperatingSystemResourceProvider.OPERATING_SYSTEM_OS_TYPE_PROPERTY_ID),
          os.getOsType());

      osElement.put(PropertyHelper.getPropertyName(OperatingSystemResourceProvider.OPERATING_SYSTEM_STACK_NAME_PROPERTY_ID),
          entity.getStackName());
      osElement.put(PropertyHelper.getPropertyName(OperatingSystemResourceProvider.OPERATING_SYSTEM_STACK_VERSION_PROPERTY_ID),
          entity.getStackVersion());

      osBase.put(PropertyHelper.getPropertyCategory(OperatingSystemResourceProvider.OPERATING_SYSTEM_AMBARI_MANAGED_REPOS),
          osElement);

      ArrayNode reposArray = factory.arrayNode();
      for (RepositoryEntity repo : os.getRepositories()) {
        ObjectNode repoBase = factory.objectNode();

        ObjectNode repoElement = factory.objectNode();

        repoElement.put(PropertyHelper.getPropertyName(RepositoryResourceProvider.REPOSITORY_BASE_URL_PROPERTY_ID),
            repo.getBaseUrl());
        repoElement.put(PropertyHelper.getPropertyName(RepositoryResourceProvider.REPOSITORY_OS_TYPE_PROPERTY_ID),
            os.getOsType());
        repoElement.put(PropertyHelper.getPropertyName(RepositoryResourceProvider.REPOSITORY_REPO_ID_PROPERTY_ID),
            repo.getRepositoryId());
        repoElement.put(PropertyHelper.getPropertyName(RepositoryResourceProvider.REPOSITORY_REPO_NAME_PROPERTY_ID),
            repo.getName());
        repoElement.put(PropertyHelper.getPropertyName(RepositoryResourceProvider.REPOSITORY_STACK_NAME_PROPERTY_ID),
            entity.getStackName());
        repoElement.put(PropertyHelper.getPropertyName(RepositoryResourceProvider.REPOSITORY_STACK_VERSION_PROPERTY_ID),
            entity.getStackVersion());

        repoBase.put(PropertyHelper.getPropertyCategory(RepositoryResourceProvider.REPOSITORY_BASE_URL_PROPERTY_ID),
            repoElement);

        reposArray.add(repoBase);
      }

      osBase.put(new RepositoryResourceDefinition().getPluralName(), reposArray);

      subs.add(osBase);
    }

    res.setProperty(new OperatingSystemResourceDefinition().getPluralName(), subs);
  }


  /**
   * Convenience class to hold the xml String representation, the url, and the parsed object.
   */
  private static class XmlHolder {
    String url = null;
    String xmlString = null;
    VersionDefinitionXml xml = null;
    RepositoryVersionEntity entity = null;
  }


}
