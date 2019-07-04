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
require('controllers/main/admin/highAvailability/journalNode/step1_controller');
var testHelpers = require('test/helpers');

describe('App.ManageJournalNodeWizardStep2Controller', function () {
  var controller;

  beforeEach(function () {
    controller = App.ManageJournalNodeWizardStep2Controller.create({
      content: Em.Object.create()
    });
  });

  describe('#clearStep', function() {

    it('stepConfigs should be empty', function() {
      controller.clearStep();
      expect(controller.get('stepConfigs')).to.be.empty;
    });

    it('serverConfigData should be empty', function() {
      controller.clearStep();
      expect(controller.get('serverConfigData')).to.be.empty;
    });
  });

  describe('#loadStep', function() {

    beforeEach(function() {
      sinon.stub(controller, 'clearStep');
      sinon.stub(controller, 'loadConfigsTags');
    });

    afterEach(function() {
      controller.clearStep.restore();
      controller.loadConfigsTags.restore();
    });

    it('loadConfigsTags should be called', function() {
      controller.loadStep();
      expect(controller.loadConfigsTags.calledOnce).to.be.true;
    });

    it('clearStep should be called', function() {
      controller.loadStep();
      expect(controller.clearStep.calledOnce).to.be.true;
    });
  });

  describe('#loadConfigsTags', function() {

    it('App.ajax.send should be called', function() {
      controller.loadConfigsTags();
      var args = testHelpers.findAjaxRequest('name', 'config.tags');
      expect(args[0]).to.exists;
    });
  });

  describe('#onLoadConfigsTags', function() {
    var data = {
      Clusters: {
        desired_configs: {
          'hdfs-site': {
            tag: 'tag1'
          }
        }
      }
    };

    it('App.ajax.send should be called', function() {
      controller.onLoadConfigsTags(data);
      var args = testHelpers.findAjaxRequest('name', 'admin.get.all_configurations');
      expect(args[0]).to.be.eql({
        name: 'admin.get.all_configurations',
        sender: controller,
        data: {
          urlParams: '(type=hdfs-site&tag=tag1)'
        },
        success: 'onLoadConfigs',
        error: 'onTaskError'
      });
    });

    it('hdfsSiteTag should be set', function() {
      controller.onLoadConfigsTags(data);
      expect(controller.get('hdfsSiteTag')).to.be.eql({name: "hdfsSiteTag", value: 'tag1'});
    });
  });

  describe('#onLoadConfigs', function() {
    var data = {
      items: [
        {
          properties: {
            'dfs.nameservices': 'id0,id1'
          }
        }
      ]
    };

    beforeEach(function() {
      sinon.stub(controller, 'tweakServiceConfigs');
      sinon.stub(controller, 'renderServiceConfigs');
    });

    afterEach(function() {
      controller.tweakServiceConfigs.restore();
      controller.renderServiceConfigs.restore();
    });

    it('renderServiceConfigs should be called', function() {
      controller.onLoadConfigs(data);
      expect(controller.renderServiceConfigs.calledOnce).to.be.true;
    });

    it('tweakServiceConfigs should be called', function() {
      controller.onLoadConfigs(data);
      expect(controller.tweakServiceConfigs.calledOnce).to.be.true;
    });

    it('serviceConfigData should be an object', function() {
      controller.onLoadConfigs(data);
      expect(controller.get('serverConfigData')).to.be.eql(data);
    });

    it('nameServiceIds should be ["id0", "id1"]', function() {
      controller.onLoadConfigs(data);
      expect(controller.get('content.nameServiceIds')).to.eql(['id0', 'id1']);
    });

    it('isLoaded should be true', function() {
      controller.onLoadConfigs(data);
      expect(controller.get('isLoaded')).to.be.true;
    });
  });

  describe('#_prepareDependencies', function() {

    it('should return configs object', function() {
      controller.set('serverConfigData', {items: []});
      controller.set('content.nameServiceIds', ['id1', 'id2']);
      expect(controller._prepareDependencies()).to.be.eql({
        namespaceId: 'id1',
        serverConfigs: []
      });
    });
  });

  describe('#_prepareLocalDB', function() {

    beforeEach(function() {
      sinon.stub(App.Service, 'find').returns([
        Em.Object.create({
          serviceName: 'S1'
        })
      ]);
    });

    afterEach(function() {
      App.Service.find.restore();
    });

    it('should return localDB object', function() {
      controller.set('content', Em.Object.create({
        masterComponentHosts: [],
        slaveComponentHosts: [],
        hosts: []
      }));
      expect(controller._prepareLocalDB()).to.be.eql({
        masterComponentHosts: [],
        slaveComponentHosts: [],
        hosts: [],
        installedServices: ['S1']
      });
    });
  });

  describe('#tweakServiceConfigs', function() {

    beforeEach(function() {
      sinon.stub(controller, '_prepareLocalDB').returns({});
      sinon.stub(controller, '_prepareDependencies').returns({});
      sinon.stub(App.NnHaConfigInitializer, 'initialValue');
      sinon.stub(App, 'get', function (key) {
        if (key === 'hasNameNodeFederation') {
          return false;
        }
        return Em.get(App, key);
      });
      controller.set('content.controllerName', 'manageJournalNodeWizardController');
      controller.set('content.masterComponentHosts', []);
      controller.set('content.nameServiceIds', []);
    });

    afterEach(function() {
      controller._prepareLocalDB.restore();
      controller._prepareDependencies.restore();
      App.NnHaConfigInitializer.initialValue.restore();
      App.get.restore();
    });

    it('App.NnHaConfigInitializer.initialValue should be called', function() {
      controller.tweakServiceConfigs([
        {
          dependsOnNameServiceId: false,
          presentForNonFederatedHDFS: true
        }
      ]);
      expect(App.NnHaConfigInitializer.initialValue.calledOnce).to.be.true;
    });

    it('should return array of configs', function() {
      expect(controller.tweakServiceConfigs([
        {
          dependsOnNameServiceId: false,
          presentForNonFederatedHDFS: true
        }
      ])).to.be.eql([{
        dependsOnNameServiceId: false,
        isOverridable: false,
        presentForNonFederatedHDFS: true
      }]);
    });
  });

  describe('#renderServiceConfigs', function() {
    var _serviceConfig = {
      configCategories: [
        {
          name: 'S1'
        }
      ]
    };

    beforeEach(function() {
      sinon.stub(App.Service, 'find').returns([
        {
          serviceName: 'S1'
        }
      ]);
      sinon.stub(controller, 'loadComponentConfigs');
      controller.set('stepConfigs', []);
      controller.renderServiceConfigs(_serviceConfig);
    });

    afterEach(function() {
      controller.loadComponentConfigs.restore();
      App.Service.find.restore();
    });

    it('stepConfigs should not be empty', function() {
      expect(controller.get('stepConfigs')).to.not.be.empty;
    });

    it('selectedService should be object', function() {
      expect(controller.get('selectedService')).to.be.an.object;
    });

    it('once should be true', function() {
      expect(controller.get('once')).to.be.true;
    });

    it('loadComponentConfigs should be called', function() {
      expect(controller.loadComponentConfigs.calledOnce).to.be.true;
    });
  });

  describe('#loadComponentConfigs', function() {
    var componentConfig = {
      configs: []
    };

    it('configs should not be empty', function() {
      controller.loadComponentConfigs({configs: [{}]}, componentConfig);
      expect(componentConfig.configs).to.not.be.empty;
    });

    it('isEditable should be true', function() {
      controller.loadComponentConfigs({configs: [{isReconfigurable: true}]}, componentConfig);
      expect(componentConfig.configs[0].get('isEditable')).to.be.true;
    });
  });
});

