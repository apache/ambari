var App = require('app');

App.ManageJournalNodeWizardStep6Controller = App.ManageJournalNodeProgressPageController.extend({
  name: 'manageJournalNodeWizardStep6Controller',
  clusterDeployState: 'JOURNALNODE_MANAGEMENT',
  tasksMessagesPrefix: 'admin.manageJournalNode.wizard.step',

  commands: ['startServices']
});