/****************************************************************************

 The contents of this file are subject to the Mozilla Public License
 Version 1.1 (the "License"); you may not use this file except in
 compliance with the License. You may obtain a copy of the License at
 http://www.mozilla.org/MPL/ 

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 the specific language governing rights and limitations under the License. 

 The Original Code is TEAM Engine.

 The Initial Developer of the Original Code is Northrop Grumman Corporation
 jointly with The National Technology Alliance.  Portions created by
 Northrop Grumman Corporation are Copyright (C) 2005-2006, Northrop
 Grumman Corporation. All Rights Reserved.

 Contributor(s): L. Perez

 ****************************************************************************/
package com.occamlab.te.web;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.occamlab.te.realm.PasswordStorage;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;

// TODO: remove this after debugging
import java.io.PrintWriter;

import org.apache.xerces.impl.Constants;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.w3c.dom.Element;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSOutput;
import org.w3c.dom.ls.LSSerializer;

import java.security.SecureRandom;
import java.math.BigInteger;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.util.Properties;


// TODO refactor: buildDOM3LoadAndSaveFactory should not be in a listener
import com.occamlab.te.web.listeners.CleartextPasswordContextListener;

// TODO:
// * inform users that location of smtp config has changed in web.xml
// * check that EmailLogServlet still works despite this change

/**
 * Handles requests to reset user password (forgotten password).
 * 
 */
public class ForgotPasswordServlet extends HttpServlet {

    private static final long serialVersionUID = 7428127065308163495L;

    Config conf;

    public void init() throws ServletException {
        conf = new Config();
    }

    /**
     * Handles request to reset user password
     * Generate link to reset password and send it by email
     *
     */
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException {
        try {
            // Get username from request
            String username = request.getParameter("username");
            File userDir = new File(conf.getUsersDir(), username);
            
            // Debug
            response.setContentType("text/plain");
            PrintWriter out = response.getWriter();
            out.println("username: " + username);
            
            if (userDir.exists()) {
                // Username exists
                try {
                    // Create a domBuilder for xml dom manipulation
                    DocumentBuilder domBuilder = null;
                    try {
                        domBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
                    } catch (ParserConfigurationException e) {
                        throw new ServletException(e);
                    }
                    // Get user file
                    File userFile = new File(userDir, "user.xml");
                    // Parse userFile xml
                    Document doc = domBuilder.parse(userFile);
                    Element root = doc.getDocumentElement();

                    // Remove old nodes: forgotPasswordToken and forgotPasswordDate if exist
                    NodeList targets = doc.getElementsByTagName("forgotPasswordToken");
                    int toDelete = targets.getLength();
                    for (int i = toDelete-1; i >=0 ; i--) {
                        root.removeChild(targets.item(i));
                    }
                    targets = doc.getElementsByTagName("forgotPasswordDate");
                    toDelete = targets.getLength();
                    for (int i = toDelete-1; i >=0 ; i--) {
                        root.removeChild(targets.item(i));
                    }

                    // Create a random token for password reset. User will follow a link sent by email, containing this token.
                    SecureRandom random = new SecureRandom();
                    String token = new BigInteger(130, random).toString(32);
                    // Create forgotPassWordToken node
                    Element forgotPasswordTokenNode = doc.createElement("forgotPasswordToken");
                    forgotPasswordTokenNode.appendChild(doc.createTextNode(token));

                    // Create date node
                    TimeZone tz = TimeZone.getTimeZone("UTC");
                    DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'"); // Quoted "Z" to indicate UTC, no timezone offset
                    df.setTimeZone(tz);
                    String nowAsISO = df.format(new Date());
                    // Create forgotPassWordDate node
                    Element forgotPasswordDateNode = doc.createElement("forgotPasswordDate");
                    forgotPasswordDateNode.appendChild(doc.createTextNode(nowAsISO));

                    // Append nodes to root
                    root.appendChild(forgotPasswordTokenNode);
                    root.appendChild(forgotPasswordDateNode);

                    // Create a serializer to overwrite user file
                    DOMImplementationLS lsFactory = CleartextPasswordContextListener.buildDOM3LoadAndSaveFactory(); // TODO refactor: move buildDOM3LoadAndSaveFactory elsewhere, not in a listener
                    LSSerializer serializer = lsFactory.createLSSerializer();
                    serializer.getDomConfig().setParameter(Constants.DOM_XMLDECL, Boolean.FALSE);
                    serializer.getDomConfig().setParameter(Constants.DOM_FORMAT_PRETTY_PRINT, Boolean.TRUE);
                    LSOutput output = lsFactory.createLSOutput();
                    output.setEncoding("UTF-8");
                    // Overwrite contents of user file                   
                    output.setByteStream(new FileOutputStream(userFile, false));
                    serializer.write(doc, output);

                    // Get user email
                    Node pwNode = doc.getElementsByTagName("email").item(0);
                    if (null == pwNode) {
                        // TODO
                    }
                    String userEmail = pwNode.getTextContent();

                    // Send token by email
                    if (sendResetPasswordEmail(getServletContext().getInitParameter("mail.smtp.host"),
                            getServletContext().getInitParameter("mail.smtp.userid"),
                            getServletContext().getInitParameter("mail.smtp.passwd"),
                            userEmail, "noreply@teamengine.com",
                            username, token)) {
                        request.setAttribute("emailStatus", "Email sent Succesfully");
                    } else {
                        request.setAttribute("emailStatus", "Email failed");
                    }

                    /*
                    Node pwNode = doc.getElementsByTagName("password").item(0);
                    if (null == pwNode) {
                        continue;
                    }
                    String password = pwNode.getTextContent();
                    if (password.split(":").length == 5) {
                        break;
                    }
                    pwNode.setTextContent(PasswordStorage.createHash(password));
                    // overwrite contents of file
                    output.setByteStream(new FileOutputStream(userFile, false));
                    serializer.write(doc, output);*/
                } catch (Exception e) {
                    throw new ServletException(e);
                }
            }
            //response.sendRedirect("test.jsp");
        }
        catch (Exception e) {
            throw new ServletException(e);
        }
        /*try {
            String username = request.getParameter("username");
            String password = request.getParameter("password");
            String hashedPassword = PasswordStorage.createHash(password);
            String email = request.getParameter("email");
            File userDir = new File(conf.getUsersDir(), username);
            if (userDir.exists()) {
                String url = "register.jsp?error=duplicate&username=" + username;
                if (email != null) {
                    url += "&email=" + email;
                }
                response.sendRedirect(url);
            } else {
                userDir.mkdirs();
                File xmlfile = new File(userDir, "user.xml");
                PrintStream out = new PrintStream(new FileOutputStream(xmlfile));
                out.println("<user>");
                out.println(" <name>" + username + "</name>");
                out.println(" <roles>");
                out.println("  <name>user</name>");
                out.println(" </roles>");
                out.println(" <password>" + hashedPassword + "</password>");
                out.println(" <email>" + email + "</email>");
                out.println("</user>");
                out.close();
                response.sendRedirect("test.jsp");
            }
        } catch (Exception e) {
            throw new ServletException(e);
        }*/
    }

