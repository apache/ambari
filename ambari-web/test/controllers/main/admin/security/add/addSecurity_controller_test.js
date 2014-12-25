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
require('mixins/common/localStorage');
require('controllers/wizard');
require('controllers/main/admin/security/add/addSecurity_controller');
require('models/cluster');
require('models/service');

describe('App.AddSecurityController', function () {

  var controller = App.AddSecurityController.create({
    currentStep: null,
    content: Em.Object.create({
      isATSInstalled: true,
      services: [],
      isNnHa: 'false',
      serviceConfigProperties: null
    })
  });

  describe('#installedServices', function () {

    afterEach(function () {
      App.Service.find.restore();
    });

    it('No installed services', function () {
      sinon.stub(App.Service, 'find', function () {
        return [];
      });
      expect(controller.get('installedServices')).to.eql([]);
    });
    it('One service installed', function () {
      sinon.stub(App.Service, 'find', function () {
        return [Em.Object.create({serviceName: 'HDFS'})];
      });
      Em.propertyDidChange(controller, 'installedServices');
      expect(controller.get('installedServices')).to.eql(['HDFS']);
    });
  });

  describe('#loadAllPriorSteps()', function () {

    beforeEach(function () {
      sinon.stub(controller, 'loadServiceConfigs', Em.K);
      sinon.stub(controller, 'loadServices', Em.K);
      sinon.stub(controller, 'loadNnHaStatus', Em.K);
    });
    afterEach(function () {
      controller.loadServiceConfigs.restore();
      controller.loadServices.restore();
      controller.loadNnHaStatus.restore();
    });

    var commonSteps = ['4', '3', '2'];
    commonSteps.forEach(function (step) {
      it('Current step - ' + step, function () {
        controller.set('currentStep', step);
        controller.loadAllPriorSteps();
        expect(controller.loadServiceConfigs.calledOnce).to.be.true;
        expect(controller.loadServices.calledOnce).to.be.true;
        expect(controller.loadNnHaStatus.calledOnce).to.be.true;
      });
    });
    it('Current step - 1', function () {
      controller.set('currentStep', '1');
      controller.loadAllPriorSteps();
      expect(controller.loadServiceConfigs.called).to.be.false;
      expect(controller.loadServices.calledOnce).to.be.true;
      expect(controller.loadNnHaStatus.calledOnce).to.be.true;
    });
  });

  describe('#loadServices()', function () {
    it('No installed services', function () {
      controller.reopen({
        installedServices: [],
        secureServices: [
          {serviceName: 'GENERAL'}
        ]
      });
      controller.loadServices();
      expect(controller.get('content.services').mapProperty('serviceName')).to.eql(['GENERAL']);
    });
    it('Installed service does not match the secure one', function () {
      controller.set('installedServices', ["HDFS"]);
      controller.loadServices();
      expect(controller.get('content.services').mapProperty('serviceName')).to.eql(['GENERAL']);
    });
    it('Installed service matches the secure one', function () {
      controller.set('secureServices', [
        {serviceName: 'GENERAL'},
        {serviceName: 'HDFS'}
      ]);
      controller.loadServices();
      expect(controller.get('content.services').mapProperty('serviceName')).to.eql(['GENERAL', 'HDFS']);
    });
  });

  describe('#loadNnHaStatus()', function () {
    afterEach(function () {
      App.db.getIsNameNodeHa.restore();
    });
    it('NameNode HA is off', function () {
      sinon.stub(App.db, 'getIsNameNodeHa', function () {
        return false;
      });
      controller.loadNnHaStatus();
      expect(controller.get('content.isNnHa')).to.be.false;
    });
    it('NameNode HA is on', function () {
      sinon.stub(App.db, 'getIsNameNodeHa', function () {
        return true;
      });
      controller.loadNnHaStatus();
      expect(controller.get('content.isNnHa')).to.be.true;
    });
  });

  describe('#loadServiceConfigs()', function () {
    afterEach(function () {
      App.db.getSecureConfigProperties.restore();
    });
    it('SecureConfigProperties is empty', function () {
      sinon.stub(App.db, 'getSecureConfigProperties', function () {
        return [];
      });
      controller.loadServiceConfigs();
      expect(controller.get('content.serviceConfigProperties')).to.eql([]);
    });
    it('SecureConfigProperties has one config', function () {
      sinon.stub(App.db, 'getSecureConfigProperties', function () {
        return [{}];
      });
      controller.loadServiceConfigs();
      expect(controller.get('content.serviceConfigProperties')).to.eql([{}]);
    });
  });

  describe('#getConfigOverrides()', function () {
    var testCases = [
      {
        title: 'overrides is null',
        configProperty: Em.Object.create({overrides: null}),
        result: null
      },
      {
        title: 'overrides is empty',
        configProperty: Em.Object.create({overrides: []}),
        result: null
      },
      {
        title: 'overrides has one override',
        configProperty: Em.Object.create({
          overrides: [
            Em.Object.create({
              value: 'value1',
              selectedHostOptions: []
            })
          ]
        }),
        result: [{
          value: 'value1',
          hosts: []
        }]
      },
      {
        title: 'overrides has one override with hosts',
        configProperty: Em.Object.create({
          overrides: [
            Em.Object.create({
              value: 'value1',
              selectedHostOptions: ['host1']
            })
          ]
        }),
        result: [{
          value: 'value1',
          hosts: ['host1']
        }]
      }
    ];

    testCases.forEach(function(test){
      it(test.title, function () {
        expect(controller.getConfigOverrides(test.configProperty)).to.eql(test.result);
      });
    });
  });

  describe('#saveServiceConfigProperties()', function () {
    var testCases = [
      {
        title: 'stepConfigs is empty',
        stepController: Em.Object.create({
          stepConfigs: []
        }),
        result: []
      },
      {
        title: 'No configs in service',
        stepController: Em.Object.create({
          stepConfigs: [
            Em.Object.create({configs: []})
          ]
        }),
        result: []
      }
    ];

    testCases.forEach(function (test) {
      it(test.title, function () {
        sinon.stub(App.db, 'setSecureConfigProperties', Em.K);
        controller.saveServiceConfigProperties(test.stepController);
        expect(App.db.setSecureConfigProperties.calledWith(test.result)).to.be.true;
        expect(controller.get('content.serviceConfigProperties')).to.eql(test.result);
        App.db.setSecureConfigProperties.restore();
      });
    });
    it('Service has config', function () {
      var  stepController = Em.Object.create({
        stepConfigs: [
          Em.Object.create({configs: [
            Em.Object.create({
              name: 'config1',
              value: 'value1'
            })
          ]})
        ]
      });
      sinon.stub(App.db, 'setSecureConfigProperties', Em.K);
      controller.saveServiceConfigProperties(stepController);
      expect(App.db.setSecureConfigProperties.calledOnce).to.be.true;
      expect(controller.get('content.serviceConfigProperties').mapProperty('name')).to.eql(['config1']);
      App.db.setSecureConfigProperties.restore();
    });
  });
});
