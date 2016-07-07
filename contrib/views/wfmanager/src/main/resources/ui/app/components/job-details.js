/*
 *    Licensed to the Apache Software Foundation (ASF) under one or more
 *    contributor license agreements.  See the NOTICE file distributed with
 *    this work for additional information regarding copyright ownership.
 *    The ASF licenses this file to You under the Apache License, Version 2.0
 *    (the "License"); you may not use this file except in compliance with
 *    the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

import Ember from 'ember';

export default Ember.Component.extend({
  error : {},
  errorMessage : Ember.computed('error', function() {
    if(this.get('error').status === 400){
      return 'Remote API Failed';
    }else if(this.get('error').status === 401){
      return 'User Not Authorized';
    }
  }),
  id : Ember.computed('model.jobType', function() {
      if (this.get('model.jobType') === 'wf'){
        return this.get('model.id');
      } else if(this.get('model.jobType') === 'coords'){
        return this.get('model.coordJobId');
      } else if(this.get('model.jobType') === 'bundles'){
        return this.get('model.bundleJobId');
      }
  }),
  name : Ember.computed('model.jobType', function() {
      if (this.get('model.jobType') === 'wf'){
        return this.get('model.appName');
      } else if(this.get('model.jobType') === 'coords'){
        return this.get('model.coordJobName');
      } else if(this.get('model.jobType') === 'bundles'){
        return this.get('model.bundleJobName');
      }
  }),
  displayType : Ember.computed('model.jobType', function() {
    if(this.get('jobType') === 'wf'){
        return "Workflow";
    }else if(this.get('jobType') === 'coords'){
        return "Coordinator";
    }
    else if(this.get('jobType') === 'bundles'){
        return "Bundle";
    }
    return "Workflow";
  }),
  initialize : function(){
    if(this.get('currentTab')){
      this.$('.nav-tabs a[href="'+this.get('currentTab').attr("href")+'"]').tab('show');
      if(this.get('model.actions')){
        this.set('model.actionDetails', this.get('model.actions')[0]);
      }
    }
    this.$('.nav-tabs').on('shown.bs.tab', function(event){
      this.sendAction('onTabChange', this.$(event.target));
    }.bind(this));
  }.on('didInsertElement'),
  actions : {
    back (){
      this.sendAction('back');
    },
    close : function(){
      this.sendAction('close');
    },
    doRefresh : function(){
      this.sendAction('doRefresh');
    },
    getJobDefinition : function () {
     Ember.$.get(Ember.ENV.API_URL+'/v2/job/'+this.get('id')+'?show=definition&timezone=GMT',function(response){
       this.set('model.jobDefinition', (new XMLSerializer()).serializeToString(response).trim());
     }.bind(this)).fail(function(error){
       this.set('error',error);
     }.bind(this));
   },
   showFirstActionDetail : function(){
     this.set('model.actionDetails', this.get('model.actions')[0]);
   },
   getJobLog : function (params){
     var url = Ember.ENV.API_URL+'/v2/job/'+this.get('id')+'?show=log';
     if(params && params.logFilter){
       url = url + '&logfilter=' + params.logFilter;
     }
     if(params && params.logActionList){
       url = url + '&type=action&scope='+ params.logActionList;
     }
     Ember.$.get(url,function(response){
       this.set('model.jobLog', response);
     }.bind(this)).fail(function(error){
       this.set('error', error);
     }.bind(this));
   },
   getErrorLog : function (){
     Ember.$.get(Ember.ENV.API_URL+'/v2/job/'+this.get('id')+'?show=errorlog',function(response){
       this.set('model.errorLog', response);
     }.bind(this)).fail(function(error){
       this.set('error', error);
     }.bind(this));
   },
   getAuditLog : function (){
     Ember.$.get(Ember.ENV.API_URL+'/v2/job/'+this.get('id')+'?show=auditlog',function(response){
       this.set('model.auditLog', response);
     }.bind(this)).fail(function(error){
       this.set('error', error);
     }.bind(this));
   },
   getJobDag : function (){
       this.set('model.jobDag', Ember.ENV.API_URL+'/v2/job/'+this.get('id')+'?show=graph');
   },
   getCoordActionReruns : function () {
     var url = Ember.ENV.API_URL+'/v2/job/'+this.get('id')+'?show=allruns&type=action';
     if(this.get('rerunActionList')){
       url = url + '&scope=' + this.get('rerunActionList');
     }
     Ember.$.get(url, function(response){
       this.set('model.coordActionReruns', response.workflows);
     }.bind(this)).fail(function(error){
       this.set('error', error);
     }.bind(this));
   },
   getActionDetails : function (actionInfo) {
     this.set('model.actionDetails', actionInfo);
   },
   showWorkflow : function(workflowId){
     this.sendAction('showWorkflow', workflowId);
    },
   showCoord : function(coordId){
     this.sendAction('showCoord', coordId);
   }
  }
});
