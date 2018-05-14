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
  showFileSourceInput: true,
  showHdfsLocationInput: Ember.computed.equal("fileInfo.uploadSource", "hdfs"),
  showLocalLocationInput: Ember.computed.equal("fileInfo.uploadSource", "local"),
  isLocalUpload: Ember.computed.equal("fileInfo.uploadSource", "local"),

  actions: {
    toggleFileSource: function(){
      this.toggleProperty("showFileSourceInput");
    },
    closeHdfsModal: function() {
      this.set('showDirectoryViewer', false);
    },
    hdfsPathSelected: function(path) {
      this.set('fileInfo.hdfsPath', path);
      this.set('showDirectoryViewer', false);
    },

    toggleDirectoryViewer: function() {
      this.set('showDirectoryViewer', true);
    },
    onFileChanged: function(file){
      this.get("fileInfo.files")[0] = file;
      console.log("setting fifilesUploadedles as : ", this.get("fileInfo.files"));
      this.sendAction("onFileChanged");
    },
  }
});
