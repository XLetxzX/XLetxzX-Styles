package de.xletxzx.styles;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JPanel;
import javax.swing.JWindow;
import javax.swing.SwingUtilities;

/** Lyrics in a small see-through window that floats above every app. Drag it anywhere. */
final class LyricsOverlay {
    private static JWindow win;
    private static Panel panel;
    private static volatile String a = "", b = "", c = "";
    private static boolean failed;

    private LyricsOverlay() {}

    static void update(boolean on) {
        if (failed) return;
        try {
            if (!on) { if (win != null) close(); return; }
            if (GraphicsEnvironment.isHeadless()) { failed = true; return; }
            SpotifyService.Info s = SpotifyService.get();
            String st = Lyrics.status();
            String na, nb, nc;
            if (s == null) { na = ""; nb = "No song playing"; nc = ""; }
            else if (!st.isEmpty()) { na = ""; nb = st; nc = ""; }
            else {
                double pos = SpotifyService.positionSec(s);
                na = Lyrics.prev(pos); nb = Lyrics.cur(pos); nc = Lyrics.next(pos);
            }
            boolean changed = !na.equals(a) || !nb.equals(b) || !nc.equals(c);
            a = na; b = nb; c = nc;
            if (win == null) open();
            else if (changed && panel != null) SwingUtilities.invokeLater(() -> panel.repaint());
        } catch (Throwable t) {
            failed = true;
        }
    }

    private static void open() {
        Config.Hud h = Config.get().hud.get("lyrics");
        SwingUtilities.invokeLater(() -> {
            try {
                JWindow w = new JWindow();
                w.setAlwaysOnTop(true);
                w.setFocusableWindowState(false);
                w.setBackground(new Color(0, 0, 0, 0));
                Panel p = new Panel();
                w.setContentPane(p);
                w.setSize(520, 90);
                w.setLocation(h.ox, h.oy);
                MouseAdapter m = new MouseAdapter() {
                    int dx, dy;
                    @Override public void mousePressed(MouseEvent e) { dx = e.getXOnScreen() - w.getX(); dy = e.getYOnScreen() - w.getY(); }
                    @Override public void mouseDragged(MouseEvent e) { w.setLocation(e.getXOnScreen() - dx, e.getYOnScreen() - dy); }
                    @Override public void mouseReleased(MouseEvent e) { h.ox = w.getX(); h.oy = w.getY(); Config.save(); }
                };
                p.addMouseListener(m);
                p.addMouseMotionListener(m);
                w.setVisible(true);
                panel = p;
                win = w;
            } catch (Throwable t) {
                failed = true;
            }
        });
    }

    private static void close() {
        JWindow w = win;
        win = null; panel = null;
        if (w != null) SwingUtilities.invokeLater(w::dispose);
    }

    private static final class Panel extends JPanel {
        Panel() { setOpaque(false); }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setColor(new Color(11, 13, 24, 150));
            g.fillRoundRect(0, 0, getWidth(), getHeight(), 18, 18);
            line(g, a, 24, 15, new Color(255, 255, 255, 120));
            line(g, b, 52, 21, Color.WHITE);
            line(g, c, 78, 15, new Color(255, 255, 255, 120));
            g.dispose();
        }

        private void line(Graphics2D g, String s, int y, int size, Color col) {
            if (s == null || s.isEmpty()) return;
            Font f = new Font("Segoe UI", Font.BOLD, size);
            g.setFont(f);
            FontMetrics fm = g.getFontMetrics();
            while (fm.stringWidth(s) > getWidth() - 20 && s.length() > 4) s = s.substring(0, s.length() - 2);
            g.setColor(col);
            g.drawString(s, (getWidth() - fm.stringWidth(s)) / 2, y);
        }
    }
}
