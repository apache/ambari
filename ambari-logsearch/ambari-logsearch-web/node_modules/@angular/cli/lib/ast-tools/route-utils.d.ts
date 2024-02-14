import { Change } from './change';
import { Host } from './change';
/**
 * Adds imports to mainFile and adds toBootstrap to the array of providers
 * in bootstrap, if not present
 * @param mainFile main.ts
 * @param imports Object { importedClass: ['path/to/import/from', defaultStyleImport?] }
 * @param toBootstrap
 */
export declare function bootstrapItem(mainFile: string, imports: {
    [key: string]: (string | boolean)[];
}, toBootstrap: string): Change[];
/**
* Add Import `import { symbolName } from fileName` if the import doesn't exit
* already. Assumes fileToEdit can be resolved and accessed.
* @param fileToEdit (file we want to add import to)
* @param symbolName (item to import)
* @param fileName (path to the file)
* @param isDefault (if true, import follows style for importing default exports)
* @return Change
*/
export declare function insertImport(fileToEdit: string, symbolName: string, fileName: string, isDefault?: boolean): Change;
/**
 * Inserts a path to the new route into src/routes.ts if it doesn't exist
 * @param routesFile
 * @param pathOptions
 * @return Change[]
 * @throws Error if routesFile has multiple export default or none.
 */
export declare function addPathToRoutes(routesFile: string, pathOptions: any): Change[];
/**
 * Add more properties to the route object in routes.ts
 * @param routesFile routes.ts
 * @param routes Object {route: [key, value]}
 */
export declare function addItemsToRouteProperties(routesFile: string, routes: {
    [key: string]: string[];
}): Change[];
/**
 * Verifies that a component file exports a class of the component
 * @param file
 * @param componentName
 * @return whether file exports componentName
 */
export declare function confirmComponentExport(file: string, componentName: string): boolean;
/**
 * Resolve a path to a component file. If the path begins with path.sep, it is treated to be
 * absolute from the app/ directory. Otherwise, it is relative to currDir
 * @param projectRoot
 * @param currentDir
 * @param filePath componentName or path to componentName
 * @return component file name
 * @throw Error if component file referenced by path is not found
 */
export declare function resolveComponentPath(projectRoot: string, currentDir: string, filePath: string): string;
/**
 * Sort changes in decreasing order and apply them.
 * @param changes
 * @param host
 * @return Promise
 */
export declare function applyChanges(changes: Change[], host?: Host): Promise<void>;
