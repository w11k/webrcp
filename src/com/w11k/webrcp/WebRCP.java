/*******************************************************************************
 * Copyright (c) 2013 WeigleWilczek GmbH formerly iMedic GmbH
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.opensource.org/licenses/eclipse-1.0.php
 * Contributors:
 * WeigleWilczek GmbH [http://www.w11k.com] - initial API and implementation
 *******************************************************************************/

package com.w11k.webrcp;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.lang.ProcessBuilder.Redirect;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.security.Policy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.jnlp.BasicService;
import javax.jnlp.ServiceManager;
import javax.jnlp.UnavailableServiceException;
import javax.swing.JOptionPane;
import javax.swing.ProgressMonitor;
import javax.swing.Timer;

/**
 * WebRCP - Web Start Application which acts as loader for an Eclipse RCP
 * application.
 * 
 * @author by Daniel Mendler <mendler@imedic.de>
 */
public class WebRCP
{
	/*
	 * Supported system architectures (From org.eclipse.core.runtime.Platform)
	 */
	private static final String ARCH_X86               = "x86";
	private static final String ARCH_PA_RISC           = "PA_RISC";
	private static final String ARCH_PPC               = "ppc";
	private static final String ARCH_SPARC             = "sparc";
	private static final String ARCH_AMD64             = "amd64";
	private static final String ARCH_IA64              = "ia64";

	/*
	 * Supported windowing systems (From org.eclipse.core.runtime.Platform)
	 */
	private static final String WS_WIN32               = "win32";
	private static final String WS_MOTIF               = "motif";
	private static final String WS_GTK                 = "gtk";
	private static final String WS_PHOTON              = "photon";
	private static final String WS_CARBON              = "carbon";

	/*
	 * Supported operating systems (From org.eclipse.core.runtime.Platform)
	 */
	private static final String OS_WIN32               = "win32";
	private static final String OS_LINUX               = "linux";
	private static final String OS_AIX                 = "aix";
	private static final String OS_SOLARIS             = "solaris";
	private static final String OS_HPUX                = "hpux";
	private static final String OS_QNX                 = "qnx";
	private static final String OS_MACOSX              = "macosx";

	/*
	 * Configuration property names
	 */
	private static final String PROPERTY_BASEURL       = "jnlp.WebRCP.baseURL";
	private static final String PROPERTY_APPNAME       = "jnlp.WebRCP.appName";
	private static final String PROPERTY_APPVERSION    = "jnlp.WebRCP.appVersion";
	private static final String PROPERTY_LAUNCHAPP     = "jnlp.WebRCP.launchApp";
	private static final String PROPERTY_LAUNCHPRODUCT = "jnlp.WebRCP.launchProduct";
	private static final String PROPERTY_ARCHIVES      = "jnlp.WebRCP.archives";
	private static final String PROPERTY_JRE_ARCHIVE   = "jnlp.WebRCP.jreArchive";
	private static final String PROPERTY_SINGLEINST    = "jnlp.WebRCP.singleInstance";
	private static final String PROPERTY_EXECUTABLE    = "jnlp.WebRCP.executable";

	/*
	 * Port used to check for a running instance. This port should be hopefully
	 * unused.
	 */
	private static final int    SINGLEINST_PORT        = 25975;

	/*
	 * Eclipse launcher constants
	 */
	private static final String LAUNCHER_CLASS         = "org.eclipse.equinox.launcher.Main";
	private static final String LAUNCHER_JAR           = "jnlp.WebRCP.launcherjar";

	/*
	 * Try to determine system architecture by examining the system property "os.arch"
	 */
	private static String determineArch()
	{
		String arch = System.getProperty("os.arch").toLowerCase();

		if(arch.indexOf("x86") >= 0 || arch.matches("i.86"))
			return ARCH_X86;

		if(arch.indexOf("ppc") >= 0 || arch.indexOf("power") >= 0)
			return ARCH_PPC;

		if(arch.indexOf("x86_64") >= 0 || arch.indexOf("amd64") >= 0)
			return ARCH_AMD64;

		if(arch.indexOf("ia64") >= 0)
			return ARCH_IA64;

		if(arch.indexOf("risc") >= 0)
			return ARCH_PA_RISC;

		if(arch.indexOf("sparc") >= 0)
			return ARCH_SPARC;

		handleError("Unknown Architecture", "Your system has an unknown architecture: " + arch);

		return null;
	}

