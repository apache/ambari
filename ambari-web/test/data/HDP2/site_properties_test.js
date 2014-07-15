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
var siteProperties = require('data/HDP2/site_properties').configProperties;

describe('hdp2SiteProperties', function () {

  // No site properties should be made invisible
  siteProperties.forEach(function(siteProperty){
    it('Check invisible attribute of "' + siteProperty.name  + '"' + '. It should not be defined ', function () {
      expect(siteProperty.isVisible).to.equal(undefined);
    });
  });

  // No site properties should have value and defaultValue defined on client side.
  // These should be always retrieved from server.

    siteProperties.forEach(function(siteProperty){
      it('Check value and defaultValue attribute of "' + siteProperty.name + '"' + '. It should not be defined ', function () {
        expect(siteProperty.value).to.equal(undefined);
        expect(siteProperty.defaultValue).to.equal(undefined);
    });
  });

  // No site properties should have description field duplicated on client side.
  // These should be always retrieved from server.
  siteProperties.forEach(function(siteProperty){
    it('Check description attribute of "' + siteProperty.name + '"' + '. It should not be defined ', function () {
      expect(siteProperty.description).to.equal(undefined);
    });
  });

  // All the site properties should be persisted in the configuration tag
  // So isRequiredByAgent should be never defined over here
  // These should be always retrieved from server and saved in the correct configuration resource via API.
  siteProperties.forEach(function(siteProperty){
    it('Check isRequiredByAgent attribute of "' + siteProperty.name + '"' + '. It should not be defined ', function () {
      expect(siteProperty.isRequiredByAgent).to.equal(undefined);
    });
  });

  // All Falcon site properties should be mapped to site file. There is a property with same name (*.domain)
  // in different site files of Falcon service

    var falconSiteProperties = siteProperties.filterProperty('serviceName','FALCON');
    falconSiteProperties.forEach(function(siteProperty){
      it('Check filename attribute for "' + siteProperty.name + '"' + ' property of Falcon service. It should be defined ', function () {
        expect(siteProperty).to.have.property('filename');
    });
  });

});