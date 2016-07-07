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

export default Ember.Controller.extend({
  from : null,
  fromType : null,
  actions : {
    close : function(){
      this.sendAction('onCloseJobDetails');
    },
    doRefresh : function(){
      this.get('target.router').refresh();
    },
    showWorkflow : function(workflowId){
      this.transitionToRoute('job', {
          queryParams: {
              jobType: 'wf',
              id: workflowId,
              from : this.get('model.coordJobId'),
              fromType : this.get('model.jobType')
          }
      });
    },
    showCoord : function(coordJobId){
      this.transitionToRoute('job', {
          queryParams: {
              jobType: 'coords',
              id: coordJobId,
              from : this.get('model.bundleJobId'),
              fromType : this.get('model.jobType')
          }
      });
    },
    back : function (){
      this.transitionToRoute('job', {
          queryParams: {
              jobType: this.get('fromType'),
              id: this.get('from'),
              from : null,
              fromType : null
          }
      });
    },
  }
});
