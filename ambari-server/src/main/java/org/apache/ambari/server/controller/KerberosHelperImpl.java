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

package org.apache.ambari.server.controller;

import static java.util.Collections.singletonList;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.Role;
import org.apache.ambari.server.RoleCommand;
import org.apache.ambari.server.ServiceNotFoundException;
import org.apache.ambari.server.actionmanager.ActionManager;
import org.apache.ambari.server.actionmanager.RequestFactory;
import org.apache.ambari.server.actionmanager.Stage;
import org.apache.ambari.server.actionmanager.StageFactory;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.api.services.stackadvisor.StackAdvisorHelper;
import org.apache.ambari.server.api.services.stackadvisor.StackAdvisorRequest;
import org.apache.ambari.server.api.services.stackadvisor.recommendations.RecommendationResponse;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.internal.RequestResourceFilter;
import org.apache.ambari.server.controller.internal.RequestStageContainer;
import org.apache.ambari.server.controller.utilities.KerberosChecker;
import org.apache.ambari.server.metadata.RoleCommandOrder;
import org.apache.ambari.server.orm.dao.ArtifactDAO;
import org.apache.ambari.server.orm.dao.KerberosPrincipalDAO;
import org.apache.ambari.server.orm.entities.ArtifactEntity;
import org.apache.ambari.server.security.credential.Credential;
import org.apache.ambari.server.security.credential.PrincipalKeyCredential;
import org.apache.ambari.server.security.encryption.CredentialStoreService;
import org.apache.ambari.server.serveraction.ServerAction;
import org.apache.ambari.server.serveraction.kerberos.CleanupServerAction;
import org.apache.ambari.server.serveraction.kerberos.ConfigureAmbariIdentitiesServerAction;
import org.apache.ambari.server.serveraction.kerberos.CreateKeytabFilesServerAction;
import org.apache.ambari.server.serveraction.kerberos.CreatePrincipalsServerAction;
import org.apache.ambari.server.serveraction.kerberos.DestroyPrincipalsServerAction;
import org.apache.ambari.server.serveraction.kerberos.FinalizeKerberosServerAction;
import org.apache.ambari.server.serveraction.kerberos.KDCType;
import org.apache.ambari.server.serveraction.kerberos.KerberosAdminAuthenticationException;
import org.apache.ambari.server.serveraction.kerberos.KerberosIdentityDataFileWriter;
import org.apache.ambari.server.serveraction.kerberos.KerberosIdentityDataFileWriterFactory;
import org.apache.ambari.server.serveraction.kerberos.KerberosInvalidConfigurationException;
import org.apache.ambari.server.serveraction.kerberos.KerberosKDCConnectionException;
import org.apache.ambari.server.serveraction.kerberos.KerberosLDAPContainerException;
import org.apache.ambari.server.serveraction.kerberos.KerberosMissingAdminCredentialsException;
import org.apache.ambari.server.serveraction.kerberos.KerberosOperationException;
import org.apache.ambari.server.serveraction.kerberos.KerberosOperationHandler;
import org.apache.ambari.server.serveraction.kerberos.KerberosOperationHandlerFactory;
import org.apache.ambari.server.serveraction.kerberos.KerberosRealmException;
import org.apache.ambari.server.serveraction.kerberos.KerberosServerAction;
import org.apache.ambari.server.serveraction.kerberos.PrepareDisableKerberosServerAction;
import org.apache.ambari.server.serveraction.kerberos.PrepareEnableKerberosServerAction;
import org.apache.ambari.server.serveraction.kerberos.PrepareKerberosIdentitiesServerAction;
import org.apache.ambari.server.serveraction.kerberos.UpdateKerberosConfigsServerAction;
import org.apache.ambari.server.stageplanner.RoleGraph;
import org.apache.ambari.server.stageplanner.RoleGraphFactory;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.ConfigHelper;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.HostState;
import org.apache.ambari.server.state.SecurityState;
import org.apache.ambari.server.state.SecurityType;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.State;
import org.apache.ambari.server.state.ValueAttributesInfo;
import org.apache.ambari.server.state.kerberos.AbstractKerberosDescriptorContainer;
import org.apache.ambari.server.state.kerberos.KerberosComponentDescriptor;
import org.apache.ambari.server.state.kerberos.KerberosConfigurationDescriptor;
import org.apache.ambari.server.state.kerberos.KerberosDescriptor;
import org.apache.ambari.server.state.kerberos.KerberosDescriptorFactory;
import org.apache.ambari.server.state.kerberos.KerberosIdentityDescriptor;
import org.apache.ambari.server.state.kerberos.KerberosKeytabDescriptor;
import org.apache.ambari.server.state.kerberos.KerberosPrincipalDescriptor;
import org.apache.ambari.server.state.kerberos.KerberosPrincipalType;
import org.apache.ambari.server.state.kerberos.KerberosServiceDescriptor;
import org.apache.ambari.server.state.kerberos.VariableReplacementHelper;
import org.apache.ambari.server.state.svccomphost.ServiceComponentHostServerActionEvent;
import org.apache.ambari.server.utils.StageUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.directory.server.kerberos.shared.keytab.Keytab;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import com.google.inject.persist.Transactional;

@Singleton
public class KerberosHelperImpl implements KerberosHelper {

  private static final String BASE_LOG_DIR = "/tmp/ambari";

  private static final Logger LOG = LoggerFactory.getLogger(KerberosHelperImpl.class);

  /**
   * The set of states a component may be in, indicating that is have been previously installed on
   * the cluster.
   * <p>
   * These values are important when trying to determine the state of the cluster when adding new components
   */
  private static final Set<State> PREVIOUSLY_INSTALLED_STATES = EnumSet.of(State.INSTALLED, State.STARTED, State.DISABLED);

  @Inject
  private AmbariCustomCommandExecutionHelper customCommandExecutionHelper;

  @Inject
  private AmbariManagementController ambariManagementController;

  @Inject
  private AmbariMetaInfo ambariMetaInfo;

  @Inject
  private ActionManager actionManager;

  @Inject
  private RequestFactory requestFactory;

  @Inject
  private StageFactory stageFactory;

  @Inject
  private RoleGraphFactory roleGraphFactory;

  @Inject
  private Clusters clusters;

  @Inject
  private ConfigHelper configHelper;

  @Inject
  private VariableReplacementHelper variableReplacementHelper;

  @Inject
  private Configuration configuration;

  @Inject
  private KerberosOperationHandlerFactory kerberosOperationHandlerFactory;

  @Inject
  private KerberosDescriptorFactory kerberosDescriptorFactory;

  @Inject
  private KerberosIdentityDataFileWriterFactory kerberosIdentityDataFileWriterFactory;

  @Inject
  private KerberosPrincipalDAO kerberosPrincipalDAO;

  @Inject
  private ArtifactDAO artifactDAO;

  /**
   * The injector used to create new instances of helper classes like CreatePrincipalsServerAction
   * and CreateKeytabFilesServerAction.
   */
  @Inject
  private Injector injector;

  /**
   * The secure storage facility to use to store KDC administrator credential.
   */
  @Inject
  private CredentialStoreService credentialStoreService;

  @Inject
  private StackAdvisorHelper stackAdvisorHelper;

  @Override
  public RequestStageContainer toggleKerberos(Cluster cluster, SecurityType securityType,
                                              RequestStageContainer requestStageContainer,
                                              Boolean manageIdentities)
      throws AmbariException, KerberosOperationException {

    KerberosDetails kerberosDetails = getKerberosDetails(cluster, manageIdentities);

    // Update KerberosDetails with the new security type - the current one in the cluster is the "old" value
    kerberosDetails.setSecurityType(securityType);

    if (securityType == SecurityType.KERBEROS) {
      LOG.info("Configuring Kerberos for realm {} on cluster, {}", kerberosDetails.getDefaultRealm(), cluster.getClusterName());
      requestStageContainer = handle(cluster, kerberosDetails, null, null, null, null, requestStageContainer, new EnableKerberosHandler());
    } else if (securityType == SecurityType.NONE) {
      LOG.info("Disabling Kerberos from cluster, {}", cluster.getClusterName());
      requestStageContainer = handle(cluster, kerberosDetails, null, null, null, null, requestStageContainer, new DisableKerberosHandler());
    } else {
      throw new AmbariException(String.format("Unexpected security type value: %s", securityType.name()));
    }

    return requestStageContainer;
  }

  @Override
  public RequestStageContainer executeCustomOperations(Cluster cluster, Map<String, String> requestProperties,
                                                       RequestStageContainer requestStageContainer,
                                                       Boolean manageIdentities)
      throws AmbariException, KerberosOperationException {

    if (requestProperties != null) {

      for (SupportedCustomOperation operation : SupportedCustomOperation.values()) {
        if (requestProperties.containsKey(operation.name().toLowerCase())) {
          String value = requestProperties.get(operation.name().toLowerCase());

          // The operation specific logic is kept in one place and described here
          switch (operation) {
            case REGENERATE_KEYTABS:
              if (cluster.getSecurityType() != SecurityType.KERBEROS) {
                throw new AmbariException(String.format("Custom operation %s can only be requested with the security type cluster property: %s", operation.name(), SecurityType.KERBEROS.name()));
              }

              CreatePrincipalsAndKeytabsHandler handler = null;

              if ("true".equalsIgnoreCase(value) || "all".equalsIgnoreCase(value)) {
                handler = new CreatePrincipalsAndKeytabsHandler(true, true, true);
              } else if ("missing".equalsIgnoreCase(value)) {
                handler = new CreatePrincipalsAndKeytabsHandler(false, true, true);
              }

              if (handler != null) {
                requestStageContainer = handle(cluster, getKerberosDetails(cluster, manageIdentities),
                    null, null, null, null, requestStageContainer, handler);
              } else {
                throw new AmbariException(String.format("Unexpected directive value: %s", value));
              }

              break;

            default: // No other operations are currently supported
              throw new AmbariException(String.format("Custom operation not supported: %s", operation.name()));
          }
        }
      }
    }

    return requestStageContainer;
  }


  @Override
  public RequestStageContainer ensureIdentities(Cluster cluster, Map<String, ? extends Collection<String>> serviceComponentFilter,
                                                Set<String> hostFilter, Collection<String> identityFilter, Set<String> hostsToForceKerberosOperations,
                                                RequestStageContainer requestStageContainer, Boolean manageIdentities)
      throws AmbariException, KerberosOperationException {
    return handle(cluster, getKerberosDetails(cluster, manageIdentities), serviceComponentFilter, hostFilter, identityFilter,
        hostsToForceKerberosOperations, requestStageContainer, new CreatePrincipalsAndKeytabsHandler(false, false,
            false));
  }

  @Override
  public RequestStageContainer deleteIdentities(Cluster cluster, Map<String, ? extends Collection<String>> serviceComponentFilter,
                                                Set<String> hostFilter, Collection<String> identityFilter, RequestStageContainer requestStageContainer,
                                                Boolean manageIdentities)
      throws AmbariException, KerberosOperationException {
    return handle(cluster, getKerberosDetails(cluster, manageIdentities), serviceComponentFilter, hostFilter, identityFilter, null,
        requestStageContainer, new DeletePrincipalsAndKeytabsHandler());
  }

  @Override
  public void configureServices(Cluster cluster, Map<String, Collection<String>> serviceFilter)
      throws AmbariException, KerberosInvalidConfigurationException {
    Map<String, Map<String, String>> existingConfigurations = calculateExistingConfigurations(cluster, null);
    Map<String, Set<String>> installedServices = new HashMap<String, Set<String>>();
    Set<String> previouslyExistingServices = new HashSet<String>();

    // Calculate the map of installed services to installed components
    Map<String, Service> clusterServices = cluster.getServices();
    if(clusterServices != null) {
      for (Service clusterService : clusterServices.values()) {
        Set<String> installedComponents = installedServices.get(clusterService.getName());
        if (installedComponents == null) {
          installedComponents = new HashSet<String>();
          installedServices.put(clusterService.getName(), installedComponents);
        }

        Map<String, ServiceComponent> clusterServiceComponents = clusterService.getServiceComponents();
        if (clusterServiceComponents != null) {
          for (ServiceComponent clusterServiceComponent : clusterServiceComponents.values()) {
            installedComponents.add(clusterServiceComponent.getName());

            // Determine if this component was PREVIOUSLY installed, which implies that its containing service was PREVIOUSLY installed
            if (!previouslyExistingServices.contains(clusterService.getName())) {
              Map<String, ServiceComponentHost> clusterServiceComponentHosts = clusterServiceComponent.getServiceComponentHosts();
              if (clusterServiceComponentHosts != null) {
                for (ServiceComponentHost clusterServiceComponentHost : clusterServiceComponentHosts.values()) {
                  if (PREVIOUSLY_INSTALLED_STATES.contains(clusterServiceComponentHost.getState())) {
                    previouslyExistingServices.add(clusterService.getName());
                    break;
                  }
                }
              }
            }
          }
        }
      }
    }

    Map<String, Map<String, String>> updates = getServiceConfigurationUpdates(cluster,
        existingConfigurations, installedServices, serviceFilter, previouslyExistingServices, true, true);

    for (Map.Entry<String, Map<String, String>> entry : updates.entrySet()) {
      configHelper.updateConfigType(cluster, ambariManagementController, entry.getKey(), entry.getValue(), null,
          ambariManagementController.getAuthName(), "Enabling Kerberos for added components");
    }
  }

  @Override
  public Map<String, Map<String, String>> getServiceConfigurationUpdates(Cluster cluster,
                                                                         Map<String, Map<String, String>> existingConfigurations,
                                                                         Map<String, Set<String>> installedServices,
                                                                         Map<String, Collection<String>> serviceFilter,
                                                                         Set<String> previouslyExistingServices,
                                                                         boolean kerberosEnabled,
                                                                         boolean applyStackAdvisorUpdates)
      throws KerberosInvalidConfigurationException, AmbariException {

    Map<String, Map<String, String>> kerberosConfigurations = new HashMap<String, Map<String, String>>();
    KerberosDetails kerberosDetails = getKerberosDetails(cluster, null);
    KerberosDescriptor kerberosDescriptor = getKerberosDescriptor(cluster);

    Map<String, String> kerberosDescriptorProperties = kerberosDescriptor.getProperties();
    Map<String, Map<String, String>> configurations = addAdditionalConfigurations(cluster,
        deepCopy(existingConfigurations), null, kerberosDescriptorProperties);

    Map<String, Set<String>> propertiesToIgnore = new HashMap<String, Set<String>>();

    // If Ambari is managing it own identities then add AMBARI to the set of installed servcie so
    // that its Kerberos descriptor entries will be included.
    if(createAmbariIdentities(existingConfigurations.get("kerberos-env"))) {
      installedServices = new HashMap<String, Set<String>>(installedServices);
      installedServices.put("AMBARI", Collections.singleton("AMBARI_SERVER"));
    }

    // Create the context to use for filtering Kerberos Identities based on the state of the cluster
    Map<String, Object> filterContext = new HashMap<String, Object>();
    filterContext.put("configurations", configurations);
    filterContext.put("services", installedServices.keySet());

    for (Map.Entry<String, Set<String>> installedServiceEntry : installedServices.entrySet()) {
      String installedService = installedServiceEntry.getKey();

      if ((serviceFilter == null) || (serviceFilter.containsKey(installedService))) {
        Collection<String> componentFilter = (serviceFilter == null) ? null : serviceFilter.get(installedService);
        Set<String> installedComponents = installedServiceEntry.getValue();

        // Set properties...
        KerberosServiceDescriptor serviceDescriptor = kerberosDescriptor.getService(installedService);

        if (serviceDescriptor != null) {
          if (installedComponents != null) {
            boolean servicePreviouslyExisted = (previouslyExistingServices != null) && previouslyExistingServices.contains(installedService);

            for (String installedComponent : installedComponents) {

              if ((componentFilter == null) || componentFilter.contains(installedComponent)) {
                KerberosComponentDescriptor componentDescriptor = serviceDescriptor.getComponent(installedComponent);
                if (componentDescriptor != null) {
                  Map<String, Map<String, String>> identityConfigurations;

                  identityConfigurations = getIdentityConfigurations(serviceDescriptor.getIdentities(true, filterContext));
                  processIdentityConfigurations(identityConfigurations, kerberosConfigurations, configurations, propertiesToIgnore);

                  identityConfigurations = getIdentityConfigurations(componentDescriptor.getIdentities(true, filterContext));
                  processIdentityConfigurations(identityConfigurations, kerberosConfigurations, configurations, propertiesToIgnore);

                  mergeConfigurations(kerberosConfigurations,
                      componentDescriptor.getConfigurations(!servicePreviouslyExisted), configurations);
                }
              }
            }
          }
        }
      }
    }

    setAuthToLocalRules(kerberosDescriptor, kerberosDetails.getDefaultRealm(), installedServices, configurations, kerberosConfigurations);

    return (applyStackAdvisorUpdates)
        ? applyStackAdvisorUpdates(cluster, installedServices.keySet(), configurations, kerberosConfigurations, propertiesToIgnore,
        null, new HashMap<String, Set<String>>(), kerberosEnabled)
        : kerberosConfigurations;
  }

