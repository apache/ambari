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

# Filter

The filter element in the [input configuration](inputConfig.md) contains a list of filter descriptions, each describing one filter
applied on an input.

The general elements in the json are the following:

| Field                 | Description                                                                                     | Default     |
|-----------------------|-------------------------------------------------------------------------------------------------|-------------|
| filter                | The type of the filter, currently grok, keyvalue and json are supported                         | -           |
| conditions            | The conditions of which input to filter                                                         | -           |
| sort\_order           | Describes the order in which the filters should be applied                                      | -           |
| source\_field         | The source of the filter, must be set for keyvalue filters                                      | log_message |
| remove\_source\_field | Remove the source field after the filter is applied                                             | false       |
| post\_map\_values     | Mappings done after the filtering provided it's result, see [post map values](postMapValues.md) | -           |
| is\_enabled           | A flag to show if the filter should be used                                                     | true        |


## Grok Filter

Grok filters have the following additional parameters:

| Field              | Description                                                                                                | Default |
|--------------------|------------------------------------------------------------------------------------------------------------|---------|
| log4j\_format      | The log4j pattern of the log, not used, it is only there for documentation                                 | -       |
| multiline\_pattern | The grok pattern that shows that the line is not a log line on it's own but the part of a multi line entry | -       |
| message\_pattern   | The grok pattern to use to parse the log entry                                                             | -       |


## Key-value Filter

value\_borders is only used if it is specified, and value\_split is not.

Key-value filters have the following additional parameters:

| Field          | Description                                                                               | Default |
|----------------|-------------------------------------------------------------------------------------------|---------|
| field\_split   | The string that splits the key-value pairs                                                | "\t"    |
| value\_split   | The string that separates keys from values                                                | "="     |
| value\_borders | The borders around the value, must be 2 characters long, first before it, second after it | -       |


