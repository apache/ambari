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
import UILoggerMixin from '../../mixins/ui-logger';

export default Ember.Route.extend(UILoggerMixin, {

  beforeModel(){

  },

  validateUDF(udfName, udfClassName){
    if (Ember.isEmpty(udfName)) {
      this.get('logger').danger('UDF Name can not be empty.');
      return false;
    }
    if (Ember.isEmpty(udfClassName)) {
      this.get('logger').danger('UDF Class Name can not be empty.');
      return false;
    }
    return true;
  },

  validateFileResource(resourceName, resourcePath){
    if (Ember.isEmpty(resourceName) || Ember.isEmpty(resourcePath)) {
      this.get('logger').danger('File Resource can not be empty.');
      return false;
    }
    return true;
  },

  udf: Ember.inject.service(),
  store: Ember.inject.service(),

  setupController(controller, model) {
    this._super(...arguments);

    this.store.findAll('file-resource').then((data) => {
      let fileResourceList = [];
      data.forEach(x => {
        let localFileResource = {'id': x.get('id'),
                                 'name': x.get('name'),
                                 'path': x.get('path'),
                                 'owner': x.get('owner')
                                };
        fileResourceList.push(localFileResource);
      });
      fileResourceList.push({'name':'Add New File Resource', 'action':'addNewFileResource'});
      controller.set('fileResourceList', fileResourceList);
    });

    controller.set('isAddingNewFileResource', false);
    controller.set('selectedFileResource',null);
    controller.set('resourceId', null);

  },

  actions: {

    saveUDf(resourceName, resourcePath, udfName, udfClassName){

      this.get('controller').set('resourceName',resourceName);
      this.get('controller').set('resourcePath', resourcePath);
      this.get('controller').set('udfName', udfName);
      this.get('controller').set('udfClassName', udfClassName);

      if(!Ember.isEmpty( this.get('controller').get('resourceId'))){

        if (this.validateUDF(udfName, udfClassName)) {


          let newUDF = this.get('store').createRecord('udf',
            {
              name: udfName,
              classname: udfClassName,
              fileResource: this.get('controller').get('resourceId')
            });

          newUDF.save().then((data) => {
            console.log('udf saved');

            this.get('store').findAll('udf').then((data) => {
              let udfList = [];
              data.forEach(x => {
                let localUdf = {
                  'id': x.get('id'),
                  'name': x.get('name'),
                  'classname': x.get('classname'),
                  'fileResource': x.get('fileResource'),
                  'owner': x.get('owner')
                };
                udfList.pushObject(localUdf);
              });

              this.controllerFor('udfs').set('udflist', udfList);
              this.transitionTo('udfs');
            });

          });

        }

      } else {

        let resourcePayload = {"name":resourceName,"path":resourcePath};

        var newFileResource = this.get('store').createRecord('file-resource',
          {name:resourceName,
            path:resourcePath
          });


        if (this.validateFileResource(resourceName, resourcePath) && this.validateUDF(udfName, udfClassName)) {

          newFileResource.save().then((data) => {
              console.log('fileResource is', data.get('id'));
              let newUDF = this.get('store').createRecord('udf',
                {name:udfName,
                  classname:udfClassName,
                  fileResource: data.get('id')
                });

              newUDF.save().then((data) => {
                  console.log('udf saved');

                  this.get('store').findAll('udf').then((data) => {
                    let udfList = [];
                    data.forEach(x => {
                      let localUdf = {
                        'id': x.get('id'),
                        'name': x.get('name'),
                        'classname': x.get('classname'),
                        'fileResource': x.get('fileResource'),
                        'owner': x.get('owner')
                      };
                      udfList.pushObject(localUdf);
                    });

                    this.controllerFor('udfs').set('udflist',udfList);
                    this.transitionTo('udfs');
                  });
                })
                .catch((error) => {
                  this.get('logger').danger('Failed to create UDF.', this.extractError(error));
                  this.transitionTo('udfs');

                });
            })
            .catch((error) => {
              this.get('logger').danger('Failed to create File Resource.', this.extractError(error));
              this.transitionTo('udfs');
            });

        }

      }
    },

    cancelSaveUDf(){
      this.get('controller').set('resourceName','');
      this.get('controller').set('resourcePath','');
      this.get('controller').set('udfName','');
      this.get('controller').set('udfClassName','');

      this.transitionTo('udfs');
    },

    handleFileResourceChange(filter){
      console.log('filter', filter);
      if(filter.action === "addNewFileResource"){
        this.get('controller').set('isAddingNewFileResource', true);
        this.get('controller').set('resourceName','');
        this.get('controller').set('resourcePath','');
        this.get('controller').set('resourceId', null);
        this.get('controller').set('selectedFileResource',null);

      }else {
        this.get('controller').set('resourceId',filter.id);
        this.get('controller').set('selectedFileResource',filter);
        this.get('controller').set('isAddingNewFileResource', false);
      }
    }
  }

});
