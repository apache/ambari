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
    href: "http://example.com",
    items: [
      {
        href: "http://example.com/repo1",
        Stacks: { stack_name: "Stack1" },
        versions: [
          {
            href: "http://example.com/repo1/version1",
            Versions: { stack_name: "Stack1", stack_version: "1.0.0" },
            repository_versions: [
              {
                href: "http://example.com/repo1/version1/repoVersion1",
                RepositoryVersions: {
                  display_name: "RepoVersion1",
                  has_children: false,
                  hidden: false,
                  id: 1,
                  parent_id: null,
                  repository_version: "1.0.0",
                  resolved: true,
                  services: [
                    {
                      name: "Service1",
                      versions: [{ version: "1.0.0", components: [] }],
                      display_name: "Service1",
                    },
                  ],
                  stack_name: "Stack1",
                  stack_services: [
                    {
                      name: "Service1",
                      display_name: "Service1",
                      comment: "Comment1",
                      versions: ["1.0.0"],
                    },
                  ],
                  stack_version: "1.0.0",
                  type: "type1",
                  release: {
                    build: null,
                    compatible_with: null,
                    notes: "Notes1",
                    version: "1.0.0",
                  },
                },
              },
            ],
          },
        ],
      },
      {
        href: "http://example.com/repo2",
        Stacks: { stack_name: "Stack2" },
        versions: [
          {
            href: "http://example.com/repo2/version1",
            Versions: { stack_name: "Stack2", stack_version: "1.0.0" },
            repository_versions: [
              {
                href: "http://example.com/repo2/version1/repoVersion1",
                RepositoryVersions: {
                  display_name: "RepoVersion1",
                  has_children: false,
                  hidden: false,
                  id: 1,
                  parent_id: null,
                  repository_version: "1.0.0",
                  resolved: true,
                  services: [
                    {
                      name: "Service 1",
                      versions: [{ version: "1.0.0", components: [] }],
                      display_name: "Service1",
                    },
                  ],
                  stack_name: "Stack2",
                  stack_services: [
                    {
                      name: "Service1",
                      display_name: "Service1",
                      comment: "Comment1",
                      versions: ["1.0.0"],
                    },
                  ],
                  stack_version: "1.0.0",
                  type: "type1",
                  release: {
                    build: null,
                    compatible_with: null,
                    notes: "Notes1",
                    version: "1.0.0",
                  },
                },
              },
            ],
          },
        ],
      },
      {
        href: "http://example.com/repo3",
        Stacks: { stack_name: "Stack3" },
        versions: [
          {
            href: "http://example.com/repo3/version1",
            Versions: { stack_name: "Stack3", stack_version: "1.0.0" },
            repository_versions: [
              {
                href: "http://example.com/repo3/version1/repoVersion1",
                RepositoryVersions: {
                  display_name: "RepoVersion1",
                  has_children: false,
                  hidden: false,
                  id: 1,
                  parent_id: null,
                  repository_version: "1.0.0",
                  resolved: true,
                  services: [
                    {
                      name: "Service1",
                      versions: [{ version: "1.0.0", components: [] }],
                      display_name: "Service1",
                    },
                  ],
                  stack_name: "Stack3",
                  stack_services: [
                    {
                      name: "Service1",
                      display_name: "Service1",
                      comment: "Comment1",
                      versions: ["1.0.0"],
                    },
                  ],
                  stack_version: "1.0.0",
                  type: "type1",
                  release: {
                    build: null,
                    compatible_with: null,
                    notes: "Notes1",
                    version: "1.0.0",
                  },
                },
              },
            ],
          },
        ],
      },
      {
        href: "http://example.com/repo4",
        Stacks: { stack_name: "Stack4" },
        versions: [
          {
            href: "http://example.com/repo4/version1",
            Versions: { stack_name: "Stack4", stack_version: "1.0.0" },
            repository_versions: [
              {
                href: "http://example.com/repo4/version1/repoVersion1",
                RepositoryVersions: {
                  display_name: "RepoVersion1",
                  has_children: false,
                  hidden: false,
                  id: 1,
                  parent_id: null,
                  repository_version: "1.0.0",
                  resolved: true,
                  services: [
                    {
                      name: "Service1",
                      versions: [{ version: "1.0.0", components: [] }],
                      display_name: "Service1",
                    },
                  ],
                  stack_name: "Stack 4",
                  stack_services: [
                    {
                      name: "Service1",
                      display_name: "Service1",
                      comment: "Comment1",
                      versions: ["1.0.0"],
                    },
                  ],
                  stack_version: "1.0.0",
                  type: "type1",
                  release: {
                    build: null,
                    compatible_with: null,
                    notes: "Notes1",
                    version: "1.0.0",
                  },
                },
              },
            ],
          },
        ],
      },
      {
        href: "http://example.com/repo5",
        Stacks: { stack_name: "Stack5" },
        versions: [
          {
            href: "http://example.com/repo5/version1",
            Versions: { stack_name: "Stack5", stack_version: "1.0.0" },
            repository_versions: [
              {
                href: "http://example.com/repo5/version1/repoVersion1",
                RepositoryVersions: {
                  display_name: "RepoVersion1",
                  has_children: false,
                  hidden: false,
                  id: 1,
                  parent_id: null,
                  repository_version: "1.0.0",
                  resolved: true,
                  services: [
                    {
                      name: "Service1",
                      versions: [{ version: "1.0.0", components: [] }],
                      display_name: "Service1",
                    },
                  ],
                  stack_name: "Stack5",
                  stack_services: [
                    {
                      name: "Service1",
                      display_name: "Service1",
                      comment: "Comment1",
                      versions: ["1.0.0"],
                    },
                  ],
                  stack_version: "1.0.0",
                  type: "type1",
                  release: {
                    build: null,
                    compatible_with: null,
                    notes: "Notes1",
                    version: "1.0.0",
                  },
                },
              },
            ],
          },
        ],
      },
      {
        href: "http://example.com/repo6",
        Stacks: { stack_name: "Stack6" },
        versions: [
          {
            href: "http://example.com/repo6/version1",
            Versions: { stack_name: "Stack6", stack_version: "1.0.0" },
            repository_versions: [
              {
                href: "http://example.com/repo6/version1/repoVersion1",
                RepositoryVersions: {
                  display_name: "RepoVersion1",
                  has_children: false,
                  hidden: false,
                  id: 1,
                  parent_id: null,
                  repository_version: "1.0.0",
                  resolved: true,
                  services: [
                    {
                      name: "Service1",
                      versions: [{ version: "1.0.0", components: [] }],
                      display_name: "Service1",
                    },
                  ],
                  stack_name: "Stack 6",
                  stack_services: [
                    {
                      name: "Service1",
                      display_name: "Service1",
                      comment: "Comment1",
                      versions: ["1.0.0"],
                    },
                  ],
                  stack_version: "1.0.0",
                  type: "type1",
                  release: {
                    build: null,
                    compatible_with: null,
                    notes: "Notes1",
                    version: "1.0.0",
                  },
                },
              },
            ],
          },
        ],
      },
    ],
  };
  
  export const paginatedRepos = {
    href: "http://example.com",
    items: [
      {
        href: "http://example.com/repo1",
        Stacks: { stack_name: "Stack1" },
        versions: [
          {
            href: "http://example.com/repo1/version1",
            Versions: { stack_name: "Stack1", stack_version: "1.0.0" },
            repository_versions: [
              {
                href: "http://example.com/repo1/version1/repoVersion1",
                RepositoryVersions: {
                  display_name: "RepoVersion1",
                  has_children: false,
                  hidden: false,
                  id: 1,
                  parent_id: null,
                  repository_version: "1.0.0",
                  resolved: true,
                  services: [
                    {
                      name: "Service1",
                      versions: [{ version: "1.0.0", components: [] }],
                      display_name: "Service1",
                    },
                  ],
                  stack_name: "Stack1",
                  stack_services: [
                    {
                      name: "Service1",
                      display_name: "Service1",
                      comment: "Comment1",
                      versions: ["1.0.0"],
                    },
                  ],
                  stack_version: "1.0.0",
                  type: "type1",
                  release: {
                    build: null,
                    compatible_with: null,
                    notes: "Notes1",
                    version: "1.0.0",
                  },
                },
              },
            ],
          },
        ],
      },
      {
        href: "http://example.com/repo2",
        Stacks: { stack_name: "Stack2" },
        versions: [
          {
            href: "http://example.com/repo2/version1",
            Versions: { stack_name: "Stack2", stack_version: "1.0.0" },
            repository_versions: [
              {
                href: "http://example.com/repo2/version1/repoVersion1",
                RepositoryVersions: {
                  display_name: "RepoVersion1",
                  has_children: false,
                  hidden: false,
                  id: 1,
                  parent_id: null,
                  repository_version: "1.0.0",
                  resolved: true,
                  services: [
                    {
                      name: "Service 1",
                      versions: [{ version: "1.0.0", components: [] }],
                      display_name: "Service1",
                    },
                  ],
                  stack_name: "Stack2",
                  stack_services: [
                    {
                      name: "Service1",
                      display_name: "Service1",
                      comment: "Comment1",
                      versions: ["1.0.0"],
                    },
                  ],
                  stack_version: "1.0.0",
                  type: "type1",
                  release: {
                    build: null,
                    compatible_with: null,
                    notes: "Notes1",
                    version: "1.0.0",
                  },
                },
              },
            ],
          },
        ],
      },
      {
        href: "http://example.com/repo3",
        Stacks: { stack_name: "Stack3" },
        versions: [
          {
            href: "http://example.com/repo3/version1",
            Versions: { stack_name: "Stack3", stack_version: "1.0.0" },
            repository_versions: [
              {
                href: "http://example.com/repo3/version1/repoVersion1",
                RepositoryVersions: {
                  display_name: "RepoVersion1",
                  has_children: false,
                  hidden: false,
                  id: 1,
                  parent_id: null,
                  repository_version: "1.0.0",
                  resolved: true,
                  services: [
                    {
                      name: "Service1",
                      versions: [{ version: "1.0.0", components: [] }],
                      display_name: "Service1",
                    },
                  ],
                  stack_name: "Stack3",
                  stack_services: [
                    {
                      name: "Service1",
                      display_name: "Service1",
                      comment: "Comment1",
                      versions: ["1.0.0"],
                    },
                  ],
                  stack_version: "1.0.0",
                  type: "type1",
                  release: {
                    build: null,
                    compatible_with: null,
                    notes: "Notes1",
                    version: "1.0.0",
                  },
                },
              },
            ],
          },
        ],
      },
      {
        href: "http://example.com/repo4",
        Stacks: { stack_name: "Stack4" },
        versions: [
          {
            href: "http://example.com/repo4/version1",
            Versions: { stack_name: "Stack4", stack_version: "1.0.0" },
            repository_versions: [
              {
                href: "http://example.com/repo4/version1/repoVersion1",
                RepositoryVersions: {
                  display_name: "RepoVersion1",
                  has_children: false,
                  hidden: false,
                  id: 1,
                  parent_id: null,
                  repository_version: "1.0.0",
                  resolved: true,
                  services: [
                    {
                      name: "Service1",
                      versions: [{ version: "1.0.0", components: [] }],
                      display_name: "Service1",
                    },
                  ],
                  stack_name: "Stack 4",
                  stack_services: [
                    {
                      name: "Service1",
                      display_name: "Service1",
                      comment: "Comment1",
                      versions: ["1.0.0"],
                    },
                  ],
                  stack_version: "1.0.0",
                  type: "type1",
                  release: {
                    build: null,
                    compatible_with: null,
                    notes: "Notes1",
                    version: "1.0.0",
                  },
                },
              },
            ],
          },
        ],
      },
      {
        href: "http://example.com/repo5",
        Stacks: { stack_name: "Stack5" },
        versions: [
          {
            href: "http://example.com/repo5/version1",
            Versions: { stack_name: "Stack5", stack_version: "1.0.0" },
            repository_versions: [
              {
                href: "http://example.com/repo5/version1/repoVersion1",
                RepositoryVersions: {
                  display_name: "RepoVersion1",
                  has_children: false,
                  hidden: false,
                  id: 1,
                  parent_id: null,
                  repository_version: "1.0.0",
                  resolved: true,
                  services: [
                    {
                      name: "Service1",
                      versions: [{ version: "1.0.0", components: [] }],
                      display_name: "Service1",
                    },
                  ],
                  stack_name: "Stack5",
                  stack_services: [
                    {
                      name: "Service1",
                      display_name: "Service1",
                      comment: "Comment1",
                      versions: ["1.0.0"],
                    },
                  ],
                  stack_version: "1.0.0",
                  type: "type1",
                  release: {
                    build: null,
                    compatible_with: null,
                    notes: "Notes1",
                    version: "1.0.0",
                  },
                },
              },
            ],
          },
        ],
      },
      {
        href: "http://example.com/repo6",
        Stacks: { stack_name: "Stack6" },
        versions: [
          {
            href: "http://example.com/repo6/version1",
            Versions: { stack_name: "Stack6", stack_version: "1.0.0" },
            repository_versions: [
              {
                href: "http://example.com/repo6/version1/repoVersion1",
                RepositoryVersions: {
                  display_name: "RepoVersion1",
                  has_children: false,
                  hidden: false,
                  id: 1,
                  parent_id: null,
                  repository_version: "1.0.0",
                  resolved: true,
                  services: [
                    {
                      name: "Service1",
                      versions: [{ version: "1.0.0", components: [] }],
                      display_name: "Service1",
                    },
                  ],
                  stack_name: "Stack 6",
                  stack_services: [
                    {
                      name: "Service1",
                      display_name: "Service1",
                      comment: "Comment1",
                      versions: ["1.0.0"],
                    },
                  ],
                  stack_version: "1.0.0",
                  type: "type1",
                  release: {
                    build: null,
                    compatible_with: null,
                    notes: "Notes1",
                    version: "1.0.0",
                  },
                },
              },
            ],
          },
        ],
      },
      {
        href: "http://example.com/repo7",
        Stacks: { stack_name: "Stack7" },
        versions: [
          {
            href: "http://example.com/repo7/version1",
            Versions: { stack_name: "Stack7", stack_version: "1.0.0" },
            repository_versions: [
              {
                href: "http://example.com/repo7/version1/repoVersion1",
                RepositoryVersions: {
                  display_name: "RepoVersion1",
                  has_children: false,
                  hidden: false,
                  id: 1,
                  parent_id: null,
                  repository_version: "1.0.0",
                  resolved: true,
                  services: [
                    {
                      name: "Service1",
                      versions: [{ version: "1.0.0", components: [] }],
                      display_name: "Service1",
                    },
                  ],
                  stack_name: "Stack7",
                  stack_services: [
                    {
                      name: "Service1",
                      display_name: "Service1",
                      comment: "Comment1",
                      versions: ["1.0.0"],
                    },
                  ],
                  stack_version: "1.0.0",
                  type: "type1",
                  release: {
                    build: null,
                    compatible_with: null,
                    notes: "Notes1",
                    version: "1.0.0",
                  },
                },
              },
            ],
          },
        ],
      },
      {
        href: "http://example.com/repo8",
        Stacks: { stack_name: "Stack8" },
        versions: [
          {
            href: "http://example.com/repo8/version1",
            Versions: { stack_name: "Stack8", stack_version: "1.0.0" },
            repository_versions: [
              {
                href: "http://example.com/repo8/version1/repoVersion1",
                RepositoryVersions: {
                  display_name: "RepoVersion1",
                  has_children: false,
                  hidden: false,
                  id: 1,
                  parent_id: null,
                  repository_version: "1.0.0",
                  resolved: true,
                  services: [
                    {
                      name: "Service1",
                      versions: [{ version: "1.0.0", components: [] }],
                      display_name: "Service1",
                    },
                  ],
                  stack_name: "Stack 8",
                  stack_services: [
                    {
                      name: "Service1",
                      display_name: "Service1",
                      comment: "Comment1",
                      versions: ["1.0.0"],
                    },
                  ],
                  stack_version: "1.0.0",
                  type: "type1",
                  release: {
                    build: null,
                    compatible_with: null,
                    notes: "Notes1",
                    version: "1.0.0",
                  },
                },
              },
            ],
          },
        ],
      },
      {
        href: "http://example.com/repo9",
        Stacks: { stack_name: "Stack9" },
        versions: [
          {
            href: "http://example.com/repo9/version1",
            Versions: { stack_name: "Stack9", stack_version: "1.0.0" },
            repository_versions: [
              {
                href: "http://example.com/repo9/version1/repoVersion1",
                RepositoryVersions: {
                  display_name: "RepoVersion1",
                  has_children: false,
                  hidden: false,
                  id: 1,
                  parent_id: null,
                  repository_version: "1.0.0",
                  resolved: true,
                  services: [
                    {
                      name: "Service1",
                      versions: [{ version: "1.0.0", components: [] }],
                      display_name: "Service1",
                    },
                  ],
                  stack_name: "Stack9",
                  stack_services: [
                    {
                      name: "Service1",
                      display_name: "Service1",
                      comment: "Comment1",
                      versions: ["1.0.0"],
                    },
                  ],
                  stack_version: "1.0.0",
                  type: "type1",
                  release: {
                    build: null,
                    compatible_with: null,
                    notes: "Notes1",
                    version: "1.0.0",
                  },
                },
              },
            ],
          },
        ],
      },
      {
        href: "http://example.com/repo10",
        Stacks: { stack_name: "Stack10" },
        versions: [
          {
            href: "http://example.com/repo10/version1",
            Versions: { stack_name: "Stack10", stack_version: "1.0.0" },
            repository_versions: [
              {
                href: "http://example.com/repo10/version1/repoVersion1",
                RepositoryVersions: {
                  display_name: "RepoVersion1",
                  has_children: false,
                  hidden: false,
                  id: 1,
                  parent_id: null,
                  repository_version: "1.0.0",
                  resolved: true,
                  services: [
                    {
                      name: "Service1",
                      versions: [{ version: "1.0.0", components: [] }],
                      display_name: "Service1",
                    },
                  ],
                  stack_name: "Stack 10",
                  stack_services: [
                    {
                      name: "Service1",
                      display_name: "Service1",
                      comment: "Comment1",
                      versions: ["1.0.0"],
                    },
                  ],
                  stack_version: "1.0.0",
                  type: "type1",
                  release: {
                    build: null,
                    compatible_with: null,
                    notes: "Notes1",
                    version: "1.0.0",
                  },
                },
              },
            ],
          },
        ],
      },
      {
        href: "http://example.com/repo10",
        Stacks: { stack_name: "Stack10" },
        versions: [
          {
            href: "http://example.com/repo10/version1",
            Versions: { stack_name: "Stack10", stack_version: "1.0.0" },
            repository_versions: [
              {
                href: "http://example.com/repo10/version1/repoVersion1",
                RepositoryVersions: {
                  display_name: "RepoVersion1",
                  has_children: false,
                  hidden: false,
                  id: 1,
                  parent_id: null,
                  repository_version: "1.0.0",
                  resolved: true,
                  services: [
                    {
                      name: "Service1",
                      versions: [{ version: "1.0.0", components: [] }],
                      display_name: "Service1",
                    },
                  ],
                  stack_name: "Stack 10",
                  stack_services: [
                    {
                      name: "Service1",
                      display_name: "Service1",
                      comment: "Comment1",
                      versions: ["1.0.0"],
                    },
                  ],
                  stack_version: "1.0.0",
                  type: "type1",
                  release: {
                    build: null,
                    compatible_with: null,
                    notes: "Notes1",
                    version: "1.0.0",
                  },
                },
              },
            ],
          },
        ],
      },
    ],
  };
  