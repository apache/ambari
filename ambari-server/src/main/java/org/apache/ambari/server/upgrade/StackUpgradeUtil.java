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

import java.util.ArrayList;
import java.util.List;

import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.orm.dao.ClusterDAO;
import org.apache.ambari.server.orm.dao.ClusterStateDAO;
import org.apache.ambari.server.orm.dao.HostComponentDesiredStateDAO;
import org.apache.ambari.server.orm.dao.HostComponentStateDAO;
import org.apache.ambari.server.orm.dao.MetainfoDAO;
import org.apache.ambari.server.orm.dao.ServiceComponentDesiredStateDAO;
import org.apache.ambari.server.orm.dao.ServiceDesiredStateDAO;
import org.apache.ambari.server.orm.dao.StackDAO;
import org.apache.ambari.server.orm.entities.ClusterEntity;
import org.apache.ambari.server.orm.entities.ClusterStateEntity;
import org.apache.ambari.server.orm.entities.HostComponentDesiredStateEntity;
import org.apache.ambari.server.orm.entities.HostComponentStateEntity;
import org.apache.ambari.server.orm.entities.MetainfoEntity;
import org.apache.ambari.server.orm.entities.ServiceComponentDesiredStateEntity;
import org.apache.ambari.server.orm.entities.ServiceDesiredStateEntity;
import org.apache.ambari.server.orm.entities.StackEntity;
import org.apache.ambari.server.state.OperatingSystemInfo;
import org.apache.ambari.server.state.stack.OsFamily;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.persist.Transactional;

public class StackUpgradeUtil {
  @Inject
  private Injector injector;

  @Transactional
  public void updateStackDetails(String stackName, String stackVersion) {
    ClusterDAO clusterDAO = injector.getInstance(ClusterDAO.class);
    StackDAO stackDAO = injector.getInstance(StackDAO.class);
    List<Long> clusterIds = new ArrayList<Long>();

    StackEntity stackEntity = stackDAO.find(stackName, stackVersion);

    List<ClusterEntity> clusterEntities = clusterDAO.findAll();
    if (clusterEntities != null && !clusterEntities.isEmpty()) {
      for (ClusterEntity entity : clusterEntities) {
        clusterIds.add(entity.getClusterId());
        entity.setDesiredStack(stackEntity);
        clusterDAO.merge(entity);
      }
    }

    ClusterStateDAO clusterStateDAO = injector.getInstance(ClusterStateDAO.class);

    for (Long clusterId : clusterIds) {
      ClusterStateEntity clusterStateEntity = clusterStateDAO.findByPK(clusterId);
      clusterStateEntity.setCurrentStack(stackEntity);
      clusterStateDAO.merge(clusterStateEntity);
    }

    HostComponentStateDAO hostComponentStateDAO = injector.getInstance
      (HostComponentStateDAO.class);
    List<HostComponentStateEntity> hcEntities = hostComponentStateDAO.findAll();

    if (hcEntities != null) {
      for (HostComponentStateEntity hc : hcEntities) {
        hc.setCurrentStack(stackEntity);
        hostComponentStateDAO.merge(hc);
      }
    }

    HostComponentDesiredStateDAO hostComponentDesiredStateDAO =
      injector.getInstance(HostComponentDesiredStateDAO.class);

    List<HostComponentDesiredStateEntity> hcdEntities = hostComponentDesiredStateDAO.findAll();

    if (hcdEntities != null) {
      for (HostComponentDesiredStateEntity hcd : hcdEntities) {
        hcd.setDesiredStack(stackEntity);
        hostComponentDesiredStateDAO.merge(hcd);
      }
    }

    ServiceComponentDesiredStateDAO serviceComponentDesiredStateDAO =
      injector.getInstance(ServiceComponentDesiredStateDAO.class);

    List<ServiceComponentDesiredStateEntity> scdEntities =
      serviceComponentDesiredStateDAO.findAll();

    if (scdEntities != null) {
      for (ServiceComponentDesiredStateEntity scd : scdEntities) {
        scd.setDesiredStack(stackEntity);
        serviceComponentDesiredStateDAO.merge(scd);
      }
    }

    ServiceDesiredStateDAO serviceDesiredStateDAO = injector.getInstance(ServiceDesiredStateDAO.class);

    List<ServiceDesiredStateEntity> sdEntities = serviceDesiredStateDAO.findAll();

    if (sdEntities != null) {
      for (ServiceDesiredStateEntity sd : sdEntities) {
        sd.setDesiredStack(stackEntity);
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
        !repoUrl.startsWith("http")) {
      return;
    }

    String[] oses = new String[0];

    if (null != repoUrlOs) {
      oses = repoUrlOs.split(",");
    }

    AmbariMetaInfo ami = injector.getInstance(AmbariMetaInfo.class);
    MetainfoDAO metaDao = injector.getInstance(MetainfoDAO.class);
    OsFamily os_family = injector.getInstance(OsFamily.class);

    String stackRepoId = stackName + "-" + stackVersion;

    if (0 == oses.length) {
      // do them all
      for (OperatingSystemInfo osi : ami.getOperatingSystems(stackName, stackVersion)) {
        ami.updateRepoBaseURL(stackName, stackVersion, osi.getOsType(),
            stackRepoId, repoUrl);
      }

    } else {
      for (String os : oses) {

        String family = os_family.find(os);
        if (null != family) {
          String key = ami.generateRepoMetaKey(stackName, stackVersion, os,
              stackRepoId, AmbariMetaInfo.REPOSITORY_XML_PROPERTY_BASEURL);

          String familyKey = ami.generateRepoMetaKey(stackName, stackVersion, family,
              stackRepoId, AmbariMetaInfo.REPOSITORY_XML_PROPERTY_BASEURL);

          // need to use (for example) redhat6 if the os is centos6
          MetainfoEntity entity = metaDao.findByKey(key);
          if (null == entity) {
            entity = new MetainfoEntity();
            entity.setMetainfoName(key);
            entity.setMetainfoValue(repoUrl);
            metaDao.merge(entity);
          } else {
            entity.setMetainfoValue(repoUrl);
            metaDao.merge(entity);
          }

          entity = metaDao.findByKey(familyKey);
          if (null == entity) {
            entity = new MetainfoEntity();
            entity.setMetainfoName(familyKey);
            entity.setMetainfoValue(repoUrl);
            metaDao.merge(entity);
          } else {
            entity.setMetainfoValue(repoUrl);
            metaDao.merge(entity);
          }
        }
      }
    }
  }

}