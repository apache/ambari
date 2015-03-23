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
require('utils/string_utils');

var modelSetup = require('test/init_model_test');

describe('App.clusterController', function () {
  var controller = App.ClusterController.create();
  App.Service.FIXTURES = [
    {service_name: 'GANGLIA'}
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
      modelSetup.setupStackVersion(this, 'HDP-2.0.5');
      sinon.stub(App.ajax, 'send', function () {
        return {
          complete: function (callback) {
            App.set('clusterName', 'clusterNameFromServer');
            App.set('currentStackVersion', 'HDP-2.0.5');
            callback();
          }
        }
      });
    });
    afterEach(function () {
      modelSetup.restoreStackVersion(this);
      App.ajax.send.restore();
    });

    it('if clusterName is "mycluster" and reload is false then clusterName stays the same', function () {
      App.set('clusterName', 'mycluster');
      controller.loadClusterName(false);
      expect(App.ajax.send.called).to.be.false;
      expect(App.get('clusterName')).to.equal('mycluster');
    });

    it('reload is true and clusterName is not empty', function () {
      controller.loadClusterName(true);
      expect(App.ajax.send.calledOnce).to.be.true;
      expect(App.get('clusterName')).to.equal('clusterNameFromServer');
      expect(App.get('currentStackVersion')).to.equal('HDP-2.0.5');
    });

    it('reload is false and clusterName is empty', function () {
      App.set('clusterName', '');
      controller.loadClusterName(false);
      expect(App.ajax.send.calledOnce).to.be.true;
      expect(App.get('clusterName')).to.equal('clusterNameFromServer');
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
      expect(App.get('clusterName')).to.equal('tdk');
      expect(App.get('currentStackVersion')).to.equal('HDP-1.3.0');
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

  describe('#setGangliaUrl()', function () {
    beforeEach(function () {
      controller.set('gangliaUrl', null);
    });

    it('testMode = true', function () {
      App.testMode = true;
      controller.setGangliaUrl();
      expect(controller.get('gangliaUrl')).to.equal('http://gangliaserver/ganglia/?t=yes');
      expect(controller.get('isGangliaUrlLoaded')).to.be.true;
    });
    it('Cluster is not loaded', function () {
      App.testMode = false;
      controller.set('isLoaded', false);
      controller.setGangliaUrl();
      expect(controller.get('gangliaUrl')).to.equal(null);
    });
    it('GANGLIA_SERVER component is absent', function () {
      controller.set('isLoaded', true);
      App.testMode = false;
      sinon.stub(App.HostComponent, 'find', function(){
        return [];
      });
      controller.setGangliaUrl();
      expect(controller.get('gangliaUrl')).to.equal(null);
      App.HostComponent.find.restore();
    });
    it('Ganglia Server host is "GANGLIA_host"', function () {
      controller.set('isLoaded', true);
      App.testMode = false;
      sinon.stub(App.HostComponent, 'find', function(){
        return [Em.Object.create({
          componentName: 'GANGLIA_SERVER',
          hostName: 'GANGLIA_host'
        })];
      });
      sinon.spy(App.ajax, 'send');

      controller.setGangliaUrl();
      expect(App.ajax.send.calledOnce).to.be.true;
      expect(controller.get('isGangliaUrlLoaded')).to.be.false;

      App.HostComponent.find.restore();
      App.ajax.send.restore();
    });
  });

  describe('#gangliaWebProtocol', function () {
    var testCases = [
      {
        title: 'if ambariProperties is null then gangliaWebProtocol should be "http"',
        data: null,
        result: 'http'
      },
      {
        title: 'if ambariProperties is empty object then gangliaWebProtocol should be "http"',
        data: {},
        result: 'http'
      },
      {
        title: 'if ganglia.https is false then gangliaWebProtocol should be "http"',
        data: {'ganglia.https': false},
        result: 'http'
      },
      {
        title: 'if ganglia.https is true then gangliaWebProtocol should be "http"',
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


  describe('#setGangliaUrlSuccessCallback()', function () {

    it('Query return no hosts', function () {
      controller.setGangliaUrlSuccessCallback({items: []});
      expect(controller.get('gangliaUrl')).to.equal(null);
      expect(controller.get('isGangliaUrlLoaded')).to.be.true;
    });
    it('App.singleNodeInstall is true', function () {
      controller.reopen({
        gangliaWebProtocol: 'http'
      });
      App.set('singleNodeInstall', true);
      App.set('singleNodeAlias', 'localhost');
      controller.setGangliaUrlSuccessCallback({items: [{
        Hosts: {
          public_host_name: 'host1'
        }
      }]});
      expect(controller.get('gangliaUrl')).to.equal('http://localhost:42080/ganglia');
      expect(controller.get('isGangliaUrlLoaded')).to.be.true;
    });
    it('App.singleNodeInstall is false', function () {
      controller.reopen({
        gangliaWebProtocol: 'http'
      });
      App.set('singleNodeInstall', false);
      App.set('singleNodeAlias', 'localhost');
      controller.setGangliaUrlSuccessCallback({items: [{
        Hosts: {
          public_host_name: 'host1'
        }
      }]});
      expect(controller.get('gangliaUrl')).to.equal('http://host1/ganglia');
      expect(controller.get('isGangliaUrlLoaded')).to.be.true;
    });
  });

  describe("#createKerberosAdminSession()", function() {
    before(function () {
      sinon.stub(App.ajax, 'send', function() {
        return {success: Em.K}
      });
    });
    after(function () {
      App.ajax.send.restore();
    });
    it("make ajax call", function() {
      controller.createKerberosAdminSession("admin", "pass", {});
      expect(App.ajax.send.getCall(0).args[0]).to.eql({
        name: 'common.cluster.update',
        sender: controller,
        data: {
          clusterName: App.get('clusterName'),
          data: [{
            session_attributes: {
              kerberos_admin: {principal: "admin", password: "pass"}
            }
          }]
        }
      });
    });
  });

  describe('#checkDetailedRepoVersion()', function () {

    var cases = [
      {
        currentStackName: 'HDP',
        currentStackVersionNumber: '2.1',
        isStormMetricsSupported: false,
        title: 'HDP < 2.2'
      },
      {
        currentStackName: 'HDP',
        currentStackVersionNumber: '2.3',
        isStormMetricsSupported: true,
        title: 'HDP > 2.2'
      },
      {
        currentStackName: 'BIGTOP',
        currentStackVersionNumber: '0.8',
        isStormMetricsSupported: true,
        title: 'not HDP'
      }
    ];

    beforeEach(function () {
      sinon.stub(App.ajax, 'send').returns({
        promise: Em.K
      });
    });

    afterEach(function () {
      App.ajax.send.restore();
      App.get.restore();
    });

    it('should check detailed repo version for HDP 2.2', function () {
      sinon.stub(App, 'get').withArgs('currentStackName').returns('HDP').withArgs('currentStackVersionNumber').returns('2.2');
      controller.checkDetailedRepoVersion();
      expect(App.ajax.send.calledOnce).to.be.true;
    });

    cases.forEach(function (item) {
      it(item.title, function () {
        sinon.stub(App, 'get', function (key) {
          return item[key] || Em.get(App, key);
        });
        controller.checkDetailedRepoVersion();
        expect(App.ajax.send.called).to.be.false;
        expect(App.get('isStormMetricsSupported')).to.equal(item.isStormMetricsSupported);
      });
    });

  });

  describe('#checkDetailedRepoVersionSuccessCallback()', function () {

    var cases = [
      {
        items: [
          {
            repository_versions: [
              {
                RepositoryVersions: {
                  repository_version: '2.1'
                }
              }
            ]
          }
        ],
        isStormMetricsSupported: false,
        title: 'HDP < 2.2.2'
      },
      {
        items: [
          {
            repository_versions: [
              {
                RepositoryVersions: {
                  repository_version: '2.2.2'
                }
              }
            ]
          }
        ],
        isStormMetricsSupported: true,
        title: 'HDP 2.2.2'
      },
      {
        items: [
          {
            repository_versions: [
              {
                RepositoryVersions: {
                  repository_version: '2.2.3'
                }
              }
            ]
          }
        ],
        isStormMetricsSupported: true,
        title: 'HDP > 2.2.2'
      },
      {
        items: null,
        isStormMetricsSupported: true,
        title: 'empty response'
      },
      {
        items: [],
        isStormMetricsSupported: true,
        title: 'no items'
      },
      {
        items: [{}],
        isStormMetricsSupported: true,
        title: 'empty item'
      },
      {
        items: [{
          repository_versions: []
        }],
        isStormMetricsSupported: true,
        title: 'no versions'
      },
      {
        items: [{
          repository_versions: [{}]
        }],
        isStormMetricsSupported: true,
        title: 'no version info'
      },
      {
        items: [{
          repository_versions: [
            {
              RepositoryVersions: {}
            }
          ]
        }],
        isStormMetricsSupported: true,
        title: 'empty version info'
      }
    ];

    cases.forEach(function (item) {
      it(item.title, function () {
        controller.checkDetailedRepoVersionSuccessCallback({
          items: item.items
        });
        expect(App.get('isStormMetricsSupported')).to.equal(item.isStormMetricsSupported);
      });
    });

  });

  describe('#checkDetailedRepoVersionErrorCallback()', function () {
    it('should set isStormMetricsSupported to default value', function () {
      controller.checkDetailedRepoVersionErrorCallback();
      expect(App.get('isStormMetricsSupported')).to.be.true;
    });
  });

  describe('#getAllUpgrades()', function () {

    beforeEach(function () {
      sinon.stub(App.ajax, 'send', Em.K);
    });

    afterEach(function () {
      App.ajax.send.restore();
    });

    it('should send request to get upgrades data', function () {
      controller.getAllUpgrades();
      expect(App.ajax.send.calledOnce).to.be.true;
    });

  });

});
