/********************************************************************************/
/*                                                                              */
/*              samsung.js                                                      */
/*                                                                              */
/*      Interface to Smartthings using core-sdk                                 */
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

const { SmartThingsClient, BearerTokenAuthenticator } = require('@smartthings/core-sdk');

const config = require("./config");
const catre = require("./catre");


/********************************************************************************/
/*                                                                              */
/*      Global storage                                                          */
/*                                                                              */
/********************************************************************************/

var users = {};
var capabilities = {};
var presentations = {};

var skip_capabilities = new Set();

skip_capabilities.add("ocf");
skip_capabilities.add("custom.disabledCapabilities");
skip_capabilities.add("execute");
skip_capabilities.add("healthCheck");

var smartapp_id = config.getSmartThingsCredentials().appId;

setInterval(checkUpdates,5*60*1000);



/********************************************************************************/
/*                                                                              */
/*      Setup Router                                                            */
/*                                                                              */
/********************************************************************************/

function getRouter(restful) {
   restful.use(authenticate);

   restful.all("*", config.handle404)
   restful.use(config.handleError);

   return restful;
}


/********************************************************************************/
/*                                                                              */
/*      Authentication for iqsign                                               */
/*                                                                              */
/********************************************************************************/

function authenticate(req, res, next) {
   next();
}



async function addBridge(authdata, bid) {
   console.log("SAMSUNG ADD BRIDGE", authdata.uid, authdata.token, bid);

   let username = authdata.uid;
   let pat = authdata.token;
   let user = users[username];
   if (user == null) {
      let client = new SmartThingsClient(new BearerTokenAuthenticator(pat));
      user = {
         username: username, client: client, bridgeid: bid,
         devices: {}, locations: {}, rooms: {}, active: {},
         values: {},
      };
      users[username] = user;
   }
   else {
      user.bridgeid = bid;
   }

   getDevices(username);

   return false;
}


async function handleCommand(bid, uid, devid, command, values) {
   let user = users[uid];
   if (user == null) {
      console.log("SAMSUNG COMMAND: USER NOT FOUND", uid);
      return;
   }

   let dev = user.devices[devid];
   if (dev != null) {
      for (let t of dev.TRANSITIONS) {
         if (t.NAME == command) {
            return await processCommand(user, dev, t, command, values);
          }
       }
    }
}


async function handleActiveSensors(bid, uid, active) {
   console.log("SAMSUNG HANDLE ACTIVE SENSORS", uid, active);
   let user = users[uid];
   if (user == null) {
      console.log("SAMSUNG COMMAND: USER NOT FOUND", uid);
      return;
   }
   user.active = { };
   for (let param of active) {
      let devid = param.DEVICE;
      let pname = param.PARAMETER;
      let devs = user.active[devid];
      if (devs == null) {
         devs = [];
         user.active[devid] = devs;
       }
      devs.push(pname);
   }
}



async function processCommand(user, dev, trans, command, values) {
   console.log("SAMSUNG SETUP COMMAND ",command,
         JSON.stringify(trans,null,2),
         JSON.stringify(values,null,2));

   let args = [];
   let params = trans.DEFAULTS.PARAMETERS;
   for (let p of trans.DEFAULTS.PARAMETERS) {
      let pname = p.NAME;
      let pv = values[pname];
      args.push(pv);
    }
   
   let cmdarg = {
         "component": trans.COMPONENTID, 
         "capability": trans.CAPABILITYID,
         "command": command, 
         "arguments": args,
   };
   
   console.log("SAMSUNG WORK ON COMMAND ",
         JSON.stringify(cmdarg,null,2));

   let client = user.client;
   let resp = await client.devices.executeCommand(dev.UID, cmdarg);

   console.log("SAMSUNG EXECUTE COMMAND", dev, trans, cmdarg, resp);
   
   setTimeout(checkUpdates);

   return resp;
}



