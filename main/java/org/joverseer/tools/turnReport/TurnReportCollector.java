package org.joverseer.tools.turnReport;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.Map.Entry;
import java.util.logging.LogManager;

import org.apache.log4j.Logger;
import org.joverseer.domain.Army;
import org.joverseer.domain.ArmyElementType;
import org.joverseer.domain.ArmySizeEnum;
import org.joverseer.domain.Artifact;
import org.joverseer.domain.Challenge;
import org.joverseer.domain.Character;
import org.joverseer.domain.CharacterDeathReasonEnum;
import org.joverseer.domain.Combat;
import org.joverseer.domain.Company;
import org.joverseer.domain.Encounter;
import org.joverseer.domain.FortificationSizeEnum;
import org.joverseer.domain.HarborSizeEnum;
import org.joverseer.domain.NationEconomy;
import org.joverseer.domain.NationMessage;
import org.joverseer.domain.NationRelations;
import org.joverseer.domain.PlayerInfo;
import org.joverseer.domain.PopulationCenter;
import org.joverseer.domain.PopulationCenterSizeEnum;
import org.joverseer.domain.SpellProficiency;
import org.joverseer.game.Game;
import org.joverseer.game.Turn;
import org.joverseer.game.TurnElementsEnum;
import org.joverseer.metadata.GameMetadata;
import org.joverseer.metadata.domain.Hex;
import org.joverseer.metadata.domain.HexSideElementEnum;
import org.joverseer.metadata.domain.HexSideEnum;
import org.joverseer.metadata.domain.HexTerrainEnum;
import org.joverseer.metadata.domain.Nation;
import org.joverseer.metadata.domain.NationAllegianceEnum;
import org.joverseer.support.Container;
import org.joverseer.support.GameHolder;
import org.joverseer.support.NationMap;
import org.joverseer.support.StringUtils;
import org.joverseer.support.info.InfoUtils;
import org.joverseer.support.infoSources.XmlTurnInfoSource;
import org.joverseer.support.infoSources.spells.DerivedFromSpellInfoSource;
import org.joverseer.support.readers.pdf.CombatArmy;
import org.joverseer.support.readers.pdf.CombatWrapper;
import org.joverseer.tools.CombatUtils;
import org.joverseer.tools.infoCollectors.artifacts.ArtifactInfoCollector;
import org.joverseer.tools.infoCollectors.artifacts.ArtifactWrapper;
import org.joverseer.tools.infoCollectors.characters.AdvancedCharacterWrapper;
import org.joverseer.tools.infoCollectors.characters.CharacterInfoCollector;

import com.sun.corba.se.impl.orbutil.HexOutputStream;

import sun.text.normalizer.CharTrie.FriendAgent;


public class TurnReportCollector {
	
	public ArrayList CollectNonFriendlyChars(Turn t) {
		ArrayList ret = new ArrayList();
		ArrayList friendlyNations = new ArrayList();
		for (PlayerInfo pi : (ArrayList<PlayerInfo>)t.getContainer(TurnElementsEnum.PlayerInfo).getItems()) {
			friendlyNations.add(pi.getNationNo());
		}
		for (Character c : t.getAllCharacters()) {
			if (friendlyNations.contains(c.getNationNo())) continue;
			if (c.getHostage() != null && c.getHostage() == true) continue;
			if (!c.getDeathReason().equals(CharacterDeathReasonEnum.NotDead)) continue;
			Army a = t.getArmy(c.getName());
			if (a != null) continue;
			if (InfoUtils.isDragon(c.getName())) continue;

			CharacterReport cr;
			AdvancedCharacterWrapper acw = CharacterInfoCollector.instance().getCharacterForTurn(c.getName(), t.getTurnNo());
			if (acw.getTurnNo() != t.getTurnNo()) acw = null;
			if (acw != null) {
				cr = new CharacterReport(acw);
			} else {
				cr = new CharacterReport(c);
			}
			cr.setHexNo(c.getHexNo());
			PopulationCenter pop = t.getPopCenter(c.getHexNo());
			if (pop != null) {
				cr.setNotes("PC: " + popPlusNation(pop));
			}
			if (c.getInfoSource() != null && !XmlTurnInfoSource.class.isInstance(c.getInfoSource())) {
				cr.appendNote(c.getInfoSource().getDescription());
			}
			cr.setModification(ObjectModificationType.Modified);
			ret.add(cr);
		}
		return ret;
	}
	
