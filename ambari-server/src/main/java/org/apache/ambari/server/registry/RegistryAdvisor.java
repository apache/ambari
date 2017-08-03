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
package org.apache.ambari.server.registry;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.ObjectNotFoundException;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.registry.RegistryRecommendationResponse.RegistryRecommendationResponseBuilder;
import org.apache.ambari.server.registry.RegistryRecommendationResponse.RegistryRecommendations;
import org.apache.ambari.server.registry.RegistryValidationResponse.RegistryValidationResponseBuilder;
import org.apache.ambari.server.registry.RegistryValidationResponse.RegistryValidationResult;
import org.apache.ambari.server.utils.SetUtils;
import org.apache.ambari.server.utils.VersionUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * Registry Advisor
 */
@Singleton
public class RegistryAdvisor {

  private AmbariManagementController managementController;
  private long requestId = 0;

  /**
   * Given list of all possible mpack bundles, check compatibility and return compatible mpack bundles.
   * @param mpackBundles  all possible mpack bundles
   * @return              filtered compatible mpack bundles
   */
  private List<Collection<MpackEntry>> filterCompatibleMpackBundles(List<Collection<MpackEntry>> mpackBundles) {
    List<Collection<MpackEntry>> compatibleMpackBundles = new LinkedList<>();
    for(Collection<MpackEntry> mpackBundle : mpackBundles) {
      if(isCompatibleMpackBundle(mpackBundle)) {
        compatibleMpackBundles.add(mpackBundle);
      }
    }
    return compatibleMpackBundles;
  }

  /**
   * Check if the mpack bundle is compatible.
   * @param mpackBundle mpack bundle to check
   * @return True if the mpack bundle is compatible, else False
   */
  private boolean isCompatibleMpackBundle(Collection<MpackEntry> mpackBundle) {
    Map<String, MpackEntry> mpackMap = new HashMap<>();
    for(MpackEntry mvr : mpackBundle) {
      mpackMap.put(mvr.getMpackName(), mvr);
    }
    for(MpackEntry mvr: mpackMap.values()) {
      RegistryMpackVersion rmv = mvr.getRegistryMpackVersion();
      List<RegistryMpackCompatiblity> compatiblities = (List<RegistryMpackCompatiblity>) rmv.getCompatibleMpacks();
      if(compatiblities != null && !compatiblities.isEmpty()) {
        for(RegistryMpackCompatiblity compatiblity : compatiblities) {
          if(mpackMap.containsKey(compatiblity.getName())) {
            String selectedVersion = mpackMap.get(compatiblity.getName()).getMpackVersion();
            String minVersion = compatiblity.getMinVersion();
            String maxVersion = compatiblity.getMaxVersion();
            if(minVersion != null && !minVersion.isEmpty()
              && VersionUtils.compareVersions(selectedVersion, minVersion) < 0) {
              return false;
            }
            if(maxVersion != null && !maxVersion.isEmpty()
              && VersionUtils.compareVersions(selectedVersion, maxVersion) >= 0) {
              return false;
            }
          }
        }
      }
    }
    return true;
  }

  /**
   * Get merged list of mpacks that support the selected scenarios.
   * @param registry      registry to use for getting list of mpacks for selected scenarios
   * @param selectedScenarios selected scenario names
   * @return              merged list of scenario mpacks
   * @throws AmbariException
   */
  private Collection<String> getScenarioMpacks(Registry registry, Collection<ScenarioEntry> selectedScenarios)
    throws AmbariException {
    Set<String> scenarioMpackNames = new HashSet<>();
    for(ScenarioEntry selectedScenario : selectedScenarios) {
      RegistryScenario registryScenario = registry.getRegistryScenario(selectedScenario.getScenarioName());
      selectedScenario.setRegistryScenario(registryScenario);
      for(RegistryScenarioMpack scenarioMpack : registryScenario.getScenarioMpacks()) {
        if(!scenarioMpackNames.contains(scenarioMpack.getName())) {
          scenarioMpackNames.add(scenarioMpack.getName());
        }
      }
    }
    return scenarioMpackNames;
  }

