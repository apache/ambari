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

App.RadioButtonInputComponent = Ember.View.extend({
    tagName : "input",
    type : "radio",
    attributeBindings : [ "type", "value", "checked:checked" ],
    click : function() {
      this.set("selection", this.get('value'))
    }
});

App.RadioButtonComponent = Em.Component.extend({
  tagName:'label',
  classNames:['btn btn-default'],
  classNameBindings:['isActive:active'],

  //arguments
  selection:null,
  label:null,
  value:null, 
  click : function() {
    this.set("selection", this.get('value'))
  },
  isActive : function() {
    return this.get("value") == this.get("selection");   
  }.property("selection"),
  layout:Em.Handlebars.compile('{{label}} {{radio-button-input selection=selection value=value checked=isActive}}')
});
