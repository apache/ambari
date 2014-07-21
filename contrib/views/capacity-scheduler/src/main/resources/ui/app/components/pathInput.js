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

App.PathInputComponent = Em.Component.extend({
  layoutName:'components/pathInput',
  actions:{
    add:function () {
      var path = this.get('path'),
          basedir = path.substr(0,path.lastIndexOf('.'));

      if (this.get('pathMap').contains(path)) {
        this.sendAction('action',path);
        this.set('activeFlag',false);
      } else if (this.get('pathMap').contains(basedir)) {
        this.sendAction('action',basedir,path.substr(path.lastIndexOf('.')+1));
        this.set('activeFlag',false);
      }
    },
    cancel:function () {
      this.set('activeFlag',false);
    }
  }, 
  queues:[],
  activeFlag:false,
  pathMap:Em.computed.mapBy('queues','path'),
  path:'',
  inputFieldView: Em.TextField.extend({
    classNames:['form-control newQPath'],
    action:'add',
    pathSource:[],
    placeholder:"Enter queue path...",
    typeaheadInit:function () {
      $(this.get('element')).typeahead({
          source: this.get('pathSource'),
          matcher: function (item) {
            return ~item.toLowerCase().indexOf(this.query.toLowerCase())
          },
          minLength:0,
          items:100,
          scrollHeight:5
      }).focus();
    }.observes('pathSource').on('didInsertElement'),
  })
});