    boolean sendResetPasswordEmail(String host, String userId, String password,
            String to, String from, String username, String forgotPasswordToken) {
        boolean success = true;

        // Create subject
        String subject = "TeamEngine password reset";
        // Create message
        String message = "Please paste this url in your browser to reset your password:\n";
        message += getServletContext().getInitParameter("base.url");
        message += "/forgotPasswordReset";
        message += "?token=";
        message += forgotPasswordToken;
        message += "&username=";
        message += username;
        message += "\n";
        message += "This url will expire after 24h.";

        System.out.println("host: " + host);
        System.out.println("userId: " + userId);
        System.out.println("to: " + to);
        System.out.println("from: " + from);
        System.out.println("subject: " + subject);
        System.out.println("message: " + message);

        // create some properties and get the default Session
        Properties props = System.getProperties();
        props.setProperty("mail.smtp.host", host);
        props.setProperty("mail.smtp.auth", "true");

        Session session = Session.getInstance(props, null);
        session.setDebug(true);

        try {
            // create a message
            MimeMessage msg = new MimeMessage(session);
            msg.setFrom(new InternetAddress(from));
            InternetAddress[] address = { new InternetAddress(to) };
            msg.setRecipients(Message.RecipientType.TO, address);
            msg.setSubject(subject);

            // create and fill the first message part
            MimeBodyPart mbp1 = new MimeBodyPart();
            mbp1.setText(message);

            // create the Multipart and add its parts to it
            Multipart mp = new MimeMultipart();
            mp.addBodyPart(mbp1);

            // add the Multipart to the message
            msg.setContent(mp);

            // set the Date: header
            msg.setSentDate(new Date());

            // connect to the transport
            Transport trans = session.getTransport("smtp");
            trans.connect(host, userId, password);

            // send the message
            trans.sendMessage(msg, msg.getAllRecipients());

            // smtphost
            trans.close();

        } catch (MessagingException mex) {
            success = false;
            mex.printStackTrace();
            Exception ex = null;
            if ((ex = mex.getNextException()) != null) {
                ex.printStackTrace();
            }
        }
        return success;
    }

}