  /**
   * Given a list of mpacks, find all mpack versions for each mpack
   * @param registry    registry to use for getting list of mpack versions
   * @param mpackNames  list of mpack names
   * @return            all mpack versions group by mpack
   * @throws AmbariException
   */
  private List<Collection<MpackEntry>> getAllMpackEntries(Registry registry, Collection<String> mpackNames)
    throws AmbariException {
    List<Collection<MpackEntry>> allMpackEntries = new LinkedList<>();
    for(String mpackName : mpackNames) {
      RegistryMpack registryMpack = registry.getRegistryMpack(mpackName);
      List<MpackEntry> mpackEntries = new LinkedList<>();
      for(RegistryMpackVersion registryMpackVersion : registryMpack.getMpackVersions()) {
        MpackEntry mpackEntry = new MpackEntry(
          registryMpack.getMpackName(), registryMpackVersion.getMpackVersion());
        mpackEntry.setRegistryMpackVersion(registryMpackVersion);
        mpackEntries.add(mpackEntry);
      }
      allMpackEntries.add(mpackEntries);
    }
    return allMpackEntries;
  }

  /**
   * Create a mpacks hash map with key [mpack_name]-[mpack_version]
   * @param mpackEntries  collection of mpacks
   * @return  hash map of mpacks
   */
  private HashMap<String, MpackEntry> convertToMpackVersionsMap(Collection<MpackEntry> mpackEntries) {
    HashMap<String, MpackEntry> mpacksMap = new HashMap<>();
    for(MpackEntry mpackEntry : mpackEntries) {
      String key = mpackEntry.getMpackName() + "-" + mpackEntry.getMpackVersion();
      mpacksMap.put(key, mpackEntry);
    }
    return mpacksMap;
  }

  /**
   * Create a mpacks map with key [mpack_name]
   * @param mpackEntries  collection of mpacks
   * @return  hash map of mpacks
   */
  private HashMap<String, MpackEntry> convertToMpacksMap(Collection<MpackEntry> mpackEntries) {
    HashMap<String, MpackEntry> mpacksMap = new HashMap<>();
    for (MpackEntry mpackEntry : mpackEntries) {
      String key = mpackEntry.getMpackName();
      mpacksMap.put(key, mpackEntry);
    }
    return mpacksMap;
  }

  /**
   * Find a compatible mpack bundle that matches the selected list of mpacks.
   * @param compatibleMpackBundles  all compatible mpack bundles
   * @param selectedMpacks          selected mpacks
   * @return                        compatible mpack bundle
   */
  private Collection<MpackEntry> findCompatibleMpackBundle(
    List<Collection<MpackEntry>> compatibleMpackBundles, Collection<MpackEntry> selectedMpacks) {

    HashMap<String, MpackEntry> selectedMpacksMap = convertToMpackVersionsMap(selectedMpacks);
    for (Collection<MpackEntry> compatibleMpackBundle : compatibleMpackBundles) {
      boolean isCompatible = true;
      for(MpackEntry mpackEntry: compatibleMpackBundle) {
        String key = mpackEntry.getMpackName() + "-" + mpackEntry.getMpackVersion();
        if(!selectedMpacksMap.containsKey(key)) {
          isCompatible = false;
          break;
        }
      }
      if(isCompatible) {
        return compatibleMpackBundle;
      }
    }
    return null;
  }

  @Inject
  public RegistryAdvisor(AmbariManagementController managementController) {
    this.managementController = managementController;
  }

  /**
   * Returns registry recommendation response based on the request [scenario-mpacks, upgrade-mpacks]
   * @param   request the recommendation request
   * @return  {@link RegistryRecommendationResponse} for the request
   * @throws AmbariException
   */
  public synchronized RegistryRecommendationResponse recommend(RegistryAdvisorRequest request) throws AmbariException {
    switch(request.getRequestType()) {
      case SCENARIO_MPACKS:
        return recommendScenarioMpacks(request);
      case UPGRADE_MPACKS:
        return recommendUpgradeMpacks(request);
      default:
        throw new AmbariException("Unkown request type");
    }
  }

