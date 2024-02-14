import * as ts from 'typescript';
import { FileResolver } from './fileResolver/fileResolver';
import { AbstractResolver, MetadataUrls } from './urlResolvers/abstractResolver';
import { DirectiveMetadata, ComponentMetadata, TemplateMetadata, StylesMetadata } from './metadata';
import { Maybe } from '../util/function';
export declare class MetadataReader {
    private _fileResolver;
    private _urlResolver;
    constructor(_fileResolver: FileResolver, _urlResolver?: AbstractResolver);
    read(d: ts.ClassDeclaration): DirectiveMetadata;
    protected readDirectiveMetadata(d: ts.ClassDeclaration, dec: ts.Decorator): DirectiveMetadata;
    protected readComponentMetadata(d: ts.ClassDeclaration, dec: ts.Decorator): ComponentMetadata & DirectiveMetadata & {
        template: TemplateMetadata;
        styles: StylesMetadata;
    };
    protected getDecoratorArgument(decorator: ts.Decorator): Maybe<ts.ObjectLiteralExpression>;
    protected readComponentTemplateMetadata(dec: ts.Decorator, external: MetadataUrls): Maybe<TemplateMetadata>;
    protected readComponentStylesMetadata(dec: ts.Decorator, external: MetadataUrls): Maybe<StylesMetadata>;
    private _resolve(url);
}
