"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const error_1 = require("./error");
class InvalidSchema extends error_1.JsonSchemaErrorBase {
}
exports.InvalidSchema = InvalidSchema;
class InvalidValueError extends error_1.JsonSchemaErrorBase {
}
exports.InvalidValueError = InvalidValueError;
class MissingImplementationError extends error_1.JsonSchemaErrorBase {
}
exports.MissingImplementationError = MissingImplementationError;
class SettingReadOnlyPropertyError extends error_1.JsonSchemaErrorBase {
}
exports.SettingReadOnlyPropertyError = SettingReadOnlyPropertyError;
class InvalidUpdateValue extends error_1.JsonSchemaErrorBase {
}
exports.InvalidUpdateValue = InvalidUpdateValue;
/**
 * Holds all the information, including the value, of a node in the schema tree.
 */
class SchemaTreeNode {
    constructor(nodeMetaData) {
        this._defined = false;
        this._dirty = false;
        this._schema = nodeMetaData.schema;
        this._name = nodeMetaData.name;
        this._value = nodeMetaData.value;
        this._forward = nodeMetaData.forward;
        this._parent = nodeMetaData.parent;
    }
    dispose() {
        this._parent = null;
        this._schema = null;
        this._value = null;
        if (this._forward) {
            this._forward.dispose();
        }
        this._forward = null;
    }
    get defined() { return this._defined; }
    get dirty() { return this._dirty; }
    set dirty(v) {
        if (v) {
            this._defined = true;
            this._dirty = true;
            if (this._parent) {
                this._parent.dirty = true;
            }
        }
    }
    get value() { return this.get(); }
    get name() { return this._name; }
    get readOnly() { return this._schema['readOnly']; }
    get frozen() { return true; }
    get description() {
        return 'description' in this._schema ? this._schema['description'] : null;
    }
    get required() {
        if (!this._parent) {
            return false;
        }
        return this._parent.isChildRequired(this.name);
    }
    isChildRequired(_name) { return false; }
    get parent() { return this._parent; }
    get children() { return null; }
    get items() { return null; }
    get itemPrototype() { return null; }
    set(_v, _init = false, _force = false) {
        if (!this.readOnly) {
            throw new MissingImplementationError();
        }
        throw new SettingReadOnlyPropertyError();
    }
    isCompatible(_v) { return false; }
    static _defineProperty(proto, treeNode) {
        if (treeNode.readOnly) {
            Object.defineProperty(proto, treeNode.name, {
                enumerable: true,
                get: () => treeNode.get()
            });
        }
        else {
            Object.defineProperty(proto, treeNode.name, {
                enumerable: true,
                get: () => treeNode.get(),
                set: (v) => treeNode.set(v)
            });
        }
    }
}
exports.SchemaTreeNode = SchemaTreeNode;
/** Base Class used for Non-Leaves TreeNode. Meaning they can have children. */
class NonLeafSchemaTreeNode extends SchemaTreeNode {
    dispose() {
        for (const key of Object.keys(this.children || {})) {
            this.children[key].dispose();
        }
        for (let item of this.items || []) {
            item.dispose();
        }
        super.dispose();
    }
    get() {
        if (this.defined) {
            return this._value;
        }
        else {
            return undefined;
        }
    }
    destroy() {
        this._defined = false;
        this._value = null;
    }
    // Helper function to create a child based on its schema.
    _createChildProperty(name, value, forward, schema, define = true) {
        const type = ('oneOf' in schema) ? 'oneOf' :
            ('enum' in schema) ? 'enum' : schema['type'];
        let Klass = null;
        switch (type) {
            case 'object':
                Klass = ObjectSchemaTreeNode;
                break;
            case 'array':
                Klass = ArraySchemaTreeNode;
                break;
            case 'string':
                Klass = StringSchemaTreeNode;
                break;
            case 'boolean':
                Klass = BooleanSchemaTreeNode;
                break;
            case 'number':
                Klass = NumberSchemaTreeNode;
                break;
            case 'integer':
                Klass = IntegerSchemaTreeNode;
                break;
            case 'enum':
                Klass = EnumSchemaTreeNode;
                break;
            case 'oneOf':
                Klass = OneOfSchemaTreeNode;
                break;
            default:
                throw new InvalidSchema('Type ' + type + ' not understood by SchemaClassFactory.');
        }
        const metaData = new Klass({ parent: this, forward, value, schema, name });
        if (define) {
            SchemaTreeNode._defineProperty(this._value, metaData);
        }
        return metaData;
    }
}
exports.NonLeafSchemaTreeNode = NonLeafSchemaTreeNode;
class OneOfSchemaTreeNode extends NonLeafSchemaTreeNode {
    constructor(metaData) {
        super(metaData);
        let { value, forward, schema } = metaData;
        this._typesPrototype = schema['oneOf'].map((schema) => {
            return this._createChildProperty('', '', forward, schema, false);
        });
        this._currentTypeHolder = null;
        this._set(value, true, false);
    }
    _set(v, init, force) {
        if (!init && this.readOnly && !force) {
            throw new SettingReadOnlyPropertyError();
        }
        // Find the first type prototype that is compatible with the
        let proto = null;
        for (let i = 0; i < this._typesPrototype.length; i++) {
            const p = this._typesPrototype[i];
            if (p.isCompatible(v)) {
                proto = p;
                break;
            }
        }
        if (proto == null) {
            return;
        }
        if (!init) {
            this.dirty = true;
        }
        this._currentTypeHolder = proto;
        this._currentTypeHolder.set(v, false, true);
    }
    set(v, _init = false, force = false) {
        return this._set(v, false, force);
    }
    get() {
        return this._currentTypeHolder ? this._currentTypeHolder.get() : null;
    }
    get defaultValue() {
        return null;
    }
    get defined() { return this._currentTypeHolder ? this._currentTypeHolder.defined : false; }
    get items() { return this._typesPrototype; }
    get type() { return 'oneOf'; }
    get tsType() { return null; }
    serialize(serializer) { serializer.outputOneOf(this); }
}
exports.OneOfSchemaTreeNode = OneOfSchemaTreeNode;
/** A Schema Tree Node that represents an object. */
class ObjectSchemaTreeNode extends NonLeafSchemaTreeNode {
    constructor(metaData) {
        super(metaData);
        this._frozen = false;
        this._set(metaData.value, true, false);
    }
    _set(value, init, force) {
        if (!init && this.readOnly && !force) {
            throw new SettingReadOnlyPropertyError();
        }
        const schema = this._schema;
        const forward = this._forward;
        this._defined = !!value;
        this._children = Object.create(null);
        this._value = Object.create(null);
        this._dirty = this._dirty || !init;
        if (schema['properties']) {
            for (const name of Object.keys(schema['properties'])) {
                const propertySchema = schema['properties'][name];
                this._children[name] = this._createChildProperty(name, value ? value[name] : undefined, forward ? forward.children[name] : null, propertySchema);
            }
        }
        else if (!schema['additionalProperties']) {
            throw new InvalidSchema('Schema does not have a properties, but doesnt allow for '
                + 'additional properties.');
        }
        if (!schema['additionalProperties']) {
            this._frozen = true;
            Object.freeze(this._value);
            Object.freeze(this._children);
        }
        else if (value) {
            // Set other properties which don't have a schema.
            for (const key of Object.keys(value)) {
                if (!this._children[key]) {
                    this._value[key] = value[key];
                }
            }
        }
    }
    set(v, force = false) {
        return this._set(v, false, force);
    }
    get frozen() { return this._frozen; }
    get children() { return this._children; }
    get type() { return 'object'; }
    get tsType() { return Object; }
    get defaultValue() { return null; }
    isCompatible(v) { return typeof v == 'object' && v !== null; }
    isChildRequired(name) {
        if (this._schema['required']) {
            return this._schema['required'].indexOf(name) != -1;
        }
        return false;
    }
    serialize(serializer) { serializer.object(this); }
}
exports.ObjectSchemaTreeNode = ObjectSchemaTreeNode;
/** A Schema Tree Node that represents an array. */
class ArraySchemaTreeNode extends NonLeafSchemaTreeNode {
    constructor(metaData) {
        super(metaData);
        this._set(metaData.value, true, false);
        // Keep the item's schema as a schema node. This is important to keep type information.
        this._itemPrototype = this._createChildProperty('', undefined, null, metaData.schema['items'], false);
    }
    _set(value, init, _force) {
        const schema = this._schema;
        const forward = this._forward;
        this._value = Object.create(null);
        this._dirty = this._dirty || !init;
        if (value) {
            this._defined = true;
        }
        else {
            this._defined = false;
            value = [];
        }
        this._items = [];
        this._value = [];
        for (let index = 0; index < value.length; index++) {
            this._items[index] = this._createChildProperty('' + index, value && value[index], forward && forward.items[index], schema['items']);
        }
    }
    set(v, init = false, force = false) {
        return this._set(v, init, force);
    }
    isCompatible(v) { return Array.isArray(v); }
    get type() { return 'array'; }
    get tsType() { return Array; }
    get items() { return this._items; }
    get itemPrototype() { return this._itemPrototype; }
    get defaultValue() { return null; }
    serialize(serializer) { serializer.array(this); }
}
exports.ArraySchemaTreeNode = ArraySchemaTreeNode;
/**
 * The root class of the tree node. Receives a prototype that will be filled with the
 * properties of the Schema root.
 */
