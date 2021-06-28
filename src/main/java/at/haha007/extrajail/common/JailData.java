package at.haha007.extrajail.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.*;
import java.util.UUID;

@AllArgsConstructor
@Getter
public class JailData {
    private final UUID uuid;
    private final int blocks;

    public static JailData decode(byte[] bytes) {
        try (ByteArrayInputStream b = new ByteArrayInputStream(bytes);
             DataInputStream in = new DataInputStream(b)) {
            long msb = in.readLong();
            long lsb = in.readLong();
            int blocks = in.readInt();
            UUID uuid = new UUID(msb, lsb);
            return new JailData(uuid, blocks);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public byte[] encode() {
        try (ByteArrayOutputStream b = new ByteArrayOutputStream();
             DataOutputStream out = new DataOutputStream(b)) {
            out.writeLong(uuid.getMostSignificantBits());
            out.writeLong(uuid.getLeastSignificantBits());
            out.writeInt(blocks);
            return b.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
