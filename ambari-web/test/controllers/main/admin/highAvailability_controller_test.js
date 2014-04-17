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
require('controllers/main/admin/highAvailability_controller');
require('models/host_component');
require('models/host');
require('utils/ajax/ajax');

describe('App.MainAdminHighAvailabilityController', function () {

  var controller = App.MainAdminHighAvailabilityController.create();

  describe('#enableHighAvailability()', function () {

    beforeEach(function () {
      sinon.spy(controller, "showErrorPopup");
    });
    afterEach(function () {
      controller.showErrorPopup.restore();
    });

    it('Security enabled', function () {
      controller.set('securityEnabled', true);
      expect(controller.enableHighAvailability()).to.be.false;
      expect(controller.showErrorPopup.calledOnce).to.be.true;
    });
    it('less than 3 ZooKeeper Servers', function () {
      controller.set('securityEnabled', false);
      App.store.load(App.HostComponent, {
        id: "NAMENODE_host1",
        component_name: 'NAMENODE',
        work_status: 'STARTED'
      });
      expect(controller.enableHighAvailability()).to.be.false;
      expect(controller.showErrorPopup.calledOnce).to.be.true;
      App.store.loadMany(App.HostComponent, [
        {
          id: "ZOOKEEPER_SERVER_host1",
          component_name: 'ZOOKEEPER_SERVER'
        },
        {
          id: "ZOOKEEPER_SERVER_host2",
          component_name: 'ZOOKEEPER_SERVER'
        },
        {
          id: "ZOOKEEPER_SERVER_host3",
          component_name: 'ZOOKEEPER_SERVER'
        }
      ]);
    });
    it('Security disabled and all checks passed', function () {
      App.router.set('transitionTo', function () {
      });
      expect(controller.enableHighAvailability()).to.be.true;
      expect(controller.showErrorPopup.called).to.be.false;
    });
    it('NameNode is started', function () {
      App.HostComponent.find('NAMENODE_host1').set('workStatus', 'INSTALLED');
      expect(controller.enableHighAvailability()).to.be.false;
      expect(controller.showErrorPopup.calledOnce).to.be.true;
      App.HostComponent.find('NAMENODE_host1').set('workStatus', 'STARTED');
    });
  });

  describe('#setSecurityStatus()', function () {

    beforeEach(function () {
      sinon.stub(App.ajax, "send", Em.K);
    });
    afterEach(function () {
      App.ajax.send.restore();
    });

    it('testMode = true', function () {
      App.testEnableSecurity = false;
      App.testMode = true;
      controller.set('securityEnabled', false);
      controller.set('dataIsLoaded', false);
      controller.setSecurityStatus();
      expect(controller.get('securityEnabled')).to.be.true;
      expect(controller.get('dataIsLoaded')).to.be.true;
      expect(App.ajax.send.called).to.be.false;

    });
    it('testMode = false', function () {
      App.testMode = false;
      controller.set('securityEnabled', false);
      controller.set('dataIsLoaded', false);
      controller.setSecurityStatus();
      expect(controller.get('securityEnabled')).to.be.false;
      expect(controller.get('dataIsLoaded')).to.be.false;
      expect(App.ajax.send.calledOnce).to.be.true;
    });
  });

  describe('#getSecurityStatusFromServerSuccessCallback()', function () {

    beforeEach(function () {
      sinon.stub(controller, "getServiceConfigsFromServer", Em.K);
      sinon.stub(controller, "showErrorPopup", Em.K);
    });
    afterEach(function () {
      controller.getServiceConfigsFromServer.restore();
      controller.showErrorPopup.restore();
    });

    it('desired_configs is empty', function () {
      var data = {
        Clusters: {
          desired_configs: {}
        }
      };
      controller.getSecurityStatusFromServerSuccessCallback(data);
      expect(controller.showErrorPopup.calledOnce).to.be.true;
    });
    it('desired_configs does not have "global"', function () {
      var data = {
        Clusters: {
          desired_configs: {
            'hdfs-site': {}
          }
        }
      };
      controller.getSecurityStatusFromServerSuccessCallback(data);
      expect(controller.showErrorPopup.calledOnce).to.be.true;
    });
    it('desired_configs does not have "global"', function () {
      var data = {
        Clusters: {
          desired_configs: {
            'global': {
              tag: 1
            }
          }
        }
      };
      controller.getSecurityStatusFromServerSuccessCallback(data);
      expect(controller.get('tag')).to.equal(1);
      expect(controller.getServiceConfigsFromServer.calledOnce).to.be.true;
      expect(controller.showErrorPopup.called).to.be.false;
    });
  });

  describe('#getSecurityStatusFromServerSuccessCallback()', function () {

    beforeEach(function () {
      sinon.stub(controller, "getServiceConfigsFromServer", Em.K);
      sinon.stub(controller, "showErrorPopup", Em.K);
    });
    afterEach(function () {
      controller.getServiceConfigsFromServer.restore();
      controller.showErrorPopup.restore();
    });

    it('desired_configs is empty', function () {
      var data = {
        Clusters: {
          desired_configs: {}
        }
      };
      controller.getSecurityStatusFromServerSuccessCallback(data);
      expect(controller.showErrorPopup.calledOnce).to.be.true;
    });
    it('desired_configs does not have "global"', function () {
      var data = {
        Clusters: {
          desired_configs: {
            'hdfs-site': {}
          }
        }
      };
      controller.getSecurityStatusFromServerSuccessCallback(data);
      expect(controller.showErrorPopup.calledOnce).to.be.true;
    });
    it('desired_configs does not have "global"', function () {
      var data = {
        Clusters: {
          desired_configs: {
            'global': {
              tag: 1
            }
          }
        }
      };
      controller.getSecurityStatusFromServerSuccessCallback(data);
      expect(controller.get('tag')).to.equal(1);
      expect(controller.getServiceConfigsFromServer.calledOnce).to.be.true;
      expect(controller.showErrorPopup.called).to.be.false;
    });
  });

  describe('#joinMessage()', function () {
    it('message is empty', function () {
      var message = [];
      expect(controller.joinMessage(message)).to.be.empty;
    });
    it('message is array from two strings', function () {
      var message = ['yes', 'no'];
      expect(controller.joinMessage(message)).to.equal('yes<br/>no');
    });
    it('message is string', function () {
      var message = 'hello';
      expect(controller.joinMessage(message)).to.equal('<p>hello</p>');
    });
  });

  describe('#getServiceConfigsFromServer()', function () {

    it('properties is null', function () {
      App.router.set('configurationController', Em.Object.create({
        getConfigsByTags: function () {
          return this.get('data');
        },
        data: [
          {
            tag: 1,
            properties: null
          }
        ]
      }));
      controller.getServiceConfigsFromServer();
      expect(controller.get('dataIsLoaded')).to.be.true;
      expect(controller.get('securityEnabled')).to.be.false;
    });
    it('"security_enabled" config is absent', function () {
      App.router.set('configurationController.data', [
        {
          tag: 1,
          properties: {}
        }
      ]);
      controller.getServiceConfigsFromServer();
      expect(controller.get('dataIsLoaded')).to.be.true;
      expect(controller.get('securityEnabled')).to.be.false;
    });
    it('"security_enabled" is false', function () {
      App.router.set('configurationController.data', [
        {
          tag: 1,
          properties: {
            'security_enabled': false
          }
        }
      ]);
      controller.getServiceConfigsFromServer();
      expect(controller.get('dataIsLoaded')).to.be.true;
      expect(controller.get('securityEnabled')).to.be.false;
    });
    it('"security_enabled" is "false"', function () {
      App.router.set('configurationController.data', [
        {
          tag: 1,
          properties: {
            'security_enabled': "false"
          }
        }
      ]);
      controller.getServiceConfigsFromServer();
      expect(controller.get('dataIsLoaded')).to.be.true;
      expect(controller.get('securityEnabled')).to.be.false;
    });
    it('"security_enabled" is "true"', function () {
      App.router.set('configurationController.data', [
        {
          tag: 1,
          properties: {
            'security_enabled': "true"
          }
        }
      ]);
      controller.getServiceConfigsFromServer();
      expect(controller.get('dataIsLoaded')).to.be.true;
      expect(controller.get('securityEnabled')).to.be.true;
    });
    it('"security_enabled" is true', function () {
      App.router.set('configurationController.data', [
        {
          tag: 1,
          properties: {
            'security_enabled': true
          }
        }
      ]);
      controller.getServiceConfigsFromServer();
      expect(controller.get('dataIsLoaded')).to.be.true;
      expect(controller.get('securityEnabled')).to.be.true;
    });
  });
});
