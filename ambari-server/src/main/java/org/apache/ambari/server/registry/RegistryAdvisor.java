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
import org.apache.ambari.server.registry.RegistryValidationResponse.RegistryValidationItem;
import org.apache.ambari.server.registry.RegistryValidationResponse.RegistryValidationResponseBuilder;
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
              && VersionUtils.compareVersions(selectedVersion, maxVersion) > 0) {
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
   * @param scenarioNames selected scenario names
   * @return              merged list of scenario mpacks
   * @throws AmbariException
   */
  private Collection<String> getScenarioMpacks(Registry registry, Collection<String> scenarioNames)
    throws AmbariException {
    Set<String> scenarioMpackNames = new HashSet<>();
    for(String selectedScenario : scenarioNames) {
      RegistryScenario registryScenario = registry.getRegistryScenario(selectedScenario);
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
    for(MpackEntry mpackEntry : mpackEntries) {
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
    for(Collection<MpackEntry> compatibleMpackBundle : compatibleMpackBundles) {
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
   * Returns registry recommendation response based on the request [scenario-mpacks]
   * @param request the recommendation request
   * @return        {@link RegistryRecommendationResponse} for the request
   * @throws AmbariException
   */
  public synchronized RegistryRecommendationResponse recommend(RegistryAdvisorRequest request) throws AmbariException {
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
    // Create recommendations
    RegistryRecommendations recommendations = new RegistryRecommendations();
    recommendations.setMpackBundles(compatibleMpackBundles);
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
    Registry registry = managementController.getRegistry(request.getRegistryId());
    List<RegistryValidationItem> validationItems = new LinkedList<>();

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
        RegistryValidationItem validationItem = new RegistryValidationItem(type, level, message);
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
        RegistryValidationItem validationItem = new RegistryValidationItem(type, level, message);
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
      RegistryValidationItem validationItem = new RegistryValidationItem(type, level, message);
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
   * Generate registry advisor request id
   * TODO: Store registry advisor requests in database.
   * @return
   */
  public long generateRequestId() {
    requestId += 1;
    return requestId;
  }
}
