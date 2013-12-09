/*******************************************************************************
 * Copyright (c) 2013 WeigleWilczek GmbH formerly iMedic GmbH
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.opensource.org/licenses/eclipse-1.0.php
 *
 * Contributors:
 *   WeigleWilczek GmbH [http://www.w11k.com] - initial API and implementation
 *******************************************************************************/

package com.w11k.webrcp;

public class MalformedShortcutException extends Exception
{

	/**
	 * 
	 */
	private static final long serialVersionUID = 5990186301831959814L;

	public MalformedShortcutException()
	{
		
	}

	public MalformedShortcutException(String message)
	{
		super(message);
	}

	public MalformedShortcutException(Throwable cause)
	{
		super(cause);
	}

	public MalformedShortcutException(String message, Throwable cause)
	{
		super(message, cause);
	}

}
