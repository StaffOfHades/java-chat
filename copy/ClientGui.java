import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.lang.IllegalArgumentException;
import java.math.BigInteger;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.*;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.regex.*;

import javax.swing.*;
import javax.swing.text.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.html.*;

public class ClientGui extends Thread {

  final JTextPane jtextFilDiscu = new JTextPane();
  final JTextPane jtextListUsers = new JTextPane();
  final JTextField jtextInputChat = new JTextField();
  private String oldMsg = "";
  private Thread read;
  private String serverName;
  private int PORT;
  private String name;
  private BigInteger publicKey;
  private BigInteger privateKey;
  private BigInteger modulus;
  private BigInteger otherPublicKey;
  private BigInteger otherModulus;
  BufferedReader input;
  PrintWriter output;
  Socket server;

  public ClientGui() {
    this.serverName = "localhost";
    this.PORT = 12345;
    this.name = "nickname";

    String fontfamily = "Arial, sans-serif";
    Font font = new Font(fontfamily, Font.PLAIN, 15);

    final JFrame jfr = new JFrame("Chat");
    jfr.getContentPane().setLayout(null);
    jfr.setSize(700, 500);
    jfr.setResizable(false);
    jfr.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

    // Module du fil de discussion
    jtextFilDiscu.setBounds(25, 25, 490, 320);
    jtextFilDiscu.setFont(font);
    jtextFilDiscu.setMargin(new Insets(6, 6, 6, 6));
    jtextFilDiscu.setEditable(false);
    JScrollPane jtextFilDiscuSP = new JScrollPane(jtextFilDiscu);
    jtextFilDiscuSP.setBounds(25, 25, 490, 320);

    jtextFilDiscu.setContentType("text/html");
    jtextFilDiscu.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true);

    // Module de la liste des utilisateurs
    jtextListUsers.setBounds(520, 25, 156, 320);
    jtextListUsers.setEditable(true);
    jtextListUsers.setFont(font);
    jtextListUsers.setMargin(new Insets(6, 6, 6, 6));
    jtextListUsers.setEditable(false);
    JScrollPane jsplistuser = new JScrollPane(jtextListUsers);
    jsplistuser.setBounds(520, 25, 156, 320);

