package org.apache.ambari.server.controller;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.actionmanager.Stage;
import org.apache.ambari.server.controller.internal.RequestStageContainer;
import org.apache.ambari.server.metadata.RoleCommandOrder;
import org.apache.ambari.server.stageplanner.RoleGraph;
import org.apache.ambari.server.stageplanner.RoleGraphFactory;

/**
 * An extension of RequestStageContainer that takes the role command order into consideration when adding stages
 */
public class OrderedRequestStageContainer {
  private final RoleGraphFactory roleGraphFactory;
  private final RoleCommandOrder roleCommandOrder;
  private final RequestStageContainer requestStageContainer;

  public OrderedRequestStageContainer(RoleGraphFactory roleGraphFactory, RoleCommandOrder roleCommandOrder, RequestStageContainer requestStageContainer) {
    this.roleGraphFactory = roleGraphFactory;
    this.roleCommandOrder = roleCommandOrder;
    this.requestStageContainer = requestStageContainer;
  }

  public void addStage(Stage stage) throws AmbariException {
    RoleGraph roleGraph = roleGraphFactory.createNew(roleCommandOrder);
    roleGraph.build(stage);
    requestStageContainer.addStages(roleGraph.getStages());
  }

  public long getLastStageId() {
    return requestStageContainer.getLastStageId();
  }

  public long getId() {
    return requestStageContainer.getId();
  }

  public RequestStageContainer getRequestStageContainer() {
    return requestStageContainer;
  }

  public void setClusterHostInfo(String clusterHostInfo) {
    this.requestStageContainer.setClusterHostInfo(clusterHostInfo);
  }
}
