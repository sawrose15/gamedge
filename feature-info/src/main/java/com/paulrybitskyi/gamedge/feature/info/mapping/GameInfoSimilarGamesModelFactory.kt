/*
 * Copyright 2020 Paul Rybitskyi, paul.rybitskyi.work@gmail.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.paulrybitskyi.gamedge.feature.info.mapping

import com.paulrybitskyi.gamedge.core.factories.IgdbImageSize
import com.paulrybitskyi.gamedge.core.factories.IgdbImageUrlFactory
import com.paulrybitskyi.gamedge.core.providers.StringProvider
import com.paulrybitskyi.gamedge.domain.games.entities.Game
import com.paulrybitskyi.gamedge.feature.info.R
import com.paulrybitskyi.gamedge.feature.info.widgets.main.model.games.GameInfoRelatedGameModel
import com.paulrybitskyi.gamedge.feature.info.widgets.main.model.games.GameInfoRelatedGamesModel
import com.paulrybitskyi.gamedge.feature.info.widgets.main.model.games.GameInfoRelatedGamesType
import com.paulrybitskyi.hiltbinder.BindType
import javax.inject.Inject

internal interface GameInfoSimilarGamesModelFactory {
    fun createSimilarGamesModel(similarGames: List<Game>): GameInfoRelatedGamesModel?
}

@BindType(installIn = BindType.Component.VIEW_MODEL)
internal class GameInfoSimilarGamesModelFactoryImpl @Inject constructor(
    private val stringProvider: StringProvider,
    private val igdbImageUrlFactory: IgdbImageUrlFactory
) : GameInfoSimilarGamesModelFactory {

    override fun createSimilarGamesModel(similarGames: List<Game>): GameInfoRelatedGamesModel? {
        if (similarGames.isEmpty()) return null

        return GameInfoRelatedGamesModel(
            type = GameInfoRelatedGamesType.SIMILAR_GAMES,
            title = stringProvider.getString(R.string.game_info_similar_games_title),
            items = similarGames.toRelatedGameModels()
        )
    }

    private fun List<Game>.toRelatedGameModels(): List<GameInfoRelatedGameModel> {
        return map {
            GameInfoRelatedGameModel(
                id = it.id,
                title = it.name,
                coverUrl = it.cover?.let { cover ->
                    igdbImageUrlFactory.createUrl(cover, IgdbImageUrlFactory.Config(IgdbImageSize.BIG_COVER))
                }
            )
        }
    }
}
