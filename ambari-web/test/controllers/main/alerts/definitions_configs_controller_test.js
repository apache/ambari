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

describe('App.MainAlertDefinitionConfigsController', function () {

  beforeEach(function () {
    controller = App.MainAlertDefinitionConfigsController.create({
      allServices: ['service1', 'service2', 'service3'],
      allComponents: ['component1', 'component2', 'component3'],
      aggregateAlertNames: ['alertDefinitionName', 'alertDefinitionName2', 'alertDefinitionName3']
    });
  });

  describe('#renderConfigs()', function () {

    beforeEach(function () {
      controller.set('content', Em.Object.create({}));
      sinon.stub(controller, 'renderPortConfigs', function () {
        return [];
      });
      sinon.stub(controller, 'renderMetricConfigs', function () {
        return [];
      });
      sinon.stub(controller, 'renderWebConfigs', function () {
        return [];
      });
      sinon.stub(controller, 'renderScriptConfigs', function () {
        return [];
      });
      sinon.stub(controller, 'renderAggregateConfigs', function () {
        return [];
      });
    });

    afterEach(function () {
      controller.renderPortConfigs.restore();
      controller.renderMetricConfigs.restore();
      controller.renderWebConfigs.restore();
      controller.renderScriptConfigs.restore();
      controller.renderAggregateConfigs.restore();
    });

    it('should call renderPortConfigs method', function () {
      controller.set('content.type', 'PORT');
      controller.renderConfigs();
      expect(controller.renderPortConfigs.calledOnce).to.be.true;
    });

    it('should call renderMetricConfigs method', function () {
      controller.set('content.type', 'METRIC');
      controller.renderConfigs();
      expect(controller.renderMetricConfigs.calledOnce).to.be.true;
    });

    it('should call renderWebConfigs method', function () {
      controller.set('content.type', 'WEB');
      controller.renderConfigs();
      expect(controller.renderWebConfigs.calledOnce).to.be.true;
    });

    it('should call renderScriptConfigs method', function () {
      controller.set('content.type', 'SCRIPT');
      controller.renderConfigs();
      expect(controller.renderScriptConfigs.calledOnce).to.be.true;
    });

    it('should call renderAggregateConfigs method', function () {
      controller.set('content.type', 'AGGREGATE');
      controller.renderConfigs();
      expect(controller.renderAggregateConfigs.calledOnce).to.be.true;
    });

  });

  describe('#renderPortConfigs()', function () {

    it('should render array of configs with correct values', function () {

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

      var result = controller.renderPortConfigs();

      expect(result.length).to.equal(9);
      expect(result.someProperty('value', 'alertDefinitionName')).to.be.true;
      expect(result.someProperty('value', 'alertDefinitionService')).to.be.true;
      expect(result.someProperty('value', 'Component1')).to.be.true;
      expect(result.someProperty('value', 'Host')).to.be.true;
      expect(result.someProperty('value', 'alertDefinitionDescription')).to.be.true;
      expect(result.someProperty('value', 60)).to.be.true;
      expect(result.someProperty('value', '10-20')).to.be.true;
      expect(result.someProperty('value', 'alertDefinitionUri')).to.be.true;
      expect(result.someProperty('value', '777')).to.be.true;
    });

  });

  describe('#renderMetricConfigs()', function () {

    it('should render array of configs with correct values', function () {

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

      var result = controller.renderMetricConfigs();

      expect(result.length).to.equal(10);
      expect(result.someProperty('value', 'alertDefinitionName')).to.be.true;
      expect(result.someProperty('value', 'alertDefinitionService')).to.be.true;
      expect(result.someProperty('value', 'Component1')).to.be.true;
      expect(result.someProperty('value', 'Host')).to.be.true;
      expect(result.someProperty('value', 'alertDefinitionDescription')).to.be.true;
      expect(result.someProperty('value', 60)).to.be.true;
      expect(result.someProperty('value', '10-20')).to.be.true;
      expect(result.someProperty('value', '{\"http\":\"{{mapred-site/mapreduce.jobhistory.webapp.address}}\",\"https\":\"{{mapred-site/mapreduce.jobhistory.webapp.https.address}}\"}')).to.be.true;
      expect(result.someProperty('value', 'property1,\nproperty2')).to.be.true;
      expect(result.someProperty('value', 'jmxValue')).to.be.true;
    });

  });

  describe('#renderWebConfigs()', function () {

    it('should render array of configs with correct values', function () {

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

      var result = controller.renderWebConfigs();

      expect(result.length).to.equal(8);
      expect(result.someProperty('value', 'alertDefinitionName')).to.be.true;
      expect(result.someProperty('value', 'alertDefinitionService')).to.be.true;
      expect(result.someProperty('value', 'Component1')).to.be.true;
      expect(result.someProperty('value', 'Host')).to.be.true;
      expect(result.someProperty('value', 'alertDefinitionDescription')).to.be.true;
      expect(result.someProperty('value', 60)).to.be.true;
      expect(result.someProperty('value', '10-20')).to.be.true;
      expect(result.someProperty('value', '{\"http\":\"{{mapred-site/mapreduce.jobhistory.webapp.address}}\",\"https\":\"{{mapred-site/mapreduce.jobhistory.webapp.https.address}}\"}')).to.be.true;
    });

  });

  describe('#renderScriptConfigs()', function () {

    it('should render array of configs with correct values', function () {

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

      var result = controller.renderScriptConfigs();

      expect(result.length).to.equal(8);
      expect(result.someProperty('value', 'alertDefinitionName')).to.be.true;
      expect(result.someProperty('value', 'alertDefinitionService')).to.be.true;
      expect(result.someProperty('value', 'Component1')).to.be.true;
      expect(result.someProperty('value', 'Host')).to.be.true;
      expect(result.someProperty('value', 'alertDefinitionDescription')).to.be.true;
      expect(result.someProperty('value', 60)).to.be.true;
      expect(result.someProperty('value', '10-20')).to.be.true;
      expect(result.someProperty('value', 'path to script')).to.be.true;
    });

  });

  describe('#renderAggregateConfigs()', function () {

    it('should render array of configs with correct values', function () {

      controller.set('content', Em.Object.create({
        name: 'alertDefinitionName',
        description: 'alertDefinitionDescription'
      }));

      var result = controller.renderAggregateConfigs();

      expect(result.length).to.equal(2);
      expect(result.someProperty('value', 'alertDefinitionName')).to.be.true;
      expect(result.someProperty('value', 'alertDefinitionDescription')).to.be.true;
    });

  });

  describe('#editConfigs()', function () {

    it('should set previousValue, isDisabled for each config and change canEdit flag', function () {

      controller.set('configs', [
        Em.Object.create({value: 'value1', previousValue: '', isDisabled: true}),
        Em.Object.create({value: 'value2', previousValue: '', isDisabled: true}),
        Em.Object.create({value: 'value3', previousValue: '', isDisabled: true})
      ]);

      controller.set('canEdit', false);

      controller.editConfigs();

      expect(controller.get('configs').mapProperty('previousValue')).to.eql(['value1', 'value2', 'value3']);
      expect(controller.get('configs').someProperty('isDisabled', true)).to.be.false;
      expect(controller.get('canEdit')).to.be.true;
    });

  });

  describe('#cancelEditConfigs()', function () {

    it('should set previousValue, isDisabled for each config and change canEdit flag', function () {

      controller.set('configs', [
        Em.Object.create({value: '', previousValue: 'value1', isDisabled: false}),
        Em.Object.create({value: '', previousValue: 'value2', isDisabled: false}),
        Em.Object.create({value: '', previousValue: 'value3', isDisabled: false})
      ]);

      controller.set('canEdit', true);

      controller.cancelEditConfigs();

      expect(controller.get('configs').mapProperty('value')).to.eql(['value1', 'value2', 'value3']);
      expect(controller.get('configs').someProperty('isDisabled', false)).to.be.false;
      expect(controller.get('canEdit')).to.be.false;
    });

  });

  describe('#saveConfigs()', function () {

    beforeEach(function () {
      sinon.spy(App.ajax, 'send');
    });

    afterEach(function () {
      App.ajax.send.restore();
    });

    it('should set previousValue, isDisabled for each config and change canEdit flag', function () {

      controller.set('configs', [
        Em.Object.create({isDisabled: true}),
        Em.Object.create({isDisabled: true}),
        Em.Object.create({isDisabled: true})
      ]);

      controller.set('canEdit', true);

      controller.saveConfigs();

      expect(controller.get('configs').someProperty('isDisabled', false)).to.be.false;
      expect(controller.get('canEdit')).to.be.false;
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
        var result = controller.getPropertiesToUpdate();

        expect(result).to.eql(testCase.result);
      });
    });
  });

})
;
