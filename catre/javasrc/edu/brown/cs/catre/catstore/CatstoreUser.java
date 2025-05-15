/********************************************************************************/
/*										*/
/*		CatstoreUser.java						*/
/*										*/
/*	description of class							*/
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




package edu.brown.cs.catre.catstore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.brown.cs.catre.catre.CatreBridge;
import edu.brown.cs.catre.catre.CatreBridgeAuthorization;
import edu.brown.cs.catre.catre.CatreLog;
import edu.brown.cs.catre.catre.CatreSavableBase;
import edu.brown.cs.catre.catre.CatreStore;
import edu.brown.cs.catre.catre.CatreSubSavableBase;
import edu.brown.cs.catre.catre.CatreUniverse;
import edu.brown.cs.catre.catre.CatreUser;
import edu.brown.cs.catre.catre.CatreUtil;

 class CatstoreUser extends CatreSavableBase implements CatreUser, CatstoreConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private CatreStore	catre_store;
private String		user_name;
private String		user_email;
private String		user_password;
private String          temp_password;
private String		universe_id;
private CatreUniverse	user_universe;
private boolean         is_temporary;
private String          email_verifier;
private Map<String,CatreBridgeAuthorization> bridge_auths;

private static Pattern AUTH_PATTERN = Pattern.compile("AUTH_(\\d+)_(\\w+)");



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

CatstoreUser(CatreStore cs,String name,String email,String pwd)
{
   super(USERS_PREFIX);

   catre_store = cs;

   user_name = name;
   user_email = email;
   user_password = pwd;
   user_universe = null;
   universe_id = null;
   bridge_auths = new HashMap<>();
   is_temporary = false;
   email_verifier = null;
}



CatstoreUser(CatreStore store,Map<String,Object> doc)
{
    super(store,doc);

    catre_store = store;
}


/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

@Override public CatreUniverse getUniverse()
{
   if (user_universe == null && universe_id != null) {
      user_universe = (CatreUniverse) catre_store.loadObject(universe_id);

    }
   return user_universe;
}

@Override public void setUniverse(CatreUniverse cu)
{
   if (universe_id == null) {
      universe_id = cu.getDataUID();
      user_universe = cu;
      catre_store.saveObject(this);
    }
   else {
      CatreLog.logE("CATSTORE","Attempt to change user universe");
    }
}



@Override public String getUserName()
{
   return user_name;
}

@Override public CatreBridgeAuthorization getAuthorization(String name)
{
   return bridge_auths.get(name);
}


@Override public boolean addAuthorization(String name,Map<String,String> map)
{
   if (name == null) return false;

   if (map == null || map.isEmpty()) {
      bridge_auths.remove(name);
    }
   else {
      BridgeAuth ba = new BridgeAuth(name,map);
      bridge_auths.put(name,ba);
    }

   getUniverse().addBridge(name);

   getUniverse().getCatre().getDatabase().saveObject(this);

   return true;
}


@Override public boolean isTemporary()                  { return is_temporary; }

@Override public void setTemporary(boolean fg)         
{ 
   is_temporary = fg; 
   if (temp_password != null) {
      temp_password = null;
      catre_store.saveObject(this);
    }
}

@Override public void setTemporaryPassword(String pwd)
{
   if (pwd != null) {
      String p1 = CatreUtil.secureHash(pwd);
      String p2 = p1 + user_name;
      String p3 = CatreUtil.secureHash(p2);
      temp_password = p3;
      catre_store.saveObject(this);
    }
   else temp_password = null;
}


@Override public void setNewPassword(String pwd)
{
   temp_password = null;
   is_temporary = false;
   user_password = pwd;
   catre_store.saveObject(this);
}



@Override public boolean validateUser(String code)
{
   if (code == null) return false;
   if (!code.equals(email_verifier)) return false;
   
   email_verifier = null;
   return true;
}


@Override public String setupValidator()
{
   email_verifier = CatreUtil.randomString(24);
   return email_verifier;
}


@Override public boolean isValidated()
{
   return email_verifier == null;
}


 
/********************************************************************************/
/*										*/
/*	CatreStore methods							*/
/*										*/
/********************************************************************************/

