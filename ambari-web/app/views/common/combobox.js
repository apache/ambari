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

App.Combobox = Em.Select.extend({
  classNames:['combobox'],
  placeholderText:false,
  disabled:false,

  input:function () {
    return this.get('combobox').$element;
  }.property('combobox'),

  button:function () {
    return this.get('combobox').$button;
  }.property('combobox'),

  toggleDisabling:function () {
    var disabled = this.get('disabled') ? 'disabled' : false;

    this.get('input').attr('disabled', disabled);
    this.get('button').attr('disabled', disabled);

  }.observes('disabled'),

  content:function () {
    // convert DS.RecordsArray to array;
    var content = [
      {}
    ];
    var racks = this.get('recordArray');

    racks.forEach(function (cluster, index) {
      content.push(cluster);
    });

    return content;
  }.property('recordArray'),
  clearTextFieldValue:function () {
    var options = [];

    this.get('combobox').$element.val('');
    this.get('combobox').clearTarget();

  },

  test:function () {
    console.warn("qwerty");
  },

  didInsertElement:function () {
    this._super();

    this.set('combobox', this.$().combobox({
      template:'<div class="combobox-container"><input type="text" autocomplete="off" /><button class="add-on btn dropdown-toggle" data-dropdown="dropdown"><span class="caret"/><span class="combobox-clear"><i class="icon-remove"/></span></button></div>'
    }).data('combobox'));

    this.clearTextFieldValue(); // fix of script tags in

    if (this.get('placeholderText')) {
      this.get('combobox').$element.attr('placeholder', this.get('placeholderText'));
    }
  }
});