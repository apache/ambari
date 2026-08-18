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

import ConfigureIdentities from "../Kerberos/ConfigureIdentities";
import ConfigureKerberos from "./ConfigureKerberos";
import ConfirmConfiguration from "./ConfirmConfiguration";
import GetStartedKerberos from "./GetStarted";
import KerberizeCluster from "./KerberizeCluster";
import StartAndTestKerberosClient from "./StartAndTestKerberosClient";
import StartAndTestServices from "./StartAndTestServices";
import StopServices from "./StopServices";

export default {
    1: {
      label: "Get Started",
      completed: false,
      Component: <GetStartedKerberos />,
      canGoBack: false,
      isNextEnabled: false,
      name: "GET_STARTED",
      keysToRemove: ["CONFIGURE_KERBEROS", "CONFIGURE_IDENTITIES", "INSTALL_AND_TEST_KERBEROS_CLIENT", "CONFIRM_CONFIGURATION", "STOP_SERVICES", "KERBERIZE_CLUSTER", "START_AND_TEST_SERVICES"]
    },
    2: {
      label: "Configure Kerberos",
      completed: false,
      Component: <ConfigureKerberos />,
      canGoBack: true,
      isNextEnabled: false,
      name: "CONFIGURE_KERBEROS",
      keysToRemove: ["CONFIGURE_IDENTITIES", "INSTALL_AND_TEST_KERBEROS_CLIENT", "CONFIRM_CONFIGURATION", "STOP_SERVICES", "KERBERIZE_CLUSTER", "START_AND_TEST_SERVICES"]
    },
    3: {
      label: "Install and Test Kerberos Client",
      completed: false,
      Component: <StartAndTestKerberosClient />,
      canGoBack: true,
      isNextEnabled: false,
      name: "INSTALL_AND_TEST_KERBEROS_CLIENT",
      keysToRemove: ["CONFIGURE_IDENTITIES", "CONFIRM_CONFIGURATION", "STOP_SERVICES", "KERBERIZE_CLUSTER", "START_AND_TEST_SERVICES"]
    },
    4: {
      label: "Configure Identities",
      completed: false,
      Component: <ConfigureIdentities />,
      canGoBack: true,
      isNextEnabled: true,
      name: "CONFIGURE_IDENTITIES",
      keysToRemove: ["CONFIRM_CONFIGURATION", "STOP_SERVICES", "KERBERIZE_CLUSTER", "START_AND_TEST_SERVICES"]
    },
    5: {
      label: "Confirm Configuration",
      completed: false,
      Component: <ConfirmConfiguration />,
      canGoBack: true,
      isNextEnabled: true,
      name: "CONFIRM_CONFIGURATION",
      keysToRemove: ["STOP_SERVICES", "KERBERIZE_CLUSTER", "START_AND_TEST_SERVICES"]
    },
    6: {
      label: "Stop Services",
      completed: false,
      Component: <StopServices />,
      canGoBack: true,
      isNextEnabled: true,
      name: "STOP_SERVICES",
      keysToRemove: ["KERBERIZE_CLUSTER", "START_AND_TEST_SERVICES"]
    },
    7: {
      label: "Kerberize cluster",
      completed: false,
      Component: <KerberizeCluster />,
      canGoBack: true,
      isNextEnabled: false,
      name: "KERBERIZE_CLUSTER",
      keysToRemove: ["START_AND_TEST_SERVICES"]
    },
    8: {
      label: "Start and Test Services",
      completed: false,
      Component: <StartAndTestServices />,
      canGoBack: false,
      isNextEnabled: false,
      name: "START_AND_TEST_SERVICES"
    },
  };
