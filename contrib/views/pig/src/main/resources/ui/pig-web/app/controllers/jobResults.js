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

App.JobResultsController = Em.ObjectController.extend({
  getOutputProxy:function (output) {
    var self = this,
        promise,
        result;

    var host = this.store.adapterFor('application').get('host');
    var namespace = this.store.adapterFor('application').get('namespace');
    var jobId = this.get('content.id');
    var url = [host, namespace,'jobs',jobId, 'results',output].join('/');

    promise = new Ember.RSVP.Promise(function(resolve,reject){
      return Em.$.getJSON(url).then(function (stdoutFile) {
        resolve(stdoutFile.file);
      },function (error) {
        var responseText = JSON.parse(error.responseText);
        self.send('showAlert', {'message': Em.I18n.t('job.alert.'+output+'_error',
          {status:responseText.status, message:responseText.message}), status:'error', trace: responseText.trace});
      })
    });

    return Ember.ObjectProxy.extend(Ember.PromiseProxyMixin).create({
      promise: promise
    });
  },
  checkStatus:function (argument) {
    return (this.get('content.status')=='COMPLETED')?true:false;
  },
  stdout:function() {
    return (this.checkStatus())?this.getOutputProxy('stdout'):null;
  }.property(),
  stderr:function() {
    return (this.checkStatus())?this.getOutputProxy('stderr'):null;
  }.property(),
  exitcode:function() {
    return (this.checkStatus())?this.getOutputProxy('exit'):null;
  }.property()
});
