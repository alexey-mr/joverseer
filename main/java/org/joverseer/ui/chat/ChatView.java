package org.joverseer.ui.chat;

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.List;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Locale;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import javax.swing.text.StyledDocument;

import org.joverseer.domain.Character;
import org.joverseer.domain.Order;
import org.joverseer.game.TurnElementsEnum;
import org.joverseer.support.GameHolder;
import org.joverseer.ui.LifecycleEventsEnum;
import org.joverseer.ui.support.JOverseerEvent;
import org.joverseer.ui.support.dialogs.ErrorDialog;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.MessageSource;
import org.springframework.richclient.application.Application;
import org.springframework.richclient.application.support.AbstractView;
import org.springframework.richclient.dialog.FormBackedDialogPage;
import org.springframework.richclient.dialog.TitledPageApplicationDialog;
import org.springframework.richclient.form.FormModelHelper;
import org.springframework.richclient.image.ImageSource;
import org.springframework.richclient.layout.TableLayoutBuilder;

import com.sun.corba.se.impl.protocol.giopmsgheaders.Message;


public class ChatView extends AbstractView implements ApplicationListener {
    Thread chatThread;
    
    JTextPane text;
    JTextField message;
    StyledDocument doc;
    
    boolean connected = false;
    
    
    
    protected void setMessageEnabled(boolean v) {
        message.setEnabled(v);
    }
    
    protected JComponent createControl() {
        TableLayoutBuilder tlb = new TableLayoutBuilder();
        
        MessageSource messageSource = (MessageSource) getApplicationContext().getBean("messageSource");

//        text = new JTextArea();
//        text.setWrapStyleWord(true);
//        text.setLineWrap(true);
        text = new JTextPane();
        doc = text.getStyledDocument();
        
        Style def = StyleContext.getDefaultStyleContext().
        getStyle(StyleContext.DEFAULT_STYLE);
        Style regular = doc.addStyle("regular", def);
        StyleConstants.setFontFamily(def, "SansSerif");
        Style s = doc.addStyle("button", regular);
        StyleConstants.setAlignment(s, StyleConstants.ALIGN_CENTER);

        JScrollPane scp = new JScrollPane(text);
        scp.setPreferredSize(new Dimension(400, 100));
        tlb.cell(scp, "align=left rowSpec=fill:default:grow colspec=left:410px");
        tlb.gapCol();

        
        TableLayoutBuilder lb = new TableLayoutBuilder();
        JButton startChat = new JButton("C");
        lb.cell(startChat);
        startChat.addActionListener(new ActionListener() {
            
            public void actionPerformed(ActionEvent e) {
                if (connected) {
                    ErrorDialog dlg = new ErrorDialog("Already connected. You must disconnect first.");
                    dlg.showDialog();
                    return;
                }
                final ChatConnection conn = new ChatConnection();
                conn.setMyIP("127.0.0.1");
                conn.setMyPort(9600);
                conn.setPeerPort(9600);
                final ConnectToChatServerForm frm = new ConnectToChatServerForm(FormModelHelper.createFormModel(conn));
                
                FormBackedDialogPage page = new FormBackedDialogPage(frm);

                TitledPageApplicationDialog dialog = new TitledPageApplicationDialog(page) {
                    protected void onAboutToShow() {
                    }

                    protected boolean onFinish() {
                        frm.commit();
                        try {
//                            server = new ChatServer(conn.getPort());
//                            Thread t = new Thread(server);
//                            t.start();
//                            messageReceived("Server started - listening to port " + server.getPort() + ".");
//                            client = ChatClient.connect("localhost", conn.getPort(), new User(conn.getUsername()));
//                            setClient(client);
//                            String server = conn.getServer();
//                            messageReceived("Connected to " + conn.getServer() + ":" + conn.getPort() + " as " + conn.getUsername()); 
//                            setMessageEnabled(true);
                            addMsg("Chat started, waiting for connections...");
                        }
                        catch (Exception exc) {
                            setMessageEnabled(false);
                            ErrorDialog d = new ErrorDialog(exc);
                            d.showDialog();
                        }
                        return true;
                    }
                };
                MessageSource ms = (MessageSource)Application.services().getService(MessageSource.class);
                dialog.setTitle(ms.getMessage("editNoteDialog.title", new Object[]{""}, Locale.getDefault()));
                dialog.setModal(false);
                dialog.showDialog();
//                try {
//                    ChatServer cs = new ChatServer();
//                    Thread t = new Thread(cs);
//                    t.start();
//                    messageReceived("Server started!");
//                }
//                catch (Exception exc) {
//                    ErrorDialog d = new ErrorDialog(exc);
//                    d.showDialog();
//                }
            }
        });
        lb.relatedGapRow();
        JButton disconnect = new JButton("D");
        lb.cell(disconnect);
        disconnect.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
            	disconnect();
            }
        });
        
        
        
        tlb.cell(lb.getPanel(),"align=left");
        tlb.relatedGapRow();
        tlb.relatedGapRow();
        
        message = new JTextField();
        message.setPreferredSize(new Dimension(400, 20));
        message.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
