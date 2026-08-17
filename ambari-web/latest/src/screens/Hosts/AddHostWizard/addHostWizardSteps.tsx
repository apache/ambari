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

import Step3 from "../../ClusterWizard/Step3";
import AddHostConfigurations from "./AddHostConfigurations";
import AddHostInstall from "./AddHostInstall";
import AddHostReview from "./AddHostReview";
import AddHostSummary from "./AddHostSummary";
import Step2Wrapper from "./Step2Wrapper";
import Step6 from "../../ClusterWizard/Step6";
import { translate } from "../../../Utils/Utility";

export default {
  1: {
    label: translate("installer.step2.header"),
    completed: false,
    Component: <Step2Wrapper />,
    canGoBack: false,
    isNextEnabled: false,
    nextLabel: String(translate("installer.step2.registerAndConfirm")).toUpperCase(),
    name: "HOSTS",
    keysToRemove: [
      "HOST_STATUS",
      "SLAVES_AND_CLIENTS",
      "CONFIGURATIONS",
      "REVIEW",
      "INSTALL_START_TEST",
    ],
  },
  2: {
    label: translate("installer.step3.header"),
    completed: false,
    Component: <Step3 wizardName="addHost" />,
    canGoBack: true,
    isNextEnabled: true,
    onNext: () => {
      return new Promise((resolve) => resolve("Something"));
    },
    name: "HOST_STATUS",
    keysToRemove: [
      "SLAVES_AND_CLIENTS",
      "CONFIGURATIONS",
      "REVIEW",
      "INSTALL_START_TEST",
    ],
  },
  3: {
    label: translate("installer.step6.header"),
    completed: false,
    Component: <Step6 wizardName="addHost" />,
    canGoBack: true,
    isNextEnabled: false,
    name: "SLAVES_AND_CLIENTS",
    keysToRemove: ["CONFIGURATIONS", "REVIEW", "INSTALL_START_TEST"],
  },
  4: {
    label: translate("addHost.step4.header"),
    completed: false,
    Component: <AddHostConfigurations />,
    canGoBack: true,
    isNextEnabled: false,
    nextLabel: String(translate("common.next")).toUpperCase(),
    name: "CONFIGURATIONS",
    keysToRemove: ["REVIEW", "INSTALL_START_TEST"],
  },
  5: {
    label: translate("common.review"),
    completed: false,
    Component: <AddHostReview />,
    canGoBack: true,
    nextLabel: String(translate("common.deploy")).toUpperCase(),
    isNextEnabled: false,
    name: "REVIEW",
    keysToRemove: ["INSTALL_START_TEST"],
  },
  6: {
    label: translate("installer.step9.header"),
    completed: false,
    Component: <AddHostInstall />,
    canGoBack: false,
    isNextEnabled: false,
    name: "INSTALL_START_TEST",
    keyToRemove: [],
  },
  7: {
    label: translate("installer.step10.header"),
    completed: false,
    Component: <AddHostSummary />,
    canGoBack: false,
    isNextEnabled: false,
    keyToRemove: [],
  },
};
