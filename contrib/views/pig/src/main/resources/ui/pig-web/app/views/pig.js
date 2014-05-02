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

App.PigView = Em.View.extend({
  selectedBinding: 'controller.category',
  navs: Ember.computed(function() {
    var items = [ 
      {name:'scripts',url:'pig.scriptList',label: Em.I18n.t('scripts.scripts')},
      {name:'udfs',url:'pig.udfs',label:Em.I18n.t('udfs.udfs')},
      {name:'history',url:'pig.history',label:Em.I18n.t('common.history')}
    ];
    this.get('controller.openScripts').forEach(function(scripts){
      items.push(scripts);
    });
    return items;
  }).property('controller.openScripts'),
  navItemsView : Ember.CollectionView.extend({
    classNames: ['list-group'],
    tagName: 'div',
    content: function () { 
      return this.get('parentView.navs')
    }.property('parentView.navs'),
    itemViewClass: Ember.View.extend(Ember.ViewTargetActionSupport,{
      tagName: 'a',
      templateName: 'pig/util/script-nav',
      classNames: ['list-group-item pig-nav-item'],
      classNameBindings: ['isActive:active'],
      action: 'gotoSection',
      click: function(e) {
        if (e.target.type=="button") {
          return false;
        };
        this.triggerAction({
          actionContext:this.content
        }); 
      },
      isActive: function () {
        return this.get('content.name') === this.get('parentView.parentView.selected');
      }.property('content.name', 'parentView.parentView.selected')
    })
  })
});
