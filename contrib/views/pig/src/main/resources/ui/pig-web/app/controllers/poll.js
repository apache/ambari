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

App.PollController = Ember.ObjectController.extend({
  actions:{
    killjob:function (job) {
      var self = this;
      job.kill(function () {
        job.reload();
        self.send('showAlert', {'message': Em.I18n.t('job.alert.job_killed',{title:self.get('title')}), status:'info'});
      },function (reason) {
        var trace = null;
        if (reason && reason.responseJSON.trace)
          trace = reason.responseJSON.trace;
        self.send('showAlert', {'message': Em.I18n.t('job.alert.job_kill_error'), status:'error', trace:trace});
      });
    },
  },
  pollster:Em.Object.create({
    job:null,
    start: function(job){
      this.stop();
      this.set('job',job);
      this.timer = setInterval(this.onPoll.bind(this), 5000);
      console.log('START polling. Job: ',this.get('job.id'),'. Timer: ',this.timer)
    },
    stop: function(){
      this.set('job',null);
      clearInterval(this.timer);
      console.log('STOP polling. Job: ',this.get('job.id'),'. Timer: ',this.timer)
    },
    onPoll: function() {
      if (this.job.get('needsPing')) {
        console.log('DO polling. Job: ',this.get('job.id'),'. Timer: ', this.timer)
        this.job.reload();
      } else {
        console.log('END polling. Job: ',this.get('job.id'),'. Timer: ',this.timer)
        this.stop();
      };
    }
  })
});
