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

App = require('app');
require('ember');
require('models/host_component')
require('views/common/modal_popup');
require('mixins/common/userPref');
require('controllers/application');
require('controllers/global/background_operations_controller');
require('controllers/global/cluster_controller');
require('controllers/main/service/reassign_controller')
require('controllers/main/service/item');
var batchUtils = require('utils/batch_scheduled_requests');

describe('App.MainServiceItemController', function () {

  describe('#setStartStopState', function () {
    var tests = [
      {
        serviceController: {
          serviceName: "YARN"
        },
        backgroundOperationsController: {
          services: [
            {
              isRunning: true,
              dependentService: "ALL_SERVICES"
            }
          ]
        },
        isPending: true,
        m: 'operaion is active because all services are running'
      },
      {
        serviceController: {
          serviceName: "HBASE"
        },
        backgroundOperationsController: {
          services: [
            {
              isRunning: true,
              dependentService: "HBASE"
            }
          ]
        },
        isPending: true,
        m: 'operaion is active button because current service is running'
      },
      {
        serviceController: {
          serviceName: "HDFS"
        },
        backgroundOperationsController: {
          services: [

          ]
        },
        isPending: true,
        m: 'pending is true - backgroundOperationsController.services is empty'
      },
      {
        serviceController: {
          serviceName: "HBASE"
        },
        backgroundOperationsController: {
          services: [
            {
              isRunning: false,
              dependentService: "ALL_SERVICES"
            }
          ]
        },
        isPending: false,
        m: 'pending is false - operation is not running'
      },
      {
        serviceController: {
          serviceName: "HBASE"
        },
        backgroundOperationsController: {
          services: [
            {
              isRunning: true,
              dependentService: "HDFS"
            }
          ]
        },
        isPending: false,
        m: 'pending is false - current service is not running'
      }
    ];


    tests.forEach(function (test) {
      it(test.m, function () {
        var backgroundOperationsController = App.BackgroundOperationsController.create();
        backgroundOperationsController.set('services', []);
        test.backgroundOperationsController.services.forEach(function (service) {
          backgroundOperationsController.get('services').push(service);
        });
        backgroundOperationsController.set("serviceTimestamp", new Date().getTime());
        App.router.set('backgroundOperationsController', backgroundOperationsController);
        var mainServiceItemController = App.MainServiceItemController.create({content: {serviceName: test.serviceController.serviceName}});
        mainServiceItemController.setStartStopState();
        expect(mainServiceItemController.get('isPending')).to.equal(test.isPending);
      });
    })
  });

  describe('#reassignMaster()', function () {
    var v;
    var tests = [
      {
        host_components: [
          {component_name: "RESOURCEMANGER"}
        ],
        conponentName: "RESOURCEMANGER",
        result: true,
        m: 'run reassignMaster'
      },
      {
        host_components: [
          {component_name: "RESOURCEMANGER"}
        ],
        conponentName: "DATANODE",
        result: false,
        m: 'don\t run reassignMaster'
      }
    ];

    tests.forEach(function (test) {
      var reassignMasterController = App.ReassignMasterController.create({currentStep: ''});
      beforeEach(function () {
        sinon.spy(reassignMasterController, 'saveComponentToReassign');
        sinon.spy(reassignMasterController, 'getSecurityStatus');
        sinon.spy(reassignMasterController, 'setCurrentStep');
        App.router.transitionTo = Em.K;
      });
      afterEach(function () {
        reassignMasterController.saveComponentToReassign.restore();
        reassignMasterController.getSecurityStatus.restore();
        reassignMasterController.setCurrentStep.restore();
        App.router.transitionTo = v;
      });
      it(test.m, function () {
        v = App.router.transitionTo;
        var mainServiceItemController = App.MainServiceItemController.create({});
        App.router.set('reassignMasterController', reassignMasterController);
        App.store.loadMany(App.HostComponent, test.host_components);
        mainServiceItemController.reassignMaster(test.conponentName);
        expect(reassignMasterController.saveComponentToReassign.calledOnce).to.equal(test.result);
        expect(reassignMasterController.getSecurityStatus.calledOnce).to.equal(test.result);
        expect(reassignMasterController.setCurrentStep.calledOnce).to.equal(test.result);
      });
    }, this);
  });

  describe("#updateService", function () {

    var tests = [
      {
        params: {
          passive_state: "ON"
        },
        m: "turn on passive"
      },
      {
        params: {
          passive_state: "OFF"
        },
        m: "turn off passive"
      }
    ];
    tests.forEach(function (test) {
      it(test.m, function () {
        var clusterController = App.ClusterController.create();
        App.router.set('clusterController', clusterController);
        var mainServiceItemController = App.MainServiceItemController.create({content: {passiveState: "ON"}});
        mainServiceItemController.updateService(null, null, test.params);
        expect(mainServiceItemController.get('content.passiveState')).to.equal(test.params.passive_state);
      });
    });
  });

  describe("#doAction", function () {

    var el = document.createElement("BUTTON");
    el.disabled = false;
    var tests = [
      {
        event: {
          target: el,
          context: {
            action: 'runSmokeTest'
          }
        },
        m: "run runSmokeTest"
      },
      {
        event: {
          target: el,
          context: {
            action: 'refreshConfigs'
          }
        },
        m: "run refreshConfigs"
      },
      {
        event: {
          target: el,
          context: {
            action: 'restartAllHostComponents'
          }
        },
        m: "run restartAllHostComponents"
      },
      {
        event: {
          target: el,
          context: {
            action: 'rollingRestart'
          }
        },
        m: "run rollingRestart"
      }
    ];

    tests.forEach(function (test) {
      var mainServiceItemController = App.MainServiceItemController.create({});
      mainServiceItemController.set(test.event.context.action, Em.K);
      beforeEach(function () {
        sinon.spy(mainServiceItemController, test.event.context.action);
      });
      afterEach(function () {
        mainServiceItemController[test.event.context.action].restore();
      });
      it(test.m, function () {
        mainServiceItemController.doAction(test.event);
        expect(mainServiceItemController[test.event.context.action].calledOnce).to.equal(!test.event.target.disabled);
      });
    });
  });

  describe("#startStopPopupPrimary", function () {


    var tests = [
      {
        data: {
          "serviceName": "HDFS",
          "state": "STARTED",
          "query": Em.Object.create({ServiceInfo: "FAIL"})
        },
        request: {
          "RequestInfo": {
            "context": "_PARSE_.START.HDFS"
          },
          "Body": {
            "ServiceInfo": {
              "state": "STARTED"
            }
          }
        },
        m: "Start HDFS"
      },
      {
        data: {
          "serviceName": "YARN",
          "state": "STOPPED",
          "query": Em.Object.create({ServiceInfo: "FAIL"})
        },
        request: {
          "RequestInfo": {
            "context": "_PARSE_.STOP.YARN"
          },
          "Body": {
            "ServiceInfo": {
              "state": "STOPPED"
            }
          }
        },
        m: "Stop YARN"
      }
    ];

    beforeEach(function () {
      sinon.spy($, 'ajax');
    });

    afterEach(function () {
      $.ajax.restore();
    });


    tests.forEach(function (test) {
      it('', function () {
        var mainServiceItemController = App.MainServiceItemController.create({content: {serviceName: test.data.serviceName}});
        mainServiceItemController.startStopPopupPrimary(test.data.state, test.data.query);
        expect($.ajax.calledOnce).to.equal(true);

        expect(JSON.parse($.ajax.args[0][0].data).Body.ServiceInfo.state).to.equal(test.request.Body.ServiceInfo.state);
        expect(JSON.parse($.ajax.args[0][0].data).RequestInfo.context).to.equal(test.request.RequestInfo.context);

        expect(mainServiceItemController.get('isStopDisabled')).to.equal(true);
        expect(mainServiceItemController.get('isStartDisabled')).to.equal(true);
      });
    });


  });

  describe("#startStopPopupSuccessCallback", function () {

    var data = {
      Requests: true
    };
    var params = Em.Object.create({
      query: Em.Object.create()
    });
    var ajaxOptions = {
      data: '{"RequestInfo":{"context":"_PARSE_.STOP.ZOOKEEPER"},"Body":{"ServiceInfo":{"state":"INSTALLED"}}}'
    };

    var content = {
      workStatus: "",
      hostComponents: [
        {
          workStatus: ""
        }
      ]
    };

    var mainServiceItemController = App.MainServiceItemController.create({content: content});
    var applicationController = App.ApplicationController.create(App.UserPref, {});
    var clusterController = App.ClusterController.create({loadUpdatedStatusDelayed: Em.K});
    var backgroundOperationsController = App.BackgroundOperationsController.create({showPopup: Em.K});
    App.testMode = false;
    beforeEach(function () {
      App.testMode = false;
      sinon.spy(clusterController, "loadUpdatedStatusDelayed");
    });
    afterEach(function () {
      clusterController.loadUpdatedStatusDelayed.restore();
      App.testMode = true;
    });
    it("open bgo popup", function () {
      App.router.set('applicationController', applicationController);
      App.router.set('clusterController', clusterController);
      App.router.set('backgroundOperationsController', backgroundOperationsController);
      mainServiceItemController.startStopPopupSuccessCallback(data, ajaxOptions, params);
      expect(clusterController.loadUpdatedStatusDelayed.calledOnce).to.equal(true);
    })
  });


  describe("#startService , #stopService", function () {
    var mainServiceItemController = App.MainServiceItemController.create({startStopPopup: Em.K});
    beforeEach(function () {
      sinon.spy(mainServiceItemController, "startStopPopup");
    });
    afterEach(function () {
      mainServiceItemController.startStopPopup.restore();
    });
    it("start service", function () {
      mainServiceItemController.startService({});
      expect(mainServiceItemController.startStopPopup.calledWith({},App.HostComponentStatus.started)).to.equal(true);
    });
    it("stop service", function () {
      mainServiceItemController.stopService({});
      expect(mainServiceItemController.startStopPopup.calledWith({},App.HostComponentStatus.stopped)).to.equal(true);
    });
  });


  describe("#turnOnOffPassive", function () {
    var mainServiceItemController = App.MainServiceItemController.create({turnOnOffPassiveRequest: Em.K});
    beforeEach(function () {
      sinon.spy(mainServiceItemController, "turnOnOffPassiveRequest");
    });
    afterEach(function () {
      mainServiceItemController.turnOnOffPassiveRequest.restore();
    });
    it("turns on/off passive mode for service", function () {
      mainServiceItemController.turnOnOffPassive({}).onPrimary();
      expect(mainServiceItemController.turnOnOffPassiveRequest.calledOnce).to.equal(true);
    });
  });

  describe("#runSmokeTest", function () {
    var tests = [
      {
        content: {
          id: "YARN",
          service_name: "YARN",
          work_status: "STARTED"
        },
        startSmoke: true,
        serviceName: "MAPREDUCE2",
        m: "don't run smoke test primary for MAPREDUCE2"
      },
      {
        content: {
          id: "YARN",
          service_name: "YARN",
          work_status: "STOPPED"
        },
        startSmoke: false,
        serviceName: "MAPREDUCE2",
        m: "run smoke test primary for MAPREDUCE2"
      },
      {
        m: "run smoke test primary for all services (not MAPREDUCE2)",
        startSmoke: true,
        default: true
      }
    ];

    tests.forEach(function (test) {
      var mainServiceItemController = test.default ? App.MainServiceItemController.create({runSmokeTestPrimary: Em.K}) :
          App.MainServiceItemController.create({content: {serviceName: test.serviceName}, runSmokeTestPrimary: Em.K});
      beforeEach(function () {
        sinon.spy(mainServiceItemController, "runSmokeTestPrimary");
      });
      afterEach(function () {
        mainServiceItemController.runSmokeTestPrimary.restore();
      });
      it(test.m, function () {
        if (!test.default) {
          App.store.load(App.Service, test.content);
        }
        mainServiceItemController.runSmokeTest({}).onPrimary();
        expect(mainServiceItemController.runSmokeTestPrimary.calledOnce).to.equal(test.startSmoke);
      });
    });
  });

  describe("#refreshConfigs", function () {
    var temp = batchUtils.restartHostComponents;
    beforeEach(function () {
      sinon.stub(App.ajax, 'send', function(opt) {
        return opt.data.callback({ items: [] });
      });
      batchUtils.restartHostComponents = Em.K;
      sinon.spy(batchUtils, "restartHostComponents");
    });
    afterEach(function () {
      batchUtils.restartHostComponents.restore();
      batchUtils.restartHostComponents = temp;
      App.ajax.send.restore();
    });
    var tests = [
      {
        isClientsOnly: true,
        m: "run refresh for clients"
      },
      {
        isClientsOnly: false,
        m: "don't run refresh for non clients"
      }
    ];
    tests.forEach(function (test) {
      var mainServiceItemController = App.MainServiceItemController.create({content: {isClientsOnly: test.isClientsOnly}});
      it(test.m, function () {
        if (test.isClientsOnly) {
          mainServiceItemController.refreshConfigs().onPrimary();
        } else {
          mainServiceItemController.refreshConfigs();
        }
        expect(batchUtils.restartHostComponents.calledOnce).to.equal(test.isClientsOnly);
      });
    }, this);
  });

  describe("#startStopPopup", function () {
    var el = document.createElement("BUTTON");
    el.disabled = false;
    var event = {
      target: el
    }
    var mainServiceItemController = App.MainServiceItemController.create({content: {serviceName: "HDFS"}});
    beforeEach(function () {
      sinon.spy(mainServiceItemController, "startStopPopupPrimary");
    });
    afterEach(function () {
      mainServiceItemController.startStopPopupPrimary.restore();
    });
    it("start start/stop service popup", function () {
      mainServiceItemController.startStopPopup(event, "").onPrimary();
      expect(mainServiceItemController.startStopPopupPrimary.calledOnce).to.equal(true);
    });
  });

  describe("#restartAllHostComponents", function () {
    var temp = batchUtils.restartAllServiceHostComponents;
    beforeEach(function () {
      batchUtils.restartAllServiceHostComponents = Em.K;
      sinon.spy(batchUtils, "restartAllServiceHostComponents");
    });
    afterEach(function () {
      batchUtils.restartAllServiceHostComponents.restore();
      batchUtils.restartAllServiceHostComponents = temp;
    });

    var mainServiceItemController = App.MainServiceItemController.create({content: {displayName: "HDFS"}});

    it("start restartAllHostComponents for service", function () {
      mainServiceItemController.restartAllHostComponents({}).onPrimary();
      expect(batchUtils.restartAllServiceHostComponents.calledOnce).to.equal(true);
    });
  });

  describe("#rollingRestart", function () {
    var temp = batchUtils.launchHostComponentRollingRestart;
    beforeEach(function () {
      batchUtils.launchHostComponentRollingRestart = Em.K;
      sinon.spy(batchUtils, "launchHostComponentRollingRestart");
    });
    afterEach(function () {
      batchUtils.launchHostComponentRollingRestart.restore();
      batchUtils.launchHostComponentRollingRestart = temp;
    });

    var mainServiceItemController = App.MainServiceItemController.create();

    it("start restartAllHostComponents for service", function () {
      mainServiceItemController.rollingRestart();
      expect(batchUtils.launchHostComponentRollingRestart.calledOnce).to.equal(true);
    });
  });

  describe("#isStartDisabled", function () {
    var tests = [
      {
        content: {
          healthStatus: 'red'
        },
        isPending: true,
        disabled: true,
        m: "disabled because of pending"
      },
      {
        content: {
          healthStatus: 'green'
        },
        isPending: false,
        disabled: true,
        m: "disabled because healthStatus is not red"
      },
      {
        content: {
          healthStatus: 'red'
        },
        isPending: false,
        disabled: false,
        m: "enabled because healthStatus is red and pending is false"
      }
    ];
    tests.forEach(function (test) {
      it(test.m, function () {
        var mainServiceItemController = App.MainServiceItemController.create({content: {healthStatus: test.content.healthStatus}, isPending: test.isPending});
        expect(mainServiceItemController.get('isStartDisabled')).to.equal(test.disabled);
      });
    });
  });

  describe("#isSopDisabled", function () {
    var tests = [
      {
        content: {
          healthStatus: 'red'
        },
        isPending: true,
        disabled: true,
        m: "disabled because of pending"
      },
      {
        content: {
          healthStatus: 'green'
        },
        isPending: false,
        disabled: false,
        m: "enabled because healthStatus is green and pending is false"
      },
      {
        content: {
          healthStatus: 'red'
        },
        isPending: false,
        disabled: true,
        m: "disabled because healthStatus is not green"
      }
    ];
    tests.forEach(function (test) {
      it(test.m, function () {
        var mainServiceItemController = App.MainServiceItemController.create({content: test.content, isPending: test.isPending});
        expect(mainServiceItemController.get('isStopDisabled')).to.equal(test.disabled);
      });
    });
  });

  describe("#runRebalancer", function () {
    it("run rebalancer", function () {
      var mainServiceItemController = App.MainServiceItemController.create({content: {runRebalancer: false}});
      mainServiceItemController.runRebalancer().onPrimary();
      expect(mainServiceItemController.get("content.runRebalancer")).to.equal(true);
    });
  });

  describe("#runCompaction", function () {
    it("run compaction", function () {
      var mainServiceItemController = App.MainServiceItemController.create({content: {runCompaction: false}});
      mainServiceItemController.runCompaction().onPrimary();
      expect(mainServiceItemController.get("content.runCompaction")).to.equal(true);
    });
  });

  describe("#turnOnOffPassiveRequest", function () {
    var tests = [
      {
        data: {
          "requestInfo": 'Turn On Maintenance Mode',
          "serviceName" : "HDFS",
          "passive_state": "ON"
        },
        RequestInfo: {
          "context": 'Turn On Maintenance Mode'
        },
        Body: {
          ServiceInfo: {
            maintenance_state: "ON"
          }
        }
      }
    ];

    beforeEach(function () {
      sinon.spy($, 'ajax');
    });

    afterEach(function () {
      $.ajax.restore();
    });

    tests.forEach(function (test) {
      it('send request to turn on passive state', function () {
        var mainServiceItemController = App.MainServiceItemController.create({content: {serviceName: test.data.serviceName}});
        mainServiceItemController.turnOnOffPassiveRequest(test.data.passive_state, test.data.requestInfo);
        expect($.ajax.calledOnce).to.equal(true);

        expect(JSON.parse($.ajax.args[0][0].data).Body.ServiceInfo.maintenance_state).to.equal(test.Body.ServiceInfo.maintenance_state);
        expect(JSON.parse($.ajax.args[0][0].data).RequestInfo.context).to.equal(test.RequestInfo.context);
      });
    });
  });

  describe("#runSmokeTestPrimary", function () {
    var tests = [
      {
        data: {
          'serviceName': "HDFS",
          'displayName': "HDFS",
          'query': "test"
        },
        "RequestInfo": {
          "context": "HDFS Service Check",
          "command" : "HDFS_SERVICE_CHECK"
        },
        "Requests/resource_filters": [{"service_name" : "HDFS"}]
      }
    ];
    tests.forEach(function (test) {

    var mainServiceItemController = App.MainServiceItemController.create({content: {serviceName: test.data.serviceName,
      displayName: test.data.displayName}});
    beforeEach(function () {
      mainServiceItemController.set("runSmokeTestErrorCallBack", Em.K);
      mainServiceItemController.set("runSmokeTestSuccessCallBack", Em.K);
      sinon.spy($, 'ajax');
    });

    afterEach(function () {
      $.ajax.restore();
    });

      it('send request to run smoke test', function () {

        mainServiceItemController.runSmokeTestPrimary(test.data.query);
        expect($.ajax.calledOnce).to.equal(true);

        expect(JSON.parse($.ajax.args[0][0].data).RequestInfo.context).to.equal(test.RequestInfo.context);
        expect(JSON.parse($.ajax.args[0][0].data).RequestInfo.command).to.equal(test.RequestInfo.command);
        expect(JSON.parse($.ajax.args[0][0].data)["Requests/resource_filters"][0].serviceName).to.equal(test["Requests/resource_filters"][0].serviceName);
      });
    });
  });
});
