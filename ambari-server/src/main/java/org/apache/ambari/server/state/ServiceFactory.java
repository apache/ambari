package org.apache.ambari.server.state;

import org.apache.ambari.server.orm.entities.ClusterServiceEntity;

public interface ServiceFactory {

  Service createNew(Cluster cluster, String serviceName);

  Service createExisting(Cluster cluster, ClusterServiceEntity serviceEntity);
}
