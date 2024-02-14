/**
 * @license
 * Copyright Google Inc. All Rights Reserved.
 *
 * Use of this source code is governed by an MIT-style license that can be
 * found in the LICENSE file at https://angular.io/license
 */
import { SecurityContext } from '@angular/core';
import { AstPath } from '../ast_path';
import { CompileDirectiveSummary, CompileProviderMetadata, CompileTokenMetadata } from '../compile_metadata';
import { AST } from '../expression_parser/ast';
import { LifecycleHooks } from '../lifecycle_reflector';
import { ParseSourceSpan } from '../parse_util';
/**
 * An Abstract Syntax Tree node representing part of a parsed Angular template.
 */
export interface TemplateAst {
    /**
     * The source span from which this node was parsed.
     */
    sourceSpan: ParseSourceSpan;
    /**
     * Visit this node and possibly transform it.
     */
    visit(visitor: TemplateAstVisitor, context: any): any;
}
/**
 * A segment of text within the template.
 */
export declare class TextAst implements TemplateAst {
    value: string;
    ngContentIndex: number;
    sourceSpan: ParseSourceSpan;
    constructor(value: string, ngContentIndex: number, sourceSpan: ParseSourceSpan);
    visit(visitor: TemplateAstVisitor, context: any): any;
}
/**
 * A bound expression within the text of a template.
 */
export declare class BoundTextAst implements TemplateAst {
    value: AST;
    ngContentIndex: number;
    sourceSpan: ParseSourceSpan;
    constructor(value: AST, ngContentIndex: number, sourceSpan: ParseSourceSpan);
    visit(visitor: TemplateAstVisitor, context: any): any;
}
/**
 * A plain attribute on an element.
 */
export declare class AttrAst implements TemplateAst {
    name: string;
    value: string;
    sourceSpan: ParseSourceSpan;
    constructor(name: string, value: string, sourceSpan: ParseSourceSpan);
    visit(visitor: TemplateAstVisitor, context: any): any;
}
/**
 * A binding for an element property (e.g. `[property]="expression"`) or an animation trigger (e.g.
 * `[@trigger]="stateExp"`)
 */
export declare class BoundElementPropertyAst implements TemplateAst {
    name: string;
    type: PropertyBindingType;
    securityContext: SecurityContext;
    value: AST;
    unit: string | null;
    sourceSpan: ParseSourceSpan;
    constructor(name: string, type: PropertyBindingType, securityContext: SecurityContext, value: AST, unit: string | null, sourceSpan: ParseSourceSpan);
    visit(visitor: TemplateAstVisitor, context: any): any;
    readonly isAnimation: boolean;
}
/**
 * A binding for an element event (e.g. `(event)="handler()"`) or an animation trigger event (e.g.
 * `(@trigger.phase)="callback($event)"`).
 */
export declare class BoundEventAst implements TemplateAst {
    name: string;
    target: string | null;
    phase: string | null;
    handler: AST;
    sourceSpan: ParseSourceSpan;
    static calcFullName(name: string, target: string | null, phase: string | null): string;
    constructor(name: string, target: string | null, phase: string | null, handler: AST, sourceSpan: ParseSourceSpan);
    visit(visitor: TemplateAstVisitor, context: any): any;
    readonly fullName: string;
    readonly isAnimation: boolean;
}
/**
 * A reference declaration on an element (e.g. `let someName="expression"`).
 */
export declare class ReferenceAst implements TemplateAst {
    name: string;
    value: CompileTokenMetadata;
    sourceSpan: ParseSourceSpan;
    constructor(name: string, value: CompileTokenMetadata, sourceSpan: ParseSourceSpan);
    visit(visitor: TemplateAstVisitor, context: any): any;
}
/**
 * A variable declaration on a <ng-template> (e.g. `var-someName="someLocalName"`).
 */
export declare class VariableAst implements TemplateAst {
    name: string;
    value: string;
    sourceSpan: ParseSourceSpan;
    constructor(name: string, value: string, sourceSpan: ParseSourceSpan);
    visit(visitor: TemplateAstVisitor, context: any): any;
}
/**
 * An element declaration in a template.
 */
