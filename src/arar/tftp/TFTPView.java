package arar.tftp;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.TextArea;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;

/**
 * @author Bruno Buiret, Thomas Arnaud, Sydney Adjou
 */
public final class TFTPView extends JFrame implements ActionListener {

    private JButton btn_Send;
    private JButton btn_Receive;
    private JButton ChooseFile;
    private JButton _btn_ChooseDownloadFile;
    private JTextField Field_PathSend;
    private JTextField Field_PathReceive;
    private JTextField Fied_RemoteSend;
    private JTextField Fied_RemoteReceive;
    private JTextField Fied_IPSend;
    private JTextField Fied_IPReceive;
    private JLabel lbl_LocationSend;
    private JLabel lbl_IPSend;
    private JLabel lbl_ServerSend;
    private JLabel lbl_LocationReceive;
    private JLabel lbl_IPReceive;
    private JLabel lbl_ServerReceive;
    private TextArea _textArea;

    public TFTPView() {

        btn_Send = new JButton("Send");
        btn_Send.addActionListener(this);

        ChooseFile = new JButton("Choose");
        ChooseFile.addActionListener(this);

        lbl_LocationSend = new JLabel("Location on your computer :");
        lbl_IPSend = new JLabel("Server's IP :");
        lbl_ServerSend = new JLabel("Location on the server :");

        lbl_LocationReceive = new JLabel("Location on your computer :");
        lbl_IPReceive = new JLabel("Server's IP :");
        lbl_ServerReceive = new JLabel("Location on the server :");

        Field_PathSend = new JTextField(10);
        Fied_RemoteSend = new JTextField(20);
        Fied_IPSend = new JTextField(20);

        //Gestion receive
        btn_Receive = new JButton("Receive");
        btn_Receive.addActionListener(this);

        _btn_ChooseDownloadFile = new JButton("Choose");
        _btn_ChooseDownloadFile.addActionListener(this);

        Field_PathReceive = new JTextField(10);
        Fied_RemoteReceive = new JTextField(20);
        Fied_IPReceive = new JTextField(20);

        //Gestion fenetre & layout
        JPanel total = new JPanel();
        total.setLayout(new BorderLayout());
        this.setTitle("Connexion to TFTP server");
        this.setLocationRelativeTo(null);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel formSend = new JPanel();
        formSend.setLayout(new GridLayout(4, 1));
        JPanel sendChooser = new JPanel();
        sendChooser.setLayout(new GridLayout(1, 2));
        sendChooser.add(lbl_LocationSend);
        JPanel sendChooserText = new JPanel();
        sendChooserText.setLayout(new GridLayout(1, 2));
        sendChooserText.add(Field_PathSend);
        sendChooserText.add(ChooseFile);
        sendChooser.add(sendChooserText);

        JPanel sendServer = new JPanel();
        sendServer.setLayout(new GridLayout(1, 2));
        sendServer.add(Fied_RemoteSend);

        JPanel sendIP = new JPanel();
        sendIP.setLayout(new GridLayout(1, 2));
        sendIP.add(lbl_IPSend);
        sendIP.add(Fied_IPSend);
        formSend.add(sendChooser);
        formSend.add(sendServer);
        formSend.add(sendIP);

        sendServer.setVisible(false);

        formSend.add(btn_Send);

        JPanel formReceive = new JPanel();
        formReceive.setLayout(new GridLayout(4, 1));
        JPanel receiveChooser = new JPanel();
        receiveChooser.setLayout(new GridLayout(1, 2));
        receiveChooser.add(lbl_LocationReceive);
        JPanel receiveChooserText = new JPanel();
        receiveChooserText.setLayout(new GridLayout(1, 2));
        receiveChooserText.add(Field_PathReceive);
        receiveChooserText.add(_btn_ChooseDownloadFile);
        receiveChooser.add(receiveChooserText);

        JPanel receiveServer = new JPanel();
        receiveServer.setLayout(new GridLayout(1, 2));
        receiveServer.add(lbl_ServerReceive);
        receiveServer.add(Fied_RemoteReceive);

        JPanel receiveIP = new JPanel();
        receiveIP.setLayout(new GridLayout(1, 2));
        receiveIP.add(lbl_IPReceive);
        receiveIP.add(Fied_IPReceive);
        formReceive.add(receiveChooser);
        formReceive.add(receiveServer);
        formReceive.add(receiveIP);
        formReceive.add(btn_Receive);

        JTabbedPane panel = new JTabbedPane();
        panel.addTab("Send", formSend);
        panel.addTab("Download", formReceive);

        total.add(panel, BorderLayout.NORTH);
        _textArea = new TextArea();
        _textArea.setFocusable(false);
        total.add(_textArea);

        JPanel main = new JPanel();
        main.setLayout(new BorderLayout());
        main.add(total, BorderLayout.CENTER);
        main.setBackground(Color.GRAY);

        this.setMinimumSize(new Dimension(394, 300));
        this.setContentPane(main);
        this.setFocusable(true);
        //this.setResizable(false);
    }