  @Override
  public Map<String, Map<String, String>> applyStackAdvisorUpdates(Cluster cluster, Set<String> services,
                                                                   Map<String, Map<String, String>> existingConfigurations,
                                                                   Map<String, Map<String, String>> kerberosConfigurations,
                                                                   Map<String, Set<String>> propertiesToIgnore,
                                                                   Map<String, Map<String, String>> propertiesToInsert,
                                                                   Map<String, Set<String>> propertiesToRemove,
                                                                   boolean kerberosEnabled) throws AmbariException {

    StackId stackVersion = cluster.getCurrentStackVersion();

    List<String> hostNames = new ArrayList<String>();
    Collection<Host> hosts = cluster.getHosts();

    if (hosts != null) {
      for (Host host : hosts) {
        hostNames.add(host.getHostName());
      }
    }

    // Don't actually call the stack advisor if no hosts are in the cluster, else the stack advisor
    // will throw a StackAdvisorException stating "Hosts and services must not be empty".
    // This could happen when enabling Kerberos while installing a cluster via Blueprints due to the
    // way hosts are discovered during the install process.
    if (!hostNames.isEmpty()) {
      Map<String, Map<String, Map<String, String>>> requestConfigurations = new HashMap<String, Map<String, Map<String, String>>>();
      if (existingConfigurations != null) {
        for (Map.Entry<String, Map<String, String>> configuration : existingConfigurations.entrySet()) {
          Map<String, Map<String, String>> properties = new HashMap<String, Map<String, String>>();
          String configType = configuration.getKey();
          Map<String, String> configurationProperties = configuration.getValue();

          if (configurationProperties == null) {
            configurationProperties = Collections.emptyMap();
          }

          if ("cluster-env".equals(configType)) {
            configurationProperties = new HashMap<String, String>(configurationProperties);
            configurationProperties.put("security_enabled", (kerberosEnabled) ? "true" : "false");
          }

          properties.put("properties", configurationProperties);
          requestConfigurations.put(configType, properties);
        }
      }

      // Apply the current Kerberos properties...
      for (Map.Entry<String, Map<String, String>> configuration : kerberosConfigurations.entrySet()) {
        String configType = configuration.getKey();
        Map<String, String> configurationProperties = configuration.getValue();

        if ((configurationProperties != null) && !configurationProperties.isEmpty()) {
          Map<String, Map<String, String>> requestConfiguration = requestConfigurations.get(configType);

          if (requestConfiguration == null) {
            requestConfiguration = new HashMap<String, Map<String, String>>();
            requestConfigurations.put(configType, requestConfiguration);
          }

          Map<String, String> requestConfigurationProperties = requestConfiguration.get("properties");
          if (requestConfigurationProperties == null) {
            requestConfigurationProperties = new HashMap<String, String>();
          } else {
            requestConfigurationProperties = new HashMap<String, String>(requestConfigurationProperties);
          }

          requestConfigurationProperties.putAll(configurationProperties);
          requestConfiguration.put("properties", requestConfigurationProperties);
        }
      }

      StackAdvisorRequest request = StackAdvisorRequest.StackAdvisorRequestBuilder
          .forStack(stackVersion.getStackName(), stackVersion.getStackVersion())
          .forServices(new ArrayList<String>(services))
          .forHosts(hostNames)
          .withComponentHostsMap(cluster.getServiceComponentHostMap(null, services))
          .withConfigurations(requestConfigurations)
          .ofType(StackAdvisorRequest.StackAdvisorRequestType.CONFIGURATIONS)
          .build();

      try {
        RecommendationResponse response = stackAdvisorHelper.recommend(request);

        RecommendationResponse.Recommendation recommendation = (response == null) ? null : response.getRecommendations();
        RecommendationResponse.Blueprint blueprint = (recommendation == null) ? null : recommendation.getBlueprint();
        Map<String, RecommendationResponse.BlueprintConfigurations> configurations = (blueprint == null) ? null : blueprint.getConfigurations();

        if (configurations != null) {
          for (Map.Entry<String, RecommendationResponse.BlueprintConfigurations> configuration : configurations.entrySet()) {
            String configType = configuration.getKey();
            Map<String, String> recommendedConfigProperties = configuration.getValue().getProperties();
            Map<String, ValueAttributesInfo> recommendedConfigPropertyAttributes = configuration.getValue().getPropertyAttributes();
            Map<String, String> existingConfigProperties = (existingConfigurations == null) ? null : existingConfigurations.get(configType);
            Map<String, String> kerberosConfigProperties = kerberosConfigurations.get(configType);
            Set<String> ignoreProperties = (propertiesToIgnore == null) ? null : propertiesToIgnore.get(configType);

            addRecommendedPropertiesForConfigType(kerberosEnabled, kerberosConfigurations, configType,
                recommendedConfigProperties,
                existingConfigProperties, kerberosConfigProperties, ignoreProperties, propertiesToInsert);
            if (recommendedConfigPropertyAttributes != null) {
              removeRecommendedPropertiesForConfigType(kerberosEnabled, configType,
                  recommendedConfigPropertyAttributes,
                  existingConfigProperties,
                  kerberosConfigurations, ignoreProperties, propertiesToRemove);
            }
          }
        }

      } catch (Exception e) {
        throw new AmbariException(e.getMessage(), e);
      }
    }

    return kerberosConfigurations;
  }

  /*
   * Recommended property will be added to kerberosConfigurations if kerberosEnabled or to propertiesToInsert
   * otherwise.
   */
  private void addRecommendedPropertiesForConfigType(boolean kerberosEnabled, Map<String, Map<String, String>> kerberosConfigurations,
                                                     String configType, Map<String, String> recommendedConfigProperties,
                                                     Map<String, String> existingConfigProperties,
                                                     Map<String, String> kerberosConfigProperties,
                                                     Set<String> ignoreProperties,
                                                     Map<String, Map<String, String>> propertiesToInsert) {

    for (Map.Entry<String, String> property : recommendedConfigProperties.entrySet()) {
      String propertyName = property.getKey();

      if ((ignoreProperties == null) || !ignoreProperties.contains(propertyName)) {
        String recommendedValue = property.getValue();
        if (kerberosEnabled) {
          if (kerberosConfigProperties == null) {
            // There is no explicit update for this property from the Kerberos Descriptor...
            // add the config and property if it also does not exist in the existing configurations
            if ((existingConfigProperties == null) || !existingConfigProperties.containsKey(propertyName)) {
              LOG.debug("Adding Kerberos configuration based on StackAdvisor recommendation:" +
                      "\n\tConfigType: {}\n\tProperty: {}\n\tValue: {}",
                  configType, propertyName, recommendedValue);

              HashMap<String, String> properties = new HashMap<String, String>();
              properties.put(propertyName, recommendedValue);
              kerberosConfigurations.put(configType, properties);
            }
          } else {
            String value = kerberosConfigProperties.get(propertyName);
            if (value == null) {
              // There is no explicit update for this property from the Kerberos Descriptor...
              // add the property if it also does not exist in the existing configurations
              if ((existingConfigProperties == null) || !existingConfigProperties.containsKey(propertyName)) {
                LOG.debug("Adding Kerberos configuration based on StackAdvisor recommendation:" +
                        "\n\tConfigType: {}\n\tProperty: {}\n\tValue: {}",
                    configType, propertyName, recommendedValue);

                kerberosConfigProperties.put(propertyName, recommendedValue);
              }
            } else if (!value.equals(recommendedValue)) {
              // If the recommended value is a change, automatically change it.
              LOG.debug("Updating Kerberos configuration based on StackAdvisor recommendation:" +
                      "\n\tConfigType: {}\n\tProperty: {}\n\tOld Value: {}\n\tNew Value: {}",
                  configType, propertyName, value, recommendedValue);

              kerberosConfigProperties.put(propertyName, recommendedValue);
            }
          }
        } else if (propertiesToInsert != null && ((existingConfigProperties == null) || !existingConfigProperties
            .containsKey(propertyName))) {
          Map<String, String> properties = propertiesToInsert.get(configType);
          if (properties == null) {
            properties = new HashMap<>();
            propertiesToInsert.put(configType, properties);
          }

          LOG.debug("Property to add to configuration based on StackAdvisor recommendation:" +
                  "\n\tConfigType: {}\n\tProperty: {}\n\tValue: {}",
              configType, propertyName, recommendedValue);

          properties.put(propertyName, recommendedValue);
        }


      }
    }
  }

  /**
   * If property is marked with delete flag in recommendedConfigPropertyAttributes map and is not found in
   * ignoreProperties, nor in kerberosConfigProperties but exits in existingConfigProperties add to
   * propertiesToRemove map if kerberosEnabled or kerberosConfigurations otherwise.
   */
  private void removeRecommendedPropertiesForConfigType(boolean kerberosEnabled, String configType,
                                                        Map<String, ValueAttributesInfo> recommendedConfigPropertyAttributes,
                                                        Map<String, String> existingConfigProperties,
                                                        Map<String, Map<String, String>> kerberosConfigurations,
                                                        Set<String> ignoreProperties, Map<String, Set<String>>
                                                            propertiesToRemove) {

    for (Map.Entry<String, ValueAttributesInfo> property : recommendedConfigPropertyAttributes.entrySet()) {
      String propertyName = property.getKey();
      if ("true".equalsIgnoreCase(property.getValue().getDelete())) {
        // if property is not in ignoreProperties, nor in kerberosConfigProperties but is found in existingConfigProperties
        // add to propertiesToBeRemoved map
        Map<String, String> kerberosConfigProperties = kerberosConfigurations.get(configType);
        if (((ignoreProperties == null) || !ignoreProperties.contains(propertyName)) &&
            ((kerberosConfigProperties == null) || kerberosConfigProperties.get(propertyName) == null) &&
            (existingConfigProperties != null && existingConfigProperties.containsKey(propertyName))) {

          LOG.debug("Property to remove from configuration based on StackAdvisor recommendation:" +
                  "\n\tConfigType: {}\n\tProperty: {}",
              configType, propertyName);

          // kerberosEnabled add property to propertiesToRemove, otherwise to kerberosConfigurations map
          if (kerberosEnabled && propertiesToRemove != null) {
            Set<String> properties = propertiesToRemove.get(configType);
            if (properties == null) {
              properties = new HashSet<String>();
              propertiesToRemove.put(configType, properties);
            }
            properties.add(propertyName);
          } else {
            if (kerberosConfigProperties == null) {
              kerberosConfigProperties = new HashMap<String, String>();
              kerberosConfigurations.put(configType, kerberosConfigProperties);
            }
            kerberosConfigProperties.put(propertyName, "");
          }
        }
      }
    }
  }

  @Override
  public boolean ensureHeadlessIdentities(Cluster cluster, Map<String, Map<String, String>> existingConfigurations, Set<String> services)
      throws KerberosInvalidConfigurationException, AmbariException {

    KerberosDetails kerberosDetails = getKerberosDetails(cluster, null);

    // Only perform this task if Ambari manages Kerberos identities
    if (kerberosDetails.manageIdentities()) {
      KerberosDescriptor kerberosDescriptor = getKerberosDescriptor(cluster);

      Map<String, String> kerberosDescriptorProperties = kerberosDescriptor.getProperties();
      Map<String, Map<String, String>> configurations = addAdditionalConfigurations(cluster,
          deepCopy(existingConfigurations), null, kerberosDescriptorProperties);

      Map<String, String> kerberosConfiguration = kerberosDetails.getKerberosEnvProperties();
      KerberosOperationHandler kerberosOperationHandler = kerberosOperationHandlerFactory.getKerberosOperationHandler(kerberosDetails.getKdcType());
      PrincipalKeyCredential administratorCredential = getKDCAdministratorCredentials(cluster.getClusterName());

      try {
        kerberosOperationHandler.open(administratorCredential, kerberosDetails.getDefaultRealm(), kerberosConfiguration);
      } catch (KerberosOperationException e) {
        String message = String.format("Failed to process the identities, could not properly open the KDC operation handler: %s",
            e.getMessage());
        LOG.error(message);
        throw new AmbariException(message, e);
      }

      // Create the context to use for filtering Kerberos Identities based on the state of the cluster
      Map<String, Object> filterContext = new HashMap<String, Object>();
      filterContext.put("configurations", configurations);
      filterContext.put("services", services);

      for (String serviceName : services) {
        // Set properties...
        KerberosServiceDescriptor serviceDescriptor = kerberosDescriptor.getService(serviceName);

        if (serviceDescriptor != null) {
          Map<String, KerberosComponentDescriptor> componentDescriptors = serviceDescriptor.getComponents();
          if (null != componentDescriptors) {
            for (KerberosComponentDescriptor componentDescriptor : componentDescriptors.values()) {
              if (componentDescriptor != null) {
                List<KerberosIdentityDescriptor> identityDescriptors;

                // Handle the service-level Kerberos identities
                identityDescriptors = serviceDescriptor.getIdentities(true, filterContext);
                if (identityDescriptors != null) {
                  for (KerberosIdentityDescriptor identityDescriptor : identityDescriptors) {
                    createIdentity(identityDescriptor, KerberosPrincipalType.USER, kerberosConfiguration, kerberosOperationHandler, configurations, null);
                  }
                }

                // Handle the component-level Kerberos identities
                identityDescriptors = componentDescriptor.getIdentities(true, filterContext);
                if (identityDescriptors != null) {
                  for (KerberosIdentityDescriptor identityDescriptor : identityDescriptors) {
                    createIdentity(identityDescriptor, KerberosPrincipalType.USER, kerberosConfiguration, kerberosOperationHandler, configurations, null);
                  }
                }
              }
            }
          }
        }
      }

      // create Ambari principal & keytab, configure JAAS only if 'kerberos-env.create_ambari_principal = true'
      if (kerberosDetails.createAmbariPrincipal()) {
        installAmbariIdentities(kerberosDescriptor, kerberosOperationHandler, kerberosConfiguration, configurations, kerberosDetails);
      }

      // The KerberosOperationHandler needs to be closed, if it fails to close ignore the
      // exception since there is little we can or care to do about it now.
      try {
        kerberosOperationHandler.close();
      } catch (KerberosOperationException e) {
        // Ignore this...
      }

    }

    return true;
  }

  /**
   * Install identities needed by the Ambari server, itself.
   * <p>
   * The Ambari server needs its own identity for authentication; and, if Kerberos authentication is
   * enabled, it needs a SPNEGO principal for ticket validation routines.
   * <p>
   * Any identities needed by the Ambari server need to be installed separately since an agent may not
   * exist on the host and therefore distributing the keytab file(s) to the Ambari server host may
   * not be possible using the same workflow used for other hosts in the cluster.
   *
   * @param kerberosDescriptor       the Kerberos descriptor
   * @param kerberosOperationHandler the relevant KerberosOperationHandler
   * @param kerberosEnvProperties    the kerberos-env properties
   * @param configurations           a map of config-types to property name/value pairs representing
   *                                 the existing configurations for the cluster
   * @param kerberosDetails          a KerberosDetails containing information about relevant Kerberos
   *                                 configuration
   * @throws AmbariException
   */
  private void installAmbariIdentities(KerberosDescriptor kerberosDescriptor,
                                       KerberosOperationHandler kerberosOperationHandler,
                                       Map<String, String> kerberosEnvProperties,
                                       Map<String, Map<String, String>> configurations,
                                       KerberosDetails kerberosDetails) throws AmbariException {

    // Install Ambari's identities.....
    List<KerberosIdentityDescriptor> ambariIdentities = getAmbariServerIdentities(kerberosDescriptor);

    if (!ambariIdentities.isEmpty()) {
      String ambariServerHostname = StageUtils.getHostName();

      for (KerberosIdentityDescriptor identity : ambariIdentities) {
        if (identity != null) {
          KerberosPrincipalDescriptor principal = identity.getPrincipalDescriptor();
          if (principal != null) {
            boolean updateJAASFile = AMBARI_SERVER_KERBEROS_IDENTITY_NAME.equals(identity.getName());
            Keytab keytab = createIdentity(identity, principal.getType(), kerberosEnvProperties, kerberosOperationHandler, configurations, ambariServerHostname);
            installAmbariIdentity(identity, keytab, configurations, ambariServerHostname, kerberosDetails, updateJAASFile);

            if (updateJAASFile) {
              try {
                KerberosChecker.checkJaasConfiguration();
              } catch (AmbariException e) {
                LOG.error("Error in Ambari JAAS configuration: " + e.getLocalizedMessage(), e);
              }
            }
          }
        }
      }
    }
  }

  /**
   * Performs tasks needed to install the Kerberos identities created for the Ambari server.
   *
   * @param ambariServerIdentity the ambari server's {@link KerberosIdentityDescriptor}
   * @param keytab               the Keyab data for the relevant identity
   * @param configurations       a map of compiled configurations used for variable replacement
   * @param hostname             the hostname to use to replace _HOST in principal names, if necessary
   * @param kerberosDetails      a KerberosDetails containing information about relevant Kerberos configuration
   * @param updateJAASFile       true to update Ambari's JAAS file; false otherwise
   * @throws AmbariException
   * @see ConfigureAmbariIdentitiesServerAction#configureJAAS(String, String, org.apache.ambari.server.serveraction.ActionLog)
   */
  private void installAmbariIdentity(KerberosIdentityDescriptor ambariServerIdentity,
                                     Keytab keytab, Map<String, Map<String, String>> configurations,
                                     String hostname,
                                     KerberosDetails kerberosDetails,
                                     boolean updateJAASFile) throws AmbariException {
    KerberosPrincipalDescriptor principalDescriptor = ambariServerIdentity.getPrincipalDescriptor();

    if (principalDescriptor != null) {
      String principal = variableReplacementHelper.replaceVariables(principalDescriptor.getValue(), configurations);

      // Replace _HOST with the supplied hostname is either exist
      if (!StringUtils.isEmpty(hostname)) {
        principal = principal.replace("_HOST", hostname);
      }

      KerberosKeytabDescriptor keytabDescriptor = ambariServerIdentity.getKeytabDescriptor();
      if (keytabDescriptor != null) {
        String destKeytabFilePath = variableReplacementHelper.replaceVariables(keytabDescriptor.getFile(), configurations);
        File destKeytabFile = new File(destKeytabFilePath);

        ConfigureAmbariIdentitiesServerAction configureAmbariIdentitiesServerAction = injector.getInstance(ConfigureAmbariIdentitiesServerAction.class);

        if (keytab != null) {
          try {
            KerberosOperationHandler operationHandler = kerberosOperationHandlerFactory.getKerberosOperationHandler(kerberosDetails.getKdcType());
            File tmpKeytabFile = createTemporaryFile();
            try {
              if ((operationHandler != null) && operationHandler.createKeytabFile(keytab, tmpKeytabFile)) {
                String ownerName = variableReplacementHelper.replaceVariables(keytabDescriptor.getOwnerName(), configurations);
                String ownerAccess = keytabDescriptor.getOwnerAccess();
                boolean ownerWritable = "w".equalsIgnoreCase(ownerAccess) || "rw".equalsIgnoreCase(ownerAccess);
                boolean ownerReadable = "r".equalsIgnoreCase(ownerAccess) || "rw".equalsIgnoreCase(ownerAccess);
                String groupName = variableReplacementHelper.replaceVariables(keytabDescriptor.getGroupName(), configurations);
                String groupAccess = keytabDescriptor.getGroupAccess();
                boolean groupWritable = "w".equalsIgnoreCase(groupAccess) || "rw".equalsIgnoreCase(groupAccess);
                boolean groupReadable = "r".equalsIgnoreCase(groupAccess) || "rw".equalsIgnoreCase(groupAccess);

                configureAmbariIdentitiesServerAction.installAmbariServerIdentity(principal, tmpKeytabFile.getAbsolutePath(), destKeytabFilePath,
                    ownerName, ownerReadable, ownerWritable, groupName, groupReadable, groupWritable, null);
                LOG.debug("Successfully created keytab file for {} at {}", principal, destKeytabFile.getAbsolutePath());
              } else {
                LOG.error("Failed to create keytab file for {} at {}", principal, destKeytabFile.getAbsolutePath());
              }
            } finally {
              tmpKeytabFile.delete();
            }
          } catch (KerberosOperationException e) {
            throw new AmbariException(String.format("Failed to create keytab file for %s at %s: %s:",
                principal, destKeytabFile.getAbsolutePath(), e.getLocalizedMessage()), e);
          }
        } else {
          LOG.error("No keytab data is available to create the keytab file for {} at {}", principal, destKeytabFile.getAbsolutePath());
        }

        if (updateJAASFile) {
          configureAmbariIdentitiesServerAction.configureJAAS(principal, destKeytabFile.getAbsolutePath(), null);
        }
      }
    }
  }

  @Override
  public RequestStageContainer createTestIdentity(Cluster cluster, Map<String, String> commandParamsStage,
                                                  RequestStageContainer requestStageContainer)
      throws KerberosOperationException, AmbariException {
    return handleTestIdentity(cluster, getKerberosDetails(cluster, null), commandParamsStage, requestStageContainer,
        new CreatePrincipalsAndKeytabsHandler(false, false, false));
  }

  @Override
  public RequestStageContainer deleteTestIdentity(Cluster cluster, Map<String, String> commandParamsStage,
                                                  RequestStageContainer requestStageContainer)
      throws KerberosOperationException, AmbariException {
    requestStageContainer = handleTestIdentity(cluster, getKerberosDetails(cluster, null), commandParamsStage, requestStageContainer, new DeletePrincipalsAndKeytabsHandler());
    return requestStageContainer;
  }

