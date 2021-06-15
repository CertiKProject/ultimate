/*
 * Copyright (C) 2020 University of Freiburg
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
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
package de.uni_freiburg.informatik.ultimate.plugins.generator.traceabstraction.concurrency;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import de.uni_freiburg.informatik.ultimate.automata.petrinet.IPetriNet;
import de.uni_freiburg.informatik.ultimate.automata.petrinet.ITransition;
import de.uni_freiburg.informatik.ultimate.automata.petrinet.Marking;
import de.uni_freiburg.informatik.ultimate.automata.petrinet.unfolding.BranchingProcess;
import de.uni_freiburg.informatik.ultimate.core.model.services.ILogger;
import de.uni_freiburg.informatik.ultimate.core.model.services.IUltimateServiceProvider;
import de.uni_freiburg.informatik.ultimate.lib.modelcheckerutils.ModelCheckerUtils;
import de.uni_freiburg.informatik.ultimate.lib.modelcheckerutils.cfg.CfgSmtToolkit;
import de.uni_freiburg.informatik.ultimate.lib.modelcheckerutils.cfg.DefaultIcfgSymbolTable;
import de.uni_freiburg.informatik.ultimate.lib.modelcheckerutils.cfg.transitions.TransFormulaBuilder;
import de.uni_freiburg.informatik.ultimate.lib.modelcheckerutils.cfg.transitions.TransFormulaUtils;
import de.uni_freiburg.informatik.ultimate.lib.modelcheckerutils.cfg.transitions.UnmodifiableTransFormula;
import de.uni_freiburg.informatik.ultimate.lib.modelcheckerutils.cfg.variables.IProgramVar;
import de.uni_freiburg.informatik.ultimate.lib.modelcheckerutils.cfg.variables.ProgramVarUtils;
import de.uni_freiburg.informatik.ultimate.lib.modelcheckerutils.smt.predicates.BasicPredicateFactory;
import de.uni_freiburg.informatik.ultimate.lib.modelcheckerutils.smt.predicates.IPredicate;
import de.uni_freiburg.informatik.ultimate.lib.smtlibutils.ManagedScript;
import de.uni_freiburg.informatik.ultimate.lib.smtlibutils.SmtSortUtils;
import de.uni_freiburg.informatik.ultimate.lib.smtlibutils.SmtUtils;
import de.uni_freiburg.informatik.ultimate.lib.smtlibutils.SmtUtils.SimplificationTechnique;
import de.uni_freiburg.informatik.ultimate.lib.smtlibutils.SmtUtils.XnfConversionTechnique;
import de.uni_freiburg.informatik.ultimate.logic.Script;
import de.uni_freiburg.informatik.ultimate.logic.Term;
import de.uni_freiburg.informatik.ultimate.logic.TermVariable;
import de.uni_freiburg.informatik.ultimate.util.datastructures.DataStructureUtils;
import de.uni_freiburg.informatik.ultimate.util.datastructures.relation.HashRelation;

/**
 * Constructs an Owicki/Gries annotation for a Petri program from a Floyd/Hoare annotation of the reachability graph of
 * the Petri net, by introducing a boolean ghost variable for each place in the Petri net.
 *
 * @author Dominik Klumpp (klumpp@informatik.uni-freiburg.de)
 * @author Miriam Lagunes (miriam.lagunes@students.uni-freiburg.de)
 *
 * @param <PLACE>
 *            The type of places in the Petri program
 * @param <LETTER>
 *            The type of statements in the Petri program
 */
public class OwickiGriesConstruction<PLACE, LETTER> {

	private final IUltimateServiceProvider mServices;
	private final ILogger mLogger;
	private final ManagedScript mManagedScript;
	private final Script mScript;
	private final BasicPredicateFactory mFactory;

	private final IPetriNet<LETTER, PLACE> mNet;
	private final Map<Marking<LETTER, PLACE>, IPredicate> mFloydHoareAnnotation;
	private final DefaultIcfgSymbolTable mSymbolTable;

	private static final SimplificationTechnique mSimplificationTechnique = SimplificationTechnique.SIMPLIFY_DDA;
	private static final XnfConversionTechnique mXnfConversionTechnique =
			XnfConversionTechnique.BOTTOM_UP_WITH_LOCAL_SIMPLIFICATION;

	private final Map<PLACE, IProgramVar> mGhostVariables;
	private final OwickiGriesAnnotation<LETTER, PLACE> mAnnotation;
	

