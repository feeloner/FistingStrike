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

public class Projectile {
    double x, y, vx, vy;
    int life = 55;
    boolean isPlayer;
    Color color;

    Projectile(int sx, int sy, int mx, int my, boolean player, Color c) {
        x = sx;
        y = sy;
        double dx = mx - sx;
        double dy = my - sy;
        double dist = Math.hypot(dx, dy);
        if (dist > 0) {
            vx = dx / dist * 16;
            vy = dy / dist * 16;
        }
        isPlayer = player;
        color = c;
    }

    void update() {
        x += vx;
        y += vy;
        life--;
    }
}