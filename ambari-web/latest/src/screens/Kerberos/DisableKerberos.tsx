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
import { RequestApi } from "../../api/requestApi";
import { AppContext } from "../../store/context";
import { map } from "lodash";
import { translate } from "../../Utils/Utility";
import useKDCSessionState from "../../hooks/useKDCSessionState";
import OperationsProgress from "../../components/OperationsProgress";
import KerberosApi from "../../api/kerberosApi";

type disableKerberosProps = {
    setDisableKerberosInProgress:any;
}

export default function disableKerberos({setDisableKerberosInProgress}:disableKerberosProps) {
  const [completionStatus, setCompletionStatus] = useState(false)
  const { clusterName, services, ambariProperties } = useContext(AppContext);
  const { getKDCSessionState } = useKDCSessionState(() => {});

  useEffect(()=>{
    setDisableKerberosInProgress(!completionStatus);
  },[completionStatus])

  const operations = [
    {
        id: "1",
        label: "Start Zookeeper",
        skippable: false,
        context: "Start required services",
        callback: async () => {
            const zookeeperPayload = {
                "RequestInfo": {
                    "context": "Start required services",
                    "operation_level": {
                        "level": "CLUSTER",
                        "cluster_name": `${clusterName}`
                    }
                },
                "Body": {
                    "ServiceInfo": {
                        "state": "STARTED"
                    }
                }
            }
            const params = "ServiceInfo/service_name.in(ZOOKEEPER)"
            const requestData = await RequestApi.getServicesWithStatus(
                clusterName, 
                zookeeperPayload,
                params
            )
            return requestData;
        }
    },    
    {
        id: "2",
        label: "Stop Required Services",
        skippable: false,
        context: "Stop required services",
        callback: async () => {
            const servicesPayload = {
                "RequestInfo": {
                    "context": "Stop required services",
                    "operation_level": {
                        "level": "CLUSTER",
                        "cluster_name": `${clusterName}`
                    }
                },
                "Body": {
                    "ServiceInfo": {
                        "state": "INSTALLED"
                    }
                }
            }
            const serviceNames = map(
                services.filter((service) => service.ServiceInfo.service_name !== "ZOOKEEPER"),
                "ServiceInfo.service_name"
              ).join(",");

            const params = `ServiceInfo/service_name.in(${serviceNames})`
            const requestData = await RequestApi.getServicesWithStatus(
                clusterName, 
                servicesPayload,
                params
            )
            return requestData;
        }
    },
    {
        id: "3",
        label: "Unkerberize Cluster",
        context: "Unkerbize cluster",
        callback: async () => {
            return new Promise((resolve, reject) => {
                getKDCSessionState(async () => {
                    const payload = {
                        "Clusters": {
                            "security_type": "NONE"
                        }
                    }
                    const requestData = await RequestApi.preparingOperations(
                        clusterName,
                        payload
                    )
                    resolve(requestData);   
                }, reject);
            })
        },
        skipCallback: async () => {
            return await RequestApi.preparingOperations(
                clusterName,
                { Clusters: { security_type: "NONE" } },
                "manage_kerberos_identities=false"
            );
        },
        skippable: true,
    },
    {
        id: "4",
        label: "Remove Kerberos",
        skippable: false,
        context: "remove kerberos",
        callback: async () => {
            try {
                return await KerberosApi.deleteKerberosService(
                    clusterName,
                    "KERBEROS"
                );
            } catch {
                // Classic continues even when the obsolete service is already absent.
                return undefined;
            }
        }
    },
    {
      id: "5",
      label: "Start Services",
      skippable: false,
      context: "Start services",
      callback: async () => {
        const startAndTestServicesPayload = {
            "RequestInfo": {
                "context": "Start services",
                "operation_level": {
                    "level": "CLUSTER",
                    "cluster_name": `${clusterName}`
                }
            },
            "Body": {
                "ServiceInfo": {
                    "state": "STARTED"
                }
            }
        };
        const requestData = await RequestApi.startServices(
          clusterName,
          startAndTestServicesPayload,
          `run_smoke_test=${ambariProperties?.["skip.service.checks"] !== "true"}`
        );
        return requestData;
      },
    },
  ];
  return (
    <>
        { completionStatus && (
            <div className="alert alert-success">
                {translate('admin.security.disable.body.success.header')}
            </div>
        )}
      <OperationsProgress
        operations={operations as any}
        title="Disable kerberos"
        description="Disable Kerberos"
        setCompletionStatus={setCompletionStatus}
      />
    </>
  );
}
