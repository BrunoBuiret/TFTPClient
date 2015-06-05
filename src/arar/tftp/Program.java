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
        // Try and apply the OS' style
        try
        {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        }
        catch(ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e)
        {
            Logger.getLogger(Program.class.getName()).log(Level.SEVERE, e.getMessage(), e);
        }
        
        // Open the TFTP client view
        TFTPView view = new TFTPView();
        view.setVisible(true);
    }
}
