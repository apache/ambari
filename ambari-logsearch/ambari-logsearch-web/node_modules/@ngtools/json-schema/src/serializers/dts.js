"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const serializer_1 = require("../serializer");
class DTsSerializer {
    constructor(_writer, interfaceName, _indentDelta = 4) {
        this._writer = _writer;
        this.interfaceName = interfaceName;
        this._indentDelta = _indentDelta;
        this._state = [];
        if (interfaceName) {
            _writer(`export interface ${interfaceName} `);
        }
        else {
            _writer('interface _ ');
        }
    }
    _willOutputValue() {
        if (this._state.length > 0) {
            const top = this._top();
            top.empty = false;
            if (!top.property) {
                this._indent();
            }
        }
    }
    _top() {
        return this._state[this._state.length - 1] || {};
    }
    _indent() {
        if (this._indentDelta == 0) {
            return;
        }
        let str = '\n';
        let i = this._state.length * this._indentDelta;
        while (i--) {
            str += ' ';
        }
        this._writer(str);
    }
    start() { }
    end() {
        if (this._indentDelta) {
            this._writer('\n');
        }
        if (!this.interfaceName) {
            this._writer('export default _;\n');
        }
    }
    object(node) {
        this._willOutputValue();
        this._writer('{');
        this._state.push({ empty: true, type: 'object' });
        for (const key of Object.keys(node.children)) {
            this.property(node.children[key]);
        }
        // Fallback to direct value output for additional properties.
        if (!node.frozen) {
            this._indent();
            this._writer('[name: string]: any;');
        }
        this._state.pop();
        if (!this._top().empty) {
            this._indent();
        }
        this._writer('}');
    }
    property(node) {
        this._willOutputValue();
        if (node.description) {
            this._writer('/**');
            this._indent();
            node.description.split('\n').forEach(line => {
                this._writer(' * ' + line);
                this._indent();
            });
            this._writer(' */');
            this._indent();
        }
        this._writer(node.name);
        if (!node.required) {
            this._writer('?');
        }
        this._writer(': ');
        this._top().property = true;
        node.serialize(this);
        this._top().property = false;
        this._writer(';');
    }
    array(node) {
        this._willOutputValue();
        node.itemPrototype.serialize(this);
        this._writer('[]');
    }
    outputOneOf(node) {
        this._willOutputValue();
        if (!node.items) {
            throw new serializer_1.InvalidStateError();
        }
        this._writer('(');
        for (let i = 0; i < node.items.length; i++) {
            node.items[i].serialize(this);
            if (i != node.items.length - 1) {
                this._writer(' | ');
            }
        }
        this._writer(')');
    }
    outputEnum(node) {
        this._willOutputValue();
        this._writer('(');
        for (let i = 0; i < node.items.length; i++) {
            this._writer(JSON.stringify(node.items[i]));
            if (i != node.items.length - 1) {
                this._writer(' | ');
            }
        }
        this._writer(')');
    }
    outputValue(_node) {
        this._willOutputValue();
        this._writer('any');
    }
    outputString(_node) {
        this._willOutputValue();
        this._writer('string');
    }
    outputNumber(_node) {
        this._willOutputValue();
        this._writer('number');
    }
    outputInteger(_node) {
        this._willOutputValue();
        this._writer('number');
    }
    outputBoolean(_node) {
        this._willOutputValue();
        this._writer('boolean');
    }
}
exports.DTsSerializer = DTsSerializer;
//# sourceMappingURL=/users/hansl/sources/angular-cli/src/serializers/dts.js.map