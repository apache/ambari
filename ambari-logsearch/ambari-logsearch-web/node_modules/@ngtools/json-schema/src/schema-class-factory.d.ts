import { RootSchemaTreeNode } from './schema-tree';
import { JsonSchemaErrorBase } from './error';
import './mimetypes';
export declare class InvalidJsonPath extends JsonSchemaErrorBase {
}
/** The interface the SchemaClassFactory returned class implements. */
export interface SchemaClass<JsonType> extends Object {
    $$root(): JsonType;
    $$get(path: string): any;
    $$set(path: string, value: any): void;
    $$alias(source: string, destination: string): boolean;
    $$dispose(): void;
    $$typeOf(path: string): string;
    $$defined(path: string): boolean;
    $$delete(path: string): void;
    $$schema(): RootSchemaTreeNode;
    $$serialize(mimetype?: string, ...args: any[]): string;
}
export interface SchemaClassFactoryReturn<T> {
    new (value: T, ...fallbacks: T[]): SchemaClass<T>;
}
/**
 * Create a class from a JSON SCHEMA object. Instanciating that class with an object
 * allows for extended behaviour.
 * This is the base API to access the Configuration in the CLI.
 * @param schema
 * @returns {GeneratedSchemaClass}
 * @constructor
 */
export declare function SchemaClassFactory<T>(schema: Object): SchemaClassFactoryReturn<T>;
