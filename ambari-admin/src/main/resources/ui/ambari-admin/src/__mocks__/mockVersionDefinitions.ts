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
export default {
    "href": "http://example.com/api/v1/version_definitions?fields=VersionDefinition/stack_default,VersionDefinition/type,VersionDefinition/stack_repo_update_link_exists,operating_systems/repositories/Repositories/*,VersionDefinition/stack_services,VersionDefinition/repository_version&VersionDefinition/show_available=true",
    "items": [
        {
            "href": "http://example.com/api/v1/version_definitions/VDP-3.2",
            "VersionDefinition": {
                "id": "VDP-3.2",
                "repository_version": "3.2",
                "show_available": true,
                "stack_default": true,
                "stack_name": "VDP",
                "stack_repo_update_link_exists": true,
                "stack_services": [
                    {
                        "name": "HBASE",
                        "display_name": "HBase",
                        "comment": "Non-relational distributed database and centralized service for configuration management &\n        synchronization\n      ",
                        "versions": [
                            "2.5.2"
                        ]
                    },
                    {
                        "name": "RANGER_KMS",
                        "display_name": "Ranger KMS",
                        "comment": "Key Management Server",
                        "versions": [
                            "2.1.0"
                        ]
                    },
                    {
                        "name": "HDFS",
                        "display_name": "HDFS",
                        "comment": "Apache Hadoop Distributed File System",
                        "versions": [
                            "3.2.2"
                        ]
                    },
                    {
                        "name": "RANGER",
                        "display_name": "Ranger",
                        "comment": "Comprehensive security for Hadoop",
                        "versions": [
                            "2.1.0"
                        ]
                    },
                    {
                        "name": "SSM",
                        "display_name": "SSM",
                        "comment": "Manage and Optimize storage for HDFS",
                        "versions": [
                            "1.6.0"
                        ]
                    },
                    {
                        "name": "MAPREDUCE2",
                        "display_name": "MapReduce2",
                        "comment": "Apache Hadoop NextGen MapReduce (YARN)",
                        "versions": [
                            "3.2.2"
                        ]
                    },
                    {
                        "name": "PRESTO",
                        "display_name": "Presto",
                        "comment": "Presto is an open source distributed SQL query engine for running\n        interactive analytic queries against data sources of all sizes ranging\n        from gigabytes to petabytes.",
                        "versions": [
                            "345.0"
                        ]
                    },
                    {
                        "name": "HIVE",
                        "display_name": "Hive",
                        "comment": "Data warehouse system for ad-hoc queries & analysis of large datasets and table & storage management service",
                        "versions": [
                            "3.1.2"
                        ]
                    },
                    {
                        "name": "SQOOP",
                        "display_name": "Sqoop",
                        "comment": "Tool for transferring bulk data between Apache Hadoop and\n        structured data stores such as relational databases\n      ",
                        "versions": [
                            "1.4.7"
                        ]
                    },
                    {
                        "name": "YARN",
                        "display_name": "YARN",
                        "comment": "Apache Hadoop NextGen MapReduce (YARN)",
                        "versions": [
                            "3.2.2"
                        ]
                    },
                    {
                        "name": "AMBARI_METRICS",
                        "display_name": "Ambari Metrics",
                        "comment": "A system for metrics collection that provides storage and retrieval capability for metrics collected from the cluster\n      ",
                        "versions": [
                            "0.2.0"
                        ]
                    },
                    {
                        "name": "ZOOKEEPER",
                        "display_name": "ZooKeeper",
                        "comment": "Centralized service which provides highly reliable distributed coordination",
                        "versions": [
                            "3.5.9"
                        ]
                    },
                    {
                        "name": "TEZ",
                        "display_name": "Tez",
                        "comment": "Tez is the next generation Hadoop Query Processing framework written on top of YARN.",
                        "versions": [
                            "0.10.0"
                        ]
                    },
                    {
                        "name": "FLUME",
                        "display_name": "Flume",
                        "comment": "A distributed service for collecting, aggregating, and moving large amounts of streaming data into HDFS",
                        "versions": [
                            "1.11.0"
                        ]
                    },
                    {
                        "name": "SPARK3",
                        "display_name": "Spark3",
                        "comment": "Apache Spark 3.x is a fast and general engine for large-scale data processing.",
                        "versions": [
                            "3.2.0"
                        ]
                    },
                    {
                        "name": "KERBEROS",
                        "display_name": "Kerberos",
                        "comment": "A computer network authentication protocol which works on\n        the basis of 'tickets' to allow nodes communicating over a\n        non-secure network to prove their identity to one another in a\n        secure manner.\n      ",
                        "versions": [
                            "1.10.3-30"
                        ]
                    }
                ],
                "stack_version": "3.2",
                "type": "STANDARD"
            },
            "operating_systems": [
                {
                    "href": "http://example.com/api/v1/version_definitions/VDP-3.2/operating_systems/amazonlinux2",
                    "OperatingSystems": {
                        "os_type": "amazonlinux2",
                        "stack_name": "VDP",
                        "stack_version": "3.2",
                        "version_definition_id": "VDP-3.2"
                    },
                    "repositories": [
                        {
                            "href": "http://example.com/api/v1/version_definitions/VDP-3.2/operating_systems/amazonlinux2/repositories/VDP-3.2",
                            "Repositories": {
                                "applicable_services": [],
                                "base_url": "https://ad-vdp.s3.us-west-1.amazonaws.com/VDP/amazonlinux2/3.x/updates/3.2.2.0-1",
                                "components": null,
                                "default_base_url": "https://ad-vdp.s3.us-west-1.amazonaws.com/VDP/amazonlinux2/3.x/updates/3.2.2.0-1",
                                "distribution": null,
                                "mirrors_list": null,
                                "os_type": "amazonlinux2",
                                "repo_id": "VDP-3.2",
                                "repo_name": "VDP",
                                "stack_name": "VDP",
                                "stack_version": "3.2",
                                "tags": [],
                                "unique": false,
                                "version_definition_id": "VDP-3.2"
                            }
                        },
                        {
                            "href": "http://example.com/api/v1/version_definitions/VDP-3.2/operating_systems/amazonlinux2/repositories/VDP-3.2-GPL",
                            "Repositories": {
                                "applicable_services": [],
                                "base_url": "https://ad-vdp.s3.us-west-1.amazonaws.com/VDP-GPL/amazonlinux2/3.x/updates/3.2.2.0-1",
                                "components": null,
                                "default_base_url": "https://ad-vdp.s3.us-west-1.amazonaws.com/VDP-GPL/amazonlinux2/3.x/updates/3.2.2.0-1",
                                "distribution": null,
                                "mirrors_list": null,
                                "os_type": "amazonlinux2",
                                "repo_id": "VDP-3.2-GPL",
                                "repo_name": "VDP-GPL",
                                "stack_name": "VDP",
                                "stack_version": "3.2",
                                "tags": [
                                    "GPL"
                                ],
                                "unique": false,
                                "version_definition_id": "VDP-3.2"
                            }
                        },
                        {
                            "href": "http://example.com/api/v1/version_definitions/VDP-3.2/operating_systems/amazonlinux2/repositories/VDP-UTILS-1.1.0.22",
                            "Repositories": {
                                "applicable_services": [],
                                "base_url": "https://ad-vdp.s3.us-west-1.amazonaws.com/VDP-UTILS-1.1.0.22/repos/amazonlinux2",
                                "components": null,
                                "default_base_url": "https://ad-vdp.s3.us-west-1.amazonaws.com/VDP-UTILS-1.1.0.22/repos/amazonlinux2",
                                "distribution": null,
                                "mirrors_list": null,
                                "os_type": "amazonlinux2",
                                "repo_id": "VDP-UTILS-1.1.0.22",
                                "repo_name": "VDP-UTILS",
                                "stack_name": "VDP",
                                "stack_version": "3.2",
                                "tags": [],
                                "unique": false,
                                "version_definition_id": "VDP-3.2"
                            }
                        }
                    ]
                },
                {
                    "href": "http://example.com/api/v1/version_definitions/VDP-3.2/operating_systems/debian9",
                    "OperatingSystems": {
                        "os_type": "debian9",
                        "stack_name": "VDP",
                        "stack_version": "3.2",
                        "version_definition_id": "VDP-3.2"
                    },
                    "repositories": [
                        {
                            "href": "http://example.com/api/v1/version_definitions/VDP-3.2/operating_systems/debian9/repositories/VDP-3.2",
                            "Repositories": {
                                "applicable_services": [],
                                "base_url": "https://ad-vdp.s3.us-west-1.amazonaws.com/VDP/debian9/3.x/updates/3.2.2.0-1",
                                "components": null,
                                "default_base_url": "https://ad-vdp.s3.us-west-1.amazonaws.com/VDP/debian9/3.x/updates/3.2.2.0-1",
                                "distribution": null,
                                "mirrors_list": null,
                                "os_type": "debian9",
                                "repo_id": "VDP-3.2",
                                "repo_name": "VDP",
                                "stack_name": "VDP",
                                "stack_version": "3.2",
                                "tags": [],
                                "unique": false,
                                "version_definition_id": "VDP-3.2"
                            }
                        },
                        {
                            "href": "http://example.com/api/v1/version_definitions/VDP-3.2/operating_systems/debian9/repositories/VDP-3.2-GPL",
                            "Repositories": {
                                "applicable_services": [],
                                "base_url": "https://ad-vdp.s3.us-west-1.amazonaws.com/VDP-GPL/debian9/3.x/updates/3.2.2.0-1",
                                "components": null,
                                "default_base_url": "https://ad-vdp.s3.us-west-1.amazonaws.com/VDP-GPL/debian9/3.x/updates/3.2.2.0-1",
                                "distribution": null,
                                "mirrors_list": null,
                                "os_type": "debian9",
                                "repo_id": "VDP-3.2-GPL",
                                "repo_name": "VDP-GPL",
                                "stack_name": "VDP",
                                "stack_version": "3.2",
                                "tags": [
                                    "GPL"
                                ],
                                "unique": false,
                                "version_definition_id": "VDP-3.2"
                            }
                        },
                        {
                            "href": "http://example.com/api/v1/version_definitions/VDP-3.2/operating_systems/debian9/repositories/VDP-UTILS-1.1.0.22",
                            "Repositories": {
                                "applicable_services": [],
                                "base_url": "https://ad-vdp.s3.us-west-1.amazonaws.com/VDP-UTILS-1.1.0.22/repos/debian9",
                                "components": null,
                                "default_base_url": "https://ad-vdp.s3.us-west-1.amazonaws.com/VDP-UTILS-1.1.0.22/repos/debian9",
                                "distribution": null,
                                "mirrors_list": null,
                                "os_type": "debian9",
                                "repo_id": "VDP-UTILS-1.1.0.22",
                                "repo_name": "VDP-UTILS",
                                "stack_name": "VDP",
                                "stack_version": "3.2",
                                "tags": [],
                                "unique": false,
                                "version_definition_id": "VDP-3.2"
                            }
                        }
                    ]
                },
                {
                    "href": "http://example.com/api/v1/version_definitions/VDP-3.2/operating_systems/redhat-ppc7",
                    "OperatingSystems": {
                        "os_type": "redhat-ppc7",
                        "stack_name": "VDP",
                        "stack_version": "3.2",
                        "version_definition_id": "VDP-3.2"
                    },
                    "repositories": [
                        {
                            "href": "http://example.com/api/v1/version_definitions/VDP-3.2/operating_systems/redhat-ppc7/repositories/VDP-3.2",
                            "Repositories": {
                                "applicable_services": [],
                                "base_url": "https://ad-vdp.s3.us-west-1.amazonaws.com/VDP/centos7-ppc/3.x/updates/3.2.2.0-1",
                                "components": null,
                                "default_base_url": "https://ad-vdp.s3.us-west-1.amazonaws.com/VDP/centos7-ppc/3.x/updates/3.2.2.0-1",
                                "distribution": null,
                                "mirrors_list": null,
                                "os_type": "redhat-ppc7",
                                "repo_id": "VDP-3.2",
                                "repo_name": "VDP",
                                "stack_name": "VDP",
                                "stack_version": "3.2",
                                "tags": [],
                                "unique": false,
                                "version_definition_id": "VDP-3.2"
                            }
                        },
                        {
                            "href": "http://example.com/api/v1/version_definitions/VDP-3.2/operating_systems/redhat-ppc7/repositories/VDP-3.2-GPL",
                            "Repositories": {
                                "applicable_services": [],
                                "base_url": "https://ad-vdp.s3.us-west-1.amazonaws.com/VDP-GPL/centos7-ppc/3.x/updates/3.2.2.0-1",
                                "components": null,
                                "default_base_url": "https://ad-vdp.s3.us-west-1.amazonaws.com/VDP-GPL/centos7-ppc/3.x/updates/3.2.2.0-1",
                                "distribution": null,
                                "mirrors_list": null,
                                "os_type": "redhat-ppc7",
                                "repo_id": "VDP-3.2-GPL",
                                "repo_name": "VDP-GPL",
                                "stack_name": "VDP",
                                "stack_version": "3.2",
                                "tags": [
                                    "GPL"
                                ],
                                "unique": false,
                                "version_definition_id": "VDP-3.2"
                            }
                        },
                        {
                            "href": "http://example.com/api/v1/version_definitions/VDP-3.2/operating_systems/redhat-ppc7/repositories/VDP-UTILS-1.1.0.22",
                            "Repositories": {
                                "applicable_services": [],
                                "base_url": "https://ad-vdp.s3.us-west-1.amazonaws.com/VDP-UTILS-1.1.0.22/repos/centos7-ppc",
                                "components": null,
                                "default_base_url": "https://ad-vdp.s3.us-west-1.amazonaws.com/VDP-UTILS-1.1.0.22/repos/centos7-ppc",
                                "distribution": null,
                                "mirrors_list": null,
                                "os_type": "redhat-ppc7",
                                "repo_id": "VDP-UTILS-1.1.0.22",
                                "repo_name": "VDP-UTILS",
                                "stack_name": "VDP",
                                "stack_version": "3.2",
                                "tags": [],
                                "unique": false,
                                "version_definition_id": "VDP-3.2"
                            }
                        }
                    ]
                },
                {
                    "href": "http://example.com/api/v1/version_definitions/VDP-3.2/operating_systems/redhat7",
                    "OperatingSystems": {
                        "os_type": "redhat7",
                        "stack_name": "VDP",
                        "stack_version": "3.2",
                        "version_definition_id": "VDP-3.2"
                    },
                    "repositories": [
                        {
                            "href": "http://example.com/api/v1/version_definitions/VDP-3.2/operating_systems/redhat7/repositories/VDP-3.2",
                            "Repositories": {
                                "applicable_services": [],
                                "base_url": "https://ad-vdp.s3.us-west-1.amazonaws.com/VDP/centos7/3.x/updates/3.2.2.0-1",
                                "components": null,
                                "default_base_url": "https://ad-vdp.s3.us-west-1.amazonaws.com/VDP/centos7/3.x/updates/3.2.2.0-1",
                                "distribution": null,
                                "mirrors_list": null,
                                "os_type": "redhat7",
                                "repo_id": "VDP-3.2",
                                "repo_name": "VDP",
                                "stack_name": "VDP",
                                "stack_version": "3.2",
                                "tags": [],
                                "unique": false,
                                "version_definition_id": "VDP-3.2"
                            }
                        },
                        {
                            "href": "http://example.com/api/v1/version_definitions/VDP-3.2/operating_systems/redhat7/repositories/VDP-3.2-GPL",
                            "Repositories": {
                                "applicable_services": [],
                                "base_url": "https://ad-vdp.s3.us-west-1.amazonaws.com/VDP-GPL/centos7/3.x/updates/3.2.2.0-1",
                                "components": null,
                                "default_base_url": "https://ad-vdp.s3.us-west-1.amazonaws.com/VDP-GPL/centos7/3.x/updates/3.2.2.0-1",
                                "distribution": null,
                                "mirrors_list": null,
                                "os_type": "redhat7",
                                "repo_id": "VDP-3.2-GPL",
                                "repo_name": "VDP-GPL",
                                "stack_name": "VDP",
                                "stack_version": "3.2",
                                "tags": [
                                    "GPL"
                                ],
                                "unique": false,
                                "version_definition_id": "VDP-3.2"
                            }
                        },
                        {
                            "href": "http://example.com/api/v1/version_definitions/VDP-3.2/operating_systems/redhat7/repositories/VDP-UTILS-1.1.0.22",
                            "Repositories": {
                                "applicable_services": [],
                                "base_url": "https://ad-vdp.s3.us-west-1.amazonaws.com/VDP-UTILS-1.1.0.22/repos/centos7",
                                "components": null,
                                "default_base_url": "https://ad-vdp.s3.us-west-1.amazonaws.com/VDP-UTILS-1.1.0.22/repos/centos7",
                                "distribution": null,
                                "mirrors_list": null,
                                "os_type": "redhat7",
                                "repo_id": "VDP-UTILS-1.1.0.22",
                                "repo_name": "VDP-UTILS",
                                "stack_name": "VDP",
                                "stack_version": "3.2",
                                "tags": [],
                                "unique": false,
                                "version_definition_id": "VDP-3.2"
                            }
                        }
                    ]
                },
                {
                    "href": "http://example.com/api/v1/version_definitions/VDP-3.2/operating_systems/redhat8",
                    "OperatingSystems": {
                        "os_type": "redhat8",
                        "stack_name": "VDP",
                        "stack_version": "3.2",
                        "version_definition_id": "VDP-3.2"
                    },
                    "repositories": [
                        {
                            "href": "http://example.com/api/v1/version_definitions/VDP-3.2/operating_systems/redhat8/repositories/VDP-3.2",
                            "Repositories": {
                                "applicable_services": [],
                                "base_url": "https://ad-vdp.s3.us-west-1.amazonaws.com/VDP/redhat8/3.x/updates/3.2.2.0-1",
                                "components": null,
                                "default_base_url": "https://ad-vdp.s3.us-west-1.amazonaws.com/VDP/redhat8/3.x/updates/3.2.2.0-1",
                                "distribution": null,
                                "mirrors_list": null,
                                "os_type": "redhat8",
                                "repo_id": "VDP-3.2",
                                "repo_name": "VDP",
                                "stack_name": "VDP",
                                "stack_version": "3.2",
                                "tags": [],
                                "unique": false,
                                "version_definition_id": "VDP-3.2"
                            }
                        },
                        {
                            "href": "http://example.com/api/v1/version_definitions/VDP-3.2/operating_systems/redhat8/repositories/VDP-3.2-GPL",
                            "Repositories": {
                                "applicable_services": [],
                                "base_url": "https://ad-vdp.s3.us-west-1.amazonaws.com/VDP-GPL/redhat8/3.x/updates/3.2.2.0-1",
                                "components": null,
                                "default_base_url": "https://ad-vdp.s3.us-west-1.amazonaws.com/VDP-GPL/redhat8/3.x/updates/3.2.2.0-1",
                                "distribution": null,
                                "mirrors_list": null,
                                "os_type": "redhat8",
                                "repo_id": "VDP-3.2-GPL",
                                "repo_name": "VDP-GPL",
                                "stack_name": "VDP",
                                "stack_version": "3.2",
                                "tags": [
                                    "GPL"
                                ],
                                "unique": false,
                                "version_definition_id": "VDP-3.2"
                            }
                        },
                        {
                            "href": "http://example.com/api/v1/version_definitions/VDP-3.2/operating_systems/redhat8/repositories/VDP-UTILS-1.1.0.22",
                            "Repositories": {
                                "applicable_services": [],
                                "base_url": "https://ad-vdp.s3.us-west-1.amazonaws.com/VDP-UTILS-1.1.0.22/repos/redhat8",
                                "components": null,
                                "default_base_url": "https://ad-vdp.s3.us-west-1.amazonaws.com/VDP-UTILS-1.1.0.22/repos/redhat8",
                                "distribution": null,
                                "mirrors_list": null,
                                "os_type": "redhat8",
                                "repo_id": "VDP-UTILS-1.1.0.22",
                                "repo_name": "VDP-UTILS",
                                "stack_name": "VDP",
                                "stack_version": "3.2",
                                "tags": [],
                                "unique": false,
                                "version_definition_id": "VDP-3.2"
                            }
                        }
                    ]
                },
                {
                    "href": "http://example.com/api/v1/version_definitions/VDP-3.2/operating_systems/suse12",
                    "OperatingSystems": {
                        "os_type": "suse12",
                        "stack_name": "VDP",
                        "stack_version": "3.2",
                        "version_definition_id": "VDP-3.2"
                    },
                    "repositories": [
                        {
                            "href": "http://example.com/api/v1/version_definitions/VDP-3.2/operating_systems/suse12/repositories/VDP-3.2",
                            "Repositories": {
                                "applicable_services": [],
                                "base_url": "https://ad-vdp.s3.us-west-1.amazonaws.com/VDP/sles12/3.x/updates/3.2.2.0-1",
                                "components": null,
                                "default_base_url": "https://ad-vdp.s3.us-west-1.amazonaws.com/VDP/sles12/3.x/updates/3.2.2.0-1",
                                "distribution": null,
                                "mirrors_list": null,
                                "os_type": "suse12",
                                "repo_id": "VDP-3.2",
                                "repo_name": "VDP",
                                "stack_name": "VDP",
                                "stack_version": "3.2",
                                "tags": [],
                                "unique": false,
                                "version_definition_id": "VDP-3.2"
                            }
                        },
                        {
                            "href": "http://example.com/api/v1/version_definitions/VDP-3.2/operating_systems/suse12/repositories/VDP-3.2-GPL",
                            "Repositories": {
                                "applicable_services": [],
                                "base_url": "https://ad-vdp.s3.us-west-1.amazonaws.com/VDP-GPL/sles12/3.x/updates/3.2.2.0-1",
                                "components": null,
                                "default_base_url": "https://ad-vdp.s3.us-west-1.amazonaws.com/VDP-GPL/sles12/3.x/updates/3.2.2.0-1",
                                "distribution": null,
                                "mirrors_list": null,
                                "os_type": "suse12",
                                "repo_id": "VDP-3.2-GPL",
                                "repo_name": "VDP-GPL",
                                "stack_name": "VDP",
                                "stack_version": "3.2",
                                "tags": [
                                    "GPL"
                                ],
                                "unique": false,
                                "version_definition_id": "VDP-3.2"
                            }
                        },
                        {
                            "href": "http://example.com/api/v1/version_definitions/VDP-3.2/operating_systems/suse12/repositories/VDP-UTILS-1.1.0.22",
                            "Repositories": {
                                "applicable_services": [],
                                "base_url": "https://ad-vdp.s3.us-west-1.amazonaws.com/VDP-UTILS-1.1.0.22/repos/sles12",
                                "components": null,
                                "default_base_url": "https://ad-vdp.s3.us-west-1.amazonaws.com/VDP-UTILS-1.1.0.22/repos/sles12",
                                "distribution": null,
                                "mirrors_list": null,
                                "os_type": "suse12",
                                "repo_id": "VDP-UTILS-1.1.0.22",
                                "repo_name": "VDP-UTILS",
                                "stack_name": "VDP",
                                "stack_version": "3.2",
                                "tags": [],
                                "unique": false,
                                "version_definition_id": "VDP-3.2"
                            }
                        }
                    ]
                },
                {
                    "href": "http://example.com/api/v1/version_definitions/VDP-3.2/operating_systems/ubuntu14",
                    "OperatingSystems": {
                        "os_type": "ubuntu14",
                        "stack_name": "VDP",
                        "stack_version": "3.2",
                        "version_definition_id": "VDP-3.2"
                    },
                    "repositories": [
                        {
                            "href": "http://example.com/api/v1/version_definitions/VDP-3.2/operating_systems/ubuntu14/repositories/VDP-3.2",
                            "Repositories": {
                                "applicable_services": [],
                                "base_url": "https://ad-vdp.s3.us-west-1.amazonaws.com/VDP/ubuntu14/3.x/updates/3.2.2.0-1",
                                "components": null,
                                "default_base_url": "https://ad-vdp.s3.us-west-1.amazonaws.com/VDP/ubuntu14/3.x/updates/3.2.2.0-1",
                                "distribution": null,
                                "mirrors_list": null,
                                "os_type": "ubuntu14",
                                "repo_id": "VDP-3.2",
                                "repo_name": "VDP",
                                "stack_name": "VDP",
                                "stack_version": "3.2",
                                "tags": [],
                                "unique": false,
                                "version_definition_id": "VDP-3.2"
                            }
                        },
                        {
                            "href": "http://example.com/api/v1/version_definitions/VDP-3.2/operating_systems/ubuntu14/repositories/VDP-3.2-GPL",
                            "Repositories": {
                                "applicable_services": [],
                                "base_url": "https://ad-vdp.s3.us-west-1.amazonaws.com/VDP-GPL/ubuntu14/3.x/updates/3.2.2.0-1",
                                "components": null,
                                "default_base_url": "https://ad-vdp.s3.us-west-1.amazonaws.com/VDP-GPL/ubuntu14/3.x/updates/3.2.2.0-1",
                                "distribution": null,
                                "mirrors_list": null,
                                "os_type": "ubuntu14",
                                "repo_id": "VDP-3.2-GPL",
                                "repo_name": "VDP-GPL",
                                "stack_name": "VDP",
                                "stack_version": "3.2",
                                "tags": [
                                    "GPL"
                                ],
                                "unique": false,
                                "version_definition_id": "VDP-3.2"
                            }
                        },
                        {
                            "href": "http://example.com/api/v1/version_definitions/VDP-3.2/operating_systems/ubuntu14/repositories/VDP-UTILS-1.1.0.22",
                            "Repositories": {
                                "applicable_services": [],
                                "base_url": "https://ad-vdp.s3.us-west-1.amazonaws.com/VDP-UTILS-1.1.0.22/repos/ubuntu14",
                                "components": null,
                                "default_base_url": "https://ad-vdp.s3.us-west-1.amazonaws.com/VDP-UTILS-1.1.0.22/repos/ubuntu14",
                                "distribution": null,
                                "mirrors_list": null,
                                "os_type": "ubuntu14",
                                "repo_id": "VDP-UTILS-1.1.0.22",
                                "repo_name": "VDP-UTILS",
                                "stack_name": "VDP",
                                "stack_version": "3.2",
                                "tags": [],
                                "unique": false,
                                "version_definition_id": "VDP-3.2"
                            }
                        }
                    ]
                },
                {
                    "href": "http://example.com/api/v1/version_definitions/VDP-3.2/operating_systems/ubuntu16",
                    "OperatingSystems": {
                        "os_type": "ubuntu16",
                        "stack_name": "VDP",
                        "stack_version": "3.2",
                        "version_definition_id": "VDP-3.2"
                    },
                    "repositories": [
                        {
                            "href": "http://example.com/api/v1/version_definitions/VDP-3.2/operating_systems/ubuntu16/repositories/VDP-3.2",
                            "Repositories": {
                                "applicable_services": [],
                                "base_url": "https://ad-vdp.s3.us-west-1.amazonaws.com/VDP/ubuntu16/3.x/updates/3.2.2.0-1",
                                "components": null,
                                "default_base_url": "https://ad-vdp.s3.us-west-1.amazonaws.com/VDP/ubuntu16/3.x/updates/3.2.2.0-1",
                                "distribution": null,
                                "mirrors_list": null,
                                "os_type": "ubuntu16",
                                "repo_id": "VDP-3.2",
                                "repo_name": "VDP",
                                "stack_name": "VDP",
                                "stack_version": "3.2",
                                "tags": [],
                                "unique": false,
                                "version_definition_id": "VDP-3.2"
                            }
                        },
                        {
                            "href": "http://example.com/api/v1/version_definitions/VDP-3.2/operating_systems/ubuntu16/repositories/VDP-3.2-GPL",
                            "Repositories": {
                                "applicable_services": [],
                                "base_url": "https://ad-vdp.s3.us-west-1.amazonaws.com/VDP-GPL/ubuntu16/3.x/updates/3.2.2.0-1",
                                "components": null,
                                "default_base_url": "https://ad-vdp.s3.us-west-1.amazonaws.com/VDP-GPL/ubuntu16/3.x/updates/3.2.2.0-1",
                                "distribution": null,
                                "mirrors_list": null,
                                "os_type": "ubuntu16",
                                "repo_id": "VDP-3.2-GPL",
                                "repo_name": "VDP-GPL",
                                "stack_name": "VDP",
                                "stack_version": "3.2",
                                "tags": [
                                    "GPL"
                                ],
                                "unique": false,
                                "version_definition_id": "VDP-3.2"
                            }
                        },
                        {
                            "href": "http://example.com/api/v1/version_definitions/VDP-3.2/operating_systems/ubuntu16/repositories/VDP-UTILS-1.1.0.22",
                            "Repositories": {
                                "applicable_services": [],
                                "base_url": "https://ad-vdp.s3.us-west-1.amazonaws.com/VDP-UTILS-1.1.0.22/repos/ubuntu16",
                                "components": null,
                                "default_base_url": "https://ad-vdp.s3.us-west-1.amazonaws.com/VDP-UTILS-1.1.0.22/repos/ubuntu16",
                                "distribution": null,
                                "mirrors_list": null,
                                "os_type": "ubuntu16",
                                "repo_id": "VDP-UTILS-1.1.0.22",
                                "repo_name": "VDP-UTILS",
                                "stack_name": "VDP",
                                "stack_version": "3.2",
                                "tags": [],
                                "unique": false,
                                "version_definition_id": "VDP-3.2"
                            }
                        }
                    ]
                },
                {
                    "href": "http://example.com/api/v1/version_definitions/VDP-3.2/operating_systems/ubuntu18",
                    "OperatingSystems": {
                        "os_type": "ubuntu18",
                        "stack_name": "VDP",
                        "stack_version": "3.2",
                        "version_definition_id": "VDP-3.2"
                    },
                    "repositories": [
                        {
                            "href": "http://example.com/api/v1/version_definitions/VDP-3.2/operating_systems/ubuntu18/repositories/VDP-3.2",
                            "Repositories": {
                                "applicable_services": [],
                                "base_url": "https://ad-vdp.s3.us-west-1.amazonaws.com/VDP/ubuntu18/3.x/updates/3.2.2.0-1",
                                "components": null,
                                "default_base_url": "https://ad-vdp.s3.us-west-1.amazonaws.com/VDP/ubuntu18/3.x/updates/3.2.2.0-1",
                                "distribution": null,
                                "mirrors_list": null,
                                "os_type": "ubuntu18",
                                "repo_id": "VDP-3.2",
                                "repo_name": "VDP",
                                "stack_name": "VDP",
                                "stack_version": "3.2",
                                "tags": [],
                                "unique": false,
                                "version_definition_id": "VDP-3.2"
                            }
                        },
                        {
                            "href": "http://example.com/api/v1/version_definitions/VDP-3.2/operating_systems/ubuntu18/repositories/VDP-3.2-GPL",
                            "Repositories": {
                                "applicable_services": [],
                                "base_url": "https://ad-vdp.s3.us-west-1.amazonaws.com/VDP-GPL/ubuntu18/3.x/updates/3.2.2.0-1",
                                "components": null,
                                "default_base_url": "https://ad-vdp.s3.us-west-1.amazonaws.com/VDP-GPL/ubuntu18/3.x/updates/3.2.2.0-1",
                                "distribution": null,
                                "mirrors_list": null,
                                "os_type": "ubuntu18",
                                "repo_id": "VDP-3.2-GPL",
                                "repo_name": "VDP-GPL",
                                "stack_name": "VDP",
                                "stack_version": "3.2",
                                "tags": [
                                    "GPL"
                                ],
                                "unique": false,
                                "version_definition_id": "VDP-3.2"
                            }
                        },
                        {
                            "href": "http://example.com/api/v1/version_definitions/VDP-3.2/operating_systems/ubuntu18/repositories/VDP-UTILS-1.1.0.22",
                            "Repositories": {
                                "applicable_services": [],
                                "base_url": "https://ad-vdp.s3.us-west-1.amazonaws.com/VDP-UTILS-1.1.0.22/repos/ubuntu18",
                                "components": null,
                                "default_base_url": "https://ad-vdp.s3.us-west-1.amazonaws.com/VDP-UTILS-1.1.0.22/repos/ubuntu18",
                                "distribution": null,
                                "mirrors_list": null,
                                "os_type": "ubuntu18",
                                "repo_id": "VDP-UTILS-1.1.0.22",
                                "repo_name": "VDP-UTILS",
                                "stack_name": "VDP",
                                "stack_version": "3.2",
                                "tags": [],
                                "unique": false,
                                "version_definition_id": "VDP-3.2"
                            }
                        }
                    ]
                }
            ]
        },
        {
            "href": "http://example.com/api/v1/version_definitions/VDP-3.3",
            "VersionDefinition": {
                "id": "VDP-3.3",
                "repository_version": "3.3",
                "show_available": true,
                "stack_default": true,
                "stack_name": "VDP",
                "stack_repo_update_link_exists": true,
                "stack_services": [
                    {
                        "name": "TEZ",
                        "display_name": "Tez",
                        "comment": "Tez is the next generation Hadoop Query Processing framework written on top of YARN.",
                        "versions": [
                            "0.10.2"
                        ]
                    },
                    {
                        "name": "AMBARI_METRICS",
                        "display_name": "Ambari Metrics",
                        "comment": "A system for metrics collection that provides storage and retrieval capability for metrics collected from the cluster\n      ",
                        "versions": [
                            "0.2.1"
                        ]
                    },
                    {
                        "name": "RANGER_KMS",
                        "display_name": "Ranger KMS",
                        "comment": "Key Management Server",
                        "versions": [
                            "2.4.0"
                        ]
                    },
                    {
                        "name": "HBASE",
                        "display_name": "HBase",
                        "comment": "Non-relational distributed database and centralized service for configuration management &\n        synchronization\n      ",
                        "versions": [
                            "2.6.0"
                        ]
                    },
                    {
                        "name": "SSM",
                        "display_name": "SSM",
                        "comment": "Manage and Optimize storage for HDFS",
                        "versions": [
                            "1.6.0"
                        ]
                    },
                    {
                        "name": "MAPREDUCE2",
                        "display_name": "MapReduce2",
                        "comment": "Apache Hadoop NextGen MapReduce (YARN)",
                        "versions": [
                            "3.3.6"
                        ]
                    },
                    {
                        "name": "HDFS",
                        "display_name": "HDFS",
                        "comment": "Apache Hadoop Distributed File System",
                        "versions": [
                            "3.3.6"
                        ]
                    },
                    {
                        "name": "TRINO",
                        "display_name": "Trino",
                        "comment": "Trino is an open source distributed SQL query engine for running\n            interactive analytic queries against data sources of all sizes ranging\n            from gigabytes to petabytes.",
                        "versions": [
                            "420.0"
                        ]
                    },
                    {
                        "name": "SPARK3",
                        "display_name": "Spark3",
                        "comment": "Apache Spark 3.x is a fast and general engine for large-scale data processing.",
                        "versions": [
                            "3.4.1"
                        ]
                    },
                    {
                        "name": "ZOOKEEPER",
                        "display_name": "ZooKeeper",
                        "comment": "Centralized service which provides highly reliable distributed coordination",
                        "versions": [
                            "3.8.4"
                        ]
                    },
                    {
                        "name": "RANGER",
                        "display_name": "Ranger",
                        "comment": "Comprehensive security for Hadoop",
                        "versions": [
                            "2.4.0"
                        ]
                    },
                    {
                        "name": "HIVE",
                        "display_name": "Hive",
                        "comment": "Data warehouse system for ad-hoc queries & analysis of large datasets and table & storage management service",
                        "versions": [
                            "3.1.2"
                        ]
                    },
                    {
                        "name": "KERBEROS",
                        "display_name": "Kerberos",
                        "comment": "A computer network authentication protocol which works on\n        the basis of 'tickets' to allow nodes communicating over a\n        non-secure network to prove their identity to one another in a\n        secure manner.\n      ",
                        "versions": [
                            "1.10.3-30"
                        ]
                    },
                    {
                        "name": "YARN",
                        "display_name": "YARN",
                        "comment": "Apache Hadoop NextGen MapReduce (YARN)",
                        "versions": [
                            "3.3.6"
                        ]
                    }
                ],
                "stack_version": "3.3",
                "type": "STANDARD"
            },
            "operating_systems": [
                {
                    "href": "http://example.com/api/v1/version_definitions/VDP-3.3/operating_systems/amazonlinux2",
                    "OperatingSystems": {
                        "os_type": "amazonlinux2",
                        "stack_name": "VDP",
                        "stack_version": "3.3",
                        "version_definition_id": "VDP-3.3"
                    },
                    "repositories": [
                        {
                            "href": "http://example.com/api/v1/version_definitions/VDP-3.3/operating_systems/amazonlinux2/repositories/VDP-3.3",
                            "Repositories": {
                                "applicable_services": [],
                                "base_url": "https://ad-vdp.s3.us-west-1.amazonaws.com/VDP/amazonlinux2/3.x/updates/3.3.0.0",
                                "components": null,
                                "default_base_url": "https://ad-vdp.s3.us-west-1.amazonaws.com/VDP/amazonlinux2/3.x/updates/3.3.0.0",
                                "distribution": null,
                                "mirrors_list": null,
                                "os_type": "amazonlinux2",
                                "repo_id": "VDP-3.3",
                                "repo_name": "VDP",
                                "stack_name": "VDP",
                                "stack_version": "3.3",
                                "tags": [],
                                "unique": false,
                                "version_definition_id": "VDP-3.3"
                            }
                        },
                        {
                            "href": "http://example.com/api/v1/version_definitions/VDP-3.3/operating_systems/amazonlinux2/repositories/VDP-3.3-GPL",
                            "Repositories": {
                                "applicable_services": [],
                                "base_url": "https://ad-vdp.s3.us-west-1.amazonaws.com/VDP-GPL/amazonlinux2/3.x/updates/3.3.0.0",
                                "components": null,
                                "default_base_url": "https://ad-vdp.s3.us-west-1.amazonaws.com/VDP-GPL/amazonlinux2/3.x/updates/3.3.0.0",
                                "distribution": null,
                                "mirrors_list": null,
                                "os_type": "amazonlinux2",
                                "repo_id": "VDP-3.3-GPL",
                                "repo_name": "VDP-GPL",
                                "stack_name": "VDP",
                                "stack_version": "3.3",
                                "tags": [
                                    "GPL"
                                ],
                                "unique": false,
                                "version_definition_id": "VDP-3.3"
                            }
                        },
                        {
                            "href": "http://example.com/api/v1/version_definitions/VDP-3.3/operating_systems/amazonlinux2/repositories/VDP-UTILS-1.1.0.22",
                            "Repositories": {
                                "applicable_services": [],
                                "base_url": "https://ad-vdp.s3.us-west-1.amazonaws.com/VDP-UTILS-1.1.0.22/repos/amazonlinux2",
                                "components": null,
                                "default_base_url": "https://ad-vdp.s3.us-west-1.amazonaws.com/VDP-UTILS-1.1.0.22/repos/amazonlinux2",
                                "distribution": null,
                                "mirrors_list": null,
                                "os_type": "amazonlinux2",
                                "repo_id": "VDP-UTILS-1.1.0.22",
                                "repo_name": "VDP-UTILS",
                                "stack_name": "VDP",
                                "stack_version": "3.3",
                                "tags": [],
                                "unique": false,
                                "version_definition_id": "VDP-3.3"
                            }
                        }
                    ]
                },
                {
                    "href": "http://example.com/api/v1/version_definitions/VDP-3.3/operating_systems/debian9",
                    "OperatingSystems": {
                        "os_type": "debian9",
                        "stack_name": "VDP",
                        "stack_version": "3.3",
                        "version_definition_id": "VDP-3.3"
                    },
                    "repositories": [
                        {
                            "href": "http://example.com/api/v1/version_definitions/VDP-3.3/operating_systems/debian9/repositories/VDP-3.3",
                            "Repositories": {
                                "applicable_services": [],
                                "base_url": "https://ad-vdp.s3.us-west-1.amazonaws.com/VDP/debian9/3.x/updates/3.3.0.0",
                                "components": null,
                                "default_base_url": "https://ad-vdp.s3.us-west-1.amazonaws.com/VDP/debian9/3.x/updates/3.3.0.0",
                                "distribution": null,
                                "mirrors_list": null,
                                "os_type": "debian9",
                                "repo_id": "VDP-3.3",
                                "repo_name": "VDP",
                                "stack_name": "VDP",
                                "stack_version": "3.3",
                                "tags": [],
                                "unique": false,
                                "version_definition_id": "VDP-3.3"
                            }
                        },
                        {
                            "href": "http://example.com/api/v1/version_definitions/VDP-3.3/operating_systems/debian9/repositories/VDP-3.3-GPL",
                            "Repositories": {
                                "applicable_services": [],
                                "base_url": "https://ad-vdp.s3.us-west-1.amazonaws.com/VDP-GPL/debian9/3.x/updates/3.3.0.0",
                                "components": null,
                                "default_base_url": "https://ad-vdp.s3.us-west-1.amazonaws.com/VDP-GPL/debian9/3.x/updates/3.3.0.0",
                                "distribution": null,
                                "mirrors_list": null,
                                "os_type": "debian9",
                                "repo_id": "VDP-3.3-GPL",
                                "repo_name": "VDP-GPL",
                                "stack_name": "VDP",
                                "stack_version": "3.3",
                                "tags": [
                                    "GPL"
                                ],
                                "unique": false,
                                "version_definition_id": "VDP-3.3"
                            }
                        },
                        {
                            "href": "http://example.com/api/v1/version_definitions/VDP-3.3/operating_systems/debian9/repositories/VDP-UTILS-1.1.0.22",
                            "Repositories": {
                                "applicable_services": [],
                                "base_url": "https://ad-vdp.s3.us-west-1.amazonaws.com/VDP-UTILS-1.1.0.22/repos/debian9",
                                "components": null,
                                "default_base_url": "https://ad-vdp.s3.us-west-1.amazonaws.com/VDP-UTILS-1.1.0.22/repos/debian9",
                                "distribution": null,
                                "mirrors_list": null,
                                "os_type": "debian9",
                                "repo_id": "VDP-UTILS-1.1.0.22",
                                "repo_name": "VDP-UTILS",
                                "stack_name": "VDP",
                                "stack_version": "3.3",
                                "tags": [],
                                "unique": false,
                                "version_definition_id": "VDP-3.3"
                            }
                        }
                    ]
                },
                {
                    "href": "http://example.com/api/v1/version_definitions/VDP-3.3/operating_systems/redhat-ppc7",
                    "OperatingSystems": {
                        "os_type": "redhat-ppc7",
                        "stack_name": "VDP",
                        "stack_version": "3.3",
                        "version_definition_id": "VDP-3.3"
                    },
                    "repositories": [
                        {
                            "href": "http://example.com/api/v1/version_definitions/VDP-3.3/operating_systems/redhat-ppc7/repositories/VDP-3.3",
                            "Repositories": {
                                "applicable_services": [],
                                "base_url": "https://ad-vdp.s3.us-west-1.amazonaws.com/VDP/centos7-ppc/3.x/updates/3.3.0.0",
                                "components": null,
                                "default_base_url": "https://ad-vdp.s3.us-west-1.amazonaws.com/VDP/centos7-ppc/3.x/updates/3.3.0.0",
                                "distribution": null,
                                "mirrors_list": null,
                                "os_type": "redhat-ppc7",
                                "repo_id": "VDP-3.3",
                                "repo_name": "VDP",
                                "stack_name": "VDP",
                                "stack_version": "3.3",
                                "tags": [],
                                "unique": false,
                                "version_definition_id": "VDP-3.3"
                            }
                        },
                        {
                            "href": "http://example.com/api/v1/version_definitions/VDP-3.3/operating_systems/redhat-ppc7/repositories/VDP-3.3-GPL",
                            "Repositories": {
                                "applicable_services": [],
                                "base_url": "https://ad-vdp.s3.us-west-1.amazonaws.com/VDP-GPL/centos7-ppc/3.x/updates/3.3.0.0",
                                "components": null,
                                "default_base_url": "https://ad-vdp.s3.us-west-1.amazonaws.com/VDP-GPL/centos7-ppc/3.x/updates/3.3.0.0",
                                "distribution": null,
                                "mirrors_list": null,
                                "os_type": "redhat-ppc7",
                                "repo_id": "VDP-3.3-GPL",
                                "repo_name": "VDP-GPL",
                                "stack_name": "VDP",
                                "stack_version": "3.3",
                                "tags": [
                                    "GPL"
                                ],
                                "unique": false,
                                "version_definition_id": "VDP-3.3"
                            }
                        },
                        {
                            "href": "http://example.com/api/v1/version_definitions/VDP-3.3/operating_systems/redhat-ppc7/repositories/VDP-UTILS-1.1.0.22",
                            "Repositories": {
                                "applicable_services": [],
                                "base_url": "https://ad-vdp.s3.us-west-1.amazonaws.com/VDP-UTILS-1.1.0.22/repos/centos7-ppc",
                                "components": null,
                                "default_base_url": "https://ad-vdp.s3.us-west-1.amazonaws.com/VDP-UTILS-1.1.0.22/repos/centos7-ppc",
                                "distribution": null,
                                "mirrors_list": null,
                                "os_type": "redhat-ppc7",
                                "repo_id": "VDP-UTILS-1.1.0.22",
                                "repo_name": "VDP-UTILS",
                                "stack_name": "VDP",
                                "stack_version": "3.3",
                                "tags": [],
                                "unique": false,
                                "version_definition_id": "VDP-3.3"
                            }
                        }
                    ]
                },
                {
                    "href": "http://example.com/api/v1/version_definitions/VDP-3.3/operating_systems/redhat7",
                    "OperatingSystems": {
                        "os_type": "redhat7",
                        "stack_name": "VDP",
                        "stack_version": "3.3",
                        "version_definition_id": "VDP-3.3"
                    },
                    "repositories": [
                        {
                            "href": "http://example.com/api/v1/version_definitions/VDP-3.3/operating_systems/redhat7/repositories/VDP-3.3",
                            "Repositories": {
                                "applicable_services": [],
                                "base_url": "https://ad-vdp.s3.us-west-1.amazonaws.com/VDP/centos7/3.x/updates/3.3.0.0",
                                "components": null,
                                "default_base_url": "https://ad-vdp.s3.us-west-1.amazonaws.com/VDP/centos7/3.x/updates/3.3.0.0",
                                "distribution": null,
                                "mirrors_list": null,
                                "os_type": "redhat7",
                                "repo_id": "VDP-3.3",
                                "repo_name": "VDP",
                                "stack_name": "VDP",
                                "stack_version": "3.3",
                                "tags": [],
                                "unique": false,
                                "version_definition_id": "VDP-3.3"
                            }
                        },
                        {
                            "href": "http://example.com/api/v1/version_definitions/VDP-3.3/operating_systems/redhat7/repositories/VDP-3.3-GPL",
                            "Repositories": {
                                "applicable_services": [],
                                "base_url": "https://ad-vdp.s3.us-west-1.amazonaws.com/VDP-GPL/centos7/3.x/updates/3.3.0.0",
                                "components": null,
                                "default_base_url": "https://ad-vdp.s3.us-west-1.amazonaws.com/VDP-GPL/centos7/3.x/updates/3.3.0.0",
                                "distribution": null,
                                "mirrors_list": null,
                                "os_type": "redhat7",
                                "repo_id": "VDP-3.3-GPL",
                                "repo_name": "VDP-GPL",
                                "stack_name": "VDP",
                                "stack_version": "3.3",
                                "tags": [
                                    "GPL"
                                ],
                                "unique": false,
                                "version_definition_id": "VDP-3.3"
                            }
                        },
                        {
                            "href": "http://example.com/api/v1/version_definitions/VDP-3.3/operating_systems/redhat7/repositories/VDP-UTILS-1.1.0.22",
                            "Repositories": {
                                "applicable_services": [],
                                "base_url": "https://ad-vdp.s3.us-west-1.amazonaws.com/VDP-UTILS-1.1.0.22/repos/centos7",
                                "components": null,
                                "default_base_url": "https://ad-vdp.s3.us-west-1.amazonaws.com/VDP-UTILS-1.1.0.22/repos/centos7",
                                "distribution": null,
                                "mirrors_list": null,
                                "os_type": "redhat7",
                                "repo_id": "VDP-UTILS-1.1.0.22",
                                "repo_name": "VDP-UTILS",
                                "stack_name": "VDP",
                                "stack_version": "3.3",
                                "tags": [],
                                "unique": false,
                                "version_definition_id": "VDP-3.3"
                            }
                        }
                    ]
                },
                {
                    "href": "http://example.com/api/v1/version_definitions/VDP-3.3/operating_systems/redhat8",
                    "OperatingSystems": {
                        "os_type": "redhat8",
                        "stack_name": "VDP",
                        "stack_version": "3.3",
                        "version_definition_id": "VDP-3.3"
                    },
                    "repositories": [
                        {
                            "href": "http://example.com/api/v1/version_definitions/VDP-3.3/operating_systems/redhat8/repositories/VDP-3.3",
                            "Repositories": {
                                "applicable_services": [],
                                "base_url": "https://ad-vdp.s3.us-west-1.amazonaws.com/VDP/redhat8/3.x/updates/3.3.0.0",
                                "components": null,
                                "default_base_url": "https://ad-vdp.s3.us-west-1.amazonaws.com/VDP/redhat8/3.x/updates/3.3.0.0",
                                "distribution": null,
                                "mirrors_list": null,
                                "os_type": "redhat8",
                                "repo_id": "VDP-3.3",
                                "repo_name": "VDP",
                                "stack_name": "VDP",
                                "stack_version": "3.3",
                                "tags": [],
                                "unique": false,
                                "version_definition_id": "VDP-3.3"
                            }
                        },
                        {
                            "href": "http://example.com/api/v1/version_definitions/VDP-3.3/operating_systems/redhat8/repositories/VDP-3.3-GPL",
                            "Repositories": {
                                "applicable_services": [],
                                "base_url": "https://ad-vdp.s3.us-west-1.amazonaws.com/VDP-GPL/redhat8/3.x/updates/3.3.0.0",
                                "components": null,
                                "default_base_url": "https://ad-vdp.s3.us-west-1.amazonaws.com/VDP-GPL/redhat8/3.x/updates/3.3.0.0",
                                "distribution": null,
                                "mirrors_list": null,
                                "os_type": "redhat8",
                                "repo_id": "VDP-3.3-GPL",
                                "repo_name": "VDP-GPL",
                                "stack_name": "VDP",
                                "stack_version": "3.3",
                                "tags": [
                                    "GPL"
                                ],
                                "unique": false,
                                "version_definition_id": "VDP-3.3"
                            }
                        },
                        {
                            "href": "http://example.com/api/v1/version_definitions/VDP-3.3/operating_systems/redhat8/repositories/VDP-UTILS-1.1.0.22",
                            "Repositories": {
                                "applicable_services": [],
                                "base_url": "https://ad-vdp.s3.us-west-1.amazonaws.com/VDP-UTILS-1.1.0.22/repos/redhat8",
                                "components": null,
                                "default_base_url": "https://ad-vdp.s3.us-west-1.amazonaws.com/VDP-UTILS-1.1.0.22/repos/redhat8",
                                "distribution": null,
                                "mirrors_list": null,
                                "os_type": "redhat8",
                                "repo_id": "VDP-UTILS-1.1.0.22",
                                "repo_name": "VDP-UTILS",
                                "stack_name": "VDP",
                                "stack_version": "3.3",
                                "tags": [],
                                "unique": false,
                                "version_definition_id": "VDP-3.3"
                            }
                        }
                    ]
                },
                {
                    "href": "http://example.com/api/v1/version_definitions/VDP-3.3/operating_systems/suse12",
                    "OperatingSystems": {
                        "os_type": "suse12",
                        "stack_name": "VDP",
                        "stack_version": "3.3",
                        "version_definition_id": "VDP-3.3"
                    },
                    "repositories": [
                        {
                            "href": "http://example.com/api/v1/version_definitions/VDP-3.3/operating_systems/suse12/repositories/VDP-3.3",
                            "Repositories": {
                                "applicable_services": [],
                                "base_url": "https://ad-vdp.s3.us-west-1.amazonaws.com/VDP/sles12/3.x/updates/3.3.0.0",
                                "components": null,
                                "default_base_url": "https://ad-vdp.s3.us-west-1.amazonaws.com/VDP/sles12/3.x/updates/3.3.0.0",
                                "distribution": null,
                                "mirrors_list": null,
                                "os_type": "suse12",
                                "repo_id": "VDP-3.3",
                                "repo_name": "VDP",
                                "stack_name": "VDP",
                                "stack_version": "3.3",
                                "tags": [],
                                "unique": false,
                                "version_definition_id": "VDP-3.3"
                            }
                        },
                        {
                            "href": "http://example.com/api/v1/version_definitions/VDP-3.3/operating_systems/suse12/repositories/VDP-3.3-GPL",
                            "Repositories": {
                                "applicable_services": [],
                                "base_url": "https://ad-vdp.s3.us-west-1.amazonaws.com/VDP-GPL/sles12/3.x/updates/3.3.0.0",
                                "components": null,
                                "default_base_url": "https://ad-vdp.s3.us-west-1.amazonaws.com/VDP-GPL/sles12/3.x/updates/3.3.0.0",
                                "distribution": null,
                                "mirrors_list": null,
                                "os_type": "suse12",
                                "repo_id": "VDP-3.3-GPL",
                                "repo_name": "VDP-GPL",
                                "stack_name": "VDP",
                                "stack_version": "3.3",
                                "tags": [
                                    "GPL"
                                ],
                                "unique": false,
                                "version_definition_id": "VDP-3.3"
                            }
                        },
                        {
                            "href": "http://example.com/api/v1/version_definitions/VDP-3.3/operating_systems/suse12/repositories/VDP-UTILS-1.1.0.22",
                            "Repositories": {
                                "applicable_services": [],
                                "base_url": "https://ad-vdp.s3.us-west-1.amazonaws.com/VDP-UTILS-1.1.0.22/repos/sles12",
                                "components": null,
                                "default_base_url": "https://ad-vdp.s3.us-west-1.amazonaws.com/VDP-UTILS-1.1.0.22/repos/sles12",
                                "distribution": null,
                                "mirrors_list": null,
                                "os_type": "suse12",
                                "repo_id": "VDP-UTILS-1.1.0.22",
                                "repo_name": "VDP-UTILS",
                                "stack_name": "VDP",
                                "stack_version": "3.3",
                                "tags": [],
                                "unique": false,
                                "version_definition_id": "VDP-3.3"
                            }
                        }
                    ]
                },
                {
                    "href": "http://example.com/api/v1/version_definitions/VDP-3.3/operating_systems/ubuntu14",
                    "OperatingSystems": {
                        "os_type": "ubuntu14",
                        "stack_name": "VDP",
                        "stack_version": "3.3",
                        "version_definition_id": "VDP-3.3"
                    },
                    "repositories": [
                        {
                            "href": "http://example.com/api/v1/version_definitions/VDP-3.3/operating_systems/ubuntu14/repositories/VDP-3.3",
                            "Repositories": {
                                "applicable_services": [],
                                "base_url": "https://ad-vdp.s3.us-west-1.amazonaws.com/VDP/ubuntu14/3.x/updates/3.3.0.0",
                                "components": null,
                                "default_base_url": "https://ad-vdp.s3.us-west-1.amazonaws.com/VDP/ubuntu14/3.x/updates/3.3.0.0",
                                "distribution": null,
                                "mirrors_list": null,
                                "os_type": "ubuntu14",
                                "repo_id": "VDP-3.3",
                                "repo_name": "VDP",
                                "stack_name": "VDP",
                                "stack_version": "3.3",
                                "tags": [],
                                "unique": false,
                                "version_definition_id": "VDP-3.3"
                            }
                        },
                        {
                            "href": "http://example.com/api/v1/version_definitions/VDP-3.3/operating_systems/ubuntu14/repositories/VDP-3.3-GPL",
                            "Repositories": {
                                "applicable_services": [],
                                "base_url": "https://ad-vdp.s3.us-west-1.amazonaws.com/VDP-GPL/ubuntu14/3.x/updates/3.3.0.0",
                                "components": null,
                                "default_base_url": "https://ad-vdp.s3.us-west-1.amazonaws.com/VDP-GPL/ubuntu14/3.x/updates/3.3.0.0",
                                "distribution": null,
                                "mirrors_list": null,
                                "os_type": "ubuntu14",
                                "repo_id": "VDP-3.3-GPL",
                                "repo_name": "VDP-GPL",
                                "stack_name": "VDP",
                                "stack_version": "3.3",
                                "tags": [
                                    "GPL"
                                ],
                                "unique": false,
                                "version_definition_id": "VDP-3.3"
                            }
                        },
                        {
                            "href": "http://example.com/api/v1/version_definitions/VDP-3.3/operating_systems/ubuntu14/repositories/VDP-UTILS-1.1.0.22",
                            "Repositories": {
                                "applicable_services": [],
                                "base_url": "https://ad-vdp.s3.us-west-1.amazonaws.com/VDP-UTILS-1.1.0.22/repos/ubuntu14",
                                "components": null,
                                "default_base_url": "https://ad-vdp.s3.us-west-1.amazonaws.com/VDP-UTILS-1.1.0.22/repos/ubuntu14",
                                "distribution": null,
                                "mirrors_list": null,
                                "os_type": "ubuntu14",
                                "repo_id": "VDP-UTILS-1.1.0.22",
                                "repo_name": "VDP-UTILS",
                                "stack_name": "VDP",
                                "stack_version": "3.3",
                                "tags": [],
                                "unique": false,
                                "version_definition_id": "VDP-3.3"
                            }
                        }
                    ]
                },
                {
                    "href": "http://example.com/api/v1/version_definitions/VDP-3.3/operating_systems/ubuntu16",
                    "OperatingSystems": {
                        "os_type": "ubuntu16",
                        "stack_name": "VDP",
                        "stack_version": "3.3",
                        "version_definition_id": "VDP-3.3"
                    },
                    "repositories": [
                        {
                            "href": "http://example.com/api/v1/version_definitions/VDP-3.3/operating_systems/ubuntu16/repositories/VDP-3.3",
                            "Repositories": {
                                "applicable_services": [],
                                "base_url": "https://ad-vdp.s3.us-west-1.amazonaws.com/VDP/ubuntu16/3.x/updates/3.3.0.0",
                                "components": null,
                                "default_base_url": "https://ad-vdp.s3.us-west-1.amazonaws.com/VDP/ubuntu16/3.x/updates/3.3.0.0",
                                "distribution": null,
                                "mirrors_list": null,
                                "os_type": "ubuntu16",
                                "repo_id": "VDP-3.3",
                                "repo_name": "VDP",
                                "stack_name": "VDP",
                                "stack_version": "3.3",
                                "tags": [],
                                "unique": false,
                                "version_definition_id": "VDP-3.3"
                            }
                        },
                        {
                            "href": "http://example.com/api/v1/version_definitions/VDP-3.3/operating_systems/ubuntu16/repositories/VDP-3.3-GPL",
                            "Repositories": {
                                "applicable_services": [],
                                "base_url": "https://ad-vdp.s3.us-west-1.amazonaws.com/VDP-GPL/ubuntu16/3.x/updates/3.3.0.0",
                                "components": null,
                                "default_base_url": "https://ad-vdp.s3.us-west-1.amazonaws.com/VDP-GPL/ubuntu16/3.x/updates/3.3.0.0",
                                "distribution": null,
                                "mirrors_list": null,
                                "os_type": "ubuntu16",
                                "repo_id": "VDP-3.3-GPL",
                                "repo_name": "VDP-GPL",
                                "stack_name": "VDP",
                                "stack_version": "3.3",
                                "tags": [
                                    "GPL"
                                ],
                                "unique": false,
                                "version_definition_id": "VDP-3.3"
                            }
                        },
                        {
                            "href": "http://example.com/api/v1/version_definitions/VDP-3.3/operating_systems/ubuntu16/repositories/VDP-UTILS-1.1.0.22",
                            "Repositories": {
                                "applicable_services": [],
                                "base_url": "https://ad-vdp.s3.us-west-1.amazonaws.com/VDP-UTILS-1.1.0.22/repos/ubuntu16",
                                "components": null,
                                "default_base_url": "https://ad-vdp.s3.us-west-1.amazonaws.com/VDP-UTILS-1.1.0.22/repos/ubuntu16",
                                "distribution": null,
                                "mirrors_list": null,
                                "os_type": "ubuntu16",
                                "repo_id": "VDP-UTILS-1.1.0.22",
                                "repo_name": "VDP-UTILS",
                                "stack_name": "VDP",
                                "stack_version": "3.3",
                                "tags": [],
                                "unique": false,
                                "version_definition_id": "VDP-3.3"
                            }
                        }
                    ]
                },
                {
                    "href": "http://example.com/api/v1/version_definitions/VDP-3.3/operating_systems/ubuntu18",
                    "OperatingSystems": {
                        "os_type": "ubuntu18",
                        "stack_name": "VDP",
                        "stack_version": "3.3",
                        "version_definition_id": "VDP-3.3"
                    },
                    "repositories": [
                        {
                            "href": "http://example.com/api/v1/version_definitions/VDP-3.3/operating_systems/ubuntu18/repositories/VDP-3.3",
                            "Repositories": {
                                "applicable_services": [],
                                "base_url": "https://ad-vdp.s3.us-west-1.amazonaws.com/VDP/ubuntu18/3.x/updates/3.3.0.0",
                                "components": null,
                                "default_base_url": "https://ad-vdp.s3.us-west-1.amazonaws.com/VDP/ubuntu18/3.x/updates/3.3.0.0",
                                "distribution": null,
                                "mirrors_list": null,
                                "os_type": "ubuntu18",
                                "repo_id": "VDP-3.3",
                                "repo_name": "VDP",
                                "stack_name": "VDP",
                                "stack_version": "3.3",
                                "tags": [],
                                "unique": false,
                                "version_definition_id": "VDP-3.3"
                            }
                        },
                        {
                            "href": "http://example.com/api/v1/version_definitions/VDP-3.3/operating_systems/ubuntu18/repositories/VDP-3.3-GPL",
                            "Repositories": {
                                "applicable_services": [],
                                "base_url": "https://ad-vdp.s3.us-west-1.amazonaws.com/VDP-GPL/ubuntu18/3.x/updates/3.3.0.0",
                                "components": null,
                                "default_base_url": "https://ad-vdp.s3.us-west-1.amazonaws.com/VDP-GPL/ubuntu18/3.x/updates/3.3.0.0",
                                "distribution": null,
                                "mirrors_list": null,
                                "os_type": "ubuntu18",
                                "repo_id": "VDP-3.3-GPL",
                                "repo_name": "VDP-GPL",
                                "stack_name": "VDP",
                                "stack_version": "3.3",
                                "tags": [
                                    "GPL"
                                ],
                                "unique": false,
                                "version_definition_id": "VDP-3.3"
                            }
                        },
                        {
                            "href": "http://example.com/api/v1/version_definitions/VDP-3.3/operating_systems/ubuntu18/repositories/VDP-UTILS-1.1.0.22",
                            "Repositories": {
                                "applicable_services": [],
                                "base_url": "https://ad-vdp.s3.us-west-1.amazonaws.com/VDP-UTILS-1.1.0.22/repos/ubuntu18",
                                "components": null,
                                "default_base_url": "https://ad-vdp.s3.us-west-1.amazonaws.com/VDP-UTILS-1.1.0.22/repos/ubuntu18",
                                "distribution": null,
                                "mirrors_list": null,
                                "os_type": "ubuntu18",
                                "repo_id": "VDP-UTILS-1.1.0.22",
                                "repo_name": "VDP-UTILS",
                                "stack_name": "VDP",
                                "stack_version": "3.3",
                                "tags": [],
                                "unique": false,
                                "version_definition_id": "VDP-3.3"
                            }
                        }
                    ]
                }
            ]
        }
    ]
}