	public ArrayList CollectDragons(Turn t) {
		Nation gameNation = NationMap.getNationFromNo(GameHolder.instance().getGame().getMetadata().getNationNo());
		NationAllegianceEnum gameNationAllegiance = gameNation.getAllegiance();
		ArrayList ret = new ArrayList();
		// dragons in friendly armies
		for (Army a : (ArrayList<Army>)t.getContainer(TurnElementsEnum.Army).getItems()) {
			for (String cn : (ArrayList<String>)a.getCharacters()) {
				if (InfoUtils.isDragon(cn)) {
					CharacterReport cr = new CharacterReport();
					cr.setName(cn);
					cr.setHexNo(Integer.parseInt(a.getHexNo()));
					cr.setNationNo(a.getNationNo());
					cr.setNotes("With army " + namePlusNation(a.getCommanderName(), a.getNation()));
					cr.setModification(ObjectModificationType.Gained);
					ret.add(cr);
				}
			}
		}
		// dragons in characters
		for (Character c : (ArrayList<Character>)t.getContainer(TurnElementsEnum.Character).getItems()) {
			if (!InfoUtils.isDragon(c.getName())) continue;
			String hexNo = String.valueOf(c.getHexNo());
			if (c.getHexNo() < 1000) hexNo = "0" + hexNo;
			
			ArrayList<Character> chars = (ArrayList<Character>)t.getContainer(TurnElementsEnum.Character).findAllByProperty("hexNo", c.getHexNo());
			
			ArrayList<Army> armies = (ArrayList<Army>)t.getContainer(TurnElementsEnum.Army).findAllByProperty("hexNo", hexNo);
			Army army = null;
			boolean existsEnemyArmy = false;
			String enemyArmies = "";
			for (Army a : armies) {
				if (a.getCharacters().contains(c.getName())) {
					army = a;
					break;
				}
				if (!a.getNationAllegiance().equals(gameNationAllegiance)) {
					existsEnemyArmy = true;
					enemyArmies += (enemyArmies.equals("") ? "" : ",") + a.getCommanderName();
				}
			}
			if (army != null) continue;
			
			PopulationCenter pc = (PopulationCenter)t.getPopCenter(c.getHexNo());
			
			CharacterReport cr = new CharacterReport(c);
			if (existsEnemyArmy) {
				cr.setModification(ObjectModificationType.Lost);
				cr.setNotes("Enemy armies: " + enemyArmies);
			}
			else {
				cr.setModification(ObjectModificationType.Modified);
			}
			
			if (pc != null) {
				cr.appendNote(" PC: " + popPlusNation(pc));
			}
		
			String friendlyChars = ""; 
			for (Character ch : chars) {
				if (ch == c) continue;
				if (ch.getNationNo() != 0 && ch.getNation().getAllegiance().equals(gameNationAllegiance)) {
					friendlyChars += (friendlyChars.equals("") ? "" : ",") + charPlusNation(ch); 
				}
			}
			if (!friendlyChars.equals("")) cr.appendNote(" Chars: " + friendlyChars);

			for (Encounter e : (ArrayList<Encounter>)t.getContainer(TurnElementsEnum.Encounter).findAllByProperty("hexNo", c.getHexNo())) {
				if (e.getCleanDescription().contains(c.getName())) {
					cr.appendNote(" Encounter: " + e.getCharacter());
				}
			}
			
			ret.add(cr);
		}
		
		return ret;
	}
	
	
	public ArrayList CollectSpells(Turn t, Turn p) {
		ArrayList ret = new ArrayList();
		for (PlayerInfo pi : (ArrayList<PlayerInfo>)t.getContainer(TurnElementsEnum.PlayerInfo).getItems()) {
			int nationNo = pi.getNationNo();
			for (Character c : (ArrayList<Character>)t.getContainer(TurnElementsEnum.Character).getItems()) {
				if (!c.getNationNo().equals(nationNo)) continue;
				Character pc = (Character)p.getContainer(TurnElementsEnum.Character).findFirstByProperty("name", c.getName());
				if (pc == null) continue;
				for (SpellProficiency sp : c.getSpells()) {
					boolean found = false;
					for (SpellProficiency sp1 : pc.getSpells()) {
						if (sp.getSpellId()==sp1.getSpellId()) {
							found = true;
							break;
						}
					}
					if (!found) {
						CharacterReport cr = new CharacterReport(c);
						cr.setModification(ObjectModificationType.Gained);
						cr.setNotes("Learnt " + sp.getName() + " at " + sp.getProficiency());
						ret.add(cr);
					}
				}
			}
		}
		return ret;
	}
	public ArrayList CollectAgentActions(Turn t, Turn p) {
		ArrayList ret = new ArrayList();
		for (PlayerInfo pi : (ArrayList<PlayerInfo>)t.getContainer(TurnElementsEnum.PlayerInfo).getItems()) {
			int nationNo = pi.getNationNo();
			for (Character c : (ArrayList<Character>)t.getContainer(TurnElementsEnum.Character).getItems()) {
				if (!c.getNationNo().equals(nationNo)) continue;
				if (!c.getDeathReason().equals(CharacterDeathReasonEnum.NotDead)) continue;
				Character pc = (Character)p.getContainer(TurnElementsEnum.Character).findFirstByProperty("name", c.getName());
				String orderResults = c.getCleanOrderResults();
				String prefix = StringUtils.getFirstWord(orderResults);
				String lprefix = prefix.toLowerCase();
				String lprefixGen = lprefix.equals("she") ? "her" : "his";
				if (orderResults.contains("was ordered to assassinate a character.")) {
					boolean failure = orderResults.contains("was not able to assassinate the character") ||
										orderResults.contains("was not able to complete " + lprefixGen + " mission because the character was too well guarded");
					CharacterReport cr = new CharacterReport(c);
					if (pc != null) {
						cr.setHexNo(pc.getHexNo());
					}
					ret.add(cr);
					if (!failure) {
						String assassinatedChar = StringUtils.getUniquePart(orderResults, "was ordered to assassinate a character.", " was assassinated.", false, false);
						cr.setNotes("Assassinated " + getAdvCharWrapperStr(assassinatedChar, t));
						cr.setModification(ObjectModificationType.Gained);
					} else {
						cr.setNotes("Failed assassination");
						cr.setModification(ObjectModificationType.Lost);
					}
				}
				if (orderResults.contains("was ordered to kidnap a character.")) {
					boolean failure = orderResults.contains("was not able to kidnap the character") ||
									orderResults.contains("was not able to complete " + lprefixGen + " mission because the character was too well guarded");
					String kidnappedChar = StringUtils.getUniquePart(orderResults, "was ordered to kidnap a character.", " was kidnaped.", false, false);
					CharacterReport cr = new CharacterReport(c);
					if (pc != null) {
						cr.setHexNo(pc.getHexNo());
					}
					ret.add(cr);
					if (!failure) {
						cr.setNotes("Kidnapped " + getAdvCharWrapperStr(kidnappedChar, t));
						cr.setModification(ObjectModificationType.Gained);
					} else {
						cr.setNotes("Failed kidnap");
						cr.setModification(ObjectModificationType.Lost);
					}
				}
				if (orderResults.contains("was ordered to sabotage the fortifications.")) {
					boolean failure = orderResults.contains("was not able to sabotage the fortifications")||
								orderResults.contains("was not able to complete " + lprefixGen + " mission because the character was too well guarded");
					CharacterReport cr = new CharacterReport(c);
					if (pc != null) {
						cr.setHexNo(pc.getHexNo());
					}
					ret.add(cr);
					if (!failure) {
						String pop = StringUtils.getUniquePart(orderResults, "The fortifications were sabotaged at", "\\.", false, false);
						cr.setNotes("Sab Fort at " + popPlusNation(pop, t));
						PopulationCenter pp = p.getPopCenter(pop);
						PopulationCenter cp = t.getPopCenter(pop);
						cr.appendNote(pp.getFortification().getRenderString() + "→" + cp.getFortification().getRenderString());
						cr.setModification(ObjectModificationType.Gained);
					} else {
						cr.setNotes("Failed sab fort");
						cr.setModification(ObjectModificationType.Lost);
					}
				}
				if (orderResults.contains("was ordered to sabotage the harbor/port")) {
					boolean failure = orderResults.contains("was not able to sabotage the Harbor") ||
										orderResults.contains("was not able to sabotage the Port");
					CharacterReport cr = new CharacterReport(c);
					if (pc != null) {
						cr.setHexNo(pc.getHexNo());
					}
					ret.add(cr);
					if (!failure) {
						String pop = StringUtils.getUniquePart(orderResults, "was sabotaged at ", "\\.", false, false);
						cr.setNotes("Sab Docks at " + popPlusNation(pop, t));
						PopulationCenter pp = p.getPopCenter(pop);
						PopulationCenter cp = t.getPopCenter(pop);
						if (pp != null && cp != null && pp.getHarbor() != null && cp.getHarbor() != null) {
							cr.appendNote(pp.getHarbor().getRenderString() + "→" + cp.getHarbor().getRenderString());
						}
						cr.setModification(ObjectModificationType.Gained);
					} else {
						cr.setNotes("Failed sab docks");
						cr.setModification(ObjectModificationType.Lost);
					}
				}
				if (orderResults.contains("was ordered to sabotage a bridge")) {
					boolean failure = orderResults.contains("was not able to sabotage the bridge");
					CharacterReport cr = new CharacterReport(c);
					if (pc != null) {
						cr.setHexNo(pc.getHexNo());
					}
					ret.add(cr);
					if (!failure) {
						String hex = StringUtils.getUniquePart(orderResults, "was sabotaged at ", "\\.", false, false);
						cr.setNotes("Sab Bridge at " + hex);
						cr.setModification(ObjectModificationType.Gained);
					} else {
						cr.setNotes("Failed sab bridge");
						cr.setModification(ObjectModificationType.Lost);
					}
				}
				 
			}
		}
		return ret;
	}
	
	public ArrayList collectChallenges(Turn t, Turn p) {
		Nation gameNation = NationMap.getNationFromNo(GameHolder.instance().getGame().getMetadata().getNationNo());
		NationAllegianceEnum gameNationAllegiance = gameNation.getAllegiance();
		ArrayList ret = new ArrayList();
		for (Challenge challenge : (ArrayList<Challenge>)t.getContainer(TurnElementsEnum.Challenge).getItems()) {
			ChallengeReport cr = new ChallengeReport(challenge);
			String victor = challenge.getVictor();
			String loser = challenge.getLoser();
			AdvancedCharacterWrapper v = CharacterInfoCollector.instance().getCharacterForTurn(victor, t.getTurnNo());
			AdvancedCharacterWrapper l = CharacterInfoCollector.instance().getCharacterForTurn(loser, t.getTurnNo()-1);
			if (l == null) l = CharacterInfoCollector.instance().getCharacterForTurn(loser, t.getTurnNo());
			String vn = "";
			String ln = "";
			ObjectModificationType mod = ObjectModificationType.Modified;
			
			if (l != null && l.getNationNo() != null && l.getNationNo() > 0) {
				Nation n = NationMap.getNationFromNo(l.getNationNo());
				if (n.getAllegiance().equals(gameNationAllegiance)) mod = ObjectModificationType.Lost;
				cr.addNation(n.getNumber());
				ln = "(" + n.getShortName() + ")";
			}
			if (v != null && v.getNationNo() != null && v.getNationNo() > 0) { 
				Nation n = NationMap.getNationFromNo(v.getNationNo());
				if (n.getAllegiance().equals(gameNationAllegiance)) mod = ObjectModificationType.Gained;
				cr.addNation(n.getNumber());
				vn = "(" + n.getShortName() + ")";
			} 
			
			cr.setModification(mod);
			cr.setName(victor + " vs " + loser);
			cr.appendNote(victor + vn + " won, " + loser + ln + " died");
			ret.add(cr);
		}
		return ret;
	}
	