  @Override
  public void validateKDCCredentials(Cluster cluster) throws KerberosMissingAdminCredentialsException,
      KerberosAdminAuthenticationException,
      KerberosInvalidConfigurationException,
      AmbariException {
    validateKDCCredentials(null, cluster);
  }

  @Override
  public void setAuthToLocalRules(KerberosDescriptor kerberosDescriptor, String realm,
                                  Map<String, Set<String>> installedServices,
                                  Map<String, Map<String, String>> existingConfigurations,
                                  Map<String, Map<String, String>> kerberosConfigurations)
      throws AmbariException {

    boolean processAuthToLocalRules = true;
    Map<String, String> kerberosEnvProperties = existingConfigurations.get("kerberos-env");
    if (kerberosEnvProperties.containsKey("manage_auth_to_local")) {
      processAuthToLocalRules = Boolean.valueOf(kerberosEnvProperties.get("manage_auth_to_local"));
    }

    if (kerberosDescriptor != null && processAuthToLocalRules) {

      Set<String> authToLocalProperties;
      Set<String> authToLocalPropertiesToSet = new HashSet<String>();

      // a flag to be used by the AuthToLocalBuilder marking whether the default realm rule should contain the //L option, indicating username case insensitive behaviour
      // the 'kerberos-env' structure is expected to be available here as it was previously validated
      boolean caseInsensitiveUser = Boolean.valueOf(existingConfigurations.get("kerberos-env").get("case_insensitive_username_rules"));

      // Additional realms that need to be handled according to the Kerberos Descriptor
      String additionalRealms = kerberosDescriptor.getProperty("additional_realms");

      // Create the context to use for filtering Kerberos Identities based on the state of the cluster
      Map<String, Object> filterContext = new HashMap<String, Object>();
      filterContext.put("configurations", existingConfigurations);
      filterContext.put("services", installedServices.keySet());

      // Determine which properties need to be set
      AuthToLocalBuilder authToLocalBuilder = new AuthToLocalBuilder(realm, additionalRealms, caseInsensitiveUser);
      addIdentities(authToLocalBuilder, kerberosDescriptor.getIdentities(true, filterContext), null, existingConfigurations);

      authToLocalProperties = kerberosDescriptor.getAuthToLocalProperties();
      if (authToLocalProperties != null) {
        authToLocalPropertiesToSet.addAll(authToLocalProperties);
      }

      for(Map.Entry<String, Set<String>> installedService: installedServices.entrySet()) {
        String serviceName = installedService.getKey();

        KerberosServiceDescriptor serviceDescriptor = kerberosDescriptor.getService(serviceName);
        if(serviceDescriptor != null) {
          LOG.info("Adding identities for service {} to auth to local mapping", installedService);

          // Process the service-level Kerberos descriptor
          addIdentities(authToLocalBuilder, serviceDescriptor.getIdentities(true, filterContext), null, existingConfigurations);

          authToLocalProperties = serviceDescriptor.getAuthToLocalProperties();
          if (authToLocalProperties != null) {
            authToLocalPropertiesToSet.addAll(authToLocalProperties);
          }

          // Process the relevant component-level Kerberos descriptors
          Set<String> installedComponents = installedService.getValue();
          if(installedComponents != null) {
            for (String installedComponent : installedComponents) {
              KerberosComponentDescriptor componentDescriptor = serviceDescriptor.getComponent(installedComponent);

              if (componentDescriptor != null) {
                LOG.info("Adding identities for component {} to auth to local mapping", installedComponent);
                addIdentities(authToLocalBuilder, componentDescriptor.getIdentities(true, filterContext), null, existingConfigurations);

                authToLocalProperties = componentDescriptor.getAuthToLocalProperties();
                if (authToLocalProperties != null) {
                  authToLocalPropertiesToSet.addAll(authToLocalProperties);
                }
              }
            }
          }
        }
      }

      if (!authToLocalPropertiesToSet.isEmpty()) {
        for (String authToLocalProperty : authToLocalPropertiesToSet) {
          Matcher m = KerberosDescriptor.AUTH_TO_LOCAL_PROPERTY_SPECIFICATION_PATTERN.matcher(authToLocalProperty);

          if (m.matches()) {
            AuthToLocalBuilder builder;
            try {
              builder = (AuthToLocalBuilder) authToLocalBuilder.clone();
            } catch (CloneNotSupportedException e) {
              LOG.error("Failed to clone the AuthToLocalBuilder: " + e.getLocalizedMessage(), e);
              throw new AmbariException("Failed to clone the AuthToLocalBuilder: " + e.getLocalizedMessage(), e);
            }

            String configType = m.group(1);
            String propertyName = m.group(2);

            if (configType == null) {
              configType = "";
            }

            // Add existing auth_to_local configuration, if set
            Map<String, String> existingConfiguration = existingConfigurations.get(configType);
            if (existingConfiguration != null) {
              builder.addRules(existingConfiguration.get(propertyName));
            }

            // Add/update descriptor auth_to_local configuration, if set
            Map<String, String> kerberosConfiguration = kerberosConfigurations.get(configType);
            if (kerberosConfiguration != null) {
              builder.addRules(kerberosConfiguration.get(propertyName));
            } else {
              kerberosConfiguration = new HashMap<String, String>();
              kerberosConfigurations.put(configType, kerberosConfiguration);
            }

            kerberosConfiguration.put(propertyName,
                builder.generate(AuthToLocalBuilder.ConcatenationType.translate(m.group(3))));
          }
        }
      }
    }
  }


  @Override
  public List<ServiceComponentHost> getServiceComponentHostsToProcess(Cluster cluster,
                                                                      KerberosDescriptor kerberosDescriptor,
                                                                      Map<String, ? extends Collection<String>> serviceComponentFilter,
                                                                      Collection<String> hostFilter, Collection<String> identityFilter,
                                                                      Command<Boolean, ServiceComponentHost> shouldProcessCommand)
      throws AmbariException {
    List<ServiceComponentHost> serviceComponentHostsToProcess = new ArrayList<ServiceComponentHost>();
    Map<String, Service> services = cluster.getServices();

    if ((services != null) && !services.isEmpty()) {
      Collection<Host> hosts = cluster.getHosts();

      if ((hosts != null) && !hosts.isEmpty()) {
        // Iterate over the hosts in the cluster to find the components installed in each.  For each
        // component (aka service component host - sch) determine the configuration updates and
        // and the principals an keytabs to create.
        for (Host host : hosts) {
          String hostname = host.getHostName();

          // Filter hosts as needed....
          if ((hostFilter == null) || hostFilter.contains(hostname)) {
            // Get a list of components on the current host
            List<ServiceComponentHost> serviceComponentHosts = cluster.getServiceComponentHosts(hostname);

            if ((serviceComponentHosts != null) && !serviceComponentHosts.isEmpty()) {

              // Iterate over the components installed on the current host to get the service and
              // component-level Kerberos descriptors in order to determine which principals,
              // keytab files, and configurations need to be created or updated.
              for (ServiceComponentHost sch : serviceComponentHosts) {
                String serviceName = sch.getServiceName();
                String componentName = sch.getServiceComponentName();

                // If there is no filter or the filter contains the current service name...
                if ((serviceComponentFilter == null) || serviceComponentFilter.containsKey(serviceName)) {
                  Collection<String> componentFilter = (serviceComponentFilter == null) ? null : serviceComponentFilter.get(serviceName);
                  KerberosServiceDescriptor serviceDescriptor = kerberosDescriptor.getService(serviceName);

                  if (serviceDescriptor != null) {
                    // If there is no filter or the filter contains the current component name,
                    // test to see if this component should be processed by querying the handler...
                    if (((componentFilter == null) || componentFilter.contains(componentName)) && shouldProcessCommand.invoke(sch)) {
                      serviceComponentHostsToProcess.add(sch);
                    }
                  }
                }
              }
            }
          }
        }
      }
    }

    return serviceComponentHostsToProcess;
  }


  @Override
  public Set<String> getHostsWithValidKerberosClient(Cluster cluster)
      throws AmbariException {
    Set<String> hostsWithValidKerberosClient = new HashSet<String>();
    List<ServiceComponentHost> schKerberosClients = cluster.getServiceComponentHosts(Service.Type.KERBEROS.name(), Role.KERBEROS_CLIENT.name());

    if (schKerberosClients != null) {
      for (ServiceComponentHost sch : schKerberosClients) {
        if (sch.getState() == State.INSTALLED) {
          hostsWithValidKerberosClient.add(sch.getHostName());
        }
      }
    }

    return hostsWithValidKerberosClient;
  }

  @Override
  public KerberosDescriptor getKerberosDescriptor(Cluster cluster) throws AmbariException {
    return getKerberosDescriptor(KerberosDescriptorType.COMPOSITE, cluster, false, null);
  }

  @Override
  public KerberosDescriptor getKerberosDescriptor(KerberosDescriptorType kerberosDescriptorType, Cluster cluster,
                                                  boolean evaluateWhenClauses, Collection<String> additionalServices)
      throws AmbariException {
    KerberosDescriptor kerberosDescriptor;

    KerberosDescriptor stackDescriptor = (kerberosDescriptorType == KerberosDescriptorType.STACK || kerberosDescriptorType == KerberosDescriptorType.COMPOSITE)
        ? getKerberosDescriptorFromStack(cluster)
        : null;

    KerberosDescriptor userDescriptor = (kerberosDescriptorType == KerberosDescriptorType.USER || kerberosDescriptorType == KerberosDescriptorType.COMPOSITE)
        ? getKerberosDescriptorUpdates(cluster)
        : null;

    if (stackDescriptor == null) {
      if (userDescriptor == null) {
        return new KerberosDescriptor();  // return an empty Kerberos descriptor since we have no data
      } else {
        kerberosDescriptor = userDescriptor;
      }
    } else {
      if (userDescriptor != null) {
        stackDescriptor.update(userDescriptor);
      }
      kerberosDescriptor = stackDescriptor;
    }

    if (evaluateWhenClauses) {
      Set<String> services = new HashSet<String>(cluster.getServices().keySet());

      if(additionalServices != null) {
        services.addAll(additionalServices);
      }

      // Build the context needed to filter out Kerberos identities...
      // This includes the current set of configurations for the cluster and the set of installed services
      Map<String, Object> context = new HashMap<String, Object>();
      context.put("configurations", calculateConfigurations(cluster, null, kerberosDescriptor.getProperties()));
      context.put("services", services);

      // Get the Kerberos identities that need to be pruned
      Map<String, Set<String>> identitiesToRemove = processWhenClauses("", kerberosDescriptor, context, new HashMap<String, Set<String>>());

      // Prune off the Kerberos identities that need to be removed due to the evaluation of its _when_ clause
      for (Map.Entry<String, Set<String>> identity : identitiesToRemove.entrySet()) {
        String[] path = identity.getKey().split("/");
        AbstractKerberosDescriptorContainer container = null;

        // Follow the path to the container that contains the identities to remove
        for (String name : path) {
          if (container == null) {
            container = kerberosDescriptor;
          } else {
            container = container.getChildContainer(name);

            if (container == null) {
              break;
            }
          }
        }

        // Remove the relevant identities from the found container
        if (container != null) {
          for (String identityName : identity.getValue()) {
            container.removeIdentity(identityName);
          }
        }
      }
    }

    return kerberosDescriptor;
  }

  @Override
  public Map<String, Map<String, String>> mergeConfigurations(Map<String, Map<String, String>> configurations,
                                                              Map<String, KerberosConfigurationDescriptor> updates,
                                                              Map<String, Map<String, String>> replacements)
      throws AmbariException {

    if ((updates != null) && !updates.isEmpty()) {
      if (configurations == null) {
        configurations = new HashMap<String, Map<String, String>>();
      }

      for (Map.Entry<String, KerberosConfigurationDescriptor> entry : updates.entrySet()) {
        String type = entry.getKey();
        KerberosConfigurationDescriptor configurationDescriptor = entry.getValue();

        if (configurationDescriptor != null) {
          Map<String, String> updatedProperties = configurationDescriptor.getProperties();
          mergeConfigurations(configurations, type, updatedProperties, replacements);
        }
      }
    }

    return configurations;
  }

  @Override
  public int addIdentities(KerberosIdentityDataFileWriter kerberosIdentityDataFileWriter,
                           Collection<KerberosIdentityDescriptor> identities,
                           Collection<String> identityFilter, String hostname, String serviceName,
                           String componentName, Map<String, Map<String, String>> kerberosConfigurations,
                           Map<String, Map<String, String>> configurations)
      throws IOException {
    int identitiesAdded = 0;

    if (identities != null) {
      for (KerberosIdentityDescriptor identity : identities) {
        // If there is no filter or the filter contains the current identity's name...
        if ((identityFilter == null) || identityFilter.contains(identity.getName())) {
          KerberosPrincipalDescriptor principalDescriptor = identity.getPrincipalDescriptor();
          String principal = null;
          String principalType = null;
          String principalConfiguration = null;

          if (principalDescriptor != null) {
            principal = variableReplacementHelper.replaceVariables(principalDescriptor.getValue(), configurations);
            principalType = KerberosPrincipalType.translate(principalDescriptor.getType());
            principalConfiguration = variableReplacementHelper.replaceVariables(principalDescriptor.getConfiguration(), configurations);
          }

          if (principal != null) {
            KerberosKeytabDescriptor keytabDescriptor = identity.getKeytabDescriptor();
            String keytabFilePath = null;
            String keytabFileOwnerName = null;
            String keytabFileOwnerAccess = null;
            String keytabFileGroupName = null;
            String keytabFileGroupAccess = null;
            String keytabFileConfiguration = null;
            boolean keytabIsCachable = false;

            if (keytabDescriptor != null) {
              keytabFilePath = variableReplacementHelper.replaceVariables(keytabDescriptor.getFile(), configurations);
              keytabFileOwnerName = variableReplacementHelper.replaceVariables(keytabDescriptor.getOwnerName(), configurations);
              keytabFileOwnerAccess = variableReplacementHelper.replaceVariables(keytabDescriptor.getOwnerAccess(), configurations);
              keytabFileGroupName = variableReplacementHelper.replaceVariables(keytabDescriptor.getGroupName(), configurations);
              keytabFileGroupAccess = variableReplacementHelper.replaceVariables(keytabDescriptor.getGroupAccess(), configurations);
              keytabFileConfiguration = variableReplacementHelper.replaceVariables(keytabDescriptor.getConfiguration(), configurations);
              keytabIsCachable = keytabDescriptor.isCachable();
            }

            // Append an entry to the action data file builder...
            kerberosIdentityDataFileWriter.writeRecord(
                hostname,
                serviceName,
                componentName,
                principal,
                principalType,
                keytabFilePath,
                keytabFileOwnerName,
                keytabFileOwnerAccess,
                keytabFileGroupName,
                keytabFileGroupAccess,
                (keytabIsCachable) ? "true" : "false");

            // Add the principal-related configuration to the map of configurations
            mergeConfiguration(kerberosConfigurations, principalConfiguration, principal, null);

            // Add the keytab-related configuration to the map of configurations
            mergeConfiguration(kerberosConfigurations, keytabFileConfiguration, keytabFilePath, null);

            identitiesAdded++;
          }
        }
      }
    }

    return identitiesAdded;
  }

  @Override
  public Map<String, Map<String, String>> calculateConfigurations(Cluster cluster, String hostname,
                                                                  Map<String, String> kerberosDescriptorProperties)
      throws AmbariException {
    return addAdditionalConfigurations(cluster,
        calculateExistingConfigurations(cluster, hostname),
        hostname, kerberosDescriptorProperties);
  }

  @Override
  public Map<String, Collection<KerberosIdentityDescriptor>> getActiveIdentities(String clusterName,
                                                                                 String hostName,
                                                                                 String serviceName,
                                                                                 String componentName,
                                                                                 boolean replaceHostNames)
      throws AmbariException {

    if ((clusterName == null) || clusterName.isEmpty()) {
      throw new IllegalArgumentException("Invalid argument, cluster name is required");
    }

    Cluster cluster = clusters.getCluster(clusterName);

    if (cluster == null) {
      throw new AmbariException(String.format("The cluster object for the cluster name %s is not available", clusterName));
    }

    Map<String, Collection<KerberosIdentityDescriptor>> activeIdentities = new HashMap<String, Collection<KerberosIdentityDescriptor>>();

    // Only calculate the active identities if the kerberos-env configurtaion is available.  Else
    // important information like the realm will be missing (kerberos-env/realm)
    Config kerberosEnvConfig = cluster.getDesiredConfigByType("kerberos-env");
    if(kerberosEnvConfig == null) {
      LOG.debug("Calculating the active identities for {} is being skipped since the kerberos-env configuration is not available",
          clusterName, cluster.getSecurityType().name(), SecurityType.KERBEROS.name());
    }
    else {
      Collection<String> hosts;
      String ambariServerHostname = StageUtils.getHostName();

      if (hostName == null) {
        Map<String, Host> hostMap = clusters.getHostsForCluster(clusterName);
        if (hostMap == null) {
          hosts = Collections.emptySet();
        } else {
          hosts = hostMap.keySet();
        }

        if (!hosts.contains(ambariServerHostname)) {
          Collection<String> extendedHosts = new ArrayList<>(hosts.size() + 1);
          extendedHosts.addAll(hosts);
          extendedHosts.add(ambariServerHostname);
          hosts = extendedHosts;
        }
      } else {
        hosts = Collections.singleton(hostName);
      }

      if (!hosts.isEmpty()) {
        KerberosDescriptor kerberosDescriptor = getKerberosDescriptor(cluster);

        if (kerberosDescriptor != null) {
          Map<String, String> kerberosDescriptorProperties = kerberosDescriptor.getProperties();

          Set<String> existingServices = cluster.getServices().keySet();

          for (String hostname : hosts) {
            // Calculate the current host-specific configurations. These will be used to replace
            // variables within the Kerberos descriptor data
            Map<String, Map<String, String>> configurations = calculateConfigurations(cluster,
                hostname.equals(ambariServerHostname) ? null : hostname,
                kerberosDescriptorProperties);

            // Create the context to use for filtering Kerberos Identities based on the state of the cluster
            Map<String, Object> filterContext = new HashMap<String, Object>();
            filterContext.put("configurations", configurations);
            filterContext.put("services", existingServices);


            Map<String, KerberosIdentityDescriptor> hostActiveIdentities = new HashMap<String, KerberosIdentityDescriptor>();
            List<KerberosIdentityDescriptor> identities = getActiveIdentities(cluster, hostname,
                serviceName, componentName, kerberosDescriptor, filterContext);

            if (hostname.equals(ambariServerHostname)) {
              // Determine if we should _calculate_ the Ambari service identities.
              // If kerberos-env/create_ambari_principal is not set to false the identity should be calculated.
              if(createAmbariIdentities(kerberosEnvConfig.getProperties())) {
                List<KerberosIdentityDescriptor> ambariIdentities = getAmbariServerIdentities(kerberosDescriptor);
                if (ambariIdentities != null) {
                  identities.addAll(ambariIdentities);
                }
              }
            }

            if (!identities.isEmpty()) {
              for (KerberosIdentityDescriptor identity : identities) {
                KerberosPrincipalDescriptor principalDescriptor = identity.getPrincipalDescriptor();
                String principal = null;

                if (principalDescriptor != null) {
                  principal = variableReplacementHelper.replaceVariables(principalDescriptor.getValue(), configurations);
                }

                if (principal != null) {
                  KerberosKeytabDescriptor keytabDescriptor = identity.getKeytabDescriptor();
                  String keytabFile = null;

                  if (keytabDescriptor != null) {
                    keytabFile = variableReplacementHelper.replaceVariables(keytabDescriptor.getFile(), configurations);
                  }

                  if (replaceHostNames) {
                    principal = principal.replace("_HOST", hostname);
                  }

                  String uniqueKey = String.format("%s|%s", principal, (keytabFile == null) ? "" : keytabFile);

                  if (!hostActiveIdentities.containsKey(uniqueKey)) {
                    KerberosPrincipalType principalType = principalDescriptor.getType();

                    // Assume the principal is a service principal if not specified
                    if (principalType == null) {
                      principalType = KerberosPrincipalType.SERVICE;
                    }

                    KerberosPrincipalDescriptor resolvedPrincipalDescriptor =
                        new KerberosPrincipalDescriptor(principal,
                            principalType,
                            variableReplacementHelper.replaceVariables(principalDescriptor.getConfiguration(), configurations),
                            variableReplacementHelper.replaceVariables(principalDescriptor.getLocalUsername(), configurations));

                    KerberosKeytabDescriptor resolvedKeytabDescriptor;

                    if (keytabFile == null) {
                      resolvedKeytabDescriptor = null;
                    } else {
                      resolvedKeytabDescriptor =
                          new KerberosKeytabDescriptor(
                              keytabFile,
                              variableReplacementHelper.replaceVariables(keytabDescriptor.getOwnerName(), configurations),
                              variableReplacementHelper.replaceVariables(keytabDescriptor.getOwnerAccess(), configurations),
                              variableReplacementHelper.replaceVariables(keytabDescriptor.getGroupName(), configurations),
                              variableReplacementHelper.replaceVariables(keytabDescriptor.getGroupAccess(), configurations),
                              variableReplacementHelper.replaceVariables(keytabDescriptor.getConfiguration(), configurations),
                              keytabDescriptor.isCachable());
                    }

                    hostActiveIdentities.put(uniqueKey, new KerberosIdentityDescriptor(
                        identity.getName(),
                        identity.getReference(),
                        resolvedPrincipalDescriptor,
                        resolvedKeytabDescriptor,
                        identity.getWhen()));
                  }
                }
              }
            }

            activeIdentities.put(hostname, hostActiveIdentities.values());
          }
        }
      }
    }

    return activeIdentities;
  }

