/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.db2wkl.request.internal;

import org.w3c.dom.Document;

import com.ibm.db2wkl.helper.CryptionModule;
import com.ibm.db2wkl.logging.LogLevel;
import com.ibm.db2wkl.logging.Logger;
import com.ibm.db2wkl.request.parser.Options;
import com.ibm.staf.STAFResult;

/**
 * The RequestReciever for MISC based actions like encrypting
 * 
 * 
 */
public class MiscHandler extends InternalRequest {

	@Override
	public STAFResult execute() {
		
		if (hasRequestOption(Options.MISC_ENCRYPT)) {
			
			CryptionModule cryptionModule = null;
			try {
				cryptionModule = CryptionModule.getInstance();
				this.result = cryptionModule.getEncryptedPassword(getRequestOption(Options.MISC_ENCRYPT));
				if (hasRequestOption(Options.MISC_TOFILE) && this.result.rc == STAFResult.Ok) {
					this.result = cryptionModule.writePassfile(this.result.result, getRequestOption(Options.MISC_TOFILE));
				}
			} catch (Exception e) {
				Logger.log(e.getMessage(), LogLevel.Error);
				this.result = new STAFResult(STAFResult.JavaError, e.getMessage());
			} 
		} else {
			this.result = new STAFResult(STAFResult.InvalidRequestString, "Refer to the syntax for a correct request format.");
		}

		return this.result;
	}

	/* (non-Javadoc)
	 * @see com.ibm.db2wkl.request.internal.InternalRequest#getXML()
	 */
	@Override
	protected Document getXML() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see com.ibm.db2wkl.request.internal.InternalRequest#getText()
	 */
	@Override
	protected String getText() {
		// TODO Auto-generated method stub
		return null;
	}
}
