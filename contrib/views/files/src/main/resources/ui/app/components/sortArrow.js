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

App.SortArrowComponent = Em.Component.extend({
  layout:Ember.Handlebars.compile('<i {{bind-attr class=":fa view.asc:fa-chevron-down:fa-chevron-up view.cur::fa-gr view.cur::fa-rotate-270" }} ></i>'),
  classNames:['pull-right'],
  tagName:'span',
  sPs:[],
  sA:false,
  sP:null,
  asc:true,
  cur:false,
  sorting:function () {
    if (this.get('sPs.firstObject') == this.get('sP')) {
      this.set('asc',this.get('sA'));
      this.set('cur',true);
    } else{
      this.set('asc',true);
      this.set('cur',false);
    };
  }.observes('sPs','sA').on('init'),
});
