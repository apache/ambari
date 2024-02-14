"use strict";
var core_1 = require("@angular/core");
var compiler = require("@angular/compiler");
var config_1 = require("../config");
var ngVersion_1 = require("../../util/ngVersion");
var refId = 0;
var dummyMetadataFactory = function (declaration) {
    if (refId > 1e10) {
        refId = 0;
    }
    return {
        inputs: declaration.inputs || [],
        outputs: declaration.outputs || [],
        hostListeners: declaration.hostListeners || {},
        hostProperties: declaration.hostProperties || {},
        hostAttributes: declaration.hostAttributes || {},
        isSummary: true,
        type: {
            diDeps: [],
            lifecycleHooks: [],
            isHost: false,
            reference: ++refId + '-ref'
        },
        isComponent: false,
        selector: declaration.selector,
        exportAs: declaration.exportAs,
        providers: [],
        viewProviders: [],
        queries: [],
        entryComponents: [],
        changeDetection: 0,
        template: {
            isSummary: true,
            animations: [],
            ngContentSelectors: [],
            encapsulation: 0
        }
    };
};
var defaultDirectives = [];
exports.parseTemplate = function (template, directives) {
    if (directives === void 0) { directives = []; }
    defaultDirectives = directives.map(function (d) { return dummyMetadataFactory(d); });
    var TemplateParser = compiler.TemplateParser;
    var expressionParser = new compiler.Parser(new compiler.Lexer());
    var elementSchemaRegistry = new compiler.DomElementSchemaRegistry();
    var ngConsole = new core_1.__core_private__.Console();
    var htmlParser = new compiler.I18NHtmlParser(new compiler.HtmlParser());
    var tmplParser;
    ngVersion_1.SemVerDSL
        .gte('4.0.0-beta.8', function () {
        var config = new compiler.CompilerConfig({});
        tmplParser =
            new TemplateParser(config, expressionParser, elementSchemaRegistry, htmlParser, ngConsole, []);
    })
        .else(function () {
        tmplParser =
            new TemplateParser(expressionParser, elementSchemaRegistry, htmlParser, ngConsole, []);
    });
    var interpolation = config_1.Config.interpolation;
    var summaryKind = (compiler.CompileSummaryKind || {}).Template;
    var templateMetadata = {
        encapsulation: 0,
        template: template,
        templateUrl: '',
        styles: [],
        styleUrls: [],
        ngContentSelectors: [],
        animations: [],
        externalStylesheets: [],
        interpolation: interpolation,
        toSummary: function () {
            return {
                isSummary: true,
                animations: this.animations.map(function (anim) { return anim.name; }),
                ngContentSelectors: this.ngContentSelectors,
                encapsulation: this.encapsulation,
                summaryKind: summaryKind
            };
        }
    };
    var type = {
        diDeps: [],
        lifecycleHooks: [],
        reference: null,
        isHost: false,
        name: '',
        prefix: '',
        moduleUrl: '',
        value: '',
        identifier: null
    };
    var result = tmplParser.tryParse(compiler.CompileDirectiveMetadata.create({ type: type, template: templateMetadata }), template, defaultDirectives, [], [core_1.NO_ERRORS_SCHEMA], '').templateAst;
    return result;
};
