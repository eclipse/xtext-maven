/*******************************************************************************
 * Copyright (c) 2018 itemis AG (http://www.itemis.eu) and others.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.xtext.maven;

import java.util.List;
import java.util.Set;

import org.eclipse.xtext.builder.standalone.ILanguageConfiguration;

import com.google.common.collect.Sets;

/**
 * @author Dennis Huebner - Initial contribution and API
 * 
 */
public class Language implements ILanguageConfiguration {

	/**
	 * whether this language links or produces Java types
	 * @property
	 */
	private boolean javaSupport = true;
	
	/**
	 * @property
	 * @required
	 */
	private String setup;

	/**
	 * @property
	 */
	private List<OutputConfiguration> outputConfigurations;

	public String getSetup() {
		return setup;
	}

	public void setSetup(String setup) {
		this.setup = setup;
	}

	public Set<org.eclipse.xtext.generator.OutputConfiguration> getOutputConfigurations() {
		if (outputConfigurations == null) {
			return null;
		}
		Set<org.eclipse.xtext.generator.OutputConfiguration> set = Sets.newHashSet();
		for (OutputConfiguration outConf : outputConfigurations) {
			set.add(outConf.toOutputConfiguration());
		}
		return set;
	}

	public void setOutputConfigurations(List<OutputConfiguration> outputConfiguration) {
		this.outputConfigurations = outputConfiguration;
	}
	
	public void setJavaSupport(boolean javaSupport) {
		this.javaSupport = javaSupport;
	}

	public boolean isJavaSupport() {
		return javaSupport;
	}

}
