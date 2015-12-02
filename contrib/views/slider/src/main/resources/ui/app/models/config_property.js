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


/**
 * Config property
 * @type {object}
 */
App.ConfigProperty = Em.Object.extend({
  name: null,
  value: null,
  label: "",
  viewType: null,
  view: function () {
    switch (this.get('viewType')) {
      case 'checkbox':
        return Em.Checkbox;
      case 'select':
        return Em.Select;
      default:
        return Em.TextField;
    }
  }.property('viewType'),
  className: function () {
    return "value-for-" + this.get('label').replace(/\./g, "-");
  }.property('viewType'),
  readOnly: false,
  //used for config with "select" view
  options: [],
  //indicate whether it single config or set of configs
  isSet: false
});
