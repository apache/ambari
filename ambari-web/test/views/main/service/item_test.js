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
require('views/main/service/item');

var view;

describe('App.MainServiceItemView', function () {

  describe('#mastersExcludedCommands', function () {

    view = App.MainServiceItemView.create({
      controller: Em.Object.create({
        content: Em.Object.create({
          hostComponents: []
        })
      })
    });

    var nonCustomAction = ['RESTART_ALL', 'RUN_SMOKE_TEST', 'REFRESH_CONFIGS', 'ROLLING_RESTART', 'TOGGLE_PASSIVE', 'TOGGLE_NN_HA', 'TOGGLE_RM_HA', 'MOVE_COMPONENT', 'DOWNLOAD_CLIENT_CONFIGS', 'MASTER_CUSTOM_COMMAND'];
    var keys = Object.keys(view.mastersExcludedCommands);
    var mastersExcludedCommands = [];
    for (var i = 0; i < keys.length; i++) {
      mastersExcludedCommands[i] = view.mastersExcludedCommands[keys[i]];
    }
    var allMastersExcludedCommands = mastersExcludedCommands.reduce(function (previous, current) {
      return previous.concat(current);
    });
    var actionMap = App.HostComponentActionMap.getMap(view);

    var customActionsArray = [];
    for (var iter in actionMap) {
      customActionsArray.push(actionMap[iter]);
    }
    var customActions = customActionsArray.mapProperty('customCommand').filter(function (action) {
      return !nonCustomAction.contains(action);
    }).uniq();

    // remove null and undefined from the list
    customActions = customActions.filter(function (value) {
      return value != null;
    });

    customActions.forEach(function (action) {
      it(action + ' should be present in App.MainServiceItemView mastersExcludedCommands object', function () {
        expect(allMastersExcludedCommands).to.contain(action);
      });
    });
  });

  describe('#observeMaintenance', function () {

    var cases = [
      {
        isMaintenanceSet: true,
        isServicesInfoLoaded: true,
        observeMaintenanceOnceCallCount: 0,
        title: 'actions array set, services info loaded'
      },
      {
        isMaintenanceSet: true,
        isServicesInfoLoaded: false,
        observeMaintenanceOnceCallCount: 0,
        title: 'actions array set, services info not loaded'
      },
      {
        isMaintenanceSet: false,
        isServicesInfoLoaded: true,
        observeMaintenanceOnceCallCount: 1,
        title: 'actions array not set, services info loaded'
      },
      {
        isMaintenanceSet: false,
        isServicesInfoLoaded: false,
        observeMaintenanceOnceCallCount: 0,
        title: 'actions array not set, services info not loaded'
      }
    ];

    beforeEach(function () {
      sinon.stub(view, 'observeMaintenanceOnce', Em.K);
    });

    afterEach(function () {
      view.observeMaintenanceOnce.restore();
    });

    cases.forEach(function (item) {
      it(item.title, function () {
        view.setProperties({
          'isMaintenanceSet': item.isMaintenanceSet,
          'controller.isServicesInfoLoaded': item.isServicesInfoLoaded
        });
        view.observeMaintenance();
        expect(view.observeMaintenanceOnce.callCount).to.equal(item.observeMaintenanceOnceCallCount);
      });
    });

  });

  describe('#observeMaintenanceOnce', function () {

    var mastersExcludedCommands = {
        NAMENODE: ["DECOMMISSION", "REBALANCEHDFS"],
        RESOURCEMANAGER: ["DECOMMISSION", "REFRESHQUEUES"],
        HBASE_MASTER: ["DECOMMISSION"],
        KNOX_GATEWAY: ["STARTDEMOLDAP", "STOPDEMOLDAP"]
      },
      hasConfigTab = true,
      testCases = [
        {
          serviceName: "HDFS",
          displayName: "HDFS",
          isSingleNode: true,
          serviceTypes: ["HA_MODE"],
          slaveComponents: [
            Em.Object.create({
              componentName: 'DATANODE',
              totalCount: 1
            })
          ],
          clientComponents: [
            Em.Object.create({
              componentName: 'HDFS_CLIENT',
              totalCount: 1
            })
          ],
          hostComponents: [
            Em.Object.create({
              componentName: 'NAMENODE',
              isMaster: true,
              isSlave: false
            }),
            Em.Object.create({
              componentName: 'SECONDARY_NAMENODE',
              isMaster: true,
              isSlave: false
            })
          ],
          result: [
            {"action": "restartAllHostComponents", "context": "HDFS", "label": "Restart All", "cssClass": "icon-repeat", "disabled": false},
            {"action": "rollingRestart", "label": "Restart DataNodes", "cssClass": "icon-time", "disabled": false, "context": "DATANODE"},
            {"action": "reassignMaster", "context": "NAMENODE", "label": "Move NameNode", "cssClass": "icon-share-alt", "disabled": false},
            {"action": "reassignMaster", "context": "SECONDARY_NAMENODE", "label": "Move SNameNode", "cssClass": "icon-share-alt", "disabled": false},
            {"action": "enableHighAvailability", "label": "Enable NameNode HA", "cssClass": "icon-arrow-up", "isHidden": false, "disabled": true},
            {"action": "runSmokeTest", "label": "Run Service Check", "cssClass": "icon-thumbs-up-alt", "disabled": false},
            {"action": "turnOnOffPassive", "context": "Turn On Maintenance Mode for HDFS", "label": "Turn On Maintenance Mode", "cssClass": "icon-medkit", "disabled": false},
            {"action": "rebalanceHdfsNodes", "customCommand": "REBALANCEHDFS", "context": "Rebalance HDFS", "label": "Rebalance HDFS", "cssClass": "icon-refresh", "disabled": false},
            {"action": "downloadClientConfigs", "label": "Download Client Configs", "cssClass": "icon-download-alt", "isHidden": false, "disabled": false, hasSubmenu: false, submenuOptions: []}
          ]
        },
        {
          serviceName: "ZOOKEEPER",
          displayName: "ZooKeeper",
          serviceTypes: [],
          slaveComponents: [],
          clientComponents: [
            Em.Object.create({
              componentName: 'ZOOKEEPER_CLIENT',
              totalCount: 1
            })
          ],
          hostComponents: [
            Em.Object.create({
              componentName: 'ZOOKEEPER_SERVER',
              isMaster: true,
              isSlave: false
            })
          ],
          controller: [
            {'addDisabledTooltipZOOKEEPER_SERVER': ''},
            {'isAddDisabled-ZOOKEEPER_SERVER': 'disabled'}
          ],
          result: [
            {"action": "restartAllHostComponents", "context": "ZOOKEEPER", "label": "Restart All", "cssClass": "icon-repeat", "disabled": false},
            {"action": "runSmokeTest", "label": "Run Service Check", "cssClass": "icon-thumbs-up-alt", "disabled": false},
            {"action": "turnOnOffPassive", "context": "Turn On Maintenance Mode for ZooKeeper", "label": "Turn On Maintenance Mode", "cssClass": "icon-medkit", "disabled": false},
            {"cssClass": "icon-plus", "label": "Add ZooKeeper Server", "service": "ZOOKEEPER", "component": "ZOOKEEPER_SERVER", "action": "addZOOKEEPER_SERVER", "disabled": "disabled", tooltip: ''},
            {"action": "downloadClientConfigs", "label": "Download Client Configs", "cssClass": "icon-download-alt", "isHidden": false, "disabled": false, "hasSubmenu": false, "submenuOptions": []}
          ]
        },
        {
          serviceName: "YARN",
          displayName: "YARN",
          serviceTypes: ['HA_MODE'],
          slaveComponents: [
            Em.Object.create({
              componentName: 'NODEMANAGER',
              totalCount: 1
            })
          ],
          clientComponents: [
            Em.Object.create({
              componentName: 'YARN_CLIENT',
              totalCount: 1
            })
          ],
          hostComponents: [
            Em.Object.create({
              componentName: 'APP_TIMELINE_SERVER',
              isMaster: true,
              isSlave: false
            }),
            Em.Object.create({
              componentName: 'RESOURCEMANAGER',
              isMaster: true,
              isSlave: false
            })
          ],
          result: [
            {"action": "refreshYarnQueues", "customCommand": "REFRESHQUEUES", "context": "Refresh YARN Capacity Scheduler", "label": "Refresh YARN Capacity Scheduler", "cssClass": "icon-refresh", "disabled": false},
            {"action": "restartAllHostComponents", "context": "YARN", "label": "Restart All", "cssClass": "icon-repeat", "disabled": false},
            {"action": "rollingRestart", "label": "Restart NodeManagers", "cssClass": "icon-time", "disabled": false, "context": "NODEMANAGER"},
            {"action": "reassignMaster", "context": "APP_TIMELINE_SERVER", "label": "Move App Timeline Server", "cssClass": "icon-share-alt", "disabled": false},
            {"action": "reassignMaster", "context": "RESOURCEMANAGER", "label": "Move ResourceManager", "cssClass": "icon-share-alt", "disabled": false},
            {"action": "enableRMHighAvailability", "label": "Enable ResourceManager HA", "cssClass": "icon-arrow-up", "isHidden": false, disabled: false},
            {"action": "runSmokeTest", "label": "Run Service Check", "cssClass": "icon-thumbs-up-alt", "disabled": false},
            {"action": "turnOnOffPassive", "context": "Turn On Maintenance Mode for YARN", "label": "Turn On Maintenance Mode", "cssClass": "icon-medkit", "disabled": false},
            {"action": "downloadClientConfigs", "label": "Download Client Configs", "cssClass": "icon-download-alt", "isHidden": false, "disabled": false, "hasSubmenu": false, "submenuOptions": []}
          ]
        },
        {
          serviceName: "MAPREDUCE2",
          displayName: "MapReduce2",
          serviceTypes: [],
          slaveComponents: [],
          clientComponents: [
            Em.Object.create({
              componentName: 'MAPREDUCE2_CLIENT',
              totalCount: 1
            })
          ],
          hostComponents: [
            Em.Object.create({
              componentName: 'HISTORYSERVER',
              isMaster: true,
              isSlave: false
            })
          ],
          result: [
            {"action": "restartAllHostComponents", "context": "MAPREDUCE2", "label": "Restart All", "cssClass": "icon-repeat", "disabled": false},
            {"action": "runSmokeTest", "label": "Run Service Check", "cssClass": "icon-thumbs-up-alt", "disabled": false},
            {"action": "turnOnOffPassive", "context": "Turn On Maintenance Mode for MapReduce2", "label": "Turn On Maintenance Mode", "cssClass": "icon-medkit", "disabled": false},
            {"action": "downloadClientConfigs", "label": "Download Client Configs", "cssClass": "icon-download-alt", "isHidden": false, "disabled": false, "hasSubmenu": false, "submenuOptions": []}
          ]
        },
        {
          serviceName: "KAFKA",
          displayName: "Kafka",
          serviceTypes: [],
          slaveComponents: [],
          clientComponents: [],
          hostComponents: [
            Em.Object.create({
              componentName: 'KAFKA_BROKER',
              isMaster: true,
              isSlave: false
            })
          ],
          result: [
            {"action": "restartAllHostComponents", "context": "KAFKA", "label": "Restart All", "cssClass": "icon-repeat", "disabled": false},
            {"action": "runSmokeTest", "label": "Run Service Check", "cssClass": "icon-thumbs-up-alt", "disabled": false},
            {"action": "turnOnOffPassive", "context": "Turn On Maintenance Mode for Kafka", "label": "Turn On Maintenance Mode", "cssClass": "icon-medkit", "disabled": false},
            {"action": "downloadClientConfigs", "label": "Download Client Configs", "cssClass": "icon-download-alt", "isHidden": true, "disabled": false, "hasSubmenu": false, "submenuOptions": []}
          ]
        },
        {
          serviceName: "FLUME",
          displayName: "Flume",
          serviceTypes: [],
          clientComponents: [],
          slaveComponents: [
            Em.Object.create({
              componentName: 'FLUME_HANDLER',
              totalCount: 1
            })
          ],
          hostComponents: [
          ],
          controller: [
            {'addDisabledTooltipFLUME_HANDLER': ''},
            {'isAddDisabled-FLUME_HANDLER': ''}
          ],
          result: [
            {"action": "refreshConfigs", "label": "Refresh configs", "cssClass": "icon-refresh", "disabled": false},
            {"action": "restartAllHostComponents", "context": "FLUME", "label": "Restart All", "cssClass": "icon-repeat", "disabled": false},
            {"action": "rollingRestart", "label": "Restart Flumes", "cssClass": "icon-time", "disabled": false, "context": "FLUME_HANDLER"},
            {"action": "runSmokeTest", "label": "Run Service Check", "cssClass": "icon-thumbs-up-alt", "disabled": false},
            {"action": "turnOnOffPassive", "context": "Turn On Maintenance Mode for Flume", "label": "Turn On Maintenance Mode", "cssClass": "icon-medkit", "disabled": false},
            {"cssClass": "icon-plus", "label": "Add Flume Component", "service": "FLUME", "component": "FLUME_HANDLER", "action": "addFLUME_HANDLER", "disabled": '', tooltip: ''},
            {"action": "downloadClientConfigs", "label": "Download Client Configs", "cssClass": "icon-download-alt", "isHidden": true, "disabled": false, "hasSubmenu": false, "submenuOptions": []}
          ]
        },
        {
          serviceName: "HBASE",
          displayName: "HBase",
          serviceTypes: [],
          slaveComponents: [
            Em.Object.create({
              componentName: 'HBASE_REGIONSERVER',
              totalCount: 1
            })
          ],
          clientComponents: [
            Em.Object.create({
              componentName: 'HBASE_CLIENT',
              totalCount: 1
            })
          ],
          hostComponents: [
            Em.Object.create({
              componentName: 'HBASE_MASTER',
              isMaster: true,
              isSlave: false
            })
          ],
          controller: [
            {'addDisabledTooltipHBASE_MASTER': ''},
            {'isAddDisabled-HBASE_MASTER': ''}
          ],
          result: [
            {"action": "restartAllHostComponents", "context": "HBASE", "label": "Restart All", "cssClass": "icon-repeat", "disabled": false},
            {"action": "rollingRestart", "label": "Restart RegionServers", "cssClass": "icon-time", "disabled": false, "context": "HBASE_REGIONSERVER"},
            {"action": "runSmokeTest", "label": "Run Service Check", "cssClass": "icon-thumbs-up-alt", "disabled": false},
            {"action": "turnOnOffPassive", "context": "Turn On Maintenance Mode for HBase", "label": "Turn On Maintenance Mode", "cssClass": "icon-medkit", "disabled": false},
            {"cssClass": "icon-plus", "label": "Add HBase Master", "service": "HBASE", "component": "HBASE_MASTER", "action": "addHBASE_MASTER", "disabled": '', tooltip: ''},
            {"action": "downloadClientConfigs", "label": "Download Client Configs", "cssClass": "icon-download-alt", "isHidden": false, "disabled": false, "hasSubmenu": false, "submenuOptions": []}
          ]
        },
        {
          serviceName: "OOZIE",
          displayName: "Oozie",
          serviceTypes: [],
          slaveComponents: [],
          clientComponents: [
            Em.Object.create({
              componentName: 'OOZIE_CLIENT',
              totalCount: 1
            })
          ],
          hostComponents: [
            Em.Object.create({
              componentName: 'OOZIE_SERVER',
              isMaster: true,
              isSlave: false
            })
          ],
          result: [
            {"action": "restartAllHostComponents", "context": "OOZIE", "label": "Restart All", "cssClass": "icon-repeat", "disabled": false},
            {"action": "reassignMaster", "context": "OOZIE_SERVER", "label": "Move Oozie Server", "cssClass": "icon-share-alt", "disabled": false},
            {"action": "runSmokeTest", "label": "Run Service Check", "cssClass": "icon-thumbs-up-alt", "disabled": false},
            {"action": "turnOnOffPassive", "context": "Turn On Maintenance Mode for Oozie", "label": "Turn On Maintenance Mode", "cssClass": "icon-medkit", "disabled": false},
            {"action": "downloadClientConfigs", "label": "Download Client Configs", "cssClass": "icon-download-alt", "isHidden": false, "disabled": false, "hasSubmenu": false, "submenuOptions": []}
          ]
        },
        {
          serviceName: "KNOX",
          displayName: "Knox",
          serviceTypes: [],
          slaveComponents: [],
          clientComponents: [],
          hostComponents: [
            Em.Object.create({
              componentName: 'KNOX_GATEWAY',
              isMaster: true,
              isSlave: false
            })
          ],
          result: [
            {"action": "restartAllHostComponents", "context": "KNOX", "label": "Restart All", "cssClass": "icon-repeat", "disabled": false},
            {"action": "runSmokeTest", "label": "Run Service Check", "cssClass": "icon-thumbs-up-alt", "disabled": false},
            {"action": "turnOnOffPassive", "context": "Turn On Maintenance Mode for Knox", "label": "Turn On Maintenance Mode", "cssClass": "icon-medkit", "disabled": false},
            {"action": "startLdapKnox", "customCommand": "STARTDEMOLDAP", "context": "Start Demo LDAP",  "label": "Start Demo LDAP", "cssClass": "icon-play-sign", "disabled": false},
            {"action": "stopLdapKnox", "customCommand": "STOPDEMOLDAP", "context": "Stop Demo LDAP", "label": "Stop Demo LDAP", "cssClass": "icon-stop", "disabled": false},
            {"action": "downloadClientConfigs", "label": "Download Client Configs", "cssClass": "icon-download-alt", "isHidden": true, "disabled": false, "hasSubmenu": false, "submenuOptions": []}
          ]
        },
        {
          serviceName: "STORM",
          displayName: "Storm",
          serviceTypes: [],
          slaveComponents: [],
          clientComponents: [],
          hostComponents: [
            Em.Object.create({
              componentName: 'NIMBUS',
              isMaster: true,
              isSlave: false
            })
          ],
          result: [
            {"action": "restartAllHostComponents", "context": "STORM", "label": "Restart All", "cssClass": "icon-repeat", "disabled": false},
            {"action": "runSmokeTest", "label": "Run Service Check", "cssClass": "icon-thumbs-up-alt", "disabled": false},
            {"action": "turnOnOffPassive", "context": "Turn On Maintenance Mode for Storm", "label": "Turn On Maintenance Mode", "cssClass": "icon-medkit", "disabled": false},
            {"action": "downloadClientConfigs", "label": "Download Client Configs", "cssClass": "icon-download-alt", "isHidden": true, "disabled": false, "hasSubmenu": false, "submenuOptions": []}
          ]
        }
      ];

    beforeEach(function () {

      view = App.MainServiceItemView.create({});

      sinon.stub(App, 'get', function (k) {
        switch (k) {
          case 'isSingleNode':
            return (view.get('controller.content.serviceName') == 'HDFS');
          case 'supports.autoRollbackHA':
          case 'isRMHaEnabled':
          case 'isHaEnabled':
            return false;
          case 'components.rollinRestartAllowed':
            return ["DATANODE", "JOURNALNODE", "ZKFC", "NODEMANAGER", "GANGLIA_MONITOR", "HBASE_REGIONSERVER", "SUPERVISOR", "FLUME_HANDLER"];
          case 'components.reassignable':
            return ["NAMENODE", "SECONDARY_NAMENODE", "APP_TIMELINE_SERVER", "RESOURCEMANAGER", "WEBHCAT_SERVER", "OOZIE_SERVER"];
          case 'services.supportsServiceCheck':
            return ["HDFS", "MAPREDUCE2", "YARN", "HIVE", "HBASE", "PIG", "SQOOP", "OOZIE", "ZOOKEEPER", "FALCON", "STORM", "FLUME", "SLIDER", "KNOX", "KAFKA"];
          case 'components.addableToHost':
            return ["DATANODE", "HDFS_CLIENT", "MAPREDUCE2_CLIENT", "NODEMANAGER", "YARN_CLIENT", "TEZ_CLIENT", "GANGLIA_MONITOR", "HCAT", "HIVE_CLIENT", "HIVE_METASTORE", "HIVE_SERVER", "WEBHCAT_SERVER", "HBASE_CLIENT", "HBASE_MASTER", "HBASE_REGIONSERVER", "PIG", "SQOOP", "OOZIE_CLIENT", "OOZIE_SERVER", "ZOOKEEPER_CLIENT", "ZOOKEEPER_SERVER", "FALCON_CLIENT", "SUPERVISOR", "FLUME_HANDLER", "METRICS_MONITOR", "KAFKA_BROKER", "KERBEROS_CLIENT", "KNOX_GATEWAY", "SLIDER", "SPARK_CLIENT"];
          default:
            return Em.get(App, k);
        }
      });

      sinon.stub(App.HostComponent, 'find', function () {
        return [
          Em.Object.create({
            hostName: 'host1',
            componentName: 'NAMENODE'
          }),
          Em.Object.create({
            hostName: 'host1',
            componentName: 'SECONDARY_NAMENODE'
          }),
          Em.Object.create({
            hostName: 'host1',
            componentName: 'APP_TIMELINE_SERVER'
          }),
          Em.Object.create({
            hostName: 'host1',
            componentName: 'RESOURCEMANAGER'
          }),
          Em.Object.create({
            hostName: 'host1',
            componentName: 'OOZIE_SERVER'
          })
        ];
      });

      sinon.stub(App.StackServiceComponent, 'find', function () {
        switch (arguments[0]) {
          case 'NAMENODE':
            return Em.Object.create({ customCommands: ["DECOMMISSION", "REBALANCEHDFS"] });
          case 'RESOURCEMANAGER':
            return Em.Object.create({ customCommands: ["DECOMMISSION", "REFRESHQUEUES"] });
          case 'HBASE_MASTER':
            return Em.Object.create({ customCommands: ["DECOMMISSION"] });
          case 'KNOX_GATEWAY':
            return Em.Object.create({ customCommands: ["STARTDEMOLDAP", "STOPDEMOLDAP"] });
          case 'HISTORYSERVER':
          case 'SECONDARY_NAMENODE':
          case 'ZOOKEEPER_SERVER':
          case 'APP_TIMELINE_SERVER':
          case 'KAFKA_BROKER':
          case 'OOZIE_SERVER':
          case 'NIMBUS':
            return Em.Object.create({ customCommands: [] });
          default:
            return [
              Em.Object.create({
                customCommands: ["DECOMMISSION", "REBALANCEHDFS"],
                componentName: 'NAMENODE'
              }),
              Em.Object.create({
                customCommands: ["STARTDEMOLDAP", "STOPDEMOLDAP"],
                componentName: 'KNOX_GATEWAY'
              })
            ];
        }
      });
    });

    afterEach(function () {
      App.get.restore();
      App.HostComponent.find.restore();
      App.StackServiceComponent.find.restore();
    });

    testCases.forEach(function (testCase) {

      it('Maintenance for ' + testCase.serviceName + ' service', function () {
        view.reopen({
          controller: Em.Object.create({
            content: Em.Object.create({
              hostComponents: testCase.hostComponents,
              slaveComponents: testCase.slaveComponents,
              clientComponents: testCase.clientComponents,
              serviceName: testCase.serviceName,
              displayName: testCase.displayName,
              serviceTypes: testCase.serviceTypes,
              passiveState: 'OFF'
            }),
            isSeveralClients: false,
            clientComponents: [],
            isStopDisabled: false
          }),
          mastersExcludedCommands: mastersExcludedCommands,
          hasConfigTab: hasConfigTab
        });
        if (testCase.controller) {
          testCase.controller.forEach(function (item) {
            Object.keys(item).forEach(function (key) {
              view.set('controller.' + key, item[key]);
            });
          });
        }
        view.observeMaintenanceOnce();
        expect(view.get('maintenance')).to.eql(testCase.result);
        var oldMaintenance = JSON.parse(JSON.stringify(view.get('maintenance')));
        view.set('controller.content.passiveState', 'ON');
        view.observeMaintenanceOnce();
        expect(view.get('maintenance')).to.not.eql(oldMaintenance);
        expect(view.get('isMaintenanceSet')).to.be.true;
      });

    });
  });

  describe('#clearIsMaintenanceSet', function () {
    it('isMaintenanceSet should be false', function () {
      view.set('isMaintenanceSet', true);
      view.clearIsMaintenanceSet();
      expect(view.get('isMaintenanceSet')).to.be.false;
    });
  });
});

