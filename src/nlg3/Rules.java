package nlg3;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import lf.NLUUtils;
import lf.pos.NP;
import lf.pos.POS;
import nlg2.COPANLG2;
import nlg3.parser.ParseException;
import nlg3.parser.Parser;
import nlg3.properties.Properties;
import nlg3.reference.ReferenceStack;
import edu.usc.ict.nl.util.FunctionalLibrary;
import edu.usc.ict.nl.util.StringUtils;
import edu.usc.ict.nl.utils.ExcelUtils;

public class Rules {
	private enum GROUPMODE {ALL,COMB};
	private Map<String,Rule> rules=null;
	private List<Integer> groups=null;
	private Map<Integer,GROUPMODE> groupsProperties=null;
	private int debug;

	public Rules(String pFile, int debug) {
		this.debug=debug;
		int skipRowsUpToAndIncluding=0;
		Map<Integer, String> active = ExcelUtils.extractRowAndThisColumn(pFile, skipRowsUpToAndIncluding, 0);
		Map<Integer, String> disablings = ExcelUtils.extractRowAndThisColumn(pFile, skipRowsUpToAndIncluding, 1);
		Map<Integer, String> groupings = ExcelUtils.extractRowAndThisColumn(pFile, skipRowsUpToAndIncluding, 2);
		Map<Integer, String> groupsProperties = ExcelUtils.extractRowAndThisColumn(pFile, skipRowsUpToAndIncluding, 3);
		Map<Integer, String> triggers = ExcelUtils.extractRowAndThisColumn(pFile, skipRowsUpToAndIncluding, 5);
		int rowsSkipped=(skipRowsUpToAndIncluding>=0)?skipRowsUpToAndIncluding:0;
		Set<Integer> allGroups=null;
		int startColumn=6;
		Map<Integer, List<String>> builders = ExcelUtils.extractRowsAndColumnWiseData(pFile, 0, skipRowsUpToAndIncluding, startColumn, -1, true, true);
		if (triggers!=null && builders!=null && groupings!=null && disablings!=null && groupsProperties!=null) {
			for(Integer row:triggers.keySet()) {
				String a=(active!=null && active.get(row)!=null)?active.get(row):"true";
				if (a.equalsIgnoreCase("true")) {
					try {
						String group=groupings.get(row);
						int g=(!StringUtils.isEmptyString(group))?Math.round(Float.parseFloat(group)):0;
						if (allGroups==null) allGroups=new HashSet<Integer>();
						allGroups.add(g);
						addEntry(triggers.get(row),builders.get(row),disablings.get(row),g,row,startColumn,rowsSkipped);
						String gp=groupsProperties.get(row);
						GROUPMODE gm=GROUPMODE.COMB;
						if (!StringUtils.isEmptyString(gp)) gm=GROUPMODE.valueOf(gp);
						if (this.groupsProperties==null) this.groupsProperties=new HashMap<Integer, Rules.GROUPMODE>();
						GROUPMODE ogm=this.groupsProperties.get(g);
						if (ogm==null) this.groupsProperties.put(g, gm);
					} catch (Exception e) { e.printStackTrace(); }
				}
			}
			groups=new ArrayList<Integer>(allGroups);
			Collections.sort(groups);
		}
	}

	private void addEntry(String trigger, List<String> builders, String dss, int group,Integer row,int baseColumn,int rowsSkipped) {
		Parser pparser = new Parser(new StringReader(trigger));
		try {
			SyntaxBuilder p=pparser.syntax();
			//System.out.println(p);
			if (rules==null) rules=new HashMap<String,Rule>();
			Rule r=rules.get(p.toString());
			if (r==null) rules.put(p.toString(),r=new Rule(p));
			Parser bparser=null;
			int column=0;
			for(String b:builders) {
				if (bparser==null) bparser = new Parser(new StringReader(b));
				else bparser.ReInit(new StringReader(b));
				try {
					SyntaxBuilder builder = bparser.syntax();
					builder.setRow(row+rowsSkipped+1);
					builder.setColumn(baseColumn+column);
					builder.setGroup(group);
					r.addBuilder(builder);
					if (!StringUtils.isEmptyString(dss)) {
						String[] ds=dss.split(",");
						builder.disablesBuildersAtRows(ds);
					}
				} catch (Exception e) {
					System.err.println(b);
					e.printStackTrace();
				}
				column++;
			}
		} catch (ParseException e) {
			System.err.println(trigger);
			e.printStackTrace();
		}
	}

