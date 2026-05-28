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

import {
  faCircleCheck,
  faKitMedical,
  faWarning,
} from "@fortawesome/free-solid-svg-icons";

export type ServiceMetricMap = {
  display_name: string;
  modelKey: string;
  description: string;
  group_id?: number;
  descriptionModelKey?: string;
};
export type ServiceMetricsGroups = {
  id: number;
  display_name: string;
  metrics?: ServiceMetricMap[];
};

export enum Categories {
  MASTER = "MASTER",
  SLAVE = "SLAVE",
  CLIENT = "CLIENT",
}

export type ServiceComponentType = {
  display_name: string;
  modelKey: string;
  description: string;
  descriptionKey?: string;
  category: Categories;
  group_id: number;
};

export type ServiceComponentGroupType = {
  id: number;
  display_name: string;
  components?: ServiceComponentType[];
};

export type ServiceComponentCategoryType = {
  name: Categories;
  groups: ServiceComponentGroupType[];
};

export const statusIconMap: any = {
  started: {
    icon: faCircleCheck,
    color: "success",
  },
  starting: {
    icon: faCircleCheck,
    color: "success",
  },
  stopped: {
    icon: faWarning,
    color: "danger",
  },
  installed: {
    icon: faWarning,
    color: "danger",
  },
  Maintenance: {
    icon: faKitMedical,
  },
};

export const ServiceComponentsMap: { [key: string]: ServiceComponentType[] } = {
  hdfs: [
    {
      display_name: "Namenode uptime",
      modelKey: "namenodeUptime",
      description: "",
      descriptionKey: "",
      category: Categories.MASTER,
      group_id: 1,
    },
    {
      display_name: "Namenode Heap",
      modelKey: "percentNamenodeHeap",
      description: "",
      descriptionKey: "nnHeapUsed",
      category: Categories.MASTER,
      group_id: 1,
    },
    {
      display_name: "",
      description: "Live",
      category: Categories.SLAVE,
      group_id: 1,
      modelKey: "liveNodesDataNodes",
    },
    {
      display_name: "",
      description: "Dead",
      category: Categories.SLAVE,
      group_id: 1,
      modelKey: "deadNodesDataNodes",
    },
    {
      display_name: "",
      description: "Decommissioning",
      category: Categories.SLAVE,
      group_id: 1,
      modelKey: "decommissionedNodesDataNodes",
    },
  ],
  hbase: [
    {
      display_name: "REGIONS IN TRANSITION",
      modelKey: "regionsInTransition",
      description: "",
      descriptionKey: "",
      category: Categories.SLAVE,
      group_id: 1,
    },
  ],
  ranger: [
    {
      display_name: "RANGER HDFS PLUGIN",
      modelKey: "rangerHDFSPluginProperties",
      description: "",
      descriptionKey: "",
      category: Categories.MASTER,
      group_id: 1,
    },
    {
      display_name: "RANGER YARN PLUGIN",
      modelKey: "rangerYarnPluginProperties",
      description: "",
      descriptionKey: "",
      category: Categories.MASTER,
      group_id: 1,
    },
    {
      display_name: "RANGER HIVE PLUGIN",
      modelKey: "rangerHivePluginProperties",
      description: "",
      descriptionKey: "",
      category: Categories.MASTER,
      group_id: 1,
    },
    {
      display_name: "RANGER HBASE PLUGIN",
      modelKey: "rangerHbasePluginProperties",
      description: "",
      descriptionKey: "",
      category: Categories.MASTER,
      group_id: 1,
    },
  ],
  zk: [
    {
      display_name: "ZOOKEEPER CLIENTS",
      modelKey: "zkClientsInstalled",
      description: "",
      descriptionKey: "",
      category: Categories.MASTER,
      group_id: 1,
    },
  ],
  mapreduce2: [
    {
      display_name: "MAPREDUCE2 CLIENTS",
      modelKey: "mapReduce2Clients",
      description: "",
      descriptionKey: "",
      category: Categories.MASTER,
      group_id: 1,
    },
  ],
  tez: [
    {
      display_name: "TEZ CLIENTS",
      modelKey: "tezClientsInstalled",
      description: "",
      descriptionKey: "",
      category: Categories.MASTER,
      group_id: 1,
    },
  ],
  sqoop: [
    {
      display_name: "SQOOP CLIENTS",
      modelKey: "sqoopClientsInstalled",
      description: "",
      descriptionKey: "",
      category: Categories.MASTER,
      group_id: 1,
    },
  ],
  kerberos: [
    {
      display_name: "KERBEROS CLIENTS",
      modelKey: "kerberosClientsInstalled",
      description: "",
      descriptionKey: "",
      category: Categories.MASTER,
      group_id: 1,
    },
  ],
  spark3: [
    {
      display_name: "SPARK3 CLIENTS",
      modelKey: "spark3Clients",
      description: "",
      descriptionKey: "",
      category: Categories.MASTER,
      group_id: 1,
    },
  ],
  trino: [
    {
      display_name: "TRINO CLIS",
      modelKey: "trinoClients",
      description: "",
      descriptionKey: "",
      category: Categories.MASTER,
      group_id: 1,
    },
  ],
  yarn: [
    {
      display_name: "ResourceManager uptime",
      modelKey: "resourceManagerUptime",
      description: "",
      descriptionKey: "",
      category: Categories.MASTER,
      group_id: 2,
    },
    {
      display_name: "ResourceManager Heap",
      modelKey: "diskPartResourceManagerHeapMemory",
      description: "",
      descriptionKey: "",
      category: Categories.MASTER,
      group_id: 2,
    },
    {
      display_name: "",
      description: "Active",
      category: Categories.SLAVE,
      group_id: 1,
      modelKey: "nodeManagersCountActive",
    },
    {
      display_name: "",
      description: "Lost",
      category: Categories.SLAVE,
      group_id: 1,
      modelKey: "nodeManagersCountLost",
    },
    {
      display_name: "",
      description: "Unhealthy",
      category: Categories.SLAVE,
      group_id: 1,
      modelKey: "nodeManagersCountUnhealthy",
    },
    {
      display_name: "",
      description: "Rebooted",
      category: Categories.SLAVE,
      group_id: 1,
      modelKey: "nodeManagersCountRebooted",
    },
    {
      display_name: "",
      description: "Decommissioned",
      category: Categories.SLAVE,
      group_id: 1,
      modelKey: "nodeManagersCountDecommissioned",
    },
    {
      display_name: "YARN CLIENTS",
      modelKey: "yarnClients",
      description: "",
      descriptionKey: "",
      category: Categories.CLIENT,
      group_id: 1,
    },
  ],
  hive: [
    {
      display_name: "HIVE CLIENTS",
      modelKey: "hiveClients",
      description: "",
      descriptionKey: "",
      category: Categories.CLIENT,
      group_id: 1,
    },
  ],
};

