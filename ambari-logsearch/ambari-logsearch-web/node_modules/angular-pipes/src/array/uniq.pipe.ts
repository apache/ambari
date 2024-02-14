import { Pipe, PipeTransform  } from '@angular/core';
import { isArray, isDeepObject, unwrapDeep, deepIndexOf } from '../utils/utils';

@Pipe({
  name: 'uniq'
})
export class UniqPipe implements PipeTransform {
  
  transform (input: any): any {
    
    if (!isArray(input) && !isDeepObject(input)) {
      return input;
    }
    
    if (isDeepObject(input)) {
      const unwrappedInput = unwrapDeep(input);
      if (!isArray(unwrappedInput)) {
        return unwrappedInput;
      }
      
      return unwrappedInput.filter((value: any, index: number) => 
        deepIndexOf(unwrappedInput, value) === index
      );
    }
    
    return input.filter((value: any, index: number) => input.indexOf(value) === index);
  }
}