"use strict";
var __extends = (this && this.__extends) || function (d, b) {
    for (var p in b) if (b.hasOwnProperty(p)) d[p] = b[p];
    function __() { this.constructor = d; }
    d.prototype = b === null ? Object.create(b) : (__.prototype = b.prototype, new __());
};
var Lint = require("tslint");
var ts = require("typescript");
var compiler = require("@angular/compiler");
var templateParser_1 = require("./templates/templateParser");
var parseCss_1 = require("./styles/parseCss");
var basicCssAstVisitor_1 = require("./styles/basicCssAstVisitor");
var basicTemplateAstVisitor_1 = require("./templates/basicTemplateAstVisitor");
var recursiveAngularExpressionVisitor_1 = require("./templates/recursiveAngularExpressionVisitor");
var referenceCollectorVisitor_1 = require("./templates/referenceCollectorVisitor");
var metadata_1 = require("./metadata");
var ng2WalkerFactoryUtils_1 = require("./ng2WalkerFactoryUtils");
var config_1 = require("./config");
var logger_1 = require("../util/logger");
var utils_1 = require("../util/utils");
var getDecoratorStringArgs = function (decorator) {
    var baseExpr = decorator.expression || {};
    var args = baseExpr.arguments || [];
    return args.map(function (a) { return (a.kind === ts.SyntaxKind.StringLiteral) ? a.text : null; });
};
var Ng2Walker = (function (_super) {
    __extends(Ng2Walker, _super);
    function Ng2Walker(sourceFile, _originalOptions, _config, _metadataReader) {
        var _this = _super.call(this, sourceFile, _originalOptions) || this;
        _this._originalOptions = _originalOptions;
        _this._config = _config;
        _this._metadataReader = _metadataReader;
        _this._metadataReader = _this._metadataReader || ng2WalkerFactoryUtils_1.ng2WalkerFactoryUtils.defaultMetadataReader();
        _this._config = Object.assign({
            templateVisitorCtrl: basicTemplateAstVisitor_1.BasicTemplateAstVisitor,
            expressionVisitorCtrl: recursiveAngularExpressionVisitor_1.RecursiveAngularExpressionVisitor,
            cssVisitorCtrl: basicCssAstVisitor_1.BasicCssAstVisitor
        }, _this._config || {});
        _this._config = Object.assign({
            templateVisitorCtrl: basicTemplateAstVisitor_1.BasicTemplateAstVisitor,
            expressionVisitorCtrl: recursiveAngularExpressionVisitor_1.RecursiveAngularExpressionVisitor,
            cssVisitorCtrl: basicCssAstVisitor_1.BasicCssAstVisitor
        }, _this._config || {});
        return _this;
    }
    Ng2Walker.prototype.visitClassDeclaration = function (declaration) {
        var metadata = this._metadataReader.read(declaration);
        if (metadata instanceof metadata_1.ComponentMetadata) {
            this.visitNg2Component(metadata);
        }
        else if (metadata instanceof metadata_1.DirectiveMetadata) {
            this.visitNg2Directive(metadata);
        }
        (declaration.decorators || []).forEach(this.visitClassDecorator.bind(this));
        _super.prototype.visitClassDeclaration.call(this, declaration);
    };
    Ng2Walker.prototype.visitMethodDeclaration = function (method) {
        (method.decorators || []).forEach(this.visitMethodDecorator.bind(this));
        _super.prototype.visitMethodDeclaration.call(this, method);
    };
    Ng2Walker.prototype.visitPropertyDeclaration = function (prop) {
        (prop.decorators || []).forEach(this.visitPropertyDecorator.bind(this));
        _super.prototype.visitPropertyDeclaration.call(this, prop);
    };
    Ng2Walker.prototype.visitMethodDecorator = function (decorator) {
        var name = utils_1.getDecoratorName(decorator);
        if (name === 'HostListener') {
            this.visitNg2HostListener(decorator.parent, decorator, getDecoratorStringArgs(decorator));
        }
    };
    Ng2Walker.prototype.visitPropertyDecorator = function (decorator) {
        var name = utils_1.getDecoratorName(decorator);
        switch (name) {
            case 'Input':
                this.visitNg2Input(decorator.parent, decorator, getDecoratorStringArgs(decorator));
                break;
            case 'Output':
                this.visitNg2Output(decorator.parent, decorator, getDecoratorStringArgs(decorator));
                break;
            case 'HostBinding':
                this.visitNg2HostBinding(decorator.parent, decorator, getDecoratorStringArgs(decorator));
                break;
        }
    };
    Ng2Walker.prototype.visitClassDecorator = function (decorator) {
        var name = utils_1.getDecoratorName(decorator);
        if (!decorator.expression.arguments ||
            !decorator.expression.arguments.length ||
            !decorator.expression.arguments[0].properties) {
            return;
        }
        if (name === 'Pipe') {
            this.visitNg2Pipe(decorator.parent, decorator);
        }
    };
    Ng2Walker.prototype.visitNg2Component = function (metadata) {
        var template = metadata.template;
        var getPosition = function (node) {
            var pos = 0;
            if (node) {
                pos = node.pos + 1;
                try {
                    pos = node.getStart() + 1;
                }
                catch (e) { }
            }
            return pos;
        };
        if (template && template.template) {
            try {
                var templateAst = templateParser_1.parseTemplate(template.template.code, config_1.Config.predefinedDirectives);
                this.visitNg2TemplateHelper(templateAst, metadata, getPosition(template.node));
            }
            catch (e) {
                logger_1.logger.error('Cannot parse the template of', ((metadata.controller || {}).name || {}).text, e);
            }
        }
        var styles = metadata.styles;
        if (styles && styles.length) {
            for (var i = 0; i < styles.length; i += 1) {
                var style = styles[i];
                try {
                    this.visitNg2StyleHelper(parseCss_1.parseCss(style.style.code), metadata, style, getPosition(style.node));
                }
                catch (e) {
                    logger_1.logger.error('Cannot parse the styles of', ((metadata.controller || {}).name || {}).text, e);
                }
            }
        }
    };
    Ng2Walker.prototype.visitNg2Directive = function (metadata) { };
    Ng2Walker.prototype.visitNg2Pipe = function (controller, decorator) { };
    Ng2Walker.prototype.visitNg2Input = function (property, input, args) { };
    Ng2Walker.prototype.visitNg2Output = function (property, output, args) { };
    Ng2Walker.prototype.visitNg2HostBinding = function (property, decorator, args) { };
    Ng2Walker.prototype.visitNg2HostListener = function (method, decorator, args) { };
    Ng2Walker.prototype.visitNg2TemplateHelper = function (roots, context, baseStart) {
        var _this = this;
        if (!roots || !roots.length) {
            return;
        }
        else {
            var sourceFile = this.getContextSourceFile(context.template.url, context.template.template.source);
            var referenceVisitor = new referenceCollectorVisitor_1.ReferenceCollectorVisitor();
            var visitor = new this._config.templateVisitorCtrl(sourceFile, this._originalOptions, context, baseStart, this._config.expressionVisitorCtrl);
            compiler.templateVisitAll(referenceVisitor, roots, null);
            visitor._variables = referenceVisitor.variables;
            compiler.templateVisitAll(visitor, roots, context.controller);
            visitor.getFailures().forEach(function (f) { return _this.addFailure(f); });
        }
    };
    Ng2Walker.prototype.visitNg2StyleHelper = function (style, context, styleMetadata, baseStart) {
        var _this = this;
        if (!style) {
            return;
        }
        else {
            var sourceFile = this.getContextSourceFile(styleMetadata.url, styleMetadata.style.source);
            var visitor = new this._config.cssVisitorCtrl(sourceFile, this._originalOptions, context, styleMetadata, baseStart);
            style.visit(visitor);
            visitor.getFailures().forEach(function (f) { return _this.addFailure(f); });
        }
    };
    Ng2Walker.prototype.getContextSourceFile = function (path, content) {
        var current = this.getSourceFile();
        if (!path) {
            return current;
        }
        return ts.createSourceFile(path, "`" + content + "`", ts.ScriptTarget.ES5);
    };
    return Ng2Walker;
}(Lint.RuleWalker));
exports.Ng2Walker = Ng2Walker;
