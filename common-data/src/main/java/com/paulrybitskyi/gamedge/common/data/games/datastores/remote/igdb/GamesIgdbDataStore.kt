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

package com.paulrybitskyi.gamedge.common.data.games.datastores.remote.igdb

import com.github.michaelbull.result.mapEither
import com.paulrybitskyi.gamedge.common.api.ApiResult
import com.paulrybitskyi.gamedge.core.providers.DispatcherProvider
import com.paulrybitskyi.gamedge.common.data.common.ApiErrorMapper
import com.paulrybitskyi.gamedge.common.data.common.DataResult
import com.paulrybitskyi.gamedge.common.data.common.Pagination
import com.paulrybitskyi.gamedge.common.data.games.DataCompany
import com.paulrybitskyi.gamedge.common.data.games.DataGame
import com.paulrybitskyi.gamedge.common.data.games.common.DiscoveryGamesReleaseDatesProvider
import com.paulrybitskyi.gamedge.common.data.games.datastores.remote.GamesRemoteDataStore
import com.paulrybitskyi.gamedge.igdb.api.games.ApiGame
import com.paulrybitskyi.gamedge.igdb.api.games.GamesEndpoint
import com.paulrybitskyi.gamedge.igdb.api.games.requests.GetComingSoonGamesRequest
import com.paulrybitskyi.gamedge.igdb.api.games.requests.GetGamesRequest
import com.paulrybitskyi.gamedge.igdb.api.games.requests.GetMostAnticipatedGamesRequest
import com.paulrybitskyi.gamedge.igdb.api.games.requests.GetPopularGamesRequest
import com.paulrybitskyi.gamedge.igdb.api.games.requests.GetRecentlyReleasedGamesRequest
import com.paulrybitskyi.gamedge.igdb.api.games.requests.SearchGamesRequest
import com.paulrybitskyi.hiltbinder.BindType
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.withContext

@Singleton
@BindType
internal class GamesIgdbDataStore @Inject constructor(
    private val gamesEndpoint: GamesEndpoint,
    private val releaseDatesProvider: DiscoveryGamesReleaseDatesProvider,
    private val dispatcherProvider: DispatcherProvider,
    private val igdbGameMapper: IgdbGameMapper,
    private val apiErrorMapper: ApiErrorMapper,
) : GamesRemoteDataStore {

    override suspend fun searchGames(searchQuery: String, pagination: Pagination): DataResult<List<DataGame>> {
        return gamesEndpoint
            .searchGames(
                SearchGamesRequest(
                    searchQuery = searchQuery,
                    offset = pagination.offset,
                    limit = pagination.limit,
                )
            )
            .toDataStoreResult()
    }

    override suspend fun getPopularGames(pagination: Pagination): DataResult<List<DataGame>> {
        return gamesEndpoint
            .getPopularGames(
                GetPopularGamesRequest(
                    minReleaseDateTimestamp = releaseDatesProvider.getPopularGamesMinReleaseDate(),
                    offset = pagination.offset,
                    limit = pagination.limit,
                )
            )
            .toDataStoreResult()
    }

    override suspend fun getRecentlyReleasedGames(pagination: Pagination): DataResult<List<DataGame>> {
        return gamesEndpoint
            .getRecentlyReleasedGames(
                GetRecentlyReleasedGamesRequest(
                    minReleaseDateTimestamp = releaseDatesProvider.getRecentlyReleasedGamesMinReleaseDate(),
                    maxReleaseDateTimestamp = releaseDatesProvider.getRecentlyReleasedGamesMaxReleaseDate(),
                    offset = pagination.offset,
                    limit = pagination.limit,
                )
            )
            .toDataStoreResult()
    }

    override suspend fun getComingSoonGames(pagination: Pagination): DataResult<List<DataGame>> {
        return gamesEndpoint
            .getComingSoonGames(
                GetComingSoonGamesRequest(
                    minReleaseDateTimestamp = releaseDatesProvider.getComingSoonGamesMinReleaseDate(),
                    offset = pagination.offset,
                    limit = pagination.limit,
                )
            )
            .toDataStoreResult()
    }

    override suspend fun getMostAnticipatedGames(pagination: Pagination): DataResult<List<DataGame>> {
        return gamesEndpoint
            .getMostAnticipatedGames(
                GetMostAnticipatedGamesRequest(
                    minReleaseDateTimestamp = releaseDatesProvider.getMostAnticipatedGamesMinReleaseDate(),
                    offset = pagination.offset,
                    limit = pagination.limit,
                )
            )
            .toDataStoreResult()
    }

    override suspend fun getCompanyDevelopedGames(
        company: DataCompany,
        pagination: Pagination
    ): DataResult<List<DataGame>> {
        return gamesEndpoint
            .getGames(
                GetGamesRequest(
                    gameIds = company.developedGames,
                    offset = pagination.offset,
                    limit = pagination.limit,
                )
            )
            .toDataStoreResult()
    }

    override suspend fun getSimilarGames(game: DataGame, pagination: Pagination): DataResult<List<DataGame>> {
        return gamesEndpoint
            .getGames(
                GetGamesRequest(
                    gameIds = game.similarGames,
                    offset = pagination.offset,
                    limit = pagination.limit,
                )
            )
            .toDataStoreResult()
    }

    private suspend fun ApiResult<List<ApiGame>>.toDataStoreResult(): DataResult<List<DataGame>> {
        return withContext(dispatcherProvider.computation) {
            mapEither(igdbGameMapper::mapToDataGames, apiErrorMapper::mapToDataError)
        }
    }
}
