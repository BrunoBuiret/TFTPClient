package arar.tftp;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Hashtable;

/**
 * @author Bruno Buiret, Thomas Arnaud, Sydney Adjou
 */
public class TFTPClient
{
    // Read request (RRQ)
    protected final static byte CODE_RRQ = 1;
    
    // Write request (WRQ)
    protected final static byte CODE_WRQ = 2;
    
    // Data (DATA)
    protected final static byte CODE_DATA = 3;
    
    // Acknowledgement (ACK)
    protected final static byte CODE_ACK = 4;
    
    // Error (ERROR)
    protected final static byte CODE_ERROR = 5;
    
    /**
     * 
     * @param remoteFile Chemin vers le fichier distant sur le serveur.
     * @param localFile Chemin vers le fichier local.
     * @param serverAddress Adresse du serveur TFTP.
     * @param serverPort Port du serveur TFTP.
     * @return 
     */
    public static int receiveFile(String remoteFile, String localFile, InetAddress serverAddress, int serverPort)
    {
        // Représentation locale du fichier
        File file = new File(localFile);
        
        if(file.exists())
        {
            // Le fichier local existe déjà, on ne l'écrase pas
            return -3;
        }
        
        try
        {
            // Le fichier n'existe pas, on le crée
            BufferedOutputStream output = new BufferedOutputStream(new FileOutputStream(file));
            
            // Socket de communication
            DatagramSocket socket = new DatagramSocket();
            
            // Mémorisation des morceaux de fichiers reçus avec le nombre d'ACK
            // envoyé pour ces morceaux
            Hashtable<Integer, Integer> writtenChunks = new Hashtable<Integer, Integer>();
            
            // Variables utilisées pour la communication
            byte[] requestData, responseData;
            DatagramPacket requestPacket, responsePacket;
            
            // Envoi de la requête de téléchargement de fichier depuis le serveur
            requestData = ("\0\1" + remoteFile + "\0octet\0").getBytes();
            requestPacket = new DatagramPacket(requestData, requestData.length, serverAddress, serverPort);
            socket.send(requestPacket);
            
            // Boucle de réception du fichier
            do
            {
                // Variables relatives au morceau de fichier actuel
                int chunkNumber;
                responseData = new byte[516];
                responsePacket = new DatagramPacket(responseData, 516);
                
                // Attente d'envoi d'un message par le serveur
                socket.receive(responsePacket);
                
                if(responseData[1] == TFTPClient.CODE_DATA)
                {
                    // On a reçu un code DATA(3), on extrait le numéro du morceau
                    // de fichier et le nouveau port de communication
                    serverPort = responsePacket.getPort();
                    chunkNumber = ((responseData[2] & 0xFF) << 8)|(responseData[3] & 0xFF);
                    
                    if(!writtenChunks.containsKey(chunkNumber))
                    {
                        // Ce morceau de fichier n'a encore été reçu, alors on
                        // l'écrit dans le fichier local
                        output.write(responseData, 4, 512);
                        writtenChunks.put(chunkNumber, 1);
                    }
                    else if(writtenChunks.get(chunkNumber) == 3)
                    {
                        // C'est la quatrième fois qu'on reçoit ce morceau de fichier,
                        // on interrompt le processus
                        return -6;
                    }
                    else
                    {
                        // Ca ne fait que deux ou trois fois, on augmente le nombre
                        // de tentatives d'acquittement
                        writtenChunks.put(chunkNumber, writtenChunks.get(chunkNumber) + 1);
                    }
                    
                    // Envoi d'un acquitemment
                    requestData = ("\0\4" + responseData[2] + responseData[3]).getBytes();
                    requestData[2] -= '0';
                    requestData[3] -= '0';
                    requestPacket = new DatagramPacket(requestData, requestData.length, serverAddress, serverPort);
                    socket.send(requestPacket);
                    
                    if(responsePacket.getLength() < 516)
                    {
                        // C'est le dernier morceau de fichier reçu, il faut s'assurer que le serveur
                        // ait bien reçu l'acquittement par une tamporisation
                        boolean finalDataAcknowledged = false;
                        socket.setSoTimeout(2000);
                        
                        do
                        {
                            try
                            {
                                // L'acquittement peut s'être perdu donc on attend de voir
                                // si le serveur renvoit le dernier morceau du fichier
                                byte[] finalData = new byte[516];
                                DatagramPacket finalPacket = new DatagramPacket(finalData, 516);
                                socket.receive(finalPacket);
                                
                                if(finalData[1] == TFTPClient.CODE_DATA)
                                {
                                    requestData = ("\0\4" + finalData[2] + finalData[3]).getBytes();
                                    requestPacket = new DatagramPacket(requestData, requestData.length, serverAddress, serverPort);
                                    socket.send(requestPacket);
                                }
                            }
                            catch(SocketTimeoutException e)
                            {
                                // On n'a rien reçu c'est que le dernier acquittement
                                // a bien été reçu
                                finalDataAcknowledged = true;
                            }
                        }
                        while(!finalDataAcknowledged);
                        
                        socket.setSoTimeout(0);
                    }
                }
                else
                {
                    // Si on n'a pas reçu un code DATA(3), alors c'est qu'on a reçu
                    // un code ERROR(5) et on extrait le code de l'erreur pour le
                    // renvoyer
                    return ((responseData[2] & 0xFF) << 8)|(responseData[3] & 0xFF);
                }
            }
            while(responsePacket.getLength() == 516);
            
            // Fermeture du fichier
            output.close();
            
            // Fermeture du socket
            socket.close();
        }
        catch(FileNotFoundException e)
        {
            // Impossible de créer le fichier
            return -1;
        }
        catch(SocketException e)
        {
            // Impossible de créer la socket
            return -6;
        }
        catch(IOException e)
        {
            // Erreur d'entrée / sortie par rapport à la socket
            return -7;
        }
        
        // Il n'y a pas eu d'erreur
        return 0;
    }
    
    public static int sendFile()
    {
        return 0;
    }
}
