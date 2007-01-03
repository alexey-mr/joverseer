package org.joverseer.support.readers.pdf;

import org.joverseer.domain.Artifact;
import org.joverseer.domain.Character;
import org.joverseer.game.Turn;
import org.joverseer.game.TurnElementsEnum;
import org.joverseer.support.Container;
import org.joverseer.support.infoSources.InfoSource;
import org.joverseer.support.infoSources.TurnInfoSource;
import org.joverseer.support.infoSources.spells.DerivedFromLocateArtifactInfoSource;
import org.joverseer.support.infoSources.spells.DerivedFromRevealCharacterInfoSource;
import org.joverseer.support.infoSources.spells.DerivedFromSpellInfoSource;

public class LocateArtifactTrueResultWrapper extends LocateArtifactResultWrapper {
    public void updateGame(Turn turn, int nationNo, String casterName) {
        String hexNo = (getHexNo()< 1000 ? "0" : "") + String.valueOf(getHexNo());
        DerivedFromLocateArtifactInfoSource is1 = new DerivedFromLocateArtifactInfoSource(turn.getTurnNo(), nationNo, casterName, getHexNo());

        if (getOwner() != null && !getOwner().equals("")) {
            Container chars = turn.getContainer(TurnElementsEnum.Character);
            Character c = (Character)chars.findFirstByProperty("name", getOwner());
            if (c == null) {
                // character not found, add
                c = new Character();
                c.setName(getOwner());
                c.setId(Character.getIdFromName(getOwner()));
                c.setHexNo(hexNo);
                c.setInfoSource(is1);
                c.setNationNo(0);
                chars.addItem(c);
            } else {
                // character found
                // examine info source
                InfoSource is = c.getInfoSource();
                if (TurnInfoSource.class.isInstance(is)) {
                    // turn import, do nothing
                } else if (DerivedFromSpellInfoSource.class.isInstance(is)) {
                    // spell
                    // check if it is LA or RC
                    if (DerivedFromLocateArtifactInfoSource.class.isInstance(is) ||
                        DerivedFromRevealCharacterInfoSource.class.isInstance(is)) {
                        // replace info source and hexNo
                        c.setHexNo(hexNo);
                        c.setInfoSource(is1);
                    } else {
                        // info source is LAT or RCT
                        // add
                        ((DerivedFromSpellInfoSource)is).addInfoSource(is1);
                    }
                } 
            }
        }
        
        Container artis = turn.getContainer(TurnElementsEnum.Artifact);
        Artifact a = (Artifact)artis.findFirstByProperty("number", getArtifactNo());
        if (a == null) {
            // artifact not found, add
            a = new Artifact();
            a.setNumber(getArtifactNo());
            a.setName(getArtifactName());
            a.setOwner(getOwner());
            a.setHexNo(getHexNo());
            a.setInfoSource(is1);
            artis.addItem(a);
        } else {
            // artifact found, check info source
            InfoSource is = a.getInfoSource();
            if (TurnInfoSource.class.isInstance(is)) {
                // turn import, do nothing
                return;
            } else if (DerivedFromSpellInfoSource.class.isInstance(is)) {
                // spell
                // check if it is LA or RC
                if (DerivedFromLocateArtifactInfoSource.class.isInstance(is) ||
                    DerivedFromRevealCharacterInfoSource.class.isInstance(is)) {
                    // replace info source and hexNo
                    a.setHexNo(getHexNo());
                    a.setInfoSource(is1);
                } else {
                    // info source is LAT or RCT
                    // add
                    ((DerivedFromSpellInfoSource)is).addInfoSource(is1);
                }
            } 
        }
    }
}