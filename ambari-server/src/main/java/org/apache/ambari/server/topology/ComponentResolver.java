package org.apache.ambari.server.topology;

import java.util.Map;
import java.util.Set;

/**
 * Resolves all incompletely specified host group components in the topology:
 * finds stack and/or service type that each component is defined in.
 */
public interface ComponentResolver {

  /**
   * @return the set resolved components for each host group (the map's keys are host group names)
   * @throws IllegalArgumentException if the components cannot be unambiguously resolved
   * (eg. if some component is not known, or if there are multiple component with the same name and
   * the request does not specify which one to select)
   */
  Map<String, Set<ResolvedComponent>> resolveComponents(BlueprintBasedClusterProvisionRequest request);

}
