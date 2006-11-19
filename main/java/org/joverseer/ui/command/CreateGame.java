package org.joverseer.ui.command;

import org.springframework.richclient.command.ActionCommand;
import org.springframework.richclient.application.Application;
import org.springframework.richclient.form.FormModelHelper;
import org.springframework.richclient.dialog.FormBackedDialogPage;
import org.springframework.richclient.dialog.TitledPageApplicationDialog;
import org.springframework.binding.form.FormModel;
import org.springframework.context.MessageSource;
import org.joverseer.game.Game;
import org.joverseer.game.Turn;
import org.joverseer.metadata.GameMetadata;
import org.joverseer.metadata.GameTypeEnum;
import org.joverseer.support.GameHolder;
import org.joverseer.support.TurnInitializer;
import org.joverseer.ui.orders.OrderEditorForm;
import org.joverseer.ui.support.JOverseerEvent;
import org.joverseer.ui.LifecycleEventsEnum;
import org.joverseer.ui.NewGameForm;
import org.joverseer.ui.domain.NewGame;
import org.joverseer.domain.Order;

import java.awt.*;
import java.util.Locale;

/**
 * Created by IntelliJ IDEA.
 * User: mskounak
 * Date: 22 ��� 2006
 * Time: 10:46:41 ��
 * To change this template use File | Settings | File Templates.
 */
public class CreateGame extends ActionCommand {
    public CreateGame() {
        super("createGameCommand");
    }

    protected void doExecuteCommand() {
        final NewGame ng = new NewGame();
        FormModel formModel = FormModelHelper.createFormModel(ng);
        final NewGameForm form = new NewGameForm(formModel);
        FormBackedDialogPage page = new FormBackedDialogPage(form);

        TitledPageApplicationDialog dialog = new TitledPageApplicationDialog(page) {
            protected void onAboutToShow() {
            }

            protected boolean onFinish() {
                form.commit();
                // throw a selected hex changed event for current hex
                // first we need to find the current hex
                Game game = new Game();
                GameMetadata gm = (GameMetadata)Application.instance().getApplicationContext().getBean("gameMetadata");
                gm.setGameType(ng.getGameType());
                gm.load();
                game.setMetadata(gm);
                game.setMaxTurn(0);
                GameHolder gh = (GameHolder)Application.instance().getApplicationContext().getBean("gameHolder");
                gh.setGame(game);

                Turn t0 = new Turn();
                t0.setTurnNo(0);
                TurnInitializer ti = new TurnInitializer();
                ti.initializeTurnWith(t0, null);
                try {
                    game.addTurn(t0);
                }
                catch (Exception exc) {
                    // do nothing, exception cannoit really occur
                }

                Application.instance().getApplicationContext().publishEvent(
                                    new JOverseerEvent(LifecycleEventsEnum.GameChangedEvent.toString(), game, this));

                return true;
            }
        };
        MessageSource ms = (MessageSource)Application.services().getService(MessageSource.class);
        dialog.setTitle(ms.getMessage("newGameDialog.title", new Object[]{}, Locale.getDefault()));
        dialog.showDialog();

    }
}
