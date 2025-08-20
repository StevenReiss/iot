/********************************************************************************/
/*                                                                              */
/*              SignMakerSign.java                                              */
/*                                                                              */
/*      Hold all the information for the current sign                           */
/*                                                                              */
/********************************************************************************/

package edu.brown.cs.signmaker;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.swing.JComponent;
import javax.swing.JPanel;

import edu.brown.cs.ivy.file.IvyFile;
import edu.brown.cs.ivy.file.IvyLog;

class SignMakerSign implements SignMakerConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private SignMakerText []        text_regions;
private SignMakerImage []       image_regions;

private Color                   background_color;
private Color                   foreground_color;
private String                  font_family;
private Map<SignMakerComponent,Rectangle2D> item_positions;
private Map<String,String>      key_values;
private int                     user_id;
private boolean                 do_counts;
private Set<Integer>            used_ids;


private static double [] SCALE_VALUES = {
   1.0, 1.2, 1.4, 1.6, 1.8, 2.0
};

private static Set<String> font_names;

static {
   font_names = new HashSet<>();
   GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
   String [] nms = ge.getAvailableFontFamilyNames();
   for (String s : nms) {
      font_names.add(s.toLowerCase());
      int idx = s.indexOf(" ");
      if (idx > 0) {
         String s1 = s.substring(0,idx);
         font_names.add(s1);
       }
    }
}



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

SignMakerSign(int uid,boolean counts) 
{
   text_regions = new SignMakerText[8];
   image_regions = new SignMakerImage[8];
   background_color = Color.WHITE;
   foreground_color = Color.BLACK;
   font_family = null;
   item_positions = new HashMap<>();
   key_values = new HashMap<>();
   user_id = uid;
   do_counts = counts;
   used_ids = null;
}



/********************************************************************************/
/*                                                                              */
/*      Output methods                                                          */
/*                                                                              */
/********************************************************************************/

BufferedImage createSignImage(int w,int h)
{
// System.err.println("SIGNMAKER: create sign image " + w + " " + h + " " +
//       Arrays.toString(text_regions) + " " + 
//       Arrays.toString(image_regions) + " " + background_color + " " +
//       font_family + " " + border_width + " " + border_color);
   
   for (int i = 0; i < text_regions.length; ++i) {
      if (text_regions[i] != null && text_regions[i].isEmpty()) text_regions[i] = null;
    }
   for (int i = 0; i < image_regions.length; ++i) {
      if (image_regions[i] != null && image_regions[i].isEmpty()) image_regions[i] = null;
    }
   
   setDimensions(w,h);
   
   JPanel pnl = new JPanel();
   pnl.setSize(w,h);
   pnl.setBackground(background_color);
   pnl.setForeground(foreground_color);
   
   BufferedImage img = new BufferedImage(w,h,BufferedImage.TYPE_INT_RGB);
   Graphics2D g = img.createGraphics();
   
   g.setColor(background_color);
   g.fillRect(0,0,w,h);  
   g.setColor(foreground_color);
   
   g.setColor(foreground_color);
   g.setBackground(background_color);
   if (font_family != null) {
      Font ft = new Font(font_family,0,10);
      pnl.setFont(ft);
      g.setFont(ft);
    }
   
   for (int i = 1; i < text_regions.length; ++i) {
      setup(pnl,text_regions[i]);
    }
   for (int i = 1; i < image_regions.length; ++i) {
      setup(pnl,image_regions[i]);
    }
   
   for (int i = 0; i < text_regions.length; ++i) {
      waitForReady(text_regions[i]);
    }
   for (int i = 0; i < image_regions.length; ++i) {
      waitForReady(image_regions[i]);
    }
 
   if (image_regions[0] != null) {
      image_regions[0].addBackgroundComponent(pnl); 
    }
   
   pnl.paint(g);
// pnl.paintComponents(g); 
   
   return img;
}



private void setup(JPanel pnl,SignMakerComponent c)
{
   if (c == null) return;
   
   Rectangle2D r2 = item_positions.get(c);
   if (r2 == null) return;
   
   JComponent comp = c.setupComponent(r2,pnl,this);
   if (comp == null) return;
   
   pnl.add(comp);
   
   comp.setBounds(r2.getBounds());
}


private void waitForReady(SignMakerComponent c) 
{
   if (c == null) return;
   c.waitForReady();
}



/********************************************************************************/
/*                                                                              */
/*      Setup methods                                                           */
/*                                                                              */
/********************************************************************************/

void setBackground(Color c)
{
   background_color = c;
}

