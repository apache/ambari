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
  showPreview: false,
  fileFormatInfo: Ember.Object.create({
    csvParams: Ember.Object.create(),
    inputFileType: null,
    containsEndlines: false,
  }),
  fileInfo: Ember.Object.create({
    files: Ember.A(),
    hdfsPath: null,
    uploadSource: "local",
  }),
  tableMeta: Ember.Object.create(),
  actions: {
    onFileChanged: function () {
      console.log("inside files changed");
      this.send("preview");
    },
    preview: function () {
      let sourceObject = Ember.Object.create();
      sourceObject.set("fileFormatInfo", this.get("fileFormatInfo"));
      sourceObject.set("fileInfo", this.get("fileInfo"));
      this.sendAction("preview", sourceObject);
      this.set("showPreview", true);
    },
    toggleShowPreview: function(){
      this.toggleProperty("showPreview");
    },
    createAndUpload: function(tableMeta){
      this.set("tableMeta", tableMeta);
      let tableData = Ember.Object.create();
      tableData.set("fileFormatInfo", this.get("fileFormatInfo"));
      tableData.set("fileInfo", this.get("fileInfo"));
      tableData.set("tableMeta", this.get("tableMeta"));
      this.sendAction("createAndUpload", tableData);
    }
  }
});
