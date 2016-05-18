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

require('utils/helper');
var mappedHdp2Properties = require('data/HDP2/secure_mapping');

describe('hdp2SiteMapping', function () {

  // All mapped properties should have value of string type
  mappedHdp2Properties.forEach(function(mappedProperty){
    it('Value of "{0}" should be string'.format(mappedProperty.name), function () {
      expect(mappedProperty.value).to.be.a('string');
    });
  });
  mappedHdp2Properties.forEach(function(mappedProperty){
    it('Value of "{0}" should have serviceName and filename attribute'.format(mappedProperty.name), function () {
      expect(mappedProperty).to.have.property('serviceName');
      expect(mappedProperty).to.have.property('filename');
    });
  });
});