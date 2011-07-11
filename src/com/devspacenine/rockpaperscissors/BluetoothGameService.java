package com.devspacenine.rockpaperscissors;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

/**
 * This class does all the work for setting up and managing Bluetooth
 * connections with other devices. It has a thread that listens for
 * incoming connections, a thread for connecting with a device, and a
 * thread for performing data transmissions when connected.
 */
public class BluetoothGameService {

    // Name for the SDP record when creating server socket
    private static final String NAME = "BluetoothGameSecure";

    // Unique UUID for this application
    private static final UUID MY_UUID =
        UUID.fromString("503c3c93-988e-11e0-8d0a-0050c2490048");

    // Member fields
    private final BluetoothAdapter mAdapter;
    private final Handler mHandler;
    private AcceptThread mAcceptThread;
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;
    private int mState;

    // Constants that indicate the current connection state
    public static final int STATE_NONE = 0;       // we're doing nothing
    public static final int STATE_LISTEN = 1;     // now listening for incoming connections
    public static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 3;  // now connected to a remote device

    /**
     * Constructor. Prepares a new BluetoothChat session.
     * @param context  The UI Activity Context
     * @param handler  A Handler to send messages back to the UI Activity
     */
    public BluetoothGameService(Context context, Handler handler, String player_name) {
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        mState = STATE_NONE;
        mHandler = handler;
    }

    /**
     * Set the current state of the chat connection
     * @param state  An integer defining the current connection state
     */
    private void setState(int state) {
        if(state != mState) {
	    	mState = state;
	
	        // Give the new state to the Handler so the UI Activity can update
	        mHandler.obtainMessage(ButtonGame.MESSAGE_STATE_CHANGE, state, -1).sendToTarget();
        }
    }

    /**
     * Return the current connection state. */
    public synchronized int getState() {
        return mState;
    }

    /**
     * Start the game service. Specifically start AcceptThread to begin a
     * session in listening (server) mode. */
    public synchronized void start() {
    	Log.d("DSN Debug", "calling BluetoothGameService.start");

        // Cancel any thread attempting to make a connection
        if (mConnectThread != null) {
        	mConnectThread.cancel();
        	mConnectThread = null;
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
        	mConnectedThread.cancel();
        	mConnectedThread = null;
        }

        // Start the thread to listen on a BluetoothServerSocket
        if (mAcceptThread == null) {
            mAcceptThread = new AcceptThread();
            mAcceptThread.startThread();
        }
        
        setState(STATE_LISTEN);
    }

    /**
     * Start the ConnectThread to initiate a connection to a remote device.
     * @param device  The BluetoothDevice to connect
     */
    public synchronized void connect(BluetoothDevice device) {

        // Cancel any thread attempting to make a connection
        if (mConnectThread != null) {
        	mConnectThread.cancel();
        	mConnectThread = null;
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
        	mConnectedThread.cancel();
        	mConnectedThread = null;
        }

        // Start the thread to connect with the given device
        mConnectThread = new ConnectThread(device);
        mConnectThread.startThread();
        setState(STATE_CONNECTING);
    }

    /**
     * Start the ConnectedThread to begin managing a Bluetooth connection
     * @param socket  The BluetoothSocket on which the connection was made
     * @param device  The BluetoothDevice that has been connected
     */
    public synchronized void connected(BluetoothSocket socket, BluetoothDevice
            device) {

        // Cancel the thread that completed the connection
        if (mConnectThread != null) {
        	mConnectThread.cancel();
        	mConnectThread = null;
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
        	mConnectedThread.cancel();
        	mConnectedThread = null;
        }

        // Cancel the accept thread because we only want to connect to one device
        if (mAcceptThread != null) {
            mAcceptThread.cancel();
            mAcceptThread = null;
        }

        // Start the thread to manage the connection and perform transmissions
        mConnectedThread = new ConnectedThread(socket);
        mConnectedThread.startThread();
        setState(STATE_CONNECTED);

        // Send the name and address of the connected device back to the UI Activity
        Message msg = mHandler.obtainMessage(RockPaperScissorsGame.MESSAGE_CONNECT_SUCCESSFUL);
        Bundle bundle = new Bundle();
        bundle.putString(RockPaperScissorsGame.DEVICE_NAME, device.getName());
        bundle.putString(RockPaperScissorsGame.DEVICE_ADDRESS, device.getAddress());
        msg.setData(bundle);
        mHandler.sendMessage(msg);
    }

    /**
     * Stop all threads
     */
    public synchronized void stop() {
    	Log.d("DSN Debug", "calling BluetoothGameService.stop");

        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        if (mAcceptThread != null) {
            mAcceptThread.cancel();
            mAcceptThread = null;
        }
        setState(STATE_NONE);
    }

