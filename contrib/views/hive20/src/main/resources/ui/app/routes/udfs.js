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

export default Ember.Route.extend({

  model() {
    return this.store.findAll('udf').then(udfs => udfs.toArray());
  },
  store: Ember.inject.service(),

  setupController(controller, model) {
    this._super(...arguments);

    controller.set('udflist', model);

    this.store.findAll('file-resource').then((data) => {
      let fileResourceList = [];
      data.forEach(x => {
        let localFileResource = {
          'id': x.get('id'),
          'name': x.get('name'),
          'path': x.get('path'),
          'owner': x.get('owner')
        };
        fileResourceList.push(localFileResource);
      });
      fileResourceList.push({'name':'Add New File Resource', 'action':'addNewFileResource'});
      controller.set('fileResourceList', fileResourceList);
    });
    controller.set('name', {'noSort':true});
    controller.set('classname', {'noSort':true});
    controller.set('owner', {'noSort':true});
  },

  actions:{
    sort(sortProp, sortField, key) {
      let perm = {};
      perm[key] = true;
      this.get('controller').set(sortField, perm);
      this.get('controller').set('sortProp', [sortProp]);
    },
    refreshUdfList(){
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
    }
  }
});
