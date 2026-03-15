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
import java.util.Random;

public class Particle {
    double x, y, vx, vy;
    Color color;
    int life;
    private final Random rnd = new Random();

    Particle(double x, double y, Color c) {
        this.x = x;
        this.y = y;
        this.color = c;
        this.vx = rnd.nextDouble() * 5 - 2.5;
        this.vy = rnd.nextDouble() * 5 - 3;
        this.life = 18 + rnd.nextInt(18);
    }

    void update() {
        x += vx;
        y += vy;
        vy += 0.15;
        life--;
    }

    void draw(Graphics2D g) {
        g.setColor(color);
        int size = Math.max(2, life / 6);
        g.fillOval((int) x, (int) y, size, size);
    }
}