	private List<SyntaxBuilder> getSyntaxBuilders(POS thing, TraversalState state,int group) throws Exception {
		List<SyntaxBuilder> ret=null;
		Set<Integer> disabledRows=null;
		if (rules!=null) {
			for(String p:rules.keySet()) {
				Rule rule=rules.get(p);
				if (rule.hasBuilderInGroup(group)) {
					if (rule.evaluate(thing,state)) {
						List<SyntaxBuilder> builders = rule.getBuildersForGroup(group);
						if (builders!=null) {
							for(SyntaxBuilder b:builders) {
								if ((!b.changeParent() || hasParent(state)) && (disabledRows==null || !disabledRows.contains(b.getRow()))) {
									if (ret==null) ret=new ArrayList<SyntaxBuilder>();
									ret.add(b);
									Set<Integer> dbs = b.getDisabledBuilders();
									if (dbs!=null) {
										if (disabledRows==null) disabledRows=new HashSet<Integer>();
										disabledRows.addAll(dbs);
									}
								}
							}
						}
					}
				}
			}
		}
		if (disabledRows!=null && ret!=null) {
			//remove the builders that were added earlier but that are now disabled by later builders.
			Iterator<SyntaxBuilder> it=ret.iterator();
			while(it.hasNext()) {
				SyntaxBuilder b=it.next();
				if (disabledRows.contains(b.getRow())) it.remove();
			}
		}
		return ret;
	}
	private int getStartingGroup() {
		if (groups!=null && !groups.isEmpty()) return groups.get(0);
		return -1;
	}
	/**
	 * if no next group is found, returns the same input group.
	 * @param currentGroup
	 * @return
	 */
	private int getNextGroup(int currentGroup) {
		if (groups!=null && !groups.isEmpty()) {
			boolean ret=false;
			for(int g:groups) {
				if (g==currentGroup) ret=true;
				else if (ret) return g;
			}
		}
		return currentGroup;

	}

	private boolean hasParent(TraversalState state) {
		return (state!=null && state.coord!=null && !state.coord.isEmpty());
	}

	public List<POSwT> generateSyntax(POS root2, NLG3Lexicon nlg3Lexicon) throws Exception {
		List<POSwT> ret=new ArrayList<POSwT>();
		POSwT root=new POSwT(root2);
		ret.add(root);
		root=root.clone();
		ret.add(root);
		TraversalState state=new TraversalState();
		if (!NLUUtils.replaceWithNames) {
			state.setAsUnavailable(Properties.pnameFEMALEPROPERTY);
			state.setAsUnavailable(Properties.pnameMALEPROPERTY);
		}
		state.root=root;
		state.coord=new LinkedList<Integer>();
		state.lex=nlg3Lexicon;
		state.createEmptyReferenceStack();
		generateSyntax(root.getLast(),ret,state,new HashSet<String>(),getStartingGroup());
		return ret;
	}

