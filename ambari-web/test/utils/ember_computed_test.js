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

require('utils/ember_computed');

describe('Ember.computed macros', function () {

  describe('#notEqual', function () {

    beforeEach(function () {
      this.obj = Em.Object.create({
        prop1: '123',
        prop2: Em.computed.notEqual('prop1', '123')
      });
    });

    it('`false` if values are equal', function () {
      expect(this.obj.get('prop2')).to.be.false;
    });

    it('`true` if values are not equal', function () {
      this.obj.set('prop1', '321');
      expect(this.obj.get('prop2')).to.be.true;
    });

  });

  describe('#equalProperties', function () {

    beforeEach(function () {
      this.obj = Em.Object.create({
        prop1: '123',
        prop2: '123',
        prop3: Em.computed.equalProperties('prop1', 'prop2')
      });
    });

    it('`true` if values are equal', function () {
      expect(this.obj.get('prop3')).to.be.true;
    });

    it('`false` if values are not equal', function () {
      this.obj.set('prop1', '321');
      expect(this.obj.get('prop3')).to.be.false;
    });

  });

  describe('#notEqualProperties', function () {

    beforeEach(function () {
      this.obj = Em.Object.create({
        prop1: '123',
        prop2: '123',
        prop3: Em.computed.notEqualProperties('prop1', 'prop2')
      });
    });

    it('`false` if values are equal', function () {
      expect(this.obj.get('prop3')).to.be.false;
    });

    it('`true` if values are not equal', function () {
      this.obj.set('prop1', '321');
      expect(this.obj.get('prop3')).to.be.true;
    });

  });

  describe('#ifThenElse', function () {

    beforeEach(function () {
      this.obj = Em.Object.create({
        prop1: true,
        prop2: Em.computed.ifThenElse('prop1', '1', '0')
      });
    });

    it('`1` if `prop1` is true', function () {
      expect(this.obj.get('prop2')).to.equal('1');
    });

    it('`0` if `prop1` is false', function () {
      this.obj.set('prop1', false);
      expect(this.obj.get('prop2')).to.equal('0');
    });

  });

  describe('#and', function () {

    beforeEach(function () {
      this.obj = Em.Object.create({
        prop1: true,
        prop2: true,
        prop3: true,
        prop4: Em.computed.and('prop1', 'prop2', 'prop3')
      });
    });

    it('`true` if all dependent properties are true', function () {
      expect(this.obj.get('prop4')).to.be.true;
    });

    it('`false` if at elast one dependent property is false', function () {
      this.obj.set('prop2', false);
      expect(this.obj.get('prop4')).to.be.false;
    });

  });

  describe('#or', function () {

    beforeEach(function () {
      this.obj = Em.Object.create({
        prop1: false,
        prop2: false,
        prop3: false,
        prop4: Em.computed.or('prop1', 'prop2', 'prop3')
      });
    });

    it('`false` if all dependent properties are false', function () {
      expect(this.obj.get('prop4')).to.be.false;
    });

    it('`true` if at elast one dependent property is true', function () {
      this.obj.set('prop2', true);
      expect(this.obj.get('prop4')).to.be.true;
    });

  });

  describe('#sumProperties', function () {

    beforeEach(function () {
      this.obj = Em.Object.create({
        prop1: 1,
        prop2: 2,
        prop3: 3,
        prop4: Em.computed.sumProperties('prop1', 'prop2', 'prop3')
      });
    });

    it('should be sum of dependent values', function () {
      expect(this.obj.get('prop4')).to.equal(6);
    });

    it('should be updated if some dependent vlaue is changed', function () {
      this.obj.set('prop1', 4);
      expect(this.obj.get('prop4')).to.equal(9);
    });

  });

  describe('#gte', function () {

    beforeEach(function () {
      this.obj = Em.Object.create({
        prop1: 2,
        prop2: Em.computed.gte('prop1', 3)
      });
    });

    it('`false` if value is less than needed', function () {
      expect(this.obj.get('prop2')).to.be.false;
    });

    it('`true` if value is equal to the needed', function () {
      this.obj.set('prop1', 3);
      expect(this.obj.get('prop2')).to.be.true;
    });

    it('`true` if value is greater than needed', function () {
      this.obj.set('prop1', 4);
      expect(this.obj.get('prop2')).to.be.true;
    });

  });

  describe('#gteProperties', function () {

    beforeEach(function () {
      this.obj = Em.Object.create({
        prop1: 2,
        prop2: 3,
        prop3: Em.computed.gteProperties('prop1', 'prop2')
      });
    });

    it('`false` if value is less than needed', function () {
      expect(this.obj.get('prop3')).to.be.false;
    });

    it('`true` if value is equal to the needed', function () {
      this.obj.set('prop1', 3);
      expect(this.obj.get('prop3')).to.be.true;
    });

    it('`true` if value is greater than needed', function () {
      this.obj.set('prop1', 4);
      expect(this.obj.get('prop3')).to.be.true;
    });

  });

  describe('#lte', function () {

    beforeEach(function () {
      this.obj = Em.Object.create({
        prop1: 2,
        prop2: Em.computed.lte('prop1', 1)
      });
    });

    it('`false` if value is greater  than needed', function () {
      expect(this.obj.get('prop2')).to.be.false;
    });

    it('`true` if value is equal to the needed', function () {
      this.obj.set('prop1', 1);
      expect(this.obj.get('prop2')).to.be.true;
    });

    it('`true` if value is less than needed', function () {
      this.obj.set('prop1', 0);
      expect(this.obj.get('prop2')).to.be.true;
    });

  });

  describe('#lteProperties', function () {

    beforeEach(function () {
      this.obj = Em.Object.create({
        prop1: 2,
        prop2: 1,
        prop3: Em.computed.lteProperties('prop1', 'prop2')
      });
    });

    it('`false` if d1 is greater than d2', function () {
      expect(this.obj.get('prop3')).to.be.false;
    });

    it('`true` if d1 is equal to the d2', function () {
      this.obj.set('prop1', 1);
      expect(this.obj.get('prop3')).to.be.true;
    });

    it('`true` if d1 is less than d2', function () {
      this.obj.set('prop1', 0);
      expect(this.obj.get('prop3')).to.be.true;
    });

  });

  describe('#gt', function () {

    beforeEach(function () {
      this.obj = Em.Object.create({
        prop1: 2,
        prop2: Em.computed.gt('prop1', 3)
      });
    });

    it('`false` if value is less than needed', function () {
      expect(this.obj.get('prop2')).to.be.false;
    });

    it('`false` if value is equal to the needed', function () {
      this.obj.set('prop1', 3);
      expect(this.obj.get('prop2')).to.be.false;
    });

    it('`true` if value is greater than needed', function () {
      this.obj.set('prop1', 4);
      expect(this.obj.get('prop2')).to.be.true;
    });

  });

  describe('#gtProperties', function () {

    beforeEach(function () {
      this.obj = Em.Object.create({
        prop1: 2,
        prop2: 3,
        prop3: Em.computed.gtProperties('prop1', 'prop2')
      });
    });

    it('`false` if value is less than needed', function () {
      expect(this.obj.get('prop3')).to.be.false;
    });

    it('`false` if value is equal to the needed', function () {
      this.obj.set('prop1', 3);
      expect(this.obj.get('prop3')).to.be.false;
    });

    it('`true` if value is greater than needed', function () {
      this.obj.set('prop1', 4);
      expect(this.obj.get('prop3')).to.be.true;
    });

  });

  describe('#lt', function () {

    beforeEach(function () {
      this.obj = Em.Object.create({
        prop1: 2,
        prop2: Em.computed.lt('prop1', 1)
      });
    });

    it('`false` if value is greater  than needed', function () {
      expect(this.obj.get('prop2')).to.be.false;
    });

    it('`false` if value is equal to the needed', function () {
      this.obj.set('prop1', 1);
      expect(this.obj.get('prop2')).to.be.false;
    });

    it('`true` if value is less than needed', function () {
      this.obj.set('prop1', 0);
      expect(this.obj.get('prop2')).to.be.true;
    });

  });

  describe('#ltProperties', function () {

    beforeEach(function () {
      this.obj = Em.Object.create({
        prop1: 2,
        prop2: 1,
        prop3: Em.computed.ltProperties('prop1', 'prop2')
      });
    });

    it('`false` if d1 is greater than d2', function () {
      expect(this.obj.get('prop3')).to.be.false;
    });

    it('`false` if d1 is equal to the d2', function () {
      this.obj.set('prop1', 1);
      expect(this.obj.get('prop3')).to.be.false;
    });

    it('`true` if d1 is less than d2', function () {
      this.obj.set('prop1', 0);
      expect(this.obj.get('prop3')).to.be.true;
    });

  });

  describe('#match', function () {

    beforeEach(function () {
      this.obj = Em.Object.create({
        prop1: 'abc',
        prop2: Em.computed.match('prop1', /^ab/)
      })
    });

    it('`true` if value match regexp', function () {
      expect(this.obj.get('prop2')).to.be.true;
    });

    it('`true` if value match regexp (2)', function () {
      this.obj.set('prop1', 'abaaa');
      expect(this.obj.get('prop2')).to.be.true;
    });

    it('`false` if value doesn\'t match regexp', function () {
      this.obj.set('prop1', '!!!!');
      expect(this.obj.get('prop2')).to.be.false;
    });

  });

  describe('#someBy', function () {

    beforeEach(function () {
      this.obj = Em.Object.create({
        prop1: [{a: 1}, {a: 2}, {a: 3}],
        prop2: Em.computed.someBy('prop1', 'a', 2)
      });
    });

    it('`true` if some collection item has needed property value', function () {
      expect(this.obj.get('prop2')).to.be.true;
    });

    it('`false` if on one collection item doesn\'t have needed property value', function () {
      this.obj.set('prop1.1.a', 3);
      expect(this.obj.get('prop2')).to.be.false;
    });

  });

  describe('#everyBy', function () {

    beforeEach(function () {
      this.obj = Em.Object.create({
        prop1: [{a: 2}, {a: 2}, {a: 2}],
        prop2: Em.computed.everyBy('prop1', 'a', 2)
      });
    });

    it('`true` if all collection items have needed property value', function () {
      expect(this.obj.get('prop2')).to.be.true;
    });

    it('`false` if at least one collection item doesn\'t have needed property value', function () {
      this.obj.set('prop1.1.a', 3);
      expect(this.obj.get('prop2')).to.be.false;
    });

  });

  describe('#mapBy', function () {

    beforeEach(function () {
      this.obj = Em.Object.create({
        prop1: [{a: 1}, {a: 2}, {a: 3}],
        prop2: Em.computed.mapBy('prop1', 'a')
      });
    });

    it('should map dependent property', function () {
      expect(this.obj.get('prop2')).to.eql([1, 2, 3]);
    });

    it('should map dependent property (2)', function () {
      this.obj.get('prop1').push({a: 4});
      expect(this.obj.get('prop2')).to.eql([1, 2, 3, 4]);
    });

  });

  describe('#filterBy', function () {

    beforeEach(function () {
      this.obj = Em.Object.create({
        prop1: [{a: 2}, {a: 2}, {a: 3}],
        prop2: Em.computed.filterBy('prop1', 'a', 2)
      });
    });

    it('should filter dependent property', function () {
      expect(this.obj.get('prop2')).to.eql([{a: 2}, {a: 2}]);
    });

    it('should filter dependent property (2)', function () {
      this.obj.get('prop1').pushObject({a: 2});
      expect(this.obj.get('prop2')).to.eql([{a: 2}, {a: 2}, {a: 2}]);
    });

  });

  describe('#findBy', function () {

    beforeEach(function () {
      this.obj = Em.Object.create({
        prop1: [{b: 1, a: 2}, {b: 2, a: 2}, {a: 3}],
        prop2: Em.computed.findBy('prop1', 'a', 2)
      });
    });

    it('should filter dependent property', function () {
      expect(this.obj.get('prop2')).to.eql({b:1, a: 2});
    });

    it('should filter dependent property (2)', function () {
      this.obj.get('prop1').pushObject({b: 3, a: 2});
      expect(this.obj.get('prop2')).to.eql({b: 1, a: 2});
    });

  });

  describe('#alias', function() {

    beforeEach(function () {
      this.obj = Em.Object.create({
        prop1: {
          a: {
            b: {
              c: 1
            }
          }
        },
        prop2: Em.computed.alias('prop1.a.b.c')
      })
    });

    it('should be equal to dependent property', function () {
      expect(this.obj.get('prop2')).to.equal(1);
    });

    it('should be equal to dependent property (2)', function () {
      this.obj.set('prop1.a.b.c', 2);
      expect(this.obj.get('prop2')).to.equal(2);
    });

  });

  describe('#existsIn', function () {

    beforeEach(function () {
      this.obj = Em.Object.create({
        prop1: 'v1',
        prop2: Em.computed.existsIn('prop1', ['v1', 'v2'])
      });
    });

    it('`true` if dependent value is in the array', function () {
      expect(this.obj.get('prop2')).to.be.true;
    });

    it('`true` if dependent value is in the array (2)', function () {
      this.obj.set('prop1', 'v2');
      expect(this.obj.get('prop2')).to.be.true;
    });

    it('`false` if dependent value is not in the array', function () {
      this.obj.set('prop1', 'v3');
      expect(this.obj.get('prop2')).to.be.false;
    });

  });

});