  @Override
  public List<KerberosIdentityDescriptor> getAmbariServerIdentities(KerberosDescriptor kerberosDescriptor) throws AmbariException {
    List<KerberosIdentityDescriptor> ambariIdentities = new ArrayList<KerberosIdentityDescriptor>();

    KerberosServiceDescriptor ambariKerberosDescriptor = kerberosDescriptor.getService("AMBARI");
    if (ambariKerberosDescriptor != null) {
      List<KerberosIdentityDescriptor> serviceIdentities = ambariKerberosDescriptor.getIdentities(true, null);
      KerberosComponentDescriptor ambariServerKerberosComponentDescriptor = ambariKerberosDescriptor.getComponent("AMBARI_SERVER");

      if (serviceIdentities != null) {
        ambariIdentities.addAll(serviceIdentities);
      }

      if (ambariServerKerberosComponentDescriptor != null) {
        List<KerberosIdentityDescriptor> componentIdentities = ambariServerKerberosComponentDescriptor.getIdentities(true, null);

        if (componentIdentities != null) {
          ambariIdentities.addAll(componentIdentities);
        }
      }
    }

    return ambariIdentities;
  }

  @Override
  public boolean createAmbariIdentities(Map<String, String> kerberosEnvProperties) {
    return (kerberosEnvProperties == null) || !"false".equalsIgnoreCase(kerberosEnvProperties.get(CREATE_AMBARI_PRINCIPAL));
  }

  /**
   * Gets the previously stored KDC administrator credential.
   * <p/>
   * This implementation accesses the secure CredentialStoreService instance to get the data.
   *
   * @param clusterName the name of the relevant cluster
   * @return a PrincipalKeyCredential or null, if the KDC administrator credential is not available
   * @throws AmbariException if an error occurs while retrieving the credentials
   */
  @Override
  public PrincipalKeyCredential getKDCAdministratorCredentials(String clusterName) throws AmbariException {
    Credential credentials = credentialStoreService.getCredential(clusterName, KDC_ADMINISTRATOR_CREDENTIAL_ALIAS);

    if (credentials instanceof PrincipalKeyCredential) {
      return (PrincipalKeyCredential) credentials;
    } else {
      return null;
    }
  }

  /**
   * Creates the principal and cached keytab file for the specified identity, if it is determined to
   * be of the expected type - user (headless) or service.
   * <p/>
   * If the identity is not of the expected type, it will be skipped.
   *
   * @param identityDescriptor       the Kerberos identity to process
   * @param expectedType             the expected principal type
   * @param kerberosEnvProperties    the kerberos-env properties
   * @param kerberosOperationHandler the relevant KerberosOperationHandler
   * @param configurations           the existing configurations for the cluster
   * @param hostname                 the hostname of the host to create the identity for (nullable)
   * @return the relevant keytab data, if successful; otherwise null
   * @throws AmbariException
   */
  private Keytab createIdentity(KerberosIdentityDescriptor identityDescriptor,
                                KerberosPrincipalType expectedType, Map<String, String> kerberosEnvProperties,
                                KerberosOperationHandler kerberosOperationHandler,
                                Map<String, Map<String, String>> configurations, String hostname)
      throws AmbariException {

    Keytab keytab = null;

    if (identityDescriptor != null) {
      KerberosPrincipalDescriptor principalDescriptor = identityDescriptor.getPrincipalDescriptor();

      if (principalDescriptor != null) {
        // If this principal type is expected, continue, else skip it.
        if (expectedType == principalDescriptor.getType()) {
          String principal = variableReplacementHelper.replaceVariables(principalDescriptor.getValue(), configurations);

          // Replace _HOST with the supplied hostname is either exist
          if (!StringUtils.isEmpty(hostname)) {
            principal = principal.replace("_HOST", hostname);
          }

          // If this principal is already in the Ambari database, then don't try to recreate it or it's
          // keytab file.
          if (!kerberosPrincipalDAO.exists(principal)) {
            CreatePrincipalsServerAction.CreatePrincipalResult result;

            result = injector.getInstance(CreatePrincipalsServerAction.class).createPrincipal(
                principal,
                KerberosPrincipalType.SERVICE.equals(expectedType),
                kerberosEnvProperties,
                kerberosOperationHandler,
                false,
                null);

            if (result == null) {
              throw new AmbariException("Failed to create the account for " + principal);
            } else {
              KerberosKeytabDescriptor keytabDescriptor = identityDescriptor.getKeytabDescriptor();

              if (keytabDescriptor != null) {
                keytab = injector.getInstance(CreateKeytabFilesServerAction.class).createKeytab(
                    principal,
                    result.getPassword(),
                    result.getKeyNumber(),
                    kerberosOperationHandler,
                    true,
                    true,
                    null);

                if (keytab == null) {
                  throw new AmbariException("Failed to create the keytab for " + principal);
                }
              }
            }
          }
        }
      }
    }

    return keytab;
  }

  /**
   * Validate the KDC admin credentials.
   *
   * @param kerberosDetails the KerberosDetails containing information about the Kerberos configuration
   *                        for the cluster, if null, a new KerberosDetails will be created based on
   *                        information found in the associated cluster
   * @param cluster         associated cluster
   * @throws AmbariException if any other error occurs while trying to validate the credentials
   */
  private void validateKDCCredentials(KerberosDetails kerberosDetails, Cluster cluster) throws KerberosMissingAdminCredentialsException,
      KerberosAdminAuthenticationException,
      KerberosInvalidConfigurationException,
      AmbariException {

    if (kerberosDetails == null) {
      kerberosDetails = getKerberosDetails(cluster, null);
    }

    if (kerberosDetails.manageIdentities()) {
      PrincipalKeyCredential credentials = getKDCAdministratorCredentials(cluster.getClusterName());
      if (credentials == null) {
        throw new KerberosMissingAdminCredentialsException();
      } else {
        KerberosOperationHandler operationHandler = kerberosOperationHandlerFactory.getKerberosOperationHandler(kerberosDetails.getKdcType());

        if (operationHandler == null) {
          throw new AmbariException("Failed to get an appropriate Kerberos operation handler.");
        } else {
          boolean missingCredentials = false;
          try {
            operationHandler.open(credentials, kerberosDetails.getDefaultRealm(), kerberosDetails.getKerberosEnvProperties());
            // todo: this is really odd that open doesn't throw an exception if the credentials are missing
            missingCredentials = !operationHandler.testAdministratorCredentials();
          } catch (KerberosAdminAuthenticationException e) {
            throw new KerberosAdminAuthenticationException(
                "Invalid KDC administrator credentials.\n" +
                    "The KDC administrator credentials must be set as a persisted or temporary credential resource." +
                    "This may be done by issuing a POST (or PUT for updating) to the /api/v1/clusters/:clusterName/credentials/kdc.admin.credential API entry point with the following payload:\n" +
                    "{\n" +
                    "  \"Credential\" : {\n" +
                    "    \"principal\" : \"(PRINCIPAL)\", \"key\" : \"(PASSWORD)\", \"type\" : \"(persisted|temporary)\"}\n" +
                    "  }\n" +
                    "}", e);
          } catch (KerberosKDCConnectionException e) {
            throw new KerberosInvalidConfigurationException(
                "Failed to connect to KDC - " + e.getMessage() + "\n" +
                    "Update the KDC settings in krb5-conf and kerberos-env configurations to correct this issue.",
                e);
          } catch (KerberosRealmException e) {
            throw new KerberosInvalidConfigurationException(
                "Failed to find a KDC for the specified realm - " + e.getMessage() + "\n" +
                    "Update the KDC settings in krb5-conf and kerberos-env configurations to correct this issue.",
                e);
          } catch (KerberosLDAPContainerException e) {
            throw new KerberosInvalidConfigurationException(
                "The principal container was not specified\n" +
                    "Set the 'container_dn' value in the kerberos-env configuration to correct this issue.",
                e);
          } catch (KerberosOperationException e) {
            throw new AmbariException(e.getMessage(), e);
          } finally {
            try {
              operationHandler.close();
            } catch (KerberosOperationException e) {
              // Ignore this...
            }
          }

          // need to throw this outside of the try/catch so it isn't caught
          if (missingCredentials) {
            throw new KerberosMissingAdminCredentialsException();
          }
        }
      }
    }
  }

  /**
   * Performs operations needed to process Kerberos related tasks on the relevant cluster.
   * <p/>
   * Iterates through the components installed on the relevant cluster to determine if work
   * need to be done.  Calls into the Handler implementation to provide guidance and set up stages
   * to perform the work needed to complete the relative action.
   *
   * @param cluster                        the relevant Cluster
   * @param kerberosDetails                a KerberosDetails containing information about relevant Kerberos configuration
   * @param serviceComponentFilter         a Map of service names to component names indicating the relevant
   *                                       set of services and components - if null, no filter is relevant;
   *                                       if empty, the filter indicates no relevant services or components
   * @param hostFilter                     a set of hostname indicating the set of hosts to process -
   *                                       if null, no filter is relevant; if empty, the filter indicates no
   *                                       relevant hosts
   * @param identityFilter                 a Collection of identity names indicating the relevant identities -
   *                                       if null, no filter is relevant; if empty, the filter indicates no
   *                                       relevant identities
   * @param hostsToForceKerberosOperations a set of host names on which it is expected that the
   *                                       Kerberos client is or will be in the INSTALLED state by
   *                                       the time the operations targeted for them are to be
   *                                       executed - if empty or null, this no hosts will be
   *                                       "forced"
   * @param requestStageContainer          a RequestStageContainer to place generated stages, if needed -
   *                                       if null a new RequestStageContainer will be created.
   * @param handler                        a Handler to use to provide guidance and set up stages
   *                                       to perform the work needed to complete the relative action
   * @return the updated or a new RequestStageContainer containing the stages that need to be
   * executed to complete this task; or null if no stages need to be executed.
   * @throws AmbariException
   * @throws KerberosInvalidConfigurationException if an issue occurs trying to get the
   *                                               Kerberos-specific configuration details
   */
  @Transactional
  RequestStageContainer handle(Cluster cluster,
                               KerberosDetails kerberosDetails,
                               Map<String, ? extends Collection<String>> serviceComponentFilter,
                               Set<String> hostFilter, Collection<String> identityFilter,
                               Set<String> hostsToForceKerberosOperations,
                               RequestStageContainer requestStageContainer,
                               final Handler handler)
      throws AmbariException, KerberosOperationException {

    final KerberosDescriptor kerberosDescriptor = getKerberosDescriptor(cluster);
    final SecurityState desiredSecurityState = handler.getNewServiceSecurityState();

    List<ServiceComponentHost> schToProcess = getServiceComponentHostsToProcess(
        cluster,
        kerberosDescriptor,
        serviceComponentFilter,
        hostFilter,
        identityFilter,
        new Command<Boolean, ServiceComponentHost>() {
          @Override
          public Boolean invoke(ServiceComponentHost arg) throws AmbariException {
            return handler.shouldProcess(desiredSecurityState, arg);
          }
        });


    // While iterating over all the ServiceComponentHosts find hosts that have KERBEROS_CLIENT
    // components in the INSTALLED state and add them to the hostsWithValidKerberosClient Set.
    // This is needed to help determine which hosts to perform actions for and create tasks for.
    Set<String> hostsWithValidKerberosClient = null;

    // Create a temporary directory to store metadata needed to complete this task.  Information
    // such as which principals and keytabs files to create as well as what configurations need
    // to be update are stored in data files in this directory. Any keytab files are stored in
    // this directory until they are distributed to their appropriate hosts.
    File dataDirectory = null;

    // If there are ServiceComponentHosts to process...
    if (!schToProcess.isEmpty()) {

      validateKDCCredentials(kerberosDetails, cluster);

      // Create a temporary directory to store metadata needed to complete this task.  Information
      // such as which principals and keytabs files to create as well as what configurations need
      // to be update are stored in data files in this directory. Any keytab files are stored in
      // this directory until they are distributed to their appropriate hosts.
      dataDirectory = createTemporaryDirectory();

      hostsWithValidKerberosClient = getHostsWithValidKerberosClient(cluster);

      // Ensure that that hosts that should be assumed to be in the correct state when needed are
      // in the hostsWithValidKerberosClient collection.
      if (hostsToForceKerberosOperations != null) {
        hostsWithValidKerberosClient.addAll(hostsToForceKerberosOperations);
      }
    }

    // Always set up the necessary stages to perform the tasks needed to complete the operation.
    // Some stages may be no-ops, this is expected.
    // Gather data needed to create stages and tasks...
    Map<String, Set<String>> clusterHostInfo = StageUtils.getClusterHostInfo(cluster);
    String clusterHostInfoJson = StageUtils.getGson().toJson(clusterHostInfo);
    Map<String, String> hostParams = customCommandExecutionHelper.createDefaultHostParams(cluster);
    String hostParamsJson = StageUtils.getGson().toJson(hostParams);
    String ambariServerHostname = StageUtils.getHostName();
    ServiceComponentHostServerActionEvent event = new ServiceComponentHostServerActionEvent(
        "AMBARI_SERVER",
        ambariServerHostname, // TODO: Choose a random hostname from the cluster. All tasks for the AMBARI_SERVER service will be executed on this Ambari server
        System.currentTimeMillis());
    RoleCommandOrder roleCommandOrder = ambariManagementController.getRoleCommandOrder(cluster);

    // If a RequestStageContainer does not already exist, create a new one...
    if (requestStageContainer == null) {
      requestStageContainer = new RequestStageContainer(
          actionManager.getNextRequestId(),
          null,
          requestFactory,
          actionManager);
    }

    // Use the handler implementation to setup the relevant stages.
    handler.createStages(cluster, clusterHostInfoJson,
        hostParamsJson, event, roleCommandOrder, kerberosDetails, dataDirectory,
        requestStageContainer, schToProcess, serviceComponentFilter, hostFilter, identityFilter,
        hostsWithValidKerberosClient);

    // Add the finalize stage...
    handler.addFinalizeOperationStage(cluster, clusterHostInfoJson, hostParamsJson, event,
        dataDirectory, roleCommandOrder, requestStageContainer, kerberosDetails);

    // If all goes well, set the appropriate states on the relevant ServiceComponentHosts
    for (ServiceComponentHost sch : schToProcess) {
      // Update the desired and current states for the ServiceComponentHost
      // using new state information from the the handler implementation
      SecurityState newSecurityState;

      newSecurityState = handler.getNewDesiredSCHSecurityState();
      if (newSecurityState != null) {
        sch.setDesiredSecurityState(newSecurityState);
      }

      newSecurityState = handler.getNewSCHSecurityState();
      if (newSecurityState != null) {
        sch.setSecurityState(newSecurityState);
      }
    }

    // If all goes well, set all services to _desire_ to be secured or unsecured, depending on handler
    if (desiredSecurityState != null) {
      Map<String, Service> services = cluster.getServices();

      for (Service service : services.values()) {
        if ((serviceComponentFilter == null) || serviceComponentFilter.containsKey(service.getName())) {
          service.setSecurityState(desiredSecurityState);
        }
      }
    }

    return requestStageContainer;
  }