	/*
	 * Try to determine operating system by examining the system property "os.name"
	 */
	private static String determineOS()
	{
		String os = System.getProperty("os.name").toLowerCase();

		if(os.indexOf("linux") >= 0)
			return OS_LINUX;

		if(os.indexOf("mac") >= 0)
			return OS_MACOSX;

		if(os.indexOf("windows") >= 0)
			return OS_WIN32;

		if(os.indexOf("hp") >= 0 && os.indexOf("ux") >= 0)
			return OS_HPUX;

		if(os.indexOf("solaris") >= 0)
			return OS_SOLARIS;

		if(os.indexOf("aix") >= 0)
			return OS_AIX;

		if(os.indexOf("qnx") >= 0)
			return OS_QNX;

		handleError("Unknown Operating System", "Your operating system is unknown: " + os);

		return null;
	}

	/*
	 * Get window system by operating system name
	 */
	private static String getWindowSystem(String os)
	{
		if(os.equals(OS_WIN32))
			return WS_WIN32;

		if(os.equals(OS_LINUX))
			return WS_GTK;

		if(os.equals(OS_QNX))
			return WS_PHOTON;

		if(os.equals(OS_MACOSX))
			return WS_CARBON;

		return WS_MOTIF; // OS_AIX, OS_HPUX, OS_SOLARIS
	}

	/*
	 * Ensure that there's only one application instance running.
	 */
	private static void ensureSingleInstance()
	{
		try
		{
			new ServerSocket(SINGLEINST_PORT);
		}
		catch(Exception ex)
		{
			handleError("Already running.", "There's already an instance running.");
		}
	}

	/*
	 * Get base URL
	 */
	private static String getBaseURL()
	{
		try
		{
			BasicService service = (BasicService) ServiceManager.lookup("javax.jnlp.BasicService");
			return service.getCodeBase().toString();
		}
		catch(UnavailableServiceException ex)
		{
			handleError("WebStart Service Error", "Service javax.jnlp.BasicService unvailable: " + ex);
			return null;
		}
	}

	/*
	 * Check for new eclipse application version by comparing the value in the
	 * version file and the version argument.
	 * The version value can be an arbitrary string.
	 */
	private static boolean newVersionAvailable(String newVersion, File versionFile)
	{
		System.out.println("Version form Server: " + newVersion);
		String oldVersion = null;

		try
		{
			// Read old version
			BufferedReader in = new BufferedReader(new FileReader(versionFile));
			oldVersion = in.readLine();
			in.close();
		}
		catch(IOException ex)
		{
			// No error. File doesn't already exists.
		}

		System.out.println("Version form local Installation: " + oldVersion);
		try
		{
			// Write new version
			Writer out = new FileWriter(versionFile);
			out.write(newVersion);
			out.close();

			return oldVersion == null || !newVersion.equals(oldVersion);
		}
		catch(IOException ex)
		{
			// Not too bad. We continue.
			return true;
		}
	}

	/*
	 * Download a file from an url and store it at destFile. A progress monitor
	 * will be shown.
	 */
	private static void downloadFile(URL url, File destFile)
	{
		System.out.println("downloadFile(" + url.toString() + ")");
		try
		{
			OutputStream out = new FileOutputStream(destFile);

			URLConnection conn = url.openConnection();
			InputStream in = conn.getInputStream();

			int totalSize = conn.getContentLength(), downloadedSize = 0, size;
			byte[] buffer = new byte[32768];

			ProgressMonitor pm = createProgressMonitor("Downloading " + url, totalSize);
			boolean canceled = false;

			while((size = in.read(buffer)) > 0 && !(canceled = pm.isCanceled()))
			{
				out.write(buffer, 0, size);
				pm.setProgress(downloadedSize += size);
				// pm.setNote(downloadedSize / totalSize + "% finished");
			}

			in.close();
			out.close();

			if(canceled)
			{
				destFile.delete(); // Delete uncomplete file
				handleError("Starting canceled", "Downloading canceled. Exiting...");
			}

			pm.close();
		}
		catch(IOException ex)
		{
			handleError("Download Error", "Couldn't download file: " + ex);
		}
	}

