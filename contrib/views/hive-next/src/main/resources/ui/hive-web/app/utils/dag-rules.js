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

import Ember from 'ember';

export default Ember.ArrayProxy.create({
  content: Ember.A(
    [
      {
        targetOperator: 'TableScan',
        targetProperty: 'alias:',
        label: 'Table Scan:',

        fields: [
          {
            label: 'filterExpr:',
            targetProperty: 'filterExpr:'
          }
        ]
      },
      {
        targetOperator: 'Filter Operator',
        targetProperty: 'predicate:',
        label: 'Filter:',

        fields: []
      },
      {
        targetOperator: 'Map Join Operator',
        label: 'Map Join',

        fields: []
      },
      {
        targetOperator: 'Merge Join Operator',
        label: 'Merge Join',

        fields: []
      },
      {
        targetOperator: 'Select Operator',
        label: 'Select',

        fields: []
      },
      {
        targetOperator: 'Reduce Output Operator',
        label: 'Reduce',

        fields: [
          {
            label: 'Partition columns:',
            targetProperty: 'Map-reduce partition columns:'
          },
          {
            label: 'Key expressions:',
            targetProperty: 'key expressions:'
          },
          {
            label: 'Sort order:',
            targetProperty: 'sort order:'
          }
        ]
      },
      {
        targetOperator: 'File Output Operator',
        label: 'File Output Operator',

        fields: []
      },
      {
        targetOperator: 'Group By Operator',
        label: 'Group By:',

        fields: [
          {
            label: 'Aggregations:',
            targetProperties: 'aggregations:'
          },
          {
            label: 'Keys:',
            targetProperty: 'keys:'
          }
        ]
      },
      {
        targetOperator: 'Limit',
        targetProperty: 'Number of rows:',
        label: 'Limit:',

        fields: []
      },
      {
        targetOperator: 'Extract',
        label: 'Extract',

        fields: []
      },
      {
        targetOperator: 'PTF Operator',
        label: 'Partition Table Function',

        fields: []
      },
      {
        targetOperator: 'Dynamic Partitioning Event Operator',
        labelel: 'Dynamic Partitioning Event',

        fields: [
          {
            label: 'Target column:',
            targetProperty: 'Target column:'
          },
          {
            label: 'Target Vertex:',
            targetProperty: 'Target Vertex:'
          },
          {
            label: 'Partition key expr:',
            targetProperty: 'Partition key expr:'
          }
        ]
      }
    ]
  )
});
