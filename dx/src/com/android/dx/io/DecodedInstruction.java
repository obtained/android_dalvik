/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.dx.io;

import com.android.dx.dex.DexException;
import com.android.dx.util.Hex;

import java.io.EOFException;

/**
 * A decoded Dalvik instruction. This consists of a format codec, a
 * numeric opcode, an optional index type, and any additional
 * arguments of the instruction. The additional arguments (if any) are
 * represented as uninterpreted data.
 *
 * <p><b>Note:</b> The names of the arguments are <i>not</i> meant to
 * match the names given in the Dalvik instruction format
 * specification, specification which just names fields (somewhat)
 * arbitrarily alphabetically from A. In this class, non-register
 * fields are given descriptive names and register fields are
 * consistently named alphabetically.</p>
 */
public abstract class DecodedInstruction {
    /** non-null; instruction format / codec */
    private final InstructionCodec format;

    /** opcode number */
    private final int opcode;

    /** constant index argument */
    private final int index;

    /** null-ok; index type */
    private final IndexType indexType;

    /**
     * target address argument. This is an absolute address, not just a
     * signed offset.
     */
    private final int target;

    /**
     * literal value argument; also used for special verification error
     * constants (formats 20bc and 40sc) as well as should-be-zero values
     * (formats 10x, 20t, 30t, and 32x)
     */
    private final long literal;

    /** null-ok; literal data */
    private final short[] data;

    /**
     * Decodes an instruction from the given input source.
     */
    public static DecodedInstruction decode(CodeInput in) throws EOFException {
        int opcodeUnit = in.read();
        int opcode = Opcodes.extractOpcodeFromUnit(opcodeUnit);
        InstructionCodec format = OpcodeInfo.getFormat(opcode);

        return format.decode(opcodeUnit, in);
    }

    /**
     * Decodes an array of instructions. The result has non-null
     * elements at each offset that represents the start of an
     * instruction.
     */
    public static DecodedInstruction[] decodeAll(short[] encodedInstructions) {
        int size = encodedInstructions.length;
        DecodedInstruction[] decoded = new DecodedInstruction[size];
        ShortArrayCodeInput in = new ShortArrayCodeInput(encodedInstructions);

        try {
            while (in.hasMore()) {
                decoded[in.cursor()] = DecodedInstruction.decode(in);
            }
        } catch (EOFException ex) {
            throw new AssertionError("shouldn't happen");
        }

        return decoded;
    }

    /**
     * Constructs an instance.
     */
    public DecodedInstruction(InstructionCodec format, int opcode,
            int index, IndexType indexType, int target, long literal,
            short[] data) {
        if (format == null) {
            throw new NullPointerException("format == null");
        }

        if (!Opcodes.isValidShape(opcode)) {
            throw new IllegalArgumentException("invalid opcode");
        }

        this.format = format;
        this.opcode = opcode;
        this.index = index;
        this.indexType = indexType;
        this.target = target;
        this.literal = literal;
        this.data = data;
    }

    public final InstructionCodec getFormat() {
        return format;
    }

    public final int getOpcode() {
        return opcode;
    }

    /**
     * Gets the opcode, as a code unit.
     */
    public final short getOpcodeUnit() {
        return (short) opcode;
    }

    public final int getIndex() {
        return index;
    }

    /**
     * Gets the index, as a code unit.
     */
    public final short getIndexUnit() {
        return (short) index;
    }

    public final IndexType getIndexType() {
        return indexType;
    }

    public final int getTarget() {
        return target;
    }

    /**
     * Gets the target, as a code unit. This will throw if the value is
     * out of the range of a signed code unit.
     */
    public final short getTargetUnit() {
        if (target != (short) target) {
            throw new DexException("Target out of range: " + Hex.s4(target));
        }

        return (short) target;
    }

    /**
     * Gets the target, masked to be a byte in size. This will throw
     * if the value is out of the range of a signed byte.
     */
    public final int getTargetByte() {
        if (target != (byte) target) {
            throw new DexException("Target out of range: " + Hex.s4(target));
        }

        return target & 0xff;
    }

    public final long getLiteral() {
        return literal;
    }

    /**
     * Gets the literal value, masked to be an int in size. This will
     * throw if the value is out of the range of a signed int.
     */
    public final int getLiteralInt() {
        if (literal != (int) target) {
            throw new DexException("Literal out of range: " + Hex.u8(literal));
        }

        return (int) literal;
    }

    /**
     * Gets the literal value, as a code unit. This will throw if the
     * value is out of the range of a signed code unit.
     */
    public final short getLiteralUnit() {
        if (literal != (short) target) {
            throw new DexException("Literal out of range: " + Hex.u8(literal));
        }

        return (short) literal;
    }

    /**
     * Gets the literal value, masked to be a byte in size. This will
     * throw if the value is out of the range of a signed byte.
     */
    public final int getLiteralByte() {
        if (literal != (byte) target) {
            throw new DexException("Literal out of range: " + Hex.u8(literal));
        }

        return (int) literal & 0xff;
    }

    /**
     * Gets the literal value, masked to be a nibble in size. This
     * will throw if the value is out of the range of a signed nibble.
     */
    public final int getLiteralNibble() {
        if ((literal < -8) || (literal > 7)) {
            throw new DexException("Literal out of range: " + Hex.u8(literal));
        }

        return (int) literal & 0xf;
    }