	public ArrayList CollectCharacters(Turn t, Turn p) {
		ArrayList ret = new ArrayList();
		for (PlayerInfo pi : (ArrayList<PlayerInfo>)t.getContainer(TurnElementsEnum.PlayerInfo).getItems()) {
			int nationNo = pi.getNationNo();
			for (Character c : (ArrayList<Character>)t.getContainer(TurnElementsEnum.Character).getItems()) {
				Character pc = (Character)p.getContainer(TurnElementsEnum.Character).findFirstByProperty("name", c.getName());
				if (!c.getNationNo().equals(nationNo)) continue;
				if (!c.getDeathReason().equals(CharacterDeathReasonEnum.NotDead)) {
					// lost
					CharacterReport cr = new CharacterReport(c);
					if (c.getDeathReason().equals(CharacterDeathReasonEnum.Dead)) {
						if (c.getCleanOrderResults().contains("was killed during combat")) {
							cr.setNotes("Killed in combat");
						} else {
							cr.setNotes(c.getDeathReason().toString());
						}
					} else {
						cr.setNotes(c.getDeathReason().toString());
						if (c.getDeathReason().equals(CharacterDeathReasonEnum.Assassinated)) {
							for (NationMessage nm : t.getNationMessages()) {
								if (nm.isAssassinationRumor() && c.getName().equals(nm.getAssassinationTarget())) {
									cr.appendNote("by " + getAdvCharWrapperStr(nm.getAssassinationAttacker(),t) + " (rumor)");
									break;
								}
							}
						} else if (c.getDeathReason().equals(CharacterDeathReasonEnum.Challenged)) {
							Challenge challenge = t.findChallenge(c.getName());
							if (challenge != null) {
								cr.appendNote("by " + getAdvCharWrapperStr(challenge.getVictor(),t));
							}
						}
						
					}
					if (pc != null) cr.setCharacter(pc);
					cr.setModification(ObjectModificationType.Lost);
					ret.add(cr);
					if (c.getCleanOrderResults().contains("The army commanded by " + c.getName() + " has been disbanded because no suitable commander was present.")) {
						cr.appendNote("Army disbanded");
					}
				} else {
					if (pc == null) {
						// new
						CharacterReport cr = new CharacterReport(c);
						cr.setModification(ObjectModificationType.Gained);
						ret.add(cr);
					} else {
						// check for hostage
						String cleanOrderResults = c.getCleanOrderResults(); 
						
						if (c.getHexNo() == 0 && pc.getHexNo() != 0) {
							// hostage or missing
							if (cleanOrderResults.contains("could not escape from being held hostage")) {
								CharacterReport cr = new CharacterReport(c);
								cr.setHexNo(pc.getHexNo());
								cr.setModification(ObjectModificationType.Lost);
								cr.setNotes("Hostage");
								if (cleanOrderResults.contains(c.getName() + " was captured during combat by")) {
									cr.appendNote("Captured in combat");
								} else if (cleanOrderResults.contains(c.getName() + " was kidnaped.")) {
									cr.appendNote("Kidnapped");
									for (NationMessage nm : t.getNationMessages()) {
										if (nm.isKidnapRumor() && c.getName().equals(nm.getKidnapTarget())) {
											cr.appendNote("by " + getAdvCharWrapperStr(nm.getKidnapAttacker(),t) + " (rumor)");
										}
									}
								}
								ret.add(cr);
							}
						} else if (c.getHexNo() != 0 && pc.getHexNo() == 0) {
							CharacterReport cr = new CharacterReport(c);
							cr.setModification(ObjectModificationType.Gained);
							cr.setNotes("Escaped from hostage");
							ret.add(cr);
						} else if (c.getHealth() != null && c.getHealth() < 100 && c.getHealth() > 0 && pc.getHealth() > c.getHealth()) {
							CharacterReport cr = new CharacterReport(c);
							cr.setModification(ObjectModificationType.Modified);
							cr.setNotes("Injured (" + c.getHealth() + ")");
							if (cleanOrderResults.contains("suffered a loss of health due to a mysterious")) {
								cr.appendNote(" Cursed");
							} 
							if (cleanOrderResults.contains("was wounded during combat")) {
								cr.appendNote(" Wounded in combat");
							} 
							if (cleanOrderResults.contains("was injured by")) {
								String injuredBy = StringUtils.getUniquePart(cleanOrderResults, "was injured by ", " while performing ", false, false);
								String mission = StringUtils.getUniquePart(cleanOrderResults, " while performing ", " mission\\.", false, false);
								if (injuredBy != null && mission != null) {
									cr.appendNote(" Injured by " + getAdvCharWrapperStr(injuredBy, t));
								}
							}
							if (c.getMageTotal() > 0 && cleanOrderResults.contains(" suffered a loss of health due to casting two spells.")) {
								cr.appendNote(" Cast two spells");
							}
							if (t.findChallenge(c.getName()) != null) {
								cr.appendNote( " Wounded in challenge");
							}
							ret.add(cr);
						}
					}
				}
			}
		}
		return ret;
	}
	
	public ArrayList CollectPopCenters(Turn t, Turn p) {
		ArrayList ret = new ArrayList();
		for (PlayerInfo pi : (ArrayList<PlayerInfo>)t.getContainer(TurnElementsEnum.PlayerInfo).getItems()) {
			int nationNo = pi.getNationNo();
			for (PopulationCenter pc : (ArrayList<PopulationCenter>)t.getContainer(TurnElementsEnum.PopulationCenter).findAllByProperty("nationNo", nationNo)) {
				if (pc.getNationNo() != nationNo) continue;
				PopulationCenter ppc = (PopulationCenter)p.getContainer(TurnElementsEnum.PopulationCenter).findFirstByProperty("hexNo", pc.getHexNo());
				// if previous pop was ruins, simulate that it did not exist
				if (pc.getSize().equals(PopulationCenterSizeEnum.ruins)) continue;
				if (ppc != null && ppc.getSize().equals(PopulationCenterSizeEnum.ruins)) ppc = null;
				if (ppc == null) {
					PopCenterReport pr = new PopCenterReport(pc);
					pr.setModification(ObjectModificationType.Gained);
					pr.setCreated(true);
					if (pc.getLoyalty() < 16 && pc.getFortification().equals(FortificationSizeEnum.none)) {
						pr.appendNote("Loyalty: " + pc.getLoyalty());
					}
					ret.add(pr);
				}
				else {
					PopCenterReport pr = GetPopCenterReport(pc, ppc, t, p, nationNo);
					if (pr != null) ret.add(pr);
				}
			}
			for (PopulationCenter ppc : (ArrayList<PopulationCenter>)p.getContainer(TurnElementsEnum.PopulationCenter).findAllByProperty("nationNo", nationNo)) {
				if (ppc.getNationNo() != nationNo) continue;
				PopulationCenter pc = (PopulationCenter)t.getContainer(TurnElementsEnum.PopulationCenter).findFirstByProperty("hexNo", ppc.getHexNo());
				// if previous pop was ruins, simulate that it did not exist
				if (pc == null || pc.getSize().equals(PopulationCenterSizeEnum.ruins)) {
					PopCenterReport pr = new PopCenterReport(ppc);
					pr.setPc(pc);
					pr.setPrevPc(ppc);
					pr.setModification(ObjectModificationType.Lost);
					if (ppc.getCapital()) pr.appendNote("Capital");
					String destroyed = "Lost";
					
					Combat combat = (Combat)t.getContainer(TurnElementsEnum.Combat).findFirstByProperty("hexNo", ppc.getHexNo());
					if (combat != null) {
						CombatWrapper cw = new CombatWrapper();
						cw.parseAll(combat.getFirstNarration());
						if (ppc.getName().equals(cw.getPopName())) {
							if ("destroyed".equals(cw.getPopCenterOutcome())) {
								if (ppc.getNation() != null) {
									destroyed = "Destroyed by ";
									String destrNations = "";
									for (Nation n : cw.getNations(ppc.getNation().getAllegiance())) {
										destrNations += (destrNations.equals("") ? "" : ",") + n.getShortName();
									}
									destroyed += destrNations;
								} else {
									destroyed = "Destroyed";
								}
							}
						}
					}
					pr.appendNote(destroyed);
					
					ret.add(pr);
				} else {
					if (pc.getNationNo() != nationNo) {
						PopCenterReport pr = GetPopCenterReport(pc, ppc, t, p, nationNo);
						if (pr != null) ret.add(pr);
					}
				}
			}
		}
		return ret;
	}
	
