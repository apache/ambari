import { JsonSchemaErrorBase } from './error';
import { Serializer } from './serializer';
import { SchemaNode, TypeScriptType } from './node';
export declare class InvalidSchema extends JsonSchemaErrorBase {
}
export declare class InvalidValueError extends JsonSchemaErrorBase {
}
export declare class MissingImplementationError extends JsonSchemaErrorBase {
}
export declare class SettingReadOnlyPropertyError extends JsonSchemaErrorBase {
}
export declare class InvalidUpdateValue extends JsonSchemaErrorBase {
}
export interface Schema {
    [key: string]: any;
}
/** This interface is defined to simplify the arguments passed in to the SchemaTreeNode. */
export declare type TreeNodeConstructorArgument<T> = {
    parent?: SchemaTreeNode<T>;
    name?: string;
    value: T;
    forward?: SchemaTreeNode<any>;
    schema: Schema;
};
/**
 * Holds all the information, including the value, of a node in the schema tree.
 */
export declare abstract class SchemaTreeNode<T> implements SchemaNode {
    protected _parent: SchemaTreeNode<any>;
    protected _defined: boolean;
    protected _dirty: boolean;
    protected _schema: Schema;
    protected _name: string;
    protected _value: T;
    protected _forward: SchemaTreeNode<any>;
    constructor(nodeMetaData: TreeNodeConstructorArgument<T>);
    dispose(): void;
    readonly defined: boolean;
    dirty: boolean;
    readonly value: T;
    readonly abstract type: string;
    readonly abstract tsType: TypeScriptType;
    abstract destroy(): void;
    readonly abstract defaultValue: any | null;
    readonly name: string;
    readonly readOnly: boolean;
    readonly frozen: boolean;
    readonly description: any;
    readonly required: boolean;
    isChildRequired(_name: string): boolean;
    readonly parent: SchemaTreeNode<any>;
    readonly children: {
        [key: string]: SchemaTreeNode<any>;
    } | null;
    readonly items: SchemaTreeNode<any>[] | null;
    readonly itemPrototype: SchemaTreeNode<any> | null;
    abstract get(): T;
    set(_v: T, _init?: boolean, _force?: boolean): void;
    isCompatible(_v: any): boolean;
    abstract serialize(serializer: Serializer): void;
    protected static _defineProperty<T>(proto: any, treeNode: SchemaTreeNode<T>): void;
}
/** Base Class used for Non-Leaves TreeNode. Meaning they can have children. */
export declare abstract class NonLeafSchemaTreeNode<T> extends SchemaTreeNode<T> {
    dispose(): void;
    get(): T;
    destroy(): void;
    protected _createChildProperty<T>(name: string, value: T, forward: SchemaTreeNode<T>, schema: Schema, define?: boolean): SchemaTreeNode<T>;
}
export declare class OneOfSchemaTreeNode extends NonLeafSchemaTreeNode<any> {
    protected _typesPrototype: SchemaTreeNode<any>[];
    protected _currentTypeHolder: SchemaTreeNode<any> | null;
    constructor(metaData: TreeNodeConstructorArgument<any>);
    _set(v: any, init: boolean, force: boolean): void;
    set(v: any, _init?: boolean, force?: boolean): void;
    get(): any;
    readonly defaultValue: any | null;
    readonly defined: boolean;
    readonly items: SchemaTreeNode<any>[];
    readonly type: string;
    readonly tsType: null;
    serialize(serializer: Serializer): void;
}
/** A Schema Tree Node that represents an object. */
export declare class ObjectSchemaTreeNode extends NonLeafSchemaTreeNode<{
    [key: string]: any;
}> {
    protected _children: {
        [key: string]: SchemaTreeNode<any>;
    };
    protected _frozen: boolean;
    constructor(metaData: TreeNodeConstructorArgument<any>);
    _set(value: any, init: boolean, force: boolean): void;
    set(v: any, force?: boolean): void;
    readonly frozen: boolean;
    readonly children: {
        [key: string]: SchemaTreeNode<any>;
    } | null;
    readonly type: string;
    readonly tsType: ObjectConstructor;
    readonly defaultValue: any | null;
    isCompatible(v: any): boolean;
    isChildRequired(name: string): boolean;
    serialize(serializer: Serializer): void;
}
/** A Schema Tree Node that represents an array. */
export declare class ArraySchemaTreeNode extends NonLeafSchemaTreeNode<Array<any>> {
    protected _items: SchemaTreeNode<any>[];
    protected _itemPrototype: SchemaTreeNode<any>;
    constructor(metaData: TreeNodeConstructorArgument<Array<any>>);
    _set(value: any, init: boolean, _force: boolean): void;
    set(v: any, init?: boolean, force?: boolean): void;
    isCompatible(v: any): boolean;
    readonly type: string;
    readonly tsType: ArrayConstructor;
    readonly items: SchemaTreeNode<any>[];
    readonly itemPrototype: SchemaTreeNode<any>;
    readonly defaultValue: any | null;
    serialize(serializer: Serializer): void;
}
/**
 * The root class of the tree node. Receives a prototype that will be filled with the
 * properties of the Schema root.
 */
export declare class RootSchemaTreeNode extends ObjectSchemaTreeNode {
    constructor(proto: any, metaData: TreeNodeConstructorArgument<Object>);
}
/** A leaf in the schema tree. Must contain a single primitive value. */
export declare abstract class LeafSchemaTreeNode<T> extends SchemaTreeNode<T> {
    protected _default: T;
    constructor(metaData: TreeNodeConstructorArgument<T>);
    get(): any;
    set(v: T, init?: boolean, force?: boolean): void;
    destroy(): void;
    readonly defaultValue: T;
    readonly hasDefault: boolean;
    abstract convert(v: any): T;
    abstract isCompatible(v: any): boolean;
    serialize(serializer: Serializer): void;
}
