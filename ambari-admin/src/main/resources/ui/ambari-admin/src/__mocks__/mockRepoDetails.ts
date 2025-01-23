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
    "href": "http://example.com/api/v1/stacks/VDP/versions/3.3?fields=operating_systems/repositories/Repositories",
    "Versions": {
        "stack_name": "VDP",
        "stack_version": "3.3"
    },
    "operating_systems": [
        {
            "href": "http://example.com/api/v1/stacks/VDP/versions/3.3/operating_systems/amazonlinux2",
            "OperatingSystems": {
                "os_type": "amazonlinux2",
                "stack_name": "VDP",
                "stack_version": "3.3"
            },
            "repositories": [
                {
                    "href": "http://example.com/api/v1/stacks/VDP/versions/3.3/operating_systems/amazonlinux2/repositories/VDP-3.3",
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
                        "unique": false
                    }
                },
                {
                    "href": "http://example.com/api/v1/stacks/VDP/versions/3.3/operating_systems/amazonlinux2/repositories/VDP-3.3-GPL",
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
                        "unique": false
                    }
                },
                {
                    "href": "http://example.com/api/v1/stacks/VDP/versions/3.3/operating_systems/amazonlinux2/repositories/VDP-UTILS-1.1.0.22",
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
                        "unique": false
                    }
                }
            ]
        },
        {
            "href": "http://example.com/api/v1/stacks/VDP/versions/3.3/operating_systems/debian9",
            "OperatingSystems": {
                "os_type": "debian9",
                "stack_name": "VDP",
                "stack_version": "3.3"
            },
            "repositories": [
                {
                    "href": "http://example.com/api/v1/stacks/VDP/versions/3.3/operating_systems/debian9/repositories/VDP-3.3",
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
                        "unique": false
                    }
                },
                {
                    "href": "http://example.com/api/v1/stacks/VDP/versions/3.3/operating_systems/debian9/repositories/VDP-3.3-GPL",
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
                        "unique": false
                    }
                },
                {
                    "href": "http://example.com/api/v1/stacks/VDP/versions/3.3/operating_systems/debian9/repositories/VDP-UTILS-1.1.0.22",
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
                        "unique": false
                    }
                }
            ]
        },
        {
            "href": "http://example.com/api/v1/stacks/VDP/versions/3.3/operating_systems/redhat-ppc7",
            "OperatingSystems": {
                "os_type": "redhat-ppc7",
                "stack_name": "VDP",
                "stack_version": "3.3"
            },
            "repositories": [
                {
                    "href": "http://example.com/api/v1/stacks/VDP/versions/3.3/operating_systems/redhat-ppc7/repositories/VDP-3.3",
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
                        "unique": false
                    }
                },
                {
                    "href": "http://example.com/api/v1/stacks/VDP/versions/3.3/operating_systems/redhat-ppc7/repositories/VDP-3.3-GPL",
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
                        "unique": false
                    }
                },
                {
                    "href": "http://example.com/api/v1/stacks/VDP/versions/3.3/operating_systems/redhat-ppc7/repositories/VDP-UTILS-1.1.0.22",
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
                        "unique": false
                    }
                }
            ]
        },
        {
            "href": "http://example.com/api/v1/stacks/VDP/versions/3.3/operating_systems/redhat7",
            "OperatingSystems": {
                "os_type": "redhat7",
                "stack_name": "VDP",
                "stack_version": "3.3"
            },
            "repositories": [
                {
                    "href": "http://example.com/api/v1/stacks/VDP/versions/3.3/operating_systems/redhat7/repositories/VDP-3.3",
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
                        "unique": false
                    }
                },
                {
                    "href": "http://example.com/api/v1/stacks/VDP/versions/3.3/operating_systems/redhat7/repositories/VDP-3.3-GPL",
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
                        "unique": false
                    }
                },
                {
                    "href": "http://example.com/api/v1/stacks/VDP/versions/3.3/operating_systems/redhat7/repositories/VDP-UTILS-1.1.0.22",
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
                        "unique": false
                    }
                }
            ]
        },
        {
            "href": "http://example.com/api/v1/stacks/VDP/versions/3.3/operating_systems/redhat8",
            "OperatingSystems": {
                "os_type": "redhat8",
                "stack_name": "VDP",
                "stack_version": "3.3"
            },
            "repositories": [
                {
                    "href": "http://example.com/api/v1/stacks/VDP/versions/3.3/operating_systems/redhat8/repositories/VDP-3.3",
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
                        "unique": false
                    }
                },
                {
                    "href": "http://example.com/api/v1/stacks/VDP/versions/3.3/operating_systems/redhat8/repositories/VDP-3.3-GPL",
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
                        "unique": false
                    }
                },
                {
                    "href": "http://example.com/api/v1/stacks/VDP/versions/3.3/operating_systems/redhat8/repositories/VDP-UTILS-1.1.0.22",
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
                        "unique": false
                    }
                }
            ]
        },
        {
            "href": "http://example.com/api/v1/stacks/VDP/versions/3.3/operating_systems/suse12",
            "OperatingSystems": {
                "os_type": "suse12",
                "stack_name": "VDP",
                "stack_version": "3.3"
            },
            "repositories": [
                {
                    "href": "http://example.com/api/v1/stacks/VDP/versions/3.3/operating_systems/suse12/repositories/VDP-3.3",
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
                        "unique": false
                    }
                },
                {
                    "href": "http://example.com/api/v1/stacks/VDP/versions/3.3/operating_systems/suse12/repositories/VDP-3.3-GPL",
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
                        "unique": false
                    }
                },
                {
                    "href": "http://example.com/api/v1/stacks/VDP/versions/3.3/operating_systems/suse12/repositories/VDP-UTILS-1.1.0.22",
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
                        "unique": false
                    }
                }
            ]
        },
        {
            "href": "http://example.com/api/v1/stacks/VDP/versions/3.3/operating_systems/ubuntu14",
            "OperatingSystems": {
                "os_type": "ubuntu14",
                "stack_name": "VDP",
                "stack_version": "3.3"
            },
            "repositories": [
                {
                    "href": "http://example.com/api/v1/stacks/VDP/versions/3.3/operating_systems/ubuntu14/repositories/VDP-3.3",
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
                        "unique": false
                    }
                },
                {
                    "href": "http://example.com/api/v1/stacks/VDP/versions/3.3/operating_systems/ubuntu14/repositories/VDP-3.3-GPL",
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
                        "unique": false
                    }
                },
                {
                    "href": "http://example.com/api/v1/stacks/VDP/versions/3.3/operating_systems/ubuntu14/repositories/VDP-UTILS-1.1.0.22",
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
                        "unique": false
                    }
                }
            ]
        },
        {
            "href": "http://example.com/api/v1/stacks/VDP/versions/3.3/operating_systems/ubuntu16",
            "OperatingSystems": {
                "os_type": "ubuntu16",
                "stack_name": "VDP",
                "stack_version": "3.3"
            },
            "repositories": [
                {
                    "href": "http://example.com/api/v1/stacks/VDP/versions/3.3/operating_systems/ubuntu16/repositories/VDP-3.3",
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
                        "unique": false
                    }
                },
                {
                    "href": "http://example.com/api/v1/stacks/VDP/versions/3.3/operating_systems/ubuntu16/repositories/VDP-3.3-GPL",
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
                        "unique": false
                    }
                },
                {
                    "href": "http://example.com/api/v1/stacks/VDP/versions/3.3/operating_systems/ubuntu16/repositories/VDP-UTILS-1.1.0.22",
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
                        "unique": false
                    }
                }
            ]
        },
        {
            "href": "http://example.com/api/v1/stacks/VDP/versions/3.3/operating_systems/ubuntu18",
            "OperatingSystems": {
                "os_type": "ubuntu18",
                "stack_name": "VDP",
                "stack_version": "3.3"
            },
            "repositories": [
                {
                    "href": "http://example.com/api/v1/stacks/VDP/versions/3.3/operating_systems/ubuntu18/repositories/VDP-3.3",
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
                        "unique": false
                    }
                },
                {
                    "href": "http://example.com/api/v1/stacks/VDP/versions/3.3/operating_systems/ubuntu18/repositories/VDP-3.3-GPL",
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
                        "unique": false
                    }
                },
                {
                    "href": "http://example.com/api/v1/stacks/VDP/versions/3.3/operating_systems/ubuntu18/repositories/VDP-UTILS-1.1.0.22",
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
                        "unique": false
                    }
                }
            ]
        }
    ]
}