	/**
	 * recursively traverse the input POS looking for all POS object. For every element that fires a syntax builder execute it and add substitute the node with the possible results.
	 * 
	 * @param d
	 * @return
	 * @throws Exception 
	 */
	private void generateSyntax(POS d,List<POSwT> ret,TraversalState state,Set<String> alreadySeenStates,int group) throws Exception {
		while(true) {
			/*
			// for debugging only
			if (state.coord.size()==5 && state.coord.toString().equals("[1, 1, 1, 1, 0]") && group==5) {
				System.out.println("1");
			}
			 */
			LinkedList<Integer> parentCoord=null,currentCoord=state.coord,coordToChange=null;
			POS parent=null;
			List<SyntaxBuilder> builders = getSyntaxBuilders(d,state,group);
			String ind=(debug>3)?StringUtils.create(' ',state.coord.size()):"";
			if (debug>3) System.out.println(ind+"____group "+group+"("+getGroupMode(group)+")");
			if (debug>3) System.out.println(ind+state.coord+": "+d);
			if (debug>3) System.out.println(ind+"references: "+state.getReferencesForState(state.coord));
			if (debug>3) System.out.println(ind+"builders: "+builders);
			//update references
			if (builders!=null) {
				if (getGroupMode(group)==GROUPMODE.ALL && builders.size()>1) throw new Exception("group "+group+" is in ALL mode but multiple rules "+FunctionalLibrary.map(builders, SyntaxBuilder.class.getMethod("getRow"))+" can apply to the same node.");
				POS thingToChange=null;
				for (SyntaxBuilder b:builders) {
					if (debug>3) System.out.println(ind+"running builder: "+b);
					if (b.changeParent()) {
						if (parentCoord==null) parentCoord=POS.getParentCoord(currentCoord);
						if (parent==null) parent=state.root.getLast().getElementAt(parentCoord);
						thingToChange=parent;
						coordToChange=parentCoord;
					} else {
						coordToChange=currentCoord;
						thingToChange=d;
					}
					if (debug>3) {
						System.out.println(ind+"applying rule: "+b);
						System.out.println(ind+"before: "+state.root.getLast());
					}
					POS newd = (POS)b.generateSyntaxRoot(thingToChange,state,null,false);
					if (newd!=null) {
						POS newResultRoot=null;
						if (coordToChange==null || coordToChange.isEmpty()) {
							newResultRoot=newd;
						} else {
							newResultRoot = state.root.getLast().clone();
							newResultRoot.put(coordToChange,newd);
						}
						if (debug>3) System.out.println(ind+"result: "+newResultRoot);
						String signature=newResultRoot.toString();
						if (!alreadySeenStates.contains(signature)) {
							alreadySeenStates.add(signature);
							POSwT newRoot=state.root;
							if (getGroupMode(group)==GROUPMODE.COMB) {
								newRoot=state.root.clone();
								newRoot.addRuleAndResult(b, newResultRoot,new ArrayList<Integer>(currentCoord));
								ret.add(newRoot);

								// start from the root again
								LinkedList<Integer> tmp=new LinkedList<Integer>();
								TraversalState cstate=(TraversalState) state.clone();
								cstate.coord=tmp;
								cstate.root=newRoot;
								cstate.createEmptyReferenceStack();
								generateSyntax(newRoot.getLast(),ret,cstate,alreadySeenStates,group);
								// keep going from where we are
								/*
						LinkedList<Integer> tmp=new LinkedList<Integer>(coordToChange);
						OS newchild=newRoot.getNextItem(tmp);
						if (newchild!=null) {
							Deque<POS> newReferences=new ArrayDeque<POS>();
							newReferences.addAll(references);
							TraversalState cstate=new TraversalState();
							cstate.coord=tmp;
							cstate.root=newRoot;
							generateSyntax(newchild,ret,newReferences,cstate);
						}
								 */
							} else {
								newRoot.addRuleAndResult(b, newResultRoot,new ArrayList<Integer>(currentCoord));
							}
						}
					} else {
						System.err.println("builder "+b+" returned null.");
					}
				}
			}
			state.coord=currentCoord;
			state.updateReferencesWithCurrentPosition();
			POS newchild=state.root.getLast().getNextItem(state.coord);
			if (newchild!=null) {
				d=newchild;
			} else {
				int newgroup=getNextGroup(group);
				if (newgroup!=group) {
					if (debug>3) System.out.println("changing group from "+group+" to "+newgroup);
					state.coord=new LinkedList<Integer>();
					state.clearReferences();
					d=state.root.getLast();
					group=newgroup;
				} else {
					break;
				}
			}
		}
	}

	public GROUPMODE getGroupMode(int group) {
		if (groupsProperties!=null) {
			GROUPMODE gm=groupsProperties.get(group);
			if (gm!=null) return gm;
		}
		return GROUPMODE.COMB; 
	}

	public static void main(String[] args) {
		new Rules("rules-nlg3.xlsx",0);
	}
}
