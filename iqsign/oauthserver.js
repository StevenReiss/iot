/********************************************************************************/
/*										*/
/*		oauthserver.js							*/
/*										*/
/*	Oauth server using common dbms						*/
/*										*/
/*	Written by spr, based on node-oauth2-server				*/
/*										*/
/********************************************************************************/
"use strict";


const http = require('http');
const https = require('https');
const express = require('express');
const errorhandler = require('errorhandler');
const logger = require('morgan');
const bodyparser = require('body-parser');
const cors = require("cors");
const favicons = require("connect-favicons");
const session = require('express-session');
const cookieparser = require('cookie-parser');
const redis = require('redis');
const RedisStore = require('connect-redis')(session);
const redisClient = redis.createClient();
const uuid = require('node-uuid');
const exphbs = require("express-handlebars");
const handlebars = exphbs.create( { defaultLayout : 'main'});
const util = require('util');

const xmlbuilder = require("xmlbuilder");

const config = require("./config");
const oauthmodel = require('./modelps').Model;
const oauthserver = require('express-oauth-server');
const model = new oauthmodel();
const oauthcode = new oauthserver( {
   model: model,
   debug : true
});

const auth = require("./auth");
const db = require("./database");

const creds = config.getOauthCredentials();

	

/********************************************************************************/
/*										*/
/*	Actual server								*/
/*										*/
/********************************************************************************/

function setup()
{
   const app = express();

   app.engine('handlebars', handlebars.engine);
   app.set('view engine','handlebars');

   app.use(logger('combined'));

   app.use(favicons(__dirname + config.STATIC));

   app.oauth = oauthcode;

   app.use(bodyparser.urlencoded({ extended: true } ));
   app.use(bodyparser.json());

   app.use('/static',express.static(__dirname + config.STATIC));
   app.get('/robots.txt',(req1,res1) => { res1.redirect('/static/robots.txt')});

   app.use(cors());

   app.use(session( { secret : config.OAUTH_SESSION_KEY,
	 store : new RedisStore({ client: redisClient }),
	 saveUninitialized : true,
	 resave : true }));
   app.use(sessionManager);

   app.post("/oauth/token",handleAuthorizeToken);
   app.post("/token",handleAuthorizeToken);
   app.get("/oauth/token",handleAuthorizeToken);
   app.get('/oauth/authorize',handleAuthorizeGet);
   app.get("/authorize",handleAuthorizeGet);
   app.post('/oauth/authorize',handleAuthorizePost);
   app.post("/authorize",handleAuthorizePost);
   app.get('/oauth/login',handleOauthLoginGet);
   app.get('/login',handleOauthLoginGet);
   app.post('/oauth/login',auth.handleLogin);
   app.post('/login',auth.handleLogin);

   app.get("/default",displayDefaultPage);

   const server = app.listen(config.OAUTH_PORT);
   console.log(`HTTP Server listening on port ${config.OAUTH_PORT}`);

   try {
      const httpsserver = https.createServer(config.getHttpsCredentials(),app);
      httpsserver.listen(config.OAUTH_HTTPS_PORT);
      console.log(`HTTPS Server listening on port ${config.OAUTH_HTTPS_PORT}`);
    }
   catch (error) {
      console.log("Did not launch https server",error);
    }
}




/********************************************************************************/
/*										*/
/*	Session management							*/
/*										*/
/********************************************************************************/

function sessionManager(req,res,next)
{
   if (req.session.uuid == null) {
      req.session.uuid = uuid.v1();
      req.session.save();
      console.log("START SESSION",req.session);
    }
   next();
}

/* for testing */
function displayDefaultPage(req,res)
{
   console.log("DEFAULT PAGE",req.query,req.session,req.oauth);
   res.render("default");
}



function handle404(req,res)
{
   res.status(404);
   if (req.accepts('html')) {
      res.render('error404', { title: 'Page Not Found'});
    }
   else if (req.accepts('json')) {
      let rslt = { status : 'ERROR', reason: 'Invalid URL'} ;
      res.end(JSON.stringify(rslt));
    }
   else {
      res.type('txt').send('Not Found');
    }
}



