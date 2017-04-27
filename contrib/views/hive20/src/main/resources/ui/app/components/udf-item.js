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
import UILoggerMixin from '../mixins/ui-logger';

export default Ember.Component.extend(UILoggerMixin, {

  store: Ember.inject.service(),

  udfService: Ember.inject.service('udf'),

  tagName: '',
  expanded: false,
  expandedEdit: false,
  showDeleteUdfModal: false,
  expandedValue: null,
  udfId: null,
  editUdfId: Ember.computed('udf', function () {
    return this.get('udf.id');
  }),
  editUdfName: Ember.computed('udf', function () {
    return this.get('udf.name');
  }),
  editUdfClassName: Ember.computed('udf', function () {
    return this.get('udf.classname');
  }),
  editOwner: Ember.computed('udf', function () {
    return this.get('udf.owner');
  }),
  editFileResource: Ember.computed('udf', function () {
    return this.get('udf.fileResource');
  }),
  fileResourceList:[],
  selectedFileResource: null,
  isAddingNewFileResource: false,

  validate(udfName, udfClassName, resourceName, resourcePath){
    if (Ember.isEmpty(udfName)) {
      this.get('logger').danger('UDF Name can not be empty.');
      return false;
    }

    if (Ember.isEmpty(udfClassName)) {
      this.get('logger').danger('UDF Class Name can not be empty.');
      return false;
    }

    if (Ember.isEmpty(resourceName) || Ember.isEmpty(resourcePath)) {
      this.get('logger').danger('File Resource can not be empty.');
      return false;
    }
    return true;
  },

  actions: {
    toggleExpandUdf(fileResourceId) {

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

        this.get('store').findAll('file-resource').then((data) => {
          let fileResourceList = [];
          data.forEach(x => {
            let localFileResource = {'id': x.get('id'),
              'name': x.get('name'),
              'path': x.get('path'),
              'owner': x.get('owner')
            };
            fileResourceList.push(localFileResource);
          });

          fileResourceList.filterBy('id', fileResourceId).map((data) => {
            this.set('udfFileResourceName', data.name);
            this.set('udfFileResourcePath', data.path);

                    this.get('store').find('udf', udfId).then((data) => {
                        this.set('editUdfId', udfId);
                        this.set('editUdfName', data.get('name'));
                        this.set('editUdfClassName', data.get('classname'));
                        this.set('editOwner', data.get('owner'));
                      });
          });
        });
        this.send('setFileResource', fileResourceId);
      }
    },

    cancelEditUdf(){
      this.set('expandedEdit', false);
      this.set('isAddingNewFileResource', false);
    },

    saveUDf(name, classname, udfid, udfFileResourceName, udfFileResourcePath){
      let self = this;
      if (this.validate(name, classname, udfFileResourceName, udfFileResourcePath)) {
        if (!Ember.isEmpty(this.get('selectedFileResource'))) {
          this.get('store').findRecord('udf', udfid).then(function (resultUdf) {
            resultUdf.set('name', name);
            resultUdf.set('classname', classname);
            resultUdf.set('fileResource', self.get('selectedFileResource').id);
            resultUdf.save();
            self.set('expandedEdit', false);
          });
        }
        else {

          let resourcePayload = {"name": udfFileResourceName, "path": udfFileResourcePath};

          this.get('udfService').savefileResource(resourcePayload)
            .then((data) => {
              console.log('fileResource is', data.fileResource.id);
              self.get('store').findRecord('udf', udfid).then(function (resultUdf) {

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
      }
      this.set('isAddingNewFileResource', false);
    },

    showRemoveUdfModal(udfId){
      console.log('udfId',udfId);
      this.set('showDeleteUdfModal', true);
      this.set('udfId', udfId);
    },

    removeUdf(){
      var self = this;
      let record = this.get('store').peekRecord('udf', this.get('udfId') );
      if(record){
        record.destroyRecord().then(function(){
          self.send('cancelUdf');
          self.sendAction('refreshUdfList');
      })}
    },

    cancelUdf(){
      this.set('showDeleteUdfModal', false);
    },

    handleResourceChange(filter){
      if(filter.action === "addNewFileResource"){
        this.get('controller').set('isAddingNewFileResource', true);
        this.set('selectedFileResource',null);
      }else {
        this.set('selectedFileResource',filter);
        this.get('controller').set('isAddingNewFileResource', false);
      }
    },

    setFileResource(fileResourceId){
      this.get('store').findAll('file-resource').then((data) => {
        let fileResourceList = [];
        data.forEach(x => {
          let localFileResource = {'id': x.get('id'),
            'name': x.get('name'),
            'path': x.get('path'),
            'owner': x.get('owner')
          };
          fileResourceList.push(localFileResource);
        });

        let localSelectedFileResource =  fileResourceList.filterBy('id', fileResourceId);
        this.set('selectedFileResource',localSelectedFileResource.get('firstObject'));
      });

    }
  }

});