	/**
	 * Starts a File via CMD, Only tested on Windows
	 * @param unpackDestDir
	 * @param executable
	 * @param keysToPass
	 * @param launchApp 
	 * @param launcherJar 
	 * @param javaPropertiesToPass 
	 */
	private static void startFile(File unpackDestDir, String executable, String launcherJar, String launchApp, String javaPropertiesToPass)
	{
		System.out.println("startFile()");

		try
		{
			List<String> cmdAndArgs = new ArrayList<>();
			cmdAndArgs.add("cmd");
			cmdAndArgs.add("/c");
			cmdAndArgs.add(executable);
			
			String command = javaPropertiesToPass + " -jar " + launcherJar + launchApp;
			cmdAndArgs.add(command);
	
			File commands = new File(unpackDestDir + "/webrcp_commands.log");
			commands.createNewFile();
			
			File output = new File(unpackDestDir + "/webrcp_output.log"); 
			output.createNewFile();
			
			File errors = new File(unpackDestDir + "/webrcp_errorsLog.txt"); 
			errors.createNewFile();
			
	
			ProcessBuilder pb = new ProcessBuilder((String[]) cmdAndArgs.toArray(new String[cmdAndArgs.size()]));
			pb.redirectInput(commands);
			pb.redirectError(Redirect.appendTo(errors));
			pb.redirectOutput(Redirect.appendTo(output));
	
			pb.redirectInput();
			pb.redirectOutput();
			pb.redirectError(); 
	
			pb.directory(unpackDestDir);
			
			pb.start();
		}
		catch(IOException e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
		}
		
		System.out.println("startFile()  finished");
	}

	/*
	 * Load and start eclipse launcher org.eclipse.core.launcher.Main
	 */
	private static void startLauncher(URL url, String os, String arch, String launcherJar, String arg)
	{
		System.out.println("startLauncher()");
		try
		{
			// Reload new policy which allows all to all codebases
			// because the default policy doesn't apply to the code loaded from
			// startup.jar!!!
			Policy.setPolicy(new AllPermissionPolicy());

			String path = url + launcherJar;

			URLClassLoader classLoader = new URLClassLoader(new URL[] { new URL(path) });
			Class<?> launcher = classLoader.loadClass(LAUNCHER_CLASS);
			Method launcherMain = launcher.getMethod("main", new Class[] { String.class });

			/*
			 * Start launcher with aurguments -os <operating-system> -ws
			 * <window-system> -arch <system architecture> -install
			 * <installation directory> -data <workspace directory> -user
			 * <workspace directory> -nl <locale> <-application|product name>
			 * The default workspace directory is put under the installation
			 * directory.
			 */
			launcherMain.invoke(
			        null,
			        new Object[] { "-os " + os + " -ws " + getWindowSystem(os) + " -arch " + arch + " -install " + url
			                + " -data " + url + "/workspace/ -user " + url + "/workspace/ -nl " + Locale.getDefault()
			                + arg });
		}
		catch(InvocationTargetException ex)
		{
			handleError("Startup Error", "Invocation failed: " + ex.getCause());
		}
		catch(IllegalAccessException ex)
		{
			handleError("Startup Error", "Invocation failed: " + ex);
		}
		catch(NoSuchMethodException ex)
		{
			ex.printStackTrace();
			handleError("Startup Error", "Invalid Eclipse Launcher: " + ex);
		}
		catch(ClassNotFoundException ex)
		{
			ex.printStackTrace();
			handleError("Startup Error", "Eclipse Launcher not found: " + ex);
		}
		catch(MalformedURLException ex)
		{
			// This shouldn't happen.
			throw new RuntimeException(ex);
		}
	}

