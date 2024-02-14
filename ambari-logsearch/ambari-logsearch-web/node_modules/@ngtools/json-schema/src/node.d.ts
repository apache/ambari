import { Serializer } from './serializer';
export declare type TypeScriptType = typeof Number | typeof Boolean | typeof String | typeof Object | typeof Array | null;
export interface SchemaNode {
    readonly name: string;
    readonly type: string;
    readonly tsType: TypeScriptType;
    readonly defined: boolean;
    readonly dirty: boolean;
    readonly frozen: boolean;
    readonly readOnly: boolean;
    readonly defaultValue: any | null;
    readonly required: boolean;
    readonly parent: SchemaNode | null;
    readonly description: string | null;
    readonly children: {
        [key: string]: SchemaNode;
    } | null;
    readonly items: SchemaNode[] | null;
    readonly itemPrototype: SchemaNode | null;
    value: any;
    serialize(serializer: Serializer): void;
}