  /**
   * Performs operations needed to process Kerberos related tasks to manage a (unique) test identity
   * on the relevant cluster.
   * <p/>
   * If Ambari is not managing Kerberos identities, than this method does nothing.
   *
   * @param cluster               the relevant Cluster
   * @param kerberosDetails       a KerberosDetails containing information about relevant Kerberos
   *                              configuration
   * @param commandParameters     the command parameters map used to read and/or write attributes
   *                              related to this operation
   * @param requestStageContainer a RequestStageContainer to place generated stages, if needed -
   *                              if null a new RequestStageContainer will be created.
   * @param handler               a Handler to use to provide guidance and set up stages
   *                              to perform the work needed to complete the relative action
   * @return the updated or a new RequestStageContainer containing the stages that need to be
   * executed to complete this task; or null if no stages need to be executed.
   * @throws AmbariException
   * @throws KerberosOperationException
   */
  private RequestStageContainer handleTestIdentity(Cluster cluster,
                                                   KerberosDetails kerberosDetails,
                                                   Map<String, String> commandParameters, RequestStageContainer requestStageContainer,
                                                   Handler handler) throws AmbariException, KerberosOperationException {

    if (kerberosDetails.manageIdentities()) {
      if (commandParameters == null) {
        throw new AmbariException("The properties map must not be null.  It is needed to store data related to the service check identity");
      }

      List<ServiceComponentHost> serviceComponentHostsToProcess = new ArrayList<ServiceComponentHost>();
      KerberosDescriptor kerberosDescriptor = getKerberosDescriptor(cluster);
      KerberosIdentityDataFileWriter kerberosIdentityDataFileWriter = null;

      Map<String, String> kerberosDescriptorProperties = kerberosDescriptor.getProperties();

      // This is needed to help determine which hosts to perform actions for and create tasks for.
      Set<String> hostsWithValidKerberosClient = getHostsWithValidKerberosClient(cluster);

      // Create a temporary directory to store metadata needed to complete this task.  Information
      // such as which principals and keytabs files to create as well as what configurations need
      // to be update are stored in data files in this directory. Any keytab files are stored in
      // this directory until they are distributed to their appropriate hosts.
      File dataDirectory = createTemporaryDirectory();

      // Create the file used to store details about principals and keytabs to create
      File identityDataFile = new File(dataDirectory, KerberosIdentityDataFileWriter.DATA_FILE_NAME);

      // Calculate the current non-host-specific configurations. These will be used to replace
      // variables within the Kerberos descriptor data
      Map<String, Map<String, String>> configurations = calculateConfigurations(cluster, null, kerberosDescriptorProperties);

      String principal = variableReplacementHelper.replaceVariables("${kerberos-env/service_check_principal_name}@${realm}", configurations);
      String principalType = "user";

      String keytabFilePath = variableReplacementHelper.replaceVariables("${keytab_dir}/kerberos.service_check.${short_date}.keytab", configurations);
      String keytabFileOwnerName = variableReplacementHelper.replaceVariables("${cluster-env/smokeuser}", configurations);
      String keytabFileOwnerAccess = "rw";
      String keytabFileGroupName = variableReplacementHelper.replaceVariables("${cluster-env/user_group}", configurations);
      String keytabFileGroupAccess = "r";

      // Add the relevant principal name and keytab file data to the command params state
      commandParameters.put("principal_name", principal);
      commandParameters.put("keytab_file", keytabFilePath);

      try {
        // Get a list KERBEROS/KERBEROS_CLIENT ServiceComponentHost objects
        List<ServiceComponentHost> serviceComponentHosts = cluster.getServiceComponentHosts(Service.Type.KERBEROS.name(), Role.KERBEROS_CLIENT.name());

        if ((serviceComponentHosts != null) && !serviceComponentHosts.isEmpty()) {
          kerberosIdentityDataFileWriter = kerberosIdentityDataFileWriterFactory.createKerberosIdentityDataFileWriter(identityDataFile);

          // Iterate over the KERBEROS_CLIENT service component hosts to get the service and
          // component-level Kerberos descriptors in order to determine which principals,
          // keytab files needed to be created or updated.
          for (ServiceComponentHost sch : serviceComponentHosts) {
            if (sch.getState() == State.INSTALLED) {
              String hostname = sch.getHostName();

              kerberosIdentityDataFileWriter.writeRecord(
                  hostname,
                  Service.Type.KERBEROS.name(),
                  Role.KERBEROS_CLIENT.name(),
                  principal,
                  principalType,
                  keytabFilePath,
                  keytabFileOwnerName,
                  keytabFileOwnerAccess,
                  keytabFileGroupName,
                  keytabFileGroupAccess,
                  "false");

              hostsWithValidKerberosClient.add(hostname);
              serviceComponentHostsToProcess.add(sch);
            }
          }
        }

      } catch (IOException e) {
        String message = String.format("Failed to write index file - %s", identityDataFile.getAbsolutePath());
        LOG.error(message);
        throw new AmbariException(message, e);
      } catch (Exception e) {
        // make sure to log what is going wrong
        LOG.error("Failed " + e);
        throw e;
      } finally {
        if (kerberosIdentityDataFileWriter != null) {
          // Make sure the data file is closed
          try {
            kerberosIdentityDataFileWriter.close();
          } catch (IOException e) {
            LOG.warn("Failed to close the index file writer", e);
          }
        }
      }

      // If there are ServiceComponentHosts to process, make sure the administrator credential
      // are available
      if (!serviceComponentHostsToProcess.isEmpty()) {
        try {
          validateKDCCredentials(kerberosDetails, cluster);
        } catch (Exception e) {
          LOG.error("Cannot validate credentials: " + e);
          try {
            FileUtils.deleteDirectory(dataDirectory);
          } catch (Throwable t) {
            LOG.warn(String.format("The data directory (%s) was not deleted due to an error condition - {%s}",
                dataDirectory.getAbsolutePath(), t.getMessage()), t);
          }

          throw e;
        }
      }

      // Always set up the necessary stages to perform the tasks needed to complete the operation.
      // Some stages may be no-ops, this is expected.
      // Gather data needed to create stages and tasks...
      Map<String, Set<String>> clusterHostInfo = StageUtils.getClusterHostInfo(cluster);
      String clusterHostInfoJson = StageUtils.getGson().toJson(clusterHostInfo);
      Map<String, String> hostParams = customCommandExecutionHelper.createDefaultHostParams(cluster);
      String hostParamsJson = StageUtils.getGson().toJson(hostParams);
      String ambariServerHostname = StageUtils.getHostName();
      ServiceComponentHostServerActionEvent event = new ServiceComponentHostServerActionEvent(
          "AMBARI_SERVER",
          ambariServerHostname, // TODO: Choose a random hostname from the cluster. All tasks for the AMBARI_SERVER service will be executed on this Ambari server
          System.currentTimeMillis());
      RoleCommandOrder roleCommandOrder = ambariManagementController.getRoleCommandOrder(cluster);

      // If a RequestStageContainer does not already exist, create a new one...
      if (requestStageContainer == null) {
        requestStageContainer = new RequestStageContainer(
            actionManager.getNextRequestId(),
            null,
            requestFactory,
            actionManager);
      }

      // Use the handler implementation to setup the relevant stages.
      // Set the service/component filter to an empty map since the service/component processing
      // was done above.
      handler.createStages(cluster,
          clusterHostInfoJson, hostParamsJson, event, roleCommandOrder, kerberosDetails,
          dataDirectory, requestStageContainer, serviceComponentHostsToProcess,
          Collections.<String, Collection<String>>emptyMap(), null, null, hostsWithValidKerberosClient);


      handler.addFinalizeOperationStage(cluster, clusterHostInfoJson, hostParamsJson, event,
          dataDirectory, roleCommandOrder, requestStageContainer, kerberosDetails);
    }

    return requestStageContainer;
  }


  /**
   * Gathers the Kerberos-related data from configurations and stores it in a new KerberosDetails
   * instance.
   *
   * @param cluster          the relevant Cluster
   * @param manageIdentities a Boolean value indicating how to override the configured behavior
   *                         of managing Kerberos identities; if null the configured behavior
   *                         will not be overridden
   * @return a new KerberosDetails with the collected configuration data
   * @throws AmbariException
   */
  private KerberosDetails getKerberosDetails(Cluster cluster, Boolean manageIdentities)
      throws KerberosInvalidConfigurationException, AmbariException {

    KerberosDetails kerberosDetails = new KerberosDetails();

    if (cluster == null) {
      String message = "The cluster object is not available";
      LOG.error(message);
      throw new AmbariException(message);
    }

    Config configKrb5Conf = cluster.getDesiredConfigByType("krb5-conf");
    if (configKrb5Conf == null) {
      String message = "The 'krb5-conf' configuration is not available";
      LOG.error(message);
      throw new AmbariException(message);
    }

    Map<String, String> krb5ConfProperties = configKrb5Conf.getProperties();
    if (krb5ConfProperties == null) {
      String message = "The 'krb5-conf' configuration properties are not available";
      LOG.error(message);
      throw new AmbariException(message);
    }

    Config configKerberosEnv = cluster.getDesiredConfigByType("kerberos-env");
    if (configKerberosEnv == null) {
      String message = "The 'kerberos-env' configuration is not available";
      LOG.error(message);
      throw new AmbariException(message);
    }

    Map<String, String> kerberosEnvProperties = configKerberosEnv.getProperties();
    if (kerberosEnvProperties == null) {
      String message = "The 'kerberos-env' configuration properties are not available";
      LOG.error(message);
      throw new AmbariException(message);
    }

    kerberosDetails.setSecurityType(cluster.getSecurityType());
    kerberosDetails.setDefaultRealm(kerberosEnvProperties.get("realm"));

    kerberosDetails.setKerberosEnvProperties(kerberosEnvProperties);

    // If set, override the manage identities behavior
    kerberosDetails.setManageIdentities(manageIdentities);

    String kdcTypeProperty = kerberosEnvProperties.get("kdc_type");
    if ((kdcTypeProperty == null) && kerberosDetails.manageIdentities()) {
      String message = "The 'kerberos-env/kdc_type' value must be set to a valid KDC type";
      LOG.error(message);
      throw new KerberosInvalidConfigurationException(message);
    }

    KDCType kdcType;
    try {
      kdcType = KDCType.translate(kdcTypeProperty);
    } catch (IllegalArgumentException e) {
      String message = String.format("Invalid 'kdc_type' value: %s", kdcTypeProperty);
      LOG.error(message);
      throw new AmbariException(message);
    }

    // Set the KDCType to the the MIT_KDC as a fallback.
    kerberosDetails.setKdcType((kdcType == null) ? KDCType.MIT_KDC : kdcType);

    return kerberosDetails;
  }

  /**
   * Creates a temporary directory within the system temporary directory
   * <p/>
   * The resulting directory is to be removed by the caller when desired.
   *
   * @return a File pointing to the new temporary directory, or null if one was not created
   * @throws AmbariException if a new temporary directory cannot be created
   */
  protected File createTemporaryDirectory() throws AmbariException {
    try {
      File temporaryDirectory = getConfiguredTemporaryDirectory();

      File directory;
      int tries = 0;
      long now = System.currentTimeMillis();

      do {
        directory = new File(temporaryDirectory, String.format("%s%d-%d.d",
            KerberosServerAction.DATA_DIRECTORY_PREFIX, now, tries));

        if ((directory.exists()) || !directory.mkdirs()) {
          directory = null; // Rest and try again...
        } else {
          LOG.debug("Created temporary directory: {}", directory.getAbsolutePath());
        }
      } while ((directory == null) && (++tries < 100));

      if (directory == null) {
        throw new IOException(String.format("Failed to create a temporary directory in %s", temporaryDirectory));
      }

      return directory;
    } catch (IOException e) {
      String message = "Failed to create the temporary data directory.";
      LOG.error(message, e);
      throw new AmbariException(message, e);
    }
  }


  /**
   * Merges the specified configuration property in a map of configuration types.
   * The supplied property is processed to replace variables using the replacement Map.
   * <p/>
   * See {@link VariableReplacementHelper#replaceVariables(String, java.util.Map)}
   * for information on variable replacement.
   *
   * @param configurations             the Map of configuration types to update
   * @param configurationSpecification the config-type/property_name value specifying the property to set
   * @param value                      the value of the property to set
   * @param replacements               a Map of (grouped) replacement values
   * @throws AmbariException
   */
  private void mergeConfiguration(Map<String, Map<String, String>> configurations,
                                  String configurationSpecification,
                                  String value,
                                  Map<String, Map<String, String>> replacements) throws AmbariException {

    if (configurationSpecification != null) {
      String[] parts = configurationSpecification.split("/");
      if (parts.length == 2) {
        String type = parts[0];
        String property = parts[1];

        mergeConfigurations(configurations, type, Collections.singletonMap(property, value), replacements);
      }
    }
  }

  /**
   * Merges configuration from a Map of configuration updates into a main configurations Map.  Each
   * property in the updates Map is processed to replace variables using the replacement Map.
   * <p/>
   * See {@link VariableReplacementHelper#replaceVariables(String, java.util.Map)}
   * for information on variable replacement.
   *
   * @param configurations a Map of configurations
   * @param type           the configuration type
   * @param updates        a Map of property updates
   * @param replacements   a Map of (grouped) replacement values
   * @throws AmbariException
   */
  private void mergeConfigurations(Map<String, Map<String, String>> configurations, String type,
                                   Map<String, String> updates,
                                   Map<String, Map<String, String>> replacements) throws AmbariException {
    if (updates != null) {
      Map<String, String> existingProperties = configurations.get(type);
      if (existingProperties == null) {
        existingProperties = new HashMap<String, String>();
        configurations.put(type, existingProperties);
      }

      for (Map.Entry<String, String> property : updates.entrySet()) {
        existingProperties.put(
            variableReplacementHelper.replaceVariables(property.getKey(), replacements),
            variableReplacementHelper.replaceVariables(property.getValue(), replacements)
        );
      }
    }
  }

  /**
   * Adds identities to the AuthToLocalBuilder.
   *
   * @param authToLocalBuilder the AuthToLocalBuilder to use to build the auth_to_local mapping
   * @param identities         a List of KerberosIdentityDescriptors to process
   * @param identityFilter     a Collection of identity names indicating the relevant identities -
   *                           if null, no filter is relevant; if empty, the filter indicates no
   *                           relevant identities
   * @param configurations     a Map of configurations to use a replacements for variables
   *                           in identity fields
   * @throws org.apache.ambari.server.AmbariException
   */
  private void addIdentities(AuthToLocalBuilder authToLocalBuilder,
                             List<KerberosIdentityDescriptor> identities, Collection<String> identityFilter,
                             Map<String, Map<String, String>> configurations) throws AmbariException {
    if (identities != null) {
      for (KerberosIdentityDescriptor identity : identities) {
        // If there is no filter or the filter contains the current identity's name...
        if ((identityFilter == null) || identityFilter.contains(identity.getName())) {
          KerberosPrincipalDescriptor principalDescriptor = identity.getPrincipalDescriptor();
          if (principalDescriptor != null) {
            authToLocalBuilder.addRule(
                variableReplacementHelper.replaceVariables(principalDescriptor.getValue(), configurations),
                variableReplacementHelper.replaceVariables(principalDescriptor.getLocalUsername(), configurations));
          }
        }
      }
    }
  }

  /**
   * Creates a temporary file within the system temporary directory
   * <p/>
   * The resulting file is to be removed by the caller when desired.
   *
   * @return a File pointing to the new temporary file, or null if one was not created
   * @throws AmbariException if a new temporary directory cannot be created
   */
  protected File createTemporaryFile() throws AmbariException {
    try {
      return File.createTempFile("tmp", ".tmp", getConfiguredTemporaryDirectory());
    } catch (IOException e) {
      String message = "Failed to create a temporary file.";
      LOG.error(message, e);
      throw new AmbariException(message, e);
    }
  }

  /**
   * Gets the configured temporary directory.
   *
   * @return a File pointing to the configured temporary directory
   * @throws IOException
   */
  protected File getConfiguredTemporaryDirectory() throws IOException {
    String tempDirectoryPath = configuration.getServerTempDir();

    if (StringUtils.isEmpty(tempDirectoryPath)) {
      tempDirectoryPath = System.getProperty("java.io.tmpdir");
    }

    if (tempDirectoryPath == null) {
      throw new IOException("The System property 'java.io.tmpdir' does not specify a temporary directory");
    }

    return new File(tempDirectoryPath);
  }

  /**
   * Creates a new stage
   *
   * @param id              the new stage's id
   * @param cluster         the relevant Cluster
   * @param requestId       the relevant request Id
   * @param requestContext  a String describing the stage
   * @param clusterHostInfo JSON-encoded clusterHostInfo structure
   * @param commandParams   JSON-encoded command parameters
   * @param hostParams      JSON-encoded host parameters
   * @return a newly created Stage
   */
  private Stage createNewStage(long id, Cluster cluster, long requestId,
                               String requestContext, String clusterHostInfo,
                               String commandParams, String hostParams) {
    Stage stage = stageFactory.createNew(requestId,
        BASE_LOG_DIR + File.pathSeparator + requestId,
        cluster.getClusterName(),
        cluster.getClusterId(),
        requestContext,
        clusterHostInfo,
        commandParams,
        hostParams);

    stage.setStageId(id);
    return stage;
  }

  /**
   * Creates a new stage with a single task describing the ServerAction class to invoke and the other
   * task-related information.
   *
   * @param id                the new stage's id
   * @param cluster           the relevant Cluster
   * @param requestId         the relevant request Id
   * @param requestContext    a String describing the stage
   * @param clusterHostInfo   JSON-encoded clusterHostInfo structure
   * @param commandParams     JSON-encoded command parameters
   * @param hostParams        JSON-encoded host parameters
   * @param actionClass       The ServeAction class that implements the action to invoke
   * @param event             The relevant ServiceComponentHostServerActionEvent
   * @param commandParameters a Map of command parameters to attach to the task added to the new
   *                          stage
   * @param commandDetail     a String declaring a descriptive name to pass to the action - null or an
   *                          empty string indicates no value is to be set
   * @param timeout           the timeout for the task/action  @return a newly created Stage
   */
  private Stage createServerActionStage(long id, Cluster cluster, long requestId,
                                        String requestContext, String clusterHostInfo,
                                        String commandParams, String hostParams,
                                        Class<? extends ServerAction> actionClass,
                                        ServiceComponentHostServerActionEvent event,
                                        Map<String, String> commandParameters, String commandDetail,
                                        Integer timeout) throws AmbariException {

    Stage stage = createNewStage(id, cluster, requestId, requestContext, clusterHostInfo, commandParams, hostParams);
    stage.addServerActionCommand(actionClass.getName(), null, Role.AMBARI_SERVER_ACTION,
        RoleCommand.EXECUTE, cluster.getClusterName(), event, commandParameters, commandDetail,
        ambariManagementController.findConfigurationTagsWithOverrides(cluster, null), timeout,
        false, false);

    return stage;
  }

  /**
   * Given a Collection of ServiceComponentHosts generates a unique list of hosts.
   *
   * @param serviceComponentHosts a Collection of ServiceComponentHosts from which to to retrieve host names
   * @param allowedStates         a Set of HostStates to use to filter the list of hosts, if null, no filter is applied
   * @return a List of (unique) host names
   * @throws org.apache.ambari.server.AmbariException
   */
  private List<String> createUniqueHostList(Collection<ServiceComponentHost> serviceComponentHosts, Set<HostState> allowedStates)
      throws AmbariException {
    Set<String> hostNames = new HashSet<String>();
    Set<String> visitedHostNames = new HashSet<String>();

    if (serviceComponentHosts != null) {
      for (ServiceComponentHost sch : serviceComponentHosts) {
        String hostname = sch.getHostName();
        if (!visitedHostNames.contains(hostname)) {
          // If allowedStates is null, assume the caller doesnt care about the state of the host
          // so skip the call to get the relevant Host data and just add the host to the list
          if (allowedStates == null) {
            hostNames.add(hostname);
          } else {
            Host host = clusters.getHost(hostname);

            if (allowedStates.contains(host.getState())) {
              hostNames.add(hostname);
            }
          }

          visitedHostNames.add(hostname);
        }
      }
    }

    return new ArrayList<String>(hostNames);
  }

  @Override
  public boolean isClusterKerberosEnabled(Cluster cluster) {
    return cluster.getSecurityType() == SecurityType.KERBEROS;
  }

  @Override
  public boolean shouldExecuteCustomOperations(SecurityType requestSecurityType, Map<String, String> requestProperties) {

    if (((requestSecurityType == SecurityType.KERBEROS) || (requestSecurityType == SecurityType.NONE)) &&
        (requestProperties != null) && !requestProperties.isEmpty()) {
      for (SupportedCustomOperation type : SupportedCustomOperation.values()) {
        if (requestProperties.containsKey(type.name().toLowerCase())) {
          return true;
        }
      }
    }
    return false;
  }

  @Override
  public Boolean getManageIdentitiesDirective(Map<String, String> requestProperties) {
    String value = (requestProperties == null) ? null : requestProperties.get(DIRECTIVE_MANAGE_KERBEROS_IDENTITIES);

    return (value == null)
        ? null
        : !"false".equalsIgnoreCase(value);
  }