	/*
	 * Convenience method to create a progress monitor
	 */
	private static ProgressMonitor createProgressMonitor(String message, int max)
	{
		ProgressMonitor pm = new ProgressMonitor(null, message, "", 0, max);
		pm.setMillisToDecideToPopup(100);
		pm.setMillisToPopup(500);

		return pm;
	}

	/*
	 * Convenience method to show an error dialog
	 */
	private static void handleError(String title, String message)
	{
		JOptionPane.showMessageDialog(null, message, title, JOptionPane.ERROR_MESSAGE);
		System.exit(1);
	}

	/*
	 * Simulate progress by showing a faked progress monitor
	 */
	private static void simulateProgress(String text, int seconds)
	{
		final ProgressMonitor pm = createProgressMonitor(text, seconds * 1000 / 50);

		Timer timer = new Timer(50, new ActionListener() {
			private int progress = 0;

			public void actionPerformed(ActionEvent event)
			{
				if(pm.isCanceled())
				{
					System.exit(0);
				}
				pm.setProgress(progress += 1);
			}
		});

		timer.start();
	}

	/*
	 * Get system property or die
	 */
	private static String getSystemProperty(String key)
	{
		String value = System.getProperty(key);
		if(value != null)
			return value;

		handleError("Missing System Property", key + " is required");

		return null;
	}

	/**
	 * Creates a shortcut on the users desktop to the passed target
	 * 
	 * @param shortcutTarget
	 * @param appName 
	 */
	private static void createDesktopShortcutToExe(String shortcutTarget, String appName)
	{
		Shortcut scut;
		try
		{
			scut = new Shortcut(new File(shortcutTarget));

			String userHomeDir = System.getProperty("user.home");

			OutputStream os = new FileOutputStream(userHomeDir + "\\Desktop\\" + appName + ".lnk");
			os.write(scut.getBytes());
			os.flush();
			os.close();
		}
		catch(UnsupportedEncodingException e)
		{
			e.printStackTrace();
			handleError(
			        "Error while creating Desktop Shortcut",
			        "Wrong Shortcut target specified" + e.getLocalizedMessage());
		}
		catch(FileNotFoundException e)
		{
			e.printStackTrace();
			handleError("Error while creating Desktop Shortcut", "File not found" + e.getLocalizedMessage());
		}
		catch(IOException e)
		{
			e.printStackTrace();
			handleError("Error while creating Desktop Shortcut", e.getLocalizedMessage());
		}
	}

