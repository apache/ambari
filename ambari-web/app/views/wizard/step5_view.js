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
var lazyloading = require('utils/lazy_loading');

App.WizardStep5View = Em.View.extend({

  templateName:require('templates/wizard/step5'),

  didInsertElement:function () {
    this.get('controller').loadStep();
  },

  body: Em.I18n.t('installer.step5.body')

});

App.SelectHostView = Em.Select.extend({
  content:[],
  zId:null,
  selectedHost:null,
  componentName:null,
  attributeBindings:['disabled'],
  isLoaded: false,

  change:function () {
    this.get('controller').assignHostToMaster(this.get("componentName"), this.get("value"), this.get("zId"));
  },
  click: function () {
    var source = [];
    var selectedHost = this.get('selectedHost');
    var content = this.get('content');
    if (!this.get('isLoaded') && this.get('controller.isLazyLoading')) {
      //filter out hosts, which already pushed in select
      source = this.get('controller.hosts').filter(function(_host){
        return !content.someProperty('host_name', _host.host_name);
      }, this);
      lazyloading.run({
        destination: this.get('content'),
        source: source,
        context: this,
        initSize: 30,
        chunkSize: 50,
        delay: 200
      });
    }
  },

  didInsertElement:function () {
    this.set("value", this.get("selectedHost"));
  }
});

App.AddControlView = Em.View.extend({
  componentName:null,
  tagName:"span",
  classNames:["badge", "badge-important"],
  template:Ember.Handlebars.compile('+'),

  click:function () {
    this.get('controller').addComponent(this.get('componentName'));
  }
});

App.RemoveControlView = Em.View.extend({
  zId:null,
  componentName:null,
  tagName:"span",
  classNames:["badge", "badge-important"],
  template:Ember.Handlebars.compile('-'),

  click:function () {
    this.get('controller').removeComponent(this.get('componentName'), this.get("zId"));
  }
});
