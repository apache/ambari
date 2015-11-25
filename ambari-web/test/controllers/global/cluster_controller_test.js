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
var credentialUtils = require('utils/credentials');
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

  describe("#createKerberosAdminSession()", function() {
    beforeEach(function () {
      sinon.stub(App.ajax, 'send', function() {
        return {success: Em.K}
      });
      sinon.stub(credentialUtils, 'createOrUpdateCredentials', function() {
        return $.Deferred().resolve().promise();
      });
    });
    afterEach(function () {
      App.ajax.send.restore();
      credentialUtils.createOrUpdateCredentials.restore();
    });
    it("KDC Store supports disabled, credentials updated via kdc session call", function() {
      sinon.stub(App, 'get')
        .withArgs('supports.storeKDCCredentials').returns(false)
        .withArgs('clusterName').returns('test');
      controller.createKerberosAdminSession({
        principal: 'admin',
        key: 'pass',
        type: 'persistent'
      }, {});
      App.get.restore();
      expect(App.ajax.send.getCall(0).args[0]).to.eql({
        name: 'common.cluster.update',
        sender: controller,
        data: {
          clusterName: 'test',
          data: [{
            session_attributes: {
              kerberos_admin: {principal: "admin", password: "pass"}
            }
          }]
        }
      });
    });
    it("KDC Store supports enabled, credentials updated via credentials storage call", function() {
      sinon.stub(App, 'get')
        .withArgs('supports.storeKDCCredentials').returns(true)
        .withArgs('clusterName').returns('test');
      controller.createKerberosAdminSession({
        principal: 'admin',
        key: 'pass',
        type: 'persistent'
      }, {});
      App.get.restore();
      expect(App.ajax.send.called).to.be.eql(false);
      expect(credentialUtils.createOrUpdateCredentials.getCall(0).args).to.eql([
        'test', 'kdc.admin.credential', {
          principal: 'admin',
          key: 'pass',
          type: 'persistent'
        }
      ]);
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

  describe("#restoreUpgradeState()", function() {
    var data = {upgradeData: {}};
    var mock = {done: function(callback) {
      callback(data.upgradeData);
    }};
    var upgradeController = Em.Object.create({
      restoreLastUpgrade: Em.K,
      initDBProperties: Em.K,
      loadUpgradeData: Em.K,
      loadStackVersionsToModel: function(){return {done: Em.K};}
    });

    beforeEach(function () {
      sinon.stub(controller, 'getAllUpgrades').returns(mock);
      sinon.spy(mock, 'done');
      sinon.stub(App.router, 'get').returns(upgradeController);
      sinon.stub(App.db, 'get').returns('PENDING');
      sinon.spy(upgradeController, 'restoreLastUpgrade');
      sinon.spy(upgradeController, 'initDBProperties');
      sinon.spy(upgradeController, 'loadUpgradeData');
      sinon.spy(upgradeController, 'loadStackVersionsToModel');
    });
    afterEach(function () {
      mock.done.restore();
      controller.getAllUpgrades.restore();
      App.router.get.restore();
      App.db.get.restore();
      upgradeController.restoreLastUpgrade.restore();
      upgradeController.initDBProperties.restore();
      upgradeController.loadUpgradeData.restore();
      upgradeController.loadStackVersionsToModel.restore();
    });
    it("has upgrade request", function() {
      data.upgradeData = {items: [
        {
          Upgrade: {
            request_id: 1
          }
        }
      ]};
      controller.restoreUpgradeState();
      expect(controller.getAllUpgrades.calledOnce).to.be.true;
      expect(App.get('upgradeState')).to.equal('PENDING');
      expect(upgradeController.restoreLastUpgrade.calledWith(data.upgradeData.items[0])).to.be.true;
      expect(upgradeController.loadStackVersionsToModel.calledWith(true)).to.be.true;
      expect(upgradeController.initDBProperties.called).to.be.false;
      expect(upgradeController.loadUpgradeData.called).to.be.false;
    });
    it("does not have upgrade request", function() {
      data.upgradeData = {items: []};
      controller.restoreUpgradeState();
      expect(controller.getAllUpgrades.calledOnce).to.be.true;
      expect(App.get('upgradeState')).to.equal('PENDING');
      expect(upgradeController.restoreLastUpgrade.called).to.be.false;
      expect(upgradeController.loadStackVersionsToModel.calledWith(true)).to.be.true;
      expect(upgradeController.initDBProperties.calledOnce).to.be.true;
      expect(upgradeController.loadUpgradeData.calledWith(true)).to.be.true;
    });
  });
});
