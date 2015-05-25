package arar.tftp;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.util.Hashtable;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Bruno Buiret, Thomas Arnaud, Sydney Adjou
 */
public class TFTPClient
{
    // Read request (RRQ)
    protected final static short CODE_RRQ = 1;
    
    // Write request (WRQ)
    protected final static short CODE_WRQ = 2;
    
    // Data (DATA)
    protected final static short CODE_DATA = 3;
    
    // Acknowledgement (ACK)
    protected final static short CODE_ACK = 4;
    
    // Error (ERROR)
    protected final static short CODE_ERROR = 5;
    
    /**
     * Téléchargement d'un fichier depuis le serveur TFTP.
     * 
     * @param remoteFile Chemin vers le fichier distant sur le serveur.
     * @param localFile Chemin vers le fichier local.
     * @param serverAddress Adresse du serveur TFTP.
     * @param serverPort Port du serveur TFTP.
     * @return 0 ou le code d'erreur.
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
            // Impossible de créer le socket
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
    
    /**
     * Téléchargement d'un fichiers vers le serveur TFTP.
     * 
     * @param localFile Chemin vers le fichier local.
     * @param remoteFile Chemin vers le fichier distant sur le serveur.
     * @param serverAddress Adresse du serveur TFTP.
     * @param serverPort Port du serveur TFTP.
     * @return 0 ou le code d'erreur.
     */
    public static int sendFile(String localFile, String remoteFile, InetAddress serverAddress, int serverPort)
    {
        // Représentation locale du fichier
        File file = new File(localFile);
        
        if(!file.exists())
        {
            // Le fichier n'existe pas
            return -1;
        }
        else if(!file.isFile())
        {
            // Format de fichier invalide
            return -5;
        }
        else if(!file.canRead())
        {
            // Droit de lecture non possédé
            return -2;
        }
        
        try
        {
            // Ouverture du fichier en lecture
            FileInputStream input = new FileInputStream(file);
            
            // Socket de communication
            DatagramSocket socket = new DatagramSocket();
            
            // Envoi de la requête de téléchargement de fichier vers le serveur
            socket.send(TFTPClient.createWRQ(remoteFile, "octet", serverAddress, serverPort));
            
            // Variables utilisées pour le transfert
            int chunkNumber, attemptsNumber, code;
            long totalBytes, readBytes;
            boolean dataAcknowledged;
            
            // Variables utilisées pour la communication
            DatagramPacket responsePacket;
            byte[] responseData, fileData;
            ByteBuffer responseBuffer;
            
            // Attente de la réponse
            responseData = new byte[516];
            responsePacket = new DatagramPacket(responseData, 516);
            socket.receive(responsePacket);
            
            // Puis décodage de la réponse
            responseBuffer = ByteBuffer.wrap(responseData);
            code = responseBuffer.getShort();
            chunkNumber = responseBuffer.getShort();
            
            if(code == 4 && chunkNumber == 0)
            {
                // Mémorisation du nouveau port
                serverPort = responsePacket.getPort();
                
                // Initialisation des variables de transfert
                readBytes = 0;
                totalBytes = file.length();
                chunkNumber = 1;
                
                do
                {
                    // Lecture d'un morceau du fichier
                    fileData = new byte[512];
                    input.read(fileData);
                    readBytes += fileData.length;
                    dataAcknowledged = false;
                    attemptsNumber = 0;
                    
                    // Changement du timeout pour ne pas attendre indéfiniment
                    socket.setSoTimeout(3000);
                    
                    do
                    {
                        try
                        {
                            // Envoi du morceau de fichier
                            socket.send(TFTPClient.createDATA(chunkNumber, fileData, serverAddress, serverPort));

                            // Attente de l'acquittement
                            responseData = new byte[512];
                            responsePacket = new DatagramPacket(responseData, 512);
                            socket.receive(responsePacket);

                            // Décodage de la réponse
                            responseBuffer = ByteBuffer.wrap(responseData);

                            if(responseBuffer.getShort() == TFTPClient.CODE_ACK)
                            {
                                if(responseBuffer.getShort() == chunkNumber)
                                {
                                    dataAcknowledged = true;
                                }
                                else
                                {
                                    // Erreur d'acquittement, problème de réseau
                                    return -6;
                                }
                            }
                            else
                            {
                                // Une erreur a eu lieu, on extrait le code d'erreur
                                // du serveur
                                return responseBuffer.getShort();
                            }

                            // Incrémentation du nombre de tentatives
                            attemptsNumber++;
                        }
                        catch(SocketTimeoutException e)
                        {
                            dataAcknowledged = false;
                        }
                    }
                    while(!dataAcknowledged && attemptsNumber < 3);
                    
                    // Rétablissement du timeout infini
                    socket.setSoTimeout(0);
                    
                    if(attemptsNumber == 3)
                    {
                        // Trop de tentatives d'envoi du morceau de fichier
                        return -4;
                    }
                    
                    // Incrémentation du numéro de fichier
                    chunkNumber++;
                }
                while(readBytes < totalBytes);
                
                // Fermeture du fichier
                input.close();
                
                // Fermeture du socket
                socket.close();
            }
            else
            {
                // Le serveur n'a pas accepté la demande et a renvoyé une erreur,
                // on l'extrait
                return code;
            }
        }
        catch(FileNotFoundException e)
        {
            // Le fichier n'existe pas
            return -1;
        }
        catch(SocketException e)
        {
            // Impossible de créer le socket
            return -6;
        }
        catch(IOException e)
        {
            // Erreur d'entrée/sortie par rapport à la socket
            return -7;
        }
        
        return 0;
    }
    
