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


import { reassignSteps } from "./constants";
import Step1 from "./Step1";
import Step2 from "./Step2";
import Step3 from "./Step3";
import Step4 from "./Step4";
import Step5 from "./Step5";
import Step6 from "./Step6";


export default {
  1: {
    label: "Get Started",
    completed: false,
    Component: <Step1 />,
    canGoBack: false,
    isNextEnabled: false,
    name: reassignSteps.GET_STARTED,
  },
  2: {
    label: "Assign Master",
    completed: false,
    Component: <Step2 />,
    canGoBack: true,
    isNextEnabled: false,
    name: reassignSteps.ASSIGN_MASTER,
  },
  3: {
    label: "Review",
    completed: false,
    Component: <Step3 />,
    canGoBack: true,
    isNextEnabled: false,
    name: reassignSteps.REVIEW,
    nextLabel: "DEPLOY"
  },
  4: {
    label: "Configure Component",
    completed: false,
    Component: <Step4 />,
    canGoBack: false,
    isNextEnabled: true,
    name: reassignSteps.CONFIGURE_COMPONENT,
    nextLabel: "Complete"
  },
  5: {
    label: "Manual Commands",
    completed: false,
    Component: <Step5 />,
    canGoBack: true,
    isNextEnabled: false,
    name: reassignSteps.MANUAL_COMMANDS,
    nextLabel: "Next"
  },
  6: {
    label: "Start and Test Services",
    completed: false,
    Component: <Step6 />,
    canGoBack: true,
    isNextEnabled: false,
    name: reassignSteps.START_AND_TEST_SERVICES,
    nextLabel: "Next"
  },
  7: {
    label: "Finalize Move",
    completed: false,
    Component: <></>,
    canGoBack: true,
    isNextEnabled: false,
    name: reassignSteps.FINALIZE_MOVE,
    nextLabel: "Complete"
  }
};
