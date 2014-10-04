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

App.BreadCrumbsComponent = Ember.CollectionView.extend({
  classNames: ['breadcrumb pull-left'],
  tagName: 'ul',
  path:'',
  content: function (argument) {
    var crumbs = [];
    var currentPath = this.get('path').match(/((?!\/)\S)+/g)||[];
    currentPath.forEach(function (cur,i,array) {
      return crumbs.push({name:cur,path:'/'+array.slice(0,i+1).join('/')});
    });
    crumbs.unshift({name:'/',path:'/'});
    crumbs.get('lastObject').last = 'true';
    return crumbs;
  }.property('path'),
  itemViewClass: Ember.View.extend({
    classNameBindings: ['isActive:active'],
    template: Ember.Handlebars.compile("{{#link-to 'files' (query-params path=view.content.path)}}{{view.content.name}}{{/link-to}}"),
    isActive: function () {
      return this.get('content.last');
    }.property('content'),
    click:function () {
      if (this.get('isActive')) {
        this.get('controller').send('refreshDir');
      }
    }
  })
});
