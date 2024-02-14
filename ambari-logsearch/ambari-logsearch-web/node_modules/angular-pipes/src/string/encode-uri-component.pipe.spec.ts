import { EncodeURIComponentPipe } from './encode-uri-component.pipe';
        


describe('EncodeURIComponentPipe', () => {
    
    let pipe: EncodeURIComponentPipe;
    
    beforeEach(() => {
       pipe = new EncodeURIComponentPipe(); 
    });
    
    it('Should return the value encoded', () => {
        const url = 'https://test.com';
        expect(pipe.transform(url)).toEqual(encodeURIComponent(url));
    });

    it('Should return the value unchanged', () => {
       
       expect(pipe.transform(1)).toEqual(1); 
    });
});