/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to You under the Apache License, Version 2.0 (the
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
import { errorMessage, readResource } from "../api/clusterManagement";

export default function useAdminResource<T>(path: string | null, collection = false) {
  const [revision, setRevision] = useState(0);
  const [state, setState] = useState<{ path: string | null; data?: T; error?: string; loading: boolean }>({ path: null, loading: false });
  useEffect(() => {
    if (!path) return;
    const controller = new AbortController();
    setState({ path, loading: true });
    readResource<T>(path, controller.signal).then((data) => {
      if (collection && !Array.isArray((data as { items?: unknown }).items)) throw new Error("Invalid resource collection");
      if (!controller.signal.aborted) setState({ path, data, loading: false });
    }).catch((error) => {
      if (!controller.signal.aborted) setState({ path, error: errorMessage(error), loading: false });
    });
    return () => controller.abort();
  }, [path, revision, collection]);
  return { ...(state.path === path ? state : { loading: Boolean(path), data: undefined, error: undefined }),
    reload: () => setRevision((value) => value + 1) };
}