	protected PopCenterReport GetPopCenterReport(PopulationCenter pc, PopulationCenter ppc, Turn turn, Turn previous, int nationNo) {
		// check for modifications
		boolean sizeChanged = !pc.getSize().equals(ppc.getSize());
		boolean fortChanged = !pc.getFortification().equals(ppc.getFortification());
		boolean nationChanged = !pc.getNationNo().equals(ppc.getNationNo());
		boolean portChanged = !pc.getHarbor().equals(ppc.getHarbor());
		boolean hiddenChanged = pc.getHidden() != ppc.getHidden();
		boolean captured = !pc.getNation().getAllegiance().equals(ppc.getNation().getAllegiance()) || (nationChanged && (pc.getNationNo() == 0 || ppc.getNationNo() == 0));
		boolean capturedByEnemy = pc.getNationNo() != nationNo;
		boolean loyaltyReduced = pc.getLoyalty() < ppc.getLoyalty();
		if (sizeChanged || fortChanged || nationChanged || hiddenChanged || captured) {
			PopCenterReport pr = new PopCenterReport(pc);
			pr.setPrevPc(ppc);
			pr.setNationNo(nationNo);
			pr.addNation(pc.getNationNo());
			if (captured) {
				if (capturedByEnemy) {
					pr.setModification(ObjectModificationType.Lost);
				} else {
					pr.setModification(ObjectModificationType.Gained);
				}
			} else {
				pr.setModification(ObjectModificationType.Modified);
			}
			if (pc.getCapital()) pr.appendNote("Capital");
			if (nationChanged) {
				if (!sizeChanged) {
					for (Character pch : previous.getCharactersAtHex(pc.getHexNo())) {
						Character c = (Character)turn.getCharByName(pch.getName());
						if (c == null) continue;
						String cleanOrderResults = c.getCleanOrderResults(); 
						if (cleanOrderResults.contains(pc.getName() + " is now under our control")) {
							if (cleanOrderResults.contains("to threaten the population center")) {
								pr.appendNote(" Threatened");
								break;
							}
							if (cleanOrderResults.contains("The loyalty was influenced/reduced at " + pc.getName())) {
								pr.appendNote(" InfOthered");
								break;
							}
						} 
					} 
					for (Character c : turn.getCharactersAtHex(pc.getHexNo())) {
						String cleanOrderResults = c.getCleanOrderResults();
						if (c.getNationNo().equals(ppc.getNationNo()) && cleanOrderResults.contains(" was ordered to transfer the ownership of the population center.")) {// " + pc.getName() + " is no longer under our control.")) {
							pr.appendNote(" Transferred");
							break;
						}
					}
					for (NationMessage nm : turn.getNationMessages(ppc.getNationNo())) {
						if (nm.isInfOtherRumor() && nm.getInfoOtherPop().equals(pc.getName())) {
							pr.appendNote(" InfOthered");
							break;
						}
					}
				} else {
					Combat combat = (Combat)turn.getContainer(TurnElementsEnum.Combat).findFirstByProperty("hexNo", ppc.getHexNo());
					if (combat != null) {
						CombatWrapper cw = new CombatWrapper();
						cw.parseAll(combat.getFirstNarration());
						if (cw.getPopName().equals(ppc.getName())) {
							if ("captured".equals(cw.getPopCenterOutcome())) {
								pr.appendNote(" Captured");
							}
						}
					}
					
				}
				pr.appendNote(ppc.getNation().getShortName() + "→" + pc.getNation().getShortName());
				pr.addNation(ppc.getNationNo());
				if (sizeChanged) {
					String pcDescr = shortenPopSize(pc.getSize().getRenderString());
					if (!pc.getFortification().equals(FortificationSizeEnum.none)) pcDescr += "/" + shortenPopFort(pc.getFortification().getRenderString());
					if (pc.getHarbor() != null && !pc.getHarbor().equals(HarborSizeEnum.none)) pcDescr += "/" + shortenPopDocks(pc.getHarbor().getRenderString());
					
					String ppcDescr = shortenPopSize(ppc.getSize().getRenderString());
					if (!ppc.getFortification().equals(FortificationSizeEnum.none)) ppcDescr += "/" + shortenPopFort(ppc.getFortification().getRenderString());
					if (ppc.getHarbor() != null && !ppc.getHarbor().equals(HarborSizeEnum.none)) ppcDescr += "/" + shortenPopDocks(ppc.getHarbor().getRenderString());
					pr.appendNote(ppcDescr + "→" + pcDescr);
				}
			
			}
			
			if (hiddenChanged) pr.appendNote(pc.getHidden() ? "Hidden" : "Revealed");
			if (sizeChanged && !captured) {
				if (ppc.getSize().getCode() < pc.getSize().getCode()) {
					// check for improve
					for (Character pch : previous.getCharactersAtHex(pc.getHexNo())) {
						Character c = turn.getCharByName(pch.getName());
						if (c == null) continue;
						if (c.getNationNo().equals(pc.getNationNo())) {
							if (c.getCleanOrderResults().contains("was ordered to improve the population center size. " + pc.getName() + " was improved ")) {
								pr.appendNote("Improved");
							}
						}
					}
				} 
				pr.appendNote(shortenPopSize(ppc.getSize().getRenderString()) + "→" + shortenPopSize(pc.getSize().getRenderString()));
			}
			if (fortChanged) {
				if (!captured) {
					if (ppc.getFortification().getSize() < pc.getFortification().getSize()) {
						// check for improve
						for (Character pch : previous.getCharactersAtHex(pc.getHexNo())) {
							Character c = turn.getCharByName(pch.getName());
							if (c == null) continue;
							if (c.getNationNo() == pc.getNationNo()) {
								if (c.getCleanOrderResults().contains("was ordered to fortify the population center. The fortifications at " + pc.getName() + " were improved to a ")) {
									pr.appendNote("Fortified");
								}
							}
						}
					} else {
						for (Character pch : previous.getCharactersAtHex(pc.getHexNo())) {
							Character c = turn.getCharByName(pch.getName());
							if (c == null) continue;
							if (c.getNationNo() == pc.getNationNo()) {
								if (c.getCleanOrderResults().contains(" was ordered to remove the fortifications. The fortifications were ")) {
									pr.appendNote("RemFort");
								}
							}
						}
					}
					pr.appendNote(shortenPopFort(ppc.getFortification().getRenderString()) + "→" + shortenPopFort(pc.getFortification().getRenderString()));
				} 
			}
			if (portChanged && !captured) pr.appendNote(shortenPopDocks(ppc.getHarbor().getRenderString()) + "→" + shortenPopDocks(pc.getHarbor().getRenderString()));
			
			return pr;
		}
		if (pc.getLoyalty() < ppc.getLoyalty()) {
			for (NationMessage nm : turn.getNationMessages(pc.getNationNo())) {
				if (nm.isInfOtherRumor() && nm.getInfoOtherPop().equals(pc.getName())) {
					PopCenterReport pr = new PopCenterReport(pc);
					pr.setModification(ObjectModificationType.Modified);
					pr.setNotes("InfOthered, Loyalty: " + ppc.getLoyalty() + "→" + pc.getLoyalty());
					return pr;
				}
			}
		}
		if (pc.getLoyalty() < 16) {
			PopCenterReport pr = new PopCenterReport(pc);
			pr.setModification(ObjectModificationType.Modified);
			if (ppc != null) {
				pr.setNotes("Loyalty: " + ppc.getLoyalty() + "→" + pc.getLoyalty() + " (at risk)");
			} else {
				pr.setNotes("Loyalty: " + pc.getLoyalty() + " (at risk)");
			}
			return pr;
		} 
		return null;
	}
	
	public ArrayList collectEncounters(Turn t) {
		ArrayList ret = new ArrayList();
		for (Encounter e : (ArrayList<Encounter>)t.getContainer(TurnElementsEnum.Encounter).getItems()) {
			Character c = t.getCharByName(e.getCharacter());
			EncounterReport er = new EncounterReport();
			er.setHexNo(e.getHexNo());
			er.setEncounter(e);
			if (c == null) {
				er.setName(e.getCharacter());
			} else {
				er.setName(c.getName());
				er.setNationNo(c.getNationNo());
			}
			if (e.isReacting()) 
			{
				er.setNotes("Reacting");
				if (e.isDragon()) 
				{
					String dragonName = e.getDragonName();
					if (dragonName != null) {
						er.appendNote("Dragon " + dragonName);
					} else {
						er.appendNote("Dragon");
					}
				}
				er.setModification(ObjectModificationType.Gained);
			} else {
				er.setNotes("Reacted");
				er.setModification(ObjectModificationType.Modified);
			}
			ret.add(er);
		}
		for (NationMessage nm : (ArrayList<NationMessage>)t.getContainer(TurnElementsEnum.NationMessage).getItems()) {
			if (nm.isEncounterRumor()) {
				EncounterReport er = new EncounterReport();
				String name = nm.getEncounterCharacter();
				int hexNo = nm.getEncounterHexNo();
				if (t.getEncounter(hexNo, name) != null) continue;
				er.setHexNo(hexNo);
				er.setName(getAdvCharWrapperStr(name, t));
				er.setModification(ObjectModificationType.Lost);
				er.setNotes("Rumor");
				
				ret.add(er);
			}
			
		}
		return ret;
	}
	
	public ArrayList CollectCompanies(Turn t, Turn p) {
		ArrayList ret = new ArrayList();
		for (Company c : (ArrayList<Company>)t.getContainer(TurnElementsEnum.Company).getItems()) {
			Company pc = (Company)p.getContainer(TurnElementsEnum.Company).findFirstByProperty("commander", c.getCommander());
			if (pc == null) {
				CompanyReport cr = new CompanyReport();
				cr.setHexNo(c.getHexNo());
				cr.setName(c.getCommander());
				cr.setModification(ObjectModificationType.Gained);
				cr.appendNote("Members: " + c.getMemberStr());
				addNation(cr, c.getCommander(), t);
				for (String m : c.getMembers()) addNation(cr, m, t);
				ret.add(cr);
			} else {
				String leftStr = "";
				String joinedStr = "";
				CompanyReport cr = new CompanyReport();
				cr.setHexNo(c.getHexNo());
				cr.setName(c.getCommander());
				cr.setModification(ObjectModificationType.Modified);
				addNation(cr, c.getCommander(), t);
				boolean modified = false;
				for (String cm : c.getMembers()) {
					if (!pc.getMembers().contains(cm)) {
						joinedStr += (joinedStr.equals("") ? "" : ",") + cm;
						modified = true;
						addNation(cr, cm, t);
					}
				}
				for (String cm : pc.getMembers()) {
					if (!c.getMembers().contains(cm)) {
						leftStr += (leftStr.equals("") ? "" : ",") + cm;
						modified = true;
						addNation(cr, cm, p);
					}
				}
				if (modified) {
					if (!joinedStr.equals("")) cr.appendNote("Joined: " + joinedStr);
					if (!leftStr.equals("")) cr.appendNote("Left: " + leftStr);
					ret.add(cr);
				}
			}
		}
		for (Company pc : (ArrayList<Company>)p.getContainer(TurnElementsEnum.Company).getItems()) {
			Company c = (Company)t.getContainer(TurnElementsEnum.Company).findFirstByProperty("commander", pc.getCommander());
			if (c == null) {
				CompanyReport cr = new CompanyReport();
				cr.setHexNo(pc.getHexNo());
				cr.setName(pc.getCommander());
				cr.setModification(ObjectModificationType.Lost);
				addNation(cr, pc.getCommander(), p);
				cr.setNotes("Members: " + pc.getMemberStr());
				for (String m : pc.getMembers()) addNation(cr, m, p);
				ret.add(cr);
			}
		}
		return ret;
	}
	
