import * as ts from 'typescript';
import { RawSourceMap } from 'source-map';
export interface CodeWithSourceMap {
    code: string;
    source?: string;
    map?: RawSourceMap;
}
export interface TemplateMetadata {
    template: CodeWithSourceMap;
    node: ts.Node;
    url: string;
}
export interface StyleMetadata {
    style: CodeWithSourceMap;
    node: ts.Node;
    url: string;
}
export interface StylesMetadata {
    [index: number]: StyleMetadata;
    length: number;
    push(e: StyleMetadata): number;
}
export declare class DirectiveMetadata {
    selector: string;
    controller: ts.ClassDeclaration;
    decorator: ts.Decorator;
}
export declare class ComponentMetadata extends DirectiveMetadata {
    template: TemplateMetadata;
    styles: StylesMetadata;
}
