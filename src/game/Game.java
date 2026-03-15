/*
 * FistingStrike.
 * Copyright (C) 2025-2026  Saturn Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package game;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

public class Game extends JFrame {
    State currentState = State.MENU;
    Map<String, Integer> binds = new LinkedHashMap<>();
    String waitingForKey = null;
    Color playerColor = Color.BLUE;
    Color menuBg1 = new Color(80, 0, 0);
    Color menuBg2 = new Color(40, 0, 0);
    int volume = 50;
    boolean isFullscreen = true;
    boolean showFPS = true;
    int fps = 0;
    long lastFpsTime = 0;
    int frameCount = 0;
    String selectedMap = "de_pesok", selectedWeapon = "AK-47", playerSide = "CT";
    int hp = 100, ammo = 30, maxAmmo = 30, ctScore = 0, tScore = 0;
    int freezeTime = 5, roundTime = 150, autoNextTimer = -1, dildoTimer = -1;
    double px = 100, py = 100;
    long lastShotTime = 0;
    boolean reloading = false;
    long reloadEndTime = 0;
    String winnerText = "";
    float playerAngle = 0;
    List<Enemy> enemies = new ArrayList<>();
    List<Projectile> projectiles = new ArrayList<>();
    List<Wall> walls = new ArrayList<>();
    List<Particle> particles = new ArrayList<>();
    Set<Integer> activeKeys = new HashSet<>();
    Random rnd = new Random();
    Color uiBgColor = new Color(0, 0, 0, 180);
    Color uiAccentColor = new Color(255, 215, 0);
    Color uiTextColor = Color.WHITE;
    private JPanel canvas;
    int mouseX = 0, mouseY = 0;
    String selectedGlasses = "NONE";

    public Game() {
        setTitle("FistingStrike: Saturn Inc.");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        binds.put("UP", KeyEvent.VK_W);
        binds.put("DOWN", KeyEvent.VK_S);
        binds.put("LEFT", KeyEvent.VK_A);
        binds.put("RIGHT", KeyEvent.VK_D);
        binds.put("RELOAD", KeyEvent.VK_R);
        binds.put("SURRENDER", KeyEvent.VK_P);
        canvas = new JPanel() {
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                if (currentState == State.MENU) drawMenu(g2);
                else if (currentState == State.SETUP) drawSetup(g2);
                else if (currentState == State.SETTINGS) drawSettings(g2);
                else if (currentState == State.CUSTOMIZE) drawCustomize(g2);
                else {
                    drawGame(g2);
                    if (currentState == State.PAUSE) drawPauseMenu(g2);
                }
                if (showFPS && currentState != State.MENU) {
                    frameCount++;
                    long currentTime = System.currentTimeMillis();
                    if (currentTime - lastFpsTime >= 1000) {
                        fps = frameCount;
                        frameCount = 0;
                        lastFpsTime = currentTime;
                    }
                    g2.setColor(Color.GREEN);
                    g2.setFont(new Font("Monospaced", Font.PLAIN, 12));
                    g2.drawString("FPS: " + fps, getWidth() - 70, 20);
                }
            }
        };
        canvas.setFocusable(true);
        canvas.requestFocusInWindow();
        canvas.addMouseWheelListener(e -> {
            if (currentState == State.SETTINGS) {
                volume = Math.max(0, Math.min(100, volume - e.getWheelRotation() * 5));
            }
        });
        canvas.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                int keyCode = e.getKeyCode();
                if (keyCode == KeyEvent.VK_ESCAPE) {
                    if (currentState == State.GAME) {
                        currentState = State.PAUSE;
                    } else if (currentState == State.PAUSE) {
                        currentState = State.GAME;
                    } else if (currentState == State.SETTINGS || currentState == State.CUSTOMIZE) {
                        currentState = State.MENU;
                    } else if (currentState == State.SETUP) {
                        currentState = State.MENU;
                    }
                    return;
                }
                if (waitingForKey != null) {
                    binds.put(waitingForKey, keyCode);
                    waitingForKey = null;
                    return;
                }
                activeKeys.add(keyCode);
                if (keyCode == binds.get("RELOAD")) reload();
                if (keyCode == binds.get("SURRENDER") && currentState == State.GAME) {
                    if (playerSide.equals("CT")) tScore++;
                    else ctScore++;
                    winnerText = (playerSide.equals("CT") ? "TERRORISTS" : "CT") + " WIN (SURRENDER)";
                    autoNextTimer = 4;
                }
            }
            public void keyReleased(KeyEvent e) {
                activeKeys.remove(e.getKeyCode());
            }
        });
        canvas.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                if (currentState == State.GAME && freezeTime <= 0 && winnerText.isEmpty() && dildoTimer == -1) {
                    shoot(e.getX(), e.getY());
                } else {
                    handleClicks(e.getX(), e.getY());
                }
            }
        });
        canvas.addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseMoved(MouseEvent e) {
                mouseX = e.getX();
                mouseY = e.getY();
                if (currentState == State.GAME) {
                    double dx = e.getX() - (px + 15);
                    double dy = e.getY() - (py + 15);
                    playerAngle = (float) Math.atan2(dy, dx);
                }
            }
        });
        new javax.swing.Timer(16, e -> { update(); canvas.repaint(); }).start();
        new javax.swing.Timer(1000, e -> {
            if (currentState == State.GAME) {
                if (freezeTime > 0) freezeTime--;
                else if (roundTime > 0 && winnerText.isEmpty()) roundTime--;
                if (dildoTimer > 0) dildoTimer--;
                else if (dildoTimer == 0) activateDildoPower();
                if (autoNextTimer > 0) autoNextTimer--;
                else if (autoNextTimer == 0) {
                    if (ctScore >= 5 || tScore >= 5) {
                        currentState = State.MENU;
                        ctScore = 0;
                        tScore = 0;
                    } else {
                        resetRound();
                    }
                    autoNextTimer = -1;
                }
            }
        }).start();
        add(canvas);
        applyWindowMode();
    }

    void applyWindowMode() {
        dispose();
        setUndecorated(isFullscreen);
        if (isFullscreen) {
            setExtendedState(JFrame.MAXIMIZED_BOTH);
        } else {
            setExtendedState(JFrame.NORMAL);
            setSize(1280, 720);
            setLocationRelativeTo(null);
        }
        if (canvas != null) {
            add(canvas);
            canvas.requestFocusInWindow();
        }
        setVisible(true);
    }

    void update() {
        if (currentState != State.GAME) return;
        if (freezeTime > 0 && winnerText.isEmpty()) return;
        if (reloading && System.currentTimeMillis() > reloadEndTime) {
            reloading = false;
            ammo = maxAmmo;
        }
        double ox = px, oy = py;
        if (activeKeys.contains(binds.get("UP"))) py -= 6;
        if (activeKeys.contains(binds.get("DOWN"))) py += 6;
        if (activeKeys.contains(binds.get("LEFT"))) px -= 6;
        if (activeKeys.contains(binds.get("RIGHT"))) px += 6;
        int hudHeight = 105;
        px = Math.max(0, Math.min(getWidth() - 30, px));
        py = Math.max(0, Math.min(getHeight() - hudHeight - 30, py));
        Rectangle pRect = new Rectangle((int) px, (int) py, 30, 30);
        for (Wall w : walls) {
            if (pRect.intersects(w.rect)) {
                px = ox;
                py = oy;
                break;
            }
        }
        Iterator<Projectile> it = projectiles.iterator();
        while (it.hasNext()) {
            Projectile p = it.next();
            p.update();
            if (p.life <= 0 || p.x < 0 || p.x > getWidth() || p.y < 0 || p.y > getHeight()) {
                it.remove();
                continue;
            }
            boolean hitWall = false;
            for (Wall w : walls) {
                if (w.rect.intersects((int)p.x - 4, (int)p.y - 4, 9, 9)) {
                    hitWall = true;
                    break;
                }
            }
            if (hitWall) {
                it.remove();
                continue;
            }
            if (p.isPlayer) {
                for (Enemy en : enemies) {
                    if (en.hp > 0 && new Rectangle(en.x, en.y, 30, 30).contains((int) p.x, (int) p.y)) {
                        en.hp -= 34;
                        p.life = 0;
                        for (int i = 0; i < 8; i++) particles.add(new Particle(p.x, p.y, Color.RED));
                        break;
                    }
                }
            } else if (pRect.contains((int) p.x, (int) p.y)) {
                hp -= 10;
                p.life = 0;
                for (int i = 0; i < 8; i++) particles.add(new Particle(p.x, p.y, Color.RED));
                if (hp <= 0 && winnerText.isEmpty()) {
                    if (playerSide.equals("CT")) tScore++;
                    else ctScore++;
                    winnerText = (playerSide.equals("CT") ? "TERRORISTS" : "CT") + " WIN";
                    autoNextTimer = 4;
                }
            }
        }
        Iterator<Particle> particleIt = particles.iterator();
        while (particleIt.hasNext()) {
            Particle p = particleIt.next();
            p.update();
            if (p.life <= 0) particleIt.remove();
        }
        if (winnerText.isEmpty()) {
            for (Enemy en : enemies) {
                if (en.hp > 0) {
                    en.update(px, py, projectiles, walls);
                }
            }
            if (!enemies.isEmpty() && enemies.stream().allMatch(en -> en.hp <= 0)) {
                if (playerSide.equals("CT")) ctScore++;
                else tScore++;
                winnerText = (playerSide.equals("CT") ? "CT" : "TERRORISTS") + " WIN";
                autoNextTimer = 4;
                for (int i = 0; i < 30; i++) {
                    particles.add(new Particle(px + 15, py + 15, Color.YELLOW));
                }
            }
        }
    }

    void shoot(int mx, int my) {
        if (reloading || (ammo <= 0 && !selectedWeapon.equals("BAYONET") && !selectedWeapon.equals("Dildo"))) return;
        long now = System.currentTimeMillis();
        if (selectedWeapon.equals("Dildo")) {
            if (ammo <= 0) return;
            ammo--;
            dildoTimer = 2;
            DildoProjectile dildo = new DildoProjectile((int) px + 15, (int) py + 15, mx, my, true, playerColor);
            dildo.setSpeed(7.5);
            dildo.setScale(1.6f);
            projectiles.add(dildo);
            for (int i = 0; i < 12; i++) particles.add(new Particle(px + 15, py + 15, Color.PINK));
            return;
        }
        if (selectedWeapon.equals("BAYONET")) {
            if (now - lastShotTime > 450) {
                lastShotTime = now;
                for (Enemy en : enemies) {
                    double dist = Math.hypot(en.x + 15 - (px + 15), en.y + 15 - (py + 15));
                    if (dist < 55) {
                        en.hp -= 55;
                        for (int i = 0; i < 15; i++) particles.add(new Particle(en.x + 15, en.y + 15, Color.RED));
                    }
                }
            }
            return;
        }
        int rate = selectedWeapon.equals("AWP") ? 1350 : 110;
        if (now - lastShotTime > rate) {
            lastShotTime = now;
            ammo--;
            projectiles.add(new Projectile((int) px + 15, (int) py + 15, mx, my, true, Color.YELLOW));
            for (int i = 0; i < 5; i++) particles.add(new Particle(px + 15, py + 15, Color.ORANGE));
        }
    }

    void reload() {
        if (reloading || ammo == maxAmmo || selectedWeapon.equals("BAYONET") || selectedWeapon.equals("Dildo")) return;
        reloading = true;
        reloadEndTime = System.currentTimeMillis() + 1850;
    }

    void drawMenu(Graphics2D g) {
        int w = getWidth(), h = getHeight();
        GradientPaint gradient = new GradientPaint(0, 0, menuBg1, w, h, menuBg2);
        g.setPaint(gradient);
        g.fillRect(0, 0, w, h);
        for (int i = 0; i < 7; i++) {
            int x = (int) (Math.sin(System.currentTimeMillis() * 0.0012 + i) * 130 + w / 2);
            int y = (int) (Math.cos(System.currentTimeMillis() * 0.0012 + i * 0.7) * 110 + h / 3);
            g.setColor(new Color(255, 215, 0, 55));
            g.fillOval(x - 6, y - 6, 12, 12);
        }
        g.setColor(uiAccentColor);
        g.setFont(new Font("Impact", Font.PLAIN, h / 8));
        drawCenteredString(g, "FISTING STRIKE", w / 2, h / 4);
        g.setColor(Color.GRAY);
        g.setFont(new Font("Monospaced", Font.BOLD, h / 45));
        g.drawString("Alpha 1.0 by Saturn Inc.", w - 250, 35);
        String[] btns = {"PLAY", "SETTINGS", "CUSTOMIZE", "QUIT"};
        g.setFont(new Font("Arial", Font.BOLD, h / 25));
        Point mousePos = getMousePosition();
        int mouseY = mousePos != null ? mousePos.y : -1;
        for (int i = 0; i < btns.length; i++) {
            int buttonY = h / 2 + i * (h / 12);
            boolean hover = mouseY > buttonY - h / 24 && mouseY < buttonY + h / 24;
            g.setColor(hover ? uiAccentColor : Color.WHITE);
            g.setFont(new Font("Arial", Font.BOLD, hover ? h / 24 : h / 25));
            drawCenteredString(g, btns[i], w / 2, buttonY);
        }
    }

    void drawSetup(Graphics2D g) {
        int w = getWidth(), h = getHeight();
        g.setColor(new Color(20, 20, 20));
        g.fillRect(0, 0, w, h);
        g.setFont(new Font("Arial", Font.BOLD, h / 30));
        g.setColor(Color.WHITE);
        g.drawString("TEAM:", w / 10, h / 8);
        g.setColor(playerSide.equals("CT") ? uiAccentColor : Color.GRAY);
        g.setFont(new Font("Arial", Font.BOLD, playerSide.equals("CT") ? h / 25 : h / 30));
        g.drawString("CT", w / 4, h / 8);
        g.setColor(playerSide.equals("T") ? uiAccentColor : Color.GRAY);
        g.setFont(new Font("Arial", Font.BOLD, playerSide.equals("T") ? h / 25 : h / 30));
        g.drawString("T", (int) (w / 3.0), h / 8);
        g.setFont(new Font("Arial", Font.BOLD, h / 30));
        g.setColor(Color.WHITE);
        g.drawString("MAP:", w / 10, h / 4);
        g.setColor(selectedMap.equals("de_pesok") ? uiAccentColor : Color.GRAY);
        g.setFont(new Font("Arial", Font.BOLD, selectedMap.equals("de_pesok") ? h / 25 : h / 30));
        g.drawString("de_pesok", w / 4, h / 4);
        g.setColor(selectedMap.equals("de_rebus") ? uiAccentColor : Color.GRAY);
        g.setFont(new Font("Arial", Font.BOLD, selectedMap.equals("de_rebus") ? h / 25 : h / 30));
        g.drawString("de_rebus", w / 2, h / 4);
        g.setFont(new Font("Arial", Font.BOLD, h / 30));
        g.setColor(Color.WHITE);
        g.drawString("GUN:", w / 10, (int) (h / 2.5));
        String[] gs = {"AK-47", "AWP", "GLOCK", "USP-S", "BAYONET", "Dildo"};
        for (int i = 0; i < gs.length; i++) {
            boolean sel = selectedWeapon.equals(gs[i]);
            g.setColor(sel ? uiAccentColor : Color.GRAY);
            g.setFont(new Font("Arial", Font.BOLD, sel ? h / 25 : h / 30));
            g.drawString(gs[i], (int) (w / 5.0 + i * (w / 10.0)), (int) (h / 2.5));
        }
        int btnX = w / 2 - w / 8;
        int btnY = (int) (h / 1.3);
        int btnW = w / 4;
        int btnH = h / 12;
        Point mousePos = getMousePosition();
        boolean hover = mousePos != null && mousePos.x >= btnX && mousePos.x <= btnX + btnW &&
                mousePos.y >= btnY && mousePos.y <= btnY + btnH;
        g.setColor(hover ? uiAccentColor.brighter() : uiAccentColor);
        g.fillRoundRect(btnX, btnY, btnW, btnH, 20, 20);
        g.setColor(Color.BLACK);
        g.setFont(new Font("Impact", Font.BOLD, h / 20));
        drawCenteredString(g, "START GAME", w / 2, btnY + btnH / 2 + 10);
        g.setColor(new Color(180, 180, 180));
        g.setFont(new Font("Arial", Font.PLAIN, h / 45));
        drawCenteredString(g, "Выбери сторону, карту и оружие!", w / 2, btnY + btnH + 25);
    }

    void drawSettings(Graphics2D g) {
        int w = getWidth(), h = getHeight();
        g.setColor(new Color(30, 30, 30));
        g.fillRect(0, 0, w, h);
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, h / 35));
        int i = 0;
        for (String k : binds.keySet()) {
            String t = k + ": " + KeyEvent.getKeyText(binds.get(k));
            if (k.equals(waitingForKey)) t += " <...>";
            Point mousePos = getMousePosition();
            boolean hover = mousePos != null && mousePos.x < w / 3 &&
                    mousePos.y > h / 10 + i * (h / 20) - 15 && mousePos.y < h / 10 + i * (h / 20) + 15;
            g.setColor(hover ? uiAccentColor : Color.WHITE);
            g.drawString(t, w / 10, h / 10 + i * (h / 20));
            i++;
        }
        g.setColor(Color.WHITE);
        g.drawString("Volume: " + volume + "%", w / 10, (int) (h / 1.8));
        g.setColor(Color.DARK_GRAY);
        g.fillRect(w / 4, (int) (h / 1.8) - 15, 200, 20);
        g.setColor(uiAccentColor);
        g.fillRect(w / 4, (int) (h / 1.8) - 15, volume * 2, 20);
        g.setColor(Color.WHITE);
        g.drawString("Mode: " + (isFullscreen ? "Fullscreen" : "Windowed"), w / 10, (int) (h / 1.6));
        g.setColor(uiAccentColor);
        g.drawString("BACK", w / 10, (int) (h / 1.2));
    }

    void drawCustomize(Graphics2D g) {
        int w = getWidth(), h = getHeight();
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, w, h);
        Color[] cs = {Color.RED, Color.GREEN, Color.YELLOW, Color.BLACK, Color.WHITE, Color.BLUE, Color.PINK, new Color(255, 105, 180)};
        String[] colorNames = {"RED", "GREEN", "YELLOW", "BLACK", "WHITE", "BLUE", "PINK", "HOT PINK"};
        g.setFont(new Font("Arial", Font.BOLD, h / 25));
        g.setColor(Color.WHITE);
        drawCenteredString(g, "PICK YOUR CUBE COLOR", w / 2, h / 5);
        g.setColor(playerColor);
        g.fillRect(w / 2 - 60, h / 4, 120, 40);
        g.setColor(Color.WHITE);
        g.drawRect(w / 2 - 60, h / 4, 120, 40);
        for (int i = 0; i < cs.length; i++) {
            g.setColor(cs[i]);
            int rectW = w / 15;
            int x = (int) (w / 6.0 + i * (w / 10.0));
            int y = h / 3;
            g.fillRect(x, y, rectW, rectW);
            if (playerColor.equals(cs[i])) {
                g.setColor(Color.WHITE);
                g.setStroke(new BasicStroke(4));
                g.drawRect(x - 6, y - 6, rectW + 12, rectW + 12);
            }
        }
        g.setColor(Color.WHITE);
        drawCenteredString(g, "MENU BACKGROUND", w / 2, (int) (h * 0.55));
        for (int i = 0; i < cs.length; i++) {
            g.setColor(cs[i]);
            int rectW = w / 15;
            int x = (int) (w / 6.0 + i * (w / 10.0));
            int y = (int) (h * 0.6);
            g.fillRect(x, y, rectW, rectW);
            if (menuBg1.equals(cs[i])) {
                g.setColor(Color.WHITE);
                g.setStroke(new BasicStroke(4));
                g.drawRect(x - 6, y - 6, rectW + 12, rectW + 12);
            }
        }
        g.setColor(Color.WHITE);
        drawCenteredString(g, "GLASSES", w / 2, (int) (h * 0.75));
        String[] glasses = {"NONE", "CLASSIC", "COOL", "CYBER"};
        for (int i = 0; i < glasses.length; i++) {
            boolean sel = selectedGlasses.equals(glasses[i]);
            g.setColor(sel ? uiAccentColor : Color.GRAY);
            g.setFont(new Font("Arial", Font.BOLD, sel ? h / 25 : h / 30));
            g.drawString(glasses[i], (int) (w / 5.0 + i * (w / 8.0)), (int) (h * 0.82));
        }
        g.setColor(uiAccentColor);
        g.setFont(new Font("Arial", Font.BOLD, h / 30));
        drawCenteredString(g, "BACK", w / 2, (int) (h / 1.15));
    }

    void drawGame(Graphics2D g) {
        int w = getWidth(), h = getHeight();
        Color bgColor1 = selectedMap.equals("de_pesok") ? new Color(210, 180, 140) : new Color(100, 100, 110);
        Color bgColor2 = selectedMap.equals("de_pesok") ? new Color(160, 130, 90) : new Color(70, 70, 80);
        g.setPaint(new GradientPaint(0, 0, bgColor1, w, h, bgColor2));
        g.fillRect(0, 0, w, h);
        for (Wall wll : walls) {
            Color wallColor = selectedMap.equals("de_pesok") ? new Color(100, 60, 20) : Color.GRAY;
            g.setColor(wallColor);
            g.fill(wll.rect);
            g.setColor(wallColor.darker());
            g.setStroke(new BasicStroke(3));
            g.draw(wll.rect);
        }
        for (Particle p : particles) p.draw(g);
        for (Enemy en : enemies) {
            if (en.hp > 0) {
                g.setColor(playerSide.equals("CT") ? Color.RED : Color.BLUE);
                g.fillRect(en.x, en.y, 30, 30);
                g.setColor(Color.RED);
                g.fillRect(en.x, en.y - 12, 30, 6);
                g.setColor(Color.GREEN);
                g.fillRect(en.x, en.y - 12, (int) (30 * (en.hp / 100.0)), 6);
                g.setColor(Color.WHITE);
                int eyeX = en.dirX > 0 ? en.x + 21 : en.x + 8;
                g.fillOval(eyeX, en.y + 7, 6, 6);
            }
        }
        for (Projectile p : projectiles) {
            if (p instanceof DildoProjectile) {
                ((DildoProjectile) p).draw(g);
            } else {
                g.setColor(new Color(p.color.getRed(), p.color.getGreen(), p.color.getBlue(), 110));
                g.fillOval((int) p.x - 3, (int) p.y - 3, 12, 12);
                g.setColor(p.color);
                g.fillOval((int) p.x, (int) p.y, 7, 7);
            }
        }
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.translate(px + 15, py + 15);
        g2d.rotate(playerAngle);
        g2d.setColor(playerSide.equals("CT") ? playerColor : Color.RED);
        g2d.fillRect(-15, -15, 30, 30);
        g2d.setColor(Color.DARK_GRAY);
        g2d.fillRect(12, -4, 22, 7);
        if (!selectedGlasses.equals("NONE")) {
            g2d.setColor(Color.BLACK);
            if (selectedGlasses.equals("CLASSIC")) {
                g2d.fillRect(-12, -8, 24, 6);
                g2d.fillOval(-10, -10, 8, 12);
                g2d.fillOval(2, -10, 8, 12);
            } else if (selectedGlasses.equals("COOL")) {
                g2d.fillRect(-14, -9, 28, 8);
                g2d.fillOval(-12, -12, 10, 14);
                g2d.fillOval(2, -12, 10, 14);
                g2d.setColor(new Color(200, 0, 0));
                g2d.fillOval(-10, -8, 6, 6);
                g2d.fillOval(4, -8, 6, 6);
            } else if (selectedGlasses.equals("CYBER")) {
                g2d.setColor(new Color(0, 255, 255));
                g2d.fillRect(-15, -10, 30, 4);
                g2d.fillRect(-12, -6, 24, 4);
                g2d.fillOval(-10, -12, 8, 12);
                g2d.fillOval(2, -12, 8, 12);
            }
        }
        g2d.dispose();
        if (currentState == State.GAME) {
            g.setColor(new Color(255, 255, 255, 180));
            g.setStroke(new BasicStroke(1.5f));
            g.drawOval(mouseX - 8, mouseY - 8, 16, 16);
            g.drawLine(mouseX - 14, mouseY, mouseX + 14, mouseY);
            g.drawLine(mouseX, mouseY - 14, mouseX, mouseY + 14);
        }
        int cx = w / 2;
        g.setColor(uiBgColor);
        g.fillRoundRect(cx - 160, 8, 320, 68, 12, 12);
        g.setColor(Color.RED);
        g.setFont(new Font("Arial", Font.BOLD, 42));
        g.drawString(tScore + "", cx - 135, 57);
        g.setColor(Color.CYAN);
        g.drawString(ctScore + "", cx + 95, 57);
        g.setColor(uiAccentColor);
        g.setFont(new Font("Monospaced", Font.BOLD, 36));
        g.drawString(String.format("%02d:%02d", roundTime / 60, roundTime % 60), cx - 58, 55);
        g.setColor(uiBgColor);
        g.fillRect(0, h - 105, w, 105);
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, h / 38));
        g.drawString("HP:", w / 22, h - 48);
        g.setColor(Color.RED);
        g.fillRect(w / 22 + 48, h - 68, 220, 22);
        g.setColor(Color.GREEN);
        g.fillRect(w / 22 + 48, h - 68, hp * 2, 22);
        g.setColor(Color.WHITE);
        String ammoText = selectedWeapon.equals("BAYONET") ? "∞" :
                selectedWeapon.equals("Dildo") ? ammo + "/1" : ammo + "/" + maxAmmo;
        g.drawString(selectedWeapon + ": " + ammoText, w / 22 + 290, h - 48);
        if (reloading) {
            g.setColor(uiAccentColor);
            g.drawString("RELOADING...", w / 22 + 460, h - 48);
        }
        if (freezeTime > 0 && winnerText.isEmpty()) {
            g.setFont(new Font("Impact", Font.PLAIN, h / 8));
            g.setColor(new Color(255, 255, 0, 190));
            drawCenteredString(g, "00:0" + freezeTime, cx, h / 2);
        }
        if (!winnerText.isEmpty()) {
            g.setColor(new Color(255, 215, 0, 220));
            g.setFont(new Font("Impact", Font.PLAIN, h / 11));
            drawCenteredString(g, winnerText, cx, h / 2);
            g.setColor(new Color(0, 0, 0, 120));
            drawCenteredString(g, winnerText, cx + 4, h / 2 + 4);
        }
    }

    void drawPauseMenu(Graphics2D g) {
        int w = getWidth(), h = getHeight();
        g.setColor(new Color(0, 0, 0, 200));
        g.fillRect(0, 0, w, h);
        g.setColor(uiAccentColor);
        g.setFont(new Font("Arial", Font.BOLD, h / 10));
        drawCenteredString(g, "PAUSE", w / 2, h / 4);
        String[] items = {"Продолжить игру", "Главное меню", "Выйти из игры"};
        g.setFont(new Font("Arial", Font.BOLD, h / 20));
        Point mousePos = getMousePosition();
        int mouseY = mousePos != null ? mousePos.y : -1;
        for (int i = 0; i < items.length; i++) {
            int y = h / 2 + i * (h / 10);
            boolean hover = mouseY > y - 30 && mouseY < y + 30;
            if (hover) {
                g.setColor(new Color(uiAccentColor.getRed(), uiAccentColor.getGreen(), uiAccentColor.getBlue(), 50));
                g.fillRoundRect(w / 2 - 200, y - 30, 400, 60, 20, 20);
            }
            g.setColor(hover ? uiAccentColor : Color.WHITE);
            g.setFont(new Font("Arial", Font.BOLD, hover ? h / 18 : h / 20));
            drawCenteredString(g, items[i], w / 2, y);
        }
        g.setColor(Color.GRAY);
        g.setFont(new Font("Arial", Font.PLAIN, h / 40));
        drawCenteredString(g, "ESC - вернуться в игру", w / 2, (int) (h * 0.85));
    }

    void handleClicks(int x, int y) {
        int w = getWidth(), h = getHeight();
        if (currentState == State.MENU) {
            int buttonY = h / 2;
            int buttonHeight = h / 12;
            for (int i = 0; i < 4; i++) {
                int buttonTop = buttonY + i * buttonHeight - buttonHeight / 2;
                if (y >= buttonTop && y <= buttonTop + buttonHeight) {
                    if (i == 0) currentState = State.SETUP;
                    else if (i == 1) currentState = State.SETTINGS;
                    else if (i == 2) currentState = State.CUSTOMIZE;
                    else if (i == 3) System.exit(0);
                    break;
                }
            }
        } else if (currentState == State.SETUP) {
            if (y > h / 14 && y < h / 7) {
                if (x < w / 2.8) playerSide = "CT";
                else if (x < w / 2.2) playerSide = "T";
            }
            if (y > h / 5 && y < h / 3.5) {
                if (x < w / 2.5) selectedMap = "de_pesok";
                else if (x < w / 1.8) selectedMap = "de_rebus";
            }
            if (y > h / 2.8 && y < h / 2.2) {
                int idx = (int) ((x - w / 5.0) / (w / 10.0));
                if (idx >= 0 && idx < 6) {
                    selectedWeapon = new String[]{"AK-47", "AWP", "GLOCK", "USP-S", "BAYONET", "Dildo"}[idx];
                    maxAmmo = new int[]{30, 10, 20, 12, 0, 1}[idx];
                    ammo = maxAmmo;
                }
            }
            int btnX = w / 2 - w / 8;
            int btnY = (int) (h / 1.3);
            int btnW = w / 4;
            int btnH = h / 12;
            if (x >= btnX && x <= btnX + btnW && y >= btnY && y <= btnY + btnH) {
                ctScore = 0;
                tScore = 0;
                resetRound();
                currentState = State.GAME;
            }
        } else if (currentState == State.SETTINGS) {
            if (y > (int) (h / 1.2)) {
                currentState = State.MENU;
            } else if (y > (int) (h / 1.7) && y < (int) (h / 1.5)) {
                isFullscreen = !isFullscreen;
                applyWindowMode();
            } else if (x < w / 3) {
                int idx = (int) ((y - h / 12.0) / (h / 20.0));
                if (idx >= 0 && idx < binds.size()) {
                    waitingForKey = (String) binds.keySet().toArray()[idx];
                }
            }
        } else if (currentState == State.CUSTOMIZE) {
            if (y > h / 3 && y < h / 3 + w / 15) {
                int idx = (int) ((x - w / 6.0) / (w / 10.0));
                Color[] cs = {Color.RED, Color.GREEN, Color.YELLOW, Color.BLACK, Color.WHITE, Color.BLUE, Color.PINK, new Color(255, 105, 180)};
                if (idx >= 0 && idx < cs.length) playerColor = cs[idx];
            }
            if (y > (int) (h * 0.6) && y < (int) (h * 0.6 + w / 15)) {
                int idx = (int) ((x - w / 6.0) / (w / 10.0));
                Color[] cs = {Color.RED, Color.GREEN, Color.YELLOW, Color.BLACK, Color.WHITE, Color.BLUE, Color.PINK, new Color(255, 105, 180)};
                if (idx >= 0 && idx < cs.length) {
                    menuBg1 = cs[idx];
                    menuBg2 = cs[idx].darker();
                }
            }
            if (y > (int) (h * 0.78) && y < (int) (h * 0.85)) {
                String[] glasses = {"NONE", "CLASSIC", "COOL", "CYBER"};
                int idx = (int) ((x - w / 5.0) / (w / 8.0));
                if (idx >= 0 && idx < glasses.length) selectedGlasses = glasses[idx];
            }
            if (y > (int) (h / 1.15)) {
                currentState = State.MENU;
            }
        } else if (currentState == State.PAUSE) {
            int yPos = h / 2;
            int step = h / 10;
            if (y > yPos - 30 && y < yPos + 30) currentState = State.GAME;
            else if (y > yPos + step - 30 && y < yPos + step + 30) {
                currentState = State.MENU;
                ctScore = 0;
                tScore = 0;
            } else if (y > yPos + step * 2 - 30 && y < yPos + step * 2 + 30) {
                System.exit(0);
            }
        }
    }

    void drawCenteredString(Graphics g, String text, int x, int y) {
        FontMetrics fm = g.getFontMetrics();
        int nx = x - fm.stringWidth(text) / 2;
        int ny = y - fm.getHeight() / 2 + fm.getAscent();
        g.drawString(text, nx, ny);
    }

    void resetRound() {
        hp = 100;
        ammo = maxAmmo;
        freezeTime = 5;
        roundTime = 150;
        px = 100;
        py = 100;
        winnerText = "";
        autoNextTimer = -1;
        dildoTimer = -1;
        reloading = false;
        enemies.clear();
        projectiles.clear();
        walls.clear();
        particles.clear();
        if (selectedMap.equals("de_pesok")) {
            walls.add(new Wall(400, 300, 200, 40));
            walls.add(new Wall(700, 150, 40, 300));
            walls.add(new Wall(200, 500, 300, 40));
        } else {
            walls.add(new Wall(600, 100, 40, 500));
            walls.add(new Wall(300, 200, 200, 40));
            walls.add(new Wall(800, 400, 40, 300));
        }
        for (int i = 0; i < 4; i++) {
            int ex, ey;
            boolean valid;
            do {
                valid = true;
                ex = rnd.nextInt(getWidth() - 180) + 60;
                ey = rnd.nextInt(getHeight() - 180) + 60;
                Rectangle r = new Rectangle(ex, ey, 30, 30);
                for (Wall w : walls) if (r.intersects(w.rect)) valid = false;
                if (Math.hypot(ex - px, ey - py) < 280) valid = false;
            } while (!valid);
            enemies.add(new Enemy(ex, ey));
        }
    }

    void activateDildoPower() {
        dildoTimer = -1;
        for (Enemy en : enemies) en.hp = 0;
        if (playerSide.equals("CT")) ctScore = 5;
        else tScore = 5;
        winnerText = "DILDO DOMINATION!";
        autoNextTimer = 4;
        for (int i = 0; i < 50; i++) {
            particles.add(new Particle(getWidth() / 2 + rnd.nextInt(400) - 200,
                    getHeight() / 2 + rnd.nextInt(300) - 150, Color.PINK));
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Game game = new Game();
            game.setVisible(true);
        });
    }
}