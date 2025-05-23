/********************************************************************************/
/*										*/
/*		CatserveSessionImpl.java					*/
/*										*/
/*	Implementation of local session 					*/
/*										*/
/********************************************************************************/
/* Copyright 2023 Brown University -- Steven P. Reiss, Molly E. McHenry         */
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




package edu.brown.cs.catre.catserve;

import edu.brown.cs.catre.catre.CatreController;
import edu.brown.cs.catre.catre.CatreLog;
import edu.brown.cs.catre.catre.CatreSavableBase;
import edu.brown.cs.catre.catre.CatreSession;
import edu.brown.cs.catre.catre.CatreStore;
import edu.brown.cs.catre.catre.CatreUniverse;
import edu.brown.cs.catre.catre.CatreUser;
import edu.brown.cs.ivy.bower.BowerRouter;
import edu.brown.cs.ivy.bower.BowerSession;

import java.util.Map;

import javax.annotation.Tainted;

import com.sun.net.httpserver.HttpExchange;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;

class CatserveSessionImpl extends CatreSavableBase implements CatreSession, CatserveConstants, BowerSession
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private String		user_id;
private String		universe_id;
private Date		last_used;
private long		expires_at;
private Map<String,String> value_map;
private BowerSessionStore<CatserveSessionImpl> session_store;


private static final long EXPIRE_DELTA = 1000*60*60*24*4;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

CatserveSessionImpl(BowerSessionStore<CatserveSessionImpl> ss)
{
   super(SESSION_PREFIX);

   user_id = null;
   universe_id = null;
   last_used = new Date();
   expires_at = 0;
   value_map = new HashMap<>();
   session_store = ss;
}


CatserveSessionImpl(BowerSessionStore<CatserveSessionImpl> ss,CatreStore store,Map<String,Object> data)
{
   super(store,data);
   session_store = ss;
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

@Override 
public boolean isValid()
{
   if (expires_at == 0) return true;
   long now = System.currentTimeMillis();
   if (now > expires_at) return false;
   return true;
}



@Override
public CatreUser getUser(CatreController cc)
{
   CatreLog.logD("CATSERVE","Get user " + user_id + " " + expires_at);

   if (user_id == null || expires_at == 0) return null;

   return (CatreUser) cc.getDatabase().loadObject(user_id);
}


@Override
public CatreUniverse getUniverse(CatreController cc)
{
   CatreLog.logD("CATSERVE","Get universe " + universe_id + " " + expires_at);

   if (universe_id == null || expires_at == 0) return null;

   return (CatreUniverse) cc.getDatabase().loadObject(universe_id);
}



@Override
public String getSessionId()
{
   return getDataUID();
}


@Override 
public void setupSession()
{
   setupSession(null);
}


@Override
public void setupSession(CatreUser user)
{
   CatreUniverse univ = user.getUniverse();
   user_id = (user == null ? null : user.getDataUID());
   universe_id = (univ == null ? null : univ.getDataUID());
   last_used = new Date();
   expires_at = last_used.getTime() + EXPIRE_DELTA;

   CatreLog.logD("CATSERVE","Setup session " + user_id + " " + universe_id);
}


@Override 
public BowerSessionStore<CatserveSessionImpl> getSessionStore()
{
   return session_store;
}



/********************************************************************************/
/*										*/
/*	Response methods							*/
/*										*/
/********************************************************************************/

@Override public String jsonResponse(Object... val)
{
   return BowerRouter.jsonOKResponse(this,val);
}


@Override public String getParameter(HttpExchange e,String id)
{
   return BowerRouter.getParameter(e,id);
}




/********************************************************************************/
/*										*/
/*	I/O methods								*/
/*										*/
/********************************************************************************/

@Override
public Map<String,Object> toJson()
{
   Map<String,Object> rslt = super.toJson();
   rslt.put("USER_ID",user_id);
   rslt.put("UNIVERSE_ID",universe_id);
   rslt.put("LAST_USED", new Date());
   for (Map.Entry<String,String> ent : value_map.entrySet()) {
      rslt.put("VALUE_" + ent.getKey(),ent.getValue());
    }

   return rslt;
}



@Override
public void fromJson(CatreStore cs,Map<String,Object> map)
{
   super.fromJson(cs,map);
   user_id = getSavedString(map,"USER_ID",user_id);
   universe_id = getSavedString(map,"UNIVERSE_ID",universe_id);
   last_used = getSavedDate(map,"LAST_USED",last_used);
   expires_at = last_used.getTime() + EXPIRE_DELTA;
   if (value_map == null) value_map = new HashMap<>();
   for (String k : map.keySet()) {
      if (k.startsWith("VALUE_")) {
	 String kk = k.substring(6);
	 String ov = value_map.get(kk);
	 value_map.put(kk,getSavedString(map,k,ov));
       }
    }
}




void removeSession(CatreController cc)
{
   cc.getDatabase().removeObject(getDataUID());
}



/********************************************************************************/
/*										*/
/*	Value maintenance							*/
/*										*/
/********************************************************************************/

@Override public void setValue(String key,Object val)
{
   String sval = (val == null ? null : val.toString());
   value_map.put(key,sval);
}


@Override public void setValue(String key,String val)
{
   value_map.put(key,val);
}

@Override public @Tainted Object getValue(String key) 
{
   return value_map.get(key);
}


@Override public Collection<String> getValueKeys()
{
   return value_map.keySet();
}


@Override public @Tainted String getStringValue(String key)
{
   return value_map.get(key);
}



@Override public void saveSession(CatreController cc)
{
   if (expires_at == 0) return;
   last_used = new Date();
   cc.getDatabase().saveObject(this);
}



}	// end of class CatserveSessionImpl




/* end of CatserveSessionImpl.java */

