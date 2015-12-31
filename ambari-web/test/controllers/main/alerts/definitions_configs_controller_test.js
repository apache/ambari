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

var controller;

function getController() {
  return App.MainAlertDefinitionConfigsController.create({
    allServices: ['service1', 'service2', 'service3'],
    allComponents: ['component1', 'component2', 'component3'],
    aggregateAlertNames: ['alertDefinitionName', 'alertDefinitionName2', 'alertDefinitionName3']
  });
}

function getEmptyArray() {
  return [];
}

describe('App.MainAlertDefinitionConfigsController', function () {

  beforeEach(function () {
    controller = getController();
  });

  App.TestAliases.testAsComputedOr(getController(), 'hasErrors', ['someConfigIsInvalid', 'hasThresholdsError']);

  describe('#renderConfigs()', function () {

    beforeEach(function () {
      controller.set('content', Em.Object.create({}));
      sinon.stub(controller, 'renderPortConfigs', getEmptyArray);
      sinon.stub(controller, 'renderMetricConfigs', getEmptyArray);
      sinon.stub(controller, 'renderWebConfigs', getEmptyArray);
      sinon.stub(controller, 'renderScriptConfigs', getEmptyArray);
      sinon.stub(controller, 'renderAggregateConfigs', getEmptyArray);
    });

    afterEach(function () {
      controller.renderPortConfigs.restore();
      controller.renderMetricConfigs.restore();
      controller.renderWebConfigs.restore();
      controller.renderScriptConfigs.restore();
      controller.renderAggregateConfigs.restore();
    });

    it('should call renderPortConfigs method', function () {
      controller.set('alertDefinitionType', 'PORT');
      controller.renderConfigs();
      expect(controller.renderPortConfigs.calledOnce).to.be.true;
    });

    it('should call renderMetricConfigs method', function () {
      controller.set('alertDefinitionType', 'METRIC');
      controller.renderConfigs();
      expect(controller.renderMetricConfigs.calledOnce).to.be.true;
    });

    it('should call renderWebConfigs method', function () {
      controller.set('alertDefinitionType', 'WEB');
      controller.renderConfigs();
      expect(controller.renderWebConfigs.calledOnce).to.be.true;
    });

    it('should call renderScriptConfigs method', function () {
      controller.set('alertDefinitionType', 'SCRIPT');
      controller.renderConfigs();
      expect(controller.renderScriptConfigs.calledOnce).to.be.true;
    });

    it('should call renderAggregateConfigs method', function () {
      controller.set('alertDefinitionType', 'AGGREGATE');
      controller.renderConfigs();
      expect(controller.renderAggregateConfigs.calledOnce).to.be.true;
    });

  });

  describe('#renderPortConfigs()', function () {

    beforeEach(function () {
      controller.set('content', Em.Object.create({
        name: 'alertDefinitionName',
        service: {displayName: 'alertDefinitionService'},
        componentName: 'component1',
        scope: 'HOST',
        description: 'alertDefinitionDescription',
        interval: 60,
        reporting: [
          Em.Object.create({
            type: 'warning',
            value: 10
          }),
          Em.Object.create({
            type: 'critical',
            value: 20
          }),
          Em.Object.create({
            type: 'ok',
            value: 30
          })
        ],
        uri: 'alertDefinitionUri',
        defaultPort: '777'
      }));
    });

    it('isWizard = true', function () {
      controller.set('isWizard', true);
      var result = controller.renderPortConfigs();
      expect(result.length).to.equal(11);
    });

    it('isWizard = false', function () {
      controller.set('isWizard', false);
      var result = controller.renderPortConfigs();
      expect(result.length).to.equal(5);
    });

  });

  describe('#renderMetricConfigs()', function () {

    beforeEach(function () {
      controller.set('content', Em.Object.create({
        name: 'alertDefinitionName',
        service: {displayName: 'alertDefinitionService'},
        componentName: 'component1',
        scope: 'HOST',
        description: 'alertDefinitionDescription',
        interval: 60,
        reporting: [
          Em.Object.create({
            type: 'warning',
            value: 10
          }),
          Em.Object.create({
            type: 'critical',
            value: 20
          }),
          Em.Object.create({
            type: 'ok',
            value: 30
          })
        ],
        uri: {
          "http": "{{mapred-site/mapreduce.jobhistory.webapp.address}}",
          "https": "{{mapred-site/mapreduce.jobhistory.webapp.https.address}}",
          "https_property": "{{mapred-site/mapreduce.jobhistory.http.policy}}",
          "https_property_value": "HTTPS_ONLY",
          "default_port": 0.0
        },
        jmx: {
          propertyList: ['property1', 'property2'],
          value: 'jmxValue'
        },
        ganglia: {
          propertyList: null,
          value: null
        }
      }));
    });

    it('isWizard = true', function () {
      controller.set('isWizard', true);
      var result = controller.renderMetricConfigs();
      expect(result.length).to.equal(11);
    });

    it('isWizard = false', function () {
      controller.set('isWizard', false);
      var result = controller.renderMetricConfigs();
      expect(result.length).to.equal(5);
    });

  });

  describe('#renderWebConfigs()', function () {

    beforeEach(function () {
      controller.set('content', Em.Object.create({
        name: 'alertDefinitionName',
        service: {displayName: 'alertDefinitionService'},
        componentName: 'component1',
        scope: 'HOST',
        description: 'alertDefinitionDescription',
        interval: 60,
        reporting: [
          Em.Object.create({
            type: 'warning',
            value: 10
          }),
          Em.Object.create({
            type: 'critical',
            value: 20
          }),
          Em.Object.create({
            type: 'ok',
            value: 30
          })
        ],
        uri: {
          "http": "{{mapred-site/mapreduce.jobhistory.webapp.address}}",
          "https": "{{mapred-site/mapreduce.jobhistory.webapp.https.address}}",
          "https_property": "{{mapred-site/mapreduce.jobhistory.http.policy}}",
          "https_property_value": "HTTPS_ONLY",
          "default_port": 0.0
        }
      }));
    });

    it('isWizard = true', function () {
      controller.set('isWizard', true);
      var result = controller.renderWebConfigs();
      expect(result.length).to.equal(11);
    });

    it('isWizard = false', function () {
      controller.set('isWizard', false);
      var result = controller.renderWebConfigs();
      expect(result.length).to.equal(5);
    });

  });

  describe('#renderScriptConfigs()', function () {

    beforeEach(function () {
      controller.set('content', Em.Object.create({
        name: 'alertDefinitionName',
        service: {displayName: 'alertDefinitionService'},
        componentName: 'component1',
        scope: 'HOST',
        description: 'alertDefinitionDescription',
        interval: 60,
        reporting: [
          Em.Object.create({
            type: 'warning',
            value: 10
          }),
          Em.Object.create({
            type: 'critical',
            value: 20
          }),
          Em.Object.create({
            type: 'ok',
            value: 30
          })
        ],
        location: 'path to script'
      }));
    });

    it('isWizard = true', function () {
      controller.set('isWizard', true);
      var result = controller.renderScriptConfigs();
      expect(result.length).to.equal(8);
    });

    it('isWizard = false', function () {
      controller.set('isWizard', false);
      var result = controller.renderScriptConfigs();
      expect(result.length).to.equal(2);
    });

  });

  describe('#renderAggregateConfigs()', function () {

    it('should render array of configs with correct values', function () {

      controller.set('content', Em.Object.create({
        name: 'alertDefinitionName',
        description: 'alertDefinitionDescription',
        interval: 60,
        reporting: [
          Em.Object.create({
            type: 'warning',
            value: 10
          }),
          Em.Object.create({
            type: 'critical',
            value: 20
          }),
          Em.Object.create({
            type: 'ok',
            value: 30
          })
        ]
      }));

      var result = controller.renderAggregateConfigs();

      expect(result.length).to.equal(5);
    });

  });

  describe('#editConfigs()', function () {

    beforeEach(function () {
      controller.set('configs', [
        Em.Object.create({value: 'value1', previousValue: '', isDisabled: true}),
        Em.Object.create({value: 'value2', previousValue: '', isDisabled: true}),
        Em.Object.create({value: 'value3', previousValue: '', isDisabled: true})
      ]);
      controller.set('canEdit', false);
      controller.editConfigs();
    });

    it('should set previousValue', function () {
      expect(controller.get('configs').mapProperty('previousValue')).to.eql(['value1', 'value2', 'value3']);
    });
    it('should set isDisabled for each config', function () {
      expect(controller.get('configs').someProperty('isDisabled', true)).to.be.false;
    });
    it('should change canEdit flag', function () {
      expect(controller.get('canEdit')).to.be.true;
    });

  });

  describe('#cancelEditConfigs()', function () {

    beforeEach(function () {
      controller.set('configs', [
        Em.Object.create({value: '', previousValue: 'value1', isDisabled: false}),
        Em.Object.create({value: '', previousValue: 'value2', isDisabled: false}),
        Em.Object.create({value: '', previousValue: 'value3', isDisabled: false})
      ]);
      controller.set('canEdit', true);
      controller.cancelEditConfigs();
    });

    it('should set previousValue', function () {
      expect(controller.get('configs').mapProperty('value')).to.eql(['value1', 'value2', 'value3']);
    });
    it('should set isDisabled for each config', function () {
      expect(controller.get('configs').someProperty('isDisabled', false)).to.be.false;
    });
    it('should change canEdit flag', function () {
      expect(controller.get('canEdit')).to.be.false;
    });

  });

  describe('#saveConfigs()', function () {

    beforeEach(function () {
      sinon.spy(App.ajax, 'send');
      controller.set('configs', [
        Em.Object.create({isDisabled: true}),
        Em.Object.create({isDisabled: true}),
        Em.Object.create({isDisabled: true})
      ]);
      controller.set('canEdit', true);
      controller.saveConfigs();
    });

    afterEach(function () {
      App.ajax.send.restore();
    });

    it('should set isDisabled for each config', function () {
      expect(controller.get('configs').someProperty('isDisabled', false)).to.be.false;
    });
    it('should change canEdit flag', function () {
      expect(controller.get('canEdit')).to.be.false;
    });
    it('should sent 1 request', function () {
      expect(App.ajax.send.calledOnce).to.be.true;
    });

  });

  describe('#getPropertiesToUpdate()', function () {

    beforeEach(function () {
      controller.set('content', {
        rawSourceData: {
          path1: 'value',
          path2: {
            path3: 'value'
          }
        }
      });
    });

    var testCases = [
      {
        m: 'should ignore configs with wasChanged false',
        configs: [
          Em.Object.create({
            wasChanged: false,
            apiProperty: 'name1',
            apiFormattedValue: 'test1'
          }),
          Em.Object.create({
            wasChanged: true,
            apiProperty: 'name2',
            apiFormattedValue: 'test2'
          }),
          Em.Object.create({
            wasChanged: false,
            apiProperty: 'name3',
            apiFormattedValue: 'test3'
          })
        ],
        result: {
          'AlertDefinition/name2': 'test2'
        }
      },
      {
        m: 'should correctly map deep source properties',
        configs: [
          Em.Object.create({
            wasChanged: true,
            apiProperty: 'name1',
            apiFormattedValue: 'test1'
          }),
          Em.Object.create({
            wasChanged: true,
            apiProperty: 'source.path1',
            apiFormattedValue: 'value1'
          }),
          Em.Object.create({
            wasChanged: true,
            apiProperty: 'source.path2.path3',
            apiFormattedValue: 'value2'
          })
        ],
        result: {
          'AlertDefinition/name1': 'test1',
          'AlertDefinition/source': {
            path1: 'value1',
            path2: {
              path3: 'value2'
            }
          }
        }
      },
      {
        m: 'should correctly multiple apiProperties',
        configs: [
          Em.Object.create({
            wasChanged: true,
            apiProperty: ['name1', 'name2'],
            apiFormattedValue: ['value1', 'value2']
          })
        ],
        result: {
          'AlertDefinition/name1': 'value1',
          'AlertDefinition/name2': 'value2'
        }
      }
    ];

    testCases.forEach(function (testCase) {

      it(testCase.m, function () {

        controller.set('configs', testCase.configs);
        var result = controller.getPropertiesToUpdate(true);

        expect(result).to.eql(testCase.result);
      });
    });
  });

  describe('#changeType()', function () {

    beforeEach(function () {
      controller.set('allServices', ['service1', 'service2']);
      controller.set('allScopes', ['scope1', 'scope2']);

      controller.set('configs', [
        Em.Object.create({name: 'service', isDisabled: false}),
        Em.Object.create({name: 'component', isDisabled: false}),
        Em.Object.create({name: 'scope', isDisabled: false})
      ]);
    });

    describe('Host Alert Definition', function () {

      beforeEach(function () {
        controller.changeType('Host Alert Definition');
      });

      it('all configs are disabled', function () {
        expect(controller.get('configs').everyProperty('isDisabled', true)).to.be.true;
      });
      it('service.options = ["Ambari"]', function () {
        expect(controller.get('configs').findProperty('name', 'service').get('options')).to.eql(['Ambari']);
      });
      it('service.value = "Ambari"', function () {
        expect(controller.get('configs').findProperty('name', 'service').get('value')).to.equal('Ambari');
      });
      it('component.value = "Ambari Agent"', function () {
        expect(controller.get('configs').findProperty('name', 'component').get('value')).to.equal('Ambari Agent');
      });
      it('scope.options = ["Host"]', function () {
        expect(controller.get('configs').findProperty('name', 'scope').get('options')).to.eql(['Host']);
      });
      it('isDisabled.value = "Host"', function () {
        expect(controller.get('configs').findProperty('name', 'scope').get('value')).to.equal('Host');
      });
    });

    describe('alert_type_service', function () {

      beforeEach(function () {
        controller.changeType('alert_type_service');
      });
      it('all configs are not disabled', function () {
        expect(controller.get('configs').everyProperty('isDisabled', false)).to.be.true;
      });
      it('service.options = ["service1", "service2"]', function () {
        expect(controller.get('configs').findProperty('name', 'service').get('options')).to.eql(['service1', 'service2']);
      });
      it('service.value = "service1"', function () {
        expect(controller.get('configs').findProperty('name', 'service').get('value')).to.equal('service1');
      });
      it('component.value = "No component"', function () {
        expect(controller.get('configs').findProperty('name', 'component').get('value')).to.equal('No component');
      });
      it('scope.options = ["scope1", "scope2"]', function () {
        expect(controller.get('configs').findProperty('name', 'scope').get('options')).to.eql(['scope1', 'scope2']);
      });
      it('scope.value = "scope1"', function () {
        expect(controller.get('configs').findProperty('name', 'scope').get('value')).to.equal('scope1');
      });
    });

  });

  describe('#renderCommonWizardConfigs()', function () {

    it('should return correct number of configs', function () {

      var result = controller.renderCommonWizardConfigs();

      expect(result.length).to.equal(6);

    });

  });

  describe('#getConfigsValues()', function () {

    it('should create key-value map from configs', function () {

      controller.set('configs', [
        Em.Object.create({name: 'name1', value: 'value1'}),
        Em.Object.create({name: 'name2', value: 'value2'}),
        Em.Object.create({name: 'name3', value: 'value3'})
      ]);

      var result = controller.getConfigsValues();

      expect(result).to.eql([
        {name: 'name1', value: 'value1'},
        {name: 'name2', value: 'value2'},
        {name: 'name3', value: 'value3'}
      ]);

    });

  });

});