  @Override
  public boolean getForceToggleKerberosDirective(Map<String, String> requestProperties) {
    return (requestProperties != null) && "true".equalsIgnoreCase(requestProperties.get(DIRECTIVE_FORCE_TOGGLE_KERBEROS));
  }

  @Override
  public Map<String, Map<String, String>> getIdentityConfigurations(List<KerberosIdentityDescriptor> identityDescriptors) {
    Map<String, Map<String, String>> map = new HashMap<String, Map<String, String>>();

    if (identityDescriptors != null) {
      for (KerberosIdentityDescriptor identityDescriptor : identityDescriptors) {
        KerberosPrincipalDescriptor principalDescriptor = identityDescriptor.getPrincipalDescriptor();
        if (principalDescriptor != null) {
          putConfiguration(map, principalDescriptor.getConfiguration(), principalDescriptor.getValue());
        }

        KerberosKeytabDescriptor keytabDescriptor = identityDescriptor.getKeytabDescriptor();
        if (keytabDescriptor != null) {
          putConfiguration(map, keytabDescriptor.getConfiguration(), keytabDescriptor.getFile());
        }
      }
    }

    return map;
  }

  /**
   * Inserts a configuration property and value into a map of configuration types to property
   * name/value pair maps.
   *
   * @param map           the Map to insert into
   * @param configuration a configuration property in the form of config-type/property_name
   * @param value         the value of the configuration property
   */
  private void putConfiguration(Map<String, Map<String, String>> map, String configuration, String value) {
    if (configuration != null) {
      String[] principalTokens = configuration.split("/");

      if (principalTokens.length == 2) {
        String type = principalTokens[0];
        String propertyName = principalTokens[1];

        Map<String, String> properties = map.get(type);
        if (properties == null) {
          properties = new HashMap<String, String>();
          map.put(type, properties);
        }

        properties.put(propertyName, value);
      }
    }
  }

  /**
   * Returns the active identities for the named service component in the cluster.
   *
   * @param cluster            the relevant cluster (mandatory)
   * @param hostname           the name of a host for which to find results, null indicates all hosts
   * @param serviceName        the name of a service for which to find results, null indicates all
   *                           services
   * @param componentName      the name of a component for which to find results, null indicates all
   *                           components
   * @param kerberosDescriptor the relevant Kerberos Descriptor
   *                           requested service component
   * @param filterContext      the context to use for filtering identities based on the state of the cluster
   * @return a list of KerberosIdentityDescriptors representing the active identities for the
   * @throws AmbariException if an error occurs processing the cluster's active identities
   */
  private List<KerberosIdentityDescriptor> getActiveIdentities(Cluster cluster,
                                                               String hostname,
                                                               String serviceName,
                                                               String componentName,
                                                               KerberosDescriptor kerberosDescriptor,
                                                               Map<String, Object> filterContext)
      throws AmbariException {

    List<KerberosIdentityDescriptor> identities = new ArrayList<KerberosIdentityDescriptor>();

    List<ServiceComponentHost> serviceComponentHosts = cluster.getServiceComponentHosts(hostname);

    if (serviceComponentHosts != null) {
      for (ServiceComponentHost serviceComponentHost : serviceComponentHosts) {
        String schServiceName = serviceComponentHost.getServiceName();
        String schComponentName = serviceComponentHost.getServiceComponentName();

        if (((serviceName == null) || serviceName.equals(schServiceName)) &&
            ((componentName == null) || componentName.equals(schComponentName))) {

          KerberosServiceDescriptor serviceDescriptor = kerberosDescriptor.getService(schServiceName);

          if (serviceDescriptor != null) {
            List<KerberosIdentityDescriptor> serviceIdentities = serviceDescriptor.getIdentities(true, filterContext);
            if (serviceIdentities != null) {
              identities.addAll(serviceIdentities);
            }

            KerberosComponentDescriptor componentDescriptor = serviceDescriptor.getComponent(schComponentName);
            if (componentDescriptor != null) {
              List<KerberosIdentityDescriptor> componentIdentities = componentDescriptor.getIdentities(true, filterContext);
              if (componentIdentities != null) {
                identities.addAll(componentIdentities);
              }
            }
          }
        }
      }
    }

    return identities;
  }

  /**
   * Determines the existing configurations for the cluster, related to a given hostname (if provided)
   *
   * @param cluster  the cluster
   * @param hostname a hostname
   * @return a map of the existing configurations
   * @throws AmbariException
   */
  private Map<String, Map<String, String>> calculateExistingConfigurations(Cluster cluster, String hostname) throws AmbariException {
    // For a configuration type, both tag and an actual configuration can be stored
    // Configurations from the tag is always expanded and then over-written by the actual
    // global:version1:{a1:A1,b1:B1,d1:D1} + global:{a1:A2,c1:C1,DELETED_d1:x} ==>
    // global:{a1:A2,b1:B1,c1:C1}
    Map<String, Map<String, String>> configurations = new HashMap<String, Map<String, String>>();
    Map<String, Map<String, String>> configurationTags = ambariManagementController.findConfigurationTagsWithOverrides(cluster, hostname);

    Map<String, Map<String, String>> configProperties = configHelper.getEffectiveConfigProperties(cluster, configurationTags);

    // Apply the configurations saved with the Execution Cmd on top of
    // derived configs - This will take care of all the hacks
    for (Map.Entry<String, Map<String, String>> entry : configProperties.entrySet()) {
      String type = entry.getKey();
      Map<String, String> allLevelMergedConfig = entry.getValue();
      Map<String, String> configuration = configurations.get(type);

      if (configuration == null) {
        configuration = new HashMap<String, String>(allLevelMergedConfig);
      } else {
        Map<String, String> mergedConfig = configHelper.getMergedConfig(allLevelMergedConfig, configuration);
        configuration.clear();
        configuration.putAll(mergedConfig);
      }

      configurations.put(type, configuration);
    }

    return configurations;
  }

  /**
   * Add configurations related to Kerberos, to a previously created map of configurations.
   * <p/>
   * The supplied map of configurations is expected to be mutable and will be altered.
   *
   * @param cluster                      the cluster
   * @param configurations               a map of configurations
   * @param hostname                     a hostname
   * @param kerberosDescriptorProperties the Kerberos descriptor properties
   * @return the supplied map of configurations with updates applied
   * @throws AmbariException
   */
  private Map<String, Map<String, String>> addAdditionalConfigurations(Cluster cluster, Map<String, Map<String, String>> configurations,
                                                                       String hostname, Map<String, String> kerberosDescriptorProperties)
      throws AmbariException {

    // A map to hold un-categorized properties.  This may come from the KerberosDescriptor
    // and will also contain a value for the current host
    Map<String, String> generalProperties = configurations.get("");
    if (generalProperties == null) {
      generalProperties = new HashMap<String, String>();
      configurations.put("", generalProperties);
    }

    // If any properties are set in the calculated KerberosDescriptor, add them into the
    // Map of configurations as an un-categorized type (using an empty string)
    if (kerberosDescriptorProperties != null) {
      generalProperties.putAll(kerberosDescriptorProperties);
    }

    if (!StringUtils.isEmpty(hostname)) {
      // Add the current hostname under "host" and "hostname"
      generalProperties.put("host", hostname);
      generalProperties.put("hostname", hostname);
    }

    // Add the current cluster's name
    generalProperties.put("cluster_name", cluster.getClusterName());

    // Add the current date in short format (MMddyy)
    generalProperties.put("short_date", new SimpleDateFormat("MMddyy").format(new Date()));

    // add clusterHostInfo config
    if (configurations.get("clusterHostInfo") == null) {
      Map<String, Set<String>> clusterHostInfo = StageUtils.getClusterHostInfo(cluster);

      if (clusterHostInfo != null) {
        Map<String, String> componentHosts = new HashMap<String, String>();

        clusterHostInfo = StageUtils.substituteHostIndexes(clusterHostInfo);

        for (Map.Entry<String, Set<String>> entry : clusterHostInfo.entrySet()) {
          componentHosts.put(entry.getKey(), StringUtils.join(entry.getValue(), ","));
        }

        configurations.put("clusterHostInfo", componentHosts);
      }
    }

    return configurations;
  }

  /**
   * Creates a deep copy of a map of maps, typically used to copy configuration sets.
   *
   * @param map the map to copy
   * @return a deep copy of the supplied map
   */
  private Map<String, Map<String, String>> deepCopy(Map<String, Map<String, String>> map) {
    if (map == null) {
      return null;
    } else {
      Map<String, Map<String, String>> copy = new HashMap<String, Map<String, String>>();

      for (Map.Entry<String, Map<String, String>> entry : map.entrySet()) {
        Map<String, String> innerMap = entry.getValue();
        copy.put(entry.getKey(), (innerMap == null) ? null : new HashMap<String, String>(innerMap));
      }

      return copy;
    }
  }

  /**
   * Get the user-supplied Kerberos descriptor from the set of cluster artifacts
   *
   * @param cluster the cluster
   * @return a Kerberos descriptor
   */
  private KerberosDescriptor getKerberosDescriptorUpdates(Cluster cluster) throws AmbariException {
    // find instance using name and foreign keys
    TreeMap<String, String> foreignKeys = new TreeMap<String, String>();
    foreignKeys.put("cluster", String.valueOf(cluster.getClusterId()));

    ArtifactEntity entity = artifactDAO.findByNameAndForeignKeys("kerberos_descriptor", foreignKeys);
    return (entity == null) ? null : kerberosDescriptorFactory.createInstance(entity.getArtifactData());
  }

  /**
   * Get the default Kerberos descriptor from the stack, which is the same as the value from
   * <code>stacks/:stackName/versions/:version/artifacts/kerberos_descriptor</code>
   *
   * @param cluster the cluster
   * @return a Kerberos Descriptor
   * @throws AmbariException if an error occurs while retrieving the Kerberos descriptor
   */
  private KerberosDescriptor getKerberosDescriptorFromStack(Cluster cluster) throws AmbariException {
    StackId stackId = cluster.getCurrentStackVersion();

    // -------------------------------
    // Get the default Kerberos descriptor from the stack, which is the same as the value from
    // stacks/:stackName/versions/:version/artifacts/kerberos_descriptor
    return ambariMetaInfo.getKerberosDescriptor(stackId.getStackName(), stackId.getStackVersion());
    // -------------------------------
  }

  /**
   * Recursively walk the Kerberos descriptor tree to find all Kerberos identity definitions and
   * determine which should be filtered out.
   *
   * No actual filtering is performed while processing since any referenced Kerberos identities need
   * to be accessible throughout the process. So a map of container path to a list of identities is
   * created an returned
   *
   * @param currentPath
   * @param container
   * @param context
   * @param identitiesToRemove
   * @return
   * @throws AmbariException
   */
  private Map<String,Set<String>> processWhenClauses(String currentPath, AbstractKerberosDescriptorContainer container, Map<String, Object> context, Map<String,Set<String>> identitiesToRemove) throws AmbariException {

    // Get the list of this container's identities.
    // Do not filter these identities using KerberosIdentityDescriptor#shouldInclude since we will do
    // that later.
    List<KerberosIdentityDescriptor> identities = container.getIdentities(true, null);

    if((identities != null) && !identities.isEmpty()) {
      Set<String> set = null;

      for (KerberosIdentityDescriptor identity : identities) {
        if (!identity.shouldInclude(context)) {
          if (set == null) {
            set = new HashSet<String>();
            identitiesToRemove.put(currentPath, set);
          }

          set.add(identity.getName());
        }
      }
    }

    Collection<? extends AbstractKerberosDescriptorContainer> children = container.getChildContainers();
    if(children != null) {
      for(AbstractKerberosDescriptorContainer child: children) {
        identitiesToRemove = processWhenClauses(currentPath + "/" + child.getName(), child, context, identitiesToRemove);
      }
    }

    return identitiesToRemove;
  }

  /**
   * Processes the configuration values related to a particular Kerberos descriptor identity definition
   * by:
   * <ol>
   * <li>
   * merging the declared properties and their values from <code>identityConfigurations</code> with the set of
   * Kerberos-related configuration updates in <code>kerberosConfigurations</code>, using the existing cluster
   * configurations in <code>configurations</code>
   * </li>
   * <li>
   * ensuring that these properties are not overwritten by recommendations by the stack advisor later
   * in the workflow by adding them to the <code>propertiesToIgnore</code> map
   * </li>
   * </ol>
   *
   * @param identityConfigurations a map of config-types to property name/value pairs to process
   * @param kerberosConfigurations a map of config-types to property name/value pairs to be applied
   *                               as configuration updates
   * @param configurations         a map of config-types to property name/value pairs representing
   *                               the existing configurations for the cluster
   * @param propertiesToIgnore     a map of config-types to property names to be ignored while
   *                               processing stack advisor recommendations
   * @throws AmbariException
   */
  private void processIdentityConfigurations(Map<String, Map<String, String>> identityConfigurations,
                                             Map<String, Map<String, String>> kerberosConfigurations,
                                             Map<String, Map<String, String>> configurations,
                                             Map<String, Set<String>> propertiesToIgnore)
      throws AmbariException {
    if (identityConfigurations != null) {
      for (Map.Entry<String, Map<String, String>> identitiyEntry : identityConfigurations.entrySet()) {
        String configType = identitiyEntry.getKey();
        Map<String, String> properties = identitiyEntry.getValue();

        mergeConfigurations(kerberosConfigurations, configType, identitiyEntry.getValue(), configurations);

        if ((properties != null) && !properties.isEmpty()) {
          Set<String> propertyNames = propertiesToIgnore.get(configType);
          if (propertyNames == null) {
            propertyNames = new HashSet<String>();
            propertiesToIgnore.put(configType, propertyNames);
          }
          propertyNames.addAll(properties.keySet());
        }
      }
    }

  }

  /* ********************************************************************************************
   * Helper classes and enums
   * ******************************************************************************************** *\

  /**
   * A enumeration of the supported custom operations
   */
  public enum SupportedCustomOperation {
    REGENERATE_KEYTABS
  }

  /**
   * Handler is an interface that needs to be implemented by toggle handler classes to do the
   * "right" thing for the task at hand.
   */
  private abstract class Handler {
    /**
     * Tests the Service and ServiceComponentHost to see if they are in the appropriate security
     * state to be processed for the relevant task.
     *
     * @param desiredSecurityState the SecurityState to be transitioned to
     * @param sch                  the ServiceComponentHost to test
     * @return true if both the Service and ServiceComponentHost are in the appropriate security
     * state to be processed; otherwise false
     * @throws AmbariException of an error occurs while testing
     */
    abstract boolean shouldProcess(SecurityState desiredSecurityState, ServiceComponentHost sch) throws AmbariException;

    /**
     * Returns the new SecurityState to be set as the ServiceComponentHost's _desired_ SecurityState.
     *
     * @return a SecurityState to be set as the ServiceComponentHost's _desired_ SecurityState;
     * or null if no state change is desired
     */
    abstract SecurityState getNewDesiredSCHSecurityState();

    /**
     * Returns the new SecurityState to be set as the ServiceComponentHost's _current_ SecurityState.
     *
     * @return a SecurityState to be set as the ServiceComponentHost's _current_ SecurityState;
     * or null if no state change is desired
     */
    abstract SecurityState getNewSCHSecurityState();


    /**
     * Returns the new SecurityState to be set as the Service's SecurityState.
     *
     * @return a SecurityState to be set as the Service's SecurityState;
     * or null if no state change is desired
     */
    abstract SecurityState getNewServiceSecurityState();

    /**
     * Creates the necessary stages to complete the relevant task and stores them in the supplied
     * or a newly created RequestStageContainer.
     * <p/>
     * If the supplied RequestStageContainer is null, a new one must be created and filled.
     * {@link org.apache.ambari.server.controller.internal.RequestStageContainer#persist()} should
     * not be called since it is not known if the set of states for this container is complete.
     *
     * @param cluster                the relevant Cluster
     * @param clusterHostInfo        JSON-encoded clusterHostInfo structure
     * @param hostParams             JSON-encoded host parameters
     * @param event                  a ServiceComponentHostServerActionEvent to pass to any created tasks
     * @param roleCommandOrder       the RoleCommandOrder to use to generate the RoleGraph for any newly created Stages
     * @param kerberosDetails        a KerberosDetails containing the information about the relevant Kerberos configuration
     * @param dataDirectory          a File pointing to the (temporary) data directory
     * @param requestStageContainer  a RequestStageContainer to store the new stages in, if null a
     *                               new RequestStageContainer will be created
     * @param serviceComponentHosts  a List of ServiceComponentHosts that needs to be updated as part of this operation
     * @param serviceComponentFilter a Map of service names to component names indicating the relevant
     *                               set of services and components - if null, no filter is relevant;
     *                               if empty, the filter indicates no relevant services or components
     * @param hostFilter             a set of hostname indicating the set of hosts to process -
     *                               if null, no filter is relevant; if empty, the filter indicates no
     *                               relevant hosts
     * @param identityFilter         a Collection of identity names indicating the relevant identities -
     *                               if null, no filter is relevant; if empty, the filter indicates no
     *                               relevant identities
     * @return the last stage id generated, or -1 if no stages were created
     * @throws AmbariException if an error occurs while creating the relevant stages
     */
    abstract long createStages(Cluster cluster,
                               String clusterHostInfo, String hostParams,
                               ServiceComponentHostServerActionEvent event,
                               RoleCommandOrder roleCommandOrder,
                               KerberosDetails kerberosDetails, File dataDirectory,
                               RequestStageContainer requestStageContainer,
                               List<ServiceComponentHost> serviceComponentHosts,
                               Map<String, ? extends Collection<String>> serviceComponentFilter,
                               Set<String> hostFilter, Collection<String> identityFilter,
                               Set<String> hostsWithValidKerberosClient)
        throws AmbariException;


    public void addPrepareEnableKerberosOperationsStage(Cluster cluster, String clusterHostInfoJson,
                                                        String hostParamsJson, ServiceComponentHostServerActionEvent event,
                                                        Map<String, String> commandParameters,
                                                        RoleCommandOrder roleCommandOrder, RequestStageContainer requestStageContainer)
        throws AmbariException {
      Stage stage = createServerActionStage(requestStageContainer.getLastStageId(),
          cluster,
          requestStageContainer.getId(),
          "Preparing Operations",
          clusterHostInfoJson,
          "{}",
          hostParamsJson,
          PrepareEnableKerberosServerAction.class,
          event,
          commandParameters,
          "Preparing Operations",
          configuration.getDefaultServerTaskTimeout());

      RoleGraph roleGraph = roleGraphFactory.createNew(roleCommandOrder);
      roleGraph.build(stage);
      requestStageContainer.addStages(roleGraph.getStages());
    }

    public void addPrepareKerberosIdentitiesStage(Cluster cluster, String clusterHostInfoJson,
                                                  String hostParamsJson, ServiceComponentHostServerActionEvent event,
                                                  Map<String, String> commandParameters,
                                                  RoleCommandOrder roleCommandOrder, RequestStageContainer requestStageContainer)
        throws AmbariException {
      Stage stage = createServerActionStage(requestStageContainer.getLastStageId(),
          cluster,
          requestStageContainer.getId(),
          "Preparing Operations",
          clusterHostInfoJson,
          "{}",
          hostParamsJson,
          PrepareKerberosIdentitiesServerAction.class,
          event,
          commandParameters,
          "Preparing Operations",
          configuration.getDefaultServerTaskTimeout());

      RoleGraph roleGraph = roleGraphFactory.createNew(roleCommandOrder);
      roleGraph.build(stage);
      requestStageContainer.addStages(roleGraph.getStages());
    }

