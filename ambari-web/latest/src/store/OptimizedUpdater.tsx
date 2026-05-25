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

import { useHDFSConfigUpdater } from "../hooks/useHDFSConfigUpdater";
import { useZkConfigUpdater } from "../hooks/useZkConfigUpdater";
import { useHbaseConfigUpdater } from "../hooks/useHbaseConfigUpdater";
import { useRangerConfigUpdater } from "../hooks/useRangerConfigUpdater";
import { useMapReduce2ConfigUpdater } from "../hooks/useMapReduce2ConfigUpdater";
import { useTezConfigUpdater } from "../hooks/useTezConfigUpdater";
import { useSpark3ConfigUpdater } from "../hooks/useSpark3ConfigUpdater";
import { useKerberosConfigUpdater } from "../hooks/useKerberosConfigUpdater";
import { useRangerKMSConfigUpdater } from "../hooks/useRangerKMSConfigUpdater";
import { useAmbariMetricsConfigUpdater } from "../hooks/useAmbariMetricsConfigUpdater";
import { useTrinoConfigUpdater } from "../hooks/useTrinoConfigUpdater.tsx";
import { useSSMConfigUpdater } from "../hooks/useSSMConfigUpdater";
import { useYarnConfigUpdater } from "../hooks/useYarnConfigUpdater";
import { useHiveConfigUpdater } from "../hooks/useHiveConfigUpdater";
import { useKyuubiConfigUpdater } from "../hooks/useKyuubiConfigUpdater";
import { useSqoopConfigUpdater } from "../hooks/useSqoopConfigUpdater";
import { useTrinoGatewayConfigUpdater } from "../hooks/useTrinoGatewayConfigUpdater";
import { usePinotConfigUpdater } from "../hooks/usePinotConfigUpdater";

function OptimizedUpdater() {

  // Call all hooks unconditionally - this fixes the Rules of Hooks violation
  // The hooks will internally check if their service is installed and act accordingly
  // This maintains the same functionality but follows React's Rules of Hooks

  useHDFSConfigUpdater();
  useHbaseConfigUpdater();
  useRangerConfigUpdater();
  useZkConfigUpdater();
  useMapReduce2ConfigUpdater();
  useTezConfigUpdater();
  useSpark3ConfigUpdater();
  useKerberosConfigUpdater();
  useRangerKMSConfigUpdater();
  useAmbariMetricsConfigUpdater();
  useTrinoConfigUpdater();
  useSSMConfigUpdater();
  useHiveConfigUpdater();
  useYarnConfigUpdater();
  useSqoopConfigUpdater();
  useKyuubiConfigUpdater();
  useTrinoGatewayConfigUpdater();
  usePinotConfigUpdater();

  return <></>;
}

export default OptimizedUpdater;
