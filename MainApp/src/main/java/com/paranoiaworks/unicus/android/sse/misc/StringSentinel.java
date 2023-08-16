package com.paranoiaworks.unicus.android.sse.misc;

import android.os.SystemClock;

import com.paranoiaworks.unicus.android.sse.utils.CipherProvider;
import com.paranoiaworks.unicus.android.sse.utils.Encryptor;
import com.paranoiaworks.unicus.android.sse.utils.Helpers;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import sse.org.bouncycastle.crypto.modes.SICBlockCipher;

/**
 * Stores texts in operating memory in encrypted form
 *
 * @author Paranoia Works
 * @version 1.0.1
 */
public class StringSentinel implements Serializable, Cloneable, Comparable<StringSentinel> {

    private static final long serialVersionUID = 10L;

    private static Map<Integer, KeyData> keyMap;
    private int groupID;
    transient private byte[] keyStamp;
    transient private byte[] nonce;
    transient private byte[] data;

    static {
        keyMap = new HashMap<Integer, KeyData>();
    }

    public StringSentinel(int groupID)
    {
        this.groupID = groupID;
    }

    public StringSentinel(char[] text, int groupID)
    {
        this.groupID = groupID;
        setString(text);
    }

    public StringSentinel(char[] text, boolean wipe, int groupID)
    {
        this.groupID = groupID;
        setNonce();
        setString(text, wipe);
    }

    public static StringSentinel init(int groupID)
    {
        KeyData keyData = new KeyData(Encryptor.getRandomBA(32));
        KeyData oldKeyData = keyMap.get(groupID);
        keyMap.put(groupID, keyData);
        if(oldKeyData != null) {
            Arrays.fill(oldKeyData.key, (byte) 0);
        }
        return new StringSentinel(groupID);
    }

    public static boolean isInitialized(int groupID)
    {
        return keyMap.get(groupID) != null;
    }

    public void setString(char[] text) throws IllegalStateException
    {
        setString(text, true);
    }

    public void setString(char[] text, boolean wipe) throws IllegalStateException
    {
        byte[] inputData = toBytes(text);
        KeyData keyData = keyMap.get(groupID);
        if(keyData == null) throw new IllegalStateException("Group ID does not exist");
        setNonce();
        SICBlockCipher cipher = CipherProvider.getCTRCipher(true, nonce, keyData.key, 0, 0);
        cipher.processBytes(inputData, 0, inputData.length, inputData, 0);
        this.keyStamp = keyData.keyStamp;
        this.data = inputData;
        if(wipe) Arrays.fill(text, '\u0000');
    }

    public char[] getString()
    {
        if(data == null) return "".toCharArray();
        byte[] outputData = new byte[data.length];
        KeyData keyData = keyMap.get(groupID);
        if(keyData == null) throw new Error("Group ID does not exist");
        if(!Arrays.equals(this.keyStamp, keyData.keyStamp)) throw new Error("Incorrect KeyStamp");
        SICBlockCipher cipher = CipherProvider.getCTRCipher(false, nonce, keyData.key, 0, 0);
        cipher.processBytes(data, 0, data.length, outputData, 0);

        char[] outputString = toChars(outputData);
        Arrays.fill(outputData, (byte) 0);

        return outputString;
    }

    public byte[] getEncryptedData()
    {
        return data;
    }

    public static void destroy(int groupID) throws IllegalStateException
    {
        KeyData keyData = keyMap.get(groupID);
        if(keyData != null) {
            keyMap.remove(groupID);
            Arrays.fill(keyData.key, (byte) 0);
        }
    }

    public int compareTo(StringSentinel stringSentinel)
    {
        return compareDestructively(this.getString(), stringSentinel.getString(), false);
    }

    public int searchIndexOf(char[] searchFor)
    {
        return searchIndexOf(searchFor, false);
    }

    public int searchIndexOf(char[] searchFor, boolean sourceCaseSensitive)
    {
        char[] source = getString();
        int index = Helpers.indexOf(source, 0, source.length, searchFor, 0, searchFor.length, 0, sourceCaseSensitive);
        Arrays.fill(source, '\u0000');
        return index;
    }

    @Override
    public boolean equals(Object object) {
        return compareDestructively(this.getString(), ((StringSentinel)object).getString(), true) == 0 ? true : false;
    }

    @Override
    public Object clone() throws CloneNotSupportedException
    {
        Object obj = super.clone();
        StringSentinel ssc = (StringSentinel)obj;

        ssc.keyStamp = Helpers.getByteArrayCopy(this.keyStamp);
        ssc.nonce = Helpers.getByteArrayCopy(this.nonce);
        ssc.data = Helpers.getByteArrayCopy(this.data);

        return ssc;
    }

    @Override
    public String toString()  {
        return "DO NOT USE !!!";
    }

    private void setNonce()
    {
        this.nonce = Encryptor.getBlake2Hash(
                (System.identityHashCode(this) + " " + SystemClock.uptimeMillis()).getBytes(), 128);
    }

    private static byte[] toBytes(char[] chars)
    {
        ByteBuffer byteBuffer = Charset.forName("UTF-8").encode(CharBuffer.wrap(chars));
        byte[] bytes = Arrays.copyOfRange(byteBuffer.array(),
                byteBuffer.position(), byteBuffer.limit());
        Arrays.fill(byteBuffer.array(), (byte) 0);
        return bytes;
    }

    private static char[] toChars(byte[] data)
    {
        CharBuffer charBuffer = Charset.forName("UTF-8").decode(ByteBuffer.wrap(data));
        char[] bytes = Arrays.copyOfRange(charBuffer.array(),
                charBuffer.position(), charBuffer.limit());
        Arrays.fill(charBuffer.array(), '\u0000');
        return bytes;
    }

    private static int compareDestructively(char[] a, char[] b, boolean caseSensitive)
    {
        int comp = 0;
        int length = a.length < b.length ? a.length : b.length;
        for(int i = 0; i < length; ++i)
        {
            if(!caseSensitive) {
                a[i] = Character.toLowerCase(a[i]);
                b[i] = Character.toLowerCase(b[i]);
            }
            comp = Character.compare(a[i], b[i]);
            if(comp != 0) break;
        }
        if(comp == 0 && a.length != b.length) comp = a.length > b.length ? 1 : -1;
        Arrays.fill(a, '\u0000');
        Arrays.fill(b, '\u0000');
        return comp;
    }

    private void readObject(ObjectInputStream ois) throws Exception
    {
        ois.defaultReadObject();
        char[] pt = (char[])ois.readObject();
        setNonce();
        setString(pt);
    }

    private void writeObject(ObjectOutputStream oos) throws Exception
    {
        oos.defaultWriteObject();
        char[] pt = getString();
        oos.writeObject(pt);
        Arrays.fill(pt, '\u0000');
    }

    private static class KeyData
    {
        private byte[] key;
        private byte[] keyStamp;

        private KeyData(byte[] key) {
            this.key = key;
            this.keyStamp = Encryptor.getBlake2Hash(key, 128);
        }
    }
}
