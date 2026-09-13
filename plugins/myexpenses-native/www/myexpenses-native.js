var exec = require('cordova/exec');
window.MyExpensesNative = {
  printHtml: function(title, html, success, error) {
    exec(success || function(){}, error || function(){}, 'MyExpensesNative', 'printHtml', [title || 'MyExpenses', html || '']);
  },
  saveBackup: function(filename, json, success, error) {
    exec(success || function(){}, error || function(){}, 'MyExpensesNative', 'saveBackup', [filename || 'MyExpenses_Backup.json', json || '']);
  }
};
