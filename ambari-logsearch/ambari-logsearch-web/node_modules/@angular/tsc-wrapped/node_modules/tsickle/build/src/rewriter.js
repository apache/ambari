/**
 * @license
 * Copyright Google Inc. All Rights Reserved.
 *
 * Use of this source code is governed by an MIT-style license that can be
 * found in the LICENSE file at https://angular.io/license
 */
"use strict";
var source_map_1 = require("source-map");
var ts = require("typescript");
/**
 * A Rewriter manages iterating through a ts.SourceFile, copying input
 * to output while letting the subclass potentially alter some nodes
 * along the way by implementing maybeProcess().
 */
var Rewriter = (function () {
    function Rewriter(file) {
        this.file = file;
        this.output = [];
        /** Errors found while examining the code. */
        this.diagnostics = [];
        /** Current position in the output. */
        this.position = { line: 1, column: 1 };
        /**
         * The current level of recursion through TypeScript Nodes.  Used in formatting internal debug
         * print statements.
         */
        this.indent = 0;
        this.sourceMap = new source_map_1.SourceMapGenerator({ file: file.fileName });
        this.sourceMap.addMapping({
            original: this.position,
            generated: this.position,
            source: file.fileName,
        });
    }
    Rewriter.prototype.getOutput = function () {
        if (this.indent !== 0) {
            throw new Error('visit() failed to track nesting');
        }
        return {
            output: this.output.join(''),
            diagnostics: this.diagnostics,
            sourceMap: this.sourceMap,
        };
    };
    /**
     * visit traverses a Node, recursively writing all nodes not handled by this.maybeProcess.
     */
    Rewriter.prototype.visit = function (node) {
        // this.logWithIndent('node: ' + ts.SyntaxKind[node.kind]);
        this.indent++;
        if (!this.maybeProcess(node)) {
            this.writeNode(node);
        }
        this.indent--;
    };
    /**
     * maybeProcess lets subclasses optionally processes a node.
     *
     * @return True if the node has been handled and doesn't need to be traversed;
     *    false to have the node written and its children recursively visited.
     */
    Rewriter.prototype.maybeProcess = function (node) {
        return false;
    };
    /** writeNode writes a ts.Node, calling this.visit() on its children. */
    Rewriter.prototype.writeNode = function (node, skipComments) {
        var _this = this;
        if (skipComments === void 0) { skipComments = false; }
        var pos = node.getFullStart();
        if (skipComments) {
            // To skip comments, we skip all whitespace/comments preceding
            // the node.  But if there was anything skipped we should emit
            // a newline in its place so that the node remains separated
            // from the previous node.  TODO: don't skip anything here if
            // there wasn't any comment.
            if (node.getFullStart() < node.getStart()) {
                this.emit('\n');
            }
            pos = node.getStart();
        }
        ts.forEachChild(node, function (child) {
            _this.writeRange(pos, child.getFullStart());
            _this.visit(child);
            pos = child.getEnd();
        });
        this.writeRange(pos, node.getEnd());
    };
    // Write a span of the input file as expressed by absolute offsets.
    // These offsets are found in attributes like node.getFullStart() and
    // node.getEnd().
    Rewriter.prototype.writeRange = function (from, to) {
        // getSourceFile().getText() is wrong here because it has the text of
        // the SourceFile node of the AST, which doesn't contain the comments
        // preceding that node.  Semantically these ranges are just offsets
        // into the original source file text, so slice from that.
        var text = this.file.text.slice(from, to);
        if (text) {
            // Add a source mapping. writeRange(from, to) always corresponds to
            // original source code, so add a mapping at the current location that
            // points back to the location at `from`. The additional code generated
            // by tsickle will then be considered part of the last mapped code
            // section preceding it. That's arguably incorrect (e.g. for the fake
            // methods defining properties), but is good enough for stack traces.
            var pos = this.file.getLineAndCharacterOfPosition(from);
            this.sourceMap.addMapping({
                original: { line: pos.line + 1, column: pos.character + 1 },
                generated: this.position,
                source: this.file.fileName,
            });
            this.emit(text);
        }
    };
    Rewriter.prototype.emit = function (str) {
        this.output.push(str);
        for (var _i = 0, str_1 = str; _i < str_1.length; _i++) {
            var c = str_1[_i];
            this.position.column++;
            if (c === '\n') {
                this.position.line++;
                this.position.column = 1;
            }
        }
    };
    /** Removes comment metacharacters from a string, to make it safe to embed in a comment. */
    Rewriter.prototype.escapeForComment = function (str) {
        return str.replace(/\/\*/g, '__').replace(/\*\//g, '__');
    };
    /* tslint:disable: no-unused-variable */
    Rewriter.prototype.logWithIndent = function (message) {
        /* tslint:enable: no-unused-variable */
        var prefix = new Array(this.indent + 1).join('| ');
        console.log(prefix + message);
    };
    /**
     * Produces a compiler error that references the Node's kind.  This is useful for the "else"
     * branch of code that is attempting to handle all possible input Node types, to ensure all cases
     * covered.
     */
    Rewriter.prototype.errorUnimplementedKind = function (node, where) {
        this.error(node, ts.SyntaxKind[node.kind] + " not implemented in " + where);
    };
    Rewriter.prototype.error = function (node, messageText) {
        this.diagnostics.push({
            file: this.file,
            start: node.getStart(),
            length: node.getEnd() - node.getStart(),
            messageText: messageText,
            category: ts.DiagnosticCategory.Error,
            code: 0,
        });
    };
    return Rewriter;
}());
exports.Rewriter = Rewriter;
/** Returns the string contents of a ts.Identifier. */
function getIdentifierText(identifier) {
    // NOTE: the 'text' property on an Identifier may be escaped if it starts
    // with '__', so just use getText().
    return identifier.getText();
}
exports.getIdentifierText = getIdentifierText;
/**
 * Converts an escaped TypeScript name into the original source name.
 * Prefer getIdentifierText() instead if possible.
 */
function unescapeName(name) {
    // See the private function unescapeIdentifier in TypeScript's utilities.ts.
    if (name.match(/^___/))
        return name.substr(1);
    return name;
}
exports.unescapeName = unescapeName;

//# sourceMappingURL=rewriter.js.map
