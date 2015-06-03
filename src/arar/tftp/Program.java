package arar.tftp;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

/**
 * @author Bruno Buiret, Thomas Arnaud, Sydney Adjou
 */
public abstract class Program
{
    public static void main(String[] args)
    {
        try
        {
            // Exemple de téléchargement depuis le serveur
            /*
            System.out.println(
                    "Code de retour : " +
                    TFTPClient.receiveFile(
                        "mIRC.tx",
                        "local-mIRC.txt",
                        InetAddress.getByName("127.0.0.1"),
                        69
                    )
            );
            */
            
            // Exemple de téléchargement vers le serveur
            // Get the OS style and apply it
        
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            TFTPView vue = new TFTPView();           
            vue.setVisible(true);
            
        }
        catch(ClassNotFoundException|InstantiationException|IllegalAccessException|UnsupportedLookAndFeelException e)
        {
            Logger.getLogger(Program.class.getName()).log(Level.SEVERE, null, e);
        } 
    }
}