export const componentCategories: {
  [key: string]: ServiceComponentCategoryType[];
} = {
  hdfs: [
    {
      name: Categories.MASTER,
      groups: [
        {
          id: 1,
          display_name: "",
        },
      ],
    },
    {
      name: Categories.SLAVE,
      groups: [
        {
          id: 1,
          display_name: "Datanodes Status",
        },
      ],
    },
  ],
  hbase: [
    {
      name: Categories.SLAVE,
      groups: [
        {
          id: 1,
          display_name: "",
        },
      ],
    },
  ],
  ranger: [
    {
      name: Categories.MASTER,
      groups: [
        {
          id: 1,
          display_name: "",
        },
      ],
    },
    {
      name: Categories.SLAVE,
      groups: [
        {
          id: 1,
          display_name: "",
        },
      ],
    },
  ],
  zk: [
    {
      name: Categories.MASTER,
      groups: [
        {
          id: 1,
          display_name: "",
        },
      ],
    },
  ],
  mapreduce2: [
    {
      name: Categories.MASTER,
      groups: [
        {
          id: 1,
          display_name: "",
        },
      ],
    },
  ],
  tez: [
    {
      name: Categories.MASTER,
      groups: [
        {
          id: 1,
          display_name: "",
        },
      ],
    },
  ],
  sqoop: [
    {
      name: Categories.MASTER,
      groups: [
        {
          id: 1,
          display_name: "",
        },
      ],
    },
  ],
  kerberos: [
    {
      name: Categories.MASTER,
      groups: [
        {
          id: 1,
          display_name: "",
        },
      ],
    },
  ],
  spark3: [
    {
      name: Categories.MASTER,
      groups: [
        {
          id: 1,
          display_name: "",
        },
      ],
    },
    {
      name: Categories.SLAVE,
      groups: [
        {
          id: 1,
          display_name: "",
        },
      ],
    },
  ],
  ambari_metrics: [
    {
      name: Categories.MASTER,
      groups: [
        {
          id: 1,
          display_name: "",
        },
      ],
    },
    {
      name: Categories.SLAVE,
      groups: [
        {
          id: 1,
          display_name: "",
        },
      ],
    },
  ],
  ranger_kms: [
    {
      name: Categories.MASTER,
      groups: [
        {
          id: 1,
          display_name: "",
        },
      ],
    },
  ],
  trino: [
    {
      name: Categories.MASTER,
      groups: [
        {
          id: 1,
          display_name: "",
        },
      ],
    },
    {
      name: Categories.SLAVE,
      groups: [
        {
          id: 1,
          display_name: "",
        },
      ],
    },
  ],
  ssm: [
    {
      name: Categories.MASTER,
      groups: [
        {
          id: 1,
          display_name: "",
        },
      ],
    },
    {
      name: Categories.SLAVE,
      groups: [
        {
          id: 1,
          display_name: "",
        },
      ],
    },
  ],
  yarn: [
    {
      name: Categories.MASTER,
      groups: [
        {
          id: 1,
          display_name: "",
        },
        {
          id: 2,
          display_name: "",
        },
      ],
    },
    {
      name: Categories.SLAVE,
      groups: [
        {
          id: 1,
          display_name: "Nodemanagers Status",
        },
      ],
    },
    {
      name: Categories.CLIENT,
      groups: [
        {
          id: 1,
          display_name: "",
        },
      ],
    },
  ],
  hive: [
    {
      name: Categories.MASTER,
      groups: [
        {
          id: 1,
          display_name: "",
        },
        {
          id: 2,
          display_name: "",
        },
      ],
    },
    {
      name: Categories.CLIENT,
      groups: [
        {
          id: 1,
          display_name: "",
        },
      ],
    },
  ],
  kyuubi: [
    {
      name: Categories.MASTER,
      groups: [
        {
          id: 1,
          display_name: "",
        },
      ],
    },
  ],
  trino_gateway: [
    {
      name: Categories.MASTER,
      groups: [
        {
          id: 1,
          display_name: "",
        },
      ],
    },
  ],
  pinot: [
    {
      name: Categories.MASTER,
      groups: [
        {
          id: 1,
          display_name: "",
        },
      ],
    },
    {
      name: Categories.SLAVE,
      groups: [
        {
          id: 1,
          display_name: "",
        },
      ],
    },
  ]
};

