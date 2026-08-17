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

import ConfigsApi from "../../api/configsApi";
import { useEffect, useRef, useState } from "react";
import { useConfigs } from "../../hooks/useConfigs";
import useStackServices from "../../hooks/useStackServices";
import { Alert, Button, Card, CardBody } from "react-bootstrap";
import Spinner from "../../components/Spinner";
import Table from "../../components/Table";
import Center from "../../components/Center";
import { useContext } from "react";
import { AppContext } from "../../store/context";
import UpgradeGuard from "../../components/UpgradeGuard";
import { serviceAccountConfigs } from "./serviceAccountUtils";

function ServiceAccounts() {
  const { clusterName } = useContext(AppContext);
  const {
    services: stackServices,
    loading: stackServicesLoading,
    error: stackServicesError,
    retry: retryStackServices,
  } = useStackServices();
  const [isLoading, setIsLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [accounts, setAccounts] = useState<any[]>([]);
  const [configRetryAttempt, setConfigRetryAttempt] = useState(0);
  const { getConfigsFromJSON } = useConfigs(
    [],
    stackServices as any
  );
  const getConfigsFromJSONRef = useRef(getConfigsFromJSON);

  useEffect(() => {
    getConfigsFromJSONRef.current = getConfigsFromJSON;
  }, [getConfigsFromJSON]);

  useEffect(() => {
    if (stackServices.length) {
      const getClusterConfigs = async () => {
        setIsLoading(true);
        setLoadError(null);
        try {
          const tags = await ConfigsApi.updateConfigTags(clusterName);
          const urlParams = tags.map(
            (tag: any) => `(type=${tag.siteName}&tag=${tag.tagName})`
          );
          const response = await ConfigsApi.getConfigsByTags(
            clusterName,
            urlParams.join("|")
          );
          let configs: any[] = [];
          (response.items || []).forEach((configObject: any) => {
            configs = configs.concat(getConfigsFromJSONRef.current(configObject));
          });
          setAccounts(serviceAccountConfigs(configs));
        } catch (error: any) {
          setLoadError(
            error?.response?.data?.message
              || error?.message
              || "Service accounts could not be loaded"
          );
        } finally {
          setIsLoading(false);
        }
      };
      void getClusterConfigs();
    } else if (!stackServicesLoading && !stackServicesError) {
      setAccounts([]);
      setIsLoading(false);
    }
  }, [clusterName, configRetryAttempt, stackServices, stackServicesError, stackServicesLoading]);

  if (stackServicesLoading || isLoading) {
    return <Center><Spinner /></Center>;
  }

  const effectiveLoadError = stackServicesError || loadError;
  if (effectiveLoadError) {
    return (
      <UpgradeGuard>
        <Alert variant="danger" className="m-4 d-flex justify-content-between align-items-center">
          <span>{effectiveLoadError}</span>
          <Button
            size="sm"
            variant="outline-danger"
            onClick={() => {
              if (stackServicesError) {
                retryStackServices();
              } else {
                setConfigRetryAttempt((attempt) => attempt + 1);
              }
            }}
          >
            Retry
          </Button>
        </Alert>
      </UpgradeGuard>
    );
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
  return (
    <UpgradeGuard>
      <Card className="m-4">
        <CardBody>
          <h3 style={{fontSize:20}}>Service Users and Groups</h3>
          <Table data={accounts} columns={columns}/>
        </CardBody>
      </Card>
    </UpgradeGuard>
  )
}

export default ServiceAccounts;
