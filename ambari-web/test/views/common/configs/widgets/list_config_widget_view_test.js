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
      config: Em.Object.create({
        name: 'a.b.c',
        defaultValue: '2,1',
        value: '2,1',
        stackConfigProperty: Em.Object.create({
          valueAttributes: {
            entries: ['1', '2', '3', '4', '5'],
            entry_labels: ['first label', 'second label', 'third label', '4th label', '5th label'],
            entry_descriptions: ['1', '2', '3', '4', '5'],
            selection_cardinality: '3'
          }
        })
      })
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

    it('should trigger error', function () {
      view.set('config.stackConfigProperty.valueAttributes.entry_descriptions', ['1', '2', '3', '4']);
      expect(view.calculateOptions.bind(view)).to.throw(Error, 'assertion failed');
    });

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

    it('should restore default value', function () {
      view.toggleOption({context: view.get('options')[0]});
      view.toggleOption({context: view.get('options')[1]});
      view.toggleOption({context: view.get('options')[2]});
      expect(view.get('config.value')).to.equal('3');
      view.restoreValue();
      expect(view.get('config.value')).to.equal('2,1');
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

});
