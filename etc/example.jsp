<!-- @APP_NAME@ @BUILD_VERSION@ @RELEASE_CANDIDATE@ (@BUILD_DATE@ / @BUILD_TIME@) -->

<%@ page contentType="application/x-java-jnlp-file" %>
<%
    String codebase = request.getScheme() + "://" + request.getServerName();
    if (request.getServerPort() != (request.isSecure() ? 443 : 80))
        codebase += ":" + request.getServerPort();
    codebase += request.getContextPath();
%>

<?xml version="1.0" encoding="ISO-8859-1"?>
<jnlp
	spec="1.0+"
	codebase="<%= codebase %>"
	href="<%= request.getRequestURI() %>">
	<application-desc></application-desc>

	<information>
		<title>@APP_NAME@</title>
		<vendor>@VENDOR@</vendor>
		<description>@DESCRIPTION@</description>
		<homepage href="@URL@"/>
		<icon href="@ICON@"/>
		<offline-allowed/>
	</information>

	<security>
		<all-permissions/>
	</security>

  <resources>
  		<j2se version="1.5+" />
		<jar href="webrcp.jar" />
 		<property name="jnlp.WebRCP.appName" value="@APP_NAME@"/>
	   	<property name="jnlp.WebRCP.appVersion" value="@BUILD_VERSION@.@BUILD_NUMBER@"/>
	   	<property name="jnlp.WebRCP.launchProduct" value="@LAUNCH_PRODCUT@"/>
	    <property name="jnlp.WebRCP.archives" value="@ARCHIVES@"/>
	   	<property name="jnlp.WebRCP.sysArchives" value="@SYS_ARCHIVES@"/>
	   	<property name="jnlp.WebRCP.executable" value="@EXECUTABLE@"/>
	   	<property name="jnlp.WebRCP.singleInstance" value="false"/>
	   	<property name="jnlp.WebRCP.launcherjar" value="@EQUINOX_LAUNCHER@"/>
		
		<!-- more custom properties -->
		<!-- will be loaded and set as System Property without the jnlp.custom part -->
  		<property name="jnlp.custom.java.security.auth.login.config" value="http://@SERVER@:@WEBPORT@/login.conf"/>
		<property name="jnlp.custom.server" value="@SERVER@:@JNPPORT@"/>
    
  </resources>
</jnlp>