  /**
   * Returns registry recommendation response for a request of type "scenario-mpacks"
   * @param   request the recommendation response
   * @return  {@link RegistryRecommendationResponse} for the request
   * @throws  AmbariException
   */
  private RegistryRecommendationResponse recommendScenarioMpacks(RegistryAdvisorRequest request) throws AmbariException {
    // Get registry
    Registry registry = managementController.getRegistry(request.getRegistryId());
    // Get all scenario mpacks
    Collection<String> scenarioMpackNames = getScenarioMpacks(registry, request.getSelectedScenarios());
    // Get all mpack versions for each scenario mpack
    List<Collection<MpackEntry>> allMpackEntries = getAllMpackEntries(registry, scenarioMpackNames);
    // Get all possible mpack bundles
    List<Collection<MpackEntry>> allMpackBundles = SetUtils.permutations(allMpackEntries);
    // Filter down to compatible mpack bundles
    List<Collection<MpackEntry>> compatibleMpackBundles = filterCompatibleMpackBundles(allMpackBundles);
    // Order recommentations by versions with latest at pole position.
    compatibleMpackBundles.sort(new Comparator<Collection<MpackEntry>>() {
      @Override
      public int compare(final Collection<MpackEntry> o1, final Collection<MpackEntry> o2) {
        int o1Wins = 0;
        int o2Wins = 0;
        HashMap<String, MpackEntry> o1Map = convertToMpacksMap(o1);
        HashMap<String, MpackEntry> o2Map = convertToMpacksMap(o2);
        for(Map.Entry<String, MpackEntry> mapEntry : o1Map.entrySet()) {
          MpackEntry o1Entry = mapEntry.getValue();
          MpackEntry o2Entry = o2Map.get(mapEntry.getKey());
          int compareResult = VersionUtils.compareVersions(o1Entry.getMpackVersion(), o2Entry.getMpackVersion());
          if(compareResult > 0) {
            o1Wins++;
          } else if(compareResult < 0) {
            o2Wins++;
          }
        }
        // Order in reverse order
        return o2Wins - o1Wins;
      }
    });
    Long rank = 1L;
    List<MpackBundle> mpackBundles = new LinkedList<>();
    for(Collection<MpackEntry> mpackEntries : compatibleMpackBundles) {
      MpackBundle mpackBundle = new MpackBundle(rank++, mpackEntries);
      mpackBundles.add(mpackBundle);
    }
    // Create recommendations
    RegistryRecommendations recommendations = new RegistryRecommendations();
    recommendations.setMpackBundles(mpackBundles);
    return RegistryRecommendationResponseBuilder.forRegistry(request.getRegistryId())
      .ofType(request.getRequestType())
      .forScenarios(request.getSelectedScenarios())
      .forMpacks(request.getSelectedMpacks())
      .withId(generateRequestId())
      .withRecommendations(recommendations).build();
  }

