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

var result = '';
var tableCount = 15000;
var columnCount = 100;

//tables and columns script
for (var i = 0; i < tableCount; i++) {
  result += 'CREATE TABLE TABLE_' + i + ' (';
  (function () {
    for (var j = 0; j < columnCount; j++) {
      result += 'field_' + j + ' STRING';

      if (j < columnCount - 1) {
        result += ',';
      } else {
        result += ') '
      }
    }
  }());

  result += "ROW FORMAT DELIMITED FIELDS TERMINATED BY ',' STORED AS TEXTFILE; \nLOAD  DATA LOCAL INPATH 'test.csv' OVERWRITE INTO TABLE " +
            'TABLE_' + i + ';\n\n';
}

console.log(result);

//csv script
var fill = '';
for (var i = 0; i < columnCount; i++) {
  fill += 'field_' + i;

  if (i < columnCount - 1) {
    fill += ', ';
  }
}

console.log(fill);