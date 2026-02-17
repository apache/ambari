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
import { useContext, useEffect, useState } from "react";
import ClusterApi from "../api/clusterApi";
import { isEmpty } from "lodash";
import { AppContext } from "../store/context";
import ConfigsApi from "../api/configsApi";
import { ChooseServicesApi } from "../api/chooseServicesApi";

function useStackServices() {
  const [services, setServices] = useState<any[]>([]);
  const [versionDetails, setVersionDetails] = useState<any>({});
  const {isClusterInstalled}=useContext(AppContext);
  const getConfigsCollectionMap = async () => { 
    //@ts-ignore
    const configs = await ConfigsApi.loadConfigsFromStack(
      versionDetails.stack,
      versionDetails.version,
      []
    );
  };

  async function getClusterVersionDetails() {
    const clusterName = await ClusterApi.getClusterName();
    const clusterDetails = await ClusterApi.getDesiredClusterConfigs(
      clusterName,
      `Clusters/provisioning_state,Clusters/security_type,Clusters/version,Clusters/cluster_id`
    );
    const cluster = clusterDetails?.Clusters;
    console.log("Cluster Details", clusterDetails, cluster);
    setVersionDetails({
      stack: cluster.version.split("-")[0],
      version: cluster.version.split("-")[1],
    });
  }

  useEffect(() => {
    const fetchServices = async () => {
      const services = await ChooseServicesApi.getServices(
        versionDetails?.stack,
        versionDetails?.version
      );
      setServices(services.items);
    };
    console.log("Version Details are",versionDetails);
    if (!isEmpty(versionDetails)) {
      fetchServices();
      getConfigsCollectionMap();
    }
  }, [versionDetails]);

  useEffect(()=>{
    if(isClusterInstalled)
    getClusterVersionDetails()
  },[isClusterInstalled])

  return { services };
}

export default useStackServices;
