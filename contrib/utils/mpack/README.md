<!---
   Licensed to the Apache Software Foundation (ASF) under one or more
   contributor license agreements.  See the NOTICE file distributed with
   this work for additional information regarding copyright ownership.
   The ASF licenses this file to You under the Apache License, Version 2.0
   (the "License"); you may not use this file except in compliance with
   the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
--->
# Ambari Management Pack Tool  Usage Guide

The Ambari Management Pack Tool is an instrumental tool designed for comprehensively packaging service code or stack code from your project into a single MPack package. This capability greatly facilitates the process of updating your Ambari services, ensuring a smooth and efficient deployment of new features or modifications.

## Use Cases
n day-to-day operations, 99% of modifications usually occur under the Ambari Stack rather than the Ambari Server itself. In such circumstances, using MPack to update only the contents of the Stack becomes highly necessary. For instance, when you are updating the management code of an Ambari service in an already existing environment, such as the HDFS service, there's no need for manual distribution or synchronization, just simply package and update with an MPack.
For example, when you are developing a new service or modifying a service's configuration, it can be challenging to apply changes to an existing live environment. This is because Ambari writes configurations into the database at startup, making your modifications hard to implement without reinstallation. However, using MPack can resolve this issue, as the installation of an MPack will re-enter the contents of the MPack into the database, making it a powerful tool for applying updates or changes without the need for cumbersome manual processes.

## Preparation

Before starting, make sure you have Python 3 and Ambari Server installed, and that you have the address of the Ambari project at hand.

## Overview of Steps

1. **Generate MPack**: Generate the needed MPack package by providing specific commands.
2. **Stop Ambari Server**: Stop the Ambari Server before installing a new MPack.
3. **Install MPack**: Install the generated MPack package using specified commands.
4. **Start Ambari Server**: Restart the Ambari Server after completing the MPack installation.

### Detailed Steps

#### Generate MPack

Use the following command to generate an MPack package. Be sure to replace the variables with your actual path and version information.

```shell
python3 ambari-server/src/main/resources/scripts/ambari_mpack_tools.py -ambari-dir=$AMBARI_DIR -stack-name=BIGTOP -stack-version=3.3.0 -mpack-version=3.3.0 -output-dir=$OUT_DIR
```

Parameter description:

- `-ambari-dir`: The path to the Ambari project.
- `-stack-name`: The stack name of the MPack being packaged, which must be consistent with the stack name in the project.
- `-stack-version`: Under which stack version the packaged service is.
- `-mpack-version`: The version of the MPack package being output.
- `-output-dir`: The location where the MPack package is output.

#### Stop Ambari Server

Stop the Ambari Server before installing the new MPack:

```shell
ambari-server stop
```

#### Install MPack

Use the following command to install the MPack, replacing the path with the location of your MPack package:

```shell
ambari-server install-mpack -s --purge --verbose --mpack=/tmp/bgtp-ambari-mpack-1.0.0.0-SNAPSHOT-bgtp-ambari-mpack.tar.gz
```

The `--purge` option will automatically delete the previous old stack definitions and then symlink the installed MPack to the corresponding location.

#### Start Ambari Server

After completing the steps above, restart the Ambari Server:

```shell
ambari-server start
```

## Reference Material

For more detailed information, please refer to the [Ambari official documentation](https://cwiki.apache.org/confluence/display/AMBARI/Management+Packs#ManagementPacks-ManagementPackStructure).

---
Thank you for choosing the Ambari Management Pack. We hope this documentation helps you more effectively manage and update the contents of your Ambari Stack.