	protected void addNation(BaseReportObject ro, String characterName, Turn t) {
		Character c = (Character)t.getContainer(TurnElementsEnum.Character).findFirstByProperty("name", characterName);
		if (c != null) ro.addNation(c.getNationNo());
	}
	
	public ArrayList<ArtifactReport> CollectArtifacts(Turn t, Turn p) {
		ArrayList ret = new ArrayList();
		Container taws = ArtifactInfoCollector.instance().computeWrappersForTurn(t.getTurnNo());
		Container paws = ArtifactInfoCollector.instance().computeWrappersForTurn(p.getTurnNo());
		
		for (ArtifactWrapper aw : (ArrayList<ArtifactWrapper>)taws.getItems()) {
			if (aw.getTurnNo() != t.getTurnNo()) continue;
			if (DerivedFromSpellInfoSource.class.isInstance(aw.getInfoSource())) {
				DerivedFromSpellInfoSource s = (DerivedFromSpellInfoSource)aw.getInfoSource();
				ArtifactReport ar = new ArtifactReport();
				ar.setName(aw.getName() + " (" + aw.getNumber() + ")");
				ar.setArtifactNo(aw.getNumber());
				ar.setNationNo(s.getNationNo());
				String owner = aw.getOwner();
				AdvancedCharacterWrapper acw = CharacterInfoCollector.instance().getCharacterForTurn(owner, t.getTurnNo());
				if (acw != null && acw.getNationNo() != null) {
					ar.setNationNo(acw.getNationNo());
				}
				if (owner != null && !owner.equals("")) ar.setNotes("Owner: " + owner);
				ar.appendNote(aw.getInfoSource().getDescription());
				ar.setHexNo(aw.getHexNo());
				ar.setModification(ObjectModificationType.Modified);
				ret.add(ar);
				continue;
			}
			ArtifactWrapper paw = (ArtifactWrapper)paws.findFirstByProperty("number", aw.getNumber());
			if (paw != null && paw.getTurnNo() != p.getTurnNo()) paw = null;
			if (paw == null) {
				ArtifactReport ar = new ArtifactReport();
				ar.setName(aw.getName() + " (" + aw.getNumber() + ")");
				ar.setArtifactNo(aw.getNumber());
				ar.setNotes(aw.getOwner());
				ar.setNationNo(aw.getNationNo());
				ar.setHexNo(aw.getHexNo());
				ar.setModification(ObjectModificationType.Gained);
				ret.add(ar);
			} else {
				if (paw.getNationNo() != null && !paw.getNationNo().equals(aw.getNationNo())) {
					ArtifactReport ar = new ArtifactReport();
					ar.setArtifactNo(aw.getNumber());
					ar.setName(aw.getName() + " (" + aw.getNumber() + ")");
					ar.setNotes(aw.getOwner());
					ar.setNationNo(aw.getNationNo());
					ar.setHexNo(aw.getHexNo());
					ar.setModification(ObjectModificationType.Modified);
					ar.setNotes(charPlusNation(paw.getOwner(),t) + " → " + charPlusNation(aw.getOwner(),t));
					ret.add(ar);
				}
			}
		}
		for (ArtifactWrapper paw : (ArrayList<ArtifactWrapper>)paws.getItems()) {
			if (paw.getTurnNo() != p.getTurnNo()) continue;
			if (DerivedFromSpellInfoSource.class.isInstance(paw.getInfoSource())) continue;
			ArtifactWrapper aw = (ArtifactWrapper)taws.findFirstByProperty("number", paw.getNumber());
			if (aw != null && aw.getTurnNo() != t.getTurnNo()) aw = null;
			if (paw.getNationNo() == null) continue;
			if (aw == null) {
				ArtifactReport ar = new ArtifactReport();
				ar.setName(paw.getName() + " (" + paw.getNumber() + ")");
				ar.setArtifactNo(paw.getNumber());
				ar.setNotes(paw.getOwner());
				ar.setNationNo(paw.getNationNo());
				ar.setHexNo(paw.getHexNo());
				ar.setModification(ObjectModificationType.Lost);
				ret.add(ar);
			}
		}
		return ret;
	}
	
	protected String getArtifactOwner(Nation n, Character c, Artifact a) {
		if (c != null) return charPlusNation(c);
		if (n != null) n.getName();
		return a.getOwner();
	}
	
	public ArrayList CollectTransports(Turn t, Turn p) {
		Nation gameNation = NationMap.getNationFromNo(GameHolder.instance().getGame().getMetadata().getNationNo());
		NationAllegianceEnum gameNationAllegiance = gameNation.getAllegiance();

		ArrayList ret = new ArrayList();
		for (NationMessage nm : (ArrayList<NationMessage>)t.getContainer(TurnElementsEnum.NationMessage).getItems()) {
			if (nm.isFriendlyTransportMessage()) {
				TransportReport tr = new TransportReport();
				String msg = nm.getMessage();
				tr.addNation(nm.getFriendlyTrasnportFromNation());
				PopulationCenter pc = t.getPopCenter(nm.getFriendlyTransportDestPop());
				if (pc != null) {
					tr.addNation(pc.getNationNo());
					tr.setHexNo(pc.getHexNo());
					msg = msg.replace(pc.getName(), popPlusNation(pc));
				}
				tr.setNotes(msg);
				tr.setModification(ObjectModificationType.Gained);
				ret.add(tr);
			} else if (nm.isEnemyTransportMessage()) {
				TransportReport tr = new TransportReport();
				String msg = nm.getMessage(); 
				String fromPop = nm.getEnemyTransportOriginPop();
				String toPop = nm.getEnemyTransportDestPop();
				
				PopulationCenter fp = t.getPopCenter(fromPop);
				PopulationCenter tp = t.getPopCenter(toPop);
				if (fp != null) {
					tr.addNation(fp.getNationNo());
					msg = msg.replace(fromPop, popPlusNation(fp));
				}
				if (tp != null) {
					tr.addNation(tp.getNationNo());
					msg = msg.replace(toPop, popPlusNation(tp));
					tr.setHexNo(tp.getHexNo());
				}
				if (fp != null && tp != null && fp.getNation().getAllegiance().equals(gameNationAllegiance) && tp.getNation().getAllegiance().equals(gameNationAllegiance)) {
					tr.setModification(ObjectModificationType.Gained);
				} else {
					tr.setModification(ObjectModificationType.Lost);
				}
				tr.setNotes(msg);
				
				ret.add(tr);
			}
		}
		return ret;
	}
	public ArrayList CollectGoldSteals(Turn t, Turn p) {
		ArrayList ret = new ArrayList();
		for (NationMessage nm : (ArrayList<NationMessage>)t.getContainer(TurnElementsEnum.NationMessage).getItems()) {
			if (nm.isStealRumor()) {
				StealGoldReport r = new StealGoldReport();
				r.addNation(nm.getNationNo());
				r.setModification(ObjectModificationType.Lost);
				r.setGold(nm.getStealAmount());
				r.setStolenFromNation(nm.getNationNo());
				r.setGainedByNation(-1);
				r.setNotes(r.getGold() + " gold");
				r.setHexNo(nm.getX() * 100 + nm.getY());
				ret.add(r);
			}
		}
		for (Character c : (ArrayList<Character>)t.getContainer(TurnElementsEnum.Character).getItems()) {
			String orderResults = c.getCleanOrderResults();
			int i = orderResults.indexOf(" Gold was stolen at ");
			if (i > -1) {
				int k = orderResults.indexOf(".", i);
				int j = i;
				while (java.lang.Character.isDigit(orderResults.charAt(j-1))) {
					j--;
				}
				String amountStr = orderResults.substring(j, i);
				StealGoldReport sgr = new StealGoldReport();
				Character pc = (Character)p.getContainer(TurnElementsEnum.Character).findFirstByProperty("name", c.getName());
				if (pc != null) {
					sgr.setHexNo(pc.getHexNo());
					PopulationCenter ppc = (PopulationCenter)p.getContainer(TurnElementsEnum.PopulationCenter).findFirstByProperty("hexNo", pc.getHexNo());
					if (ppc != null) {
						sgr.setNotes(amountStr + " gold from " + ppc.getNation().getShortName() + " by " + c.getName());
						sgr.setStolenFromNation(ppc.getNationNo());
					} else {
						sgr.setStolenFromNation(-1);
					}
				}
				try {
					sgr.setGold(Integer.parseInt(amountStr));
				}
				catch (Exception e) {
					sgr.setGold(0);
				}
				sgr.setNationNo(c.getNationNo());
				sgr.setGainedByNation(c.getNationNo());
				sgr.setModification(ObjectModificationType.Gained);
				ret.add(sgr);
			}
		}
		return ret;
	}
	
