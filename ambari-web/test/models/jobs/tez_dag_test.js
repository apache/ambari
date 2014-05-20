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

var modelSetup = require('test/init_model_test');
require('models/jobs/tez_dag');

var vertex,
  vertexData = {
    id: 'vertex'
  },
  timeData = {
    startTime: 1000,
    endTime: 2000
  },
  tasksCases = [
    {
      count: 5,
      number: 5,
      title: 'should return tasks count'
    },
    {
      count: null,
      number: 0,
      title: 'should return 0'
    }
  ],
  dataSizeCases = [
    {
      file: 'fileReadBytes',
      hdfs: 'hdfsReadBytes',
      total: 'totalReadBytes',
      totalDisplay: 'totalReadBytesDisplay'
    },
    {
      file: 'fileWriteBytes',
      hdfs: 'hdfsWriteBytes',
      total: 'totalWriteBytes',
      totalDisplay: 'totalWriteBytesDisplay'
    }
  ],
  setDataSize = function (vertex, fileProp, fileVal, hdfsProp, hdfsVal) {
    vertex.set(fileProp, fileVal);
    vertex.set(hdfsProp, hdfsVal);
  };

describe('App.TezDagVertex', function () {

  beforeEach(function () {
    vertex = App.TezDagVertex.createRecord(vertexData);
  });

  afterEach(function () {
    modelSetup.deleteRecord(vertex);
  });

  describe('#duration', function () {
    it('should calculate the difference between endTime and startTime', function () {
      vertex.setProperties(timeData);
      expect(vertex.get('duration')).to.equal(1000);
    });
  });

  tasksCases.forEach(function(item) {
    describe('#tasksNumber', function () {
      it(item.title, function () {
        vertex.set('tasksCount', item.count);
        expect(vertex.get('tasksNumber')).to.equal(item.number);
      });
    });
  });

  dataSizeCases.forEach(function (item) {
    describe('#' + item.total, function () {
      it('should sum ' + item.file + ' and ' + item.hdfs, function () {
        setDataSize(vertex, item.file, 1024, item.hdfs, 2048);
        expect(vertex.get(item.total)).to.equal(3072);
      });
    });
    describe('#' + item.totalDisplay, function () {
      it('should return formatted ' + item.total, function () {
        setDataSize(vertex, item.file, 1024, item.hdfs, 2048);
        expect(vertex.get(item.totalDisplay)).to.equal('3 KB');
      });
    });
  });

  describe('#durationDisplay', function () {
    it('should return formatted string', function () {
      vertex.setProperties(timeData);
      expect(vertex.get('durationDisplay')).to.equal('1.00 secs');
    });
  });

});
