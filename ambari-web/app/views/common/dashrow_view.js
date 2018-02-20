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

App.DashRow = Ember.View.extend({
  
  templateName: require('templates/common/dashrow'),
  
  hasMenu: Em.computed.or('menu', 'menuClass'),

  encodeMenu: false,

  menu: null,

  //Define a property named menuClass which extends Ember.View to use an arbitrary Handlebars template as the menu.
  //The template should contain a structure compatible with Bootstrap dropdowns, such as:
  // <ul class="dropdown-menu dropdown-menu-right">
  //   <li><a href="#">...</a></li>
  //   <li><a href="#">...</a></li>
  //   <li><a href="#">...</a></li>
  // </ul>

  encodeHeader: false,

  header: '&nbsp;',
  
  //Define a property named headerClass which extends Ember.View to use an arbitrary Handlebars template as the header.

  //set this to provide an id for the collapsible part
  //so it can be targeted by the Bootstrap collapse plugin
  collapseId: null,

  collapseTarget: function () {
    const collapseId = this.get('collapseId');
    
    if (collapseId) {
      return "#" + collapseId;
    }
    
    return collapseId;
  }.property('collapseId'),

  encodeBody: false,

  body: '&nbsp;',
    
  //Define a property named bodyClass which extends Ember.View to use an arbitrary Handlebars template as the body.
    
  didInsertElement: function () {
    this.$().find('.dropdown-toggle').dropdown();
    this.$().find('.collapse').collapse({ toggle: false });
  }
});