	public ArrayList CollectNations(Turn t, Turn p) {
		ArrayList ret = new ArrayList();
		for (PlayerInfo pi : (ArrayList<PlayerInfo>)t.getContainer(TurnElementsEnum.PlayerInfo).getItems()) {
			NationEconomy e = (NationEconomy)t.getContainer(TurnElementsEnum.NationEconomy).findFirstByProperty("nationNo", pi.getNationNo());
			NationEconomy pe = (NationEconomy)p.getContainer(TurnElementsEnum.NationEconomy).findFirstByProperty("nationNo", pi.getNationNo());
			if (pe == null) continue;
			if (e.getTaxRate() != pe.getTaxRate()) {
				NationReport er = new NationReport();
				er.setNationNo(e.getNationNo());
				er.setNotes("Tax rate: " + pe.getTaxRate() + "→" + e.getTaxRate());
				er.setModification(ObjectModificationType.Modified);
				ret.add(er);
			}
			
			NationRelations nr = (NationRelations)t.getContainer(TurnElementsEnum.NationRelation).findFirstByProperty("nationNo", pi.getNationNo());
			NationRelations pnr = (NationRelations)p.getContainer(TurnElementsEnum.NationRelation).findFirstByProperty("nationNo", pi.getNationNo());
			if (pnr == null) continue;
			if (nr.getEliminated() && !pnr.getEliminated()) {
				NationReport er = new NationReport();
				er.setNationNo(nr.getNationNo());
				er.setNotes("Eliminated");
				er.setModification(ObjectModificationType.Lost);
				ret.add(er);
				continue;
			}
			if (!nr.getAllegiance().equals(pnr.getAllegiance())) {
				NationReport er = new NationReport();
				er.setNationNo(nr.getNationNo());
				er.setNotes("Allegiance: " + nr.getAllegiance());
				er.setModification(ObjectModificationType.Modified);
				ret.add(er);
			}
			for (int i=1; i<26; i++) {
				if (nr.getRelationsFor(i) != pnr.getRelationsFor(i)) {
					NationReport er = new NationReport();
					er.setNationNo(nr.getNationNo());
					er.setNotes(nr.getRelationsFor(i) + " to " + NationMap.getNationFromNo(i).getName());
					er.setModification(ObjectModificationType.Modified);
					ret.add(er);
				}
			}
			PopulationCenter capital = t.getCapital(pi.getNationNo());
			PopulationCenter pcapital = p.getCapital(pi.getNationNo());
			if (capital != null && pcapital != null && capital.getHexNo() != pcapital.getHexNo()) {
				NationReport er = new NationReport();
				er.setNationNo(nr.getNationNo());
				er.setNotes("Capital moved to " + capital.getHexNo() + " (" + capital.getName() + ")");
				er.setModification(ObjectModificationType.Modified);
				ret.add(er);
			}
		}
		return ret;
	}
	
	public ArrayList collectUpcomingCombats(Turn t) {
		ArrayList ret = new ArrayList();
		Nation gameNation = NationMap.getNationFromNo(GameHolder.instance().getGame().getMetadata().getNationNo());
		NationAllegianceEnum gameNationAllegiance = gameNation.getAllegiance();
		for (Object ho : GameHolder.instance().getGame().getMetadata().getHexes()) {
			Hex h = (Hex)ho;
			ArrayList<Army> armies = t.getArmies(h.getHexNo());
			if (armies.size() == 0) continue;
			PopulationCenter pc = t.getPopCenter(h.getHexNo());
			if (pc != null && pc.getSize() == PopulationCenterSizeEnum.ruins) pc = null;
				
			boolean hasFP = false;
			boolean hasDS = false;
			boolean hasNeutral = false;
			boolean hasFriendlyArmy = false;
			boolean hasNonFriendlyArmy = false;
			boolean landArmies = false;
			boolean navies = false;
			String fps = "";
			String ds = "";
			String neuts = "";
			BaseReportObject bro = new BaseReportObject();
			for (Army a : armies) {
				if (a.isAnchoredShips(a)) continue;
				if (a.isNavy()) {
					navies = true;
				} else 
				{
					landArmies = true;
				}
				if (a.getNationNo() != 0 && a.getNationAllegiance().equals(gameNationAllegiance)) 
				{
					bro.addNation(a.getNationNo());
					hasFriendlyArmy = true;
				} else {
					hasNonFriendlyArmy = true;
				}
				if (a.getNationAllegiance().equals(NationAllegianceEnum.FreePeople)) {
					hasFP = true;
					fps += (fps.equals("") ? "" : ", ") + getArmyDescription(a, t);
				}
				if (a.getNationAllegiance().equals(NationAllegianceEnum.DarkServants)) {
					hasDS = true;
					ds += (ds.equals("") ? "" : ", ") + getArmyDescription(a, t);
				}
				if (a.getNationAllegiance().equals(NationAllegianceEnum.Neutral)) {
					hasNeutral = true;
					neuts += (neuts.equals("") ? "" : ", ") + getArmyDescription(a, t);
				}
			}
			if (pc != null && !pc.getNationNo().equals(0)) {
				NationAllegianceEnum pcalleg = pc.getNation().getAllegiance();
				if (pcalleg.equals(gameNationAllegiance) && pc.getNationNo() != 0) bro.addNation(pc.getNationNo());
				if (pcalleg.equals(NationAllegianceEnum.FreePeople)) {
					hasFP = true;
					if (fps.equals("")) fps = "PC";
				}
				if (pcalleg.equals(NationAllegianceEnum.DarkServants)) {
					hasDS = true;
					if (ds.equals("")) ds = "PC";
				}
				if (pcalleg.equals(NationAllegianceEnum.Neutral)) {
					hasNeutral = true;
					if (neuts.equals("")) neuts = "PC";
				}
			}
			if (!(hasFP && hasDS || hasFP && hasNeutral || hasDS && hasNeutral)) continue;
			
			bro.setHexNo(h.getHexNo());
			String name = "";
			if (hasFP && hasDS) {
				name = fps + " vs " + ds;
				if (hasNeutral) {
					name += " neutrals: " + neuts;
				}
			} else if (hasNeutral) {
				if (hasFP) {
					name = fps + " vs " + neuts;
				} else {
					name = ds + " vs " + neuts;
				}
			} else { continue; }
			boolean naval = !landArmies && navies; 
			if (naval) name = "Naval - " + name;
			bro.setName(name);
			if (hasFriendlyArmy) {
				if (hasNonFriendlyArmy) {
					bro.setModification(ObjectModificationType.Modified);
				} else {
					bro.setModification(ObjectModificationType.Gained);
				}
			} else if (hasNonFriendlyArmy) {
				bro.setModification(ObjectModificationType.Lost);
			} else {
				bro.setModification(ObjectModificationType.Modified);
			}
			if (pc != null) {
				bro.appendNote(getPopInfo(pc.getName(), pc.getNation(), pc.getSize().getRenderString(), pc.getFortification().getRenderString()));
			}
			for (Character c : t.getCharactersAtHex(h.getHexNo())) {
				if (InfoUtils.isDragon(c.getName())) {
					boolean foundInArmy = false;
					for (Army a : armies) {
						if (a.getCharacters().contains(c.getName())) {
							bro.appendNote("Dragon " + c.getName() + " w/" + a.getCommanderName());
							foundInArmy = true;
						}
					}
					if (!foundInArmy) bro.appendNote("Dragon " + c.getName());
				}
			}
			ret.add(bro);
		}
		return ret;
	}
	
	protected String getArmyDescription(Army a, Turn t) {
		String ret = a.getCommanderName();
		ret += " (" + a.getNation().getShortName();
		if (a.getElements().size() > 0) {
			ret += " " + a.getENHI() + "EHI";
		} else if (a.getTroopCount() > 0) {
			ret += " " + a.getTroopCount() + "men";
		} else if (!a.getSize().equals(ArmySizeEnum.unknown)) {
			if (a.getSize().equals(ArmySizeEnum.army)) {
				ret += "";
			} else {
				ret += " " + a.getSize() + "";
			}
		}
		if (a.isNavy()) {
			if (a.getElements().size() > 0) {
				ret += " " + a.getNumber(ArmyElementType.Warships) + "WS";
			} else {
				ret += " navy";
			}
		}
		ret += ")";
		return ret;
	}
	
