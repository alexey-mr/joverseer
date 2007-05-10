package org.joverseer.ui.views;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileWriter;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;
import java.util.prefs.Preferences;
import java.util.regex.Pattern;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import org.apache.commons.beanutils.BeanComparator;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.methods.multipart.StringPart;
import org.joverseer.domain.Character;
import org.joverseer.domain.PlayerInfo;
import org.joverseer.game.Game;
import org.joverseer.game.TurnElementsEnum;
import org.joverseer.orders.export.OrderFileGenerator;
import org.joverseer.support.GameHolder;
import org.joverseer.tools.ordercheckerIntegration.OrderResultContainer;
import org.joverseer.tools.ordercheckerIntegration.OrderResultTypeEnum;
import org.joverseer.ui.command.OpenXmlDir;
import org.joverseer.ui.command.SaveGame;
import org.joverseer.ui.support.dialogs.ErrorDialog;
import org.joverseer.ui.support.dialogs.InputDialog;
import org.springframework.binding.form.FormModel;
import org.springframework.context.MessageSource;
import org.springframework.richclient.application.Application;
import org.springframework.richclient.dialog.ConfirmationDialog;
import org.springframework.richclient.dialog.MessageDialog;
import org.springframework.richclient.form.AbstractForm;
import org.springframework.richclient.layout.GridBagLayoutBuilder;


public class ExportOrdersForm extends AbstractForm {
    public static int ORDERS_OK = 0;
    public static int ORDERS_NOT_OK = 1;
    
    JComboBox nation;
    JComboBox version;
    JTextArea orders;
    boolean ordersOk = false;
    boolean cancelExport = false;
    
    int orderCheckResult = 0;
    
    boolean uncheckedOrders = false;
    boolean ordersWithErrors = false;
    boolean missingOrders = false;
    
    public ExportOrdersForm(FormModel model) {
        super(model, "ExportOrdersForm");
    }
    
    private ArrayList getNationItems() {
        Game g = GameHolder.instance().getGame();
        ArrayList<PlayerInfo> pis = (ArrayList<PlayerInfo>)g.getTurn().getContainer(TurnElementsEnum.PlayerInfo).getItems();
        ArrayList ret = new ArrayList();
        for (PlayerInfo pi : pis) {
            ret.add(g.getMetadata().getNationByNum(pi.getNationNo()).getName());
        }
        return ret;
    }
    
    private int getSelectedNationNo() {
        String nationName = nation.getSelectedItem().toString();
        Game g = GameHolder.instance().getGame();
        return g.getMetadata().getNationByName(nationName).getNumber();
    }
    
