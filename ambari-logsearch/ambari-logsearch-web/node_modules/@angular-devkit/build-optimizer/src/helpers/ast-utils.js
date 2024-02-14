"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
/**
 * @license
 * Copyright Google Inc. All Rights Reserved.
 *
 * Use of this source code is governed by an MIT-style license that can be
 * found in the LICENSE file at https://angular.io/license
 */
const ts = require("typescript");
// Find all nodes from the AST in the subtree of node of SyntaxKind kind.
function collectDeepNodes(node, kind) {
    const nodes = [];
    const helper = (child) => {
        if (child.kind === kind) {
            nodes.push(child);
        }
        ts.forEachChild(child, helper);
    };
    ts.forEachChild(node, helper);
    return nodes;
}
exports.collectDeepNodes = collectDeepNodes;
function drilldownNodes(startingNode, path) {
    let currentNode = startingNode;
    for (const segment of path) {
        if (segment.prop) {
            // ts.Node has no index signature, so we need to cast it as any.
            // tslint:disable-next-line:no-any
            currentNode = currentNode[segment.prop];
        }
        if (!currentNode || currentNode.kind !== segment.kind) {
            return null;
        }
    }
    return currentNode;
}
exports.drilldownNodes = drilldownNodes;
//# sourceMappingURL=data:application/json;base64,eyJ2ZXJzaW9uIjozLCJmaWxlIjoiYXN0LXV0aWxzLmpzIiwic291cmNlUm9vdCI6Ii9Vc2Vycy9oYW5zbC9Tb3VyY2VzL2RldmtpdC8iLCJzb3VyY2VzIjpbInBhY2thZ2VzL2FuZ3VsYXJfZGV2a2l0L2J1aWxkX29wdGltaXplci9zcmMvaGVscGVycy9hc3QtdXRpbHMudHMiXSwibmFtZXMiOltdLCJtYXBwaW5ncyI6Ijs7QUFBQTs7Ozs7O0dBTUc7QUFDSCxpQ0FBaUM7QUFFakMseUVBQXlFO0FBQ3pFLDBCQUFvRCxJQUFhLEVBQUUsSUFBbUI7SUFDcEYsTUFBTSxLQUFLLEdBQVEsRUFBRSxDQUFDO0lBQ3RCLE1BQU0sTUFBTSxHQUFHLENBQUMsS0FBYztRQUM1QixFQUFFLENBQUMsQ0FBQyxLQUFLLENBQUMsSUFBSSxLQUFLLElBQUksQ0FBQyxDQUFDLENBQUM7WUFDeEIsS0FBSyxDQUFDLElBQUksQ0FBQyxLQUFVLENBQUMsQ0FBQztRQUN6QixDQUFDO1FBQ0QsRUFBRSxDQUFDLFlBQVksQ0FBQyxLQUFLLEVBQUUsTUFBTSxDQUFDLENBQUM7SUFDakMsQ0FBQyxDQUFDO0lBQ0YsRUFBRSxDQUFDLFlBQVksQ0FBQyxJQUFJLEVBQUUsTUFBTSxDQUFDLENBQUM7SUFFOUIsTUFBTSxDQUFDLEtBQUssQ0FBQztBQUNmLENBQUM7QUFYRCw0Q0FXQztBQUVELHdCQUNFLFlBQXFCLEVBQ3JCLElBQW9EO0lBRXBELElBQUksV0FBVyxHQUE0QixZQUFZLENBQUM7SUFDeEQsR0FBRyxDQUFDLENBQUMsTUFBTSxPQUFPLElBQUksSUFBSSxDQUFDLENBQUMsQ0FBQztRQUMzQixFQUFFLENBQUMsQ0FBQyxPQUFPLENBQUMsSUFBSSxDQUFDLENBQUMsQ0FBQztZQUNqQixnRUFBZ0U7WUFDaEUsa0NBQWtDO1lBQ2xDLFdBQVcsR0FBSSxXQUFtQixDQUFDLE9BQU8sQ0FBQyxJQUFJLENBQUMsQ0FBQztRQUNuRCxDQUFDO1FBQ0QsRUFBRSxDQUFDLENBQUMsQ0FBQyxXQUFXLElBQUksV0FBVyxDQUFDLElBQUksS0FBSyxPQUFPLENBQUMsSUFBSSxDQUFDLENBQUMsQ0FBQztZQUN0RCxNQUFNLENBQUMsSUFBSSxDQUFDO1FBQ2QsQ0FBQztJQUNILENBQUM7SUFFRCxNQUFNLENBQUMsV0FBZ0IsQ0FBQztBQUMxQixDQUFDO0FBakJELHdDQWlCQyIsInNvdXJjZXNDb250ZW50IjpbIi8qKlxuICogQGxpY2Vuc2VcbiAqIENvcHlyaWdodCBHb29nbGUgSW5jLiBBbGwgUmlnaHRzIFJlc2VydmVkLlxuICpcbiAqIFVzZSBvZiB0aGlzIHNvdXJjZSBjb2RlIGlzIGdvdmVybmVkIGJ5IGFuIE1JVC1zdHlsZSBsaWNlbnNlIHRoYXQgY2FuIGJlXG4gKiBmb3VuZCBpbiB0aGUgTElDRU5TRSBmaWxlIGF0IGh0dHBzOi8vYW5ndWxhci5pby9saWNlbnNlXG4gKi9cbmltcG9ydCAqIGFzIHRzIGZyb20gJ3R5cGVzY3JpcHQnO1xuXG4vLyBGaW5kIGFsbCBub2RlcyBmcm9tIHRoZSBBU1QgaW4gdGhlIHN1YnRyZWUgb2Ygbm9kZSBvZiBTeW50YXhLaW5kIGtpbmQuXG5leHBvcnQgZnVuY3Rpb24gY29sbGVjdERlZXBOb2RlczxUIGV4dGVuZHMgdHMuTm9kZT4obm9kZTogdHMuTm9kZSwga2luZDogdHMuU3ludGF4S2luZCk6IFRbXSB7XG4gIGNvbnN0IG5vZGVzOiBUW10gPSBbXTtcbiAgY29uc3QgaGVscGVyID0gKGNoaWxkOiB0cy5Ob2RlKSA9PiB7XG4gICAgaWYgKGNoaWxkLmtpbmQgPT09IGtpbmQpIHtcbiAgICAgIG5vZGVzLnB1c2goY2hpbGQgYXMgVCk7XG4gICAgfVxuICAgIHRzLmZvckVhY2hDaGlsZChjaGlsZCwgaGVscGVyKTtcbiAgfTtcbiAgdHMuZm9yRWFjaENoaWxkKG5vZGUsIGhlbHBlcik7XG5cbiAgcmV0dXJuIG5vZGVzO1xufVxuXG5leHBvcnQgZnVuY3Rpb24gZHJpbGxkb3duTm9kZXM8VCBleHRlbmRzIHRzLk5vZGU+KFxuICBzdGFydGluZ05vZGU6IHRzLk5vZGUsXG4gIHBhdGg6IHsgcHJvcDogc3RyaW5nIHwgbnVsbCwga2luZDogdHMuU3ludGF4S2luZCB9W10sXG4pOiBUIHwgbnVsbCB7XG4gIGxldCBjdXJyZW50Tm9kZTogVCB8IHRzLk5vZGUgfCB1bmRlZmluZWQgPSBzdGFydGluZ05vZGU7XG4gIGZvciAoY29uc3Qgc2VnbWVudCBvZiBwYXRoKSB7XG4gICAgaWYgKHNlZ21lbnQucHJvcCkge1xuICAgICAgLy8gdHMuTm9kZSBoYXMgbm8gaW5kZXggc2lnbmF0dXJlLCBzbyB3ZSBuZWVkIHRvIGNhc3QgaXQgYXMgYW55LlxuICAgICAgLy8gdHNsaW50OmRpc2FibGUtbmV4dC1saW5lOm5vLWFueVxuICAgICAgY3VycmVudE5vZGUgPSAoY3VycmVudE5vZGUgYXMgYW55KVtzZWdtZW50LnByb3BdO1xuICAgIH1cbiAgICBpZiAoIWN1cnJlbnROb2RlIHx8IGN1cnJlbnROb2RlLmtpbmQgIT09IHNlZ21lbnQua2luZCkge1xuICAgICAgcmV0dXJuIG51bGw7XG4gICAgfVxuICB9XG5cbiAgcmV0dXJuIGN1cnJlbnROb2RlIGFzIFQ7XG59XG4iXX0=