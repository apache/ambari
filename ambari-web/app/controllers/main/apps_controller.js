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
var misc = require('utils/misc');
var date = require('utils/date');

App.MainAppsController = Em.ArrayController.extend({

  name:'mainAppsController',
  content: [],

  loaded : false,
  loading : false,

  /**
   * List of users.
   * Will be used for filtering in user column.
   * Go to App.MainAppsView.userFilterView for more information
   */
  users: function () {
    return this.get('content').mapProperty("userName").uniq().map(function(userName){
      return {
        name: userName,
        checked: false
      };
    });
  }.property('content.length'),

  loadRuns:function () {

    this.set('loading', true);
    var self = this;

    //var runsUrl = App.testMode ? "/data/apps/runs.json" : App.apiPrefix + "/jobhistory/workflow?orderBy=startTime&sortDir=DESC&limit=" + App.maxRunsForAppBrowser;
    var runsUrl = App.get('testMode') ? "/data/apps/runs.json" : App.get('apiPrefix') + this.get("runUrl");

    App.HttpClient.get(runsUrl, App.runsMapper, {
      complete:function (jqXHR, textStatus) {
        self.set('loading', false);
        self.set('loaded', true);
      }
    });
  },

  //Pagination Object

  paginationObject:{
    iTotalDisplayRecords :0,
    iTotalRecords:0,
    startIndex:0,
    endIndex:0
  },

  /*
   Set number of filtered jobs when switching to all jobs
   */
  iTotalDisplayRecordsObserver:function(){
    if(this.get("filterObject.allFilterActivated")){
      this.set("filterObject.allFilterActivated", false);
    }else{
      this.set("filterObject.filteredDisplayRecords",this.get("paginationObject.iTotalDisplayRecords"));
    }
  }.observes("paginationObject.iTotalDisplayRecords"),


  //Filter object

  filterObject : Ember.Object.create({
    sSearch_0:"",
    sSearch_1:"",
    sSearch_2:"",
    sSearch_3:"",
    minJobs:"",
    maxJobs:"",
    minInputBytes:"",
    maxInputBytes:"",
    minOutputBytes:"",
    maxOutputBytes:"",
    minDuration:"",
    maxDuration:"",
    minStartTime:"",
    maxStartTime:"",
    sSearch:"",
    iDisplayLength:"",
    iDisplayStart:"",
    iSortCol_0:"",
    sSortDir_0:"",

    allFilterActivated:false,
    filteredDisplayRecords:null,

    viewType:"all",
    viewTypeClickEvent:false,

    /**
     * Direct binding to job filter field
     */
    runType:"",
    onRunTypeChange:function(){
      if(this.runType == "MapReduce"){
        this.set("sSearch_2","mr");
      }else if(this.runType == "Hive"){
        this.set("sSearch_2","hive");
      }else if(this.runType == "Pig"){
        this.set("sSearch_2","pig");
      }else{
        this.set("sSearch_2","");
      }
    }.observes("runType"),

    /**
     * Direct binding to job filter field
     */
    jobs:"",
    onJobsChange:function(){
      var minMaxTmp = this.parseNumber(this.jobs);
      this.set("minJobs", minMaxTmp.min);
      this.set("maxJobs", minMaxTmp.max);
    }.observes("jobs"),

    /**
     * Direct binding to Input filter field
     */
    input:"",
    onInputChange:function(){
      var minMaxTmp = this.parseBandWidth(this.input);
      this.set("minInputBytes", minMaxTmp.min);
      this.set("maxInputBytes", minMaxTmp.max);
    }.observes("input"),

    /**
     * Direct binding to Output filter field
     */
    output:"",
    onOutputChange:function(){
      var minMaxTmp = this.parseBandWidth(this.output);
      this.set("minOutputBytes", minMaxTmp.min);
      this.set("maxOutputBytes", minMaxTmp.max);
    }.observes("output"),

    /**
     * Direct binding to Duration filter field
     */
    duration:"",
    onDurationChange:function(){
      var minMaxTmp = this.parseDuration(this.duration);
      this.set("minDuration", minMaxTmp.min);
      this.set("maxDuration", minMaxTmp.max);
    }.observes("duration"),

    /**
     * Direct binding to Run Date filter field
     */
    runDate:"",
    onRunDateChange:function(){
      var minMaxTmp = this.parseDate(this.runDate);
      this.set("minStartTime", minMaxTmp.min);
      this.set("maxStartTime", minMaxTmp.max);
    }.observes("runDate"),

    parseDuration:function(value){
      var tmp={
        min:"",
        max:""
      };

      var compareChar = isNaN(value.charAt(0)) ? value.charAt(0) : false;
      var compareScale = value.match(/s|m|h/);
      compareScale = compareScale ? compareScale[0] : "";
      var compareValue = compareChar ? parseFloat(value.substr(1, value.length)) : parseFloat(value.substr(0, value.length));
      if(isNaN(compareValue)){
        return tmp;
      }
      switch (compareScale) {
        case 'h':
        tmp.min = Math.ceil((parseFloat(compareValue)-0.0001)*1000*60*60);
        tmp.max = Math.floor((parseFloat(compareValue)+0.0001)*1000*60*60);
        break;
        case 'm':
        tmp.min = Math.ceil((parseFloat(compareValue)-0.001)*1000*60);
        tmp.max = Math.floor((parseFloat(compareValue)+0.001)*1000*60);
        break;
        case 's':
        tmp.min = Math.ceil((parseFloat(compareValue)-0.01)*1000);
        tmp.max = Math.floor((parseFloat(compareValue)+0.01)*1000);
        break;
        default:
          tmp.min = Math.ceil((parseFloat(compareValue)-0.01)*1000);
          tmp.max = Math.floor((parseFloat(compareValue)+0.01)*1000);
      }
      switch (compareChar) {
        case '<':
          tmp.min="";
          break;
        case '>':
          tmp.max="";
          break;
      }
      return tmp;
    },

    parseDate:function(value){
      var tmp={
        min:"",
        max:""
      };
      var nowTime = App.dateTime();

      switch (value){
        case 'Any':
          break;
        case 'Past 1 Day':
          tmp.min= nowTime - 86400000;
          break;
        case 'Past 2 Days':
          tmp.min= nowTime - 172800000;
          break;
        case 'Past 7 Days':
          tmp.min= nowTime - 604800000;
          break;
        case 'Past 14 Days':
          tmp.min= nowTime - 1209600000;
          break;
        case 'Past 30 Days':
          tmp.min= nowTime - 2592000000;
          break;
        case 'Running Now':
          tmp.min= nowTime;
          break;
      }
      return tmp;
    },

    parseBandWidth:function(value){
      var tmp={
        min:"",
        max:""
      };

      var compareChar = isNaN(value.charAt(0)) ? value.charAt(0) : false;
      var compareScale = value.match(/kb|k|mb|m|gb|g/);
      compareScale = compareScale ? compareScale[0] : "";
      var compareValue = compareChar ? parseFloat(value.substr(1, value.length)) : parseFloat(value.substr(0, value.length));
      if(isNaN(compareValue)){
        return tmp;
      }
      switch (compareScale) {
        case 'g': case 'gb':
          tmp.min = Math.max(1073741824,Math.ceil((compareValue-0.005)*1073741824));
          tmp.max = Math.floor((compareValue+0.005)*1073741824);
          break;
        case 'm': case 'mb':
          tmp.min = Math.max(1048576,Math.ceil((compareValue-0.05)*1048576));
          tmp.max = Math.min(1073741823,Math.floor((compareValue+0.05)*1048576));
          break;
        case 'k': case 'kb':
          tmp.min = Math.max(1024,Math.ceil((compareValue-0.05)*1024));
          tmp.max = Math.min(1048575,Math.floor((compareValue+0.05)*1024));
          break;
        default:
          tmp.min = Math.max(1024,Math.ceil((compareValue-0.05)*1024));
          tmp.max = Math.min(1048575,Math.floor((compareValue+0.05)*1024));
      }
      switch (compareChar) {
        case '<':
          tmp.min="";
          break;
        case '>':
          tmp.max="";
          break;
      }
      return tmp;
    },
    parseNumber:function(value){
      var tmp={
        min:"",
        max:""
      };
      switch (value.charAt(0)) {
        case '<':
          tmp.max=value.substr(1);
          break;
        case '>':
          tmp.min=value.substr(1);
          break;
        case '=':
          tmp.min=value.substr(1);
          tmp.max=value.substr(1);
          break;
        default:
          tmp.min=value;
          tmp.max=value;
      }
      return tmp;
    },

    /**
     * Create link for server request
     * @return {String}
     */
    createAppLink:function(){
      var link = "/jobhistory/datatable?";


      var arr = [
        "sSearch_0", "sSearch_1", "sSearch_2", "sSearch_3", "minJobs",
        "maxJobs", "minInputBytes", "maxInputBytes", "minOutputBytes",
        "maxOutputBytes", "minDuration", "maxDuration", "minStartTime",
        "maxStartTime", "sSearch", "iDisplayLength", "iDisplayStart",
        "iSortCol_0", "sSortDir_0"
      ];

      var notFilterFields = ["iDisplayLength", "iDisplayStart", "iSortCol_0", "sSortDir_0"];

      var filtersUsed = false;

      for (var n=0; n<arr.length;n++) {
        if(this.get(arr[n])){
          link += arr[n] + "=" + this.get(arr[n]) + "&";
          if (!notFilterFields.contains(arr[n])) {
            filtersUsed = true;
          }
        }
      }

      link = link.slice(0,link.length-1);

      if(!this.get("viewTypeClickEvent")) {
        this.set('viewType', filtersUsed?'filtered':'all');
      }

      return link;
    }
  }),

  /**
   * reset all filters in table
   *
   */
  clearFilters: function () {
    var obj=this.get("filterObject");
    obj.set("sSearch","");
    obj.set("sSearch_0","");
    obj.set("sSearch_1","");
    obj.set("sSearch_2","");
    obj.set("sSearch_3","");
    obj.set("runType","Any");
    obj.set("jobs","");
    obj.set("input","");
    obj.set("output","");
    obj.set("duration","");
    obj.set("runDate","Any");
  },


  runUrl : "/jobhistory/datatable",
  runTimeout : null,

  valueObserver: function(){
    if(this.get('filterObject.iDisplayLength') > this.get('content.length')) {
      this.set('filterObject.iDisplayStart', 0);
    }
    var link = this.get('filterObject').createAppLink();

    if(this.get("filterObject.viewType") == "filtered"){
      this.set("runUrl", link);
    }else{
      this.set("runUrl", "/jobhistory/datatable?iDisplayStart=" + this.get('filterObject.iDisplayStart') + "&iDisplayLength=" + this.get('filterObject.iDisplayLength') +
         '&iSortCol_0=' + this.get('filterObject.iSortCol_0') + '&sSortDir_0=' + this.get('filterObject.sSortDir_0'));
    }

    var timeout = this.get('runTimeout');
    var self = this;

    clearTimeout(timeout);
    timeout = setTimeout(function(){
      console.log(self.get("runUrl"));
      self.loadRuns();
    }, 300);

    this.set('runTimeout', timeout);

  }.observes(
      'filterObject.sSearch_0',
      'filterObject.sSearch_1',
      'filterObject.sSearch_2',
      'filterObject.sSearch_3',
      'filterObject.minJobs',
      'filterObject.maxJobs',
      'filterObject.minInputBytes',
      'filterObject.maxInputBytes',
      'filterObject.minOutputBytes',
      'filterObject.maxOutputBytes',
      'filterObject.minDuration',
      'filterObject.maxDuration',
      'filterObject.minStartTime',
      'filterObject.maxStartTime',
      'filterObject.sSearch',
      'filterObject.iDisplayLength',
      'filterObject.iDisplayStart',
      'filterObject.iSortCol_0',
      'filterObject.sSortDir_0',
      'filterObject.viewType'
  ),

  serverData: "",
  summary: null,

  /**
   * Observer for summary data from server
   */
  summaryInfo: function(){
    var tmp;
    var summary = this.get('serverData');
    if(!summary){
      tmp = {
        'jobs': {
          'avg': '-',
          'min': '-',
          'max': '-'
        },
        'input': {
          'avg': '-',
          'min': '-',
          'max': '-'
        },
        'output': {
          'avg': '-',
          'min': '-',
          'max': '-'
        },
        'duration': {
          'avg': '-',
          'min': '-',
          'max': '-'
        },
        'times': {
          'oldest': '-',
          'youngest': '-'
        }
      };
    }else{
      tmp = {
        'jobs': {
          'avg': summary.jobs.avg.toFixed(2),
          'min': summary.jobs.min,
          'max': summary.jobs.max
        },
        'input': {
          'avg': misc.formatBandwidth(summary.input.avg),
          'min': misc.formatBandwidth(summary.input.min),
          'max': misc.formatBandwidth(summary.input.max)
        },
        'output': {
          'avg': misc.formatBandwidth(summary.output.avg),
          'min': misc.formatBandwidth(summary.output.min),
          'max': misc.formatBandwidth(summary.output.max)
        },
        'duration': {
          'avg': date.timingFormat(Math.round(summary.duration.avg)),
          'min': date.timingFormat(summary.duration.min),
          'max': date.timingFormat(summary.duration.max)
        },
        'times': {
          'oldest': new Date(summary.times.oldest).toDateString(),
          'youngest': new Date(summary.times.youngest).toDateString()
        }
      };
    }
    this.set("summary",tmp);
  }.observes('serverData'),


  columnsName: Ember.ArrayController.create({
    content: [
      { name: Em.I18n.t('apps.table.column.appId'), index: 0 },
      { name: Em.I18n.t('common.name'), index: 1 },
      { name: Em.I18n.t('common.type'), index: 2 },
      { name: Em.I18n.t('common.user'), index: 3 },
      { name: Em.I18n.t('apps.avgTable.jobs'), index: 4 },
      { name: Em.I18n.t('apps.avgTable.input'), index: 5 },
      { name: Em.I18n.t('apps.avgTable.output'), index: 6 },
      { name: Em.I18n.t('apps.avgTable.duration'), index: 7 },
      { name: Em.I18n.t('apps.table.column.runDate'), index: 8 }
    ]
  })

});
