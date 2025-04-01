/********************************************************************************/
/*                                                                              */
/*              generic.js                                                      */
/*                                                                              */
/*      Handle RESTful interface for generic devices and CATRE                  */
/*                                                                              */
/*      Written by spr                                                          */
/*                                                                              */
/********************************************************************************/
/*      Copyright 2023 Brown University -- Steven P. Reiss                      */
/*********************************************************************************
 *  Copyright 2023, Brown University, Providence, RI.                            *
 *                                                                               *
 *                        All Rights Reserved                                    *
 *                                                                               *
 *  Permission to use, copy, modify, and distribute this software and its        *
 *  documentation for any purpose other than its incorporation into a            *
 *  commercial product is hereby granted without fee, provided that the          *
 *  above copyright notice appear in all copies and that both that               *
 *  copyright notice and this permission notice appear in supporting             *
 *  documentation, and that the name of Brown University not be used in          *
 *  advertising or publicity pertaining to distribution of the software          *
 *  without specific, written prior permission.                                  *
 *                                                                               *
 *  BROWN UNIVERSITY DISCLAIMS ALL WARRANTIES WITH REGARD TO THIS                *
 *  SOFTWARE, INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND            *
 *  FITNESS FOR ANY PARTICULAR PURPOSE.  IN NO EVENT SHALL BROWN UNIVERSITY      *
 *  BE LIABLE FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY          *
 *  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS,              *
 *  WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS               *
 *  ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE          *
 *  OF THIS SOFTWARE.                                                            *
 *                                                                               *
 ********************************************************************************/

"use strict";


const config = require("./config");
const catre = require("./catre");



/********************************************************************************/
/*                                                                              */
/*      Module storage                                                          */
/*                                                                              */
/********************************************************************************/

let users = { };
let tokens = { };
let queues = { };
"de"

/********************************************************************************/
/*                                                                              */
/*      Setup Router                                                            */
/*                                                                              */
/********************************************************************************/

function getRouter(restful)
{
   restful.post("/generic/attach",handleAttach);
   restful.post("/generic/authorize",handleAuthorize);
   restful.post("/generic/log",handleLog);
   
   restful.use(authenticate);

   restful.post("/generic/devices",handleDevices);
   restful.post("/generic/ping",handlePing);
   restful.post("/generic/event",handleEvent);

   restful.all("*",config.handle404);
   restful.use(config.handleError);

   return restful;
}




/********************************************************************************/
/*                                                                              */
/*      Authentication for generic devices                                      */
/*                                                                              */
/********************************************************************************/

function authenticate(req,res,next)
{
   console.log("GENERIC AUTHENTICATE",req.token);

   let tok = req.token;
   if (tokens[tok] == null) config.handleFail(req,res,"Unauthorized");
   else {
      req.body.user = tokens[tok];
      next();
    }
}

/**
 *      ADDBRIDGE { authdata{ uid: user_id, pat: encoded PAT }, bridgeid: ID }
 **/

function addBridge(authdata,bid)
{
   console.log('GENERIC ADD BRIDGE',authdata.uid,authdata.pat);

   let uid = authdata.uid;
   let pat = authdata.pat;

   users[uid] = { uid: uid,
         seed: config.randomString(24),
         pat : pat,
         token: config.randomString(24),
         bridgeid: bid,
         devices : [ ],
         active: null,
         lastping: { },
         devicecounter : 1 };
   queues[uid] = [];

   return true;
}


/********************************************************************************/
/*                                                                              */
/*      Request handlers                                                        */
/*                                                                              */
/********************************************************************************/

/**
 *      ATTACH { uid : <user> }
 *         -> { status: OK, seed: SEED }
 **/

function handleAttach(req,res)
{
   console.log("GENERIC ATTACH",req.body);

   let uid = req.body.uid;
   if (users[uid] == null) {
      console.log("GENERIC NO USER FOUND");
      config.handleFail(req,res,"No such user",403);
    }
   else {
      let seed = users[uid].seed;
      if (seed == null) config.handleFail(req,res,"No such user",403);
      else config.handleSuccess(req,res,{ status: "OK", seed: seed });
    }
}


/**
 *      AUTHORIZE { uid: <user>, patencoded: H(H(H(pat) + uid) + seed)}
 *         -> { status: OK, token : TOKEN}
 **/

function handleAuthorize(req,res)
{
   console.log("GENERIC AUTHORIZE",req.body);

   let patencode = req.body.patencoded;
   let uid = req.body.uid;
   let user = users[uid];
   if (user == null) config.handleFail(req,res,"No such user",403);
   else {
      let p1 = user.pat;
      let p2 = config.hasher(p1 + user.seed);
      // console.log("GENERIC CHECK PWD",p1,p2,user.seed,patencode);
      if (p2 != patencode) {
         config.handleFail(req,res,"Bad uid or password",404);
       }
      else {
         let rslt = { status: "OK", token : user.token };
         config.handleSuccess(req,res,rslt);
         tokens[user.token] = user;
         user.devicecounter++;
       }
    }
}



/**
 *      DEVICES { devices: [ {JSON} ] }
 *              add devices from a single source
 **/

