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
describe('App.ListConfigWidgetView', function () {

  beforeEach(function () {

    view = App.ListConfigWidgetView.create({
      initPopover: Em.K,
      config: Em.Object.create({
        validate: App.ServiceConfigProperty.prototype.validate,
        name: 'a.b.c',
        savedValue: '2,1',
        value: '2,1',
        filename: 'f1',
        isFinal: false,
        supportsFinal: true,
        stackConfigProperty: Em.Object.create({
          valueAttributes: {
            entries: [
              {
                value: '1',
                label: 'first label',
                description: '1'
              },
              {
                value: '2',
                label: 'second label',
                description: '2'
              },
              {
                value: '3',
                label: 'third label',
                description: '3'
              },
              {
                value: '4',
                label: '4th label',
                description: '4'
              },
              {
                value: '5',
                label: '4th label',
                description: '5'
              }
            ],
            selection_cardinality: '3'
          }
        })
      }),
      controller: App.MainServiceInfoConfigsController.create({})
    });
    view.willInsertElement();
    view.didInsertElement();

  });

  describe('#displayVal', function () {

    it('init value', function () {
      expect(view.get('displayVal')).to.equal('second label, first label');
    });

    it('deselect all', function () {
      view.get('options').setEach('isSelected', false);
      expect(view.get('displayVal')).to.equal(Em.I18n.t('services.service.widgets.list-widget.nothingSelected'));
    });

    it('check that value is trimmed', function () {
      view.get('options').setEach('isSelected', true);
      expect(view.get('displayVal').endsWith(' ...')).to.be.true;
    });

  });

  describe('#calculateOptions', function () {

    it('should create options for each entry', function () {
      view.set('options', []);
      view.calculateOptions();
      expect(view.get('options.length')).to.equal(view.get('config.stackConfigProperty.valueAttributes.entries.length'));
    });

    it('should selected options basing on `value`-property', function () {
      expect(view.get('options').mapProperty('isSelected')).to.eql([true, true, false, false, false]);
    });

    it('should set order to the options basing on `value`-property', function () {
      expect(view.get('options').mapProperty('order')).to.eql([2, 1, 0, 0, 0]);
    });

    it('should disable options basing on `valueAttributes.selection_cardinality`-property', function () {
      expect(view.get('options').everyProperty('isDisabled', false)).to.be.true;
    });

  });

  describe('#calculateInitVal', function () {

    it('should take only selected options', function () {
      expect(view.get('val').length).to.equal(2);
    });

    it('should set `val` empty if `value` is empty', function() {
      view.set('val', [{}]);
      view.set('config.value', '');
      view.calculateInitVal();
      expect(view.get('val')).to.eql([]);
    });

  });

  describe('#calculateVal', function () {

    it('value updates if some option', function () {
      view.toggleOption({context: view.get('options')[2]});
      expect(view.get('config.value')).to.equal('2,1,3');
      view.toggleOption({context: view.get('options')[1]});
      expect(view.get('config.value')).to.equal('1,3');
      view.toggleOption({context: view.get('options')[1]});
      expect(view.get('config.value')).to.equal('1,3,2');
    });

  });

  describe('#restoreValue', function () {

    beforeEach(function() {
      sinon.stub(view, 'restoreDependentConfigs', Em.K);
      sinon.stub(view.get('controller'), 'removeCurrentFromDependentList', Em.K)
    });
    afterEach(function() {
      view.restoreDependentConfigs.restore();
      view.get('controller.removeCurrentFromDependentList').restore();
    });
    it('should restore saved value', function () {
      view.toggleOption({context: view.get('options')[0]});
      view.toggleOption({context: view.get('options')[1]});
      view.toggleOption({context: view.get('options')[2]});
      expect(view.get('config.value')).to.equal('3');
      view.restoreValue();
      expect(view.get('config.value')).to.equal('2,1');
      expect(view.get('controller.removeCurrentFromDependentList')).to.be.called
    });

  });

  describe('#toggleOption', function () {

    it('should doesn\'t do nothing if maximum number of options is selected', function () {
      view.toggleOption({context: view.get('options')[2]});
      expect(view.get('options')[2].get('isSelected')).to.be.true;
      expect(view.get('options')[3].get('isDisabled')).to.be.true;
      expect(view.get('options')[3].get('isSelected')).to.be.false;
      expect(view.get('options')[4].get('isDisabled')).to.be.true;
      expect(view.get('options')[4].get('isSelected')).to.be.false;
      view.toggleOption({context: view.get('options')[3]});
      expect(view.get('options')[3].get('isDisabled')).to.be.true;
      expect(view.get('options')[3].get('isSelected')).to.be.false;
    });

  });

  describe('#checkSelectedItemsCount', function () {

    beforeEach(function () {
      view.set('config.stackConfigProperty.valueAttributes.selection_cardinality', '1+');
      view.parseCardinality();
    });

    it('should check minimum count of the selected items', function () {
      view.get('options').setEach('isSelected', false);
      expect(view.get('config.errorMessage')).to.have.property('length').that.is.least(1);
      view.get('options').setEach('isSelected', true);
      expect(view.get('config.errorMessage')).to.equal('');
    });
  });

  describe('#onOptionsChangeBeforeRender', function () {

    beforeEach(function () {
      sinon.stub(view, 'calculateOptions', Em.K);
      sinon.stub(view, 'calculateInitVal', Em.K);
      view.onOptionsChangeBeforeRender();
    });

    afterEach(function () {
      view.calculateOptions.restore();
      view.calculateInitVal.restore();
    });

    it('should calculate options array', function () {
      expect(view.calculateOptions.calledOnce).to.be.true;
    });

    it('should calculate initial value', function () {
      expect(view.calculateInitVal.calledOnce).to.be.true;
    });

  });

  describe('#onOptionsChangeAfterRender', function () {

    var cases = [
      {
        calculateValCallCount: 1,
        isValueCompatibleWithWidget: true,
        title: 'correct value'
      },
      {
        calculateValCallCount: 0,
        isValueCompatibleWithWidget: false,
        title: 'incorrect value'
      }
    ];

    cases.forEach(function (item) {

      describe(item.title, function () {

        beforeEach(function () {
          sinon.stub(view, 'addObserver', Em.K);
          sinon.stub(view, 'calculateVal', Em.K);
          sinon.stub(view, 'checkSelectedItemsCount', Em.K);
          sinon.stub(view, 'isValueCompatibleWithWidget').returns(item.isValueCompatibleWithWidget);
          view.onOptionsChangeAfterRender();
        });

        afterEach(function () {
          view.addObserver.restore();
          view.calculateVal.restore();
          view.checkSelectedItemsCount.restore();
          view.isValueCompatibleWithWidget.restore();
        });

        it('observers registration', function () {
          expect(view.addObserver.calledTwice).to.be.true;
        });

        it('calculateVal observer', function () {
          expect(view.addObserver.firstCall.args).to.eql(['options.@each.isSelected', view, view.calculateVal]);
        });

        it('checkSelectedItemsCount observer', function () {
          expect(view.addObserver.secondCall.args).to.eql(['options.@each.isSelected', view, view.checkSelectedItemsCount]);
        });

        it('value calculation', function () {
          expect(view.calculateVal.callCount).to.equal(item.calculateValCallCount);
        });

        it('should check selected items count', function () {
          expect(view.checkSelectedItemsCount.calledOnce).to.be.true;
        });

      });

    });

  });

  describe('#entriesObserver', function () {

    beforeEach(function () {
      sinon.stub(view, 'removeObserver', Em.K);
      sinon.stub(view, 'onOptionsChangeBeforeRender', Em.K);
      sinon.stub(view, 'initIncompatibleWidgetAsTextBox', Em.K);
      sinon.stub(view, 'onOptionsChangeAfterRender', Em.K);
      view.entriesObserver();
    });

    afterEach(function () {
      view.removeObserver.restore();
      view.onOptionsChangeBeforeRender.restore();
      view.initIncompatibleWidgetAsTextBox.restore();
      view.onOptionsChangeAfterRender.restore();
    });

    it('observers removal', function () {
      expect(view.removeObserver.calledTwice).to.be.true;
    });

    it('calculateVal observer', function () {
      expect(view.removeObserver.firstCall.args).to.eql(['options.@each.isSelected', view, view.calculateVal]);
    });

    it('checkSelectedItemsCount observer', function () {
      expect(view.removeObserver.secondCall.args).to.eql(['options.@each.isSelected', view, view.checkSelectedItemsCount]);
    });

    it('first options change handler', function () {
      expect(view.onOptionsChangeBeforeRender.calledOnce).to.be.true;
    });

    it('incompatible value processing', function () {
      expect(view.initIncompatibleWidgetAsTextBox.calledOnce).to.be.true;
    });

    it('second options change handler', function () {
      expect(view.onOptionsChangeAfterRender.calledOnce).to.be.true;
    });


  });


});
