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

// import Step0 from "./Step0";
// import Step1 from "./Step1";
import Step2 from "./Step2";
import Step3 from "./Step3";
// import Step4 from "./Step4";
// import Step5 from "./Step5";
// import Step6 from "./Step6";
// import Step7 from "./Step7";
// import Step8 from "./Step8"
// import Step9 from "./Step9";
// import Step10 from "./Step10";

function ComponentInProgress() {
  return <h1>Component In Progress</h1>;
}

export default {
  0: {
    label: "Get Started",
    completed: false,
    Component: <ComponentInProgress />,
    canGoBack: false,
    isNextEnabled: false,
    name: "NAME",
    keysToRemove: [
      "VERSION",
      "HOSTS",
      "HOST_STATUS",
      "SERVICES",
      "MASTERS",
      "SLAVES_AND_CLIENTS",
      "CONFIGURATION",
      "REVIEW",
      "INSTALL_START_TEST",
    ],
  },
  1: {
    label: "Select Version",
    completed: false,
    Component: <ComponentInProgress />,
    canGoBack: true,
    isNextEnabled: false,
    name: "VERSION",
    keysToRemove: [
      "HOSTS",
      "HOST_STATUS",
      "SERVICES",
      "MASTERS",
      "SLAVES_AND_CLIENTS",
      "CONFIGURATION",
      "REVIEW",
      "INSTALL_START_TEST",
    ],
  },
  2: {
    label: "Install Options",
    completed: false,
    Component: <Step2 />,
    canGoBack: true,
    isNextEnabled: false,
    nextLabel: "REGISTER AND CONFIRM",
    name: "HOSTS",
    keysToRemove: [
      "HOST_STATUS",
      "SERVICES",
      "MASTERS",
      "SLAVES_AND_CLIENTS",
      "CONFIGURATION",
      "REVIEW",
      "INSTALL_START_TEST",
    ],
  },
  3: {
    label: "Confirm Hosts",
    completed: false,
    Component: <Step3 />,
    canGoBack: true,
    isNextEnabled: true,
    onNext: () => {
      return new Promise((resolve) => resolve("Something"));
    },
    name: "HOST_STATUS",
    keysToRemove: [
      "SERVICES",
      "MASTERS",
      "SLAVES_AND_CLIENTS",
      "CONFIGURATION",
      "REVIEW",
      "INSTALL_START_TEST",
    ],
  },
  4: {
    label: "Choose Services",
    completed: false,
    Component: <ComponentInProgress />,
    canGoBack: true,
    isNextEnabled: true,
    onNext: () => {
      return new Promise((resolve) => resolve("Something"));
    },
    name: "SERVICES",
    keysToRemove: [
      "MASTERS",
      "SLAVES_AND_CLIENTS",
      "CONFIGURATION",
      "REVIEW",
      "INSTALL_START_TEST",
    ],
  },
  5: {
    label: "Assign Masters",
    completed: false,
    Component: <ComponentInProgress />,
    canGoBack: true,
    isNextEnabled: true,
    onNext: () => {
      return new Promise((resolve) => resolve("Something"));
    },
    name: "MASTERS",
    keysToRemove: [
      "SLAVES_AND_CLIENTS",
      "CONFIGURATION",
      "REVIEW",
      "INSTALL_START_TEST",
    ],
  },
  6: {
    label: "Assign Slaves and Clients",
    completed: false,
    Component: <ComponentInProgress />,
    canGoBack: true,
    isNextEnabled: false,
    name: "SLAVES_AND_CLIENTS",
    keysToRemove: ["CONFIGURATION", "REVIEW", "INSTALL_START_TEST"],
  },
  7: {
    label: "Customize Services",
    completed: false,
    Component: <ComponentInProgress />,
    canGoBack: true,
    isNextEnabled: false,
    name: "CONFIGURATION",
    keysToRemove: ["REVIEW", "INSTALL_START_TEST"],
  },
  8: {
    label: "Review",
    completed: false,
    Component: <ComponentInProgress />,
    canGoBack: true,
    nextLabel: "DEPLOY",
    isNextEnabled: false,
    name: "REVIEW",
    keysToRemove: ["INSTALL_START_TEST"],
  },
  9: {
    label: "Install, Start and Test",
    completed: false,
    Component: <ComponentInProgress />,
    canGoBack: false,
    isNextEnabled: false,
    name: "INSTALL_START_TEST",
    keysToRemove: [],
  },
  10: {
    label: "Summary",
    completed: false,
    Component: <ComponentInProgress />,
    canGoBack: false,
    isNextEnabled: false,
    keyToRemove: [],
  },
};
