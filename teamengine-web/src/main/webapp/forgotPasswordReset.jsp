<%@ page language="java" session="false"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<!-- ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

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

 +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++ -->
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en-US" lang="en-US">
	<head>
		<meta http-equiv="Content-Type" content="text/html; charset=utf-8"/>
		<title>Reset password</title>
	</head>
	<body>
		<%@ include file="header.jsp" %>
		<h2>Reset password</h2>
<%
if (request.getParameter("error") != null) {
	out.println("<span style=\"color: red\">This url is invalid or has expired.</span>");
}
%>
		<form method="post" action="forgotPasswordReset">
			<p>
				Enter your new password:<br/>
				<br/>
				Password: <input type="text" name="password"/><br/>
				<br/>
				Confirm your new password:<br/>
				<br/>
				Password: <input type="text" name="password_confirm"/><br/>
				<br/>
				<input type="submit" value="Submit"/><br/>
			</p>
		</form>
		<%@ include file="footer.jsp" %>
	</body>
</html>
