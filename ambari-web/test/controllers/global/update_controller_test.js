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
      sinon.spy(App.updater, 'run');
    });
    afterEach(function () {
      App.updater.run.restore();
    });
    it('isWorking = false', function () {
      controller.set('isWorking', false);
      expect(App.updater.run.called).to.equal(false);
    });

    it('isWorking = true, App.supports.hostOverrides = false', function () {
      App.supports.hostOverrides = false;
      controller.set('isWorking', true);
      expect(App.updater.run.callCount).to.equal(4);
      controller.set('isWorking', false);
    });

    it('isWorking = true, App.supports.hostOverrides = true', function () {
      App.supports.hostOverrides = true;
      controller.set('isWorking', true);
      expect(App.updater.run.callCount).to.equal(5);
    });
  });

  describe('#updateHostConditionally()', function () {
    var context = {
      callback: function(){}
    };

    beforeEach(function () {
      sinon.spy(controller, 'updateHost');
      sinon.spy(context, 'callback');
    });
    afterEach(function () {
      controller.updateHost.restore();
      context.callback.restore();
    });

    it('location is empty', function () {
      controller.set('location', '');
      controller.updateHostConditionally(context.callback);
      expect(controller.updateHost.called).to.equal(false);
      expect(context.callback.called).to.equal(true);
    });
    it('location is "/main/dashboard"', function () {
      controller.set('location', '/main/dashboard');
      controller.updateHostConditionally(context.callback);
      expect(controller.updateHost.called).to.equal(false);
      expect(context.callback.called).to.equal(true);
    });
    it('location is "/main/hosts"', function () {
      controller.set('location', '/main/hosts');
      controller.updateHostConditionally(context.callback);
      expect(controller.updateHost.called).to.equal(true);
      expect(context.callback.called).to.equal(false);
    });
    it('location is "/main/charts/heatmap"', function () {
      controller.set('location', '/main/charts/heatmap');
      controller.updateHostConditionally(context.callback);
      expect(controller.updateHost.called).to.equal(true);
      expect(context.callback.called).to.equal(false);
    });
    it('location is "/main/hosts/host1"', function () {
      controller.set('location', '/main/hosts/host1');
      controller.updateHostConditionally(context.callback);
      expect(controller.updateHost.called).to.equal(true);
      expect(context.callback.called).to.equal(false);
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
        result: ["host_components/metrics/flume/flume"]
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
          "ServiceComponentInfo/rm_metrics/cluster/activeNMcount," +
          "ServiceComponentInfo/rm_metrics/cluster/unhealthyNMcount," +
          "ServiceComponentInfo/rm_metrics/cluster/rebootedNMcount," +
          "ServiceComponentInfo/rm_metrics/cluster/decommissionedNMcount"]
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
          "ServiceComponentInfo/MasterStartTime," +
          "ServiceComponentInfo/MasterActiveTime," +
          "ServiceComponentInfo/AverageLoad," +
          "ServiceComponentInfo/Revision," +
          "ServiceComponentInfo/RegionsInTransition"]
      },
      {
        title: 'MAPREDUCE service',
        services: [
          {
            ServiceInfo: {
              service_name: 'MAPREDUCE'
            }
          }
        ],
        result: ["ServiceComponentInfo/AliveNodes," +
          "ServiceComponentInfo/GrayListedNodes," +
          "ServiceComponentInfo/BlackListedNodes," +
          "ServiceComponentInfo/jobtracker/*,"]
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
        result: ["metrics/api/cluster/summary,"]
      }
    ];

    testCases.forEach(function(test){
      it(test.title, function () {
        App.cache['services'] = test.services;
        expect(controller.getConditionalFields()).to.eql(test.result);
      });
    });
  });
});