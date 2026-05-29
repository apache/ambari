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

import { get } from "lodash";
import Step10 from "../../ClusterWizard/Step10";
import Step8 from "../../ClusterWizard/Step8";
import Step9 from "../../ClusterWizard/Step9";
import { messages } from "../../messages";
import Step6 from "../../ClusterWizard/Step6";
import Step4 from "../../ClusterWizard/Step4";
import Step5 from "../../ClusterWizard/Step5";
import Step7 from "../../ClusterWizard/Step7";

export default {
  1: {
    label: get(messages, "installer.step4.header"),
    completed: false,
    Component: <Step4 wizardName="addService" />,
    canGoBack: false,
    isNextEnabled: false,
    name: "SERVICES",
    keysToRemove: [
      "MASTERS",
      "SLAVES_AND_CLIENTS",
      "CONFIGURATION",
      "REVIEW",
      "INSTALL_START_TEST",
    ],
  },
  2: {
    label: get(messages, "installer.step5.header"),
    completed: false,
    Component: <Step5 wizardName="addService" />,
    canGoBack: true,
    isNextEnabled: true,
    name: "MASTERS",
    keysToRemove: [
      "SLAVES_AND_CLIENTS",
      "CONFIGURATION",
      "REVIEW",
      "INSTALL_START_TEST",
    ],
  },
  3: {
    label: get(messages, "installer.step6.header"),
    completed: false,
    Component: <Step6 wizardName="addService" />,
    canGoBack: true,
    isNextEnabled: false,
    name: "SLAVES_AND_CLIENTS",
    keysToRemove: ["CONFIGURATION", "REVIEW", "INSTALL_START_TEST"],
  },
  4: {
    label: "Configurations",
    completed: false,
    Component: <Step7 wizardName="addService" />,
    canGoBack: true,
    isNextEnabled: false,
    nextLabel: get(messages, "common.next").toUpperCase(),
    name: "CONFIGURATION",
    keysToRemove: ["REVIEW", "INSTALL_START_TEST"],
  },
  5: {
    label: get(messages, "common.review"),
    completed: false,
    Component: <Step8 wizardName="addService" />,
    canGoBack: true,
    nextLabel: get(messages, "common.deploy").toUpperCase(),
    isNextEnabled: false,
    name: "REVIEW",
    keysToRemove: ["INSTALL_START_TEST"],
  },
  6: {
    label: get(messages, "installer.step9.header"),
    completed: false,
    Component: <Step9 wizardName="addService" />,
    canGoBack: false,
    isNextEnabled: false,
    name: "INSTALL_START_TEST",
  },
  7: {
    label: get(messages, "installer.step10.header"),
    completed: false,
    Component: <Step10 wizardName="addService" />,
    canGoBack: false,
    isNextEnabled: false,
  },
};
