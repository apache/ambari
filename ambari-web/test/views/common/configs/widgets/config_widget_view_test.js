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

var view;
describe('App.ConfigWidgetView', function () {

  beforeEach(function () {
    view = App.ConfigWidgetView.create({
      initPopover: Em.K,
      config: Em.Object.create({
        isOriginalSCP: false,
        isPropertyOverridable: false,
        cantBeUndone: false,
        isNotDefaultValue: false
      })
    });
  });

  describe('#undoAllowed', function () {

    Em.A([
      {
        cfg: {
          cantBeUndone: false,
          isNotDefaultValue: false
        },
        view: {
          disabled: false,
          isOriginalSCP: false
        },
        e: false
      },
      {
        cfg: {
          cantBeUndone: true,
          isNotDefaultValue: false
        },
        view: {
          disabled: false,
          isOriginalSCP: false
        },
        e: false
      },
      {
        cfg: {
          cantBeUndone: false,
          isNotDefaultValue: true
        },
        view: {
          disabled: false,
          isOriginalSCP: true
        },
        e: true
      },
      {
        cfg: {
          cantBeUndone: true,
          isNotDefaultValue: true
        },
        view: {
          disabled: true,
          isOriginalSCP: false
        },
        e: false
      }
    ]).forEach(function (test, index) {
        it('test #' + index, function () {
          view.get('config').setProperties(test.cfg);
          view.setProperties(test.view);
          expect(view.get('undoAllowed')).to.equal(test.e);
        });
      });

  });

  describe('#overrideAllowed', function () {

    Em.A([
        {
          cfg: {
            isOriginalSCP: false,
            isPropertyOverridable: false,
            isComparison: false
          },
          e: false
        },
        {
          cfg: {
            isOriginalSCP: true,
            isPropertyOverridable: false,
            isComparison: false
          },
          e: false
        },
        {
          cfg: {
            isOriginalSCP: false,
            isPropertyOverridable: true,
            isComparison: false
          },
          e: false
        },
        {
          cfg: {
            isOriginalSCP: true,
            isPropertyOverridable: true,
            isComparison: false
          },
          e: true
        },
        {
          cfg: {
            isOriginalSCP: false,
            isPropertyOverridable: false,
            isComparison: true
          },
          e: false
        },
        {
          cfg: {
            isOriginalSCP: true,
            isPropertyOverridable: false,
            isComparison: true
          },
          e: false
        },
        {
          cfg: {
            isOriginalSCP: false,
            isPropertyOverridable: true,
            isComparison: true
          },
          e: false
        },
        {
          cfg: {
            isOriginalSCP: true,
            isPropertyOverridable: true,
            isComparison: true
          },
          e: false
        }
      ]).forEach(function (test, index) {
        it('test #' + index, function () {
          view.get('config').setProperties(test.cfg);
          expect(view.get('overrideAllowed')).to.equal(test.e);
        });
      });

  });

  describe('#restoreDependentConfigs', function() {
    beforeEach(function() {
      view = App.ConfigWidgetView.create({
        controller: Em.Object.extend(App.EnhancedConfigsMixin, {
        }).create({
          updateDependentConfigs: function() {},
        }),
        config: Em.Object.create({ name: 'config1'})
      });
    });

    var tests = [
      {
        dependentConfigs: [
          {name: 'dependent1', parentConfigs: ['config1']},
          {name: 'dependent2', parentConfigs: ['config2']},
          {name: 'dependent3', parentConfigs: ['config1']}
        ],
        e: ['dependent2'],
        m: 'when dependent configs has one parent they should be removed'
      },
      {
        dependentConfigs: [
          {name: 'dependent1', parentConfigs: ['config1', 'config2']},
          {name: 'dependent2', parentConfigs: ['config2']},
          {name: 'dependent3', parentConfigs: ['config1']}
        ],
        e: ['dependent1', 'dependent2'],
        m: 'when dependent configs has multiple parents they should not be removed'
      }
    ];

    tests.forEach(function(test) {
      it(test.m, function() {
        view.set('controller._dependentConfigValues', test.dependentConfigs);
        view.restoreDependentConfigs(view.get('config'));
        expect(view.get('controller._dependentConfigValues').mapProperty('name')).to.be.eql(test.e);
      });
    });

    it('when dependent configs has multiple parents appropriate parent config should be removed', function() {
      view.set('controller._dependentConfigValues', [
        {name: 'dependent1', parentConfigs: ['config1', 'config2']},
        {name: 'dependent2', parentConfigs: ['config2', 'config1']},
        {name: 'dependent3', parentConfigs: ['config1']}
      ]);
      view.restoreDependentConfigs(view.get('config'));
      expect(view.get('controller._dependentConfigValues').findProperty('name', 'dependent1').parentConfigs.toArray()).to.be.eql(["config2"]);
      expect(view.get('controller._dependentConfigValues').findProperty('name', 'dependent2').parentConfigs.toArray()).to.be.eql(["config2"]);
      expect(view.get('controller._dependentConfigValues.length')).to.be.eql(2);
    });

    it('dependent config value should be set with inital or saved when it has one parent', function() {
      var ctrl = view.get('controller');
      ctrl.set('stepConfigs', [
        Em.Object.create({
          configs: Em.A([
            Em.Object.create({ name: 'dependent3', savedValue: '1', value: 2, filename: 'some-file.xml' }),
            Em.Object.create({ name: 'dependent2', savedValue: '4', value: '10', filename: 'some-file.xml' })
          ])
        })
      ]);
      view.set('controller._dependentConfigValues', [
        {propertyName: 'dependent1', parentConfigs: ['config1', 'config2'], fileName: 'some-file' },
        {propertyName: 'dependent2', parentConfigs: ['config2', 'config1'], fileName: 'some-file'},
        {propertyName: 'dependent3', parentConfigs: ['config1'], fileName: 'some-file' }
      ]);
      view.restoreDependentConfigs(view.get('config'));
      expect(view.get('controller').findConfigProperty('dependent3', 'some-file.xml').get('value')).to.be.eql('1');
      // config with multi dependency should not be updated
      expect(view.get('controller').findConfigProperty('dependent2', 'some-file.xml').get('value')).to.be.eql('10');
    });

  });

  describe('#isValueCompatibleWithWidget()', function() {
    it('pass validation', function() {
      view.set('config.isValid', true);
      expect(view.isValueCompatibleWithWidget()).to.be.true;
    });

    it('fail validation', function() {
      view.set('config.isValid', false);
      view.set('supportSwitchToTextBox', true);
      expect(view.isValueCompatibleWithWidget()).to.be.false;
    });
  });

  describe('#setRecommendedValue', function () {

    beforeEach(function () {
      sinon.stub(view, 'sendRequestRorDependentConfigs', function () {
        return $.Deferred().resolve().promise();
      });
      sinon.stub(view, 'restoreDependentConfigs', Em.K);
      view.set('config', Em.Object.create({
        value: 1,
        recommendedValue: 1,
        savedValue: 1
      }));
    });

    afterEach(function () {
      view.sendRequestRorDependentConfigs.restore();
      view.restoreDependentConfigs.restore();
    });

    it('should call restoreDependentConfigs if config.value is equal to config.savedValue', function () {
      view.setRecommendedValue();
      expect(view.restoreDependentConfigs.calledOnce).to.be.true;
    });

    it('should not call restoreDependentConfigs if config.value is not equal to config.savedValue', function () {
      view.set('config.savedValue', 2);
      view.setRecommendedValue();
      expect(view.restoreDependentConfigs.called).to.be.false;
    });

  });

});
