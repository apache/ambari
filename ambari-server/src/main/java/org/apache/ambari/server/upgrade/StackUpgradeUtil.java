/*
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
package org.apache.ambari.server.upgrade;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.orm.dao.ClusterDAO;
import org.apache.ambari.server.orm.dao.ClusterStateDAO;
import org.apache.ambari.server.orm.dao.HostComponentDesiredStateDAO;
import org.apache.ambari.server.orm.dao.HostComponentStateDAO;
import org.apache.ambari.server.orm.dao.ServiceComponentDesiredStateDAO;
import org.apache.ambari.server.orm.dao.ServiceDesiredStateDAO;
import org.apache.ambari.server.orm.entities.ClusterEntity;
import org.apache.ambari.server.orm.entities.ClusterStateEntity;
import org.apache.ambari.server.orm.entities.HostComponentDesiredStateEntity;
import org.apache.ambari.server.orm.entities.HostComponentStateEntity;
import org.apache.ambari.server.orm.entities.ServiceComponentDesiredStateEntity;
import org.apache.ambari.server.orm.entities.ServiceDesiredStateEntity;
import org.apache.ambari.server.state.OperatingSystemInfo;
import org.apache.ambari.server.state.StackId;

import com.google.gson.Gson;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.persist.Transactional;

public class StackUpgradeUtil {
  @Inject
  private Gson gson;
  @Inject
  private Injector injector;

  private String getStackIdString(String originalStackId, String stackName,
                                  String stackVersion) {
    if (stackVersion == null) {
      stackVersion = gson.fromJson(originalStackId, StackId.class).getStackVersion();
    }

    return String.format(
      "{\"stackName\":\"%s\",\"stackVersion\":\"%s\"}",
      stackName,
      stackVersion
    );
  }

  @Transactional
  public void updateStackDetails(String stackName, String stackVersion) {
    ClusterDAO clusterDAO = injector.getInstance(ClusterDAO.class);
    List<Long> clusterIds = new ArrayList<Long>();

    List<ClusterEntity> clusterEntities = clusterDAO.findAll();
    if (clusterEntities != null && !clusterEntities.isEmpty()) {
      for (ClusterEntity entity : clusterEntities) {
        clusterIds.add(entity.getClusterId());
        String stackIdString = entity.getDesiredStackVersion();
        entity.setDesiredStackVersion(getStackIdString(stackIdString,
          stackName, stackVersion));
        clusterDAO.merge(entity);
      }
    }

    ClusterStateDAO clusterStateDAO = injector.getInstance(ClusterStateDAO.class);

    for (Long clusterId : clusterIds) {
      ClusterStateEntity clusterStateEntity = clusterStateDAO.findByPK(clusterId);
      String currentStackVersion = clusterStateEntity.getCurrentStackVersion();
      clusterStateEntity.setCurrentStackVersion(getStackIdString
        (currentStackVersion, stackName, stackVersion));
      clusterStateDAO.merge(clusterStateEntity);
    }

    HostComponentStateDAO hostComponentStateDAO = injector.getInstance
      (HostComponentStateDAO.class);
    List<HostComponentStateEntity> hcEntities = hostComponentStateDAO.findAll();

    if (hcEntities != null) {
      for (HostComponentStateEntity hc : hcEntities) {
        String currentStackVersion = hc.getCurrentStackVersion();
        hc.setCurrentStackVersion(getStackIdString(currentStackVersion,
          stackName, stackVersion));
        hostComponentStateDAO.merge(hc);
      }
    }

    HostComponentDesiredStateDAO hostComponentDesiredStateDAO =
      injector.getInstance(HostComponentDesiredStateDAO.class);

    List<HostComponentDesiredStateEntity> hcdEntities = hostComponentDesiredStateDAO.findAll();

    if (hcdEntities != null) {
      for (HostComponentDesiredStateEntity hcd : hcdEntities) {
        String desiredStackVersion = hcd.getDesiredStackVersion();
        hcd.setDesiredStackVersion(getStackIdString(desiredStackVersion,
          stackName, stackVersion));
        hostComponentDesiredStateDAO.merge(hcd);
      }
    }

    ServiceComponentDesiredStateDAO serviceComponentDesiredStateDAO =
      injector.getInstance(ServiceComponentDesiredStateDAO.class);

    List<ServiceComponentDesiredStateEntity> scdEntities =
      serviceComponentDesiredStateDAO.findAll();

    if (scdEntities != null) {
      for (ServiceComponentDesiredStateEntity scd : scdEntities) {
        String desiredStackVersion = scd.getDesiredStackVersion();
        scd.setDesiredStackVersion(getStackIdString(desiredStackVersion,
          stackName, stackVersion));
        serviceComponentDesiredStateDAO.merge(scd);
      }
    }

    ServiceDesiredStateDAO serviceDesiredStateDAO = injector.getInstance(ServiceDesiredStateDAO.class);

    List<ServiceDesiredStateEntity> sdEntities = serviceDesiredStateDAO.findAll();

    if (sdEntities != null) {
      for (ServiceDesiredStateEntity sd : sdEntities) {
        String desiredStackVersion = sd.getDesiredStackVersion();
        sd.setDesiredStackVersion(getStackIdString(desiredStackVersion,
          stackName, stackVersion));
        serviceDesiredStateDAO.merge(sd);
      }
    }


  }

  /**
   * @param stackName
   * @param stackVersion
   * @param repoUrl
   * @param repoUrlOs
   * @throws Exception
   */
  public void updateLocalRepo(String stackName, String stackVersion,
      String repoUrl, String repoUrlOs) throws Exception {

    if (null == repoUrl ||
        repoUrl.isEmpty() ||
        !repoUrl.startsWith("http"))
      return;
    
    String server = repoUrl;
    
    String[] oses = new String[0]; 
    
    if (null != repoUrlOs) {
      oses = repoUrlOs.split(",");
    }
    
    AmbariMetaInfo ami = injector.getInstance(AmbariMetaInfo.class);
    
    if (0 == oses.length) {
      // do them all
      for (OperatingSystemInfo osi : ami.getOperatingSystems(stackName, stackVersion)) {
        ami.updateRepoBaseURL(stackName, stackVersion, osi.getOsType(),
            stackName + "-" + stackVersion, server);
      }
      
    } else {
      for (String os : oses) {
        ami.updateRepoBaseURL(stackName, stackVersion, os,
            stackName + "-" + stackVersion, server);
      }
    }
  }

}