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

import java.security.AllPermission;
import java.security.CodeSource;
import java.security.PermissionCollection;
import java.security.Permissions;
import java.security.Policy;
import java.security.ProtectionDomain;

/**
 * Permission policy which allows everything to every codebase and domain. This
 * permission will be loaded because the manually loaded classes (e.g. the
 * eclipse launcher) haven't enough permissions with the standard web start
 * policy.
 * 
 * @author Daniel Mendler <mendler@imedic.de>
 */
class AllPermissionPolicy extends Policy {
	public PermissionCollection getPermissions(CodeSource code) {
		Permissions permissions = new Permissions();
		permissions.add(new AllPermission());
		return permissions;
	}

	public PermissionCollection getPermissions(ProtectionDomain domain) {
		Permissions permissions = new Permissions();
		permissions.add(new AllPermission());
		return permissions;
	}

	public void refresh() {
	}
}