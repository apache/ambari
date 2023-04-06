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
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

var App = require("app");

App.quicklinksMapper = App.QuickDataMapper.create({
  model: App.QuickLinksConfig,

  config: {
    "id": "QuickLinkInfo.service_name",
    "file_name" : "QuickLinkInfo.file_name",
    "service_name" : "QuickLinkInfo.service_name",
    "stack_name" : "QuickLinkInfo.stack_name",
    "stack_version" : "QuickLinkInfo.stack_version",
    "name" : "QuickLinkInfo.quicklink_data.QuickLinksConfiguration.name",
    "protocol" : "QuickLinkInfo.quicklink_data.QuickLinksConfiguration.configuration.protocol",
    "links" : "QuickLinkInfo.quicklink_data.QuickLinksConfiguration.configuration.links"
  },

  map: function(json){
    console.time('App.quicklinksMapper execution time');

    var result = [];
    var linkResult = [];

    json.items.forEach(function(item) {
      var parseResult = this.parseIt(item, this.get('config'));
      console.log("parseResult", parseResult);
      result.push(parseResult);
    }, this);

    App.store.safeLoadMany(this.get('model'), result);
    console.timeEnd('App.quicklinksMapper execution time');
  }
});
