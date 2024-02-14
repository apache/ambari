"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
class JsonSerializer {
    constructor(_writer, _indentDelta = 2) {
        this._writer = _writer;
        this._indentDelta = _indentDelta;
        this._state = [];
    }
    _willOutputValue() {
        if (this._state.length > 0) {
            const top = this._top();
            const wasEmpty = top.empty;
            top.empty = false;
            if (!wasEmpty && !top.property) {
                this._writer(',');
            }
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
    }
    object(node) {
        if (node.defined == false) {
            return;
        }
        this._willOutputValue();
        this._writer('{');
        this._state.push({ empty: true, type: 'object' });
        for (const key of Object.keys(node.children)) {
            this.property(node.children[key]);
        }
        // Fallback to direct value output for additional properties.
        if (!node.frozen) {
            for (const key of Object.keys(node.value)) {
                if (key in node.children) {
                    continue;
                }
                this._willOutputValue();
                this._writer(JSON.stringify(key));
                this._writer(': ');
                this._writer(JSON.stringify(node.value[key]));
            }
        }
        this._state.pop();
        if (!this._top().empty) {
            this._indent();
        }
        this._writer('}');
    }
    property(node) {
        if (node.defined == false) {
            return;
        }
        this._willOutputValue();
        this._writer(JSON.stringify(node.name));
        this._writer(': ');
        this._top().property = true;
        node.serialize(this);
        this._top().property = false;
    }
    array(node) {
        if (node.defined == false) {
            return;
        }
        this._willOutputValue();
        if (node.items.length === 0) {
            this._writer('[]');
            return;
        }
        this._writer('[');
        this._state.push({ empty: true, type: 'array' });
        for (let i = 0; i < node.items.length; i++) {
            node.items[i].serialize(this);
        }
        this._state.pop();
        if (!this._top().empty) {
            this._indent();
        }
        this._writer(']');
    }
    outputOneOf(node) {
        this.outputValue(node);
    }
    outputEnum(node) {
        this.outputValue(node);
    }
    outputValue(node) {
        this._willOutputValue();
        this._writer(JSON.stringify(node.value, null, this._indentDelta));
    }
    outputString(node) {
        this._willOutputValue();
        this._writer(JSON.stringify(node.value));
    }
    outputNumber(node) {
        this._willOutputValue();
        this._writer(JSON.stringify(node.value));
    }
    outputInteger(node) {
        this._willOutputValue();
        this._writer(JSON.stringify(node.value));
    }
    outputBoolean(node) {
        this._willOutputValue();
        this._writer(JSON.stringify(node.value));
    }
}
exports.JsonSerializer = JsonSerializer;
//# sourceMappingURL=/users/hansl/sources/angular-cli/src/serializers/json.js.map