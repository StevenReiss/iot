/********************************************************************************/
/*                                                                              */
/*              SignMakerText.java                                              */
/*                                                                              */
/*      Container for contents of a text block                                  */
/*                                                                              */
/********************************************************************************/



package edu.brown.cs.signmaker;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.geom.Rectangle2D;
import java.util.Stack;

import javax.swing.JComponent;
import javax.swing.JLabel;

import edu.brown.cs.ivy.swing.SwingColorSet;

class SignMakerText extends SignMakerComponent implements SignMakerConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private StringBuffer            current_text;
private boolean                 is_empty;
private Stack<String>           nest_items;
private int                     tab_level;

private String[] TABS = { "", "  ", "    ", "      ", "        " };

/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

SignMakerText()
{
   current_text = new StringBuffer();
   current_text.append("<html>");
   is_empty = true;
   nest_items = new Stack<>();
   tab_level = 0;
}


/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

@Override JComponent setupComponent(Rectangle2D r,JComponent par,SignMakerSign sgn)
{
   String text = sgn.mapString(current_text.toString());
   JLabel lbl = new JLabel(text,JLabel.CENTER);
   lbl.setFont(par.getFont());
   lbl.setVerticalAlignment(JLabel.CENTER);
   Dimension sz = lbl.getPreferredSize();
   
   Font lastfont = lbl.getFont();
   double szw  = r.getWidth()/sz.getWidth();
   double szh = r.getHeight()/sz.getHeight();
   double scale = Math.min(szw,szh);
   float s = lastfont.getSize2D();
   s *= scale * 0.9;
   Font newfont = lastfont.deriveFont(s);
   lbl.setFont(newfont);
   if (text.contains("++")) {
      String t1 = text.replace("+"," ");
      lbl.setText(t1);
    }
   if (tab_level != 0 ) {
      String t0 = TABS[tab_level];
      String t1 = t0 + text + t0;
      lbl.setText(t1);
    }
   
   return lbl;
}



@Override boolean isEmpty()
{
   return is_empty;
}


@Override boolean isImage()
{
   return false;
}



/********************************************************************************/
/*                                                                              */
/*      Setup methods                                                           */
/*                                                                              */
/********************************************************************************/

void addText(char c)
{
   current_text.append(c);
   is_empty = false;
}


void addText(String s)
{
   for (int i = 0; i < s.length(); ++i) {
      char c = s.charAt(i);
      if (c == '*' || c == '_') {
         int ct = 1;
         while (i+1 < s.length() && s.charAt(i+1) == c) {
            ++ct;
            ++i;
          }
         if (ct == 1) {
            if (nest_items.isEmpty() || 
                  (!nest_items.peek().equals("i") &&
                        !nest_items.peek().equals("em"))) {
               setItalic();
             }
            else pop();
          }
         else if (ct == 2) {
            if (nest_items.isEmpty() || 
                  (!nest_items.peek().equals("b") && 
                        !nest_items.peek().equals("strong"))) {
               setBold();
             }
            else pop();
          }
         else if (ct == 3) {
            int popct = 0;
            boolean done = false;
            while (!nest_items.isEmpty() && !done && popct < 2) {
               switch (nest_items.peek()) {
                  case "b" :
                  case "strong" :
                  case "i" :
                  case "em" :
                     pop();
                     ++popct;
                     break;
                  default : 
                     done = true;
                     break;
                }
             }
            if (popct == 0) {
               setItalic();
               setBold();
             }
          }
       }
      else {
         current_text.append(c);
         is_empty = false;
       }
    }
}


void setTabLevel(int lvl)
{
   if (lvl < 0) lvl = 0;
   if (lvl > 4) lvl = 4;
   tab_level = lvl;
}

boolean setFont(Color c,String family)
{
   if (family != null) {
      family = SignMakerSign.getFontFamily(family);
      if (family == null) return false;
    }
   String cs = null;
   if (c != null) cs = SwingColorSet.getColorName(c);
   setFont(cs,family);
   
   return true;
}



private void setFont(String color,String family)
{
   String cnts = "";
   if (color == null && family == null) return;
   if (family != null) cnts += " family='" + family + "'";
   if (color != null) cnts += " color='" + color + "'";
   current_text.append("<font" + cnts + ">");
   nest_items.push("font");
}


void setBold()
{
   current_text.append("<strong>");
   nest_items.push("strong");
}

void setItalic()
{
   current_text.append("<em>");
   nest_items.push("em");
}


void setUnderline()
{
   current_text.append("<u>");
   nest_items.push("u");
}

void setNormal()
{
   while (!nest_items.isEmpty()) {
      String what = nest_items.peek();
      switch (what) {
         case "i" :
         case "u" :
         case "b" :
         case "em" :
         case "strong" :
            pop();
            break;
         default :
            return;
       }
    }
}



void pop()
{
   if (nest_items.isEmpty()) return;
   String what = nest_items.pop();
   current_text.append("</" + what + ">");
}


void popAll()
{
   while (!nest_items.isEmpty()) {
      pop();
    }
}



/********************************************************************************/
/*                                                                              */
/*      Output methods                                                          */
/*                                                                              */
/********************************************************************************/

@Override public String toString()
{
   return current_text.toString();
}


}       // end of class SignMakerText




/* end of SignMakerText.java */

