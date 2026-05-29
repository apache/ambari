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

import { ServiceActionEnums } from "../../enums/ServiceActionEnums";

export const ComponentActionsMapping = {
  HDFS: [
    {
      component: "DATANODE",
      actionMap: {
        actionRestart: ServiceActionEnums.restartDataNodeAction,
      },
    },
    {
      component: "JOURNALNODE",
      actionMap: {
        actionRestart: ServiceActionEnums.restartJournalNodeAction,
      },
    },
    {
      component: "ZKFC",
      actionMap: {
        actionRestart: ServiceActionEnums.restartZKFC,
      },
    },
  ],
  RANGER: [
    {
      component: "RANGER_TAGSYNC",
      actionMap: {
        actionRestart: ServiceActionEnums.restartRangerTagSyncsAction,
      },
    },
  ],
  YARN: [
  {
    component: "NODEMANAGER",
    actionMap: {
      actionRestart: ServiceActionEnums.restartNodeManagerAction,
    },
  },
],
  ZOOKEEPER: [
    {
      component: "ZOOKEEPER_SERVER",
      actionMap: {
        actionRestart: ServiceActionEnums.restartZooKeeperServerAction,
      },
    },
  ],
  PINOT: [
    {
      component: "PINOT_BROKER",
      actionMap: {
        actionRestart: "Restart Pinot Brokers",
      },
    },
    {
      component: "PINOT_MINION",
      actionMap: {
        actionRestart: "Restart Pinot Minions",
      },
    },
    {
      component: "PINOT_SERVER",
      actionMap: {
        actionRestart: "Restart Pinot Servers",
      },
    },
  ],
};
