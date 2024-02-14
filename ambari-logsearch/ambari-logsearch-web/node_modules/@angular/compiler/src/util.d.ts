import * as o from './output/output_ast';
import { ParseError } from './parse_util';
export declare const MODULE_SUFFIX = "";
export declare function camelCaseToDashCase(input: string): string;
export declare function dashCaseToCamelCase(input: string): string;
export declare function splitAtColon(input: string, defaultValues: string[]): string[];
export declare function splitAtPeriod(input: string, defaultValues: string[]): string[];
export declare function visitValue(value: any, visitor: ValueVisitor, context: any): any;
export declare function isDefined(val: any): boolean;
export declare function noUndefined<T>(val: T | undefined): T;
export interface ValueVisitor {
    visitArray(arr: any[], context: any): any;
    visitStringMap(map: {
        [key: string]: any;
    }, context: any): any;
    visitPrimitive(value: any, context: any): any;
    visitOther(value: any, context: any): any;
}
export declare class ValueTransformer implements ValueVisitor {
    visitArray(arr: any[], context: any): any;
    visitStringMap(map: {
        [key: string]: any;
    }, context: any): any;
    visitPrimitive(value: any, context: any): any;
    visitOther(value: any, context: any): any;
}
export declare type SyncAsync<T> = T | Promise<T>;
export declare const SyncAsync: {
    assertSync: <T>(value: SyncAsync<T>) => T;
    then: <T, R>(value: SyncAsync<T>, cb: (value: T) => SyncAsync<R>) => SyncAsync<R>;
    all: <T>(syncAsyncValues: SyncAsync<T>[]) => SyncAsync<T[]>;
};
export declare function syntaxError(msg: string, parseErrors?: ParseError[]): Error;
export declare function isSyntaxError(error: Error): boolean;
export declare function getParseErrors(error: Error): ParseError[];
export declare function escapeRegExp(s: string): string;
export declare function utf8Encode(str: string): string;
export interface OutputContext {
    genFilePath: string;
    statements: o.Statement[];
    importExpr(reference: any, typeParams?: o.Type[] | null): o.Expression;
}
