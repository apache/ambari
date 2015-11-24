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

var App = require('app');
require('utils/helper');
require('data/HDP2/gluster_fs_properties');
var siteProperties = require('data/HDP2.3/site_properties').configProperties;

describe('hdp2SiteProperties', function () {
  /**
   * @stackProperties: All the properties that are derived from stack definition
   */
  var stackProperties = siteProperties.filter(function(item){
    return  (!(item.isRequiredByAgent === false || item.filename === 'alert_notification' || item.category === 'Ambari Principals' || item.name === 'oozie_hostname'))
  });

  stackProperties.forEach(function(siteProperty){
    /**
     * Following config attributes are stack driven and should be defined in the stack metainfo instead of ambari-web site-properties file
     * isVisible
     * isOverridable
     * value
     * recommendedValue
     * isReconfigurable
     * isRequired
     * displayName
     * description
     * showLabel
     * unit
     */
    it('Check attributes of "' + siteProperty.filename + '/' + siteProperty.name  + '"' + '. Stack driven attributes should be undefined ', function () {
      expect(siteProperty.isVisible).to.equal(undefined);
      expect(siteProperty.value).to.equal(undefined);
      expect(siteProperty.recommendedValue).to.equal(undefined);
      expect(siteProperty.description).to.equal(undefined);
      expect(siteProperty.isReconfigurable).to.equal(undefined);
      expect(siteProperty.isRequired).to.equal(undefined);
      expect(siteProperty.displayName).to.equal(undefined);
      expect(siteProperty.showLabel).to.equal(undefined);
      expect(siteProperty.unit).to.equal(undefined);
    });


    /**
     * displayTypes <code>supportTextConnection<code> and <code>radio button<code>
     * can be used as exception. Other displayTypes values should be used in stack definition
     */
    it('Check attributes of "' + siteProperty.filename + '/' + siteProperty.name  + '"' + '. Display type value ' + siteProperty.displayType + ' should be described in stack ', function () {
      expect(siteProperty.displayType).to.match(/undefined|supportTextConnection|radio button/);
    });

    /**
     * Following config attributes uniquely represent a config property
     * name
     * filename
     */
    it('Check primary attributes of "' + siteProperty.filename + '/' + siteProperty.name  + '"' + '. Attributes that uniquely represent a property should be defined ', function () {
      expect(siteProperty.name).to.not.equal(undefined);
      expect(siteProperty.filename).to.not.equal(undefined);
    });
  });

});
