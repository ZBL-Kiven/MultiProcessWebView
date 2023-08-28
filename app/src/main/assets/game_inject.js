javascript:(function(ns){ 'use strict';ns.apptype = 'simple';ns.apihost = 'core.ccdev.lerjin.com';ns.token = 'MTE1N2U4YzItMWM4Zi00ZTQwLTk2YTQtYzgwZGY3MWI2MWFm';ns.user = { "avatar":"", "code":"6894749405", "country":"PH", "id":"117916", "lang":"zh", "nickname":"ccc", "paypal":"" };ns.asset = { "cash":0.12, "gold":100 };ns.appver = '2.6.1.2';ns.uuid = 'f7a7a41667afbcd9';ns.capsule = { "height":93, "right":45, "top":96, "width":243 }; })(window.cg||(window.cg={}));(function (ns) {
    'use strict';
    var async = (ns.async = {});
    var idxes = (async.idxes = {});
    var send = function(cmd, params) {
        if (typeof cmd !== 'string' || cmd.length === 0) {
        return;
    }
        var idx = (idxes[cmd] && (idxes[cmd] += 1)) || (idxes[cmd] = 1);
        var cid = cmd + '_' + idx;
        window.webkit.callcmd(JSON.stringify({ cmd: cmd, cid: cid, params: params }));
        return cid;
    };
    var call = function(cmd, params, timeout) {
        return new Promise (function(resolve, reject) {
            var cid = send(cmd, params);
            if (!cid) {
                reject('NULL_CMD');
                return;
            }
            var timer = setTimeout(function() {
                async.reject(cid, 'TIMEOUT');
            }, timeout || 20 * 1000);
            async[cid] = { resolve: resolve, reject: reject, timer: timer };
        });
    };
    async.resolve = function(cid, data) {
        if (!cid || typeof cid !== 'string') return;
        var delegate = async[cid];
        if (delegate) {
            delegate.resolve(data);
            clearTimeout(delegate.timer);
            delete async [cid];
        }
    };
    async.reject = function(cid, err) {
        if (!cid || typeof cid !== 'string') return;
        var delegate = async[cid];
        if (delegate) {
            delegate.reject(err);
            clearTimeout(delegate.timer);
            delete async [cid];
        }
    };
    ns.trigger = function(event, data) {
        var callback = ns.on[event];
        if (callback && typeof callback === 'function') {
        callback(data);
    }
    };
    ns.on = function(event, callback) {
        ns.on[event] = callback;
    };
    ns.off = function(event) {
        delete ns . on [event];
    };
    ns.log = function(data) {
        send('log', data);
    };
    ns.exit = function() {
        send('exit');
    };
    ns.emit = function(event, data) {
        var params = { event: event };
        if (data) {
            params.data = data;
        }
        return call('emit', params);
    };
    ns.paste = function(text) {
        send('paste', { text: text });
    };
    ns.track = function(event, props) {
        send('track', { event: event, props: props });
    };
    ns.share = function(params) {
        return call('share', params, 60 * 1000);
    };
    ns.album = function(image) {
        return call('album', { image: image }, 60 * 1000);
    };
    ns.withdraw = function(type, cents) {
        return call('withdraw', { type: type, cents: cents }, 60 * 1000);
    };
    ns.ad = {
        on: function (event, callback) {
        ns.ad.on[event] = callback;
    },
        off: function (event) {
        delete ns . ad . on [event];
    },
        emit: function (event, data) {
        var callback = ns.ad.on[event];
        if (callback && typeof callback === 'function') {
        callback(data);
    }
    },
        load: function (types) {
        send('adload', { types: types });
    },
        show: function (type) {
        send('adshow', { type: type });
    },
        usable: function (type) {
        return call('adcheck', { type: type });
    },
    };
    ns.ssl = true;
    window.dispatchEvent(new Event ('cgkit'));
})(window.cg || (window.cg = {}));