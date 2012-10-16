package org.apache.ambari.server.state;

import com.google.inject.assistedinject.Assisted;
import org.apache.ambari.server.orm.entities.HostComponentDesiredStateEntity;
import org.apache.ambari.server.orm.entities.HostComponentStateEntity;

public interface ServiceComponentHostFactory {

  ServiceComponentHost createNew(ServiceComponent serviceComponent,
                                 String hostName, boolean isClient);

  ServiceComponentHost createExisting(ServiceComponent serviceComponent,
                                      HostComponentStateEntity stateEntity,
                                      HostComponentDesiredStateEntity desiredStateEntity);
}
