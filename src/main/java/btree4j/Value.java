/*
 * Copyright (c) 2006 and onwards Makoto Yui
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package btree4j;

import btree4j.utils.io.FastByteArrayInputStream;
import btree4j.utils.lang.HashUtils;
import btree4j.utils.lang.Primitives;
import btree4j.utils.lang.StringUtils;

import java.io.Externalizable;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.OutputStream;

public class Value implements Comparable<Value>, Cloneable, Externalizable {
    private static final long serialVersionUID = -8649821046511401335L;

    private int _hash = -1;
    protected/* final */byte[] _data; // immutable
    protected/* final */int _pos;
    protected/* final */int _len;

    private transient int refcnt = 0;

    // for Externalizable
    public Value() {}

    public Value(String data) {
        this(StringUtils.getBytes(data));
    }

    public Value(byte[] data) {
        this(data, 0, data.length);
    }

    public Value(byte[] data, int len) {
        this(data, 0, len);
    }

    public Value(byte[] data, int pos, int len) {
        if (data == null) {
            throw new IllegalArgumentException();
        }
        this._data = data;
        this._pos = pos;
        this._len = len;
    }

    public Value(long data) {
        this(Primitives.toBytes(data));
    }

    /**
     * getData retrieves the data being stored by the Value as a byte array.
     */
    public byte[] getData() {
        if (_len != _data.length) {
            byte[] b = new byte[_len];
            System.arraycopy(_data, _pos, b, 0, _len);
            return b;
        } else {
            return _data;
        }
    }

    public int getPosition() {
        return _pos;
    }

    /**
     * getLength retrieves the length of the data being stored by the Value.
     */
    public final int getLength() {
        return _len;
    }

    /**
     * getInputStream returns an InputStream for the Value.
     */
    public final InputStream getInputStream() {
        return new FastByteArrayInputStream(_data, _pos, _len);
    }

    /**
     * streamTo streams the content of the Value to an OutputStream.
     */
    public final void writeTo(OutputStream out) throws IOException {
        out.write(_data, _pos, _len);
    }

    public final void writeTo(OutputStream out, int offset, int len) throws IOException {
        out.write(_data, _pos + offset, len);
    }

    public final void copyTo(byte[] tdata, int tpos) {
        System.arraycopy(_data, _pos, tdata, tpos, _len);
    }

    public final void copyTo(byte[] toValue, int toPos, int len) {
        System.arraycopy(_data, _pos, toValue, toPos, len);
    }

    @Override
    public int compareTo(Value value) {
        byte[] ddata = value.getData();
        int dpos = value.getPosition();
        int dlen = value.getLength();
        int stop = Math.min(_len, dlen);
        for (int i = 0; i < stop; i++) {
            int s1 = _data[_pos + i] & 0xFF;
            int s2 = ddata[dpos + i] & 0xFF;
            if (s1 != s2) {
                return s1 > s2 ? (i + 1) : -(i + 1);
            }
        }
        if (_len == dlen) {
            return 0;
        } else {
            return _len > dlen ? stop + 1 : -(stop + 1);
        }
    }

    public boolean equals(Value value) {
        return _len == value._len ? compareTo(value) == 0 : false;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof Value) {
            return equals((Value) obj);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        if (_hash != -1) {
            return _hash;
        }
        int h = HashUtils.hashCode(_data, _pos, _len);
        this._hash = h;
        return h;
    }

    @Override
    public String toString() {
        char[] ch = Primitives.toChars(_data, _pos, _len);
        return String.valueOf(ch);
    }

    public final boolean startsWith(Value value) {
        int vlen = value.getLength();
        if (_len < vlen) {
            return false;
        }
        byte[] ddata = value.getData();
        int dpos = value.getPosition();
        for (int i = 0; i < vlen; i++) {
            if (_data[i + _pos] != ddata[i + dpos]) {
                return false;
            }
        }
        return true;
    }

    @Override
    public Value clone() {
        return new Value(_data, _pos, _len);
    }

    public final int incrRefCount() {
        return ++refcnt;
    }

    public final int decrRefCount() {
        return --refcnt;
    }

    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeInt(_hash);
        out.writeInt(_len);
        out.write(_data, _pos, _len);
    }

    public int size() {
        return 8 + _len;
    }

    public void readExternal(ObjectInput in) throws IOException {
        this._hash = in.readInt();
        final int len = in.readInt();
        final byte[] b = new byte[len];
        in.read(b);
        this._data = b;
        this._pos = 0;
        this._len = len;
    }

    public static Value readObject(final ObjectInput in) throws IOException {
        final Value v = new Value();
        v.readExternal(in);
        return v;
    }
}