  /**
   * Returns registry recommendation response for a request of type "upgrade-mpacks"
   * @param   request the recommendation response
   * @return  {@link RegistryRecommendationResponse} for the request
   * @throws  AmbariException
   */
  private RegistryRecommendationResponse recommendUpgradeMpacks(RegistryAdvisorRequest request) throws AmbariException {
    Registry registry = managementController.getRegistry(request.getRegistryId());
    List<RegistryValidationResult> validationItems = new LinkedList<>();
    List<MpackEntry> selectedMpacks = request.getSelectedMpacks();

    if(selectedMpacks == null || selectedMpacks.isEmpty()) {
      throw new AmbariException("Must select mpack to be upgraded");
    }

    if(selectedMpacks.size() > 1) {
      throw new AmbariException("Must select single mpack for upgrade recommendations");
    }

    // TODO: Add logic to get mpacks used by cluster and remove hardcoded logic
    // Clusters clusters = managementController.getClusters();
    // Cluster cluster = clusters.getCluster(request.getClusterName());
    Collection<MpackEntry> currentMpacks = new LinkedList<>();
    currentMpacks.add(new MpackEntry("HDPCore", "3.1.0"));
    currentMpacks.add(new MpackEntry("HDS", "4.3.0"));

    String selectedMpackName = selectedMpacks.get(0).getMpackName();
    HashMap<String, MpackEntry> currentMpacksMap = convertToMpacksMap(currentMpacks);
    MpackEntry minMpackEntry = currentMpacksMap.get(selectedMpackName);

    RegistryMpack registryMpack = registry.getRegistryMpack(selectedMpackName);

    List<MpackBundle> mpackBundles = new LinkedList<>();
    Long rank = 1L;
    List<RegistryMpackVersion> registryMpackVersions = (List<RegistryMpackVersion>) registryMpack.getMpackVersions();
    registryMpackVersions.sort(new Comparator<RegistryMpackVersion>() {
      @Override
      public int compare(final RegistryMpackVersion o1, final RegistryMpackVersion o2) {
        return -1 * VersionUtils.compareVersions(o1.getMpackVersion(), o2.getMpackVersion());
      }
    });
    for(RegistryMpackVersion registryMpackVersion : registryMpackVersions) {
      if(VersionUtils.compareVersions(registryMpackVersion.getMpackVersion(), minMpackEntry.getMpackVersion()) > 0) {
        MpackEntry mpackEntry = new MpackEntry(selectedMpackName, registryMpackVersion.getMpackVersion());
        MpackBundle mpackBundle = new MpackBundle(rank++, Collections.singletonList(mpackEntry));
        mpackBundles.add(mpackBundle);
      }
    }

    RegistryRecommendations recommendations = new RegistryRecommendations();
    recommendations.setMpackBundles(mpackBundles);
    return RegistryRecommendationResponseBuilder.forRegistry(request.getRegistryId())
      .ofType(request.getRequestType())
      .forScenarios(request.getSelectedScenarios())
      .forMpacks(request.getSelectedMpacks())
      .withId(generateRequestId())
      .withRecommendations(recommendations).build();
  }

  /**
   * Returns registry validation response based on the request [scenario-mpacks]
   * @param request the validation request
   * @return        {@link RegistryValidationResponse} for the request
   * @throws AmbariException
   */
  public synchronized RegistryValidationResponse validate(RegistryAdvisorRequest request) throws AmbariException {

    switch(request.getRequestType()) {
      case SCENARIO_MPACKS:
        return validateScenarioMpacks(request);
      case UPGRADE_MPACKS:
        return validateUpgradeMpacks(request);
      default:
        throw new AmbariException("Unkown request type");
    }
  }

