package com.mylibrary.xmpp;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.cabily.utils.SessionManager;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.ReconnectionManager;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.chat.ChatManager;
import org.jivesoftware.smack.chat.ChatManagerListener;
import org.jivesoftware.smack.chat.ChatMessageListener;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.jivesoftware.smackx.receipts.DeliveryReceipt;
import org.jivesoftware.smackx.receipts.DeliveryReceiptManager;
import org.jivesoftware.smackx.receipts.DeliveryReceiptRequest;
import org.jivesoftware.smackx.receipts.ReceiptReceivedListener;
import org.jxmpp.jid.DomainBareJid;
import org.jxmpp.jid.EntityJid;
import org.jxmpp.jid.Jid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.stringprep.XmppStringprepException;

import java.io.IOException;
import java.util.HashMap;
import java.util.Random;


/**
 * Created by Prem Kumar and Anitha on 12/14/2016.
 */

public class MyXMPP {

    public static boolean connected = false;
    public boolean loggedin = false;
    public static boolean isconnecting = false;
    public static boolean isToasted = true;
    public static boolean chat_created = false;
    private String serviceName = "", hostAddress = "";
    public static XMPPTCPConnection connection;
    public static String loginUser;
    public static String passwordUser;
    Context context;
    public static MyXMPP instance = null;
    public static boolean instanceCreated = false;
    private static ChatHandler chatHandler;
    static boolean isChatEnabled;
    private String TAG = "MyXMPP DeliveryManage";


    public MyXMPP(Context context, String mServiceName, String mHostAddress, String loginUser, String passwordUser) {
        this.serviceName = mServiceName;
        this.hostAddress = mHostAddress;
        this.loginUser = loginUser;
        this.passwordUser = passwordUser;
        this.context = context;
        init();
    }

    public static MyXMPP getInstance(Context context, String mServiceName, String mHostAddress, String user, String pass) {

        if (instance == null) {
            instance = new MyXMPP(context, mServiceName, mHostAddress, user, pass);
            instanceCreated = true;
        }
        return instance;

    }

    public org.jivesoftware.smack.chat.Chat Mychat;

    ChatManagerListenerImpl mChatManagerListener;
    MMessageListener mMessageListener;

    String text = "";
    String mMessage = "", mReceiver = "";

    static {
        try {
            Class.forName("org.jivesoftware.smack.ReconnectionManager");
        } catch (ClassNotFoundException ex) {
            // problem loading reconnection manager
        }
    }

    public void init() {
        mMessageListener = new MMessageListener(context);
        mChatManagerListener = new ChatManagerListenerImpl();
        initialiseConnection();
    }

    private void initialiseConnection() {
        DomainBareJid serviceNames = null;
        try {
            serviceNames = JidCreate.domainBareFrom(serviceName);
        } catch (XmppStringprepException e) {
            e.printStackTrace();
        }

        ProviderManager.addExtensionProvider(DeliveryReceipt.ELEMENT, DeliveryReceipt.NAMESPACE, new DeliveryReceipt.Provider());
        ProviderManager.addExtensionProvider(DeliveryReceiptRequest.ELEMENT, new DeliveryReceiptRequest().getNamespace(), new DeliveryReceiptRequest.Provider());

        XMPPTCPConnectionConfiguration.Builder config = XMPPTCPConnectionConfiguration.builder();
        config.setSecurityMode(ConnectionConfiguration.SecurityMode.disabled);
        config.setServiceName(serviceNames);
        config.setHost(hostAddress);
        config.setDebuggerEnabled(true);
        XMPPTCPConnection.setUseStreamManagementResumptiodDefault(true);
        XMPPTCPConnection.setUseStreamManagementDefault(true);
        connection = new XMPPTCPConnection(config.build());
        XMPPConnectionListener connectionListener = new XMPPConnectionListener();
        connection.addConnectionListener(connectionListener);


    }