	public OwickiGriesConstruction(final IUltimateServiceProvider services, final CfgSmtToolkit csToolkit,
			final IPetriNet<LETTER, PLACE> net, final Map<Marking<LETTER, PLACE>, IPredicate> floydHoare) {
		mServices = services;
		mLogger = services.getLoggingService().getLogger(ModelCheckerUtils.PLUGIN_ID);
		mManagedScript = csToolkit.getManagedScript();
		mScript = mManagedScript.getScript();

		mNet = net;
		mFloydHoareAnnotation = floydHoare;
		mSymbolTable = new DefaultIcfgSymbolTable(csToolkit.getSymbolTable(), csToolkit.getProcedures());
		mFactory = new BasicPredicateFactory(mServices, mManagedScript, mSymbolTable);

		mGhostVariables = getGhostVariables();
		final Map<PLACE, IPredicate> formulaMapping = getFormulaMapping();
		final Map<ITransition<LETTER, PLACE>, UnmodifiableTransFormula> assignmentMapping = getAssignmentMapping();
		final Map<IProgramVar, Term> ghostInitAssignment = getGhostInitAssignment();

		mAnnotation = new OwickiGriesAnnotation<>(formulaMapping, assignmentMapping,
				new HashSet<>(mGhostVariables.values()), ghostInitAssignment, mNet, mSymbolTable);
	}
	


	/**
	 * Constructs the mapping from places to formulas. A place is mapped to a disjunction of marking predicates, where
	 * each marking predicate is a conjunction of ghost variables and a Floyd/Hoare predicate.
	 *
	 * @return a map with a predicate for each place in the Petri net
	 */
	private Map<PLACE, IPredicate> getFormulaMapping() {
		final Map<PLACE, IPredicate> mapping = new HashMap<>();

		final Set<Marking<LETTER, PLACE>> reachableMarkings = mFloydHoareAnnotation.keySet();
		for (final PLACE place : mNet.getPlaces()) {
			final Set<Term> clauses = reachableMarkings.stream().filter(m -> m.contains(place))
					.map(this::getMarkingPredicate).collect(Collectors.toSet());
			final Term disjunction = SmtUtils.or(mScript, clauses);
			mapping.put(place, mFactory.newPredicate(disjunction));
		}

		return mapping;
	}

	/**
	 * @param place
	 * @param marking
	 * @return Predicate with conjunction of Ghost variables and predicate of marking
	 */
	private Term getMarkingPredicate(final Marking<LETTER, PLACE> marking) {
		final Set<Term> terms = new HashSet<>();
		for (final PLACE otherPlace : marking) {
			final Term ghost = mGhostVariables.get(otherPlace).getTerm();
			terms.add(ghost);
		}
		terms.addAll(getHitNotMarking(marking));
		terms.add(mFloydHoareAnnotation.get(marking).getFormula());
		return SmtUtils.and(mScript, terms);
	}

	/**
	 *
	 * @param marking
	 * @return Formula MethodA:Predicate with GhostVariables of all other places not in marking
	 */
	private Set<Term> getAllNotMarking(final Marking<LETTER, PLACE> marking) {
		final Set<PLACE> markPlaces = marking.stream().collect(Collectors.toSet());
		final Set<PLACE> notMarking = DataStructureUtils.difference(new HashSet<>(mNet.getPlaces()), markPlaces);
		final Set<Term> predicates = new HashSet<>();
		for (final PLACE place : notMarking) {
			final Term ghost = mGhostVariables.get(place).getTerm();
			predicates.add(SmtUtils.not(mScript, ghost));
		}
		return predicates;
	}
	
	
	/**
	 *
	 * @param marking
	 * @return Formula MethodB:Predicate with GhostVariables of hitting set of other places not in marking
	 */
	private Set<Term> getHitNotMarking(final Marking<LETTER, PLACE> marking) {
		//TODO: quitar marking de este Hitting set?
		final Set<PLACE> markPlaces = marking.stream().collect(Collectors.toSet());
		final Set<Set<PLACE>> symmDifference = getSymmDifferences(marking);
		final Set<PLACE> notMarking = getHittingSet(symmDifference);
		final Set<Term> predicates = new HashSet<>();
		for (final PLACE place : notMarking) {
			final Term ghost = mGhostVariables.get(place).getTerm();
			predicates.add(SmtUtils.not(mScript, ghost));
		}
		return predicates;
	}
	
	/**
	 * @param marking
	 * @return set of symmetric differences of marking to all different reachable markings
	 */
	private Set<Set<PLACE>> getSymmDifferences(final Marking<LETTER, PLACE> marking){
		 Set<Set<PLACE>> differences= new HashSet<>();
		 Set<Marking<LETTER, PLACE>> reachableMarkings = mFloydHoareAnnotation.keySet();
		 for (final Marking<LETTER, PLACE> reachable : reachableMarkings) {
			 if (!marking.equals(reachable)) {
			 differences.add(SymmDiff(marking.stream().collect(Collectors.toSet()), 
					 	reachable.stream().collect(Collectors.toSet())));}}
		 return differences;
	}
	
