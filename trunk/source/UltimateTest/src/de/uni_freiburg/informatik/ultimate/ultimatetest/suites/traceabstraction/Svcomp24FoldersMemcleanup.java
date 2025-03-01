/*
 * Copyright (C) 2015 Daniel Dietsch (dietsch@informatik.uni-freiburg.de)
 * Copyright (C) 2015 Matthias Heizmann (heizmann@informatik.uni-freiburg.de)
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
/**
 *
 */
package de.uni_freiburg.informatik.ultimate.ultimatetest.suites.traceabstraction;

import java.util.Collection;

import de.uni_freiburg.informatik.ultimate.test.UltimateRunDefinition;
import de.uni_freiburg.informatik.ultimate.test.UltimateTestCase;
import de.uni_freiburg.informatik.ultimate.test.decider.ITestResultDecider;
import de.uni_freiburg.informatik.ultimate.test.decider.SvcompTestResultDeciderMemcleanup;
import de.uni_freiburg.informatik.ultimate.test.util.SvcompFolderSubset;
import de.uni_freiburg.informatik.ultimate.test.util.TestUtil;
import de.uni_freiburg.informatik.ultimate.test.util.UltimateRunDefinitionGenerator;
import de.uni_freiburg.informatik.ultimate.util.datastructures.relation.Pair;

/**
 * @author heizmann@informatik.uni-freiburg.de
 *
 */
public class Svcomp24FoldersMemcleanup extends AbstractTraceAbstractionTestSuite {

	/** Limit the number of files per directory. */
//	private static final int FILES_PER_DIR_LIMIT = Integer.MAX_VALUE;
	 private static final int FILES_PER_DIR_LIMIT = 10;
	private static final int FILE_OFFSET = 0;

	private static final String PROPERTY = TestUtil.SVCOMP_PROP_VALIDMEMCLEANUP;

	// @formatter:off
	private static final SvcompFolderSubset[] BENCHMARKS = {
            new SvcompFolderSubset("examples/svcomp/list-ext-properties/", PROPERTY, null, FILE_OFFSET,  FILES_PER_DIR_LIMIT),
            new SvcompFolderSubset("examples/svcomp/heap-manipulation/", PROPERTY, null, FILE_OFFSET,  FILES_PER_DIR_LIMIT),
            new SvcompFolderSubset("examples/svcomp/forester-heap/", PROPERTY, null, FILE_OFFSET,  FILES_PER_DIR_LIMIT),
            new SvcompFolderSubset("examples/svcomp/list-properties/", PROPERTY, null, FILE_OFFSET,  FILES_PER_DIR_LIMIT),
            new SvcompFolderSubset("examples/svcomp/list-ext3-properties/", PROPERTY, null, FILE_OFFSET,  FILES_PER_DIR_LIMIT),
            new SvcompFolderSubset("examples/svcomp/memsafety/", PROPERTY, null, FILE_OFFSET,  FILES_PER_DIR_LIMIT),
            new SvcompFolderSubset("examples/svcomp/memsafety-bftpd/", PROPERTY, null, FILE_OFFSET,  FILES_PER_DIR_LIMIT),
            new SvcompFolderSubset("examples/svcomp/Juliet_Test/", PROPERTY, null, FILE_OFFSET,  FILES_PER_DIR_LIMIT),
	};

	@Override
	protected ITestResultDecider constructITestResultDecider(final UltimateRunDefinition urd) {
		return new SvcompTestResultDeciderMemcleanup(urd, false);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public long getTimeout() {
		return 20 * 1000;
	}

	private static final Pair[] SETTINGS = {
			new Pair<>("default/automizer/svcomp-MemCleanup-32bit-Automizer_Default.epf", "default/automizer/svcomp-MemCleanup-64bit-Automizer_Default.epf"),
			new Pair<>("default/automizer/svcomp-MemCleanup-32bit-Automizer_Bitvector.epf", "default/automizer/svcomp-MemCleanup-64bit-Automizer_Bitvector.epf"),
	};


	private static final String[] TOOLCHAINS = {
//		"AutomizerC.xml",
		"AutomizerCInline.xml",
//		"AutomizerCInlineTransformed.xml",
//		"AutomizerCInlineBlockencodedTransformed.xml"
//		"AutomizerCInline_WitnessPrinter.xml"
	};
	// @formatter:on

	@Override
	public Collection<UltimateTestCase> createTestCases() {
		for (final SvcompFolderSubset dfep : BENCHMARKS) {
			for (final String toolchain : TOOLCHAINS) {
				addTestCase(UltimateRunDefinitionGenerator.getRunDefinitionsFromSvcompYaml(dfep, SETTINGS,
						toolchain, getTimeout()));
			}
		}
		return super.createTestCases();
	}

}