	public ArrayList CollectCombats(Turn t) {
		ArrayList ret = new ArrayList();
		Nation gameNation = NationMap.getNationFromNo(GameHolder.instance().getGame().getMetadata().getNationNo());
		NationAllegianceEnum gameNationAllegiance = gameNation.getAllegiance();
		for (Combat c : (ArrayList<Combat>)t.getContainer(TurnElementsEnum.Combat).getItems()) {
			CombatWrapper cw = new CombatWrapper();
			CombatReport cr = null;
			for (int i=1; i<26; i++) {
				String r = c.getNarrationForNation(i);
				if (r != null) {
					if (cr != null) 
					{
						cr.addNation(i);
						continue;
					}
					try {
						cw.parseAll(r);
						ArrayList<Integer> nations = new ArrayList<Integer>();
						ArrayList<Integer> winners = new ArrayList<Integer>();
						String commanders = "";
						for (CombatArmy ca : (ArrayList<CombatArmy>)cw.getArmies().getItems()) {
							Nation n = NationMap.getNationFromName(ca.getNation());
							int num = n.getNumber();
							if (!nations.contains(num)) nations.add(num);
							if (ca.isSurvived()) {
								if (!winners.contains(num)) winners.add(num);
							}
							if (ca.getCommanderOutcome() != null && (ca.getCommanderOutcome().equals("captured") || ca.getCommanderOutcome().equals("killed"))) {
								commanders += (commanders.equals("") ? "" : ",") + namePlusNation(ca.getCommanderName(),n) + " " + ca.getCommanderOutcome();
							}
						}
						cr = new CombatReport(c);
						String participants = "";
						cr.addNation(i);
						for (Integer num : nations) {
							//cr.appendNote();
							participants += (participants.equals("") ? "" : ",") + NationMap.getNationFromNo(num).getShortName(); 
						}
						cr.setParticipants(participants);
						if (cw.getPopName() != null) {
							Nation n = NationMap.getNationFromName(cw.getPopNation());
							cr.setPopInfo(getPopInfo(cw.getPopName(), n, cw.getPopSize(), cw.getPopFort()));
						}
						HashSet<NationAllegianceEnum> winnerAllegiances = new HashSet<NationAllegianceEnum>();
						if (winners.size() > 0) {
							String winStr = "";
							for (Integer num : winners) {
								Nation n = NationMap.getNationFromNo(num);
								winStr += (winStr.equals("") ? "" : ",") + n.getShortName();
								winnerAllegiances.add(n.getAllegiance());
							}
							//cr.appendNote(" Won by:" + winStr);
							cr.setWinners(winStr);
						} else {
							//cr.appendNote("No winners");
							cr.setWinners("No winners");
						}
						String popOutcome;
						if (cw.getPopCenterOutcome() != null && !cw.getPopCenterOutcome().equals("not affected")) {
							popOutcome =" PC " + cw.getPopCenterOutcome();
							if (cw.getPopCenterOutcome().equals("captured")) {
								Nation nn = NationMap.getNationFromName(cw.getPopOutcomeNation());
								if (nn != null) {
									popOutcome += " by " + nn.getShortName();
								} else {
									popOutcome += " by " + cw.getPopOutcomeNation();
								}
							}
							//cr.appendNote(popOutcome);
							cr.setPopOutcome(popOutcome);
						} else if (cw.getPopName() != null) {
							popOutcome = " PC not affected";
							//cr.appendNote(popOutcome);
							cr.setPopOutcome(popOutcome);
						}
						if (cw.isNaval()) cr.appendNote("Naval combat");
						if (!commanders.equals("")) cr.appendNote(commanders);
						
						if (winnerAllegiances.contains(gameNationAllegiance)) {
							cr.setModification(ObjectModificationType.Gained);
						} else if (winnerAllegiances.size() > 0){
							cr.setModification(ObjectModificationType.Lost);
						} else {
							cr.setModification(ObjectModificationType.Modified);
						}
						ret.add(cr);
					}
					catch (Exception e) {
						int b = 1;
						Logger.getLogger(TurnReportCollector.class).error(e);
						
					}
				}
			}
		}
		return ret;
	}
	
	protected String getPopInfo(String name, Nation n, String size, String fort) {
		String ret = name + " (";
		if (n != null) ret += n.getShortName() + ",";
		ret += shortenPopSize(size);
		if (fort != null) {
			String f = shortenPopFort(fort);
			if (!f.equals("") && !f.equals("None")) ret += "/" + f;
		}
		ret += ")";
		return ret;
	}
	
	public ArrayList collectBridges(Turn t, Turn p) {
		ArrayList ret = new ArrayList();
		GameMetadata gm = GameHolder.instance().getGame().getMetadata();
		ArrayList<Hex> thc = (ArrayList<Hex>)gm.getHexOverrides(t.getTurnNo()).getItems(); 
		ArrayList<Hex> phc = (ArrayList<Hex>)gm.getHexOverrides(t.getTurnNo()).getItems();
		HashSet<Integer> hexes = new HashSet<Integer>();
		for (Hex h : thc) {
			hexes.add(h.getHexNo());
		}
		for (Hex h : phc) {
			hexes.add(h.getHexNo());
		}
		for (Integer hexNo : hexes) {
			Hex ch = gm.getHexForTurn(t.getTurnNo(), hexNo);
			Hex ph = gm.getHexForTurn(p.getTurnNo(), hexNo);
			if (ph == ch) continue;
			for (HexSideEnum hse : HexSideEnum.values()) {
				boolean cb = ch.getHexSideElements(hse).contains(HexSideElementEnum.Bridge);
				boolean pb = ph.getHexSideElements(hse).contains(HexSideElementEnum.Bridge);
				if (cb != pb) {
					int otherHex = hse.getHexNoAtSide(hexNo);
					boolean found = false;
					for (BridgeReport br : (ArrayList<BridgeReport>)ret) {
						if (br.getHexNo() == otherHex) {
							found = true;
							break;
						}
					}
					if (found) continue;
					BridgeReport br = new BridgeReport();
					br.setHexNo(ch.getHexNo());
					if (cb) {
						br.setModification(ObjectModificationType.Gained);
						br.setNotes("Bridge built " + hexNo + "-" + otherHex);
					} else {
						br.setModification(ObjectModificationType.Lost);
						br.setNotes("Bridge destroyed " + hexNo + "-" + otherHex);
					}
					PopulationCenter pc = t.getPopCenter(hexNo);
					if (pc != null) br.appendNote(" PC: " + popPlusNation(pc));
					pc = t.getPopCenter(otherHex);
					if (pc != null) br.appendNote(popPlusNation(pc));
					ret.add(br);
				}
			}
		}
		for (NationMessage nm : (ArrayList<NationMessage>)t.getContainer(TurnElementsEnum.NationMessage).getItems()) 
		{
			if (nm.isBridgeSabotagedRumor()) {
				String location = nm.getBridgeSabotagedLocation();
				int hexNo = -1;
				try {
					hexNo = Integer.parseInt(location);
				}
				catch (Exception exc) {}
				PopulationCenter pc;
				if (hexNo != -1) {
					pc = t.getPopCenter(hexNo);
				} else {
					pc = t.getPopCenter(location);
					hexNo = pc.getHexNo();
				}
				BridgeReport br = new BridgeReport();
				br.setHexNo(hexNo);
				br.setModification(ObjectModificationType.Lost);
				br.setNotes("Sabotage rumor");
				if (pc != null) br.appendNote(" PC: " + pc.getName() + "(" + pc.getNation().getShortName() + ")");
				ret.add(br);
			}
		}
		return ret;
	}
	
	protected String getAdvCharWrapperStr(String name, Turn t) 
	{
		AdvancedCharacterWrapper acw = CharacterInfoCollector.instance().getCharacterForTurn(name, t.getTurnNo());
		if (acw == null) return name;
		Nation n = NationMap.getNationFromNo(acw.getNationNo());
		return namePlusNation(name, n);
	}
	
	protected String popPlusNation(PopulationCenter pc) {
		return namePlusNation(pc.getName(), pc.getNation());
	}
	
	protected String popPlusNation(String name, Turn t) {
		PopulationCenter pc = t.getPopCenter(name);
		if (pc == null) return name;
		return namePlusNation(pc.getName(), pc.getNation());
	}
	
	protected String charPlusNation(Character c) {
		return namePlusNation(c.getName(), c.getNation());
	}
	
	protected String charPlusNation(String name, Turn t) {
		Character c = t.getCharByName(name);
		if (c == null) return name;
		return charPlusNation(c);
	}
	
	protected String namePlusNation(String name, Nation n) {
		if (n == null || n.getNumber() == 0) return name;
		return name + "(" + n.getShortName() + ")";
	}
	
	public String renderCollection(String title, ArrayList reports) {
		return renderCollection(title, null, reports);
	}
	
	public String renderCollection(String title, String comments, ArrayList reports) {
		Collections.sort(reports);
		String ret = "<b style='font-size:11pt'> " + title + "</b>";
		if (comments != null) ret += "<i> - " + comments + "</i><br/>";
		if (reports.size() == 0) return ret + "-" + "<br/>";
		ret += "<table style='border-style:solid; border-width:1px' cellspacing=0 cellpadding=2>";
		for (int i=0; i<reports.size(); i++) {
			BaseReportObject bro = (BaseReportObject)reports.get(i);
			
			String color = "#DDFFDD";
			if (bro.getModification().equals(ObjectModificationType.Lost)) color = "#FFDDDD";
			if (bro.getModification().equals(ObjectModificationType.Modified)) color = "#FFFFDD";
			
			ret += "<tr style='background-color:" + color + ";'>" + bro.getHtmlString() + "</tr>";
		}
		ret += "</table>";
		return ret;
	}
	
	public String renderStealsSummary(ArrayList steals) {
		if (steals.size() == 0) return "";
		int goldGained = 0;
		int goldLost = 0;
		for (StealGoldReport sgr : (ArrayList<StealGoldReport>)steals) {
			if (sgr.getModification().equals(ObjectModificationType.Gained)) {
				goldGained += sgr.getGold();
			} else {
				goldLost += sgr.getGold();
			}
		}
		DecimalFormat df = new DecimalFormat("###,##0");
		return "<div style='font-family:Tahoma; font-size:11pt'><b>" +
				"Total gold gained: " + df.format(goldGained) + "<br/>" + 
				"Total gold lost: " + df.format(goldLost) + "</b></div>";
	}
	
