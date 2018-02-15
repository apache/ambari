package org.apache.ambari.server.topology;

import java.util.Map;
import java.util.Set;

public interface ComponentResolver {

  Map<String, Set<ResolvedComponent>> resolveComponents(BlueprintBasedClusterProvisionRequest request);

}
