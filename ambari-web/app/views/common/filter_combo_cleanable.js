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


/**
 * Filter component with custom dropdown and cross-circled cleaner. Build on Twitter Bootstrap styles
 * @type {*}
 */
App.FilterComboCleanableView = Ember.View.extend({
  templateName: require('templates/common/filter_combo_cleanable'),

  classNames: ['filter-combobox', 'input-append'],

  placeHolder: function(){
    return this.t('common.combobox.placeholder');
  }.property(),

  /**
   * Onclick handler for dropdown menu
   * @param event
   */
  selectFilterColumn: function(event){
    var column = event.context;
    column.set('selected', !column.get('selected'));
  },

  filterNotEmpty: function(target){
    return (this.get('filter').length > 0);
  }.property('filter'),

  /**
   * clear Filter textfield
   */
  clearFilter: function() {
    this.set('filter', '');
  }
});
