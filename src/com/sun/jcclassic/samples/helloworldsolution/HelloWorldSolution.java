/**
 * Copyright (c) 1998, 2015, Oracle and/or its affiliates. All rights reserved.
 * 
 */

/*
 */

// /*
// Workfile:@(#)HelloWorld.java	1.7
// Version:1.7
// Date:01/03/06
//
// Archive:  /Products/Europa/samples/com/sun/javacard/samples/HelloWorld/HelloWorld.java
// Modified:01/03/06 12:13:08 
// Original author:  Mitch Butler
// */
package com.sun.jcclassic.samples.helloworldsolution;

import javacard.framework.APDU;
import javacard.framework.Applet;
import javacard.framework.ISO7816;
import javacard.framework.ISOException;
import javacard.framework.JCSystem;
import javacard.framework.Util;

/**
 */

public class HelloWorldSolution extends Applet {
	
    private byte[] echoBytes; // echoBytes reference in NVM, OK :)
    private static final short LENGTH_ECHO_BYTES = 256; // Constant in NVM, OK :)

    /**
     * Only this class's install method should create the applet object. 
     */
    protected HelloWorldSolution() {
    	
    	// All objects are allocated at installation time, in the main constructor, OK :)
    	echoBytes = JCSystem.makeTransientByteArray(LENGTH_ECHO_BYTES, JCSystem.MEMORY_TYPE_TRANSIENT_DESELECT); 
	// echoBytes content in RAM, OK :) Will be cleaned on APP deselection
	
        register(); //The applet now has an AID known by the JCRE and the OS
    }

    /**
     * Installs this applet.
     *
     * @param bArray
     *            the array containing installation parameters
     * @param bOffset
     *            the starting offset in bArray
     * @param bLength
     *            the length in bytes of the parameter data in bArray
     */
    public static void install(byte[] bArray, short bOffset, byte bLength) {
        new HelloWorldSolution(); //Just call the main constructor to instantiate the App
    }

    /**
     * Processes an incoming APDU.
     *
     * @see APDU
     * @param apdu
     *            the incoming APDU
     * @exception ISOException
     *                with the response bytes per ISO 7816-4
     */
   public void process(APDU apdu) {
	   
        // ---------  C-APDU Part  ------------

	   
	//Get the APDU Buffer array (the full byte array sent by the script)
        byte buffer[] = apdu.getBuffer();

        // check SELECT APDU command, ignore it to give it back to the JCRE (Selection/Deselection mechanism)
        if ((buffer[ISO7816.OFFSET_CLA] == 0) &&
                (buffer[ISO7816.OFFSET_INS] == (byte) (0xA4))) {
            return;
        }
	   
	// Check the INCOMIN DATA LENGTH
        // Assuming this command has incoming data
        // Lc tells us the incoming apdu command length
        short lc = (short) (buffer[ISO7816.OFFSET_LC] & 0x00FF);
        if (lc != (short)5) ISOException.throwIt( ISO7816.SW_WRONG_LENGTH );

        short bytesRead = apdu.setIncomingAndReceive(); // Get the Max APDU DATA possible from the buffer : 
        // -> 7 bytes of data taken after the 5 bytes header (Considering we sent 7 bytes of Data in the test script)
        
        short echoOffset = (short) 0; //Offset to keep track of where we are when reading the buffer array. Cast all possible integer to short to save space !!

        // (APDU is now in state STATE_PARTIAL_INCOMING, we must use "receiveBytes" method to parse the data until all is read, and count the data length.)
        
        while (bytesRead > 0) { // Loop to parse all the data and compute the data length dynamically at the same time
            Util.arrayCopyNonAtomic(buffer, ISO7816.OFFSET_CDATA, echoBytes, echoOffset, bytesRead); 
	    // We have to copy the data if we want to manipulate them because references to the APDU Buffer are forbidden (from Javadoc)
            echoOffset += bytesRead; // Keep track of the offset
            bytesRead = apdu.receiveBytes(ISO7816.OFFSET_CDATA); //Get the remaining Data if their is some. All data is read when bytesRead=0
        }
        
        // (bytesRead == 0 ,  APDU is now in state STATE_FULL_INCOMING, we can start sending back the R-APDU)

        // ---------  R-APDU Part  ------------
        
        apdu.setOutgoing(); //Tell the JCRE that we are now sending back the APDU
        apdu.setOutgoingLength((short) (echoOffset + 5)); // Tell the JCRE the Length of the Data we will send back : Data length + Header size

        // echo header
        apdu.sendBytes((short) 0, (short) 5); // We send back the first 5 bytes of the buffer : the header.
	   
	// Could have used directly 
	// apdu.setOutgoingAndSend((short) 0, (short)  5);
	// instead of the lines 108 109 and 112
        
	// If we did not copy the data into a separated array, we could send everything back using the computed length : apdu.sendBytes((short) 0, (short) (echoOffset + 5));
        // echo data
        apdu.sendBytesLong(echoBytes, (short) 0, echoOffset); // Since we copied the data into echoBytes, we send it back using sendBytesLong, since the simple sendBytes allows to send back only from the buffer.
        
	// Status Word : force 9000 status is not mandatory since it is the default when everything goes OK !
        ISOException.throwIt(ISO7816.SW_NO_ERROR); // 9000 status
        
    }

}
