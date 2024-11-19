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
export interface VersionDefinitionResponse {
    href: string
    items: Item[]
  }
  
  export interface Item {
    href: string
    VersionDefinition: VersionDefinition
    operating_systems: OperatingSystem[]
  }
  
  export interface VersionDefinition {
    id: string
    repository_version: string
    show_available: boolean
    stack_default: boolean
    stack_name: string
    stack_repo_update_link_exists: boolean
    stack_services: StackService[]
    stack_version: string
    type: string
  }
  
  export interface StackService {
    name: string
    display_name: string
    comment: string
    versions: string[]
  }
  
  export interface OperatingSystem {
    href: string
    OperatingSystems: OperatingSystems
    repositories: Repository[]
  }
  
  export interface OperatingSystems {
    os_type: string
    stack_name: string
    stack_version: string
    version_definition_id: string
  }
  
  export interface Repository {
    href: string
    Repositories: Repositories
  }
  
  export interface Repositories {
    applicable_services: any[]
    base_url: string
    components: any
    default_base_url: string
    distribution: any
    mirrors_list: any
    os_type: string
    repo_id: string
    repo_name: string
    stack_name: string
    stack_version: string
    tags: string[]
    unique: boolean
    version_definition_id: string
  }
  