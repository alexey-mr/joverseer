package org.joverseer.support;

import java.util.ArrayList;

import org.joverseer.domain.Army;
import org.joverseer.domain.Challenge;
import org.joverseer.domain.Character;
import org.joverseer.domain.CharacterDeathReasonEnum;
import org.joverseer.domain.Encounter;
import org.joverseer.domain.Note;
import org.joverseer.domain.PopulationCenter;
import org.joverseer.game.Game;
import org.joverseer.game.Turn;
import org.joverseer.game.TurnElementsEnum;
import org.joverseer.ui.LifecycleEventsEnum;
import org.joverseer.ui.support.JOverseerEvent;
import org.springframework.richclient.application.Application;

/**
 * Class the post-processes a newly imported turn, i.e. it is called after new
 * files have been imported
 * 
 * Note that it should be idempotent, i.e. it may be called many times for the
 * same turn, the 1st time after half the files have been imported and the 2nd
 * time after the same turn is re-imported with additional files
 * 
 * Currently it: - deletes all notes from the turn for which it can no longer
 * find the target - copies all notes from the previous turn that are persistent
 * and the target can be found
 * 
 * @author Marios Skounakis
 * 
 */
public class TurnPostProcessor {
	public void postProcessTurn(Turn turn) {
		// delete all notes for which target can no longer be found
		ArrayList<Note> toRemove = new ArrayList<Note>();
		for (Note n : turn.getNotes().getItems()) {
			boolean keep = false;
			Object target = null;
			if (Integer.class.isInstance(n.getTarget())) {
				// hexNo target, copy
				keep = true;
				target = n.getTarget();
			} else if (Character.class.isInstance(n.getTarget())) {
				Character c = (Character) n.getTarget();
				// check if char exists
				target = turn.getContainer(TurnElementsEnum.Character).findFirstByProperty("name", c.getName());
				if (target != null) {
					keep = true;

				}
			} else if (PopulationCenter.class.isInstance(n.getTarget())) {
				PopulationCenter pc = (PopulationCenter) n.getTarget();
				// check if pop center exists
				if ((target = turn.getContainer(TurnElementsEnum.PopulationCenter).findFirstByProperty("name", pc.getName())) != null) {
					keep = true;
				}
			} else if (Army.class.isInstance(n.getTarget())) {
				Army a = (Army) n.getTarget();
				// check if pop center exists
				if ((target = turn.getContainer(TurnElementsEnum.Army).findFirstByProperty("commanderName", a.getCommanderName())) != null) {
					keep = true;
				}
			}
			if (!keep) {
				toRemove.add(n);
			}
		}
		turn.getNotes().removeAll(toRemove);

		// copy persistent notes from previous turn
		final Game g = ((GameHolder) Application.instance().getApplicationContext().getBean("gameHolder")).getGame();
		Turn previousTurn = null;
		for (int i = g.getCurrentTurn() - 1; i >= 0; i--) {
			if (g.getTurn(i) != null) {
				previousTurn = g.getTurn(i);
				break;
			}
		}
		if (previousTurn == null)
			return;

		for (Note n : previousTurn.getNotes().findAllByProperty("persistent", true)) {
			boolean copy = false;
			Object target = null;
			if (Integer.class.isInstance(n.getTarget())) {
				// hexNo target, copy
				copy = true;
				target = n.getTarget();
			} else if (Character.class.isInstance(n.getTarget())) {
				Character c = (Character) n.getTarget();
				// check if char exists
				target = turn.getContainer(TurnElementsEnum.Character).findFirstByProperty("name", c.getName());
				if (target != null) {
					copy = true;

				}
			} else if (PopulationCenter.class.isInstance(n.getTarget())) {
				PopulationCenter pc = (PopulationCenter) n.getTarget();
				// check if pop center exists
				if ((target = turn.getContainer(TurnElementsEnum.PopulationCenter).findFirstByProperty("name", pc.getName())) != null) {
					copy = true;
				}
			} else if (Army.class.isInstance(n.getTarget())) {
				Army a = (Army) n.getTarget();
				// check if pop center exists
				if ((target = turn.getContainer(TurnElementsEnum.Army).findFirstByProperty("commanderName", a.getCommanderName())) != null) {
					copy = true;
				}
			}
			if (copy) {
				if (turn.getContainer(TurnElementsEnum.Notes).findFirstByProperty("id", n.getId()) == null) {
					Note newNote = new Note();
					newNote.setId(n.getId());
					newNote.setTarget(target);
					newNote.setText(n.getText() + "");
					newNote.setPersistent(n.getPersistent());
					newNote.setNationNo(n.getNationNo());
					turn.getNotes().addItem(newNote);
				}
			}
		}

		// change CharacterDeathReasonEnum.Dead to Challenged where appropriate
		for (Character c : turn.getCharacters().findAllByProperty("deathReason", CharacterDeathReasonEnum.Dead)) {
			for (Challenge ch : turn.getChallenges().getItems()) {
				if (ch.getLoser() != null && ch.getLoser().equals(c.getName())) {
					c.setDeathReason(CharacterDeathReasonEnum.Challenged);
				}
			}
		}

		// create encounters for caves that can be investigated
		for (Character c : turn.getAllCharacters()) {
			if (!c.hasOrderResults())
				continue;
			String cleanOrderResults = c.getCleanOrderResults();
			String investigate = StringUtils.getUniquePart(cleanOrderResults, "has encountered [\\w\\s]+ which can be investigated", "\\.", true, true);
			if (investigate != null) {
				Encounter e = turn.getEncounter(c.getHexNo(), c.getName());
				if (e != null) {
					turn.getEncounters().removeItem(e);
				}
				e = new Encounter();
				e.setCanInvestigate(true);
				e.setCharacter(c.getName());
				e.setHexNo(c.getHexNo());
				e.setDescription(c.getName() + " " + investigate);
				turn.getEncounters().addItem(e);
			}
		}

		Application.instance().getApplicationContext().publishEvent(new JOverseerEvent(LifecycleEventsEnum.ListviewRefreshItems.toString(), this, this));
	}
}
