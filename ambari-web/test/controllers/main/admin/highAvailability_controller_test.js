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

    var hostComponents = [];

    beforeEach(function () {
      sinon.stub(App.router, 'transitionTo', Em.K);
      sinon.stub(App.HostComponent, 'find', function(){
        return hostComponents;
      });
      sinon.spy(controller, "showErrorPopup");
    });
    afterEach(function () {
      App.router.transitionTo.restore();
      controller.showErrorPopup.restore();
      App.HostComponent.find.restore();
    });

    it('Security enabled', function () {
      controller.set('securityEnabled', true);
      expect(controller.enableHighAvailability()).to.be.false;
      expect(controller.showErrorPopup.calledOnce).to.be.true;
    });
    it('NAMENODE in INSTALLED state', function () {
      controller.set('securityEnabled', false);
      hostComponents = [
        Em.Object.create({
          componentName: 'NAMENODE',
          workStatus: 'INSTALLED'
        }),
        Em.Object.create({
          componentName: 'ZOOKEEPER_SERVER',
          workStatus: 'INSTALLED'
        }),
        Em.Object.create({
          componentName: 'ZOOKEEPER_SERVER',
          workStatus: 'INSTALLED'
        }),
        Em.Object.create({
          componentName: 'ZOOKEEPER_SERVER',
          workStatus: 'INSTALLED'
        })
      ];

      sinon.stub(App.router, 'get', function(){
        return 3;
      });
      expect(controller.enableHighAvailability()).to.be.false;
      expect(controller.showErrorPopup.calledOnce).to.be.true;
      App.router.get.restore();
    });
    it('Cluster has less than 3 ZOOKEPER_SERVER components', function () {
      hostComponents = [
        Em.Object.create({
          componentName: 'NAMENODE',
          workStatus: 'STARTED'
        })
      ];

      sinon.stub(App.router, 'get', function(){
        return 3;
      });
      expect(controller.enableHighAvailability()).to.be.false;
      expect(controller.showErrorPopup.called).to.be.true;
      App.router.get.restore();
    });
    it('total hosts number less than 3', function () {
      controller.set('securityEnabled', false);
      hostComponents = [
        Em.Object.create({
          componentName: 'NAMENODE',
          workStatus: 'STARTED'
        }),
        Em.Object.create({
          componentName: 'ZOOKEEPER_SERVER',
          workStatus: 'INSTALLED'
        }),
        Em.Object.create({
          componentName: 'ZOOKEEPER_SERVER',
          workStatus: 'INSTALLED'
        }),
        Em.Object.create({
          componentName: 'ZOOKEEPER_SERVER',
          workStatus: 'INSTALLED'
        })
      ];
      sinon.stub(App.router, 'get', function () {
        return 1;
      });
      expect(controller.enableHighAvailability()).to.be.false;
      expect(controller.showErrorPopup.calledOnce).to.be.true;
      App.router.get.restore();
    });
    it('All checks passed', function () {
      controller.set('securityEnabled', false);
      hostComponents = [
        Em.Object.create({
          componentName: 'NAMENODE',
          workStatus: 'STARTED'
        }),
        Em.Object.create({
          componentName: 'ZOOKEEPER_SERVER',
          workStatus: 'INSTALLED'
        }),
        Em.Object.create({
          componentName: 'ZOOKEEPER_SERVER',
          workStatus: 'INSTALLED'
        }),
        Em.Object.create({
          componentName: 'ZOOKEEPER_SERVER',
          workStatus: 'INSTALLED'
        })
      ];
      sinon.stub(App.router, 'get', function(){
        return 3;
      });
      expect(controller.enableHighAvailability()).to.be.true;
      expect(App.router.transitionTo.calledWith('main.admin.enableHighAvailability')).to.be.true;
      expect(controller.showErrorPopup.calledOnce).to.be.false;
      App.router.get.restore();
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
    it('desired_configs does not have "hadoop-env"', function () {
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
    it('desired_configs has "hadoop-env"', function () {
      var data = {
        Clusters: {
          desired_configs: {
            'hadoop-env': {
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
    it('desired_configs does not have "hadoop-env"', function () {
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
    it('desired_configs has "hadoop-env"', function () {
      var data = {
        Clusters: {
          desired_configs: {
            'hadoop-env': {
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
    it('configs present', function () {
      sinon.stub(App.router.get('configurationController'), 'getConfigsByTags', function () {
        return {
          done: function (callback) {
            callback([{tag: '1'}]);
          }
        }
      });
      sinon.stub(controller, 'isSecurityEnabled', function(){
        return true;
      });
      controller.set('tag', '1');
      controller.getServiceConfigsFromServer();

      expect(App.router.get('configurationController').getConfigsByTags.calledWith([
        {
          siteName: "hadoop-env",
          tagName: '1'
        }
      ])).to.be.true;
      expect(controller.isSecurityEnabled.calledOnce).to.be.true;
      expect(controller.get('dataIsLoaded')).to.be.true;
      expect(controller.get('securityEnabled')).to.be.true;

      App.router.get('configurationController').getConfigsByTags.restore();
      controller.isSecurityEnabled.restore();
    });
  });

  describe('#isSecurityEnabled()', function () {
    it('properties is null', function () {
      expect(controller.isSecurityEnabled(null)).to.be.false;
    });
    it('properties is empty object', function () {
      expect(controller.isSecurityEnabled({})).to.be.false;
    });
    it('security_enabled is false', function () {
      expect(controller.isSecurityEnabled({security_enabled: false})).to.be.false;
    });
    it('security_enabled is true', function () {
      expect(controller.isSecurityEnabled({security_enabled: true})).to.be.true;
    });
    it('security_enabled is "true"', function () {
      expect(controller.isSecurityEnabled({security_enabled: 'true'})).to.be.true;
    });
  });
});
