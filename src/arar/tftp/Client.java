/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template localFile, choose Tools | Templates
 * and open the template in the editor.
 */

package arar.tftp;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author p1310563
 */
public class Client {
    
    // Read request (RRQ)
    protected final static String CODE_RRQ = "1";
    
    // Write request (WRQ)
    protected final static String CODE_WRQ = "2";
    
    // Data (DATA)
    protected final static String CODE_DATA = "3";
    
    // Acknowledgement (ACK)
    protected final static String CODE_ACK = "4";
    
    // Error (ERROR)
    protected final static String CODE_ERROR = "5";
    
    private File fichier;
    private int totalBytes , readBytes, ChunkNumber, attempts ;
    private byte [] data = null;
    private DatagramSocket socket;
    private DatagramPacket packet;
    private boolean acknowledged;
    private int crem = 0 ;
    private String remoteFile;
    
    
    public int sendFile (String remotelFile, String localFile , InetAddress serverAdress ,int ServerPort) {
        
        FileInputStream stream = null;
        this.remoteFile = remoteFile;
        
        
        fichier = new File (localFile);
        String extention =  fichier.getName().substring(fichier.getName().lastIndexOf("."));
        // on vérifier qu'on peut bien utiliser le fichier
        //sinon on renvoie les erreurs correspondantes en local
        
        if (!fichier.exists()) { return -1; }
        
        else if ( !fichier.canExecute()) { return -5;}
        
        if (!fichier.canRead())  { return -2; }
        
        try { 
                stream = new FileInputStream(localFile);
                byte[] buffer = new byte[4];
                DatagramPacket ack;
                if ( stream != null)
                {
                    data = new byte [512];
                    socket = new DatagramSocket();
		    packet = this.WRQ( "octet", serverAdress, ServerPort);
                    socket.send(packet);
                    System.out.println("envoie du packet....");
                    
                    //ensuite on attend le nouveau paquet( ACK )
                    packet = new DatagramPacket(buffer,4);
                    socket.receive(packet); 
                    ByteBuffer buff_code = ByteBuffer.wrap(buffer);
                    byte [] codeOp = new byte[2]; // on récupère l'entête
                    buff_code.get(codeOp);
                    
                    //on recupere ensuite le numero de block
                    ByteBuffer buff_block = buff_code.slice();
                    byte [] block = new byte [2];
                    buff_block.get(block);
                    //suite
                    String c = new String(codeOp, "UTF-8");
                    String b = new String(block, "UTF-8");
                    
                    if ( c.equals(CODE_ACK) && b.equals("0")) {
                    this.ChunkNumber++;
                    this.readBytes= 0;
                    this.totalBytes = localFile.length();
                    
                     do {
                            stream.read(data);
                            this.readBytes+= data.length;
                            this.acknowledged = false;
                            this.attempts=1;
                            
                            do {
                                 this.packet = DATA(this.ChunkNumber,data,serverAdress, ServerPort);
                                 this.socket.send(packet);
                                 
                                 buffer = new byte[4];
                                 this.packet = new DatagramPacket(buffer,4);
                                 socket.receive(packet);
                                 //analyse de ACK
                                 buffer = packet.getData();
                                 buff_code = ByteBuffer.wrap(buffer);
                                 codeOp = new byte[2]; // on récupère l'entête
                                 buff_code.get(codeOp);
                                 c = new String(codeOp, "UTF-8");
                                 //on recupere ensuite le numero de block
                                buff_block = buff_code.slice();
                                block = new byte [2];
                                buff_block.get(block);
                                b = new String(block, "UTF-8");
                              
                                 
                                 if ( c.equals(CODE_ACK))
                                 {
                                     if (this.ChunkNumber == Integer.valueOf(b)) //verifie si c'est le bon block
                                     { 
                                         this.acknowledged = true;
                                     }
                                     else { return -6;} // sinon erreur réseau
                                     
                                         
                                 }
                                 else if ( c.equals(CODE_ERROR))
                                 {
                                     // erreur côté Serveur
                                     return Integer.valueOf(b);
                                 }
                                
                                 this.attempts++;
                                 
                            }
                            while ( this.acknowledged == true || this.attempts ==3);
                            
                            if (this.attempts == 3) { return -4;}
                            this.ChunkNumber++;
                                         
                       }
                    while(!(this.readBytes== this.totalBytes)); // tant qu'on as pas fini de lire
                     stream.close(); // a la fin transfert on ferme le fichier
                     this.socket.close();
                     return 0;
                     }
                    else { /* return the servor error*/
                            buffer = packet.getData();
                            buff_code = ByteBuffer.wrap(buffer);
                            codeOp = new byte[2]; // on récupère l'entête
                            buff_code.get(codeOp);
                             //on recupere le code erreur
                            buff_block = buff_code.slice();
                            block = new byte [2];
                            buff_block.get(block);
                            b = new String(block, "UTF-8");
                             
                             return Integer.valueOf(b);
                          
                          
                    }
                     
                }
                else { return -2;}
                
               
            }
        catch ( FileNotFoundException e)
        {
            return -1;
        } catch (SocketException | UnknownHostException ex) {
            //Si il y a une erreur réseau on renvoit -6
            Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
            crem = -6;
        } 
        catch(SocketTimeoutException e)
                            {
                                this.acknowledged  = false;
                                crem =-6;
                            }
        catch (IOException ex) {
            Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
        }
        return this.crem;
    }
    	//packets
    public DatagramPacket WRQ ( String mode, InetAddress addr,int port )
   {
      ByteArrayOutputStream   BAout;
      DataOutputStream        Dout;

      //*** Create a byte array output stream and write to it through the data output stream methods ***//
      Dout = new DataOutputStream( (BAout = new ByteArrayOutputStream( 4 + this.remoteFile.length() + mode.length() )) );
      try {
         Dout.writeShort( 2 );
         Dout.writeBytes(this.fichier.getName());
         Dout.writeByte( 0 );
         Dout.writeBytes( mode );
         Dout.writeByte( 0 );
      } catch ( IOException x ) { }   // si la taille est correcte il n'y as aucune raison que ça se passe mal

      return( new DatagramPacket( BAout.toByteArray(), BAout.size(), addr, port ) );
   }

   public DatagramPacket ACK( int block, InetAddress addr, int port )
   {
      ByteArrayOutputStream   BAout;
      DataOutputStream        Dout;

      //*** Create a byte array output stream and write to it through the data output stream methods ***//
      Dout = new DataOutputStream( (BAout = new ByteArrayOutputStream( 4 )) );
      try {
            Dout.writeShort( 4 );
            Dout.writeShort( block );
      } catch ( IOException x ) { }   // Should not happen with fixed byte array

      return( new DatagramPacket( BAout.toByteArray(), BAout.size(), addr, port ) );
   }
   
   public DatagramPacket DATA (int block, byte [] data , InetAddress addr, int port)
   {
       ByteArrayOutputStream   BAout;
      DataOutputStream        Dout;

      //*** Create a byte array output stream and write to it through the data output stream methods ***//
      Dout = new DataOutputStream( (BAout = new ByteArrayOutputStream( 4+data.length )) );
      try {
            Dout.writeShort( 3 ); //donnees
            Dout.writeShort(block);
            Dout.write(data);
            
      } catch ( IOException x ) { }   // Should not happen with fixed byte array

      return( new DatagramPacket( BAout.toByteArray(), BAout.size(), addr, port ) );
   }
}
