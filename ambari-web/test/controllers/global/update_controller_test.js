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
      expect(App.updater.run.callCount).to.equal(10);
    });
  });

  describe('#updateServiceMetricConditionally()', function () {
    var context = {
      callback: function(){}
    };

    beforeEach(function () {
      sinon.spy(controller, 'updateServiceMetric');
      sinon.spy(context, 'callback');
    });
    afterEach(function () {
      controller.updateServiceMetric.restore();
      context.callback.restore();
    });

    it('location is empty', function () {
      controller.set('location', '');
      controller.updateServiceMetricConditionally(context.callback);
      expect(controller.updateServiceMetric.called).to.equal(false);
      expect(context.callback.called).to.equal(true);
    });
    it('location is "/main/hosts"', function () {
      controller.set('location', '/main/hosts');
      controller.updateServiceMetricConditionally(context.callback);
      expect(controller.updateServiceMetric.called).to.equal(false);
      expect(context.callback.called).to.equal(true);
    });
    it('location is "/main/dashboard"', function () {
      controller.set('location', '/main/dashboard');
      controller.updateServiceMetricConditionally(context.callback);
      expect(controller.updateServiceMetric.called).to.equal(true);
      expect(context.callback.called).to.equal(false);
    });
    it('location is "/main/services/HDFS"', function () {
      controller.set('location', '/main/services/HDFS');
      controller.updateServiceMetricConditionally(context.callback);
      expect(controller.updateServiceMetric.called).to.equal(true);
      expect(context.callback.called).to.equal(false);
    });
  });


  describe('#getConditionalFields()', function () {

    var testCases = [
      {
        title: 'No services exist',
        services: [],
        result: []
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
        result: []
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
        result: ["host_components/processes/HostComponentProcess"]
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
        result: ["host_components/metrics/yarn/Queue," +
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
        result: ["host_components/metrics/hbase/master/IsActiveMaster," +
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
        result: ["metrics/api/v1/cluster/summary," +
          "metrics/api/v1/topology/summary"]
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
        result: ["metrics/api/cluster/summary"]
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
        result: ["metrics/api/v1/cluster/summary,metrics/api/v1/topology/summary"]
      }
    ];
    testCases.forEach(function(test){
      it(test.title, function () {
        App.cache['services'] = test.services;
        expect(controller.getConditionalFields()).to.eql(test.result);
      });
    });

    testCasesByStackVersion.forEach(function(test) {
      it(test.title, function() {
        App.cache['services'] = test.services;
        sinon.stub(App, 'get', function(key) {
          if (key == 'currentStackVersionNumber') {
            return test.stackVersionNumber;
          }
        });
        expect(controller.getConditionalFields()).to.eql(test.result);
        App.get.restore();
      });
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
});
