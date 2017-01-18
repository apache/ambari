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
const { computed } = Ember;

export default Ember.Component.extend({
  "isDeleteDraftConformation": false,
  "currentDraft": undefined,
  "deleteInProgress": false,
  "deleteMsg": undefined,
  elementsInserted: function () {
      this.$('.actions').hide();
  }.on("didInsertElement"),
  rendered : function(){
    var self = this;
    this.$("#configureJob").on('hidden.bs.modal', function () {
      self.set("deleteMsg", null);
      self.set("deleteInProgress", false);
    }.bind(this));
  }.on('didInsertElement'),
  store: Ember.inject.service(),
  actions: {
    importActionToEditor ( path ) {
      this.sendAction('editWorkflow', path);
    },
    deleteDraftConformation (job ){
	    this.set("isDeleteDraftConformation", true);
	    this.$("#configureJob").modal("show");
	    this.set("currentDraft", job);
    },
    deleteDraft () {
	    this.set("deleteInProgress", true);
	    var job = this.get("currentDraft"), self= this;
  		this.get("store").findRecord('wfproject', job.id).then(function(post) {
  		  post.destroyRecord();
  		}).then(function () {
  	      self.set("deleteInProgress", false);
  	      self.set("deleteMsg", "Draft successfully deleted.");
            console.log("Deleted successfully");
  	    }).catch(function (response) {
  	      self.set("deleteInProgress", false);
  	      self.set("deleteMsg", "There is some problem while deletion.Please try again.");
  	    });
    },
    showActions (job) {
      this.$('.'+job.get("updatedAt")+'Actions').show();
      this.$('.Actions'+job.get("updatedAt")).hide();
    },
    hideActions (job) {
      this.$('.'+job.get("updatedAt")+'Actions').hide();
      this.$('.Actions'+job.get("updatedAt")).show();
    }
  }
});
