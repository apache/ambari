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

import { useHDFSConfigUpdater } from "../hooks/useHDFSConfigUpdater";
import { useZkConfigUpdater } from "../hooks/useZkConfigUpdater";
import { useHbaseConfigUpdater } from "../hooks/useHbaseConfigUpdater";
import { useRangerConfigUpdater } from "../hooks/useRangerConfigUpdater";
import { useContext } from "react";
import { AppContext } from "./context";
import { useMapReduce2ConfigUpdater } from "../hooks/useMapReduce2ConfigUpdater";
import { useTezConfigUpdater } from "../hooks/useTezConfigUpdater";
import { useSpark3ConfigUpdater } from "../hooks/useSpark3ConfigUpdater";
import { useKerberosConfigUpdater } from "../hooks/useKerberosConfigUpdater";
import { useRangerKMSConfigUpdater } from "../hooks/useRangerKMSConfigUpdater";
import { useTrinoConfigUpdater } from "../hooks/useTrinoConfigUpdater";
import { useSSMConfigUpdater } from "../hooks/useSSMConfigUpdater";
import { useYarnConfigUpdater } from "../hooks/useYarnConfigUpdater";
import { useHiveConfigUpdater } from "../hooks/useHiveConfigUpdater";
import { useKyuubiConfigUpdater } from "../hooks/useKyuubiConfigUpdater";
import { useSqoopConfigUpdater } from "../hooks/useSqoopConfigUpdater";
import { useTrinoGatewayConfigUpdater } from "../hooks/useTrinoGatewayConfigUpdater";

function Updater() {
  const { services } = useContext(AppContext);
  console.log("service for updater ", services);
  services.forEach((service) => {
    const serviceName = service.ServiceInfo.service_name;
    switch (serviceName) {
      case "HDFS":
        useHDFSConfigUpdater();
        break;
      case "HBASE":
        useHbaseConfigUpdater();
        break;
      case "RANGER":
        useRangerConfigUpdater();
        break;
      case "ZOOKEEPER":
        useZkConfigUpdater();
        break;
      case "MAPREDUCE2":
        useMapReduce2ConfigUpdater();
        break;
      case "TEZ":
        useTezConfigUpdater();
        break;
      case "SPARK3":
        useSpark3ConfigUpdater();
        break;
      case "KERBEROS":
        useKerberosConfigUpdater();
        break;
      case "RANGER_KMS":
        useRangerKMSConfigUpdater();
        break;
      case "TRINO":
        useTrinoConfigUpdater();
        break;
      case "SSM":
        useSSMConfigUpdater();
        break;
      case "HIVE":
        useHiveConfigUpdater();
        break;
      case "YARN":
        useYarnConfigUpdater();
        break;
      case "SQOOP":
        useSqoopConfigUpdater();
        break;
      case "KYUUBI":
        useKyuubiConfigUpdater();
        break;
      case "TRINO_GATEWAY":
        useTrinoGatewayConfigUpdater();
        break;
      default:
        // Handle unknown service names if necessary
        break;
    }
  });
  return <></>;
}

export default Updater;
