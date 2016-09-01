/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

var App = require('app');

App.ConfigDiffView = Em.View.extend({
  template: Em.Handlebars.compile('{{view.diff}}'),
  diff: function () {
    var trimAndSort = function (value) {
      if (value == null) {
        return [];
      }
      var values = value.split("\n").filter(function (item) {
        return item != "";
      }).sort().join("\n");
      return difflib.stringAsLines(values);
    };
    var initialValues = trimAndSort(this.get('config.initialValue'));
    var recommendedValues = trimAndSort(this.get('config.recommendedValue'));
    return new Handlebars.SafeString(diffview.buildView({
      baseTextLines: initialValues,
      newTextLines: recommendedValues,
      opcodes: new difflib.SequenceMatcher(initialValues, recommendedValues).get_opcodes()
    }).outerHTML);
  }.property('config.initialValues', 'config.recommendedValues')
});
