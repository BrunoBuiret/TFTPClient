package arar.tftp;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Bruno Buiret, Thomas Arnaud, Sydney Adjou
 */
public abstract class Program
{
    public static void main(String[] args)
    {
        String serverAddress = "134.214.117.93";
        int serverPort = 69;
        
        try
        {
            // Exemple de téléchargement depuis le serveur
            System.out.println(
                    "Code de retour : " +
                    TFTPClient.receiveFile(
                        "Lighthouse.jpg",
                        "local-Lighthouse.jpg",
                        InetAddress.getByName(serverAddress),
                        serverPort
                    )
            );
            
            // Exemple de téléchargement vers le serveur
            /*
            System.out.println(
                "Code de retour : " +
                TFTPClient.sendFile(
                    "Desert.jpg",
                    "Desert-remote.jpg",
                    InetAddress.getByName(serverAddress),
                    serverPort
                )
            );
            */
        }
        catch(UnknownHostException e)
        {
            Logger.getLogger(Program.class.getName()).log(Level.SEVERE, e.getMessage(), e);
        }
    }
}
