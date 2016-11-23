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
var testHelpers = require('test/helpers');

describe('App.MainAdminServiceAutoStartController', function() {
  var controller;

  beforeEach(function() {
    controller = App.MainAdminServiceAutoStartController.create({
      clusterConfigs: {}
    });
  });


  describe('#valueChanged()', function() {

    it('servicesAutoStart is changed', function() {
      controller.reopen({
        servicesAutoStart: true,
        servicesAutoStartSaved: false,
        tabs: []
      });
      controller.valueChanged();
      expect(controller.get('isSaveDisabled')).to.be.false;
    });

    it('servicesAutoStart is not changed', function() {
      controller.reopen({
        servicesAutoStart: true,
        servicesAutoStartSaved: true,
        tabs: []
      });
      controller.valueChanged();
      expect(controller.get('isSaveDisabled')).to.be.true;
    });

    it('components state is not changed', function() {
      controller.reopen({
        servicesAutoStart: true,
        servicesAutoStartSaved: true,
        tabs: [Em.Object.create({
          components: [
            Em.Object.create({
              isChanged: false
            })
          ]
        })]
      });
      controller.valueChanged();
      expect(controller.get('isSaveDisabled')).to.be.true;
    });

    it('components state is changed', function() {
      controller.reopen({
        servicesAutoStart: true,
        servicesAutoStartSaved: true,
        tabs: [Em.Object.create({
          components: [
            Em.Object.create({
              isChanged: true
            })
          ]
        })]
      });
      controller.valueChanged();
      expect(controller.get('isSaveDisabled')).to.be.false;
    });
  });

  describe('#loadClusterConfig()', function() {

    it('App.ajax.send should be called', function() {
      controller.loadClusterConfig();
      var args = testHelpers.findAjaxRequest('name', 'config.tags.site');
      expect(args[0]).to.be.eql({
        name: 'config.tags.site',
        sender: controller,
        data: {
          site: 'cluster-env'
        }
      });
    });
  });

  describe('#loadComponentsConfigs()', function() {

    it('App.ajax.send should be called', function() {
      controller.loadComponentsConfigs();
      var args = testHelpers.findAjaxRequest('name', 'components.get_category');
      expect(args[0]).to.be.eql({
        name: 'components.get_category',
        sender: controller,
        success: 'loadComponentsConfigsSuccess'
      });
    });
  });

  describe('#loadComponentsConfigsSuccess()', function() {

    it('componentsConfigs should be set', function() {
      controller.loadComponentsConfigsSuccess({items: {prop1: 'val1'}});
      expect(controller.get('componentsConfigs')).to.be.eql({prop1: 'val1'});
    });
  });

  describe('#saveClusterConfigs()', function() {
    var clusterConfigs = {};

    it('App.ajax.send should be called', function() {
      controller.saveClusterConfigs(clusterConfigs);
      var args = testHelpers.findAjaxRequest('name', 'admin.save_configs');
      expect(args[0]).to.be.eql({
        name: 'admin.save_configs',
        sender: controller,
        data: {
          siteName: 'cluster-env',
          properties: clusterConfigs
        }
      });
    });
  });

  describe('#saveComponentSettingsCall()', function() {
    it('App.ajax.send should be called', function() {
      controller.saveComponentSettingsCall(true, ['c1', 'c2']);
      var args = testHelpers.findAjaxRequest('name', 'components.update');
      expect(args[0]).to.be.eql({
        name: 'components.update',
        sender: controller,
        data: {
          ServiceComponentInfo: {
            recovery_enabled: true
          },
          query: 'ServiceComponentInfo/component_name.in(c1,c2)'
        }
      });
    });
  });

  describe('#createRecoveryComponent()', function() {

    it('should return recovery component', function() {
      var serviceComponentInfo = {
        component_name: 'c1',
        recovery_enabled: 'true',
        service_name: 's1'
      };
      expect(controller.createRecoveryComponent(serviceComponentInfo)).to.be.an.object;
    });
  });

  describe('#createTab()', function() {

    it('should return tab', function() {
      var serviceComponentInfo = {
        component_name: 'c1',
        recovery_enabled: 'true',
        service_name: 's1'
      };
      expect(controller.createTab(serviceComponentInfo, {})).to.be.an.object;
    });
  });

  describe('#filterTabsByRecovery()', function() {

    it('should return empty when no components were changed', function() {
      var tabs = [Em.Object.create({
        components: [
          Em.Object.create({
            isChanged: false
          })
        ]
      })];
      expect(controller.filterTabsByRecovery(tabs, true)).to.be.empty;
    });

    it('should return enabled components ', function() {
      var tabs = [Em.Object.create({
        components: [
          Em.Object.create({
            isChanged: true,
            recoveryEnabled: true
          })
        ]
      })];
      expect(controller.filterTabsByRecovery(tabs, true)).to.have.length(1);
    });

    it('should return disabled components ', function() {
      var tabs = [Em.Object.create({
        components: [
          Em.Object.create({
            isChanged: true,
            recoveryEnabled: false
          })
        ]
      })];
      expect(controller.filterTabsByRecovery(tabs, false)).to.have.length(1);
    });
  });

  describe('#syncStatus()', function() {
    var mock = {
      getConfigsByTags: function() {
        return {
          done: function(callback) {
            callback([{properties: {}}]);
          }
        }
      },
      saveToDB: Em.K
    };

    beforeEach(function() {
      sinon.stub(App.router, 'get').returns(mock);
      sinon.spy(mock, 'saveToDB');
      sinon.stub(controller, 'valueChanged');
    });

    afterEach(function() {
      App.router.get.restore();
      mock.saveToDB.restore();
      controller.valueChanged.restore();
    });

    it('should save servicesAutoStart to local DB', function() {
      controller.setProperties({
        servicesAutoStart: true
      });
      controller.syncStatus();
      expect(mock.saveToDB.getCall(0).args[0]).to.be.eql([{
        properties: {
          recovery_enabled: 'true'
        }
      }]);
    });

    it('recoveryEnabledSaved should be synced with recoveryEnabled', function() {
      controller.reopen({
        servicesAutoStart: true,
        tabs: [Em.Object.create({
          components: [
            Em.Object.create({
              recoveryEnabled: true,
              recoveryEnabledSaved: false
            })
          ]
        })]
      });
      controller.syncStatus();
      expect(controller.get('tabs')[0].get('components')[0].get('recoveryEnabledSaved')).to.be.true;
    });

    it('valueChanged should be called', function() {
      controller.syncStatus();
      expect(controller.valueChanged).to.be.calledonce;
    });
  });

  describe('#revertStatus()', function() {

    it('component recoveryEnabled should be reverted', function() {
      controller.reopen({
        tabs: [Em.Object.create({
          components: [
            Em.Object.create({
              recoveryEnabled: true,
              recoveryEnabledSaved: false
            })
          ]
        })]
      });
      controller.revertStatus();
      expect(controller.get('tabs')[0].get('components')[0].get('recoveryEnabled')).to.be.false;
    });

    it('servicesAutoStart should be reverted', function() {
      controller.reopen({
        servicesAutoStart: true,
        servicesAutoStartSaved: false
      });
      controller.revertStatus();
      expect(controller.get('servicesAutoStart')).to.be.false;
    });
  });

  describe('#enableAll()', function() {

    it('should set each recoveryEnabled to true', function() {
      var components = [
        Em.Object.create({recoveryEnabled: false})
      ];
      controller.enableAll({context: Em.Object.create({components: components})});
      expect(components[0].get('recoveryEnabled')).to.be.true;
    });
  });

  describe('#disableAll()', function() {

    it('should set each recoveryEnabled to false', function() {
      var components = [
        Em.Object.create({recoveryEnabled: true})
      ];
      controller.disableAll({context: Em.Object.create({components: components})});
      expect(components[0].get('recoveryEnabled')).to.be.false;
    });
  });

});