    jtextListUsers.setContentType("text/html");
    jtextListUsers.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true);

    // Field message user input
    jtextInputChat.setBounds(0, 350, 400, 50);
    jtextInputChat.setFont(font);
    jtextInputChat.setMargin(new Insets(6, 6, 6, 6));
    final JScrollPane jtextInputChatSP = new JScrollPane(jtextInputChat);
    jtextInputChatSP.setBounds(25, 350, 650, 50);

    // button send
    final JButton jsbtn = new JButton("Send");
    jsbtn.setFont(font);
    jsbtn.setBounds(575, 410, 100, 35);

    // button Disconnect
    final JButton jsbtndeco = new JButton("Disconnect");
    jsbtndeco.setFont(font);
    jsbtndeco.setBounds(25, 410, 130, 35);

    jtextInputChat.addKeyListener(new KeyAdapter() {
      // send message on Enter
      public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ENTER) {
          sendMessage();
        }

        // Get last message typed
        if (e.getKeyCode() == KeyEvent.VK_UP) {
          String currentMessage = jtextInputChat.getText().trim();
          jtextInputChat.setText(oldMsg);
          oldMsg = currentMessage;
        }

        if (e.getKeyCode() == KeyEvent.VK_DOWN) {
          String currentMessage = jtextInputChat.getText().trim();
          jtextInputChat.setText(oldMsg);
          oldMsg = currentMessage;
        }
      }
    });

    // Click on send button
    jsbtn.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        sendMessage();
      }
    });

    // Check if file exists
    final Path private_key = Paths.get("chat_rsa");
    final Path public_key = Paths.get("chat_rsa.pub");
    boolean fileExists = Files.exists(private_key) && Files.exists(public_key);

    // Connection view
    final JTextField jtfName = new JTextField(this.name);
    final JTextField jtfport = new JTextField(Integer.toString(this.PORT));
    final JTextField jtfAddr = new JTextField(this.serverName);
    final JButton jkbtn =
      fileExists ?
      new JButton("Load Keys") :
      new JButton("Generate Key");
    final JButton jopbtn = new JButton("Open Key");
    final JButton jcbtn = new JButton("Connect");

    jcbtn.setEnabled(false);

    // check if those field are not empty
    jtfName.getDocument().addDocumentListener(new TextListener(jtfName, jtfport, jtfAddr, jkbtn, jcbtn));
    jtfport.getDocument().addDocumentListener(new TextListener(jtfName, jtfport, jtfAddr, jkbtn, jcbtn));
    jtfAddr.getDocument().addDocumentListener(new TextListener(jtfName, jtfport, jtfAddr, jkbtn, jcbtn));

    // position des Modules
    jkbtn.setFont(font);
    jopbtn.setFont(font);
    jcbtn.setFont(font);
    jtfAddr.setBounds(25, 380, 100, 40);
    jtfport.setBounds(130, 380, 80, 40);
    jtfName.setBounds(215, 380, 120, 40);
    jopbtn.setBounds(340, 380, 100, 40);
    jkbtn.setBounds(445, 380, 125, 40);
    jcbtn.setBounds(575, 380, 90, 40);

    // couleur par defaut des Modules fil de discussion et liste des utilisateurs
    jtextFilDiscu.setBackground(Color.LIGHT_GRAY);
    jtextListUsers.setBackground(Color.LIGHT_GRAY);

    // ajout des éléments
    jfr.add(jcbtn);
    jfr.add(jkbtn);
    jfr.add(jopbtn);
    jfr.add(jtextFilDiscuSP);
    jfr.add(jsplistuser);
    jfr.add(jtfName);
    jfr.add(jtfport);
    jfr.add(jtfAddr);
    jfr.setVisible(true);


    // info sur le Chat
    appendToPane(jtextFilDiscu, "<h4>The commands available for the chat are:</h4>"
        +"<ul>"
        +"<li><b>@nickname</b> to send a Private Message to the user 'nickname'</li>"
        +"<li><b>#d3961b</b> to change the color of your nickname to indicated hexadecimal</li>"
        //+"<li><b>;)</b>, some smileys are implemented</li>"
        +"<li><b>top arrow</b> to select the last message typed</li>"
        +"</ul><br/>");

    final ClientGui that = this;

    jopbtn.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        JFileChooser chooser = new JFileChooser(Paths.get("..").toFile());
        FileNameExtensionFilter filter = new FileNameExtensionFilter(
            "Public Key", "pub");
        chooser.setFileFilter(filter);
        int returnVal = chooser.showOpenDialog(jfr);
        if(returnVal == JFileChooser.APPROVE_OPTION) {
          try {
            Path other_public_key = chooser.getSelectedFile().toPath();
            final byte[] other_public_key_bytes =
              Base64.getDecoder().decode(Files.readAllBytes(other_public_key));
            final String other_public_key_data = new String(other_public_key_bytes);
            that.otherModulus = new BigInteger(other_public_key_data.split(",")[0]);
            that.otherPublicKey = new BigInteger(other_public_key_data.split(",")[1]);
            System.out.println("e: " + that.otherPublicKey.toString());
            System.out.println("n: " + that.otherModulus.toString());
          } catch(IOException i) {
            i.printStackTrace();
          }
        }
      }
    });

    jkbtn.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        boolean fileExists = Files.exists(private_key) && Files.exists(public_key);

        if (fileExists) {
          try {
            final byte[] private_key_bytes =
              Base64.getDecoder().decode(Files.readAllBytes(private_key));
            final byte[] public_key_bytes =
              Base64.getDecoder().decode(Files.readAllBytes(public_key));
            final String private_key_data = new String(private_key_bytes);
            final String public_key_data = new String(public_key_bytes);
            that.modulus = new BigInteger(private_key_data.split(",")[0]);
            that.publicKey = new BigInteger(public_key_data.split(",")[1]);
            that.privateKey = new BigInteger(private_key_data.split(",")[1]);
            System.out.println("e: " + that.publicKey.toString());
            System.out.println("d: " + that.privateKey.toString());
            System.out.println("n: " + that.modulus.toString());
            jkbtn.setEnabled(false);  
            jcbtn.setEnabled(true);
          } catch (IOException i) {
            i.printStackTrace();
            System.exit(1);
          }
        } else {
          String keyGen = null;
          while(keyGen == null || keyGen.length() <= 0) {
            keyGen = (String) JOptionPane.showInputDialog("Phone Number: ", "000-0000");
          }

          SecureRandom random = new SecureRandom(keyGen.getBytes());
          int KEY_SIZE = 512  ;
          BigInteger p = BigInteger.probablePrime(KEY_SIZE / 2, random);
          BigInteger q = BigInteger.probablePrime(KEY_SIZE / 2, random);
          BigInteger n = p.multiply(q);
          BigInteger phi = (p.subtract(BigInteger.ONE)).multiply(q.subtract(BigInteger.ONE));
          BigInteger e = new BigInteger(KEY_SIZE, random);
          System.out.println(n.bitLength());
          while(  n.bitLength() != KEY_SIZE ||
                  e.compareTo(BigInteger.ONE.add(BigInteger.ONE)) < 0 ||
                  e.compareTo(phi.subtract(BigInteger.ONE)) > 0 ||
                  e.gcd(phi).longValueExact() > 1
          ) {
            System.out.println();
            if(n.bitLength() <= KEY_SIZE) {
              System.out.println("Key too small");
            }
            if(e.compareTo(BigInteger.ONE.add(BigInteger.ONE)) < 0 ) {
              System.out.println("e smaller than 2");
            }
            if(e.compareTo(phi.subtract(BigInteger.ONE)) > 0) {
              System.out.println("e bigger than phy");
            }
            if(e.gcd(phi).longValueExact() > 1) {
              System.out.println("gcd between e and phi greater than 1");
            }
            p = BigInteger.probablePrime(KEY_SIZE / 2, random);
            q = BigInteger.probablePrime(KEY_SIZE / 2, random);
            n = p.multiply(q);
            phi = (p.subtract(BigInteger.ONE)).multiply(q.subtract(BigInteger.ONE));
            e = new BigInteger(KEY_SIZE, random);
          }
          BigInteger d = e.modInverse(phi);
          System.out.println("e: " + e.toString());
          System.out.println("d: " + d.toString());
          System.out.println("n: " + n.toString());

          final String publicKey = n + "," + e;
          final String privateKey = n + "," + d;
          final byte[] public_key_bytes = Base64.getEncoder().encode(publicKey.getBytes());
          final byte[] private_key_bytes = Base64.getEncoder().encode(privateKey.getBytes());
          try {
            Files.write(public_key, public_key_bytes);
            Files.write(private_key, private_key_bytes);
          } catch (IOException i) {
            i.printStackTrace();
            System.exit(1);
          }

          jkbtn.setText("Load Key");
        }
      }
    });

    // On connect
    jcbtn.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        try {
          name = jtfName.getText();
          String port = jtfport.getText();
          serverName = jtfAddr.getText();
          PORT = Integer.parseInt(port);

          appendToPane(jtextFilDiscu, "<span>Connecting to " + serverName + " on port " + PORT + "...</span>");
          server = new Socket(serverName, PORT);

          appendToPane(jtextFilDiscu, "<span>Connected to " +
              server.getRemoteSocketAddress()+"</span>");

          input = new BufferedReader(new InputStreamReader(server.getInputStream()));
          output = new PrintWriter(server.getOutputStream(), true);

          // send nickname to server
          output.println(name);

          // create new Read Thread
          read = new Read();
          read.start();
          jfr.remove(jtfName);
          jfr.remove(jtfport);
          jfr.remove(jtfAddr);
          jfr.remove(jopbtn);
          jfr.remove(jkbtn);
          jfr.remove(jcbtn);
          jfr.add(jsbtn);
          jfr.add(jtextInputChatSP);
          jfr.add(jsbtndeco);
          jfr.revalidate();
          jfr.repaint();
          jtextFilDiscu.setBackground(Color.WHITE);
          jtextListUsers.setBackground(Color.WHITE);
        } catch (Exception ex) {
          appendToPane(jtextFilDiscu, "<span>Could not connect to Server</span>");
          JOptionPane.showMessageDialog(jfr, ex.getMessage());
        }
      }

    });

    // on deco
    jsbtndeco.addActionListener(new ActionListener()  {
      public void actionPerformed(ActionEvent ae) {
        jfr.add(jtfName);
        jfr.add(jtfport);
        jfr.add(jtfAddr);
        jfr.add(jopbtn);
        jfr.add(jkbtn);
        jfr.add(jcbtn);
        jfr.remove(jsbtn);
        jfr.remove(jtextInputChatSP);
        jfr.remove(jsbtndeco);
        jfr.revalidate();
        jfr.repaint();
        read.interrupt();
        jtextListUsers.setText(null);
        jtextFilDiscu.setBackground(Color.LIGHT_GRAY);
        jtextListUsers.setBackground(Color.LIGHT_GRAY);
        appendToPane(jtextFilDiscu, "<span>Connection closed.</span>");
        output.close();
      }
    });

  }

  // check if if all field are not empty
  public class TextListener implements DocumentListener {
    JTextField jtf1;
    JTextField jtf2;
    JTextField jtf3;
    JButton jcbtn;
    JButton jkbtn;

    public TextListener(JTextField jtf1, JTextField jtf2, JTextField jtf3, JButton jkbtn,  JButton jcbtn) {
      this.jtf1 = jtf1;
      this.jtf2 = jtf2;
      this.jtf3 = jtf3;
      this.jcbtn = jcbtn;
      this.jkbtn = jkbtn;
    }

    public void changedUpdate(DocumentEvent e) {}

    public void removeUpdate(DocumentEvent e) {
      if(
        jtf1.getText().trim().equals("") ||
        jtf2.getText().trim().equals("") ||
        jtf3.getText().trim().equals("") ||
        jkbtn.isEnabled()
      ) {
        jcbtn.setEnabled(false);
      }else{
        jcbtn.setEnabled(true);
      }
    }
    public void insertUpdate(DocumentEvent e) {
      if(
        jtf1.getText().trim().equals("") ||
        jtf2.getText().trim().equals("") ||
        jtf3.getText().trim().equals("") ||
        jkbtn.isEnabled()
      ) {
        jcbtn.setEnabled(false);
      } else{
        jcbtn.setEnabled(true);
      }
    }
  }

  // envoi des messages
  public void sendMessage() {
    try {
      String message = jtextInputChat.getText().trim();
      if (message.equals("")) {
        return;
      }
      this.oldMsg = message;

      // Private message
      if (message.charAt(0) == '@'){
        if(message.contains(" ")){
          int firstSpace = message.indexOf(' ');
          String head = message.substring(0, firstSpace);
          String userPrivate = message.substring(firstSpace + 1);
          //System.out.println(userPrivate);
          //System.out.println(head);
          userPrivate = encrypt(userPrivate);
          message = head + " " + userPrivate;
        }

      // Color update
      } else if (message.charAt(0) != '#') {
        message = encrypt(message);
      }

      output.println(message);

      jtextInputChat.requestFocus();
      jtextInputChat.setText(null);
    } catch (Exception ex) {
      JOptionPane.showMessageDialog(null, ex.getMessage());
      System.exit(0);
    }
  }

  private String encrypt(String message) {
    if(otherPublicKey == null || otherModulus == null) {
      otherPublicKey = publicKey;
      otherModulus = modulus;
    }
    System.out.println("Encrypting");
    byte[] bytes = Base64.getEncoder().encode(message.getBytes());
    BigInteger encryptedMsg = new BigInteger(bytes);
    System.out.println("p: " + encryptedMsg.toString());
    encryptedMsg = encryptedMsg.modPow(otherPublicKey, otherModulus);
    System.out.println("c: " + encryptedMsg.toString());
    return encryptedMsg.toString();
  }

  private String decrypt(String cipher) {
    System.out.println("Decrypting");
    byte[] input = cipher.getBytes();
    BigInteger decryptedMsg = new BigInteger(cipher);
    System.out.println("c: " + decryptedMsg.toString());
    decryptedMsg = decryptedMsg.modPow(privateKey, modulus);
    System.out.println("p: " + decryptedMsg.toString());
    byte[] bytes = Base64.getDecoder().decode(decryptedMsg.toByteArray());
    return new String(bytes);
  }

  public static void main(String[] args) throws Exception {
    ClientGui client = new ClientGui();
  }

  // read new incoming messages
  class Read extends Thread {
    public void run() {
      String message;
      while(!Thread.currentThread().isInterrupted()){
        try {
          message = input.readLine();
          if(message != null){
            if (message.charAt(0) == '[') {
              message = message.substring(1, message.length()-1);
              ArrayList<String> ListUser = new ArrayList<String>(
                  Arrays.asList(message.split(", "))
                  );
              jtextListUsers.setText(null);
              for (String user : ListUser) {
                appendToPane(jtextListUsers, "@" + user);
              }
            }else{
              System.out.println(message);
              final String copy = message;
              Pattern heading = Pattern.compile(
                "(\\(<b>Private</b>\\))?<u><span style='color:#[a-fA-F0-9]+'>[a-zA-Z0-9! ]+</span></u>( -> \\(<b>[a-zA-Z0-9! ]+</b>\\))?(<span>)?[ :|:]\\ *"
              );
              Matcher matcher = heading.matcher(message);
              if(matcher.find()) {
                String head = matcher.group(0);
                message = message.substring(head.length());
                heading = Pattern.compile("</span>");
                matcher = heading.matcher(message);
                String tail = "";
                if(matcher.find()) {
                  tail = matcher.group(0);
                  message = message.substring(0, message.length() - tail.length());
                }
                try {
                  //System.out.println(message);
                  message = decrypt(message);
                  message = head + message + tail; 
                } catch(IllegalArgumentException e) {
                  System.err.println("Unable to decrypt");
                  message = copy;
                }
              } else {
                  System.out.println("Could not find pattern, message is: " + message);
              }
              appendToPane(jtextFilDiscu, message);
            }
          }
        }
        catch (IOException ex) {
          System.err.println("Failed to parse incoming message");
        }
      }
    }
  }

  // send html to pane
  private void appendToPane(JTextPane tp, String msg){
    HTMLDocument doc = (HTMLDocument)tp.getDocument();
    HTMLEditorKit editorKit = (HTMLEditorKit)tp.getEditorKit();
    try {
      editorKit.insertHTML(doc, doc.getLength(), msg, 0, 0, null);
      tp.setCaretPosition(doc.getLength());
    } catch(Exception e){
      e.printStackTrace();
    }
  }
}