	/**
	 * @param s1
	 * @param s2
	 * @return Symmetric difference of set
	 * TODO:Already implemented in Ultimate? Guava library?
	 */
	private Set<PLACE> SymmDiff(Set<PLACE> s1, Set<PLACE> s2){
		Set<PLACE> symmetricDiff = new HashSet<PLACE>(s1);
	    symmetricDiff.addAll(s2);
	    Set<PLACE> tmp = new HashSet<PLACE>(s1);
	    tmp.retainAll(s2);
	    symmetricDiff.removeAll(tmp); // use DataStructre.difference??
	    return symmetricDiff;
	}
	/**	
	 * 
	 * @param symmDifference
	 * @return hitting set of symmetricDifference
	 */
	private Set<PLACE> getHittingSet(Set<Set<PLACE>> symmDifference) {
		Set<PLACE> hittingSet = new HashSet<PLACE>();
		Set<Set<PLACE>> uncovered = new HashSet<Set<PLACE>>(symmDifference);
		Set<Set<PLACE>> covered = new HashSet<Set<PLACE>>();
		Set<Set<PLACE>> unchecked = new HashSet<Set<PLACE>>(symmDifference);//TODO: Order by size, more efficient

		for (Set<PLACE> set : unchecked) {
			
			if (!checkHittingSet(hittingSet,symmDifference)) {
				Set<PLACE> greedySet = getGreedySet(uncovered, unchecked);
				hittingSet.addAll(greedySet);
				ArrayList<Set<PLACE>> inter = getIntersections(greedySet, uncovered);
				covered.addAll(inter);
				uncovered.removeAll(inter);
			 }
			else {
				break;
			}
			unchecked.remove(set);
		}
		
		assert checkHittingSet(hittingSet,symmDifference) : "error hittings set";
		return hittingSet;
	}
	
	/**
	 * 
	 * @param hittingSet
	 * @param setUniverse
	 * @return true if hittinSet intersects with all sets in Universe
	 */
	
	private boolean checkHittingSet(Set<PLACE> hittingSet, Set<Set<PLACE>> setUniverse) {
		Set<Set<PLACE>> universe = new HashSet<Set<PLACE>>(setUniverse);
		for (Set<PLACE> set : universe) {
			if (DataStructureUtils.intersection(set, hittingSet).isEmpty()) {
				return false;
			}			
		}		
		return true;
	}
	
	
	/**
	 * 
	 * @param set
	 * @param collection
	 * @return List of sets with which a set intersects
	 */
	private ArrayList<Set<PLACE>> getIntersections(Set<PLACE> set, Set<Set<PLACE>> collection){
		ArrayList<Set<PLACE>> intersections = new ArrayList<Set<PLACE>>();
		for (Set<PLACE> s : collection) {
			if (!DataStructureUtils.intersection(set, s).isEmpty()) {
				intersections.add(s);
			}
		}
		return intersections;
	}

	/**
	 * 
	 * @param uncovered
	 * @param collection of not selected sets
	 * @return return set in uncovered
	 */
	private Set<PLACE> getGreedySet(Set<Set<PLACE>> uncovered, Set<Set<PLACE>> collection) {
		Set<PLACE> greedy = new HashSet<PLACE>();
		ArrayList<Set<PLACE>> intersections = new ArrayList<Set<PLACE>>();
		for (Set<PLACE> set : collection) {
			ArrayList<Set<PLACE>> setInter = getIntersections(set,uncovered); //correct getintersections
			if ( setInter.size() > intersections.size()) {
				greedy = set;
				intersections = setInter;
			}
		}
		return greedy;
		
	}
	
	/**
	 *
	 * @param marking
	 * @return Formula MethodA: GhostVariables if marking is subset of other marking
	 */
	private Set<Term> getSubsetMarking(final Marking<LETTER, PLACE> marking) {
		final Set<PLACE> markPlaces = marking.stream().collect(Collectors.toSet());
		final Set<Marking<LETTER, PLACE>> markings = mFloydHoareAnnotation.keySet();
		final Set<PLACE> notInMarking = new HashSet<>();
		for (final Marking<LETTER, PLACE> otherMarking : markings) {
			notInMarking.addAll(getSupPlaces(otherMarking, markPlaces));
		}
		final Set<Term> predicates = new HashSet<>();
		for (final PLACE place : notInMarking) {
			final Term ghost = mGhostVariables.get(place).getTerm();
			predicates.add(SmtUtils.not(mScript, ghost));
		}
		return predicates;
	}

	private Set<PLACE> getSupPlaces(final Marking<LETTER, PLACE> otherMarking, final Set<PLACE> markPlaces) {
		Set<PLACE> subPlaces = new HashSet<>();
		final Set<PLACE> otherPlaces = otherMarking.stream().collect(Collectors.toSet());
		if (otherPlaces.containsAll(markPlaces)) {
			subPlaces = DataStructureUtils.difference(otherPlaces, markPlaces);
		}
		return subPlaces;
	}