//                if (client == null) {
//                    ErrorDialog dlg = new ErrorDialog("Not connected.");
//                    dlg.showDialog();
//                    setMessageEnabled(false);
//                    return;
//                }
//                if (!client.sendMessage(message.getText())) {
//                    ErrorDialog dlg = new ErrorDialog("Unexpected error sending message. Disconnecting...");
//                    dlg.showDialog();
//                    disconnect();
//                    return;
//                } else {
//                    message.setText("");
//                }
                message.setText("");
            }
        });
        setMessageEnabled(false);
        tlb.cell(message, "align=left");
//        tlb.gapCol();
//        JButton send = new JButton("Send");
//        tlb.cell(send);
//        send.addActionListener(new ActionListener() {
//            public void actionPerformed(ActionEvent e) {
//                client.sendMessage(message.getText());
//                message.setText("");
//            }
//        });
        tlb.relatedGapRow();
        
        
        
        return tlb.getPanel();
    }
    
    public JButton getAcceptButtonForOrderWrapper(OrderWrapper orderWrapper) {
        final OrderWrapper ow = orderWrapper;
        final JButton button = new JButton();
        final Character c = (Character)GameHolder.instance().getGame().getTurn().getContainer(TurnElementsEnum.Character).findFirstByProperty("id", ow.getCharId());
        if (c == null) return null;
        ImageSource imgSource = (ImageSource) Application.instance().getApplicationContext().getBean("imageSource");
        Icon ico = new ImageIcon(imgSource.getImage("acceptOrder.icon"));
        button.setIcon(ico);
        button.setPreferredSize(new Dimension(16, 16));
        button.setToolTipText("Accept order");
        button.setCursor(Cursor.getDefaultCursor());
        button.setMargin(new Insets(0,0,0,0));
        button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Order o = c.getOrders()[ow.getOrderIdx()];
                o.setOrderNo(ow.getOrderNo());
                o.setParameters(ow.getParameters());
                Application.instance().getApplicationContext().publishEvent(
                      new JOverseerEvent(LifecycleEventsEnum.OrderChangedEvent.toString(), o, this));
            }
        });
        
        return button;
    }
    
    public JButton getSelectCharButtonForOrderWrapper(OrderWrapper orderWrapper) {
        final OrderWrapper ow = orderWrapper;
        final JButton button = new JButton();
        final Character c = (Character)GameHolder.instance().getGame().getTurn().getContainer(TurnElementsEnum.Character).findFirstByProperty("id", ow.getCharId());
        if (c == null) return null;
        ImageSource imgSource = (ImageSource) Application.instance().getApplicationContext().getBean("imageSource");
        Icon ico = new ImageIcon(imgSource.getImage("selectHexCommand.icon"));
        button.setIcon(ico);
        button.setPreferredSize(new Dimension(16, 16));
        button.setToolTipText("Find char on map");
        button.setCursor(Cursor.getDefaultCursor());
        button.setMargin(new Insets(0,0,0,0));
        button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Application.instance().getApplicationContext().publishEvent(
                      new JOverseerEvent(LifecycleEventsEnum.SelectedHexChangedEvent.toString(), new Point(c.getX(), c.getY()), this));
            }
        });
        
        return button;
    }
    
    public void orderWrapperReceived(OrderWrapper orderWrapper, String username) {
        String msgStr;
        final OrderWrapper ow = orderWrapper;
        final Character c = (Character)GameHolder.instance().getGame().getTurn().getContainer(TurnElementsEnum.Character).findFirstByProperty("id", ow.getCharId());
        if (c != null) {
            if (c.getHexNo() != ow.getHexNo()) {
                msgStr = username + ": Order for '" + ow.getCharId() + "' but character was found at a different location.";
                addMsg(msgStr);
                return;
            }
            msgStr = username + "> Order for '" + ow.getCharId() + "' " + ow.getOrderNo() + " " + ow.getParameters().replace(Order.DELIM, " ") + ".";
            addMsg(msgStr);
            
            JButton btn = getAcceptButtonForOrderWrapper(ow);
            Style s= doc.getStyle("button");
            StyleConstants.setComponent(s, btn);
            final Style ns = s;
            try {
                doc.insertString(doc.getLength(), " ", ns);
                text.setCaretPosition(doc.getLength()-1);
            }
            catch (Exception exc) {};
            btn = getSelectCharButtonForOrderWrapper(ow);
            StyleConstants.setComponent(s, btn);
            try {
                doc.insertString(doc.getLength(), " ", doc.getStyle("regular"));
                doc.insertString(doc.getLength(), " ", ns);
                text.setCaretPosition(doc.getLength()-1);
            }
            catch (Exception exc) {};
        } else {
            msgStr = username + "> order for character " + ow.getCharId() + " but character was not found.";
            addMsg(msgStr);
        }
    }

    public void messageReceived(Message msg) {
        if (msg == null) return;
        String msgStr = "";
    }
    
    public void showObject(Object obj) {
        String msgStr = "";
        if (OrderWrapper.class.isInstance(obj)) {
            orderWrapperReceived((OrderWrapper)obj, "");
        } else if (MultiOrderWrapper.class.isInstance(obj)) {
            final MultiOrderWrapper mow = (MultiOrderWrapper)obj;
            addMsg("" + "> " + "sent group of orders.");
            for (OrderWrapper ow : mow.getOrderWrappers()) {
                orderWrapperReceived(ow, "");
            }
            addMsg("");
            //addMsg(msg.getUser().getUsername() + ": " + "Accept all orders.");
            JButton btn = new JButton("Accept all orders in this group");
            btn.setCursor(Cursor.getDefaultCursor());
            btn.setMargin(new Insets(0,0,0,0));
            btn.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    for (OrderWrapper ow : mow.getOrderWrappers()) {
                        JButton b = getAcceptButtonForOrderWrapper(ow);  
                        if (b == null) continue;
                        b.doClick();
                    }
                }
            });
            Style s= doc.getStyle("button");
            StyleConstants.setComponent(s, btn);
            final Style ns = s;
            try {
                doc.insertString(doc.getLength(), " ", ns);
                text.setCaretPosition(doc.getLength()-1);
            }
            catch (Exception exc) {};
            addMsg("");

        }
        else {
            msgStr = "" + "> " + obj.toString();
            addMsg(msgStr);
        }
    }

    public void messageReceived(String msg) {
        addMsg( msg);
        //text.setCaretPosition(text.getText().length());
    }

    private void addMsg(String msg) {
        try {
            final String m = msg;
            try {
                doc.insertString(doc.getLength(), "\n" + m, doc.getStyle("regular"));
                text.setCaretPosition(doc.getLength()-1);
            }
            catch (Exception exc) {
                exc.printStackTrace();
            }
        }
        catch (Exception exc) {
            exc.printStackTrace();
        }
    }
    
    public void sendOrder(Order o) {
    }
    
    public void sendOrders(ArrayList<Order> os) {
    }
    
    public void onApplicationEvent(ApplicationEvent applicationEvent) {
        if (applicationEvent instanceof JOverseerEvent) {
            JOverseerEvent e = (JOverseerEvent)applicationEvent;
            if (e.getEventType().equals(LifecycleEventsEnum.SendOrdersByChat.toString())) {
                if (Order.class.isInstance(e.getObject())) {
                    sendOrder((Order)e.getObject());
                } else if (ArrayList.class.isInstance(e.getObject())) {
                    sendOrders((ArrayList<Order>)e.getObject());
                }
                
            } 
        }
    }
    
    protected void disconnect() {
    }

    
}
