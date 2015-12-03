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
require('utils/updater');
require('controllers/global/update_controller');

describe('App.UpdateController', function () {
  var controller = App.UpdateController.create({
    clusterName: '',
    location: '',
    updateServiceMetric: function(){}
  });

  describe('#getUrl()', function () {

    it('testMode = true', function () {
      App.set('testMode', true);
      expect(controller.getUrl('test', '/real')).to.equal('test');
    });

    it('testMode = false', function () {
      App.set('testMode', false);
      expect(controller.getUrl('test', '/real')).to.equal('/api/v1/clusters//real');
    });

    it('testMode = false', function () {
      App.set('testMode', false);
      controller.set('clusterName', 'mycluster');
      expect(controller.getUrl('test', '/real')).to.equal('/api/v1/clusters/mycluster/real');
    });
  });

  describe('#updateAll()', function () {

    beforeEach(function () {
      sinon.stub(App.updater, 'run', Em.K);
    });
    afterEach(function () {
      App.updater.run.restore();
    });
    it('isWorking = false', function () {
      controller.set('isWorking', false);
      expect(App.updater.run.called).to.equal(false);
    });

    it('isWorking = true', function () {
      controller.set('isWorking', true);
      expect(App.updater.run.callCount).to.equal(11);
    });
  });

  describe('#getConditionalFields()', function () {

    var testCases = [
      {
        title: 'No services exist',
        services: [],
        result: ['metrics/1']
      },
      {
        title: 'HDFS service',
        services: [
          {
            ServiceInfo: {
              service_name: 'HDFS'
            }
          }
        ],
        result: ['metrics/1']
      },
      {
        title: 'FLUME service',
        services: [
          {
            ServiceInfo: {
              service_name: 'FLUME'
            }
          }
        ],
        result: ['metrics/1', "host_components/processes/HostComponentProcess"]
      },
      {
        title: 'YARN service',
        services: [
          {
            ServiceInfo: {
              service_name: 'YARN'
            }
          }
        ],
        result: ['metrics/1', "host_components/metrics/yarn/Queue," +
        "host_components/metrics/yarn/ClusterMetrics/NumActiveNMs," +
        "host_components/metrics/yarn/ClusterMetrics/NumLostNMs," +
        "host_components/metrics/yarn/ClusterMetrics/NumUnhealthyNMs," +
        "host_components/metrics/yarn/ClusterMetrics/NumRebootedNMs," +
        "host_components/metrics/yarn/ClusterMetrics/NumDecommissionedNMs"]
      },
      {
        title: 'HBASE service',
        services: [
          {
            ServiceInfo: {
              service_name: 'HBASE'
            }
          }
        ],
        result: ['metrics/1', "host_components/metrics/hbase/master/IsActiveMaster," +
        "host_components/metrics/hbase/master/MasterStartTime," +
        "host_components/metrics/hbase/master/MasterActiveTime," +
        "host_components/metrics/hbase/master/AverageLoad," +
        "host_components/metrics/master/AssignmentManger/ritCount"]
      },
      {
        title: 'STORM service',
        services: [
          {
            ServiceInfo: {
              service_name: 'STORM'
            }
          }
        ],
        result: ['metrics/1', "metrics/api/v1/cluster/summary," +
        "metrics/api/v1/topology/summary," +
        "metrics/api/v1/nimbus/summary"]
      }
    ];

    var testCasesByStackVersion = [
      {
        title: 'STORM service stack 2.1',
        services: [
          {
            ServiceInfo: {
              service_name: 'STORM'
            }
          }
        ],
        stackVersionNumber: '2.1',
        result: ['metrics/1', "metrics/api/cluster/summary"]
      },
      {
        title: 'STORM service stack 2.2',
        services: [
          {
            ServiceInfo: {
              service_name: 'STORM'
            }
          }
        ],
        stackVersionNumber: '2.2',
        result: ['metrics/1', "metrics/api/v1/cluster/summary,metrics/api/v1/topology/summary"]
      },
      {
        title: 'STORM service stack 2.3',
        services: [
          {
            ServiceInfo: {
              service_name: 'STORM'
            }
          }
        ],
        stackVersionNumber: '2.3',
        result: ['metrics/1', "metrics/api/v1/cluster/summary,metrics/api/v1/topology/summary,metrics/api/v1/nimbus/summary"]
      }
    ];

    beforeEach(function () {
      this.mock = sinon.stub(App, 'get');
      controller.set('serviceComponentMetrics', ['metrics/1']);
    });
    afterEach(function () {
      this.mock.restore();
    });
    testCases.forEach(function (test) {
      it(test.title, function () {
        App.cache['services'] = test.services;
        this.mock.withArgs('router.clusterController.isServiceMetricsLoaded').returns(true);
        expect(controller.getConditionalFields()).to.eql(test.result);
      });
    });

    testCasesByStackVersion.forEach(function (test) {
      it(test.title, function () {
        App.cache['services'] = test.services;
        this.mock.withArgs('currentStackVersionNumber').returns(test.stackVersionNumber);
        this.mock.withArgs('router.clusterController.isServiceMetricsLoaded').returns(true);
        expect(controller.getConditionalFields()).to.eql(test.result);
      });
    });

    it('FLUME service, first load', function () {
      App.cache['services'] = [
        {
          ServiceInfo: {
            service_name: 'FLUME'
          }
        }
      ];
      this.mock.withArgs('router.clusterController.isServiceMetricsLoaded').returns(false);
      expect(controller.getConditionalFields()).to.eql(["host_components/processes/HostComponentProcess"]);
    });
  });

  describe("#getComplexUrl()", function () {
    beforeEach(function () {
      sinon.stub(App, 'get').returns('mock');
      sinon.stub(controller, 'computeParameters').returns('params');
    });
    afterEach(function () {
      App.get.restore();
      controller.computeParameters.restore();
    });
    it("queryParams is empty", function () {
      expect(controller.getComplexUrl('<parameters>')).to.equal('mock/clusters/mock');
    });
    it("queryParams is present", function () {
      var queryParams = [
        {
          type: "EQUAL",
          key: "key",
          value: "value"
        }
      ];
      expect(controller.getComplexUrl('<parameters>', queryParams)).to.equal('mock/clusters/mockparams&');
    });
  });

  describe("#addParamsToHostsUrl()", function () {
    beforeEach(function () {
      sinon.stub(App, 'get').returns('mock');
      sinon.stub(controller, 'computeParameters').returns('params');
    });
    afterEach(function () {
      App.get.restore();
      controller.computeParameters.restore();
    });
    it("", function () {
      expect(controller.addParamsToHostsUrl([], [], 'url')).to.equal('mock/clusters/mockurl&params&params');
    });
  });

  describe("#loadHostsMetric()", function () {
    beforeEach(function () {
      this.mock = sinon.stub(App.Service, 'find');
      sinon.stub(controller, 'computeParameters');
      sinon.stub(controller, 'addParamsToHostsUrl');
      sinon.stub(App.ajax, 'send');
    });
    afterEach(function () {
      App.Service.find.restore();
      controller.computeParameters.restore();
      controller.addParamsToHostsUrl.restore();
      App.ajax.send.restore();
    });
    it("AMBARI_METRICS is not started", function () {
      this.mock.returns(Em.Object.create({isStarted: false}));
      expect(controller.loadHostsMetric([])).to.be.null;
      expect(App.ajax.send.called).to.be.false;
    });
    it("AMBARI_METRICS is started", function () {
      this.mock.returns(Em.Object.create({isStarted: true}));
      expect(controller.loadHostsMetric([])).to.be.object;
      expect(App.ajax.send.calledOnce).to.be.true;
    });
  });

  describe("#loadHostsMetricSuccessCallback()", function () {
    beforeEach(function () {
      sinon.stub(App.hostsMapper, 'setMetrics');
    });
    afterEach(function () {
      App.hostsMapper.setMetrics.restore();
    });
    it("", function () {
      controller.loadHostsMetricSuccessCallback({});
      expect(App.hostsMapper.setMetrics.calledWith({})).to.be.true;
    });
  });

  describe('#updateUpgradeState()', function () {

    var cases = [
        {
          currentStateName: 'versions',
          parentStateName: 'stackAndUpgrade',
          upgradeIsNotFinished: true,
          isLoadUpgradeDataPending: true,
          loadUpgradeDataCallCount: 0,
          callbackCallCount: 1,
          title: 'stack versions page'
        },
        {
          currentStateName: 'stackUpgrade',
          parentStateName: 'admin',
          upgradeIsNotFinished: true,
          isLoadUpgradeDataPending: true,
          loadUpgradeDataCallCount: 0,
          callbackCallCount: 1,
          title: 'upgrade popup open'
        },
        {
          currentStateName: 'versions',
          parentStateName: 'admin',
          upgradeIsNotFinished: true,
          isLoadUpgradeDataPending: false,
          loadUpgradeDataCallCount: 1,
          callbackCallCount: 0,
          title: 'another page with \'versions\' name'
        },
        {
          currentStateName: 'versions',
          parentStateName: 'admin',
          upgradeIsNotFinished: false,
          isLoadUpgradeDataPending: false,
          loadUpgradeDataCallCount: 0,
          callbackCallCount: 1,
          title: 'another page with \'versions\' name, upgrade finished'
        },
        {
          currentStateName: 'versions',
          parentStateName: 'admin',
          upgradeIsNotFinished: true,
          isLoadUpgradeDataPending: true,
          loadUpgradeDataCallCount: 0,
          callbackCallCount: 1,
          title: 'another page with \'versions\' name, another update upgrade request not completed'
        },
        {
          currentStateName: 'services',
          parentStateName: 'stackAndUpgrade',
          upgradeIsNotFinished: true,
          isLoadUpgradeDataPending: false,
          loadUpgradeDataCallCount: 1,
          callbackCallCount: 0,
          title: 'another page from \'Stack and Versions\' section'
        },
        {
          currentStateName: 'services',
          parentStateName: 'stackAndUpgrade',
          upgradeIsNotFinished: false,
          isLoadUpgradeDataPending: false,
          loadUpgradeDataCallCount: 0,
          callbackCallCount: 1,
          title: 'another page from \'Stack and Versions\' section, upgrade finished'
        },
        {
          currentStateName: 'services',
          parentStateName: 'stackAndUpgrade',
          upgradeIsNotFinished: true,
          isLoadUpgradeDataPending: true,
          loadUpgradeDataCallCount: 0,
          callbackCallCount: 1,
          title: 'another page from \'Stack and Versions\' section, another update upgrade request not completed'
        },
        {
          currentStateName: 'widgets',
          parentStateName: 'dashboard',
          upgradeIsNotFinished: true,
          isLoadUpgradeDataPending: false,
          loadUpgradeDataCallCount: 1,
          callbackCallCount: 0,
          title: 'not \'Stack and Versions\' section'
        },
        {
          currentStateName: 'widgets',
          parentStateName: 'dashboard',
          upgradeIsNotFinished: false,
          isLoadUpgradeDataPending: false,
          loadUpgradeDataCallCount: 0,
          callbackCallCount: 1,
          title: 'not \'Stack and Versions\' section, upgrade finished'
        },
        {
          currentStateName: 'widgets',
          parentStateName: 'dashboard',
          upgradeIsNotFinished: true,
          isLoadUpgradeDataPending: true,
          loadUpgradeDataCallCount: 0,
          callbackCallCount: 1,
          title: 'not \'Stack and Versions\' section, another update upgrade request not completed'
        }
      ],
      mock = {
        callback: Em.K,
        loadUpgradeData: function () {
          return {
            done: Em.K
          };
        }
      },
      mainAdminStackAndUpgradeController = App.get('router.mainAdminStackAndUpgradeController'),
      appGetMock;

    beforeEach(function () {
      sinon.spy(mock, 'callback');
      sinon.spy(mock, 'loadUpgradeData');
      appGetMock = sinon.stub(App, 'get');
    });

    afterEach(function () {
      mock.callback.restore();
      mock.loadUpgradeData.restore();
      App.get.restore();
      appGetMock.restore();
    });

    cases.forEach(function (item) {
      it(item.title, function () {
        appGetMock.withArgs('router.mainAdminStackAndUpgradeController').returns(Em.Object.create({
          loadUpgradeData: mock.loadUpgradeData,
          isLoadUpgradeDataPending: item.isLoadUpgradeDataPending
        })).withArgs('upgradeIsNotFinished').returns(item.upgradeIsNotFinished)
          .withArgs('router.currentState.name').returns(item.currentStateName)
          .withArgs('router.currentState.parentState.name').returns(item.parentStateName);
        controller.updateUpgradeState(mock.callback);
        expect(mock.loadUpgradeData.callCount).to.equal(item.loadUpgradeDataCallCount);
        expect(mock.callback.callCount).to.equal(item.callbackCallCount);
      });
    });

  });

});
