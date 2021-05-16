/*
 * Copyright (C) 2021 Dominik Klumpp (klumpp@informatik.uni-freiburg.de)
 * Copyright (C) 2021 University of Freiburg
 *
 * This file is part of the ULTIMATE Automata Library.
 *
 * The ULTIMATE Automata Library is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The ULTIMATE Automata Library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ULTIMATE Automata Library. If not, see <http://www.gnu.org/licenses/>.
 *
 * Additional permission under GNU GPL version 3 section 7:
 * If you modify the ULTIMATE Automata Library, or any covered work, by linking
 * or combining it with Eclipse RCP (or a modified version of Eclipse RCP),
 * containing parts covered by the terms of the Eclipse Public License, the
 * licensors of the ULTIMATE Automata Library grant you additional permission
 * to convey the resulting work.
 */
package de.uni_freiburg.informatik.ultimate.automata.partialorder;

import java.util.Set;
import java.util.function.Predicate;

import de.uni_freiburg.informatik.ultimate.util.statistics.IStatisticsDataProvider;

/**
 * Wrapper around an {@link IPersistentSetChoice} which delegates calls to the underlying instance if a given condition
 * is met, and returns the trivial persistent set otherwise.
 *
 * @author Dominik Klumpp (klumpp@informatik.uni-freiburg.de)
 *
 * @param <L>
 *            The type of letters in the persistent sets
 * @param <S>
 *            The type of states for which persistent sets are computed
 */
public class FilteredPersistentSetChoice<L, S> implements IPersistentSetChoice<L, S> {
	private final IPersistentSetChoice<L, S> mUnderlying;
	private final Predicate<S> mFilter;

	public FilteredPersistentSetChoice(final IPersistentSetChoice<L, S> underlying, final Predicate<S> filter) {
		mUnderlying = underlying;
		mFilter = filter;
	}

	@Override
	public Set<L> persistentSet(final S state) {
		if (mFilter.test(state)) {
			return mUnderlying.persistentSet(state);
		}
		return null;
	}

	@Override
	public IStatisticsDataProvider getStatistics() {
		return mUnderlying.getStatistics();
	}
}