    /**
     * Création d'un datagramme TFTP WRQ.
     * 
     * @param remoteFile Chemin vers le fichier distant sur le serveur.
     * @param mode Mode de transfert
     * @param serverAddress Adresse du serveur TFTP.
     * @param serverPort Port du serveur TFTP.
     * @return Datagramme TFTP QRW ou null s'il y a eu une erreur.
     */
    protected static DatagramPacket createWRQ(String remoteFile, String mode, InetAddress serverAddress, int serverPort)
    {
        ByteArrayOutputStream dataStream;
        DataOutputStream dataWriter;
        
        // Manipulateur de tableau d'octets
        dataWriter = new DataOutputStream(dataStream = new ByteArrayOutputStream(4 + remoteFile.length() + mode.length()));
        
        try
        {
            // Ecriture du contenu du paquet
            dataWriter.writeShort(TFTPClient.CODE_WRQ);
            dataWriter.writeBytes(remoteFile);
            dataWriter.writeByte(0);
            dataWriter.writeBytes(mode);
            dataWriter.writeByte(0);
            
            return new DatagramPacket(dataStream.toByteArray(), dataStream.size(), serverAddress, serverPort);
        }
        catch(IOException e)
        {
            Logger.getLogger(TFTPClient.class.getName()).log(Level.SEVERE, e.getMessage(), e);
        }
        
        return null;
    }
    
    /**
     * Création d'un datagramme TFTP DATA.
     * 
     * @param chunkNumber Numéro de bloc de données du fichier.
     * @param fileData Données du fichier à transmettre.
     * @param serverAddress Adresse du serveur TFTP.
     * @param serverPort Port du serveur TFTP.
     * @return Datagramme TFTP DATA ou null s'il y a eu une erreur.
     */
    protected static DatagramPacket createDATA(int chunkNumber, byte[] fileData, InetAddress serverAddress, int serverPort)
    {
        ByteArrayOutputStream dataStream;
        DataOutputStream dataWriter;
        
        // Manipulateur de tableau d'octets
        dataWriter = new DataOutputStream(dataStream = new ByteArrayOutputStream(4 + fileData.length));
        
        try
        {
            // Ecriture du contenu du paquet
            dataWriter.writeShort(TFTPClient.CODE_DATA);
            dataWriter.writeShort(chunkNumber);
            dataWriter.write(fileData);
            
            return new DatagramPacket(dataStream.toByteArray(), dataStream.size(), serverAddress, serverPort);
        }
        catch(IOException e)
        {
            Logger.getLogger(TFTPClient.class.getName()).log(Level.SEVERE, e.getMessage(), e);
        }
        
        return null;
    }
}
