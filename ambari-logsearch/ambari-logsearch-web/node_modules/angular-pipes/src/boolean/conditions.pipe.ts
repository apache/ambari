import { Pipe, PipeTransform  } from '@angular/core';


@Pipe({
  name: 'greater'
})
export class IsGreaterPipe implements PipeTransform {
  
  transform (first: any, second: any): boolean {
    
    return first > second;
  }
}

@Pipe({
  name: 'greaterOrEqual'
})
export class IsGreaterOrEqualPipe implements PipeTransform {
  
  transform (first: any, second: any): boolean {
    
    return first >= second;
  }
}

@Pipe({
  name: 'less'
})
export class IsLessPipe implements PipeTransform {
  
  transform (first: any, second: any): boolean {
    
    return first < second;
  }
}

@Pipe({
  name: 'lessOrEqual'
})
export class IsLessOrEqualPipe implements PipeTransform {
  
  transform (first: any, second: any): boolean {
    
    return first <= second;
  }
}

@Pipe({
  name: 'equal'
})
export class IsEqualPipe implements PipeTransform {
  
  transform (first: any, second: any): boolean {
    
    return first == second;
  }
}

@Pipe({
  name: 'notEqual'
})
export class IsNotEqualPipe implements PipeTransform {
  
  transform (first: any, second: any): boolean {
    
    return first != second;
  }
}

@Pipe({
  name: 'identical'
})
export class IsIdenticalPipe implements PipeTransform {
  
  transform (first: any, second: any): boolean {
    
    return first === second;
  }
}

@Pipe({
  name: 'notIdentical'
})
export class IsNotIdenticalPipe implements PipeTransform {
  
  transform (first: any, second: any): boolean {
    
    return first !== second;
  }
}

