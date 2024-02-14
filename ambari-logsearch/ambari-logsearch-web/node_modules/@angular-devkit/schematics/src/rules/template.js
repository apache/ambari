"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const exception_1 = require("../exception/exception");
const path_1 = require("../utility/path");
const base_1 = require("./base");
const template_1 = require("./template/template");
const is_binary_1 = require("./utils/is-binary");
class OptionIsNotDefinedException extends exception_1.BaseException {
    constructor(name) { super(`Option "${name}" is not defined.`); }
}
exports.OptionIsNotDefinedException = OptionIsNotDefinedException;
class UnknownPipeException extends exception_1.BaseException {
    constructor(name) { super(`Pipe "${name}" is not defined.`); }
}
exports.UnknownPipeException = UnknownPipeException;
class InvalidPipeException extends exception_1.BaseException {
    constructor(name) { super(`Pipe "${name}" is invalid.`); }
}
exports.InvalidPipeException = InvalidPipeException;
exports.kPathTemplateComponentRE = /__([^_]+)__/g;
exports.kPathTemplatePipeRE = /@([^@]+)/;
function applyContentTemplate(options) {
    return (entry) => {
        const { path, content } = entry;
        if (is_binary_1.isBinary(content)) {
            return entry;
        }
        return {
            path: path,
            content: new Buffer(template_1.template(content.toString('utf-8'), {})(options)),
        };
    };
}
exports.applyContentTemplate = applyContentTemplate;
function contentTemplate(options) {
    return base_1.forEach(applyContentTemplate(options));
}
exports.contentTemplate = contentTemplate;
function applyPathTemplate(options) {
    return (entry) => {
        let path = entry.path;
        const content = entry.content;
        const original = path;
        // Path template.
        path = path_1.normalizePath(path.replace(exports.kPathTemplateComponentRE, (_, match) => {
            const [name, ...pipes] = match.split(exports.kPathTemplatePipeRE);
            const value = typeof options[name] == 'function'
                ? options[name].call(options, original)
                : options[name];
            if (value === undefined) {
                throw new OptionIsNotDefinedException(name);
            }
            return pipes.reduce((acc, pipe) => {
                if (!pipe) {
                    return acc;
                }
                if (!(pipe in options)) {
                    throw new UnknownPipeException(pipe);
                }
                if (typeof options[pipe] != 'function') {
                    throw new InvalidPipeException(pipe);
                }
                // Coerce to string.
                return '' + options[pipe](acc);
            }, '' + value);
        }));
        return { path, content };
    };
}
exports.applyPathTemplate = applyPathTemplate;
function pathTemplate(options) {
    return base_1.forEach(applyPathTemplate(options));
}
exports.pathTemplate = pathTemplate;
function template(options) {
    return base_1.chain([
        contentTemplate(options),
        pathTemplate(options),
    ]);
}
exports.template = template;
//# sourceMappingURL=data:application/json;base64,eyJ2ZXJzaW9uIjozLCJmaWxlIjoidGVtcGxhdGUuanMiLCJzb3VyY2VSb290IjoiL1VzZXJzL2hhbnNsL1NvdXJjZXMvZGV2a2l0LyIsInNvdXJjZXMiOlsicGFja2FnZXMvYW5ndWxhcl9kZXZraXQvc2NoZW1hdGljcy9zcmMvcnVsZXMvdGVtcGxhdGUudHMiXSwibmFtZXMiOltdLCJtYXBwaW5ncyI6Ijs7QUFRQSxzREFBdUQ7QUFFdkQsMENBQWdEO0FBQ2hELGlDQUF3QztBQUN4QyxrREFBOEQ7QUFDOUQsaURBQTZDO0FBRzdDLGlDQUF5QyxTQUFRLHlCQUFhO0lBQzVELFlBQVksSUFBWSxJQUFJLEtBQUssQ0FBQyxXQUFXLElBQUksbUJBQW1CLENBQUMsQ0FBQyxDQUFDLENBQUM7Q0FDekU7QUFGRCxrRUFFQztBQUdELDBCQUFrQyxTQUFRLHlCQUFhO0lBQ3JELFlBQVksSUFBWSxJQUFJLEtBQUssQ0FBQyxTQUFTLElBQUksbUJBQW1CLENBQUMsQ0FBQyxDQUFDLENBQUM7Q0FDdkU7QUFGRCxvREFFQztBQUdELDBCQUFrQyxTQUFRLHlCQUFhO0lBQ3JELFlBQVksSUFBWSxJQUFJLEtBQUssQ0FBQyxTQUFTLElBQUksZUFBZSxDQUFDLENBQUMsQ0FBQyxDQUFDO0NBQ25FO0FBRkQsb0RBRUM7QUFHWSxRQUFBLHdCQUF3QixHQUFHLGNBQWMsQ0FBQztBQUMxQyxRQUFBLG1CQUFtQixHQUFHLFVBQVUsQ0FBQztBQVU5Qyw4QkFBZ0UsT0FBVTtJQUN4RSxNQUFNLENBQUMsQ0FBQyxLQUFnQjtRQUN0QixNQUFNLEVBQUMsSUFBSSxFQUFFLE9BQU8sRUFBQyxHQUFHLEtBQUssQ0FBQztRQUM5QixFQUFFLENBQUMsQ0FBQyxvQkFBUSxDQUFDLE9BQU8sQ0FBQyxDQUFDLENBQUMsQ0FBQztZQUN0QixNQUFNLENBQUMsS0FBSyxDQUFDO1FBQ2YsQ0FBQztRQUVELE1BQU0sQ0FBQztZQUNMLElBQUksRUFBRSxJQUFJO1lBQ1YsT0FBTyxFQUFFLElBQUksTUFBTSxDQUFDLG1CQUFZLENBQUMsT0FBTyxDQUFDLFFBQVEsQ0FBQyxPQUFPLENBQUMsRUFBRSxFQUFFLENBQUMsQ0FBQyxPQUFPLENBQUMsQ0FBQztTQUMxRSxDQUFDO0lBQ0osQ0FBQyxDQUFDO0FBQ0osQ0FBQztBQVpELG9EQVlDO0FBR0QseUJBQTJELE9BQVU7SUFDbkUsTUFBTSxDQUFDLGNBQU8sQ0FBQyxvQkFBb0IsQ0FBQyxPQUFPLENBQUMsQ0FBQyxDQUFDO0FBQ2hELENBQUM7QUFGRCwwQ0FFQztBQUdELDJCQUE2RCxPQUFVO0lBQ3JFLE1BQU0sQ0FBQyxDQUFDLEtBQWdCO1FBQ3RCLElBQUksSUFBSSxHQUFHLEtBQUssQ0FBQyxJQUFJLENBQUM7UUFDdEIsTUFBTSxPQUFPLEdBQUcsS0FBSyxDQUFDLE9BQU8sQ0FBQztRQUM5QixNQUFNLFFBQVEsR0FBRyxJQUFJLENBQUM7UUFFdEIsaUJBQWlCO1FBQ2pCLElBQUksR0FBRyxvQkFBYSxDQUFDLElBQUksQ0FBQyxPQUFPLENBQUMsZ0NBQXdCLEVBQUUsQ0FBQyxDQUFDLEVBQUUsS0FBSztZQUNuRSxNQUFNLENBQUMsSUFBSSxFQUFFLEdBQUcsS0FBSyxDQUFDLEdBQUcsS0FBSyxDQUFDLEtBQUssQ0FBQywyQkFBbUIsQ0FBQyxDQUFDO1lBQzFELE1BQU0sS0FBSyxHQUFHLE9BQU8sT0FBTyxDQUFDLElBQUksQ0FBQyxJQUFJLFVBQVU7a0JBQzNDLE9BQU8sQ0FBQyxJQUFJLENBQTBCLENBQUMsSUFBSSxDQUFDLE9BQU8sRUFBRSxRQUFRLENBQUM7a0JBQy9ELE9BQU8sQ0FBQyxJQUFJLENBQUMsQ0FBQztZQUVsQixFQUFFLENBQUMsQ0FBQyxLQUFLLEtBQUssU0FBUyxDQUFDLENBQUMsQ0FBQztnQkFDeEIsTUFBTSxJQUFJLDJCQUEyQixDQUFDLElBQUksQ0FBQyxDQUFDO1lBQzlDLENBQUM7WUFFRCxNQUFNLENBQUMsS0FBSyxDQUFDLE1BQU0sQ0FBQyxDQUFDLEdBQVcsRUFBRSxJQUFZO2dCQUM1QyxFQUFFLENBQUMsQ0FBQyxDQUFDLElBQUksQ0FBQyxDQUFDLENBQUM7b0JBQ1YsTUFBTSxDQUFDLEdBQUcsQ0FBQztnQkFDYixDQUFDO2dCQUNELEVBQUUsQ0FBQyxDQUFDLENBQUMsQ0FBQyxJQUFJLElBQUksT0FBTyxDQUFDLENBQUMsQ0FBQyxDQUFDO29CQUN2QixNQUFNLElBQUksb0JBQW9CLENBQUMsSUFBSSxDQUFDLENBQUM7Z0JBQ3ZDLENBQUM7Z0JBQ0QsRUFBRSxDQUFDLENBQUMsT0FBTyxPQUFPLENBQUMsSUFBSSxDQUFDLElBQUksVUFBVSxDQUFDLENBQUMsQ0FBQztvQkFDdkMsTUFBTSxJQUFJLG9CQUFvQixDQUFDLElBQUksQ0FBQyxDQUFDO2dCQUN2QyxDQUFDO2dCQUVELG9CQUFvQjtnQkFDcEIsTUFBTSxDQUFDLEVBQUUsR0FBSSxPQUFPLENBQUMsSUFBSSxDQUEwQixDQUFDLEdBQUcsQ0FBQyxDQUFDO1lBQzNELENBQUMsRUFBRSxFQUFFLEdBQUcsS0FBSyxDQUFDLENBQUM7UUFDakIsQ0FBQyxDQUFDLENBQUMsQ0FBQztRQUVKLE1BQU0sQ0FBQyxFQUFFLElBQUksRUFBRSxPQUFPLEVBQUUsQ0FBQztJQUMzQixDQUFDLENBQUM7QUFDSixDQUFDO0FBbkNELDhDQW1DQztBQUdELHNCQUF3RCxPQUFVO0lBQ2hFLE1BQU0sQ0FBQyxjQUFPLENBQUMsaUJBQWlCLENBQUMsT0FBTyxDQUFDLENBQUMsQ0FBQztBQUM3QyxDQUFDO0FBRkQsb0NBRUM7QUFHRCxrQkFBb0QsT0FBVTtJQUM1RCxNQUFNLENBQUMsWUFBSyxDQUFDO1FBQ1gsZUFBZSxDQUFDLE9BQU8sQ0FBQztRQUN4QixZQUFZLENBQUMsT0FBTyxDQUFDO0tBQ3RCLENBQUMsQ0FBQztBQUNMLENBQUM7QUFMRCw0QkFLQyIsInNvdXJjZXNDb250ZW50IjpbIi8qKlxuICogQGxpY2Vuc2VcbiAqIENvcHlyaWdodCBHb29nbGUgSW5jLiBBbGwgUmlnaHRzIFJlc2VydmVkLlxuICpcbiAqIFVzZSBvZiB0aGlzIHNvdXJjZSBjb2RlIGlzIGdvdmVybmVkIGJ5IGFuIE1JVC1zdHlsZSBsaWNlbnNlIHRoYXQgY2FuIGJlXG4gKiBmb3VuZCBpbiB0aGUgTElDRU5TRSBmaWxlIGF0IGh0dHBzOi8vYW5ndWxhci5pby9saWNlbnNlXG4gKi9cbmltcG9ydCB7IEZpbGVPcGVyYXRvciwgUnVsZSB9IGZyb20gJy4uL2VuZ2luZS9pbnRlcmZhY2UnO1xuaW1wb3J0IHsgQmFzZUV4Y2VwdGlvbiB9IGZyb20gJy4uL2V4Y2VwdGlvbi9leGNlcHRpb24nO1xuaW1wb3J0IHsgRmlsZUVudHJ5IH0gZnJvbSAnLi4vdHJlZS9pbnRlcmZhY2UnO1xuaW1wb3J0IHsgbm9ybWFsaXplUGF0aCB9IGZyb20gJy4uL3V0aWxpdHkvcGF0aCc7XG5pbXBvcnQgeyBjaGFpbiwgZm9yRWFjaCB9IGZyb20gJy4vYmFzZSc7XG5pbXBvcnQge3RlbXBsYXRlIGFzIHRlbXBsYXRlSW1wbCB9IGZyb20gJy4vdGVtcGxhdGUvdGVtcGxhdGUnO1xuaW1wb3J0IHsgaXNCaW5hcnkgfSBmcm9tICcuL3V0aWxzL2lzLWJpbmFyeSc7XG5cblxuZXhwb3J0IGNsYXNzIE9wdGlvbklzTm90RGVmaW5lZEV4Y2VwdGlvbiBleHRlbmRzIEJhc2VFeGNlcHRpb24ge1xuICBjb25zdHJ1Y3RvcihuYW1lOiBzdHJpbmcpIHsgc3VwZXIoYE9wdGlvbiBcIiR7bmFtZX1cIiBpcyBub3QgZGVmaW5lZC5gKTsgfVxufVxuXG5cbmV4cG9ydCBjbGFzcyBVbmtub3duUGlwZUV4Y2VwdGlvbiBleHRlbmRzIEJhc2VFeGNlcHRpb24ge1xuICBjb25zdHJ1Y3RvcihuYW1lOiBzdHJpbmcpIHsgc3VwZXIoYFBpcGUgXCIke25hbWV9XCIgaXMgbm90IGRlZmluZWQuYCk7IH1cbn1cblxuXG5leHBvcnQgY2xhc3MgSW52YWxpZFBpcGVFeGNlcHRpb24gZXh0ZW5kcyBCYXNlRXhjZXB0aW9uIHtcbiAgY29uc3RydWN0b3IobmFtZTogc3RyaW5nKSB7IHN1cGVyKGBQaXBlIFwiJHtuYW1lfVwiIGlzIGludmFsaWQuYCk7IH1cbn1cblxuXG5leHBvcnQgY29uc3Qga1BhdGhUZW1wbGF0ZUNvbXBvbmVudFJFID0gL19fKFteX10rKV9fL2c7XG5leHBvcnQgY29uc3Qga1BhdGhUZW1wbGF0ZVBpcGVSRSA9IC9AKFteQF0rKS87XG5cblxuZXhwb3J0IHR5cGUgVGVtcGxhdGVWYWx1ZSA9IGJvb2xlYW4gfCBzdHJpbmcgfCBudW1iZXI7XG5leHBvcnQgdHlwZSBUZW1wbGF0ZVBpcGVGdW5jdGlvbiA9ICh4OiBzdHJpbmcpID0+IFRlbXBsYXRlVmFsdWU7XG5leHBvcnQgdHlwZSBUZW1wbGF0ZU9wdGlvbnMgPSB7XG4gIFtrZXk6IHN0cmluZ106IFRlbXBsYXRlVmFsdWUgfCBUZW1wbGF0ZU9wdGlvbnMgfCBUZW1wbGF0ZVBpcGVGdW5jdGlvbixcbn07XG5cblxuZXhwb3J0IGZ1bmN0aW9uIGFwcGx5Q29udGVudFRlbXBsYXRlPFQgZXh0ZW5kcyBUZW1wbGF0ZU9wdGlvbnM+KG9wdGlvbnM6IFQpOiBGaWxlT3BlcmF0b3Ige1xuICByZXR1cm4gKGVudHJ5OiBGaWxlRW50cnkpID0+IHtcbiAgICBjb25zdCB7cGF0aCwgY29udGVudH0gPSBlbnRyeTtcbiAgICBpZiAoaXNCaW5hcnkoY29udGVudCkpIHtcbiAgICAgIHJldHVybiBlbnRyeTtcbiAgICB9XG5cbiAgICByZXR1cm4ge1xuICAgICAgcGF0aDogcGF0aCxcbiAgICAgIGNvbnRlbnQ6IG5ldyBCdWZmZXIodGVtcGxhdGVJbXBsKGNvbnRlbnQudG9TdHJpbmcoJ3V0Zi04JyksIHt9KShvcHRpb25zKSksXG4gICAgfTtcbiAgfTtcbn1cblxuXG5leHBvcnQgZnVuY3Rpb24gY29udGVudFRlbXBsYXRlPFQgZXh0ZW5kcyBUZW1wbGF0ZU9wdGlvbnM+KG9wdGlvbnM6IFQpOiBSdWxlIHtcbiAgcmV0dXJuIGZvckVhY2goYXBwbHlDb250ZW50VGVtcGxhdGUob3B0aW9ucykpO1xufVxuXG5cbmV4cG9ydCBmdW5jdGlvbiBhcHBseVBhdGhUZW1wbGF0ZTxUIGV4dGVuZHMgVGVtcGxhdGVPcHRpb25zPihvcHRpb25zOiBUKTogRmlsZU9wZXJhdG9yIHtcbiAgcmV0dXJuIChlbnRyeTogRmlsZUVudHJ5KSA9PiB7XG4gICAgbGV0IHBhdGggPSBlbnRyeS5wYXRoO1xuICAgIGNvbnN0IGNvbnRlbnQgPSBlbnRyeS5jb250ZW50O1xuICAgIGNvbnN0IG9yaWdpbmFsID0gcGF0aDtcblxuICAgIC8vIFBhdGggdGVtcGxhdGUuXG4gICAgcGF0aCA9IG5vcm1hbGl6ZVBhdGgocGF0aC5yZXBsYWNlKGtQYXRoVGVtcGxhdGVDb21wb25lbnRSRSwgKF8sIG1hdGNoKSA9PiB7XG4gICAgICBjb25zdCBbbmFtZSwgLi4ucGlwZXNdID0gbWF0Y2guc3BsaXQoa1BhdGhUZW1wbGF0ZVBpcGVSRSk7XG4gICAgICBjb25zdCB2YWx1ZSA9IHR5cGVvZiBvcHRpb25zW25hbWVdID09ICdmdW5jdGlvbidcbiAgICAgICAgPyAob3B0aW9uc1tuYW1lXSBhcyBUZW1wbGF0ZVBpcGVGdW5jdGlvbikuY2FsbChvcHRpb25zLCBvcmlnaW5hbClcbiAgICAgICAgOiBvcHRpb25zW25hbWVdO1xuXG4gICAgICBpZiAodmFsdWUgPT09IHVuZGVmaW5lZCkge1xuICAgICAgICB0aHJvdyBuZXcgT3B0aW9uSXNOb3REZWZpbmVkRXhjZXB0aW9uKG5hbWUpO1xuICAgICAgfVxuXG4gICAgICByZXR1cm4gcGlwZXMucmVkdWNlKChhY2M6IHN0cmluZywgcGlwZTogc3RyaW5nKSA9PiB7XG4gICAgICAgIGlmICghcGlwZSkge1xuICAgICAgICAgIHJldHVybiBhY2M7XG4gICAgICAgIH1cbiAgICAgICAgaWYgKCEocGlwZSBpbiBvcHRpb25zKSkge1xuICAgICAgICAgIHRocm93IG5ldyBVbmtub3duUGlwZUV4Y2VwdGlvbihwaXBlKTtcbiAgICAgICAgfVxuICAgICAgICBpZiAodHlwZW9mIG9wdGlvbnNbcGlwZV0gIT0gJ2Z1bmN0aW9uJykge1xuICAgICAgICAgIHRocm93IG5ldyBJbnZhbGlkUGlwZUV4Y2VwdGlvbihwaXBlKTtcbiAgICAgICAgfVxuXG4gICAgICAgIC8vIENvZXJjZSB0byBzdHJpbmcuXG4gICAgICAgIHJldHVybiAnJyArIChvcHRpb25zW3BpcGVdIGFzIFRlbXBsYXRlUGlwZUZ1bmN0aW9uKShhY2MpO1xuICAgICAgfSwgJycgKyB2YWx1ZSk7XG4gICAgfSkpO1xuXG4gICAgcmV0dXJuIHsgcGF0aCwgY29udGVudCB9O1xuICB9O1xufVxuXG5cbmV4cG9ydCBmdW5jdGlvbiBwYXRoVGVtcGxhdGU8VCBleHRlbmRzIFRlbXBsYXRlT3B0aW9ucz4ob3B0aW9uczogVCk6IFJ1bGUge1xuICByZXR1cm4gZm9yRWFjaChhcHBseVBhdGhUZW1wbGF0ZShvcHRpb25zKSk7XG59XG5cblxuZXhwb3J0IGZ1bmN0aW9uIHRlbXBsYXRlPFQgZXh0ZW5kcyBUZW1wbGF0ZU9wdGlvbnM+KG9wdGlvbnM6IFQpOiBSdWxlIHtcbiAgcmV0dXJuIGNoYWluKFtcbiAgICBjb250ZW50VGVtcGxhdGUob3B0aW9ucyksXG4gICAgcGF0aFRlbXBsYXRlKG9wdGlvbnMpLFxuICBdKTtcbn1cbiJdfQ==