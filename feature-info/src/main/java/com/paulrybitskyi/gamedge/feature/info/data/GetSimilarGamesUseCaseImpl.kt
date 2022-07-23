/*
 * Copyright 2022 Paul Rybitskyi, paul.rybitskyi.work@gmail.com
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

package com.paulrybitskyi.gamedge.feature.info.data

import com.paulrybitskyi.gamedge.core.providers.DispatcherProvider
import com.paulrybitskyi.gamedge.core.utils.resultOrError
import com.paulrybitskyi.gamedge.common.data.common.utils.toDataPagination
import com.paulrybitskyi.gamedge.common.data.games.datastores.local.GamesLocalDataStore
import com.paulrybitskyi.gamedge.common.data.games.usecases.common.GameMapper
import com.paulrybitskyi.gamedge.common.data.games.usecases.common.mapToDomainGames
import com.paulrybitskyi.gamedge.common.domain.games.entities.Game
import com.paulrybitskyi.gamedge.feature.info.domain.usecases.GetSimilarGamesUseCase
import com.paulrybitskyi.gamedge.feature.info.domain.usecases.GetSimilarGamesUseCase.Params
import com.paulrybitskyi.gamedge.feature.info.domain.usecases.RefreshSimilarGamesUseCase
import com.paulrybitskyi.hiltbinder.BindType
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEmpty

@Singleton
@BindType
internal class GetSimilarGamesUseCaseImpl @Inject constructor(
    private val refreshSimilarGamesUseCase: RefreshSimilarGamesUseCase,
    private val gamesLocalDataStore: GamesLocalDataStore,
    private val dispatcherProvider: DispatcherProvider,
    private val gameMapper: GameMapper
) : GetSimilarGamesUseCase {

    override suspend fun execute(params: Params): Flow<List<Game>> {
        return refreshSimilarGamesUseCase
            .execute(RefreshSimilarGamesUseCase.Params(params.game, params.pagination))
            .resultOrError()
            .onEmpty {
                val localSimilarGamesFlow = flow {
                    val dataGame = gameMapper.mapToDataGame(params.game)
                    val dataPagination = params.pagination.toDataPagination()

                    emit(gamesLocalDataStore.getSimilarGames(dataGame, dataPagination))
                }
                .flowOn(dispatcherProvider.main)
                .map(gameMapper::mapToDomainGames)
                .flowOn(dispatcherProvider.computation)

                emitAll(localSimilarGamesFlow)
            }
    }
}