async function handleParameters(bid, uid, devid, parameters) {
   let user = users[uid];
   if (user == null) {
      console.log("SAMSUNG PARAMETERS: USER NOT FOUND", uid);
      return;
   }
   
   // updateValues(user,devid); -- this will send event messages for all

   let rslt = await getParameterValuesByCapability(user,devid,parameters = null);
   let r1 = { }
   for (let p in rslt) {
      let v = rslt[p].value;
      r1[p] = v;
    }

   return r1;
}


async function getParameterValues(user, devid, parameters = null) {
   let client = user.client;
   let rslt = {};
   let sts = await client.devices.getStatus(devid);
   console.log("SAMSUNG DEVICE STATUS", devid, sts);
   for (let compname in sts.components) {
      let compstatus = sts.components[compname];
      for (let attrname in compstatus) {
         let capstatus = compstatus[attrname];
         console.log("SAMSUNG CAPABILITY STATUS", capstatus);
         for (let aname in capstatus) {
            if (parameters == null || parameters.includes(aname)) {
               let attrstate = capstatus[aname];
               rslt[aname] = attrstate;
            }
         }
      }
   }

   return rslt;
}


async function getParameterValuesByCapability(user,devid,parameters)
{
   let rslt = {};
   let client = user.client;
   let dev = user.devices[devid];
   if (dev == null) return null;
   let caps = {};
   for (let param of dev.PARAMETERS) {
      if (parameters == null || parameters.includes(param.NAME)) {
         let plist = caps[param.DATA];
         if (plist == null) {
            plist = [];
            caps[param.DATA] = plist;
          }
         plist.push(param.NAME);
       }
    }
   console.log("SAMSUNG CAP STATUS",caps);
   for (let data in caps) {
      let x = data.split("@@@");
      let compid = x[0];
      let capid = x[1];
      console.log("SAMSUNG CAP CHECK",compid,capid,caps[data])
      let capstatus = await client.devices.getCapabilityStatus(devid,compid,capid);
      console.log("SAMSUNG CAP STATUS",capstatus);
      for (let pnm of caps[data]) {
         rslt[pnm] = capstatus[pnm];
       }
    }
   return rslt;
}


async function updateValues(user, devid,parameters = null) {
// let rslt = await getParameterValues(user, devid, parameters);
   let rslt = await getParameterValuesByCapability(user,devid,parameters);
   for (let p in rslt) {
      // check previous value
      let  v1 = user.values[devid];
      if (v1 == null) {
         v1 = {};
         user.values[devid] = v1;
       }
      let v2 = v1[p];
      if (v2 == rslt[p].value) continue;
      v1[p] = rslt[p].value;
      
      let event = {
            TYPE: "PARAMETER",
            DEVICE: devid,
            PARAMETER: p,
            VALUE: rslt[p].value
       };
      await catre.sendToCatre({
         command: "EVENT",
         bid: user.bridgeid,
         event: event
       });
    }
}



/********************************************************************************/
/*                                                                              */
/*      List devices                                                            */
/*                                                                              */
/********************************************************************************/

async function getDevices(username) {
   let user = users[username];
   let client = user.client;
   await setupLocations(user);

   user.devices = {};
   let devs = await client.devices.list();
   console.log("SAMSUNG FOUND DEVICES", devs.length, devs);
   let devlst = [];
   for (let dev of devs) {
      console.log("SAMSUNG WORK ON DEVICE", dev.deviceId);
      let newdev = new SamsungDevice(user, dev);
      let devdef = await newdev.setup();
      if (devdef != null) {
         user.devices[devdef.UID] = devdef;
         devlst.push(devdef);
       }
      console.log("SAMSUNG NEW DEFINITION", JSON.stringify(devdef,null,2));
//    await defineDevice(user, dev);
   }

   console.log("SAMSUNG OUTPUT DEVICES", devlst.length);

   let msg = {
         command: "DEVICES",
         uid: user.username,
         bridge: "samsung",
         bid: user.bridgeid,
         devices: devlst,
    };
   await catre.sendToCatre(msg);

   for (let dev of devlst) {
      let params = getParameters(dev);
      await updateValues(user, dev.UID,params);
   }
}



