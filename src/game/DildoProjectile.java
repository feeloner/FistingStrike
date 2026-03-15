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

import java.awt.Color;
import java.awt.Graphics2D;

public class DildoProjectile extends Projectile {
    float scale = 1f;

    DildoProjectile(int sx, int sy, int mx, int my, boolean player, Color c) {
        super(sx, sy, mx, my, player, c);
    }

    void setSpeed(double speed) {
        double len = Math.hypot(vx, vy);
        if (len > 0) {
            vx = vx / len * speed;
            vy = vy / len * speed;
        }
    }

    void setScale(float s) {
        scale = s;
    }

    void draw(Graphics2D g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.translate((int) x, (int) y);
        double ang = Math.atan2(vy, vx);
        g2.rotate(ang);
        g2.setColor(new Color(255, 105, 180));
        g2.fillRect(-12, -7, (int) (38 * scale), 14);
        g2.setColor(Color.RED);
        g2.fillOval((int) (26 * scale), -11, 22, 22);
        g2.dispose();
    }
}