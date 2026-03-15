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

import java.awt.*;
import java.util.List;
import java.util.Random;

public class Enemy {
    int x, y, hp = 100;
    double dirX = 1;
    private final Random rnd = new Random();

    Enemy(int ex, int ey) {
        x = ex;
        y = ey;
    }

    void update(double px, double py, List<Projectile> projectiles, List<Wall> walls) {
        double dx = px - x;
        double dy = py - y;
        double dist = Math.hypot(dx, dy);
        if (dist < 25) return;
        dx /= dist;
        dy /= dist;
        double speed = 2.95;
        double bestNx = x;
        double bestNy = y;
        double bestScore = -999;
        for (int i = -6; i <= 6; i++) {
            double angle = i * 0.26;
            double testDx = dx * Math.cos(angle) - dy * Math.sin(angle);
            double testDy = dx * Math.sin(angle) + dy * Math.cos(angle);
            double nx = x + testDx * speed * 1.15;
            double ny = y + testDy * speed * 1.1;
            Rectangle nr = new Rectangle((int) nx, (int) ny, 28, 28);
            boolean collide = false;
            for (Wall w : walls) {
                if (nr.intersects(w.rect)) {
                    collide = true;
                    break;
                }
            }
            if (!collide) {
                double score = 12 - Math.abs(i) * 1.6;
                if (score > bestScore) {
                    bestScore = score;
                    bestNx = nx;
                    bestNy = ny;
                }
            }
        }
        if (bestScore < -50) {
            bestNx = x - dx * speed * 1.3 + (rnd.nextDouble() - 0.5) * 22;
            bestNy = y - dy * speed * 1.3 + (rnd.nextDouble() - 0.5) * 22;
        }
        x = (int) bestNx;
        y = (int) bestNy;
        dirX = dx;
        if (rnd.nextInt(32) == 0 && hasLineOfSight(px + 15, py + 15, walls)) {
            projectiles.add(new Projectile(x + 15, y + 15, (int) px + 15, (int) py + 15, false, Color.RED));
        }
    }

    private boolean hasLineOfSight(double tx, double ty, List<Wall> walls) {
        double dx = tx - x - 15;
        double dy = ty - y - 15;
        double dist = Math.hypot(dx, dy);
        if (dist < 40) return true;
        int steps = (int) (dist / 9) + 1;
        double stepX = dx / steps;
        double stepY = dy / steps;
        double cx = x + 15;
        double cy = y + 15;
        for (int i = 1; i < steps; i++) {
            cx += stepX;
            cy += stepY;
            Rectangle r = new Rectangle((int) cx - 6, (int) cy - 6, 12, 12);
            for (Wall w : walls) {
                if (w.rect.intersects(r)) return false;
            }
        }
        return true;
    }
}