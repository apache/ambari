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

App.MainHostDetailsView = Em.View.extend({
  templateName: require('templates/main/host/details'),
  content: function(){
    return App.router.get('mainHostDetailsController.content');
  }.property('App.router.mainHostDetailsController.content'),
  maintenance: function(){
    var options = [{action: 'deleteHost', 'label': 'Delete Host'}];
    return options;
  }.property('controller.content')
});

App.MainHostDetailsOperations = Em.View.extend({
  content: null,
  classNames: ['background-operations'],
  classNameBindings: ['isOpen'],
  isOpen: false,
  logDetails: null,
  isOpenShowLog: false,
  iconClass: function(){
    return this.get('isOpen') ? 'icon-minus' : 'icon-plus';
  }.property('isOpen'),
  openDetails: function(){
    this.set('isOpen', !this.get('isOpen'))
  },
  showOperationLog:function(){
    this.set('isOpenShowLog', !this.get('isOpenShowLog'))
  }
});