import { Pipe, PipeTransform  } from '@angular/core';
import { rightPad, isString } from '../utils/utils';


@Pipe({
  name: 'rightpad'
})
export class RightPadPipe implements PipeTransform {
  
  transform (input: any, length: number = 0, character: string = ' '): any {
    
    if (!isString(input)) {
      return input;
    }
    
    return rightPad(input, length, character);
  }
}