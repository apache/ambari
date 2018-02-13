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

import {LogLevel} from '@app/classes/string';

export type HomogeneousObject<T> = {[key: string]: T};

export interface LogLevelObject {
  name: LogLevel;
  label: string;
  color: string;
}

/**
 * This is an interface for the service and audit log fields.
 */
export interface LogField {
  group?: string; // eg.: HDFS, Ambari, etc this prop is only used in Audit logs
  label: string;
  name: string;
  filterable: boolean; // it can be used in a filter query
  visible: boolean; // visible by default in the log list
}

/**
 * This is an interface for the service and audit log fields.
 */
export interface AuditFieldsDefinitionSet {
  defaults: LogField[],
  overrides: {
    [key: string]: LogField[]
  }
}
