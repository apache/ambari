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
  classNames: ['list-filter'],
  header: '',
  subHeader: '',
  caseInsensitive: true,
  items: [],
  filterText: '',
  emptyFilterText: Ember.computed('filterText', function() {
    return this.get('filterText').length === 0;
  }),
  filteredItems: Ember.computed('filterText', 'items.@each', function() {
    return this.get('items').filter((item) => {
      let filterText = this.get('caseInsensitive') ? this.get('filterText').toLowerCase() : this.get('filterText');
      let itemName = this.get('caseInsensitive') ? item.get('name').toLowerCase() : item.get('name');
      return itemName.indexOf(filterText) !== -1;
    });
  }),

  actions: {
    enableFilter() {
      this.$('input').focus();
    },

    disableFilter() {
      this.set('filterText', '');
    }
  }
});
