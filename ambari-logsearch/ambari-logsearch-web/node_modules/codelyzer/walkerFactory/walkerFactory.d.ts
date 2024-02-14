import * as ts from 'typescript';
import { Ng2Walker } from '../angular/ng2Walker';
import { IOptions } from 'tslint';
import { ComponentMetadata } from '../angular/metadata';
import { F1, Maybe } from '../util/function';
export declare type Walkable = 'Ng2Component';
export declare function allNg2Component(): WalkerBuilder<'Ng2Component'>;
export declare class Failure {
    node: ts.Node;
    message: string;
    constructor(node: ts.Node, message: string);
}
export interface WalkerBuilder<T extends Walkable> {
    where: (validate: F1<ComponentMetadata, Maybe<Failure>>) => WalkerBuilder<T>;
    build: (sourceFile: ts.SourceFile, options: IOptions) => Ng2Walker;
}
