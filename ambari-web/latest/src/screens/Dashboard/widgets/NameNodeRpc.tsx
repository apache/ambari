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

import { useContext } from "react";
import { ServiceContext } from "../../../store/ServiceContext";
import ChartContainer from "../ChartContainer";
import { isEmpty } from "lodash";

export default function NameNodeRpc() {
  const { allServiceModels } = useContext(ServiceContext);

  const modelValue = isEmpty(allServiceModels["hdfs"]?.["nameNodeRpcValues"]) ? 0 : allServiceModels["hdfs"]?.["nameNodeRpcValues"];

  const getData = () => {
    if (modelValue === 0) {
      return "0";
    }
    if (typeof modelValue === "number") {
      return modelValue.toFixed(2);
    }
    return null;
  };

  const data = getData();
  const content = data !== null ? `${data} ms` : "n/a";
  const hoverContent = data !== null ? `${data} ms average RPC \n queue wait time` : "n/a";

  return <ChartContainer text={content} onHoverContent={hoverContent}>
    <div className="p-3"></div>
  </ChartContainer>;
}
