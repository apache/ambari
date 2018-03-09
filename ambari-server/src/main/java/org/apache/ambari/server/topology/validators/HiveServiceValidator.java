/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.topology.validators;

import org.apache.ambari.server.topology.ClusterTopology;
import org.apache.ambari.server.topology.Configuration;
import org.apache.ambari.server.topology.InvalidTopologyException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Validates hive service related setup before provisioning the cluster.
 */
public class HiveServiceValidator implements TopologyValidator {

  private static final Logger LOGGER = LoggerFactory.getLogger(HiveServiceValidator.class);
  private static final String HIVE_ENV = "hive-env";
  private static final String HIVE_DB_DEFAULT = "New MySQL Database";
  private static final String HIVE_DB_PROPERTY = "hive_database";
  private static final String MYSQL_SERVER_COMPONENT = "MYSQL_SERVER";
  private static final String HIVE_SERVICE = "HIVE";


  @Override
  public void validate(ClusterTopology topology) throws InvalidTopologyException {
    // there is no hive configured in the blueprint, nothing to do (does the validator apply?)
    if (!topology.getServices().contains(HIVE_SERVICE)) {
      LOGGER.info(" [{}] service is not listed in the blueprint, skipping hive service validation.", HIVE_SERVICE);
      return;
    }

    Configuration clusterConfiguration = topology.getConfiguration();

    // hive database settings are missing (this should never be the case, defaults come from the stack def.)
    if (!clusterConfiguration.getAllConfigTypes().contains(HIVE_ENV)) {
      String errorMessage = String.format(" [ %s ] config type is missing from the service [ %s ]. HIVE service validation failed.", HIVE_ENV, HIVE_SERVICE);
      LOGGER.error(errorMessage);
      throw new InvalidTopologyException(errorMessage);
    }

    boolean hiveWantsMysql = HIVE_DB_DEFAULT.equals(clusterConfiguration.getPropertyValue(HIVE_ENV, HIVE_DB_PROPERTY));
    boolean topologyContainsMysql = topology.getComponents()
      .anyMatch(c -> MYSQL_SERVER_COMPONENT.equals(c.componentName()) && HIVE_SERVICE.equals(c.effectiveServiceName()));

    if (topologyContainsMysql && !hiveWantsMysql) {
      String errorMessage = String.format(
        "Incorrect configuration: %s component is specified in blueprint, but Hive is configured to use existing DB",
        MYSQL_SERVER_COMPONENT);
      LOGGER.error(errorMessage);
      throw new InvalidTopologyException(errorMessage);
    }

    if (hiveWantsMysql && !topologyContainsMysql) {
      String errorMessage = String.format(
        "The component %s must explicitly be specified in the blueprint if Hive database is configured with %s.",
        MYSQL_SERVER_COMPONENT, HIVE_DB_DEFAULT);
      LOGGER.error(errorMessage);
      throw new InvalidTopologyException(errorMessage);
    }
  }

}
