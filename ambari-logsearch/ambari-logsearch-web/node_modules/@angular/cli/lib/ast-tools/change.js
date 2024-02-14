"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const fs = require("fs");
const denodeify = require("denodeify");
const readFile = denodeify(fs.readFile);
const writeFile = denodeify(fs.writeFile);
exports.NodeHost = {
    write: (path, content) => writeFile(path, content, 'utf8'),
    read: (path) => readFile(path, 'utf8')
};
/**
 * An operation that does nothing.
 */
class NoopChange {
    constructor() {
        this.description = 'No operation.';
        this.order = Infinity;
        this.path = null;
    }
    apply() { return Promise.resolve(); }
}
exports.NoopChange = NoopChange;
/**
 * An operation that mixes two or more changes, and merge them (in order).
 * Can only apply to a single file. Use a ChangeManager to apply changes to multiple
 * files.
 */
class MultiChange {
    constructor(...changes) {
        this._changes = [];
        [].concat(...changes).forEach(change => this.appendChange(change));
    }
    appendChange(change) {
        // Do not append Noop changes.
        if (change instanceof NoopChange) {
            return;
        }
        // Validate that the path is the same for everyone of those.
        if (this._path === undefined) {
            this._path = change.path;
        }
        else if (change.path !== this._path) {
            throw new Error('Cannot apply a change to a different path.');
        }
        this._changes.push(change);
    }
    get description() {
        return `Changes:\n   ${this._changes.map(x => x.description).join('\n   ')}`;
    }
    // Always apply as early as the highest change.
    get order() { return Math.max(...this._changes.map(c => c.order)); }
    get path() { return this._path; }
    apply(host) {
        return this._changes
            .sort((a, b) => b.order - a.order)
            .reduce((promise, change) => {
            return promise.then(() => change.apply(host));
        }, Promise.resolve());
    }
}
exports.MultiChange = MultiChange;
/**
 * Will add text to the source code.
 */
class InsertChange {
    constructor(path, pos, toAdd) {
        this.path = path;
        this.pos = pos;
        this.toAdd = toAdd;
        if (pos < 0) {
            throw new Error('Negative positions are invalid');
        }
        this.description = `Inserted ${toAdd} into position ${pos} of ${path}`;
        this.order = pos;
    }
    /**
     * This method does not insert spaces if there is none in the original string.
     */
    apply(host) {
        return host.read(this.path).then(content => {
            let prefix = content.substring(0, this.pos);
            let suffix = content.substring(this.pos);
            return host.write(this.path, `${prefix}${this.toAdd}${suffix}`);
        });
    }
}
exports.InsertChange = InsertChange;
/**
 * Will remove text from the source code.
 */
class RemoveChange {
    constructor(path, pos, toRemove) {
        this.path = path;
        this.pos = pos;
        this.toRemove = toRemove;
        if (pos < 0) {
            throw new Error('Negative positions are invalid');
        }
        this.description = `Removed ${toRemove} into position ${pos} of ${path}`;
        this.order = pos;
    }
    apply(host) {
        return host.read(this.path).then(content => {
            let prefix = content.substring(0, this.pos);
            let suffix = content.substring(this.pos + this.toRemove.length);
            // TODO: throw error if toRemove doesn't match removed string.
            return host.write(this.path, `${prefix}${suffix}`);
        });
    }
}
exports.RemoveChange = RemoveChange;
/**
 * Will replace text from the source code.
 */
class ReplaceChange {
    constructor(path, pos, oldText, newText) {
        this.path = path;
        this.pos = pos;
        this.oldText = oldText;
        this.newText = newText;
        if (pos < 0) {
            throw new Error('Negative positions are invalid');
        }
        this.description = `Replaced ${oldText} into position ${pos} of ${path} with ${newText}`;
        this.order = pos;
    }
    apply(host) {
        return host.read(this.path).then(content => {
            const prefix = content.substring(0, this.pos);
            const suffix = content.substring(this.pos + this.oldText.length);
            const text = content.substring(this.pos, this.pos + this.oldText.length);
            if (text !== this.oldText) {
                return Promise.reject(new Error(`Invalid replace: "${text}" != "${this.oldText}".`));
            }
            // TODO: throw error if oldText doesn't match removed string.
            return host.write(this.path, `${prefix}${this.newText}${suffix}`);
        });
    }
}
exports.ReplaceChange = ReplaceChange;
//# sourceMappingURL=/users/hansl/sources/angular-cli/lib/ast-tools/change.js.map