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

    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException {
        try {
            String username = request.getParameter("username");
            String forgotPasswordToken = request.getParameter("token");
            //request.setAttribute("error", true);
            
            RequestDispatcher rd = request.getRequestDispatcher("forgotPasswordReset.jsp");
            rd.forward(request, response);

        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
}