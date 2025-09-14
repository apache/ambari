export interface CredentialConfigType {
    final: string;
    property_name: string;
    property_type: string[];
    property_value: string;
    service_name: string;
    stack_name: string;
    stack_version: string;
    type: string;
    passwordProperty?: Object;
    usernameProperty?: Object;
    [key: string]: any;
  }