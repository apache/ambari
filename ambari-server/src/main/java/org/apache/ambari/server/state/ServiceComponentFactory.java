package org.apache.ambari.server.state;

import org.apache.ambari.server.orm.entities.ServiceComponentDesiredStateEntity;

public interface ServiceComponentFactory {

  ServiceComponent createNew(Service service, String componentName);

  ServiceComponent createExisting(Service service, ServiceComponentDesiredStateEntity serviceComponentDesiredStateEntity);
}
