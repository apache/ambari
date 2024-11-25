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
import { ControlType } from "./enums";

interface ClusterInfoItemType {
  href: string;
  Clusters: {
    cluster_id: number;
    cluster_name: string;
  };
}
 
interface ClusterInfoType {
  href: string;
  items: ClusterInfoItemType[];
}
 
interface RemoteClusterInfoItemType {
  href: string;
  ClusterInfo: {
    cluster_id: number;
    name: string;
    services: string[];
  };
}
 
interface RemoteClusterInfoType {
  href: string;
  items: RemoteClusterInfoItemType[];
}
 
interface MappingType {
  label: string;
  value: string;
  placeholder: string;
  defaultValue: string;
  isRequired: boolean;
  masked: boolean;
  error: string;
}
 
interface ConfigType {
  [key: string]: string;
}
 
interface ParametersType {
  name: string;
  description: string;
  label: string;
  placeholder: null;
  defaultValue: null;
  clusterConfig: string;
  required: boolean;
  masked: boolean;
}
 
export type ViewInputs = {
  [key: string]: {
    isEditable: boolean;
    value: string;
    label: string;
    type: string;
    originalValue: string;
    required?: boolean;
    hasError?: boolean;
    clickCallback?: () => void;
  };
};
 
export type ParameterFields = {
  name: string;
  description: string;
  label: string;
  placeholder: string | null;
  defaultValue: string | null;
  clusterConfig: string;
  required: boolean;
  masked: boolean;
};
 
export type User = {
  href: string;
  Users: {
    user_name: string;
  };
};
export type Group = {
  href: string;
  Groups: {
    group_name: string;
  };
};
export type Options = {
  value: string;
  label: string;
};
 
export type PrivilegesType = {
  href: string;
  PrivilegeInfo: {
    instance_name: string;
    permission_label: string;
    permission_name: string;
    principal_name: string;
    principal_type: string;
    privilege_id: number;
    version: string;
    view_name: string;
  };
};
 
export type setPermissionsType = {
  PrivilegeInfo: {
    permission_name: string;
    principal_name: string;
    principal_type: string;
  };
};
 
export type setDetailsType = {
  ViewInstanceInfo: {
    visible: string;
    label: string;
    description: string;
  };
};
 
interface ViewDetailsItemType {
  href: string;
  ViewInfo: {
    view_name: string;
  };
  versions: {
    href: string;
    ViewVersionInfo: {
      archive: string;
      build_number: string;
      cluster_configurable: boolean;
      description: string;
      label: string;
      masker_class: string | null;
      max_ambari_version: string | null;
      min_ambari_version: string;
      parameters: ParametersType[];
      status: string;
      status_detail: string;
      system: boolean;
      version: string;
      view_name: string;
    };
    instances: {}[];
    permissions: {}[];
  }[];
}
 
interface ViewDetailsType {
  href: string;
  items: ViewDetailsItemType[];
}
 
type ClusterType = "local" | "remote" | "custom";
 
export type {
  RemoteClusterInfoType,
  RemoteClusterInfoItemType,
  ClusterInfoType,
  MappingType,
  ClusterType,
  ViewDetailsType,
  ParametersType,
  ConfigType,
  ViewDetailsItemType,
  ClusterInfoItemType,
};

interface FieldType {
  label: string;
  type: ControlType;
  hasError: boolean;
  isEditable: boolean;
  id: string;
  value: string | boolean;
  originalValue: string;
  apiResponseKey: string[];
  required?: boolean;
  validationRegEx?: RegExp;
  errorMessage?: string;
  isdeletable?: boolean;
  deleteCallBack?: () => void;
  href?: string;
  placeholder?: string[];
   [key:string]:string|unknown;
}

interface SectionType {
  isEditable: boolean;
  isEditing: boolean;
  apiResponsible: any; 
  fields: FieldType[];
}

interface ViewSectionsType {
  details: SectionType;
   [key:string]:SectionType;
}

interface ValidationErrorType {
    [key: string]: string;
}

export type{ViewSectionsType, FieldType, SectionType, ValidationErrorType }
