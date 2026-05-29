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

export const durationOptions = [
  {
    value: 1,
    label: "15 minutes",
  },
  {
    value: 2,
    label: "30 minutes",
  },
  {
    value: 3,
    label: "1 hour",
  },
  {
    value: 4,
    label: "2 hours",
  },
  {
    value: 5,
    label: "4 hours",
  },
  {
    value: 6,
    label: "12 hours",
  },
  {
    value: 7,
    label: "24 hours",
  },
  {
    value: 8,
    label: "1 week",
  },
  {
    value: 9,
    label: "1 month",
  },
  {
    value: 10,
    label: "1 year",
  },
  {
    value: 11,
    label: "Custom",
  },
];

export const durationMap: { [key: string]: number } = {
    "15 minutes": 900,
    "30 minutes": 1800,
    "1 hour": 3600,
    "2 hours": 7200,
    "4 hours": 14400,
    "12 hours": 43200,
    "24 hours": 86400,
    "1 week": 604800,
    "1 month": 2592000,
    "1 year": 31536000,
  };
