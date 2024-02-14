/**
 * @license
 * Copyright Google Inc. All Rights Reserved.
 *
 * Use of this source code is governed by an MIT-style license that can be
 * found in the LICENSE file at https://angular.io/license
 */
import { InjectionToken, SchemaMetadata, ɵConsole as Console } from '@angular/core';
import { CompileDirectiveMetadata, CompileDirectiveSummary, CompilePipeSummary, CompileTypeMetadata } from '../compile_metadata';
import { CompileReflector } from '../compile_reflector';
import { CompilerConfig } from '../config';
import { Parser } from '../expression_parser/parser';
import { I18NHtmlParser } from '../i18n/i18n_html_parser';
import { ParseTreeResult } from '../ml_parser/html_parser';
import { InterpolationConfig } from '../ml_parser/interpolation_config';
import { ParseError, ParseErrorLevel, ParseSourceSpan } from '../parse_util';
import { ElementSchemaRegistry } from '../schema/element_schema_registry';
import { CssSelector } from '../selector';
import { TemplateAst, TemplateAstVisitor } from './template_ast';
/**
 * Provides an array of {@link TemplateAstVisitor}s which will be used to transform
 * parsed templates before compilation is invoked, allowing custom expression syntax
 * and other advanced transformations.
 *
 * This is currently an internal-only feature and not meant for general use.
 */
export declare const TEMPLATE_TRANSFORMS: InjectionToken<{}>;
export declare class TemplateParseError extends ParseError {
    constructor(message: string, span: ParseSourceSpan, level: ParseErrorLevel);
}
export declare class TemplateParseResult {
    templateAst: TemplateAst[];
    usedPipes: CompilePipeSummary[];
    errors: ParseError[];
    constructor(templateAst?: TemplateAst[], usedPipes?: CompilePipeSummary[], errors?: ParseError[]);
}
export declare class TemplateParser {
    private _config;
    private _reflector;
    private _exprParser;
    private _schemaRegistry;
    private _htmlParser;
    private _console;
    transforms: TemplateAstVisitor[];
    constructor(_config: CompilerConfig, _reflector: CompileReflector, _exprParser: Parser, _schemaRegistry: ElementSchemaRegistry, _htmlParser: I18NHtmlParser, _console: Console, transforms: TemplateAstVisitor[]);
    parse(component: CompileDirectiveMetadata, template: string, directives: CompileDirectiveSummary[], pipes: CompilePipeSummary[], schemas: SchemaMetadata[], templateUrl: string, preserveWhitespaces: boolean): {
        template: TemplateAst[];
        pipes: CompilePipeSummary[];
    };
    tryParse(component: CompileDirectiveMetadata, template: string, directives: CompileDirectiveSummary[], pipes: CompilePipeSummary[], schemas: SchemaMetadata[], templateUrl: string, preserveWhitespaces: boolean): TemplateParseResult;
    tryParseHtml(htmlAstWithErrors: ParseTreeResult, component: CompileDirectiveMetadata, directives: CompileDirectiveSummary[], pipes: CompilePipeSummary[], schemas: SchemaMetadata[]): TemplateParseResult;
    expandHtml(htmlAstWithErrors: ParseTreeResult, forced?: boolean): ParseTreeResult;
    getInterpolationConfig(component: CompileDirectiveMetadata): InterpolationConfig | undefined;
}
export declare function splitClasses(classAttrValue: string): string[];
export declare function createElementCssSelector(elementName: string, attributes: [string, string][]): CssSelector;
export declare function removeSummaryDuplicates<T extends {
    type: CompileTypeMetadata;
}>(items: T[]): T[];
