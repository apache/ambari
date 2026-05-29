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

import { useParams } from "react-router-dom";
import ServiceDashboard from "./ServiceDashboard";

function ServiceLoader(){
    const {componentName}=useParams();
    function mapEnableFlow(){
        switch(componentName?.toLowerCase()){
            case "namenode":
            case "snamenode":
            case "secondary_namenode":
                return <ServiceDashboard serviceName="HDFS" />
            case "resourcemanager":
            case "historyserver":
                return <ServiceDashboard serviceName="YARN" />
            case "JournalNode":
            case "journalnode":
                return <ServiceDashboard serviceName="HDFS" />
            case "RangerAdmin":
            case "rangeradmin":
                return <ServiceDashboard serviceName="RANGER" />
            case "ResourceManager":
            case "resourcemanager":
                 return <ServiceDashboard serviceName="YARN" />
                return <ServiceDashboard serviceName="YARN" />
            case "app_timeline_server":
                return <ServiceDashboard serviceName="YARN" />
            case "YARN_REGISTRY_DNS":
            case "yarn_registry_dns":
                return <ServiceDashboard serviceName="YARN" />
            case "metrics_collector":
            case "METRICS_COLLECTOR":
                return <ServiceDashboard serviceName="AMBARI_METRICS" />
            case "ssm_server":
                return <ServiceDashboard serviceName="SSM" />
            case "hive_server":
                return <ServiceDashboard serviceName="HIVE" />
            case "hive_metastore":
                return <ServiceDashboard serviceName="HIVE" />
            case "historyserver":
                return <ServiceDashboard serviceName="YARN" />    

      default:
        return <div>Service Dashboard Not Available</div>;
    }
  }

  return <>{mapEnableFlow()}</>;
}

export default ServiceLoader;