	/**
	 * Run WebRCP
	 * 
	 * @param args
	 */
	public static void main(String[] args)
	{
		// http://stackoverflow.com/questions/19407102/java-7-update-45-broke-my-web-start-swt-application
		Properties properties = System.getProperties();
		// copy properties to avoid ConcurrentModificationException
		Properties copiedProperties = new Properties();
		copiedProperties.putAll(properties);
		Set<Object> keys = copiedProperties.keySet();
		List<String> keysToPass = new ArrayList<>();
		String javaPropertiesToPass = "";
		
		for(Object key: keys)
		{
			if(key instanceof String)
			{
				String keyString = (String) key;
				if(keyString.startsWith("jnlp.custom."))
				{
					// re set all properties starting with the jnlp.custom-prefix
					// and set them without the prefix
					String property = System.getProperty(keyString);
					String replacedKeyString = keyString.replaceFirst("jnlp.custom.", "");

					System.out.println("Found custom property: " + keyString);

					// is needed, if the RCP App is started via the launcher jar
					System.setProperty(replacedKeyString, property);
					String string = replacedKeyString + "=" + property;
					
					// is needed, if the RCP app is started via a launcher file (e.g. go.bat)
					keysToPass.add("\"-D" + string + "\"");
					javaPropertiesToPass += " -D" + string;
				}
			}
		}

		if(Boolean.getBoolean(PROPERTY_SINGLEINST))
		{
			ensureSingleInstance();
		}

		// Get required properties
		String appName = getSystemProperty(PROPERTY_APPNAME);
		System.out.println("appName = " + appName);
		String appVersion = getSystemProperty(PROPERTY_APPVERSION);
		System.out.println("appVersion = " + appVersion);
		String[] archive = getSystemProperty(PROPERTY_ARCHIVES).split("\\s*,\\s*");
		System.out.println("archives = " + getSystemProperty(PROPERTY_ARCHIVES));
		final String launcherJar = getSystemProperty(LAUNCHER_JAR);
		System.out.println("launcher jar: " + launcherJar);

		String jreArchive = null;
		if(System.getProperty(PROPERTY_JRE_ARCHIVE) != null) {
			jreArchive = getSystemProperty(PROPERTY_JRE_ARCHIVE);
			System.out.println("jreArchive = " + jreArchive);
		}
		String executable = null;
		if(System.getProperty(PROPERTY_EXECUTABLE) != null) {
			executable = getSystemProperty(PROPERTY_EXECUTABLE);
			System.out.println("executable = " + executable);
		}

		// Get application/product to launch
		String launchApp = System.getProperty(PROPERTY_LAUNCHAPP);

		if(launchApp == null)
		{
			launchApp = System.getProperty(PROPERTY_LAUNCHPRODUCT);

			if(launchApp == null)
			{
				handleError("Missing System Property", PROPERTY_LAUNCHAPP + " or " + PROPERTY_LAUNCHPRODUCT
				        + " are required");
			}
			else
			{
				launchApp = " -product " + launchApp;
			}
		}
		else
		{
			launchApp = " -application " + launchApp;
		}

		String baseURL = getBaseURL();
		String arch = determineArch();
		String os = determineOS();

		File tempDir = new File(getSystemProperty("java.io.tmpdir"), appName);
		tempDir.mkdirs();

		File unpackDestDir = new File(tempDir, "unpacked");

		// Check for new version
		boolean override = newVersionAvailable(appVersion, new File(tempDir, "version"));

		// Start background thread for unpacking
		UnpackThread unpackThread = new UnpackThread(unpackDestDir, override);

		// Download and unpack system-independant archives
		for(String element: archive)
		{
			downloadFile(baseURL, tempDir, override, unpackThread, element);
		}

		if(jreArchive != null && jreArchive.length() > 0) {
			downloadFile(baseURL, tempDir, override, unpackThread, jreArchive);
		}

		// Show a progress monitor which "simulates" the startup of
		// the application.
		simulateProgress("Loading " + appName + "...", 3);

		// Wait for the unpacking thread to complete
		unpackThread.finish();

		// Store base url (might be used by the loaded program)
		System.setProperty(PROPERTY_BASEURL, baseURL);

		// Then start the launcher!
		if (executable != null && executable.length() > 0 && jreArchive != null && jreArchive.length() > 0) {
			System.out.println("starting with executable: " + executable);
			startFile(unpackDestDir, executable, launcherJar, launchApp, javaPropertiesToPass);
		} else  {
			System.out.println("starting with launcher");
			try {
				startLauncher(unpackDestDir.toURI().toURL(), os, arch, launcherJar, launchApp);
			} catch (MalformedURLException e) {
				e.printStackTrace();
				throw new RuntimeException(e);
			}
		}

		System.exit(0);
	}

	private static void downloadFile(
	        String baseURL,
	        File tempDir,
	        boolean override,
	        UnpackThread unpackThread,
	        String element)
	{
		System.out.println("Checking for download of file: " + element);
		File destFile = new File(tempDir, element + ".zip");

		if(!destFile.exists() || override)
		{
			try
			{
				System.out.println("downloading file: " + element);
				downloadFile(new URL(baseURL + element + ".zip"), destFile);
			}
			catch(MalformedURLException ex)
			{
				// This shouldn't happen.
				throw new RuntimeException(ex);
			}
		}

		unpackThread.addNextFile(destFile);
	}

	private static void printSystemProperties()
	{
		Properties p = System.getProperties();
		Enumeration keys = p.keys();
		while(keys.hasMoreElements())
		{
			String key = (String) keys.nextElement();
			String value = (String) p.get(key);
			System.out.println(key + ": " + value);
		}
	}
}