class RootSchemaTreeNode extends ObjectSchemaTreeNode {
    constructor(proto, metaData) {
        super(metaData);
        for (const key of Object.keys(this._children)) {
            if (this._children[key]) {
                SchemaTreeNode._defineProperty(proto, this._children[key]);
            }
        }
    }
}
exports.RootSchemaTreeNode = RootSchemaTreeNode;
/** A leaf in the schema tree. Must contain a single primitive value. */
class LeafSchemaTreeNode extends SchemaTreeNode {
    constructor(metaData) {
        super(metaData);
        this._defined = metaData.value !== undefined;
        if ('default' in metaData.schema) {
            this._default = this.convert(metaData.schema['default']);
        }
    }
    get() {
        if (!this.defined && this._forward) {
            return this._forward.get();
        }
        if (!this.defined) {
            return 'default' in this._schema ? this._default : undefined;
        }
        return this._value === undefined
            ? undefined
            : (this._value === null ? null : this.convert(this._value));
    }
    set(v, init = false, force = false) {
        if (this.readOnly && !force) {
            throw new SettingReadOnlyPropertyError();
        }
        let convertedValue = this.convert(v);
        if (convertedValue === null || convertedValue === undefined) {
            if (this.required) {
                throw new InvalidValueError(`Invalid value "${v}" on a required field.`);
            }
        }
        this.dirty = !init;
        this._value = convertedValue;
    }
    destroy() {
        this._defined = false;
        this._value = null;
    }
    get defaultValue() {
        return this.hasDefault ? this._default : null;
    }
    get hasDefault() {
        return 'default' in this._schema;
    }
    serialize(serializer) {
        serializer.outputValue(this);
    }
}
exports.LeafSchemaTreeNode = LeafSchemaTreeNode;
/** Basic primitives for JSON Schema. */
class StringSchemaTreeNode extends LeafSchemaTreeNode {
    serialize(serializer) { serializer.outputString(this); }
    isCompatible(v) { return typeof v == 'string' || v instanceof String; }
    convert(v) { return v === undefined ? undefined : '' + v; }
    get type() { return 'string'; }
    get tsType() { return String; }
}
class EnumSchemaTreeNode extends LeafSchemaTreeNode {
    constructor(metaData) {
        super(metaData);
        if (!Array.isArray(metaData.schema['enum'])) {
            throw new InvalidSchema();
        }
        if (this.hasDefault && !this._isInEnum(this._default)) {
            throw new InvalidSchema();
        }
        this.set(metaData.value, true, true);
    }
    _isInEnum(value) {
        return this._schema['enum'].some((v) => v === value);
    }
    get items() { return this._schema['enum']; }
    set(value, init = false, force = false) {
        if (!(value === undefined || this._isInEnum(value))) {
            throw new InvalidUpdateValue('Invalid value can only be one of these: ' + this.items);
        }
        super.set(value, init, force);
    }
    isCompatible(v) {
        return this._isInEnum(v);
    }
    convert(v) {
        if (v === undefined) {
            return undefined;
        }
        if (!this._isInEnum(v)) {
            return undefined;
        }
        return v;
    }
    get type() {
        return this._schema['type'] || 'any';
    }
    get tsType() { return null; }
    serialize(serializer) { serializer.outputEnum(this); }
}
class BooleanSchemaTreeNode extends LeafSchemaTreeNode {
    serialize(serializer) { serializer.outputBoolean(this); }
    isCompatible(v) { return typeof v == 'boolean' || v instanceof Boolean; }
    convert(v) { return v === undefined ? undefined : !!v; }
    get type() { return 'boolean'; }
    get tsType() { return Boolean; }
}
class NumberSchemaTreeNode extends LeafSchemaTreeNode {
    serialize(serializer) { serializer.outputNumber(this); }
    isCompatible(v) { return typeof v == 'number' || v instanceof Number; }
    convert(v) { return v === undefined ? undefined : +v; }
    get type() { return 'number'; }
    get tsType() { return Number; }
}
class IntegerSchemaTreeNode extends NumberSchemaTreeNode {
    convert(v) { return v === undefined ? undefined : Math.floor(+v); }
}
//# sourceMappingURL=/users/hansl/sources/angular-cli/src/schema-tree.js.map