    public final short[] getData() {
        return data;
    }

    public abstract int getRegisterCount();

    public int getA() {
        return 0;
    }

    public int getB() {
        return 0;
    }

    public int getC() {
        return 0;
    }

    public int getD() {
        return 0;
    }

    public int getE() {
        return 0;
    }

    /**
     * Gets the register count, as a code unit. This will throw if the
     * value is out of the range of an unsigned code unit.
     */
    public final short getRegisterCountUnit() {
        int registerCount = getRegisterCount();

        if ((registerCount & ~0xffff) != 0) {
            throw new DexException("Register count out of range: "
                    + Hex.u8(registerCount));
        }

        return (short) registerCount;
    }

    /**
     * Gets the A register number, as a code unit. This will throw if the
     * value is out of the range of an unsigned code unit.
     */
    public final short getAUnit() {
        int a = getA();

        if ((a & ~0xffff) != 0) {
            throw new DexException("Register A out of range: " + Hex.u8(a));
        }

        return (short) a;
    }

    /**
     * Gets the A register number, as a byte. This will throw if the
     * value is out of the range of an unsigned byte.
     */
    public final short getAByte() {
        int a = getA();

        if ((a & ~0xff) != 0) {
            throw new DexException("Register A out of range: " + Hex.u8(a));
        }

        return (short) a;
    }

    /**
     * Gets the A register number, as a nibble. This will throw if the
     * value is out of the range of an unsigned nibble.
     */
    public final short getANibble() {
        int a = getA();

        if ((a & ~0xf) != 0) {
            throw new DexException("Register A out of range: " + Hex.u8(a));
        }

        return (short) a;
    }

    /**
     * Gets the B register number, as a code unit. This will throw if the
     * value is out of the range of an unsigned code unit.
     */
    public final short getBUnit() {
        int b = getB();

        if ((b & ~0xffff) != 0) {
            throw new DexException("Register B out of range: " + Hex.u8(b));
        }

        return (short) b;
    }

    /**
     * Gets the B register number, as a byte. This will throw if the
     * value is out of the range of an unsigned byte.
     */
    public final short getBByte() {
        int b = getB();

        if ((b & ~0xff) != 0) {
            throw new DexException("Register B out of range: " + Hex.u8(b));
        }

        return (short) b;
    }

    /**
     * Gets the B register number, as a nibble. This will throw if the
     * value is out of the range of an unsigned nibble.
     */
    public final short getBNibble() {
        int b = getB();

        if ((b & ~0xf) != 0) {
            throw new DexException("Register B out of range: " + Hex.u8(b));
        }

        return (short) b;
    }

    /**
     * Gets the C register number, as a code unit. This will throw if the
     * value is out of the range of an unsigned code unit.
     */
    public final short getCUnit() {
        int c = getC();

        if ((c & ~0xffff) != 0) {
            throw new DexException("Register C out of range: " + Hex.u8(c));
        }

        return (short) c;
    }

    /**
     * Gets the C register number, as a byte. This will throw if the
     * value is out of the range of an unsigned byte.
     */
    public final short getCByte() {
        int c = getC();

        if ((c & ~0xff) != 0) {
            throw new DexException("Register C out of range: " + Hex.u8(c));
        }

        return (short) c;
    }

    /**
     * Gets the C register number, as a nibble. This will throw if the
     * value is out of the range of an unsigned nibble.
     */
    public final short getCNibble() {
        int c = getC();

        if ((c & ~0xf) != 0) {
            throw new DexException("Register C out of range: " + Hex.u8(c));
        }

        return (short) c;
    }

    /**
     * Gets the D register number, as a code unit. This will throw if the
     * value is out of the range of an unsigned code unit.
     */
    public final short getDUnit() {
        int d = getD();

        if ((d & ~0xffff) != 0) {
            throw new DexException("Register D out of range: " + Hex.u8(d));
        }

        return (short) d;
    }

    /**
     * Gets the D register number, as a byte. This will throw if the
     * value is out of the range of an unsigned byte.
     */
    public final short getDByte() {
        int d = getD();

        if ((d & ~0xff) != 0) {
            throw new DexException("Register D out of range: " + Hex.u8(d));
        }

        return (short) d;
    }

    /**
     * Gets the D register number, as a nibble. This will throw if the
     * value is out of the range of an unsigned nibble.
     */
    public final short getDNibble() {
        int d = getD();

        if ((d & ~0xf) != 0) {
            throw new DexException("Register D out of range: " + Hex.u8(d));
        }

        return (short) d;
    }

    /**
     * Gets the E register number, as a nibble. This will throw if the
     * value is out of the range of an unsigned nibble.
     */
    public final short getENibble() {
        int e = getE();

        if ((e & ~0xf) != 0) {
            throw new DexException("Register E out of range: " + Hex.u8(e));
        }

        return (short) e;
    }

    /**
     * Encodes this instance to the given output.
     */
    public final void encode(CodeOutput out) {
        format.encode(this, out);
    }

    /**
     * Returns an instance just like this one, except with the index replaced
     * with the given one.
     */
    public abstract DecodedInstruction withIndex(int newIndex);
}
