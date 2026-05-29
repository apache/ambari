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

import { useLocation, useParams } from "react-router-dom";
import EnableHighAvailibilityNameNode from "./highAvailibility/nameNode";
import ManageJournalNodes from "./highAvailibility/journalNode";
import EnableNamenodeFederation from "./highAvailibility/Federation";
import EnableHighAvailibilityRangerAdmin from "./highAvailibility/rangerAdmin";
import EnableHighAvailibilityResourceManger from "./highAvailibility/resourceManager";
import ReassignComponent from "./reassign";

function ServiceActionsUrlMapping({serviceName}: {serviceName: string}) {
    const {componentName}=useParams();
    const location=useLocation();
    function mapUrlToComponent(){
        if(location.pathname.includes("highAvailability")&&componentName==="NameNode"){
            return <EnableHighAvailibilityNameNode isMappingOnly/>
        }
        if(location.pathname.includes("federation") && componentName === "NameNode"){
            return <EnableNamenodeFederation isMappingOnly/>
        }
         if(location.pathname.includes("highAvailability")&&componentName==="JournalNode"){
            return <ManageJournalNodes isMappingOnly/>
        }
         if(location.pathname.includes("reassign")){
            return <ReassignComponent serviceName={serviceName} isMappingOnly/>
        }
        if(location.pathname.includes("highAvailability")&&componentName==="RangerAdmin"){
            return <EnableHighAvailibilityRangerAdmin isMappingOnly/>
        }
        if(location.pathname.includes("highAvailability")&&componentName==="ResourceManager"){
            return <EnableHighAvailibilityResourceManger isMappingOnly/>
        }
    }
    return mapUrlToComponent()
}
export default ServiceActionsUrlMapping;