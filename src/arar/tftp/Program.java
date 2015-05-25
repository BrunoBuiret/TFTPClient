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
        try
        {
            /*System.out.println(
                    "Code de retour : " +
                    TFTPClient.receiveFile(
                        "mIRC.tx",
                        "local-mIRC.txt",
                        InetAddress.getByName("127.0.0.1"),
                        69
                    )
            );*/
            
            Client c = new Client();
            
            System.out.println(
                    "Code de retour : " + c.sendFile("mIRC.txt","local-mIRC.txt",InetAddress.getByName("127.0.0.1"), 69)
            );
        }
        catch(UnknownHostException e)
        {
            Logger.getLogger(Program.class.getName()).log(Level.SEVERE, e.getMessage(), e);
        }
    }
}
