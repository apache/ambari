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

var App = require('app');

App.MainAdminServiceGroupsController = Em.Controller.extend(App.LocalStorage, {
  name: 'mainAdminServiceGroupsController',

  /** 
   * Dummy data that will be removed when we have an API to query.
   */
  serviceGroups: [
    {
      id: "HDPCore-300",
      name: "HDP Core",
      version: "3.0.0",
      installedServices: [
        {
          name: "HDFS",
          version: "x.x"
        },
        {
          name: "Zookeeper",
          version: "x.x"
        }
      ],
      otherServices: [
        {
          name: "Blarg",
          version: "x.x"
        },
        {
          name: "Snarf",
          version: "x.x"
        }
      ],
      history: [
        {
          date: new Date(2017, 0, 14),
          version: '2.7',
          versionType: 'release'
        },
        {
          date: new Date(2017, 12, 16),
          version: '3.0',
          versionType: 'release'
        },
        {
          date: new Date(2017, 9, 17),
          version: '3.1',
          versionType: 'release'
        },
        {
          date: new Date(2017, 0, 11),
          version: '2.7',
          versionType: 'release'
        },
        {
          date: new Date(2017, 12, 12),
          version: '3.0',
          versionType: 'release'
        },
        {
          date: new Date(2017, 9, 13),
          version: '3.1',
          versionType: 'release'
        },
        {
          date: new Date(2017, 6, 15),
          version: '2.6.1.1',
          versionType: 'hotfix'
        },
        {
          date: new Date(2017, 3, 15),
          version: '2.6.2',
          versionType: 'patch'
        },
        {
          date: new Date(2017, 0, 15),
          version: '2.7',
          versionType: 'release'
        },
        {
          date: new Date(2017, 12, 15),
          version: '3.0',
          versionType: 'release'
        },
        {
          date: new Date(2017, 9, 15),
          version: '3.1',
          versionType: 'release'
        }
      ]
    },
    {
      id: "ODS-300",
      name: "ODS",
      version: "3.0.0",
      installedServices: [
        {
          name: "HDFS",
          version: "x.x"
        },
        {
          name: "Zookeeper",
          version: "x.x"
        }
      ],
      otherServices: [
        {
          name: "Blarg",
          version: "x.x"
        },
        {
          name: "Snarf",
          version: "x.x"
        }
      ],
      history: [
        {
          date: new Date(2017, 6, 15),
          version: '2.6.1.1',
          versionType: 'hotfix'
        },
        {
          date: new Date(2017, 3, 15),
          version: '2.6.2',
          versionType: 'patch'
        },
        {
          date: new Date(2017, 0, 15),
          version: '2.7',
          versionType: 'release'
        },
        {
          date: new Date(2017, 9, 15),
          version: '3.0',
          versionType: 'release'
        }
      ]
    }
  ],

  // upgrade: null,
  upgrade: {
    id: "upgrade-1",
    currentStep: "prerequisites", //can be "prerequisites", "install", or "upgrade"
    //currentStep: "install", //can be "prerequisites", "install", or "upgrade"
    //currentStep: "upgrade", //can be "prerequisites", "install", or "upgrade"
    history: [
      {
        name: "Event 1",
        user: "Jason",
        date: new Date(2018,1,1)
      },
      {
        name: "Event 2",
        user: "Jason",
        date: new Date(2018,2,1)
      },
      {
        name: "Event 3",
        user: "Jason",
        date: new Date(2017,3,1)
      },
      {
        name: "Event 4",
        user: "Jason",
        date: new Date(2017,4,1)
      },
      {
        name: "Event 5",
        user: "Jason",
        date: new Date(2017,5,1)
      }
    ],
    mpacks: [
      {
        name: "Hortonworks Data Platform Core",
        currentVersion: "2.6",
        newVersion: "2.7",
        services: [
          {
            name: "HDFS",
            currentVersion: "2.6",
            newVersion: "3.0"
          },
          {
            name: "Zookeeper",
            currentVersion: "2.6",
            newVersion: "3.0"
          },
          {
            name: "Zookeeper Client",
            currentVersion: "2.6",
            newVersion: "3.0"
          },
          {
            name: "MapReduce",
            currentVersion: "2.6",
            newVersion: "3.0"
          }
        ]
      },
      {
        name: "Data Science and Machine Learning",
        currentVersion: "2.1",
        newVersion: "2.2",
        services: [
          {
            name: "HDFS",
            currentVersion: "2.6",
            newVersion: "3.0"
          },
          {
            name: "Zookeeper",
            currentVersion: "2.6",
            newVersion: "3.0"
          },
          {
            name: "Zookeeper Client",
            currentVersion: "2.6",
            newVersion: "3.0"
          },
          {
            name: "MapReduce",
            currentVersion: "2.6",
            newVersion: "3.0"
          }
        ]
      }
    ]
  },

  createPlan: function () { },
  editPlan: function () { },
  discardPlan: function () {}
});
