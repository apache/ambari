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

App.Job = DS.Model.extend({
  pigScript:DS.belongsTo('file', { async: true }),
  //pythonScript:DS.belongsTo('script'),
  scriptId: DS.attr('number'),
  title: DS.attr('string'),
  templetonArguments:DS.attr('string'),
  owner: DS.attr('string'),
  forcedContent:DS.attr('string'),

  sourceFile:DS.attr('string'),
  sourceFileContent:DS.attr('string'),

  statusDir: DS.attr('string'),
  status: DS.attr('string'),
  dateStarted:DS.attr('isodate'),
  jobId: DS.attr('string'),
  jobType: DS.attr('string'),
  percentComplete: DS.attr('number'),
  percentStatus:function () {
    if (this.get('isTerminated')) {
      return 100;
    };
    return (this.get('status')==='COMPLETED')?100:(this.get('percentComplete')||0);
  }.property('status','percentComplete'),

  isTerminated:function(){
    return (this.get('status')=='KILLED'||this.get('status')=='FAILED');
  }.property('status'),
  isKilling:false,
  kill:function(success,error){
    var self = this;
    var host = self.store.adapterFor('application').get('host');
    var namespace = self.store.adapterFor('application').get('namespace');
    var url = [host, namespace,'jobs',self.get('id')].join('/');

    self.set('isKilling',true)
    return Em.$.ajax(url, {
      type:'DELETE',
      contentType:'application/json',
      beforeSend:function(xhr){
        xhr.setRequestHeader('X-Requested-By','ambari');
      }
    }).always(function() {
      self.set('isKilling',false);
    }).then(success,error);
  },

  isExplainJob: function(){
    return this.jobType == "explain";
  },
  isSyntaxCheckJob: function(){
      return this.jobType == "syntax_check";
  },
  isUtilityJob:  function(){
      return this.isExplainJob() || this.isSyntaxCheckJob();
  },

  pingStatusMap:{
    'SUBMITTING':true,
    'SUBMITTED':true,
    'RUNNING':true,
    'COMPLETED':false,
    'SUBMIT_FAILED':false,
    'KILLED':false,
    'FAILED':false
  },
  needsPing:function () {
    return this.pingStatusMap[this.get('status')];
  }.property('status')
});
