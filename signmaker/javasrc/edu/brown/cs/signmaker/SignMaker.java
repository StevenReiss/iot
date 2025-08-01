/********************************************************************************/
/*										*/
/*		SignMaker.java							*/
/*										*/
/*	Main program make sign image from text specification			*/
/*										*/
/********************************************************************************/

package edu.brown.cs.signmaker;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Connection;
import java.util.Properties;

import javax.imageio.ImageIO;

import org.json.JSONException;
import org.json.JSONObject;

import edu.brown.cs.ivy.file.IvyDatabase;
import edu.brown.cs.ivy.file.IvyFile;
import edu.brown.cs.ivy.file.IvyLog;


public class SignMaker implements SignMakerConstants {


/********************************************************************************/
/*										*/
/*	Main program								*/
/*										*/
/********************************************************************************/

public static void main(String [] args)
{
   System.setProperty("java.awt.headless","true");

   SignMaker sm = new SignMaker(args);
   sm.process();
}



/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private boolean 	run_server;
private RunContext	base_context;

private static File	base_directory;

private static Connection sql_database;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

private SignMaker(String [] args)
{
   run_server = false;
   base_context = new RunContext();
   base_context.setInputStream(System.in);
   base_context.setOutputStream(System.out);
   
   base_directory = null;
   sql_database = null;

   scanArgs(args);
   
   setupBase();
}



private void setupBase()
{
   if (base_directory == null) {
      base_directory = findBaseDirectory();
    }
   
   sql_database = null;
   checkDatabase();
}



private static void checkDatabase()
{
   if (sql_database != null) return;
   
   File f1 = new File(base_directory,"secret");
   File f2 = new File(f1,"signmaker.props");
   Properties props = new Properties();
   props.put("database","iqsign");
   if (f2.exists()) {
      try (FileInputStream fis = new FileInputStream(f2)) {
         props.loadFromXML(fis);
       }
      catch (IOException e) { }
    }
   
   String dbnm = props.getProperty("database","iqsign");
   try {
      File dbf = SignMaker.getDatabasePropertyFile();
      IvyLog.logI("SIGNMAKER","Using database file " + dbf + " " + dbnm);
      IvyDatabase.setProperties(dbf);
      sql_database = IvyDatabase.openDatabase(dbnm);
    }
   catch (Exception e) {
      IvyLog.logE("SIGNMAKER","Problem connecting to database ",e);
    }
   System.err.println("Connected to database " + dbnm);
}



/********************************************************************************/
/*										*/
/*	Argument processing							*/
/*										*/
/********************************************************************************/

private void scanArgs(String [] args)
{
   for (int i = 0; i < args.length; ++i) {
      boolean more = i+1 < args.length;
      if (args[i].startsWith("-")) {
	 if (args[i].startsWith("-w") && more) {                // -width #
	    try {
	       base_context.setWidth(Integer.parseInt(args[++i]));
	     }
	    catch (NumberFormatException e) {
	       badArgs();
	     }
	  }
	 else if (args[i].startsWith("-h") && more) {           // -height #
	    try {
	       base_context.setHeight(Integer.parseInt(args[++i]));
	     }
	    catch (NumberFormatException e) {
	       badArgs();
	     }
	  }
	 else if (args[i].startsWith("-u") && more) {           // -userid #
	    try {
	       base_context.setUserId(Integer.parseInt(args[++i]));
	     }
	    catch (NumberFormatException e) {
	       badArgs();
	     }
	  }
	 else if (args[i].startsWith("-d") && more) {           // -directory <dir>
	    base_directory = new File(args[++i]);
	  }
	 else if (args[i].startsWith("-i") && more) {           // -input <input file>
	   try {
	      base_context.setInputStream(new File(args[++i]));
	    }
	   catch (IOException e) {
	      badArgs();
	    }
	  }
	 else if (args[i].startsWith("-o") && more) {           // -output <output file>
	    try {
	       base_context.setOutputStream(new File(args[++i]));
	     }
	    catch (IOException e) {
	       badArgs();
	     }
	  }
	 else if (args[i].startsWith("-s")) {                   // -server
	    run_server = true;
	  }
	 else if (args[i].startsWith("-c")) {                   // -counts
	    base_context.setDoCounts(true);
	  }
         else if (args[i].startsWith("-LD")) {                          // -LDebug
	    IvyLog.setLogLevel(IvyLog.LogLevel.DEBUG);
	  }
	 else if (args[i].startsWith("-LI")) {                          // -LInfo
	    IvyLog.setLogLevel(IvyLog.LogLevel.INFO);
	  }
	 else if (args[i].startsWith("-LW")) {                          // -LWarning
	    IvyLog.setLogLevel(IvyLog.LogLevel.WARNING);
	  }
	 else if (args[i].startsWith("-L") && i+1 < args.length) {      // -Log <file>
	    IvyLog.setLogFile(args[++i]);
	  }
         else if (args[i].startsWith("-S")) {                           // -Stderr
	    IvyLog.useStdErr(true);
	  }
	 else badArgs();
       }
      else if (base_context.getInputStream() == System.in) {
	 try {
	    base_context.setInputStream(new File(args[i]));
	  }
	 catch (IOException e) {
	    badArgs();
	  }
       }
      else if (base_context.getOutputStream() == System.out) {
	 try {
	    base_context.setOutputStream(new File(args[i]));
	  }
	 catch (IOException e) {
	    badArgs();
	  }
       }
      else badArgs();
    }
}



private void badArgs()
{
   System.out.println("signmaker [-w <width>] [-h <height>] [-u <userid>] [-i <source>] [-o <target>]");
   System.exit(1);
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

static File getSvgLibrary()
{
   if (base_directory == null) return null;

   File f1 = new File(base_directory,"svgimagelib");
   if (!f1.exists()) return null;

   return f1;
}


static File getImageLibrary()
{
   if (base_directory == null) return null;
   
   File f1 = new File(base_directory,"savedimages");
   if (!f1.exists()) return null;
   
   return f1;
}

static File getDatabasePropertyFile()
{
   if (base_directory == null) return null;
   File f1 = new File(base_directory,"secret");
   File f2 = new File(f1,"Database.props");
   return f2;
}


static String getFontAwesomeToken()
{
   if (base_directory == null) return null;
   File f1 = new File(base_directory,"secret");
   File f2 = new File(f1,"fatoken");
   try {
      String token = IvyFile.loadFile(f2);
      if (token != null) return token.trim();
    }
   catch (IOException e) {
      IvyLog.logE("SIGNMAKER","Problem reading fa token",e);
    }

   return null;
}


static Connection getSqlDatabase()
{
   checkDatabase();
   return sql_database;
}

static void clearDatabase()
{
   sql_database = null;
}



/********************************************************************************/
/*										*/
/*	Processing methods							*/
/*										*/
/********************************************************************************/

private void process()
{
   if (run_server) {
      base_context = null;
      processServer();
    }
   else {
      try {
	 processContext(base_context);
       }
      catch (IOException e) {
	 IvyLog.logE("SIGNMAKER","Problem saving image: ",e);
       }
      catch (SignMakerException e) {
	 IvyLog.logE("SIGNMAKER","Problem processing sign data: ",e);
	 e.printStackTrace();
       }
      System.exit(0);
    }
}



private void processContext(RunContext ctx) throws IOException, SignMakerException
{
   SignMakerParser p = new SignMakerLineParser(ctx.getUserId(),ctx.getDoCounts());
   SignMakerSign ss = p.parse(ctx.getInputStream());
   BufferedImage bi = ss.createSignImage(ctx.getWidth(),ctx.getHeight());
// System.err.println("Result image: " + bi);
   ImageIO.write(bi,"png",ctx.getOutputStream());
   ctx.finish();
}



private void processServer()
{
   ServerThread sthrd = new ServerThread();
   sthrd.start();
}


/********************************************************************************/
/*										*/
/*	Setup methods								*/
/*										*/
/********************************************************************************/

private static File findBaseDirectory()
{
   File f1 = new File(System.getProperty("user.dir"));
   for (File f2 = f1; f2 != null; f2 = f2.getParentFile()) {
     if (isBaseDirectory(f2)) return f2;
    }
   File f3 = new File(System.getProperty("user.home"));
   if (isBaseDirectory(f3)) return f3;

   File fc = new File("/vol");
   File fd = new File(fc,"iot");
   if (isBaseDirectory(fd)) return fd;

   File fa = new File("/pro");
   File fb = new File(fa,"iot");
   if (isBaseDirectory(fb)) return fb;

   return null;
}


private static boolean isBaseDirectory(File dir)
{
   File f2 = new File(dir,"secret");
   if (!f2.exists()) return false;

   File f3 = new File(f2,"Database.props");
   File f4 = new File(dir,"savedimages");
   File f5 = new File(dir,"svgimagelib");  //TODO -- this isn't in my stuff :(
   if (f3.exists() && f4.exists() && f5.exists()) return true;

   return false;
}


/********************************************************************************/
/*										*/
/*	Server Thread								*/
/*										*/
/********************************************************************************/

private class ServerThread extends Thread {

   private ServerSocket server_socket;

   ServerThread() {
      super("SignMakerServerThread");
      try {
	 server_socket = new ServerSocket(SERVER_PORT);
       }
      catch (IOException e) {
         IvyLog.logE("Can't create server socket on " + SERVER_PORT,e);
	 System.out.println("signmaker: Can't create server socket on " + SERVER_PORT);
	 System.exit(1);
       }
      IvyLog.logT("SIGNMAKER","Server running on " + SERVER_PORT);
    }

   @Override public void run() {
      for ( ; ; ) {
	 try {
	    Socket client = server_socket.accept();
	    createClient(client);
	  }
	 catch (IOException e) {
	    IvyLog.logE("SIGNMAKER","Error on server accept",e);
	    server_socket = null;
	    break;
	  }
       }
      System.exit(0);
    }

}	// end of inner class ServerThread



/********************************************************************************/
/*										*/
/*	Client managment							*/
/*										*/
/********************************************************************************/

private void createClient(Socket s)
{
   ClientThread cthread = new ClientThread(s);
   cthread.start();
}


private class ClientThread extends Thread {

   private Socket client_socket;

   ClientThread(Socket s) {
      super("SignMakerClient_" + s.getRemoteSocketAddress());
      client_socket = s;
      IvyLog.logD("SIGNMAKER","CLIENT " + s.getRemoteSocketAddress());
    }

   @Override public void run() {
      ByteArrayOutputStream output = null;
      JSONObject result = new JSONObject();
      try {
         InputStream ins = client_socket.getInputStream();
         InputStreamReader fr = new InputStreamReader(ins); 
         char [] buf = new char[10240];
         int len =  fr.read(buf);
         IvyLog.logD("SIGNMAKER","Read length = " + len);
         String args = new String(buf,0,len);
//       String args = IvyFile.loadFile(client_socket.getInputStream());
         IvyLog.logD("SIGNMAKER","CLIENT INPUT: " + args);
         JSONObject argobj = new JSONObject(args);
         RunContext ctx = new RunContext();
         ctx.setWidth(argobj.optInt("width"));
         ctx.setHeight(argobj.optInt("height"));
         ctx.setUserId(argobj.optInt("userid",-1));
         ctx.setDoCounts(argobj.optBoolean("counts"));
         String inf = argobj.optString("infile",null);
         if (inf != null) {
            ctx.setInputStream(new File(inf));
          }
         else {
            String cnts = argobj.getString("contents");
            ctx.setInputStream(cnts);
          }
         String otf = argobj.optString("outfile",null);
         if (otf != null) ctx.setOutputStream(new File(otf));
         else {
            output = new ByteArrayOutputStream();
            ctx.setOutputStream(output);
          }
         processContext(ctx);
         result.put("status","OK");
         if (output != null) {
            result.put("image",output.toByteArray());
          }
       }
      catch (IOException e) {
         result.put("status","ERROR");
         result.put("message",e.toString());
       }
      catch (JSONException e) {
         result.put("status","ERROR");
         result.put("message",e.toString());
       }
      catch (SignMakerException e) {
         result.put("status","ERROR");
         result.put("message",e.toString());
       }
   
      try {
         OutputStreamWriter otw = new OutputStreamWriter(client_socket.getOutputStream());
         otw.write(result.toString(2));
         otw.close();
       }
      catch (IOException e) {
   
       }
    }

}	// end of inner class ClientThread




/********************************************************************************/
/*										*/
/*	Processing context							*/
/*										*/
/********************************************************************************/

private class RunContext {

   private InputStream	   input_stream;
   private OutputStream    output_stream;
   private int		   sign_width;
   private int		   sign_height;
   private int		   user_id;
   private boolean	   do_counts;
   private File 	   output_file;
   private File 	   data_file;

   RunContext() {
      input_stream = null;
      output_stream = null;
      sign_width = DEFAULT_WIDTH;
      sign_height = 0;
      user_id = -1;
      do_counts = false;
      output_file = null;
      data_file = null;
    }

   void setInputStream(InputStream ins) 		{ input_stream = ins; }
   void setInputStream(File f) throws IOException {
      input_stream = new FileInputStream(f);
    }
   void setInputStream(String cnts) {
      input_stream = new ByteArrayInputStream(cnts.getBytes());
    }

   void setOutputStream(OutputStream ots)		{ output_stream = ots; }
   void setOutputStream(File f) throws IOException {
      output_file = f;
      data_file = new File(f.getPath() + ".temp");
      output_stream = new FileOutputStream(data_file);
    }

   void setWidth(int w) {
      if (w > 0) sign_width = w;
    }
   void setHeight(int h) {
      if (h > 0) sign_height = h;
    }

   void setUserId(int uid)				{ user_id = uid; }

   int getWidth()					{ return sign_width; }
   int getHeight() {
      if (sign_height == 0) {
         sign_height = sign_width / 16 * 9;
       }
      return sign_height;
    }
   InputStream getInputStream() 			{ return input_stream; }
   OutputStream getOutputStream()			{ return output_stream; }
   int getUserId()					{ return user_id; }

   boolean getDoCounts()				{ return do_counts; }
   void setDoCounts(boolean fg) 			{ do_counts = fg; }

   void finish() {
      if (data_file == null) return;
      try {
	 output_stream.close();
       }
      catch (IOException e) { }
      data_file.renameTo(output_file);
    }

}	// end of inner class RunContext



}	// end of class SignMaker



/* end of SignMaker.java */