  /**
   * Returns registry validation response based on the request of type "scenario-mpacks"
   * @param request the validation request
   * @return        {@link RegistryValidationResponse} for the request
   * @throws AmbariException
   */
  private RegistryValidationResponse validateScenarioMpacks(RegistryAdvisorRequest request) throws AmbariException {
    Registry registry = managementController.getRegistry(request.getRegistryId());
    List<RegistryValidationResult> validationItems = new LinkedList<>();

    // Get all mpacks required for the selected scenarios
    Collection<String> scenarioMpackNames = getScenarioMpacks(registry, request.getSelectedScenarios());

    // Validate that all mpacks required for the selected scenarios have been selected
    for(String mpackName : scenarioMpackNames) {
      boolean isSelected = false;
      for(MpackEntry mpackEntry : request.getSelectedMpacks()) {
        if(mpackName.equals(mpackEntry.getMpackName())) {
          isSelected = true;
          break;
        }
      }
      if(!isSelected) {
        // Validation failure
        String type = "ScenarioMpackValidation";
        String level = "FATAL";
        String message = "Selected mpacks does not contain " + mpackName +
          " mpack which is required for supporting the selected scenarios.";
        RegistryValidationResult validationItem = new RegistryValidationResult(type, level, message);
        validationItems.add(validationItem);
      }
    }

    // Validate if any selected mpack is not in the registry
    List<String> knownSelectedMpackNames = new LinkedList<>();
    for(MpackEntry mpackEntry : request.getSelectedMpacks()) {
      try {
        RegistryMpack registryMpack = registry.getRegistryMpack(mpackEntry.getMpackName());
        RegistryMpackVersion registryMpackVersion = registryMpack.getMpackVersion(mpackEntry.getMpackVersion());
        knownSelectedMpackNames.add(registryMpack.getMpackName());
      } catch (ObjectNotFoundException ex) {
        String type = "UnknownMpackValidation";
        String level = "WARN";
        String mpackFullName = mpackEntry.getMpackName() + "-" + mpackEntry.getMpackVersion();
        String message = "Mpack " + mpackFullName + " not found in the registry. "
          + "Cannot validate compatility with other mpacks.";
        RegistryValidationResult validationItem = new RegistryValidationResult(type, level, message);
        validationItems.add(validationItem);
      }
    }

    // Get all mpack versions for each known selected mpack
    List<Collection<MpackEntry>> allMpackEntries = getAllMpackEntries(registry, knownSelectedMpackNames);
    // Get all possible permutations
    List<Collection<MpackEntry>> allMpackBundles = SetUtils.permutations(allMpackEntries);
    // Filter down to compatible permutations
    List<Collection<MpackEntry>> compatibleMpackBundles = filterCompatibleMpackBundles(allMpackBundles);

    // Validate that the selected mpacks is present in a compatible mpack bundle
    Collection<MpackEntry> compatibleMpackBundle = findCompatibleMpackBundle(compatibleMpackBundles, request.getSelectedMpacks());
    if(compatibleMpackBundle == null) {
      // Selected mpacks are not compatible
      String type = "CompatibleMpackValidation";
      String level = "FATAL";
      String message = "Selected mpacks are not compatible and can cause issues during cluster installation.";
      RegistryValidationResult validationItem = new RegistryValidationResult(type, level, message);
      validationItems.add(validationItem);
    }

    return RegistryValidationResponseBuilder.forRegistry(request.getRegistryId())
      .ofType(request.getRequestType())
      .forScenarios(request.getSelectedScenarios())
      .forMpacks(request.getSelectedMpacks())
      .withId(generateRequestId())
      .withValidations(validationItems).build();
  }