export declare class ElementAst implements TemplateAst {
    name: string;
    attrs: AttrAst[];
    inputs: BoundElementPropertyAst[];
    outputs: BoundEventAst[];
    references: ReferenceAst[];
    directives: DirectiveAst[];
    providers: ProviderAst[];
    hasViewContainer: boolean;
    queryMatches: QueryMatch[];
    children: TemplateAst[];
    ngContentIndex: number | null;
    sourceSpan: ParseSourceSpan;
    endSourceSpan: ParseSourceSpan | null;
    constructor(name: string, attrs: AttrAst[], inputs: BoundElementPropertyAst[], outputs: BoundEventAst[], references: ReferenceAst[], directives: DirectiveAst[], providers: ProviderAst[], hasViewContainer: boolean, queryMatches: QueryMatch[], children: TemplateAst[], ngContentIndex: number | null, sourceSpan: ParseSourceSpan, endSourceSpan: ParseSourceSpan | null);
    visit(visitor: TemplateAstVisitor, context: any): any;
}
/**
 * A `<ng-template>` element included in an Angular template.
 */
export declare class EmbeddedTemplateAst implements TemplateAst {
    attrs: AttrAst[];
    outputs: BoundEventAst[];
    references: ReferenceAst[];
    variables: VariableAst[];
    directives: DirectiveAst[];
    providers: ProviderAst[];
    hasViewContainer: boolean;
    queryMatches: QueryMatch[];
    children: TemplateAst[];
    ngContentIndex: number;
    sourceSpan: ParseSourceSpan;
    constructor(attrs: AttrAst[], outputs: BoundEventAst[], references: ReferenceAst[], variables: VariableAst[], directives: DirectiveAst[], providers: ProviderAst[], hasViewContainer: boolean, queryMatches: QueryMatch[], children: TemplateAst[], ngContentIndex: number, sourceSpan: ParseSourceSpan);
    visit(visitor: TemplateAstVisitor, context: any): any;
}
/**
 * A directive property with a bound value (e.g. `*ngIf="condition").
 */
export declare class BoundDirectivePropertyAst implements TemplateAst {
    directiveName: string;
    templateName: string;
    value: AST;
    sourceSpan: ParseSourceSpan;
    constructor(directiveName: string, templateName: string, value: AST, sourceSpan: ParseSourceSpan);
    visit(visitor: TemplateAstVisitor, context: any): any;
}
/**
 * A directive declared on an element.
 */
export declare class DirectiveAst implements TemplateAst {
    directive: CompileDirectiveSummary;
    inputs: BoundDirectivePropertyAst[];
    hostProperties: BoundElementPropertyAst[];
    hostEvents: BoundEventAst[];
    contentQueryStartId: number;
    sourceSpan: ParseSourceSpan;
    constructor(directive: CompileDirectiveSummary, inputs: BoundDirectivePropertyAst[], hostProperties: BoundElementPropertyAst[], hostEvents: BoundEventAst[], contentQueryStartId: number, sourceSpan: ParseSourceSpan);
    visit(visitor: TemplateAstVisitor, context: any): any;
}
/**
 * A provider declared on an element
 */
export declare class ProviderAst implements TemplateAst {
    token: CompileTokenMetadata;
    multiProvider: boolean;
    eager: boolean;
    providers: CompileProviderMetadata[];
    providerType: ProviderAstType;
    lifecycleHooks: LifecycleHooks[];
    sourceSpan: ParseSourceSpan;
    constructor(token: CompileTokenMetadata, multiProvider: boolean, eager: boolean, providers: CompileProviderMetadata[], providerType: ProviderAstType, lifecycleHooks: LifecycleHooks[], sourceSpan: ParseSourceSpan);
    visit(visitor: TemplateAstVisitor, context: any): any;
}
export declare enum ProviderAstType {
    PublicService = 0,
    PrivateService = 1,
    Component = 2,
    Directive = 3,
    Builtin = 4,
}
/**
 * Position where content is to be projected (instance of `<ng-content>` in a template).
 */
