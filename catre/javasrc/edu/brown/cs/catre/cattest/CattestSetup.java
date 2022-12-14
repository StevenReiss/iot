/********************************************************************************/
/*                                                                              */
/*              CattestSetup.java                                               */
/*                                                                              */
/*      Setup CATRE for our own home                                            */
/*                                                                              */
/********************************************************************************/
/*      Copyright 2011 Brown University -- Steven P. Reiss                    */
/*********************************************************************************
 *  Copyright 2011, Brown University, Providence, RI.                            *
 *                                                                               *
 *                        All Rights Reserved                                    *
 *                                                                               *
 * This program and the accompanying materials are made available under the      *
 * terms of the Eclipse Public License v1.0 which accompanies this distribution, *
 * and is available at                                                           *
 *      http://www.eclipse.org/legal/epl-v10.html                                *
 *                                                                               *
 ********************************************************************************/

/* SVN: $Id$ */



package edu.brown.cs.catre.cattest;

import java.io.File;
import java.io.IOException;

import org.json.JSONObject;

import edu.brown.cs.catre.catre.CatreJson;
import edu.brown.cs.catre.catre.CatreLog;
import edu.brown.cs.catre.catre.CatreUtil;
import edu.brown.cs.ivy.file.IvyFile;

public class CattestSetup implements CattestConstants, CatreJson
{



/********************************************************************************/
/*                                                                              */
/*      Main program                                                            */
/*                                                                              */
/********************************************************************************/

public static void main(String [] args)
{
   CattestSetup setup = new CattestSetup(args);
   
   setup.runSetup();
}


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

private CattestSetup(String [] args)
{
   if (args.length > 0) {
      CattestUtil.setTestHost(TEST_HOST1);
    }
   else {
      CattestUtil.startCatre();
    }
}




/********************************************************************************/
/*                                                                              */
/*      Running methods                                                         */
/*                                                                              */
/********************************************************************************/

private void runSetup()
{
   File logindata = new File("/pro/iot/secret/catrelogin");
   JSONObject data = null;
   try {
      data = new JSONObject(IvyFile.loadFile(logindata));
    }
   catch (IOException e) { 
      System.exit(1);
    }
   String user = data.getString("user");
   String pwd = data.getString("password");
   String email = data.getString("email");
   String stacc = data.getString("smartthings-spr");
   String genuid = data.getString("generic_uid");
   String genpat = data.getString("generic_pat");
   String iqsuid = data.getString("iqsign_user");
   String iqspat = data.getString("iqsign_token");
   String gcalnms = data.getString("gcal_names");
   
   String v1 = CatreUtil.secureHash(pwd);
   String v2 = v1 + user;
   String v3 = CatreUtil.secureHash(v2);  
      
   JSONObject rslt2 = CattestUtil.sendGet("/login");      
   String sid = rslt2.getString("CATRESESSION");
   String salt = rslt2.getString("SALT");
   
   String v4 = v3 + salt;
   String v5 = CatreUtil.secureHash(v4);
   JSONObject rslt3 = CattestUtil.sendJson("POST","/login",
         "CATRESESSION",sid,"SALT",salt,
         "username",user,"password",v5);
   if (!rslt3.getString("STATUS").equals("OK")) {
      // if login fails, try to register
      sid = rslt3.getString("CATRESESSION");
      JSONObject rslt1 = CattestUtil.sendJson("POST","/register",
            "CATRESESSION",sid,
            "username",user,
            "email",email,
            "password",v3,
            "universe","MyWorld");
       sid = rslt1.getString("CATRESESSION");
    }
  
   JSONObject rslt5 = CattestUtil.sendJson("POST","/bridge/add",
         "CATRESESSION",sid,"BRIDGE","generic",
         "AUTH_UID",genuid,
         "AUTH_PAT",genpat);
   sid = rslt5.getString("CATRESESSION");
   
   JSONObject rslt6 = CattestUtil.sendJson("POST","/bridge/add",
         "CATRESESSION",sid,"BRIDGE","iqsign",
         "AUTH_UID",iqsuid,
         "AUTH_PAT",iqspat);
   sid = rslt6.getString("CATRESESSION");
   
   JSONObject rslt6a = CattestUtil.sendJson("POST","/bridge/add",
         "CATRESESSION",sid,"BRIDGE","gcal",
         "AUTH_CALENDARS",gcalnms);
   CatreLog.logI("CATTEST","Add gcal bridge = " + rslt6a.toString(2));
   
   JSONObject rslt4 = CattestUtil.sendJson("POST","/bridge/add",
         "CATRESESSION",sid,"BRIDGE","samsung",
         "AUTH_TOKEN",stacc);
   sid = rslt4.getString("CATRESESSION");
   
   JSONObject rslt7 = CattestUtil.sendJson("GET","/universe",
         "CATRESESSION",sid);
   CatreLog.logI("CATTEST","Universe = " + rslt7.toString(2));
   
   JSONObject devjson = buildJson("VTYPE","Weather","CITY","Rehoboth,MA,US",
         "UNITS","imperial");
   JSONObject rslt8 = CattestUtil.sendJson("POST","/universe/addvirtual",
         "CATRESESSION",sid,"DEVICE",devjson);
   CatreLog.logI("CATTEST","Add Virtual = " + rslt8.toString(2));
   
   JSONObject rslt9 = CattestUtil.sendJson("GET","/universe",
         "CATRESESSION",sid);
   CatreLog.logI("CATTEST","Universe = " + rslt9.toString(2));
   
   JSONObject cond1 = buildJson("TYPE","Parameter",
         "PARAMREF",buildJson("DEVICE","COMPUTER_MONITOR_geode-kkQRZVXiOmaLMKbo",
               "PARAMETER","Presence"),
               "STATE","WORKING",
               "TRIGGER",false);
   JSONObject cond2 = buildJson("TYPE","And",
         "CONDITIONS",buildJsonArray(cond1));
   JSONObject act0 = buildJson("TRANSITION",
         buildJson("DEVICE","iQsign_f6ZA6D8W_1","TRANSITION","setSign"),
         "PARAMETERS",buildJson("setTo","Working at Home"));
   JSONObject rul0 = buildJson("_id","RULE_aIRlbJhDwWdsjyjjnUtcfPYc",
         "PRIORITY",50.0,
         "CONDITION",cond2,
         "ACTIONS",buildJsonArray(act0));
   JSONObject rslt10 = CattestUtil.sendJson("POST","/rule/add",
         "CATRESESSION",sid,"RULE",rul0);
   CatreLog.logI("CATTEST","Add Rule = " + rslt10.toString(2));    
   
   JSONObject rslt11 = CattestUtil.sendJson("GET","/rules",
         "CATRESESSION",sid);
   CatreLog.logI("CATTEST","Rules: " + rslt11.toString(2));
}



}       // end of class CattestSetup




/* end of CattestSetup.java */