    public void addPrepareDisableKerberosOperationsStage(Cluster cluster, String clusterHostInfoJson,
                                                         String hostParamsJson, ServiceComponentHostServerActionEvent event,
                                                         Map<String, String> commandParameters,
                                                         RoleCommandOrder roleCommandOrder, RequestStageContainer requestStageContainer)
        throws AmbariException {
      Stage stage = createServerActionStage(requestStageContainer.getLastStageId(),
          cluster,
          requestStageContainer.getId(),
          "Preparing Operations",
          clusterHostInfoJson,
          "{}",
          hostParamsJson,
          PrepareDisableKerberosServerAction.class,
          event,
          commandParameters,
          "Preparing Operations",
          configuration.getDefaultServerTaskTimeout());

      RoleGraph roleGraph = roleGraphFactory.createNew(roleCommandOrder);
      roleGraph.build(stage);
      requestStageContainer.addStages(roleGraph.getStages());
    }

    public void addCreatePrincipalsStage(Cluster cluster, String clusterHostInfoJson,
                                         String hostParamsJson, ServiceComponentHostServerActionEvent event,
                                         Map<String, String> commandParameters,
                                         RoleCommandOrder roleCommandOrder, RequestStageContainer requestStageContainer)
        throws AmbariException {
      Stage stage = createServerActionStage(requestStageContainer.getLastStageId(),
          cluster,
          requestStageContainer.getId(),
          "Create Principals",
          clusterHostInfoJson,
          "{}",
          hostParamsJson,
          CreatePrincipalsServerAction.class,
          event,
          commandParameters,
          "Create Principals",
          Math.max(ServerAction.DEFAULT_LONG_RUNNING_TASK_TIMEOUT_SECONDS, configuration.getDefaultServerTaskTimeout()));

      RoleGraph roleGraph = roleGraphFactory.createNew(roleCommandOrder);
      roleGraph.build(stage);
      requestStageContainer.addStages(roleGraph.getStages());
    }

    public void addDestroyPrincipalsStage(Cluster cluster, String clusterHostInfoJson,
                                          String hostParamsJson, ServiceComponentHostServerActionEvent event,
                                          Map<String, String> commandParameters,
                                          RoleCommandOrder roleCommandOrder, RequestStageContainer requestStageContainer)
        throws AmbariException {
      Stage stage = createServerActionStage(requestStageContainer.getLastStageId(),
          cluster,
          requestStageContainer.getId(),
          "Destroy Principals",
          clusterHostInfoJson,
          "{}",
          hostParamsJson,
          DestroyPrincipalsServerAction.class,
          event,
          commandParameters,
          "Destroy Principals",
          Math.max(ServerAction.DEFAULT_LONG_RUNNING_TASK_TIMEOUT_SECONDS, configuration.getDefaultServerTaskTimeout()));

      RoleGraph roleGraph = roleGraphFactory.createNew(roleCommandOrder);
      roleGraph.build(stage);
      requestStageContainer.addStages(roleGraph.getStages());
    }

    public void addConfigureAmbariIdentityStage(Cluster cluster, String clusterHostInfoJson,
                                                String hostParamsJson, ServiceComponentHostServerActionEvent event,
                                                Map<String, String> commandParameters,
                                                RoleCommandOrder roleCommandOrder, RequestStageContainer requestStageContainer)
        throws AmbariException {
      Stage stage = createServerActionStage(requestStageContainer.getLastStageId(),
          cluster,
          requestStageContainer.getId(),
          "Configure Ambari Identity",
          clusterHostInfoJson,
          "{}",
          hostParamsJson,
          ConfigureAmbariIdentitiesServerAction.class,
          event,
          commandParameters,
          "Configure Ambari Identity",
          configuration.getDefaultServerTaskTimeout());

      RoleGraph roleGraph = roleGraphFactory.createNew(roleCommandOrder);
      roleGraph.build(stage);
      requestStageContainer.addStages(roleGraph.getStages());
    }

    public void addCreateKeytabFilesStage(Cluster cluster, String clusterHostInfoJson,
                                          String hostParamsJson, ServiceComponentHostServerActionEvent event,
                                          Map<String, String> commandParameters,
                                          RoleCommandOrder roleCommandOrder, RequestStageContainer requestStageContainer)
        throws AmbariException {
      Stage stage = createServerActionStage(requestStageContainer.getLastStageId(),
          cluster,
          requestStageContainer.getId(),
          "Create Keytabs",
          clusterHostInfoJson,
          "{}",
          hostParamsJson,
          CreateKeytabFilesServerAction.class,
          event,
          commandParameters,
          "Create Keytabs",
          Math.max(ServerAction.DEFAULT_LONG_RUNNING_TASK_TIMEOUT_SECONDS, configuration.getDefaultServerTaskTimeout()));

      RoleGraph roleGraph = roleGraphFactory.createNew(roleCommandOrder);
      roleGraph.build(stage);
      requestStageContainer.addStages(roleGraph.getStages());
    }

    public void addDistributeKeytabFilesStage(Cluster cluster, List<ServiceComponentHost> serviceComponentHosts,
                                              String clusterHostInfoJson, String hostParamsJson,
                                              Map<String, String> commandParameters,
                                              RoleCommandOrder roleCommandOrder,
                                              RequestStageContainer requestStageContainer,
                                              Set<String> hostsWithValidKerberosClient)
        throws AmbariException {

      Stage stage = createNewStage(requestStageContainer.getLastStageId(),
          cluster,
          requestStageContainer.getId(),
          "Distribute Keytabs",
          clusterHostInfoJson,
          StageUtils.getGson().toJson(commandParameters),
          hostParamsJson);

      Collection<ServiceComponentHost> filteredComponents = filterServiceComponentHostsForHosts(
          new ArrayList<ServiceComponentHost>(serviceComponentHosts), hostsWithValidKerberosClient);

      if (!filteredComponents.isEmpty()) {
        List<String> hostsToUpdate = createUniqueHostList(filteredComponents, Collections.singleton(HostState.HEALTHY));
        Map<String, String> requestParams = new HashMap<String, String>();
        List<RequestResourceFilter> requestResourceFilters = new ArrayList<RequestResourceFilter>();
        RequestResourceFilter reqResFilter = new RequestResourceFilter(Service.Type.KERBEROS.name(), Role.KERBEROS_CLIENT.name(), hostsToUpdate);
        requestResourceFilters.add(reqResFilter);

        ActionExecutionContext actionExecContext = new ActionExecutionContext(
            cluster.getClusterName(),
            "SET_KEYTAB",
            requestResourceFilters,
            requestParams);
        customCommandExecutionHelper.addExecutionCommandsToStage(actionExecContext, stage,
            requestParams);
      }

      RoleGraph roleGraph = roleGraphFactory.createNew(roleCommandOrder);
      roleGraph.build(stage);
      requestStageContainer.addStages(roleGraph.getStages());
    }

    /**
     * Filter out ServiceComponentHosts that are on on hosts in the specified set of host names.
     * <p/>
     * It is expected that the supplied collection is modifiable. It will be modified inplace.
     *
     * @param serviceComponentHosts a collection of ServiceComponentHost items to test
     * @param hosts                 a set of host names indicating valid hosts
     * @return a collection of filtered ServiceComponentHost items
     */
    private Collection<ServiceComponentHost> filterServiceComponentHostsForHosts(Collection<ServiceComponentHost> serviceComponentHosts,
                                                                                 Set<String> hosts) {

      if ((serviceComponentHosts != null) && (hosts != null)) {
        Iterator<ServiceComponentHost> iterator = serviceComponentHosts.iterator();
        while (iterator.hasNext()) {
          ServiceComponentHost sch = iterator.next();

          if (!hosts.contains(sch.getHostName())) {
            iterator.remove();
          }
        }
      }

      return serviceComponentHosts;
    }

    void addDisableSecurityHookStage(Cluster cluster,
                                            String clusterHostInfoJson,
                                            String hostParamsJson,
                                            Map<String, String> commandParameters,
                                            RoleCommandOrder roleCommandOrder,
                                            RequestStageContainer requestStageContainer)
      throws AmbariException
    {
      Stage stage = createNewStage(requestStageContainer.getLastStageId(),
        cluster,
        requestStageContainer.getId(),
        "Disable security",
        clusterHostInfoJson,
        StageUtils.getGson().toJson(commandParameters),
        hostParamsJson);
      addDisableSecurityCommandToAllServices(cluster, stage);
      RoleGraph roleGraph = roleGraphFactory.createNew(roleCommandOrder);
      roleGraph.build(stage);
      requestStageContainer.addStages(roleGraph.getStages());
    }

    private void addDisableSecurityCommandToAllServices(Cluster cluster, Stage stage) throws AmbariException {
      for (Service service : cluster.getServices().values()) {
        for (ServiceComponent component : service.getServiceComponents().values()) {
            if (!component.getServiceComponentHosts().isEmpty()) {
              String firstHost = component.getServiceComponentHosts().keySet().iterator().next(); // it is only necessary to send it to one host
              ActionExecutionContext exec = new ActionExecutionContext(
                cluster.getClusterName(),
                "DISABLE_SECURITY",
                singletonList(new RequestResourceFilter(service.getName(), component.getName(), singletonList(firstHost))),
                Collections.<String, String>emptyMap());
              customCommandExecutionHelper.addExecutionCommandsToStage(exec, stage, Collections.<String, String>emptyMap());
          }
        }
      }
    }

    void addStopZookeeperStage(Cluster cluster,
                                      String clusterHostInfoJson,
                                      String hostParamsJson,
                                      Map<String, String> commandParameters,
                                      RoleCommandOrder roleCommandOrder,
                                      RequestStageContainer requestStageContainer)
      throws AmbariException
    {
      Service zookeeper;
      try {
        zookeeper = cluster.getService("ZOOKEEPER");
      } catch (ServiceNotFoundException e) {
        return;
      }
      Stage stage = createNewStage(requestStageContainer.getLastStageId(),
        cluster,
        requestStageContainer.getId(),
        "Stopping ZooKeeper",
        clusterHostInfoJson,
        StageUtils.getGson().toJson(commandParameters),
        hostParamsJson);
      for (ServiceComponent component : zookeeper.getServiceComponents().values()) {
          Set<String> hosts = component.getServiceComponentHosts().keySet();
          ActionExecutionContext exec = new ActionExecutionContext(
            cluster.getClusterName(),
            "STOP",
            singletonList(new RequestResourceFilter(zookeeper.getName(), component.getName(), new ArrayList<>(hosts))),
            Collections.<String, String>emptyMap());
          customCommandExecutionHelper.addExecutionCommandsToStage(exec, stage, Collections.<String, String>emptyMap());
      }
      RoleGraph roleGraph = roleGraphFactory.createNew(roleCommandOrder);
      roleGraph.build(stage);
      requestStageContainer.addStages(roleGraph.getStages());
    }

    public void addDeleteKeytabFilesStage(Cluster cluster, List<ServiceComponentHost> serviceComponentHosts,
                                          String clusterHostInfoJson, String hostParamsJson,
                                          Map<String, String> commandParameters,
                                          RoleCommandOrder roleCommandOrder,
                                          RequestStageContainer requestStageContainer,
                                          Set<String> hostsWithValidKerberosClient)
        throws AmbariException {

      Stage stage = createNewStage(requestStageContainer.getLastStageId(),
          cluster,
          requestStageContainer.getId(),
          "Delete Keytabs",
          clusterHostInfoJson,
          StageUtils.getGson().toJson(commandParameters),
          hostParamsJson);

      Collection<ServiceComponentHost> filteredComponents = filterServiceComponentHostsForHosts(
          new ArrayList<ServiceComponentHost>(serviceComponentHosts), hostsWithValidKerberosClient);

      if (!filteredComponents.isEmpty()) {
        List<String> hostsToUpdate = createUniqueHostList(filteredComponents, Collections.singleton(HostState.HEALTHY));

        if (!hostsToUpdate.isEmpty()) {
          Map<String, String> requestParams = new HashMap<String, String>();
          List<RequestResourceFilter> requestResourceFilters = new ArrayList<RequestResourceFilter>();
          RequestResourceFilter reqResFilter = new RequestResourceFilter("KERBEROS", "KERBEROS_CLIENT", hostsToUpdate);
          requestResourceFilters.add(reqResFilter);

          ActionExecutionContext actionExecContext = new ActionExecutionContext(
              cluster.getClusterName(),
              "REMOVE_KEYTAB",
              requestResourceFilters,
              requestParams);
          customCommandExecutionHelper.addExecutionCommandsToStage(actionExecContext, stage,
              requestParams);
        }
      }

      RoleGraph roleGraph = roleGraphFactory.createNew(roleCommandOrder);
      roleGraph.build(stage);
      requestStageContainer.addStages(roleGraph.getStages());
    }

    public void addUpdateConfigurationsStage(Cluster cluster, String clusterHostInfoJson,
                                             String hostParamsJson, ServiceComponentHostServerActionEvent event,
                                             Map<String, String> commandParameters,
                                             RoleCommandOrder roleCommandOrder, RequestStageContainer requestStageContainer)
        throws AmbariException {
      Stage stage = createServerActionStage(requestStageContainer.getLastStageId(),
          cluster,
          requestStageContainer.getId(),
          "Update Configurations",
          clusterHostInfoJson,
          "{}",
          hostParamsJson,
          UpdateKerberosConfigsServerAction.class,
          event,
          commandParameters,
          "Update Service Configurations",
          configuration.getDefaultServerTaskTimeout());

      RoleGraph roleGraph = roleGraphFactory.createNew(roleCommandOrder);
      roleGraph.build(stage);
      requestStageContainer.addStages(roleGraph.getStages());
    }

    public void addFinalizeOperationStage(Cluster cluster, String clusterHostInfoJson,
                                          String hostParamsJson, ServiceComponentHostServerActionEvent event,
                                          File dataDirectory, RoleCommandOrder roleCommandOrder,
                                          RequestStageContainer requestStageContainer,
                                          KerberosDetails kerberosDetails)
        throws AmbariException {

      // Add the finalize stage...
      Map<String, String> commandParameters = new HashMap<String, String>();
      commandParameters.put(KerberosServerAction.DEFAULT_REALM, kerberosDetails.getDefaultRealm());
      commandParameters.put(KerberosServerAction.AUTHENTICATED_USER_NAME, ambariManagementController.getAuthName());
      if (dataDirectory != null) {
        commandParameters.put(KerberosServerAction.DATA_DIRECTORY, dataDirectory.getAbsolutePath());
      }

      Stage stage = createServerActionStage(requestStageContainer.getLastStageId(),
          cluster,
          requestStageContainer.getId(),
          "Finalize Operations",
          clusterHostInfoJson,
          "{}",
          hostParamsJson,
          FinalizeKerberosServerAction.class,
          event,
          commandParameters,
          "Finalize Operations", 300);

      RoleGraph roleGraph = roleGraphFactory.createNew(roleCommandOrder);
      roleGraph.build(stage);
      requestStageContainer.addStages(roleGraph.getStages());
    }

    public void addCleanupStage(Cluster cluster, String clusterHostInfoJson,
                                String hostParamsJson, ServiceComponentHostServerActionEvent event,
                                Map<String, String> commandParameters,
                                RoleCommandOrder roleCommandOrder, RequestStageContainer requestStageContainer)
        throws AmbariException {
      Stage stage = createServerActionStage(requestStageContainer.getLastStageId(),
          cluster,
          requestStageContainer.getId(),
          "Kerberization Clean Up",
          clusterHostInfoJson,
          "{}",
          hostParamsJson,
          CleanupServerAction.class,
          event,
          commandParameters,
          "Kerberization Clean Up",
          configuration.getDefaultServerTaskTimeout());

      RoleGraph roleGraph = roleGraphFactory.createNew(roleCommandOrder);
      roleGraph.build(stage);
      requestStageContainer.addStages(roleGraph.getStages());
    }
  }

  /**
   * EnableKerberosHandler is an implementation of the Handler interface used to enable Kerberos
   * on the relevant cluster
   * <p/>
   * This implementation attempts to set the Service and ServiceComponentHost _desired_ security
   * states to {@link org.apache.ambari.server.state.SecurityState#SECURED_KERBEROS} and the
   * ServiceComponentHost _current_ security state to {@link org.apache.ambari.server.state.SecurityState#SECURING}.
   * <p/>
   * To complete the process, this implementation creates the following stages:
   * <ol>
   * <li>create principals</li>
   * <li>create keytab files</li>
   * <li>distribute keytab files to the appropriate hosts</li>
   * <li>update relevant configurations</li>
   * </ol>
   */
  private class EnableKerberosHandler extends Handler {
    @Override
    public boolean shouldProcess(SecurityState desiredSecurityState, ServiceComponentHost sch) throws AmbariException {
      return (desiredSecurityState == SecurityState.SECURED_KERBEROS);
    }

    @Override
    public SecurityState getNewDesiredSCHSecurityState() {
      return SecurityState.SECURED_KERBEROS;
    }

    @Override
    public SecurityState getNewSCHSecurityState() {
      return SecurityState.SECURING;
    }

    @Override
    public SecurityState getNewServiceSecurityState() {
      return SecurityState.SECURED_KERBEROS;
    }

