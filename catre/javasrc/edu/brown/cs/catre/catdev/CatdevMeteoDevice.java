/********************************************************************************/
/*										*/
/*		CatdevMeteoDevice.java						*/
/*										*/
/*	Weather device using Open-Meteo 					*/
/*										*/
/********************************************************************************/
/*	Copyright 2023 Brown University -- Steven P. Reiss		      */
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



package edu.brown.cs.catre.catdev;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.json.JSONArray;
import org.json.JSONObject;

import edu.brown.cs.catre.catre.CatreDevice;
import edu.brown.cs.catre.catre.CatreLog;
import edu.brown.cs.catre.catre.CatreParameter;
import edu.brown.cs.catre.catre.CatreParameterRef;
import edu.brown.cs.catre.catre.CatreProgramListener;
import edu.brown.cs.catre.catre.CatreStore;
import edu.brown.cs.catre.catre.CatreUniverse;
import edu.brown.cs.ivy.file.IvyFile;

class CatdevMeteoDevice extends CatdevDevice implements CatreProgramListener
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private String	location_name;
private double	location_latitude;
private double	location_longitude;
private String	unit_type;
private boolean is_used;

private static Collection<CatdevMeteoDevice> known_locations = null;
private static TimerTask timer_task = null;

private static final long POLL_RATE = 1000*60*10;		// every 10 minutes

private static String METEO_URL = "https://api.open-meteo.com/v1/forecast?" +
      "latitude=$(LAT)&longitude=$(LONG)&current=weather_code,temperature_2m";


private static final Map<Integer,String> CONDITIONS;

static {
   CONDITIONS = new HashMap<>();
   CONDITIONS.put(0,"Clear");
   CONDITIONS.put(1,"Partly Cloudy");
   CONDITIONS.put(2,"Partly Cloudy");
   CONDITIONS.put(3,"Cloudy");
   CONDITIONS.put(45,"Fog");
   CONDITIONS.put(48,"Fog");
   CONDITIONS.put(51,"Drizzle");
   CONDITIONS.put(53,"Drizzle");
   CONDITIONS.put(55,"Drizzle");
   CONDITIONS.put(61,"Rain");
   CONDITIONS.put(63,"Rain");
   CONDITIONS.put(65,"Rain");
   CONDITIONS.put(71,"Snow");
   CONDITIONS.put(73,"Snow");
   CONDITIONS.put(75,"Snow");
   CONDITIONS.put(77,"Snow");
   CONDITIONS.put(80,"Rain Showers");
   CONDITIONS.put(81,"Rain Showers");
   CONDITIONS.put(82,"Rain Showers");
   CONDITIONS.put(85,"Snow Showers");
   CONDITIONS.put(86,"Snow Showers");
   CONDITIONS.put(95,"Thunderstorm");
   CONDITIONS.put(96,"Thunderstorm");
   CONDITIONS.put(99,"Thunderstorm");
}


/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

CatdevMeteoDevice(CatreUniverse cu,CatreStore cs,Map<String,Object> map)
{
   super(cu);
   location_name = null;
   location_latitude = 0;
   location_longitude = 0;
   unit_type = "imperial";
   is_used = true;

   fromJson(cs,map);

   if (cu.getProgram() != null){
      cu.getProgram().addProgramListener(this);
    }

   initialize();
}


private void initialize()
{
   String did = "OpenMeteo_" + location_latitude + "_" + location_longitude +
      "_" + unit_type + "_" + location_name;
   setDeviceId(did);

   if (getName() == null) {
      String nm = location_name;
      if (nm == null || nm.isEmpty()) {
	 nm = location_latitude + "," + location_longitude;
       }
      setName("Weather-" + nm);
      setLabel("Weather for " + nm);
    }

   CatreParameter pp = for_universe.createRealParameter("Temperature",
	 -100,160);
   pp.setIsSensor(true);
   addParameter(pp);
   
   List<String> vals = List.of("Clear","Partly Cloudy","Cloudy",
         "Fog", "Drizzle", "Rain", "Snow", "Rain Showers",
         "Snow Showers", "Thunderstorm","Unknown");
   CatreParameter pp1 = for_universe.createEnumParameter("WeatherCondition",vals);
   pp1.setIsSensor(true);
   addParameter(pp1);
   // might want to add other weather conditions
   //		humidity, wind speed and direction

   addMeteoDevice(this);
}

@Override public boolean validateDevice()
{
   if (location_latitude == 0 && location_longitude == 0) return false;
   if (unit_type == null) return false;

   return super.validateDevice();
}



@Override public void updateParameterValues()
{
   List<CatdevMeteoDevice> devs = List.of(this);
   updateData(devs);
}