async function handleDevices(req,res)
{
   console.log("GENERIC DEVICES",JSON.stringify(req.body,null,2));

   let user = req.body.user;
   let devs = req.body.devices;
   for (let dev of devs) {
      let d1 = null;
      for (let d0 of user.devices) {
         if (d0.UID == dev.UID) {
            d1 = d0;
            d1.LABEL = dev.LABEL;
            d1.NAME = dev.NAME;
            d1.TRANSITIONS = dev.TRANSITIONS;
            d1.PARAMETERS = dev.PARAMETERS;
            break;
          }
       }
      if (d1 == null) user.devices.push(dev);
    }

   let msg = { command: "DEVICES", uid: user.uid,
         bridge: "generic",
         bid: user.bridgeid,
         devices: user.devices };
   await catre.sendToCatre(msg);
   config.handleSuccess(req,res);
}


/**
 *      PING { }
 *         -> { status: OK } or { status: COMMAND, command : {..} } or { status: DEVICES }
 **/

function handlePing(req,res)
{
   console.log("GENERIC PING",req.body,new Date());

   let user = req.body.user;
   let c = queues[user.uid].shift();
   let rslt = { status: "OK" };
   let ctr = req.body.counter;
   if (ctr == null) ctr = 0;
   let dev = req.body.device;
   if (dev != null) {
      user.lastping[dev] = Date.now();
    }
   
   if (c != null) {
      rslt = { status: "COMMAND", command: c };
    }
   else if (user.devicecounter > ctr) {
      console.log("")
      rslt = { status: "DEVICES", counter: user.devicecounter };
    }

   config.handleSuccess(req,res,rslt);
}


/**
 *      EVENT { event: <json> }
 **/

async function handleEvent(req,res)
{
   console.log("GENERIC EVENT",req.body);

   let report = true;
   let user = req.body.user;
   let event = req.body.event;
   if (user.active != null) {
      let devid = event.DEVICE;
      let param = event.PARAMETER;
      if (user.active[devid] != null) {
         if (!user.active[devid].has(param)) report = false;
       }
      console.log("GENERIC EVENT CHECK",devid,param,report);
    }
   
   if (report) {
      let msg = { command: "EVENT", uid : user.uid, bridge: "generic",
            bid: user.bridgeid,
            event: event };
      await catre.sendToCatre(msg);
    }
   
   config.handleSuccess(req,res);
}




function handleCommand(bid,uid,devid,command,values)
{
   let x = queues[uid];
   if (x == null) queues[uid] = [];
   queues[uid].push( { command: command, values: values } );
}


function handleParameters(bid,uid,devic,params)
{
   // not needed
}



function handleSetup(req,res)
{
   // server restarted -- handle any cleanup/setup here

   config.handleSuccess(req,res);
}


function handleActiveSensors(bid,uid,active)
{
   let user = users[uid];
   if (user == null) {
      console.log("GENERIC COMMAND: USER NOT FOUND", uid);
      return;
    }
   if (user.active == null) user.active = {};
   for (let param of active) {
      let devid = param.DEVICE;
      user.active[devid] = new Set();
    }
   for (let param of active) {
      let devid = param.DEVICE;
      let nm = param.PARAMETER;
      user.active[devid].add(nm);
    }
}


function handleLog(req,res)
{
   // log request
   console.log("GENERIC LOG",req.body.user,req.body.message);
   
   config.handleSuccess(req,res);
}




/********************************************************************************/
/*                                                                              */
/*      Initializers                                                            */
/*                                                                              */
/********************************************************************************/

async function checkUpdates() 
{
   let now = Date.now();
   
   console.log("GENERIC CHECK UPDATES",now);
   
   for (let uid in users) {
      let user = users[uid];
      for (let dev of user.devices) {
         // might want to check if device is active
         if (dev.PINGTIME != null && dev.PINGTIME > 0) {
            let lp = user.lastping[dev.UID];
            if (lp != null && lp > 0 && (now-lp) > 2 * dev.PINGTIME) {
               console.log("GENERIC LOST DEVICE",dev);
               user.lastping[dev.UID] = 0;
               for (let par of dev.PARAMETERS) {
                  await noPingEvent(user,dev,par);
                }
             }
          }
       }
    }
}


async function noPingEvent(user,dev,par)
{
   if (par.NOPING == null) return;
   let event = { DEVICE: dev.UID, 
         TYPE: "PARAMETER",
         PARAMETER: par.NAME, 
         VALUE: par.NOPING };
   let msg = { command: "EVENT", 
         uid : user.uid,
         bridge: "generic",
         bid : user.bridgeid, 
         event: event };
   await catre.sendToCatre(msg);
}


setInterval(checkUpdates,5*60*1000);



/********************************************************************************/
/*                                                                              */
/*      Exports                                                                 */
/*                                                                              */
/********************************************************************************/

exports.addBridge = addBridge;
exports.handleCommand = handleCommand;
exports.handleActiveSensors = handleActiveSensors;
exports.handleParameters = handleParameters;
exports.getRouter = getRouter;



/* end of generic.js */

