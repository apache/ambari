"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const serializer_1 = require("./serializer");
const schema_tree_1 = require("./schema-tree");
const error_1 = require("./error");
require("./mimetypes");
class InvalidJsonPath extends error_1.JsonSchemaErrorBase {
}
exports.InvalidJsonPath = InvalidJsonPath;
// The schema tree node property of the SchemaClass.
const kSchemaNode = Symbol('schema-node');
// The value property of the SchemaClass.
const kOriginalRoot = Symbol('schema-value');
/**
 * Splits a JSON path string into fragments. Fragments can be used to get the value referenced
 * by the path. For example, a path of "a[3].foo.bar[2]" would give you a fragment array of
 * ["a", 3, "foo", "bar", 2].
 * @param path The JSON string to parse.
 * @returns {string[]} The fragments for the string.
 * @private
 */
function _parseJsonPath(path) {
    const fragments = (path || '').split(/\./g);
    const result = [];
    while (fragments.length > 0) {
        const fragment = fragments.shift();
        const match = fragment.match(/([^\[]+)((\[.*\])*)/);
        if (!match) {
            throw new InvalidJsonPath();
        }
        result.push(match[1]);
        if (match[2]) {
            const indices = match[2].slice(1, -1).split('][');
            result.push(...indices);
        }
    }
    return result.filter(fragment => !!fragment);
}
/** Get a SchemaTreeNode from the JSON path string. */
function _getSchemaNodeForPath(rootMetaData, path) {
    let fragments = _parseJsonPath(path);
    // TODO: make this work with union (oneOf) schemas
    return fragments.reduce((md, current) => {
        if (md && md.children) {
            return md.children[current];
        }
        else if (md && md.items) {
            return md.items[parseInt(current, 10)];
        }
        else {
            return md;
        }
    }, rootMetaData);
}
class SchemaClassBase {
    constructor(schema, value, ...fallbacks) {
        this[kOriginalRoot] = value;
        const forward = fallbacks.length > 0
            ? (new SchemaClassBase(schema, fallbacks.pop(), ...fallbacks).$$schema())
            : null;
        this[kSchemaNode] = new schema_tree_1.RootSchemaTreeNode(this, {
            forward,
            value,
            schema
        });
    }
    $$root() { return this; }
    $$schema() { return this[kSchemaNode]; }
    $$originalRoot() { return this[kOriginalRoot]; }
    /** Sets the value of a destination if the value is currently undefined. */
    $$alias(source, destination) {
        let sourceSchemaTreeNode = _getSchemaNodeForPath(this.$$schema(), source);
        if (!sourceSchemaTreeNode) {
            return false;
        }
        const fragments = _parseJsonPath(destination);
        const maybeValue = fragments.reduce((value, current) => {
            return value && value[current];
        }, this.$$originalRoot());
        if (maybeValue !== undefined) {
            sourceSchemaTreeNode.set(maybeValue);
            return true;
        }
        return false;
    }
    /** Destroy all links between schemas to allow for GC. */
    $$dispose() {
        this.$$schema().dispose();
    }
    /** Get a value from a JSON path. */
    $$get(path) {
        const node = _getSchemaNodeForPath(this.$$schema(), path);
        return node ? node.get() : undefined;
    }
    /** Set a value from a JSON path. */
    $$set(path, value) {
        const node = _getSchemaNodeForPath(this.$$schema(), path);
        if (node) {
            node.set(value);
        }
        else {
            // This might be inside an object that can have additionalProperties, so
            // a TreeNode would not exist.
            const splitPath = _parseJsonPath(path);
            if (!splitPath) {
                return undefined;
            }
            const parent = splitPath
                .slice(0, -1)
                .reduce((parent, curr) => parent && parent[curr], this);
            if (parent) {
                parent[splitPath[splitPath.length - 1]] = value;
            }
        }
    }
    /** Get the Schema associated with a path. */
    $$typeOf(path) {
        const node = _getSchemaNodeForPath(this.$$schema(), path);
        return node ? node.type : null;
    }
    $$defined(path) {
        const node = _getSchemaNodeForPath(this.$$schema(), path);
        return node ? node.defined : false;
    }
    $$delete(path) {
        const node = _getSchemaNodeForPath(this.$$schema(), path);
        if (node) {
            node.destroy();
        }
    }
    /** Serialize into a string. */
    $$serialize(mimetype = 'application/json', ...options) {
        let str = '';
        const serializer = serializer_1.Serializer.fromMimetype(mimetype, (s) => str += s, ...options);
        serializer.start();
        this.$$schema().serialize(serializer);
        serializer.end();
        return str;
    }
}
/**
 * Create a class from a JSON SCHEMA object. Instanciating that class with an object
 * allows for extended behaviour.
 * This is the base API to access the Configuration in the CLI.
 * @param schema
 * @returns {GeneratedSchemaClass}
 * @constructor
 */
function SchemaClassFactory(schema) {
    class GeneratedSchemaClass extends SchemaClassBase {
        constructor(value, ...fallbacks) {
            super(schema, value, ...fallbacks);
        }
    }
    return GeneratedSchemaClass;
}
exports.SchemaClassFactory = SchemaClassFactory;
//# sourceMappingURL=/users/hansl/sources/angular-cli/src/schema-class-factory.js.map