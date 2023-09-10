/**
 * Copyright (C) 2014 4th Line GmbH, Switzerland and others
 *
 * The contents of this file are subject to the terms of the
 * Common Development and Distribution License Version 1 or later
 * ("CDDL") (collectively, the "License"). You may not use this file
 * except in compliance with the License. See LICENSE.txt for more
 * information.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */

package org.jupnp.tool.cli;

import java.util.UUID;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.jupnp.tool.cli.SearchCommand.SearchResultPrinter;

/**
 * This test case checks search command from tool.
 * 
 * @author Jochen Hiller - Initial contribution
 */
public class SearchCommandTest extends AbstractTestCase {

	@Before
	public void setUp() {
		// use standard tool to see output in console
		this.tool = new JUPnPTool();
	}

	@After
	public void tearDown() throws Exception {
		this.tool = null;
	}

	/**
	 * Tests whether sorting of IP address will sort in correct order.
	 */
	@Test
	public void testSearchResultSortByIpAddress() {
		// no sort, no verbose
		SearchResultPrinter res = new SearchCommand(tool).new SearchResultPrinter(
				"ip", false);
		res.add("192.168.3.101", "", "", "", "uuid:" + UUID.randomUUID());
		res.add("192.168.3.1", "", "", "", "uuid:" + UUID.randomUUID());
		res.add("192.168.3.100", "", "", "", "uuid:" + UUID.randomUUID());
		res.add("192.168.3.2", "", "", "", "uuid:" + UUID.randomUUID());
		res.add("192.168.3.15", "", "", "", "uuid:" + UUID.randomUUID());
		res.add("192.168.3.255", "", "", "", "uuid:" + UUID.randomUUID());
		String s = res.asBody();
		// System.out.println(s);
		
		int index1 = s.indexOf("192.168.3.1");
		int index2 = s.indexOf("192.168.3.2");
		int index100 = s.indexOf("192.168.3.100");
		int index101 = s.indexOf("192.168.3.101");
		int index15 = s.indexOf("192.168.3.15");
		int index255 = s.indexOf("192.168.3.255");
		Assert.assertTrue(index1 < index2);
		Assert.assertTrue(index2 < index15);
		Assert.assertTrue(index15 < index100);
		Assert.assertTrue(index100 < index101);
		Assert.assertTrue(index101 < index255);
	}

	@Test
	public void testSearchNoSort() {
		checkCommandLine(tool, JUPnPTool.RC_OK, "search");
	}

	@Test
	public void testSearchNoSortWithStatistics() {
		checkCommandLine(tool, JUPnPTool.RC_OK, "--pool=20,40,stats search");
	}

	@Test
	public void testSearchSortByIp() {
		// checkCommandLine(tool, JUPnPTool.RC_OK, "--loglevel=INFO search");
		checkCommandLine(tool, JUPnPTool.RC_OK, "search --timeout=20 --sort=ip");
	}

	@Test
	public void testSearchSortedBy() {
		checkCommandLine(tool, JUPnPTool.RC_OK, "search --sort=ip");
		checkCommandLine(tool, JUPnPTool.RC_OK, "search --sort=model");
		checkCommandLine(tool, JUPnPTool.RC_OK, "search --sort=serialNumber");
		checkCommandLine(tool, JUPnPTool.RC_OK,
				"--verbose search --sort=manufacturer");
		checkCommandLine(tool, JUPnPTool.RC_OK, "--verbose search --sort=udn");
	}

	@Test
	public void testSearchWithVerbose() {
		checkCommandLine(tool, JUPnPTool.RC_OK, "--verbose search --sort=ip");
	}

	@Test
	public void testSearchWithFilter() {
		checkCommandLine(tool, JUPnPTool.RC_OK,
				"--verbose --loglevel=INFO search --filter=D-Link");
	}

	@Test
	public void testSearchWrongArguments() {
		createSilentTool();
		checkCommandLine(tool, JUPnPTool.RC_INVALID_OPTION,
				"search --timeout=ABC");
		checkCommandLine(tool, JUPnPTool.RC_INVALID_OPTION, "search --sort=ABC");
		checkCommandLine(tool, JUPnPTool.RC_INVALID_OPTION, "search -s=XYZ");
		releaseSilentTool();
	}

}