/********************************************************************************/
/*										*/
/*	Action functions							*/
/*										*/
/********************************************************************************/

async function handleAuthorizeToken(req,res)
{
   let app = req.app;

   console.log("AUTHORIZE TOKEN",req.query,req.body,app.oauth);

   let opts = { };

   let fct = app.oauth.token(req,res,opts);
   
   if (req.session) {
      await req.session.destroy();
    }
   else {
      console.log("CHECK SESSION",req);
    }
   
   let tok1 = await fct(req,res);
   
   console.log("TOKEN DONE",tok1,res._header,res);
}


/********************************************************************************/
/*										*/
/*	Handle authorize requests						*/
/*										*/
/********************************************************************************/

async function handleAuthorizeGet(req,res)
{
   console.log("AUTHORIZE",req.originalUrl);

   console.log("GET AUTHORIZE",req.path,req.query,req.body,req.app.locals.user,req.session);
// console.log("REQUEST",req.headers);
// if (req.app.locals.original == null) {
//    req.app.locals.original = "https://" + req.headers.host + req.originalUrl;
//  }

   let user = req.app.locals.user;

// user = await db.query1("SELECT * FROM iQsignUsers WHERE username = 'spr'");
	
   if (!user) {
      let cinfo = await model.getClient(req.query.client_id,null);
      console.log("CINFO",cinfo);
      let who = "client_id=" + req.query.client_id;
      who += "&response_type=" + req.query.response_type;
      if (cinfo != null) who += "&client=" + cinfo.client;
      let redir = req.path;
      redir += "?client_id=" + req.query.client_id;
      redir +=	"&redirect_uri=" + req.query.redirect_uri;
      redir += "&response_type=" + req.query.response_type;
      redir += "&scope=" + req.query.scope;
      redir += "&state=" + req.query.state;
      redir = encodeURIComponent(redir);
      let rslt = '/oauth/login?' + who + '&redirect=' + redir;
      console.log("AUTHORIZE TO " + rslt);
      return res.redirect(rslt);
   }


   if (!user.valid) req.query.allowed = 'false';

   let oauthcode = req.app.oauth;
   req.body = req.query;

   req.app.locals.user = null;
   if (req.session) {
      await req.session.destroy();
    }
   else {
      console.log("CHECK SESSION",req);
    }

   console.log("PRESEND",res._header);

   let opts = { model : model,
	 authenticateHandler : authorizeAuthenticator(user) };
   let x = oauthcode.authorize( opts );
   let x1 = await x(req,res);

   console.log("AUTHORIZE DONE",user,res._header);
}


function authorizeAuthenticator(user)
{
   return { handle :
      function(req,res) {
	 return { id : user.id, email : user.email, username : user.username };
    }
    }
}



function handleAuthorizePost(req,res)
{
   console.log("POST AUTHORIZE",req.path,req.query,req.body,req.app.locals);
   req.query = req.body;
   return handleAuthorizeGet(req,res);
}


function handleOauthLoginGet(req,res)
{
   return auth.displayOauthLoginPage(req,res)
}



/********************************************************************************/
/*										*/
/*	Handle authorization complete						*/
/*										*/
/********************************************************************************/

function handleAuthorized(req,res)
{
   res.send("Secret area");
}


function handlePublic(req,res)
{
   res.send("Public area");
}


function handleOauthGet(req,res)
{
   console.log("OAUTH GET",req.body,req.query,req.path);
}


function handleOauthPost(req,res)
{
   console.log("OAUTH POST",req.body,req.path);
}


/********************************************************************************/
/*										*/
/*	Startup methods 							*/
/*										*/
/********************************************************************************/

setup();


/********************************************************************************/
/*										*/
/*	Exports 								*/
/*										*/
/********************************************************************************/

exports.handleAuthorizeToken = handleAuthorizeToken;
exports.handleAuthorizeGet = handleAuthorizeGet;
exports.handleAuthorizePost = handleAuthorizePost;
exports.handleOauthLoginGet = handleOauthLoginGet;
exports.handleOauthGet = handleOauthGet;
exports.handleOauthPost = handleOauthPost;







/* end of module oauthserver */
