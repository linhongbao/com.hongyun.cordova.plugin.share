var exec = require('cordova/exec');


module.exports = {
	show: function (message, win, fail) {
        exec(win, fail, 'Share', 'show', [message]);
	},
    share: function (message, win, fail) {
        exec(win, fail, 'Share', 'share', [message]);
    }
};