private void update(JSONObject data)
{
   double lat = data.getDouble("latitude");
   double lng = data.getDouble("longitude");
   if (lat != location_latitude || lng != location_longitude) {
      CatreLog.logD("CATDEV","Lat/Long don't match " + lat + " " + lng + " " +
            location_latitude + " " + location_longitude);
    }
   JSONObject units = data.getJSONObject("current_units");
   JSONObject current = data.getJSONObject("current");
   int code = current.getInt("weather_code");
   String cond = CONDITIONS.get(code);
   if (cond == null) cond = "Unknown";
   
   double temp = current.getDouble("temperature_2m");
   String tempunit = units.getString("temperature_2m");
   temp = convertTemperature(temp,tempunit);
   
   CatreParameter p0 = findParameter("Temperature");
   setParameterValue(p0,temp);
   CatreParameter p1 = findParameter("WeatherCondition");
   setParameterValue(p1,cond);
}


private double convertTemperature(double v,String unit)
{
   if (unit_type.equalsIgnoreCase("imperial")) {
      if (unit.contains("C")) {
         v = v * 9.0 / 5.0 + 32;
       }
    }
   else {
      if (unit.contains("F")) {
         v = (v-32) * 5.0 / 9.0;
       }
      
    }
   return v;
}



/********************************************************************************/
/*										*/
/*	Output methods								*/
/*										*/
/********************************************************************************/

@Override public Map<String,Object> toJson()
{
   Map<String,Object> rslt = super.toJson();

   rslt.put("VTYPE","OpenMeteo");

   rslt.put("LATITUDE",location_latitude);
   rslt.put("LONGITUDE",location_longitude);
   rslt.put("UNITS",unit_type);
   rslt.put("LOCATION",location_name);

   return rslt;
}


@Override public void fromJson(CatreStore cs,Map<String,Object> map)
{
   super.fromJson(cs,map);

   location_latitude = getSavedDouble(map,"LATITUDE",location_latitude);
   location_longitude = getSavedDouble(map,"LONGITUDE",location_longitude);
   unit_type = getSavedString(map,"UNITS",unit_type);
   location_name = getSavedString(map,"LOCATION",location_name);
}




/********************************************************************************/
/*										*/
/*	Program listener							*/
/*										*/
/********************************************************************************/

@Override public void programUpdated()
{
   // might want to check if this device is needed by a program
   is_used = false;
   Set<CatreParameterRef> refs = getUniverse().getProgram().getActiveSensors();
   for (CatreParameterRef ref : refs) {
       CatreDevice cd = ref.getDevice();
       if (cd != this) continue;
       is_used = true;
       break;
    }
}


/********************************************************************************/
/*										*/
/*	Maintain set of active devices						*/
/*										*/
/********************************************************************************/

private static synchronized void addMeteoDevice(CatdevMeteoDevice md)
{
   if (known_locations == null) {
      known_locations = new ConcurrentLinkedQueue<>();
      timer_task = new Updater();
      md.getCatre().schedule(timer_task,POLL_RATE,POLL_RATE);
    }
   known_locations.add(md);
   updateData(List.of(md));
}



private static void updateData(List<CatdevMeteoDevice> devs)
{
   StringBuffer latbuf = new StringBuffer();
   StringBuffer longbuf = new StringBuffer();
   for (CatdevMeteoDevice cmd : devs) {
      if (!latbuf.isEmpty()) latbuf.append(",");
      latbuf.append(cmd.location_latitude);
      if (!longbuf.isEmpty()) longbuf.append(",");
      longbuf.append(cmd.location_longitude);
    }
   Map<String,String> map = new HashMap<>();
   map.put("LAT",latbuf.toString());
   map.put("LONG",longbuf.toString());
   String url = IvyFile.expandName(METEO_URL,map);
   String cnts = WEB_CACHE.getContents(url,0);
   if (cnts == null) {
      CatreLog.logE("Problem getting weather data for " + url);
      return;
    }
   if (cnts.startsWith("[")) {
      JSONArray jarr = new JSONArray(cnts);
      for (int i = 0; i < jarr.length(); ++i) {
         devs.get(i).update(jarr.getJSONObject(i));
       }
    }
   else {
      JSONObject jobj = new JSONObject(cnts);
      devs.get(0).update(jobj);
    }
}
   



private static final class Updater extends TimerTask {

   @Override public void run() {
      List<CatdevMeteoDevice> toupdate = new ArrayList<>();
      for (CatdevMeteoDevice md : known_locations) {
	 if (md.isDeviceValid() && md.isEnabled() && md.is_used) {
	    toupdate.add(md);
	  }
       }
      if (toupdate.isEmpty()) return;

      updateData(toupdate);
    }
}



}	// end of class CatdevMeteoDevice




/* end of CatdevMeteoDevice.java */

