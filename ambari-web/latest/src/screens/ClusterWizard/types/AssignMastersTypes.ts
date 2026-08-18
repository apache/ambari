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

export interface AssignMastersProps {
  STACK: string;
  VERSION: string;
  hostsList: any;
  services: string[];
  setCanProceed: (canProcced: boolean) => void;
  setHasValidationIssues?: (hasIssues: boolean) => void;
  dispatch: any;
  installedServices?: string[];
  parentState?:any;
}

export interface Host {
  hostname: string;
  cores: number;
  memory: number;
  components: string[];
}

export interface Masters {
  display_name: string;
  component: string;
  serviceId: string;
  host_id: number;
  hostName: string;
  isInstalled?: boolean;
  errorMessage?: string | null | undefined;
  warnMessage?: string | null | undefined;
}

export interface State {
  hosts: { [key: string]: Host };
}

export interface Action {
  type: string;
  payload: any;
}

export interface StackServiceComponent {
  cardinality: string;
  component_category: string;
  component_name: string;
  component_type: string | null;
  display_name: string;
  is_client: boolean;
  is_master: boolean;
  isMasterWithMultipleInstances?: boolean;
  reassign_allowed: boolean;
  service_name: string;
  stack_name: string;
  stack_version: string;
}

export interface ServiceComponent {
  href: string;
  StackServiceComponents: StackServiceComponent;
}

export interface Service {
  StackServices: {
    service_name: string;
  };
  components: ServiceComponent[];
}

export interface ServicesResponse {
  items: Service[];
}
