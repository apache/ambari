import { EveryPipe } from './every.pipe';


describe('MapPipe', () => {
  
  let pipe: EveryPipe;
  
  const fn = function (item: any) {
    return item === 2;
  };
  
  beforeEach(() => {
    pipe = new EveryPipe(); 
  });
  
  it('Should return false', () => {
    
    const array = [0, 1, 2, 3];
    
    expect(pipe.transform(array, fn)).toEqual(false); 
    expect(array).toEqual([0, 1, 2, 3]); // Check integrity
  });
  
  it('Should return true', () => {
    
    expect(pipe.transform([2, 2, 2], fn)).toEqual(true); 
  });
  
  it('Should return the value unchanged', () => {
    
    expect(pipe.transform('a', null)).toEqual('a'); 
  });
  
})