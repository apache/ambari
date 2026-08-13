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

import { useEffect, useState } from "react";
import { Navigate, useLocation } from "react-router-dom";
import Spinner from "../../components/Spinner";
import { ServiceApi } from "../../api/serviceApi";

type ServerVersionResponse = {
  components?: Array<{
    RootServiceComponents?: { component_version?: string };
  }>;
};

function latestServerVersion(data: ServerVersionResponse): string | null {
  const versions = (data?.components || [])
    .map((item) => item.RootServiceComponents?.component_version)
    .filter((value): value is string => Boolean(value))
    .map((value) => value.replace(/[^\d.-]/g, ""))
    .sort();
  return versions.at(-1) || null;
}

export default function AdminViewRedirect() {
  const [failed, setFailed] = useState(false);
  const location = useLocation();

  useEffect(() => {
    async function redirect() {
      try {
        const data = await ServiceApi.getAmbariServerVersion();
        const version = latestServerVersion(data);
        if (!version) {
          throw new Error("Ambari Server version is unavailable");
        }
        const page = new URLSearchParams(location.search).get("page");
        const adminHash = page ? `#/${encodeURIComponent(page)}` : "#/";
        window.location.assign(
          `/views/ADMIN_VIEW/${encodeURIComponent(version)}/INSTANCE/${adminHash}`,
        );
      } catch {
        setFailed(true);
      }
    }
    void redirect();
  }, [location.search]);

  return failed ? <Navigate to="/main/view" replace /> : <Spinner />;
}
