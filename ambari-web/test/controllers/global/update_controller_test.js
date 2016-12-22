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
var testHelpers = require('test/helpers');
var c;

describe('App.UpdateController', function () {
  var controller = App.UpdateController.create({
    clusterName: '',
    location: '',
    updateServiceMetric: function(){}
  });

  beforeEach(function () {
    c = App.UpdateController.create();
    sinon.stub(App.HttpClient, 'get');
  });

  afterEach(function() {
    App.HttpClient.get.restore();
  });

  App.TestAliases.testAsComputedAlias(App.UpdateController.create(), 'clusterName', 'App.router.clusterController.clusterName', 'string');

  App.TestAliases.testAsComputedAnd(App.UpdateController.create(), 'updateAlertInstances', ['isWorking', '!App.router.mainAlertInstancesController.isUpdating']);

  describe('#getUrl()', function () {

    it('testMode = false', function () {
      expect(controller.getUrl('test', '/real')).to.equal('/api/v1/clusters//real');
    });

    it('testMode = false (2)', function () {
      controller.set('clusterName', 'mycluster');
      expect(controller.getUrl('test', '/real')).to.equal('/api/v1/clusters/mycluster/real');
    });
  });

  describe('#updateAll()', function () {

    it('isWorking = false', function () {
      controller.set('isWorking', false);
      expect(App.updater.run.called).to.equal(false);
    });

    it('isWorking = true', function () {
      controller.set('isWorking', true);
      expect(App.updater.run.callCount).to.equal(13);
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
        App.cache.services = test.services;
        this.mock.withArgs('router.clusterController.isServiceMetricsLoaded').returns(true);
        expect(controller.getConditionalFields()).to.eql(test.result);
      });
    });

    testCasesByStackVersion.forEach(function (test) {
      it(test.title, function () {
        App.cache.services = test.services;
        this.mock.withArgs('currentStackVersionNumber').returns(test.stackVersionNumber);
        this.mock.withArgs('router.clusterController.isServiceMetricsLoaded').returns(true);
        expect(controller.getConditionalFields()).to.eql(test.result);
      });
    });

    it('FLUME service, first load', function () {
      App.cache.services = [
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
    it("valid params are added", function () {
      expect(controller.addParamsToHostsUrl([], [], 'url')).to.equal('mock/clusters/mockurl&params&params');
    });
  });

  describe("#loadHostsMetric()", function () {
    beforeEach(function () {
      this.mock = sinon.stub(App.Service, 'find');
      sinon.stub(controller, 'computeParameters');
      sinon.stub(controller, 'addParamsToHostsUrl');
    });
    afterEach(function () {
      App.Service.find.restore();
      controller.computeParameters.restore();
      controller.addParamsToHostsUrl.restore();
    });
    it("AMBARI_METRICS is not started", function () {
      this.mock.returns(Em.Object.create({isStarted: false}));
      expect(controller.loadHostsMetric([])).to.be.null;
      var args = testHelpers.findAjaxRequest('name', 'hosts.metrics.lazy_load');
      expect(args).to.not.exists;
    });
    it("AMBARI_METRICS is started", function () {
      this.mock.returns(Em.Object.create({isStarted: true}));
      expect(controller.loadHostsMetric([])).to.be.object;
      var args = testHelpers.findAjaxRequest('name', 'hosts.metrics.lazy_load');
      expect(args).to.exists;
    });
  });

  describe("#loadHostsMetricSuccessCallback()", function () {
    beforeEach(function () {
      sinon.stub(App.hostsMapper, 'setMetrics');
    });
    afterEach(function () {
      App.hostsMapper.setMetrics.restore();
    });
    it("setMetrics called with valid arguments", function () {
      controller.loadHostsMetricSuccessCallback({});
      expect(App.hostsMapper.setMetrics.calledWith({})).to.be.true;
    });
  });

  describe('#updateUpgradeState()', function () {

    var cases = [
        {
          currentStateName: 'versions',
          parentStateName: 'stackAndUpgrade',
          wizardIsNotFinished: true,
          isLoadUpgradeDataPending: true,
          loadUpgradeDataCallCount: 0,
          callbackCallCount: 1,
          title: 'stack versions page'
        },
        {
          currentStateName: 'stackUpgrade',
          parentStateName: 'admin',
          wizardIsNotFinished: true,
          isLoadUpgradeDataPending: true,
          loadUpgradeDataCallCount: 0,
          callbackCallCount: 1,
          title: 'upgrade popup open'
        },
        {
          currentStateName: 'versions',
          parentStateName: 'admin',
          wizardIsNotFinished: true,
          isLoadUpgradeDataPending: false,
          loadUpgradeDataCallCount: 1,
          callbackCallCount: 0,
          title: 'another page with \'versions\' name'
        },
        {
          currentStateName: 'versions',
          parentStateName: 'admin',
          wizardIsNotFinished: false,
          isLoadUpgradeDataPending: false,
          loadUpgradeDataCallCount: 0,
          callbackCallCount: 1,
          title: 'another page with \'versions\' name, upgrade finished'
        },
        {
          currentStateName: 'versions',
          parentStateName: 'admin',
          wizardIsNotFinished: true,
          isLoadUpgradeDataPending: true,
          loadUpgradeDataCallCount: 0,
          callbackCallCount: 1,
          title: 'another page with \'versions\' name, another update upgrade request not completed'
        },
        {
          currentStateName: 'services',
          parentStateName: 'stackAndUpgrade',
          wizardIsNotFinished: true,
          isLoadUpgradeDataPending: false,
          loadUpgradeDataCallCount: 1,
          callbackCallCount: 0,
          title: 'another page from \'Stack and Versions\' section'
        },
        {
          currentStateName: 'services',
          parentStateName: 'stackAndUpgrade',
          wizardIsNotFinished: false,
          isLoadUpgradeDataPending: false,
          loadUpgradeDataCallCount: 0,
          callbackCallCount: 1,
          title: 'another page from \'Stack and Versions\' section, upgrade finished'
        },
        {
          currentStateName: 'services',
          parentStateName: 'stackAndUpgrade',
          wizardIsNotFinished: true,
          isLoadUpgradeDataPending: true,
          loadUpgradeDataCallCount: 0,
          callbackCallCount: 1,
          title: 'another page from \'Stack and Versions\' section, another update upgrade request not completed'
        },
        {
          currentStateName: 'widgets',
          parentStateName: 'dashboard',
          wizardIsNotFinished: true,
          isLoadUpgradeDataPending: false,
          loadUpgradeDataCallCount: 1,
          callbackCallCount: 0,
          title: 'not \'Stack and Versions\' section'
        },
        {
          currentStateName: 'widgets',
          parentStateName: 'dashboard',
          wizardIsNotFinished: false,
          isLoadUpgradeDataPending: false,
          loadUpgradeDataCallCount: 0,
          callbackCallCount: 1,
          title: 'not \'Stack and Versions\' section, upgrade finished'
        },
        {
          currentStateName: 'widgets',
          parentStateName: 'dashboard',
          wizardIsNotFinished: true,
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
      describe(item.title, function () {

        beforeEach(function () {
          appGetMock.withArgs('router.mainAdminStackAndUpgradeController').returns(Em.Object.create({
            loadUpgradeData: mock.loadUpgradeData,
            isLoadUpgradeDataPending: item.isLoadUpgradeDataPending
          })).withArgs('wizardIsNotFinished').returns(item.wizardIsNotFinished)
            .withArgs('router.currentState.name').returns(item.currentStateName)
            .withArgs('router.currentState.parentState.name').returns(item.parentStateName);
          controller.updateUpgradeState(mock.callback);
        });
        it('loadUpgradeData is called ' + item.loadUpgradeDataCallCount + ' times', function () {
          expect(mock.loadUpgradeData.callCount).to.equal(item.loadUpgradeDataCallCount);
        });
        it('callback is called ' + item.callbackCallCount + ' times', function () {
          expect(mock.callback.callCount).to.equal(item.callbackCallCount);
        });

      });
    });

  });

  describe('#computeParameters', function () {

    beforeEach(function() {
      sinon.stub(App.router.get('mainHostComboSearchBoxController'), 'generateQueryParam').returns('combo');
    });

    afterEach(function() {
      App.router.get('mainHostComboSearchBoxController').generateQueryParam.restore();
    });

    Em.A([
      {
        q: [{
          type: 'EQUAL',
          key: 'k',
          value: [1, 2]
        }],
        result: 'k.in(1,2)'
      },
      {
        q: [{
          type: 'CUSTOM',
          key: '{0} - {1}',
          value: [1, 2]
        }],
        result: '1 - 2'
      },
      {
        q: [{
          type: 'COMBO',
          key: '',
          value: []
        }],
        result: 'combo'
      },
      {
        q: [{
          type: 'MULTIPLE',
          key: 'k',
          value: [1, 2]
        }],
        result: 'k.in(1,2)'
      },
      {
        q: [{
          type: 'EQUAL',
          key: 'k',
          value: 1
        }],
        result: 'k=1'
      },
      {
        q: [
          {
            type: 'LESS',
            key: 'k',
            value: '1'
          }
        ],
        result: 'k<1'
      },
      {
        q: [
          {
            type: 'MORE',
            key: 'k',
            value: '1'
          }
        ],
        result: 'k>1'
      },
      {
        q: [
          {
            type: 'SORT',
            key: 'k',
            value: 'f'
          }
        ],
        result: 'sortBy=k.f'
      },
      {
        q: [
          {
            type: 'MATCH',
            key: 'k',
            value: 'abc'
          }
        ],
        result: 'k.matches(abc)'
      },
      {
        q: [
          {
            type: 'MATCH',
            key: 'k',
            value: ['a', 'b', 'c']
          }
        ],
        result: '(k.matches(a)|k.matches(b)|k.matches(c))'
      },
      {
        q: [
          {type: 'EQUAL', key: 'k1', value: [1,2]},
          {type: 'EQUAL', key: 'k2', value: 'abc'},
          {type: 'LESS', key: 'k3', value: 1},
          {type: 'MORE', key: 'k4', value: 1},
          {type: 'MATCH', key: 'k5', value: ['a', 'b', 'c']}
        ],
        result: 'k1.in(1,2)&k2=abc&k3<1&k4>1&(k5.matches(a)|k5.matches(b)|k5.matches(c))'
      }
    ]).forEach(function (test, index) {

      it('test#' + index, function () {
        var result = c.computeParameters(test.q);
        expect(result).to.be.equal(test.result);
      });
    });
  });

  describe('#preLoadHosts()', function() {

    beforeEach(function() {
      sinon.stub(c, 'getHostByHostComponents');
    });

    afterEach(function() {
      c.getHostByHostComponents.restore();
    });

    it('getHostByHostComponents should be called', function() {
      c.set('queryParams.Hosts', [{isComponentRelatedFilter: true}]);
      expect(c.preLoadHosts(Em.K)).to.be.true;
      expect(c.getHostByHostComponents.calledOnce).to.be.true;
    });

    it('getHostByHostComponents should not be called', function() {
      c.set('queryParams.Hosts', []);
      expect(c.preLoadHosts(Em.K)).to.be.false;
      expect(c.getHostByHostComponents.calledOnce).to.be.false;
    });
  });

  describe('#getHostByHostComponents', function() {

    it('App.ajax.send should be called', function() {
      var args = testHelpers.findAjaxRequest('name', 'hosts.host_components.pre_load');
      expect(args).to.exists;
    });
  });

  describe('#updateServices()', function() {
    it('App.HttpClient.get should be called', function() {
      c.updateServices();
      expect(App.HttpClient.get.calledOnce).to.be.true;
    });
  });

  describe('#updateComponentConfig()', function() {
    it('App.HttpClient.get should be called', function() {
      c.updateComponentConfig();
      expect(App.HttpClient.get.calledOnce).to.be.true;
    });
  });

  describe('#updateComponentsState()', function() {
    it('App.HttpClient.get should be called', function() {
      c.updateComponentsState();
      expect(App.HttpClient.get.calledOnce).to.be.true;
    });
  });

  describe('#updateAlertDefinitions()', function() {
    it('App.HttpClient.get should be called', function() {
      c.updateAlertDefinitions();
      expect(App.HttpClient.get.calledOnce).to.be.true;
    });
  });

  describe('#updateUnhealthyAlertInstances()', function() {
    it('App.HttpClient.get should be called', function() {
      c.updateUnhealthyAlertInstances();
      expect(App.HttpClient.get.calledOnce).to.be.true;
    });
  });

  describe('#updateAlertDefinitionSummary()', function() {
    it('App.HttpClient.get should be called', function() {
      c.updateAlertDefinitionSummary();
      expect(App.HttpClient.get.calledOnce).to.be.true;
    });
  });

  describe('#updateAlertGroups()', function() {
    it('App.HttpClient.get should be called', function() {
      c.updateAlertGroups();
      expect(App.HttpClient.get.calledOnce).to.be.true;
    });
  });

  describe('#updateAlertNotifications()', function() {
    it('App.HttpClient.get should be called', function() {
      c.updateAlertNotifications();
      expect(App.HttpClient.get.calledOnce).to.be.true;
    });
  });

  describe('#loadClusterConfig()', function() {

    it('App.ajax.send should be called', function() {
      c.loadClusterConfig();
      var args = testHelpers.findAjaxRequest('name', 'config.tags.site');
      expect(args).to.exists;
    });
  });
});