async function setupLocations(user) {
   let client = user.client;
   let locs = await client.locations.list();

   for (let loc of locs) {
      user.locations[loc.locationId] = loc;
      let rooms = await client.rooms.list(loc.locationId);
      for (let room of rooms) {
         room.locationName = loc.name;
         user.rooms[room.roomId] = room;
      }
   }
}


function getParameters(dev)
{
   let rslt = {};
   for (let p of dev.PARAMETERS) {
      rslt[p.NAME] = true;
    }
   return rslt;
}




/********************************************************************************/
/*                                                                              */
/*      Handle polling                                                          */
/*                                                                              */
/********************************************************************************/

function checkUpdates()
{
   for (let uid in users) {
      let user = users[uid];
      for (let devid in user.active) {
         let params = user.active[devid];
         console.log("SAMSUNG POLL",uid,devid,params);
         updateValues(user,devid,params);
       }
    }
}


/********************************************************************************/
/*                                                                              */
/*      Class to build a device                                                 */
/*                                                                              */
/********************************************************************************/

class SamsungDevice {

   constructor(user, dev) {
      this.user_id = user;
      this.samsung_device = dev;
      this.cat_dev = {
         UID: dev.deviceId,
         BRIDGE: "samsung",
         PARAMETERS: [],
         TRANSITIONS: [],
         NAME: dev.name,
         LABEL: dev.label,
         DESCRIPTION: dev.name + ":" + dev.label,
      }
      console.log("SAMSUNG BUILD DEVICE",dev,this.cat_dev,this.samsung_device);
      this.capability_map = {};
      this.condition_map = {};
      this.action_map = {};
      this.reference_map = {};
      this.referenced_set = new Set();
      this.capability_list = [];
   }

   async setup() {
      await this.analyzePresentation();
      await this.analyzeCapabilities();
      await this.analyzeParameters();
      await this.analyzeTransitions();

      return this.cat_dev;
   }

   async analyzePresentation() {
      let client = this.user_id.client;
      let devid = this.samsung_device.deviceId;
      console.log("SAMSUNG GETTING PRESENTATION",devid);
      let presentation = await client.devices.getPresentation(devid);
      if (presentation == null) return;
      let use = presentation.automation;
      if (use == null) use = presentation.dashboard;
      if (use == null) return;
      let conds = use.conditions;
      if (conds != null) {
         for (let cond of conds) {
            let cnm = cond.capability;
            if (cnm != null) this.condition_map[cnm] = cond;
            this.checkReference(cond);
         }
      }
      let acts = use.actions;
      if (acts != null) {
         for (let act in acts) {
            let cnm = act.capability;
            if (cnm != null) {
               this.action_map[cnm] = act;
            }
            this.checkReference(act);
         }
      }
   }

   checkReference(obj) {
      let sup = obj.supportedValues;
      if (sup == null) {
         let lst = obj.list;
         if (lst == null) lst = obj.numberField;
         if (lst != null) sup = lst.supportedValues;
      }
      if (sup != null && sup != "") {
         let capid = obj.capability;
         let idx = sup.lastIndexOf(".");
         let refcap = sup.substring(0, idx);
         if (capid != null) this.reference_map[capid] = refcap;
         this.referenced_set.add(refcap);
      }
   }

   async analyzeCapabilities() {
      for (let comp of this.samsung_device.components) {
         if (comp.id != 'main') continue;
         for (let capid of comp.capabilities) {
            let cap = await this.loadCapability(this.user_id, capid);
            if (cap != null) {
               cap.componentid = comp.id;
               this.capability_list.push(cap);
               if (cap.presentation != null) {
                  this.analyzePresentation(cap.presentation);
               }
            }
         }
      }
   }

   async loadCapability(user, capid) {
      if (skip_capabilities.has(capid.id)) return null;
      if (capid.id.startsWith("samsungce.")) return null;
      if (capid.id.startsWith("custom.")) return null;

      let client = user.client;
      let key = capid.id + "_" + capid.version;
      console.log("SAMSUNG GETTING CAPABILITY",capid.id,capid.version);

      let cap = await client.capabilities.get(capid.id, capid.version);
      if (cap == null || cap.status != 'live') return null;
      try {
         let present = await client.capabilities.getPresentation(capid.id, capid.version);
         cap.presentation = present;
      }
      catch (e) {
         cap.presentation = null;
      }
      this.capability_map[key] = cap;
      this.capability_map[cap.id] = cap;
      return cap;
   }

   async analyzeParameters() {
      for (let cap of this.capability_list) {
         for (let attrname in cap.attributes) {
            let attr = cap.attributes[attrname];
            let param = this.loadSensorParameter(attrname, attr, cap);
            if (param != null) {
               this.cat_dev.PARAMETERS.push(param);
            }
         }
      }
   }

   loadSensorParameter(pname, attr, cap) {
      let param = {
         NAME: pname,
         ISSENSOR: true,
         DATA: cap.componentid + "@@@" + cap.id,
      };

      let schema = attr.schema;
      let props = schema.properties;
      if (props == null) return null;
      let value = props.value;
      if (value.oneOf != null) {
         value = value.oneOf[0];
      }

      let cmds = attr.enumCommands;
      if (cmds != null && cmds.length > 0) return null;
      let unit = props.unit;
      if (unit != null) {
         let units = unit["enum"];
         let dunit = unit.default;
         param.UNITS = units;
         param.DEFAULT_UNIT = dunit;
      }

      if (this.referenced_set.has(pname)) {
         param.SENSOR = false;
      }

      let ocap = this.condition_map[cap.id];
      let sup1 = cap.supportedValues;
      if (sup1 == null && ocap != null) sup1 = ocap.supportedValues;
      if (sup1 == null) sup1 = this.reference_map[cap.id];
      if (sup1 != null && sup1 == pname) sup1 = null;
      let pref = null;
      if (sup1 != null) {
         pref = {
            DEVICE: this.cat_dev.UID,
            PARAMETER: sup1,
         };
      }
      param = this.scanParameter(value, param, pref, ocap);

      if (param != null) delete param.capabilityid;

      return param;
   }

   scanParameter(value, param, pref, ocap) {
      let evals = value["enum"];
      if (evals != null) {
         param.TYPE = 'ENUM';
         param.VALUES = evals;
         if (evals.length == 0) return null;
         if (pref != null) {
            param.RANGEREF = pref;
         }
         return param;
      }
      let type = value.type;
      switch (type) {
         case 'string':
            if (param.NAME == 'color') param.TYPE = 'COLOR';
            else {
               param.TYPE = 'STRING';
               let svals = this.getValues(ocap);
               if (svals != null) {
                  param.TYPE = 'ENUM';
                  param.VALUES = svals;
                  if (pref != null) param.RANGEREF = pref;
                }
               else if (pref != null) {
                  param.TYPE = 'ENUM';
                  param.RANGEREF = pref;
                }
               else if (param.ISSENSOR) return null;
             }
            break;
         case 'integer':
            param = this.fixNumber('INTEGER', value, param, pref);
            break;
         case 'number':
            param = this.fixNumber('REAL', value, param, pref);
            break;
         case 'boolean':
            param.TYPE = 'BOOLEAN';
            break;
         case 'array':
            let items = value.items;
            if (items == null) return null;
            if (items.type == 'string' && items.enum != null) {
               param.TYPE = "SET";
               param.values = items.enum;
            }
            else if (items.type == 'string') {
               param.ISSENSOR = false;
               param.TYPE = 'STRINGLIST';
            }
            else {
               console.log("SANSUNG UNKNOWN array/set type", items, value);
               return null;
            }
            let svals = this.getValues(ocap);
            if (svals != null) {
               param.TYPE = 'ENUM';
               param.VALUES = svals;
               // might check for svals being empty
            }
            if (param.ISSENSOR && param.VALUES == null && pref == null)
               return null;
            if (pref != null) param.RANGEREF = pref;
            break;
         case 'object':
            let flds = [];
            for (let propname in value.properties) {
               let prop = value.properties[propname];
               let pv = { NAME: propname };
               pv = this.scanParameter(prop, pv,null,null);
               if (pv != null) flds.push(pv);
            }
            param.TYPE = 'OBJECT';
            param.FIELDS = flds;
            param.ISSENSOR = false;
            break;
         default:
            console.log("SAMSUNG Unknown schema type", value);
            break;
      }

      return param;
   }

