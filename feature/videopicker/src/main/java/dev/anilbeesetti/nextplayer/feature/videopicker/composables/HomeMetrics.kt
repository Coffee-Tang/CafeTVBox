package dev.anilbeesetti.nextplayer.feature.videopicker.composables

import androidx.compose.ui.unit.dp

/**
 * The few measures the home screen's rows and poster grid have to agree on. They are here rather
 * than in each composable because the agreement is the point: a card that is not as wide as a poster,
 * or a row that starts further in than the grid, reads as a mistake no matter how good each looks
 * on its own.
 */

/** Where every list on the home screen begins, so all of them share one left edge. */
val HOME_EDGE_MARGIN = 16.dp

/** How wide a card is, in the rows and in the grid alike, so their columns line up down the screen. */
val HOME_CARD_WIDTH = 132.dp