@Override public Map<String,Object> toJson()
{
   Map<String,Object> rslt = super.toJson();

   rslt.put("USERNAME",user_name);
   rslt.put("EMAIL",user_email);
   rslt.put("PASSWORD",user_password);
   if (temp_password != null) rslt.put("TEMP_PASSWORD",temp_password);
   rslt.put("UNIVERSE_ID",universe_id);
   rslt.put("AUTHORIZATIONS",getSubObjectArrayToSave(bridge_auths.values()));

   return rslt;
}


@Override public void fromJson(CatreStore store,Map<String,Object> map)
{
   catre_store = store;
   
   super.fromJson(store,map);
   user_name = getSavedString(map,"USERNAME",user_name);
   user_email = getSavedString(map,"EMAIL",user_email);
   user_password = getSavedString(map,"PASSWORD",user_password);
   universe_id = getSavedString(map,"UNIVERSE_ID",universe_id);
   temp_password = getSavedString(map,"TEMP_PASSWORD",null);
   user_universe = getUniverse();

   bridge_auths = new HashMap<>();
   List<BridgeAuth> bal = new ArrayList<>();
   bal = getSavedSubobjectList(store,map,"AUTHORIZATIONS",
	 BridgeAuth::new,bal);
   for (BridgeAuth ba : bal) {
      String name = ba.getBridgeName();
      if (user_universe != null) {
         CatreBridge br = user_universe.findBridge(name);
         if (br == null) continue;
       }
      bridge_auths.put(name,ba);
    }
}




/********************************************************************************/
/*										*/
/*	Authorization methods							*/
/*										*/
/********************************************************************************/

private static class BridgeAuth extends CatreSubSavableBase implements CatreBridgeAuthorization {

   private String bridge_name;
   private Map<String,String> value_map;
   private int value_count;

   BridgeAuth(String name,Map<String,String> values) {
      super(null);
      CatreLog.logD("Setup Authorization " + name + " " + values);
      bridge_name = name;
      value_count = 0;
      value_map = new HashMap<>();
      for (Map.Entry<String,String> ent : values.entrySet()) {
         String key = ent.getKey();
         addKey(key,ent.getValue());
       }
      CatreLog.logD("Create authorization " + name + " " +
            value_map);
    }

   BridgeAuth(CatreStore cs,Map<String,Object> map) {
      super(cs,map);
    }

   @Override public String getBridgeName()		{ return bridge_name; }
   
   @Override public int getAuthorizationCount()         { return value_count; }

   @Override public String getValue(int idx,String key) {
      key = key.replace("#",Integer.toString(idx));
      return value_map.get(key);
    }

   @Override public Map<String,Object> toJson() {
      Map<String,Object> rslt = super.toJson();
      rslt.put("NAME",bridge_name);
      rslt.put("LENGTH",value_count);
      for (Map.Entry<String,String> ent : value_map.entrySet()) {
         rslt.put("BAKEY_" + ent.getKey(),ent.getValue());
       }
   
      return rslt;
    }

   @Override public void fromJson(CatreStore cs,Map<String,Object> map) {
      super.fromJson(cs,map);
      if (value_map == null) value_map = new HashMap<>();
      value_count = 0;
      bridge_name = getSavedString(map,"NAME",null);
      for (String s : map.keySet()) {
         if (s.startsWith("BAKEY_")) {
            String k = s.substring(6);
            String v = getSavedString(map,s,null);
            addKey(k,v);
          }
       }
      CatreLog.logD("Create authorization " + bridge_name + " " + value_map);
    }
   
   
   private void addKey(String key,String value) {
      if (key.startsWith("BAKEY_")) {
         key = key.substring(6);
       }
      key = key.replace("#","0");
      int ctr = 0;
      Matcher m = AUTH_PATTERN.matcher(key);
      if (m.matches()) {
         ctr = Integer.parseInt(m.group(1));
         value_count = Math.max(value_count,ctr+1);
       }
      CatreLog.logD("Setup auth field " + key + " " + ctr + " " + value);
     
      value_map.put(key,value);
    }
   
}


}	// end of class CatstoreUser




/* end of CatstoreUser.java */