  /**
   * Returns registry validation response based on the request of type "upgrade-mpacks"
   * @param request the validation request
   * @return        {@link RegistryValidationResponse} for the request
   * @throws AmbariException
   */
  private RegistryValidationResponse validateUpgradeMpacks(RegistryAdvisorRequest request) throws AmbariException {
    Registry registry = managementController.getRegistry(request.getRegistryId());
    List<RegistryValidationResult> validationItems = new LinkedList<>();
    List<MpackEntry> selectedMpacks = request.getSelectedMpacks();

    if(selectedMpacks == null || selectedMpacks.isEmpty()) {
      throw new AmbariException("Must select mpack to be upgraded");
    }

    if(selectedMpacks.size() > 1) {
      throw new AmbariException("Must select single mpack for upgrade validation");
    }

    // TODO: Add logic to get mpacks used by cluster and remove hardcoded logic
    // Clusters clusters = managementController.getClusters();
    // Cluster cluster = clusters.getCluster(request.getClusterName());
    Collection<MpackEntry> currentMpacks = new LinkedList<>();
    currentMpacks.add(new MpackEntry("HDPCore", "3.1.0"));
    currentMpacks.add(new MpackEntry("HDS", "4.3.0"));

    HashMap<String, MpackEntry> currentMpacksMap = convertToMpacksMap(currentMpacks);

    // TODO: Add logic for mpacks not in registry
    List<String> currentMpackNames = new LinkedList<>();
    for(MpackEntry currentMpack : currentMpacks) {
      currentMpackNames.add(currentMpack.getMpackName());
    }

    // Get all mpack versions for each current mpack
    List<Collection<MpackEntry>> allMpackEntries = getAllMpackEntries(registry, currentMpackNames);
    // Get all possible mpack bundles
    List<Collection<MpackEntry>> allMpackBundles = SetUtils.permutations(allMpackEntries);
    // Filter down to compatible mpack bundles
    List<Collection<MpackEntry>> compatibleMpackBundles = filterCompatibleMpackBundles(allMpackBundles);


    HashMap<String, MpackEntry> selectedMpacksMap = convertToMpacksMap(selectedMpacks);

    List<MpackEntry> mergedMpacks = new LinkedList<>();
    for(String key : currentMpackNames) {
      if(selectedMpacksMap.containsKey(key)) {
        mergedMpacks.add(selectedMpacksMap.get(key));
      } else {
        mergedMpacks.add(currentMpacksMap.get(key));
      }
    }

    Collection<MpackEntry> compatibleMpackBundle = findCompatibleMpackBundle(compatibleMpackBundles, mergedMpacks);
    if(compatibleMpackBundle == null) {
      // Order recommendations by versions.
      compatibleMpackBundles.sort(new Comparator<Collection<MpackEntry>>() {
        @Override
        public int compare(final Collection<MpackEntry> o1, final Collection<MpackEntry> o2) {
          int o1Wins = 0;
          int o2Wins = 0;
          HashMap<String, MpackEntry> o1Map = convertToMpacksMap(o1);
          HashMap<String, MpackEntry> o2Map = convertToMpacksMap(o2);
          for(Map.Entry<String, MpackEntry> mapEntry : o1Map.entrySet()) {
            MpackEntry o1Entry = mapEntry.getValue();
            MpackEntry o2Entry = o2Map.get(mapEntry.getKey());
            int compareResult = VersionUtils.compareVersions(o1Entry.getMpackVersion(), o2Entry.getMpackVersion());
            if(compareResult > 0) {
              o1Wins++;
            } else if(compareResult < 0) {
              o2Wins++;
            }
          }
          return o1Wins - o2Wins;
        }
      });

      boolean noMatchFound = true;
      for(Collection<MpackEntry> mpackBundle : compatibleMpackBundles) {
        HashMap<String, MpackEntry> mpackBundleMap = convertToMpacksMap(mpackBundle);
        boolean isMatch = true;
        for(Map.Entry<String, MpackEntry> selectedEntry : selectedMpacksMap.entrySet()) {
          String selectedKey = selectedEntry.getKey();
          MpackEntry selectedMpackEntry = selectedEntry.getValue();
          if (!mpackBundleMap.containsKey(selectedKey) ||
            !VersionUtils.areVersionsEqual(selectedMpackEntry.getMpackVersion(),
              mpackBundleMap.get(selectedKey).getMpackVersion(), true)) {
            isMatch = false;
          }
        }
        if(isMatch) {
          for(Map.Entry<String, MpackEntry> bundleEntry :  mpackBundleMap.entrySet()){
            if(!selectedMpacksMap.containsKey(bundleEntry.getKey())) {
              String targetMpackName = bundleEntry.getKey();
              MpackEntry targetMpackEntry = bundleEntry.getValue();
              String targetMpackFullName = targetMpackEntry.getMpackName() + "-" + targetMpackEntry.getMpackVersion();
              MpackEntry currentMpackEntry = currentMpacksMap.get(targetMpackName);
              String currentMpackFullName = currentMpackEntry.getMpackName() + "-" + currentMpackEntry.getMpackVersion();

              String type = "UpgradeMpackValidation";
              String level = "WARN";
              String message = "Mpack " + currentMpackFullName + " needs to be upgraded to " + targetMpackFullName;
              RegistryValidationResult validationItem = new RegistryValidationResult(type, level, message);
              validationItems.add(validationItem);
            }
          }
          noMatchFound = false;
          break;
        }
      }
      if(noMatchFound) {
        String type = "UpgradeMpackValidation";
        String level = "FATAL";
        String message = "No recommendations for upgrading other mpacks in the cluster found.";
        RegistryValidationResult validationItem = new RegistryValidationResult(type, level, message);
        validationItems.add(validationItem);
      }
    }

    return RegistryValidationResponseBuilder.forRegistry(request.getRegistryId())
      .ofType(request.getRequestType())
      .forScenarios(request.getSelectedScenarios())
      .forMpacks(request.getSelectedMpacks())
      .withId(generateRequestId())
      .withValidations(validationItems).build();
  }

  /**
   * Generate registry advisor request id
   * TODO: Store registry advisor requests in database.
   * @return
   */
  public long generateRequestId() {
    requestId += 1;
    return requestId;
  }
}
