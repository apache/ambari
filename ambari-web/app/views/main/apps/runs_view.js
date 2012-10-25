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
var validator = require('utils/validator');
var date = require('utils/date');

App.MainAppsRunsView = Em.View.extend({
  templateName:require('templates/main/apps/runs'),
  classNames:['table', 'dataTable'],
  didInsertElement:function () {
    var oTable = this.$('#dataTable').dataTable({
      "aoColumns":[
        null,
        { "sType":"ambari-date" },
        null,
        null,
        null,
        null,
        null,
        null
      ]
    });
  },
  RunsCheckboxView:Em.Checkbox.extend({
    content:null,
    isChecked:false,
    change:function (event) {

    }
  }),
  content:function () {
    var content = App.router.get('mainAppsItemController.content').get('runs');
    content.forEach(function (item) {
      item.set('numJobs', item.get('jobs').get('content').length);
      var startTime = item.get('startTime');
      var lastUpdateTime = item.get('lastUpdateTime');
      item.set('startTime', date.dateFormat(item.get('startTime')));
      if (validator.isValidInt(lastUpdateTime)) {
        item.set('lastUpdateTime', date.dateFormatInterval((lastUpdateTime - startTime) / 1000));
      }
    });
    return content;
  }.property('App.router.mainAppsItemController.content'),
  appItem:function () {
    return App.router.get('mainAppsItemController.content');
  }.property('App.router.mainAppsItemController.content')
});
