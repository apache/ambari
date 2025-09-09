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

import { get, startCase } from "lodash";

const components = {
  API: "API",
  DECOMMISSION_DATANODE: "Update Exclude File",
  DRPC: "DRPC",
  FLUME_HANDLER: "Flume",
  GLUSTERFS: "GLUSTERFS",
  HBASE: "HBase",
  HBASE_REGIONSERVER: "RegionServer",
  HCAT: "HCat Client",
  HDFS: "HDFS",
  HISTORYSERVER: "History Server",
  HIVE_SERVER: "HiveServer2",
  JCE: "JCE",
  MAPREDUCE2: "MapReduce2",
  MYSQL: "MySQL",
  REST: "REST",
  SECONDARY_NAMENODE: "SNameNode",
  STORM_REST_API: "Storm REST API Server",
  WEBHCAT: "WebHCat",
  YARN: "YARN",
  UI: "UI",
  ZKFC: "ZKFailoverController",
  ZOOKEEPER: "ZooKeeper",
  ZOOKEEPER_QUORUM_SERVICE_CHECK: "ZK Quorum Service Check",
  HAWQ: "HAWQ",
  PXF: "PXF",
};

export const normalizeName = (name: string) => {
  if (!name || typeof name !== "string") return "";
  if (get(components, name, "")) return get(components, name, "");
  name = name.toLowerCase();
  const suffixNoSpaces = ["node", "tracker", "manager"];
  const suffixRegExp = new RegExp(`(\\w+)(${suffixNoSpaces.join("|")})`, "gi");
  if (/_/g.test(name)) {
    name = name
      .split("_")
      .map((singleName) => normalizeName(singleName.toUpperCase()))
      .join(" ");
  } else if (suffixRegExp.test(name)) {
    suffixRegExp.lastIndex = 0;
    const matches = suffixRegExp.exec(name);
    if (matches) {
      name =
        startCase(matches[1].toLowerCase()) +
        startCase(matches[2].toLowerCase());
    }
  }
  return startCase(name.toLowerCase());
};

export const normalizeNameBySeparators = (
  name: string,
  separators: string[]
) => {
  if (!name || typeof name !== "string") return "";
  name = name.toLowerCase();
  if (!separators || separators.length === 0) {
    separators = ["_"];
  }

  for (const separator of separators) {
    if (new RegExp(separator, "g").test(name)) {
      name = name
        .split(separator)
        .map((singleName) => {
          return normalizeName(singleName.toUpperCase());
        })
        .join(" ");
      break;
    }
  }
  return startCase(name.toLowerCase());
};

export const sortPropertyLight = (data: any[], path: any) => {
  const realPath = typeof path === "string" ? path.split('.') : [];
  return data.sort((a, b) => {
    let aProperty: any = a;
    let bProperty: any = b;
    realPath.forEach((key) => {
      aProperty = aProperty[key];
      bProperty = bProperty[key];
    });
    if (aProperty > bProperty) return 1;
    if (aProperty < bProperty) return -1;
    return 0;
  });
}