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

import { filter, forEach, set } from "lodash";
import { useEffect, useState } from "react";
import { useConfigs } from "../../hooks/useConfigs";
import useStackServices from "../../hooks/useStackServices";
import { Card, CardBody } from "react-bootstrap";
import Spinner from "../../components/Spinner";
import Table from "../../components/Table";
import Center from "../../components/Center";
import { useContext } from "react";
import { AppContext } from "../../store/context.tsx";
import UpgradeGuard from "../../components/UpgradeGuard";
import ConfigsApi from "../../api/configsApi.ts";

function ServiceAccounts() {
  const { clusterName } = useContext(AppContext);
  const [serverConfigs, setServerConfigs] = useState([]);
  const { services: stackServices } = useStackServices();
  const [isLoading,setIsLoading]=useState(true);
  const [accounts,setAccounts]=useState([]);
  const { getConfigsFromJSON } = useConfigs(
    serverConfigs,
    stackServices as any
  );

  useEffect(() => {
    console.log("Server Configs", serverConfigs);
    if (serverConfigs.length) {
      createConfigObject();
    }
  }, [serverConfigs.length]);

  const getConfigBySites = async (tags: any[]) => {
    let urlParams: string[] = [];
    tags.forEach(function (_tag: any) {
      urlParams.push("(type=" + _tag.siteName + "&tag=" + _tag.tagName + ")");
    });
    const allProperties = await ConfigsApi.getConfigsByTags(
      clusterName,
      urlParams.join("|")
    );

    console.log("All Properties are", allProperties);
    setServerConfigs(allProperties.items);
    // return get(allProperties, "items.0.properties", []);
  };

  const createConfigObject = () => {
    let configs: any = [];
    serverConfigs.forEach(function (configObject: any) {
      configs = configs.concat(getConfigsFromJSON(configObject));
    });
    
    let miscConfigs = filter(configs, (config) => {
      return config.displayType === "user" && 
             config.category === "Users and Groups" &&
             config.isVisible !== false;
    });
    
    // Ensure all configs are visible and sort them for consistent display
    forEach(miscConfigs, function (config) {
      set(config, "isVisible", true);
    });
    
    // Sort by display name for consistent ordering like classic UI
    miscConfigs = miscConfigs.sort((a: any, b: any) => {
      return (a.displayName || a.name).localeCompare(b.displayName || b.name);
    });
    
    setAccounts(miscConfigs as any);
    setIsLoading(false);
  };

  const getClusterConfigs = async () => {
    const tags = await ConfigsApi.updateConfigTags(clusterName);
    await getConfigBySites(tags);
  };
  useEffect(() => {
    if (stackServices.length) {
      getClusterConfigs();
    }
  }, [stackServices.length]);

  if(isLoading){
    return <Center><Spinner/></Center>
  }

  const columns=[{
    header:"Name",
    accessorKey:"displayName",
    id:"displayName",
  },{
    header:"Value",
    accessorKey:"value",
    id:"value"
  }]
  console.log("Accounts are",accounts)
  return (
    <UpgradeGuard>
      <Card className="m-4">
        <CardBody>
          <h1 style={{fontSize:20}}>Service Users and Groups</h1>
          <Table data={accounts} columns={columns}/>
        </CardBody>
      </Card>
    </UpgradeGuard>
  )
}

export default ServiceAccounts;
