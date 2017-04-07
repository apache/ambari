import { AmbariLogsearchWebNewPage } from './app.po';

describe('ambari-logsearch-web-new App', () => {
  let page: AmbariLogsearchWebNewPage;

  beforeEach(() => {
    page = new AmbariLogsearchWebNewPage();
  });

  it('should display message saying app works', () => {
    page.navigateTo();
    expect(page.getParagraphText()).toEqual('app works!');
  });
});
