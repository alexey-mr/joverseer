package org.joverseer.ui.command;

import org.springframework.richclient.command.ActionCommand;

/**
 * Created by IntelliJ IDEA.
 * User: mskounak
 * Date: 6 ��� 2006
 * Time: 10:06:37 ��
 * To change this template use File | Settings | File Templates.
 */
public class EditRelations extends ActionCommand {
    public EditRelations() {
        super("editRelationsCommand");
    }

    protected void doExecuteCommand() {
//        Game g = ((GameHolder) Application.instance().getApplicationContext().getBean("gameHolder")).getGame();
//        final Turn t = g.getTurn();
//        FormModel formModel = FormModelHelper.createFormModel(t);
//        final EditNationRelations form = new EditNationRelations(formModel);
//        form.setFormObject(t);
//        FormBackedDialogPage page = new FormBackedDialogPage(form);
//
//        TitledPageApplicationDialog dialog = new TitledPageApplicationDialog(page) {
//            protected void onAboutToShow() {
//            }
//
//            protected boolean onFinish() {
//                form.commit();
//                return true;
//            }
//        };
//        dialog.showDialog();
    }
}