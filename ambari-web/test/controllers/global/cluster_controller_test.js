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
require('controllers/global/cluster_controller');
require('models/host_component');
require('utils/http_client');
require('models/service');
require('models/host');
require('utils/ajax/ajax');

describe('App.clusterController', function () {
  var controller = App.ClusterController.create();
  App.Service.FIXTURES = [
    {service_name: 'NAGIOS'}
  ];

  describe('#updateLoadStatus()', function () {

    controller.set('dataLoadList', Em.Object.create({
      'item1': false,
      'item2': false
    }));

    it('when none item is loaded then width should be "width:0"', function () {
      expect(controller.get('clusterDataLoadedPercent')).to.equal('width:0');
    });
    it('when first item is loaded then isLoaded should be false', function () {
      controller.updateLoadStatus.call(controller, 'item1');
      expect(controller.get('isLoaded')).to.equal(false);
    });
    it('when first item is loaded then width should be "width:50%"', function () {
      controller.updateLoadStatus.call(controller, 'item1');
      expect(controller.get('clusterDataLoadedPercent')).to.equal('width:50%');
    });

    it('when all items are loaded then isLoaded should be true', function () {
      controller.updateLoadStatus.call(controller, 'item2');
      expect(controller.get('isLoaded')).to.equal(true);
    });
    it('when all items are loaded then width should be "width:100%"', function () {
      controller.updateLoadStatus.call(controller, 'item2');
      expect(controller.get('clusterDataLoadedPercent')).to.equal('width:100%');
    });
  });

  describe('#loadClusterName()', function () {

    beforeEach(function () {
      sinon.spy(App.ajax, 'send');
    });
    afterEach(function () {
      App.ajax.send.restore();
    });

    it('if clusterName is "mycluster" and reload is false then loadClusterName should return false', function () {
      controller.set('cluster', {Clusters: {cluster_name: 'mycluster'}});
      expect(controller.loadClusterName(false)).to.equal(false);
    });

    it('reload is true and clusterName is not empty', function () {
      controller.loadClusterName(true);
      expect(App.ajax.send.calledOnce).to.equal(true);
      expect(App.get('currentStackVersion')).to.equal('HDP-2.0.5');
    });

    it('reload is false and clusterName is empty', function () {
      controller.set('cluster', {Clusters: {cluster_name: ''}});
      controller.loadClusterName(false);
      expect(App.ajax.send.calledOnce).to.equal(true);
      expect(App.get('currentStackVersion')).to.equal('HDP-2.0.5');
    });


  });

  describe('#loadClusterNameSuccessCallback', function () {
    var test_data = {
      "items": [
        {
          "Clusters": {
            "cluster_name": "tdk",
            "version": "HDP-1.3.0"
          }
        }
      ]
    };
    it('Check cluster', function () {
      controller.loadClusterNameSuccessCallback(test_data);
      expect(controller.get('cluster.Clusters.cluster_name')).to.equal('tdk');
      expect(controller.get('cluster.Clusters.version')).to.equal('HDP-1.3.0');
      expect(App.get('clusterName')).to.equal('tdk');
    });
  });

  describe('#loadClusterNameErrorCallback', function () {
    controller.loadClusterNameErrorCallback();
    it('', function () {
      expect(controller.get('isLoaded')).to.equal(true);
    });
  });

  describe('#getServerClockSuccessCallback()', function () {
    var testCases = [
      {
        title: 'if server clock is 1 then currentServerTime should be 1000',
        data: {
          RootServiceComponents: {
            server_clock: 1
          }
        },
        result: 1000
      },
      {
        title: 'if server clock is 0 then currentServerTime should be 0',
        data: {
          RootServiceComponents: {
            server_clock: 0
          }
        },
        result: 0
      },
      {
        title: 'if server clock is 111111111111 then currentServerTime should be 111111111111000',
        data: {
          RootServiceComponents: {
            server_clock: 111111111111
          }
        },
        result: 111111111111000
      },
      {
        title: 'if server clock is 1111111111113 then currentServerTime should be 1111111111113',
        data: {
          RootServiceComponents: {
            server_clock: 1111111111113
          }
        },
        result: 1111111111113
      }
    ];
    var currentServerTime = App.get('currentServerTime');
    var clockDistance = App.get('clockDistance');

    testCases.forEach(function (test) {
      it(test.title, function () {
        controller.getServerClockSuccessCallback(test.data);
        expect(App.get('currentServerTime')).to.equal(test.result);
        App.set('clockDistance', clockDistance);
        App.set('currentServerTime', currentServerTime);
      });
    });
  });

  describe('#getUrl', function () {
    controller.set('clusterName', 'tdk');
    var tests = ['test1', 'test2', 'test3'];
    it('testMode = true', function () {
      App.testMode = true;
      tests.forEach(function (test) {
        expect(controller.getUrl(test, test)).to.equal(test);
      });
    });
    it('testMode = false', function () {
      App.testMode = false;
      tests.forEach(function (test) {
        expect(controller.getUrl(test, test)).to.equal(App.apiPrefix + '/clusters/' + controller.get('clusterName') + test);
      });
    });
  });

  describe('#gangliaUrl', function () {
    it('testMode = true', function () {
      App.testMode = true;
      expect(controller.get('gangliaUrl')).to.equal('http://gangliaserver/ganglia/?t=yes');
    });
    it('Ganglia service is absent', function () {
      App.testMode = false;
      controller.set('gangliaWebProtocol', '');
      expect(controller.get('gangliaUrl')).to.equal(null);
    });
    it('Ganglia doesn\'t  have any components', function () {
      App.store.load(App.Service, {
        id: 'GANGLIA',
        service_name: 'GANGLIA'
      });
      controller.set('gangliaWebProtocol', '');
      expect(controller.get('gangliaUrl')).to.equal(null);
      expect(controller.get('isGangliaInstalled')).to.equal(true);
    });
    it('Ganglia Server doesn\'t  have host', function () {
      App.store.load(App.HostComponent, {
        id: 'GANGLIA_SERVER_GANGLIA_host',
        component_name: 'GANGLIA_SERVER',
        service_id: 'GANGLIA',
        host_id: 'GANGLIA_host'
      });
      App.store.load(App.Service, {
        id: 'GANGLIA',
        service_name: 'GANGLIA',
        host_components: ['GANGLIA_SERVER_GANGLIA_host']
      });
      controller.set('gangliaWebProtocol', '');
      expect(controller.get('gangliaUrl')).to.equal(null);
    });
    it('Ganglia Server host is "GANGLIA_host"', function () {
      App.store.load(App.Host, {
        id: 'GANGLIA_host',
        host_name: 'GANGLIA_host',
        host_components: ['GANGLIA_SERVER_GANGLIA_host'],
        public_host_name: 'GANGLIA_host'
      });
      controller.set('gangliaWebProtocol', '');
      expect(controller.get('gangliaUrl')).to.equal("http://GANGLIA_host/ganglia");
    });
    it('singleNodeInstall = true', function () {
      App.set('singleNodeInstall', true);
      controller.set('gangliaWebProtocol', '');
      expect(controller.get('gangliaUrl')).to.equal("http://" + location.hostname + ":42080/ganglia");
    });
    it('singleNodeAlias is "alias"', function () {
      App.set('singleNodeAlias', 'alias');
      controller.set('gangliaWebProtocol', '');
      expect(controller.get('gangliaUrl')).to.equal("http://alias:42080/ganglia");
      App.set('singleNodeInstall', false);
      App.set('singleNodeAlias', '');
    });
  });

  describe('#nagiosUrl', function () {
    it('testMode = true', function () {
      App.testMode = true;
      controller.set('nagiosWebProtocol', '');
      expect(controller.get('nagiosUrl')).to.equal('http://nagiosserver/nagios');
    });
    it('Nagios service is absent', function () {
      App.testMode = false;
      controller.set('nagiosWebProtocol', '');
      expect(controller.get('nagiosUrl')).to.equal(null);
    });
    it('Nagios doesn\'t  have any components', function () {
      App.store.load(App.Service, {
        id: 'NAGIOS',
        service_name: 'NAGIOS'
      });
      controller.set('nagiosWebProtocol', '');
      expect(controller.get('nagiosUrl')).to.equal(null);
      expect(controller.get('isNagiosInstalled')).to.equal(true);
    });
    it('NAGIOS Server doesn\'t  have host', function () {
      App.store.load(App.HostComponent, {
        id: 'NAGIOS_SERVER_NAGIOS_host',
        component_name: 'NAGIOS_SERVER',
        service_id: 'NAGIOS',
        host_id: 'NAGIOS_host'
      });
      App.store.load(App.Service, {
        id: 'NAGIOS',
        service_name: 'NAGIOS',
        host_components: ['NAGIOS_SERVER_NAGIOS_host']
      });
      controller.set('nagiosWebProtocol', '');
      expect(controller.get('nagiosUrl')).to.equal(null);
    });
    it('NAGIOS Server host is "NAGIOS_host"', function () {
      App.store.load(App.Host, {
        id: 'NAGIOS_host',
        host_name: 'NAGIOS_host',
        host_components: ['NAGIOS_SERVER_NAGIOS_host'],
        public_host_name: 'NAGIOS_host'
      });
      controller.set('nagiosWebProtocol', '');
      expect(controller.get('nagiosUrl')).to.equal("http://NAGIOS_host/nagios");
    });
    it('singleNodeInstall = true', function () {
      App.set('singleNodeInstall', true);
      controller.set('nagiosWebProtocol', '');
      expect(controller.get('nagiosUrl')).to.equal("http://:42080/nagios");
    });
    it('singleNodeAlias is "alias"', function () {
      App.set('singleNodeAlias', 'alias');
      controller.set('nagiosWebProtocol', '');
      expect(controller.get('nagiosUrl')).to.equal("http://alias:42080/nagios");
    });
  });

  describe('#nagiosWebProtocol', function () {
    var testCases = [
      {
        title: 'if ambariProperties is null then nagiosWebProtocol should be "http"',
        data: null,
        result: 'http'
      },
      {
        title: 'if ambariProperties is empty object then nagiosWebProtocol should be "http"',
        data: {},
        result: 'http'
      },
      {
        title: 'if nagios.https is false then nagiosWebProtocol should be "http"',
        data: {'nagios.https': false},
        result: 'http'
      },
      {
        title: 'if nagios.https is true then nagiosWebProtocol should be "http"',
        data: {'nagios.https': true},
        result: 'https'
      }
    ];

    testCases.forEach(function (test) {
      it(test.title, function () {
        controller.set('ambariProperties', test.data);
        expect(controller.get('nagiosWebProtocol')).to.equal(test.result);
      });
    });
  });

  describe('#gangliaWebProtocol', function () {
    var testCases = [
      {
        title: 'if ambariProperties is null then nagiosWebProtocol should be "http"',
        data: null,
        result: 'http'
      },
      {
        title: 'if ambariProperties is empty object then nagiosWebProtocol should be "http"',
        data: {},
        result: 'http'
      },
      {
        title: 'if nagios.https is false then nagiosWebProtocol should be "http"',
        data: {'ganglia.https': false},
        result: 'http'
      },
      {
        title: 'if nagios.https is true then nagiosWebProtocol should be "http"',
        data: {'ganglia.https': true},
        result: 'https'
      }
    ];

    testCases.forEach(function (test) {
      it(test.title, function () {
        controller.set('ambariProperties', test.data);
        expect(controller.get('gangliaWebProtocol')).to.equal(test.result);
      });
    });
  });

  describe('#startPolling()', function () {

    beforeEach(function () {
      sinon.spy(App.updater, 'run');
    });
    afterEach(function () {
      App.updater.run.restore();
    });
    it('isWorking = false', function () {
      controller.set('isWorking', false);
      expect(App.updater.run.calledOnce).to.equal(false);
      expect(controller.startPolling()).to.equal(false);
    });

    it('isWorking = true', function () {
      controller.set('isWorking', true);
      expect(App.updater.run.calledOnce).to.equal(true);
      expect(controller.startPolling()).to.equal(true);
    });
  });

  describe('#deferServiceMetricsLoad()', function () {
    beforeEach(function () {
      sinon.spy(App.serviceMetricsMapper, 'map');
    });
    afterEach(function () {
      App.serviceMetricsMapper.map.restore();
    });
    it('json is null', function () {
      controller.set('serviceMetricsJson', {});
      controller.set('dataLoadList.serviceMetrics', false);
      controller.deferServiceMetricsLoad(null);
      expect(controller.get('serviceMetricsJson')).to.equal(null);
      expect(controller.get('dataLoadList.serviceMetrics')).to.equal(true);
      expect(App.serviceMetricsMapper.map.calledOnce).to.equal(true);
    });
    it('json is correct', function () {
      controller.deferServiceMetricsLoad({data: ''});
      expect(controller.get('serviceMetricsJson')).to.eql({data: ''});
    });
    it('json is correct and dataLoadList.hosts is true', function () {
      controller.set('serviceMetricsJson', {});
      controller.set('dataLoadList.serviceMetrics', false);
      controller.set('dataLoadList.hosts', true);
      controller.deferServiceMetricsLoad(null);
      expect(controller.get('dataLoadList.serviceMetrics')).to.equal(true);
      expect(App.serviceMetricsMapper.map.calledOnce).to.equal(true);
    });
  });

  describe('#clusterName', function () {
    var testCases = [
      {
        title: 'if cluster is null then clusterName should be null',
        data: null,
        result: null
      },
      {
        title: 'if cluster.Clusters.cluster_name is null then clusterName should be null',
        data: {
          Clusters: {
            cluster_name: null
          }
        },
        result: null
      },
      {
        title: 'if cluster.Clusters.cluster_name is null then clusterName should be null',
        data: {
          Clusters: {
            cluster_name: 'mycluster'
          }
        },
        result: 'mycluster'
      }
    ];

    testCases.forEach(function (test) {
      it(test.title, function () {
        controller.set('cluster', test.data);
        expect(controller.get('clusterName')).to.equal(test.result);
      });
    });
  });

});