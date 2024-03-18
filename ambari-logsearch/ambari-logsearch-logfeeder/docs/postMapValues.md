<!--
{% comment %}
Licensed to the Apache Software Foundation (ASF) under one or more
contributor license agreements.  See the NOTICE file distributed with
this work for additional information regarding copyright ownership.
The ASF licenses this file to you under the Apache License, Version 2.0
(the "License"); you may not use this file except in compliance with
the License.  You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
{% endcomment %}
-->

# Post Map Values

The Post Map Values element in the [filter](filter.md) field names as keys, the values are lists of sets of post map values, each
describing one mapping done on a field named before obtained after filtering.

Currently there are four kind of mappings are supported:

## Map Date

The name of the mapping element should be map\_date. The value json element may contain the following parameters:

| Field                 | Description                                                                                            |
|-----------------------|--------------------------------------------------------------------------------------------------------|
| src\_date\_pattern    | If it is specified than the mapper converts from this format to the target, and also adds missing year |
| target\_date\_pattern | If 'epoch' then the field is parsed as seconds from 1970, otherwise the content used as pattern        |


## Map Copy

The name of the mapping element should be map\_copy. The value json element should contain the following parameter:

| Field      | Description                   |
|------------|-------------------------------|
| copy\_name | The name of the copied field  |


## Map Field Name

The name of the mapping element should be map\_fieldname. The value json element should contain the following parameter:

| Field            | Description                   |
|------------------|-------------------------------|
| new\_field\_name | The name of the renamed field |

## Map Field Value

The name of the mapping element should be map\_fieldvalue. The value json element should contain the following parameter:

| Field       | Description                                                        |
|-------------|--------------------------------------------------------------------|
| pre\_value  | The value that the field must match \(ignoring case\) to be mapped |
| post\_value | The value to which the field is modified to                        |

## Map Anonymize

The name of the mapping element should be map\_anonymize. The value json element should contain the following parameter:

| Field      | Description                                                                                                     |
|------------|-----------------------------------------------------------------------------------------------------------------|
| pattern    | The pattern to use to identify parts to anonymize. The parts to hide should be marked with the "<hide>" string. |
| hide\_char | The character to hide with, if it is not specified then the default is '*'                                      |
