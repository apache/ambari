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
var testHelpers = require('test/helpers');
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

  App.TestAliases.testAsComputedAnd(controller, 'isHostContentLoaded', ['isHostsLoaded', 'isComponentsStateLoaded']);

  App.TestAliases.testAsComputedAnd(controller, 'isServiceContentFullyLoaded', ['isServiceMetricsLoaded', 'isComponentsStateLoaded', 'isComponentsConfigLoaded']);

  App.TestAliases.testAsComputedAlias(controller, 'clusterName', 'App.clusterName', 'string');

  describe('#updateLoadStatus()', function () {

    controller.set('dataLoadList', Em.Object.create({
      'item1': false,
      'item2': false
    }));

    it('when none item is loaded then width should be "width:0"', function () {
      expect(controller.get('clusterDataLoadedPercent')).to.equal('width:0');
    });
    it('when first item is loaded then isLoaded should be false', function () {
      controller.updateLoadStatus('item1');
      expect(controller.get('isLoaded')).to.equal(false);
    });
    it('when first item is loaded then width should be "width:50%"', function () {
      controller.updateLoadStatus('item1');
      expect(controller.get('clusterDataLoadedPercent')).to.equal('width:50%');
    });

    it('when all items are loaded then isLoaded should be true', function () {
      controller.updateLoadStatus('item2');
      expect(controller.get('isLoaded')).to.equal(true);
    });
    it('when all items are loaded then width should be "width:100%"', function () {
      controller.updateLoadStatus('item2');
      expect(controller.get('clusterDataLoadedPercent')).to.equal('width:100%');
    });
  });

  describe('#loadClusterName()', function () {

    beforeEach(function () {
      modelSetup.setupStackVersion(this, 'HDP-2.0.5');
      App.ajax.send.restore(); // default ajax-mock can't be used here
      sinon.stub(App.ajax, 'send', function () {
        return {
          then: function (successCallback) {
            App.set('clusterName', 'clusterNameFromServer');
            App.set('currentStackVersion', 'HDP-2.0.5');
            successCallback();
          }
        }
      });
      this.args = testHelpers.findAjaxRequest('name', 'cluster.load_cluster_name');
    });
    afterEach(function () {
      modelSetup.restoreStackVersion(this);
    });

    it('if clusterName is "mycluster" and reload is false then clusterName stays the same', function () {
      App.set('clusterName', 'mycluster');
      controller.loadClusterName(false);
      expect(this.args).to.not.exists;
      expect(App.get('clusterName')).to.equal('mycluster');
    });

    it('reload is true and clusterName is not empty', function () {
      controller.loadClusterName(true);
      expect(this.args).to.exists;
      expect(App.get('clusterName')).to.equal('clusterNameFromServer');
      expect(App.get('currentStackVersion')).to.equal('HDP-2.0.5');
    });

    it('reload is false and clusterName is empty', function () {
      App.set('clusterName', '');
      controller.loadClusterName(false);
      expect(this.args).to.exists;
      expect(App.get('clusterName')).to.equal('clusterNameFromServer');
      expect(App.get('currentStackVersion')).to.equal('HDP-2.0.5');
    });


  });

  describe('#reloadSuccessCallback', function () {
    var testData = {
      "items": [
        {
          "Clusters": {
            "cluster_name": "tdk",
            "version": "HDP-1.3.0",
            "security_type": "KERBEROS",
            "cluster_id": 1
          }
        }
      ]
    };
    it('Check cluster', function () {
      controller.reloadSuccessCallback(testData);
      expect(App.get('clusterName')).to.equal('tdk');
      expect(App.get('clusterId')).to.equal(1);
      expect(App.get('isKerberosEnabled')).to.be.true;
      expect(App.get('currentStackVersion')).to.equal('HDP-1.3.0');
    });
  });

  describe('#setServerClock()', function () {
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
        controller.setServerClock(test.data);
        expect(App.get('currentServerTime')).to.equal(test.result);
        App.set('clockDistance', clockDistance);
        App.set('currentServerTime', currentServerTime);
      });
    });
  });

  describe('#getUrl', function () {
    controller.set('clusterName', 'tdk');
    var tests = ['test1', 'test2', 'test3'];

    tests.forEach(function (test) {
      it(test, function () {
        expect(controller.getUrl(test, test)).to.equal(App.apiPrefix + '/clusters/' + controller.get('clusterName') + test);
      });
    });
  });

  describe("#createKerberosAdminSession()", function() {

    beforeEach(function () {
      sinon.stub(credentialUtils, 'createOrUpdateCredentials', function() {
        return $.Deferred().resolve().promise();
      });
      this.stub = sinon.stub(App, 'get');
      this.stub.withArgs('clusterName').returns('test');
    });

    afterEach(function () {
      credentialUtils.createOrUpdateCredentials.restore();
      App.get.restore();
    });

    it("credentials updated via credentials storage call", function() {
      controller.createKerberosAdminSession({
        principal: 'admin',
        key: 'pass',
        type: 'persistent'
      }, {});
      var args = testHelpers.findAjaxRequest('name', 'common.cluster.update');
      expect(args).to.not.exists;
      expect(credentialUtils.createOrUpdateCredentials.getCall(0).args).to.eql([
        'test', 'kdc.admin.credential', {
          principal: 'admin',
          key: 'pass',
          type: 'persistent'
        }
      ]);
    });
  });

  describe('#getAllUpgrades()', function () {

    it('should send request to get upgrades data', function () {
      controller.getAllUpgrades();
      var args = testHelpers.findAjaxRequest('name', 'cluster.load_last_upgrade');
      expect(args).to.exists;
    });

  });

  describe('#isRunningState()', function() {
    var testCases = [
      {
        status: '',
        expected: true
      },
      {
        status: 'IN_PROGRESS',
        expected: true
      },
      {
        status: 'PENDING',
        expected: true
      },
      {
        status: 'FAILED_HOLDING',
        expected: true
      },
      {
        status: 'ABORTED',
        expected: false
      }
    ];

    testCases.forEach(function(test) {
      it('status = ' + test.status, function() {
        expect(controller.isRunningState(test.status)).to.be.equal(test.expected);
      });
    });
  });

  describe('#isSuspendedState()', function() {

    it('should return true when status is ABORTED', function() {
      expect(controller.isSuspendedState('ABORTED')).to.be.true;
    });
  });

  describe('#loadRootService()', function() {

    it('App.ajax.send should be called', function() {
      controller.loadRootService();
      var args = testHelpers.findAjaxRequest('name', 'service.ambari');
      expect(args).to.exist;
    });
  });

  describe('#requestHosts()', function() {

    beforeEach(function() {
      sinon.stub(App.HttpClient, 'get');
    });

    afterEach(function() {
      App.HttpClient.get.restore();
    });

    it('App.HttpClient.get should be called', function() {
      controller.requestHosts();
      expect(App.HttpClient.get).to.be.calledOnce;
    });
  });


  describe('#loadStackServiceComponents()', function() {

    it('App.ajax.send should be called', function() {
      var stacks = [{stackUrl: '', stackVersion: ''}];
      var stackPosition = 0;
      var callback = {};
      controller.loadStackServiceComponents(stacks, callback, stackPosition);
      var args = testHelpers.findAjaxRequest('name', 'wizard.service_components');
      expect(args).to.exist;
    });
  });

  describe('#loadAmbariProperties()', function() {

    it('App.ajax.send should be called', function() {
      controller.loadAmbariProperties();
      var args = testHelpers.findAjaxRequest('name', 'ambari.service');
      expect(args).to.exist;
    });
  });

  describe('#loadAuthorizations()', function() {

    it('App.ajax.send should be called', function() {
      controller.loadAuthorizations();
      var args = testHelpers.findAjaxRequest('name', 'router.user.authorizations');
      expect(args).to.exist;
    });
  });

  describe('#loadAuthorizationsSuccessCallback()', function() {
    var auth = App.get('auth');

    beforeEach(function() {
      sinon.stub(App.db, 'setAuth');
    });

    afterEach(function() {
      App.db.setAuth.restore();
      App.set('auth', auth);
    });

    it('App.db.setAuth should not be called when response is null', function() {
      controller.loadAuthorizationsSuccessCallback(null);
      expect(App.db.setAuth).to.not.be.called;
    });

    it('App.db.setAuth should not be called when response has no items', function() {
      controller.loadAuthorizationsSuccessCallback({items: null});
      expect(App.db.setAuth).to.not.be.called;
    });

    it('App.db.setAuth should be called when response correct', function() {
      controller.loadAuthorizationsSuccessCallback({items: [
        {
          AuthorizationInfo: {
            authorization_id: 'admin'
          }
        },
        {
          AuthorizationInfo: {
            authorization_id: 'admin'
          }
        }
      ]});
      expect(App.get('auth')).to.be.eql(['admin']);
      expect(App.db.setAuth.calledWith(['admin'])).to.be.true;
    });
  });

  describe('#loadAmbariPropertiesSuccess()', function() {
    var data = {
      RootServiceComponents: {
        properties: {
          p1: '1'
        }
      }
    };

    beforeEach(function() {
      sinon.stub(App.router.get('mainController'), 'monitorInactivity');
      sinon.stub(App.router.get('mainController'), 'setAmbariServerVersion');
      sinon.stub(controller, 'setServerClock');
      controller.loadAmbariPropertiesSuccess(data);
    });

    afterEach(function() {
      App.router.get('mainController').monitorInactivity.restore();
      App.router.get('mainController').setAmbariServerVersion.restore();
      controller.setServerClock.restore();
    });

    it('should set ambariProperties', function() {
      expect(controller.get('ambariProperties')).to.be.eql({p1: '1'});
    });

    it('should set isCustomJDK', function() {
      expect(controller.get('isCustomJDK')).to.be.true;
    });

    it('monitorInactivity should be called', function() {
      expect(App.router.get('mainController').monitorInactivity).to.be.calledOnce;
    });

    it('setAmbariServerVersion should be called', function() {
      expect(App.router.get('mainController').setAmbariServerVersion.calledWith(data)).to.be.true;
    });

    it('setServerClock should be called', function() {
      expect(controller.setServerClock.calledWith(data)).to.be.true;
    });
  });

  describe('#updateClusterData()', function() {

    beforeEach(function() {
      sinon.stub(App.HttpClient, 'get');
    });

    afterEach(function() {
      App.HttpClient.get.restore();
    });

    it('App.HttpClient.get should be called', function() {
      controller.updateClusterData();
      expect(App.HttpClient.get).to.be.calledOnce;
    });
  });

  describe('#getAllHostNames()', function() {

    it('App.ajax.send should be called', function() {
      controller.getAllHostNames();
      var args = testHelpers.findAjaxRequest('name', 'hosts.all');
      expect(args).to.exist;
    });
  });

  describe('#getHostNamesSuccess()', function() {

    it('should set allHostNames', function() {
      controller.getHostNamesSuccess({
        items: [
          {
            Hosts: {
              host_name: 'host1'
            }
          }
        ]
      });
      expect(App.get('allHostNames')).to.be.eql(['host1']);
    });
  });

  describe('#createKerberosAdminSession()', function() {

    beforeEach(function() {
      sinon.stub(credentialUtils, 'createOrUpdateCredentials').returns({
        then: Em.clb
      });
    });

    afterEach(function() {
      credentialUtils.createOrUpdateCredentials.restore();
    });

    it('credentialUtils.createOrUpdateCredentials should be called', function() {
      controller.createKerberosAdminSession({}, {});
      expect(credentialUtils.createOrUpdateCredentials).to.be.calledOnce;
    });
  });

  describe('#getAllUpgrades()', function() {

    it('App.ajax.send should be called', function() {
      controller.getAllUpgrades();
      var args = testHelpers.findAjaxRequest('name', 'cluster.load_last_upgrade');
      expect(args).to.exist;
    });
  });

  describe('#triggerQuickLinksUpdate()', function() {

    it('should increment quickLinksUpdateCounter', function() {
      controller.set('quickLinksUpdateCounter', 0);
      controller.triggerQuickLinksUpdate();
      expect(controller.get('quickLinksUpdateCounter')).to.be.equal(1);
    });
  });

  describe('#loadClusterData()', function() {

    beforeEach(function() {
      sinon.stub(controller, 'loadAuthorizations');
      sinon.stub(controller, 'getAllHostNames');
      sinon.stub(controller, 'loadClusterInfo');
      sinon.stub(controller, 'loadClusterDataToModel');
      sinon.stub(App.router.get('mainController'), 'startPolling');
      sinon.stub(App.router.get('userSettingsController'), 'getAllUserSettings');
      sinon.stub(App.router.get('errorsHandlerController'), 'loadErrorLogs');
      sinon.stub(App.router.get('wizardWatcherController'), 'getUser');
      sinon.stub(App.db, 'setFilterConditions');
      sinon.stub(App.router.get('updateController'), 'updateClusterEnv');
      controller.set('isLoaded', false);
    });

    afterEach(function() {
      App.router.get('updateController').updateClusterEnv.restore();
      App.db.setFilterConditions.restore();
      App.router.get('wizardWatcherController').getUser.restore();
      App.router.get('errorsHandlerController').loadErrorLogs.restore();
      App.router.get('userSettingsController').getAllUserSettings.restore();
      App.router.get('mainController').startPolling.restore();
      controller.loadAuthorizations.restore();
      controller.getAllHostNames.restore();
      controller.loadClusterInfo.restore();
      controller.loadClusterDataToModel.restore();
    });

    it('loadAuthorizations should be called', function() {
      controller.loadClusterData();
      expect(controller.loadAuthorizations.calledOnce).to.be.true;
    });

    it('getAllHostNames should be called', function() {
      controller.loadClusterData();
      expect(controller.getAllHostNames.calledOnce).to.be.true;
    });

    it('getAllUserSettings should be called', function() {
      controller.loadClusterData();
      expect(App.router.get('userSettingsController').getAllUserSettings.calledOnce).to.be.true;
    });

    it('loadErrorLogs should be called', function() {
      controller.loadClusterData();
      expect(App.router.get('errorsHandlerController').loadErrorLogs.calledOnce).to.be.true;
    });

    it('loadClusterInfo should be called', function() {
      controller.loadClusterData();
      expect(controller.loadClusterInfo.calledOnce).to.be.true;
    });

    it('getUser should be called', function() {
      controller.loadClusterData();
      expect(App.router.get('wizardWatcherController').getUser.calledOnce).to.be.true;
    });

    it('App.db.setFilterConditions should be called', function() {
      controller.loadClusterData();
      expect(App.db.setFilterConditions.calledOnce).to.be.true;
    });

    it('loadClusterDataToModel should be called', function() {
      controller.loadClusterData();
      expect(controller.loadClusterDataToModel.calledOnce).to.be.true;
    });

    it('updateClusterEnv should be called', function() {
      controller.loadClusterData();
      expect(App.router.get('updateController').updateClusterEnv.calledOnce).to.be.true;
    });

    it('startPolling should be called', function() {
      controller.set('isLoaded', true);
      controller.loadClusterData();
      expect(App.router.get('mainController').startPolling.calledOnce).to.be.true;
    });
  });

  describe('#loadClusterInfo()', function() {

    beforeEach(function() {
      sinon.stub(App.HttpClient, 'get');
    });

    afterEach(function() {
      App.HttpClient.get.restore();
    });

    it('App.HttpClient.get should be called', function() {
      controller.loadClusterInfo();
      expect(App.HttpClient.get.calledOnce).to.be.true;
    });
  });

  /* TODO: write new tests for this. Disabling them for now.
  describe('#loadClusterDataToModel()', function() {

    beforeEach(function() {
      sinon.stub(controller, 'loadStackServiceComponents', function(callback) {
        callback({items: [{
          StackServices: {}
        }]});
      });
      sinon.stub(App.stackServiceMapper, 'mapStackServices');
      sinon.stub(App.config, 'setPreDefinedServiceConfigs');
      sinon.stub(controller, 'updateLoadStatus');
      sinon.stub(controller, 'loadServicesAndComponents');
      controller.loadClusterDataToModel();
    });

    afterEach(function() {
      controller.loadServicesAndComponents.restore();
      controller.updateLoadStatus.restore();
      controller.loadStackServiceComponents.restore();
      App.stackServiceMapper.mapStackServices.restore();
      App.config.setPreDefinedServiceConfigs.restore();
    });

    it('loadStackServiceComponents should be called', function() {
      expect(controller.loadStackServiceComponents.calledOnce).to.be.true;
    });

    it('App.stackServiceMapper.mapStackServices should be called', function() {
      expect(App.stackServiceMapper.mapStackServices.calledWith({items: [
        {
          StackServices: {
            is_selected: true,
            is_installed: false
          }
        }
      ]})).to.be.true;
    });

    it('App.config.setPreDefinedServiceConfigs should be called', function() {
      expect(App.config.setPreDefinedServiceConfigs.calledWith(true)).to.be.true;
    });

    it('updateLoadStatus should be called', function() {
      expect(controller.updateLoadStatus.calledWith('stackComponents')).to.be.true;
    });

    it('loadServicesAndComponents should be called', function() {
      expect(controller.loadServicesAndComponents.calledOnce).to.be.true;
    });
  });
*/

  describe('#loadAlerts()', function() {
    var updater = App.router.get('updateController');

    beforeEach(function() {
      sinon.stub(updater, 'updateAlertGroups', Em.clb);
      sinon.stub(updater, 'updateAlertDefinitions', Em.clb);
      sinon.stub(updater, 'updateAlertDefinitionSummary', Em.clb);
      sinon.stub(updater, 'updateUnhealthyAlertInstances', Em.clb);
      controller.loadAlerts();
    });

    afterEach(function() {
      updater.updateUnhealthyAlertInstances.restore();
      updater.updateAlertDefinitionSummary.restore();
      updater.updateAlertDefinitions.restore();
      updater.updateAlertGroups.restore();
    });

    it('updateAlertGroups should be called', function() {
      expect(updater.updateAlertGroups.calledOnce).to.be.true;
    });

    it('updateAlertDefinitions should be called', function() {
      expect(updater.updateAlertDefinitions.calledOnce).to.be.true;
    });

    it('updateAlertDefinitionSummary should be called', function() {
      expect(updater.updateAlertDefinitionSummary.calledOnce).to.be.true;
    });

    it('updateUnhealthyAlertInstances should be called', function() {
      expect(updater.updateUnhealthyAlertInstances.calledOnce).to.be.true;
    });

    it('should set isAlertsLoaded to true', function() {
      expect(controller.get('isAlertsLoaded')).to.be.true;
    });
  });

  describe('#loadConfigProperties()', function() {

    beforeEach(function() {
      sinon.stub(App.config, 'loadConfigsFromStack').returns({
        complete: Em.clb
      });
      sinon.stub(App.config, 'loadClusterConfigsFromStack').returns({
        complete: Em.clb
      });
    });

    afterEach(function() {
      App.config.loadClusterConfigsFromStack.restore();
      App.config.loadConfigsFromStack.restore();
    });

    it('App.config.loadConfigsFromStack should be called', function() {
      controller.loadConfigProperties();
      expect(App.config.loadConfigsFromStack.calledOnce).to.be.true;
    });

    it('App.config.loadClusterConfigsFromStack should be called', function() {
      controller.loadConfigProperties();
      expect(App.config.loadClusterConfigsFromStack.calledOnce).to.be.true;
    });

    it('isConfigsPropertiesLoaded should be true', function() {
      controller.loadConfigProperties();
      expect(controller.get('isConfigsPropertiesLoaded')).to.be.true;
    });
  });

  describe('#loadServicesAndComponents()', function() {
    var updater = App.router.get('updateController');

    beforeEach(function() {
      sinon.stub(updater, 'updateServices', Em.clb);
      sinon.stub(controller, 'updateLoadStatus');
      sinon.stub(updater, 'updateHost', Em.clb);
      sinon.stub(controller, 'loadAlerts');
      sinon.stub(controller, 'loadConfigProperties');
      sinon.stub(updater, 'updateComponentsState', Em.clb);
      sinon.stub(updater, 'updateServiceMetric', Em.clb);
      sinon.stub(controller, 'loadComponentWithStaleConfigs', Em.clb);

      controller.loadServicesAndComponents();
    });

    afterEach(function() {
      controller.loadConfigProperties.restore();
      controller.loadAlerts.restore();
      updater.updateHost.restore();
      updater.updateServices.restore();
      controller.updateLoadStatus.restore();
      updater.updateComponentsState.restore();
      updater.updateServiceMetric.restore();
      controller.loadComponentWithStaleConfigs.restore();
    });

    it('updateServices should be called', function() {
      expect(updater.updateServices.calledOnce).to.be.true;
    });

    it('updateLoadStatus should be called', function() {
      expect(controller.updateLoadStatus.calledWith('services')).to.be.true;
    });

    it('updateHost should be called', function() {
      expect(updater.updateHost.calledOnce).to.be.true;
    });

    it('isHostsLoaded should be true', function() {
      expect(controller.get('isHostsLoaded')).to.be.true;
    });

    it('loadAlerts should be called', function() {
      expect(controller.loadAlerts.calledOnce).to.be.true;
    });

    it('loadConfigProperties should be called', function() {
      expect(controller.loadConfigProperties.calledOnce).to.be.true;
    });

    it('updateComponentsState should be called', function() {
      expect(updater.updateComponentsState.calledOnce).to.be.true;
    });

    it('isComponentsStateLoaded should be true', function() {
      expect(controller.get('isComponentsStateLoaded')).to.be.true;
    });

    it('updateServiceMetric should be called', function() {
      expect(updater.updateServiceMetric.calledTwice).to.be.true;
    });

    it('isServiceMetricsLoaded should be true', function() {
      expect(controller.get('isServiceMetricsLoaded')).to.be.true;
    });

    it('isHostComponentMetricsLoaded should be true', function() {
      expect(controller.get('isHostComponentMetricsLoaded')).to.be.true;
    });

    it('loadComponentWithStaleConfigs should be called', function() {
      expect(controller.loadComponentWithStaleConfigs.calledOnce).to.be.true;
    });

    it('isComponentsConfigLoaded should be true', function() {
      expect(controller.get('isComponentsConfigLoaded')).to.be.true;
    });
  });

  describe('#loadComponentWithStaleConfigs', function() {
    it('App.ajax.send should be called', function() {
      controller.loadComponentWithStaleConfigs();
      var args = testHelpers.findAjaxRequest('name', 'components.get.staleConfigs');
      expect(args).to.exist;
    });
  });

  describe('#loadComponentWithStaleConfigsSuccessCallback', function() {
    beforeEach(function() {
      sinon.stub(App.componentsStateMapper, 'updateStaleConfigsHosts');
    });
    afterEach(function() {
      App.componentsStateMapper.updateStaleConfigsHosts.restore();
    });

    it('updateStaleConfigsHosts should be called', function() {
      var json = {
        items: [
          {
            ServiceComponentInfo: {
              component_name: 'C1'
            },
            host_components: [
              {
                HostRoles: {
                  host_name: 'host1'
                }
              }
            ]
          }
        ]
      };
      controller.loadComponentWithStaleConfigsSuccessCallback(json);
      expect(App.componentsStateMapper.updateStaleConfigsHosts.calledWith('C1', ['host1'])).to.be.true;
    });
  });
});