void setForeground(Color c)
{
   foreground_color = c;
}

Color getForeground()
{
   return foreground_color;
}

void setBorder(int w,String c)
{ }

boolean setFontFamily(String name)
{
   name = getFontFamily(name);
   if (name == null) return false;
   
   font_family = name;
   return true;
}



static String getFontFamily(String name)
{
   name = name.toLowerCase().trim();
   
   if (!font_names.contains(name)) {
      int idx = name.indexOf("_");
      if (idx < 0) return null;
      String base = name.substring(0,idx);
      if (!font_names.contains(base)) return null;
      name = name.replace("_"," ");
    }
   
   return name;
}


void setTextRegion(int which,SignMakerText rgn)
{
   if (which <= 0 || which >= text_regions.length) return;
   if (rgn != null && !rgn.isEmpty()) text_regions[which] = rgn;
}


void setImageRegion(int which,SignMakerImage rgn)
{
   if (which < 0 || which >= image_regions.length) return;
   if (rgn != null && !rgn.isEmpty()) image_regions[which] = rgn;
}

boolean isTextRegionUsed(int which) 
{
   if (which <= 0 || which >= text_regions.length) return true;
   if (text_regions[which] != null) return true;
   if (which == 4 && image_regions[5] != null) return true;
   if (which == 5 && image_regions[6] != null) return true;
   return false;
}

boolean isImageRegionUsed(int which) 
{
   if (which <= 0 || which >= image_regions.length) return true;
   if (image_regions[which] != null) return true;
   if (which == 5 && text_regions[4] != null) return true;
   if (which == 6 && text_regions[5] != null) return true;
   return false;
}

void setProperty(String key,String value)
{
   key_values.put(key,value);
}



String mapString(String s)
{
   if (key_values.isEmpty()) return s;
   s = IvyFile.expandText(s,key_values,false);
   return s;
}



String useSavedImage(String name)
{
   for (int i = 0; i < 4; ++i) {
      Connection sql = SignMaker.getSqlDatabase();
      if (sql == null || name == null || name.length() == 0) return null;
      if (used_ids == null) used_ids = new HashSet<>();
      String cnts = null;
      
      try {
         String q1 = "SELECT * FROM iQsignDefines WHERE name = ? AND ";
         q1 += "( userid = ? OR userid IS NULL )";
         PreparedStatement st1 = sql.prepareStatement(q1);
         st1.setString(1,name);
         st1.setInt(2,user_id);
         ResultSet rs1 = st1.executeQuery();
         
         int bestid = 0;
         int bestuid = 0;
         while (rs1.next()) {
            IvyLog.logD("SIGNMAKER","Consider defined sign " + rs1.getInt("id") +
                  " " + rs1.getInt("userid") + " " + user_id);
            int did = rs1.getInt("id");
            if (used_ids.contains(did)) {
               IvyLog.logD("SIGNMAKER","Sign id " + did + " already used");
               continue;
             }
            int uid = rs1.getInt("userid");
            if (uid > 0 && uid != user_id) continue;
            if (uid <= 0 && bestuid > 0) continue;
            cnts = rs1.getString("contents");
            bestid = did;
            bestuid = uid;
          }
         
         if (bestid <= 0) {
            IvyLog.logE("SIGNMAKER","Problem loading definition: `" + name + 
                  "' user=" + user_id);
            cnts = "# Bad Sign Name";
          }
         else {
            IvyLog.logD("SIGNMAKER","Loaded sign " + name + " " + bestid + " " +
                  bestuid);
            used_ids.add(bestid);
            if (do_counts) {
               String q3 = "SELECT * FROM iQsignUseCounts WHERE defineid = ? AND userid = ?";
               PreparedStatement st3 = sql.prepareStatement(q3);
               st3.setInt(1,bestid);
               st3.setInt(2,user_id);
               ResultSet rs3 = st3.executeQuery();
               if (rs3.next()) {
                  int count = rs3.getInt("count");
                  String q4 = "UPDATE iQsignUseCounts SET count = ?, " +
                        "last_used = CURRENT_TIMESTAMP " +
                        "WHERE defineid = ? AND userid = ?";
                  PreparedStatement st4 = sql.prepareStatement(q4);
                  st4.setInt(1,count+1);
                  st4.setInt(2,bestid);
                  st4.setInt(3,user_id);
                  st4.execute();
                }
               else {
                  String q5 = "INSERT INTO iQsignUseCounts(defineid,userid,count) " +
                     "VALUES (?,?,1)";
                  PreparedStatement st5 = sql.prepareStatement(q5);
                  st5.setInt(1,bestid);
                  st5.setInt(2,user_id);
                  st5.execute();
                }
             }
          }
         
         return cnts;
       }
      catch (SQLException e) {
         IvyLog.logE("SIGNMAKER","Database problem on saved: ",e);
         SignMaker.clearDatabase();
       }
    }
   
   return null;
}



