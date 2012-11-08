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

App.MainAppsItemView = Em.View.extend({
  tagName: 'tr',
  classNames : ['containerRow'],
  templateName:require('templates/main/apps/item'),
  menuTabs:[
    Em.Object.create({
      label:'DAG',
      active:'active',
      content:'App.MainAppsItemDagView'
    }),
    Em.Object.create({
      label:'Charts',
      active:'',
      content:'App.MainAppsItemBarView'
    })
  ],
  content:function(){
    return App.router.get('mainAppsItemController.content');
  }.property('App.router.mainAppsItemController.content'),
  activeTab:null,
  switchTab:function(event){
    var tabs = this.get('menuTabs');
    for(var i = 0; i < tabs.length; i++){
      if(tabs[i] === event.context){
        tabs[i].set('active', 'active');
      }
      else {
        tabs[i].set('active', '');
      }
    }
    this.set('activeTab', event.context);
  },
  didInsertElement: function(){
    var tabs = this.get('menuTabs');
    tabs[0].set('active', 'active');
    tabs[1].set('active', '');
  },
  containerView:Em.ContainerView.extend({
    jobs:function(){
      return this.get('parentView.content').get('jobs');
    }.property('parentView.content'),
    onchange:function(){
      var view = this.get('parentView.activeTab.content').split('.')[1];
      view = App[view].create();

      if(this.get('childViews')){
        this.get('childViews').get('firstObject').destroy();
      }
      this.get('childViews').pushObject(view);
    }.observes('parentView.activeTab')
  })
});
