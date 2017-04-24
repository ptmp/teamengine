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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import javax.servlet.RequestDispatcher;

// TODO refactor: buildDOM3LoadAndSaveFactory should not be in a listener
import com.occamlab.te.web.listeners.CleartextPasswordContextListener;

// TODO:
// * inform users that location of smtp config has changed in web.xml
// * check that EmailLogServlet still works despite this change

/**
 * Handles requests to reset user password (forgotten password).
 * 
 */
public class ForgotPasswordResetServlet extends HttpServlet {

    Config conf;

    public void init() throws ServletException {
        conf = new Config();
    }

    /**
     * Checks if token is valid and show form to reset password
     *
     */
    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException {
        try {
            String username = request.getParameter("username");
            String forgotPasswordToken = request.getParameter("token");
            
            // Check if token is valid
            if (!checkToken(username, forgotPasswordToken)) {
                // Invalid token
                request.setAttribute("error", true);
            }
            
            RequestDispatcher rd = request.getRequestDispatcher("forgotPasswordReset.jsp");
            rd.forward(request, response);

        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    /**
     * Check if token is valid and set new password
     *
     */
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException {
        try {
            String username = request.getParameter("username");
            String forgotPasswordToken = request.getParameter("token");
            
            // Check if token is valid
            if (!checkToken(username, forgotPasswordToken)) {
                // Invalid token
                request.setAttribute("error", true);
            }
            else {
                // Token is valid
                String password = request.getParameter("password");
                String passwordConfirm = request.getParameter("password_confirm");

                // Check if password has been provided
                if (!(password.length() > 0)) {
                    request.setAttribute("error_password_required", true);
                }
                // Check if the two password match
                else if (!password.equals(passwordConfirm)) {
                    request.setAttribute("error_password_match", true);
                }
                else {
                    // All verifications OK, save new password
                    saveNewPassword(username, password);
                    request.setAttribute("done", true);
                }
            }
            RequestDispatcher rd = request.getRequestDispatcher("forgotPasswordReset.jsp");
            rd.forward(request, response);

        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    /**
     * Checks if username and forgotPasswordToken matches, and that token is not expired
     * @return true if match, otherwise false
     *
     */
    boolean checkToken(String username, String forgotPasswordToken)
                throws ServletException {
            File userDir = new File(conf.getUsersDir(), username);
                       
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

                    // Get user token
                    Node fptNode = doc.getElementsByTagName("forgotPasswordToken").item(0);
                    if (null == fptNode) {
                        // No token for this user
                        return false;
                    }
                    String userToken = fptNode.getTextContent();

                    // Check if tokens match
                    if (!userToken.equals(forgotPasswordToken)) {
                        // Tokens do not match
                        return false;
                    }

                    // TODO check if token is expired

                    // All validations passed, token is valid
                    return true;

                } catch (Exception e) {
                    throw new ServletException(e);
                }
            }
            else {
                // No user with this username
                return false;
            }
    }

    /**
     * Save new password in user file
     *
     */
    void saveNewPassword(String username, String password) throws ServletException {

        File userDir = new File(conf.getUsersDir(), username);
                   
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

                // Hash password
                String hashedPassword = PasswordStorage.createHash(password);
                // Get password node
                Node pwNode = doc.getElementsByTagName("password").item(0);
                // Update password in node
                pwNode.setTextContent(hashedPassword);

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

            } catch (Exception e) {
                throw new ServletException(e);
            }
        }

    }
}