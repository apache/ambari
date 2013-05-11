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

// This logic is substituted by MainAdminView for now.
App.MainAdminMenuView = Em.CollectionView.extend({
  //contentBinding: 'controller',
  /*content: [
    {
      route:'user',
      label:'Users'
    },
    {
      route:'security',
      label:'Security'
    },
    {
      route:'cluster',
      label:'Cluster'
    }
    /*,
    {
      route:'authentication',
      label:'Authentication'
    },

{
      route: 'user',
      label: 'Users'

    },
    {
      route: 'security',
      label: 'Security'
    }/*,
     {
     route:'authentication',
     label:'Authentication'
     },

     {
     route:'audit',
     label:'Audit'
     }*/
    /*,
     {
     route:'advanced',
     label:'Advanced'
     }

  ],
  tagName: "ul",
  classNames: ["nav", "nav-list"],

  init: function () {
    this._super();
    this.activateView(); // default selected menu
  },

  activateView: function () {
    var route = App.get('router.mainAdminController.category');
    $.each(this._childViews, function () {
      this.set('active', (this.get('content.route') == route ? "active" : ""));
    });
  }.observes('App.router.mainAdminController.category'),

  itemViewClass:Em.View.extend({
    classNameBindings:["active"],
    active:"",
    template:Ember.Handlebars.compile('<a class="text-center" {{action adminNavigate view.content.route }} href="#"> {{unbound view.content.label}}</a>')
  })
*/
});