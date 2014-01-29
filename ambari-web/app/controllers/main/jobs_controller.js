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

App.MainJobsController = App.MainAppsController.extend({

  name:'mainJobsController',

  content: [],

  loaded : false,
  loading : false,

  clearFilters: function () {
    var obj=this.get("filterObject");
    obj.set("id","");
    obj.set("user","");
    obj.set("startTime","");
    obj.set("endTime","");
  },

  //Filter object

  filterObject : Ember.Object.create({
    id:"",
    user:"",
    startTime:"",
    endTime:"",


    allFilterActivated:false,
    filteredDisplayRecords:null,

    viewType:"all",
    viewTypeClickEvent:false

  }),

  columnsName: Ember.ArrayController.create({
    content: [
      { name: Em.I18n.t('jobs.column.id'), index: 0 },
      { name: Em.I18n.t('jobs.column.user'), index: 1 },
      { name: Em.I18n.t('jobs.column.start.time'), index: 2 },
      { name: Em.I18n.t('jobs.column.end.time'), index: 3 },
      { name: Em.I18n.t('jobs.column.duration'), index: 4 }
    ]
  })

})
