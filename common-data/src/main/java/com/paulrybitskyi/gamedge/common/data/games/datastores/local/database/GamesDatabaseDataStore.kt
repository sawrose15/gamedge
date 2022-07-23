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

package com.paulrybitskyi.gamedge.common.data.games.datastores.local.database

import com.paulrybitskyi.gamedge.core.providers.DispatcherProvider
import com.paulrybitskyi.gamedge.common.data.common.Pagination
import com.paulrybitskyi.gamedge.common.data.games.DataCompany
import com.paulrybitskyi.gamedge.common.data.games.DataGame
import com.paulrybitskyi.gamedge.common.data.games.common.DiscoveryGamesReleaseDatesProvider
import com.paulrybitskyi.gamedge.common.data.games.datastores.local.GamesLocalDataStore
import com.paulrybitskyi.gamedge.database.games.DatabaseGame
import com.paulrybitskyi.gamedge.database.games.tables.GamesTable
import com.paulrybitskyi.hiltbinder.BindType
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

@Singleton
@BindType
internal class GamesDatabaseDataStore @Inject constructor(
    private val gamesTable: GamesTable,
    private val dispatcherProvider: DispatcherProvider,
    private val discoveryGamesReleaseDatesProvider: DiscoveryGamesReleaseDatesProvider,
    private val dbGameMapper: DbGameMapper,
) : GamesLocalDataStore {

    override suspend fun saveGames(games: List<DataGame>) {
        gamesTable.saveGames(
            withContext(dispatcherProvider.computation) {
                dbGameMapper.mapToDatabaseGames(games)
            }
        )
    }

    override suspend fun getGame(id: Int): DataGame? {
        return gamesTable.getGame(id)
            ?.let { databaseGame ->
                withContext(dispatcherProvider.computation) {
                    dbGameMapper.mapToDataGame(databaseGame)
                }
            }
    }

    override suspend fun getCompanyDevelopedGames(company: DataCompany, pagination: Pagination): List<DataGame> {
        return gamesTable.getGames(
            ids = company.developedGames,
            offset = pagination.offset,
            limit = pagination.limit
        )
        .toDataGames()
    }

    override suspend fun getSimilarGames(game: DataGame, pagination: Pagination): List<DataGame> {
        return gamesTable.getGames(
            ids = game.similarGames,
            offset = pagination.offset,
            limit = pagination.limit
        )
        .toDataGames()
    }

    override suspend fun searchGames(searchQuery: String, pagination: Pagination): List<DataGame> {
        return gamesTable.searchGames(
            searchQuery = searchQuery,
            offset = pagination.offset,
            limit = pagination.limit
        )
        .let { databaseGames ->
            withContext(dispatcherProvider.computation) {
                dbGameMapper.mapToDataGames(databaseGames)
            }
        }
    }

    override fun observePopularGames(pagination: Pagination): Flow<List<DataGame>> {
        return gamesTable.observePopularGames(
            minReleaseDateTimestamp = discoveryGamesReleaseDatesProvider.getPopularGamesMinReleaseDate(),
            offset = pagination.offset,
            limit = pagination.limit
        )
        .toDataGamesFlow()
    }

    override fun observeRecentlyReleasedGames(pagination: Pagination): Flow<List<DataGame>> {
        return gamesTable.observeRecentlyReleasedGames(
            minReleaseDateTimestamp = discoveryGamesReleaseDatesProvider.getRecentlyReleasedGamesMinReleaseDate(),
            maxReleaseDateTimestamp = discoveryGamesReleaseDatesProvider.getRecentlyReleasedGamesMaxReleaseDate(),
            offset = pagination.offset,
            limit = pagination.limit
        )
        .toDataGamesFlow()
    }

    override fun observeComingSoonGames(pagination: Pagination): Flow<List<DataGame>> {
        return gamesTable.observeComingSoonGames(
            minReleaseDateTimestamp = discoveryGamesReleaseDatesProvider.getComingSoonGamesMinReleaseDate(),
            offset = pagination.offset,
            limit = pagination.limit
        )
        .toDataGamesFlow()
    }

    override fun observeMostAnticipatedGames(pagination: Pagination): Flow<List<DataGame>> {
        return gamesTable.observeMostAnticipatedGames(
            minReleaseDateTimestamp = discoveryGamesReleaseDatesProvider.getMostAnticipatedGamesMinReleaseDate(),
            offset = pagination.offset,
            limit = pagination.limit
        )
        .toDataGamesFlow()
    }

    private suspend fun List<DatabaseGame>.toDataGames(): List<DataGame> {
        return withContext(dispatcherProvider.computation) {
            dbGameMapper.mapToDataGames(this@toDataGames)
        }
    }

    private fun Flow<List<DatabaseGame>>.toDataGamesFlow(): Flow<List<DataGame>> {
        return distinctUntilChanged()
            .map(dbGameMapper::mapToDataGames)
            .flowOn(dispatcherProvider.computation)
    }
}