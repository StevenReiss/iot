/********************************************************************************/
/*										*/
/*		iqsign.js							*/
/*										*/
/*	Handle RESTful interface for IQSIGN and CATRE				*/
/*										*/
/*	Written by spr								*/
/*										*/
/********************************************************************************/
/*	Copyright 2023 Brown University -- Steven P. Reiss			*/
/*********************************************************************************
 *  Copyright 2023, Brown University, Providence, RI.				 *
 *										 *
 *			  All Rights Reserved					 *
 *										 *
 *  Permission to use, copy, modify, and distribute this software and its	 *
 *  documentation for any purpose other than its incorporation into a		 *
 *  commercial product is hereby granted without fee, provided that the 	 *
 *  above copyright notice appear in all copies and that both that		 *
 *  copyright notice and this permission notice appear in supporting		 *
 *  documentation, and that the name of Brown University not be used in 	 *
 *  advertising or publicity pertaining to distribution of the software 	 *
 *  without specific, written prior permission. 				 *
 *										 *
 *  BROWN UNIVERSITY DISCLAIMS ALL WARRANTIES WITH REGARD TO THIS		 *
 *  SOFTWARE, INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND		 *
 *  FITNESS FOR ANY PARTICULAR PURPOSE.  IN NO EVENT SHALL BROWN UNIVERSITY	 *
 *  BE LIABLE FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY 	 *
 *  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS,		 *
 *  WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS		 *
 *  ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE 	 *
 *  OF THIS SOFTWARE.								 *
 *										 *
 ********************************************************************************/

"use strict";


const fetch = require("node-fetch");
const crypto = require('crypto');

const config = require("./config");
const catre = require("./catre");



/********************************************************************************/
/*										*/
/*	Global storage								*/
/*										*/
/********************************************************************************/

var users = { };

let iqsign_url = "https://sherpa.cs.brown.edu:3336/rest/";



/********************************************************************************/
/*										*/
/*	Setup Router								*/
/*										*/
/********************************************************************************/

function getRouter(restful)
{
   restful.use(authenticate);

   restful.all("*",config.handle404)
   restful.use(config.handleError);

   setInterval(pingChecker,60*1000);

   return restful;
}



/********************************************************************************/
/*										*/
/*	Authentication for iqsign						*/
/*										*/
/********************************************************************************/

function authenticate(req,res,next)
{
   next();
}


async function addBridge(authdata,bid)
{
   console.log("IQSIGN ADD BRIDGE",authdata.username,authdata.token,bid);
   
   let username = authdata.username;
   let pat = authdata.token;
   
   let user = users[username];
   if (user == null) {
      user = { username : username, authtoken : pat,
	    session: null, bridgeid: bid, devices : [], saved : [], };
      users[username] = user;
    }
   else {
      user.bridgeid = bid;
    }
   
   await reauthorize(user);
   if (user.session == null) return false;
   
   await getDevices(user);
   
   return true;
}


async function reauthorize(user)
{
   let resp0 = await sendToIQsign("GET","login");
   let code = resp0.code;
   let session = resp0.session;
   if (code == null) {
      user.session = null;
      return;
    }
   else {
      user.session = session;
    }
   
   let tok1 = config.hasher(user.authtoken);
   let tok2 = config.hasher(tok1 + code);
   console.log("IQSIGN REAUTH ",user.username,user.authtoken,code,tok2,session);
   
   let login = { session: session, username: user.username, accesscode: tok2 };
   let resp1 = await sendToIQsign("POST","login",login);
   if (resp1.status != 'OK') {
      user.session = null;
    }
   else {
      user.session = resp1.session;
    }
   
   console.log("REAUTHORIZE COMPLETE",user.session);
}


async function getDevices(user)
{
   console.log("IQSIGN","Get DEVICES",user);
   
   let names = await getSavedSigns(user);

   let resp = await sendToIQsign("POST","signs",{ session : user.session });
   if (resp.status != 'OK') return;

   let update = false;

   for (let newdev of resp.data) {
      let fdev = null;
      let uid = "iQsign_" + newdev.namekey + "_" + newdev.signid;
      for (let dev of user.devices) {
	 if (dev.UID == uid || dev.ID == newdev.signid) {
	    fdev = dev;
	    break;
	  }
       }
      if (fdev == null) {
	 let catdev = {
	       ID : newdev.signid,		// id for iQsign
	       UID : uid,			// id for Catre
	       BRIDGE : "iqsign",
	       NAME : "iQsign " + newdev.name,
	       LABEL : "iQsign " + newdev.name,
	       DESCRIPTION: "iQsign " + newdev.name,
               USERDESC: false,
	       PARAMETERS :  [
		  { NAME: "savedValues", TYPE: "STRINGLIST", ISSENSOR: false, VOLATILE: true,
                     SORT: true,
                     VALUES: names }
	       ],
	       TRANSITIONS: [
		  { NAME : "setSign",
		     DEFAULTS : {
		     PARAMETERS : [
			{ NAME: "setTo",
			   LABEL: "Set Sign to",
			   TYPE: "ENUM",
                           SORT: true,
			   RANGEREF: { DEVICE: uid, PARAMETER: "savedValues" }
			 },
			 { NAME: "otherText", LABEL: "Other Text", TYPE: "STRING" }
		     ]
		   }
		   }
	       ]
	  };
	 user.devices.push(catdev);
	 update = true;
       }
    }

   if (update) {
      let msg = { command: "DEVICES", uid : user.username, bridge: "iqsign",
	    bid : user.bridgeid, devices : user.devices };
      await catre.sendToCatre(msg);
      await updateAuthedValues(user,null);
    }
}



