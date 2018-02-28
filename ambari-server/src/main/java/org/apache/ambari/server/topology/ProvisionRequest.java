package org.apache.ambari.server.topology;

import java.util.Collection;
import java.util.Set;

import org.apache.ambari.server.controller.internal.ProvisionAction;
import org.apache.ambari.server.state.StackId;

public interface ProvisionRequest extends TopologyRequest {

  ConfigRecommendationStrategy getConfigRecommendationStrategy();
  ProvisionAction getProvisionAction();
  String getDefaultPassword();
  Set<StackId> getStackIds();
  Collection<MpackInstance> getMpacks();

}
