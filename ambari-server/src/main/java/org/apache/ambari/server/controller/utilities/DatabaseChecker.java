/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.controller.utilities;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Scanner;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.orm.dao.ClusterDAO;
import org.apache.ambari.server.orm.dao.MetainfoDAO;
import org.apache.ambari.server.orm.entities.ClusterEntity;
import org.apache.ambari.server.orm.entities.ClusterServiceEntity;
import org.apache.ambari.server.orm.entities.HostComponentDesiredStateEntity;
import org.apache.ambari.server.orm.entities.HostComponentStateEntity;
import org.apache.ambari.server.orm.entities.MetainfoEntity;
import org.apache.ambari.server.orm.entities.ServiceComponentDesiredStateEntity;
import org.apache.ambari.server.orm.entities.ServiceDesiredStateEntity;
import org.apache.ambari.server.state.ComponentInfo;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.utils.VersionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Injector;

public class DatabaseChecker {

  static Logger LOG = LoggerFactory.getLogger(DatabaseChecker.class);

  @Inject
  static Injector injector;
  static AmbariMetaInfo ambariMetaInfo;
  static MetainfoDAO metainfoDAO;

  public static void checkDBConsistency() throws AmbariException {
    LOG.info("Checking DB consistency");

    boolean checkPassed = true;
    if (ambariMetaInfo == null) {
      ambariMetaInfo = injector.getInstance(AmbariMetaInfo.class);
    }

    ClusterDAO clusterDAO = injector.getInstance(ClusterDAO.class);
    List<ClusterEntity> clusters = clusterDAO.findAll();
    for (ClusterEntity clusterEntity: clusters) {
      StackId stackId = new StackId(clusterEntity.getDesiredStack());

      Collection<ClusterServiceEntity> serviceEntities =
        clusterEntity.getClusterServiceEntities();
      for (ClusterServiceEntity clusterServiceEntity : serviceEntities) {

        ServiceDesiredStateEntity serviceDesiredStateEntity =
          clusterServiceEntity.getServiceDesiredStateEntity();
        if (serviceDesiredStateEntity == null) {
          checkPassed = false;
          LOG.error(String.format("ServiceDesiredStateEntity is null for " +
              "ServiceComponentDesiredStateEntity, clusterName=%s, serviceName=%s ",
            clusterEntity.getClusterName(), clusterServiceEntity.getServiceName()));
        }
        Collection<ServiceComponentDesiredStateEntity> scDesiredStateEntities =
          clusterServiceEntity.getServiceComponentDesiredStateEntities();
        if (scDesiredStateEntities == null ||
          scDesiredStateEntities.isEmpty()) {
          checkPassed = false;
          LOG.error(String.format("serviceComponentDesiredStateEntities is null or empty for " +
              "ServiceComponentDesiredStateEntity, clusterName=%s, serviceName=%s ",
            clusterEntity.getClusterName(), clusterServiceEntity.getServiceName()));
        } else {
          for (ServiceComponentDesiredStateEntity scDesiredStateEnity : scDesiredStateEntities) {

            Collection<HostComponentDesiredStateEntity> schDesiredStateEntities =
              scDesiredStateEnity.getHostComponentDesiredStateEntities();
            Collection<HostComponentStateEntity> schStateEntities =
              scDesiredStateEnity.getHostComponentStateEntities();

            ComponentInfo componentInfo = ambariMetaInfo.getComponent(
              stackId.getStackName(), stackId.getStackVersion(),
              scDesiredStateEnity.getServiceName(), scDesiredStateEnity.getComponentName());

            boolean zeroCardinality = componentInfo.getCardinality() == null
              || componentInfo.getCardinality().startsWith("0")
              || scDesiredStateEnity.getComponentName().equals("SECONDARY_NAMENODE"); // cardinality 0 for NameNode HA

            boolean componentCheckFailed = false;

            if (schDesiredStateEntities == null) {
              componentCheckFailed = true;
              LOG.error(String.format("hostComponentDesiredStateEntities is null for " +
                  "ServiceComponentDesiredStateEntity, clusterName=%s, serviceName=%s, componentName=%s ",
                clusterEntity.getClusterName(), scDesiredStateEnity.getServiceName(), scDesiredStateEnity.getComponentName()));
            } else if (!zeroCardinality && schDesiredStateEntities.isEmpty()) {
              componentCheckFailed = true;
              LOG.error(String.format("hostComponentDesiredStateEntities is empty for " +
                  "ServiceComponentDesiredStateEntity, clusterName=%s, serviceName=%s, componentName=%s ",
                clusterEntity.getClusterName(), scDesiredStateEnity.getServiceName(), scDesiredStateEnity.getComponentName()));
            }

            if (schStateEntities == null) {
              componentCheckFailed = true;
              LOG.error(String.format("hostComponentStateEntities is null for " +
                  "ServiceComponentDesiredStateEntity, clusterName=%s, serviceName=%s, componentName=%s ",
                clusterEntity.getClusterName(), scDesiredStateEnity.getServiceName(), scDesiredStateEnity.getComponentName()));
            } else if (!zeroCardinality && schStateEntities.isEmpty()) {
              componentCheckFailed = true;
              LOG.error(String.format("hostComponentStateEntities is empty for " +
                  "ServiceComponentDesiredStateEntity, clusterName=%s, serviceName=%s, componentName=%s ",
                clusterEntity.getClusterName(), scDesiredStateEnity.getServiceName(), scDesiredStateEnity.getComponentName()));
            }

            if (!componentCheckFailed &&
              schDesiredStateEntities.size() != schStateEntities.size()) {
              checkPassed = false;
              LOG.error(String.format("HostComponentStateEntities and HostComponentDesiredStateEntities " +
                  "tables must contain equal number of rows mapped to ServiceComponentDesiredStateEntity, " +
                  "(clusterName=%s, serviceName=%s, componentName=%s) ", clusterEntity.getClusterName(),
                scDesiredStateEnity.getServiceName(), scDesiredStateEnity.getComponentName()));
            }
            checkPassed = checkPassed && !componentCheckFailed;
          }
        }
      }
    }
    if (checkPassed) {
      LOG.info("DB consistency check passed.");
    } else {
      LOG.error("DB consistency check failed.");
    }
  }

  public static void checkDBVersion() throws AmbariException {

    LOG.info("Checking DB store version");
    if (metainfoDAO == null) {
      metainfoDAO = injector.getInstance(MetainfoDAO.class);
    }

    MetainfoEntity schemaVersionEntity = metainfoDAO.findByKey(Configuration.SERVER_VERSION_KEY);
    String schemaVersion = null;

    if (schemaVersionEntity != null) {
      schemaVersion = schemaVersionEntity.getMetainfoValue();
    }

    Configuration conf = injector.getInstance(Configuration.class);
    File versionFile = new File(conf.getServerVersionFilePath());
    if (!versionFile.exists()) {
      throw new AmbariException("Server version file does not exist.");
    }
    String serverVersion = null;
    try (Scanner scanner = new Scanner(versionFile)) {
      serverVersion = scanner.useDelimiter("\\Z").next();

    } catch (IOException ioe) {
      throw new AmbariException("Unable to read server version file.");
    }

    if (schemaVersionEntity==null || VersionUtils.compareVersions(schemaVersion, serverVersion, 3) != 0) {
      String error = "Current database store version is not compatible with " +
        "current server version"
        + ", serverVersion=" + serverVersion
        + ", schemaVersion=" + schemaVersion;
      LOG.warn(error);
      throw new AmbariException(error);
    }

    LOG.info("DB store version is compatible");
  }


}
