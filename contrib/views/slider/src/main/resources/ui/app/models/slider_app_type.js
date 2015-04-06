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

App.SliderAppType = DS.Model.extend({

  /**
   * @type {string}
   */
  index: DS.attr('string'),

  /**
   * @type {string}
   */
  typeName: DS.attr('string'),

  /**
   * @type {string}
   */
  typeVersion: DS.attr('string'),

  /**
   * @type {App.SliderAppTypeComponent[]}
   */
  components: DS.hasMany('sliderAppTypeComponent'),

  /**
   * @type {string}
   */
  description: DS.attr('string'),

  /**
   * @type {string}
   */
  version: DS.attr('string'),

  /**
   * @type {object}
   */
  configs: DS.attr('object'),
  
  displayName : function() {
    var typeName = this.get('typeName');
    var typeVersion = this.get('typeVersion');
    return (typeName == null ? '' : typeName) + " ("
        + (typeVersion == null ? '' : typeVersion) + ")"
  }.property('typeName', 'typeVersion')
});

App.SliderAppType.FIXTURES = [];