    /**
     * Write to the ConnectedThread in an unsynchronized manner
     * @param out The bytes to write
     * @see ConnectedThread#write(byte[])
     */
    public void write(byte[] out) {
        // Create temporary object
        ConnectedThread r;
        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (mState != STATE_CONNECTED) return;
            r = mConnectedThread;
        }
        // Perform the write unsynchronized
        r.write(out);
    }

    /**
     * Indicate that the connection attempt failed and notify the UI Activity.
     */
    private void connectionFailed() {
        // Send a failure message back to the Activity
        mHandler.obtainMessage(RockPaperScissorsGame.MESSAGE_CONNECT_FAILED,
        		"Unable to connect to the remote device").sendToTarget();
    }

    /**
     * Indicate that the connection was lost and notify the UI Activity.
     */
    private void connectionLost() {
        // Send a failure message back to the Activity
        mHandler.obtainMessage(RockPaperScissorsGame.MESSAGE_DISCONNECTED,
        		"Lost connection with opponent's device").sendToTarget();
    }

    /**
     * This thread runs while listening for incoming connections. It behaves
     * like a server-side client. It runs until a connection is accepted
     * (or until cancelled).
     */
    private class AcceptThread extends Thread {
        // The local server socket
        private final BluetoothServerSocket mmServerSocket;
        private volatile boolean running;

        public AcceptThread() {
            BluetoothServerSocket tmp = null;

            // Create a new listening server socket
            try {
            	tmp = mAdapter.listenUsingRfcommWithServiceRecord(NAME, MY_UUID);
            } catch (IOException e) {
            	Log.d("DSN Debug", "Could not listen for bluetooth connect: " + e.getMessage());
            }
            mmServerSocket = tmp;
        }

        public void run() {
            setName("AcceptThread");

            BluetoothSocket socket = null;

            // Listen to the server socket if we're not connected
            while (mState != STATE_CONNECTED && running) {
                try {
                    // This is a blocking call and will only return on a
                    // successful connection or an exception
                    socket = mmServerSocket.accept();
                } catch (IOException e) {
                	Log.d("DSN Debug", "Could not accept remote connection request: " + e.getMessage());
                	running = false;
                	break;
                }

                // If a connection was accepted
                if (socket != null) {
                    synchronized (BluetoothGameService.this) {
                        switch (mState) {
                        case STATE_LISTEN:
                        case STATE_CONNECTING:
                            // Situation normal. Start the connected thread.
                            connected(socket, socket.getRemoteDevice());
                            break;
                        case STATE_NONE:
                        case STATE_CONNECTED:
                            // Either not ready or already connected. Terminate new socket.
                            try {
                                socket.close();
                            } catch (IOException e) {
                            	Log.d("DSN Debug", "Could not close unwanted BluetoothSocket: " + e.getMessage());
                            }
                            running = false;
                            break;
                        }
                    }
                }
            }
        }

        public synchronized void cancel() {
        	running = false;
            try {
                mmServerSocket.close();
            } catch (IOException e) {
            	Log.d("DSN Debug", "Could not close the BluetoothServerSocket: " + e.getMessage());
            }
        }
        
        public synchronized void startThread() {
        	running = true;
        	start();
        }
    }


    /**
     * This thread runs while attempting to make an outgoing connection
     * with a device. It runs straight through; the connection either
     * succeeds or fails.
     */
    private class ConnectThread extends Thread {
        private BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {
            mmDevice = device;
            BluetoothSocket tmp = null;

            // Get a BluetoothSocket for a connection with the
            // given BluetoothDevice
            try {
            	tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) {
            	Log.d("DSN Debug", "Could not create RFCOMM Socket: " + e.getMessage());
            }
            mmSocket = tmp;
        }

        public void run() {
            setName("ConnectThread");

            // Always cancel discovery because it will slow down a connection
            mAdapter.cancelDiscovery();

            // Make a connection to the BluetoothSocket
            try {
                // This is a blocking call and will only return on a
                // successful connection or an exception
                mmSocket.connect();
            } catch (IOException e) {
            	Log.d("DSN Debug", "Could not connect the BluetoothSocket: " + e.getMessage());
            	try {
            		Method m = mmDevice.getClass().getMethod("createRfcommSocket", new Class[] {int.class});
            		BluetoothSocket tmp = null;
            		try {
            			tmp = (BluetoothSocket) m.invoke(mmDevice, 1);
            		} catch (Exception e2) {
            			Log.d("DSN Debug", "Could not create RFCOMM Socket with reflection method: " + e2.getMessage());
            		}
            		mmSocket = tmp;
            		mmSocket.connect();
            	} catch (IOException e2) {
            		Log.d("DSN Debug", "Could not connect the BluetoothSocket with the reflection method (closing socket): " + e2.getMessage());
            		// Close the socket
                    try {
                        mmSocket.close();
                    } catch (IOException e3) {
                    	Log.d("DSN Debug", "Could not close BluetoothSock after Connect Exception: " + e3.getMessage());
                    }
                    connectionFailed();
                    return;
            	} catch (NoSuchMethodException e3) {
            		Log.d("DSN Debug", "BluetoothDevice class does not have createRfcommSocket method (closing socket): " + e3.getMessage());
            		// Close the socket
                    try {
                        mmSocket.close();
                    } catch (IOException e4) {
                    	Log.d("DSN Debug", "Could not close BluetoothSock after Connect Exception: " + e4.getMessage());
                    }
                    connectionFailed();
                    return;
            	}
            }
            
            // Reset the ConnectThread because we're done
            synchronized (BluetoothGameService.this) {
                mConnectThread = null;
            }

            // Start the connected thread
            connected(mmSocket, mmSocket.getRemoteDevice());
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
            	Log.d("DSN Debug", "Could not close the BluetoothSocket on connectThread cancel: " + e.getMessage());
            }
        }
        
        public synchronized void startThread() {
        	start();
        }
    }

    /**
     * This thread runs during a connection with a remote device.
     * It handles all incoming and outgoing transmissions.
     */
    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        private volatile boolean running;

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the BluetoothSocket input and output streams
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
            	Log.d("DSN Debug", "Could not get input/output stream from socket: " + e.getMessage());
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[1024];
            int bytes;

            // Keep listening to the InputStream while connected
            while (running) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);
                    
                    // construct a string from the valid bytes in the buffer and split
                    // it into an array by ":". The first value should be a tag and the second
                    // value should be a message
                    String readString = new String(buffer, 0, bytes);
                    String[] readArray = readString.split(":");
                    String readTag = readArray[0];
                    String readMsg = readArray[1];
                    
                    // Check if the message is a disconnect message
                    if(readTag.equals("Disconnect")) {
	                    if(readMsg.equals("initiate")) {
	                    	// The hosting device has initiated a clean disconnect. Send a
	                    	// disconnect message to the client device and close this thread
	                    	Log.d("DSN Debug", "The hosting device has requested to end the connection");
	                    	String disconnectString = "Disconnect:finish";
	    					write(disconnectString.getBytes());
	                    	running = false;
	                    }
	                    else if(readMsg.equals("finish")) {
	                    	// The client device has received a clean disconnect message. Close
	                    	// this thread and this device's BluetoothSocket
	                    	running = false;
	                    	try {
	                            mmSocket.close();
	                        } catch (IOException e2) {
	                        	Log.d("DSN Debug", "Could not close the BluetoothSocket after input stream failure: " + e2.getMessage());
	                        }
	                    }
                    }
                    else { // The message is not a disconnect message so dispatch it to the device's handler
                    	if(readTag.equals("Play")) {
	                    	mHandler.obtainMessage(RockPaperScissorsGame.MESSAGE_PLAY, readMsg).sendToTarget();
                    	}
                    	else if(readTag.equals("Sync")) {
                    		mHandler.obtainMessage(RockPaperScissorsGame.MESSAGE_SYNC, readMsg).sendToTarget();
                    	}
                    	else if(readTag.equals("Replay")) {
                    		mHandler.obtainMessage(RockPaperScissorsGame.MESSAGE_REPLAY, readMsg).sendToTarget();
                    	}
                    }
                } catch (IOException e) {
                	Log.d("DSN Debug", "Could not read input stream - " + e.getMessage());
                	if(running) {
                		Log.d("DSN Debug", "The connectedThread was still running");
                		running = false;
                		connectionLost();
                		try {
                            mmSocket.close();
                        } catch (IOException e2) {
                        	Log.d("DSN Debug", "Could not close the BluetoothSocket after input stream failure: " + e2.getMessage());
                        }
                	}
                    return;
                }
            }
            // When this blocking call ends the device is ready to disconnect gracefully. Notify
            // the local activity
            mHandler.obtainMessage(ButtonGame.MESSAGE_READY_FOR_DISCONNECT).sendToTarget();
        }

        /**
         * Write to the connected OutStream.
         * @param buffer  The bytes to write
         */
        public void write(byte[] buffer) {
            try {
                mmOutStream.write(buffer);
                // Share the sent message back to the UI Activity
                String writeString = new String(buffer);
                mHandler.obtainMessage(RockPaperScissorsGame.MESSAGE_WRITE, writeString)
                        .sendToTarget();
            } catch (IOException e) {
            	Log.d("DSN Debug", "Could not write buffer to output stream: " + e.getMessage());
            }
        }

        public synchronized void cancel() {
        	Log.d("DSN Debug", "Canceling this device's connectedThread");
        	running = false;
            try {
                mmSocket.close();
            } catch (IOException e) {
            	Log.d("DSN Debug", "Could not close the BluetoothSocket on connectedThread cancel: " + e.getMessage());
            }
        }
        
        public synchronized void startThread() {
        	running = true;
        	start();
        }
    }
}