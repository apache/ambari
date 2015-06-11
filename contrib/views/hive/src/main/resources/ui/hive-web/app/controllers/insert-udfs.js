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
import constants from 'hive/utils/constants';

export default Ember.ArrayController.extend({
  needs: [ constants.namingConventions.udfs ],

  model: Ember.A(),

  udfs: Ember.computed.alias('controllers.' + constants.namingConventions.udfs + '.udfs'),

  updateUdfs: function () {
    var self = this,
        udfs = this.get('udfs'),
        udfsWithoutFiles;

    this.clear();

    if (udfs && udfs.get('length')) {
      udfs.getEach('fileResource.id').uniq().forEach(function (fileResourceId) {
        if (fileResourceId) {
          self.pushObject(Ember.Object.create({
            file: udfs.findBy('fileResource.id', fileResourceId).get('fileResource'),
            udfs: udfs.filterBy('fileResource.id', fileResourceId)
          }));
        }
      });

      udfsWithoutFiles = udfs.filter(function (udf) {
        return !udf.get('isNew') && !udf.get('fileResource.id');
      });

      if (udfsWithoutFiles.get('length')) {
       self.pushObject(Ember.Object.create({
          name: "placeholders.select.noFileResource",
          udfs: udfsWithoutFiles
        }));
      }
    }
  }.on('init').observes('udfs.@each.isNew')
});