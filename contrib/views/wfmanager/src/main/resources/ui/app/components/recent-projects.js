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
  store: Ember.inject.service(),
  rendered : function(){
  	var self = this;
    this.$("#projectDeleteModal").on('hidden.bs.modal', function () {
      self.set("isDeleteDraftConformation", true);
      self.set("deleteMsg", null);
      self.set("deleteInProgress", false);
    }.bind(this));
    this.$("#projectsList").on("hidden.bs.modal", function () {
      this.sendAction("close");
    }.bind(this));
    this.$("#projectsList").modal("show");
  }.on('didInsertElement'),
  actions: {
  	close(){
  		this.sendAction("close");
  	},
  	deleteWorkflow (job) {
	    this.set("deleteInProgress", true);
	    var self= this;
	    var rec = this.get("store").peekRecord('wfproject', job.id);
	    if(rec){
	      rec.destroyRecord().then(function () {
  	      self.set("deleteInProgress", false);
  	      self.set("deleteMsg", "Workflow successfully deleted.");
  	    }).catch(function (response) {
  	      self.set("deleteInProgress", false);
  	      self.set("deleteMsg", "There is some problem while deletion.Please try again.");
  	    });
	    }
    },
    editWorkflow ( path, type ) {
      this.sendAction('editWorkflow', path, type);
      this.$("#projectsList").modal("hide");
    },
  }
});