    @Override
    public long createStages(Cluster cluster,
                             String clusterHostInfoJson, String hostParamsJson,
                             ServiceComponentHostServerActionEvent event,
                             RoleCommandOrder roleCommandOrder, KerberosDetails kerberosDetails,
                             File dataDirectory, RequestStageContainer requestStageContainer,
                             List<ServiceComponentHost> serviceComponentHosts,
                             Map<String, ? extends Collection<String>> serviceComponentFilter,
                             Set<String> hostFilter, Collection<String> identityFilter, Set<String> hostsWithValidKerberosClient)
        throws AmbariException {
      // If there are principals, keytabs, and configurations to process, setup the following sages:
      //  1) prepare identities
      //  2) generate principals
      //  3) generate keytab files
      //  4) distribute keytab files
      //  5) update configurations

      // If a RequestStageContainer does not already exist, create a new one...
      if (requestStageContainer == null) {
        requestStageContainer = new RequestStageContainer(
            actionManager.getNextRequestId(),
            null,
            requestFactory,
            actionManager);
      }

      Map<String, String> commandParameters = new HashMap<String, String>();
      commandParameters.put(KerberosServerAction.AUTHENTICATED_USER_NAME, ambariManagementController.getAuthName());
      commandParameters.put(KerberosServerAction.UPDATE_CONFIGURATION_NOTE, "Enabling Kerberos");
      commandParameters.put(KerberosServerAction.UPDATE_CONFIGURATIONS, "true");
      commandParameters.put(KerberosServerAction.DEFAULT_REALM, kerberosDetails.getDefaultRealm());
      if (dataDirectory != null) {
        commandParameters.put(KerberosServerAction.DATA_DIRECTORY, dataDirectory.getAbsolutePath());
      }
      if (serviceComponentFilter != null) {
        commandParameters.put(KerberosServerAction.SERVICE_COMPONENT_FILTER, StageUtils.getGson().toJson(serviceComponentFilter));
      }
      if (hostFilter != null) {
        commandParameters.put(KerberosServerAction.HOST_FILTER, StageUtils.getGson().toJson(hostFilter));
      }
      if (identityFilter != null) {
        commandParameters.put(KerberosServerAction.IDENTITY_FILTER, StageUtils.getGson().toJson(identityFilter));
      }

      // *****************************************************************
      // Create stage to prepare operations
      addPrepareEnableKerberosOperationsStage(cluster, clusterHostInfoJson, hostParamsJson, event, commandParameters,
          roleCommandOrder, requestStageContainer);

      if (kerberosDetails.manageIdentities()) {
        commandParameters.put(KerberosServerAction.KDC_TYPE, kerberosDetails.getKdcType().name());

        // *****************************************************************
        // Create stage to create principals
        addCreatePrincipalsStage(cluster, clusterHostInfoJson, hostParamsJson, event, commandParameters,
            roleCommandOrder, requestStageContainer);

        // *****************************************************************
        // Create stage to generate keytabs
        addCreateKeytabFilesStage(cluster, clusterHostInfoJson, hostParamsJson, event, commandParameters,
            roleCommandOrder, requestStageContainer);

        // *****************************************************************
        // Create stage to distribute and configure keytab for Ambari server and configure JAAS
        if (kerberosDetails.createAmbariPrincipal()) {
          addConfigureAmbariIdentityStage(cluster, clusterHostInfoJson, hostParamsJson, event, commandParameters,
              roleCommandOrder, requestStageContainer);
        }

        // *****************************************************************
        // Create stage to distribute keytabs
        addDistributeKeytabFilesStage(cluster, serviceComponentHosts, clusterHostInfoJson, hostParamsJson,
            commandParameters, roleCommandOrder, requestStageContainer, hostsWithValidKerberosClient);
      }

      // *****************************************************************
      // Create stage to update configurations of services
      addUpdateConfigurationsStage(cluster, clusterHostInfoJson, hostParamsJson, event, commandParameters,
          roleCommandOrder, requestStageContainer);

      return requestStageContainer.getLastStageId();
    }
  }

  /**
   * DisableKerberosHandler is an implementation of the Handler interface used to disable Kerberos
   * on the relevant cluster
   * <p/>
   * This implementation attempts to set the Service and ServiceComponentHost _desired_ security
   * states to {@link org.apache.ambari.server.state.SecurityState#UNSECURED} and the ServiceComponentHost
   * _current_ security state to {@link org.apache.ambari.server.state.SecurityState#UNSECURING}.
   * <p/>
   * To complete the process, this implementation creates the following stages:
   * <ol>
   * <li>update relevant configurations</li>
   * <li>delete keytab files</li>
   * <li>remove principals</li>
   * <li>restart services</li>
   * </ol>
   */
  private class DisableKerberosHandler extends Handler {
    @Override
    public boolean shouldProcess(SecurityState desiredSecurityState, ServiceComponentHost sch) throws AmbariException {
      return (desiredSecurityState == SecurityState.UNSECURED) &&
          ((sch.getDesiredSecurityState() != SecurityState.UNSECURED) || (sch.getSecurityState() != SecurityState.UNSECURED)) &&
          (sch.getSecurityState() != SecurityState.UNSECURING);
    }

    @Override
    public SecurityState getNewDesiredSCHSecurityState() {
      return SecurityState.UNSECURED;
    }

    @Override
    public SecurityState getNewSCHSecurityState() {
      return SecurityState.UNSECURING;
    }

    @Override
    public SecurityState getNewServiceSecurityState() {
      return SecurityState.UNSECURED;
    }

    @Override
    public long createStages(Cluster cluster,
                             String clusterHostInfoJson, String hostParamsJson,
                             ServiceComponentHostServerActionEvent event,
                             RoleCommandOrder roleCommandOrder, KerberosDetails kerberosDetails,
                             File dataDirectory, RequestStageContainer requestStageContainer,
                             List<ServiceComponentHost> serviceComponentHosts,
                             Map<String, ? extends Collection<String>> serviceComponentFilter, Set<String> hostFilter, Collection<String> identityFilter, Set<String> hostsWithValidKerberosClient) throws AmbariException {
      //  1) revert configurations

      // If a RequestStageContainer does not already exist, create a new one...
      if (requestStageContainer == null) {
        requestStageContainer = new RequestStageContainer(
            actionManager.getNextRequestId(),
            null,
            requestFactory,
            actionManager);
      }

      Map<String, String> commandParameters = new HashMap<String, String>();
      commandParameters.put(KerberosServerAction.AUTHENTICATED_USER_NAME, ambariManagementController.getAuthName());
      commandParameters.put(KerberosServerAction.UPDATE_CONFIGURATION_NOTE, "Disabling Kerberos");
      commandParameters.put(KerberosServerAction.UPDATE_CONFIGURATIONS, "true");
      commandParameters.put(KerberosServerAction.DEFAULT_REALM, kerberosDetails.getDefaultRealm());
      if (dataDirectory != null) {
        commandParameters.put(KerberosServerAction.DATA_DIRECTORY, dataDirectory.getAbsolutePath());
      }
      if (serviceComponentFilter != null) {
        commandParameters.put(KerberosServerAction.SERVICE_COMPONENT_FILTER, StageUtils.getGson().toJson(serviceComponentFilter));
      }
      if (hostFilter != null) {
        commandParameters.put(KerberosServerAction.HOST_FILTER, StageUtils.getGson().toJson(hostFilter));
      }
      if (identityFilter != null) {
        commandParameters.put(KerberosServerAction.IDENTITY_FILTER, StageUtils.getGson().toJson(identityFilter));
      }

      addDisableSecurityHookStage(cluster, clusterHostInfoJson, hostParamsJson, commandParameters,
        roleCommandOrder, requestStageContainer);

      addStopZookeeperStage(cluster, clusterHostInfoJson, hostParamsJson, commandParameters,
        roleCommandOrder, requestStageContainer);

      // *****************************************************************
      // Create stage to prepare operations
      addPrepareDisableKerberosOperationsStage(cluster, clusterHostInfoJson, hostParamsJson, event, commandParameters,
          roleCommandOrder, requestStageContainer);

      // *****************************************************************
      // Create stage to update configurations of services
      addUpdateConfigurationsStage(cluster, clusterHostInfoJson, hostParamsJson, event, commandParameters,
          roleCommandOrder, requestStageContainer);

      if (kerberosDetails.manageIdentities()) {
        commandParameters.put(KerberosServerAction.KDC_TYPE, kerberosDetails.getKdcType().name());

        // *****************************************************************
        // Create stage to remove principals
        addDestroyPrincipalsStage(cluster, clusterHostInfoJson, hostParamsJson, event, commandParameters,
            roleCommandOrder, requestStageContainer);

        // *****************************************************************
        // Create stage to delete keytabs
        addDeleteKeytabFilesStage(cluster, serviceComponentHosts, clusterHostInfoJson,
            hostParamsJson, commandParameters, roleCommandOrder, requestStageContainer, hostsWithValidKerberosClient);
      }

      // *****************************************************************
      // Create stage to perform data cleanups (e.g. kerberos descriptor artifact database leftovers)
      addCleanupStage(cluster, clusterHostInfoJson, hostParamsJson, event, commandParameters,
          roleCommandOrder, requestStageContainer);


      return requestStageContainer.getLastStageId();
    }
  }

  /**
   * CreatePrincipalsAndKeytabsHandler is an implementation of the Handler interface used to create
   * principals and keytabs and distribute them throughout the cluster.  This is similar to enabling
   * Kerberos however no states or configurations will be updated.
   * <p/>
   * To complete the process, this implementation creates the following stages:
   * <ol>
   * <li>create principals</li>
   * <li>create keytab files</li>
   * <li>distribute keytab files to the appropriate hosts</li>
   * </ol>
   */
  private class CreatePrincipalsAndKeytabsHandler extends Handler {
    /**
     * A boolean value indicating whether to create keytabs for all principals (<code>true</code>)
     * or only the ones that are missing (<code>false</code>).
     */
    private boolean regenerateAllKeytabs;

    /**
     * A boolean value indicating whether to update service configurations (<code>true</code>)
     * or ignore any potential configuration changes (<code>false</code>).
     */
    private boolean updateConfigurations;

    /**
     * A boolean value indicating whether to include Ambari server identity (<code>true</code>)
     * or ignore it (<code>false</code>).
     */
    private boolean includeAmbariIdentity;

    /**
     * CreatePrincipalsAndKeytabsHandler constructor to set whether this instance should be used to
     * regenerate all keytabs or just the ones that have not been distributed
     *
     * @param regenerateAllKeytabs A boolean value indicating whether to create keytabs for all
     *                             principals (<code>true</code> or only the ones that are missing
     *                             (<code>false</code>)
     * @param updateConfigurations A boolean value indicating whether to update service configurations
     *                             (<code>true</code>) or ignore any potential configuration changes
     *                             (<code>false</code>)
     */
    public CreatePrincipalsAndKeytabsHandler(boolean regenerateAllKeytabs, boolean updateConfigurations,
                                             boolean includeAmbariIdentity) {
      this.regenerateAllKeytabs = regenerateAllKeytabs;
      this.updateConfigurations = updateConfigurations;
      this.includeAmbariIdentity = includeAmbariIdentity;
    }

    @Override
    public boolean shouldProcess(SecurityState desiredSecurityState, ServiceComponentHost sch) throws AmbariException {
      return true;
    }

    @Override
    public SecurityState getNewDesiredSCHSecurityState() {
      return null;
    }

    @Override
    public SecurityState getNewSCHSecurityState() {
      return null;
    }

    @Override
    public SecurityState getNewServiceSecurityState() {
      return null;
    }

    @Override
    public long createStages(Cluster cluster,
                             String clusterHostInfoJson, String hostParamsJson,
                             ServiceComponentHostServerActionEvent event,
                             RoleCommandOrder roleCommandOrder, KerberosDetails kerberosDetails,
                             File dataDirectory, RequestStageContainer requestStageContainer,
                             List<ServiceComponentHost> serviceComponentHosts,
                             Map<String, ? extends Collection<String>> serviceComponentFilter,
                             Set<String> hostFilter, Collection<String> identityFilter, Set<String> hostsWithValidKerberosClient)
        throws AmbariException {
      // If there are principals and keytabs to process, setup the following sages:
      //  1) prepare identities
      //  2) generate principals
      //  3) generate keytab files
      //  4) distribute keytab files
      //  5) update configurations (optional)

      // If a RequestStageContainer does not already exist, create a new one...
      if (requestStageContainer == null) {
        requestStageContainer = new RequestStageContainer(
            actionManager.getNextRequestId(),
            null,
            requestFactory,
            actionManager);
      }


      Map<String, String> commandParameters = new HashMap<String, String>();
      commandParameters.put(KerberosServerAction.AUTHENTICATED_USER_NAME, ambariManagementController.getAuthName());
      commandParameters.put(KerberosServerAction.DEFAULT_REALM, kerberosDetails.getDefaultRealm());
      if (dataDirectory != null) {
        commandParameters.put(KerberosServerAction.DATA_DIRECTORY, dataDirectory.getAbsolutePath());
      }
      if (serviceComponentFilter != null) {
        commandParameters.put(KerberosServerAction.SERVICE_COMPONENT_FILTER, StageUtils.getGson().toJson(serviceComponentFilter));
      }
      if (hostFilter != null) {
        commandParameters.put(KerberosServerAction.HOST_FILTER, StageUtils.getGson().toJson(hostFilter));
      }
      if (identityFilter != null) {
        commandParameters.put(KerberosServerAction.IDENTITY_FILTER, StageUtils.getGson().toJson(identityFilter));
      }

      commandParameters.put(KerberosServerAction.REGENERATE_ALL, (regenerateAllKeytabs) ? "true" : "false");
      commandParameters.put(KerberosServerAction.INCLUDE_AMBARI_IDENTITY, (includeAmbariIdentity) ? "true" : "false");

      if (updateConfigurations) {
        commandParameters.put(KerberosServerAction.UPDATE_CONFIGURATION_NOTE, "Updated Kerberos-related configurations");
        commandParameters.put(KerberosServerAction.UPDATE_CONFIGURATIONS, "true");
      }

      // *****************************************************************
      // Create stage to create principals
      addPrepareKerberosIdentitiesStage(cluster, clusterHostInfoJson, hostParamsJson, event,
          commandParameters, roleCommandOrder, requestStageContainer);

      if (kerberosDetails.manageIdentities()) {
        commandParameters.put(KerberosServerAction.KDC_TYPE, kerberosDetails.getKdcType().name());

        // *****************************************************************
        // Create stage to create principals
        addCreatePrincipalsStage(cluster, clusterHostInfoJson, hostParamsJson, event,
            commandParameters, roleCommandOrder, requestStageContainer);

        // *****************************************************************
        // Create stage to generate keytabs
        addCreateKeytabFilesStage(cluster, clusterHostInfoJson, hostParamsJson, event,
            commandParameters, roleCommandOrder, requestStageContainer);

        // *****************************************************************
        // Create stage to distribute and configure keytab for Ambari server and configure JAAS
        if (includeAmbariIdentity && kerberosDetails.createAmbariPrincipal()) {
          addConfigureAmbariIdentityStage(cluster, clusterHostInfoJson, hostParamsJson, event, commandParameters,
              roleCommandOrder, requestStageContainer);
        }

        // *****************************************************************
        // Create stage to distribute keytabs
        addDistributeKeytabFilesStage(cluster, serviceComponentHosts, clusterHostInfoJson,
            hostParamsJson, commandParameters, roleCommandOrder, requestStageContainer, hostsWithValidKerberosClient);
      }

      if (updateConfigurations) {
        // *****************************************************************
        // Create stage to update configurations of services
        addUpdateConfigurationsStage(cluster, clusterHostInfoJson, hostParamsJson, event, commandParameters,
            roleCommandOrder, requestStageContainer);
      }

      return requestStageContainer.getLastStageId();
    }
  }

  /**
   * DeletePrincipalsAndKeytabsHandler is an implementation of the Handler interface used to delete
   * principals and keytabs throughout the cluster.
   * <p/>
   * To complete the process, this implementation creates the following stages:
   * <ol>
   * <li>delete principals</li>
   * <li>remove keytab files</li>
   * </ol>
   */
  private class DeletePrincipalsAndKeytabsHandler extends Handler {

    @Override
    public boolean shouldProcess(SecurityState desiredSecurityState, ServiceComponentHost sch) throws AmbariException {
      return true;
    }

    @Override
    public SecurityState getNewDesiredSCHSecurityState() {
      return null;
    }

    @Override
    public SecurityState getNewSCHSecurityState() {
      return null;
    }

    @Override
    public SecurityState getNewServiceSecurityState() {
      return null;
    }

    @Override
    public long createStages(Cluster cluster,
                             String clusterHostInfoJson, String hostParamsJson,
                             ServiceComponentHostServerActionEvent event,
                             RoleCommandOrder roleCommandOrder, KerberosDetails kerberosDetails,
                             File dataDirectory, RequestStageContainer requestStageContainer,
                             List<ServiceComponentHost> serviceComponentHosts,
                             Map<String, ? extends Collection<String>> serviceComponentFilter, Set<String> hostFilter, Collection<String> identityFilter, Set<String> hostsWithValidKerberosClient)
        throws AmbariException {

      // If a RequestStageContainer does not already exist, create a new one...
      if (requestStageContainer == null) {
        requestStageContainer = new RequestStageContainer(
            actionManager.getNextRequestId(),
            null,
            requestFactory,
            actionManager);
      }

      if (kerberosDetails.manageIdentities()) {
        // If there are principals and keytabs to process, setup the following sages:
        //  1) prepare
        //  2) delete principals
        //  3) delete keytab files

        Map<String, String> commandParameters = new HashMap<String, String>();
        commandParameters.put(KerberosServerAction.AUTHENTICATED_USER_NAME, ambariManagementController.getAuthName());
        commandParameters.put(KerberosServerAction.DEFAULT_REALM, kerberosDetails.getDefaultRealm());
        if (dataDirectory != null) {
          commandParameters.put(KerberosServerAction.DATA_DIRECTORY, dataDirectory.getAbsolutePath());
        }
        if (serviceComponentFilter != null) {
          commandParameters.put(KerberosServerAction.SERVICE_COMPONENT_FILTER, StageUtils.getGson().toJson(serviceComponentFilter));
        }
        if (hostFilter != null) {
          commandParameters.put(KerberosServerAction.HOST_FILTER, StageUtils.getGson().toJson(hostFilter));
        }
        if (identityFilter != null) {
          commandParameters.put(KerberosServerAction.IDENTITY_FILTER, StageUtils.getGson().toJson(identityFilter));
        }

        commandParameters.put(KerberosServerAction.KDC_TYPE, kerberosDetails.getKdcType().name());

        // *****************************************************************
        // Create stage to create principals
        addPrepareKerberosIdentitiesStage(cluster, clusterHostInfoJson, hostParamsJson, event,
            commandParameters, roleCommandOrder, requestStageContainer);

        // *****************************************************************
        // Create stage to delete principals
        addDestroyPrincipalsStage(cluster, clusterHostInfoJson, hostParamsJson, event,
            commandParameters, roleCommandOrder, requestStageContainer);

        // *****************************************************************
        // Create stage to delete keytabs
        addDeleteKeytabFilesStage(cluster, serviceComponentHosts, clusterHostInfoJson,
            hostParamsJson, commandParameters, roleCommandOrder, requestStageContainer, hostsWithValidKerberosClient);
      }

      return requestStageContainer.getLastStageId();
    }
  }

  /**
   * KerberosDetails is a helper class to hold the details of the relevant Kerberos-specific
   * configurations so they may be passed around more easily.
   */
  private static class KerberosDetails {
    private String defaultRealm;
    private KDCType kdcType;
    private Map<String, String> kerberosEnvProperties;
    private SecurityType securityType;
    private Boolean manageIdentities;

    public void setDefaultRealm(String defaultRealm) {
      this.defaultRealm = defaultRealm;
    }

    public String getDefaultRealm() {
      return defaultRealm;
    }

    public void setKdcType(KDCType kdcType) {
      this.kdcType = kdcType;
    }

    public KDCType getKdcType() {
      return kdcType;
    }

    public void setKerberosEnvProperties(Map<String, String> kerberosEnvProperties) {
      this.kerberosEnvProperties = kerberosEnvProperties;
    }

    public Map<String, String> getKerberosEnvProperties() {
      return kerberosEnvProperties;
    }

    public void setSecurityType(SecurityType securityType) {
      this.securityType = securityType;
    }

    public SecurityType getSecurityType() {
      return securityType;
    }

    public boolean manageIdentities() {
      if (manageIdentities == null) {
        return (kerberosEnvProperties == null) ||
            !"false".equalsIgnoreCase(kerberosEnvProperties.get(MANAGE_IDENTITIES));
      } else {
        return manageIdentities;
      }
    }

    public void setManageIdentities(Boolean manageIdentities) {
      this.manageIdentities = manageIdentities;
    }

    public boolean createAmbariPrincipal() {
      return (kerberosEnvProperties == null) ||
          !"false".equalsIgnoreCase(kerberosEnvProperties.get(CREATE_AMBARI_PRINCIPAL));
    }
  }
}
