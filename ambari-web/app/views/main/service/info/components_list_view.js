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

App.SummaryMasterComponentsView = Em.View.extend({
  templateName: require('templates/main/service/info/summary/master_components'),
  mastersCurrentLength: 0,
  mastersComp: [],
  mastersCompWillChange: function() {
    this.removeTooltips();
  }.observesBefore('mastersComp.length'),
  mastersCompDidChange: function() {
    this.attachTooltip();
  }.observes('mastersComp.length'),

  removeTooltips: function() {
    if ($('[rel=SummaryComponentHealthTooltip]').length) {
      $('[rel=SummaryComponentHealthTooltip]').tooltip('destroy');
    }
  },

  attachTooltip: function() {
    if ($('[rel=SummaryComponentHealthTooltip]').length) {
      App.tooltip($('[rel=SummaryComponentHealthTooltip]'));
    }
  },

  didInsertElement: function() {
    this.attachTooltip();
  },

  willDestroyElement: function() {
    $('[rel=SummaryComponentHealthTooltip]').tooltip('destroy');
  }
});

App.SummaryClientComponentsView = Em.View.extend({
  templateName: require('templates/main/service/info/summary/client_components'),
  clientsObj: []
});

App.SummarySlaveComponentsView = Em.View.extend({
  templateName: require('templates/main/service/info/summary/slave_components'),
  slavesObj: []
});
