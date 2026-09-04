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
  hdfs: [],
  hbase: [],
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
