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

import Ember from 'ember';

export default Ember.Component.extend({

  store: Ember.inject.service(),

  udfService: Ember.inject.service('udf'),

  tagName: '',
  expanded: false,
  expandedEdit: false,
  showDeleteUdfModal: false,
  expandedValue: null,
  udfId: null,
  fileResourceList:[],
  selectedFileResource: null,
  isAddingNewFileResource: false,

  actions: {
    toggleExpandUdf(fileResourceId) {

      var self = this;
      if(this.get('expanded')) {
        this.set('expanded', false);
      } else {
        this.set('expanded', true);
        this.set('valueLoading', true);

        this.get('store').find('fileResource', fileResourceId).then((data) => {
          this.set('udfFileResourceName', data.get('name'));
          this.set('udfFileResourcePath', data.get('path'));
        });
      }
    },


    showEditUdf(udfId, fileResourceId){

      if(this.get('expandedEdit')) {
        this.set('expandedEdit', false);
      } else {
        this.set('expandedEdit', true);
        this.set('valueLoading', true);

        this.get('store').find('fileResource', fileResourceId).then((data) => {
          this.set('udfFileResourceName', data.get('name'));
          this.set('udfFileResourcePath', data.get('path'));
        });


        this.send('setFileResource', fileResourceId);

      }
    },

    cancelEditUdf(){
      this.set('expandedEdit', false);
    },

    saveUDf(name, classname, udfid, udfFileResourceName, udfFileResourcePath){

      let self = this;

      if(!Ember.isEmpty(this.get('selectedFileResource'))){
        this.get('store').findRecord('udf', udfid).then(function(resultUdf) {
          resultUdf.set('name', name);
          resultUdf.set('classname', classname);
          resultUdf.set('fileResource', self.get('selectedFileResource').id);
          resultUdf.save();
          self.set('expandedEdit', false);
        });
      } else {

        let resourcePayload = {"name":udfFileResourceName,"path":udfFileResourcePath};

        this.get('udfService').savefileResource(resourcePayload)
          .then((data) => {
            console.log('fileResource is', data.fileResource.id);
            self.get('store').findRecord('udf', udfid).then(function(resultUdf) {

              resultUdf.set('name', name);
              resultUdf.set('classname', classname);
              resultUdf.set('fileResource', data.fileResource.id);
              resultUdf.save();
              self.set('expandedEdit', false);
            });
          }, (error) => {
            console.log("Error encountered", error);
          });
      }

      this.set('isAddingNewFileResource', false);

    },

    showRemoveUdfModal(udfId){
      console.log('udfId',udfId);
      this.set('showDeleteUdfModal', true);
      this.set('udfId', udfId);
    },

    removeUdf(){
      let self = this;
      this.get('store').find('udf', this.get('udfId')).then(function(resultUdf) {
        resultUdf.destroyRecord().then(function(data) {
          self.send('cancelUdf');
          self.sendAction('refreshUdfList');
        }, function(response) {
          console.log('UDF NOT deleted');
        });
        return false;
      });
    },

    cancelUdf(){
      this.set('showDeleteUdfModal', false);
    },

    handleResourceChange(filter){
      if(filter.action == "addNewFileResource"){
        this.get('controller').set('isAddingNewFileResource', true);
        this.set('selectedFileResource',null);
      }else {
        this.set('selectedFileResource',filter);
        this.get('controller').set('isAddingNewFileResource', false);
      }
    },

    setFileResource(fileResourceId){
      let localSelectedFileResource = this.get('fileResourceList').filterBy('id', fileResourceId);
      this.set('selectedFileResource',localSelectedFileResource[0]);
    }

  }

});