    protected JComponent createFormControl() {
        GridBagLayoutBuilder glb = new GridBagLayoutBuilder();
        glb.append(new JLabel("Nation :"));
        glb.append(nation = new JComboBox(getNationItems().toArray()));
        nation.setPreferredSize(new Dimension(100, 24));
        nation.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                Game g = GameHolder.instance().getGame();
                int nationNo = getSelectedNationNo();
                PlayerInfo pi = (PlayerInfo)g.getTurn().getContainer(TurnElementsEnum.PlayerInfo).findFirstByProperty("nationNo", nationNo);
                version.setSelectedItem(String.valueOf(pi.getTurnVersion()));
            }
            
        });
        
        glb.nextLine();
        
        glb.append(new JLabel("Version :"));
        glb.append(version = new JComboBox(new String[]{"1", "2", "3", "4", "5", "6", "7", "8", "9"}));
        version.setPreferredSize(new Dimension(20, 24));
        glb.nextLine();
        
        orders = new JTextArea();
        orders.setWrapStyleWord(false);
        orders.setLineWrap(false);
        JScrollPane scp = new JScrollPane(orders);
        scp.setPreferredSize(new Dimension(500, 400));
        glb.append(scp, 3, 1);
        
        glb.nextLine();
        JButton generate = new JButton("Generate");
        generate.setPreferredSize(new Dimension(100, 20));
        glb.append(generate, 1, 1);
        glb.nextLine();
        generate.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                OrderFileGenerator gen = new OrderFileGenerator();
                Game g = GameHolder.instance().getGame();
                try {
                    orders.setText(gen.generateOrderFile(g, g.getTurn(), getSelectedNationNo()));
                    orders.setCaretPosition(0);
                    orderCheckResult = validateOrders();
                    ordersOk = true;
                }
                catch (Exception exc) {
                    orders.setText(exc.getMessage());
                    ordersOk = false;
                }
            }
        });
        JButton save = new JButton("Save");
        save.setPreferredSize(new Dimension(100, 20));
        glb.append(save, 1, 1);
        
        save.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (!ordersOk) return;
                if (!checkOrderValidity()) return;
                Game g = GameHolder.instance().getGame();
                int nationNo = getSelectedNationNo();
                PlayerInfo pi = (PlayerInfo)g.getTurn().getContainer(TurnElementsEnum.PlayerInfo).findFirstByProperty("nationNo", nationNo);
                String fname = String.format("me%02dv%s.%03d", getSelectedNationNo(), version.getSelectedItem(), g.getMetadata().getGameNo());
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setDialogType(JFileChooser.SAVE_DIALOG);
                fileChooser.setApproveButtonText("Save");
                fileChooser.setSelectedFile(new File(fname));
                Preferences prefs = Preferences.userNodeForPackage(OpenXmlDir.class);
                String lastDir = prefs.get("importDir", null);
                if (lastDir != null) {
                    fileChooser.setCurrentDirectory(new File(lastDir));
                }
                if (fileChooser.showSaveDialog(Application.instance().getActiveWindow().getControl()) == JFileChooser.APPROVE_OPTION) {
                    try {
                        pi.setTurnVersion(Integer.parseInt(version.getSelectedItem().toString()) + 1);
                        FileWriter f = new FileWriter(fileChooser.getSelectedFile());
                        String txt = orders.getText();
                        txt = txt.replace("\n", System.getProperty("line.separator"));
                        f.write(txt);
                        f.close();
                        MessageSource ms = (MessageSource)Application.services().getService(MessageSource.class);
                        MessageDialog md = new MessageDialog("Turn Exported", "The turn was succesfully exported to file " + fileChooser.getSelectedFile() + ".");
                        md.showDialog();
                    }
                    catch (Exception exc) {
                        MessageSource ms = (MessageSource)Application.services().getService(MessageSource.class);
                        MessageDialog md = new MessageDialog(
                                ms.getMessage("errorDialog.title", new String[]{}, Locale.getDefault()),
                                exc.getMessage());
                        md.showDialog();
                    }
                }
            }
        });
        
        JButton send = new JButton("Send");
        send.setPreferredSize(new Dimension(100, 20));
        glb.append(send, 1, 1);
        glb.append(new JLabel(), 1, 1);
        
        send.setVisible(true);
        
        send.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (!ordersOk) return;
                if (!checkOrderValidity()) return;
                Game g = GameHolder.instance().getGame();
                try {
                    String fname = String.format("me%02dv%s.%03d", getSelectedNationNo(), version.getSelectedItem(), g.getMetadata().getGameNo());
                    File file = new File(fname);
                    FileWriter f = new FileWriter(file);
                    f.write(orders.getText());
                    f.close();
                    
                    Preferences prefs = Preferences.userNodeForPackage(ExportOrdersForm.class);
                    String email = prefs.get("useremail", "");
                    String emailRegex = "^(\\p{Alnum}+(\\.|\\_|\\-)?)*\\p{Alnum}@(\\p{Alnum}+(\\.|\\_|\\-)?)*\\p{Alpha}$";
                    InputDialog idlg = new InputDialog();
                    idlg.setTitle("Send turn");
                    idlg.init("Enter the email address where you want the confirmation email to be sent.");
                    JTextField emailText = new JTextField();
                    idlg.addComponent("Email address :", emailText);
                    idlg.setPreferredSize(new Dimension(400, 80));
                    emailText.setText(email);
                    do {
                        idlg.showDialog();
                        if (!idlg.getResult()) {
                            ErrorDialog md = new ErrorDialog("Send aborted.");
                            md.showDialog();
                            return;
                        }
                        email = emailText.getText();
                    } while (!Pattern.matches(emailRegex, email));
                    prefs.put("useremail", email);
                    
                    String name = "";
                    String acct = "";
                    
                    String url = "http://test.com?submitme";
                    PostMethod filePost = new PostMethod(url);
                    Part[] parts = {
                            new StringPart("emailaddr", email),
                            new StringPart("name", name),
                            new StringPart("account", acct),
                            new FilePart(file.getName(), file)
                        };
                    filePost.setRequestEntity(new MultipartRequestEntity(parts, filePost.getParams()));
                    HttpClient client = new HttpClient();
                    client.getHttpConnectionManager().
                        getParams().setConnectionTimeout(5000);
                    int status = client.executeMethod(filePost);
                    String msg = "";
                    if (status == HttpStatus.SC_OK) {
                        msg = "Turn sent succesfully. You should save your game now (to update the order version counter). The save game dialog will automatically come up.";
                        MessageDialog dlg = new MessageDialog("Send turn", msg);
                        dlg.showDialog();
                        (new SaveGame()).execute();
                        return;
                    } else {
                        msg = "Send failed (response: " + HttpStatus.getStatusText(status) + ").";
                        MessageDialog dlg = new MessageDialog("Send turn", msg);
                        dlg.showDialog();
                    }
                }
                catch (Exception exc) {
                    ErrorDialog dlg = new ErrorDialog("Send failed. Cause: " + exc.getMessage());
                    dlg.showDialog();
                    exc.printStackTrace();
                }
            }
        });
                
        nation.setSelectedIndex(0);
        
        return glb.getPanel();
    }
    
    private int validateOrders() {
        Game g = GameHolder.instance().getGame();
        ArrayList<Character> chars = (ArrayList<Character>)g.getTurn().getContainer(TurnElementsEnum.Character).findAllByProperty("nationNo", getSelectedNationNo());
        Collections.sort(chars, new BeanComparator("id"));
        ArrayList<Character> toRemove = new ArrayList<Character>();
        for (Character ch : chars) {
            if (ch.getHealth() == null || ch.getHealth() == 0 || ch.getHexNo() <= 0) {
                toRemove.add(ch);
            }
        }
        chars.removeAll(toRemove);
        
        missingOrders = false;
        ordersWithErrors = false;
        uncheckedOrders = false;
        
        OrderResultContainer orc = (OrderResultContainer)Application.instance().getApplicationContext().getBean("orderResultContainer");
        
        for (Character ch : chars) {
            for (int i=0; i<2; i++) {
                if (ch.getOrders()[i].isBlank()) {
                    missingOrders = true;
                } else {
                    if (orc.getResultsForOrder(ch.getOrders()[i]).size() == 0) {
                        uncheckedOrders = true;
                    } else {
                        if (orc.getResultTypeForOrder(ch.getOrders()[i]) == OrderResultTypeEnum.Error ||
                                orc.getResultTypeForOrder(ch.getOrders()[i]) == OrderResultTypeEnum.Warning) {
                            ordersWithErrors = true;
                        }
                    }
                }
            }
        }
        
        if (missingOrders || uncheckedOrders || ordersWithErrors) return ORDERS_NOT_OK;
        return ORDERS_OK;
    }
    
    private boolean checkOrderValidity() {
        cancelExport = false;
        if (orderCheckResult != ORDERS_OK) {
            if (missingOrders) {
                MessageDialog dlg = new MessageDialog("Error", "Some characters are missing orders. Cannot export.");
                dlg.showDialog();
                return false;
            }
            if (ordersWithErrors) {
                cancelExport = false;
                ConfirmationDialog dlg = new ConfirmationDialog("Warning", "Some orders have been checked with Orderchecker and have errors or warnings. Continue with export?") {
                    protected void onCancel() {
                        super.onCancel();
                        cancelExport = true;
                    }
                    
                    protected void onConfirm() {
                    }
                    
                };
                dlg.showDialog();
                if (cancelExport) return false;
            }
            if (uncheckedOrders) {
                ConfirmationDialog dlg = new ConfirmationDialog("Warning", "Some orders have not been checked with Orderchecker. Continue with export?") {
                    protected void onCancel() {
                        super.onCancel();
                        cancelExport = true;
                    }

                    protected void onConfirm() {
                    }
                };
                dlg.showDialog();
                if (cancelExport) return false;
            }
        }
        return true;

    }
    
}
