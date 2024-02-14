import { MaxPipe } from './max.pipe';
    
describe('MaxPipe', () => {
  
  let pipe: MaxPipe;
  
  beforeEach(() => {
     pipe = new MaxPipe(); 
  });
  
  it('Should return 4', () => {
    
    expect(pipe.transform([1,2,3,4])).toEqual(4);
  });
  
  it('Should return 1', () => {
    
    expect(pipe.transform([1])).toEqual(1);
  });
  
  it('Should return 1', () => {
    
    expect(pipe.transform([1,1])).toEqual(1);
  });
  
  it('Should return the value unchanged', () => {
     
     expect(pipe.transform(1)).toEqual(1); 
  });

  it('Should return undefined', () => {
    expect(pipe.transform([])).toBeUndefined();
  });
});