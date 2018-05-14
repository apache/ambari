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

import Ember from 'ember';

export default Ember.Component.extend({
  tagName: 'tr',
  selectedValue: '',

  didReceiveAttrs() {
    this._super(...arguments);
    let selectedParameter = this.get('hiveParameters').filterBy('name', this.get('setting.key'));
    if (selectedParameter.get('length') === 1) {
      this.set('selectedParam', selectedParameter[0]);
      this.set('selectedValue', this.get('setting.value'));
    }
  },
  setUserSettingsAddOption: function (list, term) {
    let filteredList = list.filter(x => x.get('name').toLowerCase().indexOf('Add') !== -1);
    if (filteredList.get('length') > 0) {
      list.removeObject(filteredList.objectAt(0));
    }

    list.unshiftObject(Ember.Object.create({name: `Add '${term}' to list`, actualValue: term}));
    return list;
  },

  validate() {
    let value = this.get('selectedValue');
    let setting = this.get('selectedParam');
    let error = "";
    if (Ember.isEmpty(value)) {
      return {valid: false, error: "Value cannot be empty"};
    }

    if (Ember.isEmpty(setting.get('values')) && Ember.isEmpty(setting.get('validate'))) {
      return {valid: true};
    }

    if (setting.get('values') && setting.get('values').mapBy('value').contains(value.toLowerCase())) {
      return {valid: true};
    } else if (setting.get('values')) {
      error = `Value should be in (${setting.get('values').mapBy('value').join(', ')})`;
    }

    if (setting.get('validate') && setting.get('validate').test(value)) {
      return {valid: true};
    } else if (setting.get('validate')) {
      error = `Value should be matching regex ${setting.get('validate')}`;
    }

    return {valid: false, error: error};
  },

  actions: {
    searchAction(term) {
      this.set('currentSearchField', term);
      // Check for partial Matches
      let filteredList = this.get('hiveParameters').filter(x => x.get('name').toLowerCase().indexOf(term.toLowerCase()) !== -1);
      //check for exact matches
      if ((filteredList.get('length') !== 1) || (filteredList[0].get('name') !== term)) {
        filteredList = this.setUserSettingsAddOption(filteredList, term);
      }
      return filteredList;
    },
    selectionMade(selection, list) {
      this.get('hiveParameters').setEach('disable', false);
      if (selection.get('name').startsWith('Add')) {
        let actualValue = selection.get('actualValue');
        let newParam = Ember.Object.create({name: actualValue, disabled: true});
        this.set('selectedParam', newParam);
        this.get('hiveParameters').unshiftObject(newParam);
      } else {
        selection.set('disabled', true);
        this.set('selectedParam', selection);
      }
    },
    cancel() {
      this.set('setting.editMode', false);
      this.sendAction('cancelAction', this.get('setting'));
    },
    update() {
      let validationResult = this.validate();
      if(validationResult.valid) {
        let selected = this.get('selectedParam');
        this.set('setting.key', selected.get('name'));
        this.set('setting.value', this.get('selectedValue') || '');
        this.sendAction('updateAction', this.get('setting'));
      } else {
        this.set('invalid', true);
        this.set('currentError', validationResult.error);
      }

    }
  }
});
