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

App.PigScriptResultsNavView = Em.View.extend({
  tagName:'span',
  selectedBinding: 'controller.category',
  navResultsView : Ember.CollectionView.extend({
    classNames: ['nav nav-pills nav-results pull-right'],
    tagName: 'ul',
    content: function () { 
      var navs = [ 
        {name:'edit',route:'job',label:Em.I18n.t('common.edit')},
        {name:'results',route:'job.results',label:Em.I18n.t('job.results')},
        //{name:'logs',route:'#',label:Em.I18n.t('job.logs')}
      ];
      return navs;
    }.property(),
    itemViewClass: Ember.View.extend(Ember.ViewTargetActionSupport,{
      actions:{
        switch:function (argument) {
          return this.send('navigate',argument)
        }
      },
      tagName: 'li',
      template: Ember.Handlebars.compile('<a href="#" {{action "switch" view.content target="view"}} >{{view.content.label}}</a>'),
      classNameBindings: ['isActive:active'],
      isActive: function () {
        return this.get('content.name') === this.get('parentView.parentView.selected');
      }.property('content.name', 'parentView.parentView.selected')
    })
  })
});