	/**
	 * @return map of places to ghost variables
	 */
	private Map<PLACE, IProgramVar> getGhostVariables() {
		final Map<PLACE, IProgramVar> ghostVars = new HashMap<>();
		int i = 0;
		final Collection<PLACE> places = mNet.getPlaces();
		mManagedScript.lock(this);
		try {
			for (final PLACE place : places) {
				// TODO (Dominik 2021-01-27) Name ghost variables by place for easier debugging
				final TermVariable tVar =
						mManagedScript.constructFreshTermVariable("np" + i, SmtSortUtils.getBoolSort(mManagedScript));
				final IProgramVar pVar = ProgramVarUtils.constructGlobalProgramVarPair(tVar.getName(),
						SmtSortUtils.getBoolSort(mManagedScript), mManagedScript, this);
				mSymbolTable.add(pVar);
				ghostVars.put(place, pVar);
				i++;
			}
			return ghostVars;
		} finally {
			mManagedScript.unlock(this);
		}
	}

	/**
	 * @return initial value assignment of all ghost variables.
	 */
	private Map<IProgramVar, Term> getGhostInitAssignment() {
		final HashMap<IProgramVar, Term> initAssignments = new HashMap<>();
		for (final PLACE place : mNet.getInitialPlaces()) {
			initAssignments.put(mGhostVariables.get(place), mScript.term("true"));
		}
		final Set<IProgramVar> notInitGhostVariables =
				DataStructureUtils.difference(new HashSet<>(mGhostVariables.values()), initAssignments.keySet());
		for (final IProgramVar variable : notInitGhostVariables) {
			initAssignments.put(variable, mScript.term("false"));
		}
		return initAssignments;
	}

	/**
	 *
	 * @param place
	 * @return assignment of the place's GhostVariable.
	 */
	private UnmodifiableTransFormula getGhostAssignment(final Collection<IProgramVar> vars, final String term) {
		return TransFormulaBuilder.constructAssignment(new ArrayList<>(vars),
				Collections.nCopies(vars.size(), mScript.term(term)), mSymbolTable, mManagedScript);
	}

	/**
	 *
	 * @return Map of Places' Ghost Variables assignments to Transitions
	 *
	 */
	private Map<ITransition<LETTER, PLACE>, UnmodifiableTransFormula> getAssignmentMapping() {
		final Map<ITransition<LETTER, PLACE>, UnmodifiableTransFormula> assignmentMapping = new HashMap<>();
		for (final ITransition<LETTER, PLACE> transition : mNet.getTransitions()) {
			assignmentMapping.put(transition, getTransitionAssignment(transition));
		}
		return assignmentMapping;
	}

	/**
	 *
	 * @param transition
	 * @return TransFormula of sequential compositions of GhostVariables assignments. GhostVariables of Predecessors
	 *         Places are assign to false, GhostVariables of Successors Places are assign to true.
	 */
	private UnmodifiableTransFormula getTransitionAssignment(final ITransition<LETTER, PLACE> transition) {
		final List<UnmodifiableTransFormula> assignments = new ArrayList<>();
		for (final PLACE place : mNet.getPredecessors(transition)) {
			assignments.add(getGhostAssignment(Collections.nCopies(1, mGhostVariables.get(place)), "false"));
		}
		for (final PLACE place : mNet.getSuccessors(transition)) {
			assignments.add(getGhostAssignment(Collections.nCopies(1, mGhostVariables.get(place)), "true"));
		}
		return TransFormulaUtils.sequentialComposition(mLogger, mServices, mManagedScript, false, false, false,
				mXnfConversionTechnique, mSimplificationTechnique, assignments);
	}

	public OwickiGriesAnnotation<LETTER, PLACE> getResult() {
		return mAnnotation;
	}

	public HashRelation<ITransition<LETTER, PLACE>, PLACE> getCoMarkedPlaces() {
		final HashRelation<ITransition<LETTER, PLACE>, PLACE> relation = new HashRelation<>();
		for (final ITransition<LETTER, PLACE> transition : mNet.getTransitions()) {
			final Set<PLACE> predecessors = mNet.getPredecessors(transition);
			final Set<Marking<LETTER, PLACE>> enabledMarkings = mFloydHoareAnnotation.keySet().stream()
					.filter(marking -> marking.containsAll(predecessors)).collect(Collectors.toSet());
			for (final Marking<LETTER, PLACE> marking : enabledMarkings) {
				for (final PLACE place : marking) {
					// places that are not predecessors of transition
					if (!predecessors.contains(place)) {
						relation.addPair(transition, place);
					}
				}
			}
		}
		return relation;
	}
	

}