	public String renderCharsSummary(ArrayList chars) {
		if (chars.size() == 0) return "";
		int charsGained = 0;
		int charsLost = 0;
		for (CharacterReport cr : (ArrayList<CharacterReport>)chars) {
			if (cr.getModification().equals(ObjectModificationType.Gained)) {
				charsGained++;
			} else if (cr.getModification().equals(ObjectModificationType.Lost)) {
				charsLost++;
			}
		}
		return "<div style='font-family:Tahoma; font-size:11pt'><b>" +
				"New chars: " + charsGained + "<br/>" + 
				"Lost chars: " + charsLost + "</b></div>";
	}
	
	public String renderPopsSummary(ArrayList pops) {
		if (pops.size() == 0) return "";
		int campsCreated = 0;
		HashMap<PopulationCenterSizeEnum, Integer> gains = new HashMap<PopulationCenterSizeEnum, Integer>();
		HashMap<PopulationCenterSizeEnum, Integer> losses = new HashMap<PopulationCenterSizeEnum, Integer>();
		for (PopulationCenterSizeEnum s : PopulationCenterSizeEnum.values()) {
			gains.put(s, 0);
			losses.put(s, 0);
		}
		int taxBaseDelta = 0;
		for (PopCenterReport pcr : (ArrayList<PopCenterReport>)pops) {
			if (pcr.getModification().equals(ObjectModificationType.Gained) && 
					pcr.getPc() != null && 
					pcr.getPc().getSize().equals(PopulationCenterSizeEnum.camp) &&
					pcr.isCreated()) {
				campsCreated++;
			} 
			if (pcr.getModification().equals(ObjectModificationType.Gained)) {
				int g = gains.get(pcr.getPc().getSize());
				g++;
				gains.put(pcr.getPc().getSize(), g);
				taxBaseDelta += Math.max(pcr.getPc().getSize().getCode()-1, 0);
			} else if (pcr.getModification().equals(ObjectModificationType.Lost)) {
				int l = losses.get(pcr.getPrevPc().getSize());
				l++;
				losses.put(pcr.getPrevPc().getSize(), l);
				taxBaseDelta -= Math.max(pcr.getPrevPc().getSize().getCode()-1, 0);
			} else if (pcr.getModification().equals(ObjectModificationType.Modified) && pcr.getPrevPc() != null) {
				int pcTaxBase = Math.max(pcr.getPc().getSize().getCode() - 1, 0);
				int prevTaxBase = Math.max(pcr.getPrevPc().getSize().getCode() - 1, 0);
				taxBaseDelta += pcTaxBase - prevTaxBase;
			}
			
		}
		String gstr = "";
		String lstr = "";
		for (PopulationCenterSizeEnum s : PopulationCenterSizeEnum.values()) {
			if (s.equals(PopulationCenterSizeEnum.ruins)) continue;
			int g = gains.get(s);
			int l = losses.get(s);
			if (g > 0) gstr += (gstr.equals("") ? "" : ", ") + s.getRenderString() + ": " + g; 
			if (l > 0) lstr += (lstr.equals("") ? "" : ", ") + s.getRenderString() + ": " + l;
		}
		return "<div style='font-family:Tahoma; font-size:11pt'><b>" +
				"Camps created: " + campsCreated + "<br/>" +
				"Gains: " + gstr + "<br/>" +
				"Losses: " + lstr + "<br/>" +
				"Tax base delta: " + taxBaseDelta + 
				"</b></div>";
	}
	
	public ArrayList filterReports(ArrayList reports, int nationNo) {
		ArrayList ret = new ArrayList();
		for (BaseReportObject bro : (ArrayList<BaseReportObject>)reports) {
			if (nationNo == -1 || bro.containsNation(nationNo)) ret.add(bro);
		}
		return ret;
	}
	
	protected String shortenPopDocks(String popDocks) {
		if (popDocks.equals("-")) popDocks = "None";
		return popDocks;
	}
	
	protected String shortenPopSize(String popSize) {
		if (popSize.equals("Major Town")) popSize = "MT";
		if (popSize.equals("Town")) popSize = "T";
		if (popSize.equals("Village")) popSize = "Vil";
		return popSize;
	}
	
	protected String shortenPopFort(String popFort) {
		if (popFort.equals("-")) popFort = "None";
		if (popFort.equals("Fort")) popFort = "F";
		if (popFort.equals("Tower")) popFort = "T";
		if (popFort.equals("Citadel")) popFort = "Cit";
		return popFort;
	}
	
	
	public String renderReport() {
		try {
			String ret = "";
			Game g = GameHolder.instance().getGame();
	    	Turn t = g.getTurn();
	    	Turn p = g.getTurn(t.getTurnNo()-1);
	    	if (p == null) return "";
			ArrayList reports;
			
			ret += "<div style='font-family:MS Sans Serif; font-size:11pt'>";
			try {
				reports = CollectNations(t, p);
				ret += renderCollection("Nations", "Relation, allegiance and tax rate changes", reports);
			} 
			catch (RuntimeException e) {
				e.printStackTrace();
			}
			
			ret += "<br/>";
			try {
				reports = CollectCharacters(t, p);
				ret += renderCollection("Characters", "New, lost and injured characters", reports);
				ret += renderCharsSummary(reports);
			} catch (RuntimeException e) {
				e.printStackTrace();
			}
			
			ret += "<br/>";
			try {
				reports = CollectPopCenters(t, p);
				ret += renderCollection("Pop Centers", "New, gained, lost or changed pop centers", reports);
				ret += renderPopsSummary(reports);
			} catch (RuntimeException e) {
				e.printStackTrace();
			}
			
	    	ret += "<br/>";
			try {
				reports = CollectCompanies(t, p);
				ret += renderCollection("Companies", "New, disbanded or changed companies", reports);
			} catch (RuntimeException e) {
				e.printStackTrace();
			}
			
			ret += "<br/>";
			try {
				reports = CollectArtifacts(t, p);
				ret += renderCollection("Artifacts", "Gained, lost, transfered or located artifacts", reports);
			} catch (RuntimeException e) {
				e.printStackTrace();
			}
			
			ret += "<br/>";
			try {
				reports = collectChallenges(t, p);
				ret += renderCollection("Challenges", reports);
			} catch (RuntimeException e) {
				e.printStackTrace();
			}
			
			ret += "<br/>";
			try {
				reports = CollectCombats(t);
				ret += renderCollection("Combats", reports);
			} catch (RuntimeException e) {
				e.printStackTrace();
			}
			
			ret += "<br/>";
			try {
				reports = CollectGoldSteals(t, p);
				ret += renderCollection("Gold Steals", reports);
				ret += renderStealsSummary(reports);
			} catch (RuntimeException e) {
				e.printStackTrace();
			}
			
			ret += "<br/>";
			try {
				reports = CollectAgentActions(t, p);
				ret += renderCollection("Agent Actions", "Actions by friendly agents (assassinations, kidnaps, sabotages)", reports);
			} catch (RuntimeException e) {
				e.printStackTrace();
			}
			
			ret += "<br/>";
			try {
				reports = CollectTransports(t, p);
				ret += renderCollection("Nation Transports", "Gold and product transports and transport rumors", reports);
			} catch (RuntimeException e) {
				e.printStackTrace();
			}
			
			ret += "<br/>";
			try {
				reports = CollectSpells(t, p);
				ret += renderCollection("Spells", "Spells learnt", reports);
			} catch (RuntimeException e) {
				e.printStackTrace();
			}
			
			ret += "<br/>";
			try {
				reports = collectBridges(t, p);
				ret += renderCollection("Bridges", "Bridges built or destroyed", reports);
			} catch (RuntimeException e) {
				e.printStackTrace();
			}
			
			ret += "<br/>";
			try {
				reports = CollectNonFriendlyChars(t);
				ret += renderCollection("Other Characters", "Non-friendly characters reported (excluding army commanders)", reports);
			} catch (RuntimeException e) {
				e.printStackTrace();
			}
			
			ret += "<br/>";
			try {
				reports = collectEncounters(t);
				ret += renderCollection("Encounters", "Encounters and encounter rumors", reports);
			} catch (RuntimeException e) {
				e.printStackTrace();
			}
			
			ret += "<br/>";
			try {
				reports = CollectDragons(t);
				ret += renderCollection("Dragons", "Dragons in armies or pop centers", reports);
			} catch (RuntimeException e) {
				e.printStackTrace();
			}
			
			ret += "<br/>";
			try {
				reports = collectUpcomingCombats(t);
				ret += renderCollection("Upcoming Combats", "Hexes where combat may occur", reports);
			} catch (RuntimeException e) {
				e.printStackTrace();
			}
			
			
			ret += "<br/><i>Green: positive events<br/>Red: negative events<br/>Yellow: other</i>";
			return ret;
		}
		catch (Exception exc) {
			exc.printStackTrace();
			Logger.getLogger(TurnReportCollector.class).error(exc.getLocalizedMessage());
			return "";
		}
	}
}