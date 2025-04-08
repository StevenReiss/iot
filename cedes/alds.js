/********************************************************************************/
/*										*/
/*		alds.js 							*/
/*										*/
/*	Interface to ALDS location detector					*/
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

const fs = require('fs');
const config = require("./config");
const catre = require("./catre");


/********************************************************************************/
/*										*/
/*	Local storage								*/
/*										*/
/********************************************************************************/

var users = { };
var tokens = { };
var log_stream = null;
var data_stream = null;


/********************************************************************************/
/*										*/
/*	Handle routing								*/
/*										*/
/********************************************************************************/

function getRouter(restful)
{
   restful.post("/alds/data",handleRawData);
   restful.post("/alds/log",handleLogData)

   restful.all("*",config.handle404);
   restful.use(config.handleError);

   return restful;
}

/********************************************************************************/
/*										*/
/*	Handle raw data for testing purposes					*/
/*										*/
/********************************************************************************/

function handleRawData(req,res)
{
// console.log("ALDS DATA",req.body.data);

   if (data_stream == null) {
      data_stream = fs.createWriteStream('aldsdata.json',{flags: 'a'});
    }

   let data = req.body.data;

   if (data != null) {
      data = JSON.stringify(data,null,2);
      data_stream.write(data + "\n");
    }

   config.handleSuccess(req,res);
}


function handleLogData(req,res)
{
// console.log("ALDS LOG",req.body.message);

   if (log_stream == null) {
      log_stream = fs.createWriteStream('alds.log',{ flags: 'a'});
    }

   var data = req.body.message;

   if (data != null) {
      log_stream.write(data + "\n");
    }

   config.handleSuccess(req,res);
}



/********************************************************************************/
/*										*/
/*	Exports 								*/
/*										*/
/********************************************************************************/

exports.getRouter = getRouter;



/* end of module alds */
