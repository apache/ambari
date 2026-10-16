/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import { useEffect, useState } from "react";
import { useSearchParams } from "react-router-dom";
import MpackApi, { mpackErrorMessage, type MpackDeploymentHandoff } from "../api/mpackApi";

export default function useMpackDeployment() {
  const [parameters] = useSearchParams();
  const id = parameters.get("mpack_operation");
  const [result, setResult] = useState<{ id: string | null; value: MpackDeploymentHandoff | null; error: string }>({
    id: null, value: null, error: "",
  });
  useEffect(() => {
    let current = true;
    if (!id) { setResult({ id: null, value: null, error: "" }); return; }
    MpackApi.deployment(id).then(value => {
      if (current) setResult({ id, value, error: "" });
    }).catch(error => {
      if (current) setResult({ id, value: null, error: mpackErrorMessage(error) });
    });
    return () => { current = false; };
  }, [id]);
  return { deployment: result.id === id ? result.value?.deployment ?? null : null,
    error: result.id === id ? result.error : "", loading: !!id && result.id !== id };
}