async function getSavedSigns(user)
{
   let resp = await sendToIQsign("POST","namedsigns",{ session : user.session });
   if (resp.status != 'OK') return null;
   let names = [];
   for (let d of resp.data) {
      names.push(d.name);
    }
   user.saved = names;														
   return names;
}



/********************************************************************************/
/*										*/
/*	Command handling							*/
/*										*/
/********************************************************************************/

async function handleCommand(bid,uid,devid,command,values)
{
   console.log("IQSIGN COMMAND",devid,command,values);
   
   let user = users[uid];
   if (user == null) return;
   let sets = null;
   for (let key in values) {
      if (key == 'setTo' || key == 'otherText') continue;
      let txt = key + "=" + values[key].replace(" ","+");
      if (sets == null) sets = txt;
      else sets = sets + " " + txt;
    }

   await reauthorize(user);
   
   console.log("IQSIGN COMMAND VALUES ",values.setTo,values.otherText,values);
   
   for (let dev of user.devices) {
      if (dev.UID == devid) {
	  switch (command) {
	     case "setSign" :
		await sendToIQsign("POST","sign/setto",{
		   session: user.session,
		   signid: dev.ID,
		   value: values.setTo,
		   other: values.otherText,
		   sets: sets,
		 });
		break;
	   }
	  break;
       }
    }
}


async function handleParameters(bid,uid,devid,params)
{
   // not needed
}



/********************************************************************************/
/*										*/
/*	Update values from iQsign						*/
/*										*/
/********************************************************************************/

async function updateValues(user,devid)
{
   console.log("IQSIGN UPDATE VALUES ",devid,user);

   if (user == null || user.devices == null) return;
   
   await reauthorize(user);
   
   await getDevices(user);
   
   await updateAuthedValues(user,devid);
}


async function updateAuthedValues(user,devid)
{
   for (let dev of user.devices) {
      if (devid != null && dev.UID != devid) continue;
      let names = await getSavedSigns(user);
      if (names == null) continue;
      let event = {
	    TYPE: "PARAMETER",
	    DEVICE: dev.UID,
	    PARAMETER: "savedValues",
	    VALUE: names
       }
      await catre.sendToCatre({ command: "EVENT",
	 bid: user.bridgeid,
	 event : event });
    }
}



/********************************************************************************/
/*										*/
/*	Periodic checker to keep up to date					*/
/*										*/
/********************************************************************************/

async function pingChecker()
{
   let ulist = [];
   for (let uid in users) {
      ulist.push(uid);
    }
   if (users.length == 0) return;
   
   let resp = await sendToIQsign("POST","ping",{ users : ulist });
   console.log("IQSIGN PING",resp);

   let upds = resp.update;
   for (let uid of resp.update) {
      let user = users[uid];
      updateValues(user);
    }
}



/********************************************************************************/
/*										*/
/*	Helper methods								*/
/*										*/
/********************************************************************************/

async function sendToIQsign(method,path,data)
{
   let url = iqsign_url + path;
   let body = null;
   let hdrs = { 'Accept' : "application/json" };
   if (data != null && method != 'GET') {
      hdrs['Content-Type'] = 'application/json';
      body = JSON.stringify(data);
    }
   else if (data != null && method == 'GET') {
      let sep = "?";
      for (let k in data) {
	 url += sep + k + "=" + data[k];
	 sep = "&";
       }
    }

   console.log("IQSIGN Send to iQsign",method,path,data);

   let response = await fetch(url, {
	 method: method,
	 body : body,
	 headers: hdrs });
   
   try {
      let rslt = await response.json();
      
      console.log("IQSIGN Recieved back from iQsign",rslt);
      
      return rslt;
    }
   catch (e) {
      return null;
    }
}



/********************************************************************************/
/*										*/
/*	Exports 								*/
/*										*/
/********************************************************************************/

exports.getRouter = getRouter;
exports.addBridge = addBridge;
exports.handleCommand = handleCommand;
exports.handleParameters = handleParameters;




/* end of iqsign.js */
