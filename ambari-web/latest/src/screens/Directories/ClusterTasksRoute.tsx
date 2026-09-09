/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { Navigate, useLocation, useNavigate, useParams } from "react-router-dom";
import BackgroundOperations from "../BackgroundOperations";
import { useAuth } from "../../hooks/useAuth";
import { clusterPath } from "../../Utils/clusterRoute";
import { safeDirectoryReturnPath } from "./directoryUtils";

export default function ClusterTasksRoute() {
  const navigate = useNavigate();
  const location = useLocation();
  const { clusterName = "" } = useParams();
  const { canViewClusterTasks } = useAuth();
  if (!clusterName) {
    return <Navigate to="/clusters" replace />;
  }
  if (!canViewClusterTasks(clusterName)) {
    return <Navigate to={clusterPath(clusterName, "/main/dashboard/metrics")} replace />;
  }
  const returnTo = safeDirectoryReturnPath((location.state as { returnTo?: unknown } | null)?.returnTo)
    || clusterPath(clusterName, "/main/dashboard/metrics");
  return (
    <BackgroundOperations
      clusterName={clusterName}
      isExplicitClick
      isOpen
      onClose={() => navigate(returnTo, { replace: true })}
    />
  );
}
