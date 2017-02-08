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
import datatypes from '../configs/datatypes';

export default Ember.Component.extend({
  tagName: 'tr',
  advancedOption: false,
  datatypes: Ember.copy(datatypes),
  editMode: false,



  hasPrecision: Ember.computed.oneWay('column.type.hasPrecision'),
  hasScale: Ember.computed.oneWay('column.type.hasScale'),

  columnMetaType: null,


  didInsertElement() {
    Ember.run.later( () => {
      this.$('input').focus();
    });
  },
  didReceiveAttrs() {
    if(this.get('column.isPartitioned')) {
      this.set('columnMetaType', 'partitioned');
    } else if(this.get('column.isPartitioned')) {
      this.set('columnMetaType', 'clustered');
    } else {
      this.set('columnMetaType');
    }
  },

  actions: {
    typeSelectionMade(datatype) {
      this.set('column.type', datatype);
    },

    advanceOptionToggle() {
      this.toggleProperty('advancedOption');
    },

    edit() {
      this.set('column.editing', true);
      Ember.run.later(() => {
        this.$('input').focus();
      });
    },

    delete() {
      console.log('deleting column');
      this.sendAction('columnDeleted', this.get('column'));
    }
  }
});
