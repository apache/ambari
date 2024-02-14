/**
 * @license
 * Copyright 2016 Palantir Technologies, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
"use strict";
var __extends = (this && this.__extends) || (function () {
    var extendStatics = Object.setPrototypeOf ||
        ({ __proto__: [] } instanceof Array && function (d, b) { d.__proto__ = b; }) ||
        function (d, b) { for (var p in b) if (b.hasOwnProperty(p)) d[p] = b[p]; };
    return function (d, b) {
        extendStatics(d, b);
        function __() { this.constructor = d; }
        d.prototype = b === null ? Object.create(b) : (__.prototype = b.prototype, new __());
    };
})();
Object.defineProperty(exports, "__esModule", { value: true });
var ts = require("typescript");
var Lint = require("../index");
var Rule = (function (_super) {
    __extends(Rule, _super);
    function Rule() {
        return _super !== null && _super.apply(this, arguments) || this;
    }
    Rule.prototype.apply = function (sourceFile) {
        var orderedImportsWalker = new OrderedImportsWalker(sourceFile, this.getOptions());
        return this.applyWithWalker(orderedImportsWalker);
    };
    return Rule;
}(Lint.Rules.AbstractRule));
/* tslint:disable:object-literal-sort-keys */
Rule.metadata = {
    ruleName: "ordered-imports",
    description: "Requires that import statements be alphabetized.",
    descriptionDetails: (_a = ["\n            Enforce a consistent ordering for ES6 imports:\n            - Named imports must be alphabetized (i.e. \"import {A, B, C} from \"foo\";\")\n                - The exact ordering can be controlled by the named-imports-order option.\n                - \"longName as name\" imports are ordered by \"longName\".\n            - Import sources must be alphabetized within groups, i.e.:\n                    import * as foo from \"a\";\n                    import * as bar from \"b\";\n            - Groups of imports are delineated by blank lines. You can use these to group imports\n                however you like, e.g. by first- vs. third-party or thematically."], _a.raw = ["\n            Enforce a consistent ordering for ES6 imports:\n            - Named imports must be alphabetized (i.e. \"import {A, B, C} from \"foo\";\")\n                - The exact ordering can be controlled by the named-imports-order option.\n                - \"longName as name\" imports are ordered by \"longName\".\n            - Import sources must be alphabetized within groups, i.e.:\n                    import * as foo from \"a\";\n                    import * as bar from \"b\";\n            - Groups of imports are delineated by blank lines. You can use these to group imports\n                however you like, e.g. by first- vs. third-party or thematically."], Lint.Utils.dedent(_a)),
    hasFix: true,
    optionsDescription: (_b = ["\n            You may set the `\"import-sources-order\"` option to control the ordering of source\n            imports (the `\"foo\"` in `import {A, B, C} from \"foo\"`).\n\n            Possible values for `\"import-sources-order\"` are:\n\n            * `\"case-insensitive'`: Correct order is `\"Bar\"`, `\"baz\"`, `\"Foo\"`. (This is the default.)\n            * `\"lowercase-first\"`: Correct order is `\"baz\"`, `\"Bar\"`, `\"Foo\"`.\n            * `\"lowercase-last\"`: Correct order is `\"Bar\"`, `\"Foo\"`, `\"baz\"`.\n            * `\"any\"`: Allow any order.\n\n            You may set the `\"named-imports-order\"` option to control the ordering of named\n            imports (the `{A, B, C}` in `import {A, B, C} from \"foo\"`).\n\n            Possible values for `\"named-imports-order\"` are:\n\n            * `\"case-insensitive'`: Correct order is `{A, b, C}`. (This is the default.)\n            * `\"lowercase-first\"`: Correct order is `{b, A, C}`.\n            * `\"lowercase-last\"`: Correct order is `{A, C, b}`.\n            * `\"any\"`: Allow any order.\n\n        "], _b.raw = ["\n            You may set the \\`\"import-sources-order\"\\` option to control the ordering of source\n            imports (the \\`\"foo\"\\` in \\`import {A, B, C} from \"foo\"\\`).\n\n            Possible values for \\`\"import-sources-order\"\\` are:\n\n            * \\`\"case-insensitive'\\`: Correct order is \\`\"Bar\"\\`, \\`\"baz\"\\`, \\`\"Foo\"\\`. (This is the default.)\n            * \\`\"lowercase-first\"\\`: Correct order is \\`\"baz\"\\`, \\`\"Bar\"\\`, \\`\"Foo\"\\`.\n            * \\`\"lowercase-last\"\\`: Correct order is \\`\"Bar\"\\`, \\`\"Foo\"\\`, \\`\"baz\"\\`.\n            * \\`\"any\"\\`: Allow any order.\n\n            You may set the \\`\"named-imports-order\"\\` option to control the ordering of named\n            imports (the \\`{A, B, C}\\` in \\`import {A, B, C} from \"foo\"\\`).\n\n            Possible values for \\`\"named-imports-order\"\\` are:\n\n            * \\`\"case-insensitive'\\`: Correct order is \\`{A, b, C}\\`. (This is the default.)\n            * \\`\"lowercase-first\"\\`: Correct order is \\`{b, A, C}\\`.\n            * \\`\"lowercase-last\"\\`: Correct order is \\`{A, C, b}\\`.\n            * \\`\"any\"\\`: Allow any order.\n\n        "], Lint.Utils.dedent(_b)),
    options: {
        type: "object",
        properties: {
            "import-sources-order": {
                type: "string",
                enum: ["case-insensitive", "lowercase-first", "lowercase-last", "any"],
            },
            "named-imports-order": {
                type: "string",
                enum: ["case-insensitive", "lowercase-first", "lowercase-last", "any"],
            },
        },
        additionalProperties: false,
    },
    optionExamples: [
        "true",
        '[true, {"import-sources-order": "lowercase-last", "named-imports-order": "lowercase-first"}]',
    ],
    type: "style",
    typescriptOnly: false,
};
/* tslint:enable:object-literal-sort-keys */
Rule.IMPORT_SOURCES_UNORDERED = "Import sources within a group must be alphabetized.";
Rule.NAMED_IMPORTS_UNORDERED = "Named imports must be alphabetized.";
exports.Rule = Rule;
// Convert aBcD --> AbCd
function flipCase(x) {
    return x.split("").map(function (char) {
        if (char >= "a" && char <= "z") {
            return char.toUpperCase();
        }
        else if (char >= "A" && char <= "Z") {
            return char.toLowerCase();
        }
        return char;
    }).join("");
}
// After applying a transformation, are the nodes sorted according to the text they contain?
// If not, return the pair of nodes which are out of order.
function findUnsortedPair(xs, transform) {
    for (var i = 1; i < xs.length; i++) {
        if (transform(xs[i].getText()) < transform(xs[i - 1].getText())) {
            return [xs[i - 1], xs[i]];
        }
    }
    return null;
}
function compare(a, b) {
    var isLow = function (value) {
        return [".", "/"].some(function (x) { return value[0] === x; });
    };
    if (isLow(a) && !isLow(b)) {
        return 1;
    }
    else if (!isLow(a) && isLow(b)) {
        return -1;
    }
    else if (a > b) {
        return 1;
    }
    else if (a < b) {
        return -1;
    }
    return 0;
}
function removeQuotes(value) {
    // strip out quotes
    if (value && value.length > 1 && (value[0] === "'" || value[0] === "\"")) {
        value = value.substr(1, value.length - 2);
    }
    return value;
}
function sortByKey(xs, getSortKey) {
    return xs.slice().sort(function (a, b) {
        return compare(getSortKey(a), getSortKey(b));
    });
}
// Transformations to apply to produce the desired ordering of imports.
// The imports must be lexicographically sorted after applying the transform.
var TRANSFORMS = {
    "any": function () { return ""; },
    "case-insensitive": function (x) { return x.toLowerCase(); },
    "lowercase-first": flipCase,
    "lowercase-last": function (x) { return x; },
};
var OrderedImportsWalker = (function (_super) {
    __extends(OrderedImportsWalker, _super);
    function OrderedImportsWalker(sourceFile, options) {
        var _this = _super.call(this, sourceFile, options) || this;
        _this.currentImportsBlock = new ImportsBlock();
        var optionSet = _this.getOptions()[0] || {};
        _this.importSourcesOrderTransform =
            TRANSFORMS[optionSet["import-sources-order"] || "case-insensitive"];
        _this.namedImportsOrderTransform =
            TRANSFORMS[optionSet["named-imports-order"] || "case-insensitive"];
        return _this;
    }
    // e.g. "import Foo from "./foo";"
    OrderedImportsWalker.prototype.visitImportDeclaration = function (node) {
        var source = node.moduleSpecifier.getText();
        source = removeQuotes(source);
        source = this.importSourcesOrderTransform(source);
        var previousSource = this.currentImportsBlock.getLastImportSource();
        this.currentImportsBlock.addImportDeclaration(this.getSourceFile(), node, source);
        if (previousSource && compare(source, previousSource) === -1) {
            this.lastFix = this.createFix();
            this.addFailureAtNode(node, Rule.IMPORT_SOURCES_UNORDERED, this.lastFix);
        }
        _super.prototype.visitImportDeclaration.call(this, node);
    };
    // This is the "{A, B, C}" of "import {A, B, C} from "./foo";".
    // We need to make sure they're alphabetized.
    OrderedImportsWalker.prototype.visitNamedImports = function (node) {
        var _this = this;
        var imports = node.elements;
        var pair = findUnsortedPair(imports, this.namedImportsOrderTransform);
        if (pair !== null) {
            var a = pair[0], b = pair[1];
            var sortedDeclarations = sortByKey(imports, function (x) { return _this.namedImportsOrderTransform(x.getText()); }).map(function (x) { return x.getText(); });
            // replace in reverse order to preserve earlier offsets
            for (var i = imports.length - 1; i >= 0; i--) {
                var start = imports[i].getStart();
                var length = imports[i].getText().length;
                // replace the named imports one at a time to preserve whitespace
                this.currentImportsBlock.replaceNamedImports(start, length, sortedDeclarations[i]);
            }
            this.lastFix = this.createFix();
            this.addFailureFromStartToEnd(a.getStart(), b.getEnd(), Rule.NAMED_IMPORTS_UNORDERED, this.lastFix);
        }
        _super.prototype.visitNamedImports.call(this, node);
    };
    // keep reading the block of import declarations until the block ends, then replace the entire block
    // this allows the reorder of named imports to work well with reordering lines
    OrderedImportsWalker.prototype.visitNode = function (node) {
        var prefixLength = node.getStart() - node.getFullStart();
        var prefix = node.getFullText().slice(0, prefixLength);
        var hasBlankLine = prefix.indexOf("\n\n") >= 0 || prefix.indexOf("\r\n\r\n") >= 0;
        var notImportDeclaration = node.parent != null
            && node.parent.kind === ts.SyntaxKind.SourceFile
            && node.kind !== ts.SyntaxKind.ImportDeclaration;
        if (hasBlankLine || notImportDeclaration) {
            // end of block
            if (this.lastFix != null) {
                var replacement = this.currentImportsBlock.getReplacement();
                if (replacement != null) {
                    this.lastFix.replacements.push(replacement);
                }
                this.lastFix = null;
            }
            this.currentImportsBlock = new ImportsBlock();
        }
        _super.prototype.visitNode.call(this, node);
    };
    return OrderedImportsWalker;
}(Lint.RuleWalker));
var ImportsBlock = (function () {
    function ImportsBlock() {
        this.importDeclarations = [];
    }
    ImportsBlock.prototype.addImportDeclaration = function (sourceFile, node, sourcePath) {
        var start = this.getStartOffset(node);
        var end = this.getEndOffset(sourceFile, node);
        var text = sourceFile.text.substring(start, end);
        if (start > node.getStart() || end === 0) {
            // skip block if any statements don't end with a newline to simplify implementation
            this.importDeclarations = [];
            return;
        }
        this.importDeclarations.push({
            node: node,
            nodeEndOffset: end,
            nodeStartOffset: start,
            sourcePath: sourcePath,
            text: text,
        });
    };
    // replaces the named imports on the most recent import declaration
    ImportsBlock.prototype.replaceNamedImports = function (fileOffset, length, replacement) {
        var importDeclaration = this.getLastImportDeclaration();
        if (importDeclaration == null) {
            // nothing to replace. This can happen if the block is skipped
            return;
        }
        var start = fileOffset - importDeclaration.nodeStartOffset;
        if (start < 0 || start + length > importDeclaration.node.getEnd()) {
            throw new Error("Unexpected named import position");
        }
        var initialText = importDeclaration.text;
        importDeclaration.text = initialText.substring(0, start) + replacement + initialText.substring(start + length);
    };
    ImportsBlock.prototype.getLastImportSource = function () {
        if (this.importDeclarations.length === 0) {
            return null;
        }
        return this.getLastImportDeclaration().sourcePath;
    };
    // creates a Lint.Replacement object with ordering fixes for the entire block
    ImportsBlock.prototype.getReplacement = function () {
        if (this.importDeclarations.length === 0) {
            return null;
        }
        var sortedDeclarations = sortByKey(this.importDeclarations.slice(), function (x) { return x.sourcePath; });
        var fixedText = sortedDeclarations.map(function (x) { return x.text; }).join("");
        var start = this.importDeclarations[0].nodeStartOffset;
        var end = this.getLastImportDeclaration().nodeEndOffset;
        return new Lint.Replacement(start, end - start, fixedText);
    };
    // gets the offset immediately after the end of the previous declaration to include comment above
    ImportsBlock.prototype.getStartOffset = function (node) {
        if (this.importDeclarations.length === 0) {
            return node.getStart();
        }
        return this.getLastImportDeclaration().nodeEndOffset;
    };
    // gets the offset of the end of the import's line, including newline, to include comment to the right
    ImportsBlock.prototype.getEndOffset = function (sourceFile, node) {
        var endLineOffset = sourceFile.text.indexOf("\n", node.end) + 1;
        return endLineOffset;
    };
    ImportsBlock.prototype.getLastImportDeclaration = function () {
        return this.importDeclarations[this.importDeclarations.length - 1];
    };
    return ImportsBlock;
}());
var _a, _b;
