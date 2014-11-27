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

App.AlertSeverityFilterView = Em.View.extend({

  templateName: require('templates/main/alerts/severity_filter'),

  /**
   * Array of Boolean values binded to checkboxes
   * @type {Array}
   */
  selection: [],

  didInsertElement: function () {
    this.set('okChecked', this.get('selection')[0]);
    this.set('warningChecked', this.get('selection')[1]);
    this.set('criticalChecked', this.get('selection')[2]);
    this.set('unknownChecked', this.get('selection')[3]);
    this.addObserver('okChecked', this, 'onChange');
    this.addObserver('warningChecked', this, 'onChange');
    this.addObserver('criticalChecked', this, 'onChange');
    this.addObserver('unknownChecked', this, 'onChange');
  },

  okChecked: true,

  warningChecked: true,

  criticalChecked: true,

  unknownChecked: true,

  onChange: function () {
    this.set('selection', [this.get('okChecked'), this.get('warningChecked'), this.get('criticalChecked'), this.get('unknownChecked')]);
  }

});
