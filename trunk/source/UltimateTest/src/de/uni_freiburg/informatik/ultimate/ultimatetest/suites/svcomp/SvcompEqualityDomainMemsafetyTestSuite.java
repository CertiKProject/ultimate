/*
 * Copyright (C) 2015 Daniel Dietsch (dietsch@informatik.uni-freiburg.de)
 * Copyright (C) 2015 University of Freiburg
 *
 * This file is part of the ULTIMATE Test Library.
 *
 * The ULTIMATE Test Library is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The ULTIMATE Test Library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ULTIMATE Test Library. If not, see <http://www.gnu.org/licenses/>.
 *
 * Additional permission under GNU GPL version 3 section 7:
 * If you modify the ULTIMATE Test Library, or any covered work, by linking
 * or combining it with Eclipse RCP (or a modified version of Eclipse RCP),
 * containing parts covered by the terms of the Eclipse Public License, the
 * licensors of the ULTIMATE Test Library grant you additional permission
 * to convey the resulting work.
 */
package de.uni_freiburg.informatik.ultimate.ultimatetest.suites.svcomp;

import java.util.ArrayList;
import java.util.List;

import de.uni_freiburg.informatik.ultimate.test.UltimateRunDefinition;
import de.uni_freiburg.informatik.ultimate.test.benchexec.BenchexecRundefinitionGeneratorPreLog;
import de.uni_freiburg.informatik.ultimate.test.decider.ITestResultDecider;
import de.uni_freiburg.informatik.ultimate.test.decider.SvcompTestResultDeciderValidMemsafety;
import de.uni_freiburg.informatik.ultimate.test.reporting.IPreTestLog;

/**
 *
 * @author dietsch@informatik.uni-freiburg.de
 *
 */
public class SvcompEqualityDomainMemsafetyTestSuite extends AbstractSvcompTestSuite {

	@Override
	protected ITestResultDecider getTestResultDecider(final UltimateRunDefinition urd) {
		return new SvcompTestResultDeciderValidMemsafety(urd, false);
	}

	@Override
	protected IPreTestLog[] constructPreTestLogs() {
		// TODO Auto-generated method stub
		return new IPreTestLog[] { new BenchexecRundefinitionGeneratorPreLog(getClass()) };
	}

	@Override
	protected long getTimeout() {
		// Timeout for each test case in milliseconds
		return 90 * 1000;
	}

	@Override
	protected int getFilesPerCategory() {
		// -1 or value larger than 0
		return -1;
	}

	@Override
	protected List<SvcompTestDefinition> getTestDefinitions() {
		final List<SvcompTestDefinition> rtr = new ArrayList<>();

		// contains 69 examples, 32bit
		rtr.addAll(createTests("MemSafety-Arrays", "32bit", "DerefFreeMemtrack"));

		// contains 347 examples, 32bit
		rtr.addAll(createTests("MemSafety-Heap", "32bit", "DerefFreeMemtrack"));

		// contains 52 examples, 32bit
		rtr.addAll(createTests("MemSafety-LinkedLists", "32bit", "DerefFreeMemtrack"));

		// contains 26 examples, 32bit
		rtr.addAll(createTests("MemSafety-Other", "32bit", "DerefFreeMemtrack"));

		// contains 39 examples, 64bit
		rtr.addAll(createTests("Systems_BusyBox_MemSafety", "64bit", "DerefFreeMemtrack"));

		return rtr;
	}

	private List<SvcompTestDefinition> createTests(final String set, final int limit, final String arch,
			final String category) {
		return createTests(set, getTimeout(), limit, arch, category);
	}

	private List<SvcompTestDefinition> createTests(final String set, final String arch, final String category) {
		return createTests(set, getTimeout(), getFilesPerCategory(), arch, category);
	}

	private List<SvcompTestDefinition> createTests(final String set, final long timeout, final int limit,
			final String arch, final String category) {
		final List<SvcompTestDefinition> rtr = new ArrayList<>();

		// uses only abstract interpretation
		rtr.add(getTestDefinitionFromExamples(set, "AbstractInterpretationC.xml",
				"ai/eq-bench/svcomp-" + category + "-" + arch + "-Automizer_Camel+AI_EQ.epf", timeout, limit));
		// // uses Taipan strategy
		// rtr.add(getTestDefinitionFromExamples(set, "AutomizerC.xml",
		// "ai/eq-bench/svcomp-" + category + "-" + arch + "-Automizer_Taipan+AI_EQ.epf", timeout, limit));
		// // uses CAMEL strategy
		// rtr.add(getTestDefinitionFromExamples(set, "AutomizerC.xml",
		// "ai/eq-bench/svcomp-" + category + "-" + arch + "-Automizer_Camel+AI_EQ.epf", timeout, limit));
		// // uses RubberTaipan strategy
		// rtr.add(getTestDefinitionFromExamples(set, "AutomizerC.xml",
		// "ai/eq-bench/svcomp-" + category + "-" + arch + "-Automizer_RubberTaipan+AI_EQ.epf", timeout, limit));

		return rtr;
	}
}
