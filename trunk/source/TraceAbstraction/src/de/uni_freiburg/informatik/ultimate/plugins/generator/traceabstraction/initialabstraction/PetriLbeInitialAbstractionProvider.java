/*
 * Copyright (C) 2022 Dominik Klumpp (klumpp@informatik.uni-freiburg.de)
 * Copyright (C) 2022 University of Freiburg
 *
 * This file is part of the ULTIMATE TraceAbstraction plug-in.
 *
 * The ULTIMATE TraceAbstraction plug-in is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The ULTIMATE TraceAbstraction plug-in is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ULTIMATE TraceAbstraction plug-in. If not, see <http://www.gnu.org/licenses/>.
 *
 * Additional permission under GNU GPL version 3 section 7:
 * If you modify the ULTIMATE TraceAbstraction plug-in, or any covered work, by linking
 * or combining it with Eclipse RCP (or a modified version of Eclipse RCP),
 * containing parts covered by the terms of the Eclipse Public License, the
 * licensors of the ULTIMATE TraceAbstraction plug-in grant you additional permission
 * to convey the resulting work.
 */
package de.uni_freiburg.informatik.ultimate.plugins.generator.traceabstraction.initialabstraction;

import java.util.Set;

import de.uni_freiburg.informatik.ultimate.automata.AutomataLibraryException;
import de.uni_freiburg.informatik.ultimate.automata.petrinet.netdatastructures.BoundedPetriNet;
import de.uni_freiburg.informatik.ultimate.core.lib.results.StatisticsResult;
import de.uni_freiburg.informatik.ultimate.core.model.services.IUltimateServiceProvider;
import de.uni_freiburg.informatik.ultimate.lib.modelcheckerutils.cfg.structure.IIcfg;
import de.uni_freiburg.informatik.ultimate.lib.modelcheckerutils.cfg.structure.IIcfgTransition;
import de.uni_freiburg.informatik.ultimate.lib.modelcheckerutils.cfg.structure.IcfgLocation;
import de.uni_freiburg.informatik.ultimate.lib.modelcheckerutils.smt.predicates.IPredicate;
import de.uni_freiburg.informatik.ultimate.plugins.generator.traceabstraction.Activator;
import de.uni_freiburg.informatik.ultimate.plugins.generator.traceabstraction.BasicCegarLoop.PetriNetLbe;
import de.uni_freiburg.informatik.ultimate.plugins.generator.traceabstraction.petrinetlbe.PetriNetLargeBlockEncoding;
import de.uni_freiburg.informatik.ultimate.plugins.generator.traceabstraction.petrinetlbe.PetriNetLargeBlockEncoding.IPLBECompositionFactory;

public class PetriLbeInitialAbstractionProvider<L extends IIcfgTransition<?>>
		implements IInitialAbstractionProvider<L, BoundedPetriNet<L, IPredicate>> {

	private final IUltimateServiceProvider mServices;
	private final IInitialAbstractionProvider<L, BoundedPetriNet<L, IPredicate>> mUnderlying;
	private final IPLBECompositionFactory<L> mCompositionFactory;
	private final Class<L> mTransitionClazz;
	private final PetriNetLbe mLbeMode;

	public PetriLbeInitialAbstractionProvider(
			final IInitialAbstractionProvider<L, BoundedPetriNet<L, IPredicate>> underlying,
			final IUltimateServiceProvider services, final Class<L> transitionClazz, final PetriNetLbe lbeMode,
			final IPLBECompositionFactory<L> compositionFactory) {
		mUnderlying = underlying;
		mServices = services;
		mTransitionClazz = transitionClazz;
		mLbeMode = lbeMode;
		mCompositionFactory = compositionFactory;
	}

	@Override
	public BoundedPetriNet<L, IPredicate> getInitialAbstraction(final IIcfg<? extends IcfgLocation> icfg,
			final Set<? extends IcfgLocation> errorLocs) throws AutomataLibraryException {
		final BoundedPetriNet<L, IPredicate> net = mUnderlying.getInitialAbstraction(icfg, errorLocs);

		final PetriNetLargeBlockEncoding<L> lbe = new PetriNetLargeBlockEncoding<>(mServices, icfg.getCfgSmtToolkit(),
				net, mLbeMode, mCompositionFactory, mTransitionClazz);
		final BoundedPetriNet<L, IPredicate> lbecfg = lbe.getResult();

		mServices.getBacktranslationService().addTranslator(lbe.getBacktranslator());
		mServices.getResultService().reportResult(Activator.PLUGIN_ID, new StatisticsResult<>(Activator.PLUGIN_NAME,
				"PetriNetLargeBlockEncoding benchmarks", lbe.getStatistics()));

		return lbecfg;
	}

}
