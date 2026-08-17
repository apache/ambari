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

import { useContext } from "react";
import Step2 from "../../ClusterWizard/Step2";
import { AddHostContext } from "./wizardDataStore/context";
import { AppContext } from "../../../store/context";

export default function Step2Wrapper() {
  const { installedHosts } = useContext(AddHostContext);
  const { cluster, supports } = useContext(AppContext);
  const stackName = cluster?.stack || String(cluster?.version || "").split("-")[0];

  return (
    <Step2
      wizardName="addHost"
      installedHosts={installedHosts}
      customizeAgentUserAccount={Boolean(supports.customizeAgentUserAccount)}
      isWindowsStack={stackName === "HDPWIN"}
    />
  );
}
