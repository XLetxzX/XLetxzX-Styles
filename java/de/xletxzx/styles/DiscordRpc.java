package de.xletxzx.styles;

import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/** Minimal Discord Rich Presence over the local Discord pipe (Windows). */
final class DiscordRpc {
    static final String APP_ID = "1570917829278965881";
    private static final long START = System.currentTimeMillis() / 1000L;
    private static volatile boolean enabled;
    private static Thread thread;

    static synchronized void setEnabled(boolean on) {
        if (on == enabled) return;
        enabled = on;
        if (on && thread == null && System.getProperty("os.name", "").toLowerCase().contains("win")) {
            thread = new Thread(DiscordRpc::run, "XLetxzX-Discord");
            thread.setDaemon(true);
            thread.start();
        }
    }

    private static void run() {
        while (true) {
            if (!enabled) { sleep(2000); continue; }
            RandomAccessFile pipe = null;
            for (int i = 0; i < 10 && pipe == null; i++) {
                try { pipe = new RandomAccessFile("\\\\.\\pipe\\discord-ipc-" + i, "rw"); } catch (Exception ignored) {}
            }
            if (pipe == null) { sleep(15000); continue; }
            try {
                write(pipe, 0, "{\"v\":1,\"client_id\":\"" + APP_ID + "\"}");
                read(pipe);
                while (enabled) {
                    write(pipe, 1, activity());
                    read(pipe);
                    sleep(15000);
                }
                write(pipe, 1, clear());
            } catch (Exception ignored) {
            } finally {
                try { pipe.close(); } catch (Exception ignored) {}
            }
            sleep(10000);
        }
    }

    private static String activity() {
        return "{\"cmd\":\"SET_ACTIVITY\",\"args\":{\"pid\":" + ProcessHandle.current().pid()
            + ",\"activity\":{\"details\":\"XLetxzX Styles\",\"state\":\"Minecraft 1.21.11\",\"timestamps\":{\"start\":" + START + "},"
            + "\"assets\":{\"large_image\":\"xletxzx\",\"large_text\":\"XLetxzX Styles\"}}},\"nonce\":\"" + UUID.randomUUID() + "\"}";
    }

    private static String clear() {
        return "{\"cmd\":\"SET_ACTIVITY\",\"args\":{\"pid\":" + ProcessHandle.current().pid() + "},\"nonce\":\"" + UUID.randomUUID() + "\"}";
    }

    private static void write(RandomAccessFile f, int op, String json) throws Exception {
        byte[] b = json.getBytes(StandardCharsets.UTF_8);
        ByteBuffer bb = ByteBuffer.allocate(8 + b.length).order(ByteOrder.LITTLE_ENDIAN);
        bb.putInt(op).putInt(b.length).put(b);
        f.write(bb.array());
    }

    private static void read(RandomAccessFile f) throws Exception {
        byte[] h = new byte[8];
        f.readFully(h);
        int len = ByteBuffer.wrap(h).order(ByteOrder.LITTLE_ENDIAN).getInt(4);
        if (len < 0 || len > 1 << 20) throw new IllegalStateException("bad frame");
        f.readFully(new byte[len]);
    }

    private static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }
}