void clearContents()
{
   for (int i = 0; i < text_regions.length; ++i) {
      text_regions[i] = null;
    }
   for (int i = 0; i < image_regions.length; ++i) {
      image_regions[i] = null;
    }
}


/********************************************************************************/
/*                                                                              */
/*      Layout methods                                                          */
/*                                                                              */
/********************************************************************************/

private void setDimensions(double w,double h)
{
   // compute sizing for each of the regions
   SignMakerComponent c0 = text_regions[5];
   if (c0 == null) c0 = image_regions[6];
   SignMakerComponent c1 = text_regions[4];
   if (c1 == null) c1 = image_regions[5];
   
   boolean haveborder = (image_regions[0] != null);
   
   double [] rows = new double[5];
   rows[0] = getRelativeHeight(image_regions[3],c0,image_regions[4]);
   rows[1] = getRelativeHeight(text_regions[1]);
   rows[2] = getRelativeHeight(text_regions[2]);
   rows[3] = getRelativeHeight(text_regions[3]);
   rows[4] = getRelativeHeight(image_regions[1],c1,image_regions[2]);
   
   double tot = 0;
   for (int i = 0; i < rows.length; ++i) tot += rows[i];
   if (tot == 0) return;
   
   double h0 = h;
   double y0 = 0;
   double w0 = w;
   double x0 = 0;
   double ypos = 0;
   if (haveborder) {
      h0 = h * 0.8;
      ypos = h * 0.1;
      w0 = w * 0.8;
      x0 = w * 0.1;
    }
   for (int i = 0; i < rows.length; ++i) {
      rows[i] = y0 + rows[i] / tot * h0;
    }
   
   ypos = setPositions(x0,w0,ypos,rows[0], image_regions[3],c0,image_regions[4]);
   ypos = setPositions(x0,w0,ypos,rows[1],text_regions[1]);
   ypos = setPositions(x0,w0,ypos,rows[2],text_regions[2]);
   ypos = setPositions(x0,w0,ypos,rows[3],text_regions[3]);
   ypos = setPositions(x0,w0,ypos,rows[4],image_regions[1],c1,image_regions[2]);
}



private double getRelativeHeight(SignMakerComponent ... cset)
{
   double level = 0;
   double v;
   for (SignMakerComponent c : cset) {
      if (c == null) continue;
      int lvl = c.getSizeLevel();
      if (Math.abs(lvl) > 5) v = 1;
      else if (lvl >= 0) v = SCALE_VALUES[lvl];
      else v = 1.0 / SCALE_VALUES[-lvl];
      level = Math.max(v,level);
    }
   return level;
}





double setPositions(double x,double w,double y,double h,SignMakerComponent c0,SignMakerComponent c1,SignMakerComponent c2)
{
   // c0 and c2 are possible images, c1 is text or image
   
   if (c0 == null && c2 == null) {
      return setPositions(x,w,y,h,c1);
    }
   
   double w0 = h;
   if (c0 != null) {
      Rectangle2D r = new Rectangle2D.Double(x,y,w0,h);
      item_positions.put(c0,r);
    }
   if (c2 != null) {
      Rectangle2D r = new Rectangle2D.Double(x+w-w0,y,w0,h);
      item_positions.put(c2,r); 
    }
   if (c1 != null && c1.isImage()) {
      double w1 = w-2*w0;
      if (w1 > h) {
         w0 += (w1-h)/2;
         w1 = h;
       }
      else if (h > w1) {
         h = w1;
       }
      Rectangle2D r = new Rectangle2D.Double(x+w0,y,w1,h);
      item_positions.put(c1,r);
    }
   else if (c1 != null) {
      Rectangle2D r = new Rectangle2D.Double(x+w0,y,w-2*w0,h);
      item_positions.put(c1,r);
    }
   
   return y+h;
}


double setPositions(double x,double w,double y,double h,SignMakerComponent c0)
{
   // c0 is text region
   
   if (c0 != null) {
      Rectangle2D r = new Rectangle2D.Double(x,y,w,h);
      item_positions.put(c0,r);
    }
   
   return y+h;
}

}       // end of class SignMakerSign




/* end of SignMakerSign.java */

