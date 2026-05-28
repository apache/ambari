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

import Step1 from "./Step1";
import Step2 from "./Step2";
import Step3 from "./Step3";
import Step4 from "./Step4";

export enum enableRangerAdminSteps {
  GET_STARTED = "GET_STARTED",
  SELECT_HOSTS = "SELECT_HOSTS",
  REVIEW = "REVIEW",
  INSTALL_START_TEST="INSTALL_START_TEST",
}

export default {
  1: {
    label: "Get Started",
    completed: false,
    Component: <Step1 />,
    canGoBack: false,
    isNextEnabled: false,
    name: enableRangerAdminSteps.GET_STARTED,
    keysToRemove: [enableRangerAdminSteps.SELECT_HOSTS, enableRangerAdminSteps.REVIEW, enableRangerAdminSteps.INSTALL_START_TEST],
  },
  2: {
    label: "Select Hosts",
    completed: false,
    Component: <Step2/>,
    canGoBack: true,
    isNextEnabled: false,
    name: enableRangerAdminSteps.SELECT_HOSTS,
    keysToRemove: [enableRangerAdminSteps.REVIEW, enableRangerAdminSteps.INSTALL_START_TEST],
  },
  3: {
    label: "Review",
    completed: false,
    Component: <Step3/>,
    canGoBack: true,
    isNextEnabled: false,
    name: enableRangerAdminSteps.REVIEW,
    keysToRemove: [enableRangerAdminSteps.INSTALL_START_TEST],
  },
  4: {
    label: "Install, Start and Test",
    completed: false,
    Component: <Step4/>,
    canGoBack: false,
    isNextEnabled: true,
    name: enableRangerAdminSteps.INSTALL_START_TEST,
    nextLabel:"Complete"
  },
};
