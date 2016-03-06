var Firebase = require('firebase');
var axios = require('axios');
var _ = require('lodash');
var Q = require('q');

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
       process.stderr.write('.');
       var collection = res.data[attribute] || [];
       collection = _.map(collection, transform);

       if (res.data.cursor) {
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

function getTrailRawPoints(trail) {
    return getAll('/getTrailRawPoints', {trailId: trail.trailId, numberOfElements: NOE}, 'points', resolvedPromise)
}

function getTrailDetails(trail) {
    var deferred = Q.defer();
    axios.post(api+'/traildetails/'+trail.trailId+'?alt=json', {}).then(function (res){
        deferred.resolve(res.data);
    }, deferred.reject);
    return deferred.promise;
}

function trailDetails(trail) {
    var deferred = Q.defer();
    getTrailDetails(trail).then(function(details) {
        getTrailRawPoints(trail).then(function (points) {
            deferred.resolve(_.merge(trail, {points: points}));
            process.stderr.write('_');
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