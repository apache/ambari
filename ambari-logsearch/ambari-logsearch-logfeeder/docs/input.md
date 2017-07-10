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

# Input

The input element in the [input configuration](inputConfig.md) contains a list of input descriptions, each describing one source
of input. The general elements in the json are the following:

| Field                       | Description                                                                                           | Default      |
|-----------------------------|-------------------------------------------------------------------------------------------------------|--------------|
| type                        | The log id for this source                                                                            | -            |
| rowtype                     | The type of the row, can be service / audit                                                           | -            |
| path                        | The path of the source, may contain '*' characters too                                                | -            |
| add\_fields                 | The element contains field\_name: field\_value pairs which will be added to each rows data            | -            |
| source                      | The type of the input source, currently file and s3_file are supported                                | -            |
| tail                        | The input should check for only the latest file matching the pattern, not all of them                 | true         |
| gen\_event\_md5             | Generate an event\_md5 field for each row by creating a hash of the row data                          | true         |
| use\_event\_md5\_as\_id     | Generate an id for each row by creating a hash of the row data                                        | false        |
| cache\_enabled              | Allows the input to use a cache to filter out duplications                                            | true         |
| cache\_key\_field           | Specifies the field for which to use the cache to find duplications of                                | log\_message |
| cache\_last\_dedup\_enabled | Allow to filter out entries which are same as the most recent one irrelevant of it's time             | false        |
| cache\_size                 | The number of entries to store in the cache                                                           | 100          |
| cache\_dedup\_interval      | The maximum interval in ms which may pass between two identical log messages to filter the latter out | 1000         |
| is\_enabled                 | A flag to show if the input should be used                                                            | true         |


## File Input

File inputs have some additional parameters:

| Field                    | Description                                                        | Default |
|--------------------------|--------------------------------------------------------------------|---------|
| checkpoint\_interval\_ms | The time interval in ms when the checkpoint file should be updated | 5000    |
| process\_file            | Should the file be processed                                       | true    |
| copy\_file               | Should the file be copied \(only if not processed\)                | false   |


## S3 File Input

S3 file inputs have the following parameters in addition to the general file parameters:

| Field           | Description                             | Default |
|-----------------|-----------------------------------------|---------|
| s3\_access\_key | The access key used for AWS credentials | -       |
| s3\_secret\_key | The secret key used for AWS credentials | -       |
