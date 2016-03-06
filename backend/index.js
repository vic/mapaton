var Firebase = require('firebase');
var axios = require('axios');
var _ = require('lodash');
var Q = require('q');
var moment = require('moment');
var elastic = require('elasticsearch');
var execSync = require('child_process').execSync;

var api = 'https://mapaton-public.appspot.com/_ah/api/dashboardAPI/v1';
var ecobici = "https://www.ecobici.df.gob.mx/availability_map/getJsonObject";
var NOE = 1000; // numberOfElements per request


function getTrails() {
    return getAll('/getAllTrails', {numberOfElements: NOE}, 'trails', trailDetails);
}

function getAll(path, params, attribute, transform) {
    var deferred = Q.defer();

    axios.post(api + path + '?alt=json', params)
   .then(function (res) {
       process.stderr.write(' ');
       var collection = res.data[attribute] || [];
       collection = _.map(collection, transform);

       if (res.data.cursor && collection.length > 0) {
           var next = getAll(path, _.merge(params, {cursor: res.data.cursor}), attribute);
           next.then(function (others) {
               Q.all(collection).then(function (all) {
                   deferred.resolve(all.concat(others));
               }, deferred.reject);
           }, deferred.reject);
       } else {
           Q.all(collection).then(deferred.resolve, deferred.reject);
       }

    }, deferred.reject);

    return deferred.promise;
}

function resolvedPromise(obj) {
    var deferred = Q.defer();
    deferred.resolve(obj);
    return deferred.promise;
}

function pointTransform(point) {
    var deferred = Q.defer();
    var stamp = point.timeStamp;
    delete point.timeStamp;
    delete point.position;

    var date = new Date(stamp.date.year, stamp.date.month, stamp.date.day);
    date.setHours(stamp.time.hour, stamp.time.minute, stamp.time.second);

    point.timestamp = date.getTime();

    deferred.resolve(point);
    return deferred.promise;
}

function getTrailRawPoints(trail) {
    return getAll('/getTrailRawPoints', {trailId: trail.trailId, numberOfElements: NOE}, 'points', pointTransform)
}

function getTrailDetails(trail) {
    var deferred = Q.defer();
    axios.post(api+'/traildetails/'+trail.trailId+'?alt=json', {}).then(function (res){
        var details = res.data;

        var stdout = execSync("grep "+trail.trailId+" mapatonGTFS/routes.txt | cut -d, -f 2-3");
        var parts = (stdout + '').split(',')
        details.routeName = parts[0];
        details.routeDesc = parts[1];
        deferred.resolve(details);

    }, deferred.reject);
    return deferred.promise;
}

function trailDetails(trail) {
    var deferred = Q.defer();
    getTrailDetails(trail).then(function(details) {
        getTrailRawPoints(trail).then(function (points) {
            deferred.resolve(_.merge(trail, {points: points}));
            process.stderr.write('.');
        }, deferred.reject);
    }, deferred.reject);
    return deferred.promise;
}

getTrails().then(function (trails) {
    var json = JSON.stringify(trails, null, '\t');
    process.stdout.write(json);
}, function (err) {
    var json = JSON.stringify(err, null, '\t');
    process.stderr.write(json);
});
