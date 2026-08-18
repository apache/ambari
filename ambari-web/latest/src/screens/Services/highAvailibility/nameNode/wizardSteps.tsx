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
import Step5 from "./Step5";
import Step6 from "./Step6";
import Step7 from "./Step7";
import Step8 from "./Step8";
import Step9 from "./Step9";

export enum enableNamenodeSteps {
  GET_STARTED = "GET_STARTED",
  SELECT_HOSTS = "SELECT_HOSTS",
  REVIEW = "REVIEW",
  CREATE_CHECKPOINT = "CREATE_CHECKPOINT",
  CONFIGURE_COMPONENTS = "CONFIGURE_COMPONENTS",
  INITIALIZE_JOUNRALNODES = "INITIALIZE_JOURNALNODES",
  START_COMPONENTS = "START_COMPONENTS",
  INITIALIZE_METADATA = "INITIALIZE_METADATA",
  FINALIZE = "FINALIZE",
}

export default {
  0: {
    label: "Get Started",
    completed: false,
    Component: <Step1 />,
    canGoBack: false,
    isNextEnabled: false,
    name: enableNamenodeSteps.GET_STARTED,
    keysToRemove: [
      enableNamenodeSteps.SELECT_HOSTS,
      enableNamenodeSteps.REVIEW,
      enableNamenodeSteps.CREATE_CHECKPOINT,
      enableNamenodeSteps.CONFIGURE_COMPONENTS,
      enableNamenodeSteps.INITIALIZE_JOUNRALNODES,
      enableNamenodeSteps.START_COMPONENTS,
      enableNamenodeSteps.INITIALIZE_METADATA,
      enableNamenodeSteps.FINALIZE,
    ],
  },
  1: {
    label: "Select Hosts",
    completed: false,
    Component: <Step2 />,
    canGoBack: true,
    isNextEnabled: false,
    name: enableNamenodeSteps.SELECT_HOSTS,
    keysToRemove: [
      enableNamenodeSteps.REVIEW,
      enableNamenodeSteps.CREATE_CHECKPOINT,
      enableNamenodeSteps.CONFIGURE_COMPONENTS,
      enableNamenodeSteps.INITIALIZE_JOUNRALNODES,
      enableNamenodeSteps.START_COMPONENTS,
      enableNamenodeSteps.INITIALIZE_METADATA,
      enableNamenodeSteps.FINALIZE,
    ],
  },
  2: {
    label: "Review",
    completed: false,
    Component: <Step3 />,
    canGoBack: true,
    isNextEnabled: false,
    name: enableNamenodeSteps.REVIEW,
    keysToRemove: [
      enableNamenodeSteps.CREATE_CHECKPOINT,
      enableNamenodeSteps.CONFIGURE_COMPONENTS,
      enableNamenodeSteps.INITIALIZE_JOUNRALNODES,
      enableNamenodeSteps.START_COMPONENTS,
      enableNamenodeSteps.INITIALIZE_METADATA,
      enableNamenodeSteps.FINALIZE,
    ],
  },
  3: {
    label: "Create Checkpoint",
    completed: false,
    Component: <Step4 />,
    canGoBack: false,
    isNextEnabled: true,
    name: enableNamenodeSteps.CREATE_CHECKPOINT,
    keysToRemove: [
      enableNamenodeSteps.CONFIGURE_COMPONENTS,
      enableNamenodeSteps.INITIALIZE_JOUNRALNODES,
      enableNamenodeSteps.START_COMPONENTS,
      enableNamenodeSteps.INITIALIZE_METADATA,
      enableNamenodeSteps.FINALIZE,
    ],
  },
  4: {
    label: "Configure Components",
    completed: false,
    Component: <Step5 />,
    canGoBack: false,
    isNextEnabled: true,
    name: enableNamenodeSteps.CONFIGURE_COMPONENTS,
    keysToRemove: [
      enableNamenodeSteps.INITIALIZE_JOUNRALNODES,
      enableNamenodeSteps.START_COMPONENTS,
      enableNamenodeSteps.INITIALIZE_METADATA,
      enableNamenodeSteps.FINALIZE,
    ],
  },
  5: {
    label: "Initialize JournalNodes",
    completed: false,
    Component: <Step6 />,
    canGoBack: false,
    isNextEnabled: true,
    name: enableNamenodeSteps.INITIALIZE_JOUNRALNODES,
    keysToRemove: [
      enableNamenodeSteps.START_COMPONENTS,
      enableNamenodeSteps.INITIALIZE_METADATA,
      enableNamenodeSteps.FINALIZE,
    ],
  },
  6: {
    label: "Start Components",
    completed: false,
    Component: <Step7 />,
    canGoBack: false,
    isNextEnabled: false,
    name: enableNamenodeSteps.START_COMPONENTS,
    keysToRemove: [
      enableNamenodeSteps.INITIALIZE_METADATA,
      enableNamenodeSteps.FINALIZE,
    ],
  },
  7: {
    label: "Initilize Metadata",
    completed: false,
    Component: <Step8 />,
    canGoBack: false,
    isNextEnabled: false,
    name: enableNamenodeSteps.INITIALIZE_METADATA,
    keysToRemove: [enableNamenodeSteps.FINALIZE],
  },
  8: {
    label: "Finalize HA Setup",
    completed: false,
    Component: <Step9 />,
    canGoBack: false,
    isNextEnabled: false,
    name: enableNamenodeSteps.FINALIZE,
  },
};