export declare class NgContentAst implements TemplateAst {
    index: number;
    ngContentIndex: number;
    sourceSpan: ParseSourceSpan;
    constructor(index: number, ngContentIndex: number, sourceSpan: ParseSourceSpan);
    visit(visitor: TemplateAstVisitor, context: any): any;
}
/**
 * Enumeration of types of property bindings.
 */
export declare enum PropertyBindingType {
    /**
     * A normal binding to a property (e.g. `[property]="expression"`).
     */
    Property = 0,
    /**
     * A binding to an element attribute (e.g. `[attr.name]="expression"`).
     */
    Attribute = 1,
    /**
     * A binding to a CSS class (e.g. `[class.name]="condition"`).
     */
    Class = 2,
    /**
     * A binding to a style rule (e.g. `[style.rule]="expression"`).
     */
    Style = 3,
    /**
     * A binding to an animation reference (e.g. `[animate.key]="expression"`).
     */
    Animation = 4,
}
export interface QueryMatch {
    queryId: number;
    value: CompileTokenMetadata;
}
/**
 * A visitor for {@link TemplateAst} trees that will process each node.
 */
export interface TemplateAstVisitor {
    visit?(ast: TemplateAst, context: any): any;
    visitNgContent(ast: NgContentAst, context: any): any;
    visitEmbeddedTemplate(ast: EmbeddedTemplateAst, context: any): any;
    visitElement(ast: ElementAst, context: any): any;
    visitReference(ast: ReferenceAst, context: any): any;
    visitVariable(ast: VariableAst, context: any): any;
    visitEvent(ast: BoundEventAst, context: any): any;
    visitElementProperty(ast: BoundElementPropertyAst, context: any): any;
    visitAttr(ast: AttrAst, context: any): any;
    visitBoundText(ast: BoundTextAst, context: any): any;
    visitText(ast: TextAst, context: any): any;
    visitDirective(ast: DirectiveAst, context: any): any;
    visitDirectiveProperty(ast: BoundDirectivePropertyAst, context: any): any;
}
/**
 * A visitor that accepts each node but doesn't do anything. It is intended to be used
 * as the base class for a visitor that is only interested in a subset of the node types.
 */
export declare class NullTemplateVisitor implements TemplateAstVisitor {
    visitNgContent(ast: NgContentAst, context: any): void;
    visitEmbeddedTemplate(ast: EmbeddedTemplateAst, context: any): void;
    visitElement(ast: ElementAst, context: any): void;
    visitReference(ast: ReferenceAst, context: any): void;
    visitVariable(ast: VariableAst, context: any): void;
    visitEvent(ast: BoundEventAst, context: any): void;
    visitElementProperty(ast: BoundElementPropertyAst, context: any): void;
    visitAttr(ast: AttrAst, context: any): void;
    visitBoundText(ast: BoundTextAst, context: any): void;
    visitText(ast: TextAst, context: any): void;
    visitDirective(ast: DirectiveAst, context: any): void;
    visitDirectiveProperty(ast: BoundDirectivePropertyAst, context: any): void;
}
/**
 * Base class that can be used to build a visitor that visits each node
 * in an template ast recursively.
 */
export declare class RecursiveTemplateAstVisitor extends NullTemplateVisitor implements TemplateAstVisitor {
    constructor();
    visitEmbeddedTemplate(ast: EmbeddedTemplateAst, context: any): any;
    visitElement(ast: ElementAst, context: any): any;
    visitDirective(ast: DirectiveAst, context: any): any;
    protected visitChildren<T extends TemplateAst>(context: any, cb: (visit: (<V extends TemplateAst>(children: V[] | undefined) => void)) => void): any;
}
/**
 * Visit every node in a list of {@link TemplateAst}s with the given {@link TemplateAstVisitor}.
 */
export declare function templateVisitAll(visitor: TemplateAstVisitor, asts: TemplateAst[], context?: any): any[];
export declare type TemplateAstPath = AstPath<TemplateAst>;
