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
import FileOperationMixin from '../mixins/file-operation';

export default Ember.Service.extend(FileOperationMixin, {
  logger: Ember.inject.service('alert-messages'),

  // Returns a promise for the operation. Upon sucess or error, this also
  // appropriately sends error messages.
  move: function(srcPath, destName ) {
    return new Ember.RSVP.Promise((resolve, reject) => {
      var adapter = this.get('store').adapterFor('file');
      var baseURL = adapter.buildURL('file');
      var moveUrl = baseURL.substring(0, baseURL.lastIndexOf('/')) + "/move";
      var data = {sourcePaths: srcPath, destinationPath: destName};
      adapter.ajax(moveUrl, "POST", {data: data}).then((response) => {
        this.get('logger').success(`Successfully moved to ${destName}.`, {}, {flashOnly: true});
        resolve(response);
      }, (responseError) => {
        var error = this.extractError(responseError);
        this.get('logger').danger(`Failed to move to ${destName}`, error);
        reject(error);
      });
    });
  },

  _isDestinationPathExists(destinationPath) {
    return this.get('store').peekAll('file').isAny('path', destinationPath);
  }
});