export const serviceMetricsMap: { [key: string]: ServiceMetricMap[] } = {
  hdfs: [
    {
      display_name: "",
      modelKey: "dfsTotalBlocksValues",
      description: "Total",
      descriptionModelKey: "",
      group_id: 1,
    },
    {
      display_name: "",
      modelKey: "dfsCorruptBlocksValues",
      description: "Corrupt Replica",
      group_id: 1,
    },
    {
      display_name: "",
      modelKey: "dfsMissingBlocksValues",
      description: "Missing",
      group_id: 1,
    },
    {
      display_name: "",
      modelKey: "dfsUnderReplicatedBlocksValues",
      description: "Under Replicated",
      group_id: 1,
    },
    {
      display_name: "Total Files + Directories",
      modelKey: "dfsTotalFilesValues",
      description: "",
      group_id: 2,
    },
    {
      display_name: "Upgrade Status",
      modelKey: "upgradeFinalized",
      description: "",
      group_id: 2,
    },
    {
      display_name: "Safe Mode Status",
      modelKey: "safeModeStatus",
      description: "",
      group_id: 2,
    },
    {
      display_name: "Disk usage (dfs used)",
      modelKey: "percentDFSUsed",
      descriptionModelKey: "diskPartDFSUsed",
      description: "",
      group_id: 3,
    },
    {
      display_name: "Disk usage (non dfs used)",
      modelKey: "percentNonDFSUsed",
      description: "",
      descriptionModelKey: "diskPartNonDFSUsed",
      group_id: 3,
    },
    {
      display_name: "Disk remaining",
      modelKey: "percentDFSRemaining",
      description: "",
      descriptionModelKey: "diskPartDFSRemaining",
      group_id: 3,
    },
  ],
  hbase: [
    {
      display_name: "MASTER STARTED",
      modelKey: "masterStartTime",
      description: "",
      descriptionModelKey: "",
      group_id: 1,
    },
    {
      display_name: "MASTER ACTIVATED",
      modelKey: "masterActiveTime",
      description: "",
      descriptionModelKey: "",
      group_id: 1,
    },
    {
      display_name: "AVERAGE LOAD",
      modelKey: "averageLoad",
      description: "",
      descriptionModelKey: "",
      group_id: 1,
    },
    {
      display_name: "MASTER HEAP",
      modelKey: "percentHbaseMasterHeap",
      description: "",
      descriptionModelKey: "diskPartHbaseMasterHeap",
      group_id: 1,
    },
  ],
  yarn: [
    {
      display_name: "Allocated",
      modelKey: "containersAllocated",
      description: "",
      descriptionModelKey: "",
      group_id: 1,
    },
    {
      display_name: "Pending",
      modelKey: "containersPending",
      description: "",
      descriptionModelKey: "",
      group_id: 1,
    },
    {
      display_name: "Reserved",
      modelKey: "containersReserved",
      description: "",
      descriptionModelKey: "",
      group_id: 1,
    },
    {
      display_name: "Submitted",
      modelKey: "appsSubmitted",
      description: "",
      descriptionModelKey: "",
      group_id: 2,
    },
    {
      display_name: "Running",
      modelKey: "appsRunning",
      description: "",
      descriptionModelKey: "",
      group_id: 2,
    },
    {
      display_name: "Pending",
      modelKey: "appsPending",
      description: "",
      descriptionModelKey: "",
      group_id: 2,
    },
    {
      display_name: "Completed",
      modelKey: "appsCompleted",
      description: "",
      descriptionModelKey: "",
      group_id: 2,
    },
    {
      display_name: "Killed",
      modelKey: "appsKilled",
      description: "",
      descriptionModelKey: "",
      group_id: 3,
    },
    {
      display_name: "Failed",
      modelKey: "appsFailed",
      description: "",
      descriptionModelKey: "",
      group_id: 3,
    },
    {
      display_name: "Used",
      modelKey: "usedMemory",
      description: "",
      descriptionModelKey: "",
      group_id: 4,
    },
    {
      display_name: "Reserved",
      modelKey: "reservedMemory",
      description: "",
      descriptionModelKey: "",
      group_id: 4,
    },
    {
      display_name: "Available",
      modelKey: "availableMemory",
      description: "",
      descriptionModelKey: "",
      group_id: 4,
    },
    {
      display_name: "Queues",
      modelKey: "queueKeysPolledFormattedData",
      description: "",
      descriptionModelKey: "",
      group_id: 5,
    },
  ],
  hive: [
    // HIVESERVER2 JDBC URL is handled separately in ServiceMetrics component
    // No regular service metrics for Hive - only the dedicated JDBC URL section
  ],
};

export const serviceMetricsGroups: { [key: string]: ServiceMetricsGroups[] } = {
  hdfs: [
    {
      id: 1,
      display_name: "Blocks",
    },
    {
      id: 2,
      display_name: "",
    },
    {
      id: 3,
      display_name: "",
    },
  ],
  hbase: [
    {
      id: 1,
      display_name: "",
    },
  ],
  yarn: [
    {
      id: 1,
      display_name: "Containers",
    },
    {
      id: 2,
      display_name: "Applications",
    },
    {
      id: 3,
      display_name: "",
    },
    {
      id: 4,
      display_name: "Cluster Memory",
    },
    {
      id: 5,
      display_name: "",
    },
  ],
  hive: [
    {
      id: 1,
      display_name: "",
    },
  ],
};