    @Override
    public void actionPerformed(ActionEvent e) {

        if (e.getSource() == this.ChooseFile) {
            // création de la boîte de dialogue
            JFileChooser dialogue = new JFileChooser();
            // affichage
            if (dialogue.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                this.Field_PathSend.setText(dialogue.getSelectedFile().getPath());

            }
        }

        if (e.getSource() == this.btn_Send) {
            if (this.Field_PathSend.getText().length() != 0) {
                if (this.Fied_IPSend.getText().length() != 0) {
                    String remote = Field_PathSend.getText();
                    String ext = remote.substring(remote.lastIndexOf("."));
                    remote = remote.substring(Field_PathSend.getText().lastIndexOf("\\"),
                            remote.lastIndexOf("."));
                    remote += "-remote" + ext;
                    System.out.println(remote);
                    try {
                        TFTPClient.sendFile(Field_PathSend.getText(),
                                remote,
                                InetAddress.getByName(this.Fied_IPSend.getText()),
                                69);
                    } catch (UnknownHostException ex) {
                        Logger.getLogger(TFTPView.class.getName()).log(Level.SEVERE, null, ex);
                        JOptionPane.showMessageDialog(this, "ADRESSE IP NON VALIDE", null, JOptionPane.WARNING_MESSAGE);
                    }
                } else {
                    JOptionPane.showMessageDialog(this, "ADRESSE IP NON RENSEIGNEE", null, JOptionPane.WARNING_MESSAGE);
                }
            } else {
                JOptionPane.showMessageDialog(this, "Vous n'avez pas entrer de chemin fichier", "avertissement", JOptionPane.WARNING_MESSAGE);
            }
        }

        if (e.getSource() == this._btn_ChooseDownloadFile) {
            // création de la boîte de dialogue
            JFileChooser dialogue = new JFileChooser();
            // affichage
            if (dialogue.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                this.Field_PathSend.setText(dialogue.getSelectedFile().getPath());

            }
        }

        if (e.getSource() == this.btn_Send) {
            if (this.Field_PathReceive.getText().length() != 0) {
                if (this.Fied_IPReceive.getText().length() != 0) {
                    if (Fied_RemoteReceive.getText().length() != 0) {
                        try {
                            String local = Field_PathReceive.getText();
                            local += local.substring(Fied_RemoteReceive.getText().lastIndexOf("\\"));
                            TFTPClient.sendFile(local, this.Fied_RemoteReceive.getText(),
                                    InetAddress.getByName(this.Fied_IPReceive.getText()),
                                    69);
                        } catch (UnknownHostException ex) {
                            Logger.getLogger(TFTPView.class.getName()).log(Level.SEVERE, null, ex);
                            JOptionPane.showMessageDialog(this, "MAUVAISE ADRESSE IP", "ERREUR", JOptionPane.WARNING_MESSAGE);
                        }
                    } else {
                        JOptionPane.showMessageDialog(this, "CHAMP FICHIER DISTANT MANQUANT", "ERREUR", JOptionPane.WARNING_MESSAGE);
                    }
                } else {
                    JOptionPane.showMessageDialog(this, "IP NON RENSEIGNEE", "CHAMP IP", JOptionPane.WARNING_MESSAGE);
                }
            } else {
                JOptionPane.showMessageDialog(this, "Vous n'avez pas entrer de chemin fichier", null, JOptionPane.WARNING_MESSAGE);
            }
        }
    }
}
