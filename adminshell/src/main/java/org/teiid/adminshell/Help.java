/*
 * JBoss, Home of Professional Open Source.
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301 USA.
 */

package org.teiid.adminshell;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;

/**
 * A simple help system built off of scanning public static methods.
 */
public class Help {
	
	@Documented
	@Retention(RetentionPolicy.RUNTIME)
	@Target({ElementType.METHOD, ElementType.PARAMETER})
	public @interface Doc {
		String text();
		String[] moreText() default {};
	}

	private TreeMap<String, List<String>> help = new TreeMap<String, List<String>>();
	private List<String> shortHelp = new ArrayList<String>();
	
	public Help(Class<?> clazz) {
		Method[] methods = clazz.getMethods();
		for (Method method : methods) {
			if (!Modifier.isPublic(method.getModifiers()) || !Modifier.isStatic(method.getModifiers())) {
				continue;
			}
			StringBuilder sb = new StringBuilder();
			Help.Doc doc = method.getAnnotation(Help.Doc.class);
			StringBuilder shortSb = new StringBuilder();
			shortSb.append(method.getName()).append("("); //$NON-NLS-1$
			if (method.getParameterTypes().length > 0) {
				shortSb.append(method.getParameterTypes().length);
			}
			shortSb.append(")"); //$NON-NLS-1$
			String shortHelpStr = String.format("  %-25s", shortSb.toString()); //$NON-NLS-1$ 
			if (doc != null) {
				sb.append("/*\n *").append(doc.text()); //$NON-NLS-1$ 
				for (String string : doc.moreText()) {
					sb.append("\n *").append(string); //$NON-NLS-1$
				}
				sb.append("\n */\n"); //$NON-NLS-1$
				shortHelpStr += " -- " + doc.text(); //$NON-NLS-1$ 
			}
			shortHelp.add(shortHelpStr);
			Class<?> returnType = method.getReturnType();
			sb.append(returnType.getSimpleName()).append(" "); //$NON-NLS-1$
			sb.append(method.getName()).append("("); //$NON-NLS-1$
			Class<?>[] params = method.getParameterTypes();
			for (int i = 0; i < params.length; i++) {
				if (i > 0) {
					sb.append(","); //$NON-NLS-1$
				}
				sb.append("\n    ").append(params[i].getSimpleName()); //$NON-NLS-1$
				Annotation[] annos = method.getParameterAnnotations()[i];
				for (Annotation annotation : annos) {
					if (!(annotation instanceof Help.Doc)) {
						continue;
					}
					Help.Doc paramdoc = (Help.Doc)annotation;
					if (paramdoc.text() != null) {
						sb.append(" /* ").append(paramdoc.text()).append(" */"); //$NON-NLS-1$ //$NON-NLS-2$
						break;
					}
				}
			}
			sb.append(")\n"); //$NON-NLS-1$
			for(Class<?> exceptionClass : method.getExceptionTypes()) {
				sb.append("  throws ").append(exceptionClass.getSimpleName()).append("\n"); //$NON-NLS-1$ //$NON-NLS-2$
			}
			String key = method.getName().toUpperCase();
			List<String> signatures = help.get(key);
			if (signatures == null) {
				signatures = new LinkedList<String>();
				help.put(key, signatures);
			}
			signatures.add(sb.toString());
		}
		Collections.sort(shortHelp);
	}
	
	public void help() {
		System.out.println("/* method(arg count)        -- description */"); //$NON-NLS-1$
		for (String helpString : shortHelp) {
			System.out.println(helpString);
		}
	}
	
	public void help(String method) {
		List<String> helpStrings = null;
		if (method != null) {
			helpStrings = help.get(method.toUpperCase());
		}
		if (helpStrings != null) {
			for (String helpString : helpStrings) {
				System.out.println(helpString);
			}
		} else {
			System.out.println("Unknown method");
		}
	}
	
}
