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

App.ErrorController = Ember.ObjectController.extend({
  actions: {
    toggleStackTrace:function () {
      var value = this.get('isExpanded');
      this.set('isExpanded', !value);
    },
  },

  isExpanded: false,

  publicMessage:function () {
    var content = this.get('content');
    var text = content.statusText;
    if (content && content.responseText) {
      var json = JSON.parse(content.responseText);
      text = json.message;
    } else if (content && content.message) {
      text = content.message;
    }
    return text;
  }.property('content'),
  stackTrace:function () {
    var content = this.get('content');
    var trace = null;
    if (content && content.responseText) {
      var json = JSON.parse(content.responseText);
      trace = json.trace;
    }
    return trace;
  }.property('content'),
});
