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

export default Ember.Component.extend(Ember.I18n.TranslateableProperties, {
  tagName: 'dropdown',

  selectedLabel: function () {
    var value;

    //if there's an item selected, retrieve the property to be displayed as a label
    if (this.get('selectedValue') && this.get('labelPath')) {
      value = this.get('selectedValue').get(this.get('labelPath'));

      if (value) {
        return value;
      }
    }

    //else if a default label has been provided, use it as the selected label.
    if (this.get('defaultLabel')) {
      return this.get('defaultLabel');
    }
  }.property('selectedValue'),

  didInsertElement: function () {
    //if no selected item nor defaultLabel, set the selected value
    if (!this.get('selectedValue') && !this.get('defaultLabel') && this.get('items')) {
      this.set('selectedValue', this.get('items').objectAt(0));
    }
  },

  actions: {
    select: function (item){
      this.set('selectedValue', item);
    },

    add: function () {
      this.sendAction('itemAdded');
    },

    edit: function (item) {
      this.sendAction('itemEdited', item);
    },

    remove: function (item) {
      this.sendAction('itemRemoved', item);
    }
  }
});