    public void disconnect() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                connection.disconnect();
            }
        }).start();
    }

    public void connect(final String caller) {

        @SuppressLint("StaticFieldLeak") AsyncTask<Void, Void, Boolean> connectionThread = new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected synchronized Boolean doInBackground(Void... arg0) {
                if (connection.isConnected())
                    return false;
                isconnecting = true;
                if (isToasted)
                    new Handler(Looper.getMainLooper()).post(new Runnable() {

                        @Override
                        public void run() {

                           /* Toast.makeText(context,
                                    caller + "=>conneccting....",
                                    Toast.LENGTH_LONG).show();*/
                            System.out.println("-----------------xmpp conneccting-----------------------");
                        }
                    });
                Log.d("Connect() Function", caller + "=>connecting....");

                try {
                    connection.connect();

                    ReconnectionManager reconnectionManager = ReconnectionManager.getInstanceFor(connection);
                    reconnectionManager.setEnabledPerDefault(false);
                    reconnectionManager.enableAutomaticReconnection();
                    /*PingManager pingManager = PingManager.getInstanceFor(connection);
                    pingManager.setPingInterval(300);*/

  /*                  DeliveryReceiptManager.getInstanceFor(connection)
                            .addReceiptReceivedListener(new ReceiptReceivedListener() {
                                @Override
                                public void onReceiptReceived(Jid fromJid, Jid toJid, String receiptId, Stanza receipt) {
                                    Log.d(TAG, String.valueOf(fromJid));
                                    Log.d(TAG, String.valueOf(toJid));
                                    Log.d(TAG, "PACKED GOT--" + receiptId);
                                }

                            });*/
              /*      DeliveryReceiptManager dm = DeliveryReceiptManager
                            .getInstanceFor(connection);
                    dm.setAutoReceiptMode(AutoReceiptMode.always);
                    dm.addReceiptReceivedListener(new ReceiptReceivedListener() {

                        @Override
                        public void onReceiptReceived(Jid fromJid, Jid toJid, String receiptId, Stanza receipt) {
                            System.out.println("-----------------DeliveryReceiptManager onReceiptReceived------:"+fromJid+","+toJid+","+receiptId+","+receipt);
                        }

                    });*/
                    connected = true;

                } catch (IOException e) {
                    if (isToasted)
                        new Handler(Looper.getMainLooper())
                                .post(new Runnable() {

                                    @Override
                                    public void run() {

                                       /* Toast.makeText(
                                                context,
                                                "(" + caller + ")"
                                                        + "IOException: ",
                                                Toast.LENGTH_SHORT).show();*/
                                    }
                                });

                    Log.e("(" + caller + ")", "IOException: " + e.getMessage());
                } catch (SmackException e) {
                    new Handler(Looper.getMainLooper()).post(new Runnable() {

                        @Override
                        public void run() {
                           /* Toast.makeText(context,
                                    "(" + caller + ")" + "SMACKException: ",
                                    Toast.LENGTH_SHORT).show();*/
                        }
                    });
                    Log.e("(" + caller + ")",
                            "SMACKException: " + e.getMessage());
                } catch (XMPPException e) {
                    if (isToasted)

                        new Handler(Looper.getMainLooper())
                                .post(new Runnable() {

                                    @Override
                                    public void run() {

                                        /*Toast.makeText(
                                                context,
                                                "(" + caller + ")"
                                                        + "XMPPException: ",
                                                Toast.LENGTH_SHORT).show();*/
                                    }
                                });
                    Log.e("connect(" + caller + ")",
                            "XMPPException: " + e.getMessage());

                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e("connect(" + caller + ")",
                            "EXCEPTION: " + e.getMessage());
                }
                return isconnecting = false;
            }
        };
        connectionThread.execute();
    }

    public void login() {

        try {
            SessionManager sessionManager = new SessionManager(context);
            HashMap<String, String> user = sessionManager.getUserDetails();
            loginUser = user.get(SessionManager.KEY_USERID);
            passwordUser = user.get(SessionManager.KEY_XMPP_SEC_KEY);
            connection.login(loginUser, passwordUser);

            DeliveryReceiptManager.getInstanceFor(connection)
                    .addReceiptReceivedListener(new ReceiptReceivedListener() {
                        @Override
                        public void onReceiptReceived(Jid fromJid, Jid toJid, String receiptId, Stanza receipt) {
                            Log.d(TAG, String.valueOf(fromJid));
                            Log.d(TAG, String.valueOf(toJid));
                            Log.d(TAG, "PACKED GOT--" + receiptId);
                        }

                    });
            Log.i("LOGIN", "Yey! We're connected to the Xmpp server!");

        } catch (XMPPException | SmackException | IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
        }

    }

    private class ChatManagerListenerImpl implements ChatManagerListener {
        @Override
        public void chatCreated(final org.jivesoftware.smack.chat.Chat chat,
                                final boolean createdLocally) {
            if (!createdLocally)
                chat.addMessageListener(mMessageListener);

        }

    }

    public int sendMessage(String senderID, String mMessage) {
        EntityJid entityJid = null;
        try {
            entityJid = JidCreate.entityBareFrom(senderID);
        } catch (XmppStringprepException e) {
            e.printStackTrace();
        }
        if (!chat_created) {
            Mychat = ChatManager.getInstanceFor(connection).createChat(entityJid, mMessageListener);
            chat_created = true;
        }
        final Message message = new Message();
        message.setBody(mMessage);
        message.setStanzaId(String.format("%02d", new Random().nextInt(1000)));
        message.setType(Message.Type.chat);

        try {
            if (connection.isAuthenticated()) {
                Mychat.sendMessage(message);
                return 1;
            } else {
                login();
                return 0;
            }
        } catch (NotConnectedException e) {
            Log.e("xmpp.SendMessage()", "msg Not sent!-Not Connected!");
            return 0;
        } catch (Exception e) {
            Log.e("xmpp Message Exception", "msg Not sent!" + e.getMessage());
            return 0;

        }

    }

    public class XMPPConnectionListener implements ConnectionListener {
        @Override
        public void connected(final XMPPConnection connection) {
            System.out.println("-------------xmpp Connection connected!----------------");
            Log.d("xmpp", "Connected!");
            connected = true;
            if (!connection.isAuthenticated()) {
                login();
            }
        }

        @Override
        public void connectionClosed() {
            if (isToasted)

                new Handler(Looper.getMainLooper()).post(new Runnable() {

                    @Override
                    public void run() {
                        // TODO Auto-generated method stub

                       /* Toast.makeText(context, "ConnectionCLosed!",
                                Toast.LENGTH_SHORT).show();*/

                    }
                });
            Log.d("xmpp", "ConnectionCLosed!");

            System.out.println("-------------xmpp ConnectionCLosed!----------------");

            connected = false;
            chat_created = false;
            loggedin = false;
        }

        @Override
        public void connectionClosedOnError(Exception arg0) {
            if (isToasted)

                new Handler(Looper.getMainLooper()).post(new Runnable() {

                    @Override
                    public void run() {
                        /*Toast.makeText(context, "ConnectionClosedOn Error!!",
                                Toast.LENGTH_SHORT).show();*/

                    }
                });
            Log.d("xmpp", "ConnectionClosedOn Error!");
            connected = false;

            chat_created = false;
            loggedin = false;
        }

        @Override
        public void reconnectingIn(int arg0) {

            Log.d("xmpp", "Reconnectingin " + arg0);

            System.out.println("----------xmpp Reconnectingin----------------" + arg0);

            loggedin = false;
        }

        @Override
        public void reconnectionFailed(Exception arg0) {
            if (isToasted)

                new Handler(Looper.getMainLooper()).post(new Runnable() {

                    @Override
                    public void run() {

                        /*Toast.makeText(context, "ReconnectionFailed!",
                                Toast.LENGTH_SHORT).show();*/
                        connect("");

                    }
                });
            Log.d("xmpp", "ReconnectionFailed!");
            connected = false;

            chat_created = false;
            loggedin = false;
        }

        @Override
        public void reconnectionSuccessful() {
            if (isToasted)

                new Handler(Looper.getMainLooper()).post(new Runnable() {

                    @Override
                    public void run() {
                        // TODO Auto-generated method stub

                       /* Toast.makeText(context, "REConnected!",
                                Toast.LENGTH_SHORT).show();*/

                    }
                });
            Log.d("xmpp", "ReconnectionSuccessful");
            connected = true;

            chat_created = false;
            loggedin = false;
        }

        @Override
        public void authenticated(XMPPConnection arg0, boolean arg1) {
            Log.d("xmpp", "Authenticated!");
            loggedin = true;

            ChatManager.getInstanceFor(connection).addChatListener(
                    mChatManagerListener);

            chat_created = false;
            new Thread(new Runnable() {

                @Override
                public void run() {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }

                }
            }).start();
            if (isToasted)

                new Handler(Looper.getMainLooper()).post(new Runnable() {

                    @Override
                    public void run() {
                        // TODO Auto-generated method stub

                       /* Toast.makeText(context, "Connected!",
                                Toast.LENGTH_SHORT).show();*/

                    }
                });
        }
    }

    private class MMessageListener implements ChatMessageListener {

        public MMessageListener(Context contxt) {
        }

        @Override
        public void processMessage(final org.jivesoftware.smack.chat.Chat chat,
                                   final Message message) {
            Log.i("MyXMPP_MESSAGE_LISTENER", "Xmpp message received: '"
                    + message);

            if (message.getType() == Message.Type.chat
                    && message.getBody() != null) {
                System.out.println("-----------xmpp message-------------" + message.getBody());

                try {
                    if (chatHandler == null) {
                        chatHandler = new ChatHandler(context);
                    }
                    chatHandler.onHandleChatMessage(message);
                } catch (Exception e) {
                }
            }
        }

    }

    public static void enableChat() {
        isChatEnabled = true;
    }

    public static void disableChat() {
        isChatEnabled = false;
    }

}
