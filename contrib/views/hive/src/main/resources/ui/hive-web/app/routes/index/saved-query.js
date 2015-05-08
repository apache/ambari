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

export default Ember.Route.extend({
  setupController: function (controller, model) {
    // settings modify fileContent to extract the settings
    // when you load a saved query use the original fileContent
    // this.store.find('file', model.get('queryFile'))
    //   .then(function(queryFile) {
    //     var changes = queryFile.changedAttributes();
    //     if (changes.fileContent && changes.fileContent[0]) {
    //       queryFile.set('fileContent', changes.fileContent[0]);
    //     }
    //   });

    this.controllerFor(constants.namingConventions.routes.index).set('model', model);
  },

  actions: {
    error: function () {
      this.store.unloadAll(constants.namingConventions.savedQuery);
      this.transitionTo(constants.namingConventions.routes.index);
    }
  }
});
