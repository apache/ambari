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

export enum InputType {
  BOOLEAN = "boolean",
  PASSWORD = "password",
  INT = "int",
  CONTENT = "content",
  DIRECTORIES = "directories",
  DIRECTORY = "directory",
  VALUELIST = "value-list",
  COMPONENTHOST = "componentHost",
  STRING = "string",
  BUTTON = "button",
  LDAPURL = "ldap_url",
  CHECKBOX = "checkbox",
  BOOLEANINVERTED = "boolean-inverted",
  CUSTOM = "custom",
  FLOAT = "float",
  MULTILINE = "multiLine",
  COMPONENTHOSTS = "componentHosts",
  HOST = "host",
  HOSTS = "hosts",
  RADIOBUTTON = "radio button",
  USER = "user",
  DATABASE = "database",
  DB_USER = "db_user",
  SUPPORTTEXTCONNECTION = "supportTextConnection"
}

export enum TruthValues {
  YES = "Yes",
  NO = "No"
}

export type PropertyType = {
  propertyName: string;
  propertyDisplayname?: string;
  propertyDescription?: string;
  propertyValue: any;
  propertyAttributes: any;
  previousValue: string;
  propertyDisplayValue?: string;
  errorMessage?: string;
  value?: any;
  confirmPassword?: any;
  final?: string;
  fileName?: string;
  propertyType?: string[];
  hasError?: boolean;
  tabName?: string;
  type?: string;
  displayType?: string;
  isEditable: boolean;
  overrideValues?: any;
  isVisible?: boolean;
  foundInPropertyValues?: boolean; // Flag to track if property exists in propertyValues
  isHidden?:boolean
  propertyDependsOn?:any;
  propertyDependedBy?:any;
  oldValue?:any;
  didUserOverrideValue?: boolean;
  recommendedValue?:string;
  warnMessage?:string;
  serviceName?: string;
  savedFinal? : string ;
  supportsFinal?: boolean;
  isSecureConfig?: boolean;
  unit?: string; // Unit for the property value
  widget?: Record<string, any>;
};

export type ThemeType = {
  [key: string]: {
    tabs: TabType;
    subsectionProperties: SubsectionPropertiesType;
    widgets: WidgetType;
    errors?: string;
  };
};

export type TabType = {
  [key: string]: {
    name: string;
    displayName: string;
    tabColumns?: number;
    tabRows?: number;
    sections?: any;
    errors?: string;
  };
};

export type SubsectionPropertiesType = {
  [key: string]: {
    properties: string[];
  };
};

export type WidgetType = {
  [key: string]: {
    config: string;
    widget: {
      type: string;
    };
  };
};

export type ConfigPropertiesType = {
  [key: string]: {
    [key: string]: {
      errors: number;
      displayName?: string;
      properties: {
        [key: string]: {
          propertyName: string;
          propertyDisplayname: string;
          propertyDescription?: string;
          propertyValue: any;
          propertyAttributes: any;
          previousValue: any;
          propertyDisplayValue?: string;
          errorMessage?: string;
          value?: any;
          confirmPassword?: any;
          final: string;
          fileName?: string;
          propertyType?: string[];
          hasError?: boolean;
          tabName?: string;
          serviceName?: string;
          type?: string;
          isEditable: boolean;
          overrideValues?: configGroupOverrides[];
          isVisible?: boolean;
          recommendedValue?:any;
          propertyDependsOn?:any;
          propertyDependedBy?:any;
          oldValue?:any;
          foundInPropertyValues?: boolean; // Flag to track if property exists in propertyValues
          isHidden?: boolean; // Flag to track if property is hidden
          unit?: string; // Unit for the property value
          savedFinal? : string ;
          supportsFinal?: boolean;
          isSecureConfig?: boolean;
          // Optional dynamic keys
          [dynamicKey: string]: any;
        };
      };
    };
  };
};

export type configGroupOverrides = {
  value:any;
  groupName:string;
  previousValue?:any;
  errorMessage?:string;
  [dynamicKey: string]: any;
}

export type TabErrorsType = {
  [key: string]: {
    errors: string;
    [key: string]: string;
  };
};

export type ConfigTypeInfo = {
  items: string[];
  supportsFinal: string[];
  supportsAddingForbidden: string[];
}

export type StackServices = {
  service_name: string;
  stack_name: string;
  stack_version: string;
  config_types: any;
}

type StackServicesItem = {
  href: string;
  StackServices: StackServices;
  configurations: any[];
}

export interface StackServicesRoot {
  href: string;
  items: StackServicesItem[];
}
