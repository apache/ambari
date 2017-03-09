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
  "search": "",
  "isBundle": true,
  "isCoordinator": true,
  "isWorkflow": true,
  "sortProp": ['updatedAt:desc'],
  "filteredModels": Ember.computed("model", "search", "isBundle", "isCoordinator", "isWorkflow", function(){
    var score = 0, condition = true, searchTxt = this.get("search").toLowerCase(), isWorkflow = this.get("isWorkflow"), isCoordinator = this.get("isCoordinator"), isBundle = this.get("isBundle");
    return this.get("model").filter( (role) => {
      score = 0
      if(searchTxt && searchTxt.length) {
        condition = role.get('name') && role.get('name').toLowerCase().indexOf(searchTxt)>-1;
      }
      if(isWorkflow && role.get('type') === 'WORKFLOW') {
        score++;
      }
      if(isCoordinator && role.get('type') === 'COORDINATOR') {
        score++;
      }
      if(isBundle && role.get('type') === 'BUNDLE') {
        score++;
      }
      return condition && score > 0;
    });
  }),
  modelSorted : Ember.computed.sort("filteredModels", "sortProp"),
  "isDeleteDraftConformation": false,
  "currentDraft": undefined,
  "deleteInProgress": false,
  "deleteMsg": undefined,
  "currentJobService" : Ember.inject.service('current-job'),
  elementsInserted: function () {
      this.$('.actions').hide();
  }.on("didInsertElement"),
  rendered : function(){
    var self = this;
    this.$("#projectsList").on("hidden.bs.modal", function () {
      this.sendAction("close");
      history.back();
    }.bind(this));
    this.$("#projectDeleteModal").on('hidden.bs.modal', function () {
      self.set("isDeleteDraftConformation", true);
      self.set("deleteMsg", null);
      self.set("deleteInProgress", false);
    }.bind(this));
    this.$("#projectsList").modal("show");
    Ember.$("#loading").css("display", "none");
  }.on('didInsertElement'),
  store: Ember.inject.service(),
  actions: {
    importActionToEditor ( path, type ) {
      this.$("#projectsList").modal("hide");
      this.sendAction('editWorkflow', path, type);
    },
    confirmDelete (job ){
	    this.set("showingConfirmation", true);
	    this.set("currentDraft", job);
    },
    deleteDraft () {
	    this.sendAction("deleteWorkflow", this.get('currentDraft'));
    },
    closeProjects () {
		  //this.$('.modal-backdrop').remove();
		  this.$("#projectsList").modal("hide");
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