   fixNumber(type, value, param, pref) {
      this.fixRange(value,param);
      param.TYPE = type;
      let min = value.minimum;
      let max = value.maximum;
      if (min == null || max == null) return null;
      param.MIN = min;
      param.MAX = max;
      if (pref != null) param.RANGEREF = pref;
      return param;
   }

   fixRange(value,param) {
      switch (param.NAME) {
         case "temperatureRange" :
            value.minimum = -40;
            value.maximum = 140;
            break;
         case "coolingSetpointRange" :
            value.minimum = 60;
            value.maximum = 100;
            break;
         case "headingSetpointRange" :
            value.minimum = 40;
            value.maximum = 80;
            break;
       }
    }

   async analyzeTransitions() {
      for (let cap of this.capability_list) {
         for (let cmdname in cap.commands) {
            let cmd = cap.commands[cmdname];
            cmd.componentid = cap.componentid;
            cmd.capabilityid = cap.id;
            let trans = await this.processTransition(cmdname, cmd);
            if (trans != null) {
               this.cat_dev.TRANSITIONS.push(trans);
            }
         }
      }
   }

   async processTransition(cmdname, cmd) {
      let cattrans = {};
      if (cmd.name != null) cattrans.NAME = cmd.name;
      else cattrans.NAME = cmdname;
      let params = [];
      for (let arg of cmd.arguments) {
         let p = this.processCommandParameter(arg, cmd);
         if (p == null) return null;
         params.push(p);
      }
      cattrans.DEFAULTS = { PARAMETERS: params };
      cattrans.COMPONENTID = cmd.componentid;
      cattrans.CAPABILITYID = cmd.capabilityid;
      return cattrans;
   }

   processCommandParameter(arg, cmd) {
      let param = { NAME: arg.name };
      let schema = arg.schema;
      if (schema == null) return null;
      let oneof = schema.oneOf;
      if (oneof != null) {
         schema = oneof[0];
      }
      if (schema.title != null) {
         if (schema.title != 'String') param.LABEL = schema.title;
         else param.LABEL = param.NAME;
       }

      let capid = cmd.capabilityid;

      let cap = this.capability_map[capid];
      let ocap = this.condition_map[capid];
      if (cap == null) {
         console.log("SAMSUNG CAPABILITY NOT FOUND",capid,JSON.stringify(cmd,null,2));
         return null;
       }
      if (cap.status != 'live') return null;
      let sub1 = cap.supportedValues;
      if (sub1 == null) sub1 = this.reference_map[capid];
      console.log("SAMSUNG COMMAND PARAMETER",capid,sub1,JSON.stringify(cap,null,2),
            JSON.stringify(ocap,null,2),JSON.stringify(schema,null,2));
      let pref = null;
      if (sub1 != null) {
         pref = {
            DEVICE: this.cat_dev.UID,
            PARAMETER: sub1,
         }
      }

      param = this.scanParameter(schema, param, pref,ocap);

      console.log("SAMSUNG PARAMETER RESULT",JSON.stringify(param,null,2),JSON.stringify(pref,null,2));

      return param;
   }

   getValues(obj) {
      if (obj == null) return null;
      let lst = obj.list;
      if (lst == null) return null;
      let arr = lst.alternatives;
      if (arr == null) return null;
      let rslt = [];
      for (let alt of arr) {
         if (alt.type == null || alt.type == 'active') {
            let k = alt.key;
            rslt.push(k);
          }
       }
      if (rslt.length == 0) return null;
      return rslt;
   }

}

/********************************************************************************/
/*                                                                              */
/*      Exports                                                                 */
/*                                                                              */
/********************************************************************************/

exports.getRouter = getRouter;
exports.addBridge = addBridge;
exports.handleCommand = handleCommand;
exports.handleActiveSensors = handleActiveSensors;
exports.handleParameters = handleParameters;



/* end of module samsung */
