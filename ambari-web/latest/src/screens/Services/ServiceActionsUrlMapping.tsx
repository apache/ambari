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

import { matchPath, useLocation, useParams } from "react-router-dom";
import EnableHighAvailibilityNameNode from "./highAvailibility/nameNode";
import ManageJournalNodes from "./highAvailibility/journalNode";
import EnableNamenodeFederation from "./highAvailibility/Federation";
import AddObserverNamenode from "./highAvailibility/observerNameNode";
import EnableHighAvailibilityRangerAdmin from "./highAvailibility/rangerAdmin";
import EnableHighAvailibilityResourceManger from "./highAvailibility/resourceManager";
import ReassignComponent from "./reassign";
import RouterFederationWizard from "./highAvailibility/RouterFederation";
import HawqStandbyWizard from "./highAvailibility/HawqStandby";
import { HawqStandbyMode } from "./highAvailibility/HawqStandby/context";

const hawqModes = new Set<HawqStandbyMode>(["add", "remove", "activate"]);

function ServiceActionsUrlMapping({ serviceName }: { serviceName: string }) {
  const { componentName } = useParams();
  const location = useLocation();
  const hawqMatch = matchPath(
    "/main/services/highAvailability/:componentName/:mode/:stepNumber",
    location.pathname,
  );
  const hawqMode = hawqMatch?.params.mode as HawqStandbyMode | undefined;

  if (
    location.pathname.includes("routerBasedFederation") &&
    componentName === "NameNode"
  ) {
    return <RouterFederationWizard />;
  }
  if (location.pathname.includes("federation") && componentName === "NameNode") {
    return <EnableNamenodeFederation isMappingOnly />;
  }
  if (
    location.pathname.includes("observerNamenode") &&
    componentName === "NameNode"
  ) {
    return <AddObserverNamenode isMappingOnly />;
  }
  if (
    hawqMatch?.params.componentName?.toLowerCase() === "hawq" &&
    hawqMode &&
    hawqModes.has(hawqMode)
  ) {
    return <HawqStandbyWizard mode={hawqMode} />;
  }
  if (
    location.pathname.includes("highAvailability") &&
    componentName === "NameNode"
  ) {
    return <EnableHighAvailibilityNameNode isMappingOnly />;
  }
  if (
    location.pathname.includes("highAvailability") &&
    componentName === "JournalNode"
  ) {
    return <ManageJournalNodes isMappingOnly />;
  }
  if (location.pathname.includes("reassign")) {
    return <ReassignComponent serviceName={serviceName} isMappingOnly />;
  }
  if (
    location.pathname.includes("highAvailability") &&
    componentName === "RangerAdmin"
  ) {
    return <EnableHighAvailibilityRangerAdmin isMappingOnly />;
  }
  if (
    location.pathname.includes("highAvailability") &&
    componentName === "ResourceManager"
  ) {
    return <EnableHighAvailibilityResourceManger isMappingOnly />;
  }

  return null;
}
export default ServiceActionsUrlMapping;
