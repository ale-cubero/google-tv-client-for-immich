/*
 * TV client for Immich — a read-only Android TV client for a self-hosted Immich server.
 * Copyright (C) 2026 Alejandro Cubero
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.immichtv.ui.theme

import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme

/**
 * Screen background: the theme's background color plus, when the theme defines one
 * ([AppColorScheme.pattern]), a faint diagonal maze. Use it instead of
 * `background(colorScheme.background)` on full-screen containers. The pattern is fixed to the
 * screen (it doesn't scroll with content) and is built once per size as a single path.
 */
@Composable
fun Modifier.screenBackground(): Modifier {
    val base = background(MaterialTheme.colorScheme.background)
    val color = AppColors.pattern
    if (color == Color.Transparent) return base
    return base.drawWithCache {
        val step = MazeStep.toPx()
        val tile = MazeSize * step
        val path = Path()
        // Tile the maze across the screen, one extra tile up/left for lines that cross tile edges.
        var ty = -tile
        while (ty < size.height) {
            var tx = -tile
            while (tx < size.width) {
                for (i in MazeEdges.indices step 3) {
                    val a = MazeEdges[i].digitToInt(36)
                    val b = MazeEdges[i + 1].digitToInt(36)
                    val d = MazeDirs[MazeEdges[i + 2].digitToInt()]
                    val x = tx + a * step
                    val y = ty + b * step
                    path.moveTo(x, y)
                    path.lineTo(x + d.first * step, y + d.second * step)
                }
                tx += tile
            }
            ty += tile
        }
        val stroke = Stroke(width = MazeStroke.toPx(), cap = StrokeCap.Square)
        onDrawBehind { drawPath(path, color, style = stroke) }
    }
}

private val MazeStep = 20.dp
private val MazeStroke = 1.dp
private const val MazeSize = 24
private val MazeDirs = listOf(1 to 1, 1 to -1, -1 to 1, -1 to -1)

// One seamless MazeSize x MazeSize tile of a 45° maze (a spanning tree that prefers straight runs,
// with a few gaps cut in). Each edge is 3 characters: its start vertex (column, row) in base 36,
// then its direction as an index into MazeDirs.
private const val MazeEdges =
    "0010210220620810a10e20g20k10m10m21321521711911b21f11f21j11l11n12222422812a22c22i32k1312330353371" +
    "3b03d33f13j13l13n14024224404634824a24c04e34g34i34k14m15115125505725925d05h35j15l15n2601623661662" +
    "6826c36e16e26g26i16k16m27117337537727927b27d17f27h17l27n18018238438628828e28g18i18k28m1930933953" +
    "9739939b19f19h19j29l19n0a00a02a40a63aa3ae3ag1ai2ak0b33b50b73b93bb0bd3bf1bh2bj1bl0bn3c03c22c61c83" +
    "ca3cc0ce3cg2ci0ck3cm0d32d51d93db3dd0df2dh0dn1e01e02e22e40e62e82ec3ee2eg2ei0ek2em1f33f51f52f72f91" +
    "fb3fd3ff1fh3fj2fl1fn1g01g23g43g62g80gc3gg3gk1gm1h10h13h33h50h73h90hb3hd0hd3hh3hj3i02i43i60i83ia2" +
    "ic2ie2ig1ii3ik0im0j11j71j93jb2jd1jf1jj3jl0jn2k42k61k82ka2kc1ke1kg2kk3km0l12l32l51l72l92lb1ld1lf2" +
    "lh2lj0ln0m00m23m43m62m82ma1me2mg2mi0mk0mm3n10n52n72n91nb1nd2nf2nh2nj0nl0nn3"
