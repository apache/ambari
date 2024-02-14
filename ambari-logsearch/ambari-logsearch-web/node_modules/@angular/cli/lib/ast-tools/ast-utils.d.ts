import * as ts from 'typescript';
import { Change } from './change';
import { Observable } from 'rxjs/Observable';
import 'rxjs/add/observable/empty';
import 'rxjs/add/observable/of';
import 'rxjs/add/operator/do';
import 'rxjs/add/operator/filter';
import 'rxjs/add/operator/last';
import 'rxjs/add/operator/map';
import 'rxjs/add/operator/mergeMap';
import 'rxjs/add/operator/toArray';
import 'rxjs/add/operator/toPromise';
/**
* Get TS source file based on path.
* @param filePath
* @return source file of ts.SourceFile kind
*/
export declare function getSource(filePath: string): ts.SourceFile;
/**
 * Get all the nodes from a source, as an observable.
 * @param sourceFile The source file object.
 * @returns {Observable<ts.Node>} An observable of all the nodes in the source.
 */
export declare function getSourceNodes(sourceFile: ts.SourceFile): Observable<ts.Node>;
/**
 * Insert `toInsert` after the last occurence of `ts.SyntaxKind[nodes[i].kind]`
 * or after the last of occurence of `syntaxKind` if the last occurence is a sub child
 * of ts.SyntaxKind[nodes[i].kind] and save the changes in file.
 *
 * @param nodes insert after the last occurence of nodes
 * @param toInsert string to insert
 * @param file file to insert changes into
 * @param fallbackPos position to insert if toInsert happens to be the first occurence
 * @param syntaxKind the ts.SyntaxKind of the subchildren to insert after
 * @return Change instance
 * @throw Error if toInsert is first occurence but fall back is not set
 */
export declare function insertAfterLastOccurrence(nodes: ts.Node[], toInsert: string, file: string, fallbackPos?: number, syntaxKind?: ts.SyntaxKind): Change;
export declare function getContentOfKeyLiteral(_source: ts.SourceFile, node: ts.Node): string;
export declare function getDecoratorMetadata(source: ts.SourceFile, identifier: string, module: string): Observable<ts.Node>;
/**
* Custom function to insert a declaration (component, pipe, directive)
* into NgModule declarations. It also imports the component.
*/
export declare function addDeclarationToModule(modulePath: string, classifiedName: string, importPath: string): Promise<Change>;
/**
 * Custom function to insert a declaration (component, pipe, directive)
 * into NgModule declarations. It also imports the component.
 */
export declare function addImportToModule(modulePath: string, classifiedName: string, importPath: string): Promise<Change>;
/**
 * Custom function to insert a provider into NgModule. It also imports it.
 */
export declare function addProviderToModule(modulePath: string, classifiedName: string, importPath: string): Promise<Change>;
/**
 * Custom function to insert an export into NgModule. It also imports it.
 */
export declare function addExportToModule(modulePath: string, classifiedName: string, importPath: string